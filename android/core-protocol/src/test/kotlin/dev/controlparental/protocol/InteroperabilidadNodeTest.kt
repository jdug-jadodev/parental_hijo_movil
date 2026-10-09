package dev.controlparental.protocol

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Test

class InteroperabilidadNodeTest {
    @Test
    fun `genera sobres JVM con claves efimeras y exporta solo evidencia publica para Node`() {
        val firmantes = Rol.entries.associateWith { FirmanteEfimero() }
        val vectores = VectoresCripto.nombres.map { nombre ->
            val rol = VectoresCripto.rol(nombre)
            val fixture = VectoresCripto.sobre(nombre)
            val original = CodecCp1.leerPayloadSinVerificar(
                Base64Cp1.decodificar(fixture.payloadB64, 8192), fixture.purpose)
            val mensaje = if (original is AceptacionVinculo) original.copy(
                parentPublicKeyB64 = firmantes.getValue(Rol.PARENT).clavePublica.base64,
                childPublicKeyB64 = firmantes.getValue(Rol.CHILD).clavePublica.base64,
            ) else original
            val firmante = firmantes.getValue(rol)
            val sobre = CriptografiaCp1.firmar(mensaje, fixture.purpose, rol, firmante)
            val verificado = CriptografiaCp1.verificar(sobre, firmante.clavePublica, rol, fixture.purpose)
            assertEquals(mensaje, verificado.mensaje)
            ObjetoJson(mapOf(
                "name" to TextoJson(nombre),
                "signerRole" to TextoJson(rol.name),
                "publicKeyB64" to TextoJson(firmante.clavePublica.base64),
                "payloadSha256B64" to TextoJson(verificado.payloadSha256B64),
                "envelope" to JsonCp1.leer(CodecCp1.serializar(sobre)),
            ))
        }
        val salida = Path.of("build", "interoperabilidad", "jvm-a-node.json")
        Files.createDirectories(salida.parent)
        Files.write(salida, JsonCp1.serializar(ObjetoJson(mapOf("vectors" to ListaJson(vectores)))))
        // El archivo está bajo build/ (ignorado). Nunca se obtiene ni escribe una privada.
    }
}
