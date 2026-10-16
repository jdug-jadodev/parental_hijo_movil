package dev.controlparental.child.pairing

import dev.controlparental.child.local.*
import dev.controlparental.protocol.*
import dev.controlparental.protocol.c8.*
import dev.controlparental.protocol.cp2.*
import okhttp3.*
import okio.ByteString
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.cert.CertificateException
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** C8-X01 opt-in: UN modelo CHILD JVM contra Node real y PARENT sintético; nunca dos APK. */
class CruceNodeC8Test {
    private class Nodo(ruta: String) : AutoCloseable {
        private val script = File(ruta, "test/soporte/interop-c8.mjs")
        private val proceso: Process
        private val salida = ArrayBlockingQueue<ObjetoJson>(8)
        private val fallo = AtomicBoolean()
        private val errores = AtomicInteger()
        val eventos = AtomicInteger()
        init {
            check(script.isFile) { "No existe el arnés autorizado en rutaRelay" }
            proceso = ProcessBuilder("node", script.absolutePath).start()
            Thread({
                try {
                    proceso.inputStream.bufferedReader(Charsets.UTF_8).use { lector ->
                        while (true) {
                            val linea = lector.readLine() ?: break
                            check(linea.length <= 16384)
                            val o = JsonCp1.leer(linea.toByteArray(Charsets.UTF_8), 16384) as ObjetoJson
                            eventos.incrementAndGet(); check(salida.offer(o))
                        }
                    }
                } catch (_: Exception) { fallo.set(true) }
            }, "c8-node-pipe").apply { isDaemon = true; start() }
            Thread({ proceso.errorStream.bufferedReader().use { it.forEachLine { errores.incrementAndGet() } } },
                "c8-node-stderr").apply { isDaemon = true; start() }
        }
        @Synchronized fun escribir(vararg campos: Pair<String, ValorJson>) {
            // No log, archivo ni excepción que incluya la línea (contiene material efímero).
            // PIPE no es wire CP1/C8: PEM necesita LF escapados que JsonCp1 prohíbe.
            fun textoPipe(s: String): String {
                check(s.all { it.code in 32..126 || it in "\n\r\t" })
                return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
            }
            val bytes = campos.joinToString(",", "{", "}") { (k, v) ->
                textoPipe(k) + ":" + when (v) {
                    is TextoJson -> textoPipe(v.texto)
                    is EnteroJson -> v.numero.toString()
                    else -> error("Tipo no previsto en pipe de laboratorio")
                }
            }.toByteArray(Charsets.US_ASCII)
            try { proceso.outputStream.write(bytes); proceso.outputStream.write(10); proceso.outputStream.flush() }
            finally { bytes.fill(0) }
        }
        fun esperar(evento: String): ObjetoJson {
            val o = salida.poll(12, TimeUnit.SECONDS) ?: error("Timeout de evento Node")
            check(!fallo.get() && (o.campos["event"] as? TextoJson)?.texto == evento) { "Evento Node no esperado" }
            return o
        }
        fun finalizar(): Int {
            escribir("event" to TextoJson("DONE")); esperar("DONE")
            check(proceso.waitFor(8, TimeUnit.SECONDS)) { "Node no terminó a tiempo" }
            check(proceso.exitValue() == 0 && !fallo.get() && errores.get() == 0) { "Node terminó con fallo" }
            return proceso.exitValue()
        }
        override fun close() {
            proceso.outputStream.close()
            if (proceso.isAlive) { proceso.destroy(); if (!proceso.waitFor(2, TimeUnit.SECONDS)) proceso.destroyForcibly() }
            proceso.inputStream.close(); proceso.errorStream.close()
        }
    }
    /** Puerto IO falso sólo test: guarda/readback reales de AlmacenPairCp2, no AtomicFile Android. */
    private class DiscoRam : ArchivosPairCp2 {
        var reservas = 0; var escrituras = 0
        private var reserva: ByteArray? = null; private var registro: ByteArray? = null
        override fun leerReserva() = reserva?.copyOf()
        override fun leerRegistro() = registro?.copyOf()
        override fun reservar(bytes: ByteArray) { check(reserva == null); reserva = bytes.copyOf(); reservas++ }
        override fun escribirRegistro(bytes: ByteArray) { check(registro == null); registro = bytes.copyOf(); escrituras++ }
    }
    @Test fun tlsEfimeroConfianzaAcotadaYHostnamePredeterminado() {
        val tls = TlsEfimeroC8()
        assertTrue(tls.certificado.subjectAlternativeNames.any { it[0] == 7 && it[1] == "127.0.0.1" })
        tls.gestor.checkServerTrusted(arrayOf(tls.certificado), "ECDHE_ECDSA")
        val global = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
            init(null as KeyStore?); trustManagers.filterIsInstance<X509TrustManager>().single()
        }
        assertThrows(CertificateException::class.java) { global.checkServerTrusted(arrayOf(tls.certificado), "ECDHE_ECDSA") }
        val cliente = OkHttpClient.Builder().sslSocketFactory(tls.contexto.socketFactory, tls.gestor).build()
        // Misma política predeterminada de OkHttp; no hostnameVerifier permisivo.
        assertEquals(OkHttpClient().hostnameVerifier, cliente.hostnameVerifier)
        cliente.dispatcher.executorService.shutdown()
    }
    @Test fun childJvmCruzaNodeRealPorTlsYGuardaPairOriginalSinUso() {
        val ruta = System.getProperty("rutaRelay")
        if (ruta.isNullOrBlank()) println("OMITIDA C8-X01 Node: requiere -PrutaRelay explícita; sin conexión ni proceso")
        assumeTrue("C8-X01 requiere -PrutaRelay explícita", !ruta.isNullOrBlank())
        val version = (System.getProperty("versionC8", "1") ?: "1").toInt()
        val duracion = CodecC8.duracion(version)
        val etiqueta = if (version == 2) "C8-T10" else "C8-X01"
        val tls = TlsEfimeroC8()
        val puerto = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { it.localPort }
        Nodo(ruta!!).use { nodo ->
            val init = listOf("role" to TextoJson("CHILD"), "port" to EnteroJson(puerto.toLong()),
                "certPem" to TextoJson(tls.certPem()), "keyPem" to TextoJson(tls.keyPem())) +
                (if (version == 2) listOf("version" to EnteroJson(2)) else emptyList())
            nodo.escribir(*init.toTypedArray())
            val ready = nodo.esperar("READY")
            fun ObjetoJson.texto(k: String) = (campos[k] as? TextoJson)?.texto ?: error("Campo de pipe ausente")
            val origen = ready.texto("origin")
            check(origen == "https://127.0.0.1:$puerto") { "Origen de laboratorio distinto" }
            val publicaPadreIndependiente = ready.texto("peerPublicKeyB64")
            ClavePublicaCp1.leer(publicaPadreIndependiente)
            val par = KeyPairGenerator.getInstance("EC").run { initialize(256); generateKeyPair() }
            val publicaHijo = Base64Cp1.codificar(par.public.encoded)
            val inicio = System.nanoTime()
            fun ahora() = (System.nanoTime() - inicio) / 1_000_000 + 1
            val sesion = SesionAdulto(::ahora, { 7 }, soloC8V2 = version == 2)
            sesion.reanudar()
            // Puerto adulto ficticio acreditado. No código real, ni acceso a credencial/Keystore Android.
            if (version == 1) {
                val entrar = sesion.solicitar(FinAdulto.ENTRAR)!!
                check(sesion.completar(entrar, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
            }
            fun ticket(fin: FinAdulto) = sesion.solicitar(fin)!!.also {
                check(sesion.autorizarParaLaboratorio(it))
            }
            val g = ticket(FinAdulto.GENERAR_OFERTA); val marco = sesion.marcoAcreditado(g)!!
            val vivo = AtomicBoolean(true); val finEsperado = AtomicBoolean()
            val modeloRef = AtomicReference<ClienteEncuentroC8>()
            val disco = DiscoRam(); val almacen = AlmacenPairCp2(disco)
            val ofertas = OfertasRam(::ahora, { ContextoOferta(7, publicaHijo) },
                { sesion.accionVigente(g) && almacen.cargar() is CargaPairCp2.Ausente }, vidaSesion = {
                    vivo.get() && sesion.marcoVigente(marco) && (modeloRef.get()?.let { ahora() < it.limiteVigencia } ?: true)
                }, marcoC8V2 = if (version == 2) marco else null)
            val ref = ofertas.generar(); val caso = ValidacionPairRam(ofertas, sesion)
            val recibida = ArrayBlockingQueue<ByteArray>(1); val fallo = AtomicBoolean()
            val publicaciones = AtomicInteger(); val auths = AtomicInteger(); val codigos = AtomicInteger(); val reclamaciones = AtomicInteger()
            val firmante = object : FirmanteAuthCp2 {
                override val publica = publicaHijo
                override fun firmar(bytes: ByteArray) = Signature.getInstance("SHA256withECDSA").run {
                    initSign(par.private); update(bytes); sign()
                }
            }
            val modelo = ClienteEncuentroC8(::ahora, origen, ref, marco.limite, { ofertas.vigente(ref) }, firmante,
                { codigo -> codigos.incrementAndGet(); nodo.escribir("event" to TextoJson("CLAIM"), "code" to TextoJson(codigo)) },
                { reclamaciones.incrementAndGet() }, { bytes -> check(recibida.offer(bytes)) }, {
                    vivo.set(false); ofertas.cancelar(); caso.cancelar(); sesion.cerrar()
                    if (!finEsperado.get()) fallo.set(true)
                }, version)
            modeloRef.set(modelo)
            val puente = OkHttpClient.Builder().sslSocketFactory(tls.contexto.socketFactory, tls.gestor)
                .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
                .addInterceptor { c -> c.proceed(c.request().newBuilder().removeHeader("Sec-WebSocket-Extensions").removeHeader("Origin").build()) }.build()
            val reloj = Executors.newSingleThreadScheduledExecutor()
            val ws = puente.newWebSocket(Request.Builder().url("wss://127.0.0.1:$puerto/pair")
                .header("Sec-WebSocket-Protocol", CodecC8.protocolo(version)).build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (response.headers.values("Sec-WebSocket-Extensions").isNotEmpty()) { fallo.set(true); modelo.cerrar(1008); return }
                    modelo.abierto(object : SocketC8 {
                        override fun enviar(bytes: ByteArray): Boolean {
                            val f = CodecC8.leer(bytes, version)
                            when (f.kind) {
                                    "AUTH" -> auths.incrementAndGet()
                                    "PUBLISH" -> {
                                        publicaciones.incrementAndGet()
                                        check(CodecC8.documento(f.texto("offerB64")).contentEquals(CodecOfertaCp2.escribir(ref.oferta)))
                                        check(f.numero("ttlMs") in 1..duracion && ahora() < marco.limite)
                                    }
                            }
                            return webSocket.send(bytes.toString(Charsets.US_ASCII))
                        }
                        override fun cerrar(codigo: Int) { webSocket.close(codigo, null) }
                    }, response.headers.values("Sec-WebSocket-Protocol").singleOrNull())
                }
                override fun onMessage(webSocket: WebSocket, text: String) { modelo.recibir(text.toByteArray(Charsets.UTF_8)) }
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) { fallo.set(true); modelo.cerrar(1008) }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { if (!finEsperado.get()) fallo.set(true); modelo.cerrar(1008) }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { modelo.cerrar(); webSocket.close(code, null) }
            })
            reloj.scheduleWithFixedDelay({ modelo.comprobar() }, 100, 100, TimeUnit.MILLISECONDS)
            try {
                val original = recibida.poll(12, TimeUnit.SECONDS) ?: error("No llegó candidata PAIR por WSS local")
                check(!fallo.get() && modelo.comprobar()) { "Canal/candidata ya no vigentes" }
                val enviado = nodo.esperar("PAIR_SENT")
                val hashFrame = CodecRegistroPair.sha(original)
                check(hashFrame == enviado.texto("pairSha256B64") && enviado.texto("parentPublicKeyB64") == publicaPadreIndependiente)
                val datos = DatosConfirmadosPair(publicaPadreIndependiente, CodecPairCp2.huellaPublica(publicaPadreIndependiente),
                    CodecPairCp2.huellaPublica(publicaHijo), origen) // Ancla READY por pipe, NO desde PAIR.
                check(caso.confirmar(ref, g, datos) == null) // GENERAR no es VALIDAR.
                val t = ticket(FinAdulto.VALIDAR_PAIR); val c = caso.confirmar(ref, t, datos) ?: error("Confirmación inválida")
                val v = caso.validar(c, original) ?: error("PAIR no validado")
                check(v.bytesFrame().contentEquals(original) && v.aceptacion.enrollId == ref.oferta.enrollId &&
                    v.aceptacion.enrollNonce == ref.oferta.enrollNonce && v.aceptacion.childPublicKeyB64 == publicaHijo &&
                    v.aceptacion.parentPublicKeyB64 == publicaPadreIndependiente && v.aceptacion.relayOrigin == origen)
                check(almacen.cargar() is CargaPairCp2.Ausente && marco.limite == sesion.marcoAcreditado(t)?.limite)
                val guardar = ticket(FinAdulto.GUARDAR_PAIR)
                check(almacen.guardar(v, ConsentimientoPair(datos, 7, ref.generacion, 9, ahora())) {
                    vivo.get() && ahora() < modelo.limiteVigencia && sesion.accionVigente(guardar) && caso.resultadoActual() === v
                } == ResultadoGuardarPair.GUARDADO)
                val s = (AlmacenPairCp2(disco).cargar(publicaPadreIndependiente) as CargaPairCp2.Guardado).snapshot
                check(s.integridad.bytesFrame().contentEquals(original) && s.integridad.payloadSha256B64 == v.payloadSha256B64 &&
                    s.estadoLocal == "VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION")
                check(disco.reservas == 1 && disco.escrituras == 1 && auths.get() == 1 && publicaciones.get() == 1 && codigos.get() == 1 && reclamaciones.get() == 1)
                check(!fallo.get() && modelo.comprobar())
                finEsperado.set(true); val exit = nodo.finalizar()
                modelo.cerrar(); check(caso.resultadoActual() == null && almacen.cargar() is CargaPairCp2.Guardado)
                original.fill(0)
                println("$etiqueta CHILD c8.v$version EJECUTADO NodeTLSreal exit=$exit eventos=${nodo.eventos.get()} AUTH=1 PUBLISH=1 CLAIM=1 PAIR=1 reserva=1 escritura=1")
                println("PAIRframeSha256B64=$hashFrame payloadSha256B64=${v.payloadSha256B64}; PENDIENTE/noUSO; contraparteNode sintética")
            } finally {
                finEsperado.set(true); modelo.cerrar(); ws.cancel(); reloj.shutdownNow()
                puente.connectionPool.evictAll(); puente.dispatcher.executorService.shutdown()
            }
        }
    }
    private fun SesionAdulto.autorizarParaLaboratorio(t: TicketAdulto) = autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9))
}
