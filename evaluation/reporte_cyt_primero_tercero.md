# Diccionario de CyT ampliado a 1.º, 2.º y 3.º de secundaria (18/11/2026)

## Qué se hizo

`scripts/agregar_cyt_primero_tercero.py` agrega **324 términos** (650 → 974) y **18 áreas temáticas** (21 → 39) del
área Ciencia y Tecnología del CNEB para el VI ciclo y el inicio del VII. Antes no había **ningún término de física**
ni de Tierra y universo.

| Área (macro_id) | Clusters nuevos |
|---|---|
| `indagacion_y_tecnologia` (nueva) | indagación científica, laboratorio y medición |
| `quimica_inorganica` | materia: propiedades, estados y cambios; modelos atómicos |
| `fisica` (nueva) | cinemática; fuerzas y leyes de Newton; trabajo, energía y potencia; calor y temperatura; electricidad y magnetismo; ondas, sonido y luz; presión e hidrostática |
| `tierra_y_universo` (nueva) | universo y sistema solar; estructura y dinámica de la Tierra (sismos, huaico, friaje) |
| `botanica_y_ecologia` | niveles de organización y clasificación de los seres vivos; ambiente, cambio climático y sostenibilidad |
| `fisiologia_humana` | alimentación, nutrición y salud; enfermedades infecciosas y prevención; órganos de los sentidos |
| `genetica_y_herencia` | reproducción humana y adolescencia |

También se sumaron términos a 5 clusters existentes (digestión, sangre, depredador, helecho, nutriente…).
68 de los términos nuevos no están en el vocabulario de Vosk (`biologia_vosk_oov.json`: 336 → 403); para esos la
capa difusa es la única forma de que aparezcan bien escritos.

## Filtros (por qué faltan algunos términos del currículo)

1. **Una palabra, al menos 5 letras** (fuera masa, peso, onda, imán, roca, lava, raíz).
2. **Vecinos comunes** (`scripts/revisar_vecinos_terminos.py --modulo agregar_cyt_primero_tercero`, lista
   FrequencyWords es_50k): de 380 candidatos, 246 tienen alguna palabra común con D ≥ 0.70. Se excluyeron los
   **50** que tienen un vecino entre las 5000 palabras más frecuentes con D ≥ 0.80: fuerza~fuera, planeta~planta,
   calor~color, presión~prisión, sismo~mismo, materia~material, conservación~conversación, etc. Vosk ya escribe
   bien esas palabras (están en su vocabulario); como destino de corrección atraerían palabras comunes.
3. **Medición** (abajo): 7 términos más se excluyeron por regresiones concretas (dalton, organismo, mecánica,
   meteoro, meteorito, decibel, eyaculación). Se agregaron cóncava/convexa para que «una cóncava» no pase a «cóncavo».
4. **Regla de no repetir**: un término que ya tenía cluster (célula, bacteria, proteína, núcleo…) no se repite en
   los clusters nuevos. En la primera versión repetirlos repartía su peso entre clusters (1/k) y se perdían
   correcciones de biología (daltonismo, procariotas, eutrofización) y de química (meteorización).

Cada exclusión está en `EXCLUIDOS` del script con su motivo.

## Medición (capa difusa Python = espejo de la app, `python3 evaluar_quimica_ciclos.py --detalle`)

Corpus nuevo: `corpus_vosk_tts_cyt_prueba.json` (149 frases de clase × 3 voces Piper = 447) y
`corpus_vosk_tts_cyt_control.json` (82 frases correctas con los vecinos de riesgo × 3 = 246), generados con
`vosk_fase0/generar_corpus_tts_cyt.py` desde `vosk_fase0/frases_cyt_primero_tercero_tts.json`.

**C real de Vosk** (el motor del piloto). WER después de la capa difusa; Verd./Falsas = correcciones.

| Corpus | Antes (650) WER · V/F | Después (974) WER · V/F |
|---|---|---|
| CyT 1.º-3.º, clase | 15.67 % · 1 / 6 | **14.56 % · 26 / 12** |
| CyT 1.º-3.º, control | 8.13 % · 0 / 1 | 8.13 % · 0 / 1 |
| Química, clase | 22.42 % · 52 / 3 | 22.47 % · 51 / 3 |
| Química, control | 10.32 % · 2 / 0 | 10.32 % · 2 / 0 |
| Biología, desarrollo | 28.07 % · 47 / 7 | 28.01 % · 48 / 6 |
| Biología, prueba | 31.56 % · 30 / 4 | 31.36 % · 31 / 4 |
| Control OpenSLR 73 (cotidiano) | 7.31 % · 0 / 0 | 7.31 % · 0 / 0 |
| Control clase correcta, C alta | 0.00 % · 0 / 0 | 0.00 % · 0 / 0 |

(WER crudo: CyT clase 15.60 %, CyT control 8.07 %. El KER no es comparable entre columnas: depende de la lista de
términos clave, que cambió.)

Comparación frase por frase con C de Vosk:
- **CyT clase**: 34 frases mejoran, 0 empeoran, 6 cambian sin mejorar ni empeorar. Las 12 «falsas» son casi todas
  aciertos parciales que el conteo por palabra exacta no reconoce: «dinamo metros» → dinamómetros (era singular),
  «de cibeles» → decibelios (decía decibeles), «estratos fieras» → estratosferas. Una es un error real:
  «destrucción» → menstruación en la frase del estrógeno.
- **Fuera de CyT** (≈ 1000 frases): 2 mejoran (lisosoma, infecciones) y 1 empeora: «jónico» ya no pasa a «iónico»
  (química #67); no hay un término culpable: es el efecto conjunto de ~40 términos sobre la memoria de clase.
- **Controles**: ninguna falsa corrección nueva.

**C fija 0.65** (peor caso: motor de Android, que no da confianza): aparecen falsas correcciones nuevas en los
controles: «su turno» → saturno, «ficción» → fricción, «empuja» → empuje, «votación» → rotación, «a varios» →
ovarios (CyT control 15 → 25; OpenSLR 18 → 20). Con la C de Vosk no ocurren. Si alguna vez se usa el motor de
Android en clase de física, conviene revisar fricción y empuje.

## Cambio de código que salió de la medición

`DecisionEvaluator.pluralOf` (y su espejo `benchmark_runner.plural_of`) escribía «infecciónes», «catiónes»,
«reacciónes»: ahora una aguda con tilde la pierde en el plural (infección → infecciones). Prueba:
`SplitTermTest.pluralOfAcuteTerm_dropsTheAccent`. Afecta también a términos que ya estaban (reacción, mutación…).

## Verificación

- `./gradlew testDebugUnitTest`: 100 de 100 OK, incluida la paridad Kotlin↔Python en 1469 frases
  (`generar_paridad_decision.py` ahora incluye los dos corpus CyT).
- El script es idempotente (una segunda ejecución no cambia nada).

## Pendiente

- Validar las 18 áreas nuevas con un docente de CyT (la agrupación es propuesta del desarrollador).
- Las frases son de voz sintética; confirmar con grabaciones reales de clase de física (`evaluar_sesiones.py`).
- Respaldo del diccionario anterior (650 términos, 21 clusters): `evaluation/respaldo_diccionario_650/`.
