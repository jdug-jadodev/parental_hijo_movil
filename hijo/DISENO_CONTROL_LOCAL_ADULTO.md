# HIJO-C04 — control normal local del adulto consolidado

Actualización: 2026-10-12. Misma HIJO-C04 reanudada, base HIJO-C03 `9ae6c99`;
H05-L01 `c7fb842`. **Solo diseño, ninguna implementación o puerta aprobada.**
Las decisiones expresas se enumeran abajo; propuestas técnicas pendientes se
etiquetan como tales. CP/2 sigue siendo nombre provisional, no versión acordada.
Fuentes locales: CP/1 §5/9/9.A, HIJO-C02/03 y ADR-H-002/005/006. Contexto externo:
resumen completo del coordinador para esta reanudación y revisión padre PADRE-C03
`984693c` comunicada, no releída aquí. Tres lecturas externas previas interrumpidas;
no ampliar permisos ni inferir causa. Requisitos íntegros suministrados en el mensaje.
No cambiar compartido, fixtures, ADR aceptados, PLAN_HIJO o código por este documento.

## 1. Requisitos y separación de autoridades

**Confirmado por el propietario:**

- Código numérico propio elegido por el adulto, **5 a 10 dígitos como cadena**,
  conservando ceros iniciales; no ocho fijos ni 26 caracteres/generación aleatoria.
- Pregunta personalizada y respuesta configuradas en hijo: recuperación local
  offline para **establecer código nuevo**, nunca revelar el anterior.
- Cada desbloqueo LOCAL exige duración elegida por adulto, sin default 30 minutos.
- Todas las apps normales, sin lista parental. Viabilidad Android todavía no demostrada.
- Ambas autoridades pueden cambiar: última acción válida confirmada en hijo gana;
  remoto nuevo puede bloquear y local posterior permitir.
- LOCAL y REMOTO conservan permiso sin internet hasta plazo, bloqueo válido nuevo
  o reboot. Reconexión no lo restaura/renueva. Desconectado no significa bloqueado.
- Reboot siempre bloqueado y autorización nueva para ese boot. Administración RAM
  cierra al salir/fondo/proceso sin revocar por ello uso comprometido; caducidad breve
  aún por acordar. Emergencia sin código y Keyguard Android intacto.

Esto sustituye recomendaciones HIJO-C03 de aleatoriedad obligatoria, tiempo por
defecto, lista aprobada y bloqueo REMOTO por pérdida de red. No cambia el código CP/1.

Tres autoridades diferentes, nunca intercambiables:

1. **Sesión administrativa local RAM:** permite ver/solicitar controles tras verificar
   credencial; entrar NO autoriza uso normal ni redes/retirada por sí mismo.
2. **Autorización normal:** decisión explícita local o remota ligada a boot, revisión
   de control y plazo. No concede interfaz administrativa al niño.
3. **Recuperación limitada:** MAINTENANCE/RECOVER_PARENT/RETIRE actuales mantienen
   consumo único y alcance. No convertir mantenimiento o código impreso CP/1 en
   desbloqueo normal reutilizable. Rotación/olvido de credencial requieren diseño propio.

La respuesta a pregunta solo concede una capacidad RAM de CAMBIAR_CODIGO, no
ABIERTO_RAM administrativo o permiso normal. No imponer otro código de recuperación
como sustituto/condición de esta pregunta. Códigos CP/1 siguen rutas separadas.

El cambio contradice ALLOW offline prohibido en CP/1 y ADR-H-002 (red/canal bloquean
siempre). No introducir excepción privada ni activar un modo CP/1 con nueva semántica.
Lo probado en H05-L01 sigue válido solo para recuperación aislada, no para este uso.

## 2. Modelo de estados/acciones/eventos propuesto

