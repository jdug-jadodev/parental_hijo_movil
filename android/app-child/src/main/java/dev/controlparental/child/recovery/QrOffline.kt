package dev.controlparental.child.recovery

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dev.controlparental.protocol.*

/** Módulos con margen blanco incluido; dibujar sin interpolación ni recortar el margen. */
class MatrizQrOffline internal constructor(private val matriz: BitMatrix) {
    val lado: Int get() = matriz.width
    fun esNegro(x: Int, y: Int): Boolean = matriz[x, y]
}

/** Transporte óptico, NO firma ni autorización. Sin URI, compresión, secretos o IO. */
object QrOffline {
    // Límite de este transporte QR, no una modificación del esquema CP/1.
    const val MAX_TEXTO_BYTES = 2000
    const val MAX_LADO_IMAGEN = 1280

    fun crearDesafio(sobre: SobreSinVerificar): MatrizQrOffline {
        val bytes = CodecCp1.serializar(sobre)
        validar(bytes, esRespuesta = false)
        return MatrizQrOffline(QRCodeWriter().encode(bytes.toString(Charsets.US_ASCII),
            BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 4,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)))
    }

    /** Texto recibido sin trim/normalización. Resultado aún requiere RecoveryController. */
    fun leerRespuestaSinAutorizar(texto: String): ByteArray {
        require(texto.length in 1..MAX_TEXTO_BYTES && texto.all { it.code in 32..126 }) {
            "Texto QR fuera del formato o límite permitido."
        }
        return texto.toByteArray(Charsets.US_ASCII).also { validar(it, esRespuesta = true) }
    }

    /** Plano Y compacto sin stride/padding; cámara deberá convertirlo y cerrar su imagen. */
    fun decodificarRespuestaSinAutorizar(luminancia: ByteArray, ancho: Int, alto: Int): ByteArray {
        require(ancho in 64..MAX_LADO_IMAGEN && alto in 64..MAX_LADO_IMAGEN &&
            luminancia.size == ancho * alto) { "Imagen QR fuera del límite o plano incompleto." }
        val plano = PlanoLuminancia(luminancia.copyOf(), ancho, alto)
        val resultado = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(plano)),
            mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "US-ASCII"))
        return leerRespuestaSinAutorizar(resultado.text)
    }

    private fun validar(bytes: ByteArray, esRespuesta: Boolean) {
        require(bytes.size in 1..MAX_TEXTO_BYTES)
        val sobre = CodecCp1.leerDocumentoSinAutorizar(bytes) as? SobreSinVerificar
            ?: throw IllegalArgumentException("El QR requiere un sobre firmado CP/1.")
        require(sobre.purpose == Proposito.OFFLINE) { "Propósito QR incorrecto." }
        val payload = CodecCp1.leerPayloadSinVerificar(
            Base64Cp1.decodificar(sobre.payloadB64, JsonCp1.MAX_PAYLOAD_BYTES), Proposito.OFFLINE)
        require(payload is MensajeOffline && payload.esRespuesta == esRespuesta) {
            "Dirección del mensaje OFFLINE incorrecta."
        }
        // La clave confiable, firma, nonce, contexto y caducidad los comprueba el controlador.
    }

    private class PlanoLuminancia(private val bytes: ByteArray, ancho: Int, alto: Int) : LuminanceSource(ancho, alto) {
        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            require(y in 0 until height)
            val destino = if (row == null || row.size < width) ByteArray(width) else row
            bytes.copyInto(destino, 0, y * width, (y + 1) * width)
            return destino
        }
        override fun getMatrix(): ByteArray = bytes.copyOf()
    }
}
