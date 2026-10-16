package dev.controlparental.child.protection

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import dev.controlparental.child.admin.ChildAdminReceiver
import dev.controlparental.child.lock.ArchivoSnapshotAndroid
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Solo uninstall hijo + DISALLOW_APPS_CONTROL para TODAS apps del usuario. Sin grants. */
internal class PoliticasProteccionAndroid(c: Context) : PuertoProteccion {
    private val c = c.applicationContext
    private val dpm = c.getSystemService(DevicePolicyManager::class.java)
    private val um = c.getSystemService(UserManager::class.java)
    private val admin = ComponentName(c, ChildAdminReceiver::class.java)
    override fun validar() {
        exigirProteccion(Build.VERSION.SDK_INT >= 34 && dpm.isDeviceOwnerApp(c.packageName) && dpm.isAdminActive(admin), CausaProteccion.BASELINE)
    }
    override fun leer(): LecturaProteccion {
        validar()
        if (Build.VERSION.SDK_INT < 34) throw FalloProteccion(CausaProteccion.BASELINE)
        return LecturaProteccion(dpm.isUninstallBlocked(null, c.packageName),
            dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_APPS_CONTROL),
            dpm.getUserRestrictionsGlobally().getBoolean(UserManager.DISALLOW_APPS_CONTROL),
            um.hasUserRestriction(UserManager.DISALLOW_APPS_CONTROL))
    }
    override fun uninstall(bloquear: Boolean) { validar(); dpm.setUninstallBlocked(admin, c.packageName, bloquear) }
    override fun control(restringir: Boolean) {
        validar()
        if (restringir) dpm.addUserRestriction(admin, UserManager.DISALLOW_APPS_CONTROL)
        else dpm.clearUserRestriction(admin, UserManager.DISALLOW_APPS_CONTROL)
    }
}

/** IO serial fuera de UI. No reactivar/desactivar por lifecycle/boot/permiso de uso. */
internal object ProteccionAndroid {
    private val ejecutor = Executors.newSingleThreadExecutor { Thread(it, "proteccion-local") }
    private val handler = Handler(Looper.getMainLooper())
    private val ocupado = AtomicBoolean()
    private val generacion = AtomicLong()
    @Volatile var estado = EstadoProteccion(FaseProteccion.SIN_SNAPSHOT); private set
    private fun modelo(c: Context, autoridad: () -> Boolean) = ProteccionLocal(
        ArchivoSnapshotAndroid(File(c.noBackupFilesDir, "proteccion-local-v1.snapshot")), PoliticasProteccionAndroid(c), autoridad)
    fun consultar(c: Context, entregar: (EstadoProteccion) -> Unit) {
        ejecutor.execute { val r = modelo(c) { false }.consultar(); estado = r; handler.post { entregar(r) } }
    }
    /** Sólo desde worker de arranque; lectura serializada con aplicar/restaurar, nunca desde UI. */
    fun permitePrepararControl(c: Context): Boolean = ejecutor.submit<Boolean> {
        modelo(c) { false }.permitePrepararControl()
    }.get()
    fun ejecutar(c: Context, restaurar: Boolean, autoridad: () -> Boolean, entregar: (EstadoProteccion) -> Unit): Boolean {
        if (!autoridad() || !ocupado.compareAndSet(false, true)) return false
        val g = generacion.incrementAndGet()
        estado = EstadoProteccion(FaseProteccion.PENDIENTE)
        ejecutor.execute {
            val m = modelo(c) { generacion.get() == g && autoridad() }
            val r = if (restaurar) m.restaurar() else m.aplicar()
            estado = r; ocupado.set(false); handler.post { entregar(r) }
        }
        return true
    }
}
