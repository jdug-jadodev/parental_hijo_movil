package dev.controlparental.child.auth

import dev.controlparental.protocol.cp2.*

internal data class ContextoAuthHijo(val pairId: String, val publicaHijo: String, val publicaPadre: String,
    val origen: String, val revisionRegistro: String, val boot: Int)
internal class ConexionAuthHijo internal constructor(internal val contexto: ContextoAuthHijo)
internal class EnvioAuthHijo internal constructor(internal val conexion: ConexionAuthHijo, bytes: ByteArray) {
    private val originales = bytes.copyOf()
    fun bytesFrame() = originales.copyOf()
}
internal enum class FaseAuthHijo { CERRADO, NO_DISPONIBLE, ESPERANDO_RETO, FIRMANDO, PENDIENTE_ENVIO, ESPERANDO_OK, AUTENTICADO_TRANSPORTE }

/** Sin sockets/enforcer/autoridad adulta/ALLOW. Un objeto por cliente; callbacks ligados a referencia RAM. */
internal class ClienteAuthHijo(private val ahora: () -> Long, private val contexto: () -> ContextoAuthHijo?,
    private val firmante: (ContextoAuthHijo) -> FirmanteAuthCp2) {
    private var conexion: ConexionAuthHijo? = null
    private var fase = FaseAuthHijo.CERRADO
    private var envio: EnvioAuthHijo? = null
    private var reto: RetoAuthCp2? = null
    private var limite = 0L
    private var ultimo = -1L
    private var epoca = Any()
    private var pingPendiente: String? = null
    private val ids = mutableSetOf<String>()
    private val nonces = mutableSetOf<String>() // Máximo64 retos; jamás se persisten ni se registran.
    @Synchronized fun cerrar() { epoca = Any(); conexion = null; envio = null; reto = null; pingPendiente = null; limite = 0; fase = FaseAuthHijo.CERRADO }
    private fun disponible() = try { contexto() } catch (_: Exception) { null }
    private fun vigente(ref: ConexionAuthHijo): Boolean {
        if (conexion !== ref) return false
        val n = ahora(); val c = disponible()
        // Comparar referencia DESPUÉS de puertos: un callback reentrante puede iniciar otra generación.
        if (conexion !== ref) return false
        val valida = n >= 0 && n >= ultimo && c == ref.contexto &&
            (reto == null || fase == FaseAuthHijo.AUTENTICADO_TRANSPORTE || n < limite)
        ultimo = n
        if (!valida) { cerrar(); fase = FaseAuthHijo.NO_DISPONIBLE }
        return valida
    }
    /** Intención explícita del futuro transporte; no abre red, no se llama al boot/panel. */
    @Synchronized fun iniciar(): ConexionAuthHijo? {
        cerrar()
        val token = epoca
        val c = disponible()
        if (epoca !== token) return null
        if (c == null) { fase = FaseAuthHijo.NO_DISPONIBLE; return null }
        val n = ahora()
        if (n < 0 || n < ultimo || c.boot < 0) { fase = FaseAuthHijo.NO_DISPONIBLE; return null }
        ultimo = n
        return ConexionAuthHijo(c).also { conexion = it; fase = FaseAuthHijo.ESPERANDO_RETO }
    }
    @Synchronized fun estado(ref: ConexionAuthHijo): FaseAuthHijo =
        if (vigente(ref)) fase else if (conexion == null) fase else FaseAuthHijo.CERRADO

    /** Emisión fuera de monitor; revalidación posterior impide publicar firma de generación/contexto viejos. */
    fun recibirReto(ref: ConexionAuthHijo, bytes: ByteArray): EnvioAuthHijo? {
        val r = synchronized(this) {
            if (!vigente(ref)) return null
            if (fase != FaseAuthHijo.ESPERANDO_RETO) { cerrar(); return null }
            val r = try { CodecAuthCp2.leer(bytes) as? RetoAuthCp2 } catch (_: Exception) { null }
            if (r == null || r.connectionId in ids || r.nonce in nonces || ids.size >= 64) { cerrar(); return null }
            ids.add(r.connectionId); nonces.add(r.nonce)
            reto = r; limite = try { Math.addExact(ultimo, 10_000) } catch (_: Exception) { cerrar(); return null }
            fase = FaseAuthHijo.FIRMANDO; r
        }
        val firmado = try {
            CriptografiaAuthCp2.firmarHijo(PruebaAuthCp2(ref.contexto.pairId, RolAuthCp2.CHILD, r.connectionId, r.nonce),
                ref.contexto.publicaHijo, firmante(ref.contexto))
        } catch (_: Exception) { null }
        return synchronized(this) {
            if (!vigente(ref)) return@synchronized null
            if (firmado == null || fase != FaseAuthHijo.FIRMANDO || reto !== r) { cerrar(); return@synchronized null }
            EnvioAuthHijo(ref, firmado).also { envio = it; fase = FaseAuthHijo.PENDIENTE_ENVIO }
        }
    }
    /** El transporte confirma envío de ESTE ticket; crear bytes no equivale a enviarlos. */
    @Synchronized fun confirmarEnvio(ref: ConexionAuthHijo, ticket: EnvioAuthHijo, exito: Boolean): Boolean {
        if (!vigente(ref)) return false
        if (!exito || ticket !== envio || ticket.conexion !== ref || fase != FaseAuthHijo.PENDIENTE_ENVIO) { cerrar(); return false }
        envio = null; fase = FaseAuthHijo.ESPERANDO_OK; return true
    }
    @Synchronized fun recibirOk(ref: ConexionAuthHijo, bytes: ByteArray): Boolean {
        if (!vigente(ref)) return false
        val ok = try { CodecAuthCp2.leer(bytes) as? OkAuthCp2 } catch (_: Exception) { null }
        if (fase != FaseAuthHijo.ESPERANDO_OK || ok == null || ok.connectionId != reto?.connectionId) { cerrar(); return false }
        fase = FaseAuthHijo.AUTENTICADO_TRANSPORTE; return true
    }
    @Synchronized fun crearPing(ref: ConexionAuthHijo, nonce: String): ByteArray? {
        if (!vigente(ref) || fase != FaseAuthHijo.AUTENTICADO_TRANSPORTE || pingPendiente != null) return null
        return try { CodecAuthCp2.escribir(LatidoAuthCp2(TipoLatidoCp2.PING, nonce)).also { pingPendiente = nonce } }
            catch (_: Exception) { cerrar(); null }
    }
    @Synchronized fun recibirPong(ref: ConexionAuthHijo, bytes: ByteArray): Boolean {
        if (!vigente(ref)) return false
        val f = try { CodecAuthCp2.leer(bytes) as? LatidoAuthCp2 } catch (_: Exception) { null }
        if (fase != FaseAuthHijo.AUTENTICADO_TRANSPORTE || f?.kind != TipoLatidoCp2.PONG || f.nonce != pingPendiente) { cerrar(); return false }
        pingPendiente = null; return true
    }
    @Synchronized fun responderPing(ref: ConexionAuthHijo, bytes: ByteArray): ByteArray? {
        if (!vigente(ref)) return null
        val f = try { CodecAuthCp2.leer(bytes) as? LatidoAuthCp2 } catch (_: Exception) { null }
        if (fase != FaseAuthHijo.AUTENTICADO_TRANSPORTE || f?.kind != TipoLatidoCp2.PING) { cerrar(); return null }
        return CodecAuthCp2.escribir(LatidoAuthCp2(TipoLatidoCp2.PONG, f.nonce))
    }
}
