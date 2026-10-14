package dev.controlparental.child.network

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import org.junit.Assert.*
import org.junit.Test

class ObservadorVisibleTest {
    private class ArchivoPrueba : ArchivoSnapshotLocal {
        private var bytes: ByteArray? = null
        override fun leer() = bytes
        override fun escribir(bytes: ByteArray) { this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private val c = CondicionesObservador(true, true, true, true)
    private val p = ObservadorVisible()
    private fun modelo() = UsoRedLocal(ArchivoPrueba(), ArchivoPrueba(), { 100L }, { 1 }).also {
        assertTrue(it.preparar { true }); it.iniciarObservador(1); it.observar(ValidacionRed.VALIDADA, 1, 1)
    }
    private fun rechazar(estado: CondicionesObservador) {
        for (tipo in listOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE)) {
            val m = modelo()
            assertFalse(p.autorizar(m, tipo, estado) { true }); assertFalse(m.permiteUso)
        }
    }
    @Test fun `startForeground no confirmado rechaza ambos incluso con callback`() {
        p.confirmarCallback(); rechazar(c)
    }
    @Test fun `callback fallido rechaza ambos incluso con foreground`() {
        p.confirmarForeground(); rechazar(c)
    }
    @Test fun `sin Owner API notificaciones o canal visible rechaza ambos`() {
        for (estado in listOf(c.copy(apiAdmitida = false), c.copy(deviceOwner = false),
            c.copy(notificaciones = false), c.copy(canalVisible = false))) {
            p.confirmarForeground(); p.confirmarCallback(); rechazar(estado)
        }
    }
    @Test fun `foreground y callback confirmados permiten solo con condiciones y autoridad`() {
        p.confirmarForeground(); p.confirmarCallback()
        for (tipo in listOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE)) {
            val m = modelo(); assertFalse(p.autorizar(m, tipo, c) { false }); assertFalse(m.permiteUso)
            assertTrue(p.autorizar(m, tipo, c) { true }); assertTrue(m.permiteUso)
        }
    }
    @Test fun `stopSelf o destruccion invalida capacidad y no acepta permiso anterior`() {
        p.confirmarForeground(); p.confirmarCallback(); val m = modelo()
        assertTrue(p.autorizar(m, UsoLocal.NORMAL, c) { true }); p.invalidar()
        assertFalse(p.autorizar(m, UsoLocal.EXCEPCION_OFFLINE, c) { true }); assertFalse(m.permiteUso)
    }
    @Test fun `perder notificaciones con uso vigente revoca incluso OFFLINE`() {
        p.confirmarForeground(); p.confirmarCallback(); val m = modelo()
        assertTrue(p.autorizar(m, UsoLocal.EXCEPCION_OFFLINE, c) { true })
        assertFalse(p.autorizar(m, UsoLocal.NORMAL, c.copy(notificaciones = false)) { true })
        assertFalse(m.permiteUso)
    }
}
