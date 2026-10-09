package dev.controlparental.child.recovery

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test

class QrOfflineTest {
    private fun recurso(nombre: String) = javaClass.classLoader!!.getResourceAsStream(nombre)!!.use { it.readBytes() }
    private fun sobre(nombre: String) = CodecCp1.leerDocumentoSinAutorizar(recurso(nombre)) as SobreSinVerificar
    private fun textoRespuesta() = CodecCp1.serializar(sobre("09_offline_response.signed.json")).toString(Charsets.US_ASCII)

    @Test fun `desafio tiene margen blanco de cuatro modulos`() {
        val qr = QrOffline.crearDesafio(sobre("08_offline_challenge.signed.json"))
        assertTrue(qr.lado > 8)
        var negros = 0
        for (y in 0 until qr.lado) for (x in 0 until qr.lado) {
            if (x < 4 || y < 4 || x >= qr.lado - 4 || y >= qr.lado - 4) assertFalse(qr.esNegro(x, y))
            if (qr.esNegro(x, y)) negros++
        }
        assertTrue(negros > 0)
    }

    @Test fun `respuesta optica conserva exactamente los bytes del sobre`() {
        val texto = textoRespuesta()
        val imagen = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 640, 640,
            mapOf(EncodeHintType.MARGIN to 4))
        val plano = ByteArray(640 * 640) { i -> if (imagen[i % 640, i / 640]) 0 else 255.toByte() }
        assertArrayEquals(texto.toByteArray(Charsets.US_ASCII), QrOffline.decodificarRespuestaSinAutorizar(plano, 640, 640))
    }

    @Test fun `texto leido no es una autorizacion y firma alterada sigue necesitando verificacion`() {
        val original = sobre("09_offline_response.signed.json")
        val alterado = original.copy(sigB64 = Base64Cp1.codificar(ByteArray(70)))
        val bytes = CodecCp1.serializar(alterado)
        assertArrayEquals(bytes, QrOffline.leerRespuestaSinAutorizar(bytes.toString(Charsets.US_ASCII)))
        // El transporte no acredita una firma: devuelve bytes, nunca sesión o MensajeVerificado.
    }

    @Test fun `rechaza desafio como respuesta y respuesta como desafio`() {
        assertThrows(IllegalArgumentException::class.java) {
            QrOffline.crearDesafio(sobre("09_offline_response.signed.json"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            QrOffline.leerRespuestaSinAutorizar(CodecCp1.serializar(sobre("08_offline_challenge.signed.json")).toString(Charsets.US_ASCII))
        }
    }

    @Test fun `no acepta URI payload desnudo otro proposito ni campos extra`() {
        for (texto in listOf("https://relay.invalid/qr", recurso("09_offline_response.payload.json").toString(Charsets.UTF_8),
                CodecCp1.serializar(sobre("04_lock_for.signed.json")).toString(Charsets.US_ASCII),
                textoRespuesta().dropLast(1) + ",\"extra\":true}")) {
            assertThrows(RuntimeException::class.java) { QrOffline.leerRespuestaSinAutorizar(texto) }
        }
    }

    @Test fun `rechaza texto vacio excesivo Unicode y controles sin normalizarlos`() {
        for (texto in listOf("", "A".repeat(2001), "\n" + textoRespuesta(), "é" + textoRespuesta(), "\u0000")) {
            assertThrows(IllegalArgumentException::class.java) { QrOffline.leerRespuestaSinAutorizar(texto) }
        }
    }

    @Test fun `imagen incompleta pequena o excesiva se rechaza antes de decodificar`() {
        for ((ancho, alto) in listOf(63 to 64, 64 to 1281, Int.MAX_VALUE to 64, 640 to 640)) {
            assertThrows(IllegalArgumentException::class.java) {
                QrOffline.decodificarRespuestaSinAutorizar(ByteArray(0), ancho, alto)
            }
        }
    }

    @Test fun `imagen blanca no inventa una respuesta`() {
        assertThrows(com.google.zxing.NotFoundException::class.java) {
            QrOffline.decodificarRespuestaSinAutorizar(ByteArray(128 * 128) { 255.toByte() }, 128, 128)
        }
    }
}
