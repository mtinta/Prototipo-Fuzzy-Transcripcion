# PLAN MAESTRO DE SUBSANACIÓN DE OBSERVACIONES DE TESIS
## Título: *Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De Lógica Difusa Para Transcripción Automática Como Apoyo A Estudiantes Sordos En Educación Secundaria*
**Tesista:** Bach. Miguel Angel Tinta Aguilar  
**Asesor:** Dr. Karim Guevara Puente de la Vega  
**Institución:** Universidad Nacional de San Agustín de Arequipa (UNSA) — Escuela Profesional de Ingeniería de Sistemas  
**Documento Fuente:** `Tesis_de_Investigacion_Tinta_Aguilar_Miguel_Angel.docx`  

---

## ÍNDICE DEL PLAN DE SUBSANACIÓN

1. [Enfoque Estratégico y Matriz de Trazabilidad](#1-enfoque-estratégico-y-matriz-de-trazabilidad)
2. [Subsanación del Capítulo I: Planteamiento del Problema (Obs. 1.1 a 1.14)](#2-subsanación-del-capítulo-i-planteamiento-del-problema)
   - [1.1 Descripción de la realidad problemática](#item-11-descripción-de-la-realidad-problemática)
   - [1.2 Problema principal y precisión semántica](#item-12-problema-principal)
   - [1.3.1 Objetivo general](#item-131-objetivo-general)
   - [1.3.2 Objetivos específicos](#item-132-objetivos-específicos)
   - [1.4 Hipótesis de la investigación](#item-14-hipótesis-de-la-investigación)
   - [1.5.1 Variable independiente](#item-151-variable-independiente)
   - [1.5.2 Variable dependiente](#item-152-variable-dependiente)
   - [1.6.1 Viabilidad técnica](#item-161-viabilidad-técnica)
   - [1.6.2 Viabilidad operativa](#item-162-viabilidad-operativa)
   - [1.6.3 Viabilidad económica](#item-163-viabilidad-económica)
   - [1.7 Justificación](#item-17-justificación)
   - [1.8 Importancia](#item-18-importancia)
   - [1.9 Alcance](#item-19-alcance)
   - [1.10 Tipo, línea y nivel de investigación](#item-110-tipo-línea-y-nivel-de-investigación)
   - [1.11 Diseño de la investigación](#item-111-diseño-de-la-investigación)
   - [1.12 Técnicas de recolección](#item-112-técnicas)
   - [1.13 Instrumentos de recolección](#item-113-instrumentos)
   - [1.14 Cronograma](#item-114-cronograma)
3. [Subsanación del Capítulo II: Marco Teórico (Obs. 2.1 a 2.3)](#3-subsanación-del-capítulo-ii-marco-teórico)
   - [2.1 Antecedentes de investigación](#item-21-antecedentes-de-investigación)
   - [2.2 Estado del arte](#item-22-estado-del-arte)
   - [2.3 Marco conceptual](#item-23-marco-conceptual)
4. [Subsanación del Capítulo III: Desarrollo de la Propuesta (Obs. 3.1 a 3.2)](#4-subsanación-del-capítulo-iii-desarrollo-de-la-propuesta)
   - [3.1 Generalidades y diseño](#item-31-generalidades--diseño)
   - [3.2 Esquema de la propuesta y diagramas de flujo](#item-32-esquema-de-la-propuesta)
5. [Guía de Aplicación en el Archivo `.docx`](#5-guía-de-aplicación-en-el-archivo-docx)
6. [Tabla Maestra de Fuentes y Referencias Bibliográficas (APA 7)](#6-tabla-maestra-de-fuentes-y-referencias-bibliográficas-apa-7)
7. [Anexo A. Caracterización cuantitativa de estudiantes sordos e hipoacúsicos en Arequipa](#7-anexo-a-caracterización-cuantitativa-de-estudiantes-sordos-e-hipoacúsicos-en-la-provincia-de-arequipa)

---

## 1. ENFOQUE ESTRATÉGICO Y MATRIZ DE TRAZABILIDAD

El dictamen del jurado evaluador identifica 23 observaciones distribuidas en los Capítulos I, II y III. Dichas observaciones no cuestionan la validez de la propuesta tecnológica, sino que exigen **rigor metodológico, precisión terminológica y coherencia interna**:
- **Consistencia Matemática:** Distinguir Tasa de Error de Palabra ($\text{WER} \le 10\%$), Reducción Relativa del Error ($RR \approx 68\%$) y Exactitud ($\text{Precisión} \ge 90\%$).
- **Contextualización Local en Arequipa:** Reemplazar datos genéricos internacionales por la realidad de colegios de UGEL Norte y Sur, equipos SAANEE y la acústica real de aulas peruanas.
- **Evidencia Cuantitativa Local:** Sustentar el contexto con el conjunto de datos abiertos del MINEDU *Número de estudiantes con discapacidad de EBR 2025* (Censo Educativo 2025), filtrado a la provincia de Arequipa (29 distritos, UGEL Arequipa Norte, Arequipa Sur y La Joya). Ver Anexo A (Sección 7).
- **Operacionalización Estricta:** Separar la evaluación algorítmica de precisión textual (WER/CER) de la usabilidad (SUS), retirando afirmaciones no medidas instrumentalmente sobre "fatiga cognitiva".
- **Garantía On-Device:** Demostrar cómo opera el ASR offline de Android (`es-PE`) y Room DB sin servidores externos.

---

## 2. SUBSANACIÓN DEL CAPÍTULO I: PLANTEAMIENTO DEL PROBLEMA

---

### <a id="item-11-descripción-de-la-realidad-problemática"></a>1.1. Descripción de la Realidad Problemática

#### A. Diagnóstico de la Observación
El jurado señala que la realidad problemática se apoya en literatura internacional (EE.UU., Europa) sobre intérpretes y CART, sin aterrizar en la realidad de la provincia de Arequipa. Falta caracterizar las condiciones acústicas reales de las aulas y la cobertura del Servicio de Apoyo y Asesoramiento a las Necesidades Educativas Especiales (SAANEE).

#### B. Plan de Subsanación
1. **Incorporar Datos Estadísticos Locales (Provincia de Arequipa) — Censo Educativo 2025 (MINEDU):**
   - El conjunto de datos abiertos del MINEDU registra, en la provincia de Arequipa, **775 servicios educativos EBR** con matrícula de estudiantes con discapacidad, que suman **2,544 estudiantes**. De ellos, **85 presentan discapacidad auditiva** (51 con sordera y 34 con hipoacusia; 3.3% del total), distribuidos en **68 servicios educativos**. No se registran casos de sordoceguera.
   - En **educación secundaria** (población objetivo de la tesis) hay **35 estudiantes con discapacidad auditiva** (14 con sordera y 21 con hipoacusia) en **30 de las 215 instituciones de secundaria** con matrícula inclusiva: aproximadamente **1 a 3 estudiantes por institución**, es decir, **dispersión alta y no concentración**. Por UGEL: Arequipa Norte 12 estudiantes (12 IE), Arequipa Sur 17 (14 IE) y La Joya 6 (4 IE).
   - Distritos con mayor presencia en secundaria: Cerro Colorado (6), Arequipa (4), Alto Selva Alegre (5, contando sordera e hipoacusia), Paucarpata (3) y José Luis Bustamante y Rivero (3). Ver tablas del Anexo A.
   - **Corrección obligatoria al borrador previo:** la cifra ">180 estudiantes sordos" y la mención de la GUE Mariano Melgar y la I.E. Honorio Delgado como colegios con estudiantes sordos **no están respaldadas por el dataset**: ambos colegios reportan 0 estudiantes con discapacidad auditiva en secundaria, y la I.E. Independencia Americana reporta 1 con hipoacusia. Debe usarse la cifra de 85 (35 en secundaria) o contrastarse con otra fuente oficial (ESCALE, SIAGIE, registro SAANEE).
   - Las afirmaciones "1 profesional itinerante SAANEE por colegio" y "sin mediación comunicativa en más del 80% de las horas lectivas" **no provienen del dataset**: deben respaldarse con un oficio a la UGEL/SAANEE o redactarse como hipótesis de contexto, no como dato.
2. **Caracterización Acústica del Aula Típica en Arequipa:**
   - Construcciones de concreto armado, pisos de loseta o cemento pulido y amplios ventanales sin acondicionamiento acústico generan tiempos de reverberación elevados ($RT_{60} > 1.2\text{ s}$).
   - Niveles de ruido de fondo medidos en colegios urbanos de Arequipa oscilan entre **55 dB SPL y 68 dB SPL** (tráfico vehicular, patio escolar, ruido interno), degradando la relación señal/ruido ($SNR \le 5\text{ dB}$), lo que descalibra los motores ASR estándar.
3. **Contextualización de Costos:**
   - Aclarar que en el Perú el servicio humano CART es prácticamente inexistente en colegios públicos y los intérpretes de Lengua de Señas Peruana (LSP) perciben honorarios que las familias de estratos socioeconómicos C y D de Arequipa no pueden financiar de manera particular.

#### C. Redacción Sugerida para el Documento
> *"En la provincia de Arequipa, la inclusión de estudiantes con discapacidad auditiva en la Educación Básica Regular (EBR) presenta una población reducida y dispersa. Según el Censo Educativo 2025 del Ministerio de Educación ([MINEDU, 2025](https://www.datosabiertos.gob.pe/)), en esta provincia se registran 2,544 estudiantes con alguna discapacidad en EBR, de los cuales 85 presentan discapacidad auditiva (51 con sordera y 34 con hipoacusia). En el nivel secundario se identifican 35 estudiantes (14 con sordera y 21 con hipoacusia) distribuidos en 30 instituciones educativas de las UGEL Arequipa Norte, Arequipa Sur y La Joya, lo que equivale a uno o dos estudiantes por institución. Esta dispersión implica que difícilmente cada colegio cuente con intérpretes de Lengua de Señas Peruana (LSP) o servicios de transcripción humana en tiempo real, por lo que el estudiante depende del equipo itinerante SAANEE y de la lectura labiofacial ([MINEDU, 2021](https://busquedas.elperuano.pe/dispositivo/NL/1951686-1)). A ello se suman las condiciones acústicas de aulas de concreto sin tratamiento fonoabsorbente, con niveles de presión sonora de 55 dB a 68 dB SPL (Torres & Valencia, 2020 ⚠), superiores a los límites de 50 dB del estándar nacional ([Presidencia del Consejo de Ministros [PCM], 2003](https://sinia.minam.gob.pe/sites/default/files/sinia/archivos/public/docs/ds.085.2003.pcm_.pdf)) y a los 35 dB recomendados para entornos educativos ([Acoustical Society of America [ANSI/ASA], 2010](https://webstore.ansi.org/search/find?in=1&st=S12.60)). Bajo estas condiciones, los sistemas comerciales de reconocimiento automático del habla experimentan tasas de degradación superiores al 25% ([Bhardwaj et al., 2022](https://doi.org/10.3390/app12094419)), distorsionando el vocabulario curricular escolar (Biología, Química, Historia) y limitando el acceso oportuno del estudiante con pérdida auditiva al discurso pedagógico del docente."*

---

### <a id="item-12-problema-principal"></a>1.2. Problema Principal y Precisión Semántica

#### A. Diagnóstico de la Observación
El problema plantea mejorar la "precisión semántica", pero la métrica empleada es el WER (Word Error Rate). El jurado exige definir qué se entiende por precisión semántica y cómo se medirá, ya que el WER solo evalúa distancias textuales de edición (Levenshtein a nivel palabra).

#### B. Plan de Subsanación
1. **Identificar con claridad la intervención y el resultado esperado:** la pregunta debe nombrar (a) la intervención, una capa de corrección post-reconocimiento con inferencia difusa Mamdani que combina confianza acústica (C), similitud fonético-léxica (D) y coherencia semántica (S) sobre un diccionario curricular, y (b) el resultado, la reducción del error de transcripción frente al ASR sin corrección. Las metas numéricas (WER ≤ 10%, precisión semántica ≥ 90%) pasan a las hipótesis (1.4) y no a la pregunta.
2. **Definición operacional de precisión semántica:**
   - Se define como la **proporción de términos curriculares clave de la verdad de campo que aparecen correctamente en la transcripción final**. Un error en un artículo (*"el"* por *"un"*) no altera el concepto; un error en un término técnico (*"cédula"* por *"célula"*, *"cloro"* por *"calor"*) sí lo altera.
   - Se declara de forma explícita que es una **definición léxico-conceptual restringida**: mide la conservación de los conceptos que sostienen la clase, no la equivalencia de significado de cada oración.
3. **Tres métricas con roles distintos (el WER por sí solo no demuestra conservación de significado):**
   - **WER** (textual, todas las palabras): $\text{WER} = (S + D + I) / N$ (NIST, 2015).
   - **KER y precisión semántica** (solo términos clave, $N_k$ términos indexados en la verdad de campo): $\text{KER} = (S_k + D_k) / N_k$ y $\text{Precisión Semántica} = 1 - \text{KER}$. Un término clave cuenta como correcto si coincide con la referencia tras normalizar mayúsculas y tildes. Esto evita que un sistema reduzca el WER y falle en lo esencial.
   - **Tasa de falsas correcciones (FCR):** proporción de palabras que el ASR transcribió bien y la capa difusa cambió por error. Sin esta métrica, la capa podría mejorar el KER dañando texto correcto.
4. **Validación de que KER refleja significado (evidencia externa):** sobre una submuestra de enunciados, dos jueces (docente del área y especialista SAANEE) puntúan a ciegas si el significado pedagógico se conserva (3 niveles: conserva, se distorsiona, se pierde). Se reporta la concordancia (κ de Cohen) y la correlación entre el juicio humano y el KER. Si la correlación es baja, la limitación se declara en el documento.
5. **Cálculo de la reducción del error:** $\Delta\text{WER}_{rel} = (\text{WER}_{crudo} - \text{WER}_{difuso}) / \text{WER}_{crudo}$ y $\Delta\text{KER}_{rel}$ análogo, sobre el mismo corpus y condiciones acústicas, contrastados con la prueba de Wilcoxon de rangos pareados (Wilcoxon, 1945). Se reporta junto con la FCR. Fundamento de la relevancia del vocabulario técnico: los errores en términos especializados provocan el abandono del subtitulado (Butler et al., 2019).
6. **Honestidad sobre el alcance de los resultados actuales:** los valores del benchmark sintético (WER 18.45% a 13.59% en el repositorio) no cumplen aún la meta de ≤ 10% y no deben presentarse como logro, sino como evidencia de factibilidad pendiente de voz real de aula.

#### C. Redacción Sugerida de la Pregunta General
> *"¿En qué medida la incorporación de una capa de corrección post-reconocimiento basada en inferencia difusa Mamdani ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)), que integra la confianza acústica, la similitud fonético-léxica y la coherencia semántica sobre un diccionario curricular, reduce el error de transcripción (Tasa de Error de Palabra, WER, según [NIST, 2015](https://github.com/usnistgov/SCTK)) y aumenta la preservación de los términos curriculares clave (precisión semántica = 1 − KER) respecto de la transcripción del reconocedor on-device sin corrección, en clases de educación secundaria de Arequipa?"*

> **Definición operacional para el documento (insertar tras la pregunta):** *"En esta investigación, la precisión semántica se define como la proporción de términos curriculares clave de la transcripción de referencia que se conservan correctamente en la transcripción final (1 − KER, donde KER es la tasa de error de palabras clave). Se trata de una medida léxico-conceptual que complementa al WER: mientras el WER cuantifica las diferencias textuales entre palabras de cualquier tipo, la precisión semántica evalúa solo los términos de los que depende la comprensión del concepto pedagógico. Para controlar que la corrección no degrade el texto acertado, se reporta además la tasa de falsas correcciones y, sobre una submuestra, la concordancia con el juicio de dos evaluadores humanos sobre la conservación del significado."*

---

### <a id="item-131-objetivo-general"></a>1.3.1. Objetivo General

#### A. Diagnóstico de la Observación
El verbo *"Proponer"* es considerado insuficiente y pasivo por el jurado, dado que la tesis incluye el desarrollo completo, codificación y evaluación experimental de un prototipo de software.

#### B. Plan de Subsanación
Reemplazar *"Proponer"* por **"Desarrollar y evaluar"**, explicitando el resultado técnico concreto (capa difusa On-Device) y el entorno de aplicación (aulas de educación secundaria en Arequipa).

#### C. Redacción Sugerida
> **1.3.1. Objetivo General:**  
> *"Desarrollar y evaluar un sistema de transcripción automática en tiempo real asistido por una capa computacional de lógica difusa On-Device ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)), para mejorar la precisión en la captura de vocabulario curricular escolar y reducir la Tasa de Error de Palabra (WER) conforme a los estándares NIST (2015) durante las clases presenciales, como apoyo pedagógico a estudiantes con discapacidad auditiva de educación secundaria en la provincia de Arequipa."*

---

### <a id="item-132-objetivos-específicos"></a>1.3.2. Objetivos Específicos

#### A. Diagnóstico de la Observación
Falta incorporar explícitamente la preparación del corpus y de los diccionarios escolares (CNEB); asimismo, se debe separar la usabilidad (evaluada con SUS) de la fatiga cognitiva, la cual no tiene instrumento psicofisiológico en la tesis.

#### B. Plan de Subsanación
Reestructurar en **5 objetivos específicos ordenados y medibles**:

#### C. Redacción Sugerida
> **1.3.2. Objetivos Específicos:**
> 1. *"Diseñar y estructurar los diccionarios de vocabulario académico basados en el Currículo Nacional de la Educación Básica ([MINEDU, 2016](https://www.minedu.gob.pe/curriculo/)) y compilar un corpus fonético-acústico escolar representativo de las aulas de secundaria de Arequipa para el entrenamiento y evaluación de coincidencia de patrones."*
> 2. *"Configurar y adaptar un motor de reconocimiento automático del habla en modo local offline bajo la plataforma Android ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)), garantizando la extracción continua de las secuencias de texto y sus respectivos vectores de confianza acústica ($C$)."*
> 3. *"Implementar un algoritmo de inferencia difusa Mamdani articulado con similitud fonético-léxica de Levenshtein (Levenshtein, 1966; [Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)), para evaluar la incertidumbre acústica y ejecutar la sustitución silenciosa de términos curriculares deformados por el ruido ambiental."*
> 4. *"Desarrollar una interfaz de usuario reactiva en Jetpack Compose en formato de teleprompter continuo de alto contraste conforme a las directrices de accesibilidad WCAG 2.1 Nivel AAA ([W3C, 2018](https://www.w3.org/TR/WCAG21/#contrast-enhanced)), mitigando el esfuerzo visual del estudiante sordo."*
> 5. *"Evaluar experimentalmente la eficacia del sistema mediante la contrastación del WER y la latencia frente al corpus escolar ([NIST, 2015](https://github.com/usnistgov/SCTK)), y determinar el grado de usabilidad del prototipo a través del cuestionario estandarizado System Usability Scale (Brooke, 1996)."*

---

### <a id="item-14-hipótesis-de-la-investigación"></a>1.4. Hipótesis de la Investigación

#### A. Diagnóstico de la Observación
El texto actual mezcla *"reducción de WER a un mínimo de 90%"* (error conceptual grave: el WER es una tasa de error, por lo que debe ser baja; el 90% corresponde a la exactitud o precisión). Además, mezcla hipótesis de configuración/implementación con hipótesis de desempeño experimental.

#### B. Plan de Subsanación
1. **Clarificación Conceptual:**
   - **Tasa de Error de Palabra final:** $\text{WER} \le 10.0\%$ (NIST, 2015).
   - **Exactitud / Precisión Global:** $\text{Precisión} = (1 - \text{WER}) \ge 90.0\%$.
   - **Reducción Relativa del Error:** $RR_{\text{WER}} = \frac{\text{WER}_{\text{ASR}} - \text{WER}_{\text{Difuso}}}{\text{WER}_{\text{ASR}}} \ge 50.0\%$.
2. **Desacoplar Hipótesis:**
   - La **Hipótesis General** es de **desempeño y eficacia**.
   - Las **Hipótesis Específicas** se dividen en: (a) Hipótesis de precisión acústica/difusa, (b) Hipótesis de eficiencia temporal (latencia $< 1.0\text{ s}$) y (c) Hipótesis de usabilidad técnica (puntaje $\text{SUS} \ge 75$ puntos, rango "Bueno/Aceptable", Brooke, 1996).
   - Se elimina cualquier hipótesis sobre "reducción de fatiga cognitiva", ya que no se administraron electroencefalogramas ni escalas NASA-TLX.

#### C. Redacción Sugerida de las Hipótesis
> **Hipótesis General:**  
> *"La integración de una capa computacional basada en técnicas de lógica difusa Mamdani y coincidencia fonético-léxica sobre el reconocimiento de voz On-Device, permitirá reducir la Tasa de Error de Palabra (WER según estándar [NIST, 2015](https://github.com/usnistgov/SCTK)) a niveles inferiores al 10.0% (alcanzando una precisión de transcripción $\ge 90.0\%$) en la captura de vocabulario curricular en condiciones de aula escolar secundaria de Arequipa."*
>
> **Hipótesis Específica 1 (Precisión Textual):**  
> *"El algoritmo de sustitución difusa reducirá en al menos un 50.0% el error relativo de transcripción ($\text{WER}$) respecto a la línea base del motor ASR comercial operando de forma aislada sin corrección ([NIST, 2015](https://github.com/usnistgov/SCTK))."*
>
> **Hipótesis Específica 2 (Desempeño Temporal):**  
> *"El procesamiento On-Device de la inferencia difusa y la consulta de diccionarios en memoria mantendrá una latencia media de procesamiento inferior a 1000 ms por oración, garantizando el seguimiento en tiempo real de la clase presencial."*
>
> **Hipótesis Específica 3 (Usabilidad):**  
> *"La aplicación móvil de transcripción accesible obtendrá una puntuación media en la Escala de Usabilidad del Sistema (SUS) igual o superior a 75 puntos sobre 100, ubicándose en la categoría de usabilidad 'Buena' (Brooke, 1996)."*

---

### <a id="item-151-variable-independiente"></a>1.5.1. Variable Independiente

#### A. Diagnóstico de la Observación
Falta precisar la condición de comparación experimental (con y sin corrección difusa), definir parámetros exactos y separar los indicadores del algoritmo difuso de las características de la interfaz o la captura de hardware. Además, se debe comprobar la disponibilidad y la granularidad real del puntaje de confianza acústica $C$.

#### B. Plan de Subsanación
1. **Definición Conceptual y condiciones de comparación:**  
   La Variable Independiente es el **"Sistema de Corrección Lingüística Difusa On-Device"**, manipulado en dos tratamientos aplicados **sobre el mismo audio y la misma hipótesis del ASR** (la salida cruda se registra antes de la capa difusa, de modo que ambas condiciones se comparan palabra por palabra):
   - **Tratamiento 0 (Control / Línea Base, sin corrección difusa):** transcripción nativa del `SpeechRecognizer` de Android, sin posprocesamiento.
   - **Tratamiento 1 (Experimental, con corrección difusa):** la misma transcripción procesada por el filtro POS, la similitud $D$, la coherencia semántica $S$ y la inferencia Mamdani, con sustitución condicionada a la regla de decisión del punto 4.

2. **Componentes y flujo de decisión del sistema (por palabra):**
   1. *Entrada:* palabra del ASR $w$ y confianza $C$ (ver punto 5).
   2. *Filtro morfosintáctico:* si $w$ no es candidata a sustantivo (`isCandidateNoun`) o el diccionario está vacío, se conserva $w$.
   3. *Candidatos:* se calcula $D$ contra el diccionario académico y se retienen hasta 3 términos con $D \ge 0.50$.
   4. *Coherencia semántica $S$:* se calcula por co-ocurrencia de términos en los *topic clusters* de la asignatura y la memoria temática de la clase.
   5. *Inferencia:* motor Mamdani de 27 reglas, entradas $(C, D, S)$, operadores MIN (conjunción) y MAX (agregación), defuzzificación por centroide → $y^*$. Se evalúa cada candidato y gana el de mayor $y^*$.
   6. *Decisión:* se sustituye si $y^* \ge 0.65$ **y** $D \ge 0.70$ **y** $w$ no es ya una flexión (singular/plural) del término. En otro caso se conserva la palabra original del ASR.

3. **Parámetros del sistema (valores fijos del prototipo):**

   | Parámetro | Valor | Fuente en el código |
   |---|---|---|
   | Universo de $C$, $D$, $S$, $y^*$ | $[0.0, 1.0]$ | `JFuzzyEngine` |
   | $C$ Baja / Media / Alta | trap(0, 0, 0.35, 0.60) / tri(0.35, 0.60, 0.80) / trap(0.65, 0.85, 1, 1) | `JFuzzyEngine` |
   | $D$ Baja / Media / Alta | trap(0, 0, 0.40, 0.65) / tri(0.45, 0.70, 0.85) / trap(0.75, 0.90, 1, 1) | `JFuzzyEngine` |
   | $S$ Baja / Media / Alta | trap(0, 0, 0.25, 0.45) / tri(0.30, 0.50, 0.70) / trap(0.55, 0.75, 1, 1) | `JFuzzyEngine` |
   | $y^*$ Baja / Media / Alta | trap(0, 0, 0.30, 0.50) / tri(0.35, 0.55, 0.75) / trap(0.60, 0.80, 1, 1) | `JFuzzyEngine` |
   | Reglas | 27 (3×3×3), MIN–MAX | `JFuzzyEngine`, `fuzzy_rules.fcl` |
   | Defuzzificación | Centroide, 100 pasos de discretización | `JFuzzyEngine` |
   | Peso de $D$ | 60 % ortográfica + 40 % fonética hispana | `LevenshteinMatcher` |
   | $D$ mínima de candidato | 0.50 | `CANDIDATE_MIN_SIMILARITY` |
   | Máx. candidatos reevaluados | 3 (ventana de 0.12 respecto al mejor, $D \ge 0.70$) | `MAX_CANDIDATES`, `CANDIDATE_WINDOW`, `RERANK_MIN_SIMILARITY` |
   | $D$ mínima para sustituir | 0.70 | `SUBSTITUTION_MIN_SIMILARITY` |
   | Umbral de decisión $\tau$ | 0.65 ($y^* \ge \tau$) | `decisionThreshold` |
   | $D$ mínima de término de contexto para $S$ | 0.80 | `contextMatchThreshold` |
   | Crédito por misma área temática | 0.5 | `MACRO_CREDIT` |
   | Peso máximo de la memoria de clase | 0.40 | `LESSON_WEIGHT` |
   | $S$ sin clusters / término no indexado / sin evidencia | 0.50 / 0.20 / 0.50 | `calculateSemanticCoherence` |
   | $C$ neutra cuando el ASR no informa confianza | 0.65 | `NEUTRAL_CONFIDENCE` |
   | $C$ en resultados parciales | 0.60 (constante) | `AndroidSpeechEngine.onPartialResults` |

4. **Dimensiones e indicadores de la Variable Independiente (solo algoritmo):**
   - **Dimensión 1: Entradas del razonamiento difuso.** Indicadores: confianza acústica $C \in [0,1]$; similitud fonético-léxica $D \in [0,1]$ (con la distancia de Levenshtein subyacente); coherencia semántica $S \in [0,1]$; grados de membresía $\mu(C), \mu(D), \mu(S)$ en los conjuntos Baja/Media/Alta.
   - **Dimensión 2: Inferencia Mamdani.** Indicador: nivel de activación de cada regla (MIN) y de cada consecuente (MAX).
   - **Dimensión 3: Defuzzificación.** Indicador: grado de justificación $y^* \in [0,1]$ por centroide.
   - **Dimensión 4: Decisión y filtrado.** Indicadores: umbral $\tau = 0.65$ y piso $D \ge 0.70$ (parámetros nítidos); tasa de sustitución $= \dfrac{\text{palabras sustituidas}}{\text{palabras candidatas evaluadas}}$.

5. **Separación de la captura de audio y de la interfaz (no son indicadores de la variable independiente):**
   - *Captura de audio* (micrófono USB/inalámbrico, estabilidad del flujo, perfil ASR: reconocimiento local o sin conexión es-US, en línea es-419): se trata como **condición controlada** y se reporta, no se manipula. El mismo audio alimenta ambos tratamientos.
   - *Interfaz* (teleprompter de 2 a 3 líneas, desplazamiento): se evalúa como **usabilidad** en la variable dependiente VD 3 (SUS, ver 1.5.2); no mide el algoritmo.

6. **Disponibilidad y granularidad de la confianza acústica ($C$):**  
   - **Lo que entrega la API:** `SpeechRecognizer.CONFIDENCE_SCORES` es un arreglo con **un valor por hipótesis** (no por palabra), con valores en $[0.0, 1.0]$ o $-1$ si no está disponible (Google Developers, 2024).
   - **Lo que implementa el prototipo:** en `onResults` se toma el primer valor del arreglo y se **replica a todas las palabras de la frase**; por tanto, $C$ es una confianza **a nivel de frase**. Si el valor falta, es $\le 0$ o es NaN, se usa $C = 0.65$ constante; en resultados parciales, $C = 0.60$.
   - **Consecuencia metodológica:** $C$ no discrimina entre palabras de una misma frase, y si el equipo no informa confianza, $C$ es constante y el efecto de la capa difusa depende de $D$ y $S$.
   - **Comprobación empírica pendiente:** prueba ASR-02 del plan de pruebas SX26 (registrar con logcat el `CONFIDENCE_SCORES` crudo de al menos 20 frases en la tableta real y reportar cuántas devuelven valor válido, su rango y su variabilidad). Los resultados se incorporarán a esta sección.
   - **Alternativa de trabajo futuro:** confianza por palabra mediante `RecognitionPart` (Android 14 o superior), en niveles discretos; no implementada ni verificada en el prototipo.

#### C. Redacción Sugerida para el Documento
> *"**1.5.1. Variable Independiente: Sistema de Corrección Lingüística Difusa On-Device**  
> Se define conceptualmente como el artefacto algorítmico compuesto por el etiquetado morfosintáctico, la similitud fonético-léxica basada en la distancia de edición (Levenshtein, 1966; RAE & ASALE, 2011), la coherencia semántica por *topic clusters* y el motor de inferencia difusa de Mamdani con operadores MIN–MAX ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)), ejecutado íntegramente en el hardware local del cliente Android sin mediación de servidores externos ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)).  
> Operacionalmente, la variable se manipula en dos condiciones experimentales aplicadas sobre el mismo audio y la misma hipótesis del reconocedor:  
> 1. **Condición de control (sin corrección difusa):** transcripción cruda del reconocedor de voz de Android.  
> 2. **Condición experimental (con corrección difusa):** la misma transcripción intervenida por la capa difusa, que sustituye una palabra por un término del diccionario académico solo si la justificación defuzzificada cumple $y^* \ge 0.65$, la similitud cumple $D \ge 0.70$ y la palabra no es ya una flexión del término; en caso contrario se conserva la palabra original.  
> El sistema recibe tres entradas, todas en $[0,1]$: la confianza acústica $C$ reportada por la API del reconocedor, la similitud fonético-ortográfica $D$ con el diccionario (60 % ortográfica, 40 % fonética hispana) y la coherencia semántica $S$ con el tema de la clase. Estas se fuzzifican en conjuntos Baja, Media y Alta (funciones trapezoidales y triangulares), se evalúan con 27 reglas y se defuzzifican por centroide para obtener $y^* \in [0,1]$ ([Zadeh, 1965](https://doi.org/10.1016/S0019-9958(65)90241-X); [Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)).  
> Cabe precisar que la API entrega la confianza por hipótesis y no por palabra; el prototipo la aplica a nivel de frase y asigna un valor neutro de 0.65 cuando el reconocedor no la informa. Las condiciones de captura de audio y las características de la interfaz de subtitulado no forman parte de esta variable: las primeras se mantienen controladas y la segunda se evalúa mediante la variable dependiente de usabilidad."*

---

### <a id="item-152-variable-dependiente"></a>1.5.2. Variable Dependiente

#### A. Diagnóstico de la Observación
La variable dependiente agrupa métricas heterogéneas sin orden. Se debe separar con claridad: (1) Precisión Textual, (2) Desempeño Temporal, y (3) Usabilidad. Debe eliminarse la pretensión de medir fatiga cognitiva o carga mental a través del SUS.

#### B. Plan de Subsanación
Reorganizar la Variable Dependiente en **3 variables dependientes formalmente operacionalizadas**:

| Variable Dependiente | Dimensión | Indicador / Métrica | Instrumento / Fórmula | Escala / Rango |
|---|---|---|---|---|
| **VD 1: Precisión de la Transcripción** | Precisión a nivel de palabra | Word Error Rate ($\text{WER}$) | $\text{WER} = \frac{S + D + I}{N}$ (NIST standard) | Porcentaje $[0\%, 100\%]$ |
| | Precisión a nivel de carácter | Character Error Rate ($\text{CER}$) | $\text{CER} = \frac{S_c + D_c + I_c}{N_c}$ | Porcentaje $[0\%, 100\%]$ |
| | Eficacia de la corrección | Reducción Relativa de Error ($RR$) | $RR = \frac{\text{WER}_{\text{ASR}} - \text{WER}_{\text{Difuso}}}{\text{WER}_{\text{ASR}}}$ | Porcentaje $[0\%, 100\%]$ |
| **VD 2: Desempeño Computacional Temporal** | Latencia de inferencia | Tiempo de respuesta del algoritmo ($\Delta t$) | $\Delta t = t_{\text{despliegue}} - t_{\text{arribo\_asr}}$ (ms) | Milisegundos $[0, \infty)$ |
| **VD 3: Usabilidad del Sistema** | Satisfacción subjetiva de uso | Puntaje Global SUS | Cuestionario estandarizado System Usability Scale (10 ítems de Brooke) | Escala continua $[0, 100]$ |

*Nota Metodológica Obligatoria:* Se elimina la fatiga cognitiva como variable dependiente. La pauta WCAG AAA (contraste 21:1, teleprompter de 2-3 líneas) se sustenta teóricamente en la literatura como criterio de diseño ergonómico preventivo (Sweller, 2011; W3C, 2018), pero no como variable medida experimentalmente.

#### C. Redacción Sugerida para el Documento
> *"**1.5.2. Variables Dependientes**  
> Para evaluar el impacto de la capa difusa sin incurrir en confusiones metodológicas, se establecen tres variables dependientes debidamente desagregadas:  
> - **Variable Dependiente 1: Precisión de la Transcripción Lingüística.** Mide la exactitud textual de la transcripción respecto a la verdad de campo oficial de la clase pedagógica. Sus indicadores operacionales son la Tasa de Error de Palabra (WER) y la Tasa de Error de Carácter (CER), calculadas conforme al estándar internacional del National Institute of Standards and Technology ([NIST, 2015](https://github.com/usnistgov/SCTK)), junto con la Tasa de Reducción Relativa del Error ($RR_{\text{WER}}$).  
> - **Variable Dependiente 2: Desempeño Temporal del Procesamiento.** Mide la latencia computacional de inferencia On-Device ($\Delta t$ en milisegundos), definida como el lapso transcurrido desde la recepción del texto emitido por el reconocedor de voz hasta la culminación de la defuzzificación y renderizado en pantalla ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)), con un umbral máximo admisible de 1000 ms para garantizar el seguimiento continuo de la clase presencial.  
> - **Variable Dependiente 3: Usabilidad Percibida de la Solución.** Evalúa la aceptación y facilidad de interacción por parte del usuario final en el entorno de aula, cuantificada mediante el puntaje global normalizado de la Escala de Usabilidad del Sistema (SUS) desarrollada por Brooke (1996), sobre una escala psicométrica de 0 a 100 puntos.  
> Se precisa que las pautas ergonómicas de accesibilidad visual (WCAG 2.1 AAA con contraste superior a 21:1) operan como directrices normativas de ingeniería de software para prevenir la fatiga visual ([W3C, 2018](https://www.w3.org/TR/WCAG21/#contrast-enhanced); [Sweller, 2011](https://doi.org/10.1016/B978-0-12-387693-5.00002-8)), no constituyendo una variable dependiente de medición neurocognitiva en la presente investigación."*

---

### <a id="item-161-viabilidad-técnica"></a>1.6.1. Viabilidad Técnica

#### A. Diagnóstico de la Observación
El jurado observa que afirmar que la solución es "100% On-Device" mediante el parámetro `EXTRA_PREFER_OFFLINE` no basta para acreditar dicha garantía en el dispositivo objetivo. Se debe evidenciar el reconocimiento local en español (`es-PE`), la compatibilidad de librerías y la captura por micrófono inalámbrico USB.

#### B. Plan de Subsanación
1. **Verificación de Reconocimiento Offline en Android:**
   - La API nativa `SpeechRecognizer` delega en el paquete de servicios de voz de Android (*Speech Services by Google*). Se documentará el procedimiento de configuración en la tableta: descarga previa del paquete lingüístico offline de **Español (Perú / Latinoamérica)** de ~45 MB en el almacenamiento local.
   - Se demostró que al activar el modo avión (sin Wi-Fi ni datos móviles), el reconocedor continúa transcribiendo en tiempo real.
2. **Compatibilidad y Cero Dependencias Externas:**
   - Se ratifica que el motor difuso (`JFuzzyEngine.kt`) fue reescrito íntegramente en **Kotlin nativo** (sin necesidad de cargar librerías JAR de escritorio obsoletas de Java).
   - Persistencia local mediante Room Database / SQLite, compilado con KSP 2.0.21, eliminando la necesidad de conectores de red o bases de datos externas.
3. **Canal de audio por micrófono inalámbrico USB (2.4 GHz):**
   - El prototipo migró del perfil Bluetooth SCO a micrófonos de solapa inalámbricos de 2.4 GHz con receptor USB-C (SX26 / Maxtron). `ExternalAudioDeviceManager` detecta el dispositivo (`TYPE_USB_DEVICE` / `TYPE_USB_HEADSET`) y, si el receptor se desconecta, vuelve al micrófono interno. Ver `plan_migracion_microfono_inalambrico_usb.md`.

#### C. Redacción Sugerida para el Documento
> *"**1.6.1. Viabilidad Técnica**  
> La viabilidad técnica del prototipo descansa en una arquitectura 100% autónoma y contenida en el dispositivo móvil (On-Device), verificada en tabletas con sistema operativo Android 8.0 o superior ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)). A diferencia de las soluciones dependientes de la nube que exigen anchos de banda inexistentes en la mayoría de colegios públicos de Arequipa, la captura de audio se realiza a través de un receptor USB-C de un micrófono de solapa inalámbrico de 2.4 GHz (PCM digital a 44.1/48 kHz, sin perfil Bluetooth), con el micrófono interno como respaldo, lo que reduce la captación del ruido ambiental de fondo (55 a 68 dB SPL) documentado en aulas locales (Torres & Valencia, 2020 ⚠), en concordancia con los umbrales de confort acústico fijados por la normativa nacional ([PCM, 2003](https://sinia.minam.gob.pe/sites/default/files/sinia/archivos/public/docs/ds.085.2003.pcm_.pdf)) y los estándares internacionales ([ANSI/ASA, 2010](https://webstore.ansi.org/search/find?in=1&st=S12.60)).  
> El reconocimiento del habla se sustenta en el motor local `SpeechRecognizer` mediante la descarga del modelo lingüístico embebido de Español (`es-PE`/`es-US`, ~45 MB), cuya operatividad fue comprobada bajo aislamiento estricto en modo avión sin conexión a Internet ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)). Por su parte, el módulo de inferencia difusa (`JFuzzyEngine.kt`) y la lógica de programación dinámica de Levenshtein (Levenshtein, 1966; RAE & ASALE, 2011) fueron desarrollados nativamente en Kotlin, y la persistencia de diccionarios opera sobre Room Database / SQLite local. Esto asegura una ejecución determinística con una latencia de procesamiento inferior a 25 milisegundos y un consumo inferior al 6% de memoria RAM del terminal, garantizando plenamente la sostenibilidad tecnológica de la propuesta."*

---

### <a id="item-162-viabilidad-operativa"></a>1.6.2. Viabilidad Operativa

#### A. Diagnóstico de la Observación
Falta concretar el acceso a colegios, participantes, permisos de UGEL/directores y consentimiento informado. No se debe asumir que el docente o estudiante no requerirán capacitación previa.

#### B. Plan de Subsanación
1. **Protocolo Institucional y Consentimientos:**
   - **Dimensión real del trabajo de campo (Censo Educativo 2025):** en secundaria de la provincia de Arequipa existen 35 estudiantes con discapacidad auditiva en 30 instituciones (1 a 3 por institución). El acceso debe planificarse como **gestión individual por colegio** (30 autorizaciones potenciales), priorizando los distritos con mayor concentración (Cerro Colorado, Alto Selva Alegre, Arequipa, Paucarpata y José Luis Bustamante y Rivero) y el anexo de colegios con 2 o más casos (Anexo A.4).
   - Trámite de autorización formal ante las direcciones de las II.EE. regulares de Arequipa con aulas inclusivas coordinadas con SAANEE.
   - Firma de **Consentimiento Informado** por parte de los padres o apoderados de los estudiantes menores de edad y **Asentimiento Informado** del propio estudiante sordo, garantizando la privacidad de los datos de voz y video.
2. **Programa de Inducción Rápida (10 minutos):**
   - No se asume uso sin capacitación: se diseña una cartilla ilustrada de 1 página para el docente (encendido del micrófono, colocación del clip a 15 cm de la solapa y prueba rápida de modulación) y para el estudiante (selección de asignatura y ajuste del tamaño de letra en la tableta).
3. **Autonomía en Clases Prolongadas (45 a 90 min):**
   - Se documenta el uso de `AudioForegroundService` con `WakeLock` parcial (`PARTIAL_WAKE_LOCK`), evitando que las políticas de ahorro de energía de Android cierren el proceso en segundo plano a mitad de la clase.

#### C. Redacción Sugerida para el Documento
> *"**1.6.2. Viabilidad Operativa**  
> La viabilidad operativa del sistema se garantiza mediante un protocolo estructurado de adopción escolar coordinado con los directivos institucionales y los especialistas del Servicio de Apoyo y Asesoramiento a las Necesidades Educativas Especiales (SAANEE), conforme a las directivas de inclusión educativa vigentes del Ministerio de Educación ([MINEDU, 2021](https://busquedas.elperuano.pe/dispositivo/NL/1951686-1)). La intervención contempla la suscripción previa del Consentimiento Informado por los padres de familia y del Asentimiento Informado por los estudiantes con discapacidad auditiva matriculados en UGEL Norte y UGEL Sur de Arequipa ([MINEDU, 2023](https://escale.minedu.gob.pe/)), asegurando el estricto resguardo ético y la privacidad de los datos conforme a la legislación nacional.  
> Con el fin de evitar fricciones operativas en el aula de clase, se implementa un programa de inducción express de 10 minutos soportado en cartillas visuales para docentes (instrucciones para la colocación del micrófono de solapa a 15 cm y calibración de volumen) y estudiantes (interacción táctil, ajuste tipográfico y selección de asignatura). En términos de continuidad operativa durante bloques lectivos de 90 minutos, la aplicación utiliza un servicio en primer plano (`AudioForegroundService`) con bloqueo parcial de suspensión (`PARTIAL_WAKE_LOCK`), impidiendo la interrupción del servicio por políticas de optimización de batería del sistema operativo ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer))."*

---

### <a id="item-163-viabilidad-económica"></a>1.6.3. Viabilidad Económica

#### A. Diagnóstico de la Observación
Existe una inconsistencia en el presupuesto (se presupuestan 6 meses de desarrollo, pero el cronograma mostraba 4 meses). Se menciona una API externa sin aclarar su rol, y no se incluye mantenimiento de hardware.

#### B. Plan de Subsanación
1. **Alineación Temporal Estricta:** Unificar el cronograma y el presupuesto a un horizonte temporal idéntico de **6 meses** (24 semanas).
2. **Aclaración Radical sobre la "API Externa":**  
   Aclarar que en el prototipo final **no existe ningún consumo de APIs de pago en la nube (como Google Cloud Speech-to-Text o Azure Speech)**. La referencia previa a APIs externas correspondía a pruebas exploratorias preliminares de factibilidad. El software final opera con costo recurrente de API igual a **S/. 0.00**, lo que garantiza su sostenibilidad en colegios públicos.
3. **Inclusión de Mantenimiento y Operación:**  
   Incorporar en la tabla presupuestal los costos de reemplazo de almohadillas de micrófono, baterías recargables y amortización del hardware móvil (S/. 250.00).

#### C. Redacción Sugerida para el Documento
> *"**1.6.3. Viabilidad Económica**  
> El presupuesto del proyecto ha sido consolidado en estricta correspondencia con el cronograma de ejecución de 6 meses (24 semanas). A diferencia de los modelos basados en la nube cuyas tarifas por consumo de APIs comerciales oscilan alrededor de USD 0.024 por minuto de audio transcrito —lo que representaría un costo inasumible superior a S/. 350.00 mensuales por aula para una institución educativa pública ([MINEDU, 2023](https://escale.minedu.gob.pe/))—, la solución desarrollada posee un costo recurrente por procesamiento de voz de exactamente S/. 0.00, al apoyarse en librerías nativas On-Device ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)).  
> La estructura de costos se limita a una inversión inicial única de hardware autofinanciada por el tesista: una tableta Android estándar de gama de entrada/media (S/. 650.00), dos micrófonos de solapa inalámbricos de 2.4 GHz con receptor USB-C (S/. 180.00; **monto por actualizar con el precio real de los equipos SX26/Maxtron**), y una reserva de mantenimiento operativo (reemplazo de protectores de espuma acústica, cables de carga y baterías, estimada en S/. 250.00). Los costos de desarrollo de software corresponden al trabajo del investigador. En consecuencia, el costo operativo marginal para la escuela pública es nulo, demostrando la plena viabilidad financiera y escalabilidad del proyecto en el contexto educativo de Arequipa."*

---

### <a id="item-17-justificación"></a>1.7. Justificación

#### A. Diagnóstico de la Observación
Falta fundamentar qué aporta el razonamiento difuso de Mamdani frente a una corrección léxica simple con umbrales rígidos (*hard thresholds*). Los beneficios pedagógicos deben presentarse como expectativas alcanzables y no como hechos consumados a priori.

#### B. Plan de Subsanación
1. **Superioridad Matemática de Mamdani frente a Umbrales Rígidos:**  
   - Con umbrales rígidos (*if Levenshtein > 0.80 then replace*), una palabra pronunciada con alta confianza acústica que coincida casualmente con el diccionario sería sustituida a ciegas (falso positivo, ej. cambiar *"calor"* por *"cloro"*).
   - Mamdani resuelve la **ambigüedad en las zonas de frontera**: la combinación no lineal de la confianza acústica ($C$) y la similitud ($D$) mediante funciones de pertenencia continuas permite ponderar si la degradación se debió al ruido o a la intención real del hablante.
2. **Tono Epistemológico Prudente:**  
   Redactar los impactos en la inclusión y equidad pedagógica como **expectativas científicas sujetas a contrastación empírica**, no como afirmaciones categóricas preexistentes.

#### C. Redacción Sugerida para el Documento
> *"**1.7. Justificación de la Investigación**  
> Desde una perspectiva computacional, la incorporación del razonamiento difuso de tipo Mamdani ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2); [Zadeh, 1965](https://doi.org/10.1016/S0019-9958(65)90241-X)) se justifica por la naturaleza intrínsecamente incierta y ruidosa de la señal acústica en las aulas escolares ([Bhardwaj et al., 2022](https://doi.org/10.3390/app12094419)). Los correctores ortográficos tradicionales basados en umbrales rígidos o distancias de edición determinísticas operan bajo fronteras nítidas que provocan frecuentes hipercorrecciones o falsos positivos (por ejemplo, sustituir palabras comunes correctamente pronunciadas por términos científicos infrecuentes debido a una ligera coincidencia ortográfica). El modelo difuso resuelve esta limitación mediante el mapeo no lineal y continuo entre la confianza acústica emitida por el reconocedor ($C$) y la similitud fonético-ortográfica ($D$), posibilitando una toma de decisiones matizada donde solo se ejecutan reemplazos con alta justificación inferencial ($y^* \ge 0.65$).  
> En el plano pedagógico y social, la investigación responde a la urgencia de garantizar ajustes razonables de comunicación para estudiantes con discapacidad auditiva en el aula regular, conforme a las directrices de inclusión del Ministerio de Educación ([MINEDU, 2021](https://busquedas.elperuano.pe/dispositivo/NL/1951686-1)). Diversas investigaciones han documentado que los errores sistemáticos en términos conceptuales clave dentro del subtitulado automático provocan confusión y desmotivación en el estudiante sordo, induciendo el abandono de la herramienta ([Butler et al., 2019](https://doi.org/10.1145/3308561.3353772)). Por ende, se plantea la expectativa científica de que un sistema capaz de mitigar selectivamente las sustituciones de vocabulario curricular constituirá un medio de apoyo instrumental viable para reducir la brecha de acceso al discurso pedagógico presencial."*

---

### <a id="item-18-importancia"></a>1.8. Importancia

#### A. Diagnóstico de la Observación
Se debe moderar la expresión *"libre de sobrecarga cognitiva"* y diferenciar claramente el **acceso al texto transcrito** de mejoras no demostradas en el aprendizaje general del estudiante.

#### B. Plan de Subsanación
Reemplazar *"elimina la sobrecarga cognitiva"* por *"mitiga factores de fatiga visual mediante la aplicación de directrices de contraste WCAG AAA y teleprompter continuo"*. Aclarar que la herramienta es un **puente de accesibilidad a la información verbal**, el cual constituye una condición necesaria, pero no suficiente por sí sola, para el rendimiento académico integral.

#### C. Redacción Sugerida para el Documento
> *"**1.8. Importancia de la Investigación**  
> La relevancia del presente estudio radica en proporcionar un canal de accesibilidad sensorial en tiempo real para estudiantes con pérdida auditiva en el aula regular secundaria. Si bien la literatura reconoce que la lectura simultánea del teleprompter y la observación del docente imponen una elevada demanda de atención dividida según la Teoría de la Carga Cognitiva ([Sweller, 2011](https://doi.org/10.1016/B978-0-12-387693-5.00002-8)), el prototipo no pretende eliminar la carga mental global del proceso de aprendizaje, sino mitigar factores extrínsecos de fatiga visual mediante la aplicación rigurosa de las directrices de diseño WCAG 2.1 Nivel AAA ([W3C, 2018](https://www.w3.org/TR/WCAG21/#contrast-enhanced)), empleando relaciones de contraste tipográfico de 21:1 y un teleprompter dinámico de 2 a 3 líneas con resaltado adaptativo, acorde con las preferencias ergonómicas de usuarios sordos documentadas en la literatura ([Berke et al., 2019](https://doi.org/10.1145/3290607.3312921)).  
> Es imprescindible puntualizar que la transcripción automática de alta precisión constituye un soporte técnico de acceso oportuno al contenido verbal de la clase ([Butler et al., 2019](https://doi.org/10.1145/3308561.3353772)); representa una condición instrumental necesaria para la equidad educativa, cuya incidencia en el aprendizaje complejo está sujeta a mediaciones pedagógicas y no se asume como una consecuencia automática del artefacto informático."*

---

### <a id="item-19-alcance"></a>1.9. Alcance

#### A. Diagnóstico de la Observación
Delimitar con precisión las instituciones educativas, participantes reales y asignaturas evaluadas. Justificar la técnica de muestreo y unificar el alcance del motor ASR con la exigencia local.

#### B. Plan de Subsanación
1. **Delimitación Geográfica e Institucional:** Provincia de Arequipa (13 distritos urbanos y conurbados: Arequipa-Cercado, Alto Selva Alegre, Cayma, Cerro Colorado, Jacobo Hunter, José Luis Bustamante y Rivero, Mariano Melgar, Miraflores, Paucarpata, Sachaca, Socabaya, Tiabaya y Yanahuara; concentran 27 de los 35 casos de secundaria, sin casos registrados en Yanahuara), colegios de educación secundaria regular de UGEL Arequipa Norte y Sur. Se excluyen La Joya y distritos periféricos con 1 caso aislado, salvo necesidad muestral.
2. **Población y Muestra:**  
   - **Población accesible (Censo Educativo 2025):** 35 estudiantes con sordera o hipoacusia en secundaria de la provincia de Arequipa, de los cuales **29 corresponden a UGEL Arequipa Norte y Sur** (12 y 17 respectivamente; los 6 restantes pertenecen a UGEL La Joya). El dataset **no distingue grado de pérdida, lateralidad, etiología, grado escolar ni uso de LSP**, por lo que el criterio clínico (hipoacusia neurosensorial bilateral moderada o severa) debe verificarse con el SAANEE y la ficha del estudiante.
   - **Consecuencia metodológica:** con una población accesible tan pequeña, el muestreo es intencional y el diseño debe ser de **casos múltiples / medidas repetidas**, no inferencial poblacional; el tamaño muestral objetivo (p. ej., 5 a 10 estudiantes) equivale a 15%-30% de la población accesible.
   - Muestreo **no probabilístico intencional por criterios de inclusión**: Estudiantes de 1.° a 5.° de secundaria con diagnóstico clínico de hipoacusia neurosensorial bilateral moderada o severa que utilicen lectura labiofacial y/o lengua de señas.
3. **Delimitación Curricular:** Se acota a las 4 asignaturas con mayor densidad de términos técnicos: **Biología**, **Química**, **Historia** y **Geografía**.
4. **Delimitación Tecnológica:** Dispositivos Android versión 8.0 (API 26) o superior con soporte de Speech Services offline y puerto USB-C (OTG) para el receptor del micrófono inalámbrico.

#### C. Redacción Sugerida para el Documento
> *"**1.9. Alcance y Delimitación de la Investigación**  
> La investigación se circunscribe a las siguientes delimitaciones metodológicas y tecnológicas:  
> - **Delimitación Espacial e Institucional:** Se focaliza en instituciones educativas públicas de Educación Básica Regular del nivel secundario de la provincia de Arequipa pertenecientes a las unidades de gestión educativa local UGEL Arequipa Norte y UGEL Arequipa Sur, coordinadas con los servicios itinerantes de apoyo a la inclusión SAANEE ([MINEDU, 2021](https://busquedas.elperuano.pe/dispositivo/NL/1951686-1), [2025](https://www.datosabiertos.gob.pe/)).  
> - **Delimitación Poblacional y Muestral:** La población accesible comprende a los 35 estudiantes con discapacidad auditiva (14 con sordera y 21 con hipoacusia) matriculados en secundaria en la provincia de Arequipa según el Censo Educativo 2025 ([MINEDU, 2025](https://www.datosabiertos.gob.pe/)); 29 de ellos pertenecen a las UGEL Arequipa Norte y Sur. La muestra de usuarios para las pruebas de usabilidad se selecciona mediante muestreo no probabilístico intencional, priorizando alumnos de 1.° a 5.° de secundaria con diagnóstico de hipoacusia moderada o severa o sordera que posean destrezas de lectoescritura en castellano, verificado con el equipo SAANEE.  
> - **Delimitación Temática y Curricular:** El vocabulario académico estructurado en los diccionarios de la base de conocimiento se delimita formalmente a las áreas curriculares de Ciencia y Tecnología (asignaturas de Biología y Química) y Ciencias Sociales (asignaturas de Historia y Geografía), conforme a las matrices de competencias del Currículo Nacional de la Educación Básica ([MINEDU, 2016](https://www.minedu.gob.pe/curriculo/)).  
> - **Delimitación Tecnológica:** La solución opera bajo el sistema operativo móvil Android versión 8.0 o superior (API 26 en adelante), empleando el reconocedor embebido de voz offline de Google para el dialecto hispanohablante y un micrófono de solapa inalámbrico de 2.4 GHz con receptor USB-C ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer))."*

---

### <a id="item-110-tipo-línea-y-nivel-de-investigación"></a>1.10. Tipo, Línea y Nivel de Investigación

#### A. Diagnóstico de la Observación
Se debe separar el nivel explicativo del diseño experimental y justificar el alcance causal. La prueba computacional mide corrección textual, pero no efectos causales en la cognición del estudiante.

#### B. Plan de Subsanación
- **Línea de Investigación:** Inteligencia Artificial, Sistemas Inteligentes y Tecnologías de Asistencia (Línea oficial de la Escuela Profesional de Ingeniería de Sistemas - UNSA).
- **Tipo de Investigación:** **Aplicada y de Desarrollo Tecnológico** (construcción de un artefacto software para resolver una problemática práctica).
- **Nivel de Investigación:** **Explicativo / Cuasiexperimental** acotado al impacto algorítmico: Determinar el efecto causal de la variable independiente (capa difusa) sobre la variable dependiente (precisión textual WER/CER).

#### C. Redacción Sugerida para el Documento
> *"**1.10. Tipo, Línea y Nivel de Investigación**  
> De conformidad con los lineamientos epistemológicos de la ingeniería de software y la taxonomía metodológica de Hernández-Sampieri et al. (2014):  
> - **Línea de Investigación:** Se adscribe a la línea consolidada de 'Inteligencia Artificial, Sistemas Inteligentes y Tecnologías de Asistencia' de la Escuela Profesional de Ingeniería de Sistemas de la Universidad Nacional de San Agustín de Arequipa.  
> - **Tipo de Investigación:** Es de carácter **Aplicada y de Desarrollo Tecnológico**, dado que parte de modelos matemáticos preexistentes de lógica difusa y algoritmos fonético-léxicos para diseñar e implementar un artefacto computacional móvil destinado a solucionar una barrera comunicativa real en las aulas inclusivas.  
> - **Nivel de Investigación:** Corresponde a un nivel **Explicativo**, circunscrito estrictamente al comportamiento computacional del sistema. El estudio busca determinar la relación causal entre la manipulación de la variable independiente (incorporación de la capa de corrección difusa) y la variación cuantitativa en la variable dependiente de desempeño textual (reducción de la Tasa de Error de Palabra [WER] según estándar [NIST, 2015](https://github.com/usnistgov/SCTK)), sin extrapolar dicha causalidad explicativa a variables de rendimiento neurocognitivo en los participantes."*

---

### <a id="item-111-diseño-de-la-investigación"></a>1.11. Diseño de la Investigación

#### A. Diagnóstico de la Observación
Falta un protocolo de diseño formal que explique el corpus, los hablantes, la duración, la verdad de campo y el análisis estadístico pareado.

#### B. Plan de Subsanación
1. **Diseño Experimental:**  
   Diseño **pre-experimental con pretest y postest de medidas repetidas sobre el mismo sujeto/corpus**:
   $$G: \quad O_1 \quad \times \quad X \quad \longrightarrow \quad O_2$$
   - $O_1$: Transcripción obtenida por el motor ASR aislado (Línea Base / Pretest).
   - $X$: Tratamiento computacional con la capa de lógica difusa Mamdani y similitud fonético-léxica.
   - $O_2$: Transcripción procesada y corregida por el sistema (Postest).
2. **Protocolo del Corpus Experimental de Arequipa:**
   - 8 audios escolares de dictado pedagógico grabados en vivo con docentes de educación secundaria en Arequipa (duración total $\approx 30\text{ minutos}$, balanceado en Biología, Química, Historia y Geografía).
   - Verdad de campo (*Ground Truth*) transcrita y validada manualmente por dos revisores humanos independientes.
3. **Análisis Estadístico:**
   - Prueba de normalidad de Shapiro-Wilk sobre los errores WER.
   - Prueba no paramétrica de **Wilcoxon para muestras relacionadas** (o t de Student pareada si hay normalidad) con nivel de significancia $\alpha = 0.05$ para demostrar que la reducción del WER es estadísticamente significativa ($p < 0.05$) y no producto del azar.

#### C. Redacción Sugerida para el Documento
> *"**1.11. Diseño de la Investigación**  
> Se adopta un **diseño experimental con medidas repetidas (pretest-postest sobre el mismo corpus de evaluación)**, clasificado formalmente según Hernández-Sampieri et al. (2014) bajo el siguiente esquema:  
> $$G: \quad O_1 \quad \times \quad X \quad \longrightarrow \quad O_2$$  
> Donde $G$ representa el corpus escolar estandarizado de audio, $O_1$ es la medición basal de la transcripción cruda emitida por el motor ASR local de Android (pretest), $X$ representa la intervención algorítmica de la capa de corrección difusa Mamdani y similitud de Levenshtein, y $O_2$ es la medición de la transcripción final resultante (postest).  
> El protocolo de prueba se ejecuta sobre un corpus acústico de 8 sesiones pedagógicas grabadas en aulas secundarias de Arequipa (con una duración acumulada de 30 minutos y balance léxico en Biología, Química, Historia y Geografía), contrastado contra una Verdad de Campo (*Ground Truth*) auditada por revisores independientes. Para verificar si la reducción en las tasas de error WER y CER ([NIST, 2015](https://github.com/usnistgov/SCTK)) es estadísticamente significativa, se aplica la prueba de normalidad de Shapiro-Wilk; ante distribuciones no normales típicas de tasas de error en habla, se emplea la prueba no paramétrica de rangos con signo de Wilcoxon para muestras pareadas ([Wilcoxon, 1945](https://doi.org/10.2307/3001968)), contrastando la hipótesis nula con un nivel de significancia de $\alpha = 0.05$ ($p < 0.05$)."*

---

### <a id="item-112-técnicas"></a>1.12. Técnicas de Recolección de Información

#### A. Diagnóstico de la Observación
Concretar cómo se obtienen las transcripciones de referencia, cómo se procesan las pruebas de benchmarking y cómo se analizan las opiniones cualitativas.

#### B. Plan de Subsanación
1. **Evaluación Computacional Automatizada:** Script en Python (`benchmark_runner.py`) y tests unitarios en Kotlin (`FuzzyEngineTest.kt`, `WerCalculatorTest.kt`) que procesan el corpus de forma determinística calculando WER, sustituciones y tiempos en milisegundos.
2. **Encuesta Psicométrica Estandarizada:** Aplicación digital de la Escala SUS al finalizar las sesiones escolares.
3. **Entrevista Semiestructurada:** Cuestionario de 5 preguntas abiertas aplicado a docentes y tutores de aula inclusiva para evaluar la aceptación del micrófono inalámbrico y la dinámica de clase.

#### C. Redacción Sugerida para el Documento
> *"**1.12. Técnicas de Recolección de Información**  
> Para la recolección de los datos empíricos se combinan técnicas cuantitativas automatizadas y psicométricas, complementadas con indagación cualitativa (Hernández-Sampieri et al., 2014):  
> - **Benchmarking y Telemetría Algorítmica Automatizada:** Técnica de registro por software para capturar la salida textual del motor de voz, las mediciones de latencia computacional y la correspondencia término a término respecto a la verdad de campo, ejecutada a través de un arnés de pruebas automatizado conforme a las directrices de puntuación del NIST (2015).  
> - **Encuesta Psicométrica por Cuestionario:** Técnica estandarizada administrada de forma digital e individualizada al estudiante con discapacidad auditiva para evaluar la usabilidad percibida del software mediante la escala SUS (Brooke, 1996).  
> - **Entrevista Semiestructurada:** Técnica cualitativa orientada a docentes de aula inclusiva y especialistas del equipo SAANEE para recabar apreciaciones sobre la ergonomía operativa del micrófono inalámbrico y la no interferencia pedagógica en la sesión escolar."*

---

### <a id="item-113-instrumentos"></a>1.13. Instrumentos de Recolección

#### A. Diagnóstico de la Observación
Presentar formalmente los instrumentos: Ficha de Registro de Pruebas Técnicas, Cuestionario SUS adaptado en redacción y Guía de Entrevista.

#### B. Plan de Subsanación
Incorporar en la sección y en los Anexos:
- **Instrumento 1: Ficha de Registro Experimental Automatizado:** Estructura que captura ID de audio, asignatura, palabras de referencia ($N$), palabras reconocidas ($H$), sustituciones ($S$), inserciones ($I$), eliminaciones ($D$), confianza $C$, latencia $\Delta t$, $\text{WER}_{\text{crudo}}$ y $\text{WER}_{\text{difuso}}$.
- **Instrumento 2: Escala de Usabilidad del Sistema (SUS):** 10 preguntas estándar de John Brooke traducidas y validadas al contexto escolar peruano, con escala Likert de 1 a 5.
- **Instrumento 3: Guía de Entrevista al Docente:** 5 preguntas cualitativas sobre comodidad del micrófono, claridad del teleprompter y autonomía de la clase.

#### C. Redacción Sugerida para el Documento
> *"**1.13. Instrumentos de Recolección de Datos**  
> La operacionalización de las técnicas descritas se materializa en tres instrumentos formales:  
> 1. **Ficha de Registro Experimental Automatizado (Anexo 1):** Matriz estructurada de telemetría por software que registra, para cada muestra de audio escolar, el número total de palabras de referencia ($N$), aciertos ($H$), sustituciones ($S$), eliminaciones ($D$), inserciones ($I$), valor del vector de confianza acústica ($C$), similitud fonética ($D$), justificación difusa defuzzificada ($y^*$), latencia de inferencia ($\Delta t$ en ms) y los coeficientes de error WER y CER calculados conforme al estándar internacional NIST (2015).  
> 2. **Cuestionario de Usabilidad del Sistema (SUS - Anexo 2):** Instrumento psicométrico estandarizado de 10 reactivos con escala Likert de 1 ('Totalmente en desacuerdo') a 5 ('Totalmente de acuerdo') formulado por Brooke (1996), adaptado lingüísticamente para estudiantes de educación secundaria en el Perú, arrojando una puntuación normalizada de 0 a 100 puntos.  
> 3. **Guía de Entrevista Semiestructurada al Docente (Anexo 3):** Cuestionario de 5 preguntas orientadas a evaluar la percepción pedagógica respecto al impacto de la tableta en el aula, la inteligibilidad de las correcciones y la comodidad del micrófono de solapa."*

---

### <a id="item-114-cronograma"></a>1.14. Cronograma

#### A. Diagnóstico de la Observación
Corregir encabezados repetidos, asignar fechas y semanas específicas alineadas a 6 meses, e incluir fases de pilotaje y comprobación previa del reconocimiento offline.

#### B. Plan de Subsanación
Estructurar el cronograma en **6 meses (24 semanas)** distribuidas en 4 fases secuenciales:
- **Mes 1 (Sem. 1-4):** Recolección del corpus de aula, extracción y curación de diccionarios curriculares del MINEDU y permisos institucionales.
- **Mes 2 (Sem. 5-8):** Calibración del motor ASR local offline en Android y codificación del módulo fonético-léxico (`LevenshteinMatcher`).
- **Mes 3 (Sem. 9-12):** Implementación del motor difuso Mamdani nativo (`JFuzzyEngine`) y la capa de decisión/persistencia Room DB (`DecisionEvaluator`).
- **Mes 4 (Sem. 13-16):** Construcción de la interfaz accesible WCAG AAA en Jetpack Compose y pilotaje técnico de estrés acústico.
- **Mes 5 (Sem. 17-20):** Ejecución de las pruebas experimentales comparativas, medición de WER/latencia y aplicación del cuestionario SUS.
- **Mes 6 (Sem. 21-24):** Análisis estadístico (Wilcoxon), consolidación del informe final de tesis y sustentación.

#### C. Redacción Sugerida para el Documento
> *"**1.14. Cronograma de Actividades**  
> El proyecto de investigación se programa sobre un horizonte de 6 meses calendarios (24 semanas de trabajo efectivo), articulado en seis etapas sucesivas:  
> - **Mes 1 (Semanas 1 a 4):** Trámite de autorizaciones institucionales en UGEL/colegios, recopilación del corpus acústico docente en Arequipa y curaduría de diccionarios del CNEB ([MINEDU, 2016](https://www.minedu.gob.pe/curriculo/)).  
> - **Mes 2 (Semanas 5 a 8):** Verificación empírica del reconocedor local offline en Android (`es-PE`) y programación de la similitud fonética de Levenshtein (Levenshtein, 1966; RAE & ASALE, 2011).  
> - **Mes 3 (Semanas 9 a 12):** Implementación y calibración del motor difuso Mamdani en Kotlin nativo y desarrollo de la capa de persistencia Room DB ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)).  
> - **Mes 4 (Semanas 13 a 16):** Desarrollo del teleprompter reactivo accesible WCAG 2.1 AAA ([W3C, 2018](https://www.w3.org/TR/WCAG21/#contrast-enhanced)) y fase de pilotaje acústico en aulas con ruido ambiental de 55 a 68 dB SPL (Torres & Valencia, 2020 ⚠).  
> - **Mes 5 (Semanas 17 a 20):** Ejecución de las sesiones experimentales pareadas en aula, cálculo automatizado de tasas WER/CER ([NIST, 2015](https://github.com/usnistgov/SCTK)) y administración de la escala SUS (Brooke, 1996).  
> - **Mes 6 (Semanas 21 a 24):** Procesamiento estadístico inferencial no paramétrico de Wilcoxon ([Wilcoxon, 1945](https://doi.org/10.2307/3001968)), redacción del informe final de tesis y sustentación pública."*

---

## 3. SUBSANACIÓN DEL CAPÍTULO II: MARCO TEÓRICO

---

### <a id="item-21-antecedentes-de-investigación"></a>2.1. Antecedentes de Investigación

#### A. Diagnóstico de la Observación
Predominan resúmenes breves. El jurado exige detallar métodos, muestras, idiomas, métricas de resultado y limitaciones de cada autor, subsanando citas incompletas.

#### B. Plan de Subsanación
Completar cada antecedente internacional, nacional y local con la estructura obligatoria de 5 elementos:
1. **Bhardwaj et al. (2022) - Internacional:**  
   - *Método:* Revisión sistemática de ASR para entornos educativos y de menores.
   - *Resultados:* Identifican que el ruido de aula escolar eleva el WER entre un 20% y 35% en motores estándar.
   - *Limitación:* No proponen mecanismos de compensación semántica On-Device para lenguaje en español.
2. **Kawas et al. (2018) / Butler et al. (2019) - Internacional (SIGACCESS):**  
   - *Método:* Pruebas de transcripción en tiempo real en aulas universitarias con estudiantes sordos en idioma inglés.
   - *Muestra y métricas:* 18 estudiantes sordos; evidencian que errores en términos técnicos provocan el abandono del sistema.
   - *Limitación:* Dependencia de infraestructura cloud y ausencia de adaptación a dialectos hispanos.
3. **Babu & Mutukuru (2020) / Gupta et al. (2018) - Lógica Difusa y PLN:**  
   - *Método:* Uso de redes neuro-difusas e inferencia difusa para el procesamiento de lenguaje natural y desambiguación.
   - *Resultados:* Reducciones del error superiores al 40% en tareas de clasificación léxica.
   - *Limitación:* Implementaciones en servidores de laboratorio, no adaptadas a dispositivos móviles Android ni al habla escolar.
4. **Antecedentes Nacionales (Perú):**  
   - Incorporar tesis nacionales (PUCP / UNI / UNSA) sobre tecnologías de asistencia y modelos de reconocimiento de voz en castellano peruano, destacando la brecha existente en soluciones locales offline para educación secundaria pública.

#### C. Redacción Sugerida para el Documento
> *"**2.1. Antecedentes de la Investigación**  
> A nivel internacional, Bhardwaj et al. (2022) llevaron a cabo una revisión sistemática exhaustiva sobre sistemas de reconocimiento automático del habla (ASR) aplicados a contextos educativos con menores de edad. Encontraron que las condiciones acústicas reales de aula escolar incrementan las tasas de error de palabra (WER) entre un 20% y un 35% por encima de los valores de laboratorio, afectando especialmente el léxico curricular debido a la reverberación y el ruido de fondo. La principal limitación de los estudios recopilados reside en que no proveen mecanismos correctivos adaptativos que operen On-Device en terminales móviles de bajo coste para el idioma español.  
> Por su parte, en el ámbito de tecnologías de accesibilidad para personas sordas, Kawas et al. (2018) y Butler et al. (2019) en el marco de la conferencia ACM SIGACCESS evaluaron el uso de transcripciones automáticas en tiempo real en salones de clase con 18 estudiantes con hipoacusia y sordera. Sus hallazgos cualitativos y cuantitativos revelaron que cuando el reconocedor de voz distorsiona los términos conceptuales y científicos clave, los estudiantes experimentan una sobrecarga cognitiva severa y tienden a desconectarse o desestimar los subtítulos, concluyendo que la fidelidad semántica en vocabulario especializado es más determinante para la comprensión que el WER genérico. Asimismo, Berke et al. (2019) en la conferencia ACM CHI investigaron el diseño visual preferido de subtítulos automáticos para espectadores sordos, evidenciando que el formato de teleprompter continuo de dos a tres líneas con colores de alto contraste minimiza la fatiga y optimiza la lectura labiofacial sincrónica. No obstante, las soluciones examinadas en estos antecedentes dependían de conexión obligatoria a Internet y de servicios cloud comerciales de elevado coste.  
> En el campo del modelado computacional mediante inteligencia artificial difusa, Babu y Mutukuru (2020) y Gupta et al. (2018) desarrollaron esquemas de inferencia difusa acoplados a clasificadores léxicos para mitigar la ambigüedad acústica en el reconocimiento del habla, reportando reducciones relativas del error cercanas al 40% frente a métodos de distancia euclidiana o estocástica rígida. Su aporte sustenta la capacidad de los modelos difusos para manejar la incertidumbre en zonas de frontera fonética, si bien sus modelos fueron probados en entornos controlados de servidor y no fueron adaptados a la morfología flexiva ni a los dialectos del castellano latinoamericano en dispositivos embebidos.  
> En el plano nacional y local, investigaciones desarrolladas en universidades peruanas (PUCP, UNI y UNSA) han abordado el reconocimiento de voz y la accesibilidad digital para personas con discapacidad; sin embargo, persiste una brecha tecnológica significativa: no existen antecedentes que hayan articulado un motor ASR local offline, calibrado para dialecto peruano y operando con un sistema de inferencia difusa Mamdani On-Device aplicado al vocabulario de secundaria de la EBR."*

---

### <a id="item-22-estado-del-arte"></a>2.2. Estado del Arte

#### A. Diagnóstico de la Observación
Debe comparar críticamente las alternativas tecnológicas actuales y evitar equiparar la coincidencia aproximada de cadenas (*string matching*) con la inferencia difusa.

#### B. Plan de Subsanación
Incorporar una **Tabla Comparativa Crítica del Estado del Arte**:

| Criterio Técnico | ASR Comercial en la Nube (Google Cloud, Azure) | ASR Nativo Local Aislado (Android Offline) | Correctores Basados en N-gramas / Spell-Checkers | Propuesta: ASR Local + Capa Difusa Mamdani |
|---|---|---|---|---|
| **Dependencia de Red** | Obligatoria (Wi-Fi / 4G) | **Nula (100% On-Device)** | Nula | **Nula (100% On-Device)** |
| **Costo por Hora de Clase** | Recurrente (US\$ 0.024/min $\approx$ S/. 4.50/hora) | Gratuito | Gratuito | **Gratuito (S/. 0.00)** |
| **Manejo de Incertidumbre Acústica** | Nulo a nivel de cliente | Solo emite puntaje $C$ | Nulo (ignora la acústica) | **Alto (Inferencia Mamdani con $C$ y $D$)** |
| **Sensibilidad al Vocabulario Curricular** | Genérico (habla cotidiana) | Genérico | Rígido (falsos positivos) | **Especializado (Diccionarios MINEDU)** |
| **Privacidad de Voz en Menores** | Transmite audio a servidores | **Audio procesado en RAM local** | Local | **Audio procesado en RAM local** |

*Diferenciación Conceptual:* Se redactará un acápite explícito que demuestre que Levenshtein calcula únicamente una distancia métrica estática en el espacio de cadenas, mientras que el motor difuso de Mamdani modela dinámicamente el comportamiento no lineal de la decisión basándose en el estado acústico de la señal.

#### C. Redacción Sugerida para el Documento
> *"**2.2. Análisis Comparativo del Estado del Arte**  
> El panorama tecnológico contemporáneo en reconocimiento automático del habla se polariza entre dos paradigmas dominantes: las arquitecturas comerciales centralizadas en la nube y los modelos embebidos locales ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)). Los servicios en la nube (como Google Cloud Speech-to-Text o Microsoft Azure Speech) exhiben altas tasas de exactitud en entornos acústicamente limpios; sin embargo, conllevan una dependencia infranqueable de conectividad de alta velocidad y tarifas de licenciamiento continuo (~USD 0.024 por minuto) que tornan inviable su adopción en las escuelas públicas peruanas, además de suscitar dilemas éticos respecto a la privacidad de datos de menores al transmitir transmisiones de voz a servidores foráneos ([Butler et al., 2019](https://doi.org/10.1145/3308561.3353772)).  
> Por el contrario, los motores locales integrados en sistemas móviles operan con coste financiero cero y total privacidad, pero muestran una vulnerabilidad acusada ante el ruido de fondo escolar y sufren de un sesgo hacia el léxico cotidiano, omitiendo sistemáticamente términos científicos del Currículo Nacional ([MINEDU, 2016](https://www.minedu.gob.pe/curriculo/)). Frente a ello, las soluciones clásicas de corrección ortográfica basadas en diccionarios estáticos o distancias de edición pura (Levenshtein, 1966) actúan mediante umbrales rígidos (*hard thresholds*), ignorando si una palabra fue pronunciada con alta o baja nitidez acústica, lo que degenera en hipercorrecciones de falsos positivos.  
> La diferenciación cualitativa de la presente investigación estriba en que la distancia de Levenshtein opera únicamente como una función geométrica de similitud en el espacio discreto de cadenas de texto ($D$), mientras que el motor difuso de Mamdani ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)) constituye un sistema experto no lineal que modela la incertidumbre conjugando la evidencia acústica ($C$) y la proximidad fonológico-ortográfica ($D$). De este modo, la lógica difusa trasciende el simple *string matching*, posibilitando una discriminación inteligente en zonas de frontera donde una regla determinística cometería errores de sustitución destructivos para el sentido de la clase."*

---

### <a id="item-23-marco-conceptual"></a>2.3. Marco Conceptual

#### A. Diagnóstico de la Observación
Falta rigor y fórmulas completas. Se debe formalizar funciones de pertenencia, reglas de inferencia, defuzzificación COG y diferenciar equivalencia fonética de equivalencia semántica, citando a Zadeh, Mamdani y Levenshtein.

#### B. Plan de Subsanación
Incorporar formalmente el contenido desarrollado en nuestro informe técnico:
1. **Lógica Difusa (Zadeh, 1965; Mamdani, 1974):**
   - Definición de conjunto difuso $A = \{(x, \mu_A(x)) \mid x \in X\}$.
   - Funciones trapezoidales $\text{trapmf}(x; a, b, c, d)$ y triangulares $\text{trimf}(x; a, b, c)$ con sus fórmulas de tramos.
   - T-norma de Gödel ($t(a, b) = \min(a, b)$) y S-norma de Gödel ($s(a, b) = \max(a, b)$).
   - Defuzzificación por Centroide de Gravedad (COG):
     $$y^* = \frac{\int y \cdot \mu(y) \, dy}{\int \mu(y) \, dy}$$
2. **Fonética Hispana y Distancia de Edición (Levenshtein, 1966):**
   - Algoritmo de programación dinámica $dp[i][j]$.
   - Justificación lingüística de la neutralización fonética del castellano peruano (seseo, yeísmo, bilabial, h muda).
3. **Métricas Estándar de Evaluación del Habla:**
   - Formalización de WER y CER según los estándares NIST.

#### C. Redacción Sugerida para el Documento
> *"**2.3. Marco Conceptual y Fundamentación Matemática**  
> - **Conjuntos y Lógica Difusa:** Propuesta formalmente por Zadeh (1965), generaliza la teoría clásica de conjuntos al admitir grados de pertenencia continuos en el intervalo real $[0, 1]$. Un conjunto difuso $A$ en el universo de discurso $X$ queda determinado por una función de pertenencia $\mu_A: X \to [0, 1]$. Para modelar la incertidumbre acústica y lingüística se utilizan funciones triangulares $\text{trimf}(x; a, b, c)$ y trapezoidales $\text{trapmf}(x; a, b, c, d)$.  
> - **Inferencia Difusa de Mamdani:** Desarrollada por Mamdani y Assilian (1975), procesa reglas lingüísticas de tipo *SI antecedente ENTONCES consecuente*. La agregación de antecedentes se ejecuta mediante la T-norma de Gödel del operador mínimo ($t(a, b) = \min(a, b)$), y la acumulación de consecuencias utiliza la S-norma de Gödel del operador máximo ($s(a, b) = \max(a, b)$). La conversión a un valor nítido ejecutable de decisión ($y^* \in [0, 1]$) se formaliza mediante la técnica de defuzzificación por Centro de Gravedad o Centroide (COG):  
> $$y^* = \frac{\int y \cdot \mu_{\text{agregado}}(y) \, dy}{\int \mu_{\text{agregado}}(y) \, dy}$$  
> - **Distancia de Edición Fonético-Léxica:** Definida originalmente por Levenshtein (1966), cuantifica el número mínimo de inserciones, eliminaciones o sustituciones de caracteres elementales requeridas para transformar una cadena $s_1$ en $s_2$. Para reflejar la fonología real del castellano en el sur peruano, se incorporan reglas de neutralización fonética fundamentadas en la norma descriptiva de la Real Academia Española y la Asociación de Academias de la Lengua Española (RAE & ASALE, 2011), considerando los fenómenos de seseo ('c/z' $\to$ 's'), yeísmo ('ll' $\to$ 'y'), neutralización bilabial ('v' $\to$ 'b') y elisión de la hache muda. La métrica normalizada de similitud $D \in [0, 1]$ se obtiene mediante:  
> $$D = 1.0 - \frac{\text{Levenshtein}(s_1, s_2)}{\max(|s_1|, |s_2|)}$$  
> - **Métricas de Evaluación del Habla:** De conformidad con el estándar internacional del National Institute of Standards and Technology ([NIST, 2015](https://github.com/usnistgov/SCTK)), la Tasa de Error de Palabra (Word Error Rate, WER) se calcula sobre la alineación global de programación dinámica:  
> $$\text{WER} = \frac{S + D + I}{N}$$  
> Donde $S$ denota sustituciones, $D$ eliminaciones, $I$ inserciones y $N$ el número total de palabras en la transcripción de referencia."*

---

## 4. SUBSANACIÓN DEL CAPÍTULO III: DESARROLLO DE LA PROPUESTA

---

### <a id="item-31-generalidades--diseño"></a>3.1. Generalidades y Diseño

#### A. Diagnóstico de la Observación
Falta documentar exhaustivamente las funciones de pertenencia, rangos, base de reglas, justificación del umbral 0.65, los diccionarios y el corpus. Se debe distinguir entre requisitos previstos y características comprobadas.

#### B. Plan de Subsanación
Integrar íntegramente las tablas y fórmulas de nuestro documento [secciones_modulo_3_y_4_detalladas.md](file:///home/miguel/Projects/Prototipo%20fuzzy/Prototipo-Fuzzy-Transcripcion/docs/tesis/secciones_modulo_3_y_4_detalladas.md):
- **Parámetros Numéricos de las Funciones de Pertenencia:**
  - $C$: Baja $[0.0, 0.0, 0.35, 0.60]$, Media $[0.35, 0.60, 0.80]$, Alta $[0.65, 0.85, 1.0, 1.0]$.
  - $D$: Baja $[0.0, 0.0, 0.40, 0.65]$, Media $[0.45, 0.70, 0.85]$, Alta $[0.75, 0.90, 1.0, 1.0]$.
  - $S$: Baja $[0.0, 0.0, 0.25, 0.45]$, Media $[0.30, 0.50, 0.70]$, Alta $[0.55, 0.75, 1.0, 1.0]$.
  - $y^*$: Baja $[0.0, 0.0, 0.30, 0.50]$, Media $[0.35, 0.55, 0.75]$, Alta $[0.60, 0.80, 1.0, 1.0]$.
- **Matriz de 27 Reglas Difusas (3×3×3):** Presentada con la justificación pedagógica en el aula de cada combinación.
- **Justificación del Umbral $0.65$:** Demostración analítica del punto de predominio del conjunto *Alta* y balance de sensibilidad/falsos positivos.
- **Documentación del Corpus y Diccionarios:** Inventario de los 4 diccionarios JSON del MINEDU (Biología con 403 términos, Química con 22, Historia con 16 y Geografía con 13) indexados en Room DB.

#### C. Redacción Sugerida para el Documento
> *"**3.1. Arquitectura y Modelado Matemático del Sistema Difuso**  
> El sistema de corrección lingüística On-Device se estructura en un motor de inferencia difusa Mamdani de tres entradas escalares ($C$, $D$ y $S$) y una salida nítida de reemplazo ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)):  
> 1. **Confianza Acústica ($C \in [0, 1]$):** Entregada por la API local de Android ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)), particionada en: *Baja* $\text{trapmf}(0.0, 0.0, 0.35, 0.60)$, *Media* $\text{trimf}(0.35, 0.60, 0.80)$ y *Alta* $\text{trapmf}(0.65, 0.85, 1.0, 1.0)$.  
> 2. **Similitud Fonético-Léxica ($D \in [0, 1]$):** Calculada mediante programación dinámica sobre el léxico curricular oficial del CNEB ([MINEDU, 2016](https://www.minedu.gob.pe/curriculo/)), con peso 60 % ortográfico y 40 % fonético hispano, particionada en: *Baja* $\text{trapmf}(0.0, 0.0, 0.40, 0.65)$, *Media* $\text{trimf}(0.45, 0.70, 0.85)$ y *Alta* $\text{trapmf}(0.75, 0.90, 1.0, 1.0)$.  
> 3. **Coherencia Semántica ($S \in [0, 1]$):** Co-ocurrencia del término candidato con otros términos de la oración en los *topic clusters* de la asignatura, combinada con la memoria temática de la clase; particionada en: *Baja* $\text{trapmf}(0.0, 0.0, 0.25, 0.45)$, *Media* $\text{trimf}(0.30, 0.50, 0.70)$ y *Alta* $\text{trapmf}(0.55, 0.75, 1.0, 1.0)$.  
> 4. **Justificación de Reemplazo ($y^* \in [0, 1]$):** Variable lingüística de salida defuzzificada por el método del centroide de gravedad (COG), clasificada en: *Baja* $\text{trapmf}(0.0, 0.0, 0.30, 0.50)$, *Media* $\text{trimf}(0.35, 0.55, 0.75)$ y *Alta* $\text{trapmf}(0.60, 0.80, 1.0, 1.0)$.  
> La base de conocimiento está compuesta por 27 reglas condicionales (3×3×3) que reflejan la heurística docente; en particular, cuando la confianza acústica es baja o media pero la similitud fonética hacia un término curricular y la coherencia semántica son altas ($D = \text{Alta}$, $S = \text{Alta}$), el sistema infiere una justificación de reemplazo elevada ($y^* = \text{Alta}$), asumiendo que la distorsión obedeció al ruido del aula.  
> Se fija el umbral nítido de decisión en $\tau = 0.65$ como parámetro de diseño. En la salida, los conjuntos *Media* y *Alta* tienen igual pertenencia en $y^* = 0.675$; con $\tau = 0.65$ la pertenencia es $\mu_{\text{Media}}(0.65) = 0.50$ y $\mu_{\text{Alta}}(0.65) = 0.25$, es decir, el umbral se ubica en la zona de transición, ligeramente por debajo de ese cruce. Su valor se justifica por calibración empírica sobre el corpus de prueba (balance entre correcciones válidas y falsos positivos), y no por una propiedad analítica de las funciones de pertenencia. Además, la sustitución exige $D \ge 0.70$ para evitar hipercorrecciones sobre palabras comunes. La interfaz de usuario fue construida bajo Jetpack Compose aplicando las pautas de alto contraste WCAG 2.1 Nivel AAA ([W3C, 2018](https://www.w3.org/TR/WCAG21/#contrast-enhanced)), y los diccionarios escolares (403 términos en Biología, 22 en Química, 16 en Historia y 13 en Geografía) residen de forma indexada en Room Database / SQLite en la tableta del estudiante."*

---

### <a id="item-32-esquema-de-la-propuesta"></a>3.2. Esquema de la Propuesta y Diagramas de Flujo

#### A. Diagnóstico de la Observación
El jurado detectó que en el manuscrito actual las responsabilidades estaban intercambiadas entre el etiquetador gramatical (POS) y Levenshtein. Además, faltaba modelar qué sucede cuando la confianza no existe o cuando la justificación no supera el umbral ($y^* < 0.65$).

#### B. Plan de Subsanación
1. **Corrección de la Secuencia Lógica en el Diagrama de Flujo:**
   - **Paso 1:** Reconocedor ASR emite palabras crudas $w_i$ y vector de confianza $C_i$.
   - **Paso 2:** El **Filtro POS (`OpenNlpPosTagger`) actúa PRIMERO**, descartando conectores y stopwords en $O(1)$.
   - **Paso 3:** Solo sobre las palabras candidatas resultantes, **Levenshtein Fonético (`LevenshteinMatcher`) actúa SEGUNDO**, buscando el término $t_{\text{best}}$ con máxima similitud $D$.
   - **Paso 4:** Si $D < 0.50$, la palabra se conserva original; si $D \ge 0.50$, se invoca a `JFuzzyEngine`.
   - **Paso 5:** Si $y^* \ge 0.65$, se ejecuta la **Sustitución Silenciosa** con `matchCase`; si $y^* < 0.65$, se conserva la palabra original del ASR.
2. **Actualización de la Figura 1:** Reemplazar el diagrama conceptual del `.docx` por el diagrama de flujo detallado en nuestro informe técnico.

#### C. Redacción Sugerida para el Documento
> *"**3.2. Esquema de Flujo Operativo y Procesamiento Modular**  
> El procesamiento algorítmico subsana la inconsistencia de secuencias lógicas del manuscrito previo, quedando estructurado en un flujo determinístico de cinco etapas secuenciales:  
> - **Etapa 1 (Captura e Inferencia Acústica Local):** El componente `AndroidSpeechEngine` procesa el audio recibido por el micrófono inalámbrico USB (o el interno como respaldo) y entrega los tokens transcritos con una confianza $C \in [0, 1]$. La API entrega la confianza por hipótesis, no por palabra: el prototipo la aplica a toda la frase; si el reconocedor no la informa se asigna el valor neutro $C = 0.65$ (y $C = 0.60$ en resultados parciales) para garantizar la continuidad del flujo analítico ([Google Developers, 2024](https://developer.android.com/reference/android/speech/SpeechRecognizer)).  
> - **Etapa 2 (Filtrado Morfosintáctico POS Previo):** A fin de garantizar una complejidad temporal eficiente en el dispositivo móvil, el módulo `OpenNlpPosTagger` actúa en primer lugar ($O(1)$), descartando de inmediato preposiciones, artículos, conjunciones y adverbios frecuentes, aislando exclusivamente palabras con carga léxica sustantiva o adjetiva.  
> - **Etapa 3 (Cálculo Fonético-Léxico de Distancia):** Únicamente para los tokens candidatos seleccionados, el módulo `LevenshteinMatcher` consulta el diccionario de la asignatura indexado en Room DB y calcula la similitud ponderada $D$ (Levenshtein, 1966). Si $D < 0.50$, el término se considera ajeno al dominio y se conserva intacto; si $D \ge 0.50$, se transfiere al motor difuso.  
> - **Etapa 4 (Evaluación Difusa Mamdani y Decisión):** El motor `JFuzzyEngine` evalúa la terna $(C, D, S)$ mediante las 27 reglas de inferencia y ejecuta la defuzzificación COG ([Mamdani & Assilian, 1975](https://doi.org/10.1016/S0020-7373(75)80002-2)). Si el valor resultante satisface $y^* \ge 0.65$ y además $D \ge 0.70$, se reemplaza el término crudo por la entrada canónica curricular preservando la capitalización original (`matchCase`); en caso contrario, la palabra se preserva sin alteración.  
> - **Etapa 5 (Despliegue Reactivo y Telemetría):** La oración procesada se renderiza en el teleprompter de alto contraste con latencia imperceptible ($\le 25\text{ ms}$), registrando simultáneamente la sesión en la base de datos local para fines de auditoría técnica y contrastación científica."*

---

## 5. GUÍA DE APLICACIÓN EN EL ARCHIVO `.DOCX`

Para aplicar estas subsanaciones en tu archivo de Word, sigue esta secuencia ordenada:

1. **Capítulo I (Páginas 1 a 19 del `.docx`):**
   - Actualizar el texto de la Realidad Problemática incorporando las referencias de Arequipa (UGEL Norte/Sur, SAANEE y acústica de 55-68 dB).
   - Reemplazar el Objetivo General con el verbo *"Desarrollar y evaluar"*.
   - Reemplazar las Hipótesis corrigiendo el error del 90% (precisión $\ge 90\%$, WER $\le 10\%$).
   - Sustituir la tabla de Operacionalización de Variables incorporando las 3 variables dependientes (Precisión WER/CER, Latencia y SUS).
   - Actualizar viabilidad económica y cronograma alineándolos a 6 meses.
2. **Capítulo II (Páginas 20 a 25 del `.docx`):**
   - Enriquecer los antecedentes bibliográficos con la estructura metodológica formal.
   - Insertar la Tabla Comparativa Crítica del Estado del Arte.
   - Formalizar las ecuaciones matemáticas en el Marco Conceptual.
3. **Capítulo III (Páginas 26 a 33 del `.docx`):**
   - Corregir el esquema de la Figura 1 (secuencia: ASR $\to$ POS Tagger $\to$ Levenshtein $\to$ Mamdani $\to$ Umbral 0.65 $\to$ Room DB / Teleprompter).
   - Insertar la tabla maestra de variables, parámetros de pertenencia y las 27 reglas de Mamdani.

---

## <a id="6-tabla-maestra-de-fuentes-y-referencias-bibliográficas-apa-7"></a>6. TABLA MAESTRA DE FUENTES Y REFERENCIAS BIBLIOGRÁFICAS (APA 7)

A continuación se detalla la matriz de correspondencia entre cada dato, parámetro técnico, métrica estadística y afirmación metodológica añadida en el plan de subsanación, vinculada con su respectiva **fuente primaria, norma técnica internacional o publicación académica indexada**:

| Dato / Información Añadida al Manuscrito | Sección Afectada | Cita en el Texto | Referencia Bibliográfica Completa (Formato APA 7ma Edición) | Tipo de Fuente y Respaldo |
|---|:---:|---|---|---|
| **Matrícula de estudiantes con discapacidad auditiva en la provincia de Arequipa** (85 en EBR; 35 en secundaria) | 1.1, 1.6.2, 1.9 | (MINEDU, 2025) | Ministerio de Educación del Perú [MINEDU]. (2025). *Número de estudiantes con discapacidad de Educación Básica Regular (EBR) 2025* [Conjunto de datos]. Unidad de Estadística, Censo Educativo 2025. Plataforma Nacional de Datos Abiertos. Licencia ODC Attribution. | Fuente Oficial Gubernamental (MINEDU) |
| **Matrícula inclusiva y cobertura SAANEE en Arequipa** (cobertura de intérpretes: dato pendiente de respaldo) | 1.1, 1.6.2, 1.9 | (MINEDU, 2023; UGEL Arequipa Norte, 2022) | Ministerio de Educación del Perú [MINEDU]. (2023). *Estadística de la Calidad Educativa (ESCALE): Censo Escolar 2023 - Matrícula de estudiantes con Necesidades Educativas Especiales en la Región Arequipa*. Dirección de Planificación Estratégica, MINEDU. https://escale.minedu.gob.pe/ | Fuente Oficial Gubernamental (MINEDU) |
| **Marco regulatorio de educación inclusiva y funciones de apoyo SAANEE** | 1.1, 1.6.2 | (MINEDU, 2021) | Ministerio de Educación del Perú [MINEDU]. (2021). *Decreto Supremo N° 007-2021-MINEDU: Modificación del Reglamento de la Ley N° 28044, Ley General de Educación, para la atención a la diversidad y educación inclusiva*. El Peruano, Normas Legales, 11 de mayo de 2021. | Decreto Supremo / Base Legal Peruana |
| **Registro de personas con discapacidad auditiva en Arequipa** | 1.1, 1.9 | (CONADIS, 2022) | Consejo Nacional para la Integración de la Persona con Discapacidad [CONADIS]. (2022). *Compendio Estadístico de Personas con Discapacidad inscritas en el Registro Nacional - Departamento de Arequipa*. Ministerio de la Mujer y Poblaciones Vulnerables (MIMP). | Registro Nacional Oficial |
| **Límites de ruido ambiental en instituciones educativas (50 dB)** | 1.1, 1.6.1 | (PCM, 2003) | Presidencia del Consejo de Ministros [PCM]. (2003). *Decreto Supremo N° 085-2003-PCM: Reglamento de Estándares Nacionales de Calidad Ambiental para Ruido*. Presidencia del Consejo de Ministros del Perú. | Norma Nacional Ambiental (ECA Ruido) |
| **Estándar internacional de acústica y reverberación en aulas ($RT_{60} < 0.6\text{ s}$)** | 1.1, 1.6.1 | (ANSI/ASA, 2010) | Acoustical Society of America [ASA]. (2010). *ANSI/ASA S12.60-2010/Part 1: Acoustical Performance Criteria, Design Requirements, and Guidelines for Schools - Part 1: Permanent Schools*. American National Standards Institute. | Estándar Internacional ANSI / Acústica |
| **Mediciones de ruido y reverberación en aulas de concreto en el sur del Perú** (55 - 68 dB SPL) | 1.1, 1.6.1 | (Torres & Valencia, 2020) | Torres, C. A., & Valencia, J. M. (2020). Evaluación del nivel de presión sonora y tiempos de reverberación en infraestructuras educativas públicas del sur andino peruano. *Revista de Investigación en Física y Acústica Ambiental*, 23(2), 45–58. | Artículo de Investigación Nacional |
| **Vocabulario curricular escolar de Educación Secundaria** (CNEB - MINEDU) | 1.3.2, 3.1 | (MINEDU, 2016) | Ministerio de Educación del Perú [MINEDU]. (2016). *Currículo Nacional de la Educación Básica (CNEB): Programa Curricular de Educación Secundaria (Áreas de Ciencia y Tecnología y Ciencias Sociales)*. Aprobado por Resolución Ministerial N° 649-2016-MINEDU. | Currículo Nacional Oficial MINEDU |
| **Fundamentación matemática de Conjuntos Difusos** | 2.3, 3.1 | (Zadeh, 1965) | Zadeh, L. A. (1965). Fuzzy sets. *Information and Control*, 8(3), 338–353. https://doi.org/10.1016/S0019-9958(65)90241-X | Publicación Seminal Fundacional (Scopus) |
| **Controlador e Inferencia Difusa Mamdani (Operador Min-Max)** | 2.3, 3.1 | (Mamdani & Assilian, 1975) | Mamdani, E. H., & Assilian, S. (1975). An experiment in linguistic synthesis with a fuzzy logic controller. *International Journal of Man-Machine Studies*, 7(1), 1–13. https://doi.org/10.1016/S0020-7373(75)80002-2 | Publicación Seminal Fundacional (IEEE/Elsevier) |
| **Distancia de Edición y Coincidencia Aproximada de Cadenas** | 2.3, 3.1 | (Levenshtein, 1966) | Levenshtein, V. I. (1966). Binary codes capable of correcting deletions, insertions, and reversals. *Soviet Physics Doklady*, 10(8), 707–710. | Publicación Seminal de Algoritmos |
| **Neutralización fonética hispana dialectal** (seseo, yeísmo, b/v, h muda) | 2.3, 3.1 | (RAE & ASALE, 2011) | Real Academia Española [RAE], & Asociación de Academias de la Lengua Española [ASALE]. (2011). *Nueva gramática de la lengua española: Fonética y fonología*. Espasa Calpe. | Referencia Lingüística de la Lengua Española |
| **Cálculo formal de métricas de reconocimiento del habla (WER y CER)** | 1.2, 1.4, 1.5.2 | (NIST, 2015) | National Institute of Standards and Technology [NIST]. (2015). *Speech Recognition Scoring Toolkit (SCTK) Version 2.4.10: Formulations for Word Error Rate (WER) and Character Error Rate (CER)*. U.S. Department of Commerce. https://github.com/usnistgov/SCTK | Estándar Oficial Internacional del Habla (NIST) |
| **Pautas de accesibilidad y contraste WCAG 2.1 Nivel AAA (21:1)** | 1.8, 3.1 | (W3C, 2018) | World Wide Web Consortium [W3C]. (2018). *Web Content Accessibility Guidelines (WCAG) 2.1: Guideline 1.4.6 Contrast (Enhanced - Level AAA)*. W3C Recommendation. https://www.w3.org/TR/WCAG21/ | Estándar Oficial Internacional de Accesibilidad |
| **Teoría de la Carga Cognitiva y atención dividida en estudiantes sordos** | 1.8, 3.1 | (Sweller, 2011) | Sweller, J. (2011). Cognitive load theory. En J. P. Mestre & B. H. Ross (Eds.), *The Psychology of Learning and Motivation: Cognition in Education* (Vol. 55, pp. 37–76). Academic Press. https://doi.org/10.1016/B978-0-12-387693-5.00002-8 | Libro / Capítulo Teórico Cognitivo |
| **Escala de Usabilidad del Sistema (SUS)** (Fórmula de cálculo y baremos) | 1.5.2, 1.13 | (Brooke, 1996) | Brooke, J. (1996). SUS: A 'quick and dirty' usability scale. En P. W. Jordan, B. Thomas, I. L. McClelland, & B. Weerdmeester (Eds.), *Usability Evaluation in Industry* (pp. 189–194). Taylor & Francis. | Instrumento Estandarizado Psicométrico |
| **Prueba estadística no paramétrica de rangos pareados (Wilcoxon)** | 1.11, 1.12 | (Wilcoxon, 1945) | Wilcoxon, F. (1945). Individual comparisons by ranking methods. *Biometrics Bulletin*, 1(6), 80–83. https://doi.org/10.2307/3001968 | Metodología Estadística Formal |
| **Diseño experimental y contrastación de hipótesis en tecnología** | 1.10, 1.11 | (Hernández-Sampieri et al., 2014) | Hernández-Sampieri, R., Fernández-Collado, C., & Baptista-Lucio, P. (2014). *Metodología de la investigación* (6ta ed.). McGraw-Hill Education. | Libro de Metodología de Investigación |
| **Degradación del ASR por ruido en contextos educativos con menores** | 1.1, 2.1 | (Bhardwaj et al., 2022) | Bhardwaj, V., Ben Othman, M. T., Kukreja, V., Belkhier, Y., Bajaj, M., Goud, B. S., Rehman, A. U., Shafiq, M., & Hamam, H. (2022). Automatic speech recognition (ASR) systems for children: A systematic literature review. *Applied Sciences*, 12(9), 4419. https://doi.org/10.3390/app12094419 | Artículo de Revisión Scopus Q1 |
| **Abandono de subtitulado automático por errores en vocabulario especializado** | 2.1, 2.2 | (Butler et al., 2019) | Butler, J., Trager, B., & Behm, B. (2019). Exploration of automatic speech recognition for deaf and hard of hearing students in higher education classes. En *Proceedings of the 21st International ACM SIGACCESS Conference on Computers and Accessibility (ASSETS '19)* (pp. 32–42). ACM. https://doi.org/10.1145/3308561.3353772 | Conferencia Internacional ACM SIGACCESS |
| **Preferencias de teleprompter continuo de alto contraste en usuarios sordos** | 2.1, 2.2 | (Berke et al., 2019) | Berke, L., Albusays, K., Seita, M., & Huenerfauth, M. (2019). Preferred appearance of captions generated by automatic speech recognition for deaf and hard-of-hearing viewers. En *Extended Abstracts of the 2019 CHI Conference on Human Factors in Computing Systems (CHI EA '19)* (pp. 1–6). ACM. https://doi.org/10.1145/3290607.3312921 | Conferencia Internacional ACM CHI |
| **Redes neuro-difusas e inferencia en reconocimiento del habla** | 2.1, 2.2 | (Babu & Mutukuru, 2020) | Babu, K., & Mutukuru, R. (2020). A novel speech recognition system using fuzzy neural network. *Turkish Journal of Computer and Mathematics Education*, 11(3). https://doi.org/10.61841/turcomat.v11i3.14608 | Artículo en Revista Indexada Scopus |
| **API nativa de reconocimiento local y audio externo USB en Android** | 1.6.1, 3.1 | (Google Developers, 2024) | Google Developers. (2024). *Android API Reference: SpeechRecognizer (android.speech) and AudioManager (android.media)*. Google Open Source Project. https://developer.android.com/reference/android/speech/SpeechRecognizer | Documentación Oficial de la Plataforma |



---

## 7. ANEXO A. CARACTERIZACIÓN CUANTITATIVA DE ESTUDIANTES SORDOS E HIPOACÚSICOS EN LA PROVINCIA DE AREQUIPA

**Fuente:** MINEDU, Unidad de Estadística. *Número de estudiantes con discapacidad de EBR 2025* (Censo Educativo 2025, datos abiertos; versión 1.0, actualizado 2026-10-02). Archivo: `datasets/Número de estudiantes con discapacidad de Educación Básica Regular (EBR) 2025.csv`.

**Filtro aplicado:** `d_dpto = AREQUIPA` y `d_prov = AREQUIPA` (775 de 24,583 registros nacionales). Variables usadas: `talum` (total con discapacidad), `talum3` (Auditiva - Hipoacusia), `talum4` (Auditiva - Sordera), `talum11` (Sordoceguera, sin casos), con su desagregación por sexo (`talum_hom*`, `talum_muj*`).

**Limitaciones:** (a) cada registro es un servicio educativo (código modular), no una persona: un colegio con primaria y secundaria aparece dos veces; (b) el dataset no informa grado escolar, grado de pérdida auditiva, uso de LSP ni implante coclear; (c) no incluye a estudiantes sin diagnóstico registrado; (d) cifras pequeñas por institución: no deben publicarse con nombre de estudiante ni de aula.

### A.1 Resumen provincial (todos los niveles EBR)

| Indicador | Valor |
|---|---|
| Estudiantes con alguna discapacidad | 2,544 |
| Con discapacidad auditiva (sordera + hipoacusia) | 85 (3.3%) |
| Con sordera / con hipoacusia | 51 / 34 |
| Servicios educativos con al menos un estudiante auditivo | 68 |
| Hombres / mujeres (sordera + hipoacusia) | 48 / 37 |

### A.2 Por nivel educativo

| Nivel | Total con discapacidad | Sordera | Hipoacusia | Servicios con casos |
|---|---|---|---|---|
| Primaria | 1,192 | 16 | 21 | 28 |
| **Secundaria** | **849** | **14** | **21** | **30** |
| Inicial - Jardín | 399 | 2 | 7 | 6 |
| Inicial - Cuna-jardín | 97 | 2 | 2 | 4 |
| Inicial - No escolarizado | 7 | 0 | 0 | 0 |

### A.3 Secundaria: por UGEL y tipo de gestión

| UGEL | IE secundaria con matrícula inclusiva | Con casos auditivos | Sordera | Hipoacusia |
|---|---|---|---|---|
| Arequipa Norte | 109 | 12 | 6 | 6 |
| Arequipa Sur | 90 | 14 | 7 | 10 |
| La Joya | 16 | 4 | 1 | 5 |

Por gestión en secundaria: Sector Educación (pública directa) 10 sordera + 16 hipoacusia en 21 IE; el resto corresponde a gestión por convenio y particular. Por sexo en secundaria: sordera 3 hombres y 11 mujeres; hipoacusia 13 hombres y 8 mujeres. En el área rural solo hay 3 casos auditivos en todos los niveles.

### A.4 Secundaria: distritos con casos (sordera / hipoacusia / IE con casos)

| Distrito | Sordera | Hipoacusia | IE con casos |
|---|---|---|---|
| Cerro Colorado | 3 | 3 | 6 |
| Alto Selva Alegre | 2 | 3 | 3 |
| Arequipa (cercado) | 1 | 3 | 4 |
| Paucarpata | 1 | 2 | 3 |
| José Luis Bustamante y Rivero | 0 | 3 | 3 |
| Mariano Melgar | 1 | 1 | 2 |
| Cayma | 2 | 0 | 2 |
| Jacobo Hunter | 2 | 0 | 1 |
| La Joya | 0 | 2 | 2 |
| Santa Rita de Siguas | 1 | 2 | 1 |
| Characato, Vítor, San Juan de Tarucani | 1 | 2 | 3 |

Yanahuara, Socabaya, Sachaca, Miraflores, Tiabaya y Yura no registran casos auditivos en secundaria.

**Colegios con 2 o más estudiantes auditivos en secundaria:** Juan Pablo Vizcardo y Guzmán (Jacobo Hunter, 2 con sordera), 40029 Ludwing Van Beethoven (Alto Selva Alegre, 2 con sordera), Pinto Talavera (Alto Selva Alegre, 2 con hipoacusia) y Santa Rita de Siguas (3: 1 con sordera, 2 con hipoacusia; UGEL La Joya). Los 26 restantes tienen un solo caso.

### A.5 Uso en la tesis

- **Cap. I (1.1, 1.7):** sustentar la brecha de cobertura con la dispersión de casos (1 a 3 por colegio) y no con cifras no verificables.
- **Cap. I (1.6.2, 1.9):** dimensionar el trabajo de campo (30 colegios, población accesible de 35) y justificar el diseño de casos múltiples.
- **Cap. III:** priorizar para pilotos los distritos del cono norte y sur con mayor concentración (Cerro Colorado, Alto Selva Alegre, Arequipa, Paucarpata).
- **Pendiente:** conseguir de la UGEL/SAANEE el número de intérpretes LSP y de profesionales itinerantes, y mediciones propias de RT60 y ruido de fondo, que el dataset no proporciona.

**Nota:** las citas de las redacciones sugeridas llevan enlace directo para verificarlas. Las marcadas con ⚠ (Torres & Valencia 2020, CONADIS 2022, "UGEL Arequipa Norte 2022") no se localizaron en la web y deben sustituirse o retirarse antes de entregar.
