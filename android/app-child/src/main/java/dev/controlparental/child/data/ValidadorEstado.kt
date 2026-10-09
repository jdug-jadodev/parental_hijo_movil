package dev.controlparental.child.data

import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.ClavePublicaCp1
import dev.controlparental.protocol.CriptografiaCp1
import dev.controlparental.protocol.OrdenPolitica
import dev.controlparental.protocol.Proposito
import dev.controlparental.protocol.Rol
import dev.controlparental.protocol.UltimoResultado

/** Consistencia local y firma pública; NO aceptación de comandos ni enforcement. */
internal object ValidadorEstado {
    val UUID_V4 = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")

    fun validar(estado: EstadoControl) {
        require(estado.schemaVersion == 1 && estado.recoveryFailureCount >= 0)
        require(estado.approvedPackages.size <= 32 && estado.approvedPackages.distinct().size == estado.approvedPackages.size)
        require(estado.approvedPackages.all { it.length <= 255 && PAQUETE.matches(it) })
        val boot = estado.bootContext
        if (boot != null) {
            require(boot.systemBootCount == null || boot.systemBootCount >= 0)
            require(UUID_V4.matches(boot.bootId) && boot.ultimoElapsedObservado >= 0)
            require(boot.authorizedBootId == null || boot.authorizedBootId == boot.bootId)
        }
        val vinculo = estado.vinculo
        if (vinculo == null) {
            require(estado.provisioningStage in setOf(EtapaAprovisionamiento.NO_CONFIGURADO, EtapaAprovisionamiento.PREPARACION))
            require(estado.policy == null && estado.lastCommand == null && boot?.authorizedBootId == null)
            require(estado.recoveryConsumed.isEmpty())
            return
        }
        require(estado.provisioningStage !in setOf(EtapaAprovisionamiento.NO_CONFIGURADO, EtapaAprovisionamiento.PREPARACION))
        require(UUID_V4.matches(vinculo.pairId))
        require(vinculo.relayOrigin.length <= 200 && ORIGEN.matches(vinculo.relayOrigin))
        val publicaPadre = ClavePublicaCp1.leer(vinculo.parentPublicKeyB64)
        ClavePublicaCp1.leer(vinculo.childPublicKeyB64)
        listOf(vinculo.recoveryHashes.MAINTENANCE, vinculo.recoveryHashes.RECOVER_PARENT,
            vinculo.recoveryHashes.RETIRE).forEach { require(Base64Cp1.decodificar(it, 32).size == 32) }
        val ultima = estado.lastCommand
        val politica = estado.policy
        require((ultima == null) == (politica == null))
        if (ultima == null || politica == null) {
            require(boot?.authorizedBootId == null)
            return
        }
        require(boot != null && politica.aceptadaEnMs >= 0 && ultima.seq > 0)
        val verificado = CriptografiaCp1.verificar(ultima.signedEnvelope, publicaPadre, Rol.PARENT, Proposito.MESSAGE)
        val orden = verificado.mensaje as? OrdenPolitica ?: throw ErrorEstadoLocal()
        require(orden.pairId == vinculo.pairId && orden.commandId == ultima.commandId && orden.seq == ultima.seq)
        require(verificado.payloadSha256B64 == ultima.payloadSha256B64)
        require(orden.bootId == politica.bootId && orden.action == politica.accion && orden.durationSec == politica.duracionSegundos)
        if (boot.authorizedBootId != null) require(politica.bootId == boot.bootId)
        if (politica.bootId == boot.bootId) require(politica.aceptadaEnMs <= boot.ultimoElapsedObservado)
        val esperado = when (ultima.stage) {
            EtapaAplicacion.ACCEPTED, EtapaAplicacion.ENFORCING -> UltimoResultado.ACCEPTED_PENDING_ENFORCEMENT
            EtapaAplicacion.APPLIED -> UltimoResultado.APPLIED
            EtapaAplicacion.FAILED -> UltimoResultado.ENFORCEMENT_FAILED
        }
        require(ultima.result == esperado)
    }

    private val PAQUETE = Regex("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*")
    private val ORIGEN = Regex("https://[a-z0-9][a-z0-9.-]*(?::[0-9]{1,5})?")
}