Nombres descriptivos internos, NO campos/enums nuevos aprobados del protocolo.
Estado de uso: BLOQUEADO / PERMITIDO_LOCAL / PERMITIDO_REMOTO / ERROR_SEGURO.
Estado administrativo independiente: CERRADO / ESPERA / ABIERTO_RAM.
Existe además entrada pública mínima de HOME/quiosco: emergencia y acceso adulto;
ningún control, estado sensible o datos de vínculo sin autenticación. Una app HOME
debe poder arrancar sin código para ofrecer emergencia: proteger controles, no
pedir contraseña antes de dibujar toda la actividad o antes de ir a Keyguard.
La entrada pública de recuperación puede mostrar la pregunta personalizada sin
código previo, pero nunca respuesta, controles o secretos; si se aplica espera,
la pregunta visible no permite eludirla.

| Evento | Administración | Uso efectivo / acción propuesta |
|---|---|---|
| Abrir controles sin sesión | Pedir credencial salvo espera vigente | No cambiar permiso normal; emergencia sigue directa |
| Credencial correcta + commit de contador/readback | Abrir RAM con nueva referencia y plazo | Seguir bloqueado/permitido como antes; autenticar no desbloquea |
| Credencial incorrecta/espera | Cerrar o impedir entrada; persistir fallo antes de devolver resultado | No revocar permiso normal vigente por intentos del niño; no bloquear emergencia |
| Adulto pulsa bloquear con autoridad vigente | Revalidar ticket/contexto justo antes de transacción | Nueva revisión, revocar permiso normal y temporizador anterior; comprobar enforcement real |
| Adulto confirma permitir local con duración | Revalidar credencial/sesión/boot/revisión | Commit local de autorización para este boot; permitir solo si aplicación real comprobada; offline admitido |
| Fondo/salida/cancelación/reentrada | Cerrar RAM y diálogos/trabajos pendientes | Permiso normal ya comprometido continúa con reloj transcurrido; no devolver autorización consumida |
| Caducidad administrativa | Cerrar controles, exigir credencial otra vez | No caducar por ello el permiso normal: son plazos independientes |
| Caducidad normal vigente | Sin conceder administración | Bloquear; timer de revisión vieja no altera decisión nueva |
| Orden remota fresca y válida | No autentica controles locales; cambio invalida confirmación local pendiente | Serializar y sustituir decisión vigente según precedencia acordada; firmar ACK solo de esa orden |
| Red/canal perdidos | No abrir administración ni prolongar autoridad | LOCAL y REMOTO continúan hasta su plazo, bloqueo nuevo válido o reboot; diagnóstico desconectado separado de uso |
| Reconexión | No cambia sesión por recibir presencia | Sync nuevo, no reaplicar orden/permiso remoto viejo ni rejuvenecer tiempo |
| Muerte del proceso en mismo boot | Cerrar RAM, descartar tickets | Releer permiso normal durable y plazo original; no reiniciar cuenta. Continuidad del mismo boot debe ser comprobable |
| Boot nuevo o incierto, reloj discontinuo | Cerrar RAM/desafíos y invalidar permisos/tickets | Bloquear aun si archivo contiene ALLOW previo; nueva autorización solo para boot actual |
| Corrupción/identidad o clave de protección perdida | Error seguro, sin generar reemplazo o enrolamiento | No conceder uso/controles; recuperación autorizada y emergencia conservadas |
| Emergencia/Keyguard | Cancelar sesión administrativa/entrada sensible | Ruta del sistema sin código de app; no desbloquea Android ni concede administración |
| Respuesta correcta a pregunta | Solo ventana RAM acotada para establecer código nuevo | No abrir apps automáticamente ni recuperar administración permanente |
| Cambio de código confirmado y readback | Retirar sesiones/tickets y capacidad de recuperación | Propuesta: revocar permiso normal previo y quedar bloqueado; nuevo uso exige otra acción explícita |
| LOCK_FOR vence | No conceder administración | Apertura diferida solo si sigue vigente la misma revisión/boot y fue explícitamente autorizada; otra acción/cambio de credencial/reboot invalida el timer |
| Dos acciones conflictivas | Invalidar confirmación basada en revisión vieja | La segunda debe sincronizar/revisar y confirmar otra vez; no refirmar automáticamente |

