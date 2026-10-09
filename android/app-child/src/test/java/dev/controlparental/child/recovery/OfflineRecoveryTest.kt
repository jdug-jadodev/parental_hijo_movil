package dev.controlparental.child.recovery

import dev.controlparental.child.data.*
import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Identidades P-256 efímeras exclusivamente en memoria JVM; nunca privadas en archivo. */
class OfflineRecoveryTest {
    private class LaboratorioOffline {
        val padre = identidadEfimera()
        val hijo = identidadEfimera()
        val archivo = ArchivoSimulado()
        val almacen = ChildStateStore(archivo)
        var ahora = 100000L
        var desbloqueado = true
        var bootId = estadoConOrden().bootContext!!.bootId
        var nonceNumero = 0
        init {
            val base = estadoConOrden()
            almacen.actualizar { base.copy(vinculo = base.vinculo!!.copy(
                parentPublicKeyB64 = padre.clavePublica.base64, childPublicKeyB64 = hijo.clavePublica.base64),
                policy = null, lastCommand = null, bootContext = base.bootContext!!.copy(authorizedBootId = null)) }
        }
        fun controlador(identidad: FirmanteCp1? = hijo, nonce: (() -> ByteArray)? = null,
            reloj: (() -> Long)? = null, fuente: (() -> ContextoRecuperacion)? = null) = RecoveryController(
            almacen, { reloj?.invoke() ?: ahora }, { fuente?.invoke() ?: ContextoRecuperacion(desbloqueado, bootId) }, identidad,
            nonce ?: { ByteArray(32).also { it[0] = (++nonceNumero).toByte() } })
        fun estado() = (almacen.cargar() as CargaEstado.Disponible).estado
        fun desafio(c: RecoveryController, accion: AccionOffline = AccionOffline.MAINTENANCE): MensajeOffline {
            val respuesta = c.crearDesafioOffline(accion)
            assertEquals(ResultadoRecuperacion.DESAFIO_CREADO, respuesta.resultado)
            return CriptografiaCp1.verificar(respuesta.sobre!!, hijo.clavePublica, Rol.CHILD, Proposito.OFFLINE).mensaje as MensajeOffline
        }
        fun respuesta(d: MensajeOffline, firmante: FirmanteCp1 = padre): ByteArray = CodecCp1.serializar(
            CriptografiaCp1.firmar(d.copy(esRespuesta = true), Proposito.OFFLINE, Rol.PARENT, firmante))
    }

