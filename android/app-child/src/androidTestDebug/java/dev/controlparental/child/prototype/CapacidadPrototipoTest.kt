package dev.controlparental.child.prototype

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Solo lectura del propio paquete/capacidad. Nunca autentica ni aplica políticas. */
class CapacidadPrototipoTest {
    @Test fun `baseline debug permite prueba pero no concede DUMP a la app`() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val politicas = PoliticasPrototipo(contexto)
        assertTrue("Owner y credencial existente necesarios", politicas.capacidad())
        assertFalse("No debe haber una prueba pendiente", politicas.pendiente())
        assertEquals(ActivityManager.LOCK_TASK_MODE_NONE, politicas.modo())
        assertEquals(PackageManager.PERMISSION_DENIED,
            contexto.checkPermission(Manifest.permission.DUMP, Process.myPid(), Process.myUid()))
        @Suppress("DEPRECATION")
        val receptor = contexto.packageManager.getReceiverInfo(
            ComponentName(contexto, RollbackPrototipo::class.java), 0)
        assertTrue(receptor.exported)
        assertEquals(Manifest.permission.DUMP, receptor.permission)
        @Suppress("DEPRECATION")
        val servicio = contexto.packageManager.getServiceInfo(
            ComponentName(contexto, ServicioTemporal::class.java), 0)
        assertFalse(servicio.exported)
        assertEquals(android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
            servicio.foregroundServiceType)
    }
}
