# HIJO-C02 — puertas H05 y resumen público post-commit H08

9 de octubre de 2026. **Propuesta documental, no aprobación ni implementación.**
Base hijo: `c7fb842`. Complementa HIJO-C01/H05-L01 del relevo, sin duplicarlos.
Fuentes: PLAN_HIJO H05–H11, CP/1 §5/9/9.A, ModelosEstado.kt; lectura del diseño
PADRE-C01 `0e7b5db` y coordinación central. No cambia plan, contrato o permisos.

## 1. Matriz de puertas propuestas

Estas filas son criterios de revisión, NO etapas nuevas del protocolo ni etiquetas
de habilitación en código. H05 permanece EN_CURSO y la división no está aprobada.

| Puerta | Garantía y evidencia necesaria | Dependencias / situación | Qué permitiría y qué NO |
|---|---|---|---|
| A: garantías aisladas | JVM: firma/contexto, consumo antes de sesión, espera, cancelación, replay, cinco minutos y error seguro. Android: AtomicFile/Keystore efímeros, sin producción. Físico: emergencia básica sin quiosco, no recuperación operativa | C02/H04; H05-L01 reporta 17 tests nuevos/346 resultados JVM; 23 Android anteriores aislados; confirmaciones presenciales anteriores limitadas | Ya permite continuar pruebas aisladas H05 dentro del alcance asignado. NO identidad/oferta productiva, cierre H05, H06, H08/H10/H11 ni acreditar vínculo |
| B: puerta local previa a identidad/vinculación, propuesta | JVM: contrato de cola y lifecycle, capacidad por propósito, errores y consumo/espera. Android: ciclo de vida real, QR/cámara y archivos/identidad efímeros aislados; cierre de imágenes, proceso/boot y vigencia. Físico autorizado: recorrido de laboratorio, errores/permisos, cancelación y emergencia accesible incluso con espera; reinicio no devuelve código ficticio consumido | H05, C02/H04/H02; coordinador y puertos Android aún no integrados. No prueba identidad/vínculo reales o herramientas terminadas | Solo serviría como evidencia para solicitar revisión explícita de dependencias y asignación posterior de identidad/aceptación. NO habilita H08 con el plan actual; nunca H06 por esta fila ni por tests |
| C1: recuperación operativa real antes de endurecer, propuesta | JVM + Android: sesión autorizada abre únicamente función implementada; consumo/readback, procesos/reinicio, caducidad y salida. Físico autorizado: pareja/identidad reales de laboratorio, recuperación funcional y emergencia, sin accesos libres. Todos los propósitos y recuperación interrumpida conservan una salida segura | Necesita identidad/commit H08 y herramientas H11, todavía bloqueados por H05/H06. Es el ciclo funcional detectado; requiere decisión expresa sobre orden/subunidades | Solo tras revisión, implementación y evidencia real permitiría solicitar H06 gradual; no lo activa automáticamente. Sin salida real comprobada: NO endurecer, entregar al niño o retirar ADB |
| C2: acreditación integral de vínculo CP/1 §5 | STATE CHILD firmado y correlacionado, AUTH/PONG y ceremonia presencial con dos APK. Adulto observa commit H08/huellas/origen y recuperación MAINTENANCE real con rechazo de replay y emergencia. Validación completa por código impreso/boot se acredita aparte en hijo | H08/H10/H11; H10 exige H07/H08/H09/R03 y H07/H09 dependen H06. Diseño y persistencia de acreditación padre pendientes; TLS/red y permisos pendientes | Permitiría al padre, tras aprobación e implementación propias, acreditar vínculo; órdenes siempre con sus guardas posteriores. NO certifica políticas aplicadas, G0–G4, recuperación destructiva ni entrega final |

C1 NO se sustituye por C2 ni espera WSS para probar una función local de emergencia/
recuperación. C2 NO se usa para justificar H06 adelantada. Después de H06 gradual,
repetir emergencia y recuperación bajo restricciones reales: una prueba sin quiosco
no acredita su funcionamiento con quiosco. No reducir criterios físicos del plan.
RETIRE efectivo es destructivo y exige autorización/prueba específica aparte;
simular su sesión o revisar confirmaciones no demuestra retirada real. Si esa
salida es necesaria y no está comprobada, no se promete recuperación completa.

## 2. Mínimas unidades pendientes y decisión sobre el ciclo

Antes de solicitar una aprobación para revisar dependencias, delimitar/recoger:

1. **H05 local:** contrato probado del ejecutor serial y adaptador de lifecycle;
   actualización de proyección no es autoridad. Lista de capacidades por propósito:
   si no existe herramienta, no gastar código ni abrir sesión aparentemente útil.
   Plan de QR/cámara, errores y pausa sin dependencia de emergencia. H05-L01 ya
   aporta núcleo; no repetirlo como si fuera evidencia Android.
