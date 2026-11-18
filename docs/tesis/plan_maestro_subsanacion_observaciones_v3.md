# PLAN DE SUBSANACIÓN 3.0 — OBSERVACIONES DEL AVANCE (10/11/2026)

**Tesis:** Mejora de la precisión de sistemas Speech-to-Text mediante lógica difusa (UNSA)
**Responsable:** Bach. Miguel Angel Tinta Aguilar · **Entrega:** 02/12/2026
**Base:** código en `app/`, sesiones del 04/11 en `../csvs/`, Anexos B (registro técnico) e I (sustituciones erróneas) de `tesis_latex/`.

Cada observación tiene cuatro partes: **qué se encontró** en el código o en los datos, **qué se cambia**, **qué prueba lo demuestra** y **qué se escribe en la tesis**.

---

## 0. ORDEN DE TRABAJO (lo que no se puede invertir)

El conjunto de prueba solo es válido si la versión de la app queda **congelada antes** de grabarlo (observación 3). Por eso los cambios de código de las observaciones 1, 2 y 6 van **antes** del piloto, y el análisis (observaciones 3, 4 y 5) va después.

| Fecha | Trabajo | Observación |
|---|---|---|
| 10–11/11 | Registro de eventos del reconocedor; corrección de la pérdida en los reinicios; registro de la confianza cruda | 1, 2 |
| 11–12/11 | Interfaz: pausa del desplazamiento, volver al texto reciente, historial solo en el panel del investigador | 6 |
| 12–13/11 | Repetir ASR-03 (lectura y habla natural); CLASE-45 completa (45 min); HW-02 con recuperación; latencia externa | 1, 5, 6 |
| **13/11** | **Congelar la versión** (`v1.1.0`, etiqueta git y hash en la tesis) | 3, 5 |
| 14–15/11 | Piloto = corpus de **desarrollo**; ajuste de T1 solo con este corpus | 3, 5 |
| 16–24/11 | Corpus de **prueba** (docentes) y sesiones con estudiantes | 3, 4, 6 |
| 25–28/11 | Análisis y Capítulo IV | 3, 4, 5, 6 |

Si una prueba del 12–13/11 obliga a cambiar el código después de la congelación, se crea `v1.1.1` y **se repiten** las pruebas técnicas afectadas antes del piloto.

---

## 1. PÉRDIDA DE PALABRAS EN LA CAPTURA CONTINUA (ASR-03: 9,1 %)

### 1.1 Qué se encontró

En la sesión ASR-03 (`sesion_20261104_094816_inv_T2`) hay 6 enunciados y 5 transiciones entre ellos. Si se resta el `t_resultado` de un enunciado al `t_inicio_voz` del siguiente, sale **siempre lo mismo: 1 624 a 1 643 ms**. Un intervalo tan constante no corresponde a pausas del lector, que varían. Corresponde al **reinicio del reconocedor**:

- `onResults` → `scheduleRestart(150 ms)` → `cancel()` → `startListening()` (`AndroidSpeechEngine.kt:395-398` y `268-284`). Mientras el servicio vuelve a abrir el micrófono, **nadie escucha**.
- Las cuentas cuadran: 5 transiciones × ~1,6 s × ~2,4 palabras/s de lectura ≈ 19 palabras, justo las 19 perdidas.
- El texto lo confirma: los enunciados 4, 5 y 6 empiezan a mitad de frase («posee una membrana…», «la vegetal como la célula…», «llama zona red…»).

Hay dos fuentes de pérdida menores que el código también permite:
- Si el reconocedor da **error después de enviar parciales**, el texto parcial se descarta y no llega a la pantalla. `onError` no lo conserva, y la siguiente oración final reemplaza `activePartialLine`.
- Los reinicios tras un error esperan 350 ms (`Restart`) o 500 ms más la creación de un reconocedor nuevo (`Recreate`).

### 1.2 Qué se cambia

1. **Registro de eventos del reconocedor** (nuevo archivo `sesion_..._eventos.csv`). Una fila por callback, con su hora en `elapsedRealtime`:
   - `onReadyForSpeech`, `onBeginningOfSpeech`, `onEndOfSpeech`, `onPartialResults`, `onResults`, `onError(código)`, `reinicio_solicitado`, `reinicio_listo`.
   - Con esto, cada transición tiene un **tiempo sordo** medido: de `onResults` u `onError` al siguiente `onReadyForSpeech`.
