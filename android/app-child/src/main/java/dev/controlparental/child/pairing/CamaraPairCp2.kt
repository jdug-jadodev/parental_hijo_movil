package dev.controlparental.child.pairing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicBoolean

/** Cámara propia sin Activity externa, archivos, logs ni SDK nuevos. Solo QR, luminosidad RAM. */
internal class CamaraPairCp2(private val contexto: Context, private val vista: TextureView,
    private val recibido: (ByteArray) -> Unit, private val error: () -> Unit) : AutoCloseable {
    private val cerrado = AtomicBoolean()
    private val entregado = AtomicBoolean()
    private val hilo = HandlerThread("camara-pair").apply { start() }
    private val handler = Handler(hilo.looper)
    private var camara: CameraDevice? = null
    private var sesion: CameraCaptureSession? = null
    private var lector: ImageReader? = null
    private var superficie: Surface? = null
    fun iniciar() {
        vista.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) { handler.post { abrir(s) } }
            override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean { close(); return true }
            override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
        }
        if (vista.isAvailable) vista.surfaceTexture?.let { s -> handler.post { abrir(s) } }
    }
    private fun fallo() { if (!cerrado.get() && entregado.compareAndSet(false, true)) error(); close() }
    @Suppress("DEPRECATION") // Camera2 oficial compatible con minSdk31; no intentos externos.
    private fun abrir(textura: SurfaceTexture) {
        if (cerrado.get()) return
        try {
            check(contexto.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            val gestor = contexto.getSystemService(CameraManager::class.java)
            val id = gestor.cameraIdList.firstOrNull {
                gestor.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: error("Cámara trasera no disponible")
            val opciones = gestor.getCameraCharacteristics(id).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(ImageFormat.YUV_420_888) ?: error("Formato no disponible")
            val tamano = opciones.filter { it.width in 320..1280 && it.height in 240..960 }
                .minByOrNull { kotlin.math.abs(it.width * it.height - 640 * 480) } ?: error("Tamaño no disponible")
            textura.setDefaultBufferSize(tamano.width, tamano.height)
            superficie = Surface(textura)
            lector = ImageReader.newInstance(tamano.width, tamano.height, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener({ origen ->
                    try {
                        origen.acquireLatestImage()?.use { imagen ->
                            if (!cerrado.get() && !entregado.get()) {
                                val plano = imagen.planes[0]; val b = plano.buffer
                                val ancho = imagen.width; val alto = imagen.height
                                val y = ByteArray(ancho * alto)
                                for (fila in 0 until alto) for (col in 0 until ancho)
                                    y[fila * ancho + col] = b.get(fila * plano.rowStride + col * plano.pixelStride)
                                val fuente = PlanarYUVLuminanceSource(y, ancho, alto, 0, 0, ancho, alto, false)
                                val resultado = try {
                                    MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(fuente)),
                                        mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true))
                                } catch (_: ReaderException) { null }
                                val texto = resultado?.text
                                if (texto != null && texto.length in 1..CodecOfertaCp2.MAX_FRAME && texto.all { it.code in 32..126 }) {
                                    val bytes = texto.toByteArray(Charsets.US_ASCII)
                                    if (entregado.compareAndSet(false, true)) { recibido(bytes); close() }
                                }
                            }
                        }
                    } catch (_: Exception) { fallo() }
                }, handler)
            }
            gestor.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    if (cerrado.get()) { device.close(); return }
                    camara = device
                    try {
                        val destinos = listOf(superficie!!, lector!!.surface)
                        device.createCaptureSession(destinos, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) {
                                if (cerrado.get()) { s.close(); return }
                                sesion = s
                                try {
                                    val captura = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                        destinos.forEach(::addTarget)
                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                    }.build()
                                    s.setRepeatingRequest(captura, null, handler)
                                } catch (_: Exception) { fallo() }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) { s.close(); fallo() }
                        }, handler)
                    } catch (_: Exception) { fallo() }
                }
                override fun onDisconnected(device: CameraDevice) { device.close(); fallo() }
                override fun onError(device: CameraDevice, codigo: Int) { device.close(); fallo() }
            }, handler)
        } catch (_: Exception) { fallo() }
    }
    override fun close() {
        if (!cerrado.compareAndSet(false, true)) return
        handler.post {
            sesion?.close(); sesion = null; camara?.close(); camara = null
            lector?.close(); lector = null; superficie?.release(); superficie = null; hilo.quitSafely()
        }
    }
}
