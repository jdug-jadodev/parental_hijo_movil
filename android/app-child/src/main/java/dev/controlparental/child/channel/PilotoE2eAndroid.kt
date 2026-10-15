package dev.controlparental.child.channel

import android.app.Activity
import android.os.Handler
import android.os.Looper
import dev.controlparental.child.network.ControlRedAndroid
import dev.controlparental.protocol.cp2.SyncOrdenCp2
import java.util.concurrent.atomic.AtomicBoolean

/** Recorrido callable real: sesión adulta explícita → TLS/AUTH → guardia/writer/UsoRed → STATE firmado. */
internal class PilotoE2eAndroid(private val actividad: Activity, private val autoridad: () -> Boolean,
    private val estado: (String) -> Unit) : AutoCloseable {
    private val cerrado = AtomicBoolean()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var canal: CanalWssAndroid? = null
    @Volatile private var consulta: Pair<GeneracionCanalCp2, String>? = null
    private val vencimiento = object : Runnable {
        override fun run() {
            if (cerrado.get()) return
            if (!autoridad()) { close(); estado("Canal cerrado: sesión adulta caducada o sin primer plano"); return }
            main.postDelayed(this, 1000)
        }
    }
    private fun vigente(ref: GeneracionCanalCp2) = !cerrado.get() && ref.activa && autoridad()
    fun conectar() {
        if (cerrado.get() || !autoridad()) return
        main.removeCallbacks(vencimiento); main.postDelayed(vencimiento, 1000)
        ControlRedAndroid.iniciarCp2(actividad, { !cerrado.get() && autoridad() }, ::emitirEnActor,
            { emitirEnActor() }) { disponible ->
            if (cerrado.get() || !autoridad()) return@iniciarCp2
            if (!disponible) { estado("No se pudo preparar CONTROL: comprueba pareja, CE y ensayo local/red armado. No se abrió conexión."); return@iniciarCp2 }
            canal = CanalWssAndroid(actividad.applicationContext, { ref, bytes ->
                if (!vigente(ref)) return@CanalWssAndroid
                ControlRedAndroid.recibirCp2(actividad.applicationContext, bytes, { vigente(ref) }) { v, _ ->
                    if (!vigente(ref)) return@recibirCp2
                    (v?.mensaje as? SyncOrdenCp2)?.let { consulta = ref to it.syncNonce }
                    emitirEnActor()
                }
            }, { texto -> main.post { if (!cerrado.get()) estado(texto) } }).also { it.conectar() }
        }
    }
    private fun emitirEnActor(): Boolean {
        val (ref, nonce) = consulta ?: return false
        if (!vigente(ref)) return false
        val bytes = ControlRedAndroid.stateCp2EnActor(actividad.applicationContext, nonce) { vigente(ref) } ?: return false
        return vigente(ref) && canal?.enviar(ref, bytes) == true
    }
    override fun close() {
        if (!cerrado.compareAndSet(false, true)) return
        main.removeCallbacks(vencimiento)
        consulta = null; canal?.close(); canal = null
        ControlRedAndroid.cerrarCanalCp2()
    }
}