2. **Eliminar el hueco en el origen (Android 13+, como el Tecno Spark 20).** Hay dos opciones de la API 33, que se prueban en este orden:
   - **a) Sesión segmentada** (`RecognizerIntent.EXTRA_SEGMENTED_SESSION`). El reconocedor no se cierra en cada pausa: entrega los segmentos por `onSegmentResults` y no hace falta reiniciar.
   - **b) Audio propio** (`EXTRA_AUDIO_SOURCE`). La app captura el micrófono con `AudioRecord` en un búfer circular y se lo pasa al reconocedor. Si el reconocedor se reinicia, el audio de ese intervalo queda en el búfer y se le entrega a la sesión nueva, así que no se pierde. Además, **la app guarda el audio de la clase con el mismo reloj** que los enunciados, lo que resuelve también el fin real de la voz (observación 5) y la transcripción de referencia.
   - No está garantizado que el reconocedor local de Google acepte estas opciones. Primero se hace una **prueba corta** (unos 30 min de trabajo) en el teléfono y en la tableta, y se registra qué opción funciona en cada equipo.
3. **Recorte del reinicio en los equipos sin API 33 o si fallan a) y b):**
   - Reiniciar sin la espera de 150 ms y sin `cancel()` cuando el resultado ya es final, porque la sesión ya terminó.
   - Bajar las esperas tras un error a lo mínimo que no provoque `ERROR_RECOGNIZER_BUSY`, medido con el registro de eventos.
4. **No perder parciales:** si llega un error después de un parcial, el último parcial pasa a ser el enunciado final, marcado `parcial_promovido = 1` en el CSV. Así se puede contar cuántas veces ocurre.
5. En la tesis se cita la advertencia de Android: `SpeechRecognizer` no está pensado para reconocimiento continuo. Por eso la continuidad se **verifica**, no se supone.

### 1.3 Qué prueba lo demuestra

| Prueba | Cómo | Criterio |
|---|---|---|
| ASR-03a (lectura) | Mismo texto de 208 palabras, misma posición del micrófono, versión nueva | Pérdida < 2 % |
| ASR-03b (habla natural) | 5 min de explicación libre del investigador; referencia transcrita escuchando el audio | Pérdida < 2 %; las pérdidas se reportan **por transición** y **dentro de enunciado** por separado |
| CLASE-45 completa | **45 min** seguidos con el receptor inalámbrico, sin tocar el equipo | 0 cierres; tiempo sordo total y máximo; batería y temperatura al inicio, a los 15, 30 y 45 min |

La **pérdida por transición** se calcula alineando la referencia con el texto reconocido. Una palabra omitida que cae entre el final de un enunciado y el inicio del siguiente cuenta como pérdida por reinicio. Se agrega a `evaluar_sesiones.py`.

### 1.4 Qué se escribe en la tesis

- Anexo B: ASR-03 (04/11) se mantiene como **FALLA** con su causa identificada, y debajo van las filas nuevas (ASR-03a, ASR-03b, CLASE-45) con su resultado real, sea el que sea.
- Si la pérdida sigue en 2 % o más, se dice así: es una limitación de acceso al discurso que la capa difusa **no puede** compensar, porque no se corrige lo que no se reconoció.

---

## 2. TRATAMIENTO DE LA CONFIANZA ACÚSTICA

### 2.1 Qué se encontró

- `resolveConfidence()` (`AndroidSpeechEngine.kt:474-477`) convierte **cualquier** valor menor o igual a 0 en 0,65. Así mezcla tres casos distintos: −1 (no disponible), 0,0 (baja según la API) y la ausencia del arreglo.
- El CSV ya guarda el valor crudo (`confianza_api`, p. ej. `0.0000 0.0000`) y el usado (`c_usada = 0.650`). Esa es la base de la evidencia.
- En ASR-02, el reconocedor local entregó **0,0 en todas las hipótesis** (35 de 35, en 18 enunciados), incluidas las alternativas y frases reconocidas sin ningún error.
- Los **parciales** se corrigen con una confianza fija de **0,60** (`AndroidSpeechEngine.kt:410`), distinta del 0,65 de los finales. Es otra constante sin justificar.

### 2.2 Qué se cambia

1. **Conservar siempre el valor original** y añadir al CSV la columna `c_estado`, con estos valores:
   - `ausente`: no llega el arreglo.
   - `no_disponible`: −1.
   - `invalido`: NaN o fuera de [0, 1].
   - `cero`: 0,0.
   - `valido`: (0, 1].
   - Se añade también `c_sustitutiva` (0/1), que indica si se usó el valor de respaldo.
