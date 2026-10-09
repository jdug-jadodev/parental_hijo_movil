# Núcleo CP/1 — validación estructural

Este módulo es Kotlin/JVM, sin dependencias Android. Implementa C01.

## Flujo de lectura

1. `JsonCp1` comprueba tamaño, UTF-8 estricto, ASCII, tipos JSON y duplicados.
2. `Base64Cp1` rechaza padding, caracteres extra, bits sobrantes y tamaños excesivos.
3. `CodecCp1` comprueba campos obligatorios, tipos, valores y campos desconocidos.
4. Los DTO de `ModelosCp1` describen el contenido; **no acreditan autorización**.

`leerFrame` solo admite frames de transporte o sobres firmados, no órdenes desnudas.
`leerDocumentoSinAutorizar` sirve para inspección estructural de QR y fixtures.
`leerPayloadSinVerificar` exige bytes canónicos y propósito asociado al tipo de mensaje.
`serializar` genera los mismos bytes deterministas del contrato y valida también los DTO locales.

## Límite de seguridad de C01

`SobreSinVerificar` significa literalmente que **la firma aún no ha sido verificada**.
Una firma que tiene base64 válido podría ser falsa, de otra clave o de otro propósito.
Las claves públicas solo tienen comprobación base64/tamaño; SPKI DER y curva P-256 se comprobarán en C02.

En C02, la verificación debe usar los bytes originales del payload y el prefijo CP/1.
Solo después se valida el payload canónico; nunca se verifica un JSON reconstruido.
H09 comprobará además vínculo, rol, arranque, sesión, desafío y secuencia.
Este núcleo no llama a APIs Android ni aplica políticas o fabrica ACK.

## Pruebas

Desde `android/`: `.\gradlew.bat :core-protocol:test`.

- 28 ejemplos congelados, cada uno con lectura/serialización, rechazo de campo extra y omisión de todos sus campos.
- Contraejemplos de secuencia, duración, estado, rol, UUID, nonce, propósito, HTTPS y hashes.
- JSON ambiguo, UTF-8 inválido, controles, números inseguros, profundidad y límites en bytes.

La prueba de fixtures usa los recursos de testFixtures sin modificarlos.
El simulador y las clases de pruebas no forman parte de la variante de producción.
