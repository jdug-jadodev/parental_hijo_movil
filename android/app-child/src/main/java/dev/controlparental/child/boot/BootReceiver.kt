package dev.controlparental.child.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.controlparental.child.admin.DeviceCapabilities
import dev.controlparental.child.data.CargaEstado
import dev.controlparental.child.data.EjecutorEstado
import dev.controlparental.child.data.EstadoAndroid
import java.util.concurrent.atomic.AtomicInteger

/** H04 conserva su contexto; piloto LOCAL04 separado solo si armado previamente por adulto. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val protegido = context.applicationContext.createDeviceProtectedStorageContext()
        if (DeviceCapabilities(protegido).leerAdministracion().esDeviceOwner != true) return
        val accion = intent.action
        val desbloqueadoAlRecibir = TrazaArranque.leerDesbloqueo(protegido)
        val pendiente = goAsync()
        val partes = AtomicInteger(2)
        fun terminarParte() { if (partes.decrementAndGet() == 0) pendiente.finish() }
        ArranquePilotoAndroid.senal(protegido, accion ?: "BOOT", ::terminarParte)
        try {
            EjecutorEstado.ejecutar {
                try {
                    val carga = BootContextRepository(EstadoAndroid.obtener(protegido), FuenteArranqueAndroid(protegido)).actualizarContexto()
                    TrazaArranque.registrar(protegido, accion, desbloqueadoAlRecibir, carga)
                    if (carga == CargaEstado.ErrorSeguro) Log.e("EstadoControl", "Contexto local en error seguro; no autorizar uso")
                } catch (_: RuntimeException) {
                    TrazaArranque.registrar(protegido, accion, desbloqueadoAlRecibir, CargaEstado.ErrorSeguro)
                    Log.e("EstadoControl", "No se pudo comprobar el estado local; no autorizar uso")
                } finally {
                    terminarParte()
                }
            }
        } catch (_: RuntimeException) {
            terminarParte()
        }
    }
}
