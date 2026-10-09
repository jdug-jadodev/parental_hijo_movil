package dev.controlparental.child.domain

import dev.controlparental.protocol.AccionPolitica
import dev.controlparental.protocol.CodigoCp1
import dev.controlparental.protocol.ModoAcceso
import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoYCoherenciaTest {
    @Test
    fun `cambiar fecha y zona simuladas no pausa ni adelanta la cuenta regresiva`() {
        val reloj = RelojSimulado(120000)
        val motor = PolicyEngine(reloj)
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR)
        val original = motor.evaluar(entrada)
        listOf(-86400000L, 86400000L, Long.MAX_VALUE).forEach {
            reloj.horaCivilMs = it
            assertEquals(original, motor.evaluar(entrada))
        }
        reloj.monotonicMs = 130000
        assertEquals(30L, motor.evaluar(entrada).politicaRestanteSegundos)
    }

    @Test
    fun `suspension simulada sigue consumiendo el tiempo aunque no se evalue mientras tanto`() {
        val reloj = RelojSimulado()
        val motor = PolicyEngine(reloj)
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR)
        assertEquals(60L, motor.evaluar(entrada).politicaRestanteSegundos)
        reloj.monotonicMs += 3600000
        val resultado = motor.evaluar(entrada)
        assertEquals(ModoAcceso.LOCKED, resultado.modoSolicitado)
        assertEquals(0L, resultado.politicaRestanteSegundos)
        assertEquals(CodigoCp1.TIMER_EXPIRED, motor.evaluar(entrada.copy(
            canal = CanalObservado(true, reloj.monotonicMs))).motivo)
    }

    @Test
    fun `reevaluar y recrear motor no modifica el inicio aceptado ni restaura canal`() {
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR)
        val motor = PolicyEngine(RelojSimulado(120000))
        assertEquals(motor.evaluar(entrada), motor.evaluar(entrada))
        val recreado = PolicyEngine(RelojSimulado(150000))
        val sinCanal = recreado.evaluar(entrada.copy(canal = CanalObservado()))
        assertEquals(CodigoCp1.CHANNEL_EXPIRED, sinCanal.motivo)
        assertEquals(10L, sinCanal.politicaRestanteSegundos)
        assertEquals(100000L, entrada.politica!!.aceptadaEnMs)
    }

    @Test
    fun `una politica nueva sustituye la anterior sin sumar tiempos`() {
        val antigua = entradaLista(AccionPolitica.ALLOW_FOR)
        val nueva = antigua.copy(politica = PoliticaAceptada(AccionPolitica.LOCK_FOR, 60, 120000, BOOT_PRUEBA))
        val resultado = PolicyEngine(RelojSimulado(120000)).evaluar(nueva)
        assertEquals(CodigoCp1.PARENT_LOCK, resultado.motivo)
        assertEquals(60L, resultado.politicaRestanteSegundos)
    }

    @Test
    fun `politicas incoherentes no se convierten en permisos por defecto`() {
        val entrada = entradaLista()
        val invalidas = listOf(PoliticaAceptada(AccionPolitica.ALLOW, 60, 0, BOOT_PRUEBA),
            PoliticaAceptada(AccionPolitica.ALLOW_FOR, null, 0, BOOT_PRUEBA),
            PoliticaAceptada(AccionPolitica.LOCK_FOR, 59, 0, BOOT_PRUEBA),
            PoliticaAceptada(AccionPolitica.ALLOW_FOR, Long.MAX_VALUE, 0, BOOT_PRUEBA),
            PoliticaAceptada(AccionPolitica.ALLOW, null, -1, BOOT_PRUEBA),
            PoliticaAceptada(AccionPolitica.ALLOW, null, 100001, BOOT_PRUEBA),
            PoliticaAceptada(AccionPolitica.ALLOW, null, 0, "boot-invalido"))
        invalidas.forEach {
            assertEquals(CodigoCp1.STATE_CORRUPT, PolicyEngine(RelojSimulado()).evaluar(entrada.copy(politica = it)).motivo)
        }
        assertEquals(CodigoCp1.BOOT_AUTH_REQUIRED, PolicyEngine(RelojSimulado()).evaluar(entrada.copy(politica = null)).motivo)
    }

    @Test
    fun `timestamps futuros negativos o reloj indisponible nunca amplian acceso`() {
        val entrada = entradaLista()
        val invalidas = listOf(entrada.copy(red = RedObservada(false, -1)),
            entrada.copy(red = RedObservada(false, 100001)),
            entrada.copy(canal = CanalObservado(true, -1)), entrada.copy(canal = CanalObservado(true, 100001)))
        invalidas.forEach {
            assertEquals(CodigoCp1.STATE_CORRUPT, PolicyEngine(RelojSimulado()).evaluar(it).motivo)
        }
        assertEquals(CodigoCp1.STATE_CORRUPT, PolicyEngine(RelojSimulado(-1)).evaluar(entrada).motivo)
        assertEquals(CodigoCp1.STATE_CORRUPT, PolicyEngine { error("Reloj de pruebas indisponible") }.evaluar(entrada).motivo)
    }

    @Test
    fun `aritmetica cerca del maximo Long no desborda el temporizador`() {
        val inicio = Long.MAX_VALUE - 1000L
        val entrada = entradaLista(AccionPolitica.ALLOW_FOR, inicio, Long.MAX_VALUE)
        val resultado = PolicyEngine(RelojSimulado(Long.MAX_VALUE)).evaluar(entrada)
        assertEquals(59L, resultado.politicaRestanteSegundos)
        assertEquals(ModoAcceso.ALLOWED, resultado.modoSolicitado)
    }
}
