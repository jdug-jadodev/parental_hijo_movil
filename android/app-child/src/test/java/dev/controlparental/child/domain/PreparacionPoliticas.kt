package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica

internal const val BOOT_PRUEBA = "00000000-0000-4000-8000-000000000003"
internal const val BOOT_NUEVO = "00000000-0000-4000-8000-000000000004"

internal class RelojSimulado(var monotonicMs: Long = 100000L) : RelojMonotonico {
    var horaCivilMs = 0L // Solo en tests: el motor no tiene acceso a esta fecha.
    var lecturas = 0
    override fun ahoraMs(): Long {
        lecturas++
        return monotonicMs
    }
}

internal fun entradaLista(
    accion: AccionPolitica = AccionPolitica.ALLOW,
    inicio: Long = 100000L,
    ahora: Long = inicio,
): EntradaEvaluacion = EntradaEvaluacion(
    esDeviceOwner = true,
    aprovisionado = true,
    vinculado = true,
    integridadValida = true,
    restriccionesVerificadas = true,
    usuarioDesbloqueado = true,
    bootId = BOOT_PRUEBA,
    authorizedBootId = BOOT_PRUEBA,
    politica = PoliticaAceptada(accion,
        if (accion == AccionPolitica.LOCK_FOR || accion == AccionPolitica.ALLOW_FOR) 60L else null,
        inicio, BOOT_PRUEBA),
    red = RedObservada(validada = true),
    canal = CanalObservado(autenticado = true, ultimoPongEnMs = ahora),
)
