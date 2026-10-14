package dev.controlparental.child.network

import org.junit.Assert.*
import org.junit.Test

class SeleccionDefaultTest {
    @Test fun `capabilities de red vieja o intercambio durante lectura no validan default`() {
        assertEquals(ValidacionRed.DESCONOCIDA, seleccionarDefault(1L, 2L, true, true))
        assertEquals(ValidacionRed.DESCONOCIDA, seleccionarDefault(null, null, true, true))
        assertEquals(ValidacionRed.NO_VALIDADA, seleccionarDefault(2L, 2L, true, false))
        assertEquals(ValidacionRed.NO_VALIDADA, seleccionarDefault(2L, 2L, false, true))
        assertEquals(ValidacionRed.VALIDADA, seleccionarDefault(2L, 2L, true, true))
    }
}