2. **H05 Android aislada:** asignación separada para integración y tests con archivos
   y claves efímeros, sin inyectar vínculo en producción. Proponer prueba física
   acotada de códigos ficticios/espera/reinicio/cancelación/emergencia y pedir permiso
   antes de instalar u operar. Se necesitan también condiciones de suspensión
   real: reloj inyectado no acredita cinco minutos de pantalla apagada.
3. **Contrato de capacidades y recuperación:** especificar las herramientas mínimas
   de MAINTENANCE, revinculación transaccional RECOVER_PARENT y RETIRE con doble
   confirmación; preservación de identidad/contador en actualización y salidas ante
   pérdida de proceso/padre. No abrir Ajustes ni llamar wipe para probar núcleos.
4. **Solicitud explícita de revisión:** presentar qué evidencia alcanza B y el
   cambio de orden necesario para llegar a C1: identidad/aceptación inicial y
   herramientas limitadas antes del endurecimiento, sin declarar tarea completa.
   El propietario/coordinador debe acordar subunidades y modificar el plan en
   una tarea autorizada. Este documento no modifica ninguna arista.

Después del acuerdo, aún harían falta H08 identidad/oferta y aceptación/commit,
integración H05 con identidad real y H11 herramientas/recuperación, C1 física,
H06 gradual con regresión física, luego H07/H09/H10 según dependencias y C2.
No empezar esas tareas por esta propuesta. Un origen HTTPS o relay local probado
no resuelve el ciclo. Si el acuerdo no se produce, detener la vía productiva.

## 3. Especificación del resumen público posterior a H08

**Proyección local de lectura**, no recibo remoto ni mensaje CP/1. Se deriva cada
vez de un único snapshot de estado persistido íntegro; no de formulario, QR en
pantalla, callback de firma o copia en RAM anterior al commit. No exige publicar
endpoint, QR nuevo, timestamp, hash de aceptación o firma de recibo.

| Dato visible | Fuente y comparación |
|---|---|
| pairId completo | VinculoPublico.pairId validado UUID v4; comparar exactamente con aceptación guardada por padre |
| Huella pública padre | SHA-256 de los bytes SPKI DER decodificados y validados P-256, no del texto base64 ni de la firma |
| Huella pública niño | Mismo cálculo; comparada además localmente con pública de identidad Keystore existente cuando pueda consultarse |
| Origen HTTPS exacto | VinculoPublico.relayOrigin validado por CP/1; mostrar cadena completa sin truncar, reescribir o sustituir por URL /ws |

Huellas completas: 64 dígitos hexadecimales mayúsculos, 16 grupos de cuatro separados
por espacios, igual que PreparacionVinculo.huella del padre. Comparación corresponde
a los mismos bytes; espacios son presentación, no modificación de la clave.
No mostrar públicas completas por defecto, hashes/códigos de recuperación, privada,
alias internos, nonce/desafío reutilizable, historia de órdenes o datos del teléfono.
No guardar una copia paralela del resumen ni exportarlo automáticamente a logs/
intents/relay. No incluir códigos en errores, accesibilidad o estado guardado UI.

### Estados de presentación conceptuales (no enums nuevos CP/1)

- **Oferta visible:** pública/huella del niño y desafío RAM pendientes; no pairId
  aceptado ni resumen de vínculo confirmado. Cancelación/boot/proceso retiran oferta.
- **Aceptación capturada/en revisión:** comparación de huellas/datos del QR recibido
  claramente provisional; ni lectura óptica ni firma válida confirman escritura.
- **Commit local confirmado:** solo tras validación PAIR/oferta vigente/confirmación
  física y transacción H08 + readback. Texto propuesto: «Datos del vínculo guardados
  en este dispositivo; acreditación operativa pendiente». No «control activo».
- **Reapertura íntegra:** reconstruir mismos datos desde el registro, sin nuevo
  CHILD_OFFER/confirmación/alias/códigos. Reapertura no restaura oferta, sesión de
  recuperación o autoridad del adulto ni acredita canal/frescura actual.
- **Acreditación operativa:** evidencia separada posterior de C2, no inferida de
  la etiqueta provisioningStage=VINCULADO, resumen visible, AUTH_OK o STATE.

El EstadoControl actual no conserva un recibo de PAIR_ACCEPT ni acredita un commit
H08: H08 no existe. No migrar registros/fixtures inyectados a «confirmado» por
contener VinculoPublico o una etiqueta. H08 deberá definir origen/validación de
su registro y migración segura en su tarea; no añadir hoy formato privado ni
deducir firma de aceptación del checksum. Checksum detecta corrupción accidental,
no defiende frente a root ni certifica autoría parental.

