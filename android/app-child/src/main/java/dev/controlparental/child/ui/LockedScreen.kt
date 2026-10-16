package dev.controlparental.child.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.controlparental.child.R
import dev.controlparental.child.domain.ResultadoAccesoEmergencia

/** Fragmento siempre incluido, incluso sin Owner/vínculo o con diagnóstico fallido. */
@Composable
fun AccesoEmergenciaSistema(resultado: ResultadoAccesoEmergencia?, solicitar: () -> Unit) {
    Button(onClick = solicitar, modifier = Modifier.fillMaxWidth()) {
        Text("Emergencia · pantalla del sistema", modifier = Modifier.padding(10.dp))
    }
    Text("Usa «Llamada de emergencia» en la pantalla de bloqueo del sistema.")
    Text(stringResource(R.string.emergencia_pantalla_apagada))
    if (resultado != null) Text(stringResource(when (resultado) {
        ResultadoAccesoEmergencia.SOLICITUD_ENVIADA -> R.string.emergencia_solicitada
        ResultadoAccesoEmergencia.CREDENCIAL_ANDROID_REQUERIDA -> R.string.emergencia_falta_credencial
        ResultadoAccesoEmergencia.NO_ES_DEVICE_OWNER,
        ResultadoAccesoEmergencia.ADMINISTRADOR_NO_ACTIVO,
        ResultadoAccesoEmergencia.ADMINISTRACION_NO_DISPONIBLE -> R.string.emergencia_sin_admin
        ResultadoAccesoEmergencia.COMPROBACION_NO_DISPONIBLE,
        ResultadoAccesoEmergencia.ERROR_AL_SOLICITAR -> R.string.emergencia_error
    }))
}
