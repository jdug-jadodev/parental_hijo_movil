# Plan de implementación — hijo

Estado de todas las tareas: **PENDIENTE**. Ninguna aplicación ha sido compilada o probada por este paquete. Cada tarea termina con código, tests y actualización de `ESTADO.md`. Leer únicamente la sección indicada y las dependencias necesarias; no enviar todo el documento completo a una IA pequeña.

## Índice de ejecución

| Tarea | Entregable | Depende de |
|---|---|---|
| H00 | Inventario y plan de laboratorio | C00 |
| H01 | DPC y launcher mínimos | H00 |
| H02 | Ruta de emergencia del sistema | H01 |
| H03 | Motor puro de políticas | C01 |
| H04 | Estado atómico y contexto de arranque | H01, H03 |
| H05 | Recuperación local antes de endurecer | C02, H04 |
| H06 | Enforcement de quiosco y aplicaciones permitidas | H02, H04, H05 |
| H07 | Servicio visible y detección de red | H04, H06 |
| H08 | Identidad y vinculación QR del hijo | C02, H05; P02 para integración |
| H09 | Procesador de comandos seguro | H03, H04, H06, H08 |
| H10 | Conexión WSS y estado firmado | H07, H08, H09, R03 |
| H11 | Mantenimiento Wi-Fi, actualización y retirada | H05, H06, H08; P07 para token local |
| H12 | Pruebas adversas y release | H02–H11, P06, P07, R03; coordinar evidencia con P08 sin depender de su cierre |

## H00 — Inventario y plan de laboratorio

**Depende de:** C00.

**Leer:** Fuentes/compatibilidad; INSTALACION_A13 §1–3.

**Archivos/componentes previstos:** FICHA_DISPOSITIVO.md, BUILD_ENV.md.

**Implementar:** Registrar SM, Android/API, One UI, parche y disponibilidad de equipo de prueba. Confirmar copia/propiedad y rutas de recuperación.

**Criterio de aceptación:** No se escribe que el teléfono usa Android14 sin comprobarlo. No se hace ningún borrado automático.

**Comprobación:** Inventario completo o campos explícitos NO COMPROBADO.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H01 — DPC y launcher mínimos

**Depende de:** H00.

**Leer:** SPEC-H H2/H4; INSTALACION_A13 §3.

**Archivos/componentes previstos:** ChildAdminReceiver, LauncherActivity, manifest y device_admin.xml.

**Implementar:** Crear componentes sin endurecimiento definitivo, pantalla bloqueada por defecto y diagnóstico isDeviceOwner. Aprovisionar en laboratorio con ADB.

**Criterio de aceptación:** Se demuestra propietario real; abrir como app normal muestra NO CONFIGURADO, no simula control.

**Comprobación:** H-OWNER-01 y arranque de APK laboratorio.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H02 — Ruta de emergencia del sistema

**Depende de:** H01.

**Leer:** SPEC-H H9; ADR-H-004.

**Archivos/componentes previstos:** EmergencyAccess, LockedScreen, prueba de keyguard.

**Implementar:** Conservar keyguard y probar transición explícita a pantalla de bloqueo. Acceder al marcador del sistema sin clave parental. Revisar PIN/SIM y vuelta al launcher.

**Criterio de aceptación:** Se documenta la ruta exacta del A13 sin usar APIs privadas ni hacer llamadas de prueba injustificadas.

**Comprobación:** H-EMG-01/02; primera comprobación G0, todavía sin endurecimiento total.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H03 — Motor puro de políticas

**Depende de:** C01.

**Leer:** CP/1 §2/3; SPEC-H H6.

**Archivos/componentes previstos:** PolicyEngine, modelos de evaluación, tests de tabla.

**Implementar:** Implementar estados, prioridad y cuatro acciones con un reloj inyectable. No importar Android ni usar fecha del sistema en decisiones.

**Criterio de aceptación:** LOCK_FOR vencido no permite sin canal; reboot siempre exige nueva autorización; padre offline no invalida política.

**Comprobación:** H-TIME-01/02/03 y casos de máquina de estados.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H04 — Estado atómico y contexto de arranque

**Depende de:** H01, H03.

**Leer:** SPEC-H H8; CP/1 §10.

**Archivos/componentes previstos:** ChildStateStore, BootReceiver, BootContextRepository.

**Implementar:** Persistir última política/seq/id/hash/fase. Leer contexto Device Protected. Distinguir callbacks de un mismo arranque de uno nuevo. Reanudar sin reiniciar duración.

**Criterio de aceptación:** Archivo corrupto bloquea; proceso muerto conserva tiempos; antes de desbloqueo no se exige Keystore para conservar restricciones.

**Comprobación:** H-STATE-01/02, H-BOOT-01/02/03.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H05 — Recuperación local antes de endurecer

**Depende de:** C02, H04.

**Leer:** CP/1 §9; SPEC-H H10/H11.

**Archivos/componentes previstos:** RecoveryController, pantalla de código, sesión de mantenimiento.

**Implementar:** Implementar hashes por propósito, consumo atómico, límites de intentos y desafío OFFLINE firmado. Empezar con identidades inyectadas solo en tests/debug.

**Criterio de aceptación:** Reinicio no devuelve un código consumido. Código de mantenimiento no permite apps ni borrar. Emergencia nunca se bloquea por intentos.

**Comprobación:** H-REC-01/02/03; regenerar vínculo/códigos de producción después de pruebas.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H06 — Enforcement de quiosco y aplicaciones permitidas

**Depende de:** H02, H04, H05.

