# Guía del Código del Prototipo

**Para quién es:** tú (autor), tu asesor y cualquier jurado que abra el repositorio. Explica qué hace cada archivo, cómo viaja una palabra por el sistema y dónde se cambia cada parámetro.
**Paquete Kotlin:** `com.unsa.dhh.captions` · **Ruta base:** `app/src/main/java/com/unsa/dhh/captions/`

---

## 1. El sistema en una página

El docente habla por un micrófono (SX26 u otro). Android convierte la voz en texto (ASR), pero se equivoca con vocabulario técnico ("sitoplasma", "cédula"). El prototipo **corrige en silencio** esas palabras antes de mostrarlas al estudiante sordo, solo cuando tiene evidencia suficiente.

```
 Micrófono ──► AudioForegroundService ──► (Android SpeechRecognizer, es-PE, preferir offline)
 SX26/USB      mantiene la sesión viva          │  texto + confianza C
                                                 ▼
                                    AndroidSpeechEngine  ──►  CaptionsViewModel (cola secuencial)
                                                                    │
                                                                    ▼
                                                           DecisionEvaluator
                              ┌─────────────────────────────────────────────────────────┐
                              │ Pasada 1: ¿qué palabras parecen términos del diccionario?│
                              │           (OpenNlpPosTagger filtra, LevenshteinMatcher → D)│
                              │ Pasada 2: S = coherencia con la oración + memoria de clase │
                              │           JFuzzyEngine: reglas Mamdani (C, D, S) → y*      │
                              │           si y* ≥ 0.65 → sustituir por el término canónico │
                              └─────────────────────────────────────────────────────────┘
                                                                    │
                          ┌─────────────────────────────────────────┼──────────────────────┐
                          ▼                                         ▼                      ▼
                  TeleprompterScreen                      SessionLogManager        NotesExporter
                  (2-3 líneas, WCAG AAA)                  (Room: historial)        (TXT / PDF)
```

**Las tres variables (todas intervienen siempre):**

| Variable | Significa | Se calcula en |
|---|---|---|
| **C** Confianza acústica | Qué tan seguro está el ASR | `AndroidSpeechEngine.resolveConfidence` |
| **D** Similitud léxico-fonética | Qué tan parecida es la palabra al término del diccionario | `LevenshteinMatcher.calculateCombinedSimilarity` |
| **S** Coherencia semántica | Si el término encaja con el tema (oración + clase) | `DecisionEvaluator.calculateSemanticCoherence` |

---

## 2. Mapa de archivos

### Módulo 1 · Captura de audio (`service/`)
| Archivo | Qué hace |
|---|---|
| `AudioForegroundService.kt` | Servicio en primer plano (tipo micrófono) con WakeLock de 90 min. Muestra una notificación que refleja la fuente activa. |
| `ExternalAudioDeviceManager.kt` | Detecta el receptor USB del SX26 / micrófono 3.5 mm con `AudioDeviceCallback` + broadcasts. Si se desconecta, vuelve al micrófono interno. `classifyInputs()` es la lógica pura (probada en JVM). |

### Módulo 2 · Reconocimiento de voz (`domain/asr/`)
| Archivo | Qué hace |
|---|---|
| `AndroidSpeechEngine.kt` | Envuelve `SpeechRecognizer`. Idioma `es-PE`, `EXTRA_PREFER_OFFLINE`, resultados parciales, **reinicio automático** tras cada resultado/silencio. `resolveConfidence()` convierte confianzas desconocidas (-1, 0, ausente) en un valor neutro 0.65. Prueba una **escalera de perfiles** (local del equipo → sin conexión preferido → en línea es-PE → en línea es-419) según indique `RecognitionFallbackPolicy`; avisa del modo en uso (`modeNotice`) y se recrea si cambia el micrófono. |
| `RecognitionFallbackPolicy.kt` | Decide ante cada error si reintentar, recrear el reconocedor, pasar al siguiente perfil o rendirse. Lógica pura y probada (`RecognitionFallbackPolicyTest`). |
| `SpeechState.kt` | Estados del motor (`Idle`, `Listening`, `Processing`, `PartialText`, `FinalText`, `Error`) y `RawTranscriptionResult`. |

