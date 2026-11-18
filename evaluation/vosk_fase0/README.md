# Fase 0: ¿la confianza C de Vosk sirve para el motor difuso?

## Por qué

Con el ASR local de Android, `CONFIDENCE_SCORES` llega siempre en `0.0`. La app usa entonces C = 0.65 fija: así ocurrió en los 258 enunciados del piloto del 04/11/2026, todos con `confianza_api = 0.0000` y `c_usada = 0.650`. Con C = 0.65, la pertenencia es μ_Media = 0.75 y μ_Baja = μ_Alta = 0. En la práctica solo se activan las reglas 10–18 y el modelo funciona con dos variables (D y S).

Vosk entrega una confianza **por palabra** (`conf`, posterior del lattice). Antes de cambiar el ASR de la app, esta fase mide en la PC tres cosas:

| | Pregunta | Cómo se mide |
|---|---|---|
| **V1** | ¿Las palabras mal reconocidas salen de "Alta"? | % de palabras erróneas con C < 0.85, comparado con las correctas. Que las correctas caigan en "Alta" es lo esperado |
| **V2** | ¿C es más baja cuando Vosk se equivoca? | AUROC entre palabras correctas y erróneas (0.5 = azar) |
| **V3** | ¿Cambia lo que hace la capa difusa? | `benchmark_runner.py` sobre la salida de Vosk, con C real frente a C = 0.65 |

Vosk decodifica de forma determinista: el mismo audio con el mismo modelo da las mismas confianzas en la PC y en Android (`vosk-android`). Por eso esta prueba representa bien lo que pasaría en el teléfono. Lo único que **no** mide es la latencia en el Tecno Spark 20.

## 1. Preparar (una sola vez)

```sh
cd evaluation/vosk_fase0
uv venv -p 3.12 .venv
uv pip install -p .venv/bin/python -r requirements.txt

mkdir -p modelos && cd modelos
curl -LO https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip
unzip vosk-model-small-es-0.42.zip && cd ..
```

Se necesita `ffmpeg` en el PATH, que lee wav, m4a, ogg y mp3. Se usa Python 3.12 porque es la versión que verificamos con el paquete `vosk`; en lugar de activar el entorno, los comandos llaman directamente a `.venv/bin/python`. El modelo pequeño (~40 MB) es el que cabe en el teléfono; el grande (~1.4 GB) no es viable en el equipo de prueba, así que no tiene sentido evaluarlo aquí.

## 2a. Prueba rápida con un corpus público (sin grabar)

[OpenSLR 73](https://www.openslr.org/73/) tiene 5,447 frases leídas por hablantes peruanos, con transcripción y licencia CC BY-SA 4.0. Para citarlo: Guevara-Rukoz et al., LREC 2020. `descargar_slr73.py` baja solo una muestra repartida entre hablantes (~0.6 MB por frase), sin descargar los zips completos de 1.9 GB:

```sh
.venv/bin/python descargar_slr73.py --n 200
.venv/bin/python vosk_fase0.py audios/slr73/* --modelo modelos/vosk-model-small-es-0.42 --salida resultados/slr73_n200
```

**Qué responde:** si C separa errores de aciertos en el habla peruana (V1, V2) y si la capa difusa hace **falsas correcciones** en frases cotidianas. El corpus no trata de biología, así que cualquier sustitución en él es un error.

**Qué no responde:** el rendimiento con el SX26 en un aula ni la corrección de términos de biología, porque KER = 0 al no haber términos del diccionario. Además, el modelo de Vosk pudo entrenarse con este mismo corpus, y entonces su WER saldría optimista. Para la decisión final siguen haciendo falta las grabaciones propias del paso 2b.

## 2b. Grabar

Usa la **cadena real**: transmisor SX26 a 15–20 cm de la boca, receptor USB-C en el teléfono y una app grabadora que use el micrófono USB. Lee el guion de [`../guion_docente_biologia.md`](../guion_docente_biologia.md) y guarda los archivos en `audios/`.

| Toma | Qué leer | Para qué |
|---|---|---|
| `toma1_pausas` (obligatoria) | Bloques 1–4, con pausa de 1–2 s entre frases (~3 min) | Caso base, igual que ASR-02 |
| `toma2_continua` (obligatoria) | Bloques 1–4 sin pausas | Enunciados largos, igual que ASR-03 |
| `toma3_ruido` (recomendada) | Bloques 1–4 con ruido de aula (ventilador, voces, puerta) | Provocar errores: V2 necesita al menos 10 palabras erróneas |
| `toma4_otra_voz` (recomendada) | Bloques 1–4 leídos por otra persona, idealmente un docente | C depende del hablante |

**Referencia:** si leíste el guion tal cual, basta con `--guion`. Si te saltaste o agregaste frases, crea `audios/<nombre>.txt` con lo que se dijo realmente; ese archivo tiene prioridad sobre el guion. Si solo leíste algunos bloques, usa `--bloques 1,2`.

Las grabaciones son datos personales (la voz de quien lee). `audios/`, `modelos/` y `resultados/` están excluidos de git.

## 3. Ejecutar (grabaciones propias)

```sh
.venv/bin/python vosk_fase0.py audios/* --modelo modelos/vosk-model-small-es-0.42 --guion
```

En `resultados/<fecha_hora>/` aparecen tres archivos:

- `reporte_fase0.md`: veredicto, distribución e histograma de C, calibración y simulación de la capa difusa.
- `palabras.csv`: una fila por palabra con C, tiempos, pertenencias Baja/Media/Alta, si es candidata y si fue correcta (`ok`), sustituida (`sub`) o insertada (`ins`).
- `corpus_vosk.json`: la salida de Vosk en el formato de corpus de `benchmark_runner.py`, para reutilizarla.

Los `.txt` de referencia que estén en `audios/` se ignoran como entrada de audio, así que `audios/*` funciona igual.

## 4. Interpretar

| Veredicto | Significado | Siguiente paso |
|---|---|---|
| **CONTINUAR** | Las palabras erróneas salen de "Alta" (V1) y C separa errores de aciertos (V2) | Fase 1: interfaz `SpeechEngine` y `VoskSpeechEngine` |
| **CONTINUAR CON RECALIBRACIÓN** | C informa, pero las palabras erróneas siguen cayendo en "Alta" | Mover las funciones de pertenencia de C según la tabla de calibración del reporte, documentarlo en un ADR y luego pasar a la fase 1 |
| **NO CONTINUAR** | AUROC < 0.65: C no dice nada sobre los errores | Vosk no resuelve el problema de C; replantear cómo obtener C |
| **INCOMPLETO** | Sin referencia o con menos de 10 errores | Agregar la toma con ruido u otra voz |

Revisa también:

- **WER de Vosk.** Si supera ~35 %, los subtítulos crudos quizá no sirvan en clase aunque C funcione. La simulación muestra cuánto recupera la capa difusa.
- **V3.** Si la C real y la C fija producen las mismas sustituciones, C sigue sin influir en las decisiones con las reglas actuales, aunque varíe.

Los umbrales (40 %, 0.65, 10 errores, 35 %) son **propuestas**. Están al inicio de `vosk_fase0.py`; si se usan en la tesis, hay que justificarlos.

### Opcional: comparar con Android sobre el mismo audio

Reproduce `toma1_pausas` por un parlante cerca del teléfono con la app en condición T0. El `texto_crudo` del CSV de la sesión da la línea base de Android sobre la misma lectura, para compararla con el WER de Vosk.
