package dev.controlparental.child.pairing

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Solo CHILD_OFFER original; no sobre CP/1 ni QR de aprovisionamiento Android. */
internal object QrOfertaCp2 {
    fun crear(bytes: ByteArray): BitMatrix {
        CodecOfertaCp2.leer(bytes)
        return QRCodeWriter().encode(bytes.toString(Charsets.US_ASCII), BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    }
}
