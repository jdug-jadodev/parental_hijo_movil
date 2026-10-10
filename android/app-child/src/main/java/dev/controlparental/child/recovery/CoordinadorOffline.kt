package dev.controlparental.child.recovery

import dev.controlparental.child.data.ChildStateStore
import dev.controlparental.child.data.PropositoRecuperacionLocal
import dev.controlparental.child.domain.RelojMonotonico
import dev.controlparental.protocol.AccionOffline
import dev.controlparental.protocol.FirmanteCp1

/** Debe ejecutar trabajos una sola vez, en orden y sin solaparlos. No requiere Android. */
fun interface EjecutorRecuperacionSerial { fun ejecutar(trabajo: () -> Unit) }

/** Ticket por identidad de objeto: no reconstruible desde QR, Bundle, boot o archivo. */
class ReferenciaDesafioOffline internal constructor()

data class DesafioPresentadoOffline(val referencia: ReferenciaDesafioOffline,
    val matriz: MatrizQrOffline, val restanteMs: Long)

/** Sin textos de códigos, públicas, hashes o sobres. No es permiso de enforcement. */
data class ProyeccionOffline(
    val ocupado: Boolean = false,
    val desafio: DesafioPresentadoOffline? = null,
    val respuesta: RespuestaRecuperacion? = null,
    val acceso: AccesoRecuperacionLimitado? = null,
)

/** H05-L01 aislada. Sin singleton ni cableado UI; un dueño RAM y una cola serial. */
class CoordinadorOffline(almacen: ChildStateStore, reloj: RelojMonotonico,
    contexto: FuenteContextoRecuperacion, identidad: FirmanteCp1?,
    private val ejecutor: EjecutorRecuperacionSerial) {
    private val controlador = RecoveryController(almacen, reloj, contexto, identidad)
    private var generacion = Any()
    private var vista = ProyeccionOffline()
    private var limpiezaPendiente = false

    @Synchronized fun proyeccion(): ProyeccionOffline = vista

    fun generar(accion: AccionOffline) {
        val token = synchronized(this) {
            generacion = Any()
            vista = ProyeccionOffline(ocupado = true)
            generacion
        }
        ejecutar(token) {
            controlador.cancelar()
            val resultado = controlador.crearDesafioOffline(accion)
            val sobre = resultado.sobre
            if (sobre == null) ProyeccionOffline(respuesta = RespuestaRecuperacion(resultado.resultado))
            else {
                val matriz = QrOffline.crearDesafio(sobre)
                val restante = controlador.desafioRestanteMs()
                if (restante == null) ProyeccionOffline(respuesta = RespuestaRecuperacion(ResultadoRecuperacion.DESAFIO_VENCIDO))
                else ProyeccionOffline(desafio = DesafioPresentadoOffline(ReferenciaDesafioOffline(), matriz, restante))
            }
        }
    }

    /** La lectura óptica solo produce texto; aquí pasa por transporte y controlador. */
    fun responder(referencia: ReferenciaDesafioOffline, texto: String) {
        val token = synchronized(this) {
            if (vista.ocupado || vista.desafio?.referencia !== referencia) return
            vista = vista.copy(ocupado = true)
            generacion
        }
        ejecutar(token) {
            val resultado = try {
                controlador.aceptarRespuestaOffline(QrOffline.leerRespuestaSinAutorizar(texto))
            } catch (_: RuntimeException) { RespuestaRecuperacion(ResultadoRecuperacion.RESPUESTA_INVALIDA) }
            resumir(resultado)
        }
    }

    /** Solo puerto aislado: no habilita el consumo cerrado en la interfaz Android. */
    fun usarCodigo(proposito: PropositoRecuperacionLocal, texto: String) {
        val token = tomarTrabajo() ?: return
        ejecutar(token) { resumir(controlador.usarCodigo(proposito, texto)) }
    }

    /** El dueño debe solicitar actualización periódica; ningún temporizador oculto. */
    fun actualizar() {
        val token = tomarTrabajo() ?: return
        ejecutar(token) { resumir(proyeccion().respuesta) }
    }

    /** Pausa/salida: limpia de inmediato; no bloquea detrás de firma/IO en curso. */
    fun cancelar() {
        synchronized(this) { generacion = Any(); vista = ProyeccionOffline(); limpiezaPendiente = true }
        try { ejecutor.ejecutar { limpiarSiPendiente() } } catch (_: RuntimeException) {
            // La siguiente tarea aceptada limpiará el núcleo antes de consultar permisos.
        }
    }

    @Synchronized private fun tomarTrabajo(): Any? {
        if (vista.ocupado) return null
        vista = vista.copy(ocupado = true)
        return generacion
    }

    private fun ejecutar(token: Any, trabajo: () -> ProyeccionOffline) {
        try {
            ejecutor.ejecutar tarea@{
                limpiarSiPendiente()
                if (!vigente(token)) return@tarea
                val resultado = try { trabajo() } catch (_: RuntimeException) {
                    controlador.cancelar()
                    ProyeccionOffline(respuesta = RespuestaRecuperacion(ResultadoRecuperacion.ERROR_SEGURO))
                }
                val publicado = synchronized(this) {
                    if (generacion !== token) false else { vista = resultado; true }
                }
                // Carrera con pausa/nuevo desafío: el trabajo viejo no deja sesión RAM.
                if (!publicado) controlador.cancelar()
            }
        } catch (_: RuntimeException) {
            synchronized(this) {
                if (generacion === token) { generacion = Any(); limpiezaPendiente = true; vista = ProyeccionOffline(
                    respuesta = RespuestaRecuperacion(ResultadoRecuperacion.ERROR_SEGURO)) }
            }
            // No llamar al controlador aquí: podría estar firmando en otro hilo.
        }
    }

    @Synchronized private fun vigente(token: Any) = generacion === token

    private fun limpiarSiPendiente() {
        val limpiar = synchronized(this) { limpiezaPendiente.also { limpiezaPendiente = false } }
        if (limpiar) controlador.cancelar()
    }

    private fun resumir(respuesta: RespuestaRecuperacion?): ProyeccionOffline {
        val acceso = controlador.accesoActual()
        val restante = controlador.desafioRestanteMs()
        val desafio = if (restante == null) null else proyeccion().desafio?.copy(restanteMs = restante)
        val espera = controlador.esperaCodigoActual()
        val mensaje = when {
            espera == null -> { controlador.cancelar(); return ProyeccionOffline(
                respuesta = RespuestaRecuperacion(ResultadoRecuperacion.ERROR_SEGURO)) }
            acceso != null -> respuesta
            respuesta?.resultado == ResultadoRecuperacion.AUTORIZADA -> null
            respuesta?.resultado == ResultadoRecuperacion.ESPERA && espera == 0L -> null
            respuesta?.resultado == ResultadoRecuperacion.ESPERA -> respuesta.copy(esperaRestanteMs = espera)
            else -> respuesta
        }
        return ProyeccionOffline(desafio = desafio, respuesta = mensaje, acceso = acceso)
    }
}
