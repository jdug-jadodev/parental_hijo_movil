package dev.controlparental.child.boot

import dev.controlparental.child.data.*
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class FormatoTrazaArranqueTest {
    private val locked = "android.intent.action.LOCKED_BOOT_COMPLETED"
    private val unlocked = "android.intent.action.BOOT_COMPLETED"
    private val bootId = "00000000-0000-4000-8000-000000000010"

    private fun carga(contador: Int? = 3) = CargaEstado.Disponible(EstadoControl(
        bootContext = ContextoArranque(contador, bootId, null, 1000)))

    @Test
    fun `recepcion y persistencia antes del PIN se distinguen del callback desbloqueado`() {
        val textoLocked = FormatoTrazaArranque.crear(locked, false, false, carga())!!
        val textoUnlocked = FormatoTrazaArranque.crear(unlocked, true, true, carga())!!
        assertTrue(textoLocked.contains("evento=LOCKED_BOOT_COMPLETED desbloqueadoAlRecibir=NO desbloqueadoAlTerminar=NO"))
        assertTrue(textoUnlocked.contains("evento=BOOT_COMPLETED desbloqueadoAlRecibir=SI desbloqueadoAlTerminar=SI"))
        assertEquals(textoLocked.substringAfter("bootHash="), textoUnlocked.substringAfter("bootHash="))
    }

    @Test
    fun `desbloqueo ocurrido durante IO no se presenta como persistencia antes del PIN`() {
        val texto = FormatoTrazaArranque.crear(locked, false, true, carga())!!
        assertTrue(texto.contains("desbloqueadoAlRecibir=NO desbloqueadoAlTerminar=SI"))
    }

    @Test
    fun `hash es SHA256 del contexto y no expone el UUID ni payloads`() {
        val texto = FormatoTrazaArranque.crear(locked, false, false, carga())!!
        val esperado = MessageDigest.getInstance("SHA-256").digest(bootId.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertTrue(texto.endsWith("bootCount=3 bootHash=$esperado"))
        assertFalse(texto.contains(bootId))
        assertFalse(texto.contains("APPLIED"))
    }

    @Test
    fun `datos adicionales del snapshot no se proyectan a la traza`() {
        val inicial = carga()
        val distinto = CargaEstado.Disponible(inicial.estado.copy(approvedPackages = listOf("no.registrar.paquete"),
            recoveryFailureCount = 42, esperaRecuperacionPendiente = true,
            recoveryConsumed = setOf(PropositoRecuperacionLocal.RETIRE)))
        assertEquals(FormatoTrazaArranque.crear(locked, false, false, inicial),
            FormatoTrazaArranque.crear(locked, false, false, distinto))
    }

    @Test
    fun `ausencia y error no exponen hash ni confirman almacenamiento`() {
        for (carga in listOf(CargaEstado.Ausente, CargaEstado.ErrorSeguro, CargaEstado.Disponible(EstadoControl()))) {
            val texto = FormatoTrazaArranque.crear(locked, null, null, carga)!!
            assertTrue(texto.endsWith("resultado=ERROR_SEGURO"))
            assertTrue(texto.contains("desbloqueadoAlRecibir=DESCONOCIDO"))
            assertFalse(texto.contains("bootHash"))
        }
    }

    @Test
    fun `contador no disponible no se inventa y accion desconocida no se registra`() {
        assertTrue(FormatoTrazaArranque.crear(locked, false, false, carga(null))!!.contains("bootCount=DESCONOCIDO"))
        assertNull(FormatoTrazaArranque.crear("accion arbitraria secreta", false, false, carga()))
        assertNull(FormatoTrazaArranque.crear(null, false, false, carga()))
        assertTrue(FormatoTrazaArranque.crear("android.intent.action.MY_PACKAGE_REPLACED", true, true, carga())!!
            .startsWith("evento=MY_PACKAGE_REPLACED"))
    }

    @Test
    fun `bootId no UUID y contador negativo no se publican ni confirman`() {
        val inicial = carga()
        for (boot in listOf(inicial.estado.bootContext!!.copy(bootId = "texto sensible"),
                inicial.estado.bootContext.copy(systemBootCount = -1))) {
            val texto = FormatoTrazaArranque.crear(locked, false, false, CargaEstado.Disponible(inicial.estado.copy(bootContext = boot)))!!
            assertTrue(texto.endsWith("resultado=ERROR_SEGURO"))
            assertFalse(texto.contains("texto sensible"))
        }
    }
}
