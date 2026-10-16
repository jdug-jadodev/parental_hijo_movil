# HIJO-UI-LOCAL01 — control local, sin teléfono

## Bloque de interfaz

Cambio de prioridad explícito del propietario: C8-T10 pausada en checkpoint
`490495d`, NO finalizada ni instalada. Avances/contratos/identidades/PAIR/pending
conservados. Ningún cruce nuevo ni acceso a otros repos en esta unidad.

Ruta habitual MAIN: Mi teléfono → Acceso adulto → Control del teléfono. Resumen
derivado de modo Lock Task observado, permiso RAM/vencimiento elapsed y lectura
de protección; NONE sin permiso no se presenta como permitido, error/PINNED no
se presentan como bloqueado. Contador actualiza cada segundo mientras visible,
no modifica plazos/autoridad. Botones grandes/contraste/espacios en dp y sp.
Seguridad y diagnóstico separados. C8/QR/conexión/inicialización sin accesos
desde UI; consulta secundaria del vínculo sólo lectura sin generar ni conectar.

Seguridad: ruta existente ProteccionLocal/PoliticasProteccionAndroid conserva
setUninstallBlocked(hijo) + DISALLOW_APPS_CONTROL(usuario), snapshot previo y
verificación. No faltaba implementación de protección. Activación/restauración
sólo código adulto fresco; NO onCreate/boot automático. Restricción de controles
de TODAS las apps advertida antes de confirmar; no nueva restricción genérica
de instalación. No invulnerabilidad root/reset; no se afirma activa en A13.
F04 histórico uninstallBlocked=false/restricción inactiva sigue última lectura.

Sesión adulta y guardas/enforcement sin cambios. Navegar no concede/revoca uso;
confirmaciones conservan propósitos y códigos separados, minutos1..1440 vacíos,
checkbox excepción sin preselección, tiempo elapsed con pantalla apagada,
boot/proceso invalida, NORMAL10s y reconectar no abre. Cierre/fondo invalida RAM
adulta, no llama a revocar el permiso confirmado. Guardia de vista compartida
probada descarta vista retirada/fondo/reentrancia; callbacks existentes por
consulta/generación/autoridad se conservan. No amplitud de administración por C8.

Prueba focal ejecutada desde android (sin Gradle rutaRelay):

```text
.\gradlew.bat :app-child:testDebugUnitTest --tests dev.controlparental.child.ui.PresentacionLocalTest --tests dev.controlparental.child.local.SesionAdultoTest --tests dev.controlparental.child.local.AccesoAdulto*Test --tests dev.controlparental.child.network.*Test --tests dev.controlparental.child.protection.*Test --tests dev.controlparental.child.lock.*Test --tests dev.controlparental.child.boot.*Test --offline --console=plain
```

SUCCESS10s/7ejecutadas20UPTODATE; XML **158 casos/0fallos/0errores** en14clases
(presentación/navegación7nuevos). Compilación MAIN/debug y tests JVM efectivas.
No suite histórica completa ni cruce Node. Modelo/presentación Kotlin reales,
reloj/políticas/disco/autoridad de pruebas FALSOS. No renderizado Android ni
enforcement físico, no Keystore real. Conteo sujeto al XML preservado del pase.

## Cierre local de preparación (sin aceptación física)

Bloque1 `af7908d`. Revisión posterior: se añadió «Mis aplicaciones» al HOME
durante permiso confirmado/NONE/observador listo, con comprobación nuevamente
al abrir cada componente y en límites exactos de tiempo/gracia10s. Excepción
offline conserva vencimiento. Query MAIN+LAUNCHER sólo tras pulsación, RAM,
descarta resultados tras fondo/cambio/pérdida de permiso; sin historial ni
QUERY_ALL_PACKAGES. No cambia HOME persistente ni lista/políticas existentes,
no autoriza por navegar. No certifica seguridad universal de otras apps.
Actualizaciones/instalaciones siguen sin restricciones genéricas nuevas.
El segundo icono del prototipo DEBUG se retiró; componente explícito/rollback/
servicio del laboratorio conservados, no se deshabilitó recuperación.

