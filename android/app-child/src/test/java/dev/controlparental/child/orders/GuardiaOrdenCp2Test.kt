package dev.controlparental.child.orders

import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class GuardiaOrdenCp2Test {
    private val id = "abcdefab-1234-4234-8234-abcdefabcdef"
    private val nonce = Base64Cp1.codificar(ByteArray(32) { 7 })
    private inner class Lab {
        var tiempo = 100L
        val clave = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
        val publica = Base64Cp1.codificar(clave.public.encoded)
        var pareja: ParejaOrdenCp2? = ParejaOrdenCp2(id, publica, publica, "registro1", 5)
        var control: ControlOrdenCp2? = ControlOrdenCp2(5, id, id, 7, 10)
        var alConsolidar: () -> Unit = {}; var alVerificar: () -> Unit = {}; var alDecidir: () -> Unit = {}
        var decisiones = 0; var consolidaciones = 0; var entropia = 0; var consultasRetenidas = 0
        val escritor = object : EscritorControlOrdenCp2 {
            override fun consolidar(): ControlOrdenCp2? { consolidaciones++; alConsolidar(); return control }
            override fun consultarRetenida(orden: OrdenVerificadaCp2) { consultasRetenidas++ }
            override fun entregarParaDecision(preparacion: PreparacionOrdenCp2) { decisiones++; alDecidir() }
        }
        val guardia = GuardiaOrdenCp2({ tiempo }, { pareja }, escritor, { ByteArray(32) { (++entropia).toByte() } },
            VerificadorOrdenCp2 { b, p -> CriptografiaOrdenCp2.verificar(b, p).also { alVerificar() } })
        fun frame(m: MensajeOrdenCp2): ByteArray {
            val p = CodecOrdenCp2.escribirPayload(m)
            val sig = Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update("CPv2/MESSAGE\n".toByteArray()); update(p); sign() }
            return CodecOrdenCp2.escribirSobre(SobreOrdenCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(sig)))
        }
        fun sync(ref: GeneracionOrdenCp2, action: AccionOrdenCp2? = AccionOrdenCp2.ALLOW_FOR) =
            guardia.recibirSync(ref, frame(SyncOrdenCp2(id, nonce, action)))
        fun politica(r: RetoOrdenCp2) = PoliticaOrdenCp2(id, id, id, id, 11, r.revision, r.nonce, r.action,
            if (r.action == AccionOrdenCp2.ALLOW_FOR) 60 else null)
        fun preparar(ref: GeneracionOrdenCp2, r: RetoOrdenCp2) = guardia.prepararPolitica(ref, frame(politica(r)))
    }
    @Test fun syncConsolidaRetoTreintaSegundosYPreparacionNoEsAplicacion() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!
        assertEquals(7L, r.revision); assertEquals(100L, r.issuedElapsedMs); assertEquals(30100L, r.expiresElapsedMs)
        assertEquals(32, Base64Cp1.decodificar(r.nonce, 32).size); assertTrue(l.consolidaciones >= 4)
        val p = l.preparar(ref, r)!!; assertEquals(l.politica(r), p.orden); assertEquals(0, l.decisiones)
        assertEquals(EntregaOrdenCp2.ENTREGADA_AL_PUERTO_NO_APPLIED, l.guardia.entregar(ref, p))
        assertEquals(1, l.decisiones); assertEquals(EntregaOrdenCp2.RECHAZADA, l.guardia.entregar(ref, p))
        assertNull(l.preparar(ref, r)) // No replay RAM ni exactly-once durable inventado.
    }
    @Test fun nuevoSyncMismaRevisionReemplazaYConsultaNullAnula() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val vieja = l.sync(ref)!!.reto!!
        val nueva = l.sync(ref)!!.reto!!; assertNotEquals(vieja.nonce, nueva.nonce)
        assertTrue(nueva.generacion > vieja.generacion); assertEquals(vieja.revision, nueva.revision)
        assertNull(l.preparar(ref, vieja)); assertNull(l.sync(ref, null)!!.reto); assertNull(l.preparar(ref, nueva))
    }
    @Test fun caducaExactamenteTreintaSegundosORelojRetrocedeSinRejuvenecer() {
        for (caso in 0..2) {
            val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!
            val p = l.preparar(ref, r)!!
            when (caso) { 0 -> l.tiempo += 30000; 1 -> l.tiempo--; 2 -> l.guardia.cerrar() }
            assertEquals(EntregaOrdenCp2.RECHAZADA, l.guardia.entregar(ref, p)); assertEquals(0, l.decisiones)
        }
    }
    @Test fun politicaExigeNonceAccionRevisionBootSesionParejaYSeqMayoresPersistidos() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!; val a = l.politica(r)
        val otra = "12345678-1234-4234-8234-123456789abc"
        for (p in listOf(a.copy(commandNonce = nonce), a.copy(action = AccionOrdenCp2.LOCK, durationSec = null), a.copy(expectedRevision = 8),
            a.copy(bootId = otra), a.copy(childSessionId = otra), a.copy(pairId = otra), a.copy(seq = 10))) assertNull(l.guardia.prepararPolitica(ref, l.frame(p)))
        assertNotNull(l.preparar(ref, r))
    }
    @Test fun consolidarPuedeVencerRevocarOCambiarSnapshotDuranteCripto() {
        for (caso in 0..5) {
            val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!
            l.alVerificar = { when (caso) {
                0 -> l.control = l.control!!.copy(revisionControl = 8)
                1 -> l.control = l.control!!.copy(lastAcceptedSeq = 11)
                2 -> l.control = null
                3 -> l.pareja = l.pareja!!.copy(revisionRegistro = "registro2")
                4 -> l.pareja = l.pareja!!.copy(publicaPadre = "otra")
                5 -> l.tiempo += 30000
            } }
            assertNull(l.preparar(ref, r)); assertEquals(0, l.decisiones)
        }
    }
    @Test fun conflictoDespuesDePrepararNoEntregaNiReiniciaSecuencia() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!; val p = l.preparar(ref, r)!!
        l.control = l.control!!.copy(revisionControl = 8)
        assertEquals(EntregaOrdenCp2.RECHAZADA, l.guardia.entregar(ref, p))
        val nueva = l.sync(ref)!!.reto!!; assertEquals(8L, nueva.revision)
        assertEquals(10L, l.control!!.lastAcceptedSeq); assertEquals(0, l.decisiones)
    }
    @Test fun callbackViejoNoRetiraRetoDeNuevaGeneracion() {
        val l = Lab(); val vieja = l.guardia.iniciar()!!; var nueva: GeneracionOrdenCp2? = null; var retoNuevo: RetoOrdenCp2? = null
        l.alVerificar = { l.alVerificar = {}; nueva = l.guardia.iniciar()!!; retoNuevo = l.sync(nueva!!)!!.reto!! }
        assertNull(l.sync(vieja)); assertNotNull(l.preparar(nueva!!, retoNuevo!!))
    }
    @Test fun consumoAnteriorAlPuertoImpideReentranciaYErrorNoDevuelveReto() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!; val p = l.preparar(ref, r)!!
        l.alDecidir = {
            assertEquals(EntregaOrdenCp2.RECHAZADA, l.guardia.entregar(ref, p)); assertNull(l.preparar(ref, r)); error("Fallo de puerto simulado")
        }
        assertEquals(EntregaOrdenCp2.ERROR_PUERTO, l.guardia.entregar(ref, p)); assertEquals(1, l.decisiones)
        assertEquals(EntregaOrdenCp2.RECHAZADA, l.guardia.entregar(ref, p))
    }
    @Test fun sinEscritorDurableNoDisponibleSinAutoridadMainAlternativa() {
        val l = Lab(); val noDisponible = object : EscritorControlOrdenCp2 {
            override fun consolidar(): ControlOrdenCp2? = null
            override fun consultarRetenida(orden: OrdenVerificadaCp2) = error("No debe consultarse")
            override fun entregarParaDecision(preparacion: PreparacionOrdenCp2) = error("No debe entregarse")
        }
        val g = GuardiaOrdenCp2({ l.tiempo }, { l.pareja }, noDisponible)
        assertNull(g.iniciar()); l.control = null; assertNull(l.guardia.iniciar())
    }
    @Test fun bootSesionIdentidadProcesoYContadoresNoRetroceden() {
        for (caso in 0..6) {
            val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!; val p = l.preparar(ref, r)!!
            when (caso) {
                0 -> l.control = l.control!!.copy(boot = 6)
                1 -> l.control = l.control!!.copy(childSessionId = "12345678-1234-4234-8234-123456789abc")
                2 -> l.pareja = l.pareja!!.copy(publicaHijo = "otra")
                3 -> l.control = l.control!!.copy(revisionControl = 6)
                4 -> l.control = l.control!!.copy(lastAcceptedSeq = 9)
                5 -> l.control = l.control!!.copy(bootId = "12345678-1234-4234-8234-123456789abc")
                6 -> l.pareja = null
            }
            assertEquals(EntregaOrdenCp2.RECHAZADA, l.guardia.entregar(ref, p)); assertEquals(0, l.decisiones)
            val otro = GuardiaOrdenCp2({ l.tiempo }, { l.pareja }, l.escritor)
            assertEquals(EntregaOrdenCp2.RECHAZADA, otro.entregar(ref, p))
        }
    }
    @Test fun syncNullReentranteMismaGeneracionHaceObsoletoCallbackCripto() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!
        l.alVerificar = { l.alVerificar = {}; assertNull(l.sync(ref, null)!!.reto) }
        assertNull(l.preparar(ref, r)); assertEquals(0, l.decisiones)
    }
    @Test fun reentranciaConsolidacionNoBorraRetoNuevoNiEntregaDosVeces() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!; val p = l.preparar(ref, r)!!
        var siguiente: RetoOrdenCp2? = null
        l.alConsolidar = { l.alConsolidar = {}; siguiente = l.sync(ref)!!.reto!! }
        assertEquals(EntregaOrdenCp2.RECHAZADA, l.guardia.entregar(ref, p))
        assertNotNull(l.preparar(ref, siguiente!!)); assertEquals(0, l.decisiones)
    }
    @Test fun frontera29999YLongMaxNoInventanSeqNiRevision() {
        val l = Lab(); l.control = l.control!!.copy(revisionControl = Long.MAX_VALUE, lastAcceptedSeq = Long.MAX_VALUE - 1)
        val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!; l.tiempo += 29999
        val p = l.guardia.prepararPolitica(ref, l.frame(l.politica(r).copy(seq = Long.MAX_VALUE)))!!
        assertEquals(Long.MAX_VALUE, p.orden.seq); assertEquals(Long.MAX_VALUE, p.orden.expectedRevision)
        assertEquals(EntregaOrdenCp2.ENTREGADA_AL_PUERTO_NO_APPLIED, l.guardia.entregar(ref, p))
        l.control = l.control!!.copy(lastAcceptedSeq = Long.MAX_VALUE)
        val nuevo = l.sync(ref)!!.reto!!
        assertNull(l.guardia.prepararPolitica(ref, l.frame(l.politica(nuevo).copy(seq = Long.MAX_VALUE))))
        assertEquals(1, l.consultasRetenidas)
    }
    @Test fun retenidaSeConsultaPorHashSinReaplicarNiSustituirReto() {
        val l = Lab(); val ref = l.guardia.iniciar()!!; val r = l.sync(ref)!!.reto!!
        assertNull(l.guardia.prepararPolitica(ref, l.frame(l.politica(r).copy(seq = 10))))
        assertEquals(1, l.consultasRetenidas); assertEquals(0, l.decisiones)
        assertNotNull(l.preparar(ref, r)) // Consultar no produce otro nonce ni permiso ni reset de seq.
    }
    @Test fun entropiaReentranteORepetidaNoRejuveneceReto() {
        val l = Lab(); lateinit var g: GuardiaOrdenCp2; var cancelar = true
        g = GuardiaOrdenCp2({ l.tiempo }, { l.pareja }, l.escritor, {
            if (cancelar) g.cerrar()
            ByteArray(32) { 1 }
        })
        val r = g.iniciar()!!; assertNull(g.recibirSync(r, l.frame(SyncOrdenCp2(id, nonce, AccionOrdenCp2.LOCK))))
        cancelar = false; val nueva = g.iniciar()!!
        assertNotNull(g.recibirSync(nueva, l.frame(SyncOrdenCp2(id, nonce, AccionOrdenCp2.LOCK))))
        assertNull(g.recibirSync(nueva, l.frame(SyncOrdenCp2(id, nonce, AccionOrdenCp2.LOCK))))
    }
}
