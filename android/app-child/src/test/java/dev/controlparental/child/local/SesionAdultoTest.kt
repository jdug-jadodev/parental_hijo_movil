package dev.controlparental.child.local

import org.junit.Assert.*
import org.junit.Test

class SesionAdultoTest {
    private var tiempo = 10L
    private var boot = 1
    private val s = SesionAdulto({ tiempo }, { boot })
    @Test fun `entrar autoriza solo administracion RAM no uso`() {
        s.reanudar(); val t = s.solicitar(FinAdulto.ENTRAR)!!
        assertTrue(s.completar(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 1)))
        assertEquals(AutoridadAdulto.ADMINISTRAR, s.estado())
        assertNull(s.revisionRecuperacion())
    }
    @Test fun `respuesta no abre administracion y cambio consume capacidad`() {
        s.reanudar(); val t = s.solicitar(FinAdulto.RECUPERAR)!!
        s.completar(t, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 1))
        assertEquals(AutoridadAdulto.CAMBIAR_CODIGO, s.estado())
        val cambio = s.solicitar(FinAdulto.CAMBIAR)!!
        s.completar(cambio, ResultadoAdulto(TipoAdulto.GUARDADO))
        assertEquals(AutoridadAdulto.CERRADA, s.estado()); assertNull(s.solicitar(FinAdulto.CAMBIAR))
    }
    @Test fun `fondo cancelacion reentrada y muerte no reabren ni aceptan callback`() {
        s.reanudar(); val t = s.solicitar(FinAdulto.ENTRAR)!!; s.cerrar(); s.reanudar()
        assertFalse(s.completar(t, ResultadoAdulto(TipoAdulto.AUTENTICADO)))
        assertEquals(AutoridadAdulto.CERRADA, SesionAdulto({ tiempo }, { boot }).estado())
    }
    @Test fun `caducidad boot reloj regresivo y ticket viejo cierran autoridad`() {
        s.reanudar(); val viejo = s.solicitar(FinAdulto.ENTRAR)!!; val nuevo = s.solicitar(FinAdulto.ENTRAR)!!
        assertFalse(s.vigente(viejo)); s.completar(nuevo, ResultadoAdulto(TipoAdulto.AUTENTICADO))
        tiempo += 120_000; assertEquals(AutoridadAdulto.CERRADA, s.estado())
        s.reanudar(); val t = s.solicitar(FinAdulto.ENTRAR)!!; boot++
        assertFalse(s.vigente(t)); tiempo = 0; assertNull(s.solicitar(FinAdulto.ENTRAR))
    }
}
