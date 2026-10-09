# ADR-H-006 — Portabilidad por capacidades, no promesa universal

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

## Contexto

A13 no identifica versión exacta. Android y fabricantes tienen diferencias de energía, aprovisionamiento y telefonía.

## Decisión

Primero certificar modelo/firmware real; probar APIs y capacidades. Base hijo API31+, sin APIs privadas ni paquetes Samsung fijados como solución universal.

## Consecuencias y alternativas descartadas

Otras marcas, versiones y variantes requieren la misma matriz. iPhone hijo queda fuera. Cambiar una dependencia, target o firmware requiere volver a ejecutar pruebas críticas.

## Validación y revisión

G0–G4 y FICHA_DISPOSITIVO. Fuentes [S01, S05, S06, S14, S24].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.
