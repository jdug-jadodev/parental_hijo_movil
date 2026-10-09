package dev.controlparental.child.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.protocol.AceptacionVinculo
import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.ClavePublicaCp1
import dev.controlparental.protocol.CodecCp1
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.CriptografiaCp1
import dev.controlparental.protocol.ErrorProtocolo
import dev.controlparental.protocol.Proposito
import dev.controlparental.protocol.Rol
import dev.controlparental.protocol.SobreSinVerificar
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Requiere Android desbloqueado. Solo crea y elimina identidades efímeras de pruebas. */
@RunWith(AndroidJUnit4::class)
class CriptografiaAndroidTest {
    private val instrumento = InstrumentationRegistry.getInstrumentation()
    private val contexto get() = instrumento.targetContext

    private fun ejemplo(nombre: String): ByteArray = instrumento.context.assets.open(nombre).use { it.readBytes() }

    @Test
    fun `Android verifica los diez sobres congelados y conserva los bytes`() {
        val vinculo = CodecCp1.leerDocumentoSinAutorizar(ejemplo("07_pair_accept.payload.json")) as AceptacionVinculo
        val claves = mapOf(Rol.PARENT to ClavePublicaCp1.leer(vinculo.parentPublicKeyB64),
            Rol.CHILD to ClavePublicaCp1.leer(vinculo.childPublicKeyB64))
        val firmados = instrumento.context.assets.list("")!!.filter { it.endsWith(".signed.json") }.sorted()
        assertEquals(10, firmados.size)
        firmados.forEach { nombre ->
            val rol = if (nombre.take(2) in setOf("01", "03", "04", "07", "09")) Rol.PARENT else Rol.CHILD
            val frame = ejemplo(nombre)
            val sobre = CodecCp1.leerFrame(frame) as SobreSinVerificar
            val resultado = CriptografiaCp1.verificarFrame(frame, claves.getValue(rol), rol, sobre.purpose)
            assertTrue(Base64Cp1.decodificar(sobre.payloadB64, 8192).contentEquals(resultado.bytesOriginales()))
            assertTrue(CodecCp1.serializar(resultado.mensaje).contentEquals(resultado.bytesOriginales()))
            val otra = claves.getValue(if (rol == Rol.PARENT) Rol.CHILD else Rol.PARENT)
            assertThrows(ErrorProtocolo::class.java) { CriptografiaCp1.verificarFrame(frame, otra, rol, sobre.purpose) }
        }
    }

    @Test
    fun `Keystore firma sin permitir exportar la privada`() {
        conClaveTemporal { alias ->
            val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val entrada = almacen.getEntry(alias, null) as KeyStore.PrivateKeyEntry
            assertNull(entrada.privateKey.encoded)
            val firmante = FirmanteAndroidKeystore(contexto, alias)
            val mensaje = CodecCp1.leerDocumentoSinAutorizar(ejemplo("08_offline_challenge.payload.json"))
            val sobre = CriptografiaCp1.firmar(mensaje, Proposito.OFFLINE, Rol.CHILD, firmante)
            val resultado = CriptografiaCp1.verificar(sobre, firmante.clavePublica, Rol.CHILD, Proposito.OFFLINE)
            assertEquals(mensaje, resultado.mensaje)
        }
    }

    @Test
    fun `alias inexistente produce error seguro sin crear una identidad de reemplazo`() {
        val alias = "cp1-prueba-ausente-${UUID.randomUUID()}"
        val firmante = FirmanteAndroidKeystore(contexto, alias)
        assertEquals(CodigoCp1.KEY_UNAVAILABLE, assertThrows(ErrorProtocolo::class.java) { firmante.clavePublica }.codigo)
        assertEquals(CodigoCp1.KEY_UNAVAILABLE, assertThrows(ErrorProtocolo::class.java) {
            firmante.firmarBytes("CPv1/OFFLINE\n{}".toByteArray())
        }.codigo)
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertTrue(!almacen.containsAlias(alias))
    }

    private fun conClaveTemporal(accion: (String) -> Unit) {
        val alias = "cp1-prueba-efimera-${UUID.randomUUID()}"
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val opciones = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
                .apply { initialize(opciones) }.generateKeyPair()
            accion(alias)
        } finally {
            // Solo elimina el alias único creado por este test, nunca el de producción.
            almacen.deleteEntry(alias)
        }
    }
}
