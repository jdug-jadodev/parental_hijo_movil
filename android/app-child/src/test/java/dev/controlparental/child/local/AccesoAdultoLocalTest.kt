package dev.controlparental.child.local

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AccesoAdultoLocalTest {
    private var tiempo = 10L
    private var boot = 1
    private class Memoria : AlmacenAdulto {
        var bytes: ByteArray? = null
        var error = false
        override fun leer() = if (error) throw IllegalStateException() else bytes?.let(CodecAdulto::decodificar)
        override fun guardar(registro: RegistroAdulto) {
            if (error) throw IllegalStateException()
            bytes = CodecAdulto.codificar(registro)
        }
    }
    // Fake determinista solo JVM, no esquema criptográfico de producto.
    private val fake = object : DerivadorAdulto {
        override fun crear(texto: String) = VerificadorAdulto(ByteArray(16), texto.toByteArray().copyOf(32))
        override fun coincide(texto: String, verificador: VerificadorAdulto) = crear(texto).hash.contentEquals(verificador.hash)
    }
    private val memoria = Memoria()
    private fun motor(kdf: DerivadorAdulto = fake) = AccesoAdultoLocal(memoria, kdf, { tiempo }, { boot })
    private fun alta(m: AccesoAdultoLocal = motor()) = m.configurar("00123", "00123", "Pregunta ficticia JVM", "Respuesta sintética", "Respuesta sintética") { true }
    private fun entrar(m: AccesoAdultoLocal = motor(), codigo: String = "00123") = m.verificar(codigo, false) { true }

    @Test fun `alta exige bootstrap vigente y no reemplaza configurado`() {
        val m = motor()
        assertEquals(TipoAdulto.SIN_CONFIGURAR, m.resumen().tipo)
        assertEquals(TipoAdulto.RECHAZADO, m.configurar("00123", "00123", "Pregunta", "Respuesta", "Respuesta") { false }.tipo)
        assertEquals(TipoAdulto.GUARDADO, alta(m).tipo)
        assertEquals(TipoAdulto.RECHAZADO, alta(m).tipo)
    }
    @Test fun `codigo ASCII cinco a diez conserva ceros y nunca trim`() {
        for (s in listOf("00123", "0123456789")) assertTrue(ReglasAdulto.codigo(s))
        for (s in listOf("1234", "12345678901", " 00123", "00123 ", "１２３４５", "12a45", "")) assertFalse(ReglasAdulto.codigo(s))
        alta(); assertEquals(TipoAdulto.RECHAZADO, entrar(codigo = "123").tipo)
        assertEquals(TipoAdulto.AUTENTICADO, entrar().tipo)
    }
    @Test fun `pregunta respuesta exactas no equivalencias silenciosas ni vacios`() {
        for (s in listOf("", "   ", "a\nb", "a\u0000b", "\uD800")) assertFalse(ReglasAdulto.texto(s, 256))
        alta(); val m = motor()
        assertEquals(TipoAdulto.RECHAZADO, m.verificar("respuesta sintética", true) { true }.tipo)
        assertEquals(TipoAdulto.RESPUESTA_VALIDA, m.verificar("Respuesta sintética", true) { true }.tipo)
    }
    @Test fun `recuperacion solo cambia codigo conserva pregunta cupo y no abre sesion`() {
        alta(); val m = motor(); entrar(m, "99999")
        val r = m.verificar("Respuesta sintética", true) { true }
        val fallos = memoria.leer()!!.fallos
        assertEquals(TipoAdulto.RESPUESTA_VALIDA, r.tipo)
        assertEquals(TipoAdulto.GUARDADO, m.cambiarCodigo("000456", "000456", r.revision) { true }.tipo)
        assertEquals(fallos, memoria.leer()!!.fallos)
        assertEquals("Pregunta ficticia JVM", m.resumen().pregunta)
        assertEquals(TipoAdulto.RECHAZADO, entrar(m).tipo)
        assertEquals(TipoAdulto.AUTENTICADO, entrar(m, "000456").tipo)
    }
    @Test fun `espera agregada persiste por reentrada proceso boot y reloj regresivo`() {
        alta(); val m = motor()
        repeat(5) { m.verificar("incorrecta", it % 2 == 0) { true } }
        assertEquals(60_000L, motor().resumen().esperaMs)
        tiempo += 30_000; assertEquals(30_000L, motor().resumen().esperaMs)
        boot++; assertEquals(60_000L, motor().resumen().esperaMs)
        tiempo -= 100; assertEquals(60_000L, motor().resumen().esperaMs)
        assertEquals(TipoAdulto.ESPERA, entrar().tipo)
        tiempo += 60_000; assertEquals(TipoAdulto.AUTENTICADO, entrar().tipo)
        assertEquals(0, memoria.leer()!!.fallos)
    }
    @Test fun `diez fallos saturan y alternar formularios no limpia cupo`() {
        alta()
        repeat(10) { motor().verificar("incorrecta", it % 2 == 0) { true }; tiempo += 60_001 }
        assertEquals(10, memoria.leer()!!.fallos)
        assertTrue(motor().resumen().esperaMs > 0)
    }
    @Test fun `cancelacion durante KDF conserva reserva sin conceder resultado`() {
        alta(); var vigente = true
        val derivador = object : DerivadorAdulto by fake {
            override fun coincide(texto: String, verificador: VerificadorAdulto): Boolean { vigente = false; return true }
        }
        assertEquals(TipoAdulto.RECHAZADO, motor(derivador).verificar("00123", false) { vigente }.tipo)
        assertEquals(1, memoria.leer()!!.fallos)
    }
    @Test fun `revision vieja o callback obsoleto no cambian codigo`() {
        alta(); val m = motor()
        assertEquals(TipoAdulto.RECHAZADO, m.cambiarCodigo("99999", "99999", 2) { true }.tipo)
        assertEquals(TipoAdulto.RECHAZADO, m.cambiarCodigo("99999", "99999", 1) { false }.tipo)
        assertEquals(TipoAdulto.AUTENTICADO, entrar(m).tipo)
    }
    @Test fun `perdida corrupcion version no soportada y fallo IO cierran acceso`() {
        alta(); memoria.error = true
        assertEquals(TipoAdulto.ERROR, motor().resumen().tipo)
        assertEquals(TipoAdulto.ERROR, entrar().tipo)
        memoria.error = false; memoria.bytes!![3] = 2
        assertEquals(TipoAdulto.ERROR, motor().resumen().tipo)
        assertEquals(TipoAdulto.ERROR, alta().tipo)
    }
    @Test fun `codec version uno ida vuelta truncado sobrante y limites`() {
        alta(); val original = memoria.bytes!!
        assertArrayEquals(original, CodecAdulto.codificar(CodecAdulto.decodificar(original)))
        for (bytes in listOf(original.copyOf(8), original + 0, ByteArray(4097))) {
            try { CodecAdulto.decodificar(bytes); fail("Debía rechazar formato") } catch (_: Exception) { }
        }
    }
    @Test fun `escritor serializa dos KDF concurrentes y reservas`() {
        alta(); val entrada = CountDownLatch(1); val salida = CountDownLatch(1)
        val dentro = AtomicInteger(); val maximo = AtomicInteger()
        val derivador = object : DerivadorAdulto by fake {
            override fun coincide(texto: String, verificador: VerificadorAdulto): Boolean {
                val n = dentro.incrementAndGet(); maximo.accumulateAndGet(n, ::maxOf)
                entrada.countDown(); check(salida.await(5, TimeUnit.SECONDS)); dentro.decrementAndGet(); return false
            }
        }
        val m = motor(derivador)
        val a = Thread { entrar(m) }; val b = Thread { entrar(m) }
        a.start(); assertTrue(entrada.await(5, TimeUnit.SECONDS)); b.start(); salida.countDown()
        a.join(5000); b.join(5000); assertFalse(a.isAlive || b.isAlive)
        assertEquals(1, maximo.get()); assertEquals(2, memoria.leer()!!.fallos)
    }
    @Test fun `PBKDF real salts diferentes verificacion y separacion respuesta`() {
        val kdf = PbkdfAdulto(); val a = kdf.crear("00123"); val b = kdf.crear("00123")
        assertFalse(a.salt.contentEquals(b.salt)); assertFalse(a.hash.contentEquals(b.hash))
        assertTrue(kdf.coincide("00123", a)); assertFalse(kdf.coincide("00124", a))
    }
    @Test fun `cancelar alta durante derivacion no deja registro ni concede bootstrap`() {
        var vigente = true
        val d = object : DerivadorAdulto by fake {
            override fun crear(texto: String): VerificadorAdulto { vigente = false; return fake.crear(texto) }
        }
        assertEquals(TipoAdulto.RECHAZADO, motor(d).configurar("00123", "00123", "Pregunta", "Respuesta", "Respuesta") { vigente }.tipo)
        assertNull(memoria.bytes)
    }
    @Test fun `fallo de reserva o readback nunca devuelve autoridad`() {
        alta(); val fallido = object : AlmacenAdulto {
            override fun leer() = memoria.leer()
            override fun guardar(registro: RegistroAdulto) { memoria.guardar(registro); throw IllegalStateException() }
        }
        val m = AccesoAdultoLocal(fallido, fake, { tiempo }, { boot })
        assertEquals(TipoAdulto.ERROR, m.verificar("00123", false) { true }.tipo)
        assertEquals(1, memoria.leer()!!.fallos)
    }
}
