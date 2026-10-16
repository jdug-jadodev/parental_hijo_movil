# LOCAL-VALID01 — ronda física parcial y aceptación del propietario

## Petición y permiso directo

El propietario pidió probar pendientes para empezar a usar el hijo y seguir
mejorando después. Se eligió una ronda acotada de validación local relacionada
con H12/H-EMG-03/H-POWER-01, sin declarar cumplidas dependencias ni toda H12.
Matriz histórica CP1 no cambia: el control local vigente tiene NORMAL10s,
sin LOCK_FOR ni relay en esta ronda.

Propietario autorizó DIRECTAMENTE **«Sí, autorizo esta ronda»**: lecturasADB
de paquete/políticas/bloqueo/pantalla/proceso del hijo, despertar pantalla sin
saltar PIN si fuera necesario y guía manual de emergencia sin llamar y tiempo
con otra app/pantalla apagada. Reinicio/cierre forzado quedaban para coordinación
posterior. Dijo que despierta con el dedo sobre el lector de huella del botón
averiado. Se explicó que Android puede exigir PIN después de lockNow; método
de despertar después de ese caso NO comprobado en esta ronda.

## Realmente ejecutado

- Inventario tras permiso: único A13SM-A135M anunciado, destino igual al anterior,
  modelo confirmado. Cada operación dirigida por adb-s; A56 sin consultas
  dirigidas ni cambios, serial sólo contexto temporal externo/RAM.
- `pm path` y `sha256sum` del APK instalado acreditaron
  **e097e002434aee19982eb0f643c89ef65f30665909c6f3978dd532ba8133bce5**.
  No nueva instalación, recompilación ni acceso a contenidosCE/DP/Keystore.
- Baseline de Activity: **mLockTaskModeState=NONE**. Proceso del hijo presente
  por pidof; no se mató ni detuvo. Políticas resueltas: uninstallBlockedhijo=true,
  no_control_appsusuario=true; entrada global independiente sinadmins/null;
  LockTask paquetesvacíos/flags16. La política de protección sigue activa.
- Primer filtro shell remoto de dumpsys power falló por comillas (Display
  inaccessible/Broken pipe); no se interpretó como dato de pantalla. Se corrigió
  filtrando el resultado en memoria local: Wakefulness=Awake, Keyguardshowing=false,
  inputRestricted=false, occluded=false, SCREEN_STATE_ON/INTERACTIVE_STATE_AWAKE.
  Ninguna escritura por ese fallo ni por su corrección.
- Se preguntó por NONE y por el permiso actual antes de iniciar más pruebas;
  no se atribuyó automáticamente a fallo del controlador ni a permiso válido.

## Resultado reportado y aceptación

El propietario respondió que estaba probando sin internet, que se bloquea
correctamente, que no puede desinstalarse y que lo da por exitoso hasta detectar
otra forma de desinstalar. Se registra como **aceptación humana de esas pruebas
locales**, no certificación general ni resistencia a todas las rutas de evasión.

No especificó permiso/modalidad exactos, instante de corte, reloj de referencia
ni secuencia completa de acciones. Por tanto NO acreditar desde esta respuesta:
latencia10s medida, EXCEPCIÓN_OFFLINE, reconexiónqueNOabre, faseactual del permiso,
o bloqueo continuo con otro proceso delante. NONE fue un snapshot previo durante
sus pruebas; estado final de uso/bloqueo NO vuelto a consultar. Desinstalación
normal impedida es reporte humano, coherente con uninstallBlocked leído; no
se ejecutó uninstall/clear ni otras técnicas para probar evasión.

La ronda se cierra PARCIAL por aceptación expresa del propietario, conservando
pendientes: emergencia hasta marcador sin llamar bajo políticas actuales,
tiempo con otraapp/pantallaapagada, proceso muerto y recuperación, reinicio,
recuperación por pregunta hasta cambiar código y casos de red medidos. No se
aprobó entrega como versión final ni se levantaron pendientes G0/G2/G4.

## Relevo

Sin comandos para apagar/despertar pantalla, cambiar red, matar proceso,
reiniciar, mutar políticas, abrir apps/UI porADB, capturar secretos o llamar.
No se retiró protección ni se alteró permiso de uso. C8/padre/Render pausados,
sin otrosrepos/subagentes/push/deploy. No nuevos tests/builds por APK inalterado;
diff-check y commit documental. Último instalado e097e002…33bce5, protección
activa por última lectura, uso final no acreditado. No iniciar otra prueba física
por este relevo: concretar nuevo caso/alcance con el propietario. Si informa un
fallo, registrar acción, estado previo/resultado y reproducir sin pedir códigos.
