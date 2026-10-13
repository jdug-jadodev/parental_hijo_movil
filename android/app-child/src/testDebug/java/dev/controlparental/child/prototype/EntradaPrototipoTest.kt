package dev.controlparental.child.prototype

import org.junit.Assert.*
import org.junit.Test

class EntradaPrototipoTest {
    private var reloj = 100L
    private val puerto = Fake()
    private val entrada = EntradaPrototipo(puerto) { reloj }
    private class Fake : PuertoEntrada {
        var paquetes = true; var features = true; var restaurado = true
        var modo = ModoPrototipo.NONE
        var errorEn: Etapa? = null
        var causaError: Causa? = null
        var preparaciones = 0; var writes = 0; var starts = 0; var rollbacks = 0
        private fun error(etapa: Etapa) {
            if (errorEn == etapa) throw causaError?.let(::FalloPrototipo)
                ?: IllegalStateException("NO registrar excepción")
        }
        override fun preparar() { preparaciones++; error(Etapa.BASELINE) }
        override fun escribirPaquetes() { writes++; error(Etapa.ESCRIBIR_PAQUETES) }
        override fun escribirFeatures() { writes++; error(Etapa.ESCRIBIR_FEATURES) }
        override fun paquetesListos(): Boolean { error(Etapa.LEER_PAQUETES); return paquetes }
        override fun featuresListos(): Boolean { error(Etapa.LEER_FEATURES); return features }
        override fun iniciar() { starts++; error(Etapa.START_LOCK_TASK) }
        override fun modoObservado(): ModoPrototipo { error(Etapa.ESPERAR_LOCKED); return modo }
        override fun iniciarRestauracion() { rollbacks++; error(Etapa.RESTAURAR) }
        override fun restauracionConfirmada(): Boolean = restaurado
    }
    @Test fun `NONE inicial espera y LOCKED posterior solo se acredita por readback`() {
        val g = entrada.entrar()
        entrada.observar(g)
        assertEquals(Etapa.ESPERAR_LOCKED, entrada.diagnostico.etapa)
        reloj += 100
        puerto.modo = ModoPrototipo.LOCKED
        entrada.observar(g)
        assertEquals(Etapa.LOCKED_VERIFICADO, entrada.diagnostico.etapa)
        assertEquals(1, puerto.starts); assertEquals(2, puerto.writes); assertEquals(0, puerto.rollbacks)
    }
    @Test fun `readbacks de politica retrasados no reescriben ni inician antes`() {
        puerto.paquetes = false; puerto.features = false
        val g = entrada.entrar()
        entrada.observar(g); assertEquals(Etapa.LEER_PAQUETES, entrada.diagnostico.etapa)
        puerto.paquetes = true
        entrada.observar(g); assertEquals(Etapa.LEER_FEATURES, entrada.diagnostico.etapa)
        assertEquals(0, puerto.starts)
        puerto.features = true
        entrada.observar(g); entrada.observar(g)
        assertEquals(1, puerto.starts); assertEquals(2, puerto.writes)
    }
    @Test fun `timeout de entrada restaura una sola vez y conserva causa y etapa`() {
        val g = entrada.entrar(); entrada.observar(g)
        reloj += 3_000
        entrada.observar(g)
        assertEquals(Diagnostico(Etapa.RESTAURAR, Causa.TIMEOUT, Etapa.ESPERAR_LOCKED), entrada.diagnostico)
        entrada.observar(g)
        assertEquals(Etapa.RESTAURADO, entrada.diagnostico.etapa)
        assertEquals(Causa.TIMEOUT, entrada.diagnostico.causa)
        entrada.observar(g); assertEquals(1, puerto.rollbacks)
    }
    @Test fun `timeout de packages y features distingue fallo`() {
        for (etapa in listOf(Etapa.LEER_PAQUETES, Etapa.LEER_FEATURES)) {
            val f = Fake().apply { paquetes = etapa != Etapa.LEER_PAQUETES; features = false }
            val e = EntradaPrototipo(f) { reloj }
            val g = e.entrar(); e.observar(g); reloj += 3_000; e.observar(g)
            assertEquals(etapa, e.diagnostico.falloEn); assertEquals(0, f.starts)
        }
    }
    @Test fun `excepcion sanitizada en cada fase no se considera exito`() {
        for (etapa in listOf(Etapa.BASELINE, Etapa.ESCRIBIR_PAQUETES, Etapa.ESCRIBIR_FEATURES,
            Etapa.LEER_PAQUETES, Etapa.LEER_FEATURES, Etapa.START_LOCK_TASK, Etapa.ESPERAR_LOCKED)) {
            val f = Fake().apply { errorEn = etapa }
            val e = EntradaPrototipo(f) { reloj }
            val g = e.entrar(); e.observar(g)
            assertEquals(Causa.ANDROID_ERROR, e.diagnostico.causa)
            assertEquals(etapa, e.diagnostico.falloEn)
            assertEquals(if (etapa == Etapa.BASELINE) 0 else 1, f.rollbacks)
        }
    }
    @Test fun `PINNED nunca es LOCKED y requiere rollback`() {
        puerto.modo = ModoPrototipo.PINNED
        entrada.observar(entrada.entrar())
        assertEquals(Causa.PINNED, entrada.diagnostico.causa); assertEquals(1, puerto.rollbacks)
    }
    @Test fun `fondo mientras pendiente cancela y readback viejo no concede LOCKED`() {
        val g = entrada.entrar(); entrada.observar(g)
        entrada.cancelar(Causa.FONDO)
        puerto.modo = ModoPrototipo.LOCKED
        entrada.observar(g)
        assertEquals(Etapa.RESTAURADO, entrada.diagnostico.etapa)
        assertEquals(Causa.FONDO, entrada.diagnostico.causa)
    }
    @Test fun `fondo despues de LOCKED no autoriza salida`() {
        puerto.modo = ModoPrototipo.LOCKED
        entrada.observar(entrada.entrar()); entrada.cancelar(Causa.FONDO)
        assertEquals(Etapa.LOCKED_VERIFICADO, entrada.diagnostico.etapa)
        assertEquals(0, puerto.rollbacks)
    }
    @Test fun `generacion obsoleta y proceso nuevo no reutilizan solicitud`() {
        val g = entrada.entrar(); entrada.cancelar(Causa.CANCELADO); entrada.observar(g)
        val nuevo = entrada.entrar()
        entrada.observar(g); assertEquals(0, puerto.starts)
        entrada.observar(nuevo); assertEquals(1, puerto.starts)
        val recreada = EntradaPrototipo(puerto) { reloj }
        recreada.observar(nuevo); assertEquals(Etapa.REPOSO, recreada.diagnostico.etapa)
    }
    @Test fun `restauracion asincrona no declara exito sin readback ni borra error`() {
        puerto.restaurado = false
        val g = entrada.entrar(); entrada.cancelar(Causa.CANCELADO)
        entrada.observar(g); assertEquals(Etapa.RESTAURAR, entrada.diagnostico.etapa)
        reloj += 2_000; entrada.observar(g)
        assertEquals(Etapa.ERROR, entrada.diagnostico.etapa)
        assertEquals(Causa.RESTAURACION_NO_CONFIRMADA, entrada.diagnostico.causa)
        assertEquals(1, puerto.rollbacks)
    }
    @Test fun `fallo rollback y reloj regresivo son errores no exito`() {
        puerto.errorEn = Etapa.RESTAURAR
        val g = entrada.entrar(); entrada.cancelar(Causa.FONDO)
        assertEquals(Etapa.ERROR, entrada.diagnostico.etapa)
        assertEquals(Causa.ANDROID_ERROR, entrada.diagnostico.causa)
        entrada.observar(g)
        val f = Fake(); val e = EntradaPrototipo(f) { reloj }
        val nuevo = e.entrar(); reloj--; e.observar(nuevo)
        assertEquals(Causa.RELOJ_INVALIDO, e.diagnostico.causa)
    }
    @Test fun `salida autorizada requiere restauracion confirmada`() {
        puerto.modo = ModoPrototipo.LOCKED
        entrada.observar(entrada.entrar())
        entrada.salirAutorizado(); entrada.observar(entrada.generacion)
        assertEquals(Diagnostico(Etapa.RESTAURADO), entrada.diagnostico)
    }
    @Test fun `causas de baseline se conservan y no restauran snapshot ajeno`() {
        for (causa in listOf(Causa.NO_OWNER, Causa.ADMIN_INACTIVO, Causa.SIN_CREDENCIAL,
            Causa.KEYGUARD_ACTIVO, Causa.SNAPSHOT_PENDIENTE, Causa.BASELINE_INCOMPATIBLE,
            Causa.SNAPSHOT_NO_GUARDADO)) {
            val f = Fake().apply { errorEn = Etapa.BASELINE; causaError = causa }
            val e = EntradaPrototipo(f) { reloj }; e.entrar()
            assertEquals(Etapa.ERROR, e.diagnostico.etapa)
            assertEquals(causa, e.diagnostico.causa)
            assertEquals(0, f.writes); assertEquals(0, f.starts); assertEquals(0, f.rollbacks)
        }
    }
    @Test fun `readback obsoleto impide start y restauracion ajena se informa`() {
        puerto.errorEn = Etapa.LEER_PAQUETES; puerto.causaError = Causa.CONTEXTO_OBSOLETO
        entrada.observar(entrada.entrar())
        assertEquals(Causa.CONTEXTO_OBSOLETO, entrada.diagnostico.causa)
        assertEquals(0, puerto.starts)
        entrada.observar(entrada.generacion) // Completar rollback previo: no iniciar otro en paralelo.
        puerto.errorEn = Etapa.RESTAURAR; puerto.causaError = Causa.RESTAURACION_AJENA
        entrada.salirAutorizado()
        assertEquals(Etapa.ERROR, entrada.diagnostico.etapa)
        assertEquals(Causa.RESTAURACION_AJENA, entrada.diagnostico.causa)
    }
}