Recomendación sobre emergencia: salir a Keyguard no revoca por sí solo permiso de
uso ya confirmado; suspender no pausa su reloj. Al volver a controles pedir código.
No se afirma que estas rutas funcionen hoy bajo quiosco o que bloqueo se aplique
antes de que Android ejecute la app: Direct Boot/servicio/escapes requieren pruebas.

## 3. Código elegido, pregunta y protección propuesta

«Código maestro» será un **código adulto local reutilizable de esta instalación**,
nunca valor universal, constante en APK, clave del padre o secreto compartido con relay.
Propuesta: crear solo en preparación presencial autorizada, antes de endurecer;
vincular verificador a identidad de instalación y generación de credencial.
No crear al iniciar HOME, reiniciar, detectar ausencia de archivo o recibir una orden.
La política de uso antes de vincular sigue pendiente; recomendar mantener preparación
cerrada al uso normal hasta completar configuración local segura, no conceder ALLOW
por ausencia de padre/credencial. Offline no significa configuración omitida.

Código: entrada de 5–10 dígitos elegidos por adulto, tratar como String, no entero;
propuesta de validación ASCII 0–9 exacta, sin trim, conversión numérica o equivalencia
con dígitos Unicode. Confirmar dos entradas iguales al establecerlo. No exigir
longitud fija, generación aleatoria, código extra, biometría o regla oculta nueva.
No imprimir ejemplos de códigos/ respuestas reales ni conservarlos en Bundle/archivo.

Pregunta **visible y no secreta**, personalizada, nunca una lista obligatoria;
respuesta **secreta**, no se muestra como pista ni se revela al recuperar. Propuesta
de límites por acordar: pregunta 1–200 puntos de código y hasta 800 bytes UTF-8;
respuesta 1–256 puntos y hasta 1024 bytes. Rechazar vacío/solo espacios, controles
y saltos de línea. Mostrar pregunta no concede autoridad. No enviarla al padre/relay:
su publicidad local no justifica recopilar información personal.

Normalización de respuesta propuesta: Unicode NFC y quitar solo espacios U+0020
externos; conservar mayúsculas, acentos y espacios internos. Aplicar exactamente
igual al alta/verificación y versionar la regla; doble entrada/explicación al adulto.
Alternativa más tolerante (case folding/otros espacios) debe acordarse y probarse,
no introducirla silenciosamente: facilita adivinación y cambia equivalencias.
Pregunta puede normalizarse para presentación al configurarla, sin cambiar una
respuesta existente. No usar datos de historial, ubicación o actividad para preguntas.

**Riesgos aceptables solo tras revisión:** cinco dígitos tienen 100000 combinaciones
posibles y elección humana no es uniforme; ceros iniciales no añaden seguridad por
sí solos. Respuesta deducible/conocida por niño permite cambiar código. Pregunta
personalizada no prueba identidad real ni alta entropía; no prometer seguridad de
128 bits, imposibilidad de fuerza bruta o antirollback por un KDF/espera.

Propuesta criptográfica privada a revisar: KDF con salt aleatorio por credencial,
parámetros versionados/acotados, luego verificador autenticado con secreto no
exportable distinto de la identidad de firma. Preferir Argon2id si dependencia y
coste Android se validan; alternativa PBKDF2-HMAC-SHA256 con coste calibrado y
mínimo de seguridad acordado. No fijar cifras sin evaluar consumo/latencia del A13.
Dominios/salts/verificadores distintos para código y respuesta, separados del hash
CPv1/RECOVERY; versiones para KDF, normalización y protección. Comparación constante;
límites de entrada antes del KDF. Salt aleatorio NO implica código aleatorio obligatorio.
No basta SHA-256 rápido de una contraseña humana o cifrado reversible del texto.

