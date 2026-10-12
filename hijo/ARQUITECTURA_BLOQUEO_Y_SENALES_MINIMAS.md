# HIJO-C06 — bloqueo, instalación libre y señales mínimas

2026-10-12. Bases C04 `4636d30`, C05 `94b1bbe`. **Una unidad documental**:
arquitectura candidata, decisiones pendientes y ensayos futuros, no implementación
o autorización física. Reutiliza C04/C05 y sus límites; nuevas fuentes oficiales §8.
Laboratorio registrado Android14/API34; repo minSdk31/targetSdk36/compileSdk36.
Documentación actual no acredita comportamiento Samsung, otras versiones o firmware.

## 1. Confirmado y corrección de interpretación

- HOME propio aceptado; bloqueado: una pantalla del hijo sin salidas para menor,
  excepto emergencia/Keyguard independientes. Adulto permite remoto o código local.
- **Instalar y actualizar cuando está permitido**, sin selección parental ni
  traslado obligatorio a mantenimiento. Proteger administración/Ajustes, hijo
  contra desinstalación/force stop/borrado de datos y escapes por APIs oficiales.
- Propietario quiere máxima resistencia y corrección iterativa. **No acepta ventanas
  o fallos conocidos como entrega final.** La alternativa de C05 de tolerarlos no
  es elección aprobada; tampoco aprobación de prototipo/pruebas físicas. Un fallo
  reproducido impide certificar, aunque se informe honestamente. No prometer lo imposible.
- Permanecen código numérico 5–10 como cadena, pregunta personalizada, tiempo LOCAL
  elegido, última acción válida en hijo, permisos LOCAL/REMOTO offline, reboot
  bloqueado y administración RAM cerrada al salir. No se reabren en C06.
- Avisos requeridos: tiempo agotado, menor usando, agregado diario y conectividad.
  Se permite diseñar **indicador/agregado mínimo**, no contenido, ubicación, apps
  visitadas ni historial por app. Ningún dato real se recoge aquí.

La pantalla única es interfaz pública, no administración: entradas de código/
pregunta y emergencia no abren apps sin autoridad. Al salir de administración se
cierra RAM, no el permiso normal comprometido. No PIN parental sobre cada app o
pantalla Android; Keyguard/PIN SIM siguen del sistema.
HOME público e intents externos no son autoridad: toda entrada sensible revalida
código/ticket/revisión en el núcleo, no solo oculta un botón. Instalar otra app no
le concede administración ni permite abrir controles mediante un extra exportado.

## 2. Núcleo mínimo propuesto (sin nuevo protocolo)

Un escritor serial, registro durable de intención/revisión/boot/plazo y resultado
real de políticas; lectores de capacidades y proyección segura. Subsistemas:

1. **HOME/Lock Task persistente**: HOME fijado por DPM; hijo siempre parte de lista
   técnica. Permitir amplía a todas las apps normales del catálogo, bloquear reduce
   al hijo y excepciones estrictamente necesarias demostradas para sistema/emergencia.
   `LOCKED` no `PINNED`; preservar KEYGUARD/HOME seguro y evaluar bloqueo intra-task.
2. **Catálogo/reconciliador**: PackageManager, cambios de paquetes/roles y snapshot
   a inicio/HOME/boot/reemplazo propio. Catálogo local de política, no lista de uso.
3. **Deadline/enforcement**: alarma PendingIntent con reloj transcurrido y servicio
   visible cuando proceda; relectura y diagnóstico, no dependiente de red o medición.
4. **Observador mínimo y acumulador**: estado de pantalla/Keyguard y calidad temporal
   separados de permisos; no autoridad para abrir, cambiar código o cancelar plazos.
5. **Snapshot público firmado futuro**: modo/resultado, medición/calidad y conexión
   con frescura; sin secretos, catálogo de apps o datos por aplicación.

Abrir: verificar boot/revisión/plazo, persistir intención y armar disparador;
ampliar lista/features y comprobar resultado. Cerrar: persistir intención, reducir
lista/features, comprobar, llevar a HOME cuando sea seguro, guardar resultado.
Android no proporciona transacción conjunta disco/alarma/DPM/UI: recuperación
idempotente y readback en cada paso. En API34 políticas Lock Task/lista/features
comparten resultado `PolicyUpdateReceiver`; callback no prueba ausencia de escape.
Nueva revisión invalida confirmaciones/desafíos/timers previos, incluida apertura
LOCK_FOR; segunda acción conflictiva exige confirmar otra vez, no refirmado solo.
Seq remoto no se usa como contador de medición. ACK no sustituye estado posterior.