    @Test
    fun `desafio firmado por identidad vinculada no concede sesion y usa OFFLINE CHILD`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val d = l.desafio(c)
        assertFalse(d.esRespuesta)
        assertEquals(l.estado().vinculo!!.pairId, d.pairId)
        assertEquals(l.bootId, d.bootId)
        assertEquals(32, Base64Cp1.decodificar(d.nonce, 32).size)
        assertNull(c.accesoActual())
        assertTrue(l.estado().recoveryConsumed.isEmpty())
    }

    @Test
    fun `respuesta confiable habilita solo mantenimiento y no gasta codigo ni autoriza boot`() {
        val l = LaboratorioOffline()
        val anterior = l.estado()
        val c = l.controlador()
        val d = l.desafio(c)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.respuesta(d)).resultado)
        assertNotNull(c.mantenimientoActual())
        assertEquals(PropositoRecuperacionLocal.MAINTENANCE, c.accesoActual()!!.proposito)
        assertEquals(anterior, l.estado())
        assertNull(l.estado().bootContext!!.authorizedBootId)
    }

    @Test
    fun `espera pendiente de codigos no intercepta QR valido ni desaparece por aceptarlo`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        repeat(5) { c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, "invalido") }
        val antes = l.estado()
        val d = l.desafio(c)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.respuesta(d)).resultado)
        assertNotNull(c.mantenimientoActual())
        assertEquals(antes.recoveryFailureCount, l.estado().recoveryFailureCount)
        assertTrue(l.estado().esperaRecuperacionPendiente)
        assertEquals(ResultadoRecuperacion.ESPERA, c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, "invalido").resultado)
    }

    @Test
    fun `respuesta se consume una sola vez y replay no extiende sesion`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val bytes = l.respuesta(l.desafio(c))
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(bytes).resultado)
        l.ahora += 1000
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(bytes).resultado)
        assertEquals(299000L, c.accesoActual()!!.restanteMs)
    }

    @Test
    fun `RETIRE no concede mantenimiento ni ejecuta borrado y sesion no vuelve en otro proceso`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val d = l.desafio(c, AccionOffline.RETIRE)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.respuesta(d)).resultado)
        assertEquals(PropositoRecuperacionLocal.RETIRE, c.accesoActual()!!.proposito)
        assertNull(c.mantenimientoActual())
        assertTrue(l.estado().recoveryConsumed.isEmpty())
        assertEquals(EtapaAprovisionamiento.RETIRADA_PENDIENTE, l.estado().provisioningStage)
        assertNull(l.controlador().accesoActual())
    }

    @Test
    fun `clave ajena y rol hijo no autorizan pero dejan aceptar la respuesta correcta`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val d = l.desafio(c)
        assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, c.aceptarRespuestaOffline(l.respuesta(d, identidadEfimera())).resultado)
        val desafioComoRespuesta = CodecCp1.serializar(CriptografiaCp1.firmar(d, Proposito.OFFLINE, Rol.CHILD, l.hijo))
        assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, c.aceptarRespuestaOffline(desafioComoRespuesta).resultado)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.respuesta(d)).resultado)
    }

    @Test
    fun `pair boot accion o nonce firmados pero distintos se rechazan`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val d = l.desafio(c)
        val distintos = listOf(d.copy(pairId = "00000000-0000-4000-8000-000000000010"),
            d.copy(bootId = "00000000-0000-4000-8000-000000000010"), d.copy(action = AccionOffline.RETIRE),
            d.copy(nonce = Base64Cp1.codificar(ByteArray(32))))
        for (mensaje in distintos) assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, c.aceptarRespuestaOffline(l.respuesta(mensaje)).resultado)
        assertNull(c.accesoActual())
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.respuesta(d)).resultado)
    }

    @Test
    fun `desafio caduca exactamente a cinco minutos y respuesta antes del limite funciona`() {
        for (delta in listOf(299999L, 300000L)) {
            val l = LaboratorioOffline()
            val c = l.controlador()
            val bytes = l.respuesta(l.desafio(c))
            l.ahora += delta
            assertEquals(if (delta < 300000) ResultadoRecuperacion.AUTORIZADA else ResultadoRecuperacion.DESAFIO_VENCIDO,
                c.aceptarRespuestaOffline(bytes).resultado)
            if (delta >= 300000) assertNull(c.accesoActual())
        }
    }

    @Test
    fun `reloj retrocedido invalida desafio sin permiso`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val bytes = l.respuesta(l.desafio(c))
        l.ahora--
        assertEquals(ResultadoRecuperacion.DESAFIO_VENCIDO, c.aceptarRespuestaOffline(bytes).resultado)
        assertNull(c.accesoActual())
    }

    @Test
    fun `nuevo desafio reemplaza nonce anterior y no acepta su respuesta`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val primero = l.desafio(c)
        val segundo = l.desafio(c)
        assertNotEquals(primero.nonce, segundo.nonce)
        assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, c.aceptarRespuestaOffline(l.respuesta(primero)).resultado)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.respuesta(segundo)).resultado)
    }

    @Test
    fun `cancelar y recrear proceso pierden desafio y sesion sin tocar vinculo`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val d = l.desafio(c)
        val antes = l.estado()
        c.cancelar()
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(l.respuesta(d)).resultado)
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, l.controlador().aceptarRespuestaOffline(l.respuesta(d)).resultado)
        assertEquals(antes, l.estado())
    }

    @Test
    fun `Android bloqueado o boot diferente no permiten respuesta vigente`() {
        for (bloqueado in listOf(false, true)) {
            val l = LaboratorioOffline()
            val c = l.controlador()
            val bytes = l.respuesta(l.desafio(c))
            if (bloqueado) l.desbloqueado = false else l.bootId = "00000000-0000-4000-8000-000000000010"
            assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(bytes).resultado)
            assertNull(c.accesoActual())
        }
    }

    @Test
    fun `reemplazo de publica confiable invalida desafio anterior`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val bytes = l.respuesta(l.desafio(c))
        l.almacen.actualizar { it!!.copy(vinculo = it.vinculo!!.copy(parentPublicKeyB64 = identidadEfimera().clavePublica.base64)) }
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(bytes).resultado)
        assertNull(c.accesoActual())
    }

    @Test
    fun `fallo persistencia consume desafio y no emite sesion aunque firma sea valida`() {
        for (silencioso in listOf(false, true)) {
            val l = LaboratorioOffline()
            val c = l.controlador()
            val bytes = l.respuesta(l.desafio(c, AccionOffline.RETIRE))
            if (silencioso) l.archivo.omiteCommit = true else l.archivo.falloEscritura = true
            assertEquals(ResultadoRecuperacion.ERROR_SEGURO, c.aceptarRespuestaOffline(bytes).resultado)
            assertNull(c.accesoActual())
            assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(bytes).resultado)
            assertEquals(EtapaAprovisionamiento.VINCULADO, l.estado().provisioningStage)
        }
    }

    @Test
    fun `identidad ausente equivocada o que falla no fabrica desafio`() {
        val l = LaboratorioOffline()
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, l.controlador(null).crearDesafioOffline(AccionOffline.MAINTENANCE).resultado)
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.controlador(identidadEfimera()).crearDesafioOffline(AccionOffline.MAINTENANCE).resultado)
        val fallido = object : FirmanteCp1 {
            override val clavePublica = l.hijo.clavePublica
            override fun firmarBytes(bytes: ByteArray): ByteArray = throw java.security.GeneralSecurityException("Identidad inaccesible de prueba")
        }
        val respuesta = l.controlador(fallido).crearDesafioOffline(AccionOffline.RETIRE)
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, respuesta.resultado)
        assertNull(respuesta.sobre)
    }

    @Test
    fun `nonce mal dimensionado o repetido no se firma ni conserva desafio previo`() {
        val l = LaboratorioOffline()
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO,
            l.controlador(nonce = { ByteArray(31) }).crearDesafioOffline(AccionOffline.MAINTENANCE).resultado)
        val c = l.controlador(nonce = { ByteArray(32) })
        val anterior = l.desafio(c)
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, c.crearDesafioOffline(AccionOffline.MAINTENANCE).resultado)
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(l.respuesta(anterior)).resultado)
    }

    @Test
    fun `entrada excesiva vacia o corrupta no cuenta intentos impresos ni autoriza`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val d = l.desafio(c)
        for (bytes in listOf(ByteArray(0), ByteArray(JsonCp1.MAX_FRAME_BYTES + 1), "corrupto".toByteArray())) {
            assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, c.aceptarRespuestaOffline(bytes).resultado)
        }
        assertEquals(0, l.estado().recoveryFailureCount)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.respuesta(d)).resultado)
    }

    @Test
    fun `sesion QR vence y nueva instancia no la reconstruye`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        c.aceptarRespuestaOffline(l.respuesta(l.desafio(c)))
        assertNull(l.controlador().accesoActual())
        l.ahora += 299999
        assertEquals(1L, c.accesoActual()!!.restanteMs)
        l.ahora++
        assertNull(c.accesoActual())
    }

    @Test
    fun `doble respuesta concurrente no acepta dos veces ni prolonga ventana`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val bytes = l.respuesta(l.desafio(c))
        val ejecutor = Executors.newFixedThreadPool(2)
        try {
            val resultados = List(2) { ejecutor.submit<ResultadoRecuperacion> { c.aceptarRespuestaOffline(bytes).resultado } }
                .map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1, resultados.count { it == ResultadoRecuperacion.AUTORIZADA })
            assertEquals(1, resultados.count { it == ResultadoRecuperacion.NO_DISPONIBLE })
        } finally { ejecutor.shutdownNow() }
    }

    @Test
    fun `caducidad o bloqueo durante verificacion no conceden sesion`() {
        for (caducidad in listOf(false, true)) {
            val l = LaboratorioOffline()
            var verificando = false
            var relojes = 0
            var fuentes = 0
            val c = l.controlador(reloj = {
                if (verificando && relojes++ > 0 && caducidad) l.ahora + 300000 else l.ahora
            }, fuente = {
                ContextoRecuperacion(!(verificando && fuentes++ > 0 && !caducidad), l.bootId)
            })
            val bytes = l.respuesta(l.desafio(c))
            verificando = true
            assertEquals(if (caducidad) ResultadoRecuperacion.DESAFIO_VENCIDO else ResultadoRecuperacion.NO_DISPONIBLE,
                c.aceptarRespuestaOffline(bytes).resultado)
            assertNull(c.accesoActual())
        }
    }

    @Test
    fun `bloqueo durante firma del desafio no emite sobre utilizable`() {
        val l = LaboratorioOffline()
        val firmante = object : FirmanteCp1 {
            override val clavePublica = l.hijo.clavePublica
            override fun firmarBytes(bytes: ByteArray): ByteArray {
                val firma = l.hijo.firmarBytes(bytes)
                l.desbloqueado = false
                return firma
            }
        }
        val c = l.controlador(firmante)
        val respuesta = c.crearDesafioOffline(AccionOffline.MAINTENANCE)
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, respuesta.resultado)
        assertNull(respuesta.sobre)
        assertNull(c.accesoActual())
    }

    @Test
    fun `antes de desbloqueo no firma ni requiere Keystore`() {
        val l = LaboratorioOffline()
        l.desbloqueado = false
        var usado = false
        val firmante = object : FirmanteCp1 {
            override val clavePublica: ClavePublicaCp1 get() { usado = true; return l.hijo.clavePublica }
            override fun firmarBytes(bytes: ByteArray): ByteArray { usado = true; return l.hijo.firmarBytes(bytes) }
        }
        val respuesta = l.controlador(firmante).crearDesafioOffline(AccionOffline.RETIRE)
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, respuesta.resultado)
        assertFalse(usado)
        assertNull(respuesta.sobre)
    }

    @Test
    fun `corrupcion entre desafio y respuesta preserva archivo y no habilita recuperacion`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val bytes = l.respuesta(l.desafio(c))
        l.archivo.bytes = "corrupto".toByteArray()
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, c.aceptarRespuestaOffline(bytes).resultado)
        assertNull(c.accesoActual())
        assertArrayEquals("corrupto".toByteArray(), l.archivo.bytes)
    }

    @Test
    fun `desafio no sobrevive cambio del arranque duradero aunque fuente siga anunciando el viejo`() {
        val l = LaboratorioOffline()
        val c = l.controlador()
        val bytes = l.respuesta(l.desafio(c))
        l.almacen.actualizar { it!!.copy(bootContext = ContextoArranque(13, "00000000-0000-4000-8000-000000000010", null, 0)) }
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(bytes).resultado)
        assertNull(c.accesoActual())
    }

    companion object {
        private fun identidadEfimera(): FirmanteCp1 {
            val par = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
            return object : FirmanteCp1 {
                override val clavePublica = ClavePublicaCp1.leer(Base64Cp1.codificar(par.public.encoded))
                override fun firmarBytes(bytes: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
                    initSign(par.private); update(bytes); sign()
                }
            }
        }
    }
}
