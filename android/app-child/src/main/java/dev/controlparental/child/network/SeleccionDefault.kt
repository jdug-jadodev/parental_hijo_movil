package dev.controlparental.child.network

/** Identificador solo RAM. Dos lecturas de default rodean capabilities; nunca datos del callback viejo. */
internal fun seleccionarDefault(antes: Long?, despues: Long?, internet: Boolean, validada: Boolean): ValidacionRed =
    if (antes == null || antes != despues) ValidacionRed.DESCONOCIDA
    else if (internet && validada) ValidacionRed.VALIDADA else ValidacionRed.NO_VALIDADA
