package dev.controlparental.child.boot

import dev.controlparental.child.data.*
import dev.controlparental.child.domain.*
import dev.controlparental.protocol.CodigoCp1
import org.junit.Assert.*
import org.junit.Test

class BootContextRepositoryTest {
    private val bootNuevo = "00000000-0000-4000-8000-000000000010"
    private val bootOtro = "00000000-0000-4000-8000-000000000011"

    @Test
    fun `primer contexto no autoriza uso ni crea vinculo`() {
        val store = ChildStateStore(ArchivoSimulado())
        val carga = BootContextRepository(store, { ObservacionArranque(12, 0) }, { bootNuevo }).actualizarContexto()
        val estado = (carga as CargaEstado.Disponible).estado
        assertEquals(bootNuevo, estado.bootContext!!.bootId)
        assertNull(estado.bootContext.authorizedBootId)
        assertNull(estado.vinculo)
        assertEquals(0L, estado.lastAcceptedSeq)
        assertEquals(EtapaAprovisionamiento.PREPARACION, estado.provisioningStage)
    }

    @Test
    fun `dos callbacks y recreacion de repositorio conservan el boot e inicio originales`() {
        val store = ChildStateStore(ArchivoSimulado())
        val original = estadoConOrden()
        store.actualizar { original }
        listOf(100000L, 120000L, 160000L).forEach { ahora ->
            val estado = (BootContextRepository(store, { ObservacionArranque(12, ahora) },
                { error("No generar otro boot para callback repetido") }).actualizarContexto() as CargaEstado.Disponible).estado
            assertEquals(original.bootContext!!.bootId, estado.bootContext!!.bootId)
            assertEquals(original.bootContext.authorizedBootId, estado.bootContext.authorizedBootId)
            assertEquals(original.policy, estado.policy)
            assertEquals(original.lastCommand, estado.lastCommand)
            assertEquals(ahora, estado.bootContext.ultimoElapsedObservado)
        }
    }

    @Test
    fun `nuevo contador retira autorizacion sin perder ultima orden ni consumo de recuperacion`() {
        val store = ChildStateStore(ArchivoSimulado())
        val original = estadoConOrden().copy(recoveryConsumed = setOf(PropositoRecuperacionLocal.RETIRE),
            recoveryFailureCount = 4, esperaRecuperacionPendiente = true)
        store.actualizar { original }
        val estado = (BootContextRepository(store, { ObservacionArranque(13, 0) }, { bootNuevo })
            .actualizarContexto() as CargaEstado.Disponible).estado
        assertEquals(bootNuevo, estado.bootContext!!.bootId)
        assertNull(estado.bootContext.authorizedBootId)
        assertEquals(original.lastCommand, estado.lastCommand)
        assertEquals(original.policy, estado.policy)
        assertEquals(original.recoveryConsumed, estado.recoveryConsumed)
        assertEquals(4, estado.recoveryFailureCount)
        assertTrue(estado.esperaRecuperacionPendiente)
        val decision = PolicyEngine { 0 }.evaluar(EntradaEvaluacion(true, true, true, true, true, true,
            estado.bootContext.bootId, estado.bootContext.authorizedBootId, estado.policy,
            red = RedObservada(true), canal = CanalObservado(true, 0)))
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, decision.motivo)
    }

    @Test
    fun `contador igual con retroceso monotono no acredita continuidad`() {
        val store = ChildStateStore(ArchivoSimulado())
        store.actualizar { estadoConOrden() }
        val estado = (BootContextRepository(store, { ObservacionArranque(12, 99999) }, { bootNuevo })
            .actualizarContexto() as CargaEstado.Disponible).estado
        assertEquals(bootNuevo, estado.bootContext!!.bootId)
        assertNull(estado.bootContext.authorizedBootId)
    }

    @Test
    fun `contador ausente negativo o anterior crea contexto cerrado sin restablecer secuencia`() {
        listOf(null, -1, 11).forEach { contador ->
            val store = ChildStateStore(ArchivoSimulado())
            val original = estadoConOrden()
            store.actualizar { original }
            val estado = (BootContextRepository(store, { ObservacionArranque(contador, 120000) }, { bootNuevo })
                .actualizarContexto() as CargaEstado.Disponible).estado
            assertEquals(bootNuevo, estado.bootContext!!.bootId)
            assertNull(estado.bootContext.authorizedBootId)
            assertEquals(original.lastAcceptedSeq, estado.lastAcceptedSeq)
        }
    }

    @Test
    fun `sin contador fiable callbacks sucesivos no se presumen del mismo arranque`() {
        val store = ChildStateStore(ArchivoSimulado())
        store.actualizar { estadoConOrden() }
        BootContextRepository(store, { ObservacionArranque(null, 120000) }, { bootNuevo }).actualizarContexto()
        val estado = (BootContextRepository(store, { ObservacionArranque(null, 130000) }, { bootOtro })
            .actualizarContexto() as CargaEstado.Disponible).estado
        assertEquals(bootOtro, estado.bootContext!!.bootId)
        assertNull(estado.bootContext.authorizedBootId)
    }

    @Test
    fun `corrupcion no observa reloj ni genera identidad o reemplaza el archivo`() {
        val archivo = ArchivoSimulado().apply { bytes = "corrupto".toByteArray() }
        var observaciones = 0
        var identidades = 0
        val repo = BootContextRepository(ChildStateStore(archivo), {
            observaciones++; ObservacionArranque(13, 0)
        }, { identidades++; bootNuevo })
        assertEquals(CargaEstado.ErrorSeguro, repo.actualizarContexto())
        assertEquals(0, observaciones)
        assertEquals(0, identidades)
        assertArrayEquals("corrupto".toByteArray(), archivo.bytes)
        assertEquals(0, archivo.escrituras)
    }

    @Test
    fun `reloj negativo UUID invalido o identidad repetida no reemplazan estado previo`() {
        val store = ChildStateStore(ArchivoSimulado())
        val original = estadoConOrden()
        store.actualizar { original }
        assertEquals(CargaEstado.ErrorSeguro, BootContextRepository(store, { ObservacionArranque(13, -1) }, { bootNuevo }).actualizarContexto())
        listOf("invalido", original.bootContext!!.bootId).forEach { id ->
            assertEquals(CargaEstado.ErrorSeguro, BootContextRepository(store, { ObservacionArranque(13, 0) }, { id }).actualizarContexto())
        }
        assertEquals(CargaEstado.Disponible(original), store.cargar())
    }

    @Test
    fun `proceso recreado en mismo boot conserva tiempo pero no inventa canal sano`() {
        val store = ChildStateStore(ArchivoSimulado())
        val original = estadoConOrden()
        store.actualizar { original }
        val estado = (BootContextRepository(store, { ObservacionArranque(12, 120000) }, { bootNuevo })
            .actualizarContexto() as CargaEstado.Disponible).estado
        val entrada = EntradaEvaluacion(true, true, true, true, true, true, estado.bootContext!!.bootId,
            estado.bootContext.authorizedBootId, estado.policy, red = RedObservada(true))
        assertEquals(CodigoCp1.CHANNEL_EXPIRED, PolicyEngine { 120000 }.evaluar(entrada).motivo)
        assertEquals(original.policy, estado.policy)
    }
}
