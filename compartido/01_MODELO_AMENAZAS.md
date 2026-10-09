# Modelo de amenazas y criterios de seguridad

Versión: 1.0. Aplicable al padre, al hijo y al relay. Consultar fuentes en `02_FUENTES_Y_COMPATIBILIDAD.md`.

## Activos y confianza

El activo principal es la autoridad del padre para cambiar la política. También importan la integridad del estado local, la disponibilidad de emergencias y la posibilidad de recuperar un equipo administrado sin dejarlo inutilizado.

Se confía en Android sin root, el bloqueo de pantalla del padre, Android Keystore, el aprovisionamiento físico autorizado y la cuenta de Render del propietario. El relay transporta metadatos de control y se confía en él para disponibilidad, **no para generar órdenes válidas**. El hijo acepta órdenes únicamente de la clave pública del padre vinculada físicamente.

## Amenazas incluidas

| Amenaza | Respuesta de diseño | Prueba requerida |
|---|---|---|
| Cambiar el teléfono al modo padre | Dos APK con identificadores distintos; ninguna elevación por selector | H-SEC-01 |
| Cerrar, desinstalar o borrar datos desde la interfaz normal | Device Owner, restricciones y políticas oficiales | H-SEC-02 |
| Reiniciar para recuperar acceso | Nuevo contexto de arranque y estado bloqueado; autorización fresca del padre | H-BOOT-01 |
| Quitar internet o bloquear el dominio del relay | Evaluación local de red y vencimiento del heartbeat | H-NET-01/03 |
| Alterar la hora | Duraciones con reloj monotónico; no usar el reloj de pared para liberar | H-TIME-02 |
| Reenviar un desbloqueo antiguo | Firma, pairId, sesión, bootId, secuencia y desafío de emisión | C-SEQ-01/04 |
| Suplantar un teléfono en el relay | Claves públicas fijadas en configuración; reto firmado por conexión | R-AUTH-01 |
| Inventar una confirmación | ACK firmado por el hijo, ligado al comando y al estado aplicado | P-ACK-01 |
| Modificar o truncar el archivo local | Escritura atómica, validación, estado cerrado ante corrupción | H-STATE-02 |
| Adivinar recuperación | Códigos aleatorios de 160 bits, separados por propósito, un solo uso y límite de intentos | H-REC-03 |
| Pérdida del padre o de su clave | Recuperación física previamente preparada y nuevo vínculo; no puerta trasera del servidor | H-REC-04 |
| API de administración no aplicada por el sistema | Comprobación del resultado; nunca informar éxito sin evidencia | H-ENF-03 |

## Amenazas y garantías excluidas

No se promete resistencia a reflasheo, extracción física avanzada, vulnerabilidades del sistema, APK de desarrollo manipulado, cuenta del padre comprometida, poseedor de códigos de recuperación o control administrativo de Render. No se promete tiempo real estricto cuando el sistema no ejecuta la app.

Un relay malicioso puede ocultar órdenes, interrumpir conexiones, mentir sobre presencia o responder latidos mientras descarta mensajes. Las firmas impiden inventar un desbloqueo, pero no solucionan esa denegación/selectividad. La interfaz distingue estado verificado, estado antiguo y disponibilidad del relay. WSS protege cada conexión, pero **no es cifrado de extremo a extremo**: Render puede ver modo, duración y mensajes mínimos de control. No se transmiten claves privadas, códigos de recuperación, contenido de aplicaciones, ubicación, contactos ni historial.

## Reglas de seguridad para implementar

La pantalla de emergencia no depende del servidor y no debe quedar detrás de un PIN parental. No se ocultan la administración ni la notificación del servicio. No se añaden grabación, captura de pantalla, lectura de chats, seguimiento, accesibilidad ni permisos «por si acaso».

Los APK de prueba nunca se entregan al niño como versión final. No se añade borrado remoto automático. La retirada del dispositivo exige presencia física, autorización explícita y dos confirmaciones de pérdida de datos.

Los logs de pruebas del desarrollador no son un historial de uso del niño: se generan con datos ficticios o códigos técnicos, y nunca incluyen firmas completas, QR, claves, códigos, políticas completas ni identificadores innecesarios. En producción solo se usan códigos de error no identificables; los logs de infraestructura de Render pueden existir aunque la app no guarde historial.

## Puertas de salida

**G0:** emergencias y quiosco en el A13 real. **G1:** firma, vinculación y recuperación. **G2:** reinicio/red/reloj y ejecución local. **G3:** padre/relay/hijo integrados. **G4:** batería, actualización, proceso muerto y pruebas de evasión de interfaz.

Si una puerta falla, no se certifica el modo estricto para ese dispositivo. Se documenta el fallo y se corrige la implementación o se declara incompatibilidad; no se sustituye por una pantalla que aparenta bloquear.
