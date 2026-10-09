package dev.controlparental.child.recovery

import dev.controlparental.child.data.PropositoRecuperacionLocal
import dev.controlparental.protocol.Base64Cp1
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class CodigoRecuperacionTest {
    // Datos ficticios de tests; nunca códigos de producción ni un bypass del APK.
    private val codigo = "ABCDEFGHIJKLMNOPQRSTUVWX234567AB"
    private val par = "00000000-0000-4000-8000-000000000010"

    @Test
    fun `normaliza solo minusculas ASCII guiones y espacios`() {
        assertEquals(codigo, CodigoRecuperacion.normalizar(codigo.lowercase().chunked(4).joinToString(" - ")))
        assertEquals(codigo, CodigoRecuperacion.normalizar(" $codigo "))
    }

    @Test
    fun `rechaza sustituciones y espacios Unicode digitos ambiguos y longitudes incorrectas`() {
        listOf("A".repeat(31), "A".repeat(33), "1".repeat(32), "0".repeat(32), "8".repeat(32),
            "9".repeat(32), "Á".repeat(32), "\t$codigo", "$codigo\n", "$codigo\u00a0", "-".repeat(200) + codigo)
            .forEach { assertNull(CodigoRecuperacion.normalizar(it)) }
    }

    @Test
    fun `hash usa bytes exactos del prefijo pareja y proposito CP1`() {
        val esperado = Base64Cp1.codificar(MessageDigest.getInstance("SHA-256")
            .digest("CPv1/RECOVERY\n$par\nMAINTENANCE\n$codigo".toByteArray(Charsets.UTF_8)))
        assertEquals(esperado, CodigoRecuperacion.hash(par, PropositoRecuperacionLocal.MAINTENANCE, codigo))
        assertTrue(CodigoRecuperacion.coincide(par, PropositoRecuperacionLocal.MAINTENANCE, codigo.lowercase(), esperado))
    }

    @Test
    fun `codigo no funciona con otra pareja proposito o valor`() {
        val hash = CodigoRecuperacion.hash(par, PropositoRecuperacionLocal.MAINTENANCE, codigo)
        assertFalse(CodigoRecuperacion.coincide(par, PropositoRecuperacionLocal.RETIRE, codigo, hash))
        assertFalse(CodigoRecuperacion.coincide("00000000-0000-4000-8000-000000000011", PropositoRecuperacionLocal.MAINTENANCE, codigo, hash))
        assertFalse(CodigoRecuperacion.coincide(par, PropositoRecuperacionLocal.MAINTENANCE, "B".repeat(32), hash))
    }

    @Test
    fun `hash confiable invalido falla de forma explicita`() {
        assertThrows(RuntimeException::class.java) { CodigoRecuperacion.coincide(par, PropositoRecuperacionLocal.MAINTENANCE, codigo, "no es base64") }
        assertThrows(IllegalArgumentException::class.java) { CodigoRecuperacion.hash(par, PropositoRecuperacionLocal.MAINTENANCE, codigo.lowercase()) }
    }
}
