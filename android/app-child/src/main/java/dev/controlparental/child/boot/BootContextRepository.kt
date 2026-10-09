package dev.controlparental.child.boot

import dev.controlparental.child.data.*
import java.util.UUID

data class ObservacionArranque(val systemBootCount: Int?, val elapsedRealtimeMs: Long)
fun interface FuenteArranque { fun observar(): ObservacionArranque }

/** Idempotencia de arranque pura. No restablece secuencia, política ni recuperación. */
class BootContextRepository(
    private val almacen: ChildStateStore,
    private val fuente: FuenteArranque,
    private val crearBootId: () -> String = { UUID.randomUUID().toString() },
) {
    fun actualizarContexto(): CargaEstado = try {
        // Observar DENTRO de la transacción: no permitir timestamps desordenados por carreras.
        val estado = almacen.actualizar { anterior ->
            val observacion = fuente.observar()
            require(observacion.elapsedRealtimeMs >= 0)
            val contador = observacion.systemBootCount?.takeIf { it >= 0 }
            val base = anterior ?: EstadoControl(provisioningStage = EtapaAprovisionamiento.PREPARACION)
            val previo = base.bootContext
            val continuidad = previo != null && contador != null && previo.systemBootCount == contador &&
                observacion.elapsedRealtimeMs >= previo.ultimoElapsedObservado
            val contexto = if (continuidad) {
                previo!!.copy(ultimoElapsedObservado = observacion.elapsedRealtimeMs)
            } else {
                val nuevo = crearBootId()
                require(ValidadorEstado.UUID_V4.matches(nuevo) && nuevo != previo?.bootId)
                ContextoArranque(contador, nuevo, null, observacion.elapsedRealtimeMs)
            }
            base.copy(bootContext = contexto)
        }
        CargaEstado.Disponible(estado)
    } catch (_: RuntimeException) {
        // No borrar archivo corrupto, crear otro vínculo ni autorizar por un fallo.
        CargaEstado.ErrorSeguro
    }
}
