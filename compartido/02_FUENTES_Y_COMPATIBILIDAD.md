# Fuentes, compatibilidad y límites de la especificación

Versión documental: 1.0 · Fecha de consulta: 8 de octubre de 2026.
Estado: diseño para implementar; no existe certificación del teléfono ni una aplicación probada en este paquete.

## 1. Qué sabemos y qué no

El propietario identificó el teléfono del hijo como **A13**. Se toma como candidato un Samsung Galaxy A13, pero **no conocemos su código SM-…, versión de Android, One UI, operador ni parche de seguridad**. No se debe completar ese inventario inventando datos. Samsung publica documentación Android 14 para variantes SM-A135M y SM-A135F [S14]; eso no demuestra que el teléfono concreto la tenga instalada.

«Cualquier celular» se traduce en una arquitectura portable y una matriz de pruebas, no en una garantía imposible de verificar. Device Owner y Lock Task pertenecen a Android [S01, S02]. Este diseño no implementa control del hijo en iPhone. El panel web futuro podría administrar un hijo Android, pero no convierte un iPhone en un dispositivo Android administrado.

Decisión del proyecto, no mínimo universal de las APIs:

| Componente | Base elegida | Estado inicial |
|---|---|---|
| Padre | Android 11 / API 30 o superior | Por implementar y probar |
| Hijo | Android 12 / API 31 o superior | Por implementar y probar |
| Hijo A13 Android 12–14 | Primera familia a validar | No certificado todavía |
| Otros fabricantes y Android 15–17 | Candidatos, con pruebas específicas | No prometer compatibilidad |
| iPhone del hijo | Fuera de alcance | No soportado por este diseño |
| Navegador del padre | Evolución posterior | No implementado en v1 |

Se fija `compileSdk = 36`, `targetSdk = 36` como base deliberada del proyecto, no como afirmación de que sea la última API disponible. No se bajará el target para eludir restricciones. Los cambios de servicios en primer plano y comportamiento se revisan por versión [S05, S06, S24]. Las versiones concretas de Gradle, Kotlin, Android Gradle Plugin y dependencias se fijan al crear el proyecto desde Android Studio estable y se conservan en el repositorio.

## 2. Límites que no deben ocultarse

- El quiosco no reemplaza el cargador de arranque, la pantalla del PIN de la SIM ni el primer desbloqueo de Android.
- No se promete sobrevivir a un borrado por recovery, reinstalación del sistema, root, cargador de arranque desbloqueado ni fallos del fabricante. Restringir el restablecimiento desde Ajustes no elimina todas las vías físicas de restablecimiento [S03, S04].
- Una notificación o un WebSocket no son una garantía de ejecución continua. Hay que comprobar suspensión, reinicio del proceso y pantalla apagada [S05–S08].
- Emergencias accesibles no significa cobertura garantizada: la disponibilidad depende también del sistema de telefonía, radio, operador y estado del equipo. Nunca se exige internet al botón de emergencia [S04, S15].
- Las APIs oficiales de Device Owner son la base; no se usarán accesibilidad, superposiciones engañosas, ocultación de la app ni APIs privadas para simular control total.
- Sin servidor persistente de mensajes no se entrega una orden pendiente cuando el padre ya cerró su aplicación. El padre debe volver a abrirla, autenticarse y sincronizar.
- Solo se declara un dispositivo compatible después de aprobar las puertas de aceptación de este paquete. **Los campos de evidencia se entregan vacíos a propósito.**

## 3. Fuentes primarias

Los identificadores [Sxx] usados en el paquete apuntan a estas referencias. Los valores de intervalos, estados, límites y decisiones arquitectónicas son propuestas de este proyecto, no garantías de los proveedores.

| ID | Fuente oficial | Aplicación en este diseño |
|---|---|---|
| S01 | https://developer.android.com/work/dpc/dedicated-devices | Dispositivos dedicados y DPC |
| S02 | https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode | Lock Task, lista de paquetes y diferencia con fijar pantalla |
| S03 | https://developer.android.com/reference/android/app/admin/DevicePolicyManager | APIs de administración, propietario, quiosco, borrado y retirada |
| S04 | https://developer.android.com/reference/android/os/UserManager | Restricciones del usuario; excepción de llamadas de emergencia |
| S05 | https://developer.android.com/develop/background-work/services/fgs/service-types | Tipo `systemExempted` y sus condiciones, entre ellas Device Owner |
| S06 | https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start | Excepciones y restricciones de inicio desde segundo plano |
| S07 | https://developer.android.com/privacy-and-security/direct-boot | Almacenamiento y componentes disponibles antes del desbloqueo |
| S08 | https://developer.android.com/training/monitoring-device-state/doze-standby | Suspensión, red y pruebas de Doze |
| S09 | https://developer.android.com/develop/connectivity/network-ops/reading-network-state | Callbacks y capacidades de red validadas |
| S10 | https://developer.android.com/privacy-and-security/keystore | Claves no exportables y autenticación del usuario |
| S11 | https://render.com/docs/websocket | WSS, conexiones, reinicios y reconexión |
| S12 | https://render.com/docs/free | Suspensión y límites de los servicios gratuitos |
| S13 | https://render.com/docs/web-services | Puerto de escucha y despliegue de un web service |
| S14 | https://www.samsung.com/latin/support/model/SM-A135MZKNTPA/ ; https://www.samsung.com/levant/support/model/SM-A135FZKGMEB/ | Identificación de variantes y manuales disponibles |
| S15 | https://developer.android.com/reference/android/telecom/TelecomManager | Telefonía del sistema |
| S16 | https://android.googlesource.com/platform/frameworks/base.git/+/master/telecomm/java/android/telecom/TelecomManager.java | `createLaunchEmergencyDialerIntent` está marcado `@SystemApi` / `@hide`: no usarlo como API pública |
| S17 | https://developer.android.com/work/dpc/dedicated-devices/cookbook | Launcher persistente y aprovisionamiento de desarrollo con ADB |
| S18 | https://source.android.com/docs/devices/admin/provision | Aprovisionamiento administrado |
| S19 | https://developer.android.com/work/versions/android-12 | Handlers de aprovisionamiento requeridos desde Android 12 |
| S20 | https://developer.android.com/reference/android/os/SystemClock | Reloj monotónico para duraciones |
| S21 | https://developer.android.com/reference/android/util/AtomicFile | Escritura de archivos con sustitución atómica |
| S22 | https://developer.android.com/reference/android/net/wifi/WifiManager | Configuración de Wi-Fi; restricciones y excepciones para administración |
| S23 | https://nodejs.org/en/about/previous-releases | Selección de Node.js 24 LTS |
| S24 | https://developer.android.com/about/versions/16/behavior-changes-16 | Base target API 36 |
| S25 | https://nodejs.org/api/crypto.html | Firmas ECDSA, SPKI y codificación DER; verificar contra Node 24 al implementar |
| S26 | https://github.com/websockets/ws | Biblioteca WebSocket del relay y latidos |

## 4. Evidencia que debe completar el desarrollador

No completar «APROBADO» por haber leído documentación o por pasar pruebas unitarias. Registrar: código exacto del equipo, Android/API, One UI, parche, versión y firma del APK, fecha, caso ejecutado, resultado observado y limitación. Las pruebas de un emulador no certifican llamadas ni comportamiento de batería del A13.
