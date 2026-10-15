package dev.controlparental.child.pairing

import android.os.SystemClock
import dev.controlparental.protocol.cp2.*
import dev.controlparental.child.security.FirmanteAndroidKeystore
import android.content.Context
import okhttp3.*
import okio.ByteString
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** WSS real, sin reintentos ni fallback a cp.v2/bootstrap/QR. Sólo sesión adulta foreground. */
internal class CanalEncuentroC8Android(c: Context, origen: String, ref: ReferenciaOferta, marcoLimite: Long,
    vigente: () -> Boolean, codigo: (String) -> Unit, reclamada: () -> Unit,
    candidata: (ByteArray) -> Unit, terminada: () -> Unit) : AutoCloseable {
    private val actor = ScheduledThreadPoolExecutor(1).apply { removeOnCancelPolicy = true }
    private val cerrado = AtomicBoolean()
    private val vivo = AtomicBoolean(true)
    val disponible get() = vivo.get() && !cerrado.get()
    private val pendientes = AtomicInteger()
    private var ws: WebSocket? = null
    private val cliente = OkHttpClient.Builder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false)
        .addInterceptor { cadena -> cadena.proceed(cadena.request().newBuilder().removeHeader("Sec-WebSocket-Extensions").build()) }
        .dns(object : Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> = Dns.SYSTEM.lookup(hostname).also { ips ->
                check(ips.isNotEmpty() && ips.none { ip -> ip.isAnyLocalAddress || ip.isLoopbackAddress || ip.isLinkLocalAddress ||
                    ip.isSiteLocalAddress || ip.isMulticastAddress || (ip.address.size == 16 && (ip.address[0].toInt() and 0xfe) == 0xfc) })
            }
        }).build()
    private val existente = FirmanteAndroidKeystore(c.applicationContext, IdentidadOfertaAndroid.ALIAS)
    private val modelo = ClienteEncuentroC8(SystemClock::elapsedRealtime, origen, ref, marcoLimite,
        { disponible && vigente() }, object : FirmanteAuthCp2 {
            override val publica get() = existente.clavePublica.base64
            override fun firmar(bytes: ByteArray): ByteArray {
                check(disponible && vigente() && publica == ref.contexto.identidad)
                return existente.firmarBytes(bytes).also { check(disponible && vigente() && publica == ref.contexto.identidad) }
            }
        }, codigo, reclamada, candidata, { vivo.set(false); terminada(); close() })
    private var reloj: java.util.concurrent.ScheduledFuture<*>? = null
    private val iniciada = AtomicBoolean()
    private fun ejecutar(accion: () -> Unit) {
        if (cerrado.get()) return
        try { actor.execute { if (!cerrado.get()) accion() } } catch (_: java.util.concurrent.RejectedExecutionException) { /* Cerrado. */ }
    }
    init { CodecPairCp2.origenExacto(origen) }
    private val url = "wss://" + origen.removePrefix("https://") + "/pair"
    fun conectar() {
        if (!iniciada.compareAndSet(false, true)) return
        ejecutar {
            if (!modelo.comprobar()) return@ejecutar
            reloj = actor.scheduleWithFixedDelay({ modelo.comprobar() }, 100, 100, TimeUnit.MILLISECONDS)
            ws = cliente.newWebSocket(Request.Builder().url(url).header("Sec-WebSocket-Protocol", "c8.v1").build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (!disponible) { webSocket.cancel(); return }
                    ejecutar {
                        if (response.headers.values("Sec-WebSocket-Extensions").isNotEmpty()) { modelo.cerrar(1008); return@ejecutar }
                        modelo.abierto(object : SocketC8 {
                            override fun enviar(bytes: ByteArray): Boolean = disponible && bytes.size <= 16384 &&
                                webSocket.queueSize() + bytes.size <= 65536 && webSocket.send(bytes.toString(Charsets.UTF_8))
                            override fun cerrar(codigo: Int) { webSocket.close(codigo, null); webSocket.cancel() }
                        }, response.headers.values("Sec-WebSocket-Protocol").singleOrNull())
                    }
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!disponible) return
                    if (text.length > 16384 || text.any { it.code !in 32..126 } || pendientes.incrementAndGet() > 64) { fallo(); return }
                    ejecutar { try { modelo.recibir(text.toByteArray(Charsets.US_ASCII)) } finally { pendientes.decrementAndGet() } }
                }
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) { fallo() }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { fallo() }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { fallo() }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { fallo() }
                private fun fallo() { vivo.set(false); ejecutar { modelo.cerrar(1008) } }
            })
        }
    }
    override fun close() {
        vivo.set(false)
        if (!cerrado.compareAndSet(false, true)) return
        reloj?.cancel(false)
        actor.execute {
            modelo.cerrar(); ws?.cancel(); ws = null
            cliente.connectionPool.evictAll(); cliente.dispatcher.executorService.shutdown()
        }
        actor.shutdown()
    }
}
