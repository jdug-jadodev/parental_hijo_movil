# HIJO-C8-I01 — alternativa MAIN sin cámara

## Cierre — implementación MAIN compilada/JVM, sin canal público ni física

Unidad C8-I01 completada en código; bloque1 `d388ef8`, cierre en commit que
incorpora esta evidencia. No afirmar ceremonia real ni interoperabilidad con
servidor/Android. Fuente EXACTA R1 compatible, hash UTF8/LFfinal
**`089e0b6299ec6db3d312596e5929452321cbb60e3fb4646cceb4669b9492adde`**
recomprobado al cierre. No desviaciones wire ni cambios en CP1/CP2PAIR/AUTH/STATE/
ACK/fixtures/firmas/dependencias/manifest/Owner/enforcement/recuperación.

### Ruta productiva real y guardas

- `core-protocol/.../protocol/c8/C8.kt`: codec R1 de13 frames y payload PROOF
  separado, campos exactos/tipos/canonicalidad ASCII/UTF8 sin BOM/depth6/duplicados/
  frames16384/documentos8192/base64roundtrip/SPKI P256/DER estricto/enteros seguros.
  ttl1..120000, remaining entero seguro no negativo; el cliente siempre acota
  su límite original y cierra si restante0. PROOF nunca es frame directo.
  AUTHCHILD firma `C8v1/PROOF`+LFreal+payloadoriginal y verifica con pública de
  identidad existente. PoP sólo posesión, NO persona/familia/autoridad/pareja.
- `pairing/ClienteEncuentroC8.kt`: máquina real CHILD sin Android, una referencia
  de OfertasRam; RETO→FIRMANDO(consumido antescrypto)→AUTH→PUBLISH→CODE→CLAIMED→
  PAIR candidata. Rechaza replay/segundoREADY/entrega fuera de orden/segundaPAIR/
  PARENTops/otra clave-origen-oferta/contexto. Verifica CP2PAIR completo contra
  pública CLAIMED y oferta/origen originales ANTES de presentar candidata.
  Aun así no crea consentimiento ni guarda. Sin reconexión/republicación.
- CHALLENGE precede la medición; ttl se calcula justo antes de PUBLISH como
  presupuesto positivo del marco/ref/120s, además tope local120sdesdeCHALLENGE.
  Node ancla a SU instante CHALLENGE, tal como R1, no a recepciónPUBLISH: se
  confirma compatibilidad de algoritmo, NO se implementó Node aquí. No se
  transmiten/comparan elapsed de distintos procesos ni se renueva por READY/
  tickets/mensajes. remaining sólo acorta. Lectura volatile de deadline en
  `CanalEncuentroC8Android.disponible` impide commit tardío aunque timer100ms
  no haya corrido, sin inversión de cerrojos modelo/oferta. Cerrar pone límite0
  ANTES de cerrar socket/callbacks y descarta referencias de oferta/firmante/
  autoridad/públicaPARENT; originalesPAIR temporales se limpian tras entrega.
- `pairing/CanalEncuentroC8Android.kt`: WSS MAIN OkHttp4.12.0 existente, `/pair`,
  único `c8.v1`, mismo origen HTTPS exacto elegido localmente, TLS/hostname
  plataforma, DNS rechaza loopback/LAN/privadas, sin redirects/retry/trustAll/CA/
  query/Origin/compresión/binarios/logbody/pings-renovadores. Interceptor retira
  oferta de extensions y Origin, respuesta con extensions se rechaza. Cuota
  salida64KiB/pendientes64 y frames10/s-ráfaga20 en máquina; bibliotecas reensamblan
  antes del callback, no se afirma medir fragmentos/chunks de wire en Android.
  Cupos globales/IP/claims/tombstones y limpieza servidor pertenecen al servidor
  R1, no a este cliente. Sin socket/modelo de prueba en MAIN.
- `ui/AccesoAdultoActivity.kt`: botón GENERAR CÓDIGO8 separado del QR; GENERAR
  fresco/ausencia pareja coherente/refOfertasRam real, **no crea Qrmatriz**.
  Origen local introducido antes de recibir PAIR, huella propia LOCAL completa
  visible ANTES de abrir transporte; confirmación explícita de haberla mostrado
  al propietario/padre conocido. Si texto manchado no legible, detener. Campo8
  grande38sp/mayúsculas/RAM/no selección/copia/autofill/guardado, FLAG_SECURE
  existente, sin cámara. CLAIMED retira código, informa candidata no autoridad.
