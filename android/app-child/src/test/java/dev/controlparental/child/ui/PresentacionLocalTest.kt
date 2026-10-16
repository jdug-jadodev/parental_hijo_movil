package dev.controlparental.child.ui

import dev.controlparental.child.local.*
import dev.controlparental.child.lock.ModoLocal
import dev.controlparental.child.network.UsoLocal
import dev.controlparental.child.protection.*
import org.junit.Assert.*
import org.junit.Test

class PresentacionLocalTest {
    private val pendiente = EstadoProteccion(FaseProteccion.SIN_SNAPSHOT,
        lectura = LecturaProteccion(false, false, false, false))
    private fun resumen(modo: ModoLocal? = ModoLocal.NONE, uso: UsoLocal = UsoLocal.NORMAL,
        permiso: Boolean = true, vence: Long? = null, ahora: Long = 10,
        proteccion: EstadoProteccion = pendiente) = presentarEstadoLocal(modo, uso, permiso, vence, ahora, proteccion)

    @Test fun permisoSolicitadoNoEsPermisoRealNiBloqueoFicticio() {
        assertEquals("Estado por comprobar", resumen(permiso = false).estado)
        assertEquals("Estado por comprobar", resumen(modo = null).estado)
        assertEquals("Estado por comprobar", resumen(modo = ModoLocal.PINNED).estado)
        assertEquals("Bloqueado", resumen(modo = ModoLocal.LOCKED).estado)
        assertEquals("Permitido", resumen().estado)
    }
    @Test fun contadorElapsedRedondeaAlSegundoYNoAfirmaUsoAlVencer() {
        assertEquals("0:01:00 restantes", resumen(vence = 60010).tiempo)
        assertEquals("0:00:01 restantes", resumen(vence = 11).tiempo)
        assertEquals("Estado por comprobar", resumen(vence = 10).estado)
        assertEquals("Sin tiempo autorizado activo", resumen(vence = 9).tiempo)
        assertEquals("Sin límite de tiempo", resumen().tiempo)
        assertEquals("24:00:00 restantes", resumen(vence = 86400010).tiempo)
    }
    @Test fun modalidadNoInventaExcepcionPorFaltaDeInternet() {
        assertEquals("Normal · requiere internet", resumen().modalidad)
        assertEquals("Excepción sin internet", resumen(uso = UsoLocal.EXCEPCION_OFFLINE).modalidad)
        assertEquals("Sin permiso de uso", resumen(permiso = false, uso = UsoLocal.EXCEPCION_OFFLINE).modalidad)
        assertEquals("Sin permiso de uso", resumen(uso = UsoLocal.ERROR).modalidad)
        assertEquals("Estado por comprobar", presentarEstadoLocal(ModoLocal.NONE, UsoLocal.NORMAL, true, null, 100, pendiente, 100).estado)
    }
    @Test fun ownerOSnapshotNoSignificanProteccionActiva() {
        assertEquals("Protección pendiente", resumen().proteccion)
        assertEquals("Protección por comprobar", resumen(proteccion = EstadoProteccion(FaseProteccion.ERROR)).proteccion)
        assertEquals("Protección pendiente", resumen(proteccion = pendiente.copy(snapshot = true)).proteccion)
        assertEquals("Protección pendiente", resumen(proteccion = pendiente.copy(fase = FaseProteccion.ACTIVA)).proteccion)
        val completa = LecturaProteccion(true, true, false, true)
        assertEquals("Protección activa", resumen(proteccion = EstadoProteccion(FaseProteccion.ACTIVA, lectura = completa, snapshot = true)).proteccion)
        for (l in listOf(completa.copy(uninstallEfectivo = false), completa.copy(controlPropio = false), completa.copy(controlEfectivo = false)))
            assertNotEquals("Protección activa", resumen(proteccion = EstadoProteccion(FaseProteccion.ACTIVA, lectura = l)).proteccion)
    }
    @Test fun navegarNoAutorizaNiRenuevaSesionDosMinutos() {
        var n = 10L
        val s = SesionAdulto({ n }, { 4 }).apply { reanudar() }
        val nav = NavegacionLocal()
        assertFalse(nav.abrir(PantallaLocal.SEGURIDAD, s.estado() == AutoridadAdulto.ADMINISTRAR))
        val e = s.solicitar(FinAdulto.ENTRAR)!!
        s.completar(e, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 8))
        for (destino in listOf(PantallaLocal.SEGURIDAD, PantallaLocal.DIAGNOSTICO, PantallaLocal.CONTROL)) {
            n += 30000
            assertTrue(nav.abrir(destino, s.estado() == AutoridadAdulto.ADMINISTRAR))
            assertEquals(destino, nav.pantalla)
        }
        n = e.limite
        assertFalse(nav.abrir(PantallaLocal.SEGURIDAD, s.estado() == AutoridadAdulto.ADMINISTRAR))
        assertNull(s.solicitar(FinAdulto.USO_NORMAL))
        s.cerrar(); s.reanudar()
        assertFalse(nav.abrir(PantallaLocal.CONTROL, s.estado() == AutoridadAdulto.ADMINISTRAR))
    }
    @Test fun recuperacionNoAbreAdministracionYNuevaInstanciaNoLaRestaura() {
        val s = SesionAdulto({ 10 }, { 4 }).apply { reanudar() }
        val t = s.solicitar(FinAdulto.RECUPERAR)!!
        s.completar(t, ResultadoAdulto(TipoAdulto.RESPUESTA_VALIDA, revision = 8))
        assertFalse(NavegacionLocal().abrir(PantallaLocal.SEGURIDAD, s.estado() == AutoridadAdulto.ADMINISTRAR))
        assertEquals(AutoridadAdulto.CERRADA, SesionAdulto({ 10 }, { 4 }).estado())
        assertNull(s.solicitar(FinAdulto.PROTEGER_APP))
    }
    @Test fun botonesRetiradosFondoYReentranciaNoRepitenAccion() {
        val g = GuardiaVistaLocal(); val anterior = g.revision; var acciones = 0
        assertTrue(g.ejecutar(anterior, true) {
            acciones++
            assertFalse(g.ejecutar(anterior, true) { acciones++ })
            g.retirar() // La confirmación pasa a pantalla de verificación antes de IO.
        })
        assertFalse(g.ejecutar(anterior, true) { acciones++ })
        assertFalse(g.ejecutar(g.revision, false) { acciones++ })
        assertEquals(1, acciones)
        assertTrue(g.ejecutar(g.revision, true) { acciones++ })
        assertEquals(2, acciones)
    }
    @Test fun cajonAppsExigePermisoRealYNuncaPausaTiempoOEludeGracia() {
        fun abrir(modo: ModoLocal? = ModoLocal.NONE, uso: UsoLocal = UsoLocal.NORMAL,
            permiso: Boolean = true, listo: Boolean = true, vence: Long? = 100, gracia: Long? = null, n: Long = 99) =
            puedeAbrirApps(modo, uso, permiso, listo, vence, gracia, n)
        assertTrue(abrir()); assertFalse(abrir(n = 100)); assertFalse(abrir(gracia = 99))
        assertTrue(abrir(uso = UsoLocal.EXCEPCION_OFFLINE, gracia = 99))
        assertFalse(abrir(uso = UsoLocal.EXCEPCION_OFFLINE, n = 100))
        assertFalse(abrir(permiso = false)); assertFalse(abrir(listo = false))
        for (modo in listOf(null, ModoLocal.LOCKED, ModoLocal.PINNED)) assertFalse(abrir(modo = modo))
        assertFalse(abrir(uso = UsoLocal.ERROR)); assertFalse(abrir(uso = UsoLocal.BLOQUEADO))
        assertTrue(abrir(vence = null, n = Long.MAX_VALUE - 1))
    }
}
