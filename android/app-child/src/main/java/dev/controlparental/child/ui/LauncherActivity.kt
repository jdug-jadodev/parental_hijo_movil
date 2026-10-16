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
import dev.controlparental.child.domain.EstadoPreparacion
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
    private var bloqueadoLocal by mutableStateOf(false)
    @Volatile private var primerPlanoPiloto = false
    private var resumenLocal by mutableStateOf(ResumenLocal("Comprobando estado…", "", "", "Protección por comprobar"))
    private fun actualizarResumenLocal() {
        resumenLocal = presentarEstadoLocal(
            try { BloqueoLocalAndroid.modo(this) } catch (_: RuntimeException) { null },
            ControlRedAndroid.uso, ControlRedAndroid.permiteUsoRAM, ControlRedAndroid.vencimientoUso,
            SystemClock.elapsedRealtime(), ProteccionAndroid.estado)
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
        if (ControlRedAndroid.activo) ControlRedAndroid.senal(applicationContext)
        ArranquePilotoAndroid.actividad(this, { primerPlanoPiloto && !isFinishing && !isDestroyed }) {
            bloqueadoLocal = try { BloqueoLocalAndroid.modo(this) == ModoLocal.LOCKED } catch (_: RuntimeException) { false }
        }
        bloqueadoLocal = try { BloqueoLocalAndroid.modo(this) == ModoLocal.LOCKED } catch (_: RuntimeException) { false }
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