Guardar pregunta pública y solo salt/parámetros/verificadores/generación y contador/espera en registro
privado íntegro, autenticado/cifrado con clave de protección; no password claro,
bytes derivados en logs o copia de secreto en estado UI. Una transacción serial
para credencial, intentos y autorizaciones; no separar contador del commit.
Definir disponibilidad antes del primer desbloqueo: bloquear por boot sin usar
privadas, no verificar código con clave inexistente. Arquitectura de almacenamiento
DP/CE y claves/migración se debe diseñar/probar aparte; AtomicFile+checksum actuales
no equivalen a autenticidad frente a manipulación o rollback.

Propuesta pendiente para ambas vías: cupo/contador agregado de fallo código+respuesta
y contadores por vía para diagnóstico local, actualización serial durable antes de
responder. Cinco fallos agregados → 60 s; diez → 300 s, saturación y espera máxima
acotada. Alternar formularios no reinicia cupo; ambas entradas obedecen espera
agregada. Reinicio/proceso/contexto incierto reimponen espera pendiente completa;
hora civil, cambio de pregunta/código o reentrada no la borran como atajo.
Éxito a pregunta no limpia intentos del código; ventana de cambio no permite intentos
extra de respuesta. Propuesta: reset agregado solo tras autenticación de código
correcto fuera de espera, con readback; el reset por respuesta/cambio de código
se debe acordar expresamente, por defecto no. Nunca resetear consumos/esperas CP/1.
Fallo de persistencia no concede administración/cambio. Espera nunca afecta emergencia
o remoto legítimo; permiso normal ya comprometido no se revoca por atacar formularios.
Hay riesgo de denegación administrativa local; si pregunta/código son ambos olvidados,
no inventar una vía universal para eludirla. Política concreta aún no aprobada.
Propuesta anti-carreras: una operación de verificación admitida a la vez, cola
acotada, snapshot de generación/boot/referencia antes del KDF y revalidación al
commit. No resultados de KDF sobre código/respuesta anteriores tras una rotación.
Descontar intentos/registrar obligación durable según política acordada, sin que
cancelar trabajo o alternar formulario revierta fallos ya comprometidos. Límite
de recursos/KDF no significa protección frente a root o rollback de archivo completo.

## 4. Rotación, olvido y cambio de padre

Alta inicial propuesta: preparación presencial específica, estado íntegro/identidad
coherente y permiso del propietario; adulto elige código y configura pregunta/respuesta
con confirmaciones. Ningún alta automática por archivo ausente, boot o lectura QR.
Definir procedencia de instalación y transacción/migración antes de UI; no fingir que
una casilla software demuestra presencia adulta en un dispositivo sin preparar.

Cambio ordinario de código o pregunta/respuesta: revalidar código actual y autoridad
sensible, generación de credencial, boot y referencia de operación al confirmar;
editar pregunta no usa solo permiso normal o sesión vieja. No revelar respuesta previa.
Propuesta para quien olvidó código: verificar respuesta offline crea capacidad RAM
solo para establecer un código nuevo, ligada a generación/boot/ticket y plazo breve.
No concede edición de pregunta/respuesta, cambio de padre, aplicaciones o controles
generales. Consumir ticket al confirmar, no generar administrador permanente.

Propuesta de commit/readback: registro con nuevos verificadores/generación y revisión
de control, invalidación de desafíos/sesiones/permiso normal y apertura LOCK_FOR
previa en una transacción serial. Normal queda bloqueado tras cambio; iniciar uso
requiere autenticar nuevo código y elegir/confirmar duración aparte. Conservar pregunta/
respuesta si solo se recupera código, identidad, seq remoto y consumos/esperas CP/1.
No destruir registro viejo antes del commit ni usar reset de credencial para ocultar
intentos pendientes. Verificadores no salen de almacenamiento protegido local.

Fallos/crash: antes del commit no hay cambio ni apps; después, anterior o nuevo
completo según atomicidad/readback. Escritura incierta no muestra éxito; cerrar tickets
y autoridad hasta relectura coherente. Cancelación posterior no deshace cambio durable
ni revive permiso/código/timer anterior. RAM perdida no reanuda capacidad de reset.
Fondo/reentrada/caducidad/cambio remoto/otra generación invalidan confirmación pendiente;
no reiniciar KDF o refirmar acción automáticamente con nueva referencia para ganar carrera.

