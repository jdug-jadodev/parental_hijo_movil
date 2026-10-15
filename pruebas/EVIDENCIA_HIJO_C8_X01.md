# HIJO-C8-X01 — cruce local JVM CHILD ↔ Node real

## Resultado y límites

**CERRADA: cruce local ejecutado y correcto, sin instalación/despliegue.**
Base `625af90`. Fuente de ensayo: instrucciones C8-X01 íntegras suministradas
por propietario. R1 permanece idéntica; hash UTF8/LFfinal recomprobado:
`089e0b6299ec6db3d312596e5929452321cbb60e3fb4646cceb4669b9492adde`.

Un **ClienteEncuentroC8 CHILD JVM real** cruzó WSS/TLS/c8.v1 con servidor Node
real compilado y contraparte **PARENT Node sintética**. No son dos APK juntas,
no wrapper CanalEncuentroC8Android, UI/Android/Keystore/AtomicFile/DeviceOwner ni
enforcer físicos. C8 no concede uso: el resultado durable de laboratorio sigue
`VINCULO_CP2_GUARDADO_PENDIENTE_CANAL_Y_RECUPERACION`.

## Alcance autorizado y piezas ejecutadas

- Sólo se invocó el archivo autorizado
  `C:\Users\Usuario\Documents\parental_render\test\soporte\interop-c8.mjs`
  mediante `node`, desde test opt-in con `-PrutaRelay` explícita. Se comprobó
  existencia, **no se leyó/editó ningún archivo de Render**, no se lanzó su build
  ni CLI/main. Servidor209tests/build correcto/perfil público desactivado son
  reportes de coordinación, no ejecuciones propias. Perfil público corresponde
  al servidor, sin configurar variables remotas ni crear backend aquí.
- `android/app-child/build.gradle.kts`: prop Gradle sólo para tareas Test; si
  ruta explícita, outputs.upToDateWhen=false fuerza EJECUTAR test; no caché ni
  UP-TO-DATE del ensayo. Sin ruta, Assume omite únicamente la prueba Node y el
  reporte muestra SKIPPED explícito. Ruta suministrada pero archivo ausente falla,
  no simula éxito ni fallback a servidor público.
- `src/test/.../pairing/TlsEfimeroC8.kt`: certificado P256 autofirmado efímero
  con SAN IP127.0.0.1/validez corta, generación JCA/DER sólo test/RAM, CA limitada
  a ese certificado y TrustManagerFactory estándar. SSLContext sólo en test;
  OkHttp mantiene hostnameVerifier predeterminado, no trustAll/CA/hostname
  permisivos en producción. Test comprueba SAN, confianza acotada y rechazo del
  certificado por almacén global estándar. Sin nuevas dependencias ni APIs JDK
  internas; certificado/privada TLS efímeros pasan a Node sólo por stdinPIPE,
  no argumentos/entorno/archivos/logs. No claves familiares reales.
- `src/test/.../pairing/CruceNodeC8Test.kt`: puente OkHttpTEST de sockets TLS
  reales hacia ClienteEncuentroC8/CodecC8/OfertasRam/ValidacionPairRam/
  AlmacenPairCp2 reales. Sin cambios en CanalEncuentroC8Android (su DNS/clock/
  Keystore no se debilitan para localhost). Prueba usa reloj monotónico JVM
  real, boot fijo de laboratorio, puerto adulto acreditado **FALSO** (no código),
  firmante JCA efímero en vez de KeystoreAndroid y archivos **RAM FALSOS** en vez
  de IO Android. Validación/serialización/firmas/transacción/readback Kotlin reales.
- READY de Node aporta origen loopback y públicaPADRE sintética **independiente
  de PAIR**, obtenida ANTES de candidata. Huellas de test derivadas de esa ancla
  y públicaHIJO propia, no de la candidata; no sustituye cotejo humano físico.
  CODE pasa a Node por PIPE CLAIM sin imprimirse/guardarse. No capturar cuerpos
  de stdin/stdout en evidencias: sólo consumirlos en RAM. Stderr se drena sin
  guardar texto; se cuenta y se exige cero al cerrar correctamente.
- Un AUTH/PUBLISH, oferta original sin QR, CHALLENGE antes del presupuesto y
  ttl positivo<=120s/marco/ref; ninguna reconexión. CLAIMED/PAIR respetan máquina;
  PAIR valida firma, ambas públicas, enrollId/nonce/origen/contexto. GENERAR no
  sirve de VALIDAR, nuevos ticketsVALIDAR/GUARDAR no renuevan marco original.
  OriginalesPAIR iguales al validar/guardar/restaurar y hashes frame/payload.
  Guardado/readback único, cierre tras DONE conserva durable pendiente/noUSO.
- DONE recibido, exitNode0, recursos/sockets/proceso cerrados. Sin reintentos
  automáticos: un fallo intermedio del propio adaptador de PIPE se corrigió
  explícitamente, documentado abajo; no se modificó ni simuló el arnés.

