# HIJO-C05 — todas las apps normales y retorno al bloqueo en Android

Fecha de evaluación/consulta: **2026-10-12**. Base: HIJO-C04 `4636d30`.
Unidad exclusivamente documental. No arquitectura aprobada, prototipo, permiso
físico, implementación CP/2 ni cierre de H05/H06. Fuentes oficiales [R1–R12].

## 1. Resumen para el propietario

**Viabilidad condicionada, no compatibilidad certificada.** Android ofrece piezas
oficiales para un teléfono administrado: launcher propio, quiosco multiapp y
protección de controles. Una lista técnica calculada con **todas** las apps de uso
puede sustituir la selección parental, pero no existe un comodín que haga segura
cualquier app/superficie. La suspensión tampoco bloquea todos los paquetes.

**No puede prometerse cierre instantáneo e infalible** al segundo exacto con el
controlador muerto, Doze, reinicio o firmware arbitrario: las políticas no contienen
una fecha de expiración que Android aplique autónomamente. Una alarma despierta al
controlador; hasta que actúe, la política anterior puede seguir permitiendo tareas.
Tampoco se ha demostrado ausencia de ventana al primer desbloqueo tras reboot.
Ser Device Owner en el laboratorio no resuelve estas limitaciones.

Arquitectura candidata preferida para investigar: permanecer en Lock Task real,
HOME del hijo y conjunto técnico dinámico de todas las apps normales; al bloquear,
reducirlo al hijo, conservando Keyguard/emergencia y comprobando el resultado.
Esto **no equivale a experiencia Android irrestricta**: elegir otro HOME, modificar
administración/usuarios e instalar durante uso requieren decisiones específicas.
Si «todas» incluye dichas superficies irrestrictas, o se exige cero ventana ante
cualquier fallo, **no hay cumplimiento demostrado con estas APIs**. No sustituir
el requisito por una lista parental ni anunciar esa garantía.

Decisiones necesarias para continuar (§8): alcance de superficies administrativas,
instalación/cambios de paquetes y límite de garantía temporal/boot. Los detalles de
credenciales, duración y versión siguen la coordinación separada, no esta tarea.

## 2. Requisitos, entorno y evidencia realmente disponible

- Todas las apps normales sin elección parental, tiempo transcurrido también con
  pantalla apagada, LOCAL/REMOTO conservan autorización offline hasta plazo/bloqueo
  nuevo válido/reboot. Reconexión no extiende ni restaura permiso.
- Reboot siempre bloqueado y autorización nueva; continuidad solo del mismo boot.
- Controles propios con código; administración RAM cierra al fondo, no revoca por
  ello uso ya comprometido. Emergencia sin código ni espera; Keyguard independiente.
- No monitoreo de actividad/contenido, accesibilidad, overlays ni APIs privadas.

Inspección **solo lectura**: `android/app-child/build.gradle.kts` configura
**minSdk 31, targetSdk 36, compileSdk 36**. No significa Android 36 instalado:
`ESTADO.md` registra A13 SM-A135M Android 14/API 34, One UI 6.1 y firmware
`A135MUBSDDZB3`. No se reconfirmó físicamente ni se extrapola a otros Samsung.
Hay que separar API disponible en ejecución de reglas activadas por target SDK;
APIs 35/36 requieren guardas y no resuelven el caso API 34.

Manifest propio: HOME y `lockTaskMode="if_whitelisted"`, boot receiver Direct Boot,
RECEIVE_BOOT_COMPLETED; **no** servicio/alarma exacta/visibilidad global declarados.
`PolicyEnforcer.kt` solo tiene adaptación inicial de `lockNow`; `BootReceiver.kt`
registra contexto, no entra en quiosco/enforcement. Ninguna declaración demuestra
HOME persistente, cierre temporal o protección efectiva. H04 de contexto vacío no
prueba restauración de políticas reales; H05 abierta, H06 no autorizada.