### Módulo 3 · Núcleo difuso y PLN (`domain/fuzzy/`, `domain/nlp/`)
| Archivo | Qué hace |
|---|---|
| `MembershipFunctions.kt` | Funciones de pertenencia triangular y trapezoidal (con hombros verticales: `a==b` o `c==d`). |
| `JFuzzyEngine.kt` | Motor **Mamdani**: 27 reglas, AND=MIN, agregación MAX, defuzzificación por centroide (COG) con 101 puntos. Implementación propia en Kotlin (no usa la librería jFuzzyLogic). |
| `LevenshteinMatcher.kt` | Distancia de edición + normalización fonética hispana (b/v, c/s/z, ll/y, h muda, qu/k). **D = 0.6·ortográfica + 0.4·fonética.** |
| `OpenNlpPosTagger.kt` | Filtro heurístico de candidatos (palabras vacías, longitud, sufijos técnicos). No es el etiquetador OpenNLP real. |

### Módulo 4 · Decisión y datos (`data/`)
| Archivo | Qué hace |
|---|---|
| `decision/DecisionEvaluator.kt` | **Corazón del sistema.** Las dos pasadas, el cálculo de S, la elección del candidato y la regla `y* ≥ 0.65`. |
| `decision/LessonContext.kt` | Memoria temática de la clase (pesos por cluster con decaimiento). |
| `local/entities/AcademicTerm.kt` | Término del diccionario (`term`, `lemma`, `subject`, `category`, `phonetic`). |
| `local/entities/TopicCluster.kt` | Grupo temático (`topic_id`, `topic_name`, `terms`). |
| `local/entities/SessionLogEntity.kt` | Una fila de telemetría por palabra: C, D, S, y*, cluster, latencia, ¿sustituida? |
| `local/RoomDatabase.kt`, `dao/*` | SQLite (Room). Versión 2, **migración destructiva** (cambiar el esquema borra los datos). |
| `telemetry/SessionLogManager.kt` | Registro en memoria + Room, y cálculo formal de WER/KER (usado en tests y benchmark). |

### Módulo 5 · Presentación (`presentation/`)
| Archivo | Qué hace |
|---|---|
| `viewmodel/CaptionsViewModel.kt` | Orquesta todo. Procesa los resultados del ASR **en orden y de a uno** (bloqueo `processingLock`). |
| `TeleprompterScreen.kt` | Pantalla principal: 2-3 líneas, área fija Ciencia y Tecnología (sin selector), insignia de fuente de audio, exportación, panel de pruebas con inyección manual de frases. |
| `CorrectionsHistoryScreen.kt` | Historial: palabra original → corregida, con C, D, S, y* y tema. |
| `SUSQuestionnaireScreen.kt` | Cuestionario de usabilidad SUS + 4 ítems complementarios (Anexo A). Solo guarda las respuestas (sin puntaje) → `sus_respuestas.csv`. |
| `NasaTlxScreen.kt` | NASA-TLX Raw (Anexo E): 6 barras 0–100 sin marca inicial; guarda la condición T0/T2 sin mostrarla; solo respuestas, sin puntaje → `nasa_tlx_respuestas.csv`. |
| `NotesExporter.kt` | Exporta la transcripción a `.txt` y `.pdf`. |
| `theme/*` | Tema de alto contraste: amarillo `#FFFF00` sobre negro `#000000` (19.56:1; AAA exige 7:1). |

