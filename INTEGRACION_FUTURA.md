# Integración futura con OpenCode — Hijo Android (Samsung A13 primero)

**Ahora:** este repo se desarrolla de manera independiente. No importar el proyecto hermano ni forzar sincronización de repositorios.

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