Reutilizado §5 de `DISENO_CONTROL_LOCAL_ADULTO.md`. Leídos SPEC H5/H5.1/H7 y
ADR-H-001/004: v1 propone lista aprobada y quiosco persistente; C04 supersede la
selección como requisito futuro, no sus archivos ni el código CP/1. Este documento
no modifica contratos, planes, ADR ni fija protocolo/campos.

## 3. Opciones oficiales: capacidad y límite

| Opción | Garantía documentada | Evaluación para este requisito |
|---|---|---|
| HOME persistente, `addPersistentPreferredActivity` [R2] | Elige handler HOME y mantiene preferencia frente a cambios/resets de preferencias | Base útil, no impide intents explícitos a otro launcher ni actividades ya abiertas; necesita Lock Task. Sin paquete presente vuelve resolución normal |
| Lock Task multiapp [R1,R2] | `setLockTaskPackages` delimita paquetes; desde API 23 retirar paquetes finaliza sus tareas bloqueadas; paquetes con mismo UID también admitidos | Candidato principal, no wildcard. Debe estar realmente activo (`LOCKED`, no `PINNED`) y contener todas las apps normales descubiertas. Retirar no es suspensión ni cese garantizado de servicios/audio |
| `setLockTaskFeatures` [R2] | Controla HOME/Overview/notificaciones/Keyguard y otras funciones **solo mientras Lock Task está activo** | Mantener Keyguard y HOME seguro; habilitar Overview/notificaciones cambia experiencia y salidas. En API 34 paquetes/features son una política conjunta, con callbacks de resultado/conflicto |
| `LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK` (API 30) [R2] | Bloquea inicio de actividades no incluidas dentro de tarea bloqueada | Necesario evaluar: sin flag hay paso a actividades no listadas dentro del mismo task. Con flag también puede impedir flujos legítimos del sistema; paquete permitido no permite separar sus actividades seguras/peligrosas |
| Salir de quiosco al permitir; reentrar al vencer [R1,R8] | Puede iniciar actividad con `ActivityOptions.setLockTaskEnabled` desde API 28 | Da experiencia más amplia, pero mayor superficie y retorno requiere ejecución/BAL/estado UI. No opción preferida ni retorno garantizado por HOME |
| Suspender, `setPackagesSuspended` (API 24) [R2] | Impide actividades, oculta notificaciones/recents y bloquea toasts/diálogos/timbre; devuelve paquetes que no logró cambiar | Complemento posible, no bloqueo universal. No todos son suspendibles ni es contrato de parar todo trabajo de fondo. Leer fallos y restituir solo lo cambiado por este controlador |
| Ocultar, `setApplicationHidden` [R2] | Paquete no disponible para uso, conserva archivo/datos | No sustituto transparente de suspensión temporal de todo el sistema; resultados, dependencias y restauración requieren revisión. No deshabilitar componentes esenciales |
| `lockNow` [R2] | Bloqueo de pantalla mediante política de dispositivo | Keyguard/emergencia, **no** bloqueo parental continuo: al introducir PIN Android las apps podrían seguir autorizadas. No rotar PIN, repetir locks ni usarlo como contador/enforcement |

Suspensión excluye oficialmente **device admins, launcher activo, instalador,
desinstalador y verificador requeridos, marcador predeterminado y permission
controller** [R2]; no equivale a lista de todos los paquetes esenciales Samsung.
No suspender a ciegas Telecom/SystemUI/teclado/SIM/conectividad ni añadir paquetes
enteros a quiosco como reparación de emergencia. `setPackagesSuspended` exige que
el paquete ya esté instalado; desinstalarlo pierde suspensión.

No hay transacción Android única entre escritura propia, alarma, lista/features,
suspensión y lanzamiento HOME. La propuesta requiere pasos idempotentes, resultado
pendiente/fallo separado de bloqueo comprobado, relecturas y recuperación. API 34
`PolicyUpdateReceiver` añade evidencia de política, no demuestra UI/emergencia o
ausencia de escape [R2]. En API 31–33 faltan esos callbacks nuevos.

