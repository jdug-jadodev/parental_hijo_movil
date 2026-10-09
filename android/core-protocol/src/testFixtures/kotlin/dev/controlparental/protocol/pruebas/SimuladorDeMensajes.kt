package dev.controlparental.protocol.pruebas

/**
 * Doble de padre/relay, exclusivamente para pruebas JVM.
 * Entrega bytes de fixtures: no verifica firmas, no ejecuta políticas ni fabrica ACK.
 * El APK no depende de testFixtures y no contiene este simulador.
 */
class SimuladorDeMensajes {
    private val pendientes = ArrayDeque<ByteArray>()

    fun encolarEjemplo(nombre: String) {
        require(nombre.matches(Regex("[a-zA-Z0-9_.-]+\\.json"))) {
            "El nombre debe identificar un ejemplo JSON sin rutas."
        }
        val bytes = javaClass.classLoader.getResourceAsStream(nombre)?.use { it.readBytes() }
            ?: error("No se encontró el ejemplo congelado: $nombre")
        pendientes.addLast(bytes)
    }

    fun recibir(): ByteArray? = pendientes.removeFirstOrNull()

    fun desconectar() {
        // El relay real no conserva mensajes para entregarlos después de reconectar.
        pendientes.clear()
    }
}
