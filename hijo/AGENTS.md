# Instrucciones para una IA de contexto reducido — hijo

Tu responsabilidad es **DPC, quiosco y ejecución local del hijo**. El repositorio contiene documentación y la base Android en `android/`; consulta `ESTADO.md` para distinguir lo implementado de lo pendiente. No afirmes que el producto funciona hasta ejecutar pruebas apropiadas.

## Lectura mínima por tarea

1. Lee este archivo y `ESTADO.md` del repositorio.
2. Abre únicamente la tarea elegida en `PLAN_HIJO.md` y las secciones citadas de `SPEC_HIJO.md`.
3. Para protocolos, lee las secciones correspondientes de `compartido/00_CONTRATO_V1.md` y el esquema JSON. Para Android específico, consulta las fuentes primarias de compatibilidad antes de inventar una API.
4. Si cambias arquitectura, consulta el ADR afectado; no cambies decisiones globales para resolver un error local.

## Invariantes

Este repositorio desarrolla exclusivamente el APK del hijo: Device Owner, Lock Task y su cliente WSS. El APK del padre es externo. El servidor relay y su despliegue/configuración Render corresponden exclusivamente a `C:\Users\Usuario\Documents\parental_render`; no implementarlos ni configurarlos aquí. El contrato CP/1 y los fixtures locales se conservan como dependencia compartida congelada, no como código del servidor.

Claves P-256, firmas SHA-256 ECDSA DER y bytes deterministas CP/1. No aceptar claves suministradas por un comando. Un ACK del relay no prueba bloqueo. Duraciones con reloj monotónico, contador persistido, contexto de boot/sesión y desafío. Nunca ampliar permisos ante errores.

Emergencia del sistema siempre accesible. App y servicio visibles, sin espionaje ni accesibilidad/overlays como sustituto de políticas oficiales. Recuperación local antes de endurecer. Retirada presencial con aviso de borrado, nunca remota. A13 candidato no significa A13 certificado ni compatibilidad universal.

## Forma de trabajar

Implementa una tarea pequeña por turno. Propón división antes de modificar muchos módulos. Mantén las funciones puras separadas de Android. Usa interfaces/fakes en tests y jamás una clave maestra, bypass o simulación de APPLIED en release. No ejecutes restablecimientos ni restricciones que corten recuperación en un teléfono diario.

Prueba el cambio. Si no hay SDK/dispositivo/permisos, reporta exactamente qué no pudiste verificar y deja la tarea como pendiente de esa prueba. No marques pruebas manuales como aprobadas por inspección del código.

Entrega: breve resultado, rutas cambiadas, pruebas realmente ejecutadas, pendientes y siguiente tarea. Actualiza `ESTADO.md` sin guardar claves, códigos o información personal. Para continuar usa `pruebas/HANDOFF_PLANTILLA.md`.

## Prompt inicial sugerido

```text
Trabaja solo en la tarea [ID] de hijo/PLAN_HIJO.md.
Lee hijo/AGENTS.md, ESTADO.md y las secciones que esa tarea indica.
No implementes las tareas posteriores. Respeta CP/1 y los ADR aceptados.
Implementa, ejecuta los tests disponibles y actualiza ESTADO.md.
Indica de forma explícita cualquier prueba que requiera el A13 real.
```
