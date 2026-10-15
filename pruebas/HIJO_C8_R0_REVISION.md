# HIJO-C8-R0 — revisión corta, sin implementación

## Conclusiones (15 líneas)

1. **APTO como alternativa de transporte sin cámara; OBJECIÓN BLOQUEANTE para implementar/habilitar todavía**: faltan contrato C8/1 exacto y ancla humana independiente acordada.
2. Fuente revisada: C8-R0 íntegra suministrada por propietario; propuesta, no schema congelado. Pausa QR para este ensayo; conservar datos/identidades/reservas/pendientes.
3. MAIN: `ui/AccesoAdultoActivity.kt:331–388` debe separar `OfertasRam.generar()` de render QR: hoy `ref == null || matriz == null` impide continuar; C8 no llamará `QrOfertaCp2.crear`.
4. Mantener GENERAR_OFERTA fresco, `IdentidadOfertaAndroid.leer()`/ausencia CP2 coherente, referencia/generación RAM y `CodecOfertaCp2.escribir(ref.oferta)` original; nunca inicializar/reemplazar clave.
5. Recepción MAIN futura: alternativa explícita a `recibirPair():434–461`, sin CAMERA; entregar copia exacta PAIR a `framePair`→`presentarCandidataPair():463` sólo con referencia/canal vigentes y VALIDAR_PAIR fresco, sin promover confianza por entrega.
6. Reutilizar confirmación humana→`ValidacionPairRam.confirmar/validar`→`guardarPair():513`→`PairAndroid.guardar`, con GUARDAR_PAIR fresco, escritor adulto único, readback y reservas intactos.
7. Ancla obligatoria: propietario coteja ambas huellas desde las identidades locales de cada teléfono conocido y origen independiente; no usar la propia PAIR/C8 como huella esperada ni código8 como identidad/familia/SAS.
8. Bloqueo concreto de confianza: confirmar que el padre muestra su propia huella local ANTES/independientemente de la candidata; sin ese canal humano verificable, no validar/guardar ni autollenar expectativa desde PAIR.
9. TTL Android: fijar al publicar un límite local inmutable `min(marco.limite, ref.limite, ahoraAndroid+120000)`; presupuesto positivo restante, cierre al límite y revalidación antes/después IO/cripto/commit, sin renovar por tickets/pings/reintentos.
10. `OfertasRam.restante` sólo refleja su límite5min, NO el marco120s; calcular presupuesto con `MarcoSesionAdulto.limite`/`marcoVigente`, no pasar elapsed Android a Node ni comparar relojes entre procesos.
11. Bloqueo TTL wire: una duración enviada inicia más tarde en Node por tránsito; acordar inicio/expiración/descuento y cancelación del servidor, sin prometer que ese número solo garantiza vencimiento servidor antes del marco Android.
12. WSS C8/1 separado/perfil explícito desactivado; reto10s unuso/socket y PoP P256 de clave existente con dominio separado, sólo encuentro; `AuthAndroid` exige pareja y NO sirve de bootstrap; jamás quitar AUTH familiar ni abrir cp.v2 anónimo.
13. UI futura foreground: código8 servidor en campo grande legible, sólo RAM/sin guardado-autofill-portapapeles-logs; mantener FLAG_SECURE, cerrar socket/capacidades y retirar campo al fondo/cancelar/caducar/contexto cambiado; cero capturas de app, sin prometer impedir cámara externa.
14. Contrato pendiente debe fijar ruta/subprotocolo/bytes firmados/retos, código32símbolos×8/normalización, correlación/consumo atómico, límites/errores genéricos/cancelación; entrega no es commit y consumo nunca se revierte por fallo/callback viejo.
15. Vínculo/recuperación inciertos bloquean sin atajos: persistencia padre sin entrega conserva pendiente; hijo guardado queda PENDIENTE_CANAL_Y_RECUPERACION/noUSO. APKnuevos no contienen C8; sin código/build/test/ADB/QR/ceremonia/servicio/push/deploy; cerrar revisión y detener.

## Evidencia local de revisión y procedencia

Lectura estática, no ejecución de UI/transporte. Base previa `aed7503`.
La fuente C8-R0 procede exclusivamente del mensaje íntegro del propietario; no
se consultaron otros repositorios/roles ni se fijó wireformat/API/codec nuevo.
No cambia CP1, CP2PAIR, firmas, contratos congelados, enforcement o recuperación.

- `android/app-child/src/main/java/dev/controlparental/child/ui/AccesoAdultoActivity.kt`:
  líneas88–111 (`FLAG_SECURE`, guardado deshabilitado, onPause/onDestroy,
  limpiar/cerrarAutoridad invalidan frame/caso/ref/oferta/consulta/sesión);
  349–365 (marco, guardas, generación real dependiente del QR);
  414–461 (tickets separados/cámara); 469–505 (integridad candidata NO identidad,
  huella/origen introducidos y casilla explícita, validación existente);
  513–537 (escritor adulto, guardado/readback, mensajes pendiente/incierto).
- `android/app-child/src/main/java/dev/controlparental/child/pairing/OfertaCp2.kt`:
  69–105; OfertasRam no depende de matriz/cámara, vigencia por contexto/generación
  y vidaSesion, pero restante devuelve límite propio5min, no presupuesto adulto.
- `android/app-child/src/main/java/dev/controlparental/child/local/SesionAdulto.kt`:
  MarcoSesionAdulto.limite, finesPair, solicitar44/autorizar75/marcoVigente85
  conservan marco original y revisión/boot; un ticket nuevo no lo extiende.
- `android/app-child/src/main/java/dev/controlparental/child/pairing/ValidacionPairRam.kt`:
  confirmar45/validar61/resultadoActual86 verifican consentimiento independiente,
  oferta original, contexto, nonces, tiempo, consumo y callbacks. No cambiar esas
  guardas por haber recibido PAIR en C8.
- `android/app-child/src/main/java/dev/controlparental/child/pairing/IdentidadOfertaAndroid.kt`:
  leer49 comprueba compatibilidad/registro existente; alias privateSigning99.
  `security/FirmanteAndroidKeystore` es la primitiva existente de firma, no crear
  clave para PoP. Futuro adaptador C8 acotado a propósito/contexto tras contrato;
  `auth/AuthAndroid.kt:11–27` requiere PairAndroid.Guardado, por eso no reutilizar
  su contexto familiar para enrolar ni fabricar snapshot de pareja.
- `android/app-child/src/main/java/dev/controlparental/child/pairing/PairAndroid.kt`:
  guardar28/archivos con reserva63–73 permanecen autoridad durable real, sin
  reparación/limpieza de restos ni otra vía de alta.

**Histórico físico en esta revisión:** actualización de ambos APK y confirmación
UIadulta/emergencia sin errores REPORTADAS por propietario/coordinación; no se
reejecutaron ni aportan nueva evidencia física de C8/QR/Keystore/recuperación.
La evidencia previa propia de actualización del hijo permanece en
`pruebas/HIJO_F03_PREPARACION.md`; no reinterpretarla como permiso nuevo.
La pantalla manchada motiva pausar QR, no omitir consentimiento/huellas.

Sólo documentación: este informe, ESTADO y relevo. Revisión `git diff --check`
antes del commit; sin builds/tests repetidos, ADB, teléfonos, ofertas/aceptaciones,
códigos8 reales, enlaces de encuentro, claves, servicios remotos o despliegue.
No pedir códigos adultos/privadas/capturas. El siguiente paso pertenece al acuerdo
del contrato exacto compartido y del ancla humana; no iniciar implementación
automática, inventar mensajes o habilitar un perfil por urgencia.
