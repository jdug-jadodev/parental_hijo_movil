# ADR-H-003 — Servicio visible y eficiencia sin vigilancia

Estado: aceptado para el diseño v1; implementación por validar. Fecha: 2026-10-08.

## Contexto

Se necesitan eventos de red y WSS persistente en el hijo, sin capturar datos personales ni agotar batería con un bucle permanente.

## Decisión

Servicio en primer plano visible y elegible como `systemExempted` cuando corresponda; NetworkCallback, heartbeat y reconexión con backoff. Sin FCM, WorkManager como reloj de segundos ni wake lock permanente.

## Consecuencias y alternativas descartadas

La etiqueta del servicio no garantiza inmunidad a suspensión o al fabricante. Doze, muerte de proceso y batería son puertas de compatibilidad. No usar tipos de servicio falsos para evitar restricciones.

## Validación y revisión

H-NET-04, H-PROC-01, H-POWER-01/02. Fuentes [S05, S06, S08, S09].

Fuentes: `compartido/02_FUENTES_Y_COMPATIBILIDAD.md`. Cualquier cambio debe actualizar las especificaciones y la matriz correspondiente.
