package dev.controlparental.child

import dev.controlparental.protocol.InformacionNucleo
import org.junit.Assert.assertEquals
import org.junit.Test

class DependenciaNucleoTest {
    @Test
    fun `la aplicacion utiliza el nucleo JVM independiente`() {
        assertEquals("CP/1", InformacionNucleo.CONTRATO)
    }
}
