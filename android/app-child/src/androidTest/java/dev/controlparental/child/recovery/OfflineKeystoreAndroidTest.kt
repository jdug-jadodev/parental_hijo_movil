package dev.controlparental.child.recovery

import android.os.SystemClock
import android.os.UserManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.controlparental.child.boot.FuenteArranqueAndroid
import dev.controlparental.child.data.*
import dev.controlparental.child.security.FirmanteAndroidKeystore
import dev.controlparental.protocol.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec
import java.util.UUID

/** Laboratorio efímero: simula la firma parental, no implementa ni modifica la app padre. */
@RunWith(AndroidJUnit4::class)
class OfflineKeystoreAndroidTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private inner class Laboratorio(val almacen: ChildStateStore, val aliasHijo: String, aliasPadre: String) {
        val hijo = FirmanteAndroidKeystore(contexto, aliasHijo)
        val padre = FirmanteAndroidKeystore(contexto, aliasPadre)
        val bootId = UUID.randomUUID().toString()
        var ahora = SystemClock.elapsedRealtime()
        var desbloqueado = true
        init {
            val hashFicticio = Base64Cp1.codificar(ByteArray(32))
            almacen.actualizar { EstadoControl(vinculo = VinculoPublico(UUID.randomUUID().toString(),
                padre.clavePublica.base64, hijo.clavePublica.base64, "https://relay.invalid",
                HashesRecuperacion(hashFicticio, hashFicticio, hashFicticio)),
                bootContext = ContextoArranque(FuenteArranqueAndroid(contexto).observar().systemBootCount,
                    bootId, null, ahora), provisioningStage = EtapaAprovisionamiento.VINCULADO) }
        }
        // Reloj/contexto inyectados: pruebas de límite sin modificar Android ni esperar 5 min.
        fun controlador() = RecoveryController(almacen, { ahora },
            { ContextoRecuperacion(desbloqueado, bootId) }, hijo)
        fun estado() = (almacen.cargar() as CargaEstado.Disponible).estado
        fun emitir(c: RecoveryController, accion: AccionOffline = AccionOffline.MAINTENANCE): MensajeOffline {
            val desafio = c.crearDesafioOffline(accion)
            assertEquals(ResultadoRecuperacion.DESAFIO_CREADO, desafio.resultado)
            val resultado = CriptografiaCp1.verificar(desafio.sobre!!, hijo.clavePublica, Rol.CHILD, Proposito.OFFLINE)
            return (resultado.mensaje as MensajeOffline).also { assertFalse(it.esRespuesta) }
        }
        fun responder(d: MensajeOffline, firmante: FirmanteCp1 = padre): ByteArray = CodecCp1.serializar(
            CriptografiaCp1.firmar(d.copy(esRespuesta = true), Proposito.OFFLINE, Rol.PARENT, firmante))
    }

    private fun conLaboratorio(tarea: (Laboratorio) -> Unit) {
        assertTrue("Requiere primer desbloqueo; no intentar evitarlo", contexto.getSystemService(UserManager::class.java).isUserUnlocked)
        val id = UUID.randomUUID().toString()
        val aliasHijo = "h05-offline-prueba-hijo-$id"
        val aliasPadre = "h05-offline-prueba-respuesta-$id"
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertFalse(keystore.containsAlias(aliasHijo))
        assertFalse(keystore.containsAlias(aliasPadre))
        val directorio = File(contexto.createDeviceProtectedStorageContext().filesDir, "h05-offline-prueba-$id")
        assertTrue(directorio.mkdir())
        val archivo = File(directorio, "estado-prueba.json")
        try {
            for (alias in listOf(aliasHijo, aliasPadre)) {
                val opciones = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).build()
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
                    .apply { initialize(opciones) }.generateKeyPair()
                val privada = (keystore.getEntry(alias, null) as KeyStore.PrivateKeyEntry).privateKey
                assertNull("Privadas no exportables", privada.encoded)
            }
            tarea(Laboratorio(ChildStateStore(ArchivoAtomicAndroid(archivo)), aliasHijo, aliasPadre))
        } finally {
            // Solo estos dos alias/nombres únicos; nunca limpiar namespaces o datos de producción.
            for (alias in listOf(aliasHijo, aliasPadre)) {
                keystore.deleteEntry(alias)
                assertFalse(keystore.containsAlias(alias))
            }
            for (ruta in listOf(archivo, File(archivo.path + ".bak"), File(archivo.path + ".new"))) {
                assertTrue(!ruta.exists() || ruta.delete())
            }
            assertTrue(directorio.delete())
        }
    }

    @Test
    fun `Keystore firma desafio CHILD y respuesta parental temporal sin exportar privadas`() = conLaboratorio { l ->
        val c = l.controlador()
        val d = l.emitir(c)
        val antes = l.estado()
        assertEquals(l.bootId, d.bootId)
        assertEquals(32, Base64Cp1.decodificar(d.nonce, 32).size)
        assertNull(c.accesoActual())
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.responder(d)).resultado)
        assertEquals(PropositoRecuperacionLocal.MAINTENANCE, c.accesoActual()!!.proposito)
        assertNotNull(c.mantenimientoActual())
        assertEquals(antes, l.estado())
        assertNull(l.estado().bootContext!!.authorizedBootId)
    }

    @Test
    fun `replay no extiende sesion y recreacion pierde desafio y permiso`() = conLaboratorio { l ->
        val c = l.controlador()
        val bytes = l.responder(l.emitir(c))
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(bytes).resultado)
        l.ahora += 1000
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(bytes).resultado)
        assertEquals(299000L, c.accesoActual()!!.restanteMs)
        val nuevo = l.controlador()
        assertNull(nuevo.accesoActual())
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, nuevo.aceptarRespuestaOffline(bytes).resultado)
        c.cancelar()
        assertNull(c.accesoActual())
    }

    @Test
    fun `cinco intentos impresos no bloquean respuesta QR ni se eliminan por ella`() = conLaboratorio { l ->
        val c = l.controlador()
        repeat(5) { c.usarCodigo(PropositoRecuperacionLocal.MAINTENANCE, "invalido") }
        assertEquals(60000L, c.esperaCodigoActual())
        val bytes = l.responder(l.emitir(c))
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(bytes).resultado)
        assertNotNull(c.mantenimientoActual())
        assertEquals(5, l.estado().recoveryFailureCount)
        assertTrue(l.estado().esperaRecuperacionPendiente)
        assertTrue(l.estado().recoveryConsumed.isEmpty())
    }

    @Test
    fun `accion nonce pareja boot y clave incorrectos se rechazan antes de autorizar`() = conLaboratorio { l ->
        val c = l.controlador()
        val d = l.emitir(c)
        for (incorrecto in listOf(d.copy(action = AccionOffline.RETIRE),
                d.copy(nonce = Base64Cp1.codificar(ByteArray(32))),
                d.copy(pairId = UUID.randomUUID().toString()), d.copy(bootId = UUID.randomUUID().toString()))) {
            assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, c.aceptarRespuestaOffline(l.responder(incorrecto)).resultado)
        }
        assertEquals(ResultadoRecuperacion.RESPUESTA_INVALIDA, c.aceptarRespuestaOffline(l.responder(d, l.hijo)).resultado)
        assertNull(c.accesoActual())
        assertEquals(0, l.estado().recoveryFailureCount)
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(l.responder(d)).resultado)
    }

    @Test
    fun `RETIRE firmado solo abre autorizacion RAM sin mantenimiento ni borrado`() = conLaboratorio { l ->
        val c = l.controlador()
        val anterior = l.estado()
        val bytes = l.responder(l.emitir(c, AccionOffline.RETIRE))
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(bytes).resultado)
        assertEquals(PropositoRecuperacionLocal.RETIRE, c.accesoActual()!!.proposito)
        assertNull(c.mantenimientoActual())
        assertEquals(EtapaAprovisionamiento.RETIRADA_PENDIENTE, l.estado().provisioningStage)
        assertEquals(anterior.vinculo, l.estado().vinculo)
        assertTrue(l.estado().recoveryConsumed.isEmpty())
        assertNull(l.controlador().accesoActual())
    }

    @Test
    fun `caducidad exacta del desafio y sesion con reloj inyectado en Android`() = conLaboratorio { l ->
        val c = l.controlador()
        val bytes = l.responder(l.emitir(c))
        l.ahora += 300000
        assertEquals(ResultadoRecuperacion.DESAFIO_VENCIDO, c.aceptarRespuestaOffline(bytes).resultado)
        assertNull(c.accesoActual())
        val nuevo = l.responder(l.emitir(c))
        assertEquals(ResultadoRecuperacion.AUTORIZADA, c.aceptarRespuestaOffline(nuevo).resultado)
        l.ahora += 299999
        assertEquals(1L, c.accesoActual()!!.restanteMs)
        l.ahora++
        assertNull(c.accesoActual())
    }

    @Test
    fun `identidad Keystore retirada no se reemplaza ni fabrica desafio`() = conLaboratorio { l ->
        val c = l.controlador()
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keystore.deleteEntry(l.aliasHijo) // Solo la identidad efímera creada por este mismo test.
        val resultado = c.crearDesafioOffline(AccionOffline.MAINTENANCE)
        assertEquals(ResultadoRecuperacion.ERROR_SEGURO, resultado.resultado)
        assertNull(resultado.sobre)
        assertNull(c.accesoActual())
        assertFalse(keystore.containsAlias(l.aliasHijo))
    }

    @Test
    fun `contexto bloqueado o boot duradero distinto no permite respuesta anterior`() = conLaboratorio { l ->
        val c = l.controlador()
        l.desbloqueado = false // Contexto inyectado; no bloquear pantalla ni cambiar credencial real.
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.crearDesafioOffline(AccionOffline.MAINTENANCE).resultado)
        l.desbloqueado = true
        val bytes = l.responder(l.emitir(c))
        l.almacen.actualizar { it!!.copy(bootContext = it.bootContext!!.copy(bootId = UUID.randomUUID().toString())) }
        assertEquals(ResultadoRecuperacion.NO_DISPONIBLE, c.aceptarRespuestaOffline(bytes).resultado)
        assertNull(c.accesoActual())
    }
}
