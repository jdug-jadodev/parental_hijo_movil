package dev.controlparental.child.boot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.lock.ArchivoSnapshotAndroid
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** DP efímero/contexto simulado. No stateH04/credencial/snapshot real ni DPM writes. */
@RunWith(AndroidJUnit4::class)
class PilotoDpAtomicTest {
    private fun aislado(accion: (AlmacenPiloto, File) -> Unit) {
        val dp = InstrumentationRegistry.getInstrumentation().targetContext.createDeviceProtectedStorageContext()
        val base = "prueba-boot-${UUID.randomUUID()}"
        val marca = File(dp.noBackupFilesDir, "$base.marca"); val r = File(dp.noBackupFilesDir, "$base.registro")
        try {
            accion(AlmacenPiloto(ArchivoSnapshotAndroid(marca), ArchivoSnapshotAndroid(r),
                FuenteArranque { ObservacionArranque(42, 100) }), r)
        } finally { for (f in listOf(marca, r)) for (s in listOf("", ".bak", ".new")) File(f.path + s).delete() }
    }
    @Test fun dpReadbackDesarmeRestauraVirgen() = aislado { m, _ ->
        assertNull(m.leer()); val r = m.armar { true }; assertEquals(r, m.leer())
        assertTrue(m.vigente(r)); m.desarmar { true }; assertFalse(m.vigente(r))
        m.finalizar(true); assertNull(m.leer())
    }
    @Test fun corrupcionNoAltaLibre() = aislado { m, r ->
        m.armar { true }; r.writeBytes(byteArrayOf(0))
        try { m.leer(); fail() } catch (_: Exception) {}
        try { m.armar { true }; fail() } catch (_: Exception) {}
    }
    @Test fun backupAtomicDpMantieneArmado() = aislado { m, r ->
        val s = m.armar { true }; File(r.path + ".bak").writeBytes(r.readBytes()); r.delete()
        assertEquals(s, m.leer()); assertTrue(r.exists())
    }
}
