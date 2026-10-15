# HIJO-C8-F04 — preparación de actualización conservadora

## Resultado local y puerta de autorización

Preparación terminada; **esperando permiso DIRECTO del propietario en esta
sesión antes incluso de inventario ADB**. El mensaje del coordinador y permisos
F03/F02/LOCAL08 no lo sustituyen. No instalación ni ceremonia por esta preparación.
Base `7fd1ffa`. Sólo documentación/inspección del APK local; sin rebuild ni tests
repetidos porque no cambia código/artefacto productivo.

### Comprobaciones realmente ejecutadas

- `Get-FileHash android/app-child/build/outputs/apk/debug/app-child-debug.apk
  -Algorithm SHA256`: **`6221f3f52a7bdfbe4a4da19627c00821598fb2b2f9a59989a82e846e28250751`**.
- `apksigner verify --print-certs` de build-tools36.0.0: firma verificada,
  certificado público SHA256
  **`39f25d28e10a93f18fbdabac0223e917c9668d58a6d7ca28af40227946a9f439`**,
  igual al histórico F03. Certificado APK no es identidad familiar Keystore.
- `aapt dump badging` local: paquete `dev.controlparental.child`, versionCode1,
  versionName0.1.0, minSdk31/targetSdk36, debug/testOnly. Preparar `-r -t`, no `-d`.
- Hash local R1: **`089e0b6299ec6db3d312596e5929452321cbb60e3fb4646cceb4669b9492adde`**.
- Último instalado ACREDITADO F03: APK E2E01
  `fc7a5bd82e6bfdcb0ae0434ce2d88be03eac2cc1390d73a339c9eb52c83108ed`.
  No instalarlo otra vez. No se consultó qué está instalado actualmente;
  comparación definitiva de firma/versión/legado y preservación sigue pendiente
  del permiso ADB y de las comprobaciones posteriores, no presumirla por historia.

## Contexto reportado, sin comprobación remota propia

Render preparado `9c14372`/217tests; R1 idéntico. Cruces independientes locales
JVM↔NodeTLS real, cada uno con contraparte Node sintética: padre hashPAIR
`rJ7mi56D-qJF-iHGKrX2pk9IAz0UB0qIQLMqvTQAYZw` (reportado), hijo
`X28PLtkh_Zv4nf-pqACF5Xe8bx21xZ0MTnNmdYsGOww` (evidencia propia histórica X01,
no repetida ahora). No son dos APK juntas ni Android real.

C8 público todavía NO desplegado según coordinación. Origen futuro real
`https://parental-0j3o.onrender.com`; no contactar ahora. MCP AutoDeploy commit
ACTIVO/healthcheck keepalive/1instancia fueron comprobados por Render, **no aquí**.
Propietario debe desactivar AutoDeploy antes del push para preservar despliegue
manual; esta sesión de app no hace push/deploy ni cambios remotos, ni comprueba
su desactivación. Servidor/perfil público siguen responsabilidad externa.

## Checklist futuro — sólo después de permiso directo

1. Pedir permiso limitado para ADB del único A13SM-A135M, verificar compatibilidad
   y UNA actualización incremental si compatible. Esperar respuesta aquí antes
   de ADB, incluso inventario. A56 absolutamente excluido; no consultas dirigidas.
2. **Antes de instalar**, pedir confirmación NUEVA de pantalla operable, acceso
   adulto y salida local/emergencia disponibles. Botón físico averiado: no reboot,
   cortes de red/energía ni cambios Owner/PIN/protecciones. No llamada de prueba,
   capturas ni códigos por chat. Si no puede confirmarlo, no instalar.
3. Tras permiso, seleccionar único A13 por modelo anunciado y confirmarlo;
   cada operación dirigida explícitamente al mismo destino con `adb -s`.
   Ante varios candidatos/ambigüedad, detener; serial sólo temporal/RAM, no repo.
4. Leer lo mínimo para comparar paquete/versión/certificado instalado y baseline
   Device Owner/protecciones. No exportar datos/identidad/claves/credencial/CP1/
   vínculos/pending. APK instalado, si necesario para cotejar firma, sólo temporal
   local. Comprobar legado/ausencia de incertidumbre sin reparar o migrar.
5. Firma/versión/legado incompatibles o estado incierto: **DETENER**, sin uninstall,
   clear/reset/downgrade/reinicialización/rotación/migración ni segunda alta.
6. Reconfirmar modelo y hash del candidato C8 **6221f3…250751**. Sólo entonces,
   UNA operación prevista y NO ejecutada aún:
   `adb -s <destino_A13> install -r -t <APK_C8_verificado>`.
   Conservar datos/DeviceOwner/protecciones, sin reinstalar el APK antiguo.
   Si falla o es ambiguo, detener sin reintento automático/desinstalación.
7. Verificar APK/hash/certificado/versión, DeviceOwner y baseline consultado tras
   actualización. No afirmar preservación semántica sólo por installSuccess.
   Propietario abre UI manualmente, confirma acceso adulto con código habitual
   sólo en teléfono, salida/emergencia y ausencia de errores. Si identidad/legado
   incierto o UI propone inicializar/reparar, detener sin ejecutar esas acciones.
8. Detener antes de generar C8/ofertas/QR/aceptaciones/cámara/WSS/AUTH/órdenes.
   Actualizar NO autoriza ceremonia, aun si servidor se despliega mientras tanto.
   Pedir permiso específico posterior; nunca inferirlo de «hagamosle» coordinado.

## Cierre de preparación

Rutas: este checklist/evidencia, `ESTADO.md`, `COORDINACION_PADRE_HIJO_RENDER.md`.
Revisión `git diff --check` y commit documental; no cambios de código/APK.
Sin ADB/dispositivos/install/códigosadultos/privadas/QR-C8real/serviciosremotos/
push/deploy/otrosrepos/subagentes/sesiones/prompts. Próximo paso único: solicitar
permiso DIRECTO breve y esperar; no ejecutar instalación ni ceremonia antes.
