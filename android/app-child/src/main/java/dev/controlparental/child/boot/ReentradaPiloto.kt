package dev.controlparental.child.boot

import dev.controlparental.child.lock.ModoLocal

internal enum class FasePiloto { DESARMADO, ARMADO, PENDIENTE_KEYGUARD, ESPERANDO_ACTIVITY, ESPERANDO_LOCKED, LOCKED, FINALIZANDO, ERROR }

/** Un intento terminado no impide recuperar un bloqueo perdido en la misma generación de boot. */
internal fun intentoBloqueoVigente(mismaReferencia: Boolean, fase: FasePiloto, modo: () -> ModoLocal): Boolean =
    mismaReferencia && (fase == FasePiloto.ESPERANDO_LOCKED || (fase == FasePiloto.LOCKED && modo() == ModoLocal.LOCKED))
