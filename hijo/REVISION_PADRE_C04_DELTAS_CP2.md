# HIJO-C07 — revisión PADRE-C04 y deltas por decisiones del 13 de octubre

2026-10-13. Base hijo `c6e8d5d`, C04/C05/C06 y L02 locales. **Misma unidad
reanudada**, solo documentos. Fuente del draft PADRE-C04 `1582e66`: texto íntegro
§§1–13 suministrado en cinco partes por coordinador; §17 central suministrado
íntegro al reanudar. Dos lecturas externas se interrumpieron: archivo padre y §17
Render **no leídos externamente por esta sesión**; no se repiten ni amplían permisos.
Se revisa el texto recibido completo, no se verifica correspondencia con archivos
externos o cambios posteriores. CP/2/campos siguientes siguen propuesta, no contrato.

## 1. Resultado y decisiones vigentes

Compatible como base: bytes originales/P-256 DER/dominio separado, retos de acción,
escritor único, revisión distinta seq, no repetición de permiso por duplicado,
STATE separado de ACK/resultado histórico y recuperación sin generación libre.
**No compatible sin corregir**: LOCK_FOR/apertura, lista de acciones/combinaciones,
solo bucket actual, total de época indefinido, ausencia de arrays y exclusión de push.

Confirmado ahora: contar autorización transcurrida desde aceptación durable vigente,
**incluye apagado/Keyguard**, sin UsageStats/humano/apps. Historial agregado de varios
días se limpia domingo21:00 local configurado, hijo/padre sin depender de red.
Evaluar push padre cerrado **solo diseño**. Eliminar bloqueo temporal/apertura
automática. Acciones: LOCK, ALLOW_FOR y ALLOW libre hasta decisión nueva/reboot.
Alcance de ALLOW local/remoto y rango de duration aún no fijados; no convertir el
viejo requisito LOCAL temporal en prohibición permanente de libre sin preguntar.
Todas apps/instalar-actualizar permitido/HOME/guardas básicas/código5–10/pregunta/
ambas fuentes offline/última acción/reboot/admin RAM/emergencia siguen confirmados.
Escapes exhaustivos pospuestos, corregir encontrados; no certificación inviolable,
retiro de guardas o autorización física. C06 proxy y propuestas sin historial/push
quedan supersedidas por estas decisiones; L02 permanece evidencia sintética aislada.

## 2. Matriz contra draft y núcleo hijo leído localmente

| Bloque draft | Evaluación | Delta / evidencia local, no prueba nueva |
|---|---|---|
| §2 bytes/dominio/DER/hash/perfil | Compatible como propuesta | CriptografiaCp1 conserva bytes originales/hash, pero dominio/codec v1 no verifican v2; módulo separado tras acuerdo, nunca cambiar prefix de un payload v1 |
| §2 canonización/límites | Corregir arrays y precisión | Permitir solo array diario cerrado §5, mantener 8192/16384 y profundidad6 propuestos; falta demostrar peor tamaño nuevo. Orden objetos lexicográfico y array por fecha; verificar bytes recibidos y luego canonización estricta, no reserializar antes de firma |
| §§3–5 revisión/seq/reto/CAS | Compatible, implementación pendiente | ChildStateStore serializa por instancia/readback y preserva seq/consumos; debe haber una instancia por estado. Su regla v1 `seq igual => policy igual` impide decisiones locales v2: privado versionado separado, no relajar v1 ni añadir seq ficticio |
| §5 commit/aceptación | Precisar IO incierto | Aceptación durable no clic ni envío. Resultado pendiente y tiempo lógico no APPLIED. Readback por sí solo no prueba energía/antirollback; tras fallo no nueva apertura, relectura coherente y diagnóstico, no cero/reintento que renueva plazo |
| §§3/5/6/8/11/12 LOCK_FOR | Corregir/eliminar | §3 de este informe lista reemplazos exactos, sin apertura automática incluso al migrar |
| §6 efectivo/Keyguard | Precisar | Phase PERMITTED describe autoridad; Keyguard no cancela permiso/contador. mode LOCKED no debe confundirse con bloqueo parental aplicado solo por PIN del sistema; mantener razón SYSTEM_KEYGUARD y modo UNKNOWN si efecto normal no evaluable |
| §§6–7 stateSeq/ACK/lastRemote | Compatible, reservas | Firma no garantiza frescura. STATE ligado a sync/socket/sesión; ACK histórico solo resultado exacto en contexto original. Callback posterior de orden sustituida no demuestra APPLIED. Explicitar registro ausente migrado con lastAcceptedSeq>0, no inventar rechazo |
| §8 tiempo/unión/reconstrucción | Compatible con correcciones | Base ahora confirmada; bucket/retención cambian. BootContextRepository contrasta BOOT_COUNT/elapsed, no prueba ausencia de cierre perdido ni implementa contador/revisión v2. Registro íntegro más prueba de continuidad necesarios, no importar gaps L02 |
| §8 limpieza/lastExpiry | Corregir | Retención semanal local, array máximo8, tombstone y ledger activo separado §§4–6; no conservar total/aviso antiguo indefinidamente |
| §8 avisos/push | Corregir límite de alcance | Admitir evaluación push mínima §7, no servicio/token/proveedor/garantía activa |
| §10 PAIR/OFFLINE/migración | Pendiente bloqueante | Perfil/contextos y recuperación segura aún por definir; no migración por STATE ni pérdida de identidad ni reinterpretación de recuperación v1. H05 abierta/H08 pendiente |

