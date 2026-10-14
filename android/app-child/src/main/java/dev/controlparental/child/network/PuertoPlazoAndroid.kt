package dev.controlparental.child.network

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.Handler
import java.util.concurrent.Executor

/** Listener API34 sin permiso especial; pertenece al FGS vivo, no sobrevive al proceso ni evita Doze. */
internal class PuertoPlazoAndroid(c: Context, private val handler: Handler, private val actor: Executor) : PuertoPlazo {
    private val alarmas = c.applicationContext.getSystemService(AlarmManager::class.java)
    private var listener: AlarmManager.OnAlarmListener? = null
    override fun programar(deadline: Long, entregar: () -> Unit) {
        check(Build.VERSION.SDK_INT >= 34)
        check(listener == null)
        lateinit var l: AlarmManager.OnAlarmListener
        l = AlarmManager.OnAlarmListener { actor.execute {
            if (listener === l) listener = null
            entregar()
        } }
        listener = l
        alarmas.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, "uso-local-plazo", l, handler)
    }
    override fun cancelar() {
        val l = listener; listener = null
        if (l != null) alarmas.cancel(l)
    }
}
