package dev.controlparental.child.data

import dev.controlparental.child.domain.PoliticaAceptada
import dev.controlparental.protocol.*
import java.security.MessageDigest
import java.util.Collections

/** Formato local, NO frame CP/1. Reutiliza parser estricto sin ampliar sus límites. */
object CodecEstado {
    const val MAX_ARCHIVO_BYTES = 16384
    const val MAX_REGISTRO_BYTES = 11000

    fun codificar(estado: EstadoControl): ByteArray {
        ValidadorEstado.validar(estado)
        val payload = JsonCp1.serializar(registro(estado))
        require(payload.size <= MAX_REGISTRO_BYTES)
        return JsonCp1.serializar(objeto("formatVersion" to EnteroJson(1),
            "payloadB64" to TextoJson(Base64Cp1.codificar(payload)),
            "sha256B64" to TextoJson(hash(payload)))).also { require(it.size <= MAX_ARCHIVO_BYTES) }
    }

    fun decodificar(bytes: ByteArray): EstadoControl {
        val sobre = Campos(JsonCp1.leer(bytes, MAX_ARCHIVO_BYTES))
        require(sobre.numero("formatVersion") == 1L)
        val payload = Base64Cp1.decodificar(sobre.texto("payloadB64"), MAX_REGISTRO_BYTES)
        val checksum = sobre.texto("sha256B64")
        require(Base64Cp1.decodificar(checksum, 32).size == 32 && checksum == hash(payload))
        sobre.terminar()
        // Solo interpretar el registro después de comprobar los bytes del checksum.
        val c = Campos(JsonCp1.leer(payload, MAX_REGISTRO_BYTES))
        val version = c.numero("schemaVersion").also { require(it == 1L) }.toInt()
        val vinculo = c.opcional("binding") { v ->
            val b = Campos(v)
            val hashes = Campos(b.valor("recoveryHashes"))
            val resultado = VinculoPublico(b.texto("pairId"), b.texto("parentPublicKeyB64"),
                b.texto("childPublicKeyB64"), b.texto("relayOrigin"),
                HashesRecuperacion(hashes.texto("MAINTENANCE"), hashes.texto("RECOVER_PARENT"), hashes.texto("RETIRE")))
            hashes.terminar(); b.terminar(); resultado
        }
        val consumidos = c.listaTextos("recoveryConsumed").map { PropositoRecuperacionLocal.valueOf(it) }
        require(consumidos.distinct().size == consumidos.size)
        val fallos = c.numero("recoveryFailureCount").also { require(it <= Int.MAX_VALUE) }.toInt()
        val espera = c.booleano("recoveryWaitPending")
        val boot = c.opcional("bootContext") { v ->
            val b = Campos(v)
            val resultado = ContextoArranque(b.opcional("systemBootCount") {
                (it as? EnteroJson)?.numero?.also { n -> require(n in 0..Int.MAX_VALUE.toLong()) }?.toInt()
                    ?: throw ErrorEstadoLocal()
            }, b.texto("bootId"), b.textoOpcional("authorizedBootId"), b.decimal("lastObservedElapsed"))
            b.terminar(); resultado
        }
        val politica = c.opcional("policy") { v ->
            val p = Campos(v)
            val resultado = PoliticaAceptada(AccionPolitica.valueOf(p.texto("action")),
                p.opcional("durationSec") { (it as? EnteroJson)?.numero ?: throw ErrorEstadoLocal() },
                p.decimal("acceptedAtElapsed"), p.texto("policyBootId"))
            p.terminar(); resultado
        }
        val ultima = c.opcional("lastCommand") { v ->
            val u = Campos(v)
            val resultado = UltimaOrdenDuradera(u.decimal("seq"), u.texto("commandId"),
                u.texto("payloadSha256B64"), CodecCp1.leerFrame(JsonCp1.serializar(u.valor("signedEnvelope")))
                    as? SobreSinVerificar ?: throw ErrorEstadoLocal(),
                EtapaAplicacion.valueOf(u.texto("stage")), UltimoResultado.valueOf(u.texto("result")))
            u.terminar(); resultado
        }
        val estado = EstadoControl(version, vinculo, Collections.unmodifiableSet(consumidos.toSet()), fallos, espera, boot, politica, ultima,
            Collections.unmodifiableList(c.listaTextos("approvedPackages")), EtapaAprovisionamiento.valueOf(c.texto("provisioningStage")),
            c.textoOpcional("lastEnforcementErrorCode")?.let(CodigoCp1::valueOf))
        c.terminar()
        ValidadorEstado.validar(estado)
        return estado
    }

