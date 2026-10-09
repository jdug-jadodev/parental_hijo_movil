package dev.controlparental.child.admin

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dev.controlparental.child.domain.CondicionesEmergencia
import dev.controlparental.child.emergency.PuertoPantallaBloqueo

/** Único adaptador que modifica políticas. H02 solo solicita lockNow sin flags. */
class PolicyEnforcer(contexto: Context) : PuertoPantallaBloqueo {
    private val contexto = contexto.applicationContext

    override fun leerCondiciones(): CondicionesEmergencia = try {
        val gestor = contexto.getSystemService(DevicePolicyManager::class.java)
        val keyguard = contexto.getSystemService(KeyguardManager::class.java)
        CondicionesEmergencia(
            esDeviceOwner = gestor?.isDeviceOwnerApp(contexto.packageName),
            administradorActivo = gestor?.isAdminActive(ComponentName(contexto, ChildAdminReceiver::class.java)),
            admiteAdministracion = contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN),
            // isKeyguardSecure también considera PIN SIM: no sustituye esta comprobación.
            tieneCredencialAndroid = keyguard?.isDeviceSecure,
        )
    } catch (_: RuntimeException) {
        CondicionesEmergencia()
    }

    override fun solicitarBloqueoSistema() {
        // Revalidar en el límite Android; no confiar en un snapshot anterior de la UI.
        if (!leerCondiciones().puedeSolicitarBloqueo) throw SecurityException("No se cumplen las condiciones de bloqueo del sistema")
        val gestor = contexto.getSystemService(DevicePolicyManager::class.java)
            ?: throw IllegalStateException("Servicio de administración no disponible")
        gestor.lockNow()
        // No APPLIED: no demuestra que el usuario haya llegado al marcador de emergencia.
    }
}
