package dev.controlparental.child.laboratorio

import org.junit.Assert.*
import org.junit.Test

class AgregadoFicticioTest {
    private val ventana = VentanaFicticia(1, 0, 100, 100)
    private val contexto = ContextoFicticio(1, 1)
    private val repo = MemoriaFicticia()
    private val modelo = AgregadoFicticio(repo, Long.MAX_VALUE)

    private fun iniciar(v: VentanaFicticia = ventana, ahora: Long = 0) {
        assertEquals(ResultadoFicticio.CONFIRMADO, modelo.iniciar(v, contexto, ahora))
    }
    private fun observar(inicio: Long, fin: Long, tipo: EvidenciaFicticia = EvidenciaFicticia.SUMABLE,
        ahora: Long = fin, epoca: Long = 1, c: ContextoFicticio = contexto) =
        modelo.observar(epoca, c, inicio, fin, ahora, tipo)
    private fun subtotal(valor: Long, calidad: CalidadFicticia = CalidadFicticia.COMPLETA_FICTICIA) {
        assertEquals(valor, modelo.snapshot().subtotalConocido)
        assertEquals(calidad, modelo.snapshot().calidad)
    }

    @Test fun `ausencia no es cero ni observa al iniciar`() {
        assertNull(modelo.snapshot().subtotalConocido)
        iniciar()
        assertNull(modelo.snapshot().subtotalConocido)
        assertEquals(CalidadFicticia.NO_DISPONIBLE, modelo.snapshot().calidad)
        assertEquals(ResultadoFicticio.RECHAZADO, modelo.iniciar(ventana, contexto, 0))
    }

    @Test fun `adyacentes disjuntos y exclusion conocida`() {
        iniciar()
        observar(0, 10)
        observar(10, 20, EvidenciaFicticia.NO_SUMABLE)
        observar(20, 30)
        subtotal(20)
        assertEquals(30L, modelo.snapshot().coberturaConocida)
    }

    @Test fun `duplicado y prefijo solapado no suman otra vez`() {
        iniciar()
        observar(0, 20)
        val snapshot = modelo.snapshot()
        assertEquals(ResultadoFicticio.IGNORADO, observar(0, 20))
        assertEquals(snapshot, modelo.snapshot())
        observar(10, 30)
        subtotal(30)
        assertEquals(ResultadoFicticio.IGNORADO, observar(5, 25, ahora = 30))
        subtotal(30)
    }

    @Test fun `hueco y desconocido no se rellenan ni se reparan retrospectivamente`() {
        iniciar()
        observar(0, 10)
        observar(20, 30, EvidenciaFicticia.DESCONOCIDA)
        observar(30, 40)
        subtotal(20, CalidadFicticia.PARCIAL)
        assertEquals(20L, modelo.snapshot().coberturaConocida)
        assertEquals(ResultadoFicticio.IGNORADO, observar(10, 30, ahora = 40))
        subtotal(20, CalidadFicticia.PARCIAL)
    }

    @Test fun `solo desconocido no publica cero y exclusion si da cero observado`() {
        iniciar()
        observar(0, 10, EvidenciaFicticia.DESCONOCIDA)
        assertNull(modelo.snapshot().subtotalConocido)
        observar(10, 20, EvidenciaFicticia.NO_SUMABLE)
        subtotal(0, CalidadFicticia.PARCIAL)
    }

    @Test fun `input negativo futuro regresivo y vacio no compromete`() {
        iniciar()
        observar(0, 10)
        val anterior = repo.registro
        listOf(Triple(-1L, 20L, 20L), Triple(10L, 30L, 20L), Triple(10L, 10L, 10L),
            Triple(20L, 10L, 20L), Triple(10L, 20L, -1L), Triple(1L, 5L, 5L)).forEach {
            assertEquals(ResultadoFicticio.RECHAZADO, observar(it.first, it.second, ahora = it.third))
            assertEquals(anterior, repo.registro)
        }
    }

