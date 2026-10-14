package dev.controlparental.child.local

import org.junit.Assert.*
import org.junit.Test

class SesionAdultoTest {
    @Test fun `proteger y restaurar requieren codigo fresco proposito y ticket vigentes`() {
        for (fin in listOf(FinAdulto.PROTEGER_APP, FinAdulto.RESTAURAR_APP)) {
            s.reanudar(); assertNull(s.solicitar(fin))
            val entrada = s.solicitar(FinAdulto.ENTRAR)!!
            s.completar(entrada, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 7))
            val t = s.solicitar(fin)!!; assertFalse(s.accionVigente(t))
            assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 7)))
            assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 8)))
            assertTrue(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 7)))
            tiempo += 120_000; assertFalse(s.accionVigente(t))
        }
    }
    @Test fun `fondo boot y ticket obsoleto nunca desprotegen`() {
        for (fin in listOf(FinAdulto.PROTEGER_APP, FinAdulto.RESTAURAR_APP)) {
            s.reanudar(); val entrada = s.solicitar(FinAdulto.ENTRAR)!!
            s.completar(entrada, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 7))
            val t = s.solicitar(fin)!!
            assertTrue(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 7)))
            val nuevo = s.solicitar(fin)!!; assertFalse(s.accionVigente(t))
            assertTrue(s.autorizarAccion(nuevo, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 7)))
            boot++; assertFalse(s.accionVigente(nuevo))
            s.cerrar(); assertFalse(s.accionVigente(nuevo))
        }
    }
    private var tiempo = 10L
    @Test fun `respuesta y cambio de codigo no autorizan retirar proteccion`() {
        s.reanudar(); val r = s.solicitar(FinAdulto.RECUPERAR)!!
        s.completar(r, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 3))
        assertNull(s.solicitar(FinAdulto.RESTAURAR_APP))
        val c = s.solicitar(FinAdulto.CAMBIAR)!!
        s.completar(c, ResultadoAdulto(TipoAdulto.GUARDADO, revision = 4))
        assertNull(s.solicitar(FinAdulto.RESTAURAR_APP))
    }
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
    @Test fun `ticket de cambiar no prolonga ventana concedida por respuesta`() {
        s.reanudar(); val t = s.solicitar(FinAdulto.RECUPERAR)!!
        s.completar(t, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 1))
        tiempo += 119_999; val cambiar = s.solicitar(FinAdulto.CAMBIAR)!!
        assertEquals(t.limite, cambiar.limite)
        tiempo++; assertFalse(s.vigente(cambiar))
    }
    @Test fun `cada bloqueo o salida exige codigo fresco y revision no solo panel`() {
        s.reanudar(); assertNull(s.solicitar(FinAdulto.BLOQUEAR))
        val entrada = s.solicitar(FinAdulto.ENTRAR)!!
        s.completar(entrada, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 1))
        val t = s.solicitar(FinAdulto.BLOQUEAR)!!
        assertFalse(s.accionVigente(t))
        assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 1)))
        assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 2)))
        assertTrue(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 1)))
        assertTrue(s.accionVigente(t)); s.cerrar(); assertFalse(s.accionVigente(t))
    }
    @Test fun `recuperacion no habilita salida hasta codigo nuevo autenticado`() {
        s.reanudar(); val r = s.solicitar(FinAdulto.RECUPERAR)!!
        s.completar(r, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 1))
        assertNull(s.solicitar(FinAdulto.SALIR_ENSAYO))
    }
}