## 4. Qué es una app normal y qué es administración

Clasificación propuesta para revisión, **no nueva exclusión parental aprobada**:
uso ordinario de aplicaciones instaladas frente a modificación de la autoridad,
usuarios, launcher predeterminado o configuración destructiva. No clasificar
«app con enlaces» o «app nueva» como prohibida para evitar validar el requisito.

| Superficie/caso | Durante permiso | Al bloquear y prueba negativa futura |
|---|---|---|
| Apps ordinarias/juegos/navegador | Todas las de uso en conjunto técnico; sin observar cuál usa el niño | Retirar tareas Lock Task; probar tarea abierta, back/recents/deep links y audio/servicios que pueden seguir |
| HOME/otros launchers | HOME propio, otros launchers contienen posible administración; falta decidir si su uso ordinario se incluye | Intento explícito, cambiar predeterminado y widgets no deben abrir ruta alternativa; HOME por sí solo no basta |
| Ajustes y control del hijo | Controles hijo autenticados; ajustes ordinarios (Wi-Fi/volumen) no son toda administración | Permitir el paquete completo Settings abre muchas actividades; no hay allowlist Lock Task por componente. No ofrecer todo Ajustes; mantenimiento acotado por diseñar sin cortar recuperación |
| PiP/multiwindow/Overview | Lock Task no promete UX irrestricta; probar vídeo, ventanas y recientes [R1] | Encoger lista desde app/PiP/ventana secundaria: ningún contenido interactivo queda usable. No dar por cerrada ventana por API retornada |
| Notificaciones/Quick Settings | Habilitarlas requiere HOME y configuración correspondiente; cambia vías de entrada [R1] | Toques, acciones remotas/inline y panel sobre Keyguard; `setStatusBarDisabled` no actúa en Keyguard ni sustituye features Lock Task [R2] |
| Intents/compartir/documentos/permisos | Todas las apps de uso pueden encadenarse; pickers/resolvers/permission controller son sistema | Flag intra-task y tareas externas; no listar todo sistema para arreglar flujo. Probar picker/compartir/enlace a Ajustes desde cada categoría |
| Usuarios/perfiles/espacios | Candidato: usuario administrado único, no conmutación libre | Bloquear añadir/cambiar usuario con restricciones aplicables. Inventariar preexistentes; no suponer alcance de usuario 0 sobre otro perfil |
| Instaladores/tiendas/paquetes nuevos | Si se permite instalar, reconciliación dinámica no es atómica | Carrera instalar-y-abrir desde tienda/intents antes de broadcast; reinstalar/actualizar cambia suspensión/UID/tareas. Bloquear instalación sería cambio funcional a decidir |
| Marcador/telefonía/SIM/sistema | Uso normal según alcance acordado; no suspender marcador predeterminado | Emergencia/entrantes/devoluciones de seguridad independientes. Marcador no suspendible no autoriza toda telefonía normal al estar bloqueado |
| Teclado, asistentes, servicios de sistema | Necesarios para interacción; UI puede contener enlaces/ajustes | Cambiar IME/abrir sus ajustes, asistente, botones físicos y menú de energía. No prometer aislamiento por categoría «sistema» |

Protección oficial candidata [R2,R3], **no aplicada**:

- `setUserControlDisabledPackages` para hijo (API 30): bloquea borrar datos/force
  stop por usuario y exime de App Standby Buckets; no evita crash/LMK/Doze en general.
  `setUninstallBlocked` para hijo: proteger desinstalación, sin depender solo de ser admin.
- `DISALLOW_APPS_CONTROL` impide acciones desde Settings/launchers, **no** todos
  los medios (la referencia cita ADB y PackageInstaller). Restricción global puede
  impedir gestionar apps ordinarias; preferir protección específica donde alcance.
