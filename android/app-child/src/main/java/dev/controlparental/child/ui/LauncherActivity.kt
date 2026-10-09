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
import dev.controlparental.child.domain.DiagnosticoAdministracion
import dev.controlparental.child.domain.EstadoPreparacion

/** HOME mínimo; no inicia Lock Task ni registra HOME persistente todavía. */
class LauncherActivity : ComponentActivity() {
    private var diagnostico by mutableStateOf(DiagnosticoAdministracion())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PantallaInicial(diagnostico)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        diagnostico = DeviceCapabilities(this).leerAdministracion()
    }
}

@Composable
private fun PantallaInicial(diagnostico: DiagnosticoAdministracion) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(when (diagnostico.estadoPreparacion) {
                    EstadoPreparacion.NO_CONFIGURADO -> R.string.titulo_inicial
                    EstadoPreparacion.DIAGNOSTICO_NO_DISPONIBLE -> R.string.diagnostico_no_disponible
                    EstadoPreparacion.BLOQUEADO_EN_PREPARACION -> R.string.bloqueado_preparacion
                }),
                style = MaterialTheme.typography.headlineMedium,
            )
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
