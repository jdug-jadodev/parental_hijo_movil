package dev.controlparental.child.local

import org.junit.Assert.*
import org.junit.Test

class SesionC8V2Test {
    private var ahora = 10L
    private var boot = 7
    private fun nueva() = SesionAdulto({ ahora }, { boot }, soloC8V2 = true).apply { reanudar() }
    private fun iniciar(s: SesionAdulto): MarcoSesionAdulto {
        val t = s.solicitar(FinAdulto.GENERAR_OFERTA)!!
        assertNull(s.marcoAcreditado(t))
        assertTrue(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
        return s.marcoAcreditado(t)!!
    }
    @Test fun marcoPropioSeAnclaAFreshNoAlMenuYCadaPropositoNoLoRenueva() {
        val general = SesionAdulto({ ahora }, { boot }).apply { reanudar() }
        val e = general.solicitar(FinAdulto.ENTRAR)!!; general.completar(e, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9))
        ahora += 119000
        val s = nueva(); val marco = iniciar(s)
        assertEquals(719010L, marco.limite); assertEquals(2, marco.c8Version)
        ahora += 301000
        assertEquals(AutoridadAdulto.CERRADA, general.estado())
        assertNull(general.solicitar(FinAdulto.CONECTAR_CP2))
        assertEquals(AutoridadAdulto.VINCULAR_C8_V2, s.estado())
        for (fin in listOf(FinAdulto.VALIDAR_PAIR, FinAdulto.GUARDAR_PAIR)) {
            val t = s.solicitar(fin)!!
            assertEquals(marco.limite, t.limite)
            assertNull(s.marcoAcreditado(t)) // No acredita código por estar en el asistente.
            assertTrue(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
            assertSame(marco, s.marcoAcreditado(t))
        }
        ahora = marco.limite - 1; assertTrue(s.marcoVigente(marco))
        ahora++; assertFalse(s.marcoVigente(marco)); assertNull(s.solicitar(FinAdulto.GENERAR_OFERTA))
    }
    @Test fun diezMinutosNoOtorganAdministracionUsoRecuperacionNiCanalCp2() {
        val s = nueva(); iniciar(s)
        for (fin in FinAdulto.entries.filter { it !in setOf(FinAdulto.GENERAR_OFERTA, FinAdulto.VALIDAR_PAIR, FinAdulto.GUARDAR_PAIR) })
            assertNull("Finalidad fuera de alcance: $fin", s.solicitar(fin))
        assertNull(s.revisionRecuperacion()); assertNotEquals(AutoridadAdulto.ADMINISTRAR, s.estado())
    }
    @Test fun entradaNuevaExigeAutenticadoNoPreguntaOTicketViejo() {
        val s = nueva(); val t = s.solicitar(FinAdulto.GENERAR_OFERTA)!!
        assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 9)))
        assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 0)))
        ahora += 120000; assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
        assertFalse(s.vinculacionVigente())
    }
    @Test fun fondoBootRevisionYRelojNoRevivenMarcoNiPermitenOtraOperacion() {
        for (tipo in 0..3) {
            ahora = 10; boot = 7
            val s = nueva(); val m = iniciar(s); val t = s.solicitar(FinAdulto.GUARDAR_PAIR)!!
            when (tipo) { 0 -> s.cerrar(); 1 -> boot++; 2 -> ahora--; 3 -> assertFalse(s.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 10))) }
            if (tipo != 3) { assertFalse(s.marcoVigente(m)); s.reanudar(); assertNull(s.solicitar(FinAdulto.GENERAR_OFERTA)) }
            else assertNull(s.marcoAcreditado(t))
        }
    }
}
