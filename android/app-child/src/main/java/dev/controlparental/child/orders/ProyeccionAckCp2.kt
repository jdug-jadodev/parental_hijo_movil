package dev.controlparental.child.orders

import dev.controlparental.protocol.cp2.*

/** Sólo datos ya verificados/estado consolidado; nunca extrae identidad/hash de una trama no autenticada. */
internal object ProyeccionAckCp2 {
    fun retenida(m: RemotoControl, evaluada: Long) = AckCp2(m.orden.pairId, m.orden.bootId, m.orden.childSessionId, m.orden.commandId,
        m.orden.seq, m.hash, m.acceptedRevision, evaluada, ResultadoAckCp2.valueOf(m.resultado.name),
        if (m.resultado == ResultadoControl.ENFORCEMENT_FAILED) MotivoAckCp2.ENFORCEMENT_ERROR else MotivoAckCp2.OK, m.resultElapsed)
    fun respuesta(v: OrdenVerificadaCp2, r: RegistroControl, retenido: RemotoControl?, reto: RetoOrdenCp2?, ahora: Long): AckCp2 {
        val o = v.mensaje as PoliticaOrdenCp2
        if (retenido != null) return retenida(retenido, r.revision)
        val motivo = when {
            o.pairId != r.pareja.substringBefore(':') -> MotivoAckCp2.WRONG_PAIR
            o.seq == r.seq || o.commandId == r.remoto?.orden?.commandId -> MotivoAckCp2.SEQUENCE_CONFLICT
            o.seq < r.seq -> MotivoAckCp2.UNKNOWN_COMMAND
            o.bootId != r.bootId -> MotivoAckCp2.STALE_BOOT
            o.childSessionId != r.sesion -> MotivoAckCp2.STALE_SESSION
            r.revision == Long.MAX_VALUE -> MotivoAckCp2.COUNTER_EXHAUSTED
            o.expectedRevision != r.revision -> MotivoAckCp2.STALE_REVISION
            reto == null || ahora >= reto.expiresElapsedMs || o.commandNonce != reto.nonce || o.action != reto.action -> MotivoAckCp2.STALE_NONCE
            else -> MotivoAckCp2.STALE_REVISION // No se aceptó el CAS; jamás simular éxito por firma.
        }
        return AckCp2(o.pairId, o.bootId, o.childSessionId, o.commandId, o.seq, v.payloadSha256B64, null, r.revision,
            if (motivo == MotivoAckCp2.UNKNOWN_COMMAND) ResultadoAckCp2.UNKNOWN_COMMAND else ResultadoAckCp2.REJECTED, motivo, null).also(CodecAckCp2::validar)
    }
}
