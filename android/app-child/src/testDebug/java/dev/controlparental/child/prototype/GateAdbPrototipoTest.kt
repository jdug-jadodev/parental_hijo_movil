package dev.controlparental.child.prototype

import org.junit.Assert.*
import org.junit.Test

class GateAdbPrototipoTest {
    @Test fun `baseline NONE sin snapshot conserva gate anterior`() {
        assertEquals(Causa.NINGUNA, gateAdbPrototipo(ModoPrototipo.NONE, false, false, false, false))
    }
    @Test fun `LOCKED propio con snapshot integro permite renovacion de gate`() {
        assertEquals(Causa.NINGUNA, gateAdbPrototipo(ModoPrototipo.LOCKED, true, true, true, true))
    }
    @Test fun `LOCKED sin snapshot o PINNED no acreditan rollback propio`() {
        assertEquals(Causa.BASELINE_INCOMPATIBLE, gateAdbPrototipo(ModoPrototipo.LOCKED, false, false, true, true))
        assertEquals(Causa.SNAPSHOT_PENDIENTE, gateAdbPrototipo(ModoPrototipo.PINNED, true, true, true, true))
    }
    @Test fun `snapshot corrupto o politica ajena se rechazan`() {
        assertEquals(Causa.RESTAURACION_NO_CONFIRMADA, gateAdbPrototipo(ModoPrototipo.LOCKED, true, false, true, true))
        assertEquals(Causa.RESTAURACION_AJENA, gateAdbPrototipo(ModoPrototipo.LOCKED, true, true, false, true))
        assertEquals(Causa.RESTAURACION_AJENA, gateAdbPrototipo(ModoPrototipo.LOCKED, true, true, true, false))
    }
    @Test fun `snapshot pendiente en NONE no se renueva indiscriminadamente`() {
        assertEquals(Causa.SNAPSHOT_PENDIENTE, gateAdbPrototipo(ModoPrototipo.NONE, true, true, false, false))
    }
}
