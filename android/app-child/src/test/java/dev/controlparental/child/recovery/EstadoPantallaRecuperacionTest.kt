package dev.controlparental.child.recovery

import dev.controlparental.child.data.PropositoRecuperacionLocal
import org.junit.Assert.*
import org.junit.Test

class EstadoPantallaRecuperacionTest {
    @Test
    fun `sin funciones operativas nunca permite gastar un codigo aunque haya vinculo`() {
        for (disponibilidad in DisponibilidadRecuperacion.entries) {
            assertFalse(EstadoPantallaRecuperacion(disponibilidad).permiteEnviar)
        }
    }

    @Test
    fun `trabajo pendiente sesion activa espera y estado no disponible deshabilitan envio`() {
        val disponible = EstadoPantallaRecuperacion(DisponibilidadRecuperacion.DISPONIBLE, consumoHabilitado = true)
        assertTrue(disponible.permiteEnviar)
        assertFalse(disponible.copy(ocupado = true).permiteEnviar)
        assertFalse(disponible.copy(respuesta = RespuestaRecuperacion(ResultadoRecuperacion.ESPERA, 1)).permiteEnviar)
        assertFalse(disponible.copy(acceso = AccesoRecuperacionLimitado(PropositoRecuperacionLocal.MAINTENANCE,
            "00000000-0000-4000-8000-000000000010", 1)).permiteEnviar)
        DisponibilidadRecuperacion.entries.filter { it != DisponibilidadRecuperacion.DISPONIBLE }.forEach {
            assertFalse(disponible.copy(disponibilidad = it).permiteEnviar)
        }
    }
}
