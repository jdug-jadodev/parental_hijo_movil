package dev.controlparental.child.pairing

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dev.controlparental.protocol.Base64Cp1
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.UUID

class QrOfertaCp2Test {
    @Test fun matrizConMargenEscaneableConservaBytesOriginales() {
        val par = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
        val bytes = CodecOfertaCp2.escribir(OfertaCp2(Base64Cp1.codificar(par.public.encoded),
            UUID.randomUUID().toString(), Base64Cp1.codificar(ByteArray(32))))
        val m = QrOfertaCp2.crear(bytes)
        for (y in 0 until m.height) for (x in 0 until m.width)
            if (x < 4 || y < 4 || x >= m.width - 4 || y >= m.height - 4) assertFalse(m[x, y])
        val lado = m.width * 6
        val imagen = IntArray(lado * lado) { i -> if (m[(i % lado) / 6, (i / lado) / 6]) 0xff000000.toInt() else 0xffffffff.toInt() }
        val decodificado = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(lado, lado, imagen))))
        assertArrayEquals(bytes, decodificado.text.toByteArray(Charsets.UTF_8))
        assertEquals(CodecOfertaCp2.leer(bytes), CodecOfertaCp2.leer(decodificado.text.toByteArray()))
        // Lectura sintética JVM, no cámara/dispositivo/ceremonia ni prueba de identidad.
    }
    @Test fun noGeneraQrProvisioningNiSobresNiOfertasCp1() {
        for (texto in listOf("{}", "https://control-parental.invalid", "{\"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME\":\"DPC\"}"))
            assertThrows(RuntimeException::class.java) { QrOfertaCp2.crear(texto.toByteArray()) }
    }
}
