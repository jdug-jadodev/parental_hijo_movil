package dev.controlparental.child.recovery

import dev.controlparental.child.data.*
import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CoordinadorOfflineTest {
    private class Cola : EjecutorRecuperacionSerial {
        val trabajos = ArrayDeque<() -> Unit>()
        var rechazar = false
        override fun ejecutar(trabajo: () -> Unit) { check(!rechazar); trabajos.addLast(trabajo) }
        fun terminar() { while (trabajos.isNotEmpty()) trabajos.removeFirst().invoke() }
    }

    private class Laboratorio {
        val padre = OfflineRecoveryTest.identidadEfimera()
        val hijo = OfflineRecoveryTest.identidadEfimera()
        val archivo = ArchivoSimulado()
        var alEscribir: (() -> Unit)? = null
        val almacen = ChildStateStore(object : ArchivoEstadoAtomico by archivo {
            override fun escribirAtomico(bytes: ByteArray) { archivo.escribirAtomico(bytes); alEscribir?.invoke() }
        })
        val cola = Cola()
        var ahora = 100000L
        var boot = estadoConOrden().bootContext!!.bootId
        var alFirmar: (() -> Unit)? = null
        var ultimo: MensajeOffline? = null
        val codigo = "A".repeat(32) // Ficticio, solo test JVM; no registrar el texto.
        val identidad = object : FirmanteCp1 {
            override val clavePublica = hijo.clavePublica
            override fun firmarBytes(bytes: ByteArray): ByteArray {
                ultimo = CodecCp1.leerPayloadSinVerificar(bytes.copyOfRange("CPv1/OFFLINE\n".length, bytes.size),
                    Proposito.OFFLINE) as MensajeOffline
                return hijo.firmarBytes(bytes).also { alFirmar?.invoke() }
            }
        }
        init {
            val estado = estadoConOrden()
            val vinculo = estado.vinculo!!
            almacen.actualizar { estado.copy(policy = null, lastCommand = null,
                bootContext = estado.bootContext!!.copy(authorizedBootId = null),
                vinculo = vinculo.copy(parentPublicKeyB64 = padre.clavePublica.base64,
                    childPublicKeyB64 = hijo.clavePublica.base64, recoveryHashes = vinculo.recoveryHashes.copy(
                        MAINTENANCE = CodigoRecuperacion.hash(vinculo.pairId, PropositoRecuperacionLocal.MAINTENANCE, codigo)))) }
        }
        fun coordinador() = CoordinadorOffline(almacen, { ahora }, { ContextoRecuperacion(true, boot) }, identidad, cola)
        val c = coordinador()
        fun generar(): DesafioPresentadoOffline {
            c.generar(AccionOffline.MAINTENANCE); cola.terminar()
            return c.proyeccion().desafio!!
        }
        fun respuesta(mensaje: MensajeOffline = ultimo!!, firmante: FirmanteCp1 = padre) = CodecCp1.serializar(
            CriptografiaCp1.firmar(mensaje.copy(esRespuesta = true), Proposito.OFFLINE, Rol.PARENT, firmante)).toString(Charsets.US_ASCII)
        fun enviar(d: DesafioPresentadoOffline, texto: String = respuesta()) {
            c.responder(d.referencia, texto); cola.terminar()
        }
        fun estado() = (almacen.cargar() as CargaEstado.Disponible).estado
    }

    @Test fun `generacion es serial y lectura QR solo autoriza tras firma confiable`() {
        val l = Laboratorio()
        l.c.generar(AccionOffline.MAINTENANCE)
        assertTrue(l.c.proyeccion().ocupado)
        assertNull(l.c.proyeccion().desafio)
        l.cola.terminar()
        val d = l.c.proyeccion().desafio!!
        assertTrue(d.matriz.lado > 0)
        l.enviar(d, l.respuesta(firmante = l.hijo))
        assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, l.c.proyeccion().respuesta!!.resultado)
        assertNull(l.c.proyeccion().acceso)
        l.enviar(d)
        assertEquals(PropositoRecuperacionLocal.MAINTENANCE, l.c.proyeccion().acceso!!.proposito)
        assertNull(l.c.proyeccion().desafio)
        assertNull(l.estado().bootContext!!.authorizedBootId)
        assertTrue(l.estado().recoveryConsumed.isEmpty())
    }

    @Test fun `cancelar antes de iniciar descarta trabajo sin firmar`() {
        val l = Laboratorio()
        l.c.generar(AccionOffline.MAINTENANCE); l.c.cancelar(); l.cola.terminar()
        assertNull(l.ultimo)
        assertEquals(ProyeccionOffline(), l.c.proyeccion())
    }

    @Test fun `pausa durante firma retira QR y no deja respuesta utilizable`() {
        val l = Laboratorio()
        l.alFirmar = { l.c.cancelar() }
        l.c.generar(AccionOffline.MAINTENANCE); l.cola.terminar()
        assertEquals(ProyeccionOffline(), l.c.proyeccion())
        l.alFirmar = null
        val d = l.generar()
        l.c.responder(d.referencia, l.respuesta()); l.c.cancelar(); l.cola.terminar()
        assertNull(l.c.proyeccion().acceso)
    }

    @Test fun `nueva generacion durante firma no publica el resultado viejo`() {
        val l = Laboratorio()
        l.alFirmar = { l.alFirmar = null; l.c.generar(AccionOffline.RETIRE) }
        l.c.generar(AccionOffline.MAINTENANCE); l.cola.terminar()
        assertEquals(AccionOffline.RETIRE, l.ultimo!!.action)
        assertNotNull(l.c.proyeccion().desafio)
        assertNull(l.c.proyeccion().acceso)
    }

    @Test fun `ticket viejo y respuesta de nonce viejo no sirven con nuevo desafio`() {
        val l = Laboratorio()
        val viejo = l.generar(); val bytesViejos = l.respuesta()
        val nuevo = l.generar()
        l.enviar(viejo, bytesViejos)
        assertSame(nuevo.referencia, l.c.proyeccion().desafio!!.referencia)
        assertNull(l.c.proyeccion().respuesta)
        l.enviar(nuevo, bytesViejos)
        assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, l.c.proyeccion().respuesta!!.resultado)
        l.enviar(nuevo)
        assertNotNull(l.c.proyeccion().acceso)
    }

    @Test fun `recreacion RAM no conserva ticket desafio ni sesion`() {
        val l = Laboratorio()
        val d = l.generar(); val nuevo = l.coordinador()
        nuevo.responder(d.referencia, l.respuesta()); l.cola.terminar()
        assertEquals(ProyeccionOffline(), nuevo.proyeccion())
        l.enviar(d)
        assertEquals(ProyeccionOffline(), l.coordinador().proyeccion())
    }

    @Test fun `actualizacion retira desafio exactamente a cinco minutos`() {
        val l = Laboratorio()
        l.generar(); l.ahora += 299999
        l.c.actualizar(); l.cola.terminar()
        assertEquals(1L, l.c.proyeccion().desafio!!.restanteMs)
        l.ahora++; l.c.actualizar(); l.cola.terminar()
        assertNull(l.c.proyeccion().desafio)
        assertNull(l.c.proyeccion().acceso)
    }

    @Test fun `respuesta en frontera vencida no autoriza y no gasta codigo`() {
        val l = Laboratorio()
        val d = l.generar(); l.ahora += 300000
        l.enviar(d)
        assertEquals(ResultadoRecuperacion.DESAFIO_VENCIDO, l.c.proyeccion().respuesta!!.resultado)
        assertNull(l.c.proyeccion().acceso)
        assertTrue(l.estado().recoveryConsumed.isEmpty())
    }

    @Test fun `sesion se quita a cinco minutos y no muestra autorizacion antigua`() {
        val l = Laboratorio()
        l.enviar(l.generar()); l.ahora += 299999
        l.c.actualizar(); l.cola.terminar()
        assertEquals(1L, l.c.proyeccion().acceso!!.restanteMs)
        l.ahora++; l.c.actualizar(); l.cola.terminar()
        assertNull(l.c.proyeccion().acceso)
        assertNull(l.c.proyeccion().respuesta)
    }

    @Test fun `boot cambiado o reloj anterior al inicio invalidan desafio y acceso`() {
        for (cambiarBoot in listOf(false, true)) {
            val l = Laboratorio(); val d = l.generar()
            if (cambiarBoot) l.boot = "00000000-0000-4000-8000-000000000010" else l.ahora--
            l.enviar(d)
            assertNull(l.c.proyeccion().desafio)
            assertNull(l.c.proyeccion().acceso)
            assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.c.proyeccion().respuesta!!.resultado)
        }
    }

    @Test fun `espera persistida no impide respuesta OFFLINE ni se elimina`() {
        val l = Laboratorio()
        repeat(5) { l.c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, "invalido"); l.cola.terminar() }
        assertEquals(ResultadoRecuperacion.ESPERA, l.c.proyeccion().respuesta!!.resultado)
        l.enviar(l.generar())
        assertNotNull(l.c.proyeccion().acceso)
        assertEquals(5, l.estado().recoveryFailureCount)
        assertTrue(l.estado().esperaRecuperacionPendiente)
        assertTrue(l.estado().recoveryConsumed.isEmpty())
    }

    @Test fun `cancelacion durante IO de respuesta no publica permiso tardio`() {
        val l = Laboratorio(); val d = l.generar()
        l.alEscribir = { l.alEscribir = null; l.c.cancelar() }
        l.enviar(d)
        assertEquals(ProyeccionOffline(), l.c.proyeccion())
        l.c.actualizar(); l.cola.terminar()
        assertNull(l.c.proyeccion().acceso)
    }

    @Test fun `codigo se consume antes de publicar y cancelar durante commit no lo restaura`() {
        val l = Laboratorio()
        l.alEscribir = {
            assertNull(l.c.proyeccion().acceso)
            l.alEscribir = null; l.c.cancelar()
        }
        l.c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, l.codigo); l.cola.terminar()
        assertTrue(PropositoRecuperacionLocal.MAINTENANCE in l.estado().recoveryConsumed)
        assertEquals(ProyeccionOffline(), l.c.proyeccion())
        l.c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, l.codigo); l.cola.terminar()
        assertEquals(ResultadoRecuperacion.CODIGO_INVALIDO, l.c.proyeccion().respuesta!!.resultado)
        assertNull(l.c.proyeccion().acceso)
    }

    @Test fun `escritura fallida de codigo o QR no concede sesion`() {
        for (esQr in listOf(false, true)) {
            val l = Laboratorio(); val d = if (esQr) l.generar() else null
            l.archivo.falloEscritura = true
            if (d != null) l.enviar(d) else {
                l.c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, l.codigo); l.cola.terminar()
            }
            assertEquals(ResultadoRecuperacion.ERROR_SEGURO, l.c.proyeccion().respuesta!!.resultado)
            assertNull(l.c.proyeccion().acceso)
            assertTrue(l.estado().recoveryConsumed.isEmpty())
        }
    }

    @Test fun `cancelar en otro hilo no espera firma ni publica resultado al desbloquearla`() {
        val l = Laboratorio()
        val dentro = CountDownLatch(1); val continuar = CountDownLatch(1)
        val trabajador = Executors.newSingleThreadExecutor()
        try {
            val c = CoordinadorOffline(l.almacen, { l.ahora }, { ContextoRecuperacion(true, l.boot) },
                l.identidad, { trabajador.execute(it) })
            l.alFirmar = { dentro.countDown(); check(continuar.await(5, TimeUnit.SECONDS)) }
            c.generar(AccionOffline.MAINTENANCE)
            assertTrue(dentro.await(5, TimeUnit.SECONDS))
            c.cancelar()
            assertEquals(ProyeccionOffline(), c.proyeccion())
            continuar.countDown()
            trabajador.submit {}.get(5, TimeUnit.SECONDS)
            assertEquals(ProyeccionOffline(), c.proyeccion())
        } finally { continuar.countDown(); trabajador.shutdownNow() }
    }

    @Test fun `QR optico sintetico no evita la verificacion del controlador`() {
        val l = Laboratorio(); val d = l.generar(); val texto = l.respuesta()
        val matriz = com.google.zxing.qrcode.QRCodeWriter().encode(texto,
            com.google.zxing.BarcodeFormat.QR_CODE, 640, 640)
        val plano = ByteArray(640 * 640) { i -> if (matriz[i % 640, i / 640]) 0 else 255.toByte() }
        val bytes = QrOffline.decodificarRespuestaSinAutorizar(plano, 640, 640)
        assertNull(l.c.proyeccion().acceso)
        l.enviar(d, bytes.toString(Charsets.US_ASCII))
        assertNotNull(l.c.proyeccion().acceso)
    }

    @Test fun `rechazo de cola no reconstruye permiso tras una cancelacion`() {
        val l = Laboratorio(); l.enviar(l.generar())
        l.cola.rechazar = true
        l.c.cancelar()
        assertEquals(ProyeccionOffline(), l.c.proyeccion())
        l.cola.rechazar = false
        l.c.actualizar(); l.cola.terminar()
        assertNull(l.c.proyeccion().acceso)
    }
}
