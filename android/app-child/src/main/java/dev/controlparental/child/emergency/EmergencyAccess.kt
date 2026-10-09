package dev.controlparental.child.emergency

import dev.controlparental.child.domain.CondicionesEmergencia
import dev.controlparental.child.domain.ResultadoAccesoEmergencia

interface PuertoPantallaBloqueo {
    fun leerCondiciones(): CondicionesEmergencia
    fun solicitarBloqueoSistema()
}

/** Acción presencial explícita. No depende de red, política, padre o clave parental. */
class EmergencyAccess(private val puerto: PuertoPantallaBloqueo) {
    fun irAPantallaBloqueo(): ResultadoAccesoEmergencia {
        val condiciones = try {
            puerto.leerCondiciones()
        } catch (_: RuntimeException) {
            return ResultadoAccesoEmergencia.COMPROBACION_NO_DISPONIBLE
        }
        if (condiciones.esDeviceOwner == null || condiciones.administradorActivo == null ||
            condiciones.admiteAdministracion == null || condiciones.tieneCredencialAndroid == null) {
            return ResultadoAccesoEmergencia.COMPROBACION_NO_DISPONIBLE
        }
        if (!condiciones.esDeviceOwner) return ResultadoAccesoEmergencia.NO_ES_DEVICE_OWNER
        if (!condiciones.administradorActivo) return ResultadoAccesoEmergencia.ADMINISTRADOR_NO_ACTIVO
        if (!condiciones.admiteAdministracion) return ResultadoAccesoEmergencia.ADMINISTRACION_NO_DISPONIBLE
        if (!condiciones.tieneCredencialAndroid) return ResultadoAccesoEmergencia.CREDENCIAL_ANDROID_REQUERIDA
        return try {
            puerto.solicitarBloqueoSistema()
            ResultadoAccesoEmergencia.SOLICITUD_ENVIADA
        } catch (_: RuntimeException) {
            ResultadoAccesoEmergencia.ERROR_AL_SOLICITAR
        }
    }
}
