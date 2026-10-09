# Validación documental realizada

Fecha: 8 de octubre de 2026. Alcance: archivos de documentación, esquema y fixtures de este paquete.

## Comprobaciones ejecutadas

- JSON Schema 2020-12 estructuralmente válido.
- 28 archivos JSON de ejemplos aceptados por el esquema; 13 contraejemplos estructurales rechazados.
- 10 vectores ECDSA P-256/SHA-256/DER: 78 comprobaciones de firmas, bytes, propósito, clave incorrecta, modificación y variantes de JSON no canónicas.
- JSON del paquete legible; bloques de código Markdown cerrados; 59 identificadores de pruebas de aceptación sin duplicados.
- No se incluyeron claves privadas en archivos. Los fixtures contienen claves públicas y firmas ficticias, no material de una familia real.

Entorno efectivamente usado: **Node v22.16.0**, Python 3.13.5, jsonschema 4.26.0. El relay del proyecto fija Node 24 LTS: **este entorno no ejecutó Node 24**. Repetir el verificador y todos los tests del relay en ese runtime al implementar; no confundir una prueba en Node 22 con certificación en Node 24.

Salida del verificador:

```text
OK: 10 vectores firmados; 78 comprobaciones documentales. Runtime: v22.16.0.
Esto no prueba Device Owner, Android Keystore, temporizadores, red real, emergencias ni el A13.
OK: esquema válido; 28 ejemplos aceptados y 13 contraejemplos rechazados.
Es validación estructural: no comprueba claves, firmas, nonce fresco ni permisos reales.
```

## Repetir estas comprobaciones

Desde la raíz del paquete:

```powershell
node pruebas/verificar_vectores.mjs
# Opcional: verificación del esquema en Python.
python -m pip install jsonschema
python pruebas/verificar_esquema.py
```

El primer comando no tiene dependencias adicionales a Node. El segundo flujo requiere Python y jsonschema. Los scripts no tocan un teléfono ni hacen conexiones de red; únicamente la instalación de la dependencia requiere acceso a su repositorio.

## No probado y no entregado

No se compiló una app Android ni un relay real; no hay APK. No se ejecutaron Device Owner, Lock Task, Android Keystore, arranque, restricciones, batería, latencia, mantenimiento, actualización ni retirada en el A13. Tampoco se hizo una llamada de emergencia ni una simulación telefónica. No se desplegó ningún servicio en Render.

Todos los **59 casos de aceptación del producto siguen PENDIENTES**. Los resultados anteriores no cambian su estado. La revisión de documentación reduce ambigüedades, pero no sustituye implementar, probar, revisar seguridad y validar físicamente el dispositivo.
