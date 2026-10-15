package dev.controlparental.child.orders

import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

class ProyeccionAckCp2Test {
    private val id = "abcdefab-1234-4234-8234-abcdefabcdef"
    private val otro = "12345678-1234-4234-8234-123456789abc"
    private val hash = Base64Cp1.codificar(ByteArray(32))
    private val k = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
    private fun orden(seq: Long = 4) = PoliticaOrdenCp2(id, id, id, otro, seq, 7, hash, AccionOrdenCp2.LOCK, null)
    private fun verificar(o: PoliticaOrdenCp2): OrdenVerificadaCp2 {
        val p = CodecOrdenCp2.escribirPayload(o)
        val s = Signature.getInstance("SHA256withECDSA").run { initSign(k.private); update("CPv2/MESSAGE\n".toByteArray()); update(p); sign() }
        return CriptografiaOrdenCp2.verificar(CodecOrdenCp2.escribirSobre(SobreOrdenCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(s))), Base64Cp1.codificar(k.public.encoded))
    }
    private fun registro(seq: Long = 3) = RegistroControl("$id:registro", 1, id, id, 7, seq, null, null)
    @Test fun rechazosCasBootSesionRevisionNonceConservanHashYContextoSinAccepted() {
        for ((o, motivo) in listOf(orden().copy(bootId = otro) to MotivoAckCp2.STALE_BOOT,
            orden().copy(childSessionId = otro) to MotivoAckCp2.STALE_SESSION,
            orden().copy(expectedRevision = 6) to MotivoAckCp2.STALE_REVISION,
            orden() to MotivoAckCp2.STALE_NONCE)) {
            val v = verificar(o); val a = ProyeccionAckCp2.respuesta(v, registro(), null, null, 100)
            assertEquals(ResultadoAckCp2.REJECTED, a.result); assertEquals(motivo, a.reason)
            assertNull(a.acceptedRevision); assertNull(a.resultElapsedMs)
            assertEquals(v.payloadSha256B64, a.commandPayloadSha256B64); assertEquals(o.bootId, a.bootId); assertEquals(o.childSessionId, a.childSessionId)
            assertEquals(a, CodecAckCp2.leer(CodecAckCp2.escribir(a)))
        }
    }
    @Test fun secuenciaMismaNoRetenidaConflictoAnteriorDesconocidaNoResetea() {
        val misma = ProyeccionAckCp2.respuesta(verificar(orden(3)), registro(), null, null, 100)
        assertEquals(MotivoAckCp2.SEQUENCE_CONFLICT, misma.reason)
        val anterior = ProyeccionAckCp2.respuesta(verificar(orden(2)), registro(), null, null, 100)
        assertEquals(ResultadoAckCp2.UNKNOWN_COMMAND, anterior.result); assertNull(anterior.acceptedRevision)
        assertEquals(3L, registro().seq)
    }
    @Test fun duplicadoRetenidoOtroBootRelojOriginalSinReaplicarNiRenovar() {
        val o = orden(3).copy(bootId = otro, childSessionId = otro)
        val v = verificar(o)
        val remoto = RemotoControl(o, v.payloadSha256B64, v.bytesFrame(), 5, ResultadoControl.APPLIED, Long.MAX_VALUE)
        val a = ProyeccionAckCp2.respuesta(v, registro(), remoto, null, 100)
        assertEquals(ResultadoAckCp2.APPLIED, a.result); assertEquals(5L, a.acceptedRevision); assertEquals(7L, a.evaluatedRevision)
        assertEquals(Long.MAX_VALUE, a.resultElapsedMs); assertEquals(otro, a.bootId)
        CodecAckCp2.validar(a) // Relojes de contextos distintos no se comparan.
    }
}
