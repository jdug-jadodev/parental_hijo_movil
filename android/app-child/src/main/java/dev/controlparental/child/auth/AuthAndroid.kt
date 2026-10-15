package dev.controlparental.child.auth

import android.content.Context
import android.os.SystemClock
import dev.controlparental.child.pairing.*
import dev.controlparental.child.security.FirmanteAndroidKeystore
import dev.controlparental.protocol.cp2.FirmanteAuthCp2

/** Borde productivo callable sólo por futuro transporte explícito. NO abre socket ni solicita permiso. */
internal object AuthAndroid {
    fun crear(contexto: Context): ClienteAuthHijo {
        val app = contexto.applicationContext
        fun actual(): ContextoAuthHijo? {
            val carga = PairAndroid.cargar(app) as? CargaPairCp2.Guardado ?: return null
            val s = carga.snapshot
            val identidad = IdentidadOfertaAndroid(app).leer().contexto ?: return null
            if (identidad.identidad != s.publicaHijo) return null
            val revision = CodecRegistroPair.sha(CodecRegistroPair.escribir(s.integridad, s.consentimiento))
            return ContextoAuthHijo(s.pairId, s.publicaHijo, s.publicaPadreEsperada, s.origen, revision, identidad.boot)
        }
        return ClienteAuthHijo(SystemClock::elapsedRealtime, ::actual) { esperado ->
            check(actual() == esperado)
            val existente = FirmanteAndroidKeystore(app, IdentidadOfertaAndroid.ALIAS)
            object : FirmanteAuthCp2 {
                override val publica get() = existente.clavePublica.base64
                override fun firmar(bytes: ByteArray): ByteArray {
                    check(actual() == esperado && publica == esperado.publicaHijo)
                    return existente.firmarBytes(bytes) // Sólo handle privateSigning existente, sin crear/exportar.
                }
            }
        }
    }
}
