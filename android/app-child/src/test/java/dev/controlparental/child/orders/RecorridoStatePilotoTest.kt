package dev.controlparental.child.orders

import dev.controlparental.child.auth.*
import dev.controlparental.child.channel.*
import dev.controlparental.child.lock.ArchivoSnapshotLocal
import dev.controlparental.child.network.*
import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.UUID

/** Vertical JVM: componentes reales, glue del ensayo y puertos disco/reloj/socket/enforcement FALSOS. */
class RecorridoStatePilotoTest {
    private class Archivo : ArchivoSnapshotLocal {
        var b: ByteArray? = null
        override fun leer() = b?.copyOf()
        override fun escribir(bytes: ByteArray) { b = bytes.copyOf() }
        override fun borrar() { b = null }
    }
    private class Disco : ArchivosControl {
        var b: ByteArray? = null; var reserva: ByteArray? = null; var fallo = false
        override fun leer() = b?.copyOf()
        override fun reserva() = reserva?.copyOf()
        override fun iniciar(bytes: ByteArray) { b = bytes.copyOf() }
        override fun reservar(bytes: ByteArray) { reserva = bytes.copyOf(); check(!fallo) }
        override fun escribir(bytes: ByteArray) { b = bytes.copyOf() }
        override fun terminarReserva() { reserva = null }
    }
    private inner class Lab {
        val padre = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        val hijo = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        val pubP = Base64Cp1.codificar(padre.public.encoded); val pubH = Base64Cp1.codificar(hijo.public.encoded)
        val pairId = UUID.randomUUID().toString(); var t = 100L; val disco = Disco()
        val pareja = ParejaOrdenCp2(pairId, pubP, pubH, "snapshot1", 1)
        val uso = UsoRedLocal(Archivo(), Archivo(), { t }, { 1 }).also {
            it.preparar { true }; it.iniciarObservador(1); it.observar(ValidacionRed.VALIDADA, 1, 1)
        }
        val writer = EscritorControlCp2(disco, { t }, { pareja }, { uso.bloquear { true } }).also { assertTrue(it.abrir { true }) }
        val nonceSync = Base64Cp1.codificar(ByteArray(32) { 5 }); var consulta = nonceSync
        var stateSeq = 0L; var lock = true; var aplicaciones = 0; var pendiente: DecisionControl? = null
        var reto: RetoOrdenCp2? = null; val estados = mutableListOf<StateCp2>(); val acks = mutableListOf<AckCp2>()
        fun firma(k: KeyPair, bytes: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(k.private); update(bytes); sign() }
        val firmante = object : FirmanteAuthCp2 {
            override val publica get() = pubH
            override fun firmar(bytes: ByteArray) = firma(hijo, bytes)
        }
        lateinit var ref: GeneracionCanalCp2
        val auth = ClienteAuthHijo({ t }, { ContextoAuthHijo(pairId, pubH, pubP, "https://relay.example", "snapshot1", 1) }) { firmante }
        val canal = ClienteCanalCp2(auth, { _, b -> recibir(b) }, {})
        val guardia = GuardiaOrdenCp2({ t }, { pareja }, object : EscritorControlOrdenCp2 {
            override fun consolidar(): ControlOrdenCp2? { this@Lab.consolidar(); return writer.contexto() }
            override fun consultarRetenida(orden: OrdenVerificadaCp2) { if (writer.retenida(orden) != null) emitir() }
            override fun entregarParaDecision(preparacion: PreparacionOrdenCp2) {
                val d = writer.remoto(preparacion) ?: error("CAS")
                reto = null; pendiente = d; emitir() // Ordenación observable: pendiente firmado antes del efecto falso.
            }
        })
        val generacion = guardia.iniciar()!!
        fun autenticar() {
            canal.cerrar(); ref = canal.iniciar()!!
            val conexion = UUID.randomUUID().toString()
            canal.abierto(ref, object : SocketCp2 {
                override fun enviar(texto: String): Boolean {
                    if (texto.contains("\"purpose\":\"AUTH\"")) CriptografiaAuthCp2.verificar(texto.toByteArray(), pubH, RolAuthCp2.CHILD)
                    else if ((SobreMessageCp2.leer(texto.toByteArray()).campos["kind"] as dev.controlparental.protocol.TextoJson).texto == "ACK")
                        acks.add(CriptografiaAckCp2.verificar(texto.toByteArray(), pubH))
                    else estados.add(CriptografiaStateCp2.verificar(texto.toByteArray(), pubH))
                    return true
                }
                override fun cerrar() {}
            }, "cp.v2")
            canal.recibir(ref, CodecAuthCp2.escribir(RetoAuthCp2(conexion, Base64Cp1.codificar(ByteArray(32) { 1 }))))
            canal.recibir(ref, CodecAuthCp2.escribir(OkAuthCp2(conexion))); assertTrue(canal.vigente(ref))
        }
        fun frame(m: MensajeOrdenCp2): ByteArray {
            val p = CodecOrdenCp2.escribirPayload(m)
            return CodecOrdenCp2.escribirSobre(SobreOrdenCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(firma(padre, "CPv2/MESSAGE\n".toByteArray() + p))))
        }
        fun recibir(b: ByteArray) {
            val v = CriptografiaOrdenCp2.verificar(b, pubP)
            when (val m = v.mensaje) {
                is SyncOrdenCp2 -> { val s = guardia.recibirSync(generacion, b) ?: return; consulta = m.syncNonce; reto = s.reto; emitir() }
                is PoliticaOrdenCp2 -> { guardia.prepararPolitica(generacion, b)?.let { guardia.entregar(generacion, it) } }
            }
        }
        fun sync(a: AccionOrdenCp2?) { canal.recibir(ref, frame(SyncOrdenCp2(pairId, nonceSync, a))) }
        fun ordenar(a: AccionOrdenCp2): ByteArray {
            sync(a); val r = reto!!; val c = writer.contexto()!!
            return frame(PoliticaOrdenCp2(pairId, c.bootId, c.childSessionId, UUID.randomUUID().toString(), c.lastAcceptedSeq + 1,
                r.revision, r.nonce, a, if (a == AccionOrdenCp2.ALLOW_FOR) 60 else null)).also { canal.recibir(ref, it) }
        }
        fun aplicar() {
            val d = pendiente!!; pendiente = null; aplicaciones++
            assertEquals(ResultadoStateCp2.ACCEPTED_PENDING_ENFORCEMENT, estados.last().lastRemote!!.result)
            assertEquals(ResultadoAckCp2.ACCEPTED_PENDING_ENFORCEMENT, acks.last().result)
            assertNull(acks.last().resultElapsedMs)
            if (d.action == AccionOrdenCp2.LOCK) { uso.bloquear { true }; lock = true }
            else { assertTrue(uso.autorizar(UsoLocal.NORMAL, limiteAbsoluto = d.deadline) { writer.esRevision(d.revision) }); lock = false }
            writer.confirmar(d.revision, true); emitir()
        }
        fun consolidar() {
            val antes = uso.permiteUso; uso.referencia?.let(uso::comprobarDeadline)
            if (antes && !uso.permiteUso) { writer.revocar(if (uso.motivoRevocacion == MotivoRed.RED_10S) MotivoStateCp2.NETWORK_LOST else MotivoStateCp2.TIMER_EXPIRED); reto = null }
        }
        fun emitir() {
            consolidar(); val r = writer.actual()
            r.remoto?.let { assertTrue(canal.enviar(ref, CriptografiaAckCp2.firmar(ProyeccionAckCp2.retenida(it, r.revision), pubH, firmante))) }
            val s = ProyeccionStateCp2.crear(r, pairId, consulta, ++stateSeq, t, writer.motivoActual, reto,
                uso.red == ValidacionRed.VALIDADA, true, lock, uso.permiteUso, true)
            assertTrue(canal.enviar(ref, CriptografiaStateCp2.firmar(s, pubH, firmante)))
        }
    }
    @Test fun authSyncLockAllowAllowForStateFirmadosPendienteAntesDeAppliedYReplaySinEfecto() {
        val l = Lab(); l.autenticar(); l.sync(null); assertNull(l.estados.last().commandChallenge)
        for (a in listOf(AccionOrdenCp2.LOCK, AccionOrdenCp2.ALLOW, AccionOrdenCp2.ALLOW_FOR)) {
            val b = l.ordenar(a); assertEquals(ModoStateCp2.UNKNOWN, l.estados.last().effective.mode)
            l.aplicar(); val s = l.estados.last()
            assertEquals(ResultadoStateCp2.APPLIED, s.lastRemote!!.result)
            val ack = l.acks.last()
            assertEquals(ResultadoAckCp2.APPLIED, ack.result); assertEquals(s.lastRemote!!.payloadSha256B64, ack.commandPayloadSha256B64)
            assertEquals(s.lastRemote!!.commandId, ack.commandId); assertEquals(s.lastRemote!!.acceptedRevision, ack.acceptedRevision)
            assertEquals(a, s.effective.action); assertEquals(l.consulta, s.syncNonce)
            val cantidad = l.aplicaciones; l.canal.recibir(l.ref, b); assertEquals(cantidad, l.aplicaciones)
            assertEquals(s.lastRemote, l.estados.last().lastRemote)
        }
    }
    @Test fun normalRedDiezSegundosStateNetworkLostNoReabreAlReconectar() {
        val l = Lab(); l.autenticar(); l.ordenar(AccionOrdenCp2.ALLOW); l.aplicar()
        l.uso.observar(ValidacionRed.NO_VALIDADA, 1, 2); l.t += 10000; l.emitir()
        assertFalse(l.uso.permiteUso); assertEquals(MotivoStateCp2.NETWORK_LOST, l.estados.last().effective.reason)
        assertEquals(AccionOrdenCp2.LOCK, l.estados.last().effective.action)
        l.uso.observar(ValidacionRed.VALIDADA, 1, 3); l.sync(null); assertFalse(l.uso.permiteUso)
        assertEquals(ResultadoStateCp2.APPLIED, l.estados.last().lastRemote!!.result) // Histórico, no permiso actual.
    }
    @Test fun ioAmbiguoNoLlamaAplicacionNiGeneraApplied() {
        val l = Lab(); l.autenticar(); l.disco.fallo = true
        l.ordenar(AccionOrdenCp2.ALLOW_FOR)
        assertNull(l.pendiente); assertFalse(l.uso.permiteUso); assertFalse(l.writer.disponible); assertEquals(0, l.aplicaciones)
    }
}
