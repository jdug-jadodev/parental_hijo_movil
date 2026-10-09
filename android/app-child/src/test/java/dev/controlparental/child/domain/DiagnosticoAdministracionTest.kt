package dev.controlparental.child.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticoAdministracionTest {
    @Test
    fun `un administrador tradicional nunca equivale a propietario real`() {
        assertEquals(EstadoPreparacion.NO_CONFIGURADO, DiagnosticoAdministracion(false, true).estadoPreparacion)
    }

    @Test
    fun `app normal y datos iniciales no muestran proteccion activa`() {
        assertEquals(EstadoPreparacion.NO_CONFIGURADO, DiagnosticoAdministracion(false, false).estadoPreparacion)
        assertEquals(EstadoPreparacion.DIAGNOSTICO_NO_DISPONIBLE, DiagnosticoAdministracion().estadoPreparacion)
    }

    @Test
    fun `ser propietario solo habilita diagnostico de preparacion no acceso a aplicaciones`() {
        assertEquals(EstadoPreparacion.BLOQUEADO_EN_PREPARACION, DiagnosticoAdministracion(true, true).estadoPreparacion)
    }

    @Test
    fun `valores incompletos o incoherentes no se interpretan como aprovisionamiento correcto`() {
        listOf(DiagnosticoAdministracion(true, null), DiagnosticoAdministracion(true, false),
            DiagnosticoAdministracion(null, true), DiagnosticoAdministracion(null, false)).forEach {
            assertEquals(EstadoPreparacion.DIAGNOSTICO_NO_DISPONIBLE, it.estadoPreparacion)
        }
    }
}
