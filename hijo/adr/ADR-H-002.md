# ADR-H-002 — Decisión de bloqueo local y estados cerrados

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

## Contexto

Un relay sin base de datos no puede ser la memoria ni el ejecutor del bloqueo. Reiniciar o desconectar no debe devolver acceso.

## Decisión

La máquina de estados del hijo decide con datos duraderos. Nuevo arranque exige autorización fresca. Red perdida y canal vencido bloquean independientemente del padre.

## Consecuencias y alternativas descartadas

Una caída de Render puede bloquear. El padre apagado no bloquea por sí mismo. Reanudar red no elimina una orden de bloqueo o una autorización de arranque faltante. Las limitaciones de ejecución del sistema se verifican, no se ocultan.

## Validación y revisión

H-BOOT-01, H-NET-01/03, H-TIME-01/02, H-PROC-01. Fuentes [S07–S09, S20].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.
