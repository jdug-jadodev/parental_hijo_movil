# HIJO-L02 — agregado temporal FICTICIAS_JVM

2026-10-12. Base documental C06 `ae1c7eb`. **Implementación exclusivamente de
laboratorio JVM**, sin decidir medición humana/día/retención/UsageStats/marcador.
No funcionamiento Android, bloqueo, emergencia o avisos demostrado por estos tests.

## Aislamiento y rutas

- Modelo: `android/app-child/src/test/java/dev/controlparental/child/laboratorio/AgregadoFicticio.kt`.
- Tests/fake en memoria: misma ruta, `AgregadoFicticioTest.kt`.
- Evidencia: este archivo, `ESTADO.md`, `COORDINACION_PADRE_HIJO_RENDER.md`.

Gradle no modificado, sin dependencias nuevas. `src/test/java` ya compila en pruebas
locales JVM, no en main o androidTest. Tipos internos, package `child.laboratorio`,
sin imports Android/IO/red ni vínculo al protocolo. JUnit4 existente. Solo Long,
enums/registros constantes; ningún paquete/app/persona/evento/secreto/lista de uso.
467 líneas Kotlin propias (166 modelo + 301 tests), único commit de cierre.

Inspección posterior al comando JVM, sin assemble/install:

- `build/tmp/kotlin-classes/debugUnitTest/dev/controlparental/child/laboratorio`:
  **12 clases** (modelo, valores, test y fake); `AgregadoFicticio.class` y Test presentes.
- Mismo subdirectorio de `kotlin-classes/debug`: **0 clases**.
- `build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar`
  y `runtime_app_classes_jar/debug/bundleDebugClassesToRuntimeJar/classes.jar`:
  **0 entradas** `child/laboratorio/`, comprobado con ZipFile .NET.
- Búsqueda `child\.laboratorio|AgregadoFicticio|RegistroFicticio|SnapshotFicticio|VentanaFicticia`
  en `src/main` y `src/androidTest`: **sin coincidencias**. Búsqueda inicial de la
  palabra genérica «laboratorio» sí encontró textos/tests antiguos ajenos a L02;
  no son referencias al componente y no se modificaron.

Son evidencias de exclusión por sourceSet y clases principales compiladas **debug**,
no inspección de un APK nuevo o prueba release/instrumentada. Tareas main quedaron
UP-TO-DATE; el sourceSet de producción no recibe este código. No assemble, dex,
lint, release, instrumentación, APK o dispositivos ejecutados en esta unidad.

## Política conservadora solo de laboratorio

Entradas ficticias explícitas: ventana (época, límites elapsed, límite del subtotal/
cobertura), contexto (boot/generación de observador), ahora elapsed, [inicio,fin) y
clasificación SUMABLE/NO_SUMABLE/DESCONOCIDA. `maxIntervalo` inyectado, nunca hora
civil/Android implícitos. No entrada de permiso/ACK/seq/código/heartbeat. No detector.

- Registro mínimo vigente: ventana/contexto/cursor/último ahora comprometido/subtotal/
  cobertura/parcial/revisión local. Repositorio fake CAS + readback exacto; escritor
  **serial**, sin pretensión de seguridad multihilo. Espacio O(1), sin historial.
- Sumable añade duración sintética; no-sumable añade solo cobertura de evidencia.
  Desconocido y huecos avanzan cursor sin sumar ni crear cobertura. Si aún no hay
  cobertura, snapshot subtotal `null`, no cero observado. Subtotal 0 con cobertura
  conocida no-sumable sí puede ser evidencia sintética de cero sumable.
- Calidad COMPLETA_FICTICIA significa cobertura sintética continua **hasta cursor**,
  no ventana/día completo, precisión humana o futuro observado. Huecos/reentrada
  quedan PARCIAL; lectura/commit inciertos NO_DISPONIBLE conserva último subtotal
  verificado si existe, sin publicarlo como total disponible.
- Cursor es una barrera conservadora: duplicados/atrasados totalmente consumidos
  se ignoran; solapamiento solo admite sufijo nuevo. No intenta decidir qué
  clasificación anterior era correcta ni reparar huecos retrospectivamente.
  Sin historial, **el subtotal no es independiente del orden de llegada**: entrada
  retrasada puede quedar sin sumar, marcado parcial, nunca doble suma. Duplicado
  ignorado no mueve reloj/revisión; regresión se compara con último ahora comprometido.
- Input negativo, vacío/invertido, futuro (`fin > ahora`), regresivo, contexto/época
  no vigentes, exceso de límite o overflow se rechaza sin commit. Ahora es dato
  inyectado, no prueba independiente de realidad. Sumas/revisión usan addExact.
- Objeto recreado no queda activo por leer datos: exige reentrada explícita con
  generación nueva o boot mayor. Reiniciar contexto conserva subtotal/calidad,
  cambia cursor a ahora sin inferir tramo perdido. Boot nuevo puede remapear límites
  de **la misma** ventana sintética con idéntico límite; calidad siempre parcial.