    @Test fun `epocas contextos y snapshots antiguos no retroceden`() {
        iniciar()
        observar(0, 10)
        val viejo = repo.registro!!
        assertEquals(ResultadoFicticio.RECHAZADO, observar(10, 20, epoca = 0))
        assertEquals(ResultadoFicticio.RECHAZADO, observar(10, 20, c = contexto.copy(boot = 0)))
        observar(10, 20)
        repo.registro = viejo
        assertEquals(ResultadoFicticio.INCIERTO, observar(20, 30))
        subtotal(20, CalidadFicticia.NO_DISPONIBLE)
    }

    @Test fun `recrear proceso exige reentrada no prolonga cursor previo`() {
        iniciar()
        observar(0, 10)
        val nuevo = AgregadoFicticio(repo, 100)
        assertEquals(ResultadoFicticio.RECHAZADO,
            nuevo.observar(1, contexto, 10, 30, 30, EvidenciaFicticia.SUMABLE))
        val c = contexto.copy(observador = 2)
        assertEquals(ResultadoFicticio.CONFIRMADO, nuevo.reanudar(ventana, c, 30))
        assertEquals(ResultadoFicticio.RECHAZADO,
            nuevo.observar(1, contexto, 10, 40, 40, EvidenciaFicticia.SUMABLE))
        nuevo.observar(1, c, 30, 40, 40, EvidenciaFicticia.SUMABLE)
        assertEquals(20L, nuevo.snapshot().subtotalConocido)
        assertEquals(CalidadFicticia.PARCIAL, nuevo.snapshot().calidad)
    }

    @Test fun `boot nuevo conserva subtotal sin extender monotono anterior`() {
        iniciar()
        observar(0, 40)
        val c = ContextoFicticio(2, 0)
        val v = ventana.copy(inicio = 0, fin = 60)
        assertEquals(ResultadoFicticio.CONFIRMADO, modelo.reanudar(v, c, 5))
        observar(5, 15, c = c)
        subtotal(50, CalidadFicticia.PARCIAL)
        assertEquals(ResultadoFicticio.RECHAZADO, modelo.reanudar(ventana, contexto, 40))
    }

    @Test fun `ventanas inyectadas 23 y 25 horas y cruce se recorta sin rollover oculto`() {
        val h = 3_600_000L
        val v = VentanaFicticia(1, 0, 23 * h, 23 * h)
        iniciar(v)
        assertEquals(ResultadoFicticio.RECORTADO, observar(0, 23 * h + 10))
        subtotal(23 * h)
        assertEquals(1L, modelo.snapshot().epoca)
        val siguiente = VentanaFicticia(2, 23 * h, 48 * h, 25 * h)
        assertEquals(ResultadoFicticio.CONFIRMADO,
            modelo.cambiarVentana(siguiente, contexto, 23 * h + 10))
        assertNull(modelo.snapshot().subtotalConocido)
        observar(23 * h + 10, 48 * h, epoca = 2)
        subtotal(25 * h - 10, CalidadFicticia.PARCIAL)
    }

    @Test fun `cruce adyacente explicito conserva cada ventana sin doble conteo`() {
        iniciar()
        observar(0, 100)
        val v = VentanaFicticia(2, 100, 200, 100)
        assertEquals(ResultadoFicticio.CONFIRMADO, modelo.cambiarVentana(v, contexto, 100))
        observar(100, 130, epoca = 2)
        subtotal(30)
        assertEquals(ResultadoFicticio.RECHAZADO, modelo.cambiarVentana(v, contexto, 130))
        assertEquals(ResultadoFicticio.RECHAZADO, observar(0, 100, ahora = 130, epoca = 1))
    }

    @Test fun `ventana incoherente salto de ancla y limite no resetean subtotal`() {
        iniciar()
        observar(0, 10)
        val anterior = repo.registro
        listOf(ventana.copy(epoca = 2, inicio = 90, fin = 190),
            ventana.copy(epoca = 2, inicio = 110, fin = 210),
            ventana.copy(epoca = 2, inicio = 100, fin = 200, limite = 99)).forEach {
            assertEquals(ResultadoFicticio.RECHAZADO, modelo.cambiarVentana(it, contexto, 120))
            assertEquals(anterior, repo.registro)
        }
        assertEquals(ResultadoFicticio.RECHAZADO,
            modelo.reanudar(ventana.copy(fin = 90), contexto.copy(observador = 2), 20))
        subtotal(10)
    }