**Leer:** SPEC-H H5; ADR-H-001/004.

**Archivos/componentes previstos:** PolicyEnforcer, AllowedAppsScreen, diagnóstico de políticas.

**Implementar:** Aplicar HOME/lista/LockTask y flags mínimos; aprobar apps presencialmente. Reducir lista al bloquear, conservar emergencias y verificar resultados. Endurecer gradualmente.

**Criterio de aceptación:** No queda pantalla fijada ni selector HOME libre. No se abre todo Ajustes para resolver un fallo. La ruta de emergencia sigue operativa.

**Comprobación:** H-ENF-01/02/03, H-SEC-01/02/03, H-EMG-03/04; cerrar G0 solo aquí.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H07 — Servicio visible y detección de red

**Depende de:** H04, H06.

**Leer:** SPEC-H H7; CP/1 §2.3/8.

**Archivos/componentes previstos:** ControlService, NetworkObserver, prueba de callbacks.

**Implementar:** Crear foreground service elegible por versión. Registrar una sola red predeterminada, evaluar capacidades y pérdidas con reloj monotónico. Añadir backoff, sin bucles ocupados.

**Criterio de aceptación:** Un Wi-Fi sin internet no permite uso. Cambio Wi-Fi/datos breve no duplica servicios ni sockets.

**Comprobación:** H-NET-01/02/04; no simular aplicación de políticas en esta prueba.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H08 — Identidad y vinculación QR del hijo

**Depende de:** C02, H05; P02 para integración.

**Leer:** CP/1 §4/5; SPEC-H H8/H10.

**Archivos/componentes previstos:** ChildKeyStore, PairingController, PairingScreen.

**Implementar:** Generar identidad no exportable, oferta con desafío visible, validar aceptación física y firmada. Guardar claves públicas/hashes y bloquear nueva vinculación libre.

**Criterio de aceptación:** La app vinculada rechaza otro padre salvo recuperación autorizada. No se exporta la privada para soportar Direct Boot.

**Comprobación:** C-PAIR-01, H-KEY-01 y pruebas de reinicio de oferta.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H09 — Procesador de comandos seguro

**Depende de:** H03, H04, H06, H08.

**Leer:** CP/1 §7; SPEC-H H5/H8.

**Archivos/componentes previstos:** CommandProcessor, generador ACK, tests secuenciales.

**Implementar:** Validar contexto/nonce/seq, aceptar atómicamente, aplicar con único escritor y confirmar resultado real. Repetición exacta no reinicia duración.

**Criterio de aceptación:** Mismo seq con otro payload se rechaza. Un fallo de política no genera APPLIED. No ampliar permisos si falla persistencia.

**Comprobación:** C-SEQ-01/02/03/04, H-STATE-01, H-ENF-03.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H10 — Conexión WSS y estado firmado

**Depende de:** H07, H08, H09, R03.

**Leer:** CP/1 §6–8; SPEC-H H7.

**Archivos/componentes previstos:** ChildRelayConnection, STATE/SYNC, heartbeat tests.

**Implementar:** Autenticar identidad contra relay, rotar childSessionId en cada conexión, emitir estado con syncNonce/desafío y usar PONG correlacionado.

**Criterio de aceptación:** 45s sin canal bloquean aunque siga habiendo internet; volver a conectar no acepta comandos de la sesión vieja.

**Comprobación:** H-NET-03/04, P-ACK-01, G2/G3 con evidencia.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H11 — Mantenimiento Wi-Fi, actualización y retirada

**Depende de:** H05, H06, H08; P07 para token local.

**Leer:** SPEC-H H10/H11.

**Archivos/componentes previstos:** MaintenanceScreen, WifiConfigurator, LocalUpdater, RetireDevice.

**Implementar:** Configurar red desde UI propia con APIs permitidas para DPC. Verificar firma/versión de actualización. Implementar retirada local con doble confirmación y selección correcta de API.

**Criterio de aceptación:** Mantenimiento no abre apps normales; actualización preserva identidad y secuencia; retiro no se ejecuta por una orden remota.

**Comprobación:** H-REC-04, H-UPDATE-01/02, H-RET-01 en dispositivo de prueba autorizado.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## H12 — Pruebas adversas y release

**Depende de:** H02–H11, P06, P07, R03; coordinar evidencia con P08 sin depender de su cierre.

**Leer:** MATRIZ_ACEPTACION y FICHA_DISPOSITIVO.

**Archivos/componentes previstos:** Evidencias G0–G4, manifest release, APK firmado.

**Implementar:** Probar proceso muerto con app permitida delante, suspensión, apagado, cortes, cambio de reloj, intentos de desinstalar y rutas de escapes. Medir consumo en reposo natural.

**Criterio de aceptación:** Solo marcar compatible al cumplir requisitos en SM/firmware real. Retirar testOnly/debug/ADB y repetir pruebas críticas con release.

**Comprobación:** Toda la matriz H/C aplicable; fallos explícitos no se convierten en aprobaciones.

**Cierre de tarea:** registrar archivos cambiados, comando/test realmente ejecutado, resultado observado y bloqueos restantes. Si una API no existe o exige privilegios no disponibles, detener esa vía y documentar el problema; no reemplazarla por reflexión o una falsa confirmación.

## Coordinación entre roles

H08/P02 y H11/P07 pueden implementarse con fakes locales, sin esperar que el otro módulo termine primero; las pruebas cruzadas se cierran en P08/H12. Estas dos tareas finales comparten evidencia, pero no son dependencias circulares. Nunca reemplazar la validación integrada por fakes para declarar el dispositivo compatible.