2. **Política explícita por proveedor** en lugar de la regla general:
   - Con `valido`, se usa el valor medido.
   - Con `ausente`, `no_disponible` o `invalido`, se usa el respaldo.
   - Con `cero`, se usa el respaldo **solo** si el perfil activo está en la lista de proveedores con ceros no informativos comprobados (hoy, el reconocedor local es-US del Tecno Spark 20). En cualquier otro caso, 0,0 se trata como confianza baja, como dice la documentación.
3. Los parciales usan el **mismo** respaldo que los finales.

### 2.3 Cómo se comprueba que los ceros no informan (y se documenta)

1. **Contraste de calidad:** 20 frases leídas con claridad y 20 con ruido, murmullo o palabras inventadas, con el reconocedor local. Si la confianza informara, los dos grupos darían valores distintos. Si los 40 dan exactamente 0,0, el valor no informa.
2. **Contraste de proveedor:** las mismas frases con el perfil en línea (es-419). Si ese proveedor sí devuelve valores variables, el cero es propio del reconocedor local y no de la frase.
3. **Repetir en la tableta.** Si allí la confianza sí informa, en la tableta se usa el valor medido.
4. Las líneas `ASR02` de logcat y el CSV se guardan como evidencia (Anexo B).

### 2.4 Sensibilidad al valor de respaldo

`benchmark_runner.py` ya admite `fixed_conf`. Se recalcula la corrección sobre **la misma salida cruda** con C = 0,0 / 0,3 / 0,5 / 0,65 / 0,8 / 1,0. Para cada valor se reporta:
- WER, aciertos, falsas correcciones y precisión de la sustitución.
- El **número de decisiones que cambian** respecto a C = 0,65.

Si el resultado depende mucho del valor elegido, hay que decirlo. Si casi no cambia, también se reporta.

### 2.5 Qué se escribe en la tesis

- En el teléfono evaluado, el sistema funciona con **confianza sustitutiva constante**. La decisión depende en la práctica de D y S. **No se afirma** que integre incertidumbre acústica medida por enunciado.
- El modelo de tres entradas queda descrito como diseño. Su comportamiento con C real solo se afirma en un equipo donde se haya medido.

---

## 3. MEJORA SIN OCULTAR EL DAÑO

### 3.1 Qué se encontró

- `evaluar_sesiones.py` ya calcula, por condición (T0, T1, T2): WER, CER, KER, sustituciones, aciertos, falsas correcciones, cambios inútiles, TCR, FCR y precisión de la sustitución. Lo que falta es **presentarlo completo** en el Capítulo IV.
- **El Anexo I tiene filas sin sesiones que las respalden.** El propio archivo lo indica: `% PENDIENTE: reemplazar las filas con la salida de evaluar_sesiones.py de las pruebas en aula`. En `csvs/` no hay ninguna sesión con código D01 ni D02. Las 19 filas no pueden quedar en la tesis como resultados.
- Las sustituciones del video («cédula→célula», «flema→floema») vienen de **errores pronunciados a propósito**. Sirven para demostrar el funcionamiento, no como evidencia de mejora.

### 3.2 Qué se cambia

1. **Retirar ahora las filas del Anexo I.** Se deja el anexo con su estructura y la frase «se completa con la salida de `evaluar_sesiones.py` del corpus de prueba». Si se quiere conservar un ejemplo, se rotula como **ilustrativo** y sin códigos de sesión.
2. La tabla final del anexo va **con sus denominadores**: palabras que el reconocedor acertó (`crudo_correctas`), palabras erradas (`crudo_sust`), aciertos, falsas e inútiles. Así cada falsa corrección se ve en proporción (FCR = falsas / palabras correctas).
3. **Tres corpus separados**, cada uno con su propio fin:
   - **Demostración:** errores pronunciados a propósito. Solo para el video y las capturas. No entra en ninguna métrica.
   - **Desarrollo:** el piloto. Se usa para ajustar T1 y para encontrar problemas.
   - **Prueba:** docentes del 16 al 24/11, con habla natural y errores reales del reconocedor. Es el **único** que sustenta las hipótesis.
4. **Regla de congelación:** casos como «planta→plantae» o «cerebro→cerebelo» que aparezcan en el corpus de prueba **no se usan** para cambiar reglas, diccionario ni umbrales. Si se corrige algo, la nueva versión se evalúa con otro corpus independiente, y se reportan las dos.

---

## 4. PROCEDENCIA DEL CORPUS Y CAPÍTULO DE RESULTADOS

### 4.1 Qué se encontró

Las sesiones de `csvs/` son del 04/11 y tienen el participante `SINCODIGO` o `inv`. Son pruebas técnicas del investigador, no clases. El cronograma deja el aula como pendiente, pero el Anexo I habla de D01 y D02 (observación 3). Esa contradicción se resuelve retirando las filas.

