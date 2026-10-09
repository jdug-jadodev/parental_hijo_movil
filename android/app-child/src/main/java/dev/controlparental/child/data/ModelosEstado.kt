package dev.controlparental.child.data

import dev.controlparental.child.domain.PoliticaAceptada
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.HashesRecuperacion
import dev.controlparental.protocol.SobreSinVerificar
import dev.controlparental.protocol.UltimoResultado

enum class EtapaAplicacion { ACCEPTED, ENFORCING, APPLIED, FAILED }
enum class EtapaAprovisionamiento { NO_CONFIGURADO, PREPARACION, VINCULADO, RECUPERACION_PADRE_PENDIENTE, RETIRADA_PENDIENTE }
enum class PropositoRecuperacionLocal { MAINTENANCE, RECOVER_PARENT, RETIRE }

data class VinculoPublico(
    val pairId: String,
    val parentPublicKeyB64: String,
    val childPublicKeyB64: String,
    val relayOrigin: String,
    val recoveryHashes: HashesRecuperacion,
)

data class ContextoArranque(
    val systemBootCount: Int?,
    val bootId: String,
    val authorizedBootId: String? = null,
    val ultimoElapsedObservado: Long,
)

data class UltimaOrdenDuradera(
    val seq: Long,
    val commandId: String,
    val payloadSha256B64: String,
    val signedEnvelope: SobreSinVerificar,
    val stage: EtapaAplicacion,
    val result: UltimoResultado,
)

/** Snapshot local: no contiene privadas, códigos, contraseñas Wi-Fi ni historial. */
data class EstadoControl(
    val schemaVersion: Int = 1,
    val vinculo: VinculoPublico? = null,
    val recoveryConsumed: Set<PropositoRecuperacionLocal> = emptySet(),
    val recoveryFailureCount: Int = 0,
    val esperaRecuperacionPendiente: Boolean = false,
    val bootContext: ContextoArranque? = null,
    val policy: PoliticaAceptada? = null,
    val lastCommand: UltimaOrdenDuradera? = null,
    val approvedPackages: List<String> = emptyList(),
    val provisioningStage: EtapaAprovisionamiento = EtapaAprovisionamiento.NO_CONFIGURADO,
    val lastEnforcementErrorCode: CodigoCp1? = null,
) {
    val lastAcceptedSeq: Long get() = lastCommand?.seq ?: 0L
}

sealed interface CargaEstado {
    data object Ausente : CargaEstado
    data class Disponible(val estado: EstadoControl) : CargaEstado
    /** No se convierte en Ausente ni autoriza enrolamiento por defecto. */
    data object ErrorSeguro : CargaEstado
}

class ErrorEstadoLocal : IllegalStateException("Estado local inválido o persistencia no comprobada")
