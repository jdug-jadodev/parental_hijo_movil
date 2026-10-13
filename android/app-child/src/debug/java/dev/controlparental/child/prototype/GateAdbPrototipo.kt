package dev.controlparental.child.prototype

/** Solo diagnóstico de una ruta shell ya autorizada; nunca concede salida o permiso. */
internal fun gateAdbPrototipo(modo: ModoPrototipo, snapshot: Boolean,
    snapshotIntegro: Boolean, listaPropia: Boolean, featuresPropios: Boolean): Causa = when {
    !snapshot && modo == ModoPrototipo.NONE -> Causa.NINGUNA
    !snapshot -> Causa.BASELINE_INCOMPATIBLE
    !snapshotIntegro -> Causa.RESTAURACION_NO_CONFIRMADA
    modo != ModoPrototipo.LOCKED -> Causa.SNAPSHOT_PENDIENTE
    !listaPropia || !featuresPropios -> Causa.RESTAURACION_AJENA
    else -> Causa.NINGUNA
}
