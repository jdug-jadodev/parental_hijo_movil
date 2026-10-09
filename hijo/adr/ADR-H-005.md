# ADR-H-005 — Estado atómico, secuencia y reloj monotónico

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

## Contexto

Cierres, ACK perdidos y cambios de hora pueden hacer repetir una orden o reiniciar un temporizador.

## Decisión

Un solo escritor y archivo AtomicFile para política, contador, hash/id y fase de aplicación. Medir duración con elapsedRealtime en el mismo arranque; vincular nuevas órdenes a sesión, boot y desafío.

## Consecuencias y alternativas descartadas

No separar contador y política en escrituras independientes; no comparar firmas DER como identificadores; no restablecer contadores por reinicio. Datos corruptos mantienen bloqueo y requieren recuperación.

## Validación y revisión

C-SEQ-01/02/03/04, H-STATE-01/02, H-TIME-02. Fuentes [S07, S20, S21, S25].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.
