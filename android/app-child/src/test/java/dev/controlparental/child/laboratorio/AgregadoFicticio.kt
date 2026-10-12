package dev.controlparental.child.laboratorio

/** Solo FICTICIAS_JVM. No define uso humano, día civil ni un DTO de protocolo. */
internal data class VentanaFicticia(val epoca: Long, val inicio: Long, val fin: Long, val limite: Long)
internal data class ContextoFicticio(val boot: Long, val observador: Long)
internal enum class EvidenciaFicticia { SUMABLE, NO_SUMABLE, DESCONOCIDA }
internal enum class CalidadFicticia { COMPLETA_FICTICIA, PARCIAL, NO_DISPONIBLE }
internal enum class ResultadoFicticio { CONFIRMADO, RECORTADO, IGNORADO, RECHAZADO, INCIERTO }

internal data class RegistroFicticio(
    val ventana: VentanaFicticia,
    val contexto: ContextoFicticio,
    val cursor: Long,
    val ultimoAhora: Long,
    val subtotal: Long,
    val cobertura: Long,
    val parcial: Boolean,
    val revision: Long,
)

internal data class SnapshotFicticio(
    val epoca: Long?, val revision: Long?, val subtotalConocido: Long?,
    val coberturaConocida: Long?, val calidad: CalidadFicticia,
)

/** CAS y relectura simulados; nunca archivo/Android. Un único registro, sin lista. */
internal interface RepositorioFicticio {
    fun leer(): RegistroFicticio?
    fun comprometer(anterior: RegistroFicticio?, siguiente: RegistroFicticio): Boolean
}

