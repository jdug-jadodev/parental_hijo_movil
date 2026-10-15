package dev.controlparental.child.orders

import dev.controlparental.protocol.cp2.*

/** Proyección sin autoridad: VERIFIED exige confirmación de ESA revisión y observación actual compatible. */
internal object ProyeccionStateCp2 {
    fun crear(r: RegistroControl, pairId: String, syncNonce: String, secuencia: Long, sampled: Long,
        motivo: MotivoStateCp2, reto: RetoOrdenCp2?, red: Boolean?, owner: Boolean?, lockTask: Boolean?, permiteRam: Boolean,
        observacionCompatible: Boolean): StateCp2 {
        val d = r.decision
        val fase = when {
            d == null -> FaseStateCp2.NONE
            d.action == AccionOrdenCp2.LOCK -> FaseStateCp2.BLOCKED
            d.deadline != null && sampled >= d.deadline -> FaseStateCp2.EXPIRED
            else -> FaseStateCp2.PERMITTED
        }
        val efectoConfirmado = observacionCompatible && d?.efecto == EfectoControl.VERIFIED && owner == true &&
            ((fase == FaseStateCp2.PERMITTED && permiteRam && lockTask == false) || (fase == FaseStateCp2.BLOCKED && !permiteRam && lockTask == true))
        val efecto = when {
            d == null -> EfectoStateCp2.UNKNOWN
            efectoConfirmado -> EfectoStateCp2.VERIFIED
            d.efecto == EfectoControl.VERIFIED -> EfectoStateCp2.UNKNOWN
            else -> EfectoStateCp2.valueOf(d.efecto.name)
        }
        val modo = when {
            !efectoConfirmado -> ModoStateCp2.UNKNOWN
            fase == FaseStateCp2.PERMITTED -> ModoStateCp2.ALLOWED
            else -> ModoStateCp2.LOCKED
        }
        val restante = d?.deadline?.let { if (it <= sampled) 0L else (it - sampled - 1) / 1000 + 1 }
        val efectivo = EfectivoStateCp2(modo, if (owner == false) MotivoStateCp2.NOT_DEVICE_OWNER else motivo,
            if (d == null) FuenteStateCp2.NONE else if (d.local) FuenteStateCp2.LOCAL else FuenteStateCp2.REMOTE,
            d?.revision, d?.action, fase, d?.accepted, d?.accepted, d?.deadline, restante, efecto)
        val remoto = r.remoto?.let { RemotoStateCp2(it.orden.commandId, it.orden.seq, it.hash, it.orden.bootId, it.orden.childSessionId,
            it.acceptedRevision, ResultadoStateCp2.valueOf(it.resultado.name),
            if (it.resultado == ResultadoControl.ENFORCEMENT_FAILED) MotivoResultadoCp2.ENFORCEMENT_ERROR else MotivoResultadoCp2.OK, it.resultElapsed) }
        return StateCp2(pairId, r.bootId, r.sesion, syncNonce, secuencia, sampled, r.revision, r.seq,
            reto?.let { ChallengeStateCp2(it.nonce, it.action, it.revision, it.issuedElapsedMs, it.expiresElapsedMs) },
            efectivo, remoto, red, null, owner, lockTask).also(CodecStateCp2::validar)
    }
}
