package dev.controlparental.child.pairing

import dev.controlparental.child.local.*
import dev.controlparental.protocol.Base64Cp1
import dev.controlparental.protocol.cp2.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID

class ValidacionPairRamTest {
    private inner class Laboratorio(verificador: VerificadorIntegridadPair? = null) {
        var tiempo = 10L; var boot = 5; var publicaHijo = publica(clave())
        val padre = clave(); val publicaPadre = publica(padre)
        var alLeerContexto: () -> Unit = {}
        val sesion = SesionAdulto({ tiempo }, { boot })
        private var limiteOfertaSesion = 0L
        val ofertas = OfertasRam({ tiempo }, { alLeerContexto(); ContextoOferta(boot, publicaHijo) },
            { sesion.estado() == AutoridadAdulto.ADMINISTRAR && tiempo < limiteOfertaSesion })
        val caso = if (verificador == null) ValidacionPairRam(ofertas, sesion) else ValidacionPairRam(ofertas, sesion, verificador)
        init { entrar() }
        fun entrar() {
            sesion.reanudar(); val t = sesion.solicitar(FinAdulto.ENTRAR)!!
            check(sesion.completar(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
        }
        fun oferta(): ReferenciaOferta {
            val t = sesion.solicitar(FinAdulto.GENERAR_OFERTA)!!
            check(sesion.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9)))
            limiteOfertaSesion = t.limite // Puerto de vida de sesión original, NO ampliado al validar PAIR.
            return ofertas.generar()
        }
        fun ticket(): TicketAdulto {
            val t = sesion.solicitar(FinAdulto.VALIDAR_PAIR)!!
            check(sesion.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9))); return t
        }
        fun datos() = DatosConfirmadosPair(publicaPadre, CodecPairCp2.huellaPublica(publicaPadre),
            CodecPairCp2.huellaPublica(publicaHijo), "https://relay.invalid")
        fun aceptacion(ref: ReferenciaOferta) = AceptacionPairCp2(UUID.randomUUID().toString(), ref.oferta.enrollId,
            ref.oferta.enrollNonce, publicaPadre, publicaHijo, datos().origen,
            HashesPairCp2(ref.oferta.enrollNonce, ref.oferta.enrollNonce, ref.oferta.enrollNonce))
        fun frame(a: AceptacionPairCp2): ByteArray {
            val p = CodecPairCp2.escribirPayload(a)
            val sig = Signature.getInstance("SHA256withECDSA").run {
                initSign(padre.private); update("CPv2/PAIR\n".toByteArray()); update(p); sign()
            }
            return CodecPairCp2.escribirSobre(SobrePairCp2(Base64Cp1.codificar(p), Base64Cp1.codificar(sig)))
        }
    }
    private fun clave() = KeyPairGenerator.getInstance("EC").run { initialize(ECGenParameterSpec("secp256r1")); generateKeyPair() }
    private fun publica(k: java.security.KeyPair) = Base64Cp1.codificar(k.public.encoded)
    @Test fun validacionRealConConfirmacionesYOfertaActualSinCommit() {
        val l = Laboratorio(); val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
        val a = l.aceptacion(ref); val f = l.frame(a); val v = l.caso.validar(c, f)!!
        assertEquals(a, v.aceptacion); assertEquals(ref.generacion, v.generacionOferta); assertEquals(5, v.boot)
        f.fill(0); assertEquals(a, CodecPairCp2.leerPayload(v.bytesPayload())); assertSame(v, l.caso.resultadoActual())
        assertTrue(l.ofertas.vigente(ref)) // Validación no persiste pareja ni concede uso.
    }
    @Test fun replayRamNoValidaDosVecesNiCancelarRestauraConsumo() {
        val l = Laboratorio(); val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
        val f = l.frame(l.aceptacion(ref)); assertNotNull(l.caso.validar(c, f)); assertNull(l.caso.validar(c, f))
        l.caso.cancelar(); assertNull(l.caso.resultadoActual()); assertNull(l.caso.confirmar(ref, l.ticket(), l.datos()))
    }
    @Test fun ningunaConfirmacionHumanaSeExtraeDelPayload() {
        val l = Laboratorio(); val ref = l.oferta(); val t = l.ticket(); val d = l.datos()
        for (datos in listOf(d.copy(huellaPadre = ""), d.copy(huellaHijo = ""), d.copy(origen = "http://relay.invalid"),
            d.copy(publicaPadreEsperada = publica(clave())))) assertNull(l.caso.confirmar(ref, t, datos))
    }
    @Test fun ticketDeGenerarNoEsAutoridadParaValidarPair() {
        val l = Laboratorio(); val ref = l.oferta(); val t = l.sesion.solicitar(FinAdulto.GENERAR_OFERTA)!!
        l.sesion.autorizarAccion(t, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 9))
        assertNull(l.caso.confirmar(ref, t, l.datos()))
        val fresco = l.sesion.solicitar(FinAdulto.VALIDAR_PAIR)!!
        assertNull(l.caso.confirmar(ref, fresco, l.datos())) // Solicitud sin código fresco acreditado no basta.
    }
    @Test fun aceptaSoloNonceEnrollHijoYOrigenDeContextoConfirmado() {
        val l = Laboratorio(); val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
        val a = l.aceptacion(ref)
        for (alterada in listOf(a.copy(enrollNonce = Base64Cp1.codificar(ByteArray(32))),
            a.copy(enrollId = UUID.randomUUID().toString()), a.copy(childPublicKeyB64 = publica(clave())),
            a.copy(relayOrigin = "https://otro.invalid"))) assertNull(l.caso.validar(c, l.frame(alterada)))
        assertNotNull(l.caso.validar(c, l.frame(a))) // Fallos no consumen una oferta ajena ni conceden nada.
    }
    @Test fun fondoSesionBootIdentidadYOfertaNuevaInvalidanConfirmacion() {
        for (tipo in 0..4) {
            val l = Laboratorio(); val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
            val f = l.frame(l.aceptacion(ref))
            when (tipo) {
                0 -> { l.sesion.cerrar(); l.ofertas.cancelar(); l.caso.cancelar() }
                1 -> l.tiempo += 120000
                2 -> l.boot++
                3 -> l.publicaHijo = publica(clave())
                4 -> l.oferta()
            }
            assertNull(l.caso.validar(c, f)); assertNull(l.caso.resultadoActual())
        }
    }
    @Test fun proveedorReentranteNoPublicaTrasCancelarNiDuplicaValidacion() {
        for (cancelar in listOf(true, false)) {
            lateinit var l: Laboratorio; lateinit var c: ConfirmacionPair; var llamadas = 0
            l = Laboratorio(VerificadorIntegridadPair { f, p ->
                llamadas++
                val v = CriptografiaPairCp2.verificarIntegridad(f, p)
                if (cancelar) l.caso.cancelar() else assertNull(l.caso.validar(c, f))
                v
            })
            val ref = l.oferta(); c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
            val r = l.caso.validar(c, l.frame(l.aceptacion(ref)))
            if (cancelar) { assertNull(r); assertNull(l.caso.resultadoActual()) } else assertNotNull(r)
            assertEquals(1, llamadas)
        }
    }
    @Test fun cambiosDuranteFirmaSeRevalidanAntesDePublicar() {
        for (tipo in 0..2) {
            lateinit var l: Laboratorio
            l = Laboratorio(VerificadorIntegridadPair { f, p ->
                val v = CriptografiaPairCp2.verificarIntegridad(f, p)
                when (tipo) { 0 -> l.tiempo--; 1 -> l.tiempo += 120000; 2 -> l.boot++ }; v
            })
            val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
            assertNull(l.caso.validar(c, l.frame(l.aceptacion(ref)))); assertNull(l.caso.resultadoActual())
        }
    }
    @Test fun callbackViejoNoBorraConfirmacionNuevaNiPublicaLaAnterior() {
        lateinit var l: Laboratorio; var nueva: ConfirmacionPair? = null; var reemplazar = true
        l = Laboratorio(VerificadorIntegridadPair { f, p ->
            val v = CriptografiaPairCp2.verificarIntegridad(f, p)
            if (reemplazar) {
                reemplazar = false
                val ref = l.oferta(); nueva = l.caso.confirmar(ref, l.ticket(), l.datos())!!
            }
            v
        })
        val ref = l.oferta(); val vieja = l.caso.confirmar(ref, l.ticket(), l.datos())!!
        assertNull(l.caso.validar(vieja, l.frame(l.aceptacion(ref))))
        val actual = nueva!!; val f = l.frame(l.aceptacion(actual.referencia))
        assertNotNull(l.caso.validar(actual, f)); assertNotNull(l.caso.resultadoActual())
        assertNull(l.caso.validar(vieja, f)); assertNotNull(l.caso.resultadoActual())
    }
    @Test fun bytesEntradaCopiadosAntesDeProveedorYSnapshotNoEsAutoridadTrasCaducar() {
        lateinit var entrada: ByteArray
        val l = Laboratorio(VerificadorIntegridadPair { f, p ->
            entrada.fill(0); CriptografiaPairCp2.verificarIntegridad(f, p)
        })
        val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
        entrada = l.frame(l.aceptacion(ref)); val v = l.caso.validar(c, entrada)!!
        assertEquals(0.toByte(), entrada[0]); assertEquals(v.aceptacion, CodecPairCp2.leerPayload(v.bytesPayload()))
        l.tiempo += 120000; assertNull(l.caso.resultadoActual())
        // El snapshot histórico sigue inmutable: no es permiso/commit ni se revalida por conservar sus bytes.
        assertEquals(v.aceptacion, CodecPairCp2.leerPayload(v.bytesPayload()))
    }
    @Test fun confirmarOrigenDiferenteAunqueFirmaValidaNoConfiaEnElMensaje() {
        val l = Laboratorio(); val ref = l.oferta(); val d = l.datos().copy(origen = "https://confirmado.invalid")
        val c = l.caso.confirmar(ref, l.ticket(), d)!!
        val f = l.frame(l.aceptacion(ref)); assertNotNull(CriptografiaPairCp2.verificarIntegridad(f, l.publicaPadre))
        assertNull(l.caso.validar(c, f)); assertNull(l.caso.resultadoActual())
    }
    @Test fun codigoFrescoTardioNoExtiendeVidaDeOfertaDeSesionOriginal() {
        val l = Laboratorio(); val ref = l.oferta(); l.tiempo += 119999
        val t = l.ticket(); val c = l.caso.confirmar(ref, t, l.datos())!!
        l.tiempo++; assertTrue(l.sesion.accionVigente(t)) // Código reciente aún vigente.
        assertNull(l.caso.validar(c, l.frame(l.aceptacion(ref)))) // Oferta de la sesión original ya no.
    }
    @Test fun procesoNuevoNoReconstruyeConfirmacionNiResultado() {
        val l = Laboratorio(); val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
        val nuevo = ValidacionPairRam(l.ofertas, l.sesion)
        assertNull(nuevo.validar(c, l.frame(l.aceptacion(ref)))); assertNull(nuevo.resultadoActual())
        val ofertaNueva = OfertasRam({ l.tiempo }, { ContextoOferta(l.boot, l.publicaHijo) }, { true })
        assertFalse(ofertaNueva.vigente(ref))
    }
    @Test fun consultaViejaReentranteNoBorraResultadoNuevo() {
        val l = Laboratorio(); val ref = l.oferta(); val c = l.caso.confirmar(ref, l.ticket(), l.datos())!!
        assertNotNull(l.caso.validar(c, l.frame(l.aceptacion(ref))))
        var nuevo: ValidacionPair? = null
        l.alLeerContexto = {
            l.alLeerContexto = {} // Evitar recursión de la fuente inyectada de laboratorio.
            val siguiente = l.oferta(); val confirmacion = l.caso.confirmar(siguiente, l.ticket(), l.datos())!!
            nuevo = l.caso.validar(confirmacion, l.frame(l.aceptacion(siguiente)))!!
        }
        assertNull(l.caso.resultadoActual()); assertNotNull(nuevo); assertSame(nuevo, l.caso.resultadoActual())
    }
}
