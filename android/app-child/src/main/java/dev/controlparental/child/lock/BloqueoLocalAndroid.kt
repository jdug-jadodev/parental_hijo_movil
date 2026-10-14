package dev.controlparental.child.lock

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Actor RAM para piloto MAIN. Snapshot durable NO significa supervivencia de enforcement. */
internal object BloqueoLocalAndroid {
    private val ejecutor = Executors.newSingleThreadExecutor { Thread(it, "lock-local") }
    private val handler = Handler(Looper.getMainLooper())
    private val ocupado = AtomicBoolean()
    private val operacion = AtomicLong()
    @Volatile var diagnostico = DiagnosticoLocal(FaseLocal.REPOSO); private set
    private fun archivo(c: Context) = ArchivoSnapshotAndroid(File(c.noBackupFilesDir, "lock-local-v1.snapshot"))
    fun modo(c: Context): ModoLocal = when (c.getSystemService(ActivityManager::class.java).lockTaskModeState) {
        ActivityManager.LOCK_TASK_MODE_NONE -> ModoLocal.NONE
        ActivityManager.LOCK_TASK_MODE_LOCKED -> ModoLocal.LOCKED
        else -> ModoLocal.PINNED
    }
    fun ejecutar(a: Activity, salir: Boolean, autoridad: () -> Boolean,
        entregar: (DiagnosticoLocal) -> Unit): Boolean = transicionar(a, salir, false, autoridad, entregar)
    fun reentrar(a: Activity, autoridad: () -> Boolean, entregar: (DiagnosticoLocal) -> Unit): Boolean =
        transicionar(a, false, true, autoridad, entregar, true)
    fun enrolar(a: Activity, autoridad: () -> Boolean, entregar: (DiagnosticoLocal) -> Unit): Boolean =
        transicionar(a, false, false, autoridad, entregar, true)
    private fun transicionar(a: Activity, salir: Boolean, reentrada: Boolean, autoridad: () -> Boolean,
        entregar: (DiagnosticoLocal) -> Unit, conservar: Boolean = false): Boolean {
        if (!autoridad() || !ocupado.compareAndSet(false, true)) return false
        val g = operacion.incrementAndGet()
        val inicioHabilitado = AtomicBoolean(true)
        val p = PoliticasLockAndroid(a, a) { inicioHabilitado.get() && operacion.get() == g && autoridad() }
        val adaptador = AdaptadorLockLocal(archivo(a), p)
        val puerto = if (!conservar) adaptador else object : PuertoLockLocal by adaptador {
            override fun preparar() { if (reentrada) adaptador.comprobarReentrada() else adaptador.preparar() }
            override fun aplicar() { if (reentrada) adaptador.comprobarReentrada() else adaptador.aplicar() }
            override fun solicitarInicio() {
                if (!reentrada || adaptador.modo() != ModoLocal.LOCKED) adaptador.solicitarInicio()
            }
        }
        val modelo = TransicionLocal(puerto, SystemClock::elapsedRealtime, autoridad, conservar)
        diagnostico = DiagnosticoLocal(if (salir) FaseLocal.RESTAURANDO else FaseLocal.PREPARANDO)
        ejecutor.execute {
            val gm = if (salir) modelo.salir() else modelo.entrar()
            fun observar() {
                if (operacion.get() != g) return
                modelo.observar(gm); diagnostico = modelo.diagnostico
                if (modelo.pendiente) handler.postDelayed({ ejecutor.execute { observar() } }, 100)
                else {
                    inicioHabilitado.set(false); ocupado.set(false)
                    handler.post { entregar(modelo.diagnostico) }
                }
            }
            observar()
        }
        return true
    }
    /** Lectura sin secretos para baseline de ensayo; nunca crea snapshot. */
    fun comprobar(c: Context, entregar: (Boolean) -> Unit) {
        ejecutor.execute {
            val ok = try {
                PoliticasLockAndroid(c).validar()
                archivo(c).leer() == null && modo(c) == ModoLocal.NONE
            } catch (_: Exception) { false }
            entregar(ok)
        }
    }
    /** Solo rollback de ensayo/errores de políticas propias, sin autoridad de uso. No componente MAIN exportado. */
    fun restaurarPropias(c: Context, autoridadTecnica: () -> Boolean, entregar: (DiagnosticoLocal) -> Unit) {
        if (!autoridadTecnica() || !ocupado.compareAndSet(false, true)) {
            entregar(DiagnosticoLocal(FaseLocal.ERROR, CausaLocal.BASELINE)); return
        }
        val g = operacion.incrementAndGet()
        ejecutor.execute {
            val resultado = try {
                val f = archivo(c)
                if (f.leer() == null) {
                    exigirLocal(modo(c) == ModoLocal.NONE, CausaLocal.BASELINE)
                    DiagnosticoLocal(FaseLocal.RESTAURADO)
                } else {
                    val m = TransicionLocal(AdaptadorLockLocal(f, PoliticasLockAndroid(c)), SystemClock::elapsedRealtime, autoridadTecnica)
                    val gm = m.salir()
                    fun leer() {
                        if (operacion.get() != g) return
                        m.observar(gm); diagnostico = m.diagnostico
                        if (m.pendiente) handler.postDelayed({ ejecutor.execute { leer() } }, 100)
                        else { ocupado.set(false); entregar(m.diagnostico) }
                    }
                    leer(); return@execute
                }
            } catch (e: Exception) { DiagnosticoLocal(FaseLocal.ERROR, (e as? FalloLocal)?.causa ?: CausaLocal.IO_O_ANDROID) }
            diagnostico = resultado; ocupado.set(false); entregar(resultado)
        }
    }
}
