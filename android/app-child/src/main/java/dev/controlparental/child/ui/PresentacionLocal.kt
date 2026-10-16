package dev.controlparental.child.ui

import dev.controlparental.child.lock.ModoLocal
import dev.controlparental.child.network.UsoLocal
import dev.controlparental.child.protection.EstadoProteccion
import dev.controlparental.child.protection.FaseProteccion

/** Presentación solamente: nunca concede uso ni sustituye la lectura del sistema. */
internal data class ResumenLocal(val estado: String, val tiempo: String, val modalidad: String, val proteccion: String)
internal fun puedeAbrirApps(modo: ModoLocal?, uso: UsoLocal, permiso: Boolean, listo: Boolean,
    vencimiento: Long?, graciaRed: Long?, ahora: Long): Boolean = modo == ModoLocal.NONE && permiso && listo &&
    uso in setOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE) &&
    (vencimiento == null || ahora < vencimiento) &&
    (uso != UsoLocal.NORMAL || graciaRed == null || ahora < graciaRed)

internal fun presentarEstadoLocal(modo: ModoLocal?, uso: UsoLocal, permiso: Boolean, vencimiento: Long?,
    ahora: Long, proteccion: EstadoProteccion, graciaRed: Long? = null): ResumenLocal {
    val vigente = permiso && uso in setOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE) &&
        (vencimiento == null || ahora < vencimiento) &&
        (uso != UsoLocal.NORMAL || graciaRed == null || ahora < graciaRed)
    val estado = when {
        modo == ModoLocal.LOCKED -> "Bloqueado"
        modo == ModoLocal.NONE && vigente -> "Permitido"
        else -> "Estado por comprobar"
    }
    val tiempo = when {
        !vigente -> "Sin tiempo autorizado activo"
        vencimiento == null -> "Sin límite de tiempo"
        else -> {
            val s = (vencimiento - ahora + 999) / 1000
            "%d:%02d:%02d restantes".format(s / 3600, s / 60 % 60, s % 60)
        }
    }
    val modalidad = when {
        !vigente -> "Sin permiso de uso"
        uso == UsoLocal.NORMAL -> "Normal · requiere internet"
        uso == UsoLocal.EXCEPCION_OFFLINE -> "Excepción sin internet"
        else -> "Permiso por comprobar"
    }
    val l = proteccion.lectura
    val protegida = proteccion.fase == FaseProteccion.ACTIVA && l?.uninstallEfectivo == true &&
        l.controlPropio && l.controlEfectivo
    return ResumenLocal(estado, tiempo, modalidad, when {
        protegida -> "Protección activa"
        proteccion.fase == FaseProteccion.ERROR || l == null -> "Protección por comprobar"
        else -> "Protección pendiente"
    })
}

internal enum class PantallaLocal { CONTROL, SEGURIDAD, DIAGNOSTICO }
/** Navegación sin autoridad ni efectos sobre permisos; no restaura sesiones tras recreación. */
internal class NavegacionLocal {
    var pantalla = PantallaLocal.CONTROL; private set
    fun abrir(destino: PantallaLocal, autenticada: Boolean): Boolean {
        if (!autenticada) return false
        pantalla = destino; return true
    }
}

/** Sólo hilo UI. Descarta pulsaciones de vistas retiradas y reentrancia, no valida códigos. */
internal class GuardiaVistaLocal {
    var revision = 0L; private set
    private var ejecutando = false
    fun retirar() { revision++ }
    fun ejecutar(vista: Long, activa: Boolean, accion: () -> Unit): Boolean {
        if (!activa || vista != revision || ejecutando) return false
        ejecutando = true
        try { accion() } finally { ejecutando = false }
        return true
    }
}
