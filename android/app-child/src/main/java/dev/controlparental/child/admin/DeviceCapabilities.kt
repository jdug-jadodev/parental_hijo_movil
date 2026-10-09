package dev.controlparental.child.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import dev.controlparental.child.domain.DiagnosticoAdministracion

/** Solo lectura: ser administrador activo NO equivale a ser Device Owner. */
class DeviceCapabilities(contexto: Context) {
    private val contexto = contexto.applicationContext

    fun leerAdministracion(): DiagnosticoAdministracion = try {
        val gestor = contexto.getSystemService(DevicePolicyManager::class.java)
        if (gestor == null) DiagnosticoAdministracion() else DiagnosticoAdministracion(
            esDeviceOwner = gestor.isDeviceOwnerApp(contexto.packageName),
            administradorActivo = gestor.isAdminActive(ComponentName(contexto, ChildAdminReceiver::class.java)),
        )
    } catch (_: RuntimeException) {
        // Un servicio inaccesible no concede protección ni permiso por defecto.
        DiagnosticoAdministracion()
    }
}
