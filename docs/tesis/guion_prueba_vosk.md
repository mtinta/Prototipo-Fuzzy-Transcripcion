# Guion de prueba: motor Vosk (fase 1)

**Objetivo:** comprobar que la app transcribe con Vosk sin conexión, que la confianza acústica **C varía por palabra** y que el resto (micrófono SX26, telemetría, cambio de motor) sigue funcionando. Al final se envían los archivos para la fase 2 (latencia y ajuste del fin de frase).

- **Parte A, emulador** (~20 min): comprobar que todo funciona, con audios repetibles.
- **Parte B, Tecno Spark 20 con SX26** (~40 min): la prueba real; **es la que vale para la tesis**.

Texto para leer en voz alta: los bloques 1 a 4 de [`evaluation/guion_docente_biologia.md`](../../evaluation/guion_docente_biologia.md).

---

## 0. Preparación (una vez)

```sh
cd ~/Projects/"Prototipo fuzzy"/Prototipo-Fuzzy-Transcripcion
./gradlew testDebugUnitTest assembleDebug     # 78 pruebas + APK en app/build/outputs/apk/debug/
alias adb=~/Android/Sdk/platform-tools/adb
```

---

## Parte A: emulador

### A1. Arrancar

```sh
~/Android/Sdk/emulator/emulator -avd Spark20_API33 -allow-host-audio -gpu host &
adb wait-for-device && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En la ventana del emulador ve a **`⋯` → Microphone** y activa **"Virtual microphone uses host audio input"**. Hay que hacerlo **cada vez** que arrancas el emulador; si no, la app recibe silencio.

Deja abierta otra terminal con el registro del motor:

```sh
adb logcat -c && adb logcat -s VoskSpeechEngine:I AndroidRuntime:E
```

### A2. Pruebas

| # | Qué hacer | Qué debe pasar | ✔ / ✘ y notas |
|---|---|---|---|
| A2.1 | Abrir la app por primera vez | Aviso "Preparando el modelo de voz sin conexión…" que desaparece en pocos segundos; en el registro, `Modelo Vosk listo` | |
| A2.2 | Panel del investigador (pulsación larga sobre **"Prueba"**): participante `INV`, etiqueta `emu_voz`, motor **Vosk**, *Aplicar* | Pantalla limpia, archivo nuevo | |
| A2.3 | **INICIAR MICRÓFONO** y quedarse en silencio 10 s | Cada 5 s aparece `NIVEL pico=…%`. Con 0.0 % falta el interruptor de A1 | |
| A2.4 | Leer el **bloque 1** cerca del micrófono del PC | Texto en amarillo mientras hablas; al terminar cada frase, una línea `ASR02 … confianzas=[…]` con **valores distintos entre sí** | |
| A2.5 | Leer el **bloque 4** (control: *calor*, *forma*…) | Ninguna palabra común cambia por un término técnico | |
| A2.6 | Reproducir audios (con la app escuchando, desde otra terminal):<br>`bash scripts/emulador_audio.sh evaluation/vosk_fase0/audios/slr73/pef_00*.wav` (6 clips, ~40 s) | Un enunciado por clip, con un texto parecido al `.txt` de cada clip. Al final: "Conexiones de audio restauradas" | |
| A2.7 | **DETENER MICRÓFONO** a mitad de una frase | La frase en curso se publica completa; "Micrófono pausado" | |
| A2.8 | Panel → motor **Android** → *Aplicar*; leer una frase | Funciona o muestra un aviso, pero sin cerrarse la app. En el emulador puede no haber voz de Google sin conexión: es normal | |
| A2.9 | Panel → volver a **Vosk** | Vuelve a transcribir | |
| A2.10 | Apagar y encender T2 (corrección difusa) en el panel | Cada cambio abre un archivo nuevo | |

Para revisar el CSV del emulador:

```sh
adb pull /sdcard/Documents/PrototipoFuzzy/ resultados_emulador/
```

---

## Parte B: Tecno Spark 20 con SX26

### B0. Antes de instalar

1. **Respalda las sesiones anteriores**, porque son datos del piloto:
   ```sh
   adb pull /sdcard/Documents/PrototipoFuzzy/ respaldo_piloto_$(date +%F)/
   ```
2. Instala **encima** de la versión que ya tienes, **sin desinstalar**: la firma es la misma.
   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Si sale `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, **no desinstales**: desinstalar borra las sesiones. Avísame.