- PAIR original→framePair→VALIDARfresco→presentarCandidataPair→HUELLAS de
  identidades LOCALES independientes y origen local→VALIDARfresco→
  ValidacionPairRam real→GUARDARfresco→PairAndroid real dentro del escritor adulto
  único. Huella esperada y origen nunca autollenados desde PAIR/C8; el origen
  introducido también debe igualar el elegido antes de conectar. Familia no se
  infiere de código8/PoP/firma candidata. Marco original120s jamás se renueva.
- Fondo/cancelar/caducidad/contexto cambiado/cierre inesperado invalida canal y
  candidata RAM; callbacks UI exigen MISMA referencia/instancia viva. VidaOferta
  y guardas de commit ven disponibilidad/deadline C8 sin esperar cierre UI.
  Si commit ya ocurrió no rollback/limpieza/segundaalta; reserva/lectura incierta
  queda cerrada. Resultado sigue PENDIENTE_CANAL_Y_RECUPERACION, nunca USO/ACTIVO.
  No inicializar/importar/rotar/exportar claves, ni tocar CP1/legado/credencial/
  custodia offline/Owner/PIN/emergencia/protecciones por cambiar transporte.

### Pruebas, builds y fallo intermedio — sólo lo ejecutado

Nuevos **5 core +16 app distintos**; debug/release ejecutan mismos16, no sumar32.
`C8Test`: todos los frames/campos exactos, PROOF separado, canonicalidad/size/
base64/UUID/nonce/DER/roles/origen, firma P256 real/dominios y contexto externos.
`ClienteEncuentroC8Test`: recorrido oferta sin QR→PROOF→CODE→CLAIMED→PAIRoriginal
→confirmación independiente→ValidacionPairRam→AlmacenPairCp2/readback/restauración;
pendiente no permiso; tickets GENERAR no VALIDAR/otro propósito no renueva;
TTLdesdeCHALLENGE/presupuesto no5min; orden/replay/otra candidata/cierre/fondo/
boot/contexto/reloj/firma lenta-reentrante/caducidad estricta; cancelación antes
de commit/trasreserva/durantecommit/readbackcorrupto. Prueba final específica:
deadlineC8 vencido con marco adulto aún vivo rechaza antes de reservar sin
esperar timer. UI/cámara/KeystoreAndroid/AtomicFile real y TLS no se ejecutaron.
Reloj/socket/disco y verificación adulta acreditada son puertos FAKE de tests;
firmas JCA, codecs/casos/almacén Kotlin reales. No afirmar UI instrumentada ni
preservación física por tests JVM.

Desde `android/`, comando final completo:

```text
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintRelease --offline --rerun-tasks --console=plain
```

- Focal inicial app12casos debug SUCCESS23s/4ejecutadas23UPTODATE.
- Focal5core/15app debug-release SUCCESS42s/10ejecutadas42UPTODATE; coretest
  UPTODATE en esa pasada, no contarlo como reejecutado.
- Primer full SUCCESS2m59s/131ejecutadas0UPTODATE,210core/505debug/450release
  0fallos/lint0errores12avisos. InspecciónAPK parcial entonces exitosa; hashes
  de esa pasada supersedidos, NO artefactos de entrega actuales.
- Revisión añadió guarda de deadline inmediata sin esperar timer y16ªprueba.
  Full FAILED1m32s/90ejecutadas: inferencia recursiva Kotlin entre propiedad
  disponible/modelo en adaptador, no fallo de contrato. Se corrigió con tipos
  explícitos Boolean/ClienteEncuentroC8; no atribuir aprobación app a ese pase.
- **FINAL SUCCESS3m43s/131tareas131ejecutadas0UPTODATE**, XML
  **210core/506debug/451release,0fallos/errores**. LintRelease análisis/reporte
  ejecutados: **0errores12avisos previos de versiones**, sin suppress nuevo.
  APKAndroidTest compilado/empaquetado, NO instalado/instrumentado.

Inspección dirigida final Python-B/ZipFile/hash/cadenasDEX: MAIN C8cliente-WSS/
codec-cripto/OfertasRam/ValidacionPairRam/PairAndroid y CONTROL/STATE/ACK presentes;
tests/vectoresfuente-vueltas/schema/contrato ausentes, rollback sóloDEBUG,
ignoreFGSúnico igual. No auditoríaexhaustiva ni lintDebugprototype/verificadorFake
históricos resueltos. `apksigner verify --print-certs` local verificó debug;
certificadoSHA256 `39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439`
igual al histórico, no prueba de instalación ni clave familiar nueva.