/** Escritor serial de laboratorio; tamaño O(1), reloj y límite de entrada inyectados. */
internal class AgregadoFicticio(
    private val repositorio: RepositorioFicticio,
    private val maxIntervalo: Long,
) {
    init { require(maxIntervalo > 0) }
    private var verificado: RegistroFicticio? = null
    private var incierto = false
    private var iniciado = false

    fun snapshot(): SnapshotFicticio {
        val r = verificado
        return SnapshotFicticio(r?.ventana?.epoca, r?.revision,
            r?.subtotal?.takeIf { r.cobertura > 0 }, r?.cobertura,
            when {
                incierto || r == null || r.cobertura == 0L -> CalidadFicticia.NO_DISPONIBLE
                r.parcial -> CalidadFicticia.PARCIAL
                else -> CalidadFicticia.COMPLETA_FICTICIA
            })
    }

    /** Inicio explícito solo sobre ausencia. Recrear objeto no inicializa ni rellena. */
    fun iniciar(v: VentanaFicticia, c: ContextoFicticio, ahora: Long): ResultadoFicticio {
        if (!ventanaValida(v) || !contextoValido(c) || ahora !in v.inicio..v.fin) return rechazar()
        val anterior = cargar() ?: if (incierto) return ResultadoFicticio.INCIERTO else null
        if (anterior != null || incierto) return rechazar()
        return activar(guardar(null, RegistroFicticio(v, c, ahora, ahora, 0, 0, ahora != v.inicio, 1)))
    }

    /** Reentrada explícita con generación nueva; jamás arrastra cursor de proceso/boot. */
    fun reanudar(v: VentanaFicticia, c: ContextoFicticio, ahora: Long): ResultadoFicticio {
        val r = cargar() ?: return ResultadoFicticio.INCIERTO
        if (!ventanaValida(v) || !contextoValido(c) || ahora !in v.inicio..v.fin ||
            v.epoca != r.ventana.epoca || v.limite != r.ventana.limite ||
            c.boot < r.contexto.boot ||
            (c.boot == r.contexto.boot && (c.observador <= r.contexto.observador ||
                v != r.ventana || ahora < r.ultimoAhora))) return rechazar()
        val siguiente = r.copy(ventana = v, contexto = c, cursor = ahora,
            ultimoAhora = ahora, parcial = true, revision = siguienteRevision(r) ?: return rechazar())
        return activar(guardar(r, siguiente))
    }

    /** Rollover explícito coherente en el mismo boot; no normaliza hora/día. */
    fun cambiarVentana(v: VentanaFicticia, c: ContextoFicticio, ahora: Long): ResultadoFicticio {
        val r = cargar() ?: return ResultadoFicticio.INCIERTO
        if (!iniciado || incierto || !ventanaValida(v) || c != r.contexto || v.epoca <= r.ventana.epoca ||
            v.inicio != r.ventana.fin || ahora !in v.inicio..v.fin ||
            ahora < r.ultimoAhora) return rechazar()
        return guardar(r, RegistroFicticio(v, c, ahora, ahora, 0, 0, ahora != v.inicio,
            siguienteRevision(r) ?: return rechazar()))
    }

    /** Intervalo [inicio, fin) ya clasificado por el test. Prefijo pasado no se corrige. */
    fun observar(epoca: Long, c: ContextoFicticio, inicio: Long, fin: Long,
        ahora: Long, evidencia: EvidenciaFicticia): ResultadoFicticio {
        val r = cargar() ?: return ResultadoFicticio.INCIERTO
        if (!iniciado || incierto || epoca != r.ventana.epoca || c != r.contexto || inicio < 0 ||
            fin <= inicio || ahora < r.ultimoAhora || fin > ahora ||
            fin - inicio > maxIntervalo || inicio < r.ventana.inicio ||
            inicio >= r.ventana.fin) return rechazar()
        val hasta = minOf(fin, r.ventana.fin)
        if (hasta <= r.cursor) return ResultadoFicticio.IGNORADO
        val desde = maxOf(inicio, r.cursor)
        val delta = hasta - desde
        val subtotal = sumar(r.subtotal, if (evidencia == EvidenciaFicticia.SUMABLE) delta else 0)
            ?: return rechazar()
        val cobertura = sumar(r.cobertura, if (evidencia != EvidenciaFicticia.DESCONOCIDA) delta else 0)
            ?: return rechazar()
        if (subtotal > r.ventana.limite || cobertura > r.ventana.limite) return rechazar()
        val siguiente = r.copy(cursor = hasta, ultimoAhora = ahora, subtotal = subtotal,
            cobertura = cobertura, parcial = r.parcial || inicio > r.cursor ||
                evidencia == EvidenciaFicticia.DESCONOCIDA,
            revision = siguienteRevision(r) ?: return rechazar())
        val resultado = guardar(r, siguiente)
        return if (resultado == ResultadoFicticio.CONFIRMADO && fin > hasta)
            ResultadoFicticio.RECORTADO else resultado
    }

    private fun cargar(): RegistroFicticio? {
        val leido = try { repositorio.leer() } catch (_: Exception) { incierto = true; return null }
        val previo = verificado
        if (leido == null) {
            if (previo != null) incierto = true
            return null
        }
        if (!registroValido(leido) || (previo != null &&
                (leido.revision < previo.revision ||
                    (leido.revision == previo.revision && leido != previo) ||
                    leido.ventana.epoca < previo.ventana.epoca || leido.contexto.boot < previo.contexto.boot ||
                    (leido.contexto.boot == previo.contexto.boot &&
                        (leido.contexto.observador < previo.contexto.observador || leido.ultimoAhora < previo.ultimoAhora)) ||
                    (leido.ventana.epoca == previo.ventana.epoca &&
                        (leido.subtotal < previo.subtotal || leido.cobertura < previo.cobertura ||
                            leido.ventana.limite != previo.ventana.limite ||
                            (previo.parcial && !leido.parcial) ||
                            (leido.contexto.boot == previo.contexto.boot &&
                                (leido.ventana != previo.ventana || leido.cursor < previo.cursor))))))) {
            incierto = true
            return null
        }
        verificado = leido
        return leido
    }

    private fun guardar(anterior: RegistroFicticio?, siguiente: RegistroFicticio): ResultadoFicticio {
        return try {
            if (!repositorio.comprometer(anterior, siguiente) || repositorio.leer() != siguiente) {
                incierto = true
                ResultadoFicticio.INCIERTO
            } else {
                verificado = siguiente
                incierto = false
                ResultadoFicticio.CONFIRMADO
            }
        } catch (_: Exception) {
            incierto = true
            ResultadoFicticio.INCIERTO
        }
    }

    private fun rechazar() = ResultadoFicticio.RECHAZADO
    private fun activar(resultado: ResultadoFicticio): ResultadoFicticio {
        if (resultado == ResultadoFicticio.CONFIRMADO) iniciado = true
        return resultado
    }
    private fun sumar(a: Long, b: Long): Long? = try { Math.addExact(a, b) } catch (_: ArithmeticException) { null }
    private fun siguienteRevision(r: RegistroFicticio) = sumar(r.revision, 1)
    private fun contextoValido(c: ContextoFicticio) = c.boot >= 0 && c.observador >= 0
    private fun ventanaValida(v: VentanaFicticia) = v.epoca >= 0 && v.inicio >= 0 &&
        v.fin > v.inicio && v.limite > 0 && v.fin - v.inicio <= v.limite
    private fun registroValido(r: RegistroFicticio) = ventanaValida(r.ventana) &&
        contextoValido(r.contexto) && r.revision > 0 &&
        r.cursor in r.ventana.inicio..r.ventana.fin && r.ultimoAhora >= r.cursor &&
        r.subtotal >= 0 && r.cobertura >= r.subtotal && r.cobertura <= r.ventana.limite
}