Inspección local adicional: `BootContextRepository.kt`, `ChildStateStore.kt`,
`CriptografiaCp1.kt`, ADR-H-005; sin ejecución. L02 22/199 JVM y núcleo169
UP-TO-DATE son antecedentes, no repetidos ni acreditación de este diseño.

## 3. Deltas exactos de acciones y temporización (§§3/5/6/8/9/11/12)

1. Tipo Acción queda **`LOCK|ALLOW|ALLOW_FOR`**. D pasa a «entero positivo segundos,
   rango de producto pendiente»: 60..604800 **no aprobado**. Schema futuro debe
   fijarlo antes de emitir. No inventar 8 dígitos/caducidades/KDF u otros rangos.
2. SET_POLICY elimina por completo **`deferredAllowSec`**, no conservar null como
   llave obligatoria. `durationSec` D solo ALLOW_FOR, null LOCK/ALLOW. requestedAction
   y commandChallenge.action usan nueva enum. LOCK_FOR o llave eliminada => BAD_SCHEMA
   autenticado cuando identificable; no alias LOCK, ni espera con apertura implícita.
3. Tabla §5: LOCK revoca ahora sin apertura; ALLOW libre hasta sustitución/revocación
   válida/reboot; ALLOW_FOR desde aceptación durable hasta deadline original, luego
   nueva revisión y objetivo bloqueado. LOCAL libre pendiente de alcance UI/autoridad,
   no esconder default ni tiempo infinito en acción temporal. Ningún bloqueo caduca
   a permiso. No cron, callback, reconexión o recuperación que abra automáticamente.
4. Borrar fila y todos los párrafos de LOCK_FOR, dos incrementos apertura+expiración,
   condiciones/red de apertura y plan deferred del §5; §8 borra reconstrucción de
   apertura diferida y «LOCK_FOR no consume bloqueado». Mantener consolidación tardía
   **solo de expiración ALLOW_FOR**: sin abrir fugazmente, contar hasta deadline y
   registrar detección/aplicación tardía por separado. Deadline overflow impide aceptar.
5. effective.phase queda **`BLOCKED|PERMITTED|EXPIRED|NONE`**, quitar WAITING_ALLOW.
   Mantener acceptedElapsedMs y phaseStartedElapsedMs por mínimo cambio, pero este
   último **igual a acceptedElapsedMs** en decisiones normales: sin fase de apertura.
   LOCK BLOCKED/null deadline; ALLOW PERMITTED/null; ALLOW_FOR PERMITTED o EXPIRED
   con deadline de permiso original/remaining0. Máximo remainingSec depende de rango
   acordado, no 604800 fijo. Boot NONE/sourceNONE no aplica política previa.
6. §9 no contiene un error exclusivo LOCK_FOR que conservar; BAD_SCHEMA cubre action/
   combinación vieja. Mantener TIMER_EXPIRED para permiso, no bloqueo/código/admin.
   Quitar términos WAITING_ALLOW/deferred de enums, validadores y pruebas **futuras**.
