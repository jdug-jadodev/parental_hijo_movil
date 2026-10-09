# ADR-H-004 — Emergencia mediante el sistema antes que endurecimiento

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

## Contexto

El producto debe mantener una ruta de emergencia real, incluso sin internet y tras arrancar. Un botón sin destino comprobado no cumple.

## Decisión

Conservar keyguard y telefonía. Primer prototipo usa la ruta de emergencia del keyguard; navegación explícita a pantalla bloqueada mediante API pública. Probar físicamente antes de cerrar otras salidas.

## Consecuencias y alternativas descartadas

No API privada de marcador ni allowlist indiscriminada de Ajustes. No suprimir llamadas o devoluciones de seguridad. Las pruebas reales de llamada requieren coordinación; no llamar a emergencias para probar.

## Validación y revisión

G0, H-EMG-01/02/03/04. Fuentes [S02–S04, S15, S16].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.
