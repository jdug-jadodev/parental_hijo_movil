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
    private var mensajePublico = ""
    private var ofertas: OfertasRam? = null
    private var imagenOferta: ImageView? = null
    private var referenciaPair: ReferenciaOferta? = null
    private var casoPair: ValidacionPairRam? = null
    private var camaraPair: CamaraPairCp2? = null
    private var framePair: ByteArray? = null
    private val trabajoOferta = Executors.newSingleThreadExecutor { Thread(it, "oferta-local") }
    private val caducidad = object : Runnable {
        override fun run() {
            if (!activa) return
            if ((alta != null && !sesion.vigente(alta!!)) ||
                (alta == null && sesion.estado() == AutoridadAdulto.CERRADA)) {
                cerrarAutoridad(); sesion.reanudar(); mensajePublico = "Autorización caducada. Vuelve a autenticarte."; consultar()
            } else handler.postDelayed(this, 1000)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        columna = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        setContentView(ScrollView(this).apply {
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            isSaveEnabled = false; addView(columna)
        })
    }
    override fun onResume() { super.onResume(); activa = true; sesion.reanudar(); consultar() }
    override fun onPause() { activa = false; cerrarAutoridad(); limpiar(); super.onPause() }
    override fun onDestroy() { cerrarAutoridad(); trabajoOferta.shutdown(); super.onDestroy() }

    private fun limpiar() {
        camaraPair?.close(); camaraPair = null
        imagenOferta?.setImageDrawable(null); imagenOferta = null
        entradas.forEach { it.text.clear() }; entradas.clear(); columna.removeAllViews()
    }
    private fun cerrarAutoridad() {
        camaraPair?.close(); camaraPair = null
        framePair?.fill(0); framePair = null; casoPair?.cancelar(); casoPair = null; referenciaPair = null
        imagenOferta?.setImageDrawable(null)
        ofertas?.cancelar(); ofertas = null
        consulta.incrementAndGet(); bootstrap.set(false); alta = null
        prompt?.cancel(); prompt = null; sesion.cerrar(); handler.removeCallbacksAndMessages(null)
    }
    private fun base(titulo: String) {
        limpiar(); texto(titulo)
        val modo = try { BloqueoLocalAndroid.modo(this).name } catch (_: RuntimeException) { "DESCONOCIDO" }
        texto("Lock Task observado=$modo; transición=${BloqueoLocalAndroid.diagnostico.fase}/${BloqueoLocalAndroid.diagnostico.causa}")
        val p = ProteccionAndroid.estado
        texto("Protección de app=${p.fase}/${p.causa}; snapshot=${p.snapshot}; lectura=${p.lectura ?: "DESCONOCIDA"}")
        texto("Piloto reinicio=${ArranquePilotoAndroid.fase}; enrolado=${ArranquePilotoAndroid.enrolado}. Si sigue armado, el siguiente reinicio exige bloqueo y nueva autoridad adulta.")
        texto("Red=${ControlRedAndroid.validacion}; uso solicitado=${ControlRedAndroid.uso}; observador listo=${ControlRedAndroid.listo}; deadlinegracia=${ControlRedAndroid.deadline}; vencimientoUso=${ControlRedAndroid.vencimientoUso}. Plazo null: sin temporal activo. Aplicación real: ver Lock Task observado, no solo uso solicitado.")
        boton("Emergencia / Keyguard del sistema") {
            cerrarAutoridad()
            EmergencyAccess(PolicyEnforcer(this)).irAPantallaBloqueo()
            if (activa) { sesion.reanudar(); consultar() }
        }
        boton("Salir del acceso adulto") { cerrarAutoridad(); finish() }
    }
    private fun texto(s: String) { columna.addView(TextView(this).apply { text = s; textSize = 18f }) }
    private fun boton(s: String, habilitado: Boolean = true, accion: () -> Unit = {}) {
        columna.addView(Button(this).apply { text = s; isEnabled = habilitado; setOnClickListener { accion() } })
    }
    private fun campo(titulo: String, codigo: Boolean = false, secreto: Boolean = true, maximo: Int = 256): EditText {
        texto(titulo)
        return EditText(this).apply {
            inputType = if (codigo) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT or if (secreto) InputType.TYPE_TEXT_VARIATION_PASSWORD
                    else InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(if (codigo) 10 else maximo))
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
        base("Acceso adulto local — comprobando almacén")
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
        base("Acceso adulto local")
        if (mensaje.isNotEmpty()) texto(mensaje)
        when (resumen.tipo) {
            TipoAdulto.SIN_CONFIGURAR -> {
                texto("Alta presencial: la credencial Android solo protege la configuración inicial. No hay código universal.")
                boton("Configurar código y pregunta con credencial Android") { autenticarAlta() }
            }
            TipoAdulto.CONFIGURADO -> {
                if (resumen.esperaMs > 0) texto("Espera de seguridad activa. Código y respuesta comparten el límite de intentos.")
                val codigo = campo("Código adulto: 5–10 dígitos, conserva ceros", codigo = true)
                boton("Entrar en administración", resumen.esperaMs == 0L) {
                    verificar(codigo.text.toString(), false)
                }
                boton("Olvidé el código: recuperar por pregunta", resumen.esperaMs == 0L) { recuperar() }
                boton("Actualizar espera") { consultar() }
            }
            else -> texto("Almacén no disponible o con integridad no confirmada. Acceso cerrado: no se permite alta ni reemplazo. Emergencia disponible.")
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
        base("Administración adulta autenticada")
        texto("Sesión solo RAM, hasta2 minutos absolutos desde la solicitud. Salir/fondo cierra. No concede uso ni altera ningún permiso vigente.")
        val modo = try { BloqueoLocalAndroid.modo(this) } catch (_: RuntimeException) { null }
        val locked = modo == ModoLocal.LOCKED
        val enroladoBoot = ArranquePilotoAndroid.enrolado
        boton("BLOQUEAR AHORA — piloto MAIN", modo == ModoLocal.NONE && !enroladoBoot) { confirmarBloqueo(FinAdulto.BLOQUEAR) }
        boton("SALIDA REVERSIBLE DE ENSAYO — no permiso productivo", locked && !enroladoBoot) { confirmarBloqueo(FinAdulto.SALIR_ENSAYO) }
        boton("ARMAR PILOTO REINICIO — persistirá al reiniciar", !enroladoBoot && modo == ModoLocal.NONE) { confirmarBloqueo(FinAdulto.ARMAR_BOOT) }
        boton("FINALIZAR ENSAYO REINICIO — desarmar y restaurar", enroladoBoot && !ControlRedAndroid.activo) { confirmarBloqueo(FinAdulto.FINALIZAR_BOOT) }
        boton("ARMAR ENSAYO RED — FGS visible y bloqueo de boot", !enroladoBoot && !ControlRedAndroid.activo && modo == ModoLocal.NONE) { confirmarBloqueo(FinAdulto.ARMAR_RED) }
        boton("REANUDAR OBSERVADOR RED — no concede uso", enroladoBoot && ControlRedAndroid.activo && !ControlRedAndroid.listo && ControlRedAndroid.uso == UsoLocal.BLOQUEADO) { confirmarBloqueo(FinAdulto.ARMAR_RED) }
        boton("USO LIBRE NORMAL LOCAL — requiere internet validada", ControlRedAndroid.listo && ControlRedAndroid.validacion == ValidacionRed.VALIDADA) { confirmarBloqueo(FinAdulto.USO_NORMAL) }
        boton("USO LIBRE EXCEPCIÓN OFFLINE LOCAL — autorización explícita", ControlRedAndroid.listo) { confirmarBloqueo(FinAdulto.USO_OFFLINE) }
        boton("PERMITIR POR TIEMPO NORMAL LOCAL — elegir minutos", ControlRedAndroid.listo && ControlRedAndroid.validacion == ValidacionRed.VALIDADA) { confirmarBloqueo(FinAdulto.USO_TIEMPO_NORMAL) }
        boton("PERMITIR POR TIEMPO OFFLINE LOCAL — excepción explícita", ControlRedAndroid.listo) { confirmarBloqueo(FinAdulto.USO_TIEMPO_OFFLINE) }
        boton("BLOQUEAR AHORA — cancelar NORMAL y EXCEPCIÓN", ControlRedAndroid.activo) { confirmarBloqueo(FinAdulto.BLOQUEAR_RED) }
        boton("FINALIZAR ENSAYO RED — cancelar observador, desarmar, restaurar", ControlRedAndroid.activo) { confirmarBloqueo(FinAdulto.FINALIZAR_RED) }
        boton("PROTEGER APP — gestión global de aplicaciones", !ProteccionAndroid.estado.snapshot) { confirmarBloqueo(FinAdulto.PROTEGER_APP) }
        boton("RESTAURAR PROTECCIÓN — baseline de ensayo", ProteccionAndroid.estado.snapshot) { confirmarBloqueo(FinAdulto.RESTAURAR_APP) }
        boton("GENERAR OFERTA CP/2 — no vincula ni concede uso") { confirmarOferta(false) }
        boton("Consultar vínculo CP/2 local — no canal ni permiso") {
            base("Estado local CP/2")
            val n = consulta.incrementAndGet()
            trabajoOferta.execute {
                val carga = PairAndroid.cargar(applicationContext)
                runOnUiThread {
                    if (!activa || consulta.get() != n) return@runOnUiThread
                    texto(when (carga) {
                        is CargaPairCp2.Guardado -> carga.snapshot.estadoLocal
                        CargaPairCp2.Ausente -> "Sin registro CP/2. No se ha generado oferta ni vínculo."
                        CargaPairCp2.Incierta -> "CP/2 incierto: alta cerrada; no reparar ni reemplazar."
                    })
                    texto("Recuperación CP/2 y canal pendientes. No es APPLIED ni autoriza uso.")
                }
            }
        }
        boton("INICIALIZAR IDENTIDAD — solo alta presencial separada") { confirmarOferta(true) }
        for (s in listOf("Red remota — pendiente", "Otras políticas de Ajustes — pendiente")) boton(s, false)
        boton("Conexión AUTH CP/2 — deshabilitada hasta transporte TLS/canal compatible", false)
        boton("Órdenes CP/2 — NO_DISPONIBLE: falta escritor CONTROL durable y canal compatible", false)
        boton("Cerrar sesión") { cerrarAutoridad(); sesion.reanudar(); consultar() }
    }
    private fun confirmarOferta(inicializar: Boolean) {
        if (sesion.estado() != AutoridadAdulto.ADMINISTRAR) { consultar(); return }
        ofertas?.cancelar(); ofertas = null
        base(if (inicializar) "Inicialización presencial de identidad" else "Generar oferta CP/2 local")
        texto("Requiere código fresco. Generar no vincula, no migra CP/1, no concede uso ni cambia políticas. Recibir/confirmar/guardar exigen pasos separados.")
        texto(if (inicializar) "Solo ausencia inicial coherente, sin vínculo ni restos. Crea privateSigning P-256 no exportable una vez; no repara claves perdidas. Requiere autorización física concreta antes de usar este botón. No genera QR."
            else "Oferta RAM hasta5 minutos; la sesión adulta de2 minutos puede cerrarla antes. Fondo, salir, cancelar, caducidad o contexto incierto retiran QR. No compartir/capturar/copiar. Requiere autorización física concreta para mostrarla.")
        val codigo = campo("Código adulto fresco", codigo = true)
        boton("Confirmar operación separada con mi código") {
            val fin = if (inicializar) FinAdulto.INICIALIZAR_IDENTIDAD else FinAdulto.GENERAR_OFERTA
            val t = sesion.solicitar(fin) ?: return@boton
            val secreto = codigo.text.toString(); base("Verificando autoridad…")
            val n = consulta.incrementAndGet()
            val aceptada = AdultoAndroid.ejecutar(applicationContext, { it.verificar(secreto, false) { sesion.vigente(t) } }) { r ->
                runOnUiThread {
                    if (!activa || consulta.get() != n) return@runOnUiThread
                    if (!sesion.autorizarAccion(t, r)) { cerrarOferta("Operación no autorizada."); return@runOnUiThread }
                    val identidad = IdentidadOfertaAndroid(applicationContext)
                    val marco = sesion.marcoAcreditado(t)
                    val controlador = OfertasRam(SystemClock::elapsedRealtime, { identidad.leer().contexto },
                        { sesion.accionVigente(t) && PairAndroid.cargar(applicationContext) is CargaPairCp2.Ausente },
                        vidaSesion = { marco != null && sesion.marcoVigente(marco) })
                    ofertas = controlador
                    casoPair = ValidacionPairRam(controlador, sesion)
                    trabajoOferta.execute {
                         val creada = if (inicializar) identidad.inicializar {
                             sesion.accionVigente(t) && PairAndroid.cargar(applicationContext) is CargaPairCp2.Ausente
                         } else false
                        val ref = if (!inicializar) try { controlador.generar() } catch (_: Exception) { null } else null
                        val matriz = try { ref?.let { QrOfertaCp2.crear(CodecOfertaCp2.escribir(it.oferta)) } } catch (_: Exception) { null }
                        runOnUiThread {
                            if (!activa || consulta.get() != n || !sesion.accionVigente(t)) { controlador.cancelar(); return@runOnUiThread }
                            if (inicializar) cerrarOferta(if (creada) "Identidad inicializada con readback. Generar oferta exige otra autorización."
                                else "NO_DISPONIBLE: estado incierto, identidad existente o creación no completada. No se reemplaza ninguna clave.")
                            else if (ref == null || matriz == null) cerrarOferta("NO_DISPONIBLE: identidad ausente o vínculo/contexto incierto. Inicialización, si procede, es otra operación.")
                            else {
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
        base("Operación CP/2 separada: $fin")
        texto("La sesión original sigue corriendo; este código no renueva sus120 segundos. Fondo/cancelar retira todo. No concede uso.")
        if (fin == FinAdulto.GUARDAR_PAIR) texto("Confirmas guardar UNA pareja CP/2 local con las huellas/origen ya comprobados. Es irreversible aquí: no hay reemplazo ni recuperación CP/2 operativa; canal pendiente. No concede ningún permiso de uso.")
        val codigo = campo("Código adulto fresco para esta operación", codigo = true)
        boton("Autorizar operación") {
            val t = sesion.solicitar(fin) ?: return@boton cerrarOferta("Sesión original caducada.")
            val secreto = codigo.text.toString(); base("Verificando autoridad CP/2…")
            val n = consulta.incrementAndGet()
            val admitida = AdultoAndroid.ejecutar(applicationContext, { it.verificar(secreto, false) { sesion.vigente(t) } }) { r ->
                runOnUiThread {
                    if (!activa || consulta.get() != n || referenciaPair !== ref) return@runOnUiThread
                    if (sesion.autorizarAccion(t, r)) accion(t) else cerrarOferta("Operación CP/2 no autorizada.")
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
            // Solo integridad con candidata del QR: no autoridad ni confirmación humana implícita.
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
                texto("Compara presencialmente AMBAS huellas con el padre conocido. Introduce la huella PADRE y origen desde su pantalla independiente, no desde este QR. Si difieren, cancela.")
                val esperada = campo("Huella PADRE esperada independiente (base64url43)", secreto = false, maximo = 43)
                val origen = campo("Origen HTTPS esperado independiente", secreto = false, maximo = 200)
                val confirmar = CheckBox(this).apply {
                    text = getString(R.string.confirmar_identidad_pair)
                    isChecked = false; isSaveEnabled = false; columna.addView(this)
                }
                boton("Confirmar contexto humano y validar con otro código") {
                    if (!confirmar.isChecked || esperada.text.toString() != fp || origen.text.toString() != a.relayOrigin) {
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
        autorizarPair(FinAdulto.GUARDAR_PAIR) { ticket ->
            val caso = casoPair ?: return@autorizarPair
            base("Guardando vínculo local CP/2 — pendiente readback")
            val n = consulta.incrementAndGet()
            var resultado = ResultadoGuardarPair.RECHAZADO
            // Mantiene el escritor adulto ocupado: ninguna revisión de credencial puede cambiar durante el commit.
            val admitida = AdultoAndroid.ejecutar(applicationContext, { adulto ->
                val actual = adulto.resumen()
                if (actual.tipo == TipoAdulto.CONFIGURADO && actual.revision == v.marco.revision && actual.esperaMs == 0L)
                    resultado = try { PairAndroid.guardar(applicationContext, caso, v, sesion, ticket) }
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
            FinAdulto.ARMAR_RED -> "Armar ensayo RED: notificación visible y boot bloqueado"
            FinAdulto.USO_NORMAL -> "Uso libre NORMAL LOCAL con internet validada"
            FinAdulto.USO_OFFLINE -> "Excepción OFFLINE LOCAL explícita"
            FinAdulto.USO_TIEMPO_NORMAL -> "Permitir por tiempo NORMAL LOCAL"
            FinAdulto.USO_TIEMPO_OFFLINE -> "Permitir por tiempo: excepción OFFLINE LOCAL"
            FinAdulto.BLOQUEAR_RED -> "Bloquear ahora: revocar ambos usos"
            FinAdulto.FINALIZAR_RED -> "Finalizar RED y devolver baseline desarmado"
            FinAdulto.ARMAR_BOOT -> "Armar reinicio: persistirá al reiniciar"
            FinAdulto.FINALIZAR_BOOT -> "Finalizar ensayo: desarmar ANTES de restaurar políticas"
            FinAdulto.PROTEGER_APP -> "Confirmar protección de app — alcance global"
            FinAdulto.RESTAURAR_APP -> "Restaurar protección al baseline de ensayo"
            FinAdulto.BLOQUEAR -> "Confirmar bloqueo local"
            else -> "Confirmar salida reversible de ensayo"
        })
        if (red) texto("Piloto LOCAL: red predeterminada INTERNET+VALIDATED, no DNS/relay. NORMAL pierde autorización tras10.000ms elapsedRealtime desde señal negativa; si vuelve antes cancela gracia, después NO reabre. Excepción OFFLINE LOCAL no se revoca solo por falta de red, ni se crea por recuperación. Reboot invalida ambos. Armar requiere baseline desarmado/LOCAL03 restaurada y crea bootpiloto + FGS visible; FINALIZAR RED cancela referencias/gracia/observador antes desarmar/restaurar. No permiso remoto ni persistencia permanente autorizada. Instalación, notificaciones, armado y cambios de conectividad requieren coordinación física específica.")
        val minutos = if (temporal) {
            texto("Elige 1–1440 minutos enteros, sin valor predeterminado. Cuenta tiempo transcurrido con pantalla apagada y otras apps; al vencer exige bloqueo sin código. NORMAL puede revocarse antes por red; OFFLINE también vence. Cerrar panel no pausa. Nueva acción válida reemplaza la anterior; no se reanuda tras boot/proceso. Doze/latencia física no garantizados.")
            campo("Minutos (vacío hasta que el adulto elija)", secreto = false, maximo = 10).apply { inputType = InputType.TYPE_CLASS_NUMBER }
        } else null
        val confirmarOffline = if (offline) CheckBox(this).apply {
            text = getString(R.string.confirmar_offline_local)
            isChecked = false; columna.addView(this)
        } else null
        if (boot) texto("Piloto enrolado con intención DP durable: reinicio exige bloqueo nuevo, no reanuda tiempo/permiso/sesión. No garantía de cero ventana antes de LOCKED ni supervivencia ante crash. Keyguard/emergencia permanecen. Para volver al baseline usa FINALIZAR ENSAYO con código fresco; salir de administración no desarma. No activar protección LOCAL03 ni reiniciar sin coordinar ensayo específico.")
        if (proteccion) texto("setUninstallBlocked: solo hijo. DISALLOW_APPS_CONTROL: TODAS las apps del usuario; impide desinstalar/deshabilitar/borrar datos o caché/forzar detención/cambiar valores predeterminados desde Ajustes o launcher. No bloquea todo Ajustes ni establece restricciones de instalación/actualización; compatibilidad de estas pendiente. ADB conserva acceso de laboratorio. Puede persistir tras reboot; no es Lock Task persistente. Cerrar/fondo/recuperar código o salir del quiosco NO retiran protección; restaurar exige otra autenticación.")
        texto("Requiere código adulto fresco para esta operación. No cambia conectividad ni autoriza ALLOW CP/2. Cancelar/fondo no sale de LOCKED.")
        val codigo = campo("Código adulto", codigo = true)
        boton("Confirmar con mi código") {
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
                    base("Intención aceptada; pendiente de readback — no afirmar aplicado todavía")
                    if (red) {
                        val resultado = {
                            if (activa && consulta.get() == n) {
                                cerrarAutoridad(); sesion.reanudar()
                                mensajePublico = "Resultado red=${ControlRedAndroid.uso}; default=${ControlRedAndroid.validacion}. Permiso solicitado no prueba NONE/LOCKED: consultar readback y diagnóstico."
                                consultar()
                            }
                        }
                        when (fin) {
                            FinAdulto.ARMAR_RED -> {
                                if (!getSystemService(NotificationManager::class.java).areNotificationsEnabled()) {
                                    if (android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 105)
                                    texto("Se requiere notificación visible; vuelve a autenticar después de autorizar Android. No se ha armado RED.")
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
                                mensajePublico = "Piloto reinicio=${ArranquePilotoAndroid.fase}; enrolado=${ArranquePilotoAndroid.enrolado}. Comprueba readback Lock Task; armado persistirá al próximo reboot hasta finalizar ensayo."
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
                            mensajePublico = "Protección=${d.fase}/${d.causa}; uninstall/control propio/global/efectivo=${d.lectura}. No es permiso de uso."
                            consultar()
                        }
                        if (!iniciada) { cerrarAutoridad(); sesion.reanudar(); mensajePublico = "Protección ocupada o autoridad caducada."; consultar() }
                        return@runOnUiThread
                    }
                    val iniciada = BloqueoLocalAndroid.ejecutar(this, fin == FinAdulto.SALIR_ENSAYO,
                        { sesion.accionVigente(t) }) resultadoLock@{ d ->
                        if (!activa || consulta.get() != n) return@resultadoLock
                        cerrarAutoridad(); sesion.reanudar()
                        mensajePublico = "Resultado=${d.fase}/${d.causa}. ${if (d.fase == FaseLocal.LOCKED) "Bloqueo real leído." else "No es autorización de uso productiva."}"
                        consultar()
                    }
                    if (!iniciada) { cerrarAutoridad(); sesion.reanudar(); mensajePublico = "Otra transición en curso o autoridad caducada."; consultar() }
                }
            }
            if (!admitida) { cerrarAutoridad(); sesion.reanudar(); consultar() }
        }
        boton("Cancelar operación (no desbloquea)") { cerrarAutoridad(); sesion.reanudar(); consultar() }
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
                TipoAdulto.GUARDADO -> "Cambio guardado con readback. Entra con tu código; no se concede uso."
                TipoAdulto.RECHAZADO, TipoAdulto.ESPERA, TipoAdulto.ERROR ->
                    "No se pudo autorizar la operación. Comprueba los datos o la espera de seguridad."
                else -> ""
            }
            when (sesion.estado()) {
                AutoridadAdulto.ADMINISTRAR -> administrar()
                AutoridadAdulto.CAMBIAR_CODIGO -> nuevoCodigo()
                AutoridadAdulto.CERRADA -> {
                    sesion.reanudar(); consultar()
                    // Resumen fresco decidirá si sigue configurado; no alta por errores.
                }
            }
            if (sesion.estado() != AutoridadAdulto.CERRADA) handler.postDelayed(caducidad, 1000)
        } }
        if (!aceptado) { cerrarAutoridad(); sesion.reanudar(); consultar() }
    }
}
