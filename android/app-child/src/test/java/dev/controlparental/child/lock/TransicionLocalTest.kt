package dev.controlparental.child.lock

import org.junit.Assert.*
import org.junit.Test

class TransicionLocalTest {
    private var tiempo = 10L
    private var autoridad = true
    private class PuertoPrueba : PuertoLockLocal {
        var observado = ModoLocal.NONE; var inicios = 0; var restores = 0; var fallo = false
        override fun preparar() {}
        override fun aplicar() {}
        override fun solicitarInicio() { inicios++ }
        override fun modo() = observado
        override fun restaurar() { restores++; if (fallo) throw FalloLocal(CausaLocal.POLITICA_AJENA) }
        override fun restaurado() = !fallo && observado == ModoLocal.NONE
    }
    private val p = PuertoPrueba()
    private val m = TransicionLocal(p, { tiempo }, { autoridad })
    @Test fun `primero NONE espera una solicitud luego readback LOCKED`() {
        val g = m.entrar(); m.observar(g)
        assertEquals(FaseLocal.ESPERANDO_LOCKED, m.diagnostico.fase)
        p.observado = ModoLocal.LOCKED; m.observar(g)
        assertEquals(FaseLocal.LOCKED, m.diagnostico.fase); assertEquals(1, p.inicios)
    }
    @Test fun `timeout tiene rollback previo y no intenta hasta verde`() {
        val g = m.entrar(); tiempo += 3000; m.observar(g); m.observar(g)
        assertEquals(FaseLocal.RESTAURADO, m.diagnostico.fase)
        assertEquals(CausaLocal.TIMEOUT, m.diagnostico.causa)
        assertEquals(1, p.inicios); assertEquals(1, p.restores)
    }
    @Test fun `PINNED no es bloqueo real`() {
        val g = m.entrar(); p.observado = ModoLocal.PINNED; m.observar(g)
        assertEquals(CausaLocal.PINNED, m.diagnostico.causa)
        p.observado = ModoLocal.NONE; m.observar(g); assertEquals(FaseLocal.RESTAURADO, m.diagnostico.fase)
    }
    @Test fun `fondo o sesion caduca antes de entrada cancelan sin nuevo inicio`() {
        val g = m.entrar(); autoridad = false; m.observar(g); m.observar(g)
        assertEquals(FaseLocal.RESTAURADO, m.diagnostico.fase); assertEquals(1, p.restores)
    }
    @Test fun `LOCKED no sale por fondo muerte de sesion o cancelacion`() {
        val g = m.entrar(); p.observado = ModoLocal.LOCKED; autoridad = false
        m.observar(g); tiempo += 999_999; m.observar(g)
        assertEquals(FaseLocal.LOCKED, m.diagnostico.fase); assertEquals(0, p.restores)
    }
    @Test fun `salida exige autoridad y espera NONE verificado`() {
        autoridad = false; m.salir(); assertEquals(FaseLocal.ERROR, m.diagnostico.fase)
        assertEquals(0, p.restores)
        autoridad = true; p.observado = ModoLocal.LOCKED; val g = m.salir(); m.observar(g)
        assertEquals(FaseLocal.RESTAURANDO, m.diagnostico.fase)
        p.observado = ModoLocal.NONE; m.observar(g); assertEquals(FaseLocal.RESTAURADO, m.diagnostico.fase)
    }
    @Test fun `callback generacion vieja no materializa readback nuevo`() {
        val vieja = m.entrar(); tiempo += 3000; m.observar(vieja); m.observar(vieja)
        val nueva = m.entrar(); p.observado = ModoLocal.LOCKED; m.observar(vieja)
        assertEquals(FaseLocal.ESPERANDO_LOCKED, m.diagnostico.fase)
        m.observar(nueva); assertEquals(FaseLocal.LOCKED, m.diagnostico.fase)
    }
    @Test fun `restauracion ajena falla explicito no borra ni concede salida`() {
        p.fallo = true; m.salir()
        assertEquals(DiagnosticoLocal(FaseLocal.ERROR, CausaLocal.POLITICA_AJENA), m.diagnostico)
    }
    @Test fun `timeout restore es error no exito ni segunda restauracion`() {
        p.observado = ModoLocal.LOCKED; val g = m.salir(); tiempo += 2000; m.observar(g)
        assertEquals(FaseLocal.ERROR, m.diagnostico.fase); assertEquals(1, p.restores)
    }
    @Test fun `error concurrente si ya quedo LOCKED no aplica rollback automatico`() {
        val puerto = object : PuertoLockLocal by p {
            override fun aplicar() { p.observado = ModoLocal.LOCKED; throw FalloLocal(CausaLocal.IO_O_ANDROID) }
        }
        val modelo = TransicionLocal(puerto, { tiempo }, { autoridad }); modelo.entrar()
        assertEquals(FaseLocal.ERROR, modelo.diagnostico.fase); assertEquals(0, p.restores)
    }
}
