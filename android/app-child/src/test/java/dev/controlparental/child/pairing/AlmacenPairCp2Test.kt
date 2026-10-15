package dev.controlparental.child.pairing

import dev.controlparental.child.local.*
import dev.controlparental.protocol.*
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID

class AlmacenPairCp2Test {
    private class Memoria : ArchivosPairCp2 {
        var reserva: ByteArray? = null; var registro: ByteArray? = null
        var fallo = ""; var despuesReserva: () -> Unit = {}; var despuesRegistro: () -> Unit = {}
        override fun leerReserva() = reserva?.copyOf()
        override fun leerRegistro() = registro?.copyOf()
        override fun reservar(bytes: ByteArray) {
            if (fallo == "ANTES_RESERVA") error("IO")
            reserva = bytes.copyOf(); despuesReserva()
            if (fallo == "TRAS_RESERVA") error("Crash simulado")
        }
        override fun escribirRegistro(bytes: ByteArray) {
            if (fallo == "ANTES_REGISTRO") error("Crash simulado")
            registro = bytes.copyOf(); despuesRegistro()
            if (fallo == "TRAS_REGISTRO") error("Crash simulado")
            if (fallo == "READBACK") registro = byteArrayOf(0)
        }
    }
    private inner class Laboratorio {
        var ahora = 10L; val sesion = SesionAdulto({ ahora }, { 5 })
        val padre = clave(); val hijo = publica(clave())
        val archivos = Memoria(); val almacen = AlmacenPairCp2(archivos)
        val marco: MarcoSesionAdulto
        val ofertas: OfertasRam
        val ref: ReferenciaOferta
        val caso: ValidacionPairRam
        val v: ValidacionPair
        init {
            sesion.reanudar(); val e = sesion.solicitar(FinAdulto.ENTRAR)!!
            sesion.completar(e, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9))
            val g = ticket(FinAdulto.GENERAR_OFERTA); marco = sesion.marcoAcreditado(g)!!
            ofertas = OfertasRam({ ahora }, { ContextoOferta(5, hijo) }, { sesion.accionVigente(g) }, vidaSesion = { sesion.marcoVigente(marco) })
            ref = ofertas.generar(); caso = ValidacionPairRam(ofertas, sesion)
            val datos = DatosConfirmadosPair(publica(padre), CodecPairCp2.huellaPublica(publica(padre)), CodecPairCp2.huellaPublica(hijo), "https://relay.invalid")
            val c = caso.confirmar(ref, ticket(FinAdulto.VALIDAR_PAIR), datos)!!
            val h = ref.oferta.enrollNonce
            val a = AceptacionPairCp2(UUID.randomUUID().toString(), ref.oferta.enrollId, h, publica(padre), hijo, datos.origen, HashesPairCp2(h, h, h))
            val p = CodecPairCp2.escribirPayload(a)
            val firma = Signature.getInstance("SHA256withECDSA").run { initSign(padre.private); update("CPv2/PAIR\n".toByteArray()); update(p); sign() }
            v = caso.validar(c, CodecPairCp2.escribirSobre(SobrePairCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(firma))))!!
        }
        fun ticket(fin: FinAdulto): TicketAdulto = sesion.solicitar(fin)!!.also {
            check(sesion.autorizarAccion(it, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
        }
        fun consentimiento() = ConsentimientoPair(v.consentimiento, 5, ref.generacion, 9, ahora)
        fun guardar(): ResultadoGuardarPair {
            val t = ticket(FinAdulto.GUARDAR_PAIR)
            return almacen.guardar(v, consentimiento()) { sesion.accionVigente(t) && caso.resultadoActual() === v }
        }
    }
    private fun clave() = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
    private fun publica(k: java.security.KeyPair) = Base64Cp1.codificar(k.public.encoded)
    @Test fun puenteFreshNoRenuevaSesionNiOfertaYGuardaConReadback() {
        val l = Laboratorio(); assertTrue(l.ofertas.vigente(l.ref)); assertSame(l.v, l.caso.resultadoActual())
        assertEquals(ResultadoGuardarPair.GUARDADO, l.guardar())
        val s = (l.almacen.cargar() as CargaPairCp2.Guardado).snapshot
        assertEquals(l.v.aceptacion.pairId, s.pairId); assertEquals(l.v.aceptacion.recoveryHashes, s.recoveryHashes)
        assertEquals(CodecRegistroPair.sha(Base64Cp1.decodificar(l.v.aceptacion.enrollNonce, 32)), s.nonceConsumidoSha256)
        assertArrayEquals(l.v.bytesFrame(), s.integridad.bytesFrame()); assertEquals(l.v.payloadSha256B64, s.integridad.payloadSha256B64)
        assertEquals("VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION", s.estadoLocal)
        l.ahora += 120000; assertFalse(l.ofertas.vigente(l.ref)); assertNull(l.caso.resultadoActual())
    }
    @Test fun replayYReemplazoRechazadosTrasRestore() {
        val l = Laboratorio(); assertEquals(ResultadoGuardarPair.GUARDADO, l.guardar())
        val restaurado = AlmacenPairCp2(l.archivos)
        assertEquals(ResultadoGuardarPair.RECHAZADO, restaurado.guardar(l.v, l.consentimiento()) { true })
        assertTrue(restaurado.cargar(publica(clave())) is CargaPairCp2.Incierta)
    }
    @Test fun crashReservaNoReabreAltaYRegistroCompletoSeRestauraSinOferta() {
        for (fase in listOf("TRAS_RESERVA", "ANTES_REGISTRO", "TRAS_REGISTRO", "READBACK")) {
            val l = Laboratorio(); l.archivos.fallo = fase
            assertEquals(ResultadoGuardarPair.INCIERTO, l.guardar())
            val restaurado = AlmacenPairCp2(l.archivos)
            if (fase == "TRAS_REGISTRO") assertTrue(restaurado.cargar() is CargaPairCp2.Guardado)
            else assertTrue(restaurado.cargar() is CargaPairCp2.Incierta)
            assertEquals(ResultadoGuardarPair.RECHAZADO, restaurado.guardar(l.v, l.consentimiento()) { true })
        }
    }
    @Test fun cancelarAntesODespuesDeReservaNoPublicaNiFabricaVirginidad() {
        val l = Laboratorio()
        assertEquals(ResultadoGuardarPair.RECHAZADO, l.almacen.guardar(l.v, l.consentimiento()) { false })
        assertTrue(l.almacen.cargar() is CargaPairCp2.Ausente)
        l.archivos.despuesReserva = { l.sesion.cerrar() }
        assertEquals(ResultadoGuardarPair.INCIERTO, l.guardar()); assertTrue(l.almacen.cargar() is CargaPairCp2.Incierta)
    }
    @Test fun cancelacionDuranteEscrituraConservaHechoDurableSinAutoridad() {
        val l = Laboratorio(); l.archivos.despuesRegistro = { l.sesion.cerrar() }
        assertEquals(ResultadoGuardarPair.GUARDADO_SIN_SESION, l.guardar()); assertTrue(l.almacen.cargar() is CargaPairCp2.Guardado)
    }
    @Test fun reentranciaNoProduceSegundaTransaccion() {
        val l = Laboratorio(); l.archivos.despuesReserva = {
            assertTrue(l.almacen.cargar() is CargaPairCp2.Incierta)
            assertEquals(ResultadoGuardarPair.RECHAZADO, l.almacen.guardar(l.v, l.consentimiento()) { true })
        }
        assertEquals(ResultadoGuardarPair.GUARDADO, l.guardar())
    }
    @Test fun corrupcionOArtefactoPerdidoNuncaEsVirgen() {
        val l = Laboratorio(); l.guardar(); val guardado = l.archivos.registro!!.copyOf()
        l.archivos.registro = null; assertTrue(l.almacen.cargar() is CargaPairCp2.Incierta)
        l.archivos.registro = guardado.also { it[10] = 0 }; assertTrue(l.almacen.cargar() is CargaPairCp2.Incierta)
        l.archivos.reserva = null; assertTrue(l.almacen.cargar() is CargaPairCp2.Incierta)
    }
    @Test fun consentimientoOtraRevisionHuellaOContextoNoEsGrabable() {
        for (tipo in 0..3) {
            val l = Laboratorio(); val c = l.consentimiento()
            val distinto = when (tipo) {
                0 -> c.copy(revisionAdulta = 8)
                1 -> c.copy(boot = 6)
                2 -> c.copy(generacion = c.generacion + 1)
                else -> c.copy(datos = c.datos.copy(huellaPadre = ""))
            }
            assertEquals(ResultadoGuardarPair.INCIERTO, l.almacen.guardar(l.v, distinto) { true })
            assertNull(l.archivos.reserva); assertNull(l.archivos.registro)
        }
    }
    @Test fun registroModificadoNoValidaConReservaRecalculadaNiFirmaDeOtraCandidata() {
        val l = Laboratorio(); l.guardar()
        val original = l.archivos.registro!!.toString(Charsets.US_ASCII)
        val alterado = original.replace(l.v.consentimiento.huellaPadre, Base64Cp1.codificar(ByteArray(32)))
        l.archivos.registro = alterado.toByteArray()
        l.archivos.reserva = CodecRegistroPair.sha(l.archivos.registro!!).toByteArray()
        assertTrue(AlmacenPairCp2(l.archivos).cargar() is CargaPairCp2.Incierta)
    }
    @Test fun errorAntesDeReservaNoPermiteReintentoEnMismoEscritor() {
        val l = Laboratorio(); l.archivos.fallo = "ANTES_RESERVA"
        assertEquals(ResultadoGuardarPair.INCIERTO, l.guardar())
        assertTrue(l.almacen.cargar() is CargaPairCp2.Incierta)
        l.archivos.fallo = ""; assertEquals(ResultadoGuardarPair.RECHAZADO, l.guardar())
        assertNull(l.archivos.reserva); assertNull(l.archivos.registro)
    }
    @Test fun contextoCambiaEnGuardaIntermediaNoEscribeRegistroTrasReserva() {
        val l = Laboratorio(); var lecturas = 0
        val r = l.almacen.guardar(l.v, l.consentimiento()) { ++lecturas < 3 }
        assertEquals(ResultadoGuardarPair.INCIERTO, r); assertNotNull(l.archivos.reserva); assertNull(l.archivos.registro)
    }
}