    @Test fun `limite de recursos entrada excesiva y extremos Long`() {
        assertThrows(IllegalArgumentException::class.java) { AgregadoFicticio(repo, 0) }
        val acotado = AgregadoFicticio(repo, 10)
        acotado.iniciar(ventana, contexto, 0)
        assertEquals(ResultadoFicticio.RECHAZADO,
            acotado.observar(1, contexto, 0, 11, 11, EvidenciaFicticia.SUMABLE))
        val memoria = MemoriaFicticia()
        val extremo = AgregadoFicticio(memoria, Long.MAX_VALUE)
        val v = VentanaFicticia(Long.MAX_VALUE, 0, Long.MAX_VALUE, Long.MAX_VALUE)
        extremo.iniciar(v, contexto, 0)
        extremo.observar(v.epoca, contexto, 0, Long.MAX_VALUE, Long.MAX_VALUE, EvidenciaFicticia.SUMABLE)
        assertEquals(Long.MAX_VALUE, extremo.snapshot().subtotalConocido)
        val c = ContextoFicticio(2, 0)
        extremo.reanudar(v, c, 0)
        assertEquals(ResultadoFicticio.RECHAZADO,
            extremo.observar(v.epoca, c, 0, 1, 1, EvidenciaFicticia.SUMABLE))
        assertEquals(Long.MAX_VALUE, extremo.snapshot().subtotalConocido)
    }

    @Test fun `lectura corrupta o ausente no inicializa y conserva subtotal verificado`() {
        iniciar()
        observar(0, 10)
        repo.registro = repo.registro!!.copy(cursor = -1)
        assertEquals(ResultadoFicticio.INCIERTO, observar(10, 20))
        subtotal(10, CalidadFicticia.NO_DISPONIBLE)
        repo.registro = null
        assertEquals(ResultadoFicticio.INCIERTO, modelo.iniciar(ventana, contexto, 20))
        val recreado = AgregadoFicticio(repo, 100)
        assertEquals(ResultadoFicticio.INCIERTO, recreado.reanudar(ventana, contexto, 20))
        assertNull(recreado.snapshot().subtotalConocido)
    }

    @Test fun `fallo CAS perdida silenciosa y throw no declaran commit`() {
        for (modo in listOf(FalloFicticio.CAS, FalloFicticio.OMITIR, FalloFicticio.ANTES)) {
            val memoria = MemoriaFicticia()
            val m = AgregadoFicticio(memoria, 100)
            m.iniciar(ventana, contexto, 0)
            m.observar(1, contexto, 0, 10, 10, EvidenciaFicticia.SUMABLE)
            memoria.fallo = modo
            assertEquals(ResultadoFicticio.INCIERTO,
                m.observar(1, contexto, 10, 20, 20, EvidenciaFicticia.SUMABLE))
            assertEquals(10L, m.snapshot().subtotalConocido)
            assertEquals(CalidadFicticia.NO_DISPONIBLE, m.snapshot().calidad)
            memoria.fallo = FalloFicticio.NINGUNO
            assertEquals(ResultadoFicticio.CONFIRMADO,
                m.reanudar(ventana, contexto.copy(observador = 2), 20))
        }
    }

    @Test fun `commit con throw posterior requiere relectura y no doble suma`() {
        iniciar()
        repo.fallo = FalloFicticio.DESPUES
        assertEquals(ResultadoFicticio.INCIERTO, observar(0, 10))
        assertNull(modelo.snapshot().subtotalConocido)
        repo.fallo = FalloFicticio.NINGUNO
        val c = contexto.copy(observador = 2)
        assertEquals(ResultadoFicticio.CONFIRMADO, modelo.reanudar(ventana, c, 10))
        observar(0, 10, c = c)
        subtotal(10, CalidadFicticia.PARCIAL)
    }

