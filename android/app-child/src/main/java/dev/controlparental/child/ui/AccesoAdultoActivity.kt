package dev.controlparental.child.ui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import dev.controlparental.child.admin.PolicyEnforcer
import dev.controlparental.child.emergency.EmergencyAccess
import dev.controlparental.child.local.*
import dev.controlparental.child.lock.BloqueoLocalAndroid
import dev.controlparental.child.lock.FaseLocal
import dev.controlparental.child.lock.ModoLocal
import dev.controlparental.child.protection.ProteccionAndroid
import dev.controlparental.child.boot.ArranquePilotoAndroid
import dev.controlparental.child.boot.FasePiloto
import dev.controlparental.child.network.ControlRedAndroid
import dev.controlparental.child.network.UsoLocal
import dev.controlparental.child.network.ValidacionRed
import dev.controlparental.child.network.MinutosUso
import dev.controlparental.child.pairing.*
import dev.controlparental.child.channel.PilotoE2eAndroid
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import java.util.concurrent.Executors
import dev.controlparental.child.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Componente MAIN no exportado, sin extras autoritativos ni estado secreto en Bundle. */
class AccesoAdultoActivity : ComponentActivity() {
    private val sesion = SesionAdulto(SystemClock::elapsedRealtime) {
        Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
    }
    private val consulta = AtomicInteger()
    private val bootstrap = AtomicBoolean()
    private var prompt: CancellationSignal? = null
    private var alta: TicketAdulto? = null
    private var resumen = ResultadoAdulto(TipoAdulto.ERROR)
    private lateinit var columna: LinearLayout
    private val entradas = mutableListOf<EditText>()
    private val handler = Handler(Looper.getMainLooper())
    private var activa = false
    private var pilotoCp2: PilotoE2eAndroid? = null
    private var estadoCanalCp2: TextView? = null
    private var mensajePublico = ""
    private var ofertas: OfertasRam? = null
    private var imagenOferta: ImageView? = null
    private var referenciaPair: ReferenciaOferta? = null
    private var casoPair: ValidacionPairRam? = null
    private var camaraPair: CamaraPairCp2? = null
    private var framePair: ByteArray? = null
    @Volatile private var canalC8: dev.controlparental.child.pairing.CanalEncuentroC8Android? = null
    private var origenC8Confirmado: String? = null
    private var sesionVinculacion: SesionAdulto? = null
    private var marcoVinculacion: dev.controlparental.child.local.MarcoSesionAdulto? = null
    private var relojVinculacion: TextView? = null
    private val navegacion = NavegacionLocal()
    private val guardiaVista = GuardiaVistaLocal()
    private val revisionVista get() = guardiaVista.revision
    private var resumenVista: TextView? = null
    private val refresco = object : Runnable {
        override fun run() {
            if (!activa || resumenVista == null) return
            resumenVista?.text = resumenActual().let { "${it.estado}\n${it.tiempo}\n${it.modalidad}\n${it.proteccion}" }
            handler.postDelayed(this, 1000)
        }
    }
    private fun resumenActual(): ResumenLocal = presentarEstadoLocal(
        try { BloqueoLocalAndroid.modo(this) } catch (_: RuntimeException) { null },
        ControlRedAndroid.uso, ControlRedAndroid.permiteUsoRAM, ControlRedAndroid.vencimientoUso,
        SystemClock.elapsedRealtime(), ProteccionAndroid.estado)
    private val trabajoOferta = Executors.newSingleThreadExecutor { Thread(it, "oferta-local") }
    private val caducidad = object : Runnable {
        override fun run() {
            if (!activa) return
            val c8 = sesionVinculacion
            val caducada = if (c8 != null) !c8.vinculacionVigente() else
                (alta != null && !sesion.vigente(alta!!)) || (alta == null && sesion.estado() == AutoridadAdulto.CERRADA)
            if (caducada) {
                cerrarAutoridad(); sesion.reanudar(); mensajePublico = "Sesión terminada. Entra de nuevo para administrar."; consultar()
            } else { actualizarRelojVinculacion(); handler.postDelayed(this, 1000) }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        columna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(EstiloLocal.dp(this@AccesoAdultoActivity, 24), EstiloLocal.dp(this@AccesoAdultoActivity, 32),
                EstiloLocal.dp(this@AccesoAdultoActivity, 24), EstiloLocal.dp(this@AccesoAdultoActivity, 24))
            setBackgroundColor(EstiloLocal.fondo)
        }
        setContentView(ScrollView(this).apply {
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            isSaveEnabled = false; addView(columna)
        })
    }
    override fun onResume() { super.onResume(); activa = true; sesion.reanudar(); consultar() }
    override fun onPause() { activa = false; cerrarAutoridad(); limpiar(); super.onPause() }
    override fun onDestroy() { cerrarAutoridad(); trabajoOferta.shutdown(); super.onDestroy() }

