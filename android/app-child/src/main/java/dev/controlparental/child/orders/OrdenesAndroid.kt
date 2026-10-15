package dev.controlparental.child.orders

import android.content.Context
import dev.controlparental.child.pairing.*

internal object OrdenesAndroid {
    fun pareja(contexto: Context): ParejaOrdenCp2? {
        val s = (PairAndroid.cargar(contexto) as? CargaPairCp2.Guardado)?.snapshot ?: return null
        val identidad = IdentidadOfertaAndroid(contexto).leer().contexto ?: return null
        if (identidad.identidad != s.publicaHijo) return null
        return ParejaOrdenCp2(s.pairId, s.publicaPadreEsperada, s.publicaHijo,
            CodecRegistroPair.sha(CodecRegistroPair.escribir(s.integridad, s.consentimiento)), identidad.boot)
    }
}