### 4.2 Qué se cambia

1. **Tabla de procedencia** (nuevo anexo y `evaluation/procedencia.csv`). Una fila por sesión, con estas columnas:
   - `archivo`, `fecha`, `tipo` (`simulacion` | `laboratorio` | `docente` | `aula`), `hablante` (código), `duracion_min`, `texto` (`lectura` | `habla_natural`).
   - `referencia` (`si`/`no`, y quién la transcribió), `condicion`, `version_app`, `corpus` (`demostracion` | `desarrollo` | `prueba`).
   - `evaluar_sesiones.py` lee esta tabla y **rechaza** las sesiones que no figuren en ella.
2. **Resultados por docente y globales.** Cada métrica se reporta por hablante y en total. En la discusión se aclara que 300 enunciados de 2 docentes son **2 hablantes**, no 300 observaciones independientes:
   - La prueba de Wilcoxon por enunciado se mantiene, pero se acompaña del resultado por docente.
   - Se calcula un intervalo bootstrap **remuestreando por docente**.
3. El Capítulo IV se ordena por procedencia: (1) pruebas técnicas, (2) corpus de prueba de docentes, (3) sesiones con estudiantes.

---

## 5. COMPARACIÓN T1–T2 Y MEDICIÓN DEL TIEMPO

### 5.1 Qué se encontró

- T1 ya se calcula sobre el mismo texto crudo y se ajusta solo con el corpus de desarrollo (`evaluar_sesiones.py:532-546`). Esto se mantiene.
- T2 se toma **de lo que calculó la app en vivo**. La memoria temática (`LessonContext`) depende del historial y del tiempo: tiene una semivida de 45 s. Si una sesión empieza con memoria arrastrada, o con otro ritmo, T2 cambia por razones ajenas a la capa difusa.
- La «latencia» de 198 ms (p95) va de `onEndOfSpeech` a la publicación. Pero `onEndOfSpeech` es **el aviso del reconocedor** de que detectó silencio, y llega después del fin real de la voz.

### 5.2 Qué se cambia

1. **Recalcular T2 fuera de línea** con el espejo Python del motor (`benchmark_runner.py`, cuya paridad con Kotlin ya está verificada):
   - Se reinicia la memoria al comienzo de cada sesión.
   - Se usan los tiempos registrados en el CSV (`t_resultado`), de modo que la semivida actúe igual que en vivo.
   - Antes de usarlo, se comprueba que el T2 recalculado coincide con el T2 de la app en el corpus de desarrollo.
2. **Nueva condición T2-sin-contexto** (ablación): igual que T2, pero sin memoria temática (`lessonContext = null`, `use_memory=False`). Así se separa lo que aporta el contexto de lo que aporta la regla difusa.
   - Si T2 supera a T1, la tesis dice que se compara **el sistema completo**.
   - La diferencia T2 frente a T2-sin-contexto indica el aporte del contexto.
3. **Parámetros fijados** antes del corpus de prueba: umbrales de T1, reglas, diccionario y C de respaldo. Todo queda en la etiqueta `v1.1.0`.
4. **Tres tiempos separados en la tesis:**
   - **Tiempo de corrección** (`ms_capa_difusa`): media 23 ms, p95 37 ms.
   - **Tiempo de presentación** (de `onEndOfSpeech` a la publicación): p95 198 ms, presentado como lo que es.
   - **Latencia percibida**, del **fin real de la voz** al texto visible. Se mide de dos formas:
     - **Audio:** con la opción 1.2-b, el fin real de la voz se obtiene de la energía de la señal grabada, con el mismo reloj. Se reporta cuánto tarda el reconocedor en avisar (`t_fin_voz − fin_real`).
     - **Video:** se graba en cámara lenta a la vez la boca del hablante, o una palmada, y la pantalla. Así se verifican de forma externa unos 20 enunciados (prueba A8b del plan v3).

---

## 6. INTERFAZ DESDE LAS TAREAS DEL ESTUDIANTE

### 6.1 Qué se encontró

- **El desplazamiento automático no se puede pausar.** Cada línea nueva ejecuta `animateScrollToItem(último)` (`TeleprompterScreen.kt:94-98`). Si el estudiante sube a releer una frase, la siguiente línea lo devuelve abajo.
- El **historial de correcciones**, con «C: 65 %» (`CorrectionsHistoryScreen.kt:219`), se abre desde la barra del estudiante. Muestra como medida un valor que es fijo.
- Ya existen: avisos de conexión y desconexión del receptor, exportación a TXT y PDF, y botones A−/A+.

### 6.2 Qué se cambia

