# Control parental — proyecto Hijo Android (Samsung A13 primero)

**Paquete de especificaciones v1.0. Se desarrolla por separado. Aún NO hay código funcional ni APK.**

## Qué se construye aquí

- App Android Kotlin Device Owner + modo quiosco: bloqueo local, reinicio, desconexión, tiempos y emergencias.
- Pruebas de Samsung Galaxy A13 y registro explícito de versión/firmware.
- Integración inicial con un *simulador* de mensajes padre/relay, sin necesitar el repo real del padre.

Este repositorio **no contiene el código del otro dispositivo**. El protocolo compartido CP/1 tiene la misma copia en los dos paquetes. Mantenerlo sin editar hasta la integración.

## Orden de trabajo para una IA pequeña o un desarrollador principiante

1. Copiar estas carpetas como documentación de un nuevo repositorio Git independiente.
2. En OpenCode, abrir **solo este repositorio** y leer `AGENTS.md` y `ESTADO.md`.
3. Ejecutar C00, C01 y C02 de `compartido/03_INICIO_TECNICO.md` **adaptados a este rol**; actualizar `ESTADO.md`.
4. Continuar con **una tarea por turno** de `hijo/PLAN_HIJO.md`; revisar la SPEC pertinente y el ADR indicado.
5. Usar `pruebas/MATRIZ_ACEPTACION.md` solo como criterios de aceptación. Las pruebas de dispositivo real no están aprobadas todavía.
6. Al integrar con el otro repo, seguir `INTEGRACION_FUTURA.md` y verificar ambas huellas del contrato.

## Documentos clave

- `hijo/AGENTS.md`: modo de trabajo detallado del rol.
- `hijo/PLAN_HIJO.md`: tareas y dependencias.
- `hijo/SPEC_HIJO.md`: requisitos técnicos y reglas.
- `hijo/INSTALACION_A13.md`: instalación y validación del Samsung A13.
- `hijo/adr/`: decisiones de arquitectura.
- `compartido/00_CONTRATO_V1.md`: el protocolo común, congelado.
- `compartido/protocolo.schema.json` y `compartido/ejemplos/`: mensajes y formatos de prueba.
- `pruebas/`: verificadores, matriz y evidencias.
- `INTEGRACION_FUTURA.md`: sincronización posterior en OpenCode.
- `HIJO_ADR_Y_SPECS.md`: documento largo de lectura opcional; para IA de poco contexto **NO** cargue todo de una vez.

## Alcance y seguridad

- No hay servidor central de datos personales ni historial. Reglas y claves operativas viven en teléfonos; Render es intermediario de mensajes y presencia.
- Un WebSocket puede desconectarse y no garantiza entrega instantánea ni permanente. El hijo aplica política local, no confía en recibir siempre una orden.
- El acceso de emergencia del sistema debe funcionar independientemente del servicio. No iniciar restricciones irreversibles sin recuperación comprobada.
- Device Owner requiere aprovisionamiento real; A13 es candidato, no compatibilidad universal certificada.
- Los ejemplos y scripts **solo validan el contrato documental**. Ninguna app ni relay se han implementado aún.

## Comprobación de documentación

`node pruebas/verificar_vectores.mjs`

Opcional con Python + jsonschema: `python pruebas/verificar_esquema.py`.

**Primera tarea:** C00. No empezar implementando todas las funciones simultáneamente.