Si se olvida respuesta además del código: no enrolamiento libre, regeneración,
«pregunta alternativa», código universal o borrado automático. Detener edición y
conservar datos; control remoto ya autorizado puede seguir según contexto válido,
pero no le concede de forma implícita reset de secretos locales. Se requiere una
decisión específica del propietario sobre salida presencial/retirada autorizada,
sin ampliar ahora protocolo ni imponer otro código. Identidad/clave de protección
perdida o corrupción no se repara verificando una respuesta contra datos no confiables.

Revinculación: RECOVER_PARENT conserva su propósito/consumo y vínculo anterior hasta
commit. Recomendación: requerir decisión explícita de rotar credencial local en la
ceremonia de cambio de adulto; no transmitirla al nuevo padre, derivarla de su
pública o tratar cambio de relay como recuperación. Identidad/clave de protección
perdida no habilitan estos flujos libremente: acordar qué prueba independiente
permite recuperación y qué casos solo admiten retirada autorizada.

## 5. Decisiones no confirmadas: alternativas recomendadas

| Tema | Opciones | Recomendación a revisar |
|---|---|---|
| Duración de uso | Tiempo LOCAL elegido cada vez confirmado; falta rango/granularidad | Proponer 60–86400 s, sin preselección/default y sin uso indefinido oculto; revisar también ALLOW/ALLOW_FOR remotos en nueva versión |
| Caducidad administrativa/reset | Breve RAM, cierre al salir/fondo/proceso confirmado; no plazo aprobado | Proponer dos minutos absolutos sin renovar por actividad; alternativa una sola acción. Reset solo ticket de cambiar código, nunca sesión admin |
| Pregunta/respuesta | Personalizadas confirmadas; normalización/límites pendientes | NFC, espacios U+0020 externos, caso/acentos conservados, límites de §3; doble entrada y explicación |
| Intentos/KDF | Ambas vías protegidas y datos locales; parámetros pendientes | Cupo agregado y por vía de §3, KDF versionado/calibrado después; no presumir coste/seguridad física |
| LOCK_FOR | Control temporal remoto existente; futura apertura debe respetar revisión | Proponer que su expiración abra solo un intervalo finito elegido/confirmado al crear acción y limitado al mismo boot; rango y representación requieren acuerdo |
| Cambio de credencial | Solo nuevo código vía pregunta confirmado; efecto sobre uso pendiente | Recomendación: cerrar autoridad y revocar permiso/timers previos atómicamente; no desbloqueo automático |

Precedencia confirmada con mecanismo propuesto: cada decisión aceptada cambia revisión de control
durable global; no reloj civil para ordenar dos emisores. Confirmación local se
liga a snapshot/revisión/ticket. Orden remota debe traer desafío fresco asociado
a esa revisión; si cambió mientras viajaba, rechazar sin sobrescribir local, pedir
sync y nueva confirmación. No ignorar LOCK remoto fresco por estar offline antes:
al recibirlo válido, es acción nueva, no mera reconexión. Las dos autoridades
deben revisar esta regla; alternativa de remoto soberano contradice la capacidad
local de volver a desbloquear si se convierte en bloqueo irrebasable.

Aceptar acción local/remota revoca desafíos/confirmaciones y todos los timers de
revisión reemplazada, incluida apertura diferida LOCK_FOR. Segunda acción conflictiva
requiere snapshot/sync y confirmación nuevos, nunca refirmado automático. Un cambio
temporal natural solo puede ejecutar su transición previamente confirmada si boot/
revisión siguen vigentes; proponer nueva revisión al materializarla para invalidar
confirmaciones concurrentes. Secuencia de padre permanece exclusiva de órdenes remotas y nunca
se reinicia por acción local/boot/rotación; revisión global es otra magnitud.
Duplicado exacto remoto devuelve evidencia de esa orden sin reaplicar ni extender
duración aunque hoy mande una acción local posterior. Nuevo seq pero contexto
viejo tampoco gana. Caída de red/conexión y timer viejo no revocan ni rejuvenecen
permiso LOCAL o REMOTO. Error real de integridad/enforcement sí puede bloquear con causa
explícita: no llamarlo NETWORK_LOST para esconder un conflicto de requisitos.

