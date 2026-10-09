# Evidencia C02 — firmas P-256

Fecha: 8 de octubre de 2026. Estado: JVM/Node probados; validación Android PENDIENTE.

## Entorno y comandos ejecutados

Windows 11 amd64, JDK 21.0.10 de Android Studio, Gradle 8.13, Kotlin 2.2.21,
AGP 8.11.1, Node 24.21.0. Versiones completas en `BUILD_ENV.md`.

Desde `android/`:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain
```

Resultado: BUILD SUCCESSFUL; 170 tests, cero fallos y cero errores.

Desde la raíz:

```powershell
node pruebas/verificar_vectores.mjs
node pruebas/verificar_interoperabilidad_jvm.mjs
```

Resultados: 78 comprobaciones de los 10 vectores congelados y 70 comprobaciones
de diez sobres nuevos generados por el código JVM y verificados en Node.
Los datos nuevos contienen solo públicas y sobres ficticios en un archivo build/ ignorado.
Las privadas de pruebas se generan en memoria; no se exportan ni se guardan.

## Demostrado

- Verificación de los diez sobres originales con públicas P-256 del fixture.
- Firma mediante SHA256withECDSA de JCA, sobre bytes originales y prefijo de propósito CP/1.
- SPKI DER con round-trip exacto y comprobación de parámetros P-256 completos.
- Contenedor DER de firma: enteros positivos/mínimos, longitudes y rango r/s.
- Rechazo de claves, payloads, firmas, propósitos y roles distintos.
- JSON no canónico sigue rechazado aunque tenga una firma matemáticamente válida.
- Firma inválida del puerto local no se emite como sobre legítimo.
- Firmas S complementarias válidas conservan el hash de payload, sin imponer low-S unilateralmente.
- PAIR_ACCEPT no sustituye la pública del firmante ni permite otra curva en sus públicas.

## Preparado para Android, sin ejecutar

`FirmanteAndroidKeystore` usa una identidad existente por alias, exige primer desbloqueo,
firma con su handle y devuelve KEY_UNAVAILABLE si la identidad no está disponible.
No genera identidades de producción, no importa privadas ni habilita un bypass.

Tres tests instrumentados preparados: verificar fixtures en Android, firmar con Keystore
sin privada exportable y rechazar alias inexistente sin crear identidad de reemplazo.
Solo crean/eliminan claves de test con alias efímeros únicos; no modifican políticas.

Construcción ejecutada desde android/: `:core-protocol:test :app-child:testDebugUnitTest
:app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:lintDebug --console=plain`.
Resultado: BUILD SUCCESSFUL; APK de app y APK instrumentado compilados; lint con cero errores
y diez avisos de versiones más recientes, sin supresiones. La compilación final reutilizó
los tests de núcleo ya aprobados cuando sus entradas no habían cambiado.

`python -B pruebas/verificar_apk_base.py`: APK del hijo sin fixtures ni clases de tests,
incluyendo las clases Android instrumentadas. Inspección ZIP/DEX adicional del APK de tests:
contiene sus diez sobres y CriptografiaAndroidTest, solo dentro de esa variante de pruebas.
**No se ejecutó connectedDebugAndroidTest ni se instaló nada en el A13.**

## Pendiente

- Ejecutar los tres tests en Android desbloqueado y registrar modelo/API/firmware.
- Direct Boot y manejo de identidad indisponible en el flujo completo: H04/H08.
- Vinculación física, pareja/boot/sesión/nonce/secuencia: H08/H09.
- No certifica bloqueo, emergencia, quiosco, energía ni latencia. G0–G4 siguen pendientes.

Esta evidencia es parcial para C-CRYPTO-01: JVM y Node comprobados,
Android y Android Keystore pendientes. No cerrar la aceptación por compilar tests.
