package dev.controlparental.child.network

/** Una obligación ya vencida también necesita enforcement después de quitar el Keyguard.
 * Sin modelo de red sólo solicita revisar el enrolamiento de arranque; no lo crea.
 */
internal fun requiereReentradaAlDespertar(modelo: UsoRedLocal?, cerrando: Boolean): Boolean {
    if (cerrando || modelo?.uso == UsoLocal.FINALIZANDO) return false
    modelo?.referencia?.let(modelo::comprobarDeadline)
    return modelo?.permiteUso != true
}