Resumen también deja de afirmar «Permitido» si vence gracia de NORMAL aunque
el callback de enforcement esté pendiente. Modo NONE sin permiso=«Estado por
comprobar», nunca falso «Bloqueado»/«Permitido». En ese caso consultar diagnóstico.

Pasadas posteriores:
- `:app-child:testDebugUnitTest --tests dev.controlparental.child.ui.PresentacionLocalTest
  :app-child:assembleDebug :app-child:lintDebug --offline --console=plain`:
  **FAILED59s**, 22ejecutadas34UPTODATE. Tests8/0fallos y assembleDebug completados,
  pero lintDebug **1error31avisos**, error ForegroundServicePermission en servicio
  anterior `src/debug/AndroidManifest.xml`/ServicioTemporal(systemExempted).
  NO se afirma pasada exitosa ni se añaden permisos/suppress para ocultarlo.
- `:app-child:lintRelease --offline --console=plain`: SUCCESS33s,
  12ejecutadas17UPTODATE, MAIN compilado release sin buildrelease.
- Tras revisión final (guardia doble pulsación apps/retirada iconoDEBUG):
  `:app-child:testDebugUnitTest --tests dev.controlparental.child.ui.PresentacionLocalTest
  :app-child:assembleDebug :app-child:lintRelease --offline --console=plain`:
  **SUCCESS23s/20ejecutadas50UPTODATE**, XML8/0fallos/0errores/0omitidos,
  APKdebug empaquetado y lintRelease **0errores27avisos**. No repetir suite
  histórica completa. Total de casos distintos de esta unidad: **159**
  (158 del primer pase más nuevo caso cajónApps; no sumar pases repetidos).

Los27avisos Release son7versiones-dependencias/12recursosya-no-usados/
8SetTextI18n (7checkpointT10conservado y1checkboxoffline); no ignorados.
lintDebug sigue NO aprobado por el servicio del prototipo señalado; no implica
fallo demostrado del servicio productivo ni acredita funcionamiento físico.

## APK inspeccionado, NO instalado

Ruta: `android/app-child/build/outputs/apk/debug/app-child-debug.apk`.
SHA256 final **2b5ea13052d7c45a34bf8b98f4f95667ef074e0a3a58468797e22573b815a821**.
APK intermedio de2182c7…411bc6 fue reemplazado por el build final; NO usarlo.
`apksigner verify --print-certs` correcto; certificadoSHA256
**39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439**,
igual al certificado históricoF04, sin consulta instalada actual.
`aapt dump badging`: paquete dev.controlparental.child/versión1-0.1.0/
min31-target36/testOnly, un solo LAUNCHER productivo LauncherActivity.
Sin permisos nuevos; queries sólo MAIN/LAUNCHER en manifest. InspecciónZIP/DEX:
LauncherActivity/AccesoAdultoActivity/PresentacionLocalKt/GuardiaVistaLocal/
ProteccionAndroid/ControlRedAndroid presentes; ceroassets-fixtures-schema-
contratos-testXML. No descompilación ni análisis exhaustivo de seguridad.
`git diff --check` correcto antes del cierre.

### Límites y siguiente paso

Preparación local cerrada, **apariencia/tacto/navegación real/Keyguard/Keystore/
AtomicFile/enforcement/home/instalación-actualización/protección física sin
validar en esta unidad**. JVM no sustituye Android físico. Último instalado
continúa C8debug6221f3…250751 y protección F04 inactiva según última lectura;
el APK nuevo no está en el A13. No declarar «no la puede quitar».

Guía `pruebas/GUIA_HIJO_UI_LOCAL01.md`; relevo `pruebas/HANDOFF_HIJO_UI_LOCAL01.md`.
Detener tras commit. Cualquier lecturaADB/actualización conservadora/activación
y prueba local necesita permiso DIRECTO NUEVO aquí, coordinado por botón físico
averiado, con acceso adulto y salida/emergencia comprobados de nuevo antes de
cambios; únicoA13SM-A135M/selector explícito, A56excluido. Si estado/firma/datos
inciertos detener sin clear/uninstall/reset/migración. No ejecutar permisos de
otras sesiones. NO ADB/A13/A56/instalación/endurecimiento/CE-Keystore/Owner/PIN/
red/reboot/uninstall-clear/root/reset/push/deploy/remotos/otrosrepos/subagentes.
