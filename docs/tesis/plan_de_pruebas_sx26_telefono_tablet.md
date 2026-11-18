# Plan de Pruebas en Dispositivo: Micrófono SX26 Wireless + Teléfono / Tablet Android

**Proyecto:** Prototipo de subtitulado con lógica difusa on-device (UNSA) · **Paquete:** `com.unsa.dhh.captions`
**Complementa:** `plan_de_pruebas_calidad_software.md` (casos TC-*), que sigue vigente. Este documento agrega la verificación **en hardware real**, que las pruebas automáticas no pueden cubrir.

---

## 0. Estado de la verificación automática (ya ejecutada, sin hardware)

| Verificación | Comando | Resultado (2026-11-01) |
|---|---|---|
| Pruebas unitarias (Mamdani, paridad Kotlin↔Python, memoria de clase, Levenshtein, WER, decisión, clusters, fuente de audio, confianza ASR, diccionarios reales) | `./gradlew testDebugUnitTest` | 68 de 68 OK |
| Compilación del APK | `./gradlew assembleDebug` | OK |
| Benchmark WER/KER (8 frases de Biología, corpus simulado) | `python3 evaluation/benchmark_runner.py` | WER 18.45 % → 1.94 %; KER 61.29 % → 6.45 % (corpus sintético, dentro de la muestra) |
| SQA automatizado (TC-DOM-01, ACC-01, PERF-01, AUDIO-02 lógica) | `python3 evaluation/ejecutor_pruebas_calidad.py` | 4 de 4 OK |
| El anexo `fuzzy_rules.fcl` equivale al motor que ejecuta la app | `python3 evaluation/verificar_fcl.py` | OK (diferencia máx. 3e-16 en 4913 puntos) |
| Ablación de S / rerank, umbral y comparación con regla booleana | `benchmark_runner.py --ablacion`, `--umbral`, `--booleana` | ver `factor_semantico_topic_clusters.md` |

**Lo que estas pruebas NO demuestran:** que el SX26 enrute audio a la app, que el reconocedor de voz funcione sin red en tu equipo, ni que la confianza del ASR sea útil. Eso lo cubre este plan.

## 1. Datos que debes confirmar sobre el SX26 antes de empezar

No asumas las especificaciones; léelas del equipo real. Hay variantes del SX26 (receptor USB-C, Lightning o 3.5 mm). Esta app solo se ha diseñado para el receptor **USB-C**.

1. Conecta el receptor al teléfono con USB depuración activa y ejecuta:
   ```bash
   adb shell dumpsys audio | grep -iE "usb|input" | head -30
   adb shell dumpsys usb | grep -iE "product|class|audio"
   ```
2. Anota: nombre del producto, frecuencia de muestreo, si aparece como `TYPE_USB_DEVICE` o `TYPE_USB_HEADSET`, y si expone también salida (altavoz). El código acepta ambos tipos; si aparece otro tipo (p. ej. `TYPE_USB_ACCESSORY`), **la app mostrará "Micrófono del teléfono" aunque el SX26 funcione** y habrá que ampliar `ExternalAudioDeviceManager.classifyInputs`.

## 2. Matriz de dispositivos

| ID | Equipo | Android | USB-C con audio USB | Servicios de voz de Google | Paquete de voz español offline |
|---|---|---|---|---|---|
| T1 | Teléfono de gama media (el que usarás en aula) | anotar | sí/no | sí/no | sí/no |
| T2 | Tablet principal | anotar | sí/no | sí/no | sí/no |
| T3 | Teléfono con Android 8 a 9 (mínimo soportado, `minSdk 26`) | 8.x | opcional | opcional | opcional |

Antes de cada ronda: instalar APK (`adb install -r app/build/outputs/apk/debug/app-debug.apk`), conceder micrófono y notificaciones, batería ≥ 80 %.

> **Riesgo específico de tablets:** muchas tablets económicas no traen Google Speech Services. Si `SpeechRecognizer.isRecognitionAvailable()` es falso, la app mostrará "Reconocimiento de voz no disponible" y no transcribirá. Esta es la causa más probable de fallo en tablet; ejecutar TAB-01 primero.

