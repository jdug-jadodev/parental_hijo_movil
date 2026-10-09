package dev.controlparental.child.emergency

import dev.controlparental.child.domain.CondicionesEmergencia
import dev.controlparental.child.domain.ResultadoAccesoEmergencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EmergencyAccessTest {
    private class PuertoPrueba : PuertoPantallaBloqueo {
        var condiciones = CondicionesEmergencia(true, true, true, true)
        var falloLectura = false
        var falloSolicitud = false
        var solicitudes = 0
        override fun leerCondiciones(): CondicionesEmergencia {
            if (falloLectura) throw IllegalStateException("Servicio no disponible en prueba")
            return condiciones
        }
        override fun solicitarBloqueoSistema() {
            solicitudes++
            if (falloSolicitud) throw SecurityException("Permiso revocado en prueba")
        }
    }

    @Test
    fun `crear el controlador o consultar condiciones no bloquea automaticamente`() {
        val puerto = PuertoPrueba()
        EmergencyAccess(puerto)
        puerto.leerCondiciones()
        assertEquals(0, puerto.solicitudes)
    }

    @Test
    fun `accion explicita envia una solicitud no una confirmacion de acceso`() {
        val puerto = PuertoPrueba()
        assertEquals(ResultadoAccesoEmergencia.SOLICITUD_ENVIADA, EmergencyAccess(puerto).irAPantallaBloqueo())
        assertEquals(1, puerto.solicitudes)
    }

    @Test
    fun `las cuatro precondiciones falsas impiden llamadas al adaptador`() {
        val casos = listOf(
            CondicionesEmergencia(false, true, true, true) to ResultadoAccesoEmergencia.NO_ES_DEVICE_OWNER,
            CondicionesEmergencia(true, false, true, true) to ResultadoAccesoEmergencia.ADMINISTRADOR_NO_ACTIVO,
            CondicionesEmergencia(true, true, false, true) to ResultadoAccesoEmergencia.ADMINISTRACION_NO_DISPONIBLE,
            CondicionesEmergencia(true, true, true, false) to ResultadoAccesoEmergencia.CREDENCIAL_ANDROID_REQUERIDA,
        )
        casos.forEach { (condiciones, esperado) ->
            val puerto = PuertoPrueba().apply { this.condiciones = condiciones }
            assertEquals(esperado, EmergencyAccess(puerto).irAPantallaBloqueo())
            assertEquals(0, puerto.solicitudes)
            assertFalse(condiciones.puedeSolicitarBloqueo)
        }
    }

    @Test
    fun `todos los snapshots incompletos quedan sin autorizar el bloqueo`() {
        repeat(16) { mascara ->
            fun dato(bit: Int): Boolean? = if (mascara and (1 shl bit) != 0) true else null
            val puerto = PuertoPrueba().apply { condiciones = CondicionesEmergencia(dato(0), dato(1), dato(2), dato(3)) }
            val resultado = EmergencyAccess(puerto).irAPantallaBloqueo()
            assertEquals(if (mascara == 15) ResultadoAccesoEmergencia.SOLICITUD_ENVIADA
                else ResultadoAccesoEmergencia.COMPROBACION_NO_DISPONIBLE, resultado)
            assertEquals(if (mascara == 15) 1 else 0, puerto.solicitudes)
        }
    }

    @Test
    fun `fallo de lectura no realiza la accion`() {
        val puerto = PuertoPrueba().apply { falloLectura = true }
        assertEquals(ResultadoAccesoEmergencia.COMPROBACION_NO_DISPONIBLE, EmergencyAccess(puerto).irAPantallaBloqueo())
        assertEquals(0, puerto.solicitudes)
    }

    @Test
    fun `fallo de API no se presenta como acceso de emergencia correcto`() {
        val puerto = PuertoPrueba().apply { falloSolicitud = true }
        assertEquals(ResultadoAccesoEmergencia.ERROR_AL_SOLICITAR, EmergencyAccess(puerto).irAPantallaBloqueo())
        assertEquals(1, puerto.solicitudes)
    }
}
