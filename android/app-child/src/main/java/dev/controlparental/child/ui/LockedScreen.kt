package dev.controlparental.child.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.controlparental.child.R
import dev.controlparental.child.domain.ResultadoAccesoEmergencia

/** Fragmento siempre incluido, incluso sin Owner/vínculo o con diagnóstico fallido. */
@Composable
fun AccesoEmergenciaSistema(resultado: ResultadoAccesoEmergencia?, solicitar: () -> Unit) {
    Text(stringResource(R.string.emergencia_ruta))
    Text(stringResource(R.string.emergencia_pantalla_apagada))
    Button(onClick = solicitar) {
        Text(stringResource(R.string.emergencia_ir_bloqueo))
    }
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
