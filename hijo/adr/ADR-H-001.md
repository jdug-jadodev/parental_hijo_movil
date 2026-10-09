# ADR-H-001 — Device Owner y quiosco persistente

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

## Contexto

El niño puede cerrar o desactivar controles convencionales. Una pantalla que cubre otras apps no establece autoridad de administración.

## Decisión

Usar APIs oficiales Device Owner, HOME persistente y Lock Task real. Mantener el quiosco también en el modo permitido, cambiando solo la lista aprobada.

## Consecuencias y alternativas descartadas

Requiere preparación del dispositivo y compatibilidad por modelo. No root, accesibilidad, pantalla fijada ni ocultación. No se promete impedir recovery o reinstalación del sistema.

## Validación y revisión

H-F01/02/09, H-SEC-02, G0. Fuentes [S01–S04, S17].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.
