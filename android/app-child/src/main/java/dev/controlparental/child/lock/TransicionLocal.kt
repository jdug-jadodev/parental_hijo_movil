package dev.controlparental.child.lock

internal enum class ModoLocal { NONE, LOCKED, PINNED }
internal enum class FaseLocal { REPOSO, PREPARANDO, ESPERANDO_LOCKED, LOCKED, RESTAURANDO, RESTAURADO, ERROR }
internal enum class CausaLocal { NINGUNA, AUTORIDAD_OBSOLETA, BASELINE, SNAPSHOT, POLITICA_AJENA, KEYGUARD, PINNED, TIMEOUT, IO_O_ANDROID }
internal data class DiagnosticoLocal(val fase: FaseLocal, val causa: CausaLocal = CausaLocal.NINGUNA)
internal class FalloLocal(val causa: CausaLocal) : RuntimeException()
internal fun exigirLocal(ok: Boolean, causa: CausaLocal) { if (!ok) throw FalloLocal(causa) }
internal interface PuertoLockLocal {
    fun preparar()
    fun aplicar()
    fun solicitarInicio()
    fun modo(): ModoLocal
    fun modoProteccion(): ModoLocal = modo()
    fun restaurar()
    fun restaurado(): Boolean
}

/** Un intento, lectura acotada y rollback previo a LOCKED. Nunca rollback por cerrar sesión ya LOCKED. */
internal class TransicionLocal(private val puerto: PuertoLockLocal, private val ahora: () -> Long,
    private val autoridad: () -> Boolean, private val conservarEnFallo: Boolean = false) {
    var diagnostico = DiagnosticoLocal(FaseLocal.REPOSO); private set
    var generacion = 0L; private set
    private var inicio = 0L
    private var preparado = false
    val pendiente get() = diagnostico.fase in setOf(FaseLocal.ESPERANDO_LOCKED, FaseLocal.RESTAURANDO)
    fun entrar(): Long {
        check(!pendiente); generacion++; inicio = ahora(); preparado = false
        try {
            exigirLocal(autoridad(), CausaLocal.AUTORIDAD_OBSOLETA)
            diagnostico = DiagnosticoLocal(FaseLocal.PREPARANDO)
            puerto.preparar(); preparado = true
            exigirLocal(autoridad(), CausaLocal.AUTORIDAD_OBSOLETA)
            puerto.aplicar()
            exigirLocal(autoridad(), CausaLocal.AUTORIDAD_OBSOLETA)
            puerto.solicitarInicio(); diagnostico = DiagnosticoLocal(FaseLocal.ESPERANDO_LOCKED)
        } catch (e: Exception) { fallar(e) }
        return generacion
    }
    fun salir(): Long {
        check(!pendiente); generacion++; inicio = ahora()
        try {
            exigirLocal(autoridad(), CausaLocal.AUTORIDAD_OBSOLETA)
            puerto.restaurar(); diagnostico = DiagnosticoLocal(FaseLocal.RESTAURANDO)
        } catch (e: Exception) { diagnostico = DiagnosticoLocal(FaseLocal.ERROR, causa(e)) }
        return generacion
    }
    fun observar(g: Long) {
        if (g != generacion || !pendiente) return
        try {
            val t = ahora(); exigirLocal(t >= inicio, CausaLocal.TIMEOUT)
            if (diagnostico.fase == FaseLocal.RESTAURANDO) {
                if (puerto.restaurado()) diagnostico = DiagnosticoLocal(FaseLocal.RESTAURADO, diagnostico.causa)
                else exigirLocal(t - inicio < 2_000, CausaLocal.TIMEOUT)
                return
            }
            // Un LOCKED real no se revoca por autoridad que caducó después de startLockTask.
            when (puerto.modo()) {
                ModoLocal.LOCKED -> { diagnostico = DiagnosticoLocal(FaseLocal.LOCKED); return }
                ModoLocal.PINNED -> throw FalloLocal(CausaLocal.PINNED)
                ModoLocal.NONE -> Unit
            }
            exigirLocal(autoridad(), CausaLocal.AUTORIDAD_OBSOLETA)
            exigirLocal(t - inicio < 3_000, CausaLocal.TIMEOUT)
        } catch (e: Exception) { fallar(e) }
    }
    private fun fallar(e: Exception) {
        val c = causa(e)
        if (conservarEnFallo) { diagnostico = DiagnosticoLocal(FaseLocal.ERROR, c); return }
        if (preparado && diagnostico.fase != FaseLocal.RESTAURANDO) {
            // Si ya quedó LOCKED, un error/cierre no es autoridad para devolver uso.
            try {
                if (puerto.modoProteccion() == ModoLocal.LOCKED) {
                    diagnostico = DiagnosticoLocal(FaseLocal.ERROR, c); return
                }
            } catch (_: Exception) {
                diagnostico = DiagnosticoLocal(FaseLocal.ERROR, c); return
            }
            try { puerto.restaurar(); inicio = ahora(); diagnostico = DiagnosticoLocal(FaseLocal.RESTAURANDO, c) }
            catch (fallo: Exception) { diagnostico = DiagnosticoLocal(FaseLocal.ERROR, causa(fallo)) }
        } else diagnostico = DiagnosticoLocal(FaseLocal.ERROR, c)
    }
    private fun causa(e: Exception) = (e as? FalloLocal)?.causa ?: CausaLocal.IO_O_ANDROID
}
