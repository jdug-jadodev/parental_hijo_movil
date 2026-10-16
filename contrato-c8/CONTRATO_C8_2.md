# C8/2 — vinculación presencial de hasta diez minutos

Unidad coordinada C8-T10, autorizada por el propietario: «sí, pero necesito
avanzaaaaaaar!». Acuerdo de implementación local nuevo; cada rol confirma este
archivo idéntico antes de sincronizar su implementación y comunica objeciones
concretas al coordinador. No autoriza instalación ni publicación por sí mismo.
C8/1 R1 permanece congelado. CP/1 y los documentos/firmas CP/2 no cambian.

## Diferencias exactas respecto de C8/1

- Ruta WSS `/pair`, subprotocolo único **`c8.v2`**.
- Todos los frames C8 y el payload PROOF llevan **`v:2`**.
- Firma provisional sobre `UTF8("C8v2/PROOF\n") || bytesOriginalesPayload`.
- TTL de PUBLISH: entero **1..600000 ms**. Socket autenticado: máximo600000 ms
  desde CHALLENGE. El reto y la prueba AUTH conservan **10000 ms**.
- Los campos, direcciones, canonicalidad, claves, límites de tamaño y tipos son
  exactamente los de C8/1: no mensajes nuevos, negociación ni fallback.
- Cada instancia selecciona UNA versión; ninguna conexión C8/1 obtiene plazos C8/2.
  Rechazar subprotocolo, frame o firma de versión distinta; no degradar en silencio.

## Marco humano y plazos locales

El hijo inicia explícitamente el asistente C8/2 con código adulto fresco y crea
un marco propio de **600000 ms**, anclado a esa autorización nueva. No hereda los
segundos que quedaban del menú administrativo, ni revive un marco ya vencido.
La autoridad ampliada sirve exclusivamente para esa vinculación: generar oferta,
validar y guardar. No amplía administración general, canal de órdenes, recuperación,
credenciales, permiso de uso, NORMAL/OFFLINE ni temporizadores de bloqueo.

El asistente mantiene la oferta RAM hasta el límite de ese mismo marco (no el
tope previo de cinco minutos del flujo QR). Los códigos frescos de cada operación
siguen siendo obligatorios y no renuevan el marco. Padre también separa la vida
del asistente de la vigencia corta de autenticación/firmante: autenticar cuando
corresponda sin ampliar Keystore ni capacidades de firma por estar conectado.
Los tickets de entrega/preparación C8/2 deben admitir ese marco; QR/C8/1 conservan
sus topes existentes. No dejar un tope residual de120s o300s en la ruta C8/2.

Después de CHALLENGE y justo antes de PUBLISH, el hijo mide el restante hasta
`min(marcoC8V2, referenciaOfertaC8V2, ahora+600000)`. Envía ese ttlMs una sola vez.
Node fija `deadline = instanteMonotonicoCHALLENGE + ttlMs`, nunca ahora+ttlMs;
lo acorta a la ventana pública del servidor. El padre limita por su CHALLENGE
+600000 y, al recibir OFFER, por `ahora+remainingMs`. El hijo conserva el límite
local final. No comparar relojes, sumar tránsito o renovar por CLAIM/AUTH/pings.
Fondo, salida, cancelación, muerte o cambio de contexto invalidan el asistente.
Una autenticación visible no equivale a fondo; conservar el criterio Android
existente y probado, sin excepciones que permitan continuar tras STOP real.

## Identidad, datos y resultado

Se conservan comparación presencial de huellas completas con identidades LOCALES
independientes, origen local, custodia offline de los tres códigos, firmas CP/2
sobre bytes originales, escritor único y readback. PoP y el código8 no identifican
familia. El padre guarda antes de SEND_PAIR y realiza una sola entrega.

Mostrar etapas legibles y tiempo restante real: encuentro iniciado, identidad
por confirmar, aceptación pendiente y vínculo guardado localmente. El padre no
puede afirmar guardado del hijo por SENT; el hijo sólo declara guardado tras
readback. No nuevo ACK de guardado ni reconciliación automática en este cambio.
Mantener su propia huella local visible durante la comparación, incluso tras CLAIM.
La consulta de estado persistido debe poder distinguir sin regenerar: ausente,
pendiente, guardado o incierto. No confundir caducidad del asistente con pérdida
de un registro ya guardado. El intento físico previo tiene resultado desconocido:
ninguna actualización borra, sustituye o rehace una aceptación pendiente.

## Servidor y activación futura

Perfiles nuevos: `RELAY_MODE=c8-v2-local` para laboratorio y `c8-v2` para Render.
Los perfiles `c8-local`/`c8` conservan C8/1. Cada proceso admite una sola versión.
El perfil público mantiene opt-in C8_ENROLLMENT_ENABLED=true, ventana explícita
60000..600000 ms, origen Render coincidente y ausencia de claves familiares.
La ventana global puede acortar una ceremonia: mostrar el restante recibido,
no prometer diez minutos completos si el servicio está próximo a cerrar.
No se amplía automáticamente la ventana pública por esta autorización local.
Health503/keepalive200, cierre1012, cupos18sockets/16encuentros,40CLAIM/min y cinco
fallidos por socket permanecen. No auto-reconexión, reentrega, canal de control
permanente ni historia por C8/2. Desarmar C8 al terminar la prueba pública.

## Verificación y entrega de esta unidad

Probar ruta real local después de120s y300s; expirar en600000 exactos, sin renovación.
Probar rechazo cruzado de versiones/dominios, AUTH10s, cancelación/fondo/callback
viejo, límite global, datos previos y fallo parcial. Conservar regresiones C8/1.
Cada móvil entrega pruebas focales, APK/hash e instrucciones cortas con etiquetas
reales. Render entrega `pnpm run verificar` y arnés de cruce compatible C8/2.
No ADB, instalaciones, nuevas identidades reales, códigos reales, push o deploy
en la preparación. La vinculación y el control remoto físicos siguen pendientes.