### Instalación/actualización libre durante permiso

No activar DISALLOW_INSTALL_APPS como solución a las carreras. Tiendas/instaladores
y confirmación de permisos forman parte del flujo cotidiano. Identificar paquetes
necesarios sin permitir todo Settings por comodidad; no se promete aislamiento por
actividad dentro de un paquete permitido. Si una configuración de fuente desconocida
requiere Settings amplio, exponer incompatibilidad y diseñar entrada oficial acotada,
no ocultarla bloqueando instalación. No instalar silenciosamente ni autoaprobar
permisos de apps nuevas bajo la etiqueta «libre».

Lista técnica de todas las apps del alcance, sin aprobación parental: agregar
automáticamente antes de lanzamiento desde HOME; invalidar catálogo ante cambios.
Visibilidad target>=30 puede filtrar consultas: justificar queries y comprobar
cobertura; QUERY_ALL_PACKAGES solo si necesario y con revisión de privacidad/
distribución, **no concedido ahora** [S6]. Paquetes sin icono/componentes/UID compartido
también requieren análisis. No convertir fallo de clasificación en exclusión silenciosa.

`ACTION_PACKAGE_ADDED/REPLACED/REMOVED` son avisos, no barrera previa al lanzamiento.
Receivers dinámicos mientras controlador vivo y relecturas; no asumir recepción
manifest de todo broadcast: REPLACED no está exento de restricciones de fondo;
MY_PACKAGE_REPLACED solo cubre hijo [S7]. Un launch desde tienda/intents puede
anteceder reconciliación. Mientras aún no esté en lista, Lock Task puede impedirlo
temporalmente; eso es fallo de integración a corregir, no selección parental.
Paquete ya permitido actualizado mantiene nombre y puede introducir actividades
peligrosas antes de inspección: no existe aquí hook oficial atómico que asegure
revisión antes de cada ejecución. Procesar catálogo siempre contra revisión actual:
si vence permiso durante instalación, **no** reampliar después al recibir broadcast.
No asumir que poder cerrar UI cancela instalación iniciada: paquete que termina
instalándose bloqueado no recibe permiso hasta acción adulta nueva válida.

Proteger hijo con `setUninstallBlocked` y `setUserControlDisabledPackages`; proteger
usuarios/default HOME y administración por restricciones aplicables. DISALLOW_APPS_CONTROL
no cubre todo medio; factory reset desde Ajustes no es recovery físico. Suspensión
complementaria solo si aporta algo probado, devoluciones de fallos y restauración
de cambios propios, nunca indiscriminada: marcador/launcher/instaladores esenciales
son excepciones oficiales (C05). Sin accesibilidad, overlays o APIs privadas.

## 3. Matriz de escapes/carreras y cotidianos inseparables