- `DISALLOW_ADD_USER`/`DISALLOW_USER_SWITCH`; `DISALLOW_ADD_MANAGED_PROFILE` está
  obsoleta desde API 30 e ignorada por DO en fully managed: no venderla como cobertura.
  Espacios/funciones Samsung no tienen compatibilidad demostrada por esas claves.
- `DISALLOW_INSTALL_APPS` impide instalar también al DO: mantenimiento debe tener
  salida autorizada, no activarla sin recuperación. Fuentes desconocidas global no
  equivale a prohibir toda tienda; referencia actual incluye excepciones de tiendas
  registradas, cuya disponibilidad por versión hay que separar [R3].
- `DISALLOW_FACTORY_RESET` cubre reset desde Ajustes, no recovery/reflasheo físico;
  `DISALLOW_SAFE_BOOT` y debugging necesitan pruebas y no eliminan retirada legítima.
- `DISALLOW_CREATE_WINDOWS` restringe toast/overlay, no significa cerrar toda ventana
  de aplicación/PiP o desactivar todos los servicios ajenos. No lo usamos como overlay.

No pedir permisos de monitoreo, ni convertir todas las pantallas en PIN constante.
Las protecciones deben conservar Wi-Fi/actualización/retirada autorizadas sin regalar
administración general. Su ausencia operativa actual bloquea endurecimiento.

### Descubrimiento sin selección parental

`PackageManager` y consultas MAIN/LAUNCHER permiten construir catálogo técnico;
algunas apps tienen varias actividades o ninguna launcher. Con target >=30 hay
filtrado de visibilidad: `getInstalledApplications` no prueba inventario completo
[R10,R11]. HOME/DO no se toma aquí como exención universal sin validar. Propuesta:
consultas declaradas mínimas y comprobar cobertura; si hace falta QUERY_ALL_PACKAGES,
justificar privacidad/distribución (Google Play exige aprobación), no añadirlo ahora.
El inventario mínimo local sirve para políticas, no se exporta como historial.

Reconciliar al boot/inicio y ante cambios de paquetes/roles (instalación, eliminación,
reemplazo, suspensión) con snapshot/relectura. Broadcast no precede necesariamente
al primer launch: hay carrera. Shared UID extiende lista [R2]; actualización puede
cambiar flujos sin cambiar nombre. Lista técnica sin aprobación parental debe incluir
automáticamente todas las apps del alcance acordado, identificar fallos y no excluir
silenciosamente. Conservar estado previo/propiedad de cada suspensión; no retirar
restricciones de otro admin ni desocultar arbitrariamente apps al restaurar.

## 5. Vencimiento real, proceso muerto y pantalla apagada

**Reloj:** `elapsedRealtime` incluye suspensión [R12]; deadline durable ligado al
boot, misma regla LOCAL/REMOTO. No timer por app ni por pantalla, hora civil o red.
Reconciliar sin reiniciar plazo en cada entrada del controlador/HOME/receiver.

**Disparador candidato:** AlarmManager `ELAPSED_REALTIME_WAKEUP`, PendingIntent a
receiver acotado; `setExactAndAllowWhileIdle` si capacidad real y política permiten
[R4,R5]. Alarma fuera del proceso puede despertarlo; no usar solo OnAlarmListener
en RAM para proceso muerto. Alarmas se pierden al apagar/reboot; programar/releer
tras boot no restaura permiso viejo. Comprobar `canScheduleExactAlarms`, no asumir
concesión DO. SCHEDULE_EXACT_ALARM es acceso especial revocable y no preconcedido
a instalaciones nuevas target >=33; revocación detiene app y cancela alarmas [R4].
USE_EXACT_ALARM tiene casos de uso/política limitados: no escogerlo como atajo.