### Datos (`app/src/main/assets/`)
| Archivo | Contenido |
|---|---|
| `dictionaries/{biologia,quimica,historia,geografia}.json` | 403 / 22 / 16 / 13 términos. |
| `clusters/*_clusters.json` | Biología: 12 clusters, cada uno con un `macro_id` (4 áreas temáticas: biología celular, genética y herencia, fisiología humana, botánica y ecología); Química: 4; Historia: 3; Geografía: 3. Todo término del diccionario está en algún cluster. El `macro_id` es opcional. |
| `fuzzy_rules.fcl` | Definición formal (anexo de la tesis). **Equivale** al motor Kotlin (se verifica, ver §8). |

### Evaluación (`evaluation/`)
| Archivo | Para qué |
|---|---|
| `benchmark_runner.py` | Calcula WER/KER sobre un corpus. Espejo en Python del pipeline (mismas constantes). Flags: `--ablacion`, `--umbral`, `--sin-memoria`, `--sin-rerank`. |
| `corpus_arequipa_test.json` | 8 frases de Biología con errores de ASR (**sintético**). |
| `corpus_clase_continua.json` | 13 frases cortas con cambio de tema (**sintético**). |
| `corpus_control_cotidiano.json` | 12 frases cotidianas correctas: control de falsos positivos (**sintético**). |
| `generar_paridad_mamdani.py` + `paridad_mamdani.json` | Tabla de referencia del motor (4 913 puntos) para la prueba de paridad Kotlin↔Python. |
| `verificar_fcl.py` | Prueba que `fuzzy_rules.fcl` produce los mismos valores que el motor. |
| `ejecutor_pruebas_calidad.py` | Casos SQA automatizables (dominio, contraste, latencia, lógica de fallback). |

---

## 3. Recorrido de una oración

Entrada del ASR (confianza 0.5 en todas las palabras): **"la sedula tiene nucleo y citoplasma"**

**Pasada 1 – ¿qué se parece a qué?** Para cada palabra que `OpenNlpPosTagger` acepta como candidata, se calcula D contra los 648 términos y se guardan los 3 mejores:

| Palabra | Mejor término (D) | Otros candidatos cercanos |
|---|---|---|
| sedula | médula (0.83) | **célula (0.73)** |
| nucleo | núcleo (1.00) | – |
| citoplasma | citoplasma (1.00) | – |

Los términos con D ≥ 0.80 forman el **contexto de la oración**: {médula, núcleo, citoplasma}.

**Pasada 2 – decidir.** Para cada candidato se calcula S y se evalúa el motor Mamdani:

| Candidato para "sedula" | D | S | y* |
|---|---|---|---|
| médula | 0.83 | 0.00 (no comparte cluster con núcleo/citoplasma) | 0.47 |
| **célula** | 0.73 | 0.67 | **0.77** |

Gana el de mayor y* (célula) y como **0.77 ≥ 0.65** (y D = 0.73 ≥ 0.70, y "sedula" no es plural de "célula"), la palabra se sustituye. Con el criterio anterior (solo mayor D) habría ganado "médula" y se habría descartado por y* = 0.47: la palabra quedaba sin corregir.

**Tres condiciones para sustituir** (todas deben cumplirse): `y* ≥ 0.65`, `D ≥ 0.70` y que la palabra no sea el plural/singular regular del término (`isInflectionOf`).

**Después.** Si el resultado era final, el contexto ({núcleo, citoplasma, célula…}) se anota en la memoria de la clase y se registra la telemetría.

---

## 4. Cómo se calcula cada variable

### C – Confianza acústica
`SpeechRecognizer` entrega **una confianza por hipótesis**, no por palabra; todas las palabras de una frase comparten el valor. Si el motor no la entrega (o devuelve -1/0), se usa **0.65**. Los parciales llevan 0.60 fija.

### D – Similitud
`D = 0.6 · (1 − lev(a,b)/máx) + 0.4 · (1 − lev(fon(a),fon(b))/máx)`, sin tildes ni mayúsculas. Solo compiten palabras con **D ≥ 0.50**.

### S – Coherencia semántica
Se combinan dos evidencias:

1. **De la oración:** fracción de los *otros* términos sólidos (D ≥ 0.80) de la oración que están en el mismo cluster que el candidato (se toma el mejor cluster). Un término de **otro cluster de la misma área temática** cuenta la mitad (`MACRO_CREDIT = 0.5`), porque los clusters son finos y una oración real mezcla subtemas del mismo bloque ("célula" y "mitocondria").
2. **De la clase (`LessonContext`):** afinidad del candidato con el tema vigente (peso del cluster relativo al dominante, con el mismo crédito por área), con decaimiento exponencial. **Solo aprende de términos resueltos** (sustituidos o ya bien escritos): un candidato rechazado no entra a la memoria, para que un error no se refuerce a sí mismo.

| Situación | S |
|---|---|
| Candidato sin cluster | 0.20 |
| Sin evidencia de oración ni de clase | 0.50 |
| Solo clase | `0.50 + (L − 0.50)·ρ` |
| Solo oración | fracción de la oración |
| Ambas | `(1 − 0.4ρ)·S_oración + 0.4ρ·L` |

`L` = afinidad (mínimo 0.20), `ρ` = fuerza de la memoria (0 a 1; 1 con ≈3 términos sólidos recientes).

### y* – Justificación (salida)
Mamdani con 3 términos por variable (Baja/Media/Alta) → 27 reglas, p. ej.:
`SI C es Baja Y D es Alta Y S es Alta ENTONCES y* es Alta`.
Con **C Alta** (el ASR está seguro) la salida máxima es "Media", por lo que **nunca se sustituye una palabra en la que el ASR confía**: es una propiedad deliberada del diseño.

---

## 5. Parámetros que puedes ajustar

| Parámetro | Valor | Dónde | Efecto |
|---|---|---|---|
| Umbral de decisión y* | 0.65 | `DecisionEvaluator.decisionThreshold` / `DECISION_THRESHOLD` (Python) | Más bajo = más correcciones y más riesgo de falsos positivos. Barrido: `--umbral` (resultados idénticos entre 0.60 y 0.75; con ≤ 0.55 aparece un falso positivo). |
| Similitud mínima de candidato | 0.50 | `CANDIDATE_MIN_SIMILARITY` | Bajo ese valor la palabra no se toca. |
| Umbral de contexto | 0.80 | `contextMatchThreshold` | Qué coincidencias cuentan como "término de contexto" para S. |
| Candidatos evaluados | 3 | `MAX_CANDIDATES` | Costo máximo: 3 inferencias por palabra. |
| Ventana de candidatos | 0.12 | `CANDIDATE_WINDOW` | Distancia máxima de D respecto al mejor. |
| Piso de sustitución | 0.70 | `SUBSTITUTION_MIN_SIMILARITY` | D mínima para sustituir, decida lo que decida el motor. Evita que un tema vigente "robe" palabras comunes. |
| Piso de D del alternativo | 0.70 | `RERANK_MIN_SIMILARITY` | Evita que un término del tema "robe" palabras comunes. |
| Peso máx. de la memoria | 0.40 | `LESSON_WEIGHT` | Cuánto pesa la clase frente a la oración. |
| Crédito por área temática | 0.5 | `MACRO_CREDIT` (Kotlin y Python; también en `LessonContext`) | Cuánto vale un término del mismo bloque temático pero de otro cluster. |
| Vida media de la memoria | **45 s** | `LessonContext.DEFAULT_HALF_LIFE_MS` (y `LESSON_HALF_LIFE_S` en Python) | Resultados idénticos entre 20 y 90 s en los corpus; 120 s ya empeora. Aun así, calibrar con voz real. |
| Confianza neutra | 0.65 | `AndroidSpeechEngine.NEUTRAL_CONFIDENCE` | Cuando el ASR no entrega confianza. |
| Mezcla D | 60 % / 40 % | `LevenshteinMatcher.calculateCombinedSimilarity` | Ortografía / fonética. |

> **Regla de oro:** si cambias una constante en Kotlin, cámbiala también en `benchmark_runner.py` y regenera `paridad_mamdani.json` si tocaste el motor; si no, la ablación dejará de describir a la app.

