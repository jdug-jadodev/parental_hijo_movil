# HIJO-F03-PREPARACION — actualización conservadora y ceremonia manual futura

## Estado y alcance

15 de octubre de 2026. Preparación local terminada; **esperando habilitación
DIRECTA del propietario en esta sesión**. No ADB, inventario, consulta de teléfono,
instalación, QR, cámara, aceptación real, AUTH, órdenes ni contacto remoto.
Una unidad, sin rebuild/tests repetidos, subagentes, otros repos, push o deploy.
E2E01 conserva su cierre de implementación/JVM; no acredita efectos físicos.

### Coordinación recibida, no verificada contra servicios o repositorios

- Padre/hijo E2E01 cerradas. RenderCP2-P01 preparado en `2fb1ea6`,189tests y
  proceso Node compilado con Render SIMULADO/ambos AUTH: resultados REPORTADOS,
  no ejecutados aquí ni pruebas de Android/relay público CP/2.
- Origen real acordado: **`https://parental-0j3o.onrender.com`**. Servicio público
  aún bootstrap `aa2bfbb`; keepalive200/health503/cp.v1 rechazado503/cp.v2
  rechazado400 son comprobaciones públicas REPORTADAS, no repetidas aquí.
  Conocer la URL no significa que el relay CP/2 esté habilitado.
- Perfil público futuro: misma PUBLIC_ORIGIN, ACTIVE_PAIR_ID minúsculo y ambas
  públicas reales de ESA aceptación. Exportador padre llama PAIR_ID al valor que
  Render mapea a ACTIVE_PAIR_ID. Sin claves reales de esa aceptación no habilitar
  relay. Esta guía no recoge claves ni configura servidores. Autodeploy activo:
  no push/deploy; timer keepalive y perfil público siguen locales según reporte.

## Preflight exclusivamente local realizado

Artefacto existente, sin recompilar:
`android/app-child/build/outputs/apk/debug/app-child-debug.apk`.

| Comprobación | Resultado local |
| --- | --- |
| SHA256 APK (`Get-FileHash`) | `fc7a5bd82e6bfdcb0ae0434ce2d88be03eac2cc1390d73a339c9eb52c83108ed` |
| Firma (`apksigner verify --print-certs`, build-tools36.0.0) | Verificada, Android Debug |
| Certificado firmante SHA256 | `39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439` |
| Paquete (`aapt dump badging`, build-tools36.0.0) | `dev.controlparental.child` |
| versionCode / versionName | `1` / `0.1.0` |
| minSdk / targetSdk | `31` / `36` |
| Variante | Debug; testOnly; actualización prevista `-r -t`, nunca `-d` |

`git show 71e4e2a:android/app-child/build.gradle.kts` confirma igual paquete,
versionCode/versionName/minSdk/targetSdk en configuración PAIR01. Es coherencia
de fuentes, **no comparación binaria con la instalación actual**. El último APK
instalado acreditado por `EVIDENCIA_HIJO_PAIR01.md` tiene SHA256
`0d363886e50f908e88188a4e5b9e40ea5aa7c57569b0e7e80aa63db2124344e9`.
Ese hash no demuestra el certificado ni qué versión está instalada hoy.

No se obtuvo copia local del APK PAIR01 para comparar certificados. La búsqueda
por nombre APK en el temporal aprobado no la encontró en el nivel superior;
la búsqueda recursiva dio acceso denegado en dos subdirectorios y timeout, sin
resultado utilizable. No se reintentó ni se amplió a otros repositorios.
**Compatibilidad incremental definitiva pendiente** de lectura autorizada de
paquete/versión/certificado instalado. No reconstruir PAIR01 ni crear claves para
suplir esa comprobación. Certificado APK no es identidad CP/2 de Android Keystore.

Revisión local de flujo existente: `EVIDENCIA_HIJO_PAIR03.md` y
`ui/AccesoAdultoActivity.kt` documentan consentimiento independiente, tickets
por propósito, marco original120s y guardado separado/readback. No se ejecutó UI.

## Puerta de permiso — antes incluso de inventario

Solicitar directamente al propietario en esta sesión permiso limitado para:
identificar el único A13 SM-A135M por ADB, comprobar compatibilidad y estado
mínimo de preservación, y **una sola actualización `install -r -t`**, únicamente
si todas las comprobaciones son compatibles. Si no lo concede, no ejecutar nada.
La respuesta del coordinador, permisos de otra sesión o conexión del cable no
son habilitación. El permiso de actualización **no incluye ceremonia QR**.

## Checklist futuro: UNA actualización, sólo después de permiso directo

- [ ] Confirmar con propietario que salida local/emergencia siguen accesibles y
  que puede operar la pantalla. Botón físico A13 averiado: **cero reboot**, corte
  de red/radios/energía, cambio Owner/PIN/protecciones. Si no hay salida segura,
  detener antes de instalar; no ensayar una recuperación alternativa.
- [ ] Inventario ADB sólo después del permiso: seleccionar único modelo anunciado
  SM-A135M; A56 absolutamente excluido, sin consultas dirigidas a él. Serial sólo
  en RAM/temporal privado, nunca evidencia/chat/repositorio. Ante ambigüedad, parar.
- [ ] Toda operación dirigida al mismo A13 mediante `adb -s <destino_A13>`;
  confirmar modelo en ese destino antes de cualquier lectura posterior. No
  comandos indiscriminados, instrumentación ni selección implícita del primero.
