package dev.controlparental.child.pairing

import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import dev.controlparental.protocol.c8.*

internal interface SocketC8 {
    fun enviar(bytes: ByteArray): Boolean
    fun cerrar(codigo: Int)
}
internal enum class FaseC8 { RETO, FIRMANDO, AUTH, PUBLICADA, CODIGO, RECLAMADA, CANDIDATA, CERRADA }

/** Autoridad externa siempre vigente. Sólo transporte/candidata; no valida consentimiento ni guarda. */
internal class ClienteEncuentroC8(private val ahora: () -> Long, private val origen: String,
    referencia: ReferenciaOferta, private val marcoLimite: Long,
    vigente: () -> Boolean, firmante: FirmanteAuthCp2,
    private val codigo: (String) -> Unit, private val reclamada: () -> Unit,
    private val candidata: (ByteArray) -> Unit, private val terminada: () -> Unit) {
    var fase = FaseC8.RETO; private set
    private var referenciaActual: ReferenciaOferta? = referencia
    private var firmanteActual: FirmanteAuthCp2? = firmante
    private var autoridadActual: (() -> Boolean)? = vigente
    private var socket: SocketC8? = null
    private var retoRecibido = -1L
    private var ultimo = ahora()
    @Volatile private var limite = minOf(marcoLimite, referencia.limite, Math.addExact(ultimo, 10_000))
    /** Lectura sin cerrojo para guardas de commit; evita inversión modelo→ofertas→modelo. */
    val limiteVigencia get() = limite
    private var publicaPadre: String? = null
    private var tokens = 20L
    private var reposicion = ultimo
    init { CodecPairCp2.origenExacto(origen) }
    @Synchronized fun abierto(s: SocketC8, protocolo: String?) {
        if (fase == FaseC8.CERRADA) { s.cerrar(1008); return }
        if (socket != null || protocolo != "c8.v1") { s.cerrar(1008); cerrar(); return }
        socket = s; comprobar()
    }
    @Synchronized fun comprobar(): Boolean {
        if (fase == FaseC8.CERRADA) return false
        val n = ahora()
        val valida = try { n >= ultimo && n < limite && autoridadActual?.invoke() == true } catch (_: Exception) { false }
        ultimo = n
        if (!valida) cerrar()
        return valida
    }
    private fun enviar(f: FrameC8) {
        check(comprobar() && socket?.enviar(CodecC8.escribir(f)) == true)
    }
    @Synchronized fun recibir(bytes: ByteArray) {
        if (!comprobar()) return
        try {
            check(socket != null)
            val referencia = referenciaActual ?: error("Oferta cerrada")
            val n = ahora()
            val intervalos = (n - reposicion) / 100
            tokens = minOf(20, tokens + intervalos); reposicion += intervalos * 100
            check(tokens > 0); tokens--
            val f = CodecC8.leer(bytes)
            if (f.kind == "UNAVAILABLE") { cerrar(); return }
            when (fase) {
                FaseC8.RETO -> {
                    check(f.kind == "CHALLENGE" && f.texto("relayOrigin") == origen)
                    retoRecibido = ahora(); limite = minOf(marcoLimite, referencia.limite, Math.addExact(retoRecibido, 10_000))
                    fase = FaseC8.FIRMANDO // Consumir ANTES de firmar: no replay/reentrada de reto.
                    val p = PruebaC8(f.texto("connectionId"), f.texto("nonceB64"), referencia.contexto.identidad, origen, "CHILD")
                    val auth = CriptografiaC8.firmarHijo(p, firmanteActual ?: error("Identidad no disponible"))
                    check(fase == FaseC8.FIRMANDO && comprobar()); fase = FaseC8.AUTH; enviar(auth)
                }
                FaseC8.AUTH -> {
                    check(f.kind == "READY" && retoRecibido >= 0 && ahora() - retoRecibido < 10_000)
                    val nPublicacion = ahora() // DESPUÉS de CHALLENGE, inmediatamente antes de PUBLISH.
                    check(comprobar())
                    limite = minOf(marcoLimite, referencia.limite, Math.addExact(nPublicacion, 120_000), Math.addExact(retoRecibido, 120_000))
                    val ttl = limite - nPublicacion; check(ttl in 1..120_000)
                    fase = FaseC8.PUBLICADA
                    enviar(CodecC8.crear("PUBLISH", mapOf("offerB64" to Base64Cp1.codificar(CodecOfertaCp2.escribir(referencia.oferta))), mapOf("ttlMs" to ttl)))
                }
                FaseC8.PUBLICADA -> {
                    check(f.kind == "CODE"); acotar(f.numero("remainingMs")); fase = FaseC8.CODIGO
                    codigo(f.texto("code"))
                }
                FaseC8.CODIGO -> {
                    check(f.kind == "CLAIMED"); acotar(f.numero("remainingMs"))
                    val publica = f.texto("parentPublicKeyB64"); check(publica != referencia.contexto.identidad)
                    publicaPadre = publica; fase = FaseC8.RECLAMADA; reclamada()
                }
                FaseC8.RECLAMADA -> {
                    check(f.kind == "PAIR")
                    val originales = CodecC8.documento(f.texto("pairB64"))
                    val p = CriptografiaPairCp2.verificarIntegridad(originales, publicaPadre!!).aceptacion
                    check(p.parentPublicKeyB64 == publicaPadre && p.childPublicKeyB64 == referencia.contexto.identidad &&
                        p.enrollId == referencia.oferta.enrollId && p.enrollNonce == referencia.oferta.enrollNonce && p.relayOrigin == origen)
                    check(comprobar()); fase = FaseC8.CANDIDATA
                    try { candidata(originales.copyOf()) } finally { originales.fill(0) }
                }
                else -> error("Orden C8 no prevista")
            }
        } catch (_: Exception) { cerrar(1008) }
    }
    private fun acotar(restante: Long) {
        check(restante > 0)
        limite = minOf(limite, Math.addExact(ahora(), restante)); check(comprobar())
    }
    @Synchronized fun cerrar(codigoCierre: Int = 1000) {
        if (fase == FaseC8.CERRADA) return
        limite = 0 // Invalida guardas de commit antes de cerrar socket o publicar callbacks.
        val s = socket; fase = FaseC8.CERRADA; socket = null; publicaPadre = null
        referenciaActual = null; firmanteActual = null; autoridadActual = null
        if (codigoCierre == 1000) try { s?.enviar(CodecC8.escribir(CodecC8.crear("CANCEL"))) } catch (_: Exception) { /* Sin reintento. */ }
        try { s?.cerrar(codigoCierre) } finally { terminada() }
    }
}
