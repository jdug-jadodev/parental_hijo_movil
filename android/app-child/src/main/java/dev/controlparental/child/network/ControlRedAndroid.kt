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
import dev.controlparental.child.orders.*
import dev.controlparental.protocol.cp2.*
import dev.controlparental.child.pairing.PairAndroid
import dev.controlparental.child.pairing.CargaPairCp2
import dev.controlparental.child.auth.AuthAndroid
import java.lang.ref.WeakReference
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/** Observador de default vive con FGS, no con Activity. Permisos RAM separados de sesión adulta. */
internal object ControlRedAndroid {
    @Volatile private var hiloActor: Thread? = null
    private val ejecutor = Executors.newSingleThreadExecutor { Thread(it, "control-red-local").also { hiloActor = it } }
    private val handler = Handler(Looper.getMainLooper())
    private val epoca = AtomicLong()
    private val visible = ObservadorVisible()
    private val servicioFallido = AtomicBoolean()
    private var modelo: UsoRedLocal? = null
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var plazos: ProgramadorPlazo? = null
    private var revision = 0L
    private var cerrando = false
    private var controlCp2: EscritorControlCp2? = null
    private var guardiaCp2: GuardiaOrdenCp2? = null
    private var generacionCp2: GeneracionOrdenCp2? = null
    private var permisoControlRef: Long? = null
    private var actividadCp2: WeakReference<Activity>? = null
    private var cambioCp2: (() -> Unit)? = null
    private var retenidaCp2: ((RemotoControl?) -> Unit)? = null
    private var vigenteCanalCp2: (() -> Boolean)? = null
    private var secuenciaState = 0L
    @Volatile var activo = false; private set
    @Volatile var listo = false; private set
    @Volatile var uso = UsoLocal.BLOQUEADO; private set
    @Volatile var validacion = ValidacionRed.DESCONOCIDA; private set
    @Volatile var deadline: Long? = null; private set
    @Volatile var vencimientoUso: Long? = null; private set
    @Volatile var permiteUsoRAM = false; private set
    @Volatile var referenciaPublica: ReferenciaRed? = null; private set
    private fun crear(c: Context): UsoRedLocal {
        val dp = c.applicationContext.createDeviceProtectedStorageContext()
        return UsoRedLocal(ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "uso-red-v1.marca")),
            ArchivoSnapshotAndroid(File(dp.noBackupFilesDir, "uso-red-v1.registro")), SystemClock::elapsedRealtime,
            { Settings.Global.getInt(dp.contentResolver, Settings.Global.BOOT_COUNT, -1) })
    }
    private fun publicar(m: UsoRedLocal, evento: String) {
        if (permisoControlRef != null && !m.permiteUso) {
            permisoControlRef = null
            controlCp2?.revocar(when (m.motivoRevocacion) {
                MotivoRed.RED_10S -> MotivoStateCp2.NETWORK_LOST
                MotivoRed.TIEMPO -> MotivoStateCp2.TIMER_EXPIRED
                MotivoRed.ENFORCEMENT -> MotivoStateCp2.ENFORCEMENT_ERROR
                else -> MotivoStateCp2.CONTROL_UNKNOWN
            }) // Antes de solicitar cualquier side effect de reentrada.
            cambioCp2?.invoke()
        }
        uso = m.uso; validacion = m.red; deadline = m.deadline; vencimientoUso = m.vencimientoUso; referenciaPublica = m.referencia
        permiteUsoRAM = m.permiteUso && listo
        Log.i("ControlRedLocal", "evento=$evento uso=$uso red=$validacion gen=${m.referencia?.generacion} elapsed=${SystemClock.elapsedRealtime()} deadline=$deadline inicioUso=${m.inicioUso} vencimientoUso=$vencimientoUso")
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
        if (epoca.get() != g || cerrando) return
        try {
            val app = c.applicationContext
            val p = plazos ?: ProgramadorPlazo(PuertoPlazoAndroid(app, handler, ejecutor)) { plan ->
                val actual = modelo
                if (!cerrando && listo && actual?.referencia == plan.referencia) {
                    if (!visible.vigente(condiciones(app))) servicioTerminado(app)
                    else {
                        val antes = actual.permiteUso
                        Log.i("ControlRedLocal", "evento=ENTREGA_PLAZO elapsed=${SystemClock.elapsedRealtime()} plazo=${plan.deadline} gen=${plan.referencia.generacion}")
                        actual.comprobarDeadline(plan.referencia); publicar(actual, "PLAZO_REEVALUADO")
                        if (antes && !actual.permiteUso) ArranquePilotoAndroid.exigirBloqueo(app)
                        programar(app, actual, epoca.get())
                    }
                }
            }.also { plazos = it }
            val ref = m.referencia; val d = m.proximoPlazo
            p.actualizar(if (ref != null && d != null) PlanPlazo(ref, d) else null)
        } catch (_: RuntimeException) { servicioTerminado(c) }
    }
    fun permitir(a: Activity, tipo: UsoLocal, autoridad: () -> Boolean, entregar: () -> Unit,
        minutos: MinutosUso? = null, referenciaEsperada: ReferenciaRed? = referenciaPublica,
        decisionCp2: DecisionControl? = null) {
        ejecutor.execute {
            val m = modelo
            fun rechazar() {
                val d = decisionCp2
                if (d != null && controlCp2?.esRevision(d.revision) == true) {
                    controlCp2?.confirmar(d.revision, false)
                    m?.bloquear { true }; m?.let { publicar(it, "ORDEN_NO_APTA") }
                    ArranquePilotoAndroid.exigirBloqueo(a); cambioCp2?.invoke()
                }
                handler.post(entregar)
            }
            if (m == null || !listo || cerrando || !autoridad()) { rechazar(); return@execute }
            if (m.referencia != referenciaEsperada) { rechazar(); return@execute }
            evaluar(a, epoca.get()) // Lectura default actual, no capacidades del callback antiguo.
            if (m.referencia != referenciaEsperada || !listo) { rechazar(); return@execute }
            if (!asegurarControlCp2(a, autoridad)) { rechazar(); return@execute }
            val writer = controlCp2
            val d = decisionCp2 ?: writer?.local(if (minutos == null) AccionOrdenCp2.ALLOW else AccionOrdenCp2.ALLOW_FOR,
                minutos?.cantidad?.toLong()?.times(60), tipo == UsoLocal.EXCEPCION_OFFLINE)
            if (writer != null && d == null) { handler.post(entregar); return@execute }
            fun vigente() = autoridad() && (writer == null || (d != null && writer.esRevision(d.revision)))
            if (!vigente()) { handler.post(entregar); return@execute }
            cambioCp2?.invoke() // Intención pendiente durable, todavía no APPLIED.
            if (!visible.autorizar(m, tipo, condiciones(a), minutos, d?.deadline, ::vigente)) {
                d?.let { writer?.confirmar(it.revision, false) }; cambioCp2?.invoke()
                publicar(m, "AUTORIZACION_RECHAZADA")
                if (!visible.vigente(condiciones(a))) servicioTerminado(a)
                handler.post(entregar); return@execute
            }
            permisoControlRef = d?.revision
            val ref = m.referencia!!; publicar(m, "AUTORIZACION_LOCAL_$tipo")
            programar(a, m, epoca.get())
            handler.post {
                val aceptada = BloqueoLocalAndroid.usarGuardado(a, {
                    permiteUsoRAM && referenciaPublica == ref && visible.vigente(condiciones(a)) && vigente()
                }) { diagnostico ->
                    ejecutor.execute {
                        if (m.referencia != ref) return@execute
                        if (diagnostico.fase != FaseLocal.RESTAURADO) {
                            m.falloEnforcement(ref); publicar(m, "ERROR_ABRIR"); ArranquePilotoAndroid.exigirBloqueo(a)
                        } else publicar(m, "USO_APLICADO_NONE")
                        d?.let { writer?.confirmar(it.revision, diagnostico.fase == FaseLocal.RESTAURADO && m.permiteUso) }
                        cambioCp2?.invoke()
                        handler.post(entregar)
                    }
                }
                if (!aceptada) ejecutor.execute {
                    if (m.referencia == ref) {
                        m.falloEnforcement(ref); publicar(m, "NO_INICIADO"); ArranquePilotoAndroid.exigirBloqueo(a)
                    }
                    d?.let { writer?.confirmar(it.revision, false) }; cambioCp2?.invoke()
                    handler.post(entregar)
                }
            }
        }
    }
    fun bloquear(c: Context, autoridad: () -> Boolean, entregar: () -> Unit) {
        ejecutor.execute {
            val m = modelo
            if (m != null && autoridad()) {
                if (!asegurarControlCp2(c, autoridad)) { m.bloquear { true }; publicar(m, "CONTROL_INCIERTO"); ArranquePilotoAndroid.exigirBloqueo(c); handler.post(entregar); return@execute }
                val d = controlCp2?.local(AccionOrdenCp2.LOCK, null, false)
                if (controlCp2 != null && d == null) { handler.post(entregar); return@execute }
                permisoControlRef = null
                m.bloquear { true }; publicar(m, "BLOQUEAR_ADULTO")
                programar(c, m, epoca.get())
                if (!m.permiteUso && d == null) ArranquePilotoAndroid.exigirBloqueo(c)
                d?.let { observarBloqueoCp2(c, it) }; cambioCp2?.invoke()
            }
            handler.post(entregar)
        }
    }
    private fun cancelar(c: Context) {
        visible.invalidar()
        epoca.incrementAndGet(); listo = false; permiteUsoRAM = false
        try { plazos?.cancelar() } catch (_: RuntimeException) { uso = UsoLocal.ERROR }
        plazos = null
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
            if (controlCp2 != null && !controlCp2!!.revocar()) { handler.post(entregar); return@execute }
            permisoControlRef = null; cambioCp2?.invoke()
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
    /** Entrada explícita del piloto. No arma Device Owner/boot/red ni crea pareja automáticamente. */
    private fun asegurarControlCp2(c: Context, autoridad: () -> Boolean): Boolean {
        controlCp2?.let { return it.contexto() != null }
        when (PairAndroid.cargar(c)) {
            CargaPairCp2.Ausente -> return true // Compatibilidad local sin pareja CP2, no es autoridad CP1 remota.
            CargaPairCp2.Incierta -> {
                permiteUsoRAM = false; modelo?.let { m -> m.referencia?.let(m::falloEnforcement) }
                ArranquePilotoAndroid.exigirBloqueo(c); return false
            }
            is CargaPairCp2.Guardado -> Unit
        }
        val app = c.applicationContext
        val w = EscritorControlCp2(ArchivosControlAndroid(app), SystemClock::elapsedRealtime,
            { OrdenesAndroid.pareja(app) }, {
                permiteUsoRAM = false; permisoControlRef = null
                modelo?.let { m -> m.referencia?.let(m::falloEnforcement) }
                ArranquePilotoAndroid.exigirBloqueo(app)
            })
        controlCp2 = w
        return w.abrir(autoridad)
    }
    fun iniciarCp2(a: Activity, autoridad: () -> Boolean, cambio: () -> Unit,
        retenida: (RemotoControl?) -> Unit, entregar: (Boolean) -> Unit) {
        ejecutor.execute {
            if (!autoridad() || !listo || modelo == null || !ArranquePilotoAndroid.enrolado) {
                handler.post { entregar(false) }; return@execute
            }
            actividadCp2 = WeakReference(a)
            if (!asegurarControlCp2(a, autoridad)) { handler.post { entregar(false) }; return@execute }
            val w = controlCp2
            if (w == null || !w.disponible) { handler.post { entregar(false) }; return@execute }
            cambioCp2 = cambio; retenidaCp2 = retenida
            val app = a.applicationContext
            val g = GuardiaOrdenCp2(SystemClock::elapsedRealtime, { OrdenesAndroid.pareja(app) }, object : EscritorControlOrdenCp2 {
                override fun consolidar(): ControlOrdenCp2? { consolidarCp2(app); return w.contexto() }
                override fun consultarRetenida(orden: OrdenVerificadaCp2) { retenidaCp2?.invoke(w.retenida(orden)) }
                override fun entregarParaDecision(preparacion: PreparacionOrdenCp2) {
                    val canal = vigenteCanalCp2 ?: error("Sin generación de canal")
                    check(canal())
                    val d = w.remoto(preparacion) ?: error("Conflicto CONTROL; requiere SYNC nuevo")
                    cambioCp2?.invoke()
                    val actividad = actividadCp2?.get() ?: error("Piloto sin Activity foreground")
                    if (d.action == AccionOrdenCp2.LOCK) {
                        permisoControlRef = null
                        modelo!!.bloquear { w.esRevision(d.revision) }
                        publicar(modelo!!, "ORDEN_LOCK_CP2"); programar(app, modelo!!, epoca.get())
                        observarBloqueoCp2(actividad, d)
                    } else {
                        permitir(actividad, UsoLocal.NORMAL, { canal() && w.esRevision(d.revision) }, {},
                            d.deadline?.let { MinutosUso.desdeTexto(((it - d.accepted) / 60000).toString()) }, decisionCp2 = d)
                    }
                }
            })
            guardiaCp2?.cerrar(); guardiaCp2 = g; generacionCp2 = g.iniciar()
            handler.post { entregar(generacionCp2 != null) }
        }
    }
    private fun consolidarCp2(c: Context) {
        evaluar(c, epoca.get())
        val m = modelo ?: return
        m.referencia?.let(m::comprobarDeadline); publicar(m, "CONSOLIDACION_CP2")
        val w = controlCp2 ?: return
        val d = try { w.actual().decision } catch (_: Exception) { null }
        if (d?.deadline != null && SystemClock.elapsedRealtime() >= d.deadline && m.permiteUso) {
            w.revocar(MotivoStateCp2.TIMER_EXPIRED); permisoControlRef = null; m.bloquear { true }; publicar(m, "PLAZO_CONTROL_CP2")
            ArranquePilotoAndroid.exigirBloqueo(c)
        }
    }
    private fun observarBloqueoCp2(c: Context, d: DecisionControl) {
        val w = controlCp2 ?: return
        val a = (c as? Activity) ?: actividadCp2?.get()
        if (a == null) { w.confirmar(d.revision, false); cambioCp2?.invoke(); return }
        handler.post {
            val ok = BloqueoLocalAndroid.reentrar(a, { !permiteUsoRAM && w.esRevision(d.revision) }) { diagnostico ->
                ejecutor.execute {
                    consolidarCp2(c)
                    w.confirmar(d.revision, diagnostico.fase == FaseLocal.LOCKED && BloqueoLocalAndroid.modo(c) == ModoLocal.LOCKED)
                    cambioCp2?.invoke()
                }
            }
            if (!ok) ejecutor.execute { w.confirmar(d.revision, false); cambioCp2?.invoke() }
        }
    }
    fun recibirCp2(c: Context, bytes: ByteArray, canalVigente: () -> Boolean,
        entregar: (OrdenVerificadaCp2?, RetoOrdenCp2?) -> Unit) {
        if (bytes.size !in 1..16384) { entregar(null, null); return }
        val copia = bytes.copyOf()
        ejecutor.execute {
            val g = guardiaCp2; val ref = generacionCp2
            try {
                if (g == null || ref == null || !canalVigente()) { entregar(null, null); return@execute }
                vigenteCanalCp2 = canalVigente
                consolidarCp2(c)
                val v = CriptografiaOrdenCp2.verificar(copia, ref.pareja.publicaPadre)
                if (v.mensaje is SyncOrdenCp2) {
                    val s = g.recibirSync(ref, copia)
                    entregar(if (s != null) v else null, s?.reto)
                } else {
                    g.prepararPolitica(ref, copia)?.let { g.entregar(ref, it) }
                    entregar(v, null)
                }
            } catch (_: Exception) { entregar(null, null) }
        }
    }
    fun consultarCp2(c: Context, entregar: (RegistroControl?) -> Unit) {
        ejecutor.execute {
            consolidarCp2(c)
            val r = try { controlCp2?.actual() } catch (_: Exception) { null }
            entregar(r)
        }
    }
    fun cerrarCanalCp2() { ejecutor.execute { guardiaCp2?.cerrar(); generacionCp2 = null; vigenteCanalCp2 = null; cambioCp2 = null; retenidaCp2 = null; actividadCp2 = null } }
    /** Sólo callbacks del actor. Firma y revalida antes de entregar bytes al MISMO canal. */
    fun stateCp2EnActor(c: Context, syncNonce: String, canalVigente: () -> Boolean): ByteArray? {
        check(Thread.currentThread() === hiloActor)
        return try {
            if (!canalVigente()) return null
            consolidarCp2(c)
            val w = controlCp2 ?: return null
            val r = w.actual(); val identidad = AuthAndroid.contextoActual(c) ?: return null
            val ref = generacionCp2 ?: return null
            val reto = guardiaCp2?.retoActual(ref)
            val sampled = SystemClock.elapsedRealtime()
            val owner = try { c.getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(c.packageName) } catch (_: RuntimeException) { null }
            val modo = try { BloqueoLocalAndroid.modo(c) } catch (_: RuntimeException) { null }
            val lock = modo?.let { it != ModoLocal.NONE }
            secuenciaState = Math.addExact(secuenciaState, 1)
            val s = ProyeccionStateCp2.crear(r, identidad.pairId, syncNonce, secuenciaState, sampled, w.motivoActual, reto,
                when (validacion) { ValidacionRed.DESCONOCIDA -> null; ValidacionRed.VALIDADA -> true; ValidacionRed.NO_VALIDADA -> false }, owner, lock, permiteUsoRAM,
                if (r.decision?.action == AccionOrdenCp2.LOCK) modo == ModoLocal.LOCKED else modo == ModoLocal.NONE)
            val bytes = CriptografiaStateCp2.firmar(s, identidad.publicaHijo, AuthAndroid.firmanteExistente(c, identidad))
            consolidarCp2(c)
            if (!canalVigente() || AuthAndroid.contextoActual(c) != identidad || w.actual().revision != r.revision ||
                guardiaCp2?.retoActual(ref) !== reto) null else bytes
        } catch (_: Exception) { null }
    }
    fun revisionBloqueoCp2(): Long? {
        return if (uso == UsoLocal.BLOQUEADO) controlCp2?.ticketBloqueo() else null
    }
    fun confirmarBloqueoCp2(revision: Long?, exito: Boolean) {
        if (revision == null) return
        ejecutor.execute { controlCp2?.confirmar(revision, exito); cambioCp2?.invoke() }
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
