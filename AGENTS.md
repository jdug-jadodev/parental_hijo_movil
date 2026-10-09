# Instrucciones de OpenCode — proyecto HIJO

**Proyecto autónomo.** Solo implementar aplicación del hijo Android, Device Owner y modo quiosco. No crear ni modificar el repositorio del otro rol.

**Separación del servidor acordada con el propietario:** el servidor Render/WebSocket se desarrolla exclusivamente en `C:\Users\Usuario\Documents\parental_render`, no aquí. No crear backend, endpoints de servidor, configuración de despliegue Render ni tareas de implementación del relay en este repositorio. Aquí permanecen el cliente de comunicación de la app del niño, el contrato CP/1 congelado y sus fixtures/pruebas locales. Las referencias al servidor describen una dependencia externa, no trabajo autorizado sobre él. No modificar el repositorio Render desde esta sesión sin una petición explícita.

1. Leer `00_EMPIEZA_AQUI.md`, `ESTADO.md` y `hijo/AGENTS.md`.
2. Elegir una sola tarea de `hijo/PLAN_HIJO.md` o C00-C02 en `compartido/03_INICIO_TECNICO.md`.
3. Consultar solamente las secciones precisas de las SPEC/ADR y del contrato `compartido/00_CONTRATO_V1.md` indicadas para la tarea.
4. Implementar código y pruebas; ejecutar lo que permita el entorno; no afirmar pruebas que no se hicieron.
5. Actualizar `ESTADO.md`, registrar rutas modificadas y próxima tarea. Para otra sesión ver `pruebas/HANDOFF_PLANTILLA.md`.

**Regla de integración:** `compartido/` contiene una copia local de CP/1 y fixtures congelados. No cambiar el esquema, tipos de mensajes, algoritmo de firmas o semántica unilateralmente. Proponer el cambio en `INTEGRACION_FUTURA.md` y, antes de sincronizar, acordar versión nueva y actualizar ambos repositorios juntos.

**Regla de seguridad:** no incorporar secretos, claves privadas ni códigos reales en repositorio; emergencia siempre accesible; no deshabilitar recuperación por un intento de endurecimiento; no indicar que un ACK de relay significa bloqueo aplicado.

**Primer paso:** C00 de `compartido/03_INICIO_TECNICO.md`.

## Acuerdos con el propietario — 8 de octubre de 2026

- Desarrollar y explicar en español: interfaz, documentación, comentarios, pruebas y mensajes de commit. Conservar nombres de APIs, componentes previstos por la SPEC y campos CP/1 cuando sea necesario para compatibilidad.
- Objetivo: control global del tiempo y bloqueo, sin monitorear actividad, historial, ubicación ni contenido.
- El tiempo es una cuenta regresiva de tiempo transcurrido; continúa con la pantalla apagada. Cambiar de aplicación no reinicia ni pausa el contador.
- Permitir aplicaciones aprobadas presencialmente, siempre que no permitan escapar del control. No prometer que cualquier aplicación es segura sin probarla.
- Diseñar para diferentes celulares Android mediante APIs oficiales y comprobación de capacidades; primero validar el Samsung A13. El código SM, Android y firmware concretos siguen NO COMPROBADOS. iPhone está fuera del alcance actual.
- El A13 estará disponible como equipo de laboratorio y se entregará al niño solo después de validar la versión final.
- Avisar al propietario cuando sea necesario conectar y autorizar el dispositivo por ADB. No instalar, aprovisionar, endurecer ni borrar el teléfono sin coordinar la prueba concreta.
- Acuerdo del 9 de octubre de 2026: el hijo es exclusivamente el A13 SM-A135M; NO tocar el A56. Con varios equipos conectados, seleccionar un único A13 por el modelo anunciado por ADB, comprobarlo y dirigir cada comando al mismo destino mediante `adb -s`. No usar pruebas conectadas indiscriminadas ni consultar/instalar/cambiar el A56. No guardar números de serie en el repositorio; ante ambigüedad, detenerse.
- Hacer un commit aproximadamente cada 500 líneas de código propias añadidas o modificadas. Contar Kotlin, configuración y pruebas; excluir documentación, fixtures congelados, archivos generados y Gradle Wrapper. No añadir código de relleno para alcanzar el umbral. Hacer también un commit al cerrar una tarea pequeña aunque no llegue a 500 líneas.
- Antes de cada commit revisar los cambios, ejecutar las pruebas disponibles y registrar pendientes en ESTADO.md. Nunca incluir secretos ni resultados de pruebas no realizadas.
- El propietario pidió avanzar de forma autónoma hasta la primera fase que necesite pruebas físicas y avisar entonces para habilitar ADB. No interpretar esto como permiso para saltar dependencias, omitir pruebas o esperar hasta el final para validar emergencia y recuperación. La primera coordinación física corresponde al inventario H00 y al posterior aprovisionamiento H01.