1. **Pausa y regreso:**
   - El desplazamiento automático solo actúa si el estudiante está al final del texto.
   - Si sube a leer, el texto se queda quieto y aparece el botón **«Volver al texto reciente»**.
   - La escucha no se detiene en ningún momento.
2. **Historial y valores C, D, S e y\* solo en el panel del investigador.** Si se muestra C, va como «C = 0,65 (fija, el reconocedor no informa)».
3. **Guardar apuntes sin cortar la escucha:** se comprueba que exportar no detiene el reconocedor. Se compara automáticamente el TXT exportado con la unión de `texto_mostrado` del CSV y se reportan las diferencias.
4. **Micrófono:** prueba HW-02 completa:
   - Desconectar el receptor en plena clase.
   - Medir el tiempo hasta que vuelve el texto con el micrófono interno.
   - Volver a conectarlo.
   - Comprobar el aviso en pantalla y la entrada en el registro de eventos.
   - Se verifica también la caída de la señal inalámbrica: apagar el transmisor.

### 6.3 Accesibilidad comprobada con tareas

Cada estudiante, después de la sesión, hace cuatro tareas cortas. Se anotan el éxito, el tiempo y la ayuda pedida en la ficha de observación:
1. Encontrar una frase que dijo el docente hace unos 2 minutos.
2. Volver al texto en vivo.
3. Cambiar el tamaño de la letra.
4. Guardar los apuntes.

Se registran también la distancia de lectura, el tamaño de letra elegido y la velocidad de actualización: líneas por minuto y palabras por actualización, sacadas del CSV.

### 6.4 Separar la evaluación técnica de la experiencia del estudiante

- El Capítulo IV presenta **por separado** la precisión técnica (WER y métricas afines) y la experiencia (SUS, NASA-TLX, tareas, entrevista). Una reducción del WER no se presenta como prueba de menor carga.
- **Control de las mitades de clase:**
  - El orden T0/T2 ya está contrabalanceado.
  - Además, se pide al docente que **las dos mitades traten el mismo tema con dificultad parecida**, y se anota en la ficha el tema y la densidad de términos de cada mitad.
  - La fatiga se controla con mitades cortas y con el orden.
- **Duración:** en una hora pedagógica (unos 45 min) no entran dos mitades de 20 min más los cuestionarios y la entrevista. Se proponen **mitades de 15 min**, cuestionarios entre las mitades y al final, y la entrevista después de la clase.
- Los resultados de SUS y NASA-TLX se presentan **por estudiante** (tabla individual), con interpretación **exploratoria** si la muestra es menor de 6.

---

## 7. RESUMEN DE CAMBIOS EN EL CÓDIGO

| Archivo | Cambio | Obs. |
|---|---|---|
| `AndroidSpeechEngine.kt` | Registro de eventos; sesión segmentada o audio propio (API 33); reinicio sin espera tras resultado final; promoción del último parcial; `c_estado` y política de confianza por proveedor; un solo valor de respaldo | 1, 2 |
| `UtteranceLogger.kt` | Columnas `c_estado`, `c_sustitutiva`, `parcial_promovido`; archivo `_eventos.csv` | 1, 2 |
| `TeleprompterScreen.kt` | Desplazamiento automático solo al final; botón «Volver al texto reciente»; quitar el acceso al historial de la barra del estudiante | 6 |
| `InvestigatorPanel.kt` | Acceso al historial con C (marcada como fija), D, S e y\* | 6 |
| `evaluar_sesiones.py` | Lectura de `procedencia.csv`; métricas por docente; pérdida por transición; T2 recalculado y T2-sin-contexto; sensibilidad a C; bootstrap por docente | 1–5 |

## 8. RESUMEN DE CAMBIOS EN LA TESIS

| Lugar | Cambio |
|---|---|
| Anexo I | **Retirar ya** las 19 filas D01/D02; completar con datos reales y denominadores |
| Anexo B | ASR-03 original con su causa; filas nuevas ASR-03a/b, CLASE-45 (45 min), HW-02 con recuperación, prueba de la confianza |
| Nuevo anexo | Tabla de procedencia del corpus |
| Cap. III | Confianza: estados del valor, política por proveedor, «confianza sustitutiva» en el equipo evaluado; condición T2-sin-contexto; definición de los tres tiempos |
| Cap. IV | Orden por procedencia; resultados por docente y globales; tabla completa T0/T1/T2 con daño; sensibilidad a C; tareas de interfaz; SUS y NASA-TLX individuales |
| Cronograma | Coherente con lo realizado: nada en aula se presenta como hecho antes de su fecha |
