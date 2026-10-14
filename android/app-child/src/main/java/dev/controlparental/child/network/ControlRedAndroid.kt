package dev.controlparental.child.network

import android.app.Activity
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.os.Build
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import dev.controlparental.child.boot.ArranquePilotoAndroid
import dev.controlparental.child.boot.FasePiloto
import dev.controlparental.child.lock.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/** Observador de default vive con FGS, no con Activity. Permisos RAM separados de sesión adulta. */
internal object ControlRedAndroid {
    private val ejecutor = Executors.newSingleThreadExecutor { Thread(it, "control-red-local") }
    private val handler = Handler(Looper.getMainLooper())
    private val epoca = AtomicLong()
    private val visible = ObservadorVisible()
    private val servicioFallido = AtomicBoolean()
    private var modelo: UsoRedLocal? = null
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var gracia: Runnable? = null
    private var revision = 0L
    private var cerrando = false
    @Volatile var activo = false; private set
    @Volatile var listo = false; private set
    @Volatile var uso = UsoLocal.BLOQUEADO; private set
    @Volatile var validacion = ValidacionRed.DESCONOCIDA; private set
    @Volatile var deadline: Long? = null; private set
    @Volatile var permiteUsoRAM = false; private set
    @Volatile private var referenciaPublica: ReferenciaRed? = null
    private fun crear(c: Context): UsoRedLocal {
        val dp = c.applicationContext.createDeviceProtectedStorageContext()
        return UsoRedLocal(ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "uso-red-v1.marca")),
            ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "uso-red-v1.registro")), SystemClock::elapsedRealtime,
            { Settings.Global.getInt(dp.contentResolver, Settings.Global.BOOT_COUNT, -1) })
    }
    private fun publicar(m: UsoRedLocal, evento: String) {
        uso = m.uso; validacion = m.red; deadline = m.deadline; referenciaPublica = m.referencia
        permiteUsoRAM = m.permiteUso && listo
        Log.i("ControlRedLocal", "evento=$evento uso=$uso red=$validacion gen=${m.referencia?.generacion} elapsed=${SystemClock.elapsedRealtime()} deadline=$deadline")
    }
    fun consultar(c: Context, entregar: () -> Unit) {
        ejecutor.execute {
            if (modelo == null) {
                val dp = c.createDeviceProtectedStorageContext()
                val a = ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "uso-red-v1.marca"))
                val b = ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "uso-red-v1.registro"))
                try {
                    activo = a.leer() != null || b.leer() != null
                    if (activo) modelo = crear(c).also { it.cargar(); publicar(it, "RECUPERACION_BLOQUEADA") }
                } catch (_: Exception) { activo = true; uso = UsoLocal.ERROR; permiteUsoRAM = false }
            }
            handler.post(entregar)
        }
    }
    fun preparar(c: Context, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            val m = crear(c)
            if (!autoridad() || !ArranquePilotoAndroid.enrolado || !m.preparar(autoridad)) {
                uso = UsoLocal.ERROR; handler.post(entregar); return@execute
            }
            modelo = m; activo = true; cerrando = false; publicar(m, "PREPARADO_BLOQUEADO")
            handler.post {
                if (!autoridad()) { entregar(); return@post }
                try { c.startForegroundService(Intent(c, ControlRedService::class.java)) }
                catch (_: RuntimeException) { servicioTerminado(c) }
                entregar()
            }
        }
    }
    fun reanudarObservador(c: Context, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            val m = modelo
            if (m == null || m.uso != UsoLocal.BLOQUEADO || !autoridad() || !ArranquePilotoAndroid.enrolado) {
                handler.post(entregar); return@execute
            }
            cerrando = false
            handler.post {
                if (autoridad()) try { c.startForegroundService(Intent(c, ControlRedService::class.java)) }
                catch (_: RuntimeException) { servicioTerminado(c) }
                entregar()
            }
        }
    }
    private fun actual(cm: ConnectivityManager): ValidacionRed = try {
        val n = cm.activeNetwork; val caps = n?.let(cm::getNetworkCapabilities)
        seleccionarDefault(n?.networkHandle, cm.activeNetwork?.networkHandle,
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
    } catch (_: RuntimeException) { ValidacionRed.DESCONOCIDA }
    fun confirmarForeground() { servicioFallido.set(false); visible.confirmarForeground() }
    private fun condiciones(c: Context): CondicionesObservador = try {
        val nm = c.getSystemService(NotificationManager::class.java)
        CondicionesObservador(Build.VERSION.SDK_INT >= 34,
            c.getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(c.packageName),
            nm.areNotificationsEnabled(),
            nm.getNotificationChannel("control-red-local")?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true)
    } catch (_: RuntimeException) { CondicionesObservador(false, false, false, false) }
    fun observar(c: Context) {
        ejecutor.execute {
            if (callback != null || cerrando) return@execute
            val m = modelo ?: crear(c).also { modelo = it; it.cargar() }
            val cm = c.getSystemService(ConnectivityManager::class.java)
            val g = epoca.incrementAndGet(); m.iniciarObservador(g)
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { senal(c, g) }
                override fun onLost(network: Network) { senal(c, g) }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { senal(c, g) }
            }
            try {
                cm.registerDefaultNetworkCallback(cb); callback = cb; visible.confirmarCallback()
                listo = visible.vigente(condiciones(c))
                if (!listo) { servicioTerminado(c); return@execute }
                evaluar(c, g)
            } catch (_: RuntimeException) { servicioTerminado(c) }
        }
    }
    fun senal(c: Context, g: Long = epoca.get()) { ejecutor.execute { evaluar(c, g) } }
    private fun evaluar(c: Context, g: Long) {
        if (g != epoca.get() || cerrando || !listo) return
        if (!visible.vigente(condiciones(c))) { servicioTerminado(c); return }
        val m = modelo ?: return
        val permitia = m.permiteUso
        val ahora = actual(c.getSystemService(ConnectivityManager::class.java))
        m.observar(ahora, g, ++revision); publicar(m, "RED_ACTUAL")
        if (permitia && !m.permiteUso) ArranquePilotoAndroid.exigirBloqueo(c)
        programar(c, m, g)
    }
    private fun programar(c: Context, m: UsoRedLocal, g: Long) {
        gracia?.let(handler::removeCallbacks); gracia = null
        val d = m.deadline ?: return; val ref = m.referencia ?: return
        val r = Runnable { ejecutor.execute {
            if (epoca.get() != g || cerrando || modelo?.referencia != ref) return@execute
            val antes = m.permiteUso; m.comprobarDeadline(ref); publicar(m, "GRACIA_VENCIDA")
            if (antes && !m.permiteUso) ArranquePilotoAndroid.exigirBloqueo(c)
        } }
        gracia = r; handler.postDelayed(r, maxOf(0, d - SystemClock.elapsedRealtime()))
    }
    fun permitir(a: Activity, tipo: UsoLocal, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            val m = modelo
            if (m == null || !listo || cerrando || !autoridad()) { handler.post(entregar); return@execute }
            evaluar(a, epoca.get()) // Lectura default actual, no capacidades del callback antiguo.
            if (!visible.autorizar(m, tipo, condiciones(a), autoridad)) {
                publicar(m, "AUTORIZACION_RECHAZADA")
                if (!visible.vigente(condiciones(a))) servicioTerminado(a)
                handler.post(entregar); return@execute
            }
            val ref = m.referencia!!; publicar(m, "AUTORIZACION_LOCAL_$tipo")
            handler.post {
                val aceptada = BloqueoLocalAndroid.usarGuardado(a, {
                    permiteUsoRAM && referenciaPublica == ref && visible.vigente(condiciones(a)) && autoridad()
                }) { d ->
                    ejecutor.execute {
                        if (m.referencia != ref) return@execute
                        if (d.fase != FaseLocal.RESTAURADO) {
                            m.falloEnforcement(ref); publicar(m, "ERROR_ABRIR"); ArranquePilotoAndroid.exigirBloqueo(a)
                        } else publicar(m, "USO_APLICADO_NONE")
                        handler.post(entregar)
                    }
                }
                if (!aceptada) ejecutor.execute {
                    if (m.referencia == ref) {
                        m.falloEnforcement(ref); publicar(m, "NO_INICIADO"); ArranquePilotoAndroid.exigirBloqueo(a)
                    }
                    handler.post(entregar)
                }
            }
        }
    }
    fun bloquear(c: Context, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            val m = modelo
            if (m != null && autoridad()) {
                m.bloquear(autoridad); publicar(m, "BLOQUEAR_ADULTO")
                if (!m.permiteUso) ArranquePilotoAndroid.exigirBloqueo(c)
            }
            handler.post(entregar)
        }
    }
    private fun cancelar(c: Context) {
        visible.invalidar()
        epoca.incrementAndGet(); listo = false; permiteUsoRAM = false
        gracia?.let(handler::removeCallbacks); gracia = null
        val anterior = callback; callback = null
        anterior?.let { try { c.getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) } catch (_: RuntimeException) { uso = UsoLocal.ERROR } }
    }
    fun servicioTerminado(c: Context) {
        visible.invalidar(); listo = false; permiteUsoRAM = false
        if (servicioFallido.getAndSet(true)) return
        ejecutor.execute {
            cancelar(c)
            if (!cerrando) {
                modelo?.let { m -> m.referencia?.let(m::falloEnforcement); publicar(m, "SERVICIO_TERMINADO"); ArranquePilotoAndroid.exigirBloqueo(c) }
                handler.post { c.stopService(Intent(c, ControlRedService::class.java)) }
            }
        }
    }
    fun finalizar(a: Activity, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            val m = modelo ?: crear(a).also { modelo = it; it.cargar() }
            if (!m.cerrar(autoridad)) { publicar(m, "ERROR_CIERRE"); handler.post(entregar); return@execute }
            cerrando = true; cancelar(a); publicar(m, "DESACTIVAR_OBSERVER_ANTES_RESTORE")
            handler.post {
                a.stopService(Intent(a, ControlRedService::class.java))
                ArranquePilotoAndroid.finalizar(a, autoridad) {
                    ejecutor.execute {
                        if (m.finalizar(ArranquePilotoAndroid.fase == FasePiloto.DESARMADO)) { modelo = null; activo = false }
                        publicar(m, "FINAL_RESTAURADO"); handler.post(entregar)
                    }
                }
            }
        }
    }
    /** Solo autoridad técnica proporcionada por receptor DEBUG/DUMP; sin entrada MAIN exportada. */
    fun finalizarTecnico(c: Context, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            if (!autoridad()) { handler.post(entregar); return@execute }
            val m = modelo
            if (m == null && !activo) { handler.post(entregar); return@execute }
            val cargado = m ?: crear(c).also { modelo = it; it.cargar() }
            if (!cargado.cerrar(autoridad)) { publicar(cargado, "ERROR_CIERRE_TECNICO"); handler.post(entregar); return@execute }
            cerrando = true; cancelar(c); publicar(cargado, "DESACTIVAR_TECNICO")
            handler.post {
                c.stopService(Intent(c, ControlRedService::class.java))
                ArranquePilotoAndroid.finalizarTecnico(c, autoridad) {
                    ejecutor.execute {
                        if (cargado.finalizar(ArranquePilotoAndroid.fase == FasePiloto.DESARMADO)) { modelo = null; activo = false }
                        publicar(cargado, "FINAL_TECNICO"); handler.post(entregar)
                    }
                }
            }
        }
    }
}
