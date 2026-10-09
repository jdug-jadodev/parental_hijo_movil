package dev.controlparental.child.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.controlparental.child.R
import dev.controlparental.child.data.PropositoRecuperacionLocal
import dev.controlparental.child.recovery.*

/** Sin rememberSaveable, clipboard, intents externos ni herramientas de administración. */
@Composable
fun RecoveryScreen(estado: EstadoPantallaRecuperacion,
    enviar: (PropositoRecuperacionLocal, String) -> Unit, volver: () -> Unit) {
    var codigo by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf(PropositoRecuperacionLocal.MAINTENANCE) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.recuperacion_titulo))
        Text(stringResource(when (estado.disponibilidad) {
            DisponibilidadRecuperacion.COMPROBANDO -> R.string.recuperacion_comprobando
            DisponibilidadRecuperacion.SIN_VINCULO -> R.string.recuperacion_sin_vinculo
            DisponibilidadRecuperacion.NO_DISPONIBLE -> R.string.recuperacion_no_disponible
            DisponibilidadRecuperacion.ERROR_SEGURO -> R.string.recuperacion_error
            DisponibilidadRecuperacion.DISPONIBLE -> R.string.recuperacion_limites
        }))
        if (!estado.consumoHabilitado) Text(stringResource(R.string.recuperacion_consumo_pendiente))
        if (estado.disponibilidad == DisponibilidadRecuperacion.DISPONIBLE && estado.acceso == null) {
            for (opcion in PropositoRecuperacionLocal.entries) {
                TextButton(enabled = !estado.ocupado, onClick = { codigo = ""; proposito = opcion }) {
                    Text(stringResource(tituloProposito(opcion)) + if (opcion == proposito) " ✓" else "")
                }
            }
            OutlinedTextField(value = codigo, onValueChange = { if (it.length <= 128) codigo = it },
                label = { Text(stringResource(R.string.recuperacion_codigo)) }, singleLine = true,
                enabled = estado.permiteEnviar, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password))
            Button(enabled = estado.permiteEnviar && codigo.isNotBlank(), onClick = {
                val recibido = codigo
                codigo = "" // Quitar de UI antes de entregar al trabajo de IO.
                enviar(proposito, recibido)
            }) { Text(stringResource(R.string.recuperacion_enviar)) }
        }
        estado.respuesta?.let { respuesta ->
            Text(stringResource(when (respuesta.resultado) {
                ResultadoRecuperacion.AUTORIZADA -> R.string.recuperacion_autorizada
                ResultadoRecuperacion.CODIGO_INVALIDO -> R.string.recuperacion_codigo_invalido
                ResultadoRecuperacion.ESPERA -> R.string.recuperacion_espera
                ResultadoRecuperacion.ERROR_SEGURO -> R.string.recuperacion_error
                else -> R.string.recuperacion_no_disponible
            }))
            if (respuesta.esperaRestanteMs > 0) TiempoRecuperacion(respuesta.esperaRestanteMs)
        }
        estado.acceso?.let { acceso ->
            Text(stringResource(tituloProposito(acceso.proposito)))
            TiempoRecuperacion(acceso.restanteMs)
            Text(stringResource(R.string.recuperacion_herramientas_pendientes))
        }
        Text(stringResource(R.string.recuperacion_qr_pendiente))
        TextButton(onClick = { codigo = ""; volver() }) { Text(stringResource(R.string.recuperacion_cancelar)) }
    }
}

@Composable
private fun TiempoRecuperacion(ms: Long) {
    val segundos = (ms + 999) / 1000
    Text(pluralStringResource(R.plurals.recuperacion_segundos, segundos.toInt(), segundos))
}

private fun tituloProposito(proposito: PropositoRecuperacionLocal) = when (proposito) {
    PropositoRecuperacionLocal.MAINTENANCE -> R.string.recuperacion_mantenimiento
    PropositoRecuperacionLocal.RECOVER_PARENT -> R.string.recuperacion_cambiar_padre
    PropositoRecuperacionLocal.RETIRE -> R.string.recuperacion_retirada
}
