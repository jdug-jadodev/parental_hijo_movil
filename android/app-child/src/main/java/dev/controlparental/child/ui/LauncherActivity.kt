package dev.controlparental.child.ui

import android.os.Bundle
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

/** HOME mínimo; no inicia Lock Task ni registra HOME persistente todavía. */
class LauncherActivity : ComponentActivity() {
    private var diagnostico by mutableStateOf(DiagnosticoAdministracion())
    private var resultadoEmergencia by mutableStateOf<ResultadoAccesoEmergencia?>(null)
    private var estadoComprobado by mutableStateOf<Boolean?>(null)
    private var consultaEstado = 0
    private var recuperacionVisible by mutableStateOf(false)
    private var recuperacion by mutableStateOf(EstadoPantallaRecuperacion())
    private val consultaRecuperacion = AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PantallaInicial(diagnostico, resultadoEmergencia, estadoComprobado,
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
                    volver = { cerrarRecuperacion() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        diagnostico = DeviceCapabilities(this).leerAdministracion()
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
    recuperacionVisible: Boolean,
    recuperacion: EstadoPantallaRecuperacion,
    solicitarEmergencia: () -> Unit,
    abrirRecuperacion: () -> Unit,
    actualizarRecuperacion: () -> Unit,
    enviarCodigo: (PropositoRecuperacionLocal, String) -> Unit,
    volver: () -> Unit,
) {
    LaunchedEffect(recuperacionVisible) {
        if (recuperacionVisible) while (true) { delay(1000); actualizarRecuperacion() }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(if (estadoComprobado == false) R.string.estado_local_error else when (diagnostico.estadoPreparacion) {
                    EstadoPreparacion.NO_CONFIGURADO -> R.string.titulo_inicial
                    EstadoPreparacion.DIAGNOSTICO_NO_DISPONIBLE -> R.string.diagnostico_no_disponible
                    EstadoPreparacion.BLOQUEADO_EN_PREPARACION -> R.string.bloqueado_preparacion
                }),
                style = MaterialTheme.typography.headlineMedium,
            )
            AccesoEmergenciaSistema(resultadoEmergencia, solicitarEmergencia)
            if (recuperacionVisible) {
                RecoveryScreen(recuperacion, enviarCodigo, volver)
            } else {
                Button(onClick = abrirRecuperacion) { Text(stringResource(R.string.recuperacion_abrir)) }
            }
            Text(stringResource(when (estadoComprobado) {
                true -> R.string.estado_local_comprobado
                false -> R.string.estado_local_error_detalle
                null -> R.string.estado_local_pendiente
            }))
            Text(text = stringResource(R.string.aviso_inicial))
            Text(text = stringResource(R.string.aviso_laboratorio))
            Text(text = stringResource(R.string.diagnostico_owner, textoDiagnostico(diagnostico.esDeviceOwner)))
            Text(text = stringResource(R.string.diagnostico_admin, textoDiagnostico(diagnostico.administradorActivo)))
            Text(text = stringResource(R.string.sin_quiosco))
        }
    }
}

@Composable
private fun textoDiagnostico(valor: Boolean?): String = stringResource(when (valor) {
    true -> R.string.diagnostico_si
    false -> R.string.diagnostico_no
    null -> R.string.diagnostico_desconocido
})
