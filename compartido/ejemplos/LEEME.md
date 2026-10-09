# Ejemplos de mensajes

Los archivos `.payload.json` muestran el cuerpo legible; los `.signed.json` contienen el sobre firmado de esos bytes canónicos. Los `.frame.json` son transporte o la oferta QR física. No sustituyen las comprobaciones con estado del receptor.

Secuencia ilustrativa: `12_challenge` -> `01_auth_padre` -> `13_auth_ok`; después `03_sync_request` -> `10_state_inicial` -> `04_lock_for` -> `05_ack_applied`. Los ejemplos del hijo y el padre usan conexiones diferentes. `06_state` representa otra sincronización posterior con otro nonce; no es respuesta a `03_sync_request`. `07_pair_accept` se usa presencialmente antes de la operación remota, no a través del relay.

Los IDs, nonces, origen y claves son ficticios. No contienen privadas; por tanto, no se pueden usar para controlar un dispositivo real ni generar nuevas órdenes con estas autoridades. En tests de integración crear identidades efímeras propias. Los hashes ficticios de recuperación no corresponden a códigos entregados para uso real.

Verificación sin instalar dependencias: desde la raíz del paquete ejecutar `node pruebas/verificar_vectores.mjs`. Los tests Kotlin/JVM deberán leer los mismos vectores y verificar SPKI/DER/bytes. La firma es válida sobre el payload canónico incluido, no sobre el JSON con indentación del archivo legible.
