package dev.controlparental.child.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dev.controlparental.child.ui.LauncherActivity

/** systemExempted por Device Owner real, observación visible, no sticky/wakelock ni tráfico. */
class ControlRedService : Service() {
    private var registrado = false
    private val desbloqueo = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { ControlRedAndroid.senal(c) }
    }
    override fun onCreate() {
        super.onCreate()
        try {
            check(Build.VERSION.SDK_INT >= 34 && getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(packageName))
            val nm = getSystemService(NotificationManager::class.java)
            check(nm.areNotificationsEnabled())
            nm.createNotificationChannel(NotificationChannel("control-red-local", "Control local de red", NotificationManager.IMPORTANCE_LOW))
            check(nm.getNotificationChannel("control-red-local").importance != NotificationManager.IMPORTANCE_NONE)
            val abrir = PendingIntent.getActivity(this, 105, Intent(this, LauncherActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val n = Notification.Builder(this, "control-red-local").setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Control local de red activo")
                .setContentText("Uso NORMAL exige internet validada. Excepción OFFLINE solo adulta LOCAL.")
                .setContentIntent(abrir).setOngoing(true).build()
            if (Build.VERSION.SDK_INT < 34) error("API no admitida")
            startForeground(105, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            ControlRedAndroid.confirmarForeground()
            registerReceiver(desbloqueo, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            }, Context.RECEIVER_NOT_EXPORTED)
            registrado = true
            ControlRedAndroid.observar(applicationContext)
        } catch (_: RuntimeException) {
            ControlRedAndroid.servicioTerminado(applicationContext)
            stopSelf()
        }
    }
    override fun onStartCommand(i: Intent?, flags: Int, startId: Int) = START_NOT_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() {
        if (registrado) unregisterReceiver(desbloqueo)
        ControlRedAndroid.servicioTerminado(applicationContext)
        super.onDestroy()
    }
}