Proceso en mismo boot: recomendación, permiso normal durable sobrevive sin reset de
tiempo; autoridad administrativa no. Boot nuevo/continuidad no comprobable bloquea
y rota contexto aunque bytes del permiso previo sigan guardados. Antes de aceptar
acción nueva verificar BootContextRepository/reloj; no basta comparación de fecha.
Reloj monotónico transcurrido incluye pantalla apagada; mismo boot comprobado exige
contexto/BOOT_COUNT/continuidad íntegros. Hora civil no resuelve discontinuidad.
Reboot cancela tanto permiso activo como apertura diferida; orden duradera previa
puede conservarse como evidencia remota incierta, nunca como autorización nueva.

### Todas las apps normales: requisito y límite técnico pendiente

No sustituir por lista parental aprobada. «Normal» necesita delimitar superficies
de administración del sistema sin restringir arbitrariamente apps de uso. Android
Device Owner/Lock Task tiene capacidades/listas de paquetes: evaluar si un control
global puede permitir todo uso normal y aún bloquear realmente al vencer tiempo.
Posibles escapes: HOME/launchers, Ajustes/admin, instaladores y apps nuevas,
navegadores/intents, notificaciones, multitarea, ventanas/PiP, perfiles, pantallas
de sistema, suspensión/proceso muerto y cambios de paquete. No usar APIs privadas,
accesibilidad/overlays o inventar universalidad para ocultar límites.
Si una lista técnica de paquetes resulta necesaria, no convertirla en aprobación
parental: propuesta solo incluiría todas las apps normales dinámicamente y requeriría
validar carreras/instalación y que no exista escape. Si APIs oficiales no permiten
garantía solicitada, reportar incompatibilidad al propietario antes de código/quiosco;
no afirmar que la viabilidad está probada o volver silenciosamente a lista elegida.

## 6. STATE/ACK/reconciliación y cambio mínimo coordinado

**Conceptos de contrato a proponer, no nombres de campos ni schema aprobados:**
STATE debe distinguir modo efectivo/cause, origen local/remoto de autorización,
boot, revisión de control y vigencia actual, de última orden remota aceptada
(seq/id/hash/resultado de aplicación de esa orden). No publicar credencial,
salt/verificadores/generación secreta, respuesta/pregunta personal, intentos o código
de recuperación al relay/padre/QR/exportación/logs. Pregunta solo visible localmente.
SET_POLICY/contexto de sync deben ligar revisión/desafío para evitar orden obsoleta
tras decisión local. ACK solo corresponde a orden remota identificada y contexto;
no emitir ACK inventado ni seq padre por pulsar control local.

Acción local guarda intención/resultado real como registro vigente mínimo, no
historial: solicitado/pendiente/fallo/efectivo separados. Fuente local no significa
APPLIED. No firmar éxito de enforcement si solo se escribió intención o se llamó
API; verificar capacidades/resultado con evidencia apropiada. Enviar STATE nuevo
al reconectar con contexto actual, no cola de eventos locales ni reenviar decisiones.

Padre debe reconciliar pendiente remoto contra última orden correlacionada y
mostrar aparte «modo actual por decisión local posterior». Un ACK auténtico tardío
de una orden aplicada antes no prueba que su modo siga vigente. Resultado remoto
desconocido no se rellena APPLIED a partir de modo local o revisión mayor. Conservar
contador/reservas/pending/incertidumbre y pedir sync fresco; no reproducir orden
previa automáticamente. ACK antiguo no reemplaza STATE posterior. Diagnóstico de
desconexión no equivale a modo bloqueado o permiso revocado. Una orden correlacionada
puede tener resultado conocido aunque su efecto vigente fue sustituido por LOCAL.

