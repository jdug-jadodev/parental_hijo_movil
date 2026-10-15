package dev.controlparental.child.auth

import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class ClienteAuthHijoTest {
    private val id = "12345678-1234-4234-8234-123456789abc"
    private val nonce = Base64Cp1.codificar(ByteArray(32) { 9 })
    private val reto get() = CodecAuthCp2.escribir(RetoAuthCp2(id, nonce))
    private val ok get() = CodecAuthCp2.escribir(OkAuthCp2(id))
    private inner class Lab {
        var tiempo = 10L
        val clave = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
        val publica = Base64Cp1.codificar(clave.public.encoded)
        var contexto: ContextoAuthHijo? = ContextoAuthHijo(id, publica, publica, "https://relay.example:443", "revision1", 5)
        var alFirmar: () -> Unit = {}
        var alLeer: () -> Unit = {}
        var firmaErronea = false
        val cliente = nuevo()
        fun nuevo() = ClienteAuthHijo({ tiempo }, { alLeer(); contexto }) {
            object : FirmanteAuthCp2 {
                override val publica get() = this@Lab.publica
                override fun firmar(bytes: ByteArray): ByteArray {
                    alFirmar(); if (firmaErronea) error("Keystore simulado no disponible")
                    return Signature.getInstance("SHA256withECDSA").run { initSign(clave.private); update(bytes); sign() }
                }
            }
        }
        fun autenticar(): ConexionAuthHijo {
            val r = cliente.iniciar()!!; val e = cliente.recibirReto(r, reto)!!
            check(cliente.confirmarEnvio(r, e, true)); check(cliente.recibirOk(r, ok)); return r
        }
    }
    @Test fun emiteChildRealLigadoSnapshotYTicketEnvioAntesDeOk() {
        val l = Lab(); val ref = l.cliente.iniciar()!!; val envio = l.cliente.recibirReto(ref, reto)!!
        val p = CriptografiaAuthCp2.verificar(envio.bytesFrame(), l.publica, RolAuthCp2.CHILD).prueba
        assertEquals(PruebaAuthCp2(id, RolAuthCp2.CHILD, id, nonce), p)
        val copia = envio.bytesFrame(); copia.fill(0); assertNotEquals(0.toByte(), envio.bytesFrame()[0])
        assertEquals(FaseAuthHijo.PENDIENTE_ENVIO, l.cliente.estado(ref))
        assertTrue(l.cliente.confirmarEnvio(ref, envio, true)); assertTrue(l.cliente.recibirOk(ref, ok))
        assertEquals(FaseAuthHijo.AUTENTICADO_TRANSPORTE, l.cliente.estado(ref)) // No ALLOW/APPLIED/recuperación.
    }
    @Test fun okPrematuroEnvioFallidoYOkAjenoCierran() {
        for (caso in 0..2) {
            val l = Lab(); val r = l.cliente.iniciar()!!; val e = l.cliente.recibirReto(r, reto)!!
            when (caso) {
                0 -> assertFalse(l.cliente.recibirOk(r, ok))
                1 -> assertFalse(l.cliente.confirmarEnvio(r, e, false))
                2 -> { l.cliente.confirmarEnvio(r, e, true); assertFalse(l.cliente.recibirOk(r, CodecAuthCp2.escribir(OkAuthCp2("abcdefab-1234-4234-8234-abcdefabcdef")))) }
            }
            assertNotEquals(FaseAuthHijo.AUTENTICADO_TRANSPORTE, l.cliente.estado(r))
        }
    }
    @Test fun caducaDiezSegundosRelojRetrocedeYCambioSnapshotBootIdentidadRevision() {
        for (caso in 0..5) {
            val l = Lab(); val r = l.cliente.iniciar()!!; val e = l.cliente.recibirReto(r, reto)!!
            when (caso) {
                0 -> l.tiempo += 10000
                1 -> l.tiempo--
                2 -> l.contexto = l.contexto!!.copy(boot = 6)
                3 -> l.contexto = l.contexto!!.copy(publicaHijo = "otra")
                4 -> l.contexto = l.contexto!!.copy(revisionRegistro = "revision2")
                5 -> l.contexto = null
            }
            assertFalse(l.cliente.confirmarEnvio(r, e, true)); assertFalse(l.cliente.recibirOk(r, ok))
        }
    }
    @Test fun callbackViejoNoTocaConexionNuevaYReplayRetoNoReabre() {
        val l = Lab(); val vieja = l.cliente.iniciar()!!; val envio = l.cliente.recibirReto(vieja, reto)!!
        val nueva = l.cliente.iniciar()!!
        assertFalse(l.cliente.confirmarEnvio(vieja, envio, true)); assertFalse(l.cliente.recibirOk(vieja, ok))
        assertEquals(FaseAuthHijo.ESPERANDO_RETO, l.cliente.estado(nueva))
        assertNull(l.cliente.recibirReto(nueva, reto)); assertNotEquals(FaseAuthHijo.AUTENTICADO_TRANSPORTE, l.cliente.estado(nueva))
    }
    @Test fun reentranciaFirmaCierreExpiracionYCambioGeneracionDescartan() {
        for (caso in 0..3) {
            val l = Lab(); val r = l.cliente.iniciar()!!
            l.alFirmar = { when (caso) { 0 -> l.cliente.cerrar(); 1 -> l.tiempo += 10000; 2 -> l.cliente.iniciar(); 3 -> l.contexto = null } }
            assertNull(l.cliente.recibirReto(r, reto))
        }
    }
    @Test fun estadoNoSeRestauraTrasProcesoYFirmaErroneaNoProduceTicket() {
        val l = Lab(); val r = l.cliente.iniciar()!!; l.firmaErronea = true
        assertNull(l.cliente.recibirReto(r, reto)); assertFalse(l.cliente.recibirOk(r, ok))
        val otro = l.nuevo(); assertFalse(otro.recibirOk(r, ok)); assertEquals(FaseAuthHijo.CERRADO, otro.estado(r))
        l.contexto = null; assertNull(otro.iniciar()); assertEquals(FaseAuthHijo.NO_DISPONIBLE, otro.estado(r))
    }
    @Test fun latidosNoAutenticanNiRenuevanRetoYPongExigeNoncePendiente() {
        val l = Lab(); val r = l.cliente.iniciar()!!
        val ping = CodecAuthCp2.escribir(LatidoAuthCp2(TipoLatidoCp2.PING, nonce))
        assertNull(l.cliente.responderPing(r, ping)); assertFalse(l.cliente.recibirOk(r, ok))
        val nuevo = l.autenticar(); val enviado = l.cliente.crearPing(nuevo, nonce)!!
        val pong = l.cliente.responderPing(nuevo, enviado)!!
        assertTrue(l.cliente.recibirPong(nuevo, pong)); assertFalse(l.cliente.recibirPong(nuevo, pong))
    }
    @Test fun reentranciaDeFuenteNoSobrescribeGeneracionNueva() {
        val l = Lab(); var nueva: ConexionAuthHijo? = null
        l.alLeer = { l.alLeer = {}; nueva = l.cliente.iniciar() }
        assertNull(l.cliente.iniciar()); assertNotNull(nueva)
        assertEquals(FaseAuthHijo.ESPERANDO_RETO, l.cliente.estado(nueva!!))
    }
}
