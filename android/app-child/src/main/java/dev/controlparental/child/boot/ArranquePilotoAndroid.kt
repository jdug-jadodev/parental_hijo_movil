package dev.controlparental.child.boot

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.os.UserManager
import android.util.Log
import dev.controlparental.child.lock.*
import dev.controlparental.child.protection.PoliticasProteccionAndroid
import dev.controlparental.child.protection.LecturaProteccion
import dev.controlparental.child.ui.LauncherActivity
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import dev.controlparental.child.network.ControlRedAndroid

internal enum class FasePiloto { DESARMADO, ARMADO, PENDIENTE_KEYGUARD, ESPERANDO_ACTIVITY, ESPERANDO_LOCKED, LOCKED, FINALIZANDO, ERROR }

/** Piloto DP, actor separado CP1. Sin servicio/alarma/HOME ni permiso de uso persistido. */
internal object ArranquePilotoAndroid {
    private val ejecutor = Executors.newSingleThreadExecutor { Thread(it, "arranque-piloto") }
    private val handler = Handler(Looper.getMainLooper())
    private val token = AtomicLong()
    @Volatile var fase = FasePiloto.DESARMADO; private set
    @Volatile var enrolado = false; private set
    private var receptor: BroadcastReceiver? = null
    private var lanzamiento: Pair<String, Long>? = null
    @Volatile private var intento: Pair<String, Long>? = null
    private fun almacen(c: Context): AlmacenPiloto {
        val dp = c.applicationContext.createDeviceProtectedStorageContext()
        return AlmacenPiloto(ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "boot-piloto-v1.marca")),
            ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "boot-piloto-v1.registro")), FuenteArranqueAndroid(dp))
    }
    private fun traza(c: Context, evento: String, r: RegistroPiloto? = null) {
        Log.i("PilotoArranque", "evento=$evento fase=$fase boot=${r?.boot} gen=${r?.generacion} elapsed=${SystemClock.elapsedRealtime()}")
    }
    fun consultar(c: Context, entregar: () -> Unit) {
        ejecutor.execute {
            try {
                val r = almacen(c).leer(); enrolado = r != null
                if (r == null) fase = FasePiloto.DESARMADO
                else if (!r.armado) fase = FasePiloto.FINALIZANDO
                else if (fase == FasePiloto.DESARMADO) fase = FasePiloto.ARMADO
            } catch (_: Exception) { enrolado = true; fase = FasePiloto.ERROR }
            handler.post(entregar)
        }
    }
    private fun escuchar(c: Context) {
        if (receptor != null) return
        if (Build.VERSION.SDK_INT < 33) throw FalloPiloto()
        val app = c.applicationContext
        receptor = object : BroadcastReceiver() {
            override fun onReceive(context: Context, i: Intent) {
                if (i.action in setOf(Intent.ACTION_USER_UNLOCKED, Intent.ACTION_USER_PRESENT)) senal(app, i.action!!)
            }
        }.also { app.registerReceiver(it, IntentFilter().apply {
            addAction(Intent.ACTION_USER_UNLOCKED); addAction(Intent.ACTION_USER_PRESENT)
        }, Context.RECEIVER_NOT_EXPORTED) }
    }
    /** Antes de UserUnlocked solo DP; no abrir snapshot CE/registro adulto. */
    fun senal(c: Context, evento: String, terminar: () -> Unit = {}) {
        ejecutor.execute {
            try {
                val r = almacen(c).observar()
                enrolado = r != null
                if (r == null) { fase = FasePiloto.DESARMADO; return@execute }
                if (!r.armado) { fase = FasePiloto.FINALIZANDO; return@execute }
                if (ControlRedAndroid.permiteUsoRAM) return@execute
                escuchar(c); traza(c, evento, r)
                if (!c.getSystemService(UserManager::class.java).isUserUnlocked || c.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                    fase = FasePiloto.PENDIENTE_KEYGUARD; traza(c, "KEYGUARD", r); return@execute
                }
                val ref = r.enrolamiento to r.generacion
                if (lanzamiento == ref || intento == ref) return@execute
                lanzamiento = ref; val g = token.incrementAndGet(); fase = FasePiloto.ESPERANDO_ACTIVITY
                handler.post {
                    if (token.get() != g) return@post
                    try {
                        c.startActivity(Intent(c, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    } catch (_: RuntimeException) { fase = FasePiloto.ERROR }
                }
                handler.postDelayed({
                    if (token.get() == g && fase == FasePiloto.ESPERANDO_ACTIVITY) {
                        fase = FasePiloto.ERROR; traza(c, "TIMEOUT_ACTIVITY", r)
                    }
                }, 5_000)
            } catch (_: Exception) { enrolado = true; fase = FasePiloto.ERROR; traza(c, "ERROR_DP") }
            finally { terminar() }
        }
    }
    fun actividad(a: Activity, primerPlano: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            try {
                val m = almacen(a); val r = m.observar() ?: return@execute
                enrolado = true
                if (!r.armado) { fase = FasePiloto.FINALIZANDO; return@execute }
                if (ControlRedAndroid.permiteUsoRAM) return@execute
                if (!a.getSystemService(UserManager::class.java).isUserUnlocked || a.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                    escuchar(a); fase = FasePiloto.PENDIENTE_KEYGUARD; return@execute
                }
                val ref = r.enrolamiento to r.generacion
                if (intento == ref) return@execute
                exigirPiloto(m.vigente(r)); intento = ref
                val g = token.incrementAndGet(); fase = FasePiloto.ESPERANDO_LOCKED; traza(a, "SOLICITAR_LOCKED", r)
                handler.post {
                    if (token.get() != g) return@post
                    if (!primerPlano() || ControlRedAndroid.permiteUsoRAM) { fase = FasePiloto.ERROR; entregar(); return@post }
                    val ok = BloqueoLocalAndroid.reentrar(a, { token.get() == g && primerPlano() && !ControlRedAndroid.permiteUsoRAM }) { d ->
                        if (token.get() != g) return@reentrar
                        fase = when {
                            d.fase == FaseLocal.LOCKED -> FasePiloto.LOCKED
                            d.causa == CausaLocal.KEYGUARD -> {
                                intento = null; lanzamiento = null; FasePiloto.PENDIENTE_KEYGUARD
                            }
                            else -> FasePiloto.ERROR
                        }
                        traza(a, "READBACK_${d.fase}", r); entregar()
                    }
                    if (!ok) { fase = FasePiloto.ERROR; entregar() }
                }
            } catch (_: Exception) { enrolado = true; fase = FasePiloto.ERROR; traza(a, "ERROR_REENTRADA") }
            finally { handler.post(entregar) }
        }
    }
    fun armar(a: Activity, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            try {
                exigirPiloto(autoridad())
                PoliticasLockAndroid(a).validar()
                exigirPiloto(BloqueoLocalAndroid.modo(a) == ModoLocal.NONE &&
                    ArchivoSnapshotAndroid(File(a.noBackupFilesDir, "lock-local-v1.snapshot")).leer() == null)
                exigirPiloto(ArchivoSnapshotAndroid(File(a.noBackupFilesDir, "proteccion-local-v1.snapshot")).leer() == null &&
                    PoliticasProteccionAndroid(a).leer() == LecturaProteccion(false, false, false, false))
                exigirPiloto(almacen(a).leer() == null && autoridad())
                BloqueoLocalAndroid.prepararEnrolamiento(a)
                val r = almacen(a).armar(autoridad); enrolado = true; fase = FasePiloto.ARMADO
                val g = token.incrementAndGet(); escuchar(a); traza(a, "ARMADO", r)
                handler.post {
                    val ok = BloqueoLocalAndroid.enrolar(a, { token.get() == g && autoridad() }) { d ->
                        if (token.get() != g) return@enrolar
                        fase = if (d.fase == FaseLocal.LOCKED) FasePiloto.LOCKED else FasePiloto.ERROR
                        intento = r.enrolamiento to r.generacion; traza(a, "INICIAL_${d.fase}", r); entregar()
                    }
                    if (!ok) { fase = FasePiloto.ERROR; entregar() }
                }
            } catch (_: Exception) { fase = FasePiloto.ERROR; handler.post(entregar) }
        }
    }
    /** Nueva revocación LOCAL05 invalida un intento anterior; la red reconectada no llama esto. */
    fun exigirBloqueo(c: Context) {
        ejecutor.execute {
            token.incrementAndGet(); intento = null; lanzamiento = null
            senal(c, "BLOQUEO_REQUERIDO_LOCAL05")
        }
    }
    fun finalizar(a: Activity, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            try {
                almacen(a).desarmar(autoridad)
                token.incrementAndGet(); fase = FasePiloto.FINALIZANDO; intento = null; lanzamiento = null
                receptor?.let { a.applicationContext.unregisterReceiver(it) }; receptor = null
                traza(a, "DESARMADO_ANTES_RESTORE")
                handler.post {
                    val ok = BloqueoLocalAndroid.ejecutar(a, true, autoridad) { d ->
                        ejecutor.execute {
                            try {
                                exigirPiloto(d.fase == FaseLocal.RESTAURADO && d.causa == CausaLocal.NINGUNA)
                                almacen(a).finalizar(true); enrolado = false; fase = FasePiloto.DESARMADO
                                traza(a, "FINAL_RESTAURADO")
                            } catch (_: Exception) { fase = FasePiloto.ERROR }
                            handler.post(entregar)
                        }
                    }
                    if (!ok) { fase = FasePiloto.ERROR; entregar() }
                }
            } catch (_: Exception) { fase = FasePiloto.ERROR; handler.post(entregar) }
        }
    }
    /** Solo caller técnico DEBUG/DUMP; MAIN no expone receptor ni extras de desarme. */
    fun finalizarTecnico(c: Context, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            try {
                exigirPiloto(autoridad()); val m = almacen(c); val r = m.leer()
                if (r == null) { fase = FasePiloto.DESARMADO; enrolado = false; handler.post(entregar); return@execute }
                m.desarmar(autoridad); token.incrementAndGet(); fase = FasePiloto.FINALIZANDO
                lanzamiento = null; intento = null
                receptor?.let { c.applicationContext.unregisterReceiver(it) }; receptor = null
                BloqueoLocalAndroid.restaurarPropias(c, autoridad) { d ->
                    ejecutor.execute {
                        try {
                            exigirPiloto(d.fase == FaseLocal.RESTAURADO && d.causa == CausaLocal.NINGUNA)
                            m.finalizar(true); enrolado = false; fase = FasePiloto.DESARMADO
                        } catch (_: Exception) { fase = FasePiloto.ERROR }
                        handler.post(entregar)
                    }
                }
            } catch (_: Exception) { fase = FasePiloto.ERROR; handler.post(entregar) }
        }
    }
}
