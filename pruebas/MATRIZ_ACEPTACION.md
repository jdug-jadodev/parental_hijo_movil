# Matriz de aceptación — CP/1

Todos los casos del producto se entregan **PENDIENTES**. Completar evidencia con versión, entorno, resultado observado y archivo/commit. La verificación de fixtures adjunta es documental/criptográfica; no aprueba estos casos de la aplicación.

## Puertas

G0 = administración/quiosco y emergencia en A13. G1 = identidad, protocolo y recuperación. G2 = persistencia, tiempo, arranque y red. G3 = integración padre/relay/hijo. G4 = proceso muerto, energía, escapes y actualización en release. No omitir un caso crítico porque el emulador no lo soporte. Una llamada real de emergencia no se realiza sin coordinación formal.

## Contrato y seguridad criptográfica

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| C-CRYPTO-01 | **Firma válida y verificación cruzada.** Firmar/verificar fixtures P-256 DER en JVM, Android y Node. | Se aceptan bytes originales y se rechazan cambios/clave/propósito incorrectos. | Automática | PENDIENTE |
| C-SCHEMA-01 | **Parser estricto.** Probar campos extra, UTF-8/base64 inválidos, duplicados, números y tamaños fuera de límites. | Todos se rechazan antes de aplicar políticas. | Automática | PENDIENTE |
| C-PAIR-01 | **Vinculación física.** QR vencido, clave del hijo diferente, otra pareja, cancelación y segunda vinculación sin permiso. | No cambian claves; solo oferta física vigente y confirmada crea vínculo. | Automática + A13 | PENDIENTE |
| C-SEQ-01 | **Repetición exacta.** Reenviar último comando en la misma sesión tras perder ACK. | Devuelve resultado, conserva inicio y duración; no vuelve a liberar. | Automática | PENDIENTE |
| C-SEQ-02 | **Secuencia conflictiva.** Mismo seq con otro payload/id; después un seq menor. | SEQUENCE_CONFLICT o STALE_SEQUENCE, sin cambiar estado aceptado. | Automática | PENDIENTE |
| C-SEQ-03 | **Orden concurrente.** Procesar seq7 y seq8 con demoras de enforcement. | No termina aplicando seq7 después de seq8. Un solo escritor. | Automática | PENDIENTE |
| C-SEQ-04 | **Sesión/arranque/desafío viejos.** Entregar ALLOW de sesión anterior, otro boot o nonce vencido. | Rechazo; pedir estado actual y nueva confirmación al padre. | Automática + integración | PENDIENTE |

## Hijo: administración y emergencia

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| H-OWNER-01 | **Sin privilegio de propietario.** Instalar como app normal y comparar con DPC aprovisionado. | Primera situación aparece NO CONFIGURADO; segunda verifica Device Owner. | A13 | PENDIENTE |
| H-ENF-01 | **Quiosco real.** Pulsar Home, Recientes, Atrás y gestos desde bloqueado. | Sigue bajo launcher propio y LOCK_TASK_MODE_LOCKED, no PINNED. | A13 | PENDIENTE |
| H-ENF-02 | **Cambio permitido/bloqueado.** Abrir una app aprobada y recibir LOCK. | Deja de ser utilizable; tiempo local recepción-aplicación medido, objetivo ≤1s activo. | A13 | PENDIENTE |
| H-ENF-03 | **Política no aplicada.** Inyectar error de API/resultado y recuperar. | No ACK APPLIED hasta evidencia; estado seguro y error visible. | Automática + A13 | PENDIENTE |
| H-SEC-01 | **Elevación de rol.** Buscar cambio a padre en UI, intents o actividad exportada. | No existe vía operativa para elevar rol. | A13 + revisión | PENDIENTE |
| H-SEC-02 | **Controles normales sobre app.** Intentar desinstalar, detener, borrar datos, modo seguro y reset desde Ajustes. | Restricciones impiden la vía normal; no se afirma protección contra recovery. | A13 release | PENDIENTE |
| H-SEC-03 | **Salidas laterales.** Probar notificaciones, PiP, compartir, enlaces, instalador, otro launcher y usuarios. | No abren apps/componentes no autorizados ni ajustes libres. | A13 release | PENDIENTE |
| H-EMG-01 | **Keyguard y arranque.** Arranque antes del PIN Android, con/sin PIN SIM. Abrir marcador sin llamar. | Ruta de emergencia reconocible sin clave parental; documentar cada variante. | A13 físico | PARCIAL: ruta básica y arranque sin SIM confirmados por propietario; con SIM/PIN SIM pendiente. Ver EVIDENCIA_H02.md. |
| H-EMG-02 | **Sin internet.** Desconectar datos/Wi-Fi con radio telefónica disponible; abrir marcador sin llamar. | Emergencia no depende del relay ni de credenciales parentales. | A13 físico | APROBADO en H02 sin quiosco: confirmación presencial del propietario; ver EVIDENCIA_H02.md. Repetir bajo endurecimiento. |
| H-EMG-03 | **Quiosco estricto y regreso.** Abrir marcador desde bloqueo endurecido; salir sin llamar. | Acceso funciona y vuelve a estado restringido sin escape a apps. | A13 físico | PENDIENTE |
| H-EMG-04 | **Llamada en curso y cambios.** Simulación autorizada de llamada de emergencia y devolución; cambiar política/red. | No se finaliza llamada ni ocultan controles críticos. No llamadas reales no coordinadas. | Entorno telefónico de pruebas autorizado | PENDIENTE |

