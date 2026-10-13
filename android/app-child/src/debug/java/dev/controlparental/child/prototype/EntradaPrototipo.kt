package dev.controlparental.child.prototype

internal enum class Etapa { REPOSO, EMERGENCIA, CREDENCIAL, ROLLBACK_ADB, BASELINE,
    ESCRIBIR_PAQUETES, ESCRIBIR_FEATURES, LEER_PAQUETES, LEER_FEATURES,
    START_LOCK_TASK, ESPERAR_LOCKED, LOCKED_VERIFICADO, RESTAURAR, RESTAURADO, ERROR }
internal enum class Causa { NINGUNA, CHECKBOX_FALTA, TICKET_FALTA, TICKET_CADUCADO,
    FONDO, CANCELADO, CALLBACK_OBSOLETO, CREDENCIAL_RECHAZADA, RELOJ_INVALIDO,
    ADB_FALTA, ADB_CADUCADO, NO_DEBUG, NO_OWNER, ADMIN_INACTIVO, SIN_CREDENCIAL,
    CAPACIDAD_NO_DISPONIBLE, KEYGUARD_ACTIVO, SNAPSHOT_PENDIENTE, BASELINE_INCOMPATIBLE,
    SNAPSHOT_NO_GUARDADO, CONTEXTO_OBSOLETO, ANDROID_ERROR, TIMEOUT, PINNED,
    RESTAURACION_AJENA, RESTAURACION_NO_CONFIRMADA }
internal class FalloPrototipo(val causa: Causa) : RuntimeException()
internal data class Diagnostico(val etapa: Etapa, val causa: Causa = Causa.NINGUNA,
    val falloEn: Etapa? = null)
internal enum class ModoPrototipo { NONE, LOCKED, PINNED }
internal interface PuertoEntrada {
    fun preparar()
    fun escribirPaquetes()
    fun escribirFeatures()
    fun paquetesListos(): Boolean
    fun featuresListos(): Boolean
    fun iniciar()
    fun modoObservado(): ModoPrototipo
    fun iniciarRestauracion()
    fun restauracionConfirmada(): Boolean
}

/** Un inicio, readbacks acotados y un rollback por generación; no reintenta writes. */
internal class EntradaPrototipo(private val puerto: PuertoEntrada, private val ahora: () -> Long) {
    var diagnostico = Diagnostico(Etapa.REPOSO); private set
    var generacion = 0L; private set
    private var inicio = 0L
    private var fallo: Diagnostico? = null
    private var preparado = false
    val pendiente: Boolean get() = diagnostico.etapa in setOf(Etapa.LEER_PAQUETES,
        Etapa.LEER_FEATURES, Etapa.ESPERAR_LOCKED, Etapa.RESTAURAR)

    fun entrar(): Long {
        check(!pendiente && diagnostico.etapa != Etapa.LOCKED_VERIFICADO)
        generacion++
        fallo = null
        preparado = false
        inicio = ahora()
        try {
            exigir(inicio >= 0, Causa.RELOJ_INVALIDO)
            diagnostico = Diagnostico(Etapa.BASELINE); puerto.preparar()
            preparado = true
            diagnostico = Diagnostico(Etapa.ESCRIBIR_PAQUETES); puerto.escribirPaquetes()
            diagnostico = Diagnostico(Etapa.ESCRIBIR_FEATURES); puerto.escribirFeatures()
            diagnostico = Diagnostico(Etapa.LEER_PAQUETES)
        } catch (e: RuntimeException) { fallar(causa(e)) }
        return generacion
    }
    fun observar(g: Long) {
        if (g != generacion || !pendiente) return
        try {
            val t = ahora()
            exigir(t >= inicio, Causa.RELOJ_INVALIDO)
            if (diagnostico.etapa == Etapa.RESTAURAR) {
                if (puerto.restauracionConfirmada()) diagnostico = Diagnostico(
                    Etapa.RESTAURADO, fallo?.causa ?: Causa.NINGUNA, fallo?.etapa)
                else if (t - inicio >= 2_000) diagnostico = Diagnostico(
                    Etapa.ERROR, Causa.RESTAURACION_NO_CONFIRMADA, fallo?.etapa)
                return
            }
            exigir(t - inicio < 3_000, Causa.TIMEOUT)
            if (diagnostico.etapa == Etapa.LEER_PAQUETES) {
                if (!puerto.paquetesListos()) return
                diagnostico = Diagnostico(Etapa.LEER_FEATURES)
            }
            if (diagnostico.etapa == Etapa.LEER_FEATURES) {
                if (!puerto.featuresListos()) return
                diagnostico = Diagnostico(Etapa.START_LOCK_TASK); puerto.iniciar()
                diagnostico = Diagnostico(Etapa.ESPERAR_LOCKED)
            }
            when (puerto.modoObservado()) {
                ModoPrototipo.LOCKED -> diagnostico = Diagnostico(Etapa.LOCKED_VERIFICADO)
                ModoPrototipo.PINNED -> fallar(Causa.PINNED)
                ModoPrototipo.NONE -> Unit // Solicitud pendiente, no éxito ni fallo inmediato.
            }
        } catch (e: RuntimeException) { fallar(causa(e)) }
    }
    fun cancelar(causa: Causa) {
        if (pendiente && diagnostico.etapa != Etapa.RESTAURAR) fallar(causa)
    }
    fun salirAutorizado() {
        if (diagnostico.etapa == Etapa.RESTAURAR) return
        fallo = null
        restaurar()
    }
    private fun fallar(causa: Causa) {
        if (diagnostico.etapa == Etapa.RESTAURAR) {
            diagnostico = Diagnostico(Etapa.ERROR, causa, fallo?.etapa); return
        }
        fallo = Diagnostico(diagnostico.etapa, causa)
        if (!preparado) { diagnostico = Diagnostico(Etapa.ERROR, causa, fallo?.etapa); return }
        restaurar()
    }
    private fun restaurar() {
        diagnostico = Diagnostico(Etapa.RESTAURAR, fallo?.causa ?: Causa.NINGUNA, fallo?.etapa)
        inicio = ahora()
        try { puerto.iniciarRestauracion() }
        catch (e: RuntimeException) { diagnostico = Diagnostico(Etapa.ERROR, causa(e), fallo?.etapa) }
    }
    private fun causa(e: RuntimeException) = (e as? FalloPrototipo)?.causa ?: Causa.ANDROID_ERROR
}
internal fun exigir(valor: Boolean, causa: Causa) { if (!valor) throw FalloPrototipo(causa) }