---

## 6. Tiempo real y concurrencia

- **Cola secuencial.** Un único colector (`observeSpeechEngine`) consume los resultados del ASR en orden. Un bloqueo (`processingLock`) protege `committedLines`, `activePartialLine` y los flujos de la UI. Antes, cada resultado lanzaba su propia corrutina y un parcial lento podía pisar a una oración final.
- **Parciales vs. finales.** Un parcial se corrige y se muestra, pero **no** se registra en telemetría ni alimenta la memoria de clase. Solo las finales hacen ambas cosas (evita duplicar correcciones).
- **Costos medidos (JVM de escritorio):** consulta de memoria ≈ 1 µs; oración de 14 palabras contra 403 términos ≈ 7 ms. En teléfono será más lento: se mide en el caso PERF-02 del plan de pruebas.
- **Índice de clusters** precalculado y cacheado por referencia (`ClusterIndex`); la memoria usa un `HashMap` de 12 entradas.
- **Reinicio del reconocedor.** Android corta cada escucha; el motor reinicia a los 150 ms (tras resultado) o 350 ms (tras error). Esos instantes pueden perder palabras: se mide en ASR-03.

---

## 7. Datos: cómo agregar vocabulario

1. Agrega el objeto en `dictionaries/<materia>.json`:
   `{"term":"lisosoma","lemma":"lisosoma","subject":"BIOLOGIA","category":"NOUN","phonetic":"lisosoma"}`
2. Agrega el término a **al menos un** cluster de `clusters/<materia>_clusters.json`. Un término sin cluster recibe S = 0.20 y casi nunca se corrige. `RealAssetsPipelineTest` y `TC-DOM-01` fallan si hay huérfanos.
3. **Room guarda una copia del diccionario**: se carga desde `assets` solo cuando la tabla está vacía. Para ver cambios en un teléfono ya usado, desinstala la app o borra sus datos. Los clusters sí se leen del asset en cada cambio de materia.

---

## 8. Pruebas: qué demuestra cada una

Ejecutar todo:
```bash
./gradlew testDebugUnitTest assembleDebug   # 68 pruebas
cd evaluation
python3 benchmark_runner.py            # WER/KER + reporte
python3 benchmark_runner.py --ablacion # variantes de S y rerank
python3 benchmark_runner.py --umbral   # sensibilidad de y* (incluye control de falsos positivos)
python3 benchmark_runner.py --booleana # difuso vs. mejor regla booleana
python3 verificar_fcl.py               # FCL ≡ motor de la app
python3 ejecutor_pruebas_calidad.py    # SQA automatizable
```

| Test (Kotlin) | Garantiza |
|---|---|
| `FuzzyEngineTest` | Reglas e inferencia del motor. |
| `MamdaniParityTest` | El motor Kotlin da lo mismo que el de Python en 4 913 puntos; y* no cae a 0 en los bordes C/D/S = 1.0. |
| `LevenshteinMatcherTest` | Distancia y fonética. |
| `WerCalculatorTest` | WER y KER formales. |
| `DecisionEvaluatorTest` | Pipeline de dos pasadas con diccionarios mínimos, en 4 asignaturas. |
| `RealAssetsPipelineTest` | Con el diccionario real (648 términos): corrige "sitoplasma/riposoma/nuleolo"; no toca palabras cotidianas. |
| `LessonContextTest` | Memoria: acumula, decae, olvida, se reinicia; presupuesto de tiempo. |
| `LessonMemoryPipelineTest` | La clase influye en S; los parciales no escriben en la memoria; nunca reemplaza palabras comunes. |
| `CandidateRerankTest` (+ áreas y memoria) | Con áreas temáticas "la cedula y la mitocondria" → "la célula y la mitocondria" (sin áreas no se corrige); un candidato rechazado no alimenta la memoria; hablar de organelos prepara "división celular". Además: "sedula" → célula en contexto celular, médula en contexto nervioso; con un tema activo no se secuestran "produce"/"duplica"; no se pasa de plural a singular; "meyosis" sigue siendo meiosis aunque la clase sea de mitosis. |
| `ControlCorpusTest` | 0 sustituciones en lenguaje cotidiano correcto (12 frases, confianzas variadas). |
| `SpeechConfidenceTest` | Confianza -1/0/ausente → neutra. |
| `ExternalAudioDeviceManagerTest` | Prioridad USB > 3.5 mm > interno; fallback al desconectar. |

