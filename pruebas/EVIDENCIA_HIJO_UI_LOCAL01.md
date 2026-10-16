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

Pendiente misma unidad: revisión final, builddebug/lint, inspecciónAPK/hash,
guía/relevo y commit de cierre. NO ADB/A13/A56/instalación/endurecimiento/
CE-Keystore/Owner/PIN/red/reboot/uninstall-clear/root/reset/push/deploy/remotos.