| Caso | Mitigación candidata y prueba futura | Límite que no se entrega como resuelto |
|---|---|---|
| HOME/back/recents/launcher alternativo | HOME persistente, Lock Task, lista reducida y comprobar tarea real desde cualquier app | Preferencia HOME sola no impide launch explícito; otro launcher puede mezclar uso y administración |
| Settings/tienda/instalador/permisos | Restricciones específicas, intents oficiales acotados y negativos desde cada flujo cotidiano | Lock Task es por paquete, no permite prohibir solo actividades admin del mismo paquete. Permitir Settings entero o prohibirlo entero rompe objetivos distintos |
| Instalar y abrir/actualizar app | Catálogo dinámico y relectura antes de HOME; prueba desde tienda, enlaces, paquetes múltiples y actualización con tarea abierta | Broadcast tardío/crash/cambio de UID; sin gancho global atómico. No cerrar caso restringiendo instalación a mantenimiento |
| Vencer durante instalación/reconciliación | Cada efecto comprueba revisión/boot/deadline, cola serial, lista reducida prevalece | Finalizar instalación no debe revivir permiso; instalador esencial puede no suspenderse |
| Compartir/picker/notificaciones/Quick Settings | Flag intra-task, features mínimos y prueba de destinos/acciones inline | Excepciones sistema y actividades del mismo paquete; no abrir todo marcador/Settings como arreglo |
| PiP/multiwindow/audio/servicios | Reducir tareas, probar contenido/interacción/audio residual y suspensión acotada si segura | Retirar tarea no equivale a detener todo trabajo; registrar defecto, no APPLIED por llamada DPM |
| Force stop/desinstalar/borrar hijo/usuarios/perfiles | Políticas específicas, readback/callback y negativa por UI normal | No inmunidad ante LMK/crash/root/recovery; funciones Samsung no certificadas por nombre de restricción |
| Proceso muerto/Doze/alarma revocada | PendingIntent independiente, comprobación capacidad, FGS legítimo y reevaluación al volver | Android puede diferir ejecución y mantener lista previa. C05 documenta sin lease autónomo: defecto bloqueante si reproduce uso fuera de plazo |
| Reboot/primer PIN/corrupción | Boot aware, DE mínimo bloqueado, invalidar permiso viejo y reconciliar al unlock/HOME | Orden callback/primer launch no demostrado; no encerrar tras PIN ni llamar lockNow continuamente |
| Emergencia/retorno y recuperación | KEYGUARD/sistema siempre disponibles; al regresar no nueva autorización | No reparar endureciendo la salida ni certificar solo sin SIM; llamadas reales solo coordinación específica |

`lockNow` sirve para ruta Keyguard, **no bloqueo parental persistente**. Alarma exacta
requiere capacidad real/acceso especial, no grant automático por DO; flexibilidad
idle/FGS/proceso y reglas BAL target36 permanecen según C05/[S8]. Servicio visible
API34 `systemExempted` solo si condiciones DO/declaraciones válidas; en API31–33
sin tipo inexistente. Sin wake lock permanente ni etiquetar dataSync/remoteMessaging
para evadir restricciones. Si no se elimina un fallo por APIs oficiales, detener
certificación y llevar incompatibilidad concreta al propietario, no aceptarlo de oficio.

## 4. «Está usando»: fuentes, permisos y significado

| Fuente oficial | Qué permite observar | Qué NO prueba |
|---|---|---|
| `PowerManager.isInteractive`, SCREEN_ON/OFF [S1,S7] | Dispositivo disponible para interacción; broadcasts dinámicos, sin permiso de uso | Interacción humana, persona menor, app foreground o pantalla física necesariamente encendida; puede ser dream/proximidad |
| `isKeyguardLocked` / `isDeviceLocked` [S2] | Keyguard mostrado vs autenticación requerida para usuario, semánticas distintas | Quién usa; llamada sobre Keyguard puede ocultarlo visualmente, swipe no equivale a autenticación |
| Activity `onUserInteraction` [S3] | Input enviado a **nuestra** actividad | Input global en apps ajenas o duración humana; no habilita gancho global DO |
| UsageEvents USER_INTERACTION [S4] | Un paquete fue interactuado de alguna manera; requiere PACKAGE_USAGE_STATS/acceso Settings | Flujo continuo de todos los toques, identidad, tiempo humano exacto; evento espaciado no define intervalo ocupado |
| UsageEvents SCREEN/KEYGUARD [S4] | Transiciones históricas de estado si disponibles; rango Unix time y datos pocos días | Monotónico continuo, consulta completa si permiso/CE faltan. `queryUsageStats` suma por app y periodos expandidos, no usar para agregado global |
| Permiso normal/PONG/presencia/foreground | Autoridad o proceso/conexión/actividad visible según fuente | Uso observado o Internet validado, menos aún persona presente |

**Recomendación mínima pendiente de elección:** indicador «uso posible: dispositivo
interactivo sin Keyguard durante modo normal comprobado», «sin condición de pantalla
interactiva de este indicador» o **desconocido** (sin observación/frescura/enforcement incierto).
No mostrar «menor usando» como hecho humano si solo hay proxy. Separar «autorizado»
del indicador. Video sin input puede ser uso; pantalla despierta abandonada puede
no serlo; pantalla apagada no descarta escucha de audio/llamada. No hay detector
global fiable de interacción humana continua demostrado
por estas APIs; la denominación/alcance debe acordarse antes de interfaz/medición.

