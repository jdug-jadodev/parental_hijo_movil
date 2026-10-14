package dev.controlparental.child.local

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.util.UUID

/** SOLO almacén efímero, alias y rutas propios de cada test; no credencial adulta real. */
@RunWith(AndroidJUnit4::class)
class AlmacenAdultoAndroidTest {
    private fun aislado(accion: (AlmacenAdultoAndroid, File, String) -> Unit) {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val id = UUID.randomUUID().toString()
        val d = File(c.noBackupFilesDir, "prueba-adulto-$id")
        val alias = "prueba.adulto.$id"
        try { accion(AlmacenAdultoAndroid(c, d, alias), d, alias) }
        finally {
            val k = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (k.containsAlias(alias)) k.deleteEntry(alias)
            for (nombre in listOf("registro.enc", "configurado")) {
                for (sufijo in listOf("", ".bak", ".new")) File(d, nombre + sufijo).delete()
            }
            d.delete() // No borrado recursivo ni ruta de producto.
        }
    }
    private fun registro() = RegistroAdulto(1, "Pregunta sintética Android", PbkdfAdulto().crear("00123"),
        PbkdfAdulto().crear("Respuesta sintética Android"), boot = 1)
    @Test fun cifradoKeystoreAtomicReadbackSinSecretosEnArchivo() = aislado { a, d, alias ->
        assertNull(a.leer()); val r = registro(); a.guardar(r)
        val leido = AlmacenAdultoAndroid(InstrumentationRegistry.getInstrumentation().targetContext, d, alias).leer()!!
        assertArrayEquals(CodecAdulto.codificar(r), CodecAdulto.codificar(leido))
        assertTrue(PbkdfAdulto().coincide("00123", leido.codigo))
        val bytes = File(d, "registro.enc").readBytes()
        assertFalse(bytes.toString(Charsets.ISO_8859_1).contains("Respuesta sintética Android"))
        val k = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertNull(k.getKey(alias, null).encoded)
        a.guardar(r.copy(revision = 2, fallos = 5, esperaMs = 60_000))
        assertEquals(5, a.leer()!!.fallos)
    }
    @Test fun corrupcionAutenticadaNoAbreAlta() = aislado { a, d, _ ->
        a.guardar(registro())
        val f = File(d, "registro.enc"); val bytes = f.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); f.writeBytes(bytes)
        try { a.leer(); fail("Debe fallar cerrado") } catch (_: Exception) { }
    }
    @Test fun claveOArchivoPerdidosNoSeRegeneran() = aislado { a, d, alias ->
        a.guardar(registro()); assertTrue(File(d, "registro.enc").delete())
        try { a.leer(); fail("No es instalación nueva") } catch (_: Exception) { }
        val k = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }; k.deleteEntry(alias)
        try { a.guardar(registro()); fail("No recrear clave") } catch (_: Exception) { }
        assertFalse(k.containsAlias(alias))
    }
    @Test fun actividadAdultaNoExportada() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val a = c.packageManager.getActivityInfo(ComponentName(c.packageName,
            "dev.controlparental.child.ui.AccesoAdultoActivity"), 0)
        assertFalse(a.exported)
    }
    @Test fun clavePerdidaConRegistroExistenteFallaSinAlta() = aislado { a, _, alias ->
        a.guardar(registro())
        val k = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }; k.deleteEntry(alias)
        try { a.leer(); fail("Debe fallar cerrado sin clave") } catch (_: Exception) { }
        assertFalse(k.containsAlias(alias))
    }
}
