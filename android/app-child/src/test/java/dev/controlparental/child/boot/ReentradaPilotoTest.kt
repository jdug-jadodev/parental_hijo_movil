package dev.controlparental.child.boot

import dev.controlparental.child.lock.*
import org.junit.Assert.*
import org.junit.Test

class ReentradaPilotoTest {
    @Test fun `fallo de primer intento no impide segundo en misma generacion`() {
        var tiempo = 0L; var modo = ModoLocal.NONE; var solicitudes = 0
        val puerto = object : PuertoLockLocal {
            override fun preparar() {}
            override fun aplicar() {}
            override fun solicitarInicio() { solicitudes++ }
            override fun modo() = modo
            override fun restaurar() { fail("No restaurar por error de reentrada") }
            override fun restaurado() = false
        }
        val primero = TransicionLocal(puerto, { tiempo }, { true }, true)
        val g = primero.entrar(); tiempo = 3000; primero.observar(g)
        assertEquals(FaseLocal.ERROR, primero.diagnostico.fase)
        assertFalse(intentoBloqueoVigente(true, FasePiloto.ERROR) { modo })
        val segundo = TransicionLocal(puerto, { tiempo }, { true }, true)
        val siguiente = segundo.entrar(); modo = ModoLocal.LOCKED; segundo.observar(siguiente)
        assertEquals(FaseLocal.LOCKED, segundo.diagnostico.fase); assertEquals(2, solicitudes)
        assertTrue(intentoBloqueoVigente(true, FasePiloto.LOCKED) { modo })
        modo = ModoLocal.NONE // Se permitió uso después del bloqueo: mismo boot, estado distinto.
        assertFalse(intentoBloqueoVigente(true, FasePiloto.LOCKED) { modo })
    }
    @Test fun `senal duplicada no interrumpe intento pendiente ni bloquea generacion nueva`() {
        assertTrue(intentoBloqueoVigente(true, FasePiloto.ESPERANDO_LOCKED) { error("No hace falta readback todavía") })
        assertFalse(intentoBloqueoVigente(false, FasePiloto.ESPERANDO_LOCKED) { ModoLocal.NONE })
        assertFalse(intentoBloqueoVigente(true, FasePiloto.PENDIENTE_KEYGUARD) { ModoLocal.NONE })
        assertFalse(intentoBloqueoVigente(true, FasePiloto.LOCKED) { ModoLocal.PINNED })
    }
}
