package dev.controlparental.child.ui

import android.os.Bundle
import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.controlparental.child.R
import dev.controlparental.child.admin.DeviceCapabilities
import dev.controlparental.child.admin.PolicyEnforcer
import dev.controlparental.child.domain.DiagnosticoAdministracion
import dev.controlparental.child.domain.ResultadoAccesoEmergencia
import dev.controlparental.child.emergency.EmergencyAccess
import dev.controlparental.child.boot.BootContextRepository
import dev.controlparental.child.boot.FuenteArranqueAndroid
import dev.controlparental.child.data.CargaEstado
import dev.controlparental.child.data.EjecutorEstado
import dev.controlparental.child.data.EstadoAndroid
import dev.controlparental.child.data.PropositoRecuperacionLocal
import dev.controlparental.child.recovery.EstadoPantallaRecuperacion
import dev.controlparental.child.recovery.RecuperacionAndroid
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import dev.controlparental.child.lock.BloqueoLocalAndroid
import dev.controlparental.child.lock.ModoLocal
import dev.controlparental.child.boot.ArranquePilotoAndroid
import dev.controlparental.child.network.ControlRedAndroid
import dev.controlparental.child.protection.ProteccionAndroid
import android.os.SystemClock

/** HOME no persistente. Reentrada piloto exclusivamente desde intención DP ya enrolada. */
class LauncherActivity : ComponentActivity() {
    private var diagnostico by mutableStateOf(DiagnosticoAdministracion())
    private var resultadoEmergencia by mutableStateOf<ResultadoAccesoEmergencia?>(null)
    private var estadoComprobado by mutableStateOf<Boolean?>(null)
    private var consultaEstado = 0
    private var recuperacionVisible by mutableStateOf(false)
    private var recuperacion by mutableStateOf(EstadoPantallaRecuperacion())
    private val consultaRecuperacion = AtomicInteger()
    @Volatile private var primerPlanoPiloto = false
    private var resumenLocal by mutableStateOf(ResumenLocal("Comprobando estado…", "", "", "Protección por comprobar"))
    private var appsVisibles by mutableStateOf(false)
    private var apps by mutableStateOf<List<Pair<String, android.content.ComponentName>>>(emptyList())
    private var appsPermitidas by mutableStateOf(false)
    private var avisoApps by mutableStateOf("")
    private var abriendoApp = false
    private fun permisoAppsActual(): Boolean = primerPlanoPiloto && puedeAbrirApps(
        try { BloqueoLocalAndroid.modo(this) } catch (_: RuntimeException) { null },
        ControlRedAndroid.uso, ControlRedAndroid.permiteUsoRAM, ControlRedAndroid.listo,
        ControlRedAndroid.vencimientoUso, ControlRedAndroid.deadline, SystemClock.elapsedRealtime())
    private fun mostrarApps() {
        if (!permisoAppsActual()) { actualizarResumenLocal(); return }
        appsVisibles = true; avisoApps = ""
        val consulta = ++consultaApps
        EjecutorEstado.ejecutar {
            val lista = try {
                packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    .filter { it.activityInfo.packageName != packageName && it.activityInfo.exported && it.activityInfo.enabled }
                    .map { it.loadLabel(packageManager).toString() to android.content.ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
                    .distinctBy { it.second }.sortedBy { it.first.lowercase(java.util.Locale.ROOT) }
            } catch (_: RuntimeException) { emptyList() }
            runOnUiThread {
                if (consultaApps == consulta && appsVisibles && permisoAppsActual()) {
                    apps = lista
                    if (lista.isEmpty()) avisoApps = "No se pudieron mostrar aplicaciones. Vuelve a intentarlo."
                }
            }
        }
    }
    private var consultaApps = 0
    private fun abrirApp(componente: android.content.ComponentName) {
        if (abriendoApp || !permisoAppsActual() || apps.none { it.second == componente }) { actualizarResumenLocal(); return }
        abriendoApp = true
        try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(componente)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        } catch (_: RuntimeException) { abriendoApp = false; avisoApps = "No se pudo abrir. Prueba otra aplicación." }
    }
    private fun actualizarResumenLocal() {
        resumenLocal = presentarEstadoLocal(
            try { BloqueoLocalAndroid.modo(this) } catch (_: RuntimeException) { null },
            ControlRedAndroid.uso, ControlRedAndroid.permiteUsoRAM, ControlRedAndroid.vencimientoUso,
            SystemClock.elapsedRealtime(), ProteccionAndroid.estado, ControlRedAndroid.deadline)
        appsPermitidas = permisoAppsActual()
        if (!appsPermitidas) { appsVisibles = false; apps = emptyList(); consultaApps++ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1F4B99), background = Color(0xFFF4F7FC), onBackground = Color(0xFF17243A))) {
                LaunchedEffect(Unit) {
                    while (true) {
                        if (primerPlanoPiloto) actualizarResumenLocal()
                        delay(1000)
                    }
                }
                PantallaInicial(diagnostico, resultadoEmergencia, estadoComprobado,
                    resumenLocal,
                    appsPermitidas, appsVisibles, apps, avisoApps,
                    mostrarApps = ::mostrarApps, abrirApp = ::abrirApp,
                    cerrarApps = { appsVisibles = false; apps = emptyList(); consultaApps++ },
                    recuperacionVisible, recuperacion,
                    solicitarEmergencia = {
                        cerrarRecuperacion()
                        resultadoEmergencia = EmergencyAccess(PolicyEnforcer(this)).irAPantallaBloqueo()
                    }, abrirRecuperacion = {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        recuperacionVisible = true
                        recuperacion = EstadoPantallaRecuperacion()
                        consultarRecuperacion()
                    }, actualizarRecuperacion = { if (!recuperacion.ocupado) consultarRecuperacion() },
                    enviarCodigo = { proposito, texto -> consultarRecuperacion(proposito, texto) },
                    abrirAdulto = { startActivity(Intent(this, AccesoAdultoActivity::class.java)) },
                    volver = { cerrarRecuperacion() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        primerPlanoPiloto = true
        abriendoApp = false
        if (ControlRedAndroid.activo) ControlRedAndroid.senal(applicationContext)
        ArranquePilotoAndroid.actividad(this, { primerPlanoPiloto && !isFinishing && !isDestroyed }) {
            if (primerPlanoPiloto) actualizarResumenLocal()
        }
        diagnostico = DeviceCapabilities(this).leerAdministracion()
        ProteccionAndroid.consultar(applicationContext) { if (primerPlanoPiloto) actualizarResumenLocal() }
        actualizarResumenLocal()
        val consulta = ++consultaEstado
        estadoComprobado = null
        if (diagnostico.esDeviceOwner != true) return
        val contexto = applicationContext
        EjecutorEstado.ejecutar {
            val resultado = try {
                BootContextRepository(EstadoAndroid.obtener(contexto), FuenteArranqueAndroid(contexto)).actualizarContexto()
            } catch (_: RuntimeException) {
                CargaEstado.ErrorSeguro
            }
            runOnUiThread {
                if (!isDestroyed && consultaEstado == consulta) estadoComprobado = resultado is CargaEstado.Disponible
            }
        }
    }

    override fun onPause() {
        primerPlanoPiloto = false
        consultaApps++; appsVisibles = false; apps = emptyList()
        consultaEstado++ // Descartar respuesta de una pantalla que ya dejó primer plano.
        cerrarRecuperacion()
        super.onPause()
    }

    private fun consultarRecuperacion(proposito: PropositoRecuperacionLocal? = null, texto: String = "") {
        if (!recuperacionVisible || recuperacion.ocupado) return
        val consulta = consultaRecuperacion.incrementAndGet()
        val anterior = recuperacion.respuesta
        recuperacion = recuperacion.copy(ocupado = true)
        val contexto = applicationContext
        EjecutorEstado.ejecutar {
            if (consultaRecuperacion.get() != consulta) return@ejecutar
            val resultado = if (proposito == null) RecuperacionAndroid.resumen(contexto, anterior)
                else RecuperacionAndroid.usarCodigo(contexto, proposito, texto)
            runOnUiThread {
                if (!isDestroyed && recuperacionVisible && consultaRecuperacion.get() == consulta) recuperacion = resultado
            }
        }
    }

    private fun cerrarRecuperacion() {
        consultaRecuperacion.incrementAndGet()
        recuperacionVisible = false // Desmonta texto recordado en RAM, nunca guardado en Bundle.
        recuperacion = EstadoPantallaRecuperacion()
        EjecutorEstado.ejecutar { RecuperacionAndroid.cancelarSiExiste() }
    }
}

@Composable
private fun PantallaInicial(
    diagnostico: DiagnosticoAdministracion,
    resultadoEmergencia: ResultadoAccesoEmergencia?,
    estadoComprobado: Boolean?,
    resumen: ResumenLocal,
    appsPermitidas: Boolean,
    appsVisibles: Boolean,
    apps: List<Pair<String, android.content.ComponentName>>,
    avisoApps: String,
    mostrarApps: () -> Unit,
    abrirApp: (android.content.ComponentName) -> Unit,
    cerrarApps: () -> Unit,
    recuperacionVisible: Boolean,
    recuperacion: EstadoPantallaRecuperacion,
    solicitarEmergencia: () -> Unit,
    abrirRecuperacion: () -> Unit,
    actualizarRecuperacion: () -> Unit,
    enviarCodigo: (PropositoRecuperacionLocal, String) -> Unit,
    abrirAdulto: () -> Unit,
    volver: () -> Unit,
) {
    var detalles by remember { mutableStateOf(false) }
    LaunchedEffect(recuperacionVisible) {
        if (recuperacionVisible) while (true) { delay(1000); actualizarRecuperacion() }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Mi teléfono", style = MaterialTheme.typography.titleLarge)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(resumen.estado, style = MaterialTheme.typography.headlineLarge)
                    Text(resumen.tiempo, style = MaterialTheme.typography.titleLarge)
                    Text(if (resumen.estado == "Bloqueado") "Un adulto puede darte permiso para usarlo." else resumen.modalidad)
                }
            }
            Button(onClick = abrirAdulto, modifier = Modifier.fillMaxWidth()) {
                Text("Acceso adulto", modifier = Modifier.padding(10.dp))
            }
            AccesoEmergenciaSistema(resultadoEmergencia, solicitarEmergencia)
            if (appsPermitidas) {
                if (!appsVisibles) Button(onClick = mostrarApps, modifier = Modifier.fillMaxWidth()) {
                    Text("Mis aplicaciones", modifier = Modifier.padding(10.dp))
                } else {
                    OutlinedButton(onClick = cerrarApps, modifier = Modifier.fillMaxWidth()) { Text("Volver al inicio") }
                    if (avisoApps.isNotEmpty()) Text(avisoApps)
                    for ((nombre, componente) in apps) OutlinedButton(onClick = { abrirApp(componente) }, modifier = Modifier.fillMaxWidth()) { Text(nombre) }
                }
            }
            OutlinedButton(onClick = { detalles = !detalles }, modifier = Modifier.fillMaxWidth()) { Text("Ayuda y recuperación") }
            if (detalles || recuperacionVisible) {
                Text("¿Olvidaste el código? Entra en Acceso adulto y toca «Olvidé mi código».")
                Text(resumen.proteccion)
                if (recuperacionVisible) RecoveryScreen(recuperacion, enviarCodigo, volver)
                else OutlinedButton(onClick = abrirRecuperacion) { Text("Recuperación avanzada sin internet") }
                Text("Diagnóstico", style = MaterialTheme.typography.titleMedium)
                Text(stringResource(when (estadoComprobado) {
                    true -> R.string.estado_local_comprobado
                    false -> R.string.estado_local_error_detalle
                    null -> R.string.estado_local_pendiente
                }))
                Text(text = stringResource(R.string.diagnostico_owner, textoDiagnostico(diagnostico.esDeviceOwner)))
                Text("La protección no cubre root ni restablecimiento físico. No envíes códigos por chat.")
            }
        }
    }
}

@Composable
private fun textoDiagnostico(valor: Boolean?): String = stringResource(when (valor) {
    true -> R.string.diagnostico_si
    false -> R.string.diagnostico_no
    null -> R.string.diagnostico_desconocido
})
