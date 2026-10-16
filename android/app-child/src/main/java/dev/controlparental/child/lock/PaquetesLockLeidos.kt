package dev.controlparental.child.lock

/** Android 14 LockTaskPolicySerializer: join de lista vacía → split de "" produce un elemento vacío.
 * Sólo esa representación equivale a ninguna app. No recortar ni ocultar paquetes desconocidos.
 */
internal fun paquetesLockLeidos(paquetes: Array<String>): Set<String> =
    if (paquetes.size == 1 && paquetes[0].isEmpty()) emptySet() else paquetes.toSet()
