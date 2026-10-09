# Evidencia C01 — validación estructural del protocolo

Fecha: 8 de octubre de 2026.
Alcance: núcleo Kotlin/JVM y empaquetado del APK debug. Sin teléfono conectado.

## Versiones

Windows 11 amd64, JDK de Android Studio 21.0.10, Gradle Wrapper 8.13,
AGP 8.11.1 y Kotlin 2.2.21. Consultar `BUILD_ENV.md`.

## Comandos y resultados

Desde `android/`, usando el JDK registrado:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:lintDebug --console=plain
```

Resultado: BUILD SUCCESSFUL. Los informes XML registran 112 tests, cero fallos,
cero errores y cero omitidos: 111 en el núcleo y uno en app-child.
La construcción final reutilizó las pruebas ya ejecutadas correctamente; Gradle las indicó UP-TO-DATE.
Lint: cero errores y siete avisos de versiones más recientes disponibles, sin supresiones.

Desde la raíz:

```powershell
node pruebas/verificar_vectores.mjs
python -B pruebas/verificar_apk_base.py
```

Resultados: 10 vectores y 78 comprobaciones documentales en Node 24.21.0;
APK sin ejemplos congelados ni clases declaradas del simulador/pruebas.

## Cobertura demostrada

- Lectura y serialización de los 28 ejemplos originales, sin cambiar sus bytes canónicos.
- Cada ejemplo rechaza campos extra y omisión de cualquiera de sus campos obligatorios.
- UTF-8 estricto, ASCII imprimible, claves duplicadas incluso con escapes, límites de bytes y profundidad.
- Rechazo de números negativos, exponentes, decimales, ceros iniciales y enteros JSON inseguros.
- Base64url sin padding, sin caracteres extra ni bits sobrantes.
- Secuencias positivas de 64 bits, con cero solo en lastAcceptedSeq según CP/1.
- Duración según acción, relaciones de STATE, UUID v4, roles y enumeraciones.
- Asociación de propósito y payload; rechazo de payload sin sobre en el canal.
- Recuperación: validación de estructura/hash, no ejercicio de códigos reales.

## No demostrado

- C02: verificación ECDSA, DER, SPKI, curva P-256 y rechazo criptográfico de firmas alteradas.
- Autorización por identidad vinculada, sesión, boot y nonce vigente; corresponde a H08/H09.
- Cuenta regresiva, políticas Android, Device Owner, quiosco, emergencia, recuperación física y red real.
- El script Python del esquema no pudo repetirse: instalación de jsonschema 4.26.0 en un venv temporal falló por error de conexión y timeout de 120 segundos. No se alteró el Python global.

Esta evidencia cubre la parte JVM de C-SCHEMA-01. No cierra las puertas G0–G4
ni certifica el A13 o cualquier otro dispositivo.
