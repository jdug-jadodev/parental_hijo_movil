# Código de la aplicación del hijo

## Estado inicial

C00 crea un APK mínimo con una pantalla que informa «No configurado».
Todavía no controla el tiempo, no es Device Owner, no se conecta a un relay y no aplica restricciones.

## Carpetas

| Ruta | Responsabilidad |
|---|---|
| `app-child/src/main` | Código y recursos Android que forman parte del APK. |
| `app-child/src/test` | Pruebas JVM de la integración con el núcleo. |
| `core-protocol/src/main` | Código Kotlin puro compartido por los componentes del hijo. |
| `core-protocol/src/test` | Pruebas locales del núcleo. |
| `core-protocol/src/testFixtures` | Simulador de mensajes disponible únicamente para pruebas. |
| `gradle/libs.versions.toml` | Versiones fijadas de plugins y dependencias. |

Consultar `../BUILD_ENV.md` para construir y `../ESTADO.md` para continuar el desarrollo.
Los módulos del padre y el servidor real no se desarrollan aquí.

## Reglas de funcionamiento previstas

- La autorización es una cuenta regresiva global, no un contador por aplicación.
- Las aplicaciones se aprueban presencialmente y se mantienen dentro del quiosco.
- El control de tiempo no recopila actividad ni contenido.
- Emergencia y recuperación se prueban antes de endurecer las restricciones.
- Los dobles de prueba no pueden simular un bloqueo aplicado dentro de la versión final.