Alternativa si propietario exige evidencia de interacción: señal «interacción
reportada recientemente por Android» usando USER_INTERACTION con ventana de frescura
a acordar, **no** booleano humano constante ni duración calculada entre eventos.
PACKAGE_USAGE_STATS es acceso especial concedido desde Settings, no runtime permission
que `setPermissionGrantState` conceda genéricamente. No se pide/concede ahora.
En API34 `queryEvents(begin,end)` devuelve eventos con datos por paquete: riesgo de
exceso de datos aun si descartados. API35 `UsageEventsQuery` permite filtrar tipos,
no disponible por compilar/target36 sobre A13 API34 [S4]. Solo si se aprueba ese
acceso: leer tipos necesarios en memoria, no consultar/getPackageName/clase/contenido,
ni almacenar/exportar/debuguear eventos; evaluar si entrega amplia del API34 es
compatible con minimización. Sin aprobación usar proxy sin ese acceso, no emularlo.
Revocación/null/antes primer unlock = desconocido, no «no está usando»/cero.

## 5. Agregado diario offline, no tiempo autorizado disfrazado

Propuesta de métrica con fuente explícita: **tiempo estimado de uso posible normal**
según observación interactiva sin Keyguard y modo normal efectivo; NO tiempo de
interacción humana ni cuenta regresiva autorizada. Excluir administración/pantalla
bloqueada/emergencia identificadas; si categoría no distinguible, declarar calidad
incierta en lugar de adivinar persona/actividad. Si se exige total humano exacto,
queda sin solución demostrada y no se sustituye silenciosamente por esta métrica.

- Intervalos monotónicos disjuntos mientras observador vivo/continuidad comprobada;
  inicio/fin en recepción/lectura de estado, no en cada PONG. Registrar error de
  entrega/muestreo como estimación; no afirmar continuidad física perfecta entre muestras.
  Cerrar al cambiar estado/boot/observador, y reevaluar. Las dos autoridades no
  crean contadores paralelos; el tiempo no se suma por aplicación ni por ventana.
- Checkpoint atómico del acumulado/cursor/boot/día/calidad, sin guardar secuencia
  de intervalos históricos. Commit avanza cursor junto al total; repetir input o
  restaurar proceso no vuelve a sumar. Fallo readback: subtotal conocido anterior,
  calidad parcial, no cero. Cambiar código no borra contador/calidad por conveniencia.
- Proceso perdido: mantener subtotal comprometido, tramo sin observación desconocido;
  no extender estado activo desde último checkpoint hasta ahora. Same boot permite
  acotar tamaño de hueco por elapsed, **no** inferir uso. Reboot conserva subtotal
  si día verificable, invalida cursor previo; monotónico vuelve a cero. No sumar
  downtime estimado con reloj civil. Pérdida de archivo/corrupción = no disponible.
- Día propuesto: fecha civil en zona IANA fijada por adulto, sin ubicación; zona
  estable, cambio explícito no reclasifica datos viejos. Duraciones por elapsed,
  límites de día por ancla civil/zona más elapsed. DST da días 23/25 h; no capar a
  86400 para fingir cálculo correcto. Split del intervalo en límite comprobado.
- Hora/zona/salto/incoherencia: invalidar correspondencia de día y mostrar
  «día/total incierto», conservar subtotal sin reset o doble rollover; no repetir
  medianoche al atrasar hora ni crear un día falso. Resolver una sola vez contra
  ancla coherente, sin redistribuir huecos retrospectivamente. Reloj civil local
  no es fuente infalsificable; restricciones oficiales ayudan, no prueban exactitud.
- Total enviado **absoluto**, día/zona/época de medición/calidad/cursor de snapshot,
  no deltas que padre sume. Estado más nuevo sustituye; misma época no decrece salvo
  corrección explícita con incertidumbre, nunca reset oculto. Snapshot viejo no
  revive actividad; sync fresco firmado correlacionado con contexto de recepción.
  Identificadores aquí son conceptos por acordar, no nombres/campos CP/1 nuevos.
  Firma acredita origen/integridad, no frescura: correlacionar sync/contexto vivo,
  rechazar snapshots viejos y mostrar edad medida localmente al recibir, no confiar
  solo en hora civil transmitida. Incertidumbre de medición no excusa un fallo de
  bloqueo: su diagnóstico permanece aunque el contador sea parcial.

