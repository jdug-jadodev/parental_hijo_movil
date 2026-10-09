package dev.controlparental.child.domain

/** null significa comprobación no disponible, nunca autorización implícita. */
data class CondicionesEmergencia(
    val esDeviceOwner: Boolean? = null,
    val administradorActivo: Boolean? = null,
    val admiteAdministracion: Boolean? = null,
    val tieneCredencialAndroid: Boolean? = null,
) {
    val puedeSolicitarBloqueo: Boolean
        get() = esDeviceOwner == true && administradorActivo == true &&
            admiteAdministracion == true && tieneCredencialAndroid == true
}

/** Una solicitud sin excepción NO demuestra keyguard mostrado ni marcador accesible. */
enum class ResultadoAccesoEmergencia {
    SOLICITUD_ENVIADA,
    NO_ES_DEVICE_OWNER,
    ADMINISTRADOR_NO_ACTIVO,
    ADMINISTRACION_NO_DISPONIBLE,
    CREDENCIAL_ANDROID_REQUERIDA,
    COMPROBACION_NO_DISPONIBLE,
    ERROR_AL_SOLICITAR,
}