Cambios mínimos por rol tras acuerdo:

- Hijo: motor con fuentes de autorización y reboot, credencial/administración,
  transacción revisión/permiso/orden remota, timers/challenges, proyección protegida
  y reporte firmado. Recuperación/emergencia siguen rutas independientes.
- Padre: parser/modelo nuevos, revisión en confirmación/órdenes, presentación de
  procedencia/cambio local y reconciliación sin replay; nueva ceremonia/migración.
  No almacenar código maestro ni usarlo como autoridad parental remota.
- Relay: versión/subprotocolo acordados y validadores/routing de sobres nuevos;
  solo públicas, roles y bytes. Sin endpoint/estado de credencial, lógica de permiso
  o APPLIED. No adaptar CP/1 aceptando campos desconocidos o mezclar versiones.

Proponer ADR conjunta de control normal local que revisa ADR-H-002 y extiende
ADR-H-005 (escritor/revisión/boot). Acordar versión/subprotocolo/dominios de firma,
campos exactos, causas/errores, límites, esquema/fixtures positivos y negativos y
matriz de interoperabilidad. Mantener P-256/privadas no exportables si no hay motivo
para cambiarlas; no asumir que se firma versión nueva con dominio CPv1. Nombre CP/2
no cierra esos detalles. Recibos exactos de pairing/recuperación no son requeridos
por este cambio y siguen propuesta distinta, no añadida automáticamente.

## 7. Migración y supuestos anteriores que cambian

CP/1 debe seguir interpretándose como CP/1, sin nuevo uso normal offline. Propuesta:
migración física explícita a nueva versión, nunca automática al leer un STATE.
Conservar identidad/vínculo/contador/consumos/esperas y estado incierto; nuevo formato
privado autenticado por diseñar, readback/migración atómica y errores conservadores.
No fabricar credencial/permiso a partir de hash CP/1, pública, fixture o label
VINCULADO. Adulto elige código/configura pregunta-respuesta mediante ceremonia autorizada y la instalación
queda bloqueada hasta concluir. No revivir permiso/orden vieja tras migración.

No rollback que restaure credencial/esperas/códigos consumidos: probar actualización
y downgrade; impedir o fallar cerrado ante formato/versión no soportados, no borrar
para «arreglar». Copia íntegra antigua/rollback adversarial requiere análisis propio:
sin contador hardware no prometer protección absoluta. No backups de secretos.
Peers incompatibles: error de versión y permiso local solo si nueva versión acordada
está íntegra y configurada; no conectar como CP/1 para ocultar incompatibilidad.

HIJO-C02 y PADRE-C01 siguen útiles para comparación pública y límites de STATE,
pero «sin internet bloqueado» ya no vale para fuente LOCAL ni REMOTO con permiso
vigente en versión propuesta. No introducir esta semántica en los peers CP/1.
MAINTENANCE y ceremonia CP/1 no acreditan permiso normal local ni su credencial.
Resumen público sigue sin secretos; dato de versión/capacidad que deba comparar
adulto requiere ceremonia nueva acordada, no recibo remoto inventado.
H05-L01/PADRE-C02 no demuestran esta función. Puertas A/B/C1/C2 pendientes deben
incluir credencial/olvido/offline/reboot/precedencia y revisar dependencia circular;
no promover H05/H08 o adelantar quiosco por añadir un método local de desbloqueo.

## 8. Amenazas, pruebas futuras y subunidades

Amenazas: niño observa/adivina código, teclado/clipboard/captura, reentrada a
controles, intents externos, carreras de IO/timer/remoto, crash/boot/rollback,
QR equivocado, padre antiguo, red intermitente y corrupción. Cinco dígitos o respuesta
deducible hacen débiles las dos vías; no prometer entropía alta. Ocultar entrada/
FLAG_SECURE no garantiza borrado de memoria
o teclado confiable. Rate limit permite denegación administrativa, no debe cortar
emergencia ni remoto legítimo. Root/firmware alterado/rollback de claves fuera de
garantía absoluta; validar límites del A13, no prometer iPhone/universalidad.