### Artefactos finales, NO instalados

- Debug: `android/app-child/build/outputs/apk/debug/app-child-debug.apk`
  SHA256 **`6221f3f52a7bdfbe4a4da19627c00821598fb2b2f9a59989a82e846e28250751`**.
- Release unsigned: `android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk`
  SHA256 **`029d4e2dd876f87c061f4bace6e2fb4fbaaad76dddcdd5ae5116b7f5a285b3b4`**.
- Último instalado acreditado sigue E2E01debugfc7a5bd8…83108ed/F03histórico;
  no consultado/actualizado durante C8. Reportes de ambos APK/UIadulto-emergencia
  anteriores permanecen históricos, sin nueva evidencia física.

Rutas: contratoR1exacto/coreC8+tests/appClienteEncuentroC8-CanalWSS+tests/UIadulto/
ESTADO/evidencia/relevo. Commit1d388ef8 y commitcierredeestaevidencia; sin push.
Sin ADB/dispositivos/instalación/QRreal/códigos8 reales/privadas/códigosadultos/
otrosrepos/subagentes/sesiones/prompts/serviciosremotos/bootstrap/push/deploy.

**Entregar y detener.** Endpoint C8 aún no desplegado, sin interoperabilidad
Node/Android ni handshake real probados aquí. Habilitación/configuración pública,
instalación y ceremonia requieren autorizaciones posteriores concretas y fuente
R1 idéntica en ambos roles/servidor. No iniciar otra tarea automáticamente ni
conectar al bootstrap por conocer origen. C8/READY/SENT no confirman pareja,
recuperación ni enforcement: CP2 posterior requiere configuración autenticada y
STATE CHILD fresco firmado para el piloto, no permiso de uso por este transporte.

## Historial del primer bloque (pendientes allí ya resueltos en código)

## En curso — primer bloque, no entrega final

Fuente R1 íntegra suministrada copiada UTF8/LFfinal a
`contrato-c8/CONTRATO_C8_1.md`. `Get-FileHash` confirmó SHA256 exacto
`089e0b6299ec6db3d312596e5929452321cbb60e3fb4646cceb4669b9492adde`.
Compatibilidad de fuente confirmada; no contratos CP1/CP2 modificados.

Primer bloque: codec/PROOF C8/1 en core, máquina CHILD pura, adaptador WSS real
OkHttp existente y alternativa MAIN GENERAR CÓDIGO8. OfertasRam real sin matriz,
huella propia LOCAL/origen antes de abrir canal, llegada candidata sin CAMERA
al caso/guardado existentes, tickets/marco120s y autoridad original conservados.
No claves creadas/importadas, no bypass de legado/reservas/consentimiento.
No AUTH familiar usado como bootstrap, sin cambios en cp.v2 o enforcement.

Ejecutado desde android, offline:
- `:core-protocol:test :app-child:compileDebugKotlin`: SUCCESS56s,
  6tareas ejecutadas/15UPTODATE. Pruebas core existentes ejecutadas y Kotlin
  MAIN debug recompilado; aún sin pruebas nuevas de la máquina.
- `:core-protocol:test --tests dev.controlparental.protocol.c8.C8Test
  :app-child:compileDebugKotlin`: SUCCESS6s,2ejecutadas/19UPTODATE;
  5pruebas nuevas de codec/PROOF reales JCA, sin fallos. Compilación app UPTODATE
  en esta pasada, no contarla como recompilación. Sin APK nuevo aún.

Pendientes de esta MISMA unidad: pruebas máquina/TTL/orden/guardas y recorrido
C8→validación/commit/readback/cancelación, revisión concurrencia/limpieza,
builds y lint finales/inspecciónAPK/entrega. Timer del adaptador se inicia al
conectar explícitamente; última pasada del mismo comando focal SUCCESS4s,
1ejecutada/20UPTODATE: Kotlin MAIN recompilado; coreTestUPTODATE, no reejecutado.
Sin endpoint C8 desplegado: no se conectó WSS real/bootstrap, ni ADB/teléfonos/
instalación/QR/códigos8 reales/capturas/privadas/repos ajenos/agentes/push/deploy.
Instalación/UI/emergencia históricas reportadas no prueban C8 ni dan permiso nuevo.
