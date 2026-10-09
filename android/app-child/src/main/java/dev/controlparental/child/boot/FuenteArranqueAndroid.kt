package dev.controlparental.child.boot

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

class FuenteArranqueAndroid(contexto: Context) : FuenteArranque {
    private val contexto = contexto.applicationContext.createDeviceProtectedStorageContext()
    override fun observar(): ObservacionArranque {
        val contador = try {
            Settings.Global.getInt(contexto.contentResolver, Settings.Global.BOOT_COUNT).takeIf { it >= 0 }
        } catch (_: Settings.SettingNotFoundException) {
            null
        } catch (_: RuntimeException) {
            null
        }
        return ObservacionArranque(contador, SystemClock.elapsedRealtime())
    }
}