Pruebas propuestas, **no ejecutadas**:

- Máquina pura: abrir controles no autoriza apps; permitir apps no abre controles;
  fondo/caducidad cierran RAM, permiso normal sigue; bloqueo/boot/revisión nueva
  revocan permisos anteriores; pantalla apagada no pausa/reinicia timer.
- Offline: red/canal perdidos no revocan LOCAL o REMOTO; reconexión sin orden nueva
  no cambia permisos; LOCK fresco y permitir local posterior obedecen precedencia;
  timer/seq/nonce viejos, incluida apertura LOCK_FOR, no resucitan políticas.
- Credencial: código 5–10 como cadena/ceros iniciales, límites/normalización pregunta-
  respuesta, salt/generación/comparación y entrada acotada, fallo KDF/
  clave/escritura sin acceso; intentos/espera resistentes a proceso/boot, independencia
  de códigos únicos y emergencia; alternancia no reinicia cupo; reset por respuesta
  solo cambia código, no abre apps/admin ni revela anterior. Rotación/pregunta,
  crash/carreras/reentrada/fondo y respuesta olvidada sin sustitución libre.
- Protocolo: STATE separa modo vigente y orden remota, ACK tardío no implica modo
  actual; discrepancia de revisión, firmas/roles/versiones cruzados y replay rechazados;
  padre/relay no reciben credencial/verificador. Migración negativa conserva seq/consumo.
- Android/físico por autorización posterior: Keyguard/emergencia, UI/ciclo de vida,
  Keystore/almacenamiento, reboot previo al PIN, duraciones acordadas/suspensión reales,
  enforcement, recuperación de adulto y actualización; repetir bajo quiosco gradual.

Orden propuesto, sujeto a asignaciones separadas: revisión padre/decisiones abiertas
→ ADR/versionado acordados de tres roles y puertas/plan autorizados → modelos/KDF/
persistencia/migración y pruebas puras → identidad/credencial/pairing y herramientas
en laboratorio autorizado → evidencia real emergencia/recuperación/control local
antes de H06 → quiosco gradual/regresión → WSS/STATE/reconciliación según dependencias
revisadas → acreditación física y aceptación final. No saltar H08/H10/H11 ni reducir
criterios físicos; una subunidad no equivale a tarea terminada.

Defecto QR padre `a8e4ffc` comunicado como reproducido, NO corregido; QR02 no
asignado. Ceremonia/pairing ópticos siguen bloqueados por calidad del lector y demás
dependencias; no reparar desde hijo ni declarar cruce móvil completo por simulación.

### Decisiones imprescindibles y siguiente subunidad mínima

Solicitar en bloque breve: (1) rango/granularidad de tiempo y apertura finita LOCK_FOR;
(2) caducidad admin/reset y revocación de uso tras cambiar código/pregunta;
(3) normalización/límites y política agregada de espera/reset; (4) salida si se
olvidan código y respuesta, sin autorización remota/extra/borrado implícitos;
(5) versión/ADR/contextos/migración y orden de puertas de tres roles. Recomendaciones
están en tabla/§§3–6, no son decisiones nuevas del propietario.

Siguiente subunidad mínima propuesta para revisión: especificación conjunta exacta
de revisión global, ticket/acción vigente y timers/LOCK_FOR, junto al modelo de
espera/ventana de reset, antes de autorizar modelos puros/tests. Sin contrato/schema
nuevo hasta acuerdo de versión; no nueva tarea automática. Después revisar viabilidad
Android de todas las apps y recuperar evidencia real antes de H06.

Devolución: HIJO-C04 documental consolidada; esperar acuerdo explícito de versión,
detalles/puertas antes de implementación. H05 sigue abierta/H08 pendiente; no código,
tests/builds/KDF medido o dispositivos. CP/1 y planes/ADR aceptados intactos.
