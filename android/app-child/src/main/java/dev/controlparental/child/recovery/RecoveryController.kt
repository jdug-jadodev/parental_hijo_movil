package dev.controlparental.child.recovery

import dev.controlparental.child.data.*
import dev.controlparental.child.domain.RelojMonotonico
import dev.controlparental.child.domain.SesionMantenimiento
import dev.controlparental.protocol.*
import java.security.SecureRandom

/** Un controlador por proceso; consumo y fallo duraderos ANTES de conceder acceso RAM. */
class RecoveryController(
    private val almacen: ChildStateStore,
    private val reloj: RelojMonotonico,
    private val contexto: FuenteContextoRecuperacion,
    private val identidad: FirmanteCp1? = null,
    private val nuevoNonce: () -> ByteArray = { ByteArray(32).also { SecureRandom().nextBytes(it) } },
) {
    private data class Espera(val bootId: String, val inicioMs: Long, val duracionMs: Long)
    private data class Sesion(val proposito: PropositoRecuperacionLocal, val vinculo: VinculoPublico,
        val bootId: String, val inicioMs: Long, val exigeCodigoConsumido: Boolean = true)
    private data class Desafio(val vinculo: VinculoPublico, val bootId: String,
        val accion: AccionOffline, val nonce: String, val inicioMs: Long)
    private var espera: Espera? = null
    private var sesion: Sesion? = null
    private var desafio: Desafio? = null

    @Synchronized
    fun usarCodigo(proposito: PropositoRecuperacionLocal, texto: String): RespuestaRecuperacion = try {
        var resultado = RespuestaRecuperacion(ResultadoRecuperacion.NO_DISPONIBLE)
        var nuevaSesion: Sesion? = null
        // La comprobación/consumo vive en la misma transacción que los campos de H04.
        almacen.actualizar { anterior ->
            val estado = anterior ?: throw ErrorEstadoLocal()
            val fuente = contexto.leer()
            val ahora = ahoraValido()
            val boot = estado.bootContext
            val vinculo = estado.vinculo
            if (!fuente.usuarioDesbloqueado || vinculo == null || boot == null || fuente.bootId != boot.bootId) {
                sesion = null
                return@actualizar estado
            }
            require(ahora >= boot.ultimoElapsedObservado)
            val restante = restanteEspera(estado, boot.bootId, ahora)
            if (restante > 0) {
                resultado = RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, restante)
                return@actualizar estado
            }
            val base = estado.copy(esperaRecuperacionPendiente = false)
            val hash = when (proposito) {
                PropositoRecuperacionLocal.MAINTENANCE -> vinculo.recoveryHashes.MAINTENANCE
                PropositoRecuperacionLocal.RECOVER_PARENT -> vinculo.recoveryHashes.RECOVER_PARENT
                PropositoRecuperacionLocal.RETIRE -> vinculo.recoveryHashes.RETIRE
            }
            if (proposito in base.recoveryConsumed || !CodigoRecuperacion.coincide(vinculo.pairId, proposito, texto, hash)) {
                val fallos = if (base.recoveryFailureCount == Int.MAX_VALUE) Int.MAX_VALUE else base.recoveryFailureCount + 1
                val duracion = duracionEspera(fallos)
                resultado = RespuestaRecuperacion(if (duracion > 0) ResultadoRecuperacion.ESPERA else ResultadoRecuperacion.CODIGO_INVALIDO, duracion)
                return@actualizar base.copy(recoveryFailureCount = fallos, esperaRecuperacionPendiente = duracion > 0)
            }
            nuevaSesion = Sesion(proposito, vinculo, boot.bootId, ahora)
            resultado = RespuestaRecuperacion(ResultadoRecuperacion.AUTORIZADA)
            base.copy(recoveryConsumed = base.recoveryConsumed + proposito,
                provisioningStage = when (proposito) {
                    PropositoRecuperacionLocal.MAINTENANCE -> base.provisioningStage
                    PropositoRecuperacionLocal.RECOVER_PARENT -> EtapaAprovisionamiento.RECUPERACION_PADRE_PENDIENTE
                    PropositoRecuperacionLocal.RETIRE -> EtapaAprovisionamiento.RETIRADA_PENDIENTE
                })
        }.also { persistido ->
            // Solo emitir sesión después de escritura + lectura/validación confirmadas.
            if (nuevaSesion != null) sesion = nuevaSesion
            if (persistido.esperaRecuperacionPendiente) {
                val boot = persistido.bootContext ?: throw ErrorEstadoLocal()
                if (resultado.resultado == ResultadoRecuperacion.ESPERA && resultado.esperaRestanteMs == duracionEspera(persistido.recoveryFailureCount)) {
                    espera = Espera(boot.bootId, ahoraValido(), duracionEspera(persistido.recoveryFailureCount))
                }
            } else espera = null
        }
        resultado
    } catch (_: RuntimeException) {
        sesion = null
        espera = null // Un fallo reimpone la espera persistida completa, no la elimina.
        RespuestaRecuperacion(ResultadoRecuperacion.ERROR_SEGURO)
    }

    @Synchronized
    fun crearDesafioOffline(accion: AccionOffline): RespuestaDesafioOffline {
        val anterior = desafio
        desafio = null // Incluso un intento fallido retira el desafío anterior de la pantalla.
        return try {
            val firmante = identidad ?: return RespuestaDesafioOffline(ResultadoRecuperacion.NO_DISPONIBLE)
            val carga = almacen.cargar() as? CargaEstado.Disponible ?: throw ErrorEstadoLocal()
            val estado = carga.estado
            val fuente = contexto.leer()
            val boot = estado.bootContext
            val vinculo = estado.vinculo
            if (!fuente.usuarioDesbloqueado || vinculo == null || boot == null || fuente.bootId != boot.bootId) {
                return RespuestaDesafioOffline(ResultadoRecuperacion.NO_DISPONIBLE)
            }
            val ahora = ahoraValido()
            require(ahora >= boot.ultimoElapsedObservado)
            require(firmante.clavePublica.base64 == vinculo.childPublicKeyB64)
            val nonce = nuevoNonce().also { require(it.size == 32) }.let(Base64Cp1::codificar)
            require(nonce != anterior?.nonce)
            val mensaje = MensajeOffline(vinculo.pairId, boot.bootId, accion, nonce, esRespuesta = false)
            val sobre = CriptografiaCp1.firmar(mensaje, Proposito.OFFLINE, Rol.CHILD, firmante)
            val despues = contexto.leer()
            val alTerminar = ahoraValido()
            require(despues.usuarioDesbloqueado && despues.bootId == boot.bootId &&
                alTerminar >= ahora && alTerminar - ahora < SESION_MS)
            desafio = Desafio(vinculo, boot.bootId, accion, nonce, ahora)
            RespuestaDesafioOffline(ResultadoRecuperacion.DESAFIO_CREADO, sobre)
        } catch (_: RuntimeException) {
            RespuestaDesafioOffline(ResultadoRecuperacion.ERROR_SEGURO)
        }
    }

    @Synchronized
    fun aceptarRespuestaOffline(bytes: ByteArray): RespuestaRecuperacion {
        val pendiente = desafio ?: return RespuestaRecuperacion(ResultadoRecuperacion.NO_DISPONIBLE)
        if (bytes.size !in 1..JsonCp1.MAX_FRAME_BYTES) return RespuestaRecuperacion(ResultadoRecuperacion.RESPUESTA_INVALIDA)
        return try {
            var resultado = RespuestaRecuperacion(ResultadoRecuperacion.RESPUESTA_INVALIDA)
            var nuevaSesion: Sesion? = null
            val entrada = bytes.copyOf()
            almacen.actualizar { anterior ->
                val estado = anterior ?: throw ErrorEstadoLocal()
                val fuente = contexto.leer()
                val ahora = ahoraValido()
                val boot = estado.bootContext
                if (!fuente.usuarioDesbloqueado || estado.vinculo != pendiente.vinculo ||
                    boot?.bootId != pendiente.bootId || fuente.bootId != pendiente.bootId) {
                    desafio = null
                    resultado = RespuestaRecuperacion(ResultadoRecuperacion.NO_DISPONIBLE)
                    return@actualizar estado
                }
                if (ahora < pendiente.inicioMs || ahora < boot.ultimoElapsedObservado || ahora - pendiente.inicioMs >= SESION_MS) {
                    desafio = null
                    resultado = RespuestaRecuperacion(ResultadoRecuperacion.DESAFIO_VENCIDO)
                    return@actualizar estado
                }
                // La espera de códigos NO intercepta un QR válido. Clave solo del vínculo local.
                val verificado = try {
                    CriptografiaCp1.verificarFrame(entrada, ClavePublicaCp1.leer(pendiente.vinculo.parentPublicKeyB64),
                        Rol.PARENT, Proposito.OFFLINE)
                } catch (_: ErrorProtocolo) {
                    return@actualizar estado
                }
                val respuesta = verificado.mensaje as? MensajeOffline ?: return@actualizar estado
                if (!respuesta.esRespuesta || respuesta.pairId != pendiente.vinculo.pairId ||
                    respuesta.bootId != pendiente.bootId || respuesta.action != pendiente.accion ||
                    respuesta.nonce != pendiente.nonce) return@actualizar estado
                val alConfirmar = ahoraValido()
                val fuenteFinal = contexto.leer()
                if (!fuenteFinal.usuarioDesbloqueado || fuenteFinal.bootId != pendiente.bootId) {
                    desafio = null
                    resultado = RespuestaRecuperacion(ResultadoRecuperacion.NO_DISPONIBLE)
                    return@actualizar estado
                }
                if (alConfirmar < ahora || alConfirmar - pendiente.inicioMs >= SESION_MS) {
                    desafio = null
                    resultado = RespuestaRecuperacion(ResultadoRecuperacion.DESAFIO_VENCIDO)
                    return@actualizar estado
                }
                desafio = null // Consumir en RAM antes de confirmar; crash/reinicio también lo pierden.
                val proposito = when (pendiente.accion) {
                    AccionOffline.MAINTENANCE -> PropositoRecuperacionLocal.MAINTENANCE
                    AccionOffline.RETIRE -> PropositoRecuperacionLocal.RETIRE
                }
                nuevaSesion = Sesion(proposito, pendiente.vinculo, pendiente.bootId, alConfirmar, exigeCodigoConsumido = false)
                resultado = RespuestaRecuperacion(ResultadoRecuperacion.AUTORIZADA)
                // No consumir códigos impresos, borrar esperas, autorizar boot ni tocar política/seq.
                if (proposito == PropositoRecuperacionLocal.RETIRE) estado.copy(provisioningStage = EtapaAprovisionamiento.RETIRADA_PENDIENTE)
                else estado
            }
            if (nuevaSesion != null) sesion = nuevaSesion // Únicamente tras commit/readback confirmado.
            resultado
        } catch (_: RuntimeException) {
            desafio = null
            sesion = null
            RespuestaRecuperacion(ResultadoRecuperacion.ERROR_SEGURO)
        }
    }

    @Synchronized
    fun accesoActual(): AccesoRecuperacionLimitado? {
        return try {
            val actual = sesion ?: return null
            val carga = almacen.cargar() as? CargaEstado.Disponible ?: return cancelarYSalir()
            val fuente = contexto.leer()
            val ahora = ahoraValido()
            val boot = carga.estado.bootContext ?: return cancelarYSalir()
            val duracion = SESION_MS - (ahora - actual.inicioMs)
            if (!fuente.usuarioDesbloqueado || fuente.bootId != actual.bootId || boot.bootId != actual.bootId ||
                carga.estado.vinculo != actual.vinculo ||
                (actual.exigeCodigoConsumido && actual.proposito !in carga.estado.recoveryConsumed) || ahora < actual.inicioMs ||
                ahora < boot.ultimoElapsedObservado || duracion <= 0) {
                cancelarYSalir()
            } else AccesoRecuperacionLimitado(actual.proposito, actual.bootId, duracion)
        } catch (_: RuntimeException) {
            cancelarYSalir()
        }
    }

    @Synchronized
    fun mantenimientoActual(): SesionMantenimiento? {
        if (accesoActual()?.proposito != PropositoRecuperacionLocal.MAINTENANCE) return null
        val actual = sesion ?: return null
        return SesionMantenimiento(actual.bootId, actual.inicioMs)
    }

    @Synchronized
    fun cancelar() { sesion = null; desafio = null }

    private fun cancelarYSalir(): AccesoRecuperacionLimitado? { sesion = null; return null }
    private fun ahoraValido() = reloj.ahoraMs().also { require(it >= 0) }

    private fun restanteEspera(estado: EstadoControl, bootId: String, ahora: Long): Long {
        if (!estado.esperaRecuperacionPendiente) { espera = null; return 0 }
        val duracion = duracionEspera(estado.recoveryFailureCount)
        require(duracion > 0)
        val previa = espera
        if (previa == null || previa.bootId != bootId || previa.duracionMs != duracion || ahora < previa.inicioMs) {
            // Nuevo proceso/arranque, reloj incierto o cambio de umbral: repetir completa.
            espera = Espera(bootId, ahora, duracion)
            return duracion
        }
        return maxOf(0, duracion - (ahora - previa.inicioMs))
    }

    private fun duracionEspera(fallos: Int) = when {
        fallos >= 10 -> 300000L
        fallos >= 5 -> 60000L
        else -> 0L
    }

    companion object { const val SESION_MS = 300000L }
}
