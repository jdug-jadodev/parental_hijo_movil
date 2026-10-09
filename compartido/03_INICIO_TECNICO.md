# Inicio técnico independiente — tareas C00, C01 y C02

Este proyecto se desarrolla **sin checkout del otro**. Solo usa su copia local del protocolo CP/1 y los fixtures comunes.

## C00 — Crear el repositorio y fijar herramientas

**Entregable:** repositorio del hijo con `app-child/`, `core-protocol/` y documentación de instalación del A13. **No crear el APK del padre ni el servidor Render.**

Crear proyecto Kotlin en Android Studio, registrar versiones reales de Android Studio/JDK/Gradle/AGP/Kotlin y SDK en `BUILD_ENV.md`. Añadir `.gitignore` para `.env`, keystores, claves, APK, `local.properties` y builds. Separar código Android del núcleo puro JVM para facilitar tests. Preparar un **simulador de padre/relay para pruebas** que use el contrato y los fixtures, sin usarlo como dispositivo administrado real.

**Cierre:** compila el esqueleto de `app-child` y tests del núcleo; las restricciones físicas siguen PENDIENTES hasta ejecutarse en el A13.

## C01 — Implementar modelos y validación del protocolo

**Entrada:** `compartido/00_CONTRATO_V1.md`, `compartido/protocolo.schema.json`, ejemplos. Implementar DTO, parser estricto, límites de mensaje y serialización determinista conforme CP/1, aislados del código específico de Android/Node. No reinterpretar el contrato para cada repo.

**Pruebas:** payloads correctos/incorrectos, secuencias, tamaños, campos desconocidos, claves duplicadas, firmas alteradas, orden de claves y bytes exactos. Actualizar `ESTADO.md` con comandos ejecutados.

## C02 — Firma, verificación y vectores compartidos

**Entrada:** CP/1 y fixtures de `pruebas/vectores_crypto.json`. Implementar P-256 ECDSA SHA-256/DER sin inventar criptografía, manteniendo privadas del móvil en Android Keystore. Probar interoperabilidad con los ejemplos. El código de prueba no debe usar claves reales.

Ejecutar `node pruebas/verificar_vectores.mjs`; opcionalmente `python pruebas/verificar_esquema.py` (requiere `jsonschema`). Una firma válida no significa que el comando esté autorizado: validar pareja, rol, sesión, boot, nonce y secuencia donde corresponda.

**Cierre:** pruebas que realmente pasan y evidencia en `ESTADO.md`. Comparar la huella CP/1 con el otro repositorio **antes** de integrar, no durante este desarrollo independiente.

## Regla de separación

Las dependencias de PLAN_PADRE / PLAN_HIJO que mencionan tareas del otro rol se tratan como **pendientes de integración**, no como bloqueo para programar y probar localmente con dobles de prueba. No se debe inventar el estado del otro dispositivo.