**Lo que no cubren (requiere hardware):** enrutamiento real del SX26, modo avión, confianza real del ASR, tablet, sesión de 90 min. Ver `plan_de_pruebas_sx26_telefono_tablet.md`.

---

## 9. Limitaciones y deuda técnica (decláralas tú antes de que las encuentren)

0. **Los resultados son "dentro de la muestra".** Varias mejoras (áreas temáticas, memoria de términos resueltos, salvaguardas) se decidieron tras ver fallos en los mismos corpus donde se miden. Frente a la mejor regla booleana (`--booleana`, también ajustada sobre esos corpus): con confianza neutra 0.65 el sistema difuso gana en ambos corpus; con confianzas sintéticas hay empate técnico. No es evidencia de generalización.
0b. **Los `macro_id` de los clusters son una propuesta del desarrollador** y deben ser validados por un docente de Biología.
1. **Corpus sintético y pequeño.** 8 + 13 + 12 frases de Biología con errores de ASR *inventados* y confianzas asignadas a mano. No hay grabaciones reales con el SX26.
2. **La confianza C es por frase, no por palabra,** y algunos equipos no la entregan (entonces C vale 0.65 constante y el sistema depende de D y S).
3. **Vida media de 45 s elegida sobre 21 frases sintéticas.** Con valores más largos se retrasa la adaptación al cambio de tema (ver ablación). Debe calibrarse con voz real.
4. **El contexto puede contaminarse:** el conjunto de términos de contexto se arma con el mejor candidato por D; un candidato equivocado ("sedula"→médula) cuenta como contexto para las otras palabras.
5. **`fuzzy_rules.fcl` no se ejecuta en la app;** es la especificación formal. Un script (`verificar_fcl.py`) demuestra que equivale al motor Kotlin, pero no se usa la librería jFuzzyLogic.
6. **`OpenNlpPosTagger` es un filtro heurístico,** no OpenNLP.
7. **La app no mide WER/KER en uso real** (no hay texto de referencia). Esas métricas existen solo en el benchmark. La interfaz muestra la coherencia semántica media, no una "precisión".
8. **Room con migración destructiva** y duplicación de `ExternalAudioDeviceManager` (uno en el servicio, otro en el ViewModel).
9. **`SpeechRecognizer` no permite elegir micrófono:** depende del enrutamiento de Android. Si no usa el USB, habría que capturar con `AudioRecord.setPreferredDevice` (no implementado).
10. **Biología es la única asignatura con diccionario sustancial;** las demás tienen 13 a 22 términos.

---

## 10. Cómo extender

| Quiero… | Toca… |
|---|---|
| Otra asignatura | Crear `dictionaries/x.json` + `clusters/x_clusters.json`; agregar el nombre a `subjects` en `TeleprompterScreen.kt`. |
| Cambiar el umbral | `decisionThreshold` (Kotlin) y `DECISION_THRESHOLD` (Python); validar con `--umbral` y el corpus de control. |
| Cambiar las reglas | Editar `JFuzzyEngine.kt` **y** `fuzzy_rules.fcl` **y** `benchmark_runner.py`; correr `generar_paridad_mamdani.py`, los tests y `verificar_fcl.py`. |
| Medir una variante de S | Agregar una bandera en `run_benchmarks` y una fila en `run_ablation`. |
| Desactivar la memoria / el rerank | `DecisionEvaluator(lessonContext = null)` / `DecisionEvaluator(rerankCandidates = false)`. |