7. §11 eliminar traza «Apertura tardía contextual» y mitad LOCK_FOR de «Timer viejo»;
   sustituir por LOCK persistente sin autoapertura y ALLOW_FOR tardío nunca renovado.
   Retirar elección §12 de apertura finita/indefinida: solo rango temporal y alcance
   de libre pendientes. CP1/ejemplos/codec/fixtures/trazas existentes no se editan aquí.

## 4. Cuenta durable y separación de autoridad/efecto

Registro propuesto único escritor: revisión/orden normal aceptada, boot, inicio y
deadline originales, cursor contable, subtotal diario, calidad y retención/tombstone.
Cerrar intervalo anterior y abrir nuevo al mismo instante de aceptación lógica
serializada; unión disjunta, nunca presupuestos futuros ni dos autoridades paralelas.
Transacción propia readback hace visible aceptación; conservar timestamp lógico
elegido en esa transacción, no clic ni hora ACK. Escritura incierta: no confirmar
aceptación/aplicación; tras relectura íntegra resultado original, sin renovar inicio.

Tiempo normal autorizado avanza aunque Keyguard/apagado/red/administración RAM
cierren interacción. Abrir admin no abre otro contador; salir no cierra permiso.
Objetivo/enforcement pendiente o fallido se informa aparte: si autoridad fue aceptada
y no revocada, su tiempo cuenta sin afirmar Android funcionando. Error que **revoca
autoridad** corta tramo/revisión; conocer fallo no equivale a inferir cuándo revocó.
Antes de STATE, materializar subtotal/cursor hasta sampled en mismo escritor y
readback; firmar snapshot inmutable del instante, no backdate de modo posterior.

Mismo boot: reconstruir solo intersección [cursor,sampled] con permiso durable
demostrado vigente, hasta min(deadline,cierre conocido). No iniciar todo desde cero.
Pruebas pendientes de transacciones/cierres/boot/relojes: registro válido aislado
no demuestra que no faltó evento revocador. RECONSTRUCTED significa autorización,
no humano. Si continuidad desconocida, subtotal conocido PARTIAL/null UNKNOWN.
Boot nuevo cancela permiso, no rellena último hueco con tiempo civil/duration;
mantiene agregados comprobables no vencidos por retención y calidad parcial.
L02 no se promueve: su evidencia observada ficticia/no reparación no es este ledger.

## 5. Reemplazo exacto propuesto de §8.2 y §8.2.1

Objeto permittedTime **cerrado** propuesto (nombres por acuerdo, no schema):
`basis, epochId, retentionSeq, historyRevision, consumedMs, quality,
throughElapsedMs, retention, days`.

| Campo | Regla propuesta |
|---|---|
| basis | Exactamente AUTHORIZED_ELAPSED, incluye pantalla apagada/Keyguard; elimina opción de observación §12.1 |
| epochId:U | Época **de retención vigente**, rota al corte/cambio autorizado; no reset por sync/proceso/boot normal |
| retentionSeq:C+ | Generación durable creciente de limpieza/configuración, no UUID ordenable, seq remoto ni controlRevision; no reset tras borrar historia |
| historyRevision:C | Versión durable creciente de snapshot contable, nunca wrap; cambia por materialización/limpieza/calidad, no por PONG |
| consumedMs:C? | Suma absoluta conocida **solo de días retenidos desde corte**, no total vitalicio. Monótona dentro de misma época/configuración salvo discontinuidad explícita; nueva época puede empezar cero solo con cobertura comprobada |
| quality | EXACT / RECONSTRUCTED / PARTIAL / UNKNOWN del draft; UNKNOWN=>null, PARTIAL subtotal fiable no total exacto; gaps no se curan por reconexión |
| throughElapsedMs:M | Igual sampled del boot STATE; no timestamp universal ni continuidad probada |
| retention | Objeto cerrado abajo, datos calendario mínimos no configuración remota |
| days | Array de 0..8 objetos, orden date creciente, sin duplicados; [] con calidad desconocida NO significa ningún uso/cero. Reemplaza `day`, no ambas variantes |

