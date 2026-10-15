# CP/2 parcial — fuente local suministrada

HIJO-PAIR01, 2026-10-14. Copia del texto íntegro de
`contrato-cp2/vinculacion.schema.json`, suministrado por el propietario como
FUENTE CP2-V01 Render `4ac4962`; README de esa fuente también suministrado íntegro
en la entrada siguiente. No se leyó el repositorio externo ni se verificó su commit.
117 pruebas/13 nuevas Render son resultado reportado, no ejecutado aquí.
La copia conserva el texto del schema con LF y salto final; su hash local no
certifica el hash del archivo remoto ni sus terminadores de línea.

Base normativa: draft PADRE-C04-R1 `75eb56e` §§2–3/10.1, gate local C08
`3cc206e` APTO y autorización explícita HIJO-PAIR01. No congelación CP/2 completa.
CP/1 y fixtures en compartido/ siguen intactos. Se reutilizan únicamente primitivas
técnicas JSON/base64/SPKI ya existentes sin cambiar sus dominios ni codecs CP/1.
El borde CP/2 impone profundidad6 ANTES del parser y canonicalidad exacta.

Implementación Kotlin limitada a CHILD_OFFER no firmado, seis campos cerrados,
v2/protocol cp.v2. No PAIR_ACCEPT, verificación PAIR, commitpair, códigos/hashes de
recuperación, WSS, órdenes/STATE, migración, perfil público ni ceremonia operativa.
Oferta no demuestra identidad humana, frescura al receptor, bloqueo ni permiso.
El QR es texto original CHILD_OFFER, no aprovisionamiento Android Device Owner.

Oferta RAM300000ms ligada a generación/boot/identidad/autoridad adulta fresca.
La sesión adulta actual vence antes (120000ms desde solicitar código fresco):
no se alarga para mostrar un QR cinco minutos. Fondo/salir/cancelar/caducar sesión,
boot/reloj discontinuo/identidad o contexto no disponible retiran oferta y QR.
No autooferta, persistencia de nonce, Bundle, log, clipboard, compartir ni red.
FLAG_SECURE bloquea capturas del dispositivo, no cámara externa presencial futura.
Recepción de aceptación explícitamente deshabilitada.
