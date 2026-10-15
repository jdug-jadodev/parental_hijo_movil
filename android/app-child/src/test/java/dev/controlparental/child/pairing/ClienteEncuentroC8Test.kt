package dev.controlparental.child.pairing

import dev.controlparental.child.local.*
import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.c8.*
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.UUID

/** Firmas/casos reales Kotlin; socket/reloj/disco y autoridad adulta acreditada son puertos de laboratorio. */
class ClienteEncuentroC8Test {
    private fun clave() = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
    private fun publica(k: KeyPair) = Base64Cp1.codificar(k.public.encoded)
    private class Disco : ArchivosPairCp2 {
        var reserva: ByteArray? = null; var registro: ByteArray? = null
        var trasReserva: () -> Unit = {}; var trasRegistro: () -> Unit = {}; var romperReadback = false
        override fun leerReserva() = reserva?.copyOf()
        override fun leerRegistro() = registro?.copyOf()
        override fun reservar(bytes: ByteArray) { reserva = bytes.copyOf(); trasReserva() }
        override fun escribirRegistro(bytes: ByteArray) { registro = bytes.copyOf(); trasRegistro(); if (romperReadback) registro = byteArrayOf(0) }
    }
    private inner class Lab {
        var ahora = 10L; var vivo = true; var boot = 5; var contextoDisponible = true
        val hijo = clave(); val padre = clave(); val origen = "https://relay.invalid"
        val sesion = SesionAdulto({ ahora }, { boot })
        val g: TicketAdulto; val marco: MarcoSesionAdulto
        val ofertas: OfertasRam; val ref: ReferenciaOferta; val caso: ValidacionPairRam
        val enviados = mutableListOf<FrameC8>(); val codigos = mutableListOf<String>()
        var aceptacionRecibida: ByteArray? = null; var cierres = 0; var reclamada = false; var falloEnvio = false
        var duranteFirma: () -> Unit = {}
        val disco = Disco(); val almacen = AlmacenPairCp2(disco)
        val cliente: ClienteEncuentroC8
        val firmante = object : FirmanteAuthCp2 {
            override val publica get() = publica(hijo)
            override fun firmar(bytes: ByteArray): ByteArray {
                duranteFirma()
                return Signature.getInstance("SHA256withECDSA").run { initSign(hijo.private); update(bytes); sign() }
            }
        }
        val socket = object : SocketC8 {
            override fun enviar(bytes: ByteArray): Boolean { if (falloEnvio) return false; enviados.add(CodecC8.leer(bytes)); return true }
            override fun cerrar(codigo: Int) { cierres++ }
        }
        init {
            sesion.reanudar(); val entrada = sesion.solicitar(FinAdulto.ENTRAR)!!
            check(sesion.completar(entrada, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
            g = ticket(FinAdulto.GENERAR_OFERTA); marco = sesion.marcoAcreditado(g)!!
            ofertas = OfertasRam({ ahora }, { if (contextoDisponible) ContextoOferta(boot, publica(hijo)) else null },
                { sesion.accionVigente(g) && almacen.cargar() is CargaPairCp2.Ausente }, vidaSesion = { vivo && sesion.marcoVigente(marco) })
            ref = ofertas.generar(); caso = ValidacionPairRam(ofertas, sesion)
            cliente = ClienteEncuentroC8({ ahora }, origen, ref, marco.limite, { vivo && ofertas.vigente(ref) }, firmante,
                { codigos.add(it) }, { reclamada = true; codigos.clear() }, { aceptacionRecibida = it.copyOf() },
                { vivo = false; aceptacionRecibida?.fill(0); aceptacionRecibida = null; codigos.clear(); caso.cancelar(); ofertas.cancelar(); sesion.cerrar() })
        }
        fun ticket(fin: FinAdulto) = sesion.solicitar(fin)!!.also { check(sesion.autorizarAccion(it, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9))) }
        fun recibir(f: FrameC8) = cliente.recibir(CodecC8.escribir(f))
        fun reto(origenReto: String = origen) {
            cliente.abierto(socket, "c8.v1")
            recibir(CodecC8.crear("CHALLENGE", mapOf("connectionId" to UUID.randomUUID().toString(), "nonceB64" to ref.oferta.enrollNonce, "relayOrigin" to origenReto)))
        }
        fun publicar() { reto(); ahora += 500; recibir(CodecC8.crear("READY")) }
        fun codigo() { publicar(); recibir(CodecC8.crear("CODE", mapOf("code" to "ABCDEFGH"), mapOf("remainingMs" to 110000))) }
        fun reclamar() { codigo(); recibir(CodecC8.crear("CLAIMED", mapOf("parentPublicKeyB64" to publica(padre)), mapOf("remainingMs" to 100000))) }
        fun aceptacion() = AceptacionPairCp2(UUID.randomUUID().toString(), ref.oferta.enrollId, ref.oferta.enrollNonce,
            publica(padre), publica(hijo), origen, HashesPairCp2(ref.oferta.enrollNonce, ref.oferta.enrollNonce, ref.oferta.enrollNonce))
        fun firmar(a: AceptacionPairCp2 = aceptacion()): ByteArray {
            val p = CodecPairCp2.escribirPayload(a)
            val s = Signature.getInstance("SHA256withECDSA").run { initSign(padre.private); update("CPv2/PAIR\n".toByteArray()); update(p); sign() }
            return CodecPairCp2.escribirSobre(SobrePairCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(s)))
        }
        fun entregar(original: ByteArray = firmar()) { recibir(CodecC8.crear("PAIR", mapOf("pairB64" to Base64Cp1.codificar(original)))) }
        fun datos() = DatosConfirmadosPair(publica(padre), CodecPairCp2.huellaPublica(publica(padre)), CodecPairCp2.huellaPublica(publica(hijo)), origen)
        fun validar(datos: DatosConfirmadosPair = datos()): ValidacionPair? {
            val c = caso.confirmar(ref, ticket(FinAdulto.VALIDAR_PAIR), datos) ?: return null
            return caso.validar(c, aceptacionRecibida!!)
        }
        fun guardar(v: ValidacionPair): ResultadoGuardarPair {
            val t = ticket(FinAdulto.GUARDAR_PAIR)
            return almacen.guardar(v, ConsentimientoPair(v.consentimiento, boot, ref.generacion, 9, ahora)) {
                sesion.accionVigente(t) && vivo && caso.resultadoActual() === v
            }
        }
    }
    @Test fun recorridoSinQrNiCamaraProofOriginalOfertaYPairCommitReadback() {
        val l = Lab(); l.reclamar(); assertTrue(l.reclamada)
        assertEquals(listOf("AUTH", "PUBLISH"), l.enviados.map { it.kind })
        val auth = l.enviados.first(); val p = CodecC8.leerPrueba(CodecC8.documento(auth.texto("payloadB64")))
        CriptografiaC8.verificar(auth, p); assertEquals("CHILD", p.role); assertEquals(publica(l.hijo), p.publicKeyB64)
        assertArrayEquals(CodecOfertaCp2.escribir(l.ref.oferta), CodecC8.documento(l.enviados[1].texto("offerB64")))
        val original = l.firmar(); l.entregar(original); assertArrayEquals(original, l.aceptacionRecibida)
        assertTrue(l.almacen.cargar() is CargaPairCp2.Ausente) // Transporte no es guardado ni confianza.
        val v = l.validar()!!; assertEquals(ResultadoGuardarPair.GUARDADO, l.guardar(v))
        l.cliente.cerrar()
        val s = (AlmacenPairCp2(l.disco).cargar() as CargaPairCp2.Guardado).snapshot
        assertArrayEquals(original, s.integridad.bytesFrame()); assertEquals(v.payloadSha256B64, s.integridad.payloadSha256B64)
        assertEquals("VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION", s.estadoLocal)
        assertNull(l.caso.resultadoActual()); assertNull(l.aceptacionRecibida)
    }
    @Test fun ttlSeMideTrasChallengeAlPublicarYTicketsNoRenuevanMarco() {
        val l = Lab(); l.reto(); l.ahora += 2000; l.recibir(CodecC8.crear("READY"))
        assertEquals(118000L, l.enviados.last().numero("ttlMs"))
        assertTrue(l.ofertas.restante(l.ref)!! > 120000) // No usar este restante5min como presupuesto.
        l.ticket(FinAdulto.VALIDAR_PAIR); assertEquals(120010L, l.marco.limite)
        l.ahora = l.marco.limite; assertFalse(l.cliente.comprobar()); assertEquals(FaseC8.CERRADA, l.cliente.fase)
    }
    @Test fun ordenIncorrectoReplayRetoReadyCodigoYSegundaEntregaCierran() {
        for (tipo in 0..5) {
            val l = Lab()
            when (tipo) {
                0 -> { l.cliente.abierto(l.socket, "c8.v1"); l.recibir(CodecC8.crear("READY")) }
                1 -> { l.reto(); l.recibir(CodecC8.crear("CHALLENGE", mapOf("connectionId" to UUID.randomUUID().toString(), "nonceB64" to l.ref.oferta.enrollNonce, "relayOrigin" to l.origen))) }
                2 -> { l.publicar(); l.recibir(CodecC8.crear("READY")) }
                3 -> { l.codigo(); l.recibir(CodecC8.crear("CODE", mapOf("code" to "ABCDEFGH"), mapOf("remainingMs" to 1))) }
                4 -> { l.publicar(); l.entregar() }
                5 -> { l.reclamar(); l.entregar(); l.entregar() }
            }
            assertEquals(FaseC8.CERRADA, l.cliente.fase); assertNull(l.aceptacionRecibida); assertTrue(l.almacen.cargar() is CargaPairCp2.Ausente)
        }
    }
    @Test fun origenSubprotocoloClaveIgualHijoYCamposFueraDeContratoCierran() {
        val a = Lab(); a.reto("https://otro.invalid"); assertEquals(FaseC8.CERRADA, a.cliente.fase); assertTrue(a.enviados.isEmpty())
        for (protocolo in listOf(null, "cp.v2", "c8.v1, cp.v2")) { val l = Lab(); l.cliente.abierto(l.socket, protocolo); assertEquals(FaseC8.CERRADA, l.cliente.fase) }
        val b = Lab(); b.codigo(); b.recibir(CodecC8.crear("CLAIMED", mapOf("parentPublicKeyB64" to publica(b.hijo)), mapOf("remainingMs" to 100)))
        assertEquals(FaseC8.CERRADA, b.cliente.fase)
        val c = Lab(); c.reto(); c.cliente.recibir("{\"kind\":\"READY\",\"nonce\":\"x\",\"v\":1}".toByteArray()); assertEquals(FaseC8.CERRADA, c.cliente.fase)
    }
    @Test fun timeoutAuthDiezSegundosYVencimientoLocalAunqueRemainingEnorme() {
        val l = Lab(); l.reto(); l.ahora += 10000; l.recibir(CodecC8.crear("READY")); assertEquals(FaseC8.CERRADA, l.cliente.fase)
        val b = Lab(); b.publicar(); b.recibir(CodecC8.crear("CODE", mapOf("code" to "ABCDEFGH"), mapOf("remainingMs" to 9007199254740991)))
        b.ahora = b.marco.limite; assertFalse(b.cliente.comprobar())
        val c = Lab(); c.publicar(); c.recibir(CodecC8.crear("CODE", mapOf("code" to "ABCDEFGH"), mapOf("remainingMs" to 1)))
        c.ahora++; assertFalse(c.cliente.comprobar())
    }
    @Test fun negativaServidorFalloEnvioYCambiosDuranteFirmaNoPublican() {
        val a = Lab(); a.reto(); a.recibir(CodecC8.crear("UNAVAILABLE")); assertEquals(FaseC8.CERRADA, a.cliente.fase)
        val b = Lab(); b.falloEnvio = true; b.reto(); assertEquals(FaseC8.CERRADA, b.cliente.fase)
        val c = Lab(); c.duranteFirma = { c.sesion.cerrar() }; c.reto(); assertEquals(FaseC8.CERRADA, c.cliente.fase)
        assertFalse(c.enviados.any { it.kind == "AUTH" || it.kind == "PUBLISH" })
    }
    @Test fun pairFirmadoPeroContextoDistintoNoEsCandidata() {
        for (tipo in 0..3) {
            val l = Lab(); l.reclamar(); val a = l.aceptacion()
            val otra = when (tipo) { 0 -> a.copy(enrollId = UUID.randomUUID().toString()); 1 -> a.copy(enrollNonce = Base64Cp1.codificar(ByteArray(32)))
                2 -> a.copy(childPublicKeyB64 = publica(clave())); else -> a.copy(relayOrigin = "https://otro.invalid") }
            l.entregar(l.firmar(otra)); assertEquals(FaseC8.CERRADA, l.cliente.fase); assertNull(l.aceptacionRecibida)
        }
    }
    @Test fun transporteNoSupleHuellaIndependienteNiTicketFresh() {
        val l = Lab(); l.reclamar(); l.entregar()
        assertNull(l.caso.confirmar(l.ref, l.g, l.datos()))
        val t = l.sesion.solicitar(FinAdulto.VALIDAR_PAIR)!!; assertNull(l.caso.confirmar(l.ref, t, l.datos()))
        assertNull(l.validar(l.datos().copy(huellaPadre = "")))
        assertNull(l.validar(l.datos().copy(origen = "https://otro.invalid")))
        assertTrue(l.almacen.cargar() is CargaPairCp2.Ausente)
    }
    @Test fun fondoBootContextoORelojInvalidaCandidataSinGuardar() {
        for (tipo in 0..3) {
            val l = Lab(); l.reclamar(); l.entregar()
            when (tipo) { 0 -> l.sesion.cerrar(); 1 -> l.boot++; 2 -> l.contextoDisponible = false; 3 -> l.ahora-- }
            assertFalse(l.cliente.comprobar()); assertNull(l.aceptacionRecibida); assertNull(l.caso.resultadoActual())
            assertTrue(l.almacen.cargar() is CargaPairCp2.Ausente)
        }
    }
    @Test fun cancelarTrasValidacionNoResucitaPorCallbackViejo() {
        val l = Lab(); l.reclamar(); l.entregar(); assertNotNull(l.validar())
        l.cliente.cerrar(); val cierres = l.cierres; l.entregar(); l.cliente.cerrar()
        assertEquals(cierres, l.cierres); assertNull(l.caso.resultadoActual()); assertNull(l.aceptacionRecibida)
        assertTrue(l.almacen.cargar() is CargaPairCp2.Ausente)
    }
    @Test fun cierreDuranteReservaConservaInciertoSinSegundaAlta() {
        val l = Lab(); l.reclamar(); l.entregar(); val v = l.validar()!!
        l.disco.trasReserva = { l.cliente.cerrar() }
        assertEquals(ResultadoGuardarPair.INCIERTO, l.guardar(v)); assertNotNull(l.disco.reserva); assertNull(l.disco.registro)
        val nuevo = AlmacenPairCp2(l.disco); assertTrue(nuevo.cargar() is CargaPairCp2.Incierta)
        assertEquals(ResultadoGuardarPair.RECHAZADO, nuevo.guardar(v, ConsentimientoPair(v.consentimiento, 5, l.ref.generacion, 9, l.ahora)) { true })
    }
    @Test fun cierreDuranteCommitNoRevierteYReadbackCorruptoNuncaExito() {
        val a = Lab(); a.reclamar(); a.entregar(); val v = a.validar()!!
        a.disco.trasRegistro = { a.cliente.cerrar() }
        assertEquals(ResultadoGuardarPair.GUARDADO_SIN_SESION, a.guardar(v)); assertTrue(AlmacenPairCp2(a.disco).cargar() is CargaPairCp2.Guardado)
        val b = Lab(); b.reclamar(); b.entregar(); val vb = b.validar()!!; b.disco.romperReadback = true
        assertEquals(ResultadoGuardarPair.INCIERTO, b.guardar(vb)); assertTrue(b.almacen.cargar() is CargaPairCp2.Incierta)
    }
    @Test fun firmaLentaOCanceladaOReentranteNoEntregaPruebaNiOferta() {
        for (tipo in 0..2) {
            val l = Lab(); l.duranteFirma = {
                when (tipo) { 0 -> l.ahora += 10000; 1 -> l.cliente.cerrar(); else -> l.recibir(CodecC8.crear("READY")) }
            }
            l.reto(); assertEquals(FaseC8.CERRADA, l.cliente.fase)
            assertFalse(l.enviados.any { it.kind in setOf("AUTH", "PUBLISH") })
        }
    }
    @Test fun pairDebeCoincidirConPublicaClaimedNoSoloTenerFirmaValida() {
        val l = Lab(); l.codigo()
        l.recibir(CodecC8.crear("CLAIMED", mapOf("parentPublicKeyB64" to publica(clave())), mapOf("remainingMs" to 10000)))
        l.entregar(); assertEquals(FaseC8.CERRADA, l.cliente.fase); assertNull(l.aceptacionRecibida)
    }
    @Test fun framesExcesivosOMayoresAlMaximoCierranSinGuardar() {
        val l = Lab(); l.reto(); l.cliente.recibir(ByteArray(16385) { 32 }); assertEquals(FaseC8.CERRADA, l.cliente.fase)
        val b = Lab(); b.reto()
        // No hay mensaje entrante repetible en R1; el primero fuera de orden ya cierra.
        repeat(25) { b.recibir(CodecC8.crear("SENT")) }
        assertEquals(FaseC8.CERRADA, b.cliente.fase); assertEquals(1, b.cierres)
    }
    @Test fun guardaDeCommitLeeDeadlineSinEsperarTimerNiTomarCerrojoDeModelo() {
        val l = Lab(); l.reclamar(); l.entregar(); val v = l.validar()!!
        val t = l.ticket(FinAdulto.GUARDAR_PAIR)
        l.ahora = l.cliente.limiteVigencia // Marco adulto aún no ha caducado; canal ya sí.
        assertTrue(l.sesion.marcoVigente(l.marco))
        val resultado = l.almacen.guardar(v, ConsentimientoPair(v.consentimiento, 5, l.ref.generacion, 9, l.ahora)) {
            l.ahora < l.cliente.limiteVigencia && l.sesion.accionVigente(t) && l.caso.resultadoActual() === v
        }
        assertEquals(ResultadoGuardarPair.RECHAZADO, resultado); assertNull(l.disco.reserva); assertNull(l.disco.registro)
    }
}