retention campos exactos propuestos: `policyId:U, zoneId:string,
purgedThroughUtc:string?, nextCutoffUtc:string?, calendarQuality`.
zoneId IANA local fijada explícitamente por adulto, ASCII1..64 validada; no UTC
impuesta/geolocalización. UTC de corte formato estricto RFC3339 `YYYY-MM-DDTHH:mm:ssZ`,
fecha válida y conversión única desde zona; calendarQuality=`KNOWN|UNKNOWN`.
KNOWN exige ambos instantes y next>purgedThrough. UNKNOWN permite null donde no
comprobable, obliga a no presentar calendario exacto; jamás llenar con fecha inventada.
policyId identifica configuración; corte es high-water mark que no retrocede.

Cada día objeto **`date, consumedMs, quality`**, reglas ISO/contadores/null del draft.
Contexto de policy/zone/epoch está arriba, no repetir ni mezclar configuraciones.
Cada bucket contiene solo porción posterior a corte y anterior a siguiente corte/
sampled. Primer/último domingo son días parciales deliberados: calidad PARTIAL
respecto a «día entero» y UI indica «desde limpieza», aunque subtotal de ventana
sea EXACT. Cobertura iniciada tarde/gap => parcial. Null no se suma como cero; total
puede ser subtotal conocido PARTIAL si algunos días desconocidos. Sin filas de apps,
intervalos, personas, contenido, ubicación ni eventos. Historial **no** cupo diario.

Domingo21→domingo21 toca hasta **8 fechas civiles**, no 7: domingo inicial parcial,
lunes–sábado y domingo final hasta21. DST puede hacer días 23/25h; sumar millis
monotónicos, dividir con ancla civil/zona fiable. Propuesta resuelve corte ambiguo
con primera ocurrencia y hora inexistente con primer instante válido posterior;
regla determinista pendiente de ratificación. Si cambio raro de zona/ancla requiere
más de8 buckets, no truncar/sumar todo a otro día: calendario UNKNOWN y barrera §6.
§2 reemplaza «sin arrays» por «solo permittedTime.days, max8, orden date»;
no arrays libres en otros mensajes. Comprobar peor frame/payload/profundidad antes
de congelar; límites originales no se aumentan silenciosamente. No fixture generado.

## 6. Limpieza local, permiso superviviente y anti-resurrección

Propuesta hijo/padre: detectar corte local domingo21 aunque sin red; disparador local
y **catch-up obligatorio antes de leer/renderizar/publicar historia**, al iniciar,
recrear, boot/desbloqueo y recibir STATE/push. Varios domingos omitidos: consolidar
último corte vencido, no reconstruir semanas eliminadas. Deadline monotónico de
permiso no depende del calendario de borrado ni se reinicia por limpieza.

Transacción hijo al corte: cerrar/materializar hasta corte si ancla íntegra; eliminar
buckets/total retenido y último agotamiento anterior; persistir nueva epoch/
retentionSeq/historyRevision y watermark. Conservar autoridad/inicio/deadline y
**cursor mínimo del permiso activo** separado del historial para contabilizar solo
suffix posterior al corte. No borrar lastAcceptedSeq/controlRevision/pending/
lastRemote/reservas/identidad/credenciales/consumos/esperas. Resultado remoto retenido
no se usa como canal alternativo de historial eliminado.

Al permiso largo que atraviesa corte: contador semanal nuevo suma solo suffix,
no toda duración desde aceptación vieja; libre no se cancela. Al recuperar mismo
boot tras corte puede reconstruirse suffix autorizado si prueba suficiente. Si
ancla/boot son inciertos, no fabricar corte elapsed ni reconstruir prefijo borrado;
purga conservadora y snapshot desconocido. Borrar historia **no** activa permiso.

Cambio hora/zona/DST/incoherencia: fecha civil no es autoridad segura. Propuesta
conservadora: suspender publicación de historia, borrar agregados/aviso ante
imposibilidad de demostrar que son posteriores al último corte aplicable y aún
retenibles, elevar barrera durable y esperar ancla/configuración coherente;
no conservarlos indefinidamente
«por si acaso». Cambio explícito de zona abre nueva retención y no redistribuye
pasado. Watermark máximo anterior se respeta incluso si reloj se atrasa; no doble
rollover. Si nueva programación contradice ese máximo, UNKNOWN antes que ampliar
retención. Esta política de pérdida conservadora necesita elección del propietario.

