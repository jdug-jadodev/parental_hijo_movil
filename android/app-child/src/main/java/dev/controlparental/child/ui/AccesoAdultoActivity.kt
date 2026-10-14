package dev.controlparental.child.ui

import android.app.KeyguardManager
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
    override fun onDestroy() { cerrarAutoridad(); super.onDestroy() }

    private fun limpiar() { entradas.forEach { it.text.clear() }; entradas.clear(); columna.removeAllViews() }
    private fun cerrarAutoridad() {
        consulta.incrementAndGet(); bootstrap.set(false); alta = null
        prompt?.cancel(); prompt = null; sesion.cerrar(); handler.removeCallbacksAndMessages(null)
    }
    private fun base(titulo: String) {
        limpiar(); texto(titulo)
        val modo = try { BloqueoLocalAndroid.modo(this).name } catch (_: RuntimeException) { "DESCONOCIDO" }
        texto("Lock Task observado=$modo; transición=${BloqueoLocalAndroid.diagnostico.fase}/${BloqueoLocalAndroid.diagnostico.causa}")
        val p = ProteccionAndroid.estado
        texto("Protección de app=${p.fase}/${p.causa}; snapshot=${p.snapshot}; lectura=${p.lectura ?: "DESCONOCIDA"}")
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
                        if (activa && consulta.get() == n) publica(mensajePublico)
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
        boton("BLOQUEAR AHORA — piloto MAIN", modo == ModoLocal.NONE) { confirmarBloqueo(FinAdulto.BLOQUEAR) }
        boton("SALIDA REVERSIBLE DE ENSAYO — no permiso productivo", locked) { confirmarBloqueo(FinAdulto.SALIR_ENSAYO) }
        boton("PROTEGER APP — gestión global de aplicaciones", !ProteccionAndroid.estado.snapshot) { confirmarBloqueo(FinAdulto.PROTEGER_APP) }
        boton("RESTAURAR PROTECCIÓN — baseline de ensayo", ProteccionAndroid.estado.snapshot) { confirmarBloqueo(FinAdulto.RESTAURAR_APP) }
        for (s in listOf("Permitir libre — pendiente", "Permitir tiempo — pendiente", "Red — pendiente", "Otras políticas de Ajustes — pendiente")) boton(s, false)
        boton("Cerrar sesión") { cerrarAutoridad(); sesion.reanudar(); consultar() }
    }
    private fun confirmarBloqueo(fin: FinAdulto) {
        if (sesion.estado() != AutoridadAdulto.ADMINISTRAR) { consultar(); return }
        val proteccion = fin == FinAdulto.PROTEGER_APP || fin == FinAdulto.RESTAURAR_APP
        base(when (fin) {
            FinAdulto.PROTEGER_APP -> "Confirmar protección de app — alcance global"
            FinAdulto.RESTAURAR_APP -> "Restaurar protección al baseline de ensayo"
            FinAdulto.BLOQUEAR -> "Confirmar bloqueo local"
            else -> "Confirmar salida reversible de ensayo"
        })
        if (proteccion) texto("setUninstallBlocked: solo hijo. DISALLOW_APPS_CONTROL: TODAS las apps del usuario; impide desinstalar/deshabilitar/borrar datos o caché/forzar detención/cambiar valores predeterminados desde Ajustes o launcher. No bloquea todo Ajustes ni establece restricciones de instalación/actualización; compatibilidad de estas pendiente. ADB conserva acceso de laboratorio. Puede persistir tras reboot; no es Lock Task persistente. Cerrar/fondo/recuperar código o salir del quiosco NO retiran protección; restaurar exige otra autenticación.")
        texto("Requiere código adulto fresco para esta operación. No cambia red/tiempo ni autoriza ALLOW CP/2. Cancelar/fondo no sale de LOCKED.")
        val codigo = campo("Código adulto", codigo = true)
        boton("Confirmar con mi código") {
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
