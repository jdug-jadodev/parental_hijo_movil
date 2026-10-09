package dev.controlparental.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

internal object VectoresCripto {
    private val datos = Json.parseToJsonElement(javaClass.classLoader
        .getResourceAsStream("vectores_crypto.json")!!.use { it.readBytes().decodeToString() }).jsonObject
    val nombres = datos.getValue("vectors").jsonArray.map { it.jsonObject.getValue("name").jsonPrimitive.content }
    fun clave(rol: Rol): ClavePublicaCp1 = ClavePublicaCp1.leer(
        datos.getValue("publicKeys").jsonObject.getValue(rol.name).jsonPrimitive.content)
    fun rol(nombre: String): Rol = Rol.valueOf(vector(nombre).getValue("signerRole").jsonPrimitive.content)
    fun hash(nombre: String): String = vector(nombre).getValue("payloadSha256B64").jsonPrimitive.content
    private fun vector(nombre: String) = datos.getValue("vectors").jsonArray
        .first { it.jsonObject.getValue("name").jsonPrimitive.content == nombre }.jsonObject
    fun sobre(nombre: String) = CodecCp1.leerFrame(EjemplosCp1.bytes("$nombre.signed.json")) as SobreSinVerificar
}

@RunWith(Parameterized::class)
class VectoresCriptoTest(private val nombre: String) {
    private val rol = VectoresCripto.rol(nombre)
    private val sobre = VectoresCripto.sobre(nombre)
    private val clave = VectoresCripto.clave(rol)

    @Test
    fun `verifica fixture original hash y bytes sin reconstruir lo firmado`() {
        val verificado = CriptografiaCp1.verificarFrame(EjemplosCp1.bytes("$nombre.signed.json"), clave, rol, sobre.purpose)
        assertEquals(VectoresCripto.hash(nombre), verificado.payloadSha256B64)
        assertTrue(Base64Cp1.decodificar(sobre.payloadB64, 8192).contentEquals(verificado.bytesOriginales()))
        assertEquals(rol, verificado.rolFirmante)
    }

    @Test
    fun `rechaza clave diferente`() {
        val otra = if (rol == Rol.PARENT) Rol.CHILD else Rol.PARENT
        val error = assertThrows(ErrorProtocolo::class.java) {
            CriptografiaCp1.verificar(sobre, VectoresCripto.clave(otra), rol, sobre.purpose)
        }
        assertEquals(CodigoCp1.BAD_SIGNATURE, error.codigo)
    }

    @Test
    fun `rechaza payload alterado incluso si todavia es JSON valido`() {
        val bytes = Base64Cp1.decodificar(sobre.payloadB64, 8192)
        val texto = bytes.decodeToString().replace("\"v\":1", "\"v\":2")
        val alterado = sobre.copy(payloadB64 = Base64Cp1.codificar(texto.toByteArray()))
        val error = assertThrows(ErrorProtocolo::class.java) { CriptografiaCp1.verificar(alterado, clave, rol, sobre.purpose) }
        assertEquals(CodigoCp1.BAD_SIGNATURE, error.codigo)
    }

    @Test
    fun `rechaza firma proposito y rol alterados`() {
        val firma = Base64Cp1.decodificar(sobre.sigB64, 72)
        firma[firma.lastIndex] = (firma.last().toInt() xor 1).toByte()
        assertThrows(ErrorProtocolo::class.java) {
            CriptografiaCp1.verificar(sobre.copy(sigB64 = Base64Cp1.codificar(firma)), clave, rol, sobre.purpose)
        }
        val otro = if (sobre.purpose == Proposito.AUTH) Proposito.MESSAGE else Proposito.AUTH
        assertThrows(ErrorProtocolo::class.java) { CriptografiaCp1.verificar(sobre.copy(purpose = otro), clave, rol, otro) }
        assertThrows(ErrorProtocolo::class.java) { CriptografiaCp1.verificar(sobre, clave, rol, otro) }
        val error = assertThrows(ErrorProtocolo::class.java) {
            CriptografiaCp1.verificar(sobre, clave, if (rol == Rol.PARENT) Rol.CHILD else Rol.PARENT, sobre.purpose)
        }
        assertEquals(CodigoCp1.WRONG_ROLE, error.codigo)
    }

    @Test
    fun `el resultado protege los bytes originales frente a mutacion del consumidor`() {
        val verificado = CriptografiaCp1.verificar(sobre, clave, rol, sobre.purpose)
        val copia = verificado.bytesOriginales()
        copia[0] = 0
        assertEquals('{'.code.toByte(), verificado.bytesOriginales()[0])
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun vectores(): List<Array<String>> = VectoresCripto.nombres.map { arrayOf(it) }
    }
}