3. **Depuración por Wi‑Fi.** El receptor SX26 ocupa el único puerto USB-C, así que el registro tiene que ir por Wi‑Fi:
   - En el teléfono: *Ajustes → Opciones de desarrollador → Depuración inalámbrica* → activar → *Vincular con código*.
   - En la PC:
     ```sh
     adb pair IP:PUERTO_DE_VINCULACIÓN      # pide el código de 6 dígitos
     adb connect IP:PUERTO                  # el que aparece en "Depuración inalámbrica"
     adb logcat -c && adb logcat -s VoskSpeechEngine:I AndroidRuntime:E ExternalAudioDevMgr:I > logcat_vosk.txt
     ```
   Si no consigues conectarte por Wi‑Fi, haz la prueba igual: el CSV guarda casi todo lo necesario.

### B1. Pruebas

Antes de cada prueba abre el panel y pon participante `INV` y la etiqueta indicada.

| # | Etiqueta | Qué hacer | Qué debe pasar | ✔ / ✘, tiempos y notas |
|---|---|---|---|---|
| B1.1 | — | **Modo avión**; abrir la app por primera vez tras instalar | "Preparando el modelo…" desaparece. **Anota cuántos segundos tarda** | |
| B1.2 | — | Conectar el receptor SX26 **antes** de iniciar; transmisor a 15–20 cm de la boca | La barra muestra **"SX26 inalámbrico"**; en el registro, `Captura iniciada (entrada: <nombre del receptor>)` | |
| B1.3 | `vosk_pausas` | Leer los **bloques 1 a 4 con pausas** de 1–2 s entre frases | Una línea por frase; el texto aparece mientras hablas; C variable en el historial de correcciones | |
| B1.4 | `vosk_continua` | Leer los **bloques 1 a 4 sin pausas** (~3 min) | La app no se traba; anota si las líneas salen muy largas o muy cortadas | |
| B1.5 | `vosk_usb` | Mientras escucha: **desconectar** el receptor, hablar 10 s, **reconectar**, hablar 10 s | Sigue transcribiendo con el micrófono del teléfono y luego vuelve al SX26 sin pulsar nada | |
| B1.6 | `vosk_ruido` | Leer el **bloque 2** con ruido (ventilador, radio baja o una conversación cerca) | Anota si aparecen frases inventadas por el ruido y qué C tienen | |
| B1.7 | `android_pausas` | Panel → motor **Android**; leer los **bloques 1 y 2 con pausas** | Línea base del motor anterior, para comparar | |
| B1.8 | `vosk_largo` | Panel → **Vosk**; dejar 10 min con una clase grabada, un video o una lectura continua | No se cierra ni se detiene; anota si el teléfono se calienta y cuánta batería consume | |

**Durante B1.3 a B1.7** fíjate sobre todo en:

- **Tiempo de respuesta:** ¿el texto definitivo tarda más de ~1 s en aparecer tras terminar cada frase? En el emulador tardó ~1.35 s.
- **Cortes de frase:** ¿se parten las frases a la mitad cuando haces una pausa corta?
- **Correcciones:** en el historial (icono de corrector), ¿*cédula → célula* y similares se corrigen? ¿Alguna palabra común cambió por error?

### B2. Al terminar

```sh
adb pull /sdcard/Documents/PrototipoFuzzy/ resultados_vosk_$(date +%F)/
# si el registro por Wi‑Fi no estuvo activo durante la prueba:
adb logcat -d -s VoskSpeechEngine:I AndroidRuntime:E > logcat_vosk.txt
```

**Envíame:** la carpeta `resultados_vosk_…` (los CSV `sesion_*` y sus `_meta.txt`), `logcat_vosk.txt` si lo tienes, y esta hoja con las columnas ✔ / ✘ y las notas llenas.

---

## Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `NIVEL pico=0.0%` en el emulador | Falta el interruptor del micrófono | `⋯` → Microphone → "Virtual microphone uses host audio input" |
| `NIVEL pico` muy bajo (< 1 %) hablando con el SX26 | Ganancia baja o transmisor apagado | Revisa batería y emparejamiento del SX26 y acerca el transmisor |
| "Preparando el modelo…" no desaparece o sale "No se pudo cargar el modelo" | Falta espacio o la copia falló | Libera ~100 MB y abre la app otra vez. Mientras tanto puedes usar el motor Android |
| La barra dice "Mic. del teléfono" con el SX26 conectado | El receptor no se detectó | Desconéctalo, espera 3 s, vuelve a conectarlo |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | La APK se firmó con otra clave | **No desinstales**; avísame |
| `emulador_audio.sh` dice "No encuentro la entrada del emulador" | La app no está escuchando | Pulsa INICIAR MICRÓFONO y vuelve a ejecutarlo |
