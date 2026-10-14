package dev.controlparental.child.local

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Formato privado local, NO CP/1. Solo verificadores; nunca código/respuesta en disco. */
internal data class VerificadorAdulto(val salt: ByteArray, val hash: ByteArray)
internal data class RegistroAdulto(
    val revision: Long, val pregunta: String, val codigo: VerificadorAdulto,
    val respuesta: VerificadorAdulto, val fallos: Int = 0, val esperaMs: Long = 0,
    val anclaMs: Long = 0, val ultimoMs: Long = 0, val boot: Int,
)

internal object ReglasAdulto {
    fun codigo(texto: String) = texto.length in 5..10 && texto.all { it in '0'..'9' }
    // Comparación exacta: no trim/NFC/case folding. Se explica al adulto en ambos formularios.
    fun texto(texto: String, maximo: Int): Boolean = texto.codePointCount(0, texto.length) in 1..maximo &&
        texto.isNotBlank() && texto.none { Character.isISOControl(it) } &&
        texto.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8) == texto
}

internal interface DerivadorAdulto {
    fun crear(texto: String): VerificadorAdulto
    fun coincide(texto: String, verificador: VerificadorAdulto): Boolean
}

/** PBKDF2 oficial; parámetros privados fijos de v1, pendientes de medir en A13. */
internal class PbkdfAdulto : DerivadorAdulto {
    private fun derivar(texto: String, salt: ByteArray): ByteArray {
        val caracteres = texto.toCharArray()
        val spec = PBEKeySpec(caracteres, salt, ITERACIONES, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword(); caracteres.fill('\u0000') }
    }
    override fun crear(texto: String): VerificadorAdulto {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        return VerificadorAdulto(salt, derivar(texto, salt))
    }
    override fun coincide(texto: String, verificador: VerificadorAdulto): Boolean {
        val derivado = derivar(texto, verificador.salt)
        return try { MessageDigest.isEqual(derivado, verificador.hash) } finally { derivado.fill(0) }
    }
    companion object { const val ITERACIONES = 210_000 }
}

/** Acotado y versionado; formato desconocido/corrupción NO se migra a instalación vacía. */
internal object CodecAdulto {
    const val MAX_BYTES = 4096
    fun codificar(r: RegistroAdulto): ByteArray {
        validar(r)
        return ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { d ->
                d.writeInt(1); d.writeInt(PbkdfAdulto.ITERACIONES); d.writeLong(r.revision)
                val pregunta = r.pregunta.toByteArray(Charsets.UTF_8)
                d.writeInt(pregunta.size); d.write(pregunta)
                for (v in listOf(r.codigo, r.respuesta)) { d.write(v.salt); d.write(v.hash) }
                d.writeInt(r.fallos); d.writeLong(r.esperaMs); d.writeLong(r.anclaMs)
                d.writeLong(r.ultimoMs); d.writeInt(r.boot)
            }
        }.toByteArray()
    }
    fun decodificar(bytes: ByteArray): RegistroAdulto {
        require(bytes.size in 1..MAX_BYTES)
        return DataInputStream(ByteArrayInputStream(bytes)).use { d ->
            require(d.readInt() == 1 && d.readInt() == PbkdfAdulto.ITERACIONES)
            val revision = d.readLong()
            val n = d.readInt(); require(n in 1..800)
            val p = ByteArray(n).also(d::readFully)
            val pregunta = p.toString(Charsets.UTF_8); require(p.contentEquals(pregunta.toByteArray(Charsets.UTF_8)))
            fun verificador() = VerificadorAdulto(ByteArray(16).also(d::readFully), ByteArray(32).also(d::readFully))
            val r = RegistroAdulto(revision, pregunta, verificador(), verificador(),
                d.readInt(), d.readLong(), d.readLong(), d.readLong(), d.readInt())
            require(d.available() == 0); validar(r); r
        }
    }
    private fun validar(r: RegistroAdulto) {
        require(r.revision > 0 && ReglasAdulto.texto(r.pregunta, 200))
        require(r.fallos in 0..10 && r.esperaMs in setOf(0L, 60_000L, 300_000L))
        require(r.anclaMs >= 0 && r.ultimoMs >= r.anclaMs && r.boot >= 0)
        for (v in listOf(r.codigo, r.respuesta)) require(v.salt.size == 16 && v.hash.size == 32)
    }
}
