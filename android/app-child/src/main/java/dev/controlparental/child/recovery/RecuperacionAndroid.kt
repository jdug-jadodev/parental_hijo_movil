package dev.controlparental.child.recovery

import android.content.Context
import android.os.SystemClock
import dev.controlparental.child.data.CargaEstado
import dev.controlparental.child.data.ChildStateStore
import dev.controlparental.child.data.EstadoAndroid

/** Un controlador RAM por proceso. Usar en EjecutorEstado, nunca hacer IO en la UI. */
object RecuperacionAndroid {
    private var controlador: RecoveryController? = null

    @Synchronized
    private fun obtener(contexto: Context, almacen: ChildStateStore): RecoveryController = controlador ?: RecoveryController(
        almacen, { SystemClock.elapsedRealtime() }, FuenteRecuperacionAndroid(contexto, almacen),
        // Sin identidad inventada: la integración de alias/vínculo de Keystore corresponde a H08.
    ).also { controlador = it }

    fun resumen(contexto: Context, respuesta: RespuestaRecuperacion? = null): EstadoPantallaRecuperacion = try {
        val almacen = EstadoAndroid.obtener(contexto)
        val carga = almacen.cargar()
        val disponibilidad = when {
            carga !is CargaEstado.Disponible -> DisponibilidadRecuperacion.ERROR_SEGURO
            carga.estado.vinculo == null -> DisponibilidadRecuperacion.SIN_VINCULO
            !FuenteRecuperacionAndroid(contexto, almacen).leer().usuarioDesbloqueado -> DisponibilidadRecuperacion.NO_DISPONIBLE
            else -> DisponibilidadRecuperacion.DISPONIBLE
        }
        val acceso = if (disponibilidad == DisponibilidadRecuperacion.DISPONIBLE) obtener(contexto, almacen).accesoActual() else null
        if (disponibilidad != DisponibilidadRecuperacion.DISPONIBLE) cancelarSiExiste()
        val restante = if (disponibilidad == DisponibilidadRecuperacion.DISPONIBLE) obtener(contexto, almacen).esperaCodigoActual() else null
        if (disponibilidad == DisponibilidadRecuperacion.DISPONIBLE && restante == null) {
            cancelarSiExiste()
            EstadoPantallaRecuperacion(DisponibilidadRecuperacion.ERROR_SEGURO)
        } else {
            val mensaje = when {
                acceso != null -> respuesta?.takeUnless { it.resultado == ResultadoRecuperacion.ESPERA }
                restante != null && restante > 0 -> RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, restante)
                respuesta?.resultado in setOf(ResultadoRecuperacion.AUTORIZADA, ResultadoRecuperacion.ESPERA) -> null
                else -> respuesta
            }
            EstadoPantallaRecuperacion(disponibilidad, respuesta = mensaje, acceso = acceso)
        }
    } catch (_: RuntimeException) {
        cancelarSiExiste()
        EstadoPantallaRecuperacion(DisponibilidadRecuperacion.ERROR_SEGURO)
    }

    fun usarCodigo(contexto: Context, proposito: dev.controlparental.child.data.PropositoRecuperacionLocal,
        texto: String): EstadoPantallaRecuperacion {
        return try {
            // No quemar una vía de recuperación real antes de implementar sus herramientas.
            val previo = resumen(contexto)
            if (!previo.permiteEnviar) return previo.copy(respuesta = RespuestaRecuperacion(ResultadoRecuperacion.NO_DISPONIBLE))
            val almacen = EstadoAndroid.obtener(contexto)
            val respuesta = obtener(contexto, almacen).usarCodigo(proposito, texto)
            resumen(contexto, respuesta)
        } catch (_: RuntimeException) {
            cancelarSiExiste()
            EstadoPantallaRecuperacion(DisponibilidadRecuperacion.ERROR_SEGURO)
        }
    }

    @Synchronized
    fun cancelarSiExiste() { controlador?.cancelar() }
}
