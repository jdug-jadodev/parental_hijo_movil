package dev.controlparental.child.emergency

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.admin.ChildAdminReceiver
import dev.controlparental.child.admin.PolicyEnforcer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Solo lectura. No llama lockNow ni abre marcador: esas pruebas se coordinan. */
@RunWith(AndroidJUnit4::class)
class CondicionesEmergenciaAndroidTest {
    @Test
    fun `precondiciones coinciden con las APIs publicas del sistema`() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val gestor = contexto.getSystemService(DevicePolicyManager::class.java)
        val keyguard = contexto.getSystemService(KeyguardManager::class.java)
        val condiciones = PolicyEnforcer(contexto).leerCondiciones()
        assertEquals(gestor.isDeviceOwnerApp(contexto.packageName), condiciones.esDeviceOwner)
        assertEquals(gestor.isAdminActive(ComponentName(contexto, ChildAdminReceiver::class.java)),
            condiciones.administradorActivo)
        assertEquals(contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN),
            condiciones.admiteAdministracion)
        assertEquals(keyguard.isDeviceSecure, condiciones.tieneCredencialAndroid)
        if (InstrumentationRegistry.getArguments().getString("exigirCredencialAndroid") == "true") {
            assertEquals("La prueba coordinada necesita credencial Android", true, condiciones.tieneCredencialAndroid)
            assertEquals(true, condiciones.puedeSolicitarBloqueo)
        }
    }
}
