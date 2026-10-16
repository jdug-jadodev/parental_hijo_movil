package dev.controlparental.child.lock

import dev.controlparental.child.orders.*
import dev.controlparental.protocol.cp2.AccionOrdenCp2
import org.junit.Assert.*
import org.junit.Test

/** Escritor y confirmación reales; disco, reloj y respuesta de Android simulados en JVM. */
class ConfirmacionBloqueoLocalTest {
    private class Disco : ArchivosControl {
        var datos: ByteArray? = null; var journal: ByteArray? = null
        override fun leer() = datos?.copyOf()
        override fun reserva() = journal?.copyOf()
        override fun iniciar(bytes: ByteArray) { check(datos == null); datos = bytes.copyOf() }
        override fun reservar(bytes: ByteArray) { check(journal == null); journal = bytes.copyOf() }
        override fun escribir(bytes: ByteArray) { datos = bytes.copyOf() }
        override fun terminarReserva() { journal = null }
    }
    private val disco = Disco()
    private val pareja = ParejaOrdenCp2("pareja-de-prueba", "hijo", "padre", "registro", 5)
    private val writer = EscritorControlCp2(disco, { 100L }, { pareja }, { fail("Escritor incierto") })
        .also { assertTrue(it.abrir { true }) }
    private var modo = ModoLocal.NONE
    private var recuperaciones = 0; private var entregas = 0
    private fun confirmacion(d: DecisionControl) = ConfirmacionBloqueoLocal(
        { writer.esRevision(d.revision) }, { modo }, { recuperaciones++ }) {
        writer.confirmar(d.revision, it); entregas++
    }
    @Test fun `registro presente no sustituye solicitud Android ni readback real`() {
        val d = writer.local(AccionOrdenCp2.LOCK, null, false)!!
        val c = confirmacion(d); var solicitudes = 0
        lateinit var respuesta: (Boolean) -> Unit
        c.iniciar { solicitudes++; respuesta = it; true }
        assertEquals(1, solicitudes)
        assertEquals(EfectoControl.PENDING, CodecControlDurable.leer(disco.datos!!).decision!!.efecto)
        modo = ModoLocal.LOCKED; respuesta(true)
        assertEquals(EfectoControl.VERIFIED, writer.actual().decision!!.efecto)
        respuesta(false); assertEquals(1, entregas); assertEquals(0, recuperaciones)
    }
    @Test fun `aceptacion o callback positivo con NONE PINNED o lectura incierta no acredita bloqueo`() {
        for (observado in listOf(ModoLocal.NONE, ModoLocal.PINNED, null)) {
            val d = writer.local(AccionOrdenCp2.LOCK, null, false)!!
            val c = ConfirmacionBloqueoLocal({ true }, { observado }, { recuperaciones++ }) { writer.confirmar(d.revision, it) }
            c.iniciar { it(true); true }
            assertEquals(EfectoControl.FAILED, writer.actual().decision!!.efecto)
        }
        assertEquals(3, recuperaciones)
    }
    @Test fun `puerto ocupado provoca recuperacion unica sin confirmar applied`() {
        val d = writer.local(AccionOrdenCp2.LOCK, null, false)!!; val c = confirmacion(d)
        c.iniciar { false }; c.resultado(false)
        assertEquals(1, recuperaciones); assertEquals(1, entregas)
        assertEquals(EfectoControl.FAILED, writer.actual().decision!!.efecto)
    }
    @Test fun `callback tardio del bloqueo no revoca una autorizacion posterior`() {
        val bloqueo = writer.local(AccionOrdenCp2.LOCK, null, false)!!
        val c = confirmacion(bloqueo)
        lateinit var respuesta: (Boolean) -> Unit
        c.iniciar { respuesta = it; true }
        val permiso = writer.local(AccionOrdenCp2.ALLOW_FOR, 120, false)!!
        respuesta(false)
        assertEquals(0, recuperaciones); assertEquals(permiso, writer.actual().decision)
    }
}
