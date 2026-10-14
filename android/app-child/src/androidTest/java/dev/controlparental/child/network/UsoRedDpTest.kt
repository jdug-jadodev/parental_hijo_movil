package dev.controlparental.child.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.lock.ArchivoSnapshotAndroid
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** DP UUID/contexto simulado. No políticas/credenciales/callbackred reales. */
@RunWith(AndroidJUnit4::class)
class UsoRedDpTest {
    @Test fun temporalSoloRamNoSeReconstruyeDesdeDP() = aislado { m, _ ->
        assertTrue(m.preparar { true })
        assertTrue(m.autorizar(UsoLocal.EXCEPCION_OFFLINE, MinutosUso.desdeTexto("1")!!) { true })
        assertEquals(60100L, m.vencimientoUso)
        assertTrue(m.cargar()); assertFalse(m.permiteUso)
        assertNull(m.vencimientoUso); assertNull(m.inicioUso)
        assertTrue(m.cerrar { true }); assertTrue(m.finalizar(true))
    }
    private fun aislado(accion: (UsoRedLocal, File) -> Unit) {
        val c = InstrumentationRegistry.getInstrumentation().targetContext.createDeviceProtectedStorageContext()
        val base = "prueba-red-${UUID.randomUUID()}"
        val marca = File(c.noBackupFilesDir, "$base.marca"); val archivo = File(c.noBackupFilesDir, "$base.registro")
        try { accion(UsoRedLocal(ArchivoSnapshotAndroid(marca), ArchivoSnapshotAndroid(archivo), { 100L }, { 42 }), archivo) }
        finally { for (f in listOf(marca, archivo)) for (s in listOf("", ".bak", ".new")) File(f.path + s).delete() }
    }
    @Test fun obligacionReadbackNoReconstruyeExcepcion() = aislado { m, _ ->
        assertTrue(m.preparar { true }); assertTrue(m.autorizar(UsoLocal.EXCEPCION_OFFLINE) { true })
        assertTrue(m.cargar()); assertFalse(m.permiteUso)
        assertTrue(m.cerrar { true }); assertTrue(m.finalizar(true))
    }
    @Test fun corrupcionNoNuevoEnrolamiento() = aislado { m, r ->
        m.preparar { true }; r.writeBytes(byteArrayOf(0))
        assertFalse(m.cargar()); assertFalse(m.preparar { true }); assertFalse(m.permiteUso)
    }
    @Test fun atomicBackupConservaBloqueoRequerido() = aislado { m, r ->
        m.preparar { true }; File(r.path + ".bak").writeBytes(r.readBytes()); r.delete()
        assertTrue(m.cargar()); assertFalse(m.permiteUso); assertTrue(r.exists())
    }
}