## Comandos y resultados realmente ejecutados

Desde `android/`, PowerShell; sin assemble/APK, sin `--rerun-tasks` global.

1. Pase ordinario sin ruta:

```text
.\gradlew.bat :app-child:testDebugUnitTest --tests dev.controlparental.child.pairing.CruceNodeC8Test --offline --console=plain
```

SUCCESS26s,2tareas ejecutadas/25UPTODATE; **2casos:1TLSunit correcto+1Node
OMITIDO por falta de ruta**. Ese pase NO es cruce Node ni éxito de interoperabilidad.

2. Pase opt-in con invocación real autorizada:

```text
.\gradlew.bat :app-child:testDebugUnitTest --tests dev.controlparental.child.pairing.CruceNodeC8Test "-PrutaRelay=C:\Users\Usuario\Documents\parental_render" --offline --console=plain
```

Primero **FAILED5s,1tareaejecutada26UPTODATE/2tests1fallo**, antes de mandar init:
el serializador JsonCp1 usado por error para el PIPE rechaza LF de PEM con
`La cadena debe ser ASCII imprimible`. Node fue iniciado pero no hubo encuentro
ni resultado positivo. Cierre del proceso por cleanup; no atribuirle exit0.
Se corrigió **sólo serialización de PIPE en test**, escapando LF/CR/tab/quotes
según JSON; no cambiar codec C8/CP1 ni fuentes/wire/arnés. Pipes nunca se loguean.

Tras esa corrección, el MISMO comando opt-in:
**SUCCESS5s/2tareas ejecutadas25UPTODATE; XML2tests,0omitidos,0fallos/errores**.
`:app-child:testDebugUnitTest` ejecutada realmente, no UPTODATE; `compileDebugKotlin`
MAIN UPTODATE, sólo test recompilado. Node **exit0**,3eventosPIPE
READY/PAIR_SENT/DONE, AUTH1/PUBLISH1/CLAIM1/PAIR1/reserva1/escritura1.

**Hashes públicos de ESE recorrido efímero** (no frames/claves/códigos):
- SHA256base64url bytes originales sobrePAIR, Node PAIR_SENT = JVM recibido/
  guardado/readback: **`X28PLtkh_Zv4nf-pqACF5Xe8bx21xZ0MTnNmdYsGOww`**.
- SHA256base64url payloadPAIR validado/guardado:
  **`rfaSPBDP7sRaeNm9W0FrGhSYRaOQkRZR7o4btamjscU`**.
No vectores/PAIR original persistidos ni claves/códigos de laboratorio publicados.

3. Regresión focal release ordinaria + lint, SIN ruta (no repetir cruce):

```text
.\gradlew.bat :app-child:testReleaseUnitTest --tests dev.controlparental.child.pairing.ClienteEncuentroC8Test --tests dev.controlparental.child.pairing.CruceNodeC8Test :app-child:lintRelease --offline --console=plain
```

SUCCESS53s/8ejecutadas27UPTODATE, **18casos:17correctos+1Node omitido**,
0fallos/errores (16regresiónC8+TLSunit1; Node1skip). LintRelease análisis MAIN/
test y reporte ejecutados, **0errores12avisos previos de versiones**. No se repitió
suite core/full ni física; no afirmar nuevos totalesfull210/506/451 de C8-I01.
Dos tests nuevos distintos: TLSunit y cruce opt-in, no duplicar por variantes.

## APK/contrato intactos y entrega

Recomprobación local `Get-FileHash` después de pruebas/lint, SIN recompilar APK:
- Debug `android/app-child/build/outputs/apk/debug/app-child-debug.apk` SHA256
  **`6221f3f52a7bdfbe4a4da19627c00821598fb2b2f9a59989a82e846e28250751`**.
- Releaseunsigned SHA256
  **`029d4e2dd876f87c061f4bace6e2fb4fbaaad76dddcdd5ae5116b7f5a285b3b4`**.
- Contrato R1 hash089e0b62…492adde idéntico. No cambios productivos ni configuración
  de transporte/TLS/DNS/Keystore/CP1/CP2/enforcement. La configuración Gradle nueva
  sólo afecta Test, y helper/prueba sólo src/test; no empaquetarlos en MAIN.
- Rutas modificadas: build.gradle.kts(Testoptin), helpers/tests TLS-Node,
  ESTADO/evidencia/relevo. Revisar gitdiffcheck antes de commit de cierre.

**Cerrar y detener**. Sin ADB/dispositivos/instalación/QR real/identidades reales/
códigosadultos/familiares/servicios públicos/bootstrap/push/deploy/subagentes/
sesiones/prompts/lectura-ediciónotrosrepos. Perfil público aún desactivado según
coordinación; habilitación/instalación/ceremonia posterior requiere permiso
concreto nuevo. WSSlocal/DONE no confirma dosAPK/identidadhumana/Android/USO.