## Hijo: estado, red y ejecución

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| H-BOOT-01 | **Reinicio desde permitido.** ALLOW, reiniciar y esperar que vuelva internet. | Permanece bloqueado hasta orden fresca para nuevo bootId. | A13 | PENDIENTE |
| H-BOOT-02 | **Dos broadcasts de un arranque.** Procesar LOCKED_BOOT_COMPLETED y BOOT_COMPLETED del mismo boot. | Un solo bootId, sin reset de seq ni acceso prematuro. | Automática + A13 | PENDIENTE |
| H-BOOT-03 | **Direct Boot.** Reiniciar sin introducir credencial; observar servicio/pantalla. | No lee Credential Encrypted ni exporta privadas; bloqueo y emergencia se conservan. | A13 | PENDIENTE |
| H-TIME-01 | **Cuatro políticas.** Recorrer LOCK, LOCK_FOR, ALLOW y ALLOW_FOR con reloj inyectado. | Prioridades/expiraciones exactas del contrato; política nueva sustituye anterior. | Automática | PENDIENTE |
| H-TIME-02 | **Cambio de hora y zona.** Cambiar reloj ±24h en laboratorio y repetir en release restringido. | No adelanta autorización ni evita vencimiento; pruebas no dependen de fecha. | Automática + A13 | PENDIENTE |
| H-TIME-03 | **Expiración sin red.** Vence LOCK_FOR con canal vencido; luego volver a conectar. | Sigue bloqueado offline; reevalúa al recuperar si no hay otro bloqueo. | Automática + A13 | PENDIENTE |
| H-NET-01 | **Red perdida.** Quitar Wi-Fi/datos desde un escenario autorizado de laboratorio. | Bloqueo local tras 3s, sin orden remota. Medir tiempo real. | A13 | PENDIENTE |
| H-NET-02 | **Handover de red.** Cambiar Wi-Fi/datos con un corte inferior al margen y después superior. | Evita falso positivo breve y bloquea si supera margen; un socket/callback. | A13 | PENDIENTE |
| H-NET-03 | **Solo falla Render.** Mantener internet pero cortar acceso al relay o sus PONG. | Bloqueo al superar 45s desde último latido válido. | Integración + A13 | PENDIENTE |
| H-NET-04 | **Portal, DNS, TLS y latido falso.** Wi-Fi sin salida, portal, TLS inválido, PONG de nonce viejo. | Ninguno autoriza canal; no deshabilita validación TLS. | Automática + A13 | PENDIENTE |
| H-STATE-01 | **Crash entre aceptar/aplicar.** Matar proceso en cada fase del último comando. | Estado se reconcilia; no duplica duración ni concede permiso por error. | Automática + Android | PENDIENTE |
| H-STATE-02 | **Archivo corrupto/truncado.** Corromper copia de laboratorio antes de cargar estado. | ERROR_SEGURO; no reabre enrolamiento ni pone ALLOWED. | Automática + Android | PENDIENTE |
| H-KEY-01 | **Keystore indisponible.** Simular error y probar antes de primer desbloqueo. | No autentica ni inventa ACK; conserva bloqueo y emergencia. | Android | PENDIENTE |
| H-PROC-01 | **Proceso muerto con otra app abierta.** Mientras se usa app aprobada, matar/simular muerte del controlador en laboratorio. | No hay ventana de uso que viole el requisito; si existe, falla la certificación hasta corregir. | A13 físico | PENDIENTE |
| H-POWER-01 | **Doze y despertar.** Doze forzado y reposo natural; expirar tiempo/red mientras duerme. | Al despertar no se habilita interacción fuera de política; observar otras apps, no solo launcher. | A13 físico | PENDIENTE |
| H-POWER-02 | **Consumo en reposo.** Comparar dos periodos equivalentes de 24h, con misma señal/apps/carga inicial. | Registrar energía, CPU, bytes, reconexiones; objetivo inicial delta ≤5 puntos de batería, ajustar con evidencia sin debilitar bloqueo. | A13 físico | PENDIENTE |

## Hijo: recuperación, actualización y retirada

| ID | Caso y preparación | Resultado esperado | Entorno | Estado |
|---|---|---|---|---|
| H-REC-01 | **Recuperación offline.** Sin red, usar código/token MAINTENANCE. | Solo red dentro de UI propia por 5min; no habilita apps normales. | A13 | PENDIENTE |
| H-REC-02 | **Replay de recuperación.** Repetir token, usarlo para otro propósito o reiniciar. | Rechaza; códigos consumidos no vuelven y tokens efímeros expiran. | Automática + A13 | PENDIENTE |
| H-REC-03 | **Adivinación y reinicio.** Fallar códigos 5/10 veces y reiniciar durante espera. | Mantiene/reimpone espera; emergencia sigue accesible. | Automática + A13 | PENDIENTE |
| H-REC-04 | **Padre perdido.** Usar RECOVER_PARENT, revincular y actualizar env pública. | Clave vieja ya no autoriza; nueva funciona después de configuración, siempre con bloqueo durante transición. | A13 + Render | PENDIENTE |
| H-UPDATE-01 | **Actualización firmada.** Actualizar APK propio durante mantenimiento. | Conserva vínculo, claves, seq y temporizador; no requiere ADB permanente. | A13 release | PENDIENTE |
| H-UPDATE-02 | **Firma/migración incorrecta.** APK de otra firma o migración incompatible de estado. | No instala/acepta como legítimo; no pierde restricciones ni permite por defecto. | Automática + A13 | PENDIENTE |
| H-RET-01 | **Retirada local.** Código/token RETIRE, cancelar una vez; luego doble confirmación en equipo de prueba. | Cancelar no borra. Confirmar usa API correcta; jamás se ejecuta desde SET_POLICY remoto. | Destructiva, solo equipo autorizado | PENDIENTE |
