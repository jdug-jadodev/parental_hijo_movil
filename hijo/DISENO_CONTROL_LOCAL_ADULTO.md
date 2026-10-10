# HIJO-C03 — control normal local del adulto y versión propuesta

Fecha: 2026-10-10. Base hijo: `cb62a73` / H05-L01 `c7fb842`.
**Solo diseño. Requisitos confirmados; alternativas y recomendaciones pendientes
de acuerdo. CP/2 es nombre provisional, no versión aprobada ni implementada.**
Fuentes: propuesta/coord. central de Render en lectura; CP/1 §5/9/9.A;
HIJO-C02, diseño de acreditación padre y ADR-H-002/005/006 actuales.
No cambiar compartido, fixtures, ADR aceptados, PLAN_HIJO o código por este documento.

## 1. Requisitos y separación de autoridades

El padre conserva control remoto. Adulto entra a controles del hijo con credencial
propia y puede bloquear/desbloquear uso normal, también sin internet en el hijo.
Cada reinicio bloquea y exige autorización nueva para ese arranque. Emergencia
sin credencial de app, incluso con esperas/fallos; Keyguard Android sigue intacto.

Tres autoridades diferentes, nunca intercambiables:

1. **Sesión administrativa local RAM:** permite ver/solicitar controles tras verificar
   credencial; entrar NO autoriza uso normal ni redes/retirada por sí mismo.
2. **Autorización normal:** decisión explícita local o remota ligada a boot, revisión
   de control y plazo. No concede interfaz administrativa al niño.
3. **Recuperación limitada:** MAINTENANCE/RECOVER_PARENT/RETIRE actuales mantienen
   consumo único y alcance. No convertir mantenimiento o código impreso CP/1 en
   desbloqueo normal reutilizable. Rotación/olvido de credencial requieren diseño propio.

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
| Red/canal perdidos | No abrir administración | PERMITIDO_LOCAL continúa; PERMITIDO_REMOTO se bloquea en alternativa recomendada; sin fallback a permiso local antiguo |
| Reconexión | No cambia sesión por recibir presencia | Sync nuevo, no reaplicar orden/permiso remoto viejo ni rejuvenecer tiempo |
| Muerte del proceso en mismo boot | Cerrar RAM, descartar tickets | Releer permiso normal durable y plazo original; no reiniciar cuenta. Continuidad del mismo boot debe ser comprobable |
| Boot nuevo o incierto, reloj discontinuo | Cerrar RAM/desafíos y invalidar permisos/tickets | Bloquear aun si archivo contiene ALLOW previo; nueva autorización solo para boot actual |
| Corrupción/identidad o clave de protección perdida | Error seguro, sin generar reemplazo o enrolamiento | No conceder uso/controles; recuperación autorizada y emergencia conservadas |
| Emergencia/Keyguard | Cancelar sesión administrativa/entrada sensible | Ruta del sistema sin código de app; no desbloquea Android ni concede administración |

Recomendación sobre emergencia: salir a Keyguard no revoca por sí solo permiso de
uso ya confirmado; suspender no pausa su reloj. Al volver a controles pedir código.
No se afirma que estas rutas funcionen hoy bajo quiosco o que bloqueo se aplique
antes de que Android ejecute la app: Direct Boot/servicio/escapes requieren pruebas.

## 3. Credencial por instalación: opciones y recomendación

«Código maestro» será un **código adulto local reutilizable de esta instalación**,
nunca valor universal, constante en APK, clave del padre o secreto compartido con relay.
Propuesta: crear solo en preparación presencial autorizada, antes de endurecer;
vincular verificador a identidad de instalación y generación de credencial.
No crear al iniciar HOME, reiniciar, detectar ausencia de archivo o recibir una orden.
La política de uso antes de vincular sigue pendiente; recomendar mantener preparación
cerrada al uso normal hasta completar configuración local segura, no conceder ALLOW
por ausencia de padre/credencial. Offline no significa configuración omitida.

Opciones:

- **Código aleatorio recomendado:** 128 bits de entropía segura, Base32 de 26
  caracteres con bits finales canónicos, agrupación para copiar fuera del teléfono.
  Reutilizable hasta rotación; no los 32 caracteres/códigos únicos CP/1. Mostrar una
  vez al adulto con confirmación de copia, nunca QR/clipboard automático/log/WSS.
