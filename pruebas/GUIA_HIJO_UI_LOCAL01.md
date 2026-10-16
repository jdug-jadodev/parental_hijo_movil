# Control local — guía breve del APK nuevo

**Pantalla apagada corregida y probada (16/10):** con NORMAL1min y Calculadora
delante, el tiempo venció mientras la pantalla estaba apagada. Al despertar,
el A13 volvió solo al bloqueo sin abrir el hijo; Android confirmó LOCKED unos
0,67s después del aviso de desbloqueo. Es el resultado de ese ensayo, no un plazo
garantizado ni validación de reposo profundo. Detalles: `EVIDENCIA_HIJO_LOCAL_PANTALLA01.md`.

**Corrección instalada y comprobada:** «Bloquear ahora» funciona después de permitir
uso. En la prueba coordinada de muerte del proceso con Calculadora delante,
el A13 volvió solo al bloqueo; primera lectura LOCKED a los **5,18 segundos**.
Hay una ventana de recuperación: no es instantáneo ni un plazo máximo garantizado.
La protección antidesinstalación se conservó y quedó bloqueado al terminar.
Si reaparece «Estado por comprobar», comunicar el caso sin borrar datos ni retirar
protección. Detalles y límites: `EVIDENCIA_HIJO_LOCAL_PROCESO01.md`.

Última comprobación adicional: el propietario informó bloqueo correcto al
probar sin internet y desinstalación normal impedida. Aceptó esas pruebas locales.
Evidencia `EVIDENCIA_HIJO_LOCAL_VALID01.md`: no sustituye los casos todavía
pendientes de pantalla apagada, proceso muerto, reinicio y recuperación/emergencia.

## ¿Qué ocurre al liberar RAM?

No se borra la configuración guardada. Código adulto/pregunta se conservan en
almacenamiento cifrado; la protección queda en Android. La sesión adulta y el
permiso de uso son temporales: si se cierra el proceso, hay que entrar y autorizar
otra vez. No equivale a «Borrar datos» de la aplicación.

**Corrección posterior instalada y probada:** el APK anterior UI-LOCAL01 exigía
protección desactivada para preparar. Se corrigió en LOCAL-COMPAT01, instalado
APKe097e002…33bce5 con autorización directa. Control local preparado manualmente
sin retirar protección; propietario confirmó permisoNORMAL1min, contador y
bloqueo al terminar. Acceso adulto/cierre/emergencia visible correctos después.
Protección activa acreditada por políticas. No hace falta volver a preparar ni
desactivar protección para cada permiso. Evidencia `EVIDENCIA_HIJO_LOCAL_COMPAT01.md`.

## Actualización anterior (histórica)

**Actualizado en el A13 y protección activada con permiso directo posterior.**
APK2b5ea130…15a821 verificado; UI/acceso adulto/cierre/emergencia visible confirmados
por el propietario. Antidesinstalación y restricción de controles verificadas
por lecturaADB tras activación manual. «Preparar control local» y nuevos permisos
todavía NO ejecutados: coordinar esa fase concreta antes de probarla. Botón físico
averiado; no reiniciar ni cortar red/energía. A56 fuera de alcance.

## Uso habitual (después de la actualización/prueba autorizadas)

1. **Mi teléfono → Acceso adulto → Entrar** con tu código privado. No enviarlo
   por chat. En **Control del teléfono** se ve estado, tiempo, modalidad y protección.
2. **BLOQUEAR AHORA → Confirmar** con código cancela el permiso anterior.
3. **PERMITIR LIBRE** o **PERMITIR POR TIEMPO** → **Normal · con internet**.
   Para tiempo, escribir entre **1 y1440** minutos enteros; no hay valor elegido.
   **Confirmar** con código. La última acción válida confirmada reemplaza la anterior.
4. Si hace falta usar sin internet, elegir **Excepción · sin internet** y marcar
   **Autorizo la excepción sin internet**, luego confirmar con código. No aparece
   por reconectar, recuperar el código o caducar el permiso.
5. **Cerrar acceso adulto** no cancela el permiso concedido. **Mis aplicaciones**
   aparece en inicio sólo durante permiso efectivo. El tiempo sigue con pantalla
   apagada, en otras apps y al cerrar el panel. NORMAL pierde permiso tras10s
   sin internet validada; reconectar no desbloquea. Reiniciar invalida permisos.

## Seguridad

**Seguridad y configuración** muestra protección real, no asumida por tener Owner.
**Activar protección de la app → Confirmar** requiere código y prueba presencial
coordinada: impide desinstalar el hijo y restringe controles de TODAS las apps
(borrado, deshabilitar, forzar detención). No añade bloqueo general de instalación.
**Restaurar protección anterior** también exige código. No garantiza resistencia
a root/reset físico. Última lectura posterior a la activación acredita protección
activa; la lectura inactiva F04 corresponde al estado anterior.

Si los botones de permitir están desactivados, preparar/reanudar el control
desde Seguridad durante la prueba autorizada. **Preparar control local** deja
bloqueado y exige notificación visible; NO concede uso. Si figura estado por
comprobar o la preparación no está disponible, consultar **Diagnóstico y
recuperación avanzada**, no borrar datos ni regenerar identidad/pareja.

**Olvidé mi código** pregunta tu respuesta personalizada sin internet. Sólo
permite establecer otro código, nunca muestra el viejo ni concede uso.
Códigos5–10dígitos; se conservan ceros iniciales. Sesión adulta2min; salir/fondo/
muerte la termina. Cancelar un formulario no cambia un permiso ya confirmado.

**Emergencia · pantalla del sistema** conserva la ruta existente a Keyguard;
puede apagar la pantalla. Con el botón físico averiado no probarla a ciegas:
coordinar primero cómo recuperar la pantalla. No realizar llamadas de prueba.
La recuperación avanzada previa permanece en **Ayuda y recuperación** del inicio.
Vinculación/padre/servidor pausados, sin conexiones automáticas ni códigos nuevos.
