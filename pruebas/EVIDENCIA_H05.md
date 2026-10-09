# Evidencia H05 — recuperación local antes de endurecer

Fecha: 9 de octubre de 2026. Estado: EN_CURSO — primera unidad JVM.

## Alcance implementado

Núcleo sin Android para códigos impresos CP/1 §9 y §9.A. No está conectado a UI,
Device Owner, Wi-Fi, instalador, revinculación ni borrado. No abre ninguna app ni
modifica restricciones. No hay código maestro, identidad o código fijo en release.
Pruebas usan códigos ficticios exclusivamente bajo src/test y públicas/sobres de
fixtures congelados. No se modificó CP/1, esquema, fixtures ni servidor Render.

- Normalización ASCII: minusculas → mayúsculas, eliminación solo de espacio ASCII
  y guion. Exactamente 32 símbolos A–Z/2–7; no coerciones Unicode ni 0/1/8/9.
  Entrada de texto acotada a 128 caracteres; hash requiere pairId UUID v4.
- Hash exacto SHA256(UTF8("CPv1/RECOVERY\n" + pairId + "\n" + purpose + "\n" + código)).
  Comparación con MessageDigest.isEqual sobre hashes decodificados de 32 bytes.
- Consumo y contador/obligación de espera en la transacción H04. La sesión RAM
  se emite solo tras escritura, lectura posterior y validación comprobadas.
  No se almacenan códigos recibidos ni se escriben logs de intentos/secretos.
- Cinco fallos: 60 s; diez: 300 s. Contador acumulativo saturado en Int.MAX_VALUE,
  sin reset automático por éxito/recreación. Mientras hay espera no cuenta nuevos
  intentos ni gasta códigos. Después de alcanzar un umbral, cada nuevo fallo
  reimpone conservadoramente el margen de ese nivel.
- La espera pendiente persiste; nuevo controlador/boot o incertidumbre de reloj
  la reimpone completa. No se persiste un deadline de reloj civil. Al terminar la
  espera, el indicador se elimina en una escritura comprobada.
- Sesiones por propósito de máximo 5 minutos, solo RAM, vinculadas al boot/vínculo,
  cancelables y revisadas con estado/reloj/usuario desbloqueado en cada consulta.
  No sobreviven recreación, boot distinto, error de lectura ni cambio de vínculo.
- MAINTENANCE produce solo SesionMantenimiento para el motor, sin ALLOWED,
  sin autorizar boot y sin red requerida. Las herramientas reales serán H11.
- RECOVER_PARENT consume código y deja diagnóstico de recuperación pendiente;
  conserva pública/vínculo anterior y no reabre enrolamiento al recrear proceso.
  La revinculación/commit de clave nueva pertenece al flujo posterior autorizado.
- RETIRE consume su código y abre solo autorización local RAM: no tiene llamada
  wipeDevice/wipeData ni ejecuta borrado. Confirmaciones y retirada real son H11.
- Un error de almacenamiento/corrupción no emite sesión. Si el consumo llegó a
  disco pero falló el readback, el código sigue consumido sin devolver acceso;
  no se intenta revertirlo o fabricar una nueva credencial.

Emergencia no depende de este controlador; no se cambió su UI ni su adaptador.
No afirmar que haya sido validada físicamente con estos cambios.

## Pruebas locales ejecutadas

Desde android/, con JDK registrado en BUILD_ENV.md:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:testReleaseUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:assembleRelease :app-child:lintDebug --console=plain
```

Resultado final: BUILD SUCCESSFUL. Informes:

- Núcleo: 169 tests, UP-TO-DATE.
- Hijo debug: 124 tests ejecutados, incluidos **24 nuevos H05**.
- Total núcleo+debug: **293 tests, 0 fallos/errores/omitidos**.
- Hijo release: 117 tests ejecutados, 0 fallos/errores/omitidos (repite casos,
  no sumarlos como nuevos).
- APK debug/instrumentado y release sin firmar construidos; lint debug 0 errores
  y 10 avisos, lint vital release correcto.
- verificar_apk_base.py y verificar_apk_h01.py: OK. Sin fixtures/clases de tests
  en APK app; separación de traza H04 debug/release conservada.

Un intento intermedio del último test de integración con PolicyEngine no compiló
por referirse a modo en lugar de modoSolicitado. Corregido en el test, no cambió
la API del motor; comando completo repetido y resultados finales anteriores.

Casos: hash/normalización/contexto, consumo/replay, sesiones y expiración exacta,
esperas 5/10 intentos, proceso/boot nuevos y reloj incierto, almacenamiento fallido
y readback perdido, corrupción intacta, revinculación/retirada interrumpidas,
dos controladores concurrentes (una sola aceptación), y mantenimiento offline
con boot no autorizado que nunca produce ALLOWED ni autoriza el arranque.
Estos son tests JVM con archivo fake/reloj inyectado, NO recuperación física.

## Rutas modificadas

- android/app-child/src/main/java/dev/controlparental/child/recovery/CodigoRecuperacion.kt
- android/app-child/src/main/java/dev/controlparental/child/recovery/ModelosRecuperacion.kt
- android/app-child/src/main/java/dev/controlparental/child/recovery/RecoveryController.kt
- android/app-child/src/test/java/dev/controlparental/child/recovery/CodigoRecuperacionTest.kt
- android/app-child/src/test/java/dev/controlparental/child/recovery/RecoveryControllerTest.kt
- ESTADO.md y pruebas/EVIDENCIA_H05.md

## Pendientes

Desafío/respuesta OFFLINE con firma confiable, nonce de un solo uso y expiración;
la espera de códigos NO debe bloquear QR firmado válido. Pantalla de código y
sesión limitada con emergencia siempre accesible; limpieza del texto al salir.
Puertos Android y pruebas instrumentadas sin tocar estado real de producción,
identidades efímeras solo de laboratorio. Producción espera vínculo H08.
Revisar integración/cancelación al reemplazar vínculo y singleton por proceso.
H-REC-01/02/03 físicos no aprobados; H05 NO cerrada. No avanzar a endurecimiento.

No se usó ADB, instaló, reinició ni cambió ningún teléfono en esta unidad.
El A13 conserva APK H04 con traza debug; A56 intacto. Servidor externo no consultado.
