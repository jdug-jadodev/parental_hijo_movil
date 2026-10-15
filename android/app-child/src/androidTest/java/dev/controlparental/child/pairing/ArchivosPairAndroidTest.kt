package dev.controlparental.child.pairing

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Solo archivos aislados aleatorios; jamás usa ruta real CP2/DP/credenciales/Keystore. No ejecutada en PAIR03. */
class ArchivosPairAndroidTest {
    @Test fun reservaRegistroYRestosEnDirectorioAislado() {
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "pair-prueba-${UUID.randomUUID()}")
        check(dir.mkdir())
        try {
            val base = File(dir, "registro")
            val a = ArchivosPairAndroid(base)
            assertNull(a.leerReserva()); assertNull(a.leerRegistro())
            a.reservar(byteArrayOf(1, 2)); assertArrayEquals(byteArrayOf(1, 2), a.leerReserva())
            a.escribirRegistro(byteArrayOf(3, 4)); assertArrayEquals(byteArrayOf(3, 4), a.leerRegistro())
            assertThrows(Exception::class.java) { a.reservar(byteArrayOf(5)) }
            assertThrows(Exception::class.java) { a.escribirRegistro(byteArrayOf(6)) }
            val otra = File(dir, "interrumpido")
            check(File(otra.path + ".intento").createNewFile())
            assertThrows(Exception::class.java) { ArchivosPairAndroid(otra).leerReserva() }
            val parcial = File(dir, "parcial")
            val resto = File(parcial.path + ".new").apply { writeBytes(byteArrayOf(1)) }
            assertThrows(Exception::class.java) { ArchivosPairAndroid(parcial).leerRegistro() }
            assertTrue(resto.exists()) // Ni una lectura ni otro proceso deben perder la evidencia.
        } finally { dir.listFiles()?.forEach { check(it.delete()) }; check(dir.delete()) }
    }
}
