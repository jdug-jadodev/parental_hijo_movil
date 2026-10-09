# Evidencia H03 — motor puro de políticas

Fecha: 8 de octubre de 2026. Windows 11, JDK 21.0.10, Gradle 8.13,
Kotlin 2.2.21 y AGP 8.11.1. No se conectó ningún teléfono.

## Pruebas ejecutadas

Desde `android/`, con el JDK de Android Studio:

```powershell
.\gradlew.bat :core-protocol:test :app-child:testDebugUnitTest :app-child:assembleDebug :app-child:assembleDebugAndroidTest :app-child:lintDebug --console=plain
```

Resultado: BUILD SUCCESSFUL. Informes XML: **227 tests, 0 fallos, 0 errores,
0 omitidos**; 57 nuevos tests H03 y los 170 anteriores. Los tests del núcleo
ya estaban UP-TO-DATE; los tests nuevos de cierre del motor se ejecutaron.
Ambos APK compilados; lint: 0 errores y 10 avisos de versiones disponibles.

Desde la raíz:

```powershell
python -B pruebas/verificar_apk_base.py
```

Resultado: OK, APK sin ejemplos congelados ni clases declaradas del simulador
o pruebas. Esta inspección no certifica Android, emergencias ni control del tiempo.

## Cobertura de la lógica pura

- H-TIME-01: cuatro acciones y bordes de vencimiento en tabla; nueva política sin acumular tiempos.
- Parte pura H-TIME-02: cambios de hora civil simulada no afectan la cuenta regresiva.
- Parte pura H-TIME-03: LOCK_FOR vencido no permite sin red/canal; reconexión sin reiniciar duración.
- Arranque distinto no continúa permisos ni reinterpreta timestamps del arranque anterior.
- Red: margen de 3 s; canal: 45 s desde PONG. Datos ausentes no inventan margen ni latido.
- Mantenimiento: solo MAINTENANCE, 5 min, ligado al boot; no pausa la política ni autoriza el arranque.
- 1024 combinaciones de condiciones obligatorias no permiten acceso con estados incompletos.
- Reloj fallido/negativo, timestamps futuros, políticas incoherentes y aritmética cerca de Long.MAX_VALUE.
- Próxima transición positiva, sin plazos cero que provoquen bucles ocupados.

Los campos de Owner, vínculo, restricciones, red y canal son snapshots simulados,
no privilegios o conexiones demostrados en Android. El motor solo calcula intención.
La aceptación duradera y deduplicación de comandos reales corresponden a H04/H09.
Recrear el motor prueba su lógica, no la supervivencia del servicio Android.

## NO EJECUTADO

- Cambio de hora/zona en teléfono; suspensión, Doze, reinicio y muerte de proceso reales.
- Persistencia, scheduler Android, Device Owner, Lock Task, emergencia y recuperación.
- Tres tests instrumentados C02: compilados, pendientes de ejecución coordinada.

La interfaz sigue «No configurado». H03 no demuestra bloqueo aplicado ni APPLIED.
Los casos de aceptación que requieren A13 siguen pendientes de evidencia física.