- Frase de contraseña elegida por adulto: más cómoda, pero entropía no garantizable;
  exige KDF resistente, política explícita y evaluación de dispositivos.
- PIN corto: cómodo pero débil ante observación/fuerza bruta; no recomendado como
  única credencial. Biometría/Keyguard pueden reforzar, no sustituir el requisito de
  entrada con código ni permitir atajo sin una decisión nueva.

Propuesta criptográfica privada a revisar: KDF con salt aleatorio por credencial,
parámetros versionados/acotados, luego verificador autenticado con secreto no
exportable distinto de la identidad de firma. Preferir Argon2id si dependencia y
coste Android se validan; alternativa PBKDF2-HMAC-SHA256 con coste calibrado y
mínimo de seguridad acordado. No fijar cifras sin evaluar consumo/latencia del A13.
Dominio propio de credencial local, distinto del hash CPv1/RECOVERY. Comparación
constante; límites de entrada antes del KDF y normalización explícita según opción.
No basta SHA-256 rápido de una contraseña humana o cifrado reversible del texto.

Guardar solo salt/parámetros/verificador/generación y contador/espera en registro
privado íntegro, autenticado/cifrado con clave de protección; no password claro,
bytes derivados en logs o copia de secreto en estado UI. Una transacción serial
para credencial, intentos y autorizaciones; no separar contador del commit.
Definir disponibilidad antes del primer desbloqueo: bloquear por boot sin usar
privadas, no verificar código con clave inexistente. Arquitectura de almacenamiento
DP/CE y claves/migración se debe diseñar/probar aparte; AtomicFile+checksum actuales
no equivalen a autenticidad frente a manipulación o rollback.

Espera propuesta independiente: cinco fallos → 60 s; diez → 300 s, acumulación
saturada y máximo de espera acotado, reimponer espera pendiente completa tras
boot/contexto incierto. No reset por proceso/hora civil/reinstalación «reparadora».
Éxito confirmado puede reiniciar contador de esta credencial; no borrar consumo/
esperas de recuperación. Persistencia fallida no abre controles. Espera limita solo
entrada local, no emergencia, canal remoto legítimo ni recuperación firmada válida.
Normal local ya permitido no cambia por ataque a formulario; denegación de servicio
administrativa sigue riesgo y exige salida de recuperación independiente.

## 4. Rotación, olvido y cambio de padre

Rotación ordinaria: credencial actual revalidada para acción sensible, confirmación
física, nueva aleatoria y copia fuera del teléfono. Commit/readback atómico de
generación/verificador; después retirar sesiones, tickets y permisos normales
anteriores conservadoramente. No destruir protección vieja antes de commit.
Fallo deja registro anterior o nuevo completo; no mantener ambos válidos indefinidamente.

Olvido: opciones pendientes, ninguna implementada:

1. Padre autenticado autoriza físicamente una **rotación local de credencial**, con
   reto firmado específico de instalación/boot/generación y confirmación en hijo;
   nuevo secreto se genera/exhibe solo en hijo. No mandar password por WSS ni usar
   MAINTENANCE como autoridad de rotación. Requiere propósito/acción definidos en
   versión nueva, no otro significado escondido en OFFLINE CP/1.
2. Recomendación complementaria cuando no hay padre: código único dedicado a
   recuperar credencial local, independiente de los tres existentes, alta entropía,
   copiado presencialmente. Consumo durable antes de ventana de rotación; perder
   proceso no devuelve código ni crea sesión persistente de administrador.
   Definir su propósito/formato en nueva versión y almacenamiento local privado.
3. Si todas las autorizaciones se perdieron: no hay puerta maestra ni reemplazo
   automático; retirada/restablecimiento presencial según vía aún disponible y
   permiso destructivo específico, sin evitar FRP. No ejecutar como solución aquí.

