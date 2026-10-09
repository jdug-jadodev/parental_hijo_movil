# Control parental — proyecto Hijo Android (Samsung A13 primero)

**Especificaciones v1.0 y base de desarrollo independiente. Consultar `ESTADO.md` para conocer lo realmente compilado y probado. Todavía no hay control parental funcional.**

## Regla obligatoria de dispositivos

**El hijo es el Samsung A13 SM-A135M. El A56 NO SE TOCA:** no consultar sus
datos, instalar aplicaciones, ejecutar pruebas, aprovisionar ni cambiar ajustes.
Cada operación ADB sobre el teléfono debe apuntar explícitamente al A13 mediante
`adb -s`, después de identificarlo sin publicar su número de serie. Si hay varios
candidatos o no puede comprobarse el destino, detenerse. Nunca ejecutar pruebas
conectadas indiscriminadas con ambos teléfonos presentes.

## Qué se construye aquí

- App Android Kotlin Device Owner + modo quiosco: bloqueo local, reinicio, desconexión, tiempos y emergencias.
- Pruebas de Samsung Galaxy A13 y registro explícito de versión/firmware.
- Integración inicial con un *simulador* de mensajes padre/relay, sin necesitar el repo real del padre.

Este repositorio **no contiene el código del otro dispositivo**. El protocolo compartido CP/1 tiene la misma copia en los dos paquetes. Mantenerlo sin editar hasta la integración.

**Servidor externo:** el backend WebSocket y su despliegue en Render se desarrollan exclusivamente en `C:\Users\Usuario\Documents\parental_render`. Aquí solo corresponde la app del niño, incluido su cliente de conexión al servidor. Las referencias a Render en CP/1 son documentación de compatibilidad; no autorizan desarrollar el servidor aquí.

## Orden de trabajo para una IA pequeña o un desarrollador principiante

1. Usar este repositorio Git independiente; el proyecto Android está en `android/`.
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
- Los ejemplos y scripts **solo validan el contrato documental**. La base Android de C00 no aplica políticas; no hay relay real en este repositorio.

## Comprobación de documentación

`node pruebas/verificar_vectores.mjs`

Opcional con Python + jsonschema: `python pruebas/verificar_esquema.py`.

**Inicio y continuación:** consultar `ESTADO.md`; C00 prepara el proyecto y C01 empieza el protocolo. No implementar todas las funciones simultáneamente.
