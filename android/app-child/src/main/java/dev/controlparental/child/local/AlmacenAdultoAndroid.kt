package dev.controlparental.child.local

import android.content.Context
import android.os.SystemClock
import android.os.UserManager
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** CE/noBackup, AES-GCM/Keystore distinto de identidad CP/1. Ausencia parcial = error. */
internal class AlmacenAdultoAndroid(private val contexto: Context, private val directorio: File,
    private val alias: String) : AlmacenAdulto {
    private val archivoRegistro = File(directorio, "registro.enc")
    private val marca = File(directorio, "configurado")
    private fun ks() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun restos(f: File) = listOf(f, File(f.path + ".bak"), File(f.path + ".new")).any(File::exists)
    private fun comprobarContexto() {
        check(!contexto.isDeviceProtectedStorage && contexto.getSystemService(UserManager::class.java).isUserUnlocked)
        check(directorio.isDirectory || directorio.mkdirs())
    }
    @Synchronized override fun leer(): RegistroAdulto? {
        comprobarContexto()
        val claves = ks()
        if (!restos(archivoRegistro) && !restos(marca) && !claves.containsAlias(alias)) return null
        check(restos(archivoRegistro) && restos(marca) && claves.containsAlias(alias))
        val m = leerBytes(marca, 16); check(m.size == 16)
        val bytes = leerBytes(archivoRegistro, CodecAdulto.MAX_BYTES + 64)
        check(bytes.size >= 29 && bytes[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, claves.getKey(alias, null) as SecretKey,
            GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD("HIJO/ADULTO/1".toByteArray(Charsets.UTF_8) + m)
        val plano = cipher.doFinal(bytes.copyOfRange(13, bytes.size))
        return try { CodecAdulto.decodificar(plano) } finally { plano.fill(0) }
    }
    @Synchronized override fun guardar(registro: RegistroAdulto) {
        val previo = leer() // Jamás regenerar una clave perdida sobre datos existentes.
        if (previo == null) {
            escribir(marca, ByteArray(16).also(SecureRandom()::nextBytes))
            val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generador.init(KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generador.generateKey()
        }
        val claves = ks(); check(claves.containsAlias(alias))
        val m = leerBytes(marca, 16); check(m.size == 16)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, claves.getKey(alias, null) as SecretKey)
        cipher.updateAAD("HIJO/ADULTO/1".toByteArray(Charsets.UTF_8) + m)
        val plano = CodecAdulto.codificar(registro)
        try {
            check(cipher.iv.size == 12)
            escribir(archivoRegistro, byteArrayOf(1) + cipher.iv + cipher.doFinal(plano))
            val leido = leer() ?: error("Readback no disponible")
            val verificado = CodecAdulto.codificar(leido)
            try { check(plano.contentEquals(verificado)) } finally { verificado.fill(0) }
        } finally { plano.fill(0) }
    }
    private fun leerBytes(f: File, maximo: Int): ByteArray = AtomicFile(f).openRead().use {
        val bytes = ByteArrayOutputStream(); val buffer = ByteArray(512)
        while (true) {
            val n = it.read(buffer, 0, minOf(buffer.size, maximo + 1 - bytes.size()))
            if (n < 0) break
            bytes.write(buffer, 0, n); check(bytes.size() <= maximo)
        }
        bytes.toByteArray()
    }
    private fun escribir(f: File, bytes: ByteArray) {
        val a = AtomicFile(f); val salida = a.startWrite()
        try { salida.write(bytes); a.finishWrite(salida) }
        catch (e: Exception) { a.failWrite(salida); throw e }
    }
}

/** Una sola operación admitida por proceso: sin cola ilimitada/KDF paralelos. */
internal object AdultoAndroid {
    private var motor: AccesoAdultoLocal? = null
    private val ocupado = AtomicBoolean()
    private val ejecutor = Executors.newSingleThreadExecutor { Thread(it, "adulto-local") }
    @Synchronized private fun obtener(c: Context): AccesoAdultoLocal = motor ?: AccesoAdultoLocal(
        AlmacenAdultoAndroid(c.applicationContext, File(c.noBackupFilesDir, "adulto-local-v1"), "hijo.adulto.local.v1"),
        PbkdfAdulto(), SystemClock::elapsedRealtime,
        { Settings.Global.getInt(c.contentResolver, Settings.Global.BOOT_COUNT, -1) },
    ).also { motor = it }
    fun ejecutar(c: Context, accion: (AccesoAdultoLocal) -> ResultadoAdulto,
        entregar: (ResultadoAdulto) -> Unit): Boolean {
        if (!ocupado.compareAndSet(false, true)) return false
        ejecutor.execute {
            val resultado = try { accion(obtener(c.applicationContext)) }
                catch (_: Exception) { ResultadoAdulto(TipoAdulto.ERROR) }
            ocupado.set(false)
            entregar(resultado)
        }
        return true
    }
}