    private fun registro(e: EstadoControl): ObjetoJson = objeto(
        "schemaVersion" to EnteroJson(e.schemaVersion.toLong()),
        "binding" to opcional(e.vinculo) { b -> objeto("pairId" to TextoJson(b.pairId),
            "parentPublicKeyB64" to TextoJson(b.parentPublicKeyB64), "childPublicKeyB64" to TextoJson(b.childPublicKeyB64),
            "relayOrigin" to TextoJson(b.relayOrigin), "recoveryHashes" to objeto(
                "MAINTENANCE" to TextoJson(b.recoveryHashes.MAINTENANCE),
                "RECOVER_PARENT" to TextoJson(b.recoveryHashes.RECOVER_PARENT), "RETIRE" to TextoJson(b.recoveryHashes.RETIRE))) },
        "recoveryConsumed" to ListaJson(e.recoveryConsumed.map { TextoJson(it.name) }.sortedBy { it.texto }),
        "recoveryFailureCount" to EnteroJson(e.recoveryFailureCount.toLong()),
        "recoveryWaitPending" to BooleanoJson(e.esperaRecuperacionPendiente),
        "bootContext" to opcional(e.bootContext) { b -> objeto(
            "systemBootCount" to opcional(b.systemBootCount) { EnteroJson(it.toLong()) }, "bootId" to TextoJson(b.bootId),
            "authorizedBootId" to opcional(b.authorizedBootId, ::TextoJson), "lastObservedElapsed" to decimal(b.ultimoElapsedObservado)) },
        "policy" to opcional(e.policy) { p -> objeto("action" to TextoJson(p.accion.name),
            "durationSec" to opcional(p.duracionSegundos, ::EnteroJson), "acceptedAtElapsed" to decimal(p.aceptadaEnMs),
            "policyBootId" to TextoJson(p.bootId)) },
        "lastCommand" to opcional(e.lastCommand) { u -> objeto("seq" to decimal(u.seq),
            "commandId" to TextoJson(u.commandId), "payloadSha256B64" to TextoJson(u.payloadSha256B64),
            "signedEnvelope" to JsonCp1.leer(CodecCp1.serializar(u.signedEnvelope)),
            "stage" to TextoJson(u.stage.name), "result" to TextoJson(u.result.name)) },
        "approvedPackages" to ListaJson(e.approvedPackages.map(::TextoJson)),
        "provisioningStage" to TextoJson(e.provisioningStage.name),
        "lastEnforcementErrorCode" to opcional(e.lastEnforcementErrorCode) { TextoJson(it.name) },
    )

    private fun hash(bytes: ByteArray) = Base64Cp1.codificar(MessageDigest.getInstance("SHA-256").digest(bytes))
    private fun objeto(vararg campos: Pair<String, ValorJson>) = ObjetoJson(mapOf(*campos))
    private fun decimal(n: Long): TextoJson { require(n >= 0); return TextoJson(n.toString()) }
    private fun <T> opcional(valor: T?, convertir: (T) -> ValorJson): ValorJson = valor?.let(convertir) ?: NuloJson

    private class Campos(valor: ValorJson) {
        private val campos = (valor as? ObjetoJson)?.campos?.toMutableMap() ?: throw ErrorEstadoLocal()
        fun valor(nombre: String): ValorJson = campos.remove(nombre) ?: throw ErrorEstadoLocal()
        fun texto(nombre: String) = (valor(nombre) as? TextoJson)?.texto ?: throw ErrorEstadoLocal()
        fun numero(nombre: String) = (valor(nombre) as? EnteroJson)?.numero ?: throw ErrorEstadoLocal()
        fun booleano(nombre: String) = (valor(nombre) as? BooleanoJson)?.valor ?: throw ErrorEstadoLocal()
        fun decimal(nombre: String): Long = texto(nombre).let {
            require(Regex("0|[1-9][0-9]{0,18}").matches(it)); it.toLongOrNull() ?: throw ErrorEstadoLocal()
        }
        fun <T> opcional(nombre: String, convertir: (ValorJson) -> T): T? = valor(nombre).let { if (it == NuloJson) null else convertir(it) }
        fun textoOpcional(nombre: String) = opcional(nombre) { (it as? TextoJson)?.texto ?: throw ErrorEstadoLocal() }
        fun listaTextos(nombre: String): List<String> = ((valor(nombre) as? ListaJson)?.elementos ?: throw ErrorEstadoLocal())
            .map { (it as? TextoJson)?.texto ?: throw ErrorEstadoLocal() }
        fun terminar() { require(campos.isEmpty()) }
    }
}
