package dev.controlparental.child.channel

import dev.controlparental.child.auth.*
import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature

class ClienteCanalCp2Test {
    private val id = "abcdefab-1234-4234-8234-abcdefabcdef"
    private val nonce = Base64Cp1.codificar(ByteArray(32))
    private inner class Lab {
        var tiempo = 0L
        val clave = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
        val publica = Base64Cp1.codificar(clave.public.encoded)
        var contexto: ContextoAuthHijo? = ContextoAuthHijo(id, publica, publica, "https://relay.example:443", "r1", 1)
        var mensajes = 0; val estados = mutableListOf<String>()
        val auth = ClienteAuthHijo({ tiempo }, { contexto }) { object : FirmanteAuthCp2 {
            override val publica get() = this@Lab.publica
            override fun firmar(bytes: ByteArray) = Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update(bytes); sign() }
        } }
        val canal = ClienteCanalCp2(auth, { _, _ -> mensajes++ }, estados::add)
        val enviados = mutableListOf<String>(); var cierres = 0; var fallaEnvio = false
        val socket = object : SocketCp2 {
            override fun enviar(texto: String): Boolean { enviados.add(texto); return !fallaEnvio }
            override fun cerrar() { cierres++ }
        }
        fun abrir(): GeneracionCanalCp2 = canal.iniciar()!!.also { canal.abierto(it, socket, "cp.v2") }
        fun autenticar(ref: GeneracionCanalCp2) {
            canal.recibir(ref, CodecAuthCp2.escribir(RetoAuthCp2(id, nonce)))
            val auth = CriptografiaAuthCp2.verificar(enviados.last().toByteArray(), publica, RolAuthCp2.CHILD)
            assertEquals(id, auth.prueba.pairId)
            canal.recibir(ref, CodecAuthCp2.escribir(OkAuthCp2(id)))
            assertTrue(canal.vigente(ref))
        }
        fun mensaje(): ByteArray {
            val p = CodecOrdenCp2.escribirPayload(SyncOrdenCp2(id, nonce, null))
            val s = Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update("CPv2/MESSAGE\n".toByteArray()); update(p); sign() }
            return CodecOrdenCp2.escribirSobre(SobreOrdenCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(s)))
        }
    }
    @Test fun destinoExactoWssWsSinQueryNiLanNiCleartext() {
        assertEquals("wss://relay.example:443/ws", DestinoCanalCp2.desdeOrigen("https://relay.example:443"))
        for (origen in listOf("http://relay.example", "https://relay.example/", "https://relay.example?x=1", "https://localhost",
            "https://192.168.1.2", "https://10.1.1.1", "https://maquina.local", "https://u@relay.example", "https://relay.example/#x"))
            assertThrows(Exception::class.java) { DestinoCanalCp2.desdeOrigen(origen) }
    }
    @Test fun authChildRealAntesDeDespacharMensajesYLatidosNoOtorganUso() {
        val l = Lab(); val ref = l.abrir(); l.autenticar(ref)
        l.canal.recibir(ref, CodecAuthCp2.escribir(LatidoAuthCp2(TipoLatidoCp2.PING, nonce)))
        assertEquals(TipoLatidoCp2.PONG, (CodecAuthCp2.leer(l.enviados.last().toByteArray()) as LatidoAuthCp2).kind)
        l.canal.recibir(ref, l.mensaje()) // Estructura válida, el guardia aún debe verificar firma y contexto.
        assertEquals(1, l.mensajes); assertTrue(l.canal.vigente(ref))
    }
    @Test fun sinSubprotocoloExactoNiAuthNoDespacha() {
        for (protocolo in listOf(null, "cp.v1", "cp.v2, cp.v1")) {
            val l = Lab(); val ref = l.canal.iniciar()!!; l.canal.abierto(ref, l.socket, protocolo)
            assertFalse(l.canal.vigente(ref)); assertFalse(ref.activa); assertEquals(0, l.mensajes)
        }
        val l = Lab(); val ref = l.abrir(); l.canal.recibir(ref, "{\"kind\":\"SIGNED\"}".toByteArray())
        assertFalse(ref.activa); assertEquals(0, l.mensajes)
    }
    @Test fun envioRechazadoTimeoutContextoYCierreInvalidanGeneracion() {
        for (caso in 0..3) {
            val l = Lab(); val ref = l.abrir()
            if (caso == 0) l.fallaEnvio = true
            l.canal.recibir(ref, CodecAuthCp2.escribir(RetoAuthCp2(id, nonce)))
            when (caso) { 1 -> l.tiempo = 10000; 2 -> l.contexto = l.contexto!!.copy(revisionRegistro = "r2"); 3 -> l.canal.cerrar() }
            l.canal.comprobar(); assertFalse(ref.activa); assertFalse(l.canal.vigente(ref)); assertEquals(0, l.mensajes)
        }
    }
    @Test fun callbackViejoNoCierraNuevaConexionNiReintentaGrant() {
        val l = Lab(); val antigua = l.abrir(); val nueva = l.abrir()
        assertFalse(antigua.activa); l.canal.fallo(antigua); assertTrue(nueva.activa)
        l.autenticar(nueva); l.canal.recibir(antigua, ByteArray(17000)); assertTrue(l.canal.vigente(nueva)); assertEquals(0, l.mensajes)
        l.canal.fallo(nueva); assertFalse(nueva.activa); assertEquals(0, l.mensajes)
    }
}