## 3. Casos de prueba en hardware

Registrar logs con `adb logcat -s ExternalAudioDevMgr AndroidSpeechEngine CaptionsViewModel AudioForegroundService`.

### 3.1 Conectividad del SX26

| ID | Procedimiento | Criterio de aceptación |
|---|---|---|
| HW-01 | Abrir la app sin el receptor. Conectar el receptor USB-C con el transmisor encendido. | Badge cambia a micrófono inalámbrico en ≤ 500 ms (cronometrar con video o marcas de logcat). La notificación persistente muestra la fuente. |
| HW-02 | **Prueba de enrutamiento real** (la más importante). Con el SX26 conectado, dejar el teléfono a 5 m del hablante y hablar solo al transmisor de solapa en una sala silenciosa. | Transcripción correcta. Si el texto sale vacío o solo capta cuando hablas cerca del teléfono, **el audio no se está enrutando al USB** (ver sección 5, plan B). |
| HW-03 | Con la transcripción activa, desconectar el receptor. | Fallback a micrófono interno en ≤ 300 ms, sin cierre de la app, y la transcripción continúa. Aviso visible de cambio de fuente. |
| HW-04 | Reconectar el receptor durante la sesión. | Vuelve a la fuente inalámbrica; la sesión no se reinicia ni duplica texto. |
| HW-05 | Apagar el transmisor de solapa con el receptor conectado. | La app no se cuelga; documentar si sigue mostrando "inalámbrico" (el receptor sigue presente aunque no haya voz) y qué pasa con la transcripción. |
| HW-06 | Receptor con carga pasante: conectar cargador en el puerto lateral del SX26. | La audio USB sigue funcionando mientras carga (si no, anotarlo como limitación). |
| HW-07 | Alcance: hablar a 1, 5, 10 y 15 m del receptor, con y sin paredes. | Anotar la distancia a la que el WER sube más de 10 puntos o se corta el audio. |

### 3.2 Reconocimiento de voz (ASR) y operación offline

| ID | Procedimiento | Criterio |
|---|---|---|
| TAB-01 | En cada equipo, abrir la app y leer el mensaje de estado. | No aparece "Reconocimiento de voz no disponible". |
| ASR-01 | Activar Modo Avión (Wi-Fi y datos apagados). Dictar 5 frases de `evaluation/guion_docente_biologia.md`. | Transcribe igual que con red. Si aparece "Falta el paquete de voz en español sin conexión", instalar el paquete (Ajustes > Idioma > Voz > Reconocimiento sin conexión) y repetir. |
| ASR-02 | Con logcat, registrar el valor crudo de `CONFIDENCE_SCORES` de 20 frases. | Anotar si el equipo lo devuelve (valores 0 a 1) o no (-1/0/ausente). Si no lo devuelve, el factor C vale 0.65 constante y el aporte de la lógica difusa depende solo de D y S: **documentarlo en la tesis**. |
| ASR-03 | Hablar sin parar 3 minutos. Contar palabras perdidas en cada reinicio del reconocedor (`onResults` reinicia con 150 ms de retraso). | Pérdida < 2 % de palabras. Anotar si suena un pitido en cada reinicio (común en algunos equipos). |
| ASR-04 | Silencio de 60 s y reanudación. | Reanuda en < 500 ms sin intervención. |

### 3.3 Calidad de la corrección difusa con voz real

Usar el guion de Biología (`evaluation/guion_docente_biologia.md`) leído por el docente real con el SX26. Para cada frase guardar: texto crudo del ASR, texto corregido, y si fue correcto.

