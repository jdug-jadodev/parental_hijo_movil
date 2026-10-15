package dev.controlparental.child.orders

import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

class EscritorControlCp2Test {
    private class Disco : ArchivosControl {
        var datos: ByteArray? = null; var journal: ByteArray? = null
        var error = ""; var escribe = 0
        override fun leer() = datos?.copyOf()
        override fun reserva() = journal?.copyOf()
        override fun iniciar(bytes: ByteArray) { check(datos == null); datos = bytes.copyOf() }
        override fun reservar(bytes: ByteArray) { check(journal == null); journal = bytes.copyOf(); check(error != "reserva") }
        override fun escribir(bytes: ByteArray) { escribe++; datos = bytes.copyOf(); check(error != "escritura") }
        override fun terminarReserva() { check(error != "terminar"); journal = null }
    }
    private inner class Lab {
        val clave = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        val publica = Base64Cp1.codificar(clave.public.encoded)
        val id = "abcdefab-1234-4234-8234-abcdefabcdef"
        var pareja: ParejaOrdenCp2? = ParejaOrdenCp2(id, publica, publica, "registro", 5)
        val disco = Disco(); var ahora = 100L; var cerrados = 0
        fun escritor() = EscritorControlCp2(disco, { ahora }, { pareja }, { cerrados++ })
        val writer = escritor().also { assertTrue(it.abrir { true }) }
        fun firmado(m: MensajeOrdenCp2): ByteArray {
            val p = CodecOrdenCp2.escribirPayload(m)
            val s = Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update("CPv2/MESSAGE\n".toByteArray()); update(p); sign() }
            return CodecOrdenCp2.escribirSobre(SobreOrdenCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(s)))
        }
        fun preparar(): Pair<PreparacionOrdenCp2, () -> EntregaOrdenCp2> {
            lateinit var guardia: GuardiaOrdenCp2
            val puerto = object : EscritorControlOrdenCp2 {
                override fun consolidar() = writer.contexto()
                override fun consultarRetenida(orden: OrdenVerificadaCp2) { writer.retenida(orden) }
                override fun entregarParaDecision(preparacion: PreparacionOrdenCp2) { check(writer.remoto(preparacion) != null) }
            }
            guardia = GuardiaOrdenCp2({ ahora }, { pareja }, puerto)
            val ref = guardia.iniciar()!!
            val sync = SyncOrdenCp2(id, Base64Cp1.codificar(ByteArray(32)), AccionOrdenCp2.ALLOW_FOR)
            val r = guardia.recibirSync(ref, firmado(sync))!!.reto!!
            val c = writer.contexto()!!
            val o = PoliticaOrdenCp2(id, c.bootId, c.childSessionId, id, c.lastAcceptedSeq + 1, c.revisionControl, r.nonce, r.action, 60)
            val p = guardia.prepararPolitica(ref, firmado(o))!!
            return p to { guardia.entregar(ref, p) }
        }
    }
    @Test fun inicializacionRequiereAutoridadYNoEsPermiso() {
        val d = Disco(); var fallos = 0
        val p = ParejaOrdenCp2("p", "a", "b", "r", 1)
        val w = EscritorControlCp2(d, { 1 }, { p }, { fallos++ })
        assertFalse(w.abrir { false }); assertNull(d.datos); assertEquals(1, fallos)
        val l = Lab(); assertNull(l.writer.actual().decision); assertEquals(1L, l.writer.actual().revision)
        assertEquals(0L, l.writer.actual().seq)
    }
    @Test fun pendienteDurableAntesDeEfectoYConfirmacionExacta() {
        val l = Lab(); val (p, entregar) = l.preparar()
        assertEquals(EntregaOrdenCp2.ENTREGADA_AL_PUERTO_NO_APPLIED, entregar())
        val r = CodecControlDurable.leer(l.disco.datos!!)
        assertEquals(ResultadoControl.ACCEPTED_PENDING_ENFORCEMENT, r.remoto!!.resultado)
        assertNull(r.remoto.resultElapsed); assertEquals(EfectoControl.PENDING, r.decision!!.efecto)
        l.ahora++
        assertTrue(l.writer.confirmar(r.revision, true))
        assertEquals(ResultadoControl.APPLIED, l.writer.actual().remoto!!.resultado)
        assertEquals(101L, l.writer.actual().remoto!!.resultElapsed)
        val n = l.disco.escribe; assertTrue(l.writer.confirmar(r.revision, true)); assertEquals(n, l.disco.escribe)
        assertEquals(EntregaOrdenCp2.RECHAZADA, entregar())
        assertNotNull(l.writer.retenida(p.verificada))
    }
    @Test fun ioAmbiguoNoDevuelveDecisionNiPermisoYNoSeResetea() {
        for (etapa in listOf("reserva", "escritura", "terminar")) {
            val l = Lab(); l.disco.error = etapa
            assertNull(l.writer.local(AccionOrdenCp2.ALLOW, null, false)); assertFalse(l.writer.disponible)
            assertTrue(l.cerrados > 0); assertNotNull(l.disco.journal)
            l.disco.error = ""; val otra = l.escritor(); assertFalse(otra.abrir { true })
            assertNull(otra.contexto()); assertNotNull(l.disco.journal)
        }
    }
    @Test fun bootYProcesoConservanSeqResultadoPeroNoPermiso() {
        for (bootNuevo in listOf(false, true)) {
            val l = Lab(); val (_, entregar) = l.preparar(); entregar()
            val r = l.writer.actual(); l.writer.confirmar(r.revision, true)
            if (bootNuevo) l.pareja = l.pareja!!.copy(boot = 6)
            val nuevo = l.escritor(); assertTrue(nuevo.abrir { false })
            val n = nuevo.actual()
            assertNull(n.decision); assertEquals(r.seq, n.seq); assertEquals(ResultadoControl.APPLIED, n.remoto!!.resultado)
            assertEquals(r.revision + 1, n.revision); assertNotEquals(r.sesion, n.sesion)
            assertEquals(!bootNuevo, r.bootId == n.bootId)
        }
    }
    @Test fun ultimaLocalRemotaValidaGanaYCallbackViejoNoConfirma() {
        val l = Lab(); val (_, entregar) = l.preparar(); entregar(); val remota = l.writer.actual()
        val local = l.writer.local(AccionOrdenCp2.LOCK, null, false)!!
        assertTrue(l.writer.confirmar(remota.revision, true))
        assertEquals(EfectoControl.PENDING, l.writer.actual().decision!!.efecto)
        assertEquals(ResultadoControl.ACCEPTED_PENDING_ENFORCEMENT, l.writer.actual().remoto!!.resultado)
        assertTrue(l.writer.confirmar(local.revision, true)); assertEquals(EfectoControl.VERIFIED, l.writer.actual().decision!!.efecto)
        assertEquals(local.revision, l.writer.actual().revision)
    }
    @Test fun casRevisionCambiadaRevocacionYExpiracionNoAmpliacion() {
        val l = Lab(); val (p, entregar) = l.preparar()
        l.writer.local(AccionOrdenCp2.ALLOW, null, true)
        assertEquals(EntregaOrdenCp2.RECHAZADA, entregar()); assertNull(l.writer.remoto(p))
        val r = l.writer.actual(); assertTrue(r.decision!!.offline)
        assertTrue(l.writer.revocar()); assertNull(l.writer.actual().decision); assertEquals(r.revision + 1, l.writer.actual().revision)
        val d = l.writer.local(AccionOrdenCp2.ALLOW_FOR, 60, false)!!
        assertEquals(l.ahora + 60000, d.deadline)
    }
    @Test fun retenidoExactoNoReaplicaNiRenuevaNonceNiPlazo() {
        val l = Lab(); val (p, entregar) = l.preparar(); entregar(); val r = l.writer.actual()
        assertNotNull(l.writer.retenida(p.verificada)); assertEquals(r, l.writer.actual())
        val alterado = l.firmado(p.orden.copy(durationSec = 120))
        assertNull(l.writer.retenida(CriptografiaOrdenCp2.verificar(alterado, l.publica)))
        assertEquals(r, l.writer.actual())
    }
    @Test fun corrupcionCambioParejaYRegresionRelojCierran() {
        for (caso in 0..2) {
            val l = Lab()
            when (caso) { 0 -> l.disco.datos!![0] = 99; 1 -> l.pareja = l.pareja!!.copy(revisionRegistro = "otra"); 2 -> l.ahora-- }
            assertNull(l.writer.contexto()); assertFalse(l.writer.disponible); assertTrue(l.cerrados > 0)
        }
    }
    @Test fun resultadoFallidoPersistidoNoEsApplied() {
        val l = Lab(); val (_, entregar) = l.preparar(); entregar(); val r = l.writer.actual()
        l.writer.confirmar(r.revision, false)
        assertEquals(ResultadoControl.ENFORCEMENT_FAILED, l.writer.actual().remoto!!.resultado)
        assertEquals(EfectoControl.FAILED, l.writer.actual().decision!!.efecto)
    }
}
