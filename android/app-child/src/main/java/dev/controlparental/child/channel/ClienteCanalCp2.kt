package dev.controlparental.child.channel

import dev.controlparental.child.auth.*
import dev.controlparental.protocol.*
import dev.controlparental.protocol.cp2.*
import java.net.URI

internal interface SocketCp2 { fun enviar(texto: String): Boolean; fun cerrar() }
internal class GeneracionCanalCp2 internal constructor(internal val auth: ConexionAuthHijo, val url: String) {
    @Volatile var activa = true; internal set
}

internal object DestinoCanalCp2 {
    fun desdeOrigen(origen: String): String {
        CodecPairCp2.origenExacto(origen)
        val host = URI(origen).host
        // MAIN no acepta loopback/IP privada/LAN ni un origen sin hostname público TLS.
        require(host.contains('.') && host.any { it in 'a'..'z' } && !host.endsWith(".local") &&
            !host.endsWith(".localhost") && host != "localhost" && !host.matches(Regex("[0-9.]+")))
        return "wss://" + origen.removePrefix("https://") + "/ws"
    }
}

/** Serializado por el adaptador WSS. AUTH no se recorta y callbacks llevan referencia de generación. */
internal class ClienteCanalCp2(private val auth: ClienteAuthHijo,
    private val mensaje: (GeneracionCanalCp2, ByteArray) -> Unit, private val estado: (String) -> Unit) {
    private var generacion: GeneracionCanalCp2? = null
    private var socket: SocketCp2? = null
    private var abierto = false
    fun iniciar(): GeneracionCanalCp2? {
        cerrar()
        val ref = auth.iniciar() ?: run { estado("Pareja o identidad no disponible"); return null }
        val url = try { DestinoCanalCp2.desdeOrigen(ref.contexto.origen) } catch (_: Exception) { auth.cerrar(); estado("Origen TLS no apto"); return null }
        return GeneracionCanalCp2(ref, url).also { generacion = it; estado("Conectando TLS; todavía sin AUTH") }
    }
    fun abierto(ref: GeneracionCanalCp2, puerto: SocketCp2, protocolo: String?) {
        if (generacion !== ref) { puerto.cerrar(); return }
        if (abierto || protocolo != "cp.v2") { puerto.cerrar(); cerrar(); return }
        socket = puerto; abierto = true; estado("TLS abierto; esperando reto AUTH")
    }
    fun recibir(ref: GeneracionCanalCp2, bytes: ByteArray) {
        if (generacion !== ref) return
        if (!abierto || bytes.size !in 1..16384) { cerrar(); return }
        try {
            when (auth.estado(ref.auth)) {
                FaseAuthHijo.ESPERANDO_RETO -> {
                    val envio = auth.recibirReto(ref.auth, bytes) ?: error("Reto inválido")
                    check(auth.confirmarEnvio(ref.auth, envio, socket?.enviar(envio.bytesFrame().toString(Charsets.UTF_8)) == true))
                }
                FaseAuthHijo.ESPERANDO_OK -> {
                    check(auth.recibirOk(ref.auth, bytes)); estado("AUTH CHILD confirmado; no equivale a permiso de uso")
                }
                FaseAuthHijo.AUTENTICADO_TRANSPORTE -> {
                    val kind = SobreMessageCp2.tipoFrame(bytes)
                    when (kind) {
                        "PING" -> check(socket?.enviar((auth.responderPing(ref.auth, bytes) ?: error("PING inválido")).toString(Charsets.UTF_8)) == true)
                        "PONG" -> check(auth.recibirPong(ref.auth, bytes))
                        "SIGNED" -> { SobreMessageCp2.leer(bytes); mensaje(ref, bytes.copyOf()) } // El guardia verifica firma/rol/contexto.
                        "PEER_UNAVAILABLE" -> { SobreMessageCp2.leerPeerUnavailable(bytes); estado("Otro dispositivo sin conexión; no es ACK ni resultado de aplicación") }
                        else -> error("Frame no previsto")
                    }
                }
                else -> error("AUTH no disponible")
            }
        } catch (_: Exception) { cerrar() }
    }
    fun vigente(ref: GeneracionCanalCp2) = generacion === ref && abierto && auth.estado(ref.auth) == FaseAuthHijo.AUTENTICADO_TRANSPORTE
    fun enviar(ref: GeneracionCanalCp2, bytes: ByteArray): Boolean {
        if (!vigente(ref) || bytes.size !in 1..16384) return false
        val ok = socket?.enviar(bytes.toString(Charsets.UTF_8)) == true
        if (!ok) cerrar()
        return ok
    }
    fun comprobar() {
        val ref = generacion ?: return
        if (auth.estado(ref.auth) in setOf(FaseAuthHijo.CERRADO, FaseAuthHijo.NO_DISPONIBLE)) cerrar()
    }
    fun fallo(ref: GeneracionCanalCp2) { if (generacion === ref) cerrar() }
    fun cerrar() {
        val anterior = socket; socket = null; generacion?.activa = false; generacion = null; abierto = false; auth.cerrar()
        anterior?.cerrar(); estado("Canal cerrado; sin reintento ni renovación de permiso")
    }
}
