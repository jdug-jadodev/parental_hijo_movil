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
internal class PoliticasPrototipo(contexto: Context, private val actividad: Activity? = null) : PuertoEntrada {
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
    fun validarCapacidad() {
        exigir(contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0, Causa.NO_DEBUG)
        exigir(dpm.isDeviceOwnerApp(paquete), Causa.NO_OWNER)
        exigir(dpm.isAdminActive(admin), Causa.ADMIN_INACTIVO)
        exigir(contexto.getSystemService(KeyguardManager::class.java).isDeviceSecure, Causa.SIN_CREDENCIAL)
        exigir(contexto.getSystemService(BiometricManager::class.java).canAuthenticate(
            BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS,
            Causa.CAPACIDAD_NO_DISPONIBLE)
    }
    fun capacidad(): Boolean = try { validarCapacidad(); true } catch (_: RuntimeException) { false }

    override fun preparar() {
        validarCapacidad()
        exigir(!pendiente(), Causa.SNAPSHOT_PENDIENTE)
        exigir(modo() == ActivityManager.LOCK_TASK_MODE_NONE, Causa.BASELINE_INCOMPATIBLE)
        val keyguard = contexto.getSystemService(KeyguardManager::class.java)
        exigir(!keyguard.isKeyguardLocked && !keyguard.isDeviceLocked, Causa.KEYGUARD_ACTIVO)
        val paquetes = dpm.getLockTaskPackages(admin).toSet()
        val flags = dpm.getLockTaskFeatures(admin)
        // Retirar el hijo al restaurar debe parar el task; no tomar un quiosco previo.
        exigir(paquete !in paquetes && paquetes.size <= 128 && paquetes.all { it.length <= 255 },
            Causa.BASELINE_INCOMPATIBLE)
        exigir(archivo.edit().putStringSet("paquetes", paquetes).putInt("flags", flags)
            .putBoolean("pendiente", true).commit(), Causa.SNAPSHOT_NO_GUARDADO)
    }
    override fun escribirPaquetes() { dpm.setLockTaskPackages(admin, arrayOf(paquete)) }
    override fun escribirFeatures() { dpm.setLockTaskFeatures(admin, flagsPrueba) }
    override fun paquetesListos(): Boolean {
        exigir(pendiente(), Causa.CONTEXTO_OBSOLETO)
        return dpm.getLockTaskPackages(admin).toSet() == setOf(paquete) && dpm.isLockTaskPermitted(paquete)
    }
    override fun featuresListos() = dpm.getLockTaskFeatures(admin) == flagsPrueba
    override fun iniciar() {
        validarCapacidad()
        exigir(pendiente() && paquetesListos() && featuresListos(), Causa.CONTEXTO_OBSOLETO)
        val keyguard = contexto.getSystemService(KeyguardManager::class.java)
        exigir(!keyguard.isKeyguardLocked && !keyguard.isDeviceLocked, Causa.KEYGUARD_ACTIVO)
        (actividad ?: throw FalloPrototipo(Causa.CONTEXTO_OBSOLETO)).startLockTask()
    }
    override fun modoObservado() = when (modo()) {
        ActivityManager.LOCK_TASK_MODE_NONE -> ModoPrototipo.NONE
        ActivityManager.LOCK_TASK_MODE_LOCKED -> ModoPrototipo.LOCKED
        else -> ModoPrototipo.PINNED
    }

    /** Solo baseline persistido de esta prueba y estados que pudo escribir este adaptador. */
    override fun iniciarRestauracion() {
        if (!pendiente()) return
        check(contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        check(dpm.isDeviceOwnerApp(paquete) && dpm.isAdminActive(admin))
        val paquetes = archivo.getStringSet("paquetes", null)?.toSet()
            ?: throw FalloPrototipo(Causa.RESTAURACION_NO_CONFIRMADA)
        exigir(archivo.contains("flags") && paquete !in paquetes && paquetes.size <= 128,
            Causa.RESTAURACION_NO_CONFIRMADA)
        val flags = archivo.getInt("flags", 0)
        val actuales = dpm.getLockTaskPackages(admin).toSet()
        val flagsActuales = dpm.getLockTaskFeatures(admin)
        exigir(actuales == paquetes || actuales == setOf(paquete), Causa.RESTAURACION_AJENA)
        exigir(flagsActuales == flags || flagsActuales == flagsPrueba, Causa.RESTAURACION_AJENA)
        dpm.setLockTaskPackages(admin, paquetes.toTypedArray())
        dpm.setLockTaskFeatures(admin, flags)
    }
    override fun restauracionConfirmada(): Boolean {
        if (!pendiente()) return modo() == ActivityManager.LOCK_TASK_MODE_NONE
        val paquetes = archivo.getStringSet("paquetes", null)?.toSet() ?: return false
        if (!archivo.contains("flags")) return false
        val flags = archivo.getInt("flags", 0)
        if (dpm.getLockTaskPackages(admin).toSet() != paquetes || dpm.getLockTaskFeatures(admin) != flags ||
            modo() != ActivityManager.LOCK_TASK_MODE_NONE) return false
        return archivo.edit().clear().commit()
    }
}
