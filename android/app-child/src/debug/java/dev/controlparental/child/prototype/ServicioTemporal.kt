package dev.controlparental.child.prototype

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log

/** FGS solo debug/systemExempted por Device Owner. No sticky, wakelock o supervivencia. */
class ServicioTemporal : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val permiso = PermisoTemporal(SystemClock::elapsedRealtime)
    private var alarma: AlarmManager.OnAlarmListener? = null
    private var operacion = 0L
    private var ocupado = false
    private var entrada: EntradaPrototipo? = null
    private lateinit var politicas: PoliticasPrototipo
    internal var ultimoDiagnostico = "TIMER_NO_LISTO"; private set

    override fun onCreate() {
        super.onCreate()
        try {
            exigir(Build.VERSION.SDK_INT >= 34, Causa.ANDROID_NO_ADMITIDO)
            politicas = PoliticasPrototipo(this)
            politicas.validarCapacidad()
            val nm = getSystemService(NotificationManager::class.java)
            exigir(nm.areNotificationsEnabled(), Causa.NOTIFICACION_NO_VISIBLE)
            nm.createNotificationChannel(NotificationChannel(CANAL, "Ensayo temporal DEBUG", NotificationManager.IMPORTANCE_LOW))
            exigir(nm.getNotificationChannel(CANAL).importance != NotificationManager.IMPORTANCE_NONE,
                Causa.NOTIFICACION_NO_VISIBLE)
            val abrir = PendingIntent.getActivity(this, 20,
                Intent(this, PrototipoBloqueoActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val n = Notification.Builder(this, CANAL).setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Ensayo temporal DEBUG preparado")
                .setContentText("Permiso temporal y retorno a bloqueo. Salida adulta en la app.")
                .setContentIntent(abrir).setOngoing(true).build()
            startForeground(20, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            instancia = this
            registrar("TIMER_LISTO")
        } catch (e: RuntimeException) { registrar("ERROR_${causa(e)}"); stopSelf() }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    private fun boot(): Int = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)

    /** Solo llamada RAM desde callback DEVICE_CREDENTIAL vigente; no Intent autoritativo. */
    internal fun permitir(minutos: Int, emergencia: Boolean) {
        exigir(!ocupado, Causa.CONTEXTO_OBSOLETO)
        exigir(emergencia, Causa.CHECKBOX_FALTA)
        exigir(boot() >= 0, Causa.BOOT_NO_COMPROBADO)
        politicas.validarCapacidad()
        exigir(RollbackPrototipo.comprobar() == Causa.NINGUNA, RollbackPrototipo.comprobar())
        exigir(minutos in 1..1440, Causa.MINUTOS_INVALIDOS)
        ocupado = true
        val g = ++operacion
        // Durante permiso vigente, su baseline ya está guardado: no borrarlo ni renovar antes.
        if (politicas.modoObservado() == ModoPrototipo.NONE) {
            try { aceptar(minutos); ocupado = false }
            catch (e: RuntimeException) { fallarPermiso(e) }
            return
        }
        val salida = EntradaPrototipo(politicas, SystemClock::elapsedRealtime)
        entrada = salida
        salida.salirAutorizado()
        vigilar(salida, g) {
            if (salida.diagnostico.etapa == Etapa.RESTAURADO) {
                try { aceptar(minutos); ocupado = false }
                catch (e: RuntimeException) { fallarPermiso(e) }
            } else { ocupado = false; registrar("PERMISO_NO_CONFIRMADO") }
        }
    }
    private fun aceptar(minutos: Int) {
        politicas.validarCapacidad()
        exigir(getSystemService(NotificationManager::class.java).areNotificationsEnabled(), Causa.NOTIFICACION_NO_VISIBLE)
        if (politicas.pendiente()) politicas.validarBaselineGuardado() else politicas.preparar()
        val b = boot(); exigir(b >= 0, Causa.BOOT_NO_COMPROBADO)
        val ref = try { permiso.preparar(minutos, b) }
        catch (_: RuntimeException) { throw FalloPrototipo(Causa.DEADLINE_INVALIDO) }
        val listener = AlarmManager.OnAlarmListener { vencer(ref) }
        // API34+, listener ligado a este FGS vivo. Sin permiso SCHEDULE_EXACT_ALARM.
        val am = getSystemService(AlarmManager::class.java)
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ref.deadlineMs, "PROT02", listener, handler)
        try { permiso.confirmar(ref) }
        catch (e: RuntimeException) { am.cancel(listener); throw e }
        alarma?.let(am::cancel)
        alarma = listener
        registrar("PERMISO_CONFIRMADO restanteMs=${ref.deadlineMs-SystemClock.elapsedRealtime()}")
    }
    private fun fallarPermiso(e: RuntimeException) {
        ocupado = false
        registrar("PERMISO_ERROR_${causa(e)}")
        if (permiso.actual == null) {
            val salida = EntradaPrototipo(politicas, SystemClock::elapsedRealtime)
            entrada = salida; salida.salirAutorizado()
            vigilar(salida, operacion) {}
        } // Un reemplazo fallido no cancela el permiso anterior ni su deadline.
    }
    private fun vencer(ref: ReferenciaTemporal) {
        val entrega = permiso.entregar(ref, boot())
        registrar("ENTREGA_$entrega retrasoMs=${SystemClock.elapsedRealtime()-ref.deadlineMs}")
        if (entrega != EntregaTemporal.VENCIDA) {
            if (entrega != EntregaTemporal.OBSOLETA) cancelarPlan()
            return // No volver a programar, renovar deadline o fingir bloqueo.
        }
        bloquear(ref)
    }
    internal fun bloquearAhora() {
        exigir(!ocupado && permiso.actual != null, Causa.CONTEXTO_OBSOLETO)
        // Revalidación sin políticas: bloquear otra vez nunca debe convertirse en salida.
        if (politicas.modoObservado() == ModoPrototipo.LOCKED) {
            cancelarPlan(); registrar("LOCKED_YA_OBSERVADO"); return
        }
        val ref = permiso.actual!!
        cancelarPlan()
        bloquear(ref)
    }
    private fun bloquear(ref: ReferenciaTemporal) {
        ocupado = true
        val g = ++operacion
        try {
            politicas.validarCapacidad()
            val automaticas = PoliticasPrototipo(this, reutilizarSnapshot = true, iniciarExterno = {
                val opciones = ActivityOptions.makeBasic().setLockTaskEnabled(true)
                startActivity(Intent(this, PrototipoBloqueoActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK), opciones.toBundle())
            })
            val bloqueo = EntradaPrototipo(automaticas, SystemClock::elapsedRealtime)
            entrada = bloqueo
            registrar("BLOQUEO_SOLICITADO retrasoMs=${SystemClock.elapsedRealtime()-ref.deadlineMs}")
            bloqueo.entrar()
            vigilar(bloqueo, g) {
                ocupado = false
                registrar("FINAL_${bloqueo.diagnostico.etapa} retrasoMs=${SystemClock.elapsedRealtime()-ref.deadlineMs}")
                if (bloqueo.diagnostico.etapa != Etapa.LOCKED_VERIFICADO && politicas.pendiente()) {
                    val salida = EntradaPrototipo(politicas, SystemClock::elapsedRealtime)
                    entrada = salida; salida.salirAutorizado()
                    vigilar(salida, g) {}
                }
            }
        } catch (e: RuntimeException) { registrar("EXPIRACION_ERROR_${causa(e)}"); cancelarPlan() }
    }
    private fun vigilar(modelo: EntradaPrototipo, g: Long, fin: () -> Unit) {
        val tarea = object : Runnable {
            private var anterior: Diagnostico? = null
            override fun run() {
                if (g != operacion) return
                modelo.observar(modelo.generacion)
                if (anterior != modelo.diagnostico) {
                    anterior = modelo.diagnostico
                    registrar("${modelo.diagnostico.etapa}/${modelo.diagnostico.causa}")
                }
                if (modelo.pendiente) handler.postDelayed(this, 100) else fin()
            }
        }
        handler.post(tarea)
    }
    internal fun cancelarPlan() {
        operacion++; ocupado = false; permiso.cancelar()
        alarma?.let { getSystemService(AlarmManager::class.java).cancel(it) }; alarma = null
        registrar("PLAN_CANCELADO")
    }
    override fun onDestroy() {
        cancelarPlan()
        if (instancia === this) instancia = null
        registrar("SERVICIO_TERMINADO_SIN_SUPERVIVENCIA")
        super.onDestroy()
    }
    private fun causa(e: RuntimeException) = (e as? FalloPrototipo)?.causa ?: Causa.ANDROID_ERROR
    private fun registrar(texto: String) { ultimoDiagnostico = texto; Log.i("PROT02", texto) }
    companion object {
        private const val CANAL = "prot02_temporal"
        internal var instancia: ServicioTemporal? = null; private set
        internal fun preparar(contexto: Context) { contexto.startForegroundService(Intent(contexto, ServicioTemporal::class.java)) }
        internal fun terminar(contexto: Context) {
            instancia?.cancelarPlan()
            contexto.stopService(Intent(contexto, ServicioTemporal::class.java))
        }
        internal fun tienePlan(): Boolean = instancia?.permiso?.actual != null
    }
}