### Fallos y crash

Antes del commit, cancelación/expiración/cambio de oferta/identidad impiden publicar
resumen confirmado. Ante escritura/readback fallido: error seguro; nunca copiar
formulario como éxito. Tras crash, releer: registro anterior o nuevo completo,
sin mezcla de claves/origen. Si hay duda de procedencia/integridad, no confirmar
ni reabrir enrolamiento libre. Un nuevo completo ya comprometido no se deshace
por cancelación UI; no se vuelve a gastar autorización/revincular al reabrir.

Corrupción no se borra ni se convierte en ausencia. Identidad perdida/pública
distinta: mostrar error e impedir afirmar vínculo utilizable, generar reemplazo
o firmar/conectar; conservar datos para recuperación autorizada. Antes del primer
desbloqueo, informar que identidad no puede comprobarse, no inventar coincidencia.
Un resumen histórico de públicas no equivale a identidad disponible. En revinculación
RECOVER_PARENT, anterior vínculo continúa hasta commit autorizado; error/salida
no revoca anticipadamente la clave vieja ni devuelve código consumido (CP/1 §9.A).

## 4. Correspondencia y acuerdo pendiente con el padre

Compatible conceptualmente con PADRE-C01 §§2/3: adulto compara **pairId, huellas
completas de ambas públicas y origen HTTPS exacto** en hijo post-commit contra
aceptación propia persistida del padre. Después observa herramientas MAINTENANCE
limitadas, repetición sin renovar permiso y emergencia sin llamada real; son
declaraciones presenciales específicas, no firmas emitidas por el niño.

Software del hijo comprueba transacción/integridad/contexto según su implementación;
padre verifica firma del hijo, pairId y STATE correlacionado. Ni resumen ni STATE
certifican el PAIR_ACCEPT exacto guardado, todos sus hashes, consumo del nonce
OFFLINE o aplicación de herramienta. SHA-256 del payload PAIR usado por la revisión
privada del padre no es una comparación nueva exigida al niño. MATCH de los cuatro
datos públicos no sustituye pruebas internas de hashes/consumo/boot en hijo.

Acuerdo a revisar antes de PADRE-C02: formato de huellas y comparación sin truncar,
origen exacto, texto/estatus de commit pendiente de acreditación, tratamiento de
errores, y declaraciones presenciales separadas de evidencia criptográfica.
PADRE-C02 sería validador puro aislado, sin promoción/escritura VINCULADO o órdenes;
el coordinador debe asignarlo por separado tras revisión de este acuerdo.
Ceremonia/persistencia/migración del padre tampoco se habilitan aquí.

Si se exige recibo remoto verificable del PAIR_ACCEPT exacto o del consumo OFFLINE,
CP/1 no lo proporciona: **detener esa vía**, proponer CP/2 y análisis de replay/
persistencia, acuerdo de ambos roles y schema/fixtures nuevos. No reutilizar ACK
de política ni añadir campos a STATE. La comparación presencial propuesta no exige CP/2.

## 5. Aceptación y pruebas futuras (NO ejecutadas)

- Puertas: evidencia aislada/label/test/booleano simulado jamás habilitan H06;
  herramienta ausente impide gasto/permiso; consumo previo/readback, cancelación,
  espera independiente y emergencia conservados. Android/proceso/boot/suspensión
  y pruebas físicas autorizadas deben acreditar lo que JVM no demuestra.
- Resumen puro: mismo snapshot produce mismos cuatro datos; decode SPKI antes de
  hash, formato igual al padre; clave/pairId/origen distintos detectados. Sin hashes
  de recuperación, secretos o recibos inventados; origen no normalizado/truncado.
- H08 futura: oferta/captura/firma/readback fallido no publican éxito; doble commit,
  entrada cambiada, crash antes/durante/después y reapertura no revinculan. Archivo
  corrupto/legado sin procedencia/identidad perdida no generan reemplazo o promoción.
- Ceremonia futura: dos APK reales, comparar snapshot persistido tras reabrir,
  OFFLINE MAINTENANCE y replay/caducidad/emergencia; otro boot exige revisión nueva.
  STATE impostor/ajeno/viejo y simulador no producen declaración presencial.
- Propósitos/códigos y retirada destructiva requieren sus pruebas independientes;
  una ceremonia QR de mantenimiento no acredita recuperación impresa ni RETIRE.

Aceptación HIJO-C02: propuestas separadas, evidencias explícitas y ningún camino
desde pruebas aisladas a endurecimiento/acreditación. Sin código/tests/builds o
operaciones físicas en esta unidad. Decisión de puertas y acuerdo padre pendientes.