Retención recomendada: un registro del **día vigente**, subtotal+calidad y cursor
mínimo; sin tabla de días/intervalos, apps, nombres, historial privado o log de
observaciones. Padre mantiene último snapshot vigente, no serie temporal de uso.
Al rollover fiable sustituir registro; si día incierto no acumular días ocultos:
suspender publicación como «hoy» y conservar un único registro no disponible para
reconciliar. Requiere política acotada de eliminación/recuperación temporal por
acordar, no retención infinita por error de reloj. Secretos/verificadores en dominio
separado, nunca QR/relay/logs/exportación. Medición no condiciona emergencia o permiso.

## 6. Avisos, conectividad y snapshot mínimo recomendado

**Recomendación:** snapshot vigente + agregado/calidad del día + **un marcador
durable del último vencimiento del día**, porque snapshot solo perdería aviso si
vence offline y adulto luego permite. Marcador se escribe al detectar vencimiento,
ligado boot/revisión/plazo; estado de cierre pendiente/comprobado/fallido aparte.
Un marcador, no cola de eventos ni historial; vencimiento posterior sustituye al
anterior. No retener todos los vencimientos/acciones ni replay de órdenes.
Padre deduplica notificación por identidad del marcador, sin confundir cursor de
snapshot con seq remoto. Al rollover fiable se elimina; si envío llega tarde mismo
día: «se agotó anteriormente; reporte recibido ahora», no «bloqueado ahora». Sin
controlador vivo al deadline, detección/aviso son tardíos: no inventar timestamp de
aplicación en la hora planificada. Marcador de detección desconocida no demuestra
acción exacta ni permite reconstruir todos los eventos perdidos. Esta retención
mínima necesita acuerdo de versión/minimización, no está autorizada en CP/1.

| Aviso al padre | Fuente/condición | Presentación honesta |
|---|---|---|
| Tiempo agotado | Deadline válido detectado/último marcador, firmado | Agotado separado de bloqueo aplicado; modo actual puede ser permiso posterior. Detección tardía explícita |
| Usando | Indicador §4 reciente, calidad y alcance elegidos | Uso posible/interacción reciente, o desconocido; no PONG como humano. Notificar transición vigente, no cada heartbeat |
| Uso del día | Total absoluto §5 con día/calidad | Total estimado/parcial, no cero ficticio; sin precisión humana no acreditada |
| Internet/conexión | Observación local ConnectivityManager y canal autenticado independientes | Internet Android validado/no validado/desconocido vs WSS conectado/sin canal; edad del último reporte |

Android `NET_CAPABILITY_INTERNET` declara capacidad, VALIDATED aporta validación
local del sistema, no llegada garantizada al relay [S5]. Callback de red default
requiere declarar ACCESS_NETWORK_STATE (permiso normal); INTERNET para transporte futuro.
No geolocalización/Wi-Fi scans/sondeos propios. onAvailable no basta; observar
onCapabilitiesChanged/onLost con frescura. Sin proceso/callback = desconocido.
No validado puede ser portal/filtrado, no afirmar ausencia física de toda Internet.

Padre sin comunicación: **«sin reporte reciente; causa desconocida»** y último
reporte fechado, no «hijo sin Internet». Puede ser hijo/padre/relay/canal/proceso.
Relay presencia/PONG solo conexión en ese instante, no uso ni Internet del teléfono.
LOCAL/REMOTO siguen offline hasta regla válida, no bloquear por medición/conexión.

Notificaciones padre son integración propuesta para su revisión: app viva puede
publicar en canal creado (API26+) y necesita POST_NOTIFICATIONS en API33+, permiso/
canal/usuario/DND afectan entrega [S9]. Padre cerrado/muerto o sin red no recibe
instantáneamente por un socket imaginario. No hereda excepción DO/systemExempted
del hijo: FGS requiere tipo y caso legítimos, lifecycle y restricciones de inicio
desde fondo; no usar remoteMessaging/dataSync como comodín [S8]. No FCM, servicios
externos, cron/autoping o push aprobado. Si se requiere aviso garantizado con padre
cerrado, incompatibilidad pendiente de decisión, no resolverla agregando servicio.

## 7. Mínimos por rol, decisiones y siguiente unidad propuesta

- **Hijo:** núcleo/catálogo/enforcement y observador mínimo separado, calidad diaria
  y marcador único; firmas solo tras versión acordada e identidad productiva
  validada (H08 pendiente), sin fabricar identidad para esta función.
