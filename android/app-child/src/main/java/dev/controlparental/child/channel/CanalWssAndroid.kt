package dev.controlparental.child.channel

import android.content.Context
import dev.controlparental.child.auth.AuthAndroid
import okhttp3.*
import okio.ByteString
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Callable por UI explícita, vida foreground. TLS/hostname/plataforma por defecto, sin CA ni trustAll. */
internal class CanalWssAndroid(c: Context, mensaje: (GeneracionCanalCp2, ByteArray) -> Unit,
    estado: (String) -> Unit) : AutoCloseable {
    private val actor = ScheduledThreadPoolExecutor(1).apply { removeOnCancelPolicy = true }
    private val cerrado = AtomicBoolean()
    private val pendientes = AtomicInteger()
    private val cliente = OkHttpClient.Builder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
            val direcciones = Dns.SYSTEM.lookup(hostname)
            check(direcciones.isNotEmpty() && direcciones.none { ip ->
                ip.isAnyLocalAddress || ip.isLoopbackAddress || ip.isLinkLocalAddress || ip.isSiteLocalAddress || ip.isMulticastAddress ||
                    (ip.address.size == 16 && (ip.address[0].toInt() and 0xfe) == 0xfc)
            })
            return direcciones
            }
        }).build()
    private val modelo = ClienteCanalCp2(AuthAndroid.crear(c), mensaje, estado)
    private var ws: WebSocket? = null
    private val reloj = actor.scheduleWithFixedDelay({ modelo.comprobar() }, 1, 1, TimeUnit.SECONDS)
    fun conectar() {
        if (cerrado.get()) return
        actor.execute {
            ws?.cancel(); ws = null
            val ref = modelo.iniciar() ?: return@execute
            val peticion = Request.Builder().url(ref.url).header("Sec-WebSocket-Protocol", "cp.v2").build()
            ws = cliente.newWebSocket(peticion, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (cerrado.get()) { webSocket.cancel(); return }
                    actor.execute { modelo.abierto(ref, object : SocketCp2 {
                        override fun enviar(texto: String) = webSocket.send(texto)
                        override fun cerrar() { webSocket.close(1000, null); webSocket.cancel() }
                    }, response.headers.values("Sec-WebSocket-Protocol").singleOrNull()) }
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (cerrado.get()) return
                    if (text.length > 16384) { webSocket.cancel(); actor.execute { modelo.fallo(ref) }; return }
                    if (pendientes.incrementAndGet() > 64) {
                        pendientes.decrementAndGet(); webSocket.cancel(); actor.execute { modelo.fallo(ref) }; return
                    }
                    actor.execute {
                        try { modelo.recibir(ref, text.toByteArray(Charsets.UTF_8)) } finally { pendientes.decrementAndGet() }
                    }
                }
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) { webSocket.cancel(); fallo() }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { fallo() }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { fallo() }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null); fallo() }
                private fun fallo() { if (!cerrado.get()) actor.execute { modelo.fallo(ref) } }
            })
        }
    }
    fun enviar(ref: GeneracionCanalCp2, bytes: ByteArray): Boolean {
        if (cerrado.get() || !ref.activa || bytes.size !in 1..16384) return false
        val copia = bytes.copyOf()
        return try { actor.execute { modelo.enviar(ref, copia) }; true } catch (_: java.util.concurrent.RejectedExecutionException) { false }
    }
    override fun close() {
        if (!cerrado.compareAndSet(false, true)) return
        reloj.cancel(false)
        actor.execute { modelo.cerrar(); ws?.cancel(); cliente.connectionPool.evictAll(); cliente.dispatcher.executorService.shutdown() }
        actor.shutdown()
    }
}
