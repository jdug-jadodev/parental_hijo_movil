package dev.controlparental.child.lock

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.AtomicFile
import android.util.Log
import dev.controlparental.child.admin.ChildAdminReceiver
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicReference

internal class ArchivoSnapshotAndroid(private val ruta: File) : ArchivoSnapshotLocal {
    private val atomic = AtomicFile(ruta)
    override fun leer(): ByteArray? {
        val existe = listOf(ruta, File(ruta.path + ".bak"), File(ruta.path + ".new")).any(File::exists)
        if (!existe) return null
        return atomic.openRead().use { f ->
            val salida = ByteArrayOutputStream(); val buffer = ByteArray(1024)
            while (true) {
                val n = f.read(buffer, 0, minOf(buffer.size, CodecSnapshotLocal.MAX + 1 - salida.size()))
                if (n < 0) break
                salida.write(buffer, 0, n); exigirLocal(salida.size() <= CodecSnapshotLocal.MAX, CausaLocal.SNAPSHOT)
            }
            salida.toByteArray()
        }
    }
    override fun escribir(bytes: ByteArray) {
        exigirLocal(bytes.size <= CodecSnapshotLocal.MAX, CausaLocal.SNAPSHOT)
        val f = atomic.startWrite()
        try { f.write(bytes); atomic.finishWrite(f) } catch (e: Exception) { atomic.failWrite(f); throw e }
    }
    override fun borrar() { atomic.delete() }
}

/** Solo whitelist/features y startLockTask oficial. No HOME, restricciones ni autorización CP/1. */
internal class PoliticasLockAndroid(c: Context, private val actividad: Activity? = null,
    private val puedeIniciar: () -> Boolean = { false }) : PoliticasLockLocal {
    private val c = c.applicationContext
    private val dpm = c.getSystemService(DevicePolicyManager::class.java)
    private val am = c.getSystemService(ActivityManager::class.java)
    private val admin = ComponentName(c, ChildAdminReceiver::class.java)
    private val falloInicio = AtomicReference<CausaLocal?>(null)
    override val paquete = c.packageName
    override val featuresPropios = DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD or DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS
    private fun owner() { exigirLocal(dpm.isDeviceOwnerApp(paquete) && dpm.isAdminActive(admin), CausaLocal.BASELINE) }
    override fun validar() {
        owner()
        val kg = c.getSystemService(KeyguardManager::class.java)
        exigirLocal(kg.isDeviceSecure, CausaLocal.BASELINE)
        exigirLocal(!kg.isKeyguardLocked && !kg.isDeviceLocked, CausaLocal.KEYGUARD)
    }
    override fun leerPaquetes(): Set<String> {
        val originales = dpm.getLockTaskPackages(admin)
        val paquetes = paquetesLockLeidos(originales)
        if (originales.size == 1 && originales[0].isEmpty())
            Log.i("BloqueoLocal", "evento=LISTA_VACIA_ANDROID elementos=1 vacios=1")
        return paquetes
    }
    override fun leerFeatures() = dpm.getLockTaskFeatures(admin)
    override fun paquetes(valor: Set<String>) { owner(); dpm.setLockTaskPackages(admin, valor.toTypedArray()) }
    override fun features(valor: Int) { owner(); dpm.setLockTaskFeatures(admin, valor) }
    override fun modo(): ModoLocal {
        return when (am.lockTaskModeState) {
            ActivityManager.LOCK_TASK_MODE_NONE -> {
                falloInicio.get()?.let { throw FalloLocal(it) }
                ModoLocal.NONE
            }
            ActivityManager.LOCK_TASK_MODE_LOCKED -> ModoLocal.LOCKED
            else -> ModoLocal.PINNED
        }
    }
    override fun iniciar() {
        val a = actividad ?: throw FalloLocal(CausaLocal.BASELINE)
        a.runOnUiThread {
            try {
                exigirLocal(puedeIniciar() && !a.isFinishing && !a.isDestroyed, CausaLocal.AUTORIDAD_OBSOLETA)
                validar()
                exigirLocal(leerPaquetes() == setOf(paquete) && leerFeatures() == featuresPropios && dpm.isLockTaskPermitted(paquete), CausaLocal.POLITICA_AJENA)
                a.startLockTask()
            } catch (e: Exception) { falloInicio.set((e as? FalloLocal)?.causa ?: CausaLocal.IO_O_ANDROID) }
        }
    }
    override fun modoRestauracion() = when (am.lockTaskModeState) {
        ActivityManager.LOCK_TASK_MODE_NONE -> ModoLocal.NONE
        ActivityManager.LOCK_TASK_MODE_LOCKED -> ModoLocal.LOCKED
        else -> ModoLocal.PINNED
    }
}
