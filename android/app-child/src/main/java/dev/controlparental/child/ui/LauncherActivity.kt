package dev.controlparental.child.ui

import android.os.Bundle
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** HOME mínimo; no inicia Lock Task ni registra HOME persistente todavía. */
class LauncherActivity : ComponentActivity() {
    private var diagnostico by mutableStateOf(DiagnosticoAdministracion())
    private var resultadoEmergencia by mutableStateOf<ResultadoAccesoEmergencia?>(null)
    private var estadoComprobado by mutableStateOf<Boolean?>(null)
    private var consultaEstado = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PantallaInicial(diagnostico, resultadoEmergencia, estadoComprobado) {
                    resultadoEmergencia = EmergencyAccess(PolicyEnforcer(this)).irAPantallaBloqueo()
                }
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
        super.onPause()
    }
}

@Composable
private fun PantallaInicial(
    diagnostico: DiagnosticoAdministracion,
    resultadoEmergencia: ResultadoAccesoEmergencia?,
    estadoComprobado: Boolean?,
    solicitarEmergencia: () -> Unit,
) {
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