Revinculación: RECOVER_PARENT conserva su propósito/consumo y vínculo anterior hasta
commit. Recomendación: requerir decisión explícita de rotar credencial local en la
ceremonia de cambio de adulto; no transmitirla al nuevo padre, derivarla de su
pública o tratar cambio de relay como recuperación. Identidad/clave de protección
perdida no habilitan estos flujos libremente: acordar qué prueba independiente
permite recuperación y qué casos solo admiten retirada autorizada.

## 5. Decisiones no confirmadas: alternativas recomendadas

| Tema | Opciones | Recomendación a revisar |
|---|---|---|
| Uso local | Hasta próximo bloqueo/boot; duración elegida; máximo diario | Duración explícita por acción, default propuesto 30 min, rango a acordar; ofrecer hasta boot solo si adulto lo aprueba, nunca por defecto oculto |
| Administración | Solo una acción; sesión corta; sesión larga persistente | RAM hasta cinco minutos absolutos, cierre inmediato al salir/fondo; revalidar credencial para rotación/recuperación sensible; no renovar por actividad |
| Permiso ante red perdida | Todo bloquea; ambos permisos offline; solo local offline | Solo fuente local conserva uso offline; conserva comportamiento remoto de red/canal salvo futura decisión. Si se quiere remoto offline, definirlo antes de implementar |
| Conflicto de órdenes | Remoto siempre gana; bloqueo siempre gana; última decisión válida gana | Última acción explícita con contexto fresco en escritor único gana; bloqueo remoto fresco revoca local, adulto local puede después permitir; no prohibir silenciosamente una de las dos autoridades |
| Aplicación que no puede aislarse | Permitir cualquiera; lista probada | Uso normal solo de apps aprobadas y capaces de mantenerse bajo control; no prometer todas las apps |

Precedencia recomendada precisa: cada decisión aceptada cambia revisión de control
durable global; no reloj civil para ordenar dos emisores. Confirmación local se
liga a snapshot/revisión/ticket. Orden remota debe traer desafío fresco asociado
a esa revisión; si cambió mientras viajaba, rechazar sin sobrescribir local, pedir
sync y nueva confirmación. No ignorar LOCK remoto fresco por estar offline antes:
al recibirlo válido, es acción nueva, no mera reconexión. Las dos autoridades
deben revisar esta regla; alternativa de remoto soberano contradice la capacidad
local de volver a desbloquear si se convierte en bloqueo irrebasable.

Aceptar acción local revoca desafío remoto anterior y temporizador de política
reemplazada. Secuencia de padre permanece exclusiva de órdenes remotas y nunca
se reinicia por acción local/boot/rotación; revisión global es otra magnitud.
Duplicado exacto remoto devuelve evidencia de esa orden sin reaplicar ni extender
duración aunque hoy mande una acción local posterior. Nuevo seq pero contexto
viejo tampoco gana. Caída de red/conexión y timer viejo no revocan ni rejuvenecen
permiso local. Error real de integridad/enforcement sí puede bloquear con causa
explícita: no llamarlo NETWORK_LOST para esconder un conflicto de requisitos.

Proceso en mismo boot: recomendación, permiso normal durable sobrevive sin reset de
tiempo; autoridad administrativa no. Boot nuevo/continuidad no comprobable bloquea
y rota contexto aunque bytes del permiso previo sigan guardados. Antes de aceptar
acción nueva verificar BootContextRepository/reloj; no basta comparación de fecha.

## 6. STATE/ACK/reconciliación y cambio mínimo coordinado

**Conceptos de contrato a proponer, no nombres de campos ni schema aprobados:**
STATE debe distinguir modo efectivo/cause, origen local/remoto de autorización,
revisión de control y vigencia en boot actual, de última orden remota aceptada
(seq/id/hash/resultado de aplicación de esa orden). No publicar credencial,
verificador/generación secreta, intentos o código de recuperación al relay/padre.
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
contador/pending y pedir sync fresco; no reproducir orden previa automáticamente.

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

CP/1 debe seguir interpretándose como CP/1, sin uso local normal offline. Propuesta:
migración física explícita a nueva versión, nunca automática al leer un STATE.
Conservar identidad/vínculo/contador/consumos/esperas y estado incierto; nuevo formato
privado autenticado por diseñar, readback/migración atómica y errores conservadores.
No fabricar credencial/permiso a partir de hash CP/1, pública, fixture o label
VINCULADO. Adulto crea/copía credencial mediante ceremonia autorizada y la instalación
queda bloqueada hasta concluir. No revivir permiso/orden vieja tras migración.