- Cruce del límite recorta a ventana vigente y devuelve RECORTADO; sufijo no
  almacenado/reprocesado automáticamente. Cambiar ventana exige operación explícita,
  época creciente/límite adyacente/contexto vigente; reemplaza único registro, no
  conserva día anterior. Si se cambia después del límite, prefix desconocido queda
  parcial. Ventanas 23/25h son valores de tests, no zona o calendario de producto.
- Ventana incoherente/remapeo en mismo boot no resetea. No intenta corregir reloj
  civil, normalizar fecha o deducir medianoche; rechazo diagnostica la entrada.
- Fallo CAS, throw antes/después, pérdida silenciosa o readback fallido => INCIERTO,
  jamás CONFIRMADO por solo invocar repositorio. Releer y reentrar explícitamente
  permite recuperar registro completo y no repetir la suma del commit incierto.
  No rehabilitar silenciosamente observación/rollover tras incertidumbre.
- Relectura rechaza registros inválidos o regresión respecto a último verificado
  (revisión, época, contexto/reloj, subtotal/cobertura/calidad). Un objeto nuevo
  **no demuestra antirollback** sin historia/autoridad externa: fake CAS se supone
  íntegro; no aplicar esta limitación al teléfono como fallo final aceptado.

No hay marcador de vencimiento ni interfaz implementada para él: retención no
aprobada. Snapshots absolutos diagnósticos, sin firma/serialización/DTO CP1 ni sync
real. La suma no certifica «menor usando», tiempo autorizado ni total real del día.

## Ejecuciones reales y todos los fallos

PowerShell en `android/`, JDK de Android Studio:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app-child:testDebugUnitTest --tests 'dev.controlparental.child.laboratorio.AgregadoFicticioTest' --console=plain --offline
```

**Primera ejecución: BUILD FAILED, 19 tests, 1 fallo, 0 errores/omitidos.** Caso
`lectura corrupta o ausente no inicializa y conserva subtotal verificado`, línea
187 en esa revisión: `expected:<RECHAZADO> but was:<INCIERTO>`. La expectativa era
incorrecta para registro perdido tras lectura verificada; modelo conserva subtotal
y no inicializa. Cambiada expectativa a INCIERTO, sin relajar implementación.
En revisión también se ajustó test de revisión Long.MAX_VALUE para ejercitar
overflow de reentrada, no quedar en guardia de objeto no iniciado. Añadidos tres
casos: ventanas iniciales inválidas, orden atrasado y boot regresivo con revisión
mayor. No se reejecutó comando sin cambios «hasta verde».

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest --console=plain --offline
```

**Segunda ejecución: BUILD SUCCESSFUL.** App-child debug **199 ejecutados**:
22 L02 + 177 anteriores, 0 fallos/errores/omitidos. Núcleo **169 resultados XML
UP-TO-DATE**, no 169 tests nuevamente ejecutados. 368 resultados consultables
totales; no sumarlos a la primera ejecución como casos nuevos. XML de la primera
ejecución reemplazado por la segunda; fallo y causa conservados aquí.

Conteos leídos de `app-child/build/test-results/testDebugUnitTest/TEST-*.xml` y
`core-protocol/build/test-results/test/TEST-*.xml` con [xml] PowerShell; clase nueva
`TEST-dev.controlparental.child.laboratorio.AgregadoFicticioTest.xml`: 22/0/0/0.

### Cobertura de los 22 tests

Inicio sin cero, intervalos adyacentes/disjuntos/exclusión, duplicado/solapamiento,
hueco/desconocido/atrasados sin reparación, input vacío/negativo/futuro/regresivo,
época/contexto/snapshot viejo, recreación y boot, ventanas 23/25h/recorte/rollover,
ancla incoherente/sin doble reset, maxIntervalo/extremos Long/overflow/revisión,
lectura corrupta/ausente, CAS/commit omitido/throw antes y después/readback, rollback
con revisión igual o mayor y 10000 entradas manteniendo un registro. No colección
de intervalos ni pruebas con apps reales.

## Pendientes sin autorización nueva

Producto: significado uso/total y fuente/acceso especial, día/zona/incertidumbre/
retención/marcador y entrega de avisos con padre cerrado, versión/contrato/puertas.
H05 sigue abierta/H06 no autorizada; máxima resistencia exige corregir fallos reales,
no aceptarlos finales. Instalación libre/todas apps/credencial/permiso offline/reboot
siguen confirmados. CP1/fixtures/PLAN_HIJO/ADR aceptados y repos ajenos intactos.
Sin KDF/UsageStats/Keystore/Android/UI/persistencia real/enforcement/WSS/notificaciones,
ADB/teléfonos/permisos/CA/LAN/push/despliegue o QR02. Esperar revisión/asignación;
no próxima tarea automática o prototipo físico autorizado por esta evidencia.