| ID | Procedimiento | Criterio |
|---|---|---|
| FZ-01 | 30 términos del diccionario dictados en frases de clase. | KER (palabras clave) con capa difusa < KER crudo. Registrar ambos. |
| FZ-02 | **Falsos positivos:** 20 frases cotidianas sin vocabulario técnico (p. ej. "ayer fuimos al parque, realiza la tarea dentro del aula") **y** 10 frases cotidianas intercaladas dentro de una clase de mitosis ("la meiosis produce gametos y se duplica el material"). | 0 sustituciones. Cada sustitución en este caso es un defecto grave (cambia lo que el estudiante lee sin avisar). |
| FZ-03 | Términos con homófonos cercanos (sedula/médula/célula, mitosis/meiosis). | Anotar los casos ambiguos; no se exige acierto, pero sí que no sustituya con confianza alta cuando el contexto no lo respalda. |
| FZ-05 | **Memoria de la clase.** Dictar 3 oraciones sobre división celular y luego, solas, "mitocis" y "sedula". Repetir tras 5 min de silencio. | En el historial, "mitocis" muestra S > 0.60 y tema "División Celular"; "sedula" no se sustituye por "médula". Tras el silencio, S vuelve a 0.50. Medir latencia con `latencyMs` (≤ 100 ms). |
| FZ-06 | **Cambio de tema.** Dictar 5 oraciones de división celular y luego, sin pausa, una frase corta de genética con un término mal reconocido ("transcrisión"). Repetir con 1 minuto de pausa. | Anotar en cuánto tiempo/oraciones el sistema empieza a corregir el tema nuevo (retraso de adaptación). Insumo para calibrar la vida media. |
| FZ-04 | Historial de correcciones en la app. | Cada sustitución aparece con palabra original, final, y* y cluster. |

### 3.4 Robustez y rendimiento (45 a 90 min)

| ID | Procedimiento | Criterio |
|---|---|---|
| ROB-01 | Sesión continua de 90 min con pantalla bloqueada a ratos, SX26 conectado, cargador conectado. | Servicio en primer plano vivo; sin cierre por el sistema; transcripción continúa. Probar con ahorro de batería activado y desactivarlo si falla (los fabricantes matan procesos agresivamente; ver dontkillmyapp.com). |
| ROB-02 | Llamada entrante o notificación con sonido durante la sesión. | La app se recupera al terminar. |
| ROB-03 | Rotar pantalla y volver a primer plano varias veces. | Sin pérdida del texto acumulado ni reinicios del servicio. |
| PERF-01 | `adb shell dumpsys cpuinfo \| grep captions` y `dumpsys meminfo com.unsa.dhh.captions` cada 10 min. | CPU promedio ≤ 15 %, RAM sin crecimiento sostenido (fugas). |
| PERF-02 | Latencia de la capa difusa en el equipo (agregar log de `latencyMs`, ya calculado en `processIncomingRawText`). | ≤ 100 ms por oración. |

### 3.5 Específicas de tablet

| ID | Procedimiento | Criterio |
|---|---|---|
| TAB-02 | Conectar el SX26 por USB-C directo y por hub/adaptador OTG. | Documentar cuál funciona; algunos adaptadores no pasan audio USB. |
| TAB-03 | Orientación horizontal y vertical, pantalla 8 a 11". | Teleprompter de 2 a 3 líneas legible a 3 m, sin texto cortado. |
| TAB-04 | Tablet sin SIM ni Google Play. | Si no hay servicios de voz, registrar como limitación y recomendar teléfono. |

### 3.6 Accesibilidad

| ID | Procedimiento | Criterio |
|---|---|---|
| ACC-01 | Medir contraste amarillo/negro. | 19.56:1 (AAA exige ≥ 7:1). No documentar 21:1: ese valor solo lo da blanco sobre negro. |
| ACC-02 | Estudiante sordo/hipoacúsico (con autorización) lee 5 min de teleprompter; aplicar SUS. | Puntuación SUS ≥ 68. |

## 4. Orden recomendado de ejecución

1. Sección 1 (identificar el SX26) → HW-01 → **HW-02** (si falla, parar y aplicar sección 5).
2. TAB-01 y ASR-01 en cada equipo (decide si ese equipo sirve).
3. HW-03 a HW-07, ASR-02 a ASR-04.
4. FZ-01 a FZ-04 con voz real.
5. ROB-01 / PERF-01 (la más larga, al final).

