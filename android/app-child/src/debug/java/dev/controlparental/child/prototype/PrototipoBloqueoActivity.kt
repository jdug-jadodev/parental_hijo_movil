package dev.controlparental.child.prototype

import android.app.ActivityManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import dev.controlparental.child.admin.PolicyEnforcer
import dev.controlparental.child.emergency.EmergencyAccess

/** Entrada separada debug. Cancelación/fondo no desbloquean; no acepta extras o PIN. */
class PrototipoBloqueoActivity : ComponentActivity() {
    private val tickets = TicketsPrototipo(SystemClock::elapsedRealtime)
    private lateinit var politicas: PoliticasPrototipo
    private lateinit var texto: TextView
    private var cancelacion: CancellationSignal? = null
    private lateinit var entrada: EntradaPrototipo
    private val handler = Handler(Looper.getMainLooper())
    private var lectura: Runnable? = null
    private var destruida = false
    private var informe = Diagnostico(Etapa.REPOSO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        politicas = PoliticasPrototipo(this, this)
        entrada = EntradaPrototipo(politicas, SystemClock::elapsedRealtime)
        val columna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
        }
        texto = TextView(this).apply { textSize = 18f }
        columna.addView(texto)
        val emergenciaVerificada = CheckBox(this).apply {
            text = "Adulto: comprobé Keyguard y acceso a Emergencia sin llamar en este A13"
        }
        columna.addView(emergenciaVerificada)
        fun boton(titulo: String, accion: () -> Unit) {
            columna.addView(Button(this).apply { text = titulo; setOnClickListener { accion() } })
        }
        boton("1. Comprobar credencial Android (sin bloquear)") { autenticar(FinAutenticacion.COMPROBAR) }
        boton("2. Entrar en bloqueo reversible") {
            ejecutar(Etapa.EMERGENCIA) {
                exigir(!entrada.pendiente, Causa.CONTEXTO_OBSOLETO)
                exigir(emergenciaVerificada.isChecked, Causa.CHECKBOX_FALTA)
                diagnosticar(Diagnostico(Etapa.CREDENCIAL))
                exigir(tickets.comprobar() == Causa.NINGUNA, tickets.comprobar())
                diagnosticar(Diagnostico(Etapa.ROLLBACK_ADB))
                exigir(RollbackPrototipo.comprobar() == Causa.NINGUNA, RollbackPrototipo.comprobar())
                exigir(tickets.consumirComprobacion(), Causa.TICKET_FALTA)
                exigir(RollbackPrototipo.consumirComprobacion(), Causa.ADB_FALTA)
                entrada.entrar()
                vigilar()
            }
        }
        boton("Salir con credencial Android del adulto") { autenticar(FinAutenticacion.SALIR) }
        boton("Cancelar autenticación (no desbloquea)") { cancelar(); diagnosticar(Diagnostico(Etapa.CREDENCIAL, Causa.CANCELADO)) }
        boton("Emergencia / Keyguard del sistema") {
            cancelar()
            entrada.cancelar(Causa.CANCELADO)
            val resultado = EmergencyAccess(PolicyEnforcer(this)).irAPantallaBloqueo()
            mostrar("Ruta del sistema solicitada: $resultado. No llamar para probar")
        }
        boton("Actualizar estado observado") { mostrar("Consulta local") }
        setContentView(ScrollView(this).apply { addView(columna) })
        mostrar("SOLO LABORATORIO: PIN Android no es código parental final")
    }

    override fun onResume() { super.onResume(); tickets.reanudar(); mostrar("Primer plano; autorización no se recupera") }
    override fun onPause() {
        val autenticando = cancelacion != null
        tickets.fondo(); cancelacion?.cancel(); cancelacion = null
        if (autenticando) diagnosticar(Diagnostico(Etapa.CREDENCIAL, Causa.FONDO))
        entrada.cancelar(Causa.FONDO)
        // Si ya era LOCKED, Keyguard/fondo no conceden salida. Solo pendiente hace rollback.
        super.onPause()
    }
    override fun onDestroy() { destruida = true; cancelar(); entrada.cancelar(Causa.FONDO); super.onDestroy() }

    private fun vigilar() {
        lectura?.let(handler::removeCallbacks)
        val g = entrada.generacion
        val tarea = object : Runnable {
            override fun run() {
                entrada.observar(g)
                diagnosticar(entrada.diagnostico)
                if (g == entrada.generacion && entrada.pendiente) handler.postDelayed(this, 100)
            }
        }
        lectura = tarea
        handler.post(tarea) // 3s para entrar + 2s para rollback, sin dormir ni reescribir.
    }
    private fun diagnosticar(d: Diagnostico) {
        if (informe != d) Log.i("PROT01", "${d.etapa}/${d.causa}/origen=${d.falloEn ?: d.etapa}")
        informe = d
        if (!destruida) mostrar("Etapa=${d.etapa}; causa=${d.causa}; falloEn=${d.falloEn ?: "NINGUNO"}")
    }

    private fun cancelar() { tickets.cancelar(); cancelacion?.cancel(); cancelacion = null }
    private fun mostrar(mensaje: String) {
        if (!::texto.isInitialized) return
        val modo = try {
            when (politicas.modo()) {
                ActivityManager.LOCK_TASK_MODE_LOCKED -> "LOCKED (quiosco real observado)"
                ActivityManager.LOCK_TASK_MODE_NONE -> "NONE (sin Lock Task observado)"
                else -> "PINNED/otro: NO es quiosco válido"
            }
        } catch (_: RuntimeException) { "DESCONOCIDO" }
        texto.text = "PROT01 DEBUG\n$modo\n${informe.etapa}/${informe.causa}\n$mensaje\nSin tiempo, red ni política CP/1. Emergencia independiente."
    }
    private fun ejecutar(etapa: Etapa, accion: () -> Unit) {
        diagnosticar(Diagnostico(etapa))
        try { accion() } catch (e: RuntimeException) {
            diagnosticar(Diagnostico(informe.etapa, (e as? FalloPrototipo)?.causa ?: Causa.ANDROID_ERROR))
        }
    }
    private fun autenticar(fin: FinAutenticacion) = ejecutar(Etapa.CREDENCIAL) {
        exigir(!entrada.pendiente, Causa.CONTEXTO_OBSOLETO)
        politicas.validarCapacidad()
        cancelar()
        val ticket = tickets.solicitar(fin) ?: throw FalloPrototipo(tickets.causa)
        val senal = CancellationSignal().also { cancelacion = it }
        BiometricPrompt.Builder(this)
            .setTitle("Autoridad temporal del adulto — laboratorio")
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build().authenticate(senal, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    val aceptado = tickets.completar(ticket,
                        resultado.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL)
                    if (cancelacion === senal) cancelacion = null
                    ejecutar(Etapa.CREDENCIAL) {
                        when (aceptado) {
                            FinAutenticacion.COMPROBAR -> mostrar("Credencial comprobada en RAM. Comprobar rollback ADB antes de entrar")
                            FinAutenticacion.SALIR -> {
                                entrada.salirAutorizado()
                                vigilar()
                            }
                            null -> diagnosticar(Diagnostico(Etapa.CREDENCIAL,
                                if (tickets.causa == Causa.NINGUNA) Causa.CALLBACK_OBSOLETO else tickets.causa))
                        }
                    }
                }
                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    tickets.completar(ticket, false)
                    if (cancelacion === senal) cancelacion = null
                    diagnosticar(Diagnostico(Etapa.CREDENCIAL,
                        if (tickets.causa == Causa.NINGUNA) Causa.CALLBACK_OBSOLETO else tickets.causa))
                }
            })
    }
}
