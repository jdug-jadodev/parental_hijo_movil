# Integración futura con OpenCode — Hijo Android (Samsung A13 primero)

**Ahora:** este repo se desarrolla de manera independiente. No importar el proyecto hermano ni forzar sincronización de repositorios.

**Límite de responsabilidad:** servidor WebSocket, endpoints y despliegue Render pertenecen a `C:\Users\Usuario\Documents\parental_render`. Aquí se implementa únicamente la app del hijo y su cliente de comunicación. Conservar la copia congelada de CP/1 para compatibilidad; no implementar tareas Rxx del servidor ni modificar el repositorio externo desde esta sesión sin autorización explícita.

## Identidad del contrato — mismo valor para ambos

- Protocolo: `CP/1` (v1).
- `compartido/00_CONTRATO_V1.md` SHA-256: `deee863f7587736d3e209f5a11a6e972a93004d0700c47eb3a2d136ad8f16a3c`
- `compartido/protocolo.schema.json` SHA-256: `eccf964fbb41869c47d86ddb0bfda6585136430bab297568d3c20b56607372bb`
- Mensajes de ejemplo y vectores criptográficos: mismos archivos copiados a ambos repositorios; los tests incluidos deben pasar sin modificaciones.

## Qué debe coincidir al integrar

1. Tipos y campos de los mensajes, contrato JSON, codificación canónica, prefijos de firma y algoritmo.
2. Roles PARENT / CHILD, claves públicas, id de pareja, identificación de sesión/arranque, contador, nonce y comprobación de repetición.
3. Acciones LOCK / LOCK_FOR / ALLOW / ALLOW_FOR, sus duraciones y el significado de los ACK y STATE.
4. El servidor solo retransmite y autentica; no inventa estados ni guarda secretos privados.
5. Una orden debe confirmarse como aplicada por el **hijo** para aparecer como aplicada en el padre.

## Plan posterior de integración

1. Congelar cambios de protocolo en los dos repositorios; comparar SHA-256 de contrato y esquema contra los valores indicados arriba.
2. Ejecutar ambos verificadores de ejemplos. Si no coinciden, **no conectar releases** hasta resolver la diferencia por revisión conjunta de ADR y nueva versión explícita del contrato.
3. Sustituir simuladores por el servicio real Render y el otro APK. Probar emparejamiento, firmas cruzadas, órdenes/ACK y reconexión.
4. Efectuar pruebas reales de cortes de red, reinicio, actualización, recuperación y acceso a emergencias en el dispositivo administrado.
5. Mantener repositorios independientes si se desea; compartir el protocolo después mediante librería versionada, submódulo o copia sincronizada, pero **elegir una sola fuente de verdad**.

## Reglas para OpenCode

- Abrir una instancia o workspace por repositorio. Cada IA lee su `AGENTS.md`, `ESTADO.md` y un solo ítem del plan.
- Usar simuladores/fixtures del protocolo, no copias improvisadas de los comportamientos del otro rol.
- Cualquier propuesta que cambie CP/1 queda pendiente de integración: registrar propuesta y consecuencia en un nuevo ADR, **sin modificar unilateralmente el contrato**.
- No subir claves privadas, contraseñas, códigos de recuperación ni datos personales a Git o prompts.

**Importante:** este documento describe un procedimiento futuro; no afirma que las aplicaciones estén implementadas ni sincronizadas hoy.

## Coordinación en lectura — 9 de octubre de 2026

El propietario confirmó que el padre espera CHILD_OFFER y autorizó consultar
parental_padre y parental_render exclusivamente en lectura. No autoriza modificar
esos proyectos, ejecutar sus herramientas, desplegar ni operar sobre el A56.

Se leyeron estado/instrucciones y fuentes pertinentes. SHA-256 del contrato y
schema comparados directamente en los tres repositorios: coinciden con los valores
de este documento. No se ejecutaron sus tests; resultados externos son evidencia
registrada por sus proyectos, no nuevas comprobaciones de esta sesión.

### Flujo comprobado por lectura

1. Niño muestra documento JSON CHILD_OFFER, no un sobre SIGNED ni un QR OFFLINE:
   v=1, kind, childPublicKeyB64, enrollId UUID v4 y enrollNonce de 32 bytes base64url.
   Identidad privada no exportable; oferta RAM visible cinco minutos, sin timestamp
   añadido a CP/1. Niño debe invalidarla por cancelación, proceso/boot o expiración.
2. Padre: CodecProtocolo.leerOferta y PairChild.preparar consumen esa oferta;
   generación de pairId, códigos/hashes, origen HTTPS y comparación de huellas.
   PairChild.aceptar exige huellas comparadas/códigos guardados y firma PAIR,
   persiste PREPARADO antes de mostrar el QR de aceptación. Huellas: SHA-256 del
   SPKI DER completo, hexadecimal mayúsculo agrupado de cuatro caracteres.
3. Niño necesita leer ese segundo QR SIGNED/PAIR_ACCEPT, comprobar firma y oferta
   vigente, ambas públicas/nonce/enrollId, comparar huellas y confirmar físicamente
   antes del commit público atómico. Una oferta visible no es vínculo completado.
4. Padre exporta solo ACTIVE_PAIR_ID, PARENT_PUBLIC_KEY_B64, CHILD_PUBLIC_KEY_B64
   y PUBLIC_ORIGIN para configuración externa del relay; no registro automático
   de la pareja ni envío de privadas/códigos al servidor.
5. Conexión posterior WSS /ws, subprotocolo cp.v1, reto AUTH firmado y latidos;
   requiere cliente hijo H10, además de estado/recuperación comprobados. No habilitar
   controles por haber leído un QR o recibido AUTH_OK/ACK del relay.

Fuentes externas leídas: padre app-parent/.../pairing/PairChild.kt y QrLocal.kt,
core-protocol/.../CodecProtocolo.kt y ESTADO.md; servidor ESTADO.md,
src/application/usecase/configurar-servidor.usecase.ts y
src/infrastructure/config/websocket.config.ts. Padre cuenta con cámara y lector
de imagen locales; integración óptica con QR emitido por APK hijo aún pendiente.
Servidor registra integración WSS loopback con cliente padre JVM y niño Node
simulado, no hijo Android. Conserva loopback, health 503 y producción bloqueada;
no hay despliegue público certificado. No consultar credenciales/configuración real.

### Orden siguiente en el hijo

H05 conserva núcleos/pruebas, pero recuperación operativa sigue incompleta y H06
bloqueada. H08 depende de H05: preparar compatibilidad no supone cerrar esa puerta
ni autoriza endurecer. Separar identidad/oferta inicial de aceptación/commit y de
conexión WSS. El trabajo futuro debe conservar emergencia, no inventar identidades
ni presentar vinculación completa sin recuperación y estado firmado del hijo.
QrOffline es exclusivo de recuperación y no se reutiliza aceptando CHILD_OFFER
o PAIR_ACCEPT por el mismo puerto; mantener separados propósito y autorización.