## 5. Plan B si el audio del SX26 no llega a la app (HW-02 falla)

`SpeechRecognizer` no permite elegir el micrófono; depende del enrutamiento del sistema. Si el sistema no usa el USB:

1. Verificar que el SX26 aparece como entrada en `dumpsys audio` (sección 1).
2. Probar con otra app de grabación para descartar el dispositivo.
3. Si otras apps sí lo usan y la nuestra no, implementar captura propia con `AudioRecord.setPreferredDevice(usbDevice)` y alimentar el reconocedor con `RecognizerIntent.EXTRA_AUDIO_SOURCE` (Android 13+). Esto no se implementó porque no se puede validar sin el dispositivo.

## 5b. Solución de problemas con el dongle USB-C (reportados en pruebas reales)

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| Al conectar el dongle aparece "Configuración OTG" | El teléfono exige activar OTG para dispositivos USB (frecuente en Xiaomi/Redmi, Oppo, Vivo, Realme) | Activar OTG en Ajustes (p. ej. *Ajustes adicionales > OTG*). Algunos equipos lo desactivan solos tras unos minutos sin uso: volver a activarlo. Si el micrófono funciona en otras apps, OTG y el dongle están bien. |
| "Idioma español no soportado por el reconocedor del equipo" (error 12) o "Falta el paquete de voz…" (13) | La app pedía reconocimiento **sin conexión** y el equipo no tiene el paquete de español offline | Corregido en la app: ahora prueba, en orden, reconocimiento local → sin conexión preferido → en línea (es-PE) → en línea (es-419) y muestra un aviso con el modo en uso. Para operar sin internet (TC-NET-01) instalar el español en *Ajustes > Idioma > Voz > Reconocimiento de voz sin conexión* (o en la app Google > Ajustes > Voz). |
| "Error del cliente ASR de Android" (error 5) | Reinicios solapados del reconocedor, o el audio cambió de micrófono con el reconocedor ya abierto (típico al conectar el dongle) | Corregido en la app: un único reinicio pendiente, recreación automática del reconocedor y reinicio al detectar el cambio de fuente de audio. Si persiste: conectar el dongle **antes** de pulsar el micrófono y pulsar REINTENTAR. |
| Persisten los errores | Servicios de voz desactualizados o ausentes (tablets sin Google) | Actualizar la app «Google» y «Servicios de voz de Google»; capturar `adb logcat -s AndroidSpeechEngine ExternalAudioDevMgr` y registrar los códigos de error. |

**Importante para la tesis:** si el equipo cae a reconocimiento en línea, **deja de ser 100 % on-device**. El aviso en pantalla lo indica; el caso ASR-01 (modo avión) solo se aprueba si el modo en uso es local o sin conexión.

## 6. Defectos conocidos y limitaciones (a declarar en la tesis)

- La confianza C que entrega Android es **una por hipótesis, no por palabra** (todas las palabras de una frase comparten el mismo valor) y algunos motores no la entregan.
- El factor S depende de clusters finos; se añadió crédito parcial por área temática (4 áreas en Biología), una **propuesta del desarrollador que debe validar un docente de Biología**.
- Los resultados de los corpus sintéticos son "dentro de la muestra": varias mejoras se decidieron mirando esos mismos datos.
- `fuzzy_rules.fcl` documenta las reglas, pero la app ejecuta la implementación Kotlin equivalente (`JFuzzyEngine.kt`); no usa la librería jFuzzyLogic en tiempo de ejecución. `evaluation/verificar_fcl.py` demuestra que ambos son equivalentes; declararlo así en la tesis.
- `OpenNlpPosTagger` es un filtro heurístico (palabras vacías y sufijos), no el etiquetador OpenNLP.
- El corpus de evaluación es simulado (8 frases, solo Biología); no sustituye grabaciones reales.

## 7. Hoja de registro

| Caso | Equipo | Fecha | Resultado (OK / FALLA / N/A) | Valor medido | Evidencia (log/foto) | Observaciones |
|---|---|---|---|---|---|---|
| | | | | | | |