Padre conserva **tombstone local sin totales**: máximo retentionSeq conocido del
hijo, epoch/policy admitidos, cutoff de purga propio y barrera de caché. No inventa
retentionSeq del hijo al limpiar offline: aumenta su barrera local y exige nuevo
sync firmado con corte igual/posterior y fechas permitidas. Hasta entonces no
reintroduce buckets ni muestra total obsoleto aunque firma/stateSeq sean válidos.
STATE con generación menor/época vieja/cutoff anterior se descarta; UUID no se
ordena. Cambio policy/época sin generación creciente es incoherente. Misma sesión
stateSeq, historyRevision y freshness verificadas; nueva sesión exige sync y
watermark persistente, no reset de defensas. Padre **reemplaza** array/total, no suma.

Eliminar cachés renderizadas, notificaciones y copias locales con historia vencida;
restauración/backup requiere validar barrera antes de publicar. No almacenar
historial en push/relay. Una reinstalación/rollback que pierda también tombstone
no queda resuelta mágicamente: sin procedencia íntegra/fecha fiable, no aceptar
historia hasta nueva reconciliación segura. No enrolamiento libre ni borrar vínculo
para «reparar». Preferir excluir historia/backups, revisar copias temporales y
rotación de protección en diseño posterior; no afirmar borrado forense de flash.

**Límite físico explícito:** con proceso muerto no puede prometerse eliminación
de bytes a las21:00 por un documento. Alarmas/Doze/arranque requieren demostrar
ejecución; catch-up evita exposición tardía pero no prueba borrado puntual mientras
apagado. Si el dueño exige borrado físico exacto aun apagado, incompatibilidad a
resolver, no retención infinita autorizada ni garantía por firma. Fallo de escritura
de limpieza bloquea acceso/publicación de historia, no emergencia/recuperación;
no confirmar purga hasta readback ni enviar snapshot viejo para «seguir funcionando».

lastExpiry reemplazo propuesto de §8.3: conservar campos existentes y añadir
`retentionSeq:C+, detectedElapsedMs:M`; el primero liga época de retención, segundo
distingue detección del deadline lógico (detected>=deadline en boot de registro).
Solo última expiración ALLOW_FOR de ventana vigente, no cola. Aviso antiguo se
elimina al corte; no debe sobrevivir por retención «hasta próxima expiración».
Callback tras corte de deadline anterior no recrea registro anterior; current
effective puede explicar estado EXPIRED sin conservar historial/aviso borrado.
Snapshot posterior a nuevo permiso no usa marcador como modo actual/APPLIED.

## 7. Push mínimo a evaluar, no transporte nuevo autorizado

Corregir §8.3/§12.3: **sí se evalúa** aviso con padre cerrado, sin prometer entrega.
Propuesta mínima: señal opaca de despertar «consultar estado», sin totales/array,
código/secretos, orden/action/duración o resultado APPLIED. Si se necesita expiración
identificable, identificador mínimo asociado a pareja/perfil/generación de retención
tras revisión de privacidad, no contenido histórico en proveedor. Datos no
autenticados solo hint rate-limited, nunca UI factual/enforcement; aviso factual
requiere prueba CHILD vinculada y STATE nuevo correlacionado por WSS/sync.
Push viejo no autoriza historia ni genera SYNC de acción/SET_POLICY automático.
Padre ejecuta limpieza antes de mostrar aviso; sin sync «aviso pendiente de verificar»,
no hijo offline/bloqueado/tiempo exacto. Permisos/canal/lifecycle/FGS/proveedor/coste/
consentimiento y recepción con app muerta/red ausente quedan a evaluación separada.

No añadir en este delta endpoint/token/dispositivo/registro de historial al relay.
Si emisión necesita componente y tokens fuera de relay, propuesta de consentimiento/
privacidad/seguridad previa a cualquier servicio/SDK/cuenta; no credencial real o
proveedor elegido. PEER/PONG no fuentes de tiempo/Internet: ausencia de reportes
sigue causa desconocida, no diagnóstico hijo sin red. Push no cola de órdenes.

## 8. PAIR/OFFLINE y migración sin bypass ni dependencia circular nueva

Mantener separación identidad/oferta/aceptación/commit y recuperación limitada.
AUTH/STATE/perfil no prueban commit PAIR ni conceden administración. Antes de
activar v2 definir formatos/contextos nuevos PAIR/OFFLINE y ruta local operativa
de emergencia/recuperación; mantener evidencias/consumos v1 bajo su procedencia,
sin aceptarlos como MESSAGE/ALLOW v2, sin reciclar códigos ni refirmar reto v1.
Si continuidad de recuperación exige puente, especificar propósito/capacidad,
versiones y consumo preservado por acuerdo conjunto; no habilitarlo aquí. Ausencia
de ruta segura **bloquea migración**, no se deshabilita recuperación para avanzar.

