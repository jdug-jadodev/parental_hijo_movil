package dev.controlparental.child.local

internal interface AlmacenAdulto {
    /** null solo si no existe ningún artefacto de configuración anterior. */
    fun leer(): RegistroAdulto?
    /** Commit y readback; un fallo no devuelve éxito ni autoridad. */
    fun guardar(registro: RegistroAdulto)
}
internal enum class TipoAdulto { SIN_CONFIGURAR, CONFIGURADO, AUTENTICADO, RESPUESTA_VALIDA, GUARDADO, RECHAZADO, ESPERA, ERROR }
internal data class ResultadoAdulto(val tipo: TipoAdulto, val pregunta: String = "", val revision: Long = 0, val esperaMs: Long = 0)

/** Escritor único local. IO/KDF solo en worker; NO cambia permisos de uso, CP/1 o políticas. */
internal class AccesoAdultoLocal(private val almacen: AlmacenAdulto, private val kdf: DerivadorAdulto,
    private val ahora: () -> Long, private val boot: () -> Int) {
    @Synchronized fun resumen(): ResultadoAdulto = seguro {
        val r = cargar() ?: return@seguro ResultadoAdulto(TipoAdulto.SIN_CONFIGURAR)
        ResultadoAdulto(TipoAdulto.CONFIGURADO, r.pregunta, r.revision, restante(r))
    }
    @Synchronized fun configurar(codigo: String, confirmar: String, pregunta: String, respuesta: String,
        confirmarRespuesta: String, bootstrapVigente: () -> Boolean): ResultadoAdulto = seguro {
        if (!bootstrapVigente() || cargar() != null || !ReglasAdulto.codigo(codigo) || codigo != confirmar ||
            !ReglasAdulto.texto(pregunta, 200) || !ReglasAdulto.texto(respuesta, 256) || respuesta != confirmarRespuesta)
            return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        val r = RegistroAdulto(1, pregunta, kdf.crear(codigo), kdf.crear(respuesta), boot = boot(),
            anclaMs = ahora(), ultimoMs = ahora())
        if (!bootstrapVigente()) return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        almacen.guardar(r)
        ResultadoAdulto(TipoAdulto.GUARDADO)
    }
    @Synchronized fun verificar(texto: String, respuesta: Boolean, vigente: () -> Boolean): ResultadoAdulto = seguro {
        var r = cargar() ?: return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        if (!vigente()) return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        val espera = restante(r)
        if (espera > 0) return@seguro ResultadoAdulto(TipoAdulto.ESPERA, esperaMs = espera)
        // Reservar fallo durable ANTES del KDF: cancelación/muerte no regalan intentos.
        val f = minOf(10, r.fallos + 1)
        r = r.copy(fallos = f, esperaMs = if (f >= 10) 300_000 else if (f >= 5) 60_000 else 0,
            anclaMs = ahora(), ultimoMs = ahora(), boot = boot())
        almacen.guardar(r)
        val valido = (if (respuesta) ReglasAdulto.texto(texto, 256) else ReglasAdulto.codigo(texto)) &&
            kdf.coincide(texto, if (respuesta) r.respuesta else r.codigo)
        if (!valido || !vigente()) return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        if (!respuesta) almacen.guardar(r.copy(fallos = 0, esperaMs = 0))
        ResultadoAdulto(if (respuesta) TipoAdulto.RESPUESTA_VALIDA else TipoAdulto.AUTENTICADO, revision = r.revision)
    }
    @Synchronized fun cambiarCodigo(codigo: String, confirmar: String, revision: Long,
        vigente: () -> Boolean): ResultadoAdulto = seguro {
        val r = cargar() ?: return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        if (!vigente() || r.revision != revision || !ReglasAdulto.codigo(codigo) || codigo != confirmar)
            return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        val nuevo = r.copy(revision = Math.incrementExact(r.revision), codigo = kdf.crear(codigo))
        if (!vigente()) return@seguro ResultadoAdulto(TipoAdulto.RECHAZADO)
        almacen.guardar(nuevo) // La respuesta/cambio NO limpian cupo ni abren administración.
        ResultadoAdulto(TipoAdulto.GUARDADO)
    }
    private fun cargar(): RegistroAdulto? {
        require(boot() >= 0 && ahora() >= 0)
        val r = almacen.leer() ?: return null
        if (r.boot != boot() || ahora() < r.ultimoMs) {
            val actualizado = r.copy(boot = boot(), anclaMs = ahora(), ultimoMs = ahora())
            almacen.guardar(actualizado) // Reimpone espera completa, no usa hora civil.
            return actualizado
        }
        val actualizado = r.copy(ultimoMs = ahora(), esperaMs = if (restante(r) == 0L) 0 else r.esperaMs)
        if (actualizado != r) almacen.guardar(actualizado)
        return actualizado
    }
    private fun restante(r: RegistroAdulto): Long = maxOf(0, r.esperaMs - (ahora() - r.anclaMs))
    private inline fun seguro(accion: () -> ResultadoAdulto): ResultadoAdulto = try { accion() }
        catch (_: Exception) { ResultadoAdulto(TipoAdulto.ERROR) }
}
