package dev.controlparental.child.pairing

/** Sólo proyección de lectura local, sin generación ni autoridad/recibo de red. */
internal fun descripcionEstadoPair(carga: CargaPairCp2): String = when (carga) {
    CargaPairCp2.Ausente -> "AUSENTE en este hijo: sin registro local. No acredita el estado del padre ni autoriza rehacer un intento previo."
    CargaPairCp2.Incierta -> "INCIERTO: puede existir un intento pendiente/incompleto o corrupción. Alta cerrada; no borrar, regenerar ni reintentar."
    is CargaPairCp2.Guardado -> "GUARDADO LOCALMENTE tras readback. PENDIENTE CANAL Y RECUPERACIÓN; no permiso de uso ni bloqueo acreditado."
}
