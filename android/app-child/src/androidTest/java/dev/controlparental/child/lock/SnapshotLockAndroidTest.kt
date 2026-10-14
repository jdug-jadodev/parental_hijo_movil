package dev.controlparental.child.lock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** AtomicFile efímero, políticas FAKE: ninguna escritura DPM ni autenticación del adulto. */
@RunWith(AndroidJUnit4::class)
class SnapshotLockAndroidTest {
    private class PoliticasAisladas : PoliticasLockLocal {
        override val paquete = "fixture.hijo"; override val featuresPropios = 48
        var lista = setOf("fixture.previa"); var flags = 0; var estado = ModoLocal.NONE
        override fun validar() {}
        override fun leerPaquetes() = lista
        override fun leerFeatures() = flags
        override fun paquetes(valor: Set<String>) { lista = valor; estado = ModoLocal.NONE }
        override fun features(valor: Int) { flags = valor }
        override fun modo() = estado
        override fun iniciar() { estado = ModoLocal.LOCKED }
    }
    private fun aislado(accion: (ArchivoSnapshotAndroid, File) -> Unit) {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val f = File(c.noBackupFilesDir, "prueba-lock-${UUID.randomUUID()}.snapshot")
        try { accion(ArchivoSnapshotAndroid(f), f) }
        finally { for (s in listOf("", ".bak", ".new")) File(f.path + s).delete() }
    }
    @Test fun snapshotReadbackYRestoreSoloPoliticasFake() = aislado { f, _ ->
        assertNull(f.leer()); val p = PoliticasAisladas(); val a = AdaptadorLockLocal(f, p)
        a.preparar(); assertNotNull(f.leer()); a.aplicar(); a.solicitarInicio()
        assertEquals(ModoLocal.LOCKED, p.estado)
        a.restaurar(); assertTrue(a.restaurado()); assertNull(f.leer())
        assertEquals(setOf("fixture.previa"), p.lista); assertEquals(0, p.flags)
    }
    @Test fun corrupcionNoRestauraNiBorra() = aislado { f, archivo ->
        val p = PoliticasAisladas(); val a = AdaptadorLockLocal(f, p); a.preparar()
        archivo.writeBytes(byteArrayOf(0))
        try { a.restaurar(); fail() } catch (e: FalloLocal) { assertEquals(CausaLocal.SNAPSHOT, e.causa) }
        assertNotNull(f.leer()); assertEquals(setOf("fixture.previa"), p.lista)
    }
}
