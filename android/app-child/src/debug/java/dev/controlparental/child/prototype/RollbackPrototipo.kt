package dev.controlparental.child.prototype

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.os.Handler
import android.os.Looper

/** Receptor SOLO debug, protegido por android.permission.DUMP (shell), no app normal. */
class RollbackPrototipo : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val politicas = PoliticasPrototipo(context)
            when (intent.action) {
                COMPROBAR -> {
                    comprobadoEn = null
                    politicas.validarCapacidad()
                    exigir(!politicas.pendiente(), Causa.SNAPSHOT_PENDIENTE)
                    exigir(politicas.modoObservado() == ModoPrototipo.NONE, Causa.BASELINE_INCOMPATIBLE)
                    comprobadoEn = SystemClock.elapsedRealtime()
                    resultCode = 1
                    resultData = "BASELINE/NINGUNA"
                }
                RESTAURAR -> {
                    ServicioTemporal.terminar(context)
                    comprobadoEn = null
                    val resultado = goAsync()
                    val entrada = EntradaPrototipo(politicas, SystemClock::elapsedRealtime)
                    entrada.salirAutorizado()
                    val handler = Handler(Looper.getMainLooper())
                    val lectura = object : Runnable {
                        override fun run() {
                            entrada.observar(entrada.generacion)
                            if (entrada.pendiente) handler.postDelayed(this, 100)
                            else {
                                resultado.setResultCode(if (entrada.diagnostico.etapa == Etapa.RESTAURADO) 1 else 0)
                                resultado.setResultData("${entrada.diagnostico.etapa}/${entrada.diagnostico.causa}")
                                resultado.finish()
                            }
                        }
                    }
                    handler.post(lectura)
                }
                else -> resultCode = 0
            }
        } catch (e: RuntimeException) {
            resultCode = 0
            resultData = "BASELINE/${(e as? FalloPrototipo)?.causa ?: Causa.ANDROID_ERROR}"
        }
    }
    companion object {
        const val COMPROBAR = "dev.controlparental.child.PROT01_COMPROBAR_ADB"
        const val RESTAURAR = "dev.controlparental.child.PROT01_RESTAURAR"
        private var comprobadoEn: Long? = null
        internal fun comprobar(): Causa {
            val t = comprobadoEn ?: return Causa.ADB_FALTA
            val ahora = SystemClock.elapsedRealtime()
            return when {
                ahora < t -> Causa.RELOJ_INVALIDO
                ahora - t >= 300_000 -> Causa.ADB_CADUCADO
                else -> Causa.NINGUNA
            }
        }
        internal fun consumirComprobacion(): Boolean {
            if (comprobar() != Causa.NINGUNA) return false
            comprobadoEn = null
            return true
        }
    }
}
