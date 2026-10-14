package dev.controlparental.child.network

/** Elección explícita local; null no se transforma en duración predeterminada. */
internal class MinutosUso private constructor(val cantidad: Int) {
    fun deadlineDesde(inicio: Long): Long {
        require(inicio >= 0)
        return Math.addExact(inicio, Math.multiplyExact(cantidad.toLong(), 60_000L))
    }
    companion object {
        fun desdeTexto(texto: String): MinutosUso? {
            if (texto.isEmpty() || texto.any { it !in '0'..'9' }) return null
            val n = texto.toIntOrNull() ?: return null
            return if (n in 1..1440) MinutosUso(n) else null
        }
    }
}
