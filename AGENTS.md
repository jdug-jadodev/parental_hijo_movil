# Instrucciones de OpenCode — proyecto HIJO

**Proyecto autónomo.** Solo implementar aplicación del hijo Android, Device Owner y modo quiosco. No crear ni modificar el repositorio del otro rol.

1. Leer `00_EMPIEZA_AQUI.md`, `ESTADO.md` y `hijo/AGENTS.md`.
2. Elegir una sola tarea de `hijo/PLAN_HIJO.md` o C00-C02 en `compartido/03_INICIO_TECNICO.md`.
3. Consultar solamente las secciones precisas de las SPEC/ADR y del contrato `compartido/00_CONTRATO_V1.md` indicadas para la tarea.
4. Implementar código y pruebas; ejecutar lo que permita el entorno; no afirmar pruebas que no se hicieron.
5. Actualizar `ESTADO.md`, registrar rutas modificadas y próxima tarea. Para otra sesión ver `pruebas/HANDOFF_PLANTILLA.md`.

**Regla de integración:** `compartido/` contiene una copia local de CP/1 y fixtures congelados. No cambiar el esquema, tipos de mensajes, algoritmo de firmas o semántica unilateralmente. Proponer el cambio en `INTEGRACION_FUTURA.md` y, antes de sincronizar, acordar versión nueva y actualizar ambos repositorios juntos.

**Regla de seguridad:** no incorporar secretos, claves privadas ni códigos reales en repositorio; emergencia siempre accesible; no deshabilitar recuperación por un intento de endurecimiento; no indicar que un ACK de relay significa bloqueo aplicado.

**Primer paso:** C00 de `compartido/03_INICIO_TECNICO.md`.
