package dev.controlparental.child.protection

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.lock.ArchivoSnapshotAndroid
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Archivos efímeros y políticas ficticias: no DPM real ni autoridad parental simulada. */
@RunWith(AndroidJUnit4::class)
class ProteccionAtomicTest {
    private class PoliticasAisladas : PuertoProteccion {
        var u = false; var control = false
        override fun validar() {}
        override fun leer() = LecturaProteccion(u, control, false, control)
        override fun uninstall(bloquear: Boolean) { u = bloquear }
        override fun control(restringir: Boolean) { control = restringir }
    }
    private fun aislado(accion: (ArchivoSnapshotAndroid, File, PoliticasAisladas) -> Unit) {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val f = File(c.noBackupFilesDir, "prueba-proteccion-${UUID.randomUUID()}.snapshot")
        try { accion(ArchivoSnapshotAndroid(f), f, PoliticasAisladas()) }
        finally { for (s in listOf("", ".bak", ".new")) File(f.path + s).delete() }
    }
    @Test fun readbackRestoreEfimero() = aislado { f, _, p ->
        val m = ProteccionLocal(f, p) { true }; assertNull(f.leer())
        assertEquals(FaseProteccion.ACTIVA, m.aplicar().fase); assertNotNull(f.leer())
        assertEquals(FaseProteccion.RESTAURADA, m.restaurar().fase); assertNull(f.leer())
        assertFalse(p.u || p.control)
    }
    @Test fun corrupcionConservaObligacionSinDefaults() = aislado { f, ruta, p ->
        val m = ProteccionLocal(f, p) { true }; m.aplicar(); ruta.writeBytes(byteArrayOf(0))
        assertEquals(CausaProteccion.SNAPSHOT, m.restaurar().causa)
        assertTrue(p.u && p.control); assertNotNull(f.leer())
    }
    @Test fun rollbackSoloRutaBackup() = aislado { f, ruta, p ->
        val b = CodecProteccion.codificar(p.leer()); File(ruta.path + ".bak").writeBytes(b)
        assertArrayEquals(b, f.leer()); assertTrue(ruta.exists())
        assertEquals(FaseProteccion.RESTAURADA, ProteccionLocal(f, p) { true }.restaurar().fase)
        assertNull(f.leer())
    }
}