    @Test fun `readback falla incluso con escritura correcta no publica exito`() {
        iniciar()
        repo.fallo = FalloFicticio.READBACK
        assertEquals(ResultadoFicticio.INCIERTO, observar(0, 10))
        assertNull(modelo.snapshot().subtotalConocido)
        repo.fallo = FalloFicticio.NINGUNO
        repo.falloLeer = false
        modelo.reanudar(ventana, contexto.copy(observador = 2), 10)
        subtotal(10, CalidadFicticia.PARCIAL)
    }

    @Test fun `revision desbordada y snapshot igual revision alterado se rechazan`() {
        iniciar()
        observar(0, 10)
        repo.registro = repo.registro!!.copy(subtotal = 9)
        assertEquals(ResultadoFicticio.INCIERTO, observar(10, 20))
        subtotal(10, CalidadFicticia.NO_DISPONIBLE)
        val memoria = MemoriaFicticia().apply {
            registro = RegistroFicticio(ventana, contexto, 10, 10, 10, 10, false, Long.MAX_VALUE)
        }
        val m = AgregadoFicticio(memoria, 100)
        assertEquals(ResultadoFicticio.RECHAZADO,
            m.reanudar(ventana, contexto.copy(observador = 2), 10))
        assertEquals(Long.MAX_VALUE, memoria.registro!!.revision)
    }

    @Test fun `muchas entradas mantienen un registro y limites sin historial`() {
        val v = VentanaFicticia(1, 0, 10_000, 10_000)
        iniciar(v)
        repeat(10_000) { observar(it.toLong(), it + 1L) }
        subtotal(10_000)
        assertEquals(10_000L, repo.registro!!.cursor)
    }

    @Test fun `ventanas iniciales negativas invertidas o exceso de limite no crean registro`() {
        listOf(ventana.copy(epoca = -1), ventana.copy(inicio = -1), ventana.copy(fin = 0),
            ventana.copy(limite = 99), ventana.copy(limite = -1)).forEach {
            assertEquals(ResultadoFicticio.RECHAZADO, modelo.iniciar(it, contexto, 0))
            assertNull(repo.registro)
        }
        assertEquals(ResultadoFicticio.RECHAZADO, modelo.iniciar(ventana, ContextoFicticio(-1, 0), 0))
    }

    @Test fun `orden atrasado conserva subtotal parcial sin reparar historia`() {
        iniciar()
        observar(10, 30)
        val anterior = modelo.snapshot()
        assertEquals(ResultadoFicticio.IGNORADO, observar(0, 20, ahora = 30))
        assertEquals(anterior, modelo.snapshot())
        subtotal(20, CalidadFicticia.PARCIAL)
    }

    @Test fun `revision mayor con boot regresivo no es snapshot valido`() {
        iniciar()
        observar(0, 10)
        repo.registro = repo.registro!!.copy(revision = 3, contexto = ContextoFicticio(0, 0))
        assertEquals(ResultadoFicticio.INCIERTO, observar(10, 20))
        subtotal(10, CalidadFicticia.NO_DISPONIBLE)
    }
}

private enum class FalloFicticio { NINGUNO, CAS, OMITIR, ANTES, DESPUES, READBACK }
private class MemoriaFicticia : RepositorioFicticio {
    var registro: RegistroFicticio? = null
    var fallo = FalloFicticio.NINGUNO
    var falloLeer = false
    override fun leer(): RegistroFicticio? {
        check(!falloLeer) { "Fallo de lectura FICTICIAS_JVM" }
        return registro
    }
    override fun comprometer(anterior: RegistroFicticio?, siguiente: RegistroFicticio): Boolean {
        if (fallo == FalloFicticio.ANTES) error("Antes del commit FICTICIAS_JVM")
        if (registro != anterior || fallo == FalloFicticio.CAS) return false
        if (fallo == FalloFicticio.OMITIR) return true
        registro = siguiente
        if (fallo == FalloFicticio.DESPUES) error("Después del commit FICTICIAS_JVM")
        if (fallo == FalloFicticio.READBACK) falloLeer = true
        return true
    }
}
