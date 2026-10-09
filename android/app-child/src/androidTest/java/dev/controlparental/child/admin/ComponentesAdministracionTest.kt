package dev.controlparental.child.admin

import android.app.admin.DeviceAdminInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.domain.EstadoPreparacion
import dev.controlparental.child.ui.LauncherActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Solo diagnóstico/metadata. No aprovisiona, inicia quiosco ni cambia políticas. */
@RunWith(AndroidJUnit4::class)
class ComponentesAdministracionTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION") // Sobrecarga disponible también en API 31 y 32.
    @Test
    fun `receiver protegido declara solo las politicas previstas`() {
        val componente = ComponentName(contexto, ChildAdminReceiver::class.java)
        val info = contexto.packageManager.getReceiverInfo(componente, PackageManager.GET_META_DATA)
        assertTrue(info.exported)
        assertEquals("android.permission.BIND_DEVICE_ADMIN", info.permission)
        val admin = DeviceAdminInfo(contexto, ResolveInfo().apply { activityInfo = info })
        assertTrue(admin.usesPolicy(DeviceAdminInfo.USES_POLICY_FORCE_LOCK))
        assertTrue(admin.usesPolicy(DeviceAdminInfo.USES_POLICY_WIPE_DATA))
        listOf(DeviceAdminInfo.USES_POLICY_LIMIT_PASSWORD, DeviceAdminInfo.USES_POLICY_WATCH_LOGIN,
            DeviceAdminInfo.USES_POLICY_RESET_PASSWORD,
            DeviceAdminInfo.USES_POLICY_EXPIRE_PASSWORD, DeviceAdminInfo.USES_ENCRYPTED_STORAGE,
            DeviceAdminInfo.USES_POLICY_DISABLE_CAMERA, DeviceAdminInfo.USES_POLICY_DISABLE_KEYGUARD_FEATURES)
            .forEach { assertFalse(admin.usesPolicy(it)) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `HOME y LAUNCHER apuntan al launcher declarado sin configurar HOME persistente`() {
        val esperado = LauncherActivity::class.java.name
        listOf(Intent.CATEGORY_HOME, Intent.CATEGORY_LAUNCHER).forEach { categoria ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(categoria).setPackage(contexto.packageName)
            val actividades = contexto.packageManager.queryIntentActivities(intent, 0)
            assertEquals(listOf(esperado), actividades.map { it.activityInfo.name })
        }
    }

    @Test
    fun `diagnostico coincide con Android y permite exigir Owner real en prueba coordinada`() {
        val gestor = contexto.getSystemService(DevicePolicyManager::class.java)
        val diagnostico = DeviceCapabilities(contexto).leerAdministracion()
        assertEquals(gestor.isDeviceOwnerApp(contexto.packageName), diagnostico.esDeviceOwner)
        assertEquals(gestor.isAdminActive(ComponentName(contexto, ChildAdminReceiver::class.java)),
            diagnostico.administradorActivo)
        val exigir = InstrumentationRegistry.getArguments().getString("exigirDeviceOwner")
        if (exigir == "true") {
            assertEquals(true, diagnostico.esDeviceOwner)
            assertEquals(true, diagnostico.administradorActivo)
            assertEquals(EstadoPreparacion.BLOQUEADO_EN_PREPARACION, diagnostico.estadoPreparacion)
        } else if (diagnostico.esDeviceOwner == false) {
            assertEquals(EstadoPreparacion.NO_CONFIGURADO, diagnostico.estadoPreparacion)
        }
        assertFalse(gestor.isLockTaskPermitted(contexto.packageName))
    }
}