- [ ] Lecturas mínimas autorizadas: paquete, versión y certificado del APK actual;
  confirmar continuidad de Device Owner/protecciones sin modificarlos y estado
  de UI con propietario. No leer/exportar datos CE/DP, códigos, claves, identidad
  privada, preguntas/respuestas o registros de pareja. Si se necesita copia del
  APK instalado para verificar su firma, sólo temporal local privado, no commit.
- [ ] Comparar paquete exacto, certificado SHA256 exacto y versión no superior al
  candidato versionCode1; comprobar que el baseline/legado no requiere migración.
  Si distinta firma/paquete, versión incompatible, legado activo incompatible,
  corrupción, pending incierto o estado inesperado: **DETENER**. No uninstall,
  clear/reset, downgrade, rotación, creación de identidad, reparación ni migración.
- [ ] Recalcular SHA del candidato y comprobar de nuevo destino antes de escribir.
  Sólo entonces, una operación prevista, no ejecutada en esta preparación:
  `adb -s <destino_A13> install -r -t <ruta_del_APK_verificado>`.
  `-r` conserva datos; no acredita por sí solo identidad/credencial preservadas.
  El reemplazo puede reiniciar el proceso de app; no reiniciar el teléfono.
- [ ] Si instalación falla/interrumpe o resultado es ambiguo: registrar fallo
  sanitizado y detener. No repetir instalación automáticamente ni desinstalar.
- [ ] Tras éxito, verificar de forma mínima paquete/versión/certificado y con
  propietario acceso local/emergencia y continuidad de identidad ya existente,
  credencial/Owner/protecciones, sin exportar secretos ni inicializar/reparar.
  Si cualquier comprobación falta, no declarar preservación validada.
- [ ] Registrar resultado/hash, sin serial/códigos/datos sensibles. Parar antes
  del QR y pedir autorización directa separada para la ceremonia manual.

## Guía futura: ceremonia óptica MANUAL, no ejecutarla ahora

1. Tras actualización comprobada y **otra autorización explícita** del propietario,
   usar el mismo padre conocido presencialmente. No operar el otro repositorio
   ni dispositivo desde esta sesión. No enviar QR, capturas, claves o códigos
   por chat/log/repositorio. Los códigos se introducen únicamente en teléfonos.
2. Entrar en sesión adulta del A13. Conservar `privateSigning` existente y su
   registro; **no usar INICIALIZAR_IDENTIDAD**, importar, rotar o generar claves
   para desbloquear el flujo. Si la identidad no está disponible, detener.
3. Sólo por orden del propietario, GENERAR_OFERTA con código fresco por propósito.
   Mostrar QR sólo en sesión adulta/primer plano, marco **original120s**, sin
   renovarlo con tickets. La oferta RAM de5min no amplía esos120s. No regenerar
   oferta/reabrir sesión automáticamente para eludir caducidad.
4. Padre lee ópticamente oferta y genera su aceptación con origen EXACTO
   `https://parental-0j3o.onrender.com`. No inventar otro host, path, puerto,
   query ni usar localhost/LAN para completar el campo. No conectar al origen.
5. Hijo recibe aceptación con cámara propia/VALIDAR_PAIR y código fresco. Si falta
   permiso CAMERA, el flujo actual cancela oferta/sesión al pedirlo o negarlo:
   parar y coordinar ese paso explícitamente; no repetir oferta automáticamente.
6. La firma con candidata prueba integridad, **no identidad del padre**. Cotejar
   presencialmente ambas HUELLAS en pantallas independientes del mismo padre y
   A13. Introducir huella PADRE esperada y ORIGEN desde contexto independiente,
   no copiarlos del QR candidato ni aceptar la casilla por rutina. Comparación
   humana explícita del propietario; cualquier diferencia implica cancelar.
7. Otro código fresco VALIDAR_PAIR tras confirmar; luego GUARDAR_PAIR con su
   código fresco y consentimiento explícito. Ninguna operación renueva marco,
   tickets anteriores ni oferta. Si no alcanza120s, cancelar/detener y coordinar
   un nuevo intento explícito; no reducir comprobaciones ni automatizar reintento.
8. Cancelar/fondo/caducidad retira QR/cámara. Reserva/commit incierto/pending:
   no borrar restos, resetear, fabricar virginidad o repetir alta. Conservar
   CP1/guardas legado y solicitar evaluación separada, sin migración implícita.
9. Guardado correcto requiere readback MAINPAIR03 y deja
   `VINCULO_CP2_GUARDADO / PENDIENTE_CANAL_Y_RECUPERACION`. **No NORMAL/USO**, no
   permiso remoto, recuperación ni producto final. No limpiar por estar pendiente.
10. No pulsar CONECTAR_CP2, armar pilotos ni enviar SYNC/LOCK/ALLOW/AUTH. Bootstrap
    público no está habilitado para relayCP2. Configuración/despliegue posterior
    con ESA aceptación requiere trabajo/autorización separados. Un primer piloto
    posterior sería foreground120s; no prometer app cerrada/uso habitual.

## Cierre de preparación y próximo paso

Sólo evidencia/ESTADO/relevo, revisión `git diff --check` y commit documental.
No código/APK modificados, rebuild, suites, ADB, teléfono, QR, claves, remoto,
instalación, LOCK/ALLOW, red/boot, LOCAL07/LOCAL08, Owner/PIN/protecciones,
push/deploy. Certificado/versiones son inspecciones de artefacto local, no test
Android. La comparación instalada y la preservación real **siguen pendientes**.

Próximo paso único: pedir y esperar permiso DIRECTO local para el alcance limitado
de actualización descrito. Sin permiso, no inventariar; sin compatibilidad, no
instalar; sin permiso separado de ceremonia, no generar ni mostrar QR.
