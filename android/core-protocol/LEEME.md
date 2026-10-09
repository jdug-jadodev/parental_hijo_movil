# Núcleo CP/1 — estructura y criptografía

Este módulo es Kotlin/JVM, sin dependencias Android. Implementa C01 y la parte pura de C02.

## Flujo de lectura

1. `JsonCp1` comprueba tamaño, UTF-8 estricto, ASCII, tipos JSON y duplicados.
2. `Base64Cp1` rechaza padding, caracteres extra, bits sobrantes y tamaños excesivos.
3. `CodecCp1` comprueba campos obligatorios, tipos, valores y campos desconocidos.
4. Los DTO de `ModelosCp1` describen el contenido; **no acreditan autorización**.

`leerFrame` solo admite frames de transporte o sobres firmados, no órdenes desnudas.
`leerDocumentoSinAutorizar` sirve para inspección estructural de QR y fixtures.
`leerPayloadSinVerificar` exige bytes canónicos y propósito asociado al tipo de mensaje.
`serializar` genera los mismos bytes deterministas del contrato y valida también los DTO locales.

## Flujo criptográfico de C02

1. Leer y validar el sobre externo, sin interpretar aún el JSON del payload.
2. Recibir la clave pública confiable, propósito y rol esperados desde el contexto local.
3. Comprobar SPKI DER, curva P-256 y contenedor DER de firma.
4. Verificar SHA256withECDSA sobre `CPv1/<purpose>\n` más los bytes originales.
5. Validar payload canónico y comprobar propósito, tipo y rol.
6. Devolver `PayloadVerificado` con copias defensivas y SHA-256 del payload.

No se elige algoritmo ni clave confiable desde un comando. En PAIR_ACCEPT, la pública
del padre tiene que corresponder al firmante y ambas públicas deben ser P-256.
H08 comprobará además la oferta del hijo y la confirmación física del vínculo.

`FirmanteCp1` es el puerto de firma. El adaptador Android usa una identidad ya existente
en Android Keystore, sin exportar la privada ni crear otra identidad cuando falla.
Las privadas efímeras de JVM solo existen en tests y no se guardan en archivos.

## Límite de seguridad

`SobreSinVerificar` significa literalmente que **la firma aún no ha sido verificada**.
Una firma que tiene base64 válido podría ser falsa, de otra clave o de otro propósito.
El codec aislado solo comprueba base64/tamaño de públicas; `ClavePublicaCp1` realiza
la comprobación criptográfica SPKI DER y curva P-256.

La verificación C02 usa los bytes originales y el prefijo CP/1.
Solo después valida el payload canónico; nunca verifica un JSON reconstruido.
H09 comprobará además vínculo, rol, arranque, sesión, desafío y secuencia.
Este núcleo no llama a APIs Android ni aplica políticas o fabrica ACK.

## Pruebas

Desde `android/`: `.\gradlew.bat :core-protocol:test`.

- 28 ejemplos congelados, cada uno con lectura/serialización, rechazo de campo extra y omisión de todos sus campos.
- Contraejemplos de secuencia, duración, estado, rol, UUID, nonce, propósito, HTTPS y hashes.
- JSON ambiguo, UTF-8 inválido, controles, números inseguros, profundidad y límites en bytes.
- Diez vectores firmados, claves/payloads/firmas/propósitos/roles alterados y DER inválido.
- Firma nueva con claves efímeras y verificación cruzada JVM a Node.

La prueba de fixtures usa los recursos de testFixtures sin modificarlos.
El simulador y las clases de pruebas no forman parte de la variante de producción.
`kotlinx-serialization-json` solo ayuda a leer el documento descriptivo de vectores en tests;
el parser de producción sigue siendo el subconjunto estricto CP/1.

Después de ejecutar los tests, desde la raíz:
`node pruebas/verificar_interoperabilidad_jvm.mjs`.
Solo se exportan públicas y sobres ficticios en `core-protocol/build/interoperabilidad/`.
