# HIJO-C08 — comprobación final PADRE-C04-R1

2026-10-13. Base hijo `8b25c24`; fuente R1 `75eb56e` §§1–14 **íntegros suministrados
en siete partes**, no archivo externo leído/verificado aquí. Sin reintento externo.

## Veredicto: APTO_PARA_GENERAR_CONTRATO_SEPARADO

No se encontró error material de especificación que requiera bloquear este gate.
No se propone cambio de campo o reinterpretación funcional. La preparación separada
CP/2/schema/validadores/pruebas locales está autorizada por propietario **después
de este gate y mediante asignación del coordinador**; nada de ello ejecutado en C08.
CP/1 sigue congelado. APTO no es codec correcto, operación pública, alta/recuperación/
migración física, H05/H06/puertas, push o enforcement acreditados.

## Comprobación concreta

| Bloque R1 | Resultado de revisión |
|---|---|
| §§2–3 bytes/cripto | Coherente: v2/socket/dominio/propósito separados, SHA256 sobre payload original, DER8..72/r,s válidos/SPKI P256, objetos cerrados/base64 canónico. No reserializar antes de firma ni pasar DTO sin autorizar al enforcer. Requiere codec v2 separado, no cambiar CriptografiaCp1/JsonCp1 |
| §§3–5 acciones/contexto | LOCK/ALLOW/ALLOW_FOR ambas autoridades; 60..86400 segundos múltiplo60, selección explícita. Campo/fase/acción retirados rechazados, sin apertura. Consolidar boot/revocación/expiración antes de CAS/sync evita nonce vivo sobre permiso agotado. Revisiones/seq no intercambiables; conflicto confirma otra vez |
| §§5–7 resultado/STATE | Aceptación/readback no APPLIED. Duplicado exacto solo último resultado/contexto retenido, no reaplica. lastRemote conserva contexto original; ACK viejo nunca reemplaza STATE ni total. stateSeq por sesión, sync/frescura20s no se renuevan por PONG/ACK/STATE espontáneo |
| §6 effective | Combinaciones cerradas coherentes: LOCK sin plazo, ALLOW libre sin deadline, ALLOW_FOR finito/EXPIRED, NONE al boot sin permiso. phaseStarted=accepted, source/policyRevision y nulos consistentes. Keyguard no revoca fase/contador y no acredita bloqueo parental |
| §§8.1–8.2 contador | Autorización transcurrida desde aceptación durable, apagado/Keyguard/offline incluidos; no duration futura/UsageStats/humano. Unión/cursor en escritor único, reconstrucción solo con continuidad demostrada, boot/gap degradan; L02 no se promueve |
| §§8.2–8.3 retención | days único max8/orden/fechas válidas, null no cero, total absoluto de ventana no vitalicio. quality y calendarQuality/nulos definidos; boundary PARTIAL por recorte civil no basta para inferir gap/EXACT. Receptor no deduce exactitud por firma. UNKNOWN calendario exige days=[]/total null/lastExpiry null |
| §8.3 versiones/barrera | retentionSeq/historyRevision durables, UUID no ordenable, misma revisión implica mismo contenido contable salvo through. Purga no toca autoridad/seq; cursor activo no reintroduce prefijo. Catch-up/readback antes de exponer, tombstone padre independiente offline, sesiones/caché/backup/push no reinician máximos |
| §§8.3–8.4 calendario/aviso | Domingo21 local configurado y DST determinista (primera ocurrencia/primer instante válido posterior) técnicamente coherentes con decisión del dueño. Catch-up y purga UNKNOWN no cancelan permiso. LastExpiry solo ALLOW_FOR retenible, deadline/detección separados; aviso anterior no modo actual ni APPLIED |
| §8.5 push | CONSULT_STATE solo hint, no nuevo frame/routing/token/endpoint. Factual requiere STATE CHILD fresco; no órdenes desde hint ni historia/proveedor/servicio activado. Emisor/permisos/privacidad/recepción quedan unidad posterior |
| §10 alta/offline/migración | Formatos cerrados/propósitos locales y procedencia de hashes coherentes; ratificación de formatos para preparar contrato, **no herramientas implementadas o migración habilitada**. Detalles abajo |

### §10 ratificado para especificación separada, no operación

- CHILD_OFFER local no firmado, v/protocol explícitos, pública/enrollId/nonce ligados
  a preparación autorizada/RAM5min; ausencia/corrupción no reabre enrolamiento.
- PAIR_ACCEPT PARENT/CPv2/PAIR liga oferta viva/ambas públicas/pairId/origen/hashes
  cerrados y confirmación humana/readback. Clave propuesta se admite solo en alta
  autorizada, no como sustitución en órdenes; firma no demuestra ceremonia óptica.
- OFFLINE_CHALLENGE/RESPONSE CPv2/OFFLINE, boot/expectedRevision/action/nonce/pair
  reproducidos y revalidados en escritor; acciones solo MAINTENANCE/RETIRE. Reto5min
  RAM, sustitución/cambio de contexto lo invalida; función limitada solo tras commit.
  El consumo de autorización OFFLINE **no implica consumir un código impreso** ni
  borrar esperas: son rutas distintas. V2 exige el compromiso privado/revalidación
  nuevos del R1; la pérdida de nonce RAM/relectura del controlador v1 no lo acredita.