- **Padre:** significados/edad/calidad visibles, reemplazo de snapshots firmados,
  deduplicación de marcador, notificaciones/permisos revisados; no almacenar catálogo/
  historial ni inferir modo actual desde ACK anterior o timeout de presencia.
- **Relay:** validación/routing compatible con versión acordada, sin calcular ni
  almacenar uso/avisos offline. Si no puede entregar al padre, siguiente sync refleja
  snapshot hijo; no nueva cola/historial o garantía push. Trabajo externo no ejecutado.

CP/1 y ADR/planes permanecen intactos; señales/semántica nuevas requieren acuerdo
de tres roles y migración explícita (CP/2 sigue provisional). No mensajes v1 nuevos
ni bypass. Mínimos no son schema ni aprobación de puertas, H05 abierta/H06 no autorizada.

**Tres decisiones todavía imprescindibles:** (1) semántica visible de «usando» y
«total» (proxy vs señal reciente), con/sin acceso especial y su entrega amplia API34;
(2) retención marcador único/calidad, día/zona y manejo acotado de día incierto;
(3) resolver límites cotidianos/admin inseparables y requisitos de aviso con padre
cerrado, sin revocar instalación libre ni aceptar fallos finales. Detalles de C04
de credenciales/rangos/LOCK_FOR siguen coordinación aparte.

Siguiente unidad mínima **propuesta para asignación**, no iniciada: modelo puro
aislado de snapshots/intervalos/día/calidad y marcador, con reloj/repositorio falsos,
sin datos por app, contrato ni Android; pruebas de duplicación/crash/salto de día/
reboot/huecos/ACK tardío. Otra unidad posterior a asignar, no ejecución
autorizada: prototipo futuro reversible del catálogo/retorno y negativas de §3/C05
Q1–Q9, en particular instalar-y-abrir/actualizar y vencimiento con proceso muerto.
Añadir ensayos de pantalla abandonada/vídeo sin input/Keyguard/permiso revocado,
recepción tardía o duplicada de snapshot, padre muerto/canal silenciado/red solo
local/relay caído; ninguna prueba se realiza aquí. Recuperación/emergencia previas
y autorización física específica A13/selector único independientes; A56 no tocar.

## 8. Fuentes oficiales y cierre

Consultadas **2026-10-12**, todas HTTP 200; capacidad documentada, no evidencia física.
API34 no obtiene por target36 los filtros de UsageEventsQuery API35.

- [S1] https://developer.android.com/reference/android/os/PowerManager#isInteractive()
- [S2] https://developer.android.com/reference/android/app/KeyguardManager
- [S3] https://developer.android.com/reference/android/app/Activity#onUserInteraction()
- [S4] https://developer.android.com/reference/android/app/usage/UsageStatsManager
  https://developer.android.com/reference/android/app/usage/UsageEvents.Event
  https://developer.android.com/reference/android/app/usage/UsageEventsQuery.Builder
- [S5] https://developer.android.com/reference/android/net/NetworkCapabilities
  https://developer.android.com/reference/android/net/ConnectivityManager
  https://developer.android.com/develop/connectivity/network-ops/reading-network-state
- [S6] https://developer.android.com/reference/android/app/admin/DevicePolicyManager
  https://developer.android.com/training/package-visibility
- [S7] https://developer.android.com/reference/android/content/Intent
  https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions
- [S8] https://developer.android.com/develop/background-work/services/alarms/schedule
  https://developer.android.com/reference/android/app/AlarmManager
  https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
  https://developer.android.com/develop/background-work/services/fgs/service-types
  https://developer.android.com/privacy-and-security/direct-boot
  https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime()
  Reutilizar además fuentes BAL/Lock Task/Doze de C05, sin ensayo nuevo.
- [S9] https://developer.android.com/develop/ui/views/notifications/notification-permission
  https://developer.android.com/develop/ui/views/notifications/channels

Revisión documental/diff únicamente. Sin código/tests/builds/APK/Keystore/KDF/WSS,
dispositivos/ADB/políticas/permisos reales/CA/LAN/push/despliegue, lecturas o cambios
de repos ajenos, sesiones/subagentes/prompts de vuelta. QR padre reproducido no
corregido; QR02 no asignado ni trabajado. Detenerse tras commit para revisión.