H05/H08/H11 dependencias siguen abiertas. Proponer revisión conjunta por etapas
sin exigir WSS productivo para investigar modelos/ceremonia local; eso no aprueba
puertas ni cierra H05 por tests. Mantener perfiles separados y corte explícito
de tres roles, sin fallback/downgrade por falla de conexión. Pendingv1/bytes/hash/
reservas/resultados inciertos conservados como pasado, nunca emitidos en v2 ni
resueltos por historia local. Nueva acción v2 requiere confirmación/reto/seq nuevo.
Tras migrar, uso normal bloqueado hasta autorización nueva del boot; arranque contable sin pasado
v1 fabricado y con nueva retención declarada, no «todo el día cero». Identidad perdida/
estado corrupto no crean credencial/pregunta/pairId ni permiso. No antirollback
universal por marcador de versión, ni retirada/borrado físico autorizado por este texto.

## 9. Casos negativos FUTUROS y decisiones aún necesarias

| Bloque | Prueba a asignar, no ejecutada |
|---|---|
| Bytes/acciones | LOCK_FOR/WAITING_ALLOW/deferredAllow rechazados, rangos sin congelar no emitidos; firma CPv1 en v2, extras/arrays9/fechas duplicadas/desorden/null/overflow/profundidad/tamaño |
| Writer/ACK | Crash antes/después de aceptación/readback, local sustituye remoto, callback atrasado, duplicado histórico, ACK viejo no STATE; limpiar no cambia seq/permiso, máximos agotados sin reset |
| Tiempo | Apagado/Keyguard/offline cuentan, sustitución sin solapamiento, libre solo transcurrido, finito al deadline sin renovación tardía; reconstructed con cierre perdido debe degradar |
| Boot/retención | Boot nuevo no arrastra gap, permiso cruza domingo21, 8 fechas/DST, proceso muerto varios domingos, fallo purga, ventana civil incoherente y cambio zona/atraso sin doble cuenta |
| Anti-resurrección | Padre offline limpia y recibe STATE viejo/firma válida/epochUUID distinta sin seq creciente; sesión nueva, caché/backup/push atrasado, tombstone perdido: no reaparece historia |
| Migración | Pendingv1 sin representación, recovery gastada/esperas/perfil cruzado, identidad perdida/circularidad: no bypass/admin/permiso ni corte sin recuperación |
| Push | Hint falsificado/duplicado/vencido, app padre muerta/red/relay ausentes, notificaciones denegadas: no APPLIED, órdenes o diagnóstico inventado |

Pocas elecciones: (1) rango ALLOW_FOR y alcance de ALLOW libre local/remoto;
(2) zona local y ratificar DST/purga conservadora por calendario incierto, alcance
de limpieza lógica/catch-up frente a exigencia física puntual;
(3) retención mínima lastExpiry dentro de semana y evaluación push/consentimiento
sin proveedor aprobado. KDF/cupos/normalización/admin/reset/olvido doble siguen
privados pendientes, no se eligen por fields. Domingo21 y contar apagado/Keyguard
ya confirmados, no se ofrecen alternativas de solo bucket/UsageStats/LOCK_FOR.

Siguiente unidad mínima **a asignar tras revisión conjunta**: revisión del draft
padre/relay con estas sustituciones y límites de bytes/recuperación; después, si
autorizado, modelo puro aislado de ledger de autorización/retención con calendario
inyectado, distinto de L02. No iniciar schema/codec/pruebas/Android por este cierre.

## 10. Entrega

Solo este documento, relevo y ESTADO.md. Lecturas locales/Git, ningún test/build,
código/CP1/fixtures/planes/ADR aceptados/Keystore/UI/WSS/productivo, dispositivos/
ADB/CA/LAN/push/despliegue/repos externos editados o QR02. QR padre comunicado
reproducido no corregido. Sin subagentes/sesiones/prompts de vuelta; commit propio
y detenerse. Coordinador revisa cliente/relay antes de acuerdo/sincronización.