**Límite oficial decisivo:** allow-while-idle admite cuotas/reordenación/flexibilidad;
referencia cita intervalos de despacho aproximadamente un minuto o hasta unos 15
minutos en bajo consumo [R5]. No convertir esas cifras en latencia máxima garantizada
ni decir que cada primera alarma se retrasa ese tiempo. Doze difiere trabajos/red y
alarmas ordinarias [R6]; WorkManager periódico no garantiza segundos. Sin capacidad
de disparo suficiente, rechazar apertura temporal con diagnóstico explícito sería
una condición funcional a aprobar, no simular temporizador exacto.

Servicio foreground visible puede apoyar reevaluación, no sustituye alarmas ni
garantiza proceso inmortal. DO figura entre excepciones de inicio FGS [R7]; API 34+
permite tipo `systemExempted` para DO con declaraciones/permisos apropiados [R9].
En 31–33 no pasar tipo inexistente. No usar dataSync/media para evadir límites,
wake lock permanente o pantalla encendida. Ningún permiso se concede por este diseño.

Receiver al vencer: releer boot/revisión/deadline; ignorar evento viejo; persistir
intención bloqueada, reducir conjunto técnico y comprobar política/tareas, luego
llevar a HOME cuando sea seguro. La alarma no concede por sí misma permiso BAL.
La guía actual de seguridad de actividades [R8] describe restricciones y opt-ins
por target 34/35/36; no enumera DO como excepción explícita en la página consultada.
Por eso no se acredita aquí lanzamiento irrestricto desde fondo. Ejemplo DPC [R1]
de inicio Lock Task **no** prueba el retorno desde cualquier estado API34/target36.
Primero cerrar mediante política; si falla retorno UI, reportar incertidumbre, no
usar overlay ni permiso privilegiado START_ACTIVITIES_FROM_BACKGROUND como supuesto.

Crash a mitad: relectura idempotente, sin APPLIED ficticio. Crash con otra app
abierta antes del plazo: puede quedar autorizada hasta ejecución nueva. Persistir
«bloqueado» no cambia lista activa. **Android no expone en estas APIs un lease con
expiración ejecutada en system_server independiente del controlador.** No hay prueba
de fail-closed instantáneo frente a fallo arbitrario; SPEC H7 ya advierte esta ventana.

## 6. Reboot/Direct Boot y emergencia

Direct Boot [R12] permite receiver `directBootAware` y almacenamiento DE antes PIN;
CE/secretos no disponibles hasta unlock. Propuesta: estado mínimo boot/bloqueado en
DE, invalidar permiso viejo sin cargar secretos ni abrir administración. Boot receiver
debe reducir políticas tan pronto pueda y reconciliar al unlock/HOME; API puede
estar presente y aun requerir comprobar servicio/estado disponible al arranque.

Las listas/políticas anteriores pueden persistir; no se transforman solas en bloqueo
por nuevo boot. Receiver puede llegar tarde/fallar. No hay prueba de orden estricto
entre entrega de boot, primer unlock y lanzamiento de task anterior. Antes PIN,
Keyguard restringe uso, **no** demuestra bloqueo parental posterior. Sin PIN Android
hay aún más exposición: no imponer PIN nuevo aquí para ocultarla.

[R1] advierte no iniciar Lock Task sobre dispositivo bloqueado por riesgo de impedir
unlock. Conservar KEYGUARD, no `setKeyguardDisabled`, ni quitar PIN/SIM. Mantener ruta
de emergencia del sistema desde preparación, bloqueo, Doze y Direct Boot. `lockNow`
puede llevar al Keyguard, no autoriza uso normal al volver. Si emergencia requiere
excepción de fabricante, investigar componente/escape, no listar todo marcador/Ajustes.
No llamar a emergencias en pruebas; pruebas de llamada real solo coordinación específica.

**Resultado boot:** requisito lógico claro; enforcement sin ventana NO demostrado.
No certificarlo con actual receiver de contexto ni con ejemplo oficial.

## 7. Mapa mínimo futuro de prototipo/pruebas (sin autorización)

Orden propuesto sujeto a revisión, sin iniciar otra unidad:

