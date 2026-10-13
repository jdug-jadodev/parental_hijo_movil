package dev.controlparental.child.prototype

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.hardware.biometrics.BiometricManager
import dev.controlparental.child.admin.ChildAdminReceiver

/** Solo dos políticas DPM. No HOME, suspensión, restricciones, claves ni estado CP/1. */
internal class PoliticasPrototipo(contexto: Context) {
    private val contexto = contexto.applicationContext
    private val dpm = contexto.getSystemService(DevicePolicyManager::class.java)
    private val am = contexto.getSystemService(ActivityManager::class.java)
    private val admin = ComponentName(contexto, ChildAdminReceiver::class.java)
    private val archivo = contexto.getSharedPreferences("prot01_politicas", Context.MODE_PRIVATE)
    private val paquete = contexto.packageName
    private val flagsPrueba = DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD or
        DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS

    fun modo(): Int = am.lockTaskModeState
    fun pendiente(): Boolean = archivo.getBoolean("pendiente", false)
    fun capacidad(): Boolean =
        contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 &&
            dpm.isDeviceOwnerApp(paquete) && dpm.isAdminActive(admin) &&
            contexto.getSystemService(KeyguardManager::class.java).isDeviceSecure &&
            contexto.getSystemService(BiometricManager::class.java).canAuthenticate(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    fun entrar(actividad: Activity) {
        check(capacidad() && !pendiente() && modo() == ActivityManager.LOCK_TASK_MODE_NONE)
        val keyguard = contexto.getSystemService(KeyguardManager::class.java)
        check(!keyguard.isKeyguardLocked && !keyguard.isDeviceLocked)
        val paquetes = dpm.getLockTaskPackages(admin).toSet()
        val flags = dpm.getLockTaskFeatures(admin)
        // Retirar el hijo al restaurar debe parar el task; no tomar un quiosco previo.
        check(paquete !in paquetes && paquetes.size <= 128 && paquetes.all { it.length <= 255 })
        check(archivo.edit().putStringSet("paquetes", paquetes).putInt("flags", flags)
            .putBoolean("pendiente", true).commit())
        try {
            dpm.setLockTaskPackages(admin, arrayOf(paquete))
            dpm.setLockTaskFeatures(admin, flagsPrueba)
            check(dpm.getLockTaskPackages(admin).toSet() == setOf(paquete))
            check(dpm.getLockTaskFeatures(admin) == flagsPrueba && dpm.isLockTaskPermitted(paquete))
            actividad.startLockTask()
            check(modo() == ActivityManager.LOCK_TASK_MODE_LOCKED)
        } catch (error: RuntimeException) {
            // Si falta readback se conserva snapshot para rollback externo, no fingir restauración.
            restaurar()
            throw error
        }
    }

    /** Solo baseline persistido de esta prueba y estados que pudo escribir este adaptador. */
    fun restaurar(): Boolean {
        if (!pendiente()) return modo() == ActivityManager.LOCK_TASK_MODE_NONE
        check(contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        check(dpm.isDeviceOwnerApp(paquete) && dpm.isAdminActive(admin))
        val paquetes = archivo.getStringSet("paquetes", null)?.toSet() ?: return false
        if (!archivo.contains("flags") || paquete in paquetes || paquetes.size > 128) return false
        val flags = archivo.getInt("flags", 0)
        val actuales = dpm.getLockTaskPackages(admin).toSet()
        val flagsActuales = dpm.getLockTaskFeatures(admin)
        if (actuales != paquetes && actuales != setOf(paquete)) return false
        if (flagsActuales != flags && flagsActuales != flagsPrueba) return false
        dpm.setLockTaskPackages(admin, paquetes.toTypedArray())
        dpm.setLockTaskFeatures(admin, flags)
        if (dpm.getLockTaskPackages(admin).toSet() != paquetes || dpm.getLockTaskFeatures(admin) != flags ||
            modo() != ActivityManager.LOCK_TASK_MODE_NONE) return false
        return archivo.edit().clear().commit()
    }
}
