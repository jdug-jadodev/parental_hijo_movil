package dev.controlparental.child.prototype

import org.junit.Assert.*
import org.junit.Test

class VencimientoKeyguardTest {
    private var ahora = 100L
    private val permiso = PermisoTemporal { ahora }
    private val pendiente = VencimientoKeyguard { ahora }
    private fun ref(): ReferenciaTemporal = permiso.preparar(1, 3).also(permiso::confirmar)
    @Test fun `pantalla apagada cuenta elapsed y conserva deadline al desbloquear`() {
        val r = ref(); ahora += 90_000
        assertEquals(EntregaTemporal.VENCIDA, permiso.entregar(r, 3))
        pendiente.armar(r, 3)
        assertFalse(pendiente.intentar(r, 3, true))
        assertEquals(EstadoVencimiento.VENCIDO_PENDIENTE, pendiente.estado)
        ahora += 30_000
        assertTrue(pendiente.intentar(r, 3, false))
        assertEquals(60_100L, r.deadlineMs)
        assertEquals(EstadoVencimiento.SOLICITADO, pendiente.estado)
        pendiente.terminar(r, true)
        assertEquals(EstadoVencimiento.LOCKED, pendiente.estado)
    }
    @Test fun `Keyguard no es LOCKED y varias senales no duplican intento`() {
        val r = ref(); ahora = r.deadlineMs; pendiente.armar(r, 3)
        repeat(3) { assertFalse(pendiente.intentar(r, 3, true)) }
        assertTrue(pendiente.intentar(r, 3, false))
        assertFalse(pendiente.intentar(r, 3, false))
        assertEquals(EstadoVencimiento.SOLICITADO, pendiente.estado)
    }
    @Test fun `cancelacion de ensayo invalida callback de despertar viejo`() {
        val r = ref(); ahora = r.deadlineMs; pendiente.armar(r, 3)
        permiso.cancelar(); pendiente.cancelar()
        assertFalse(pendiente.intentar(r, 3, false))
        assertEquals(EstadoVencimiento.INACTIVO, pendiente.estado)
    }
    @Test fun `reemplazo no deja callback ni resultado de referencia anterior`() {
        val vieja = ref(); ahora = vieja.deadlineMs; pendiente.armar(vieja, 3)
        pendiente.cancelar()
        val nueva = ref(); ahora = nueva.deadlineMs; pendiente.armar(nueva, 3)
        assertFalse(pendiente.intentar(vieja, 3, false))
        assertTrue(pendiente.intentar(nueva, 3, false))
        pendiente.terminar(vieja, true)
        assertEquals(EstadoVencimiento.SOLICITADO, pendiente.estado)
    }
    @Test fun `boot distinto y reloj regresivo no ejecutan bloqueo`() {
        val r = ref(); ahora = r.deadlineMs; pendiente.armar(r, 3)
        assertFalse(pendiente.intentar(r, 4, false)); assertEquals(EstadoVencimiento.ERROR, pendiente.estado)
        pendiente.armar(r, 3); ahora--
        assertFalse(pendiente.intentar(r, 3, false)); assertEquals(EstadoVencimiento.ERROR, pendiente.estado)
    }
    @Test fun `no se arma como vencido antes del deadline`() {
        val r = ref(); pendiente.armar(r, 3)
        assertEquals(EstadoVencimiento.ERROR, pendiente.estado)
        assertFalse(pendiente.intentar(r, 3, false))
    }
    @Test fun `timeout o readback fallido son terminales no reintento ni nuevo minuto`() {
        val r = ref(); ahora = r.deadlineMs; pendiente.armar(r, 3)
        assertTrue(pendiente.intentar(r, 3, false))
        pendiente.terminar(r, false)
        assertEquals(EstadoVencimiento.ERROR, pendiente.estado)
        ahora += 10_000
        assertFalse(pendiente.intentar(r, 3, false))
        assertEquals(60_100L, r.deadlineMs)
    }
    @Test fun `caducar administracion no cancela permiso o intencion vencida`() {
        val r = ref()
        val tickets = TicketsPrototipo { ahora }; tickets.reanudar(); tickets.fondo()
        ahora = r.deadlineMs; pendiente.armar(r, 3)
        assertFalse(pendiente.intentar(r, 3, true))
        assertTrue(pendiente.intentar(r, 3, false))
    }
    @Test fun `timeout real del modelo de entrada restaura sin repetir tras desbloqueo`() {
        var inicios = 0; var restauraciones = 0
        val puerto = object : PuertoEntrada {
            override fun preparar() {}
            override fun escribirPaquetes() {}
            override fun escribirFeatures() {}
            override fun paquetesListos() = true
            override fun featuresListos() = true
            override fun iniciar() { inicios++ }
            override fun modoObservado() = ModoPrototipo.NONE
            override fun iniciarRestauracion() { restauraciones++ }
            override fun restauracionConfirmada() = true
        }
        val r = ref(); ahora = r.deadlineMs; pendiente.armar(r, 3)
        assertTrue(pendiente.intentar(r, 3, false))
        val entrada = EntradaPrototipo(puerto) { ahora }
        val g = entrada.entrar(); entrada.observar(g)
        ahora += 3_000; entrada.observar(g); entrada.observar(g)
        assertEquals(Causa.TIMEOUT, entrada.diagnostico.causa)
        pendiente.terminar(r, entrada.diagnostico.etapa == Etapa.LOCKED_VERIFICADO)
        assertEquals(EstadoVencimiento.ERROR, pendiente.estado)
        assertFalse(pendiente.intentar(r, 3, false))
        assertEquals(1, inicios); assertEquals(1, restauraciones)
    }
}