- MAINTENANCE limitada5min no ALLOW/reset de código adulto; RETIRE confirmación
  destructiva local adicional; RECOVER_PARENT código único abre ceremonia presencial,
  preserva vínculo hasta nuevo commit y nunca regenera libre por fallo.
- Nuevos hashes: SHA256 de UTF8 `CPv2/RECOVERY\n` + pairId + newline + purpose +
  newline + código Base32 normalizado; 20bytes/32 caracteres, distintos del código
  numérico/pregunta. Hashes legados permanecen CPv1/RECOVERY con **pair/purpose/
  esquema de origen, consumos y esperas** íntegros, sin texto claro/rehash ni fallback
  WSS. Firma/PAIR/OFFLINE/MESSAGE v1 no autorizan nueva operación en perfil v2.
- Pendingv1/bytes/hash/reservas/resultados inciertos no se convierten en retry v2.
  Antes de corte: lectores versionados/herramientas/capacidades/recuperación reales;
  si faltan, migración bloqueada, no recuperación deshabilitada para avanzar.
  Conservar identidad y evidencia pasada no fabrica credencial/permiso/aceptación v2.

## Cota §14 comprobada aritméticamente, no probada por codec

Se recontaron los campos de las tablas suministradas: STATE raíz20/challenge5/
effective11/lastRemote9/permittedTime9/retention5/buckets24/lastExpiry8 =91 miembros,
84 escalares,15 objetos,1 array. No cambiar máximos8192/16384/profundidad6.

| Cálculo independiente local | Resultado |
|---|---:|
| 91×36+84×45+(66−45)+15×2+2+7 | STATE payload≤7116 |
| 4×ceil(7116/3)+300 | STATE frame≤9788 |
| 4×ceil(8192/3)+300 | Frame bajo límite payload general≤11224 |
| PAIR resto: v1+kind13+protocol7+pairId38+enrollId38+nonce45 | 142 |
| 2×202+202+3×45+142+13×36+2×2 | PAIR payload≤1355; frame≤2108 |
| (1+13+7+202+38+45)+6×36+2 | CHILD_OFFER≤524 |
| 9×(36+45)+2 | OFFLINE payload≤731; frame≤1276 |

Ejecutado solo cálculo PowerShell de esas sumas/ceil, sin JSON/fixtures/firma/tests.
Scalar máximo45 salvo zone66; objetos/UUID/C/fechas/enum no contienen escapables;
origen/públicas locales acotados y gramática sin comillas/backslash. Sobre cerrado
con DER72→base64url≤96 y valores constantes cabe holgadamente en overhead300.
Profundidad STATE4 coherente con definición raíz1→array3→bucket4. QR local2500
alcanza cotas candidatas indicadas, no asegura lectura óptica ni es permiso de UI.

Son **cotas condicionales del inventario/canonización**, no fronteras ejercitadas.
Los demás frames de tablas tienen menos campos/valores que esta cota; validadores
deben enumerarlos y probar cada frontera. Ejercicio relay2966/4122 reportado en R1
no ejecutado aquí ni usado como evidencia de firma/validez/cota formal.

## Diferencias con implementación actual y siguiente gate

Leídos localmente CodecCp1/CriptografiaCp1/DerEcdsaCp1/ClavePublicaCp1/JsonCp1,
CodigoRecuperacion/RecoveryController, BootContextRepository/ChildStateStore, C07
y estado/relevo. Codec v1 carece de expectedRevision/perfil/arrays de historial y tiene otras
acciones/resultados; JsonCp1 permite profundidad32, no garantiza límite v2 de6.
ChildStateStore v1 prohíbe cambiar policy con mismo seq remoto; v2 necesita revisión
global y privado versionado propios. BootContext contrasta boot/elapsed, no prueba
ledger sin revocaciones perdidas. Recuperación v1 no conoce hashv2/procedencia/revisión
global de OFFLINE. Son trabajos posteriores, **no incompatibilidad del formato R1**.

Sin corrección material pendiente para generar especificación separada. Las
decisiones funcionales finales no se reabren; ancla/zona concretas, protección de
estado privado/capacidades y parámetros KDF/cupos/admin/reset/normalización/olvido
doble, proveedor/emisor push siguen asignaciones posteriores, no parámetros
inventados por schema. En preparación: explicitar definición de profundidad,
canonización/errores/casos cruzados y validación semántica de calidad/retención;
ningún schema por sí solo prueba ausencia de gaps, frescura real o capacidad Android.

Coordinador puede asignar contrato/schema/validadores/pruebas **nuevos separados**,
conservar copias v1 e incluir negativos de contexto/DER/tamaño/recuperación/retención.
Esta sesión no los inicia. Sin builds/tests nuevos/código/Android/Keystore/uso real/
WSS/APK/dispositivos/ADB/políticas/CA/LAN/push/despliegue/QR02 ni repos ajenos.
QR padre reproducido no corregido; H05 abierta/H06 no autorizada. Solo esta nota,
relevo/ESTADO.md y commit Git; detenerse tras entregar veredicto.
