package dev.controlparental.child.recovery

import android.content.Context
import android.os.UserManager
import dev.controlparental.child.admin.DeviceCapabilities
import dev.controlparental.child.boot.FuenteArranqueAndroid
import dev.controlparental.child.data.CargaEstado
import dev.controlparental.child.data.ChildStateStore

/** Solo APIs públicas y datos DP; ninguna identidad privada antes del desbloqueo. */
class FuenteRecuperacionAndroid(contexto: Context, private val almacen: ChildStateStore) : FuenteContextoRecuperacion {
    private val app = contexto.applicationContext.createDeviceProtectedStorageContext()

    override fun leer(): ContextoRecuperacion = try {
        val owner = DeviceCapabilities(app).leerAdministracion().esDeviceOwner == true
        val desbloqueado = app.getSystemService(UserManager::class.java)?.isUserUnlocked == true
        val boot = (almacen.cargar() as? CargaEstado.Disponible)?.estado?.bootContext
        val observacion = FuenteArranqueAndroid(app).observar()
        val continuo = boot != null && observacion.systemBootCount != null &&
            boot.systemBootCount == observacion.systemBootCount && observacion.elapsedRealtimeMs >= boot.ultimoElapsedObservado
        ContextoRecuperacion(owner && desbloqueado && continuo, if (continuo) boot?.bootId else null)
    } catch (_: RuntimeException) {
        ContextoRecuperacion(false, null)
    }
}
