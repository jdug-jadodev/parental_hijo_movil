package dev.controlparental.child.prototype

import android.app.ActivityManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import dev.controlparental.child.admin.PolicyEnforcer
import dev.controlparental.child.emergency.EmergencyAccess

/** Entrada separada debug. Cancelación/fondo no desbloquean; no acepta extras o PIN. */
class PrototipoBloqueoActivity : ComponentActivity() {
    private val tickets = TicketsPrototipo(SystemClock::elapsedRealtime)
    private lateinit var politicas: PoliticasPrototipo
    private lateinit var texto: TextView
    private var cancelacion: CancellationSignal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        politicas = PoliticasPrototipo(this)
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
            ejecutar {
                check(emergenciaVerificada.isChecked) { "Primero comprobar emergencia sin llamar" }
                check(tickets.consumirComprobacion()) { "Primero comprobar credencial en esta pantalla" }
                check(RollbackPrototipo.consumirComprobacion()) { "Falta comprobación ADB actual" }
                politicas.entrar(this)
                mostrar("Entrada solicitada; comprobar LOCKED y salidas manualmente")
            }
        }
        boton("Salir con credencial Android del adulto") { autenticar(FinAutenticacion.SALIR) }
        boton("Cancelar autenticación (no desbloquea)") { cancelar(); mostrar("Autenticación cancelada") }
        boton("Emergencia / Keyguard del sistema") {
            cancelar()
            val resultado = EmergencyAccess(PolicyEnforcer(this)).irAPantallaBloqueo()
            mostrar("Ruta del sistema solicitada: $resultado. No llamar para probar")
        }
        boton("Actualizar estado observado") { mostrar("Consulta local") }
        setContentView(columna)
        mostrar("SOLO LABORATORIO: PIN Android no es código parental final")
    }

    override fun onResume() { super.onResume(); tickets.reanudar(); mostrar("Pantalla en primer plano") }
    override fun onPause() { tickets.fondo(); cancelacion?.cancel(); cancelacion = null; super.onPause() }
    override fun onDestroy() { cancelar(); super.onDestroy() }

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
        texto.text = "PROT01 DEBUG\n$modo\n$mensaje\nSin tiempo, red ni política CP/1. Emergencia independiente."
    }
    private fun ejecutar(accion: () -> Unit) {
        try { accion() } catch (_: RuntimeException) {
            mostrar("Error: NO se confirma éxito. No repetir entrada; consultar rollback ADB")
        }
    }
    private fun autenticar(fin: FinAutenticacion) = ejecutar {
        check(politicas.capacidad())
        cancelar()
        val ticket = tickets.solicitar(fin) ?: error("No hay ticket actual")
        val senal = CancellationSignal().also { cancelacion = it }
        BiometricPrompt.Builder(this)
            .setTitle("Autoridad temporal del adulto — laboratorio")
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build().authenticate(senal, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                    val aceptado = tickets.completar(ticket,
                        resultado.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL)
                    if (cancelacion === senal) cancelacion = null
                    ejecutar {
                        when (aceptado) {
                            FinAutenticacion.COMPROBAR -> mostrar("Credencial comprobada en RAM. Comprobar rollback ADB antes de entrar")
                            FinAutenticacion.SALIR -> {
                                check(politicas.restaurar())
                                mostrar("Políticas anteriores restauradas y NONE observado")
                            }
                            null -> mostrar("Ticket cancelado/antiguo/fuera de primer plano: NO autoriza")
                        }
                    }
                }
                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    tickets.completar(ticket, false)
                    if (cancelacion === senal) cancelacion = null
                    mostrar("Credencial cancelada/no disponible; NO desbloqueado. Estado consultado")
                }
            })
    }
}