1. Acordar §8 y requisitos de garantía; revisar versión/ADR aparte. Recuperación
   operativa previa, emergencia y salida presencial verificadas antes de restricciones.
2. Modelo/adaptadores mínimos: catálogo técnico, HOME/Lock Task, diagnóstico de
   resultado, deadlines/alarmas/boot; separar lectura de cambios y registrar solo
   evidencia mínima sin actividad ni secretos. No requiere WSS para probar cierre local.
3. Prototipo reversible en equipo de laboratorio autorizado específicamente, con
   selector único A13 SM-A135M y comandos dirigidos al mismo `adb -s`; detenerse si
   ambiguo. No A56, connected tests indiscriminados ni seriales en repo. Autorización
   física/instalación/políticas independiente de aprobación del diseño.
4. Primero HOME/quiosco/emergencia y restauración, después expansión de todas apps,
   timer y negativas; no todas las restricciones de golpe. Detener ante una excepción
   esencial, escape o fallo de recuperación; nunca desactivar salida para arreglarlo.

| Prueba futura manual/negativa | Condición y evidencia exigidas |
|---|---|
| Q1 tareas/HOME | Cada app normal incluida, back/recents/widget/launcher explícito; reducir lista con otra app foreground. Comprobar LOCKED, cierre real y retorno, no solo getLockTaskPackages |
| Q2 intents/sistema | Compartir, picker, permisos, enlace Ajustes, navegador, IME/asistente, Quick Settings; con/sin flag intra-task, intento Settings desde paquete permitido. Anotar flujos normales rotos y escapes |
| Q3 ventanas | PiP, split screen/multiwindow, notificaciones/inline, menú energía; vencer plazo en cada una. Ausencia de interacción/contenido usable residual o limitación explícita |
| Q4 paquetes | Instalar-y-abrir, actualización/reinstalación, cambio launcher/marcador, apps sin icono/múltiples actividades/shared UID. Cobertura catálogo/carreras y fallos suspensión/restauración sin lista parental |
| Q5 administración | Desinstalar hijo, borrar datos, force stop, modificar defaults, crear/cambiar usuario/perfil, safe boot/reset desde Settings. Probar interfaces normales, no destruir/reflashear; salida autorizada intacta |
| Q6 tiempo/Doze | LOCAL y REMOTO offline, pantalla apagada/Doze y ahorro OEM; hora civil cambia sin extender, reconexión no renueva. Medir deadline/entrega/cierre real y latencia, no prometer cota por prueba aislada |
| Q7 proceso/fallos | Crash con otra app abierta, muerte antes/después de programar alarma, a mitad de reducción/restauración, duplicado/timer viejo. Revocar acceso exact-alarm y parar FGS por vías permitidas; no apps ilimitadas ni resultado falso |
| Q8 boot | Reiniciar permitido y con tarea alternativa, antes PIN/tras PIN inmediato/sin credencial si autorizado; paquetes reemplazados y CE no disponible. Buscar ventana de uso con lanzamiento inmediato, no solo esperar boot callback |
| Q9 emergencia/recuperación | Bloqueado/permitido/Doze/boot/fallo proceso/espera código, sin SIM y variantes SIM/PIN coordinadas. Acceso sistema sin código parental; no llamadas de emergencia como ensayo; recuperación no depende del timer/canal |

Matriz API31–33, API34 real y 35/36 si disponibles: no extrapolar del A13. Repetir
tras firmware/actualización y cambios de apps. Cada ensayo requiere autorización;
ninguno realizado en C05. Si quedan ventanas no acotadas, no cerrar puerta/enforcement.

## 8. Tres elecciones imprescindibles, sin cambiar decisiones confirmadas

1. **Alcance normal:** ¿aceptar HOME propio y excluir modificación de autoridad/
   usuarios/default HOME mientras todas las apps de uso siguen disponibles? Ajustes,
   launchers alternativos y apps con administración integrada pueden ser inseparables
   por paquete. Si se exige su uso totalmente libre, el quiosco multiapp no ofrece
   ese aislamiento; alternativa fuera de quiosco tiene mayor riesgo de retorno.