    private fun limpiar() {
        guardiaVista.retirar(); resumenVista = null; handler.removeCallbacks(refresco)
        relojVinculacion = null
        camaraPair?.close(); camaraPair = null
        imagenOferta?.setImageDrawable(null); imagenOferta = null
        entradas.forEach { it.text.clear() }; entradas.clear(); columna.removeAllViews()
    }
    private fun cerrarAutoridad() {
        sesionVinculacion?.cerrar(); sesionVinculacion = null; marcoVinculacion = null
        ofertas?.cancelar()
        canalC8?.close(); canalC8 = null; origenC8Confirmado = null
        pilotoCp2?.close(); pilotoCp2 = null; estadoCanalCp2 = null
        camaraPair?.close(); camaraPair = null
        framePair?.fill(0); framePair = null; casoPair?.cancelar(); casoPair = null; referenciaPair = null
        imagenOferta?.setImageDrawable(null)
        ofertas?.cancelar(); ofertas = null
        consulta.incrementAndGet(); bootstrap.set(false); alta = null
        prompt?.cancel(); prompt = null; sesion.cerrar(); handler.removeCallbacksAndMessages(null)
    }
    private fun base(titulo: String) {
        limpiar(); columna.addView(TextView(this).apply { text = titulo; EstiloLocal.texto(this, titulo = true) })
        boton("Emergencia · pantalla del sistema") {
            cerrarAutoridad()
            EmergencyAccess(PolicyEnforcer(this)).irAPantallaBloqueo()
            if (activa) { sesion.reanudar(); consultar() }
        }
        boton("Cerrar acceso adulto") { cerrarAutoridad(); finish() }
        if (sesionVinculacion != null) {
            relojVinculacion = TextView(this).also { it.textSize = 20f; columna.addView(it) }
            referenciaPair?.let { ref ->
                texto("Huella HIJO LOCAL: ${dev.controlparental.protocol.cp2.CodecPairCp2.huellaPublica(ref.contexto.identidad)}\nOrigen LOCAL: ${origenC8Confirmado ?: "por confirmar"}")
            }
            actualizarRelojVinculacion()
        }
    }
    private fun actualizarRelojVinculacion() {
        val marco = marcoVinculacion
        if (marco == null) { relojVinculacion?.text = "Inicio C8/2: requiere código adulto fresco; aún no hay marco de diez minutos."; return }
        val n = SystemClock.elapsedRealtime()
        fun tiempo(limite: Long): String { val s = ((limite - n).coerceAtLeast(0) + 999) / 1000; return "%02d:%02d".format(s / 60, s % 60) }
        relojVinculacion?.text = "Asistente C8/2: ${tiempo(marco.limite)}" +
            (canalC8?.let { "; etapa/encuentro: ${tiempo(minOf(marco.limite, it.limiteVigencia))}" } ?: "") +
            ". El servidor puede acortar el encuentro; no se renueva."
    }
    private fun mostrarEstadoVinculoLocal() {
        val vista = TextView(this).apply { text = "Consultando registro local, sin generar oferta…"; textSize = 18f; columna.addView(this) }
        val n = consulta.get()
        trabajoOferta.execute {
            val carga = PairAndroid.cargar(applicationContext)
            runOnUiThread { if (activa && consulta.get() == n && vista.parent === columna) vista.text = dev.controlparental.child.pairing.descripcionEstadoPair(carga) }
        }
    }
    private fun texto(s: String) { columna.addView(TextView(this).apply { text = s; EstiloLocal.texto(this) }) }
    private fun boton(s: String, habilitado: Boolean = true, accion: () -> Unit = {}) {
        val vista = revisionVista
        columna.addView(Button(this).apply {
            text = s; isEnabled = habilitado; EstiloLocal.boton(this)
            setOnClickListener {
                guardiaVista.ejecutar(vista, activa, accion)
            }
        })
    }
    private fun campo(titulo: String, codigo: Boolean = false, secreto: Boolean = true, maximo: Int = 256): EditText {
        texto(titulo)
        return EditText(this).apply {
            inputType = if (codigo) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT or if (secreto) InputType.TYPE_TEXT_VARIATION_PASSWORD
                    else InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(if (codigo) 10 else maximo))
            setTextColor(EstiloLocal.tinta); setHintTextColor(EstiloLocal.tinta)
            textSize = 20f; minHeight = EstiloLocal.dp(context, 56)
            isSaveEnabled = false; isSaveFromParentEnabled = false; isLongClickable = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            val sinClipboard = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = false
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
                override fun onDestroyActionMode(mode: ActionMode?) {}
            }
            customSelectionActionModeCallback = sinClipboard; customInsertionActionModeCallback = sinClipboard
            entradas.add(this); columna.addView(this)
        }
    }
    private fun consultar() {
        if (!activa) return
        base("Comprobando acceso…")
        val n = consulta.incrementAndGet()
        val aceptado = AdultoAndroid.ejecutar(applicationContext, { it.resumen() }) { r ->
            runOnUiThread {
                if (activa && consulta.get() == n) {
                    resumen = r
                    ProteccionAndroid.consultar(applicationContext) {
                        ArranquePilotoAndroid.consultar(applicationContext) {
                            ControlRedAndroid.consultar(applicationContext) {
                                if (activa && consulta.get() == n) publica(mensajePublico)
                            }
                        }
                    }
                }
            }
        }
        if (!aceptado) { texto("Operación anterior en curso. No se ha abierto acceso."); boton("Actualizar") { consultar() } }
    }
    private fun publica(mensaje: String = "") {
        base("Acceso adulto")
        if (mensaje.isNotEmpty()) texto(mensaje)
        when (resumen.tipo) {
            TipoAdulto.SIN_CONFIGURAR -> {
                texto("Crea un código privado y una pregunta de recuperación. Primero confirma tu credencial de Android.")
                boton("Configurar acceso adulto") { autenticarAlta() }
            }
            TipoAdulto.CONFIGURADO -> {
                if (resumen.esperaMs > 0) texto("Espera de seguridad activa. Código y respuesta comparten el límite de intentos.")
                val codigo = campo("Código adulto · 5–10 dígitos", codigo = true)
                boton("Entrar", resumen.esperaMs == 0L) {
                    verificar(codigo.text.toString(), false)
                }
                boton("Olvidé mi código", resumen.esperaMs == 0L) { recuperar() }
                if (resumen.esperaMs > 0) boton("Comprobar espera") { consultar() }
            }
            else -> { texto("No se puede comprobar el acceso. No se han cambiado tus datos."); boton("Volver a comprobar") { consultar() } }
        }
    }
    private fun autenticarAlta() {
        try {
            val dpm = getSystemService(DevicePolicyManager::class.java)
            val kg = getSystemService(KeyguardManager::class.java)
            check(dpm.isDeviceOwnerApp(packageName) && kg.isDeviceSecure && !kg.isKeyguardLocked)
            val t = sesion.solicitar(FinAdulto.ALTA) ?: error("Contexto cerrado")
            val cancelacion = CancellationSignal().also { prompt = it }
            BiometricPrompt.Builder(this).setTitle("Configuración presencial del adulto")
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build().authenticate(cancelacion, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (prompt !== cancelacion || !activa || !sesion.vigente(t) ||
                            result.authenticationType != BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL) return
                        prompt = null; alta = t; bootstrap.set(true); formularioAlta()
                        handler.postDelayed(caducidad, 1000)
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (prompt !== cancelacion) return
                        prompt = null; cerrarAutoridad()
                        if (activa) { sesion.reanudar(); publica("No se pudo autorizar la operación.") }
                    }
                })
        } catch (_: RuntimeException) {
            cerrarAutoridad(); sesion.reanudar(); publica("No se pudo autorizar la operación.")
        }
    }
    private fun formularioAlta() {
        base("Configurar acceso adulto")
        texto("La respuesta se compara exactamente: mayúsculas, acentos y espacios cuentan; no se normaliza. Una respuesta conocida por el niño permite cambiar el código.")
        val c = campo("Código nuevo: 5–10 dígitos ASCII", codigo = true)
        val cc = campo("Confirmar código", codigo = true)
        val p = campo("Pregunta personalizada (máximo200 caracteres)", secreto = false, maximo = 200)
        val r = campo("Respuesta secreta (máximo256 caracteres)")
        val rr = campo("Confirmar respuesta exacta")
        boton("Guardar configuración local") {
            val t = alta ?: return@boton
            val codigo = c.text.toString(); val confirmar = cc.text.toString(); val pregunta = p.text.toString()
            val respuesta = r.text.toString(); val confirmarRespuesta = rr.text.toString()
            operar(t) { it.configurar(codigo, confirmar, pregunta, respuesta, confirmarRespuesta) {
                bootstrap.get() && sesion.vigente(t) &&
                    getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(packageName)
            } }
        }
        boton("Cancelar alta") { cerrarAutoridad(); sesion.reanudar(); consultar() }
    }
    private fun recuperar() {
        base("Recuperación local offline")
        texto(resumen.pregunta)
        texto("Respuesta exacta: mayúsculas, acentos y espacios cuentan. Solo permite establecer otro código, nunca consultar el anterior ni abrir uso.")
        val r = campo("Respuesta secreta")
        boton("Verificar respuesta") { verificar(r.text.toString(), true) }
        boton("Cancelar recuperación") { cerrarAutoridad(); sesion.reanudar(); consultar() }
    }
    private fun verificar(texto: String, respuesta: Boolean) {
        val t = sesion.solicitar(if (respuesta) FinAdulto.RECUPERAR else FinAdulto.ENTRAR) ?: return
        operar(t) { it.verificar(texto, respuesta) { sesion.vigente(t) } }
    }
    private fun nuevoCodigo() {
        base("Establecer código nuevo")
        texto("La pregunta/respuesta permanecen; no se revela el código anterior. Guardar cierra esta capacidad; después entra con el nuevo código.")
        val c = campo("Código nuevo: 5–10 dígitos", codigo = true); val cc = campo("Confirmar", codigo = true)
        boton("Guardar código nuevo") {
            val revision = sesion.revisionRecuperacion() ?: return@boton
            val t = sesion.solicitar(FinAdulto.CAMBIAR) ?: return@boton
            val codigo = c.text.toString(); val confirmar = cc.text.toString()
            operar(t) { it.cambiarCodigo(codigo, confirmar, revision) { sesion.vigente(t) } }
        }
        boton("Cancelar cambio") { cerrarAutoridad(); sesion.reanudar(); consultar() }
    }
    private fun administrar() {
        if (!navegacion.abrir(PantallaLocal.CONTROL, sesion.estado() == AutoridadAdulto.ADMINISTRAR)) { consultar(); return }
        base("Control del teléfono")
        mostrarResumen()
        boton("BLOQUEAR AHORA") { confirmarBloqueo(if (ControlRedAndroid.activo) FinAdulto.BLOQUEAR_RED else FinAdulto.BLOQUEAR) }
        boton("PERMITIR LIBRE", ControlRedAndroid.listo) { elegirPermiso(false) }
        boton("PERMITIR POR TIEMPO", ControlRedAndroid.listo) { elegirPermiso(true) }
        if (!ControlRedAndroid.listo) texto("Para permitir uso, prepara el control en Seguridad. No se abrirá el teléfono automáticamente.")
        boton("Seguridad y configuración") { seguridad() }
        boton("Actualizar estado") { actualizarAdministracion() }
        texto("Sesión de 2 minutos. Salir no cancela un permiso confirmado.")
    }
    private fun mostrarResumen() {
        resumenVista = TextView(this).also { EstiloLocal.texto(it); columna.addView(it) }
        handler.removeCallbacks(refresco); refresco.run()
    }
    private fun actualizarAdministracion() {
        val vista = revisionVista
        ProteccionAndroid.consultar(applicationContext) {
            ControlRedAndroid.consultar(applicationContext) {
                if (activa && revisionVista == vista && sesion.estado() == AutoridadAdulto.ADMINISTRAR) administrar()
            }
        }
    }
    private fun elegirPermiso(temporal: Boolean) {
        if (sesion.estado() != AutoridadAdulto.ADMINISTRAR) { consultar(); return }
        base(if (temporal) "Permitir por tiempo" else "Permitir libre")
        texto("Elige cómo se usará el teléfono. Después confirmarás con tu código.")
        boton("Normal · con internet", ControlRedAndroid.listo && ControlRedAndroid.validacion == ValidacionRed.VALIDADA) {
            confirmarBloqueo(if (temporal) FinAdulto.USO_TIEMPO_NORMAL else FinAdulto.USO_NORMAL)
        }
        boton("Excepción · sin internet", ControlRedAndroid.listo) {
            confirmarBloqueo(if (temporal) FinAdulto.USO_TIEMPO_OFFLINE else FinAdulto.USO_OFFLINE)
        }
        if (ControlRedAndroid.validacion != ValidacionRed.VALIDADA) texto("Internet no está validada. Puedes volver a comprobar o autorizar una excepción local.")
        boton("Volver al control") { administrar() }
    }
    private fun seguridad() {
        if (!navegacion.abrir(PantallaLocal.SEGURIDAD, sesion.estado() == AutoridadAdulto.ADMINISTRAR)) { consultar(); return }
        base("Seguridad y configuración")
        mostrarResumen()
        boton("Activar protección de la app", !ProteccionAndroid.estado.snapshot) { confirmarBloqueo(FinAdulto.PROTEGER_APP) }
        boton("Restaurar protección anterior", ProteccionAndroid.estado.snapshot) { confirmarBloqueo(FinAdulto.RESTAURAR_APP) }
        val modo = try { BloqueoLocalAndroid.modo(this) } catch (_: RuntimeException) { null }
        val preparar = !ArranquePilotoAndroid.enrolado && !ControlRedAndroid.activo && modo == ModoLocal.NONE
        val reanudar = ArranquePilotoAndroid.enrolado && ControlRedAndroid.activo && !ControlRedAndroid.listo && ControlRedAndroid.uso == UsoLocal.BLOQUEADO
        boton(if (reanudar) "Reanudar control local" else "Preparar control local", preparar || reanudar) { confirmarBloqueo(FinAdulto.ARMAR_RED) }
        texto("Preparar deja el teléfono bloqueado, mantiene el aviso del sistema y exige nueva autorización tras reiniciar.")
        boton("Cambiar código mediante mi pregunta") { cerrarAutoridad(); sesion.reanudar(); recuperar() }
        boton("Diagnóstico y recuperación avanzada") { diagnosticoLocal() }
        boton("Volver al control") { administrar() }
    }
    private fun diagnosticoLocal() {
        if (!navegacion.abrir(PantallaLocal.DIAGNOSTICO, sesion.estado() == AutoridadAdulto.ADMINISTRAR)) { consultar(); return }
        base("Diagnóstico local")
        val modo = try { BloqueoLocalAndroid.modo(this) } catch (_: RuntimeException) { null }
        texto("Lock Task: $modo\nTransición: ${BloqueoLocalAndroid.diagnostico}")
        texto("Protección: ${ProteccionAndroid.estado}\nArranque: ${ArranquePilotoAndroid.fase}, enrolado=${ArranquePilotoAndroid.enrolado}")
        texto("Red: ${ControlRedAndroid.validacion}; uso=${ControlRedAndroid.uso}; listo=${ControlRedAndroid.listo}; vencimiento=${ControlRedAndroid.vencimientoUso}")
        texto("Vinculación y control remoto pausados. Los registros existentes se conservan.")
        boton("Consultar vínculo guardado · sin conexión") { mostrarEstadoVinculoLocal() }
        boton("Salida adulta del bloqueo inicial", modo == ModoLocal.LOCKED && !ArranquePilotoAndroid.enrolado) { confirmarBloqueo(FinAdulto.SALIR_ENSAYO) }
        boton("Desactivar control local y restaurar", ControlRedAndroid.activo) { confirmarBloqueo(FinAdulto.FINALIZAR_RED) }
        boton("Restaurar configuración de arranque", ArranquePilotoAndroid.enrolado && !ControlRedAndroid.activo) { confirmarBloqueo(FinAdulto.FINALIZAR_BOOT) }
        texto("La protección usa APIs oficiales. No impide root o un restablecimiento físico. No restringe globalmente la instalación; compatibilidad de actualizaciones pendiente de prueba física.")
        boton("Volver a Seguridad") { seguridad() }
    }
    private fun confirmarCanalCp2() {
        if (sesion.estado() != AutoridadAdulto.ADMINISTRAR) return
        base("Conexión piloto CP/2 explícita")
        texto("Requiere pareja CP/2 ya guardada y ensayo red/local armado. No crea claves, QR ni pareja. Recibirá LOCK/ALLOW/ALLOW_FOR con autoridad del padre vinculado. Salir, fondo o caducar la sesión cierra el canal, sin renovar permiso ni reintentar órdenes. ACK pendiente no prueba aplicación.")
        val codigo = campo("Código adulto fresco", codigo = true)
        boton("Confirmar conexión piloto con mi código") {
            val t = sesion.solicitar(FinAdulto.CONECTAR_CP2) ?: return@boton
            val secreto = codigo.text.toString(); base("Verificando autoridad de conexión…")
            val n = consulta.incrementAndGet()
            AdultoAndroid.ejecutar(applicationContext, { it.verificar(secreto, false) { sesion.vigente(t) } }) { r ->
                runOnUiThread {
                    if (!activa || consulta.get() != n) return@runOnUiThread
                    if (!sesion.autorizarAccion(t, r)) { cerrarAutoridad(); sesion.reanudar(); publica("Conexión no autorizada."); return@runOnUiThread }
                    val marco = sesion.marcoAcreditado(t) ?: return@runOnUiThread
                    pilotoCp2?.close()
                    base("Piloto CP/2 foreground — sin prueba física")
                    texto("Historia/avisos: NO DISPONIBLE. STATE/ACK firmados no implican recuperación ni perfección. APPLIED sólo tras observación correlacionada.")
                    val vista = TextView(this).apply { setText(R.string.preparando_control_cp2); textSize = 18f }
                    estadoCanalCp2 = vista; columna.addView(vista)
                    val p = PilotoE2eAndroid(this, { activa && sesion.marcoVigente(marco) }) { vista.text = it }
                    pilotoCp2 = p; p.conectar()
                    boton("Desconectar piloto — no cambia permiso de uso") { p.close(); pilotoCp2 = null; administrar() }
                    boton("Administración local — misma sesión") { administrar() }
                }
            }
        }
    }
    private fun confirmarOferta(inicializar: Boolean, c8: Boolean = false) {
        if (sesion.estado() != AutoridadAdulto.ADMINISTRAR) { consultar(); return }
        val autorizacion = if (c8) SesionAdulto(SystemClock::elapsedRealtime,
            { Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1) }, soloC8V2 = true).also {
                sesion.cerrar(); it.reanudar(); sesionVinculacion = it
            } else sesion
        val inicioC8 = if (c8) autorizacion.solicitar(FinAdulto.GENERAR_OFERTA) else null
        ofertas?.cancelar(); ofertas = null
        base(if (inicializar) "Inicialización presencial de identidad" else "Generar oferta CP/2 local")
        texto("Requiere código fresco. Generar no vincula, no migra CP/1, no concede uso ni cambia políticas. Recibir/confirmar/guardar exigen pasos separados.")
        texto(if (inicializar) "Solo ausencia inicial coherente, sin vínculo ni restos. Crea privateSigning P-256 no exportable una vez; no repara claves perdidas. Requiere autorización física concreta antes de usar este botón. No genera QR."
            else if (c8) "Al confirmar código fresco empieza un marco NUEVO de hasta10 minutos exclusivo de vinculación. No amplía administración, órdenes ni permiso de uso. Cada validación/guardado exige otro código. Fondo/salir/cancelar cierra."
            else "Oferta RAM hasta5 minutos; la sesión adulta de2 minutos puede cerrarla antes. Fondo, salir, cancelar, caducidad o contexto incierto retiran QR. No compartir/capturar/copiar. Requiere autorización física concreta para mostrarla.")
        val origenLocal = if (c8) campo("Origen HTTPS local acordado, no desde PAIR", secreto = false, maximo = 200) else null
        if (c8) texto("C8 requiere servicio de enrolamiento habilitado posteriormente. No conectar al bootstrap sin ese despliegue autorizado. No crea identidad ni usa cámara. Si no puedes leer/comparar huellas completas, detente.")
        val codigo = campo("Código adulto fresco", codigo = true)
        boton("Confirmar operación separada con mi código") {
            val origen = origenLocal?.text?.toString()
            if (c8 && try { dev.controlparental.protocol.cp2.CodecPairCp2.origenExacto(origen!!); false } catch (_: Exception) { true }) {
                texto("Origen HTTPS local inválido; no se abre transporte."); return@boton
            }
            val fin = if (inicializar) FinAdulto.INICIALIZAR_IDENTIDAD else FinAdulto.GENERAR_OFERTA
            val t = (if (c8) inicioC8 else autorizacion.solicitar(fin)) ?: return@boton
            val secreto = codigo.text.toString(); base("Verificando autoridad…")
            val n = consulta.incrementAndGet()
            val aceptada = AdultoAndroid.ejecutar(applicationContext, { it.verificar(secreto, false) { autorizacion.vigente(t) } }) { r ->
                runOnUiThread {
                    if (!activa || consulta.get() != n) return@runOnUiThread
                    if (!autorizacion.autorizarAccion(t, r)) { cerrarOferta("Operación no autorizada."); return@runOnUiThread }
                    val identidad = IdentidadOfertaAndroid(applicationContext)
                    val marco = autorizacion.marcoAcreditado(t)
                    if (c8) marcoVinculacion = marco
                    val controlador = OfertasRam(SystemClock::elapsedRealtime, { identidad.leer().contexto },
                        { autorizacion.accionVigente(t) && PairAndroid.cargar(applicationContext) is CargaPairCp2.Ausente },
                        vidaSesion = { marco != null && autorizacion.marcoVigente(marco) && (!c8 || canalC8?.disponible != false) },
                        marcoC8V2 = if (c8) marco else null)
                    ofertas = controlador
                    casoPair = ValidacionPairRam(controlador, autorizacion)
                    trabajoOferta.execute {
                         val creada = if (inicializar) identidad.inicializar {
                             autorizacion.accionVigente(t) && PairAndroid.cargar(applicationContext) is CargaPairCp2.Ausente
                         } else false
                        val ref = if (!inicializar) try { controlador.generar() } catch (_: Exception) { null } else null
                        val matriz = if (c8) null else try { ref?.let { QrOfertaCp2.crear(CodecOfertaCp2.escribir(it.oferta)) } } catch (_: Exception) { null }
                        runOnUiThread {
                            if (!activa || consulta.get() != n || !autorizacion.accionVigente(t)) { controlador.cancelar(); return@runOnUiThread }
                            if (inicializar) cerrarOferta(if (creada) "Identidad inicializada con readback. Generar oferta exige otra autorización."
                                else "NO_DISPONIBLE: estado incierto, identidad existente o creación no completada. No se reemplaza ninguna clave.")
                            else if (ref == null || (!c8 && matriz == null)) cerrarOferta("NO_DISPONIBLE: identidad ausente o vínculo/contexto incierto. Inicialización, si procede, es otra operación.")
                            else if (c8) {
                                referenciaPair = ref
                                prepararC8(ref, marco!!, origen!!)
                            }
                            else {
                                checkNotNull(matriz)
                                referenciaPair = ref
                                base("CHILD_OFFER CP/2 — no firmado, no vinculado")
                                texto("Solo ceremonia presencial CP/2. No QR Android de aprovisionamiento. Guardar vínculo requiere autorización física nueva para ensayo; no usar sin coordinar.")
                                val lado = matriz.width * 6
                                val bitmap = createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
                                for (y in 0 until lado) for (x in 0 until lado)
                                    bitmap[x, y] = if (matriz[x / 6, y / 6]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                imagenOferta = ImageView(this).apply {
                                    isSaveEnabled = false; isSaveFromParentEnabled = false
                                    setImageBitmap(bitmap); adjustViewBounds = true
                                    contentDescription = "Oferta CP/2 local protegida, no concede uso"
                                    columna.addView(this)
                                }
                                val restante = TextView(this).also { columna.addView(it) }
                                boton("Recibir PAIR_ACCEPT por cámara propia — código fresco") { recibirPair() }
                                boton("Cancelar y retirar oferta") { cerrarOferta("Oferta retirada.") }
                                boton("Nueva oferta explícita — requiere otra autenticación") {
                                    cerrarOferta("Oferta retirada. Entra de nuevo para generar otra referencia.")
                                }
                                handler.postDelayed(caducidad, 1000)
                                actualizarOferta(controlador, ref, restante, n)
                            }
                        }
                    }
                }
            }
            if (!aceptada) cerrarOferta("Otra operación en curso; no se ha generado oferta.")
        }
        boton("Cancelar") { cerrarOferta("Operación cancelada.") }
        if (c8) handler.postDelayed(caducidad, 100)
    }
    private fun prepararC8(ref: ReferenciaOferta, marco: MarcoSesionAdulto, origen: String) {
        val autorizacion = sesionVinculacion ?: return
        origenC8Confirmado = origen
        base("Identidad LOCAL del hijo — antes de recibir candidata")
        texto("Huella HIJO LOCAL completa: ${dev.controlparental.protocol.cp2.CodecPairCp2.huellaPublica(ref.contexto.identidad)}")
        texto("Origen LOCAL elegido: $origen\nMuéstralos al propietario y al padre conocido. Código8/PoP no identifican personas ni familia. Si no pueden leer ambos textos, cancelar.")
        boton("He mostrado mi huella LOCAL; abrir encuentro C8 explícitamente") {
            if (!autorizacion.marcoVigente(marco) || ofertas?.vigente(ref) != true || canalC8 != null) {
                cerrarOferta("Contexto no disponible; no se abre C8."); return@boton
            }
            base("Etapa1: encuentro C8/2 iniciado — sin permiso de uso")
            texto("Huella HIJO LOCAL: ${dev.controlparental.protocol.cp2.CodecPairCp2.huellaPublica(ref.contexto.identidad)}\nOrigen LOCAL: $origen")
            texto("Esperando transporte. No hay reintento automático; la sesión original sigue corriendo.")
            boton("Cancelar encuentro") { cerrarOferta("Encuentro cancelado; sin borrar pendientes.") }
            lateinit var enlace: dev.controlparental.child.pairing.CanalEncuentroC8Android
            fun vigente() = activa && referenciaPair === ref && canalC8 === enlace && enlace.disponible &&
                autorizacion.marcoVigente(marco) && ofertas?.vigente(ref) == true
            enlace = dev.controlparental.child.pairing.CanalEncuentroC8Android(applicationContext, origen, ref, marco.limite,
                { referenciaPair === ref && autorizacion.marcoVigente(marco) && ofertas?.vigente(ref) == true },
                { codigo8 -> runOnUiThread {
                    if (!vigente()) return@runOnUiThread
                    base("Etapa1: código8 temporal — no es código adulto")
                    texto("Huella HIJO LOCAL: ${dev.controlparental.protocol.cp2.CodecPairCp2.huellaPublica(ref.contexto.identidad)}\nOrigen LOCAL: $origen")
                    TextView(this).apply {
                        text = codigo8; textSize = 38f; isSaveEnabled = false; isSaveFromParentEnabled = false
                        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO; setTextIsSelectable(false); columna.addView(this)
                    }
                    texto("El padre conocido lo escribe manualmente. No copiar, fotografiar ni enviar por chat. No concede confianza; comparar ambas huellas locales.")
                    boton("Cancelar y retirar código8") { cerrarOferta("Encuentro cancelado.") }
                } }, { runOnUiThread {
                    if (!vigente()) return@runOnUiThread
                    base("Etapa2: identidad por confirmar — código consumido")
                    texto("No generar otro código ni aceptación por un fallo. La sesión original no se renueva.")
                    boton("Cancelar encuentro") { cerrarOferta("Encuentro cancelado; sin borrar pendientes.") }
                } }, { bytes -> runOnUiThread {
                    if (vigente()) {
                        framePair = bytes.copyOf()
                        autorizarPair(FinAdulto.VALIDAR_PAIR) { presentarCandidataPair() }
                    }
                    bytes.fill(0)
                } }, { runOnUiThread {
                    if (referenciaPair === ref && canalC8 === enlace)
                        cerrarOferta("C8 cerrado/caducado. Candidata RAM invalidada; cualquier guardado o pendiente durable se conserva. No reintentar alta automáticamente.")
                } }, version = 2)
            canalC8 = enlace; enlace.conectar(); handler.postDelayed(caducidad, 100)
        }
        boton("Cancelar sin conectar") { cerrarOferta("Encuentro no iniciado.") }
        handler.postDelayed(caducidad, 100)
    }
    private fun cerrarOferta(mensaje: String) {
        cerrarAutoridad(); limpiar(); sesion.reanudar(); mensajePublico = mensaje; consultar()
    }
    private fun actualizarOferta(controlador: OfertasRam, ref: ReferenciaOferta, etiqueta: TextView, n: Int) {
        if (!activa || ofertas !== controlador) return
        trabajoOferta.execute {
            val restante = controlador.restante(ref)
            runOnUiThread {
                if (!activa || ofertas !== controlador) return@runOnUiThread
                if (restante == null || sesion.estado() == AutoridadAdulto.CERRADA) cerrarOferta("Oferta caducada o contexto no disponible.")
                else {
                    etiqueta.text = getString(R.string.oferta_cp2_restante, (restante + 999) / 1000)
                    handler.postDelayed({ actualizarOferta(controlador, ref, etiqueta, n) }, 1000)
                }
            }
        }
    }
    private fun autorizarPair(fin: FinAdulto, accion: (TicketAdulto) -> Unit) {
        val ref = referenciaPair ?: return
        val autorizacion = sesionVinculacion ?: sesion
        base("Operación CP/2 separada: $fin")
        texto(if (sesionVinculacion != null) "Etapa3: aceptación pendiente. Código fresco; no renueva el marco C8/2 original de hasta10 minutos. Fondo/salir cancela. No concede uso."
            else "La sesión original sigue corriendo; este código no renueva sus120 segundos. Fondo/cancelar retira todo. No concede uso.")
        if (fin == FinAdulto.GUARDAR_PAIR) texto("Confirmas guardar UNA pareja CP/2 local con las huellas/origen ya comprobados. Es irreversible aquí: no hay reemplazo ni recuperación CP/2 operativa; canal pendiente. No concede ningún permiso de uso.")
        val codigo = campo("Código adulto fresco para esta operación", codigo = true)
        boton("Autorizar operación") {
            val t = autorizacion.solicitar(fin) ?: return@boton cerrarOferta("Marco original caducado.")
            val secreto = codigo.text.toString(); base("Verificando autoridad CP/2…")
            val n = consulta.incrementAndGet()
            val admitida = AdultoAndroid.ejecutar(applicationContext, { it.verificar(secreto, false) { autorizacion.vigente(t) } }) { r ->
                runOnUiThread {
                    if (!activa || consulta.get() != n || referenciaPair !== ref) return@runOnUiThread
                    if (autorizacion.autorizarAccion(t, r)) accion(t) else cerrarOferta("Operación CP/2 no autorizada.")
                }
            }
            if (!admitida) cerrarOferta("Otra operación en curso; recepción cerrada.")
        }
        boton("Cancelar ceremonia") { cerrarOferta("Ceremonia cancelada.") }
    }
    private fun recibirPair() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            android.app.AlertDialog.Builder(this).setTitle("Cámara solo para QR CP/2")
                .setMessage("Android pedirá CAMERA (permiso runtime). No graba ni guarda imágenes. Aceptar o negar cierra esta oferta; vuelve a autenticar y genera otra, sin restauración automática.")
                .setPositiveButton("Pedir permiso Android") { _, _ ->
                    cerrarOferta("Permiso solicitado; oferta retirada. No hay escaneo automático.")
                    requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 203)
                }.setNegativeButton("No autorizar") { _, _ -> cerrarOferta("Cámara no autorizada; recepción cerrada.") }
                .setOnCancelListener { cerrarOferta("Recepción cancelada.") }.show()
            return
        }
        autorizarPair(FinAdulto.VALIDAR_PAIR) { t ->
            base("Apunta al QR PAIR_ACCEPT del padre")
            texto("Solo RAM, sin fotografías/archivo/clipboard. Integridad no prueba identidad humana.")
            val ref = referenciaPair ?: return@autorizarPair
            val vista = android.view.TextureView(this).apply { isSaveEnabled = false }
            columna.addView(vista, LinearLayout.LayoutParams(-1, 640))
            val n = consulta.incrementAndGet()
            camaraPair = CamaraPairCp2(this, vista, { bytes -> runOnUiThread {
                if (activa && consulta.get() == n && referenciaPair === ref && sesion.accionVigente(t)) {
                    framePair = bytes.copyOf(); presentarCandidataPair()
                }
                bytes.fill(0)
            } }, { runOnUiThread {
                if (activa && consulta.get() == n) cerrarOferta("Cámara no disponible; no se ha validado ni guardado vínculo.")
            } }).also { it.iniciar() }
            boton("Cancelar y cerrar cámara") { cerrarOferta("Recepción cancelada.") }
        }
    }
    private fun presentarCandidataPair() {
        base("Comprobar candidata — todavía NO confiable")
        val frame = framePair?.copyOf() ?: return
        val ref = referenciaPair ?: return
        val n = consulta.incrementAndGet()
        trabajoOferta.execute {
            // Solo integridad con candidata recibida: no autoridad ni confirmación humana implícita.
            val candidata = try {
                val s = dev.controlparental.protocol.cp2.CodecPairCp2.leerSobre(frame)
                val a = dev.controlparental.protocol.cp2.CodecPairCp2.leerPayload(dev.controlparental.protocol.Base64Cp1.decodificar(s.payloadB64, 8192))
                dev.controlparental.protocol.cp2.CriptografiaPairCp2.verificarIntegridad(frame, a.parentPublicKeyB64)
            } catch (_: Exception) { null }
            runOnUiThread {
                if (!activa || consulta.get() != n || referenciaPair !== ref) return@runOnUiThread
                if (candidata == null) { cerrarOferta("PAIR inválido; ninguna confianza ni escritura."); return@runOnUiThread }
                val a = candidata.aceptacion
                val fp = dev.controlparental.protocol.cp2.CodecPairCp2.huellaPublica(a.parentPublicKeyB64)
                val fh = dev.controlparental.protocol.cp2.CodecPairCp2.huellaPublica(ref.contexto.identidad)
                texto("Integridad con clave candidata, NO identidad acreditada. Pública PADRE candidata: ${a.parentPublicKeyB64}")
                texto("Huella PADRE: $fp\nHuella HIJO: $fh\nOrigen candidato: ${a.relayOrigin}")
                texto("Compara presencialmente AMBAS huellas con identidades LOCALES de los teléfonos conocidos, mostradas antes de recibir candidatas. Introduce huella PADRE desde su identidad LOCAL, no desde PAIR/C8/QR. Origen desde configuración local independiente. Si no son legibles o difieren, cancela.")
                val esperada = campo("Huella PADRE esperada independiente (base64url43)", secreto = false, maximo = 43)
                val origen = campo("Origen HTTPS esperado independiente", secreto = false, maximo = 200)
                val confirmar = CheckBox(this).apply {
                    text = getString(R.string.confirmar_identidad_pair)
                    isChecked = false; isSaveEnabled = false; columna.addView(this)
                }
                boton("Confirmar contexto humano y validar con otro código") {
                    if (!confirmar.isChecked || esperada.text.toString() != fp || origen.text.toString() != a.relayOrigin ||
                        (origenC8Confirmado != null && origen.text.toString() != origenC8Confirmado)) {
                        texto("Confirmación ausente o distinta; no se confía en la candidata."); return@boton
                    }
                    val datos = DatosConfirmadosPair(a.parentPublicKeyB64, esperada.text.toString(), fh, origen.text.toString())
                    autorizarPair(FinAdulto.VALIDAR_PAIR) { ticket ->
                        val caso = casoPair ?: return@autorizarPair
                        base("Validando PAIR contra oferta actual…")
                        val intento = consulta.incrementAndGet()
                        trabajoOferta.execute {
                            val c = caso.confirmar(ref, ticket, datos)
                            val v = c?.let { caso.validar(it, frame) }
                            runOnUiThread {
                                if (!activa || consulta.get() != intento || referenciaPair !== ref) return@runOnUiThread
                                if (v == null) cerrarOferta("Contexto/autoridad/PAIR no vigentes; no guardado.") else guardarPair(v)
                            }
                        }
                    }
                }
                boton("Rechazar candidata y retirar oferta") { cerrarOferta("Candidata rechazada.") }
            }
        }
    }
    private fun guardarPair(v: ValidacionPair) {
        val autorizacion = sesionVinculacion ?: sesion
        autorizarPair(FinAdulto.GUARDAR_PAIR) { ticket ->
            val caso = casoPair ?: return@autorizarPair
            base("Guardando vínculo local CP/2 — pendiente readback")
            val n = consulta.incrementAndGet()
            var resultado = ResultadoGuardarPair.RECHAZADO
            // Mantiene el escritor adulto ocupado: ninguna revisión de credencial puede cambiar durante el commit.
            val admitida = AdultoAndroid.ejecutar(applicationContext, { adulto ->
                val actual = adulto.resumen()
                if (actual.tipo == TipoAdulto.CONFIGURADO && actual.revision == v.marco.revision && actual.esperaMs == 0L)
                    resultado = try { PairAndroid.guardar(applicationContext, caso, v, autorizacion, ticket) }
                        catch (_: Exception) { ResultadoGuardarPair.INCIERTO }
                ResultadoAdulto(TipoAdulto.CONFIGURADO)
            }) {
                runOnUiThread {
                    if (!activa || consulta.get() != n) return@runOnUiThread
                    cerrarOferta(when (resultado) {
                        ResultadoGuardarPair.GUARDADO, ResultadoGuardarPair.GUARDADO_SIN_SESION ->
                            "VINCULO_CP2_GUARDADO / PENDIENTE_CANAL_Y_RECUPERACION. Readback verificado; no APPLIED ni permiso de uso."
                        ResultadoGuardarPair.RECHAZADO -> "Alta rechazada; no reemplazar vínculo ni migrar."
                        ResultadoGuardarPair.INCIERTO -> "Transacción CP/2 incierta; no reintentar alta ni borrar restos. Consulta estado local."
                    })
                }
            }
            if (!admitida) cerrarOferta("Escritor adulto ocupado; no se ha iniciado la transacción CP/2.")
        }
    }
    private fun confirmarBloqueo(fin: FinAdulto) {
        if (sesion.estado() != AutoridadAdulto.ADMINISTRAR) { consultar(); return }
        val proteccion = fin == FinAdulto.PROTEGER_APP || fin == FinAdulto.RESTAURAR_APP
        val boot = fin == FinAdulto.ARMAR_BOOT || fin == FinAdulto.FINALIZAR_BOOT
        val temporal = fin == FinAdulto.USO_TIEMPO_NORMAL || fin == FinAdulto.USO_TIEMPO_OFFLINE
        val offline = fin == FinAdulto.USO_OFFLINE || fin == FinAdulto.USO_TIEMPO_OFFLINE
        val red = temporal || fin in setOf(FinAdulto.ARMAR_RED, FinAdulto.USO_NORMAL, FinAdulto.USO_OFFLINE, FinAdulto.BLOQUEAR_RED, FinAdulto.FINALIZAR_RED)
        base(when (fin) {
            FinAdulto.ARMAR_RED -> "Preparar control local"
            FinAdulto.USO_NORMAL -> "Permitir libre · normal"
            FinAdulto.USO_OFFLINE -> "Permitir libre · sin internet"
            FinAdulto.USO_TIEMPO_NORMAL -> "Permitir por tiempo · normal"
            FinAdulto.USO_TIEMPO_OFFLINE -> "Permitir por tiempo · sin internet"
            FinAdulto.BLOQUEAR_RED -> "Bloquear ahora"
            FinAdulto.FINALIZAR_RED -> "Desactivar control local"
            FinAdulto.ARMAR_BOOT -> "Preparar bloqueo de arranque"
            FinAdulto.FINALIZAR_BOOT -> "Restaurar configuración de arranque"
            FinAdulto.PROTEGER_APP -> "Activar protección de la app"
            FinAdulto.RESTAURAR_APP -> "Restaurar protección anterior"
            FinAdulto.BLOQUEAR -> "Confirmar bloqueo local"
            else -> "Salida adulta del bloqueo inicial"
        })
        if (red) texto(when {
            fin == FinAdulto.ARMAR_RED -> "Activa el aviso visible del sistema y el bloqueo al reiniciar. No concede uso. La primera activación requiere coordinar la prueba presencial."
            fin == FinAdulto.FINALIZAR_RED -> "Cancela el permiso vigente y restaura la configuración previa del control local. No retira la protección de la app."
            fin == FinAdulto.BLOQUEAR_RED -> "Cancela el permiso y el tiempo vigente. Volver a internet no desbloquea."
            offline -> "Excepción local explícita: permite uso sin internet. Reiniciar cancela el permiso."
            else -> "Requiere internet validada. Perderla durante 10 segundos cancela el permiso; reconectar no desbloquea. Reiniciar también lo cancela."
        })
        val minutos = if (temporal) {
            texto("Cuenta también con pantalla apagada y en otras apps. Al terminar, se bloquea. Cerrar este panel no pausa el contador.")
            campo("Minutos · de 1 a 1440", secreto = false, maximo = 10).apply { inputType = InputType.TYPE_CLASS_NUMBER }
        } else null
        val confirmarOffline = if (offline) CheckBox(this).apply {
            text = "Autorizo la excepción sin internet"
            setTextColor(EstiloLocal.tinta); textSize = 17f
            isChecked = false; isSaveEnabled = false; columna.addView(this)
        } else null
        if (boot) texto("Cambia la configuración de bloqueo tras reinicio. No reinicies durante esta operación. Emergencia y recuperación permanecen disponibles.")
        if (proteccion) texto(if (fin == FinAdulto.PROTEGER_APP)
            "Impide la desinstalación normal del hijo. También restringe borrar datos, deshabilitar y forzar detención de TODAS las apps desde Ajustes. No bloquea toda instalación. Se mantiene al salir y reiniciar; restaurar requiere tu código. Primero coordina la prueba presencial y comprueba emergencia y recuperación."
            else "Retira nuestra protección y restaura la configuración anterior comprobada. El teléfono puede volver a permitir desinstalar la app; no cambia el permiso de uso.")
        texto("Confirma con tu código. Cancelar no cambia el permiso anterior.")
        val codigo = campo("Código adulto", codigo = true)
        boton("Confirmar") {
            if (confirmarOffline != null && !confirmarOffline.isChecked) { texto("Confirma la excepción OFFLINE LOCAL antes de autenticar esta operación."); return@boton }
            val eleccion = minutos?.let { MinutosUso.desdeTexto(it.text.toString()) }
            if (temporal && eleccion == null) { texto("Introduce entre 1 y 1440 minutos enteros. No hay duración por defecto."); return@boton }
            val referenciaConfirmada = ControlRedAndroid.referenciaPublica
            val t = sesion.solicitar(fin) ?: return@boton
            val secreto = codigo.text.toString(); base("Verificando autoridad de la operación…")
            val n = consulta.incrementAndGet()
            val admitida = AdultoAndroid.ejecutar(applicationContext, { it.verificar(secreto, false) { sesion.vigente(t) } }) { r ->
                runOnUiThread {
                    if (!activa || consulta.get() != n) return@runOnUiThread
                    if (!sesion.autorizarAccion(t, r)) {
                        cerrarAutoridad(); sesion.reanudar(); mensajePublico = "Operación no autorizada."; consultar()
                        return@runOnUiThread
                    }
                    base("Comprobando resultado…")
                    if (red) {
                        val resultado = {
                            if (activa && consulta.get() == n) {
                                cerrarAutoridad(); sesion.reanudar()
                                mensajePublico = resumenActual().let { "${it.estado}. ${it.tiempo}. Si no coincide con lo esperado, entra y actualiza el estado." }
                                consultar()
                            }
                        }
                        when (fin) {
                            FinAdulto.ARMAR_RED -> {
                                if (!getSystemService(NotificationManager::class.java).areNotificationsEnabled()) {
                                    if (android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 105)
                                    texto("Activa las notificaciones de esta app y vuelve a entrar. El control no se ha preparado.")
                                    boton("Volver al acceso adulto") { cerrarAutoridad(); sesion.reanudar(); consultar() }
                                    return@runOnUiThread
                                }
                                if (ControlRedAndroid.activo) {
                                    ControlRedAndroid.reanudarObservador(applicationContext, { sesion.accionVigente(t) }, resultado)
                                } else ArranquePilotoAndroid.armar(this, { sesion.accionVigente(t) }) {
                                    if (ArranquePilotoAndroid.fase == FasePiloto.LOCKED && sesion.accionVigente(t))
                                        ControlRedAndroid.preparar(applicationContext, { sesion.accionVigente(t) }, resultado)
                                    else resultado()
                                }
                            }
                            FinAdulto.USO_NORMAL, FinAdulto.USO_OFFLINE, FinAdulto.USO_TIEMPO_NORMAL, FinAdulto.USO_TIEMPO_OFFLINE ->
                                ControlRedAndroid.permitir(this, if (offline) UsoLocal.EXCEPCION_OFFLINE else UsoLocal.NORMAL,
                                    { sesion.accionVigente(t) }, resultado, eleccion, referenciaConfirmada)
                            FinAdulto.BLOQUEAR_RED -> ControlRedAndroid.bloquear(applicationContext, { sesion.accionVigente(t) }, resultado)
                            FinAdulto.FINALIZAR_RED -> ControlRedAndroid.finalizar(this, { sesion.accionVigente(t) }, resultado)
                            else -> Unit
                        }
                        return@runOnUiThread
                    }
                    if (boot) {
                        val resultado = {
                            if (activa && consulta.get() == n) {
                                cerrarAutoridad(); sesion.reanudar()
                                mensajePublico = "${resumenActual().estado}. Comprueba la configuración en Seguridad."
                                consultar()
                            }
                        }
                        if (fin == FinAdulto.ARMAR_BOOT) ArranquePilotoAndroid.armar(this, { sesion.accionVigente(t) }, resultado)
                        else ArranquePilotoAndroid.finalizar(this, { sesion.accionVigente(t) }, resultado)
                        return@runOnUiThread
                    }
                    if (proteccion) {
                        val iniciada = ProteccionAndroid.ejecutar(applicationContext, fin == FinAdulto.RESTAURAR_APP,
                            { sesion.accionVigente(t) }) resultadoProteccion@{ d ->
                            if (!activa || consulta.get() != n) return@resultadoProteccion
                            cerrarAutoridad(); sesion.reanudar()
                            mensajePublico = "${resumenActual().proteccion}. Consulta Seguridad para comprobar o restaurar."
                            consultar()
                        }
                        if (!iniciada) { cerrarAutoridad(); sesion.reanudar(); mensajePublico = "Protección ocupada o autoridad caducada."; consultar() }
                        return@runOnUiThread
                    }
                    val iniciada = BloqueoLocalAndroid.ejecutar(this, fin == FinAdulto.SALIR_ENSAYO,
                        { sesion.accionVigente(t) }) resultadoLock@{ d ->
                        if (!activa || consulta.get() != n) return@resultadoLock
                        cerrarAutoridad(); sesion.reanudar()
                        mensajePublico = if (d.fase == FaseLocal.LOCKED) "Teléfono bloqueado." else "${resumenActual().estado}. Comprueba el estado antes de continuar."
                        consultar()
                    }
                    if (!iniciada) { cerrarAutoridad(); sesion.reanudar(); mensajePublico = "Otra transición en curso o autoridad caducada."; consultar() }
                }
            }
            if (!admitida) { cerrarAutoridad(); sesion.reanudar(); consultar() }
        }
        boton("Cancelar y volver al control") { administrar() }
    }
    private fun operar(t: TicketAdulto, accion: (AccesoAdultoLocal) -> ResultadoAdulto) {
        mensajePublico = ""
        base("Verificando operación local…")
        val n = consulta.incrementAndGet()
        val aceptado = AdultoAndroid.ejecutar(applicationContext, accion) { r -> runOnUiThread {
            if (!activa || consulta.get() != n) return@runOnUiThread
            if (!sesion.completar(t, r)) {
                cerrarAutoridad(); sesion.reanudar(); mensajePublico = "No se pudo autorizar la operación."; consultar()
                return@runOnUiThread
            }
            alta = null; bootstrap.set(false)
            mensajePublico = when (r.tipo) {
                TipoAdulto.GUARDADO -> "Código guardado. Entra con tu nuevo código."
                TipoAdulto.RECHAZADO, TipoAdulto.ESPERA, TipoAdulto.ERROR ->
                    "No se pudo autorizar la operación. Comprueba los datos o la espera de seguridad."
                else -> ""
            }
            when (sesion.estado()) {
                AutoridadAdulto.ADMINISTRAR -> administrar()
                AutoridadAdulto.CAMBIAR_CODIGO -> nuevoCodigo()
                AutoridadAdulto.CERRADA, AutoridadAdulto.VINCULAR_C8_V2 -> {
                    sesion.reanudar(); consultar()
                    // Resumen fresco decidirá si sigue configurado; no alta por errores.
                }
            }
            if (sesion.estado() != AutoridadAdulto.CERRADA) handler.postDelayed(caducidad, 1000)
        } }
        if (!aceptado) { cerrarAutoridad(); sesion.reanudar(); consultar() }
    }
}
