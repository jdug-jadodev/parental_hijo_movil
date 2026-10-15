package dev.controlparental.child.orders

import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test

class ProyeccionStateCp2Test {
    private val id = "abcdefab-1234-4234-8234-abcdefabcdef"
    private val hash = dev.controlparental.protocol.Base64Cp1.codificar(ByteArray(32))
    private fun r(d: DecisionControl?) = RegistroControl("p", 1, id, id, 7, 0, d, null)
    private fun proyectar(d: DecisionControl?, lock: Boolean?, ram: Boolean, compatible: Boolean = true) =
        ProyeccionStateCp2.crear(r(d), id, hash, 1, 100, MotivoStateCp2.OK, null, true, true, lock, ram, compatible)
    @Test fun solicitudLockYPinNoSonModoLockedNiApplied() {
        val d = DecisionControl(7, AccionOrdenCp2.LOCK, false, false, 100, null, EfectoControl.PENDING)
        val s = proyectar(d, true, false)
        assertEquals(ModoStateCp2.UNKNOWN, s.effective.mode); assertEquals(EfectoStateCp2.PENDING, s.effective.enforcement)
        assertEquals(ModoStateCp2.LOCKED, proyectar(d.copy(efecto = EfectoControl.VERIFIED), true, false).effective.mode)
        assertEquals(ModoStateCp2.UNKNOWN, proyectar(d.copy(efecto = EfectoControl.VERIFIED), true, false, false).effective.mode)
    }
    @Test fun permitirExigeRevisionConfirmadaRamYNoneObservado() {
        val d = DecisionControl(7, AccionOrdenCp2.ALLOW, false, false, 100, null, EfectoControl.VERIFIED)
        assertEquals(ModoStateCp2.ALLOWED, proyectar(d, false, true).effective.mode)
        assertEquals(ModoStateCp2.UNKNOWN, proyectar(d, false, false).effective.mode)
        assertEquals(ModoStateCp2.UNKNOWN, proyectar(d, true, true).effective.mode)
    }
    @Test fun historiaNoDisponibleYSinDecisionNoSeRellenaConCeros() {
        val s = proyectar(null, false, false)
        assertEquals(FuenteStateCp2.NONE, s.effective.source); assertNull(s.effective.remainingSec)
        assertNull(s.effective.policyRevision); assertNull(s.commandChallenge)
        val bytes = CodecStateCp2.escribir(s).toString(Charsets.UTF_8)
        assertTrue(bytes.contains("\"permittedTime\":null")); assertTrue(bytes.contains("\"lastExpiry\":null"))
        assertNull(s.channelHealthy)
    }
}