2. **Cambios de catálogo:** instalar libremente durante permiso añade carreras sin
   atomicidad oficial; alternativa instalación/actualización en mantenimiento acotado
   con todas las apps instaladas automáticamente incluidas (sin aprobación parental).
   Esta alternativa limita cuándo instalar y necesita aprobación, no se impone.
3. **Garantía temporal/boot:** no hay garantía oficial de latencia cero ante fallos.
   Elegir si estudiar comportamiento acotado mediante medición y declarar límites
   tolerables, o exigir garantía estricta: en este último caso la arquitectura queda
   sin solución demostrada y no se implementa/entrega como cumplida. No fijar aquí
   tolerancia ni rediseñar uso offline. Otra plataforma/dispositivo gestionado con
   capacidades distintas requeriría evaluación nueva, no promete resolverlo.

No reabrir código 5–10/pregunta/duración elegida/offline/reboot; coordinador resuelve
rangos/LOCK_FOR/caducidad/normalización/intentos/olvido/versión aparte. Suspensión pura
y `lockNow` no satisfacen bloqueo global continuo; lista parental reducida no es
alternativa compatible. Root/recovery/reflasheo/antirollback físico fuera de garantías.

## 9. Referencias oficiales consultadas y límites de evidencia

Todas consultadas **2026-10-12** con respuesta HTTP 200; guías vigentes al consultar,
no evidencia Samsung ni resultados físicos. Referencias API muestran nivel añadido
por método/constante: no asumir todo contenido actual disponible en API34.

- [R1] Lock Task, inicio/salida, features y advertencia Keyguard:
  https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
- [R2] DevicePolicyManager, métodos/constantes citados en §3–4, resultados API34:
  https://developer.android.com/reference/android/app/admin/DevicePolicyManager
  (buscar `setLockTaskPackages`, `setLockTaskFeatures`,
  `LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK`, `setPackagesSuspended`,
  `addPersistentPreferredActivity`, `setUserControlDisabledPackages`,
  `setUninstallBlocked`, `setStatusBarDisabled`, `setApplicationHidden`, `lockNow`).
- [R3] UserManager, restricciones y excepciones/obsolescencia:
  https://developer.android.com/reference/android/os/UserManager
- [R4] Alarmas, acceso exacto/revocación y elección de reloj:
  https://developer.android.com/develop/background-work/services/alarms/schedule
- [R5] AlarmManager, flexibilidad idle/capacidad/ciclo de alarmas:
  https://developer.android.com/reference/android/app/AlarmManager
- [R6] Doze/App Standby:
  https://developer.android.com/training/monitoring-device-state/doze-standby
- [R7] Inicio FGS desde fondo y excepciones de roles:
  https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- [R8] Seguridad/inicio de actividades desde fondo y reglas por target:
  https://developer.android.com/guide/components/activities/background-starts
- [R9] Tipos FGS, `systemExempted`:
  https://developer.android.com/develop/background-work/services/fgs/service-types
- [R10] Visibilidad de paquetes:
  https://developer.android.com/training/package-visibility
- [R11] Visibilidad automática (no prueba inventario completo):
  https://developer.android.com/training/package-visibility/automatic
- [R12] Direct Boot/almacenamiento y reloj monotónico:
  https://developer.android.com/privacy-and-security/direct-boot
  https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime()

Ningún repositorio ajeno consultado/editado en C05. Sin código, pruebas/builds/APK,
ADB/dispositivos, KDF/Keystore, WSS/LAN/certificados/push/despliegue o QR02. Defecto
QR padre comunicado reproducido no corregido; no afecta este análisis de APIs ni
habilita ceremonia óptica. Siguiente paso solo revisión de estas elecciones; no
iniciar automáticamente prototipo/especificación ni alterar CP/1/ADR/puertas.
