package dev.controlparental.child.auth

import android.content.Context
import android.os.SystemClock
import dev.controlparental.child.pairing.*
import dev.controlparental.child.security.FirmanteAndroidKeystore
import dev.controlparental.protocol.cp2.FirmanteAuthCp2

/** Borde productivo callable sólo por futuro transporte explícito. NO abre socket ni solicita permiso. */
internal object AuthAndroid {
    fun contextoActual(contexto: Context): ContextoAuthHijo? {
        val carga = PairAndroid.cargar(contexto) as? CargaPairCp2.Guardado ?: return null
        val s = carga.snapshot
        val identidad = IdentidadOfertaAndroid(contexto).leer().contexto ?: return null
        if (identidad.identidad != s.publicaHijo) return null
        return ContextoAuthHijo(s.pairId, s.publicaHijo, s.publicaPadreEsperada, s.origen,
            CodecRegistroPair.sha(CodecRegistroPair.escribir(s.integridad, s.consentimiento)), identidad.boot)
    }
    fun firmanteExistente(contexto: Context, esperado: ContextoAuthHijo): FirmanteAuthCp2 {
        val app = contexto.applicationContext
        check(contextoActual(app) == esperado)
        val existente = FirmanteAndroidKeystore(app, IdentidadOfertaAndroid.ALIAS)
        return object : FirmanteAuthCp2 {
            override val publica get() = existente.clavePublica.base64
            override fun firmar(bytes: ByteArray): ByteArray {
                check(contextoActual(app) == esperado && publica == esperado.publicaHijo)
                return existente.firmarBytes(bytes)
            }
        }
    }
    fun crear(contexto: Context): ClienteAuthHijo {
        val app = contexto.applicationContext
        return ClienteAuthHijo(SystemClock::elapsedRealtime, { contextoActual(app) }) { firmanteExistente(app, it) }
    }
}