No rollback que restaure credencial/esperas/códigos consumidos: probar actualización
y downgrade; impedir o fallar cerrado ante formato/versión no soportados, no borrar
para «arreglar». Copia íntegra antigua/rollback adversarial requiere análisis propio:
sin contador hardware no prometer protección absoluta. No backups de secretos.
Peers incompatibles: error de versión y permiso local solo si nueva versión acordada
está íntegra y configurada; no conectar como CP/1 para ocultar incompatibilidad.

HIJO-C02 y PADRE-C01 siguen útiles para comparación pública y límites de STATE,
pero «sin internet bloqueado» ya no vale para fuente local en versión propuesta.
MAINTENANCE y ceremonia CP/1 no acreditan permiso normal local ni su credencial.
Resumen público sigue sin secretos; dato de versión/capacidad que deba comparar
adulto requiere ceremonia nueva acordada, no recibo remoto inventado.
H05-L01/PADRE-C02 no demuestran esta función. Puertas A/B/C1/C2 pendientes deben
incluir credencial/olvido/offline/reboot/precedencia y revisar dependencia circular;
no promover H05/H08 o adelantar quiosco por añadir un método local de desbloqueo.

## 8. Amenazas, pruebas futuras y subunidades

Amenazas: niño observa/adivina código, teclado/clipboard/captura, reentrada a
controles, intents externos, carreras de IO/timer/remoto, crash/boot/rollback,
QR equivocado, padre antiguo, red intermitente y corrupción. Código de alta entropía
no evita observación; ocultar entrada/FLAG_SECURE no garantiza borrado de memoria
o teclado confiable. Rate limit permite denegación administrativa, no debe cortar
emergencia ni remoto legítimo. Root/firmware alterado/rollback de claves fuera de
garantía absoluta; validar límites del A13, no prometer iPhone/universalidad.

Pruebas propuestas, **no ejecutadas**:

- Máquina pura: abrir controles no autoriza apps; permitir apps no abre controles;
  fondo/caducidad cierran RAM, permiso normal sigue; bloqueo/boot/revisión nueva
  revocan permisos anteriores; pantalla apagada no pausa/reinicia timer.
- Offline: red/canal perdidos no revocan LOCAL, remoto respeta regla elegida;
  reconexión sin orden nueva no cambia LOCAL; LOCK fresco y permitir local posterior
  obedecen precedencia; timer/seq/nonce viejos no resucitan políticas.
- Credencial: salt/entropía/generación/comparación y entrada acotada, fallo KDF/
  clave/escritura sin acceso; intentos/espera resistentes a proceso/boot, independencia
  de códigos únicos y emergencia; rotation/olvido crash-safe sin sustitución libre.
- Protocolo: STATE separa modo vigente y orden remota, ACK tardío no implica modo
  actual; discrepancia de revisión, firmas/roles/versiones cruzados y replay rechazados;
  padre/relay no reciben credencial/verificador. Migración negativa conserva seq/consumo.
- Android/físico por autorización posterior: Keyguard/emergencia, UI/ciclo de vida,
  Keystore/almacenamiento, reboot previo al PIN, cinco minutos/suspensión reales,
  enforcement, recuperación de adulto y actualización; repetir bajo quiosco gradual.

Orden propuesto, sujeto a asignaciones separadas: revisión padre/decisiones abiertas
→ ADR/versionado acordados de tres roles y puertas/plan autorizados → modelos/KDF/
persistencia/migración y pruebas puras → identidad/credencial/pairing y herramientas
en laboratorio autorizado → evidencia real emergencia/recuperación/control local
antes de H06 → quiosco gradual/regresión → WSS/STATE/reconciliación según dependencias
revisadas → acreditación física y aceptación final. No saltar H08/H10/H11 ni reducir
criterios físicos; una subunidad no equivale a tarea terminada.

Devolución: esperar revisión padre y acuerdo explícito de versión/decisiones/puertas.
No código nuevo, tests/builds o dispositivos en HIJO-C03; CP/1 continúa congelado.
