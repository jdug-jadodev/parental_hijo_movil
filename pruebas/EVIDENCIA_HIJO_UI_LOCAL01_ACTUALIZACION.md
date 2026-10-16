# UI-LOCAL01 — actualización física autorizada

El propietario pidió «ok, hazlo» y confirmó DIRECTAMENTE «Sí, todo listo» a
A13SM-A135M conectado/autorizado, pantalla operable, código adulto habitual y
salida/emergencia disponibles, comprobaciónADB y UNA actualización conservadora.
La confirmación es humana, no captura ni lectura de credenciales. No permiso
heredado de Render/otra sesión. Actualización y activación adulta posterior
completadas según los alcances directos registrados abajo.

## Ejecutado

- InventarioADB en RAM: únicoA13 por modelo anunciado; getprop SM-A135M confirmado.
  Cada operación dirigida al mismo destino mediante adb-s. A56 sin consultas
  dirigidas/cambios. Serial sólo en contexto temporal externo, no repositorio.
- APKprevio recuperado, SHA256
  `6221f3f52a7bdfbe4a4da19627c00821598fb2b2f9a59989a82e846e28250751`,
  igual últimoF04. Firma verificada/certificadoSHA256
  `39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439`,
  paquete dev.controlparental.child/versión1-0.1.0/min31target36/testOnly compatibles.
- Primer script preflight se interrumpió porque PowerShell trató como error el
  mensaje informativo stderr de adb-pull, aunque la copia acabó. Se verificó ese
  archivo y se retomó preflight; NO instalación ni reintento de instalación allí.
- Candidato local y destino reconfirmados; **UNA `install -r -t` → Success**.
  Sin downgrade/uninstall/clear ni reinstalar APKantiguo.
- APKposterior recuperado/verificado SHA256 exacto
  **`2b5ea13052d7c45a34bf8b98f4f95667ef074e0a3a58468797e22573b815a821`**,
  mismo certificado correcto. Cero diferencias paquete/versión/firstInstallTime/
  ceDataInode/estado de instalación consultado. Directorio continuo, NO auditoría
  semántica de datos/identidad/credencial/PAIR/pending.
- DeviceOwner ChildAdminReceiver y baseline comparado iguales. uninstallBlocked
  hijo=false; no_control_apps sinadmins/valorresuelto null; LockTask paquetesvacíos/
  flags16. **Protección NO activada** por actualizar.

No lectura de contenidosCE/DP/Keystore/credenciales/identidad/PAIR, no UI-ADB/
capturas/códigos, sin reboot/cortesred-energía/Owner/PIN/root/proteccionesnuevas/
otrasapps/C8/QR/canal/remotos/push/deploy. Sin rebuild ni tests repetidos.

## Comprobación humana y activación posterior

1. Propietario respondió **«Sí, funciona»** a nuevaUI «Control del teléfono»,
   entrada con código habitual, cierre de acceso adulto y salida/emergencia
   disponibles sin errores. Sin capturas ni códigos en chat.
2. Autorización DIRECTA NUEVA **«Sí, activar y comprobar»** después de explicar
   el alcance: bloquear desinstalación del hijo y controles de TODAS las apps
   (borrado, deshabilitar, forzar detención), sin restricción global de instalación.
   Alcance técnico adicional: sólo lecturaADB de esas políticas del mismoA13,
   sin reiniciar ni probar desinstalaciones.
3. Propietario ejecutó manualmente «Acceso adulto → Entrar → Seguridad y
   configuración → Activar protección de la app», código fresco sólo teléfono
   y «Confirmar»; respondió **«Protección activa»** al resultado mostrado.
4. getprop reconfirmó SM-A135M en el mismo destino explícito. Lectura filtrada
   de dumpsys device_policy acreditó uninstallBlocked del hijo: contribución
   ChildAdminReceiver=true, valor resuelto=true. no_control_apps por usuario:
   contribución del mismo admin=true, valor resuelto=true. Entrada global
   independiente sigue sinadmins/null. DeviceOwner ChildAdminReceiver intacto.
   **Protección ahora ACTIVA según UI reportada y políticas leídas**, no sólo
   por Owner/APK. Ninguna escritura de política por ADB.
5. Propietario respondió **«Sí, todo funciona»** a POSTprotección: entrada de
   nuevo con código habitual, «Cerrar acceso adulto» y opción de emergencia
   visible. No se solicitó apagar pantalla ni llamada por botón físico averiado.

## Cierre y límites

Actualización conservadora, UI humana y activación adulta de protección cerradas.
No se intentó desinstalar/deshabilitar/borrar datos ni se probó root/reset; no
promesa de invulnerabilidad. Recuperación por pregunta no ejercitada hasta
cambiar código; no nueva llamada ni prueba completa de emergencia/Keyguard.
No se leyó identidad/PAIR/pending para afirmar conservación semántica.

**«Preparar control local» NO ejecutado en esta unidad**, ni permisos de uso,
temporizadores/bloqueo de arranque/red nuevos. No atribuir estas pruebas a la
activación antidesinstalación. Cualquier fase física adicional requiere alcance
concreto directo nuevo; no otra instalación ni ceremonia/remoto autorizados.
Sin nuevos builds/tests porque APK intacto. Revisión documental diff-check y
commit de cierre; A56 sin consultas dirigidas ni cambios.
