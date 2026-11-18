# PLAN MAESTRO DE SUBSANACIÓN 2.0 — SEGUNDA REVISIÓN DEL ASESOR
## Título: *Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De Lógica Difusa Para Transcripción Automática Como Apoyo A Estudiantes Sordos En Educación Secundaria*
**Tesista:** Bach. Miguel Angel Tinta Aguilar  
**Asesor:** Dr. Karim Guevara Puente de la Vega  
**Institución:** UNSA — Escuela Profesional de Ingeniería de Sistemas  
**Documento revisado:** `Tesis_de_Investigacion_Tinta_Aguilar_Miguel_Angel.pdf` (64 páginas, versión del 03/11/2026)  
**Plan anterior:** `plan_maestro_subsanacion_observaciones.md` (v1)  
**Fecha de este plan:** 03/11/2026

---

## ÍNDICE

0. [Qué cambia respecto al plan v1](#0-qué-cambia-respecto-al-plan-v1)
1. [Estado de aplicación de la v1 en la tesis actual](#1-estado-de-aplicación-de-la-v1-en-la-tesis-actual)
2. [Decisiones tomadas (03/11/2026)](#2-decisiones-tomadas-03112026)
3. [Ruta crítica: cinco bloques de trabajo](#3-ruta-crítica-cinco-bloques-de-trabajo)
4. [Subsanación del Capítulo I (obs. 1.1 a 1.14)](#4-subsanación-del-capítulo-i)
5. [Subsanación del Capítulo II (obs. 2.1 a 2.3)](#5-subsanación-del-capítulo-ii)
6. [Subsanación del Capítulo III (obs. 3.1 y 3.2)](#6-subsanación-del-capítulo-iii)
7. [Protocolo de evaluación completo](#7-protocolo-de-evaluación-completo)
8. [Matriz de evidencias reproducibles](#8-matriz-de-evidencias-reproducibles)
9. [Auditoría de citas y referencias](#9-auditoría-de-citas-y-referencias)
10. [Cronograma 2.0](#10-cronograma-20)
11. [Lista de verificación final antes de entregar](#11-lista-de-verificación-final-antes-de-entregar)

---

## 0. QUÉ CAMBIA RESPECTO AL PLAN V1

La segunda revisión repite casi todas las observaciones de la primera. El dictamen general del asesor deja clara la causa: **la tesis afirma cosas que todavía no demuestra**. El plan v1 contribuyó a ese problema, porque varias de sus "redacciones sugeridas" presentaban como hechos datos que no se habían medido. Esta versión cambia de enfoque:

| Principio v2 | Consecuencia práctica |
|---|---|
| **Primero la evidencia, después la redacción.** | Ninguna afirmación técnica entra en la tesis sin una prueba registrada en la matriz de evidencias (Sección 8). |
| **Separar lo previsto de lo comprobado.** | Cada requisito o característica se marca como *previsto*, *implementado* o *comprobado en dispositivo*. Lo no medido se redacta en futuro o como expectativa. |
| **Comparar contra una alternativa simple.** | La capa difusa se evalúa frente al ASR crudo **y** frente a una regla de diccionario con umbrales (línea base nítida). |
| **Medir también el daño.** | Se reportan las correcciones acertadas **y** las sustituciones que estropean palabras correctas (FCR). |
| **Corpus de prueba independiente.** | Las reglas, umbrales y diccionarios se ajustan con un corpus de desarrollo; los resultados se reportan con otro corpus que no se usó para ajustar nada. |
| **Cada afirmación cognitiva tiene su instrumento.** | Si la tesis habla de fatiga o carga cognitiva, la mide con un instrumento específico. El SUS solo mide usabilidad. |

**Frases del plan v1 que NO deben usarse** (no tienen respaldo):
- "Se demostró que al activar el modo avión el reconocedor continúa transcribiendo" (la prueba ASR-01 no está registrada).
- "Latencia inferior a 25 ms y consumo inferior al 6 % de RAM" (no hay medición en el dispositivo).
- "Torres & Valencia (2020)" y los 55–68 dB SPL atribuidos a esa fuente: **no se localizó la fuente y la tesis actual la sigue citando**.
- "Kawas et al. / Butler et al. con 18 estudiantes sordos": Butler et al. (2019) encuestaron a 26 participantes y entrevistaron a 8.
- "Si el WER supera el 10 % los alumnos abandonan la herramienta (Butler et al., 2019)": el artículo **no dice eso**. Reporta exactitudes del 85 % al 90 % con modelo de lenguaje personalizado y critica que el WER no predice el desempeño de los usuarios (Butler et al., 2019, p. 33).
- "Escala SUS validada al contexto escolar peruano": no se ha hecho ninguna validación.

---

## 1. ESTADO DE APLICACIÓN DE LA V1 EN LA TESIS ACTUAL

Leyenda: ✅ aplicado · 🟡 parcial · ❌ no aplicado · ⚠ aplicado con error o con un dato sin respaldo.

| Obs. | Sección actual en la tesis | Estado | Qué falta (resumen) |
|---|---|---|---|
| 1.1 | 1.1 (pp. 1–4) | 🟡⚠ | El dato del Censo 2025 está bien. Siguen los costos de EE.UU. sin precisar sus límites y la cita Torres & Valencia sin verificar. No hay diagnóstico propio (aulas, ruido medido, apoyos SAANEE reales). |
| 1.2 | 1.2 (p. 4) | 🟡 | La pregunta y la definición de 1 − KER están bien. Falta explicar cómo se mide y cómo se valida que KER refleja significado; se cita "Jurafsky & Martin, 2026" sin referencia. |
| 1.3.1 | 1.3.1 (p. 4) | 🟡 | Ya dice "Desarrollar y evaluar", pero falta el resultado técnico esperado y el entorno ("motores comerciales" contradice la ejecución local). |
| 1.3.2 | 1.3.2 (p. 5) | 🟡 | El OE1 (corpus y diccionarios) está bien. El OE4 promete "mitigar la fatiga cognitiva" y el OE5 propone medirla con "informes de observación": no hay instrumento. |
| 1.4 | 1.4 (p. 6) | 🟡 | Las hipótesis específicas están bien. La hipótesis general sigue mezclando WER, precisión semántica, atención dividida y fatiga, y tiene una frase cortada ("…del modelo de transcripción al."). |
| 1.5.1 | 1.5.1 (pp. 7–9) | ❌ | Sigue incluyendo Bluetooth SCO, la interfaz y el renderizado como indicadores. No define la condición con y sin corrección, no da parámetros y no trata la granularidad de C. |
| 1.5.2 | 1.5.2 (pp. 9–11) | ❌ | Mezcla precisión y usabilidad en una sola variable, sin fórmulas ni procedimiento. Fatiga y carga cognitiva sin operacionalizar. |
| 1.6.1 | 1.6.1 (p. 11) | ❌ | Tres líneas genéricas, sin ninguna prueba en el dispositivo. |
| 1.6.2 | 1.6.2 (p. 11) | ❌⚠ | Mantiene "No se requiere capacitación" y Bluetooth. |
| 1.6.3 | 1.6.3 (pp. 11–13) | ❌ | Presupuesto de 6 meses frente a un cronograma de "4 meses" que en realidad tiene 12 semanas. API Whisper sin explicar. Sin mantenimiento. |
| 1.7 | 1.7.1 (pp. 13–15) | ❌ | No explica qué aporta Mamdani frente a umbrales. Presenta los beneficios como hechos. |
| 1.8 | 1.7.2 (p. 15) | ❌ | Mantiene "libre de sobrecarga cognitiva". |
| 1.9 | 1.8 (pp. 15–17) | ❌⚠ | Incluye **Yanahuara y Miraflores, que tienen 0 casos** en secundaria según el Censo 2025. Habla de muestreo probabilístico por conglomerados con una población de 35. "Motor comercial" contra "100 % local". "Umbral del 90 %" sin fuente. |
| 1.10 | 1.9 (pp. 17) | ❌ | "Explicativo y Experimental" sin separar nivel y diseño. Extiende la causalidad a la usabilidad. |
| 1.11 | **No existe** | ❌ | No hay sección de diseño de la investigación. |
| 1.12 | 1.10 (Tabla 2) | ❌ | Tabla de 3 filas con "meta > 90 %" en la fila de WER. |
| 1.13 | dentro de 1.10 y Anexo A | 🟡 | Solo está el SUS. Faltan guía de entrevista, ficha de observación, registro de pruebas e instrumento de fatiga. |
| 1.14 | 1.11 (Tabla 3) | ❌ | Dice 4 meses (son 12 semanas). Sin permisos, pilotaje ni prueba offline antes de las pruebas con estudiantes. Sin avance real. |
| 2.1 | 2.1 (pp. 21–23) | ❌⚠ | Tiene rótulos sueltos "2020" y "2015", descripciones de una línea, autores y años inconsistentes y citas sin referencia (ver Sección 9). |
| 2.2 | 2.2 (pp. 23–25) | 🟡⚠ | La división entre intervención acústica y corrección posterior es buena. Equipara *fuzzy string matching* con lógica difusa y afirma "WER < 10 %" con Butler y Pragt. |
| 2.3 | 2.3 (pp. 25–26) | ❌ | Son definiciones de una línea, sin fórmulas ni fuentes, y no distinguen la semejanza ortográfica o fonética de la equivalencia semántica. |
| 3.1 | 3.1–3.5 (pp. 27–39) | ❌⚠ | No presenta funciones de pertenencia, reglas, umbral ni corpus. Dice jFuzzyLogic/FCL y OpenNLP, pero el código usa un motor Kotlin propio y un filtro heurístico. Bluetooth SCO. El Módulo 4 y el 5 están vacíos. |
| 3.2 | 3.2 y Tabla 4 | ❌ | El texto dice que el POS "identifica términos" y luego "evalúa la incertidumbre" antes del cálculo de D. La Tabla 4 usa "C1" e "y1*". Falta la rama "sin confianza" y la rama "sin corrección suficiente". El modelo descrito es bivariado (C, D), pero el código usa tres entradas (C, D, S). |

**Problema transversal:** la tesis describe un sistema que ya no coincide con el código. Hay que elegir una sola versión (la que se probará) y describir exactamente esa.

| Aspecto | Tesis actual | Código del repositorio |
|---|---|---|
| Captura | Bluetooth SCO, `BluetoothScoManager` | Receptor USB-C 2.4 GHz, `ExternalAudioDeviceManager` con respaldo en micrófono interno |
| Motor difuso | jFuzzyLogic + FCL | `JFuzzyEngine.kt` en Kotlin; el `.fcl` es documentación verificada por `verificar_fcl.py` |
| Entradas | C, D | C, D, S (27 reglas) |
| POS | Apache OpenNLP | Filtro heurístico (palabras vacías + sufijos) llamado `OpenNlpPosTagger` |
| Confianza | "asignada a cada palabra" | Una por hipótesis, replicada; 0.65 si falta, 0.60 en parciales |
| Decisión | y* ≥ 0.65 | y* ≥ 0.65 **y** D ≥ 0.70 **y** la palabra no es flexión del término |

---

## 2. DECISIONES TOMADAS (03/11/2026)

Estas cuatro decisiones condicionan varias secciones a la vez. Ya están tomadas y el resto del plan las aplica.

**D1. Canal de captura de audio → receptor USB-C.** Se declara el micrófono de solapa inalámbrico de 2.4 GHz con receptor USB-C (lo que implementa el código: `ExternalAudioDeviceManager`, con respaldo en el micrófono interno). **Bluetooth SCO se retira de toda la tesis**: texto, tablas, Figura 1 y 2, árbol de módulos, fragmentos de código (`startBluetoothSco()`), requisitos de hardware y justificación económica. En la viabilidad técnica se puede mencionar en una frase que se descartó porque el perfil SCO transmite voz de banda estrecha (8/16 kHz), pero sin presentarlo como prueba propia.

**D2. Fatiga y carga cognitiva → se mantienen como hipótesis exploratoria con instrumentos propios.** Se miden con NASA-TLX (Raw), escala de fatiga antes y después y prueba de comprensión, en un diseño intrasujeto que compara **ASR crudo (T0) frente a ASR con corrección difusa (T2)**. Requiere añadir a la app un interruptor "sin corrección" que solo vea el investigador. La palabra "fatiga" no aparece en ninguna hipótesis que se contraste con el SUS.

**D3. Whisper → se elimina.** El único reconocedor es el de Android (Speech Services de Google en el dispositivo). Consecuencias:
- Se retira la fila "API comercial ASR (OpenAI Whisper)" del presupuesto (−S/ 75) y toda mención a Whisper o a APIs de pago en la tesis.
- La transcripción de referencia se hace **íntegramente a mano** escuchando el audio, sin borrador automático. Partir de la salida del propio ASR de Android sesgaría la referencia a favor del reconocedor evaluado.
- En la tabla comparativa del estado del arte, los servicios en la nube quedan como alternativa descrita en la literatura, no como algo probado en esta tesis.

**D4. Horizonte temporal → entrega el 02/12/2026.** El cronograma y el presupuesto cubren **septiembre a noviembre de 2026 (3 meses, 13 semanas: 08/09–02/12)**. Las pruebas técnicas en el dispositivo se cierran **hoy, 03/11**. El piloto ya está programado. Quedan pendientes los permisos, la evaluación en vivo y la redacción de resultados (Sección 10). Para que quepa en cuatro semanas, el corpus y la muestra se reducen al mínimo defendible (Sección 7) y se prevé un plan de contingencia si los permisos se retrasan.

---

## 3. RUTA CRÍTICA: CINCO BLOQUES DE TRABAJO

El dictamen general fija este orden de prioridad. Los bloques A y B condicionan todo lo demás.

| Bloque | Contenido | Observaciones que resuelve | Entregable |
|---|---|---|---|
| **A. Evidencia técnica en el dispositivo** | Pruebas ASR-01 a ASR-04 (offline, confianza, reinicios, silencio), captura por el micrófono externo, clase continua de 45 a 90 min, latencia y memoria en la tableta real | 1.5.1, 1.6.1, 1.6.2, 1.9, 3.1 | Hoja de registro llena + capturas + logs (Sección 8) |
| **B. Núcleo difuso documentado** | Funciones de pertenencia, 27 reglas, cálculo de D y S, defuzzificación, criterio de sustitución, justificación del umbral y comparación con la regla nítida | 1.7, 2.2, 2.3, 3.1, 3.2 | Sección 3.x nueva + anexo de reglas + figura corregida |
| **C. Protocolo de evaluación** | Corpus de desarrollo y de prueba, referencia humana, condiciones, métricas, estadística | 1.2, 1.4, 1.5.2, 1.11, 1.12 | Sección 1.11 nueva + Sección 3.6 reescrita |
| **D. Instrumentos, acceso y ética** | Guía de entrevista, ficha de observación, registro de pruebas, NASA-TLX, escala de fatiga y prueba de comprensión, cartas a colegios, consentimiento, asentimiento accesible | 1.6.2, 1.9, 1.12, 1.13 | Anexos B a H |
| **E. Coherencia del documento** | Numeración según el asesor, referencias, cronograma, presupuesto, unificación de la terminología | 1.6.3, 1.14, 2.1, cierre | Documento integrado |

**Orden con plazo al 02/12:** el bloque A se hace **hoy (03/11)**. Las cartas de permiso del bloque D salen **hoy o mañana**, porque son lo que más tarda. Los bloques B y C y los instrumentos se redactan del 04 al 09/11, mientras llegan las respuestas. El bloque E corre durante todo el proceso.

### 3.1 Pruebas de hoy (03/11) en orden

Instalar el APK compilado hoy (`app/build/outputs/apk/debug/app-debug.apk`, 09:53), que ya incluye el registro por enunciado y el interruptor T0/T2. **La guía detallada paso a paso está en `plan_de_pruebas_ejecucion_v3.md` (Fase A)**; esta lista es el resumen. Anotar cada caso en la hoja de registro 8.1 de `plan_de_pruebas_ejecucion_v3.md`, con fecha, equipo y evidencia.

1. **Datos del equipo:** modelo, versión de Android y versión de "Servicios de voz de Google" (Ajustes > Apps). Captura de pantalla.
2. **HW-01:** conectar el receptor USB-C y ejecutar `adb shell dumpsys audio | grep -iE "usb|input"`. La app debe mostrar "Micrófono externo".
3. **ASR-01:** activar el modo avión y dictar las 5 frases del guion. Captura con el icono del avión visible y el texto transcrito. Si falla, instalar el paquete de español sin conexión y repetir. **Si sigue fallando, anotarlo: cambia la redacción de 1.6.1 y del alcance.**
4. **ASR-02:** con `adb logcat -s AndroidSpeechEngine:I | grep ASR02 > ASR02.txt` en marcha, dictar 20 enunciados. Anotar cuántos traen confianzas válidas (0 a 1), cuántos traen −1, 0 o `null`, y el perfil usado (local o en línea).
5. **ASR-03:** hablar 3 minutos sin parar y contar las palabras perdidas en los reinicios.
6. **ASR-04:** 60 s de silencio y reanudar.
7. **HW-02:** retirar el receptor en plena transcripción. Debe pasar al micrófono interno sin cerrarse.
8. **PERF-01:** exportar o consultar `latencyMs` del registro de sesión y abrir Android Studio Profiler (memoria) durante 5 minutos.
9. **CLASE-45:** si hoy no se puede, hacerlo durante el piloto. Registrar la batería al inicio y al final.

Guardar todo en `docs/tesis/evidencias/` con el prefijo E04 a E09.

---

## 4. SUBSANACIÓN DEL CAPÍTULO I

**Renumeración obligatoria.** Adopta la numeración del asesor para que pueda cotejar: 1.7 Justificación, 1.8 Importancia, 1.9 Alcance, 1.10 Tipo, línea y nivel, **1.11 Diseño (nueva)**, 1.12 Técnicas, 1.13 Instrumentos, 1.14 Cronograma.

---

### 1.1 Descripción de la realidad problemática

**Observación:** fortalecer el diagnóstico local (instituciones, estudiantes, condiciones acústicas) y no trasladar a Arequipa los costos y hallazgos internacionales sin precisar sus límites.

**Acciones:**
1. **Mantener** el párrafo del Censo Educativo 2025 (85 estudiantes con discapacidad auditiva en EBR, 35 en secundaria, 30 instituciones). Corregir "uno o dos estudiantes por institución" por "entre uno y tres".
2. **Retirar Torres & Valencia (2020)** y la cifra de 55–68 dB SPL. Sustituirla por una **medición propia**: nivel sonoro equivalente (L_Aeq) durante 10 minutos con un sonómetro o una app calibrada (por ejemplo, NIOSH SLM), en al menos 2 aulas de los colegios de la prueba piloto, con clase en curso y en silencio. Reportar el valor como "medición exploratoria del investigador" e indicar el instrumento. Si no se puede medir antes de la entrega, eliminar las cifras y hablar cualitativamente del ruido, citando la norma PCM (2003) **solo** para el límite normativo.
3. **Diagnóstico de apoyos reales:** enviar un oficio o hacer una entrevista breve a la coordinación SAANEE (o al CEBE que actúa como SAANEE en la UGEL) para obtener el número de profesionales itinerantes, la frecuencia de visitas y la disponibilidad de intérpretes LSP. Si no hay respuesta, redactarlo como **supuesto de contexto pendiente de verificar**, no como dato.
4. **Costos internacionales:** conservar la referencia de 60–200 USD/h de CART, pero acotarla: "en Estados Unidos (Kawas et al., 2016)… estas cifras no son directamente trasladables al Perú, donde el servicio CART es prácticamente inexistente en la educación pública; la referencia ilustra el orden de magnitud del costo de la transcripción humana".
5. **Degradación del ASR:** el 25 % de Bhardwaj et al. (2022) proviene de habla **infantil**, no de docentes adultos. Hay que precisarlo o sustituirlo por una fuente de habla adulta en aula.
6. Retirar "severa sobrecarga de procesamiento cognitivo" o atribuirla expresamente a la fuente que la sostiene (revisar si Berke et al., 2017 lo dice).

**Redacción sugerida (párrafo de cierre):**
> *"Este diagnóstico tiene límites que conviene explicitar. Las cifras de costo de la transcripción humana provienen de Estados Unidos y solo ilustran el orden de magnitud del problema; en el Perú, la transcripción en tiempo real no forma parte de los apoyos de la educación pública. Las tasas de degradación del reconocimiento por ruido reportadas en la literatura se obtuvieron con otros idiomas, hablantes y equipos, por lo que esta investigación medirá el error del reconocedor en aulas de Arequipa antes de atribuirle una magnitud concreta. Las mediciones exploratorias de ruido realizadas en [n] aulas de [colegios] registraron niveles de [x] a [y] dB(A) (sonómetro [modelo/app], 10 min por aula), superiores al valor de referencia de 35 dB(A) para aulas (ANSI/ASA, 2010)."*

---

### 1.2 Problema principal (precisión semántica)

**Observación:** precisar qué se entiende por precisión semántica y cómo se mide; el WER no demuestra por sí solo la conservación del significado.

**Estado:** la pregunta y la definición de 1 − KER ya están en la tesis. Faltan tres cosas.

**Acciones:**
1. Añadir **cómo se identifican los términos clave**: lista cerrada que se define **antes** de transcribir, a partir del diccionario de la asignatura y de la sesión de aprendizaje del docente. Un docente del área la valida.
2. Añadir la **regla de conteo**: un término clave es correcto si aparece en la posición alineada y coincide con la referencia tras normalizar mayúsculas y tildes. KER = (S_k + D_k) / N_k.
3. Añadir la **validación con jueces**: en una submuestra (≥ 60 enunciados), dos jueces (docente del área y especialista SAANEE o docente de inclusión) clasifican a ciegas cada enunciado corregido en *conserva el significado*, *lo distorsiona* o *lo pierde*. Se reporta el κ de Cohen entre jueces y la correlación de Spearman entre KER por enunciado y el juicio.
4. Declarar el límite: es una medida **léxico-conceptual**; no evalúa paráfrasis ni significado oracional completo. Butler et al. (2019) y la literatura que citan ya señalan que el WER no predice el desempeño del usuario. Úsalo como argumento.
5. Retirar "Jurafsky & Martin, 2026" o añadir la referencia completa (*Speech and Language Processing*, 3.ª ed. en borrador, con su URL y fecha de consulta).

**Texto a añadir tras la definición actual:**
> *"Los términos clave se fijan antes de la evaluación a partir del diccionario de la asignatura y de la planificación de la sesión, y son validados por el docente del área. La precisión semántica no sustituye al juicio humano: en una submuestra de enunciados, dos evaluadores independientes clasificarán a ciegas si el significado pedagógico se conserva, se distorsiona o se pierde; se reportará su concordancia (κ de Cohen) y su relación con el KER. Esta medida es léxico-conceptual y no evalúa la equivalencia de significado de oraciones completas."*

---

### 1.3.1 Objetivo general

**Observación:** el verbo debe ser desarrollar y evaluar, y debe precisar el resultado técnico y el entorno.

**Redacción sugerida:**
> *"Desarrollar y evaluar un prototipo de transcripción automática en tiempo real para tabletas Android que incorpora, sobre la salida del reconocedor de voz del dispositivo, una capa de corrección basada en inferencia difusa Mamdani y diccionarios curriculares, con el fin de reducir la tasa de error de palabra y conservar los términos curriculares de la voz del docente, en comparación con el reconocedor sin corrección, en clases de educación secundaria de instituciones públicas de la provincia de Arequipa."*

Elimina "motores comerciales": contradice la ejecución local.

---

### 1.3.2 Objetivos específicos

**Observación:** incorporar corpus y diccionarios (ya está) y **separar usabilidad de fatiga**.

**Redacción sugerida (aplica D2: fatiga con instrumento propio):**
> 1. *Construir los diccionarios curriculares de Biología, Química, Historia y Geografía a partir del CNEB y de los textos escolares del MINEDU, y un corpus de voz de docentes de secundaria de Arequipa con transcripciones de referencia revisadas, separado en un subconjunto de desarrollo y otro de prueba.*
> 2. *Configurar el reconocedor de voz del dispositivo Android en español y verificar en la tableta objetivo su funcionamiento sin conexión, la disponibilidad y granularidad de sus puntajes de confianza y la captura continua desde el micrófono inalámbrico del docente.*
> 3. *Implementar la capa de corrección con inferencia difusa Mamdani (confianza acústica, similitud fonético-ortográfica y coherencia temática) y un criterio de sustitución explícito.*
> 4. *Desarrollar una interfaz de subtitulado en formato teleprompter que cumpla criterios de accesibilidad visual (contraste, tamaño de letra, desplazamiento continuo).*
> 5. *Evaluar la precisión textual (WER, CER, KER, tasa de falsas correcciones) y el desempeño temporal de la capa difusa frente al reconocedor sin corrección y frente a una corrección por diccionario con umbrales, sobre el corpus de prueba.*
> 6. *Evaluar la usabilidad percibida del prototipo (SUS) con estudiantes con discapacidad auditiva.*
> 7. *(Exploratorio) Comparar la carga cognitiva percibida (NASA-TLX), la fatiga autorreportada y la comprensión del contenido al leer subtítulos sin y con corrección difusa.*

En el OE4 se borra "mitigar la atención dividida y la fatiga cognitiva": la interfaz aplica criterios de accesibilidad y el posible efecto sobre la carga y la fatiga lo evalúa el OE7.

---

### 1.4 Hipótesis

**Observación:** corregir la expresión del WER y el 90 %, distinguir WER final, reducción relativa y exactitud, y separar las hipótesis de implementación de las de desempeño.

**Definiciones que deben aparecer juntas (en 1.4 o en 1.5.2):**

| Magnitud | Fórmula | Meta | Qué significa |
|---|---|---|---|
| WER final | (S + D + I) / N | ≤ 10 % | Error de la transcripción corregida |
| Exactitud de palabra | 1 − WER | ≥ 90 % | La misma meta expresada como acierto. **No es una tercera meta.** |
| Reducción relativa | (WER_crudo − WER_difuso) / WER_crudo | ≥ 30 % (ajustar) | Cuánto del error del ASR elimina la capa |
| Precisión semántica | 1 − KER | ≥ 90 % | Conservación de términos clave |
| FCR | palabras correctas estropeadas / palabras correctas del ASR | ≤ 1 % | Daño causado por la capa |

> **Advertencia sobre las metas:** "WER ≤ 10 %" depende sobre todo del reconocedor y del audio, no de la capa difusa. Si el ASR crudo da un 25 % en el aula, ninguna capa léxica sobre los sustantivos llegará al 10 %, porque solo actúa sobre una fracción de las palabras. Fija la meta del WER final **después** del piloto (Bloque A) o formúlala como reducción relativa. La meta de reducción relativa del 50 % de la tesis actual se obtuvo con corpus sintéticos; en voz real es más prudente un 20 % a 30 %.

**Hipótesis de implementación** (no se contrastan estadísticamente; se verifican con pruebas de aceptación):
- **HI1:** El reconocedor del dispositivo transcribe en español sin conexión en la tableta objetivo y entrega puntajes de confianza [por hipótesis / no entrega] *(se completa con ASR-01 y ASR-02)*.
- **HI2:** El prototipo mantiene la captura y la transcripción continuas durante una clase de 45 minutos sin intervención manual.

**Hipótesis de desempeño:**
> **HG:** *La capa de corrección difusa reduce la tasa de error de palabra (WER) de la transcripción del reconocedor del dispositivo en clases de secundaria de Arequipa, sin aumentar las sustituciones erróneas de palabras correctamente reconocidas.*
>
> **HE1 (precisión textual):** *Sobre el corpus de prueba, el WER con corrección difusa es significativamente menor que el WER del ASR crudo (Wilcoxon pareado, α = 0.05), con una reducción relativa ≥ [30] % y una tasa de falsas correcciones ≤ 1 %.*
>
> **HE2 (conservación de términos):** *La precisión semántica (1 − KER) con corrección difusa es significativamente mayor que la del ASR crudo y alcanza al menos el 90 %.*
>
> **HE3 (frente a la alternativa simple):** *La corrección difusa obtiene un WER menor o igual que la corrección por diccionario con umbrales nítidos, con una tasa de falsas correcciones menor o igual.*
>
> **HE4 (desempeño temporal):** *El percentil 95 del tiempo de procesamiento de la capa difusa por enunciado es inferior a 100 ms en la tableta objetivo, y el percentil 95 de la latencia desde el fin del enunciado hasta su presentación en pantalla es inferior a 1.0 s.*
>
> **HE5 (usabilidad):** *El puntaje SUS medio de los estudiantes participantes es igual o superior a 68 puntos.*
>
> **HE6 (exploratoria):** *Al leer subtítulos con corrección difusa, los estudiantes reportan menor carga cognitiva (NASA-TLX) y menor incremento de fatiga que al leer la transcripción sin corregir.*

Sobre el umbral del SUS: 68 es el promedio de referencia (Sauro, 2011; Bangor et al., 2009). La tesis pone 75 en 1.4 y 70 en 3.6.2. Elige un único valor y justifícalo.

---

### 1.5.1 Variable independiente

**Observación:** definir la condición con y sin corrección, dar parámetros, separar los indicadores del algoritmo de la captura y la interfaz, y comprobar la disponibilidad y granularidad real de la confianza.

**Acciones:**
1. Reemplazar la sección actual por la redacción de la v1 (apartado 1.5.1, "Redacción sugerida"), que ya es correcta, con estos ajustes:
   - Nombre: *"Capa de corrección difusa post-reconocimiento"*, con tres niveles: **T0** ASR crudo, **T1** corrección por diccionario con umbrales nítidos (línea base simple) y **T2** corrección difusa Mamdani.
   - Incluir la tabla de parámetros de la v1 (funciones de pertenencia, 27 reglas, τ = 0.65, D ≥ 0.70, C neutra = 0.65, etc.) con la columna "Fuente en el código".
   - **Dimensiones solo del algoritmo:** entradas (C, D, S), inferencia (activación de reglas), defuzzificación (y*) y decisión (τ, piso de D, tasa de sustitución).
   - **Captura e interfaz** pasan a *variables controladas* (micrófono, distancia, perfil ASR, tableta) y a la VD de usabilidad respectivamente.
2. **Granularidad de C (indispensable):** sustituir la frase actual "asignadas a cada palabra" por el resultado real de la prueba ASR-02:
   - *Si la tableta entrega confianza:* "La API entrega un valor por hipótesis en [rango observado]; en n = [20] enunciados, [k] devolvieron un valor válido, con media [x] y desviación [s]. El prototipo asigna ese valor a todas las palabras del enunciado."
   - *Si no la entrega (−1, 0 o ausente):* "En la tableta objetivo, el reconocedor no informa confianza; C toma el valor constante 0.65 y no discrimina. En ese escenario, la decisión depende de D y S." En ese caso la tesis **debe** reconocer que la ventaja de Mamdani frente a la regla nítida se apoya en S, y la comparación T1/T2 se vuelve central.
3. Mencionar como trabajo futuro la confianza por palabra (p. ej., motores como Vosk o whisper.cpp con marcas por token), sin presentarla como implementada.

---

### 1.5.2 Variable dependiente

**Observación:** separar precisión textual, desempeño temporal y usabilidad, con fórmulas y procedimientos; operacionalizar carga cognitiva y fatiga sin inferirlas del SUS.

**Acción:** reemplazar la sección por **cuatro dimensiones**, cada una con su tabla de indicadores, fórmula, instrumento, procedimiento y escala.

**D1. Precisión textual** (unidad de análisis: enunciado del corpus de prueba; comparación T0 frente a T1 frente a T2 sobre **la misma salida del ASR**)

| Indicador | Fórmula | Unidad |
|---|---|---|
| WER | (S + D + I) / N × 100 | % |
| CER | (S_c + D_c + I_c) / N_c × 100 | % |
| Reducción relativa | (WER_T0 − WER_Tk) / WER_T0 × 100 | % |
| KER y precisión semántica | KER = (S_k + D_k) / N_k; PS = 1 − KER | % |
| Correcciones acertadas (TCR) | errores del ASR corregidos / errores del ASR en palabras candidatas | % |
| Falsas correcciones (FCR) | palabras correctas del ASR que la capa cambió / palabras correctas del ASR | % |
| Precisión de la sustitución | sustituciones correctas / sustituciones totales | % |

*Procedimiento:* normalizar los textos (minúsculas, sin puntuación, números en letras y la misma regla de tildes en referencia e hipótesis), alinear con Levenshtein por palabras (`jiwer` o SCTK `sclite`), calcular por enunciado y en total, y reportar media, mediana e IC 95 % por bootstrap.

**D2. Desempeño temporal** (tableta objetivo, modelo y versión de Android declarados)

| Indicador | Fórmula | Instrumento |
|---|---|---|
| Tiempo de la capa difusa | t_fin_corrección − t_onResults (ms) | Marcas de tiempo `SystemClock.elapsedRealtimeNanos()` en el log de sesión |
| Latencia de presentación | t_render − t_onEndOfSpeech (s) | Log de sesión + verificación con video a 60 fps de pantalla y docente sobre una muestra |
| Cumplimiento | % de enunciados con latencia < 1.0 s | Log |

Se reportan la media, la mediana y el **percentil 95**. Se descartan los 10 primeros enunciados (calentamiento).

**D3. Usabilidad**

| Indicador | Cálculo |
|---|---|
| SUS (0–100) | Ítems impares: r − 1; pares: 5 − r; suma × 2.5 |
| Ítems Likert complementarios (1–5): legibilidad, velocidad del texto, comodidad, distracción | Media por ítem y % de respuestas 4–5 |

**D4. Carga cognitiva y fatiga (exploratoria)**

| Indicador | Instrumento | Cálculo |
|---|---|---|
| Carga cognitiva percibida | NASA-TLX, versión Raw (6 subescalas 0–100) (Hart & Staveland, 1988; Hart, 2006) | Media de las 6 subescalas; también demanda mental, esfuerzo y frustración por separado |
| Esfuerzo mental | Escala de Paas (1–9) (Paas, 1992) | Al final de cada bloque |
| Fatiga | Escala visual analógica 0–10 antes y después de cada condición | ΔF = F_post − F_pre |
| Comprensión | Prueba de 8 a 10 preguntas sobre el contenido de la clase, elaborada con el docente | % de aciertos |

*Procedimiento:* el mismo estudiante lee dos clases equivalentes, una con T0 y otra con T2, en orden contrabalanceado y sin saber qué condición ve. Se comparan con Wilcoxon pareado y se reporta el tamaño del efecto r. **El SUS no se usa para inferir fatiga.**

*Accesibilidad de los instrumentos:* se presentan por escrito con lenguaje simple, apoyo visual (pictogramas en la escala de fatiga) y, si el estudiante usa LSP, con un mediador que explique las instrucciones sin sugerir respuestas. Se hace un piloto de comprensión con 1 o 2 estudiantes (obs. 1.13).

*Límite que hay que declarar:* con 3 a 8 participantes, la D4 es exploratoria. Sus resultados describen tendencias en los casos estudiados y no se generalizan.

---

### 1.6.1 Viabilidad técnica

**Observación:** verificar en el dispositivo objetivo el reconocimiento offline en español, la confianza disponible, la compatibilidad de bibliotecas y la captura. `EXTRA_PREFER_OFFLINE` no basta.

**Acciones (Bloque A, sin ellas esta sección no se puede escribir):**

| Prueba | Procedimiento | Criterio | Evidencia a adjuntar |
|---|---|---|---|
| ASR-01 Offline | Modo avión, Wi-Fi y datos apagados; dictar las 5 frases del guion | Transcribe | Captura de pantalla con el modo avión visible + logcat |
| ASR-01b Paquete | Ajustes > Idioma > Reconocimiento sin conexión: paquete español instalado | Paquete presente | Captura |
| ASR-01c Ruta local | Con Android 13+, comprobar `SpeechRecognizer.isOnDeviceRecognitionAvailable()` o `createOnDeviceSpeechRecognizer`; si no, documentar qué servicio atiende (`dumpsys`) | Servicio local identificado | Salida de `adb shell dumpsys` |
| ASR-02 Confianza | Registrar `CONFIDENCE_SCORES` crudo de 20 enunciados. La app ya escribe una línea `ASR02` por enunciado (añadida el 03/11): `adb logcat -s AndroidSpeechEngine:I \| grep ASR02 > ASR02.txt` | Anotar si es válido, rango y variabilidad | `ASR02.txt` |
| ASR-03 Continuidad | Hablar 3 minutos sin parar | Pérdida < 2 % de palabras en los reinicios | Audio de referencia + transcripción |
| ASR-04 Silencio | 60 s de silencio y reanudación | Reanuda en < 500 ms | Log |
| HW-01 Micrófono | Conectar el receptor USB-C; verificar `TYPE_USB_DEVICE/HEADSET` | La app muestra "Micrófono externo" | `dumpsys audio` + captura |
| HW-02 Desconexión | Retirar el receptor en plena clase | Vuelve al micrófono interno sin cerrarse | Log |
| CLASE-45 | Sesión real o simulada de 45 min | Sin cierres; batería y temperatura registradas | Log + % de batería inicio/fin |
| PERF-01 | Tiempo de la capa difusa y memoria en la tableta | p95 < 100 ms; memoria registrada | Log + Android Studio Profiler |

**Redacción:** escribir en pasado **solo** lo que pasó, con modelo de tableta, versión de Android, versión de Speech Services y fecha. Ejemplo:
> *"En la tableta [marca/modelo] (Android [x], Speech Services [versión]) se verificó el [fecha] que, con el modo avión activo y el paquete de español sin conexión instalado, el reconocedor transcribió los [5] enunciados de prueba (Anexo [x], Figura [x]). El reconocedor [entregó / no entregó] puntajes de confianza: [resultado de ASR-02]. El motor difuso está implementado en Kotlin sin dependencias externas en tiempo de ejecución; su equivalencia con la especificación FCL del Anexo [x] se verificó numéricamente en 4913 puntos (diferencia máxima 3 × 10⁻¹⁶)."*

Si alguna prueba falla (por ejemplo, el equipo solo reconoce en línea), **se declara** y se ajusta el alcance: "el prototipo funciona sin conexión en [equipos que pasaron la prueba]".

---

### 1.6.2 Viabilidad operativa

**Observación:** concretar el acceso a colegios, participantes, autorizaciones, instalación y acompañamiento; comprobar la escucha continua en clases prolongadas; no asumir que no hace falta capacitación.

**Acciones:**
1. **Acceso:** lista de colegios candidatos tomada del Anexo A del plan v1 (los de 2 o más casos y los distritos con más casos), con el estado de cada gestión: carta enviada, respuesta y fecha. Distingue **prevista** de **realizada**.
2. **Ruta de autorización:** dirección de la IE → coordinación SAANEE/CEBE → docente de aula → padres o apoderados (consentimiento) → estudiante (asentimiento accesible). Consultar si la UNSA exige aprobación de un comité de ética para estudios con menores y, si es así, tramitarla.
3. **Instalación y acompañamiento:** quién instala la app, dónde se guarda la tableta, quién la carga y la presencia del investigador en las sesiones de prueba.
4. **Capacitación:** eliminar "No se requiere capacitación". Sustituirlo por una inducción de unos 10 minutos con cartilla visual para el docente (colocación del micrófono) y para el estudiante (materia, tamaño de letra), y registrar en la ficha de observación las incidencias de uso.
5. **Continuidad:** citar el resultado de CLASE-45 (Bloque A). El auto-reinicio del reconocedor se describe como **mecanismo implementado**, y la ausencia de cortes como **resultado comprobado o pendiente**.

---

### 1.6.3 Viabilidad económica

**Observación:** resolver la diferencia entre 6 meses y 4 meses, explicar la API externa e incluir mantenimiento y operación.

**Acciones:**
1. Igualar el periodo con el cronograma (D4): **3 meses (septiembre a noviembre de 2026)**. El rubro de Internet pasa a 3 × 70 = S/ 210. La frase "presupuesto real consolidado para los seis (6) meses" pasa a "tres (3) meses".
2. **Eliminar la fila de la API Whisper** (D3). La fila "API comercial gratuita ASR de Android" se renombra como "Reconocedor de voz del sistema Android (Speech Services), en el dispositivo, sin costo". En el texto se retira "(como OpenAI Whisper API o Google Cloud Speech API)" o se deja solo como ejemplo de lo que **no** se usa. Recalcular los totales.
3. Añadir **mantenimiento y operación**: pilas o batería del micrófono, espuma o protector, cable USB-C/OTG, cargador, funda protectora de la tableta y reposición estimada. Añadir el costo del micrófono **real** (SX26/Maxtron) con su boleta.
4. Separar tres totales: *bienes propios* (no es inversión nueva), *inversión de la investigación* y **costo de operación por aula** si se adopta el sistema (tableta + micrófono + mantenimiento anual). Este último es el dato que interesa a un colegio.
5. Quitar "100 % On-Device" de esta sección hasta que el Bloque A lo confirme.

---

### 1.7 Justificación

**Observación:** precisar qué aporta Mamdani frente a la corrección léxica con umbrales simples; presentar los beneficios educativos y la independencia de servicios externos como expectativas.

**Acciones:**
1. Añadir un párrafo técnico basado en **la evidencia del repositorio** y no en adjetivos:
   - Una regla nítida decide con un corte duro sobre cada variable (p. ej., D ≥ 0.75 y C ≤ 0.60). El sistema Mamdani combina gradualmente C, D y S, de modo que una similitud media puede compensarse con una coherencia temática alta y viceversa, en lugar de exigir que cada variable supere su corte por separado.
   - Resultado preliminar (corpus **sintéticos**, dentro de la muestra; `benchmark_runner.py --booleana`): con confianza neutra 0.65 (el caso realista), la mejor regla booleana obtiene un WER de 4.85 % y 3.45 % en los dos corpus, y la difusa completa un 2.91 % y 1.72 %. Con confianzas sintéticas informativas, la booleana empata o gana en un corpus (2.91 % frente a 1.94 %, y 0.00 % frente a 1.72 %). **Conclusión honesta:** la ventaja no está demostrada y es justamente lo que la hipótesis HE3 debe contrastar con voz real.
2. Reescribir en condicional o como expectativa: "se espera favorecer el acceso…", "podría reducir la dependencia de servicios externos…".
3. En la justificación social, quitar "fatiga cognitiva acumulada" como hecho, o atribuirla a una fuente concreta.

---

### 1.8 Importancia

**Observación:** moderar "libre de sobrecarga cognitiva" y diferenciar el acceso textual de mejoras demostradas en autonomía, comprensión o aprendizaje.

**Acción:** sustituir la primera oración por:
> *"La importancia del estudio radica en ofrecer un apoyo de acceso textual, en tiempo real y sin costo de operación, al discurso oral del docente para estudiantes con discapacidad auditiva en el aula regular. El acceso al texto es una condición necesaria, pero no suficiente, para la comprensión y el aprendizaje; esta investigación evalúa la calidad de ese acceso (precisión, latencia y usabilidad) y explora la carga cognitiva y la fatiga percibidas, pero no mide efectos sobre el rendimiento académico ni la autonomía del estudiante, que quedan como líneas de trabajo futuro."*

Retira "libre de sobrecarga cognitiva", "precisa" y "permanente".

---

### 1.9 Alcance

**Observación:** precisar los colegios y participantes disponibles, justificar estratos y conglomerados, distinguir la selección prevista de la realizada y unificar el motor comercial con la ejecución local.

**Acciones:**
1. **Corregir los distritos.** Yanahuara y Miraflores tienen 0 estudiantes con discapacidad auditiva en secundaria (Censo 2025). Usar los distritos con casos: Cerro Colorado (6), Alto Selva Alegre (5), Arequipa (4), Paucarpata (3), José Luis Bustamante y Rivero (3), Mariano Melgar (2), Cayma (2) y Jacobo Hunter (2).
2. **Eliminar el muestreo probabilístico por conglomerados estratificados.** Con una población de 35 estudiantes repartidos en 30 colegios (1 a 3 por colegio), no hay base para estratificar ni para inferir a la población. Sustituirlo por un **muestreo no probabilístico intencional por criterios y accesibilidad**, con un diseño de casos múltiples.
3. Separar las **dos poblaciones**:
   - *Corpus de voz:* docentes de secundaria (no requieren estudiantes sordos en el aula). Meta: ≥ 3 docentes, Biología, ≥ 45 min de habla en total.
   - *Usuarios:* estudiantes con discapacidad auditiva y lectoescritura funcional en castellano. Meta: 3 a 8.
4. **Tabla de selección prevista frente a realizada** (colegio, UGEL, n.º de estudiantes según el Censo, estado del permiso, participantes que finalmente intervinieron). Se completa a medida que avanzan las gestiones.
5. **Unificar el motor:** "reconocedor de voz del sistema Android (Speech Services de Google) ejecutado en el dispositivo". Retirar "API comercial avanzada".
6. Retirar "umbral del 90 % de precisión, nivel mínimo exigido por los alumnos" o citar la fuente exacta que lo diga (Butler et al. no lo dice).

---

### 1.10 Tipo, línea y nivel

**Observación:** separar el nivel explicativo del diseño experimental, justificar el alcance causal y no extender la causalidad algorítmica a la usabilidad o la carga cognitiva.

**Redacción sugerida (nivel):**
> *"El nivel es explicativo respecto a la precisión textual: se manipula la presencia de la capa de corrección (T0, T1, T2) sobre exactamente la misma salida del reconocedor, de modo que cualquier diferencia en WER, CER y KER es atribuible a la capa y no al audio, al hablante o al equipo. Esta inferencia causal se limita al comportamiento del algoritmo. La usabilidad se estudia en un nivel descriptivo, y la carga cognitiva y la fatiga en un nivel exploratorio mediante una comparación intrasujeto, sin pretender generalizar a la población."*

El diseño va en 1.11, no aquí.

---

### 1.11 Diseño de la investigación (sección nueva)

**Observación:** desarrollar el protocolo completo (corpus, hablantes, duración, asignaturas, condiciones acústicas, referencia humana, estadística) y comparar los mismos resultados ASR antes y después del procesamiento difuso.

Desarrollar con el contenido de la **Sección 7** de este plan. Resumen para el documento:
- **Vía 1, evaluación algorítmica:** diseño experimental de medidas repetidas sobre el mismo corpus. Cada enunciado del corpus de prueba se evalúa en T0, T1 y T2 a partir de **la misma hipótesis del ASR registrada**.
- **Vía 2, evaluación con usuarios:** estudio descriptivo de usabilidad y comparación intrasujeto exploratoria T0 frente a T2 (carga cognitiva, fatiga, comprensión).

---

### 1.12 Técnicas

**Observación:** concretar su aplicación, definir cómo se obtienen y revisan las transcripciones de referencia y especificar cómo se analizan las entrevistas y cómo complementan las métricas.

**Tabla sugerida:**

| Técnica | Aplicación concreta | Quién / cuándo | Análisis |
|---|---|---|---|
| Registro y evaluación computacional | La app guarda por enunciado el texto crudo, el texto corregido, C, D, S, y* y las marcas de tiempo; los scripts calculan las métricas | Investigador, en cada sesión del corpus | WER/CER/KER/FCR, Wilcoxon, bootstrap |
| Transcripción de referencia | Revisor 1 transcribe a mano escuchando el audio (sin borrador automático) → revisor 2 revisa sobre el mismo audio → consenso de discrepancias. Guía de transcripción escrita (Anexo C) | 2 revisores, después de cada grabación | WER entre revisores (acuerdo) antes del consenso |
| Encuesta | SUS + ítems Likert; NASA-TLX + escala de fatiga + prueba de comprensión tras cada condición | Estudiante, durante y al final de la sesión | Estadística descriptiva; Wilcoxon pareado T0 frente a T2 |
| Observación | Ficha estructurada durante la sesión (incidencias técnicas, interrupciones, conducta de lectura) | Investigador | Frecuencias |
| Entrevista semiestructurada | Docente, especialista SAANEE y estudiante (con mediación si usa LSP) | Al final | Análisis temático (Braun & Clarke, 2006): codificación inicial, temas, matriz que cruza temas con métricas |

**Cómo complementan las entrevistas a las métricas:** cada tema se cruza con un resultado cuantitativo (por ejemplo, "las palabras corregidas mal confunden más que las no corregidas" con la FCR; "el texto llega tarde" con el p95 de latencia). Se presenta en una tabla de triangulación.

---

### 1.13 Instrumentos

**Observación:** presentar la guía de entrevista, la ficha de observación y el registro de pruebas; revisar su comprensión y accesibilidad para los participantes; añadir una medida específica si se mantiene la fatiga.

**Anexos que hay que redactar:**

| Anexo | Instrumento | Contenido mínimo |
|---|---|---|
| A | Cuestionario SUS | 10 ítems de Brooke en español, escala 1–5, instrucciones en lenguaje simple. Indicar que se usa la traducción de [fuente] y que se pilotea su comprensión. |
| B | Registro de pruebas técnicas | Hoja de la Sección 8: caso, equipo, fecha, resultado, valor y evidencia |
| C | Guía de transcripción de referencia | Reglas de normalización, cómo marcar habla ininteligible, solapamientos y muletillas, y procedimiento de consenso |
| D | Ficha de registro experimental | Por enunciado: ID, asignatura, hablante, duración, N, S, D, I, C, D, S, y*, decisión, tiempos |
| E | Ficha de observación de aula | Contexto (aula, ruido medido, n.º de alumnos), incidencias técnicas con hora, interrupciones, distancia del micrófono, notas |
| F | Guía de entrevista | 6 a 8 preguntas para el docente, 6 a 8 para el estudiante (versión con apoyo visual), 4 a 6 para SAANEE |
| G | NASA-TLX + escala de fatiga + prueba de comprensión | Versión en español con apoyo visual |
| H | Consentimiento informado (padres) y asentimiento accesible (estudiante) | Texto en lenguaje simple con pictogramas; opción de explicación en LSP |

**Validación de los instrumentos:** juicio de expertos (3 jueces: docente de inclusión, especialista en LSP o SAANEE y metodólogo) sobre claridad y pertinencia, más un piloto de comprensión con 1 o 2 estudiantes. Se documentan los cambios que resulten.

---

### 1.14 Cronograma

**Observación:** corregir los encabezados repetidos, asignar fechas concretas, señalar el avance real e incluir permisos, pilotaje y comprobación offline antes de la evaluación con estudiantes.

Ver **Sección 10**. En el documento, añadir una columna **"Estado al [fecha]"** (Hecho / En curso / Pendiente) y colocar la fila de *comprobación técnica offline* y la de *pilotaje* **antes** de la fila de evaluación con estudiantes.

---

## 5. SUBSANACIÓN DEL CAPÍTULO II

### 2.1 Antecedentes

**Observación:** incorporar métodos, muestras, idiomas, resultados y limitaciones; corregir las citas sin referencia y las inconsistencias de autores y años.

**Acciones:**
1. Eliminar los rótulos sueltos "2020" y "2015". Organizar por **ejes**: (a) corrección post-ASR, (b) lógica difusa en habla y PLN, (c) ASR como apoyo a estudiantes sordos, (d) presentación de subtítulos. Dentro de cada eje, ordenar cronológicamente.
2. Para cada antecedente, una **ficha de 5 campos**, redactada después en un párrafo:

| Campo | Ejemplo con Butler et al. (2019), verificado en el PDF |
|---|---|
| Objetivo y método | Estudio cualitativo de las percepciones del ASR en clases universitarias; cuestionario y entrevistas |
| Muestra e idioma | 26 estudiantes sordos o con hipoacusia encuestados y 8 entrevistados; inglés; RIT/NTID (EE.UU.) |
| Tecnología | ASR comercial en la nube con modelo de lenguaje personalizado por curso; micrófono de diadema |
| Resultados | Exactitud observada del 85 % al 90 %; preocupación por la exactitud de los términos del curso y la legibilidad |
| Limitación / relación con esta tesis | Educación superior, en la nube, en inglés; no mide WER propio. Sustenta la relevancia de los términos técnicos y el límite del WER como única métrica |

3. Hacer lo mismo con Anshor y Wiyatno (2025), Kawas et al. (2016), Berke et al. (2017, 2019), Stefanenko et al. (2022), Babu y Mutukuru (2020), Kasttet et al. (2024), Jun et al. (2026) y Kim et al. (2024), leyendo los PDF de `articulos extra/`. **No copiar cifras de memoria ni del plan v1** (allí había errores: "18 estudiantes", "reducción del 40 %").
4. Añadir **al menos 1 o 2 antecedentes nacionales** reales (repositorios de la UNSA, la PUCP, la UNI o ALICIA/CONCYTEC: "reconocimiento de voz español", "discapacidad auditiva tecnología"), con su URL. Si no los hay, decirlo con la búsqueda realizada ("se consultó ALICIA y RENATI el [fecha] con los términos…; no se hallaron trabajos que…").
5. Cerrar con una **tabla resumen** (autor, año, idioma, muestra, método, métrica, resultado, limitación) y un párrafo de brecha.

### 2.2 Estado del arte

**Observación:** comparar críticamente las alternativas, sustentar las afirmaciones sobre precisión mínima, abandono de herramientas y presentación visual, y no equiparar la coincidencia aproximada de cadenas con la inferencia difusa.

**Acciones:**
1. **Distinción conceptual explícita (párrafo nuevo):** *"El término 'fuzzy' se usa en dos sentidos distintos. La coincidencia aproximada de cadenas (fuzzy string matching) es un procedimiento que mide la semejanza entre secuencias de caracteres, típicamente mediante distancias de edición (Levenshtein, 1966), sin relación con la teoría de conjuntos difusos. La inferencia difusa (Zadeh, 1965; Mamdani & Assilian, 1975) es un formalismo de razonamiento con grados de pertenencia y reglas lingüísticas. En esta propuesta, la distancia de edición produce un número (D) que se convierte en una entrada del sistema de inferencia, junto con C y S; la decisión la toma el sistema de inferencia, no la distancia."* Revisar con este criterio Kasttet et al. (2024) y Gupta et al. (2018).
2. **Afirmaciones que hay que sustentar o retirar:**
   - "WER < 10 % es necesario para la aceptación (Butler; Pragt)": verificar en Pragt et al. (2022). Butler no lo dice. Si ninguna fuente lo sostiene, reformular como meta de diseño propia.
   - "Resaltar dudas fragmenta la atención (Berke 2017, 2019; Xu & Qi 2024)": revisar lo que Berke et al. encontraron realmente sobre el marcado de confianza; en estudios con usuarios sordos las preferencias fueron mixtas. Xu y Qi tratan textos literarios, no subtítulos: no sirven como fuente aquí.
   - "Soluciones 100 % On-Device (Iglesias et al., 2014; ZainEldin et al., 2024)": comprobar si esas fuentes hablan de ejecución local.
3. **Tabla comparativa crítica** (de la v1, apartado 2.2), quitando las celdas sin respaldo ("Alto manejo de incertidumbre") y añadiendo dos columnas: *corrección con modelos de lenguaje o neuronales (p. ej., Jun et al., 2026)* y *diccionario + umbrales*. Añadir una fila de "evidencia disponible" (qué se midió y con qué datos).

### 2.3 Marco conceptual

**Observación:** desarrollar funciones de pertenencia, reglas Mamdani, defuzzificación, similitud léxica y métricas; distinguir la semejanza ortográfica o fonética de la equivalencia semántica; vincular cada definición con fuentes.

**Acciones:** insertar la redacción de la v1 (apartado 2.3, con las fórmulas de trimf, trapmf, MIN–MAX, centroide, Levenshtein normalizado y WER) y **añadir**:
1. **Ejemplo numérico completo** de inferencia: una palabra real del corpus (p. ej., *sedula*), con C, D y S, sus grados de pertenencia, las reglas activadas, la salida agregada y el y* resultante. Se puede generar con `evaluation/interactive_tester.py`.
2. **Similitud fonético-ortográfica**: la fórmula exacta del código, D = 0.6·D_ort + 0.4·D_fon, con las reglas de neutralización fonética y su fuente (RAE y ASALE, 2011).
3. **Coherencia temática S**: definición por co-ocurrencia en grupos temáticos (*topic clusters*) y memoria de la clase (de `factor_semantico_topic_clusters.md`).
4. **Párrafo de distinción:** *"Dos palabras pueden ser ortográfica o fonéticamente muy similares sin ser semánticamente equivalentes (célula/cédula, D ≈ 0.83) y dos expresiones pueden ser equivalentes en significado sin parecerse en forma (célula/unidad básica de la vida). D mide semejanza de forma; S aproxima la pertenencia temática, no el significado; ninguna de las dos mide equivalencia semántica. Por eso la conservación del significado se valida con jueces humanos (sección 1.2)."*
5. Definiciones de CER, KER, FCR, SUS y NASA-TLX, cada una con su cita.

---

## 6. SUBSANACIÓN DEL CAPÍTULO III

### 3.1 Generalidades y diseño

**Observación:** especificar funciones de pertenencia, rangos, base de reglas y justificación del umbral 0.65; documentar corpus y diccionarios; presentar evidencias de funcionamiento y distinguir requisitos previstos de características comprobadas.

**Acciones:**
1. **Nueva sección 3.x "Modelo difuso"** con:
   - Tabla de las 4 variables lingüísticas y sus funciones de pertenencia (parámetros de la v1, apartado 3.1) + **figura de las funciones** (generarla con matplotlib a partir de los parámetros).
   - **Tabla completa de las 27 reglas** (C × D × S → y*) con una frase de justificación por grupo de reglas (en un anexo si ocupa mucho).
   - Operadores (MIN, MAX), implicación (MIN, recorte), defuzzificación (centroide, 100 pasos).
   - **Criterio de sustitución:** y* ≥ 0.65 ∧ D ≥ 0.70 ∧ no es una flexión. Se explican las tres condiciones.
   - **Justificación del umbral:** no es analítica. Hay que mostrar el **barrido de sensibilidad** (`benchmark_runner.py --umbral`): con el corpus de desarrollo, los resultados son idénticos entre 0.60 y 0.75 y aparece un falso positivo con ≤ 0.55. Se elige 0.65 como punto central de la zona estable. **Repetir el barrido con el corpus de desarrollo real** y reportarlo.
2. **Corpus y diccionarios:** tabla con el número de términos por asignatura (Biología 403, Química 22, Historia 16, Geografía 13), su fuente (texto o página del MINEDU), el proceso de curación y quién validó. **Advertencia:** Química, Historia y Geografía tienen muy pocos términos; o se amplían o se restringe la evaluación a Biología y se declara.
3. **Tabla "previsto / implementado / comprobado"** para cada requisito (offline, captura externa, servicio en primer plano, latencia, contraste AAA, exportación de apuntes) con la referencia a la evidencia de la Sección 8.
4. **Unificar con el código:** retirar Bluetooth SCO, jFuzzyLogic en tiempo de ejecución y "OpenNLP" (el filtro es heurístico, y hay que decirlo con esas palabras o renombrar el componente). Actualizar el árbol de módulos y los fragmentos de código, que hoy muestran `startBluetoothSco()`.
5. **Completar el Módulo 4 y el 5**, hoy vacíos (p. 38–39).
6. **Verificar el contraste:** amarillo #FFFF00 sobre negro da 19.6:1, no 21:1 (21:1 es blanco sobre negro). Cualquiera de los dos cumple AAA (≥ 7:1). Corregir la cifra.
7. **Evidencias de funcionamiento:** capturas de la app en la tableta (pantalla de subtítulos, indicador de micrófono externo y modo sin conexión), salida de la suite de pruebas (68 de 68) y verificación FCL. Todo con fecha.

### 3.2 Esquema de la propuesta

**Observación:** corregir las responsabilidades intercambiadas entre el etiquetador y Levenshtein, uniformar C con y*, y representar qué ocurre sin confianza o sin corrección suficientemente sustentada.

**Acciones:**
1. **Rehacer la Figura 1** como diagrama de flujo **por palabra**:
   ```
   ASR → (texto, C de la hipótesis)
     ├─ ¿C disponible? ── no → C := 0.65 (neutra)   [rama "sin confianza"]
     ▼
   Filtro de candidatas (palabras vacías / sufijos)  ── no candidata → conservar
     ▼
   Similitud D con el diccionario (Levenshtein ortográfico + fonético)
     ├─ D < 0.50 o sin candidato → conservar          [rama "sin término cercano"]
     ▼
   Coherencia temática S (clusters + memoria de clase)
     ▼
   Inferencia Mamdani (C, D, S) → y*  (hasta 3 candidatos; gana el de mayor y*)
     ▼
   ¿y* ≥ 0.65 y D ≥ 0.70 y no es flexión?
     ├─ no → conservar palabra del ASR                [rama "corrección no sustentada"]
     └─ sí → sustituir (conservando mayúsculas)
     ▼
   Registro (crudo, corregido, C, D, S, y*, tiempos) → Teleprompter
   ```
2. **Corregir el texto del 3.2:** el filtro **selecciona candidatas**; Levenshtein **calcula D**; el motor difuso **evalúa la incertidumbre**. Hoy el texto atribuye al POS "identificar términos clave y evaluar la incertidumbre" antes de calcular D.
3. **Notación única:** C es **entrada** (confianza acústica) e y* es **salida** (grado de justificación de la sustitución). Retirar "C1", "y1*" y la expresión "y*0.65" de los títulos (p. 34). Usar la misma notación en las figuras, las tablas, el texto y el código mostrado.
4. Rehacer la **Figura 2** (arquitectura) con los componentes reales (`ExternalAudioDeviceManager`, `AudioForegroundService`, `AndroidSpeechEngine`, `DecisionEvaluator`, `JFuzzyEngine`, `LevenshteinMatcher`, `LessonContext`, Room, `TeleprompterScreen`).

---

## 7. PROTOCOLO DE EVALUACIÓN COMPLETO

Este es el contenido base de la sección 1.11 y de la 3.6.

### 7.1 Corpus

| Elemento | Especificación mínima |
|---|---|
| Hablantes | **Mínimo 3 docentes** de secundaria (1 para desarrollo y ≥ 2 para prueba), con consentimiento para grabar su voz. Las sesiones de la evaluación en vivo cuentan como corpus de prueba si el docente consiente |
| Asignaturas | **Biología** (es el único diccionario con volumen suficiente: 403 términos). Química, Historia y Geografía quedan fuera de la evaluación y se declara como limitación |
| Duración | **≥ 45 min de habla en total** (≈ 5 000–6 000 palabras; ≈ 15 min por docente). Basta para cientos de enunciados pareados |
| Condiciones acústicas | Aula real; ruido medido (L_Aeq, dB(A)) al inicio de cada sesión; distancia micrófono-boca ≈ 15–20 cm; se anotan los eventos de ruido |
| Registro | Audio original (WAV, grabador independiente o canal del receptor) **y** salida en vivo del ASR por enunciado con marcas de tiempo |
| Partición | **Desarrollo** (≈ 30 %: 1 o 2 docentes) para ajustar diccionarios, clusters y umbrales; **prueba** (≈ 70 %: los demás docentes, nunca vistos) solo para reportar. Dividir **por hablante**, no por frase |
| Congelación | Antes de procesar el conjunto de prueba se congela la versión del código y los diccionarios (etiqueta de git + hash) y se anota en la tesis |

> **Requisito de código (implementado el 03/11/2026):** la app guarda un CSV por sesión con una fila por enunciado: texto crudo, texto corregido, texto mostrado, confianza cruda de la API, C usada, perfil ASR, fuente de audio, marcas de tiempo (inicio de voz, primer parcial, fin de voz, resultado, fin de la corrección, publicación) y el detalle de D, S e y* por palabra evaluada (`UtteranceLogger.kt`). Tiene además un interruptor oculto T0/T2 en el panel del investigador y guarda el SUS con el código del participante. El análisis lo hace `evaluation/evaluar_sesiones.py` (T0/T1/T2, WER, CER, KER, TCR, FCR, Wilcoxon exacto, bootstrap, latencia, confianza, SUS, NASA-TLX). Procedimiento de uso: `plan_de_pruebas_ejecucion_v3.md`.

### 7.2 Transcripción de referencia

1. El revisor 1 transcribe a mano escuchando el audio, sin borrador automático (D3), siguiendo la guía del Anexo C. Tiempo estimado: unos 4 a 6 minutos de trabajo por minuto de audio. Para 45 minutos de audio son de 3 a 5 horas por revisor; hay que reservarlas en el cronograma.
2. El revisor 2 revisa la transcripción del revisor 1 de forma independiente sobre el mismo audio.
3. Se calcula el **WER entre revisores** antes del consenso (meta < 5 %) y se resuelven las discrepancias por consenso, con el audio delante.
4. Se marcan los términos clave según la lista cerrada.
5. Los revisores no ven la salida T1 ni la T2 (cegamiento).

### 7.3 Condiciones comparadas (misma salida ASR)

| Condición | Descripción |
|---|---|
| T0 | ASR crudo registrado |
| T1 | Diccionario + umbrales nítidos: sustituir si D ≥ t_D (y C ≤ t_C si hay confianza). Los umbrales se optimizan **con el conjunto de desarrollo** |
| T2 | Capa difusa completa (C, D, S + memoria + rerank) con los parámetros congelados |
| (Ablación) | T2 sin S; T2 sin memoria. Opcional, para discutir qué aporta cada componente |

T1 y T2 se aplican en diferido al mismo `rawText` mediante el arnés (paridad Kotlin↔Python verificada) o en la app con el registro activado.

### 7.4 Métricas y análisis

- WER, CER, KER, TCR, FCR y precisión de la sustitución (Sección 4, 1.5.2-D1), en total y por enunciado.
- **Prueba de hipótesis:** Wilcoxon de rangos con signo pareado por enunciado (T0 frente a T2, T1 frente a T2), α = 0.05, con corrección de Holm para comparaciones múltiples. Tamaño del efecto r = Z/√n.
- **IC 95 % por bootstrap** (10 000 remuestreos por enunciado) para el WER y la reducción relativa.
- Análisis de errores: tabla de las 20 sustituciones más frecuentes (acertadas y erróneas) con su C, D, S e y*.
- Para las falsas correcciones, lista completa en un anexo (exigencia del asesor).

### 7.5 Estudio con usuarios

| Elemento | Especificación |
|---|---|
| Participantes | 3 a 8 estudiantes con discapacidad auditiva y lectoescritura funcional |
| Sesión | Inducción (10 min) → condición A 15–20 min → instrumentos → descanso → condición B → instrumentos → SUS → entrevista |
| Contrabalanceo | La mitad empieza con T0 y la otra mitad con T2; contenido equivalente en ambas (dos sesiones de Biología del mismo docente o dos mitades de una misma clase) |
| Análisis | SUS descriptivo (media, DE, mediana, valores individuales); Wilcoxon pareado en NASA-TLX, ΔF y comprensión (exploratorio); análisis temático de las entrevistas |
| Ética | Autorización de la IE, consentimiento del apoderado, asentimiento accesible, derecho a retirarse, anonimización (código E01…), sin video del rostro salvo autorización expresa |

---

## 8. MATRIZ DE EVIDENCIAS REPRODUCIBLES

El asesor pide "evidencias reproducibles de implementación" para el siguiente avance. Cada fila debe tener su archivo en el repositorio (carpeta sugerida: `docs/tesis/evidencias/`).

| ID | Evidencia | Cómo se obtiene | Archivo | Estado al 03/11/2026 |
|---|---|---|---|---|
| E01 | Pruebas unitarias 68/68 | `./gradlew testDebugUnitTest` | `evidencias/E01_tests.txt` | Hecho (2026-11-01); guardar la salida |
| E02 | Equivalencia FCL ↔ Kotlin | `python3 evaluation/verificar_fcl.py` | `E02_fcl.txt` | Hecho; guardar la salida |
| E03 | Benchmark sintético + booleana + umbral | `benchmark_runner.py`, `--booleana`, `--umbral` | `E03_*.txt` | Hecho; **etiquetar como sintético y dentro de la muestra** |
| E04 | ASR-01 offline (modo avión) | Plan SX26 | Capturas + logcat | **Hoy 03/11** |
| E05 | ASR-02 confianza | Línea `ASR02` en logcat | `ASR02.txt` | **Hoy 03/11** |
| E06 | ASR-03/04 continuidad y silencio | Plan SX26 | Log + audio | **Hoy 03/11** |
| E07 | HW-01/02 micrófono USB y desconexión | Plan SX26 | `dumpsys` + capturas | **Hoy 03/11** |
| E08 | CLASE-45 | Sesión de 45 min | Log + batería | **Hoy 03/11** (o en el piloto, si hoy no hay 45 min disponibles) |
| E09 | PERF-01 latencia y memoria en la tableta | Log de sesión + Profiler | CSV + captura | **Hoy 03/11** |
| E10 | Mediciones de ruido en el aula | Sonómetro/app | Ficha E | Pendiente |
| E11 | Corpus de desarrollo y prueba + referencias | Sección 7 | `evaluation/corpus_real/` | Pendiente |
| E12 | Resultados T0/T1/T2 sobre el conjunto de prueba | Arnés | Reporte + CSV | Pendiente |
| E13 | Cartas, permisos, consentimientos | Gestión | PDF escaneados (sin datos personales en el repositorio) | Pendiente |
| E14 | Versión congelada del código | `git tag tesis-eval-v1` | Hash en la tesis | Pendiente (tras el piloto) |
| E15 | Registro por enunciado + interruptor T0/T2 + script de análisis | 71/71 pruebas unitarias; prueba VER-01 en la tableta | `UtteranceLogger.kt`, `evaluar_sesiones.py` | Código hecho (03/11); falta VER-01 en la tableta |

Ninguna cifra de E04 a E12 debe aparecer en la tesis antes de tener el archivo correspondiente.

---

## 9. AUDITORÍA DE CITAS Y REFERENCIAS

Revisión de la versión actual (texto frente a lista de referencias, pp. 47–50):

**Citadas en el texto pero ausentes de la lista de referencias:**
Stinson & Elliot (2002) · Iglesias et al. (2014) · Lasecki et al. (2012) · MINEDU (2016, 2021, 2025) · Presidencia del Consejo de Ministros (2003) · ANSI/ASA (2010) · Jurafsky & Martin (2026) · Anshor & Wiyatno (2025) · Shahnawazuddin et al. (2019) · Mirhassani & Ting (2014) · Sun et al. (2001) · Jun et al. (2026) · Pragt et al. (2022) · ZainEldin et al. (2024) · Torres & Valencia (2020) (**retirar: no verificable**).

**Inconsistencias de autor o año:**

| En el texto | Problema | Corrección |
|---|---|---|
| Kawas et al. (2016) y (2018) | La lista dice 2018; el artículo es de ASSETS '16 | Kawas et al. (2016) en todas partes |
| Khudonogova et al. (2022) | La lista tiene como primera autora a Stefanenko | Stefanenko et al. (2022) |
| Kasttet y Kachkari (2024) | Hay 5 autores | Kasttet et al. (2024) |
| Xu y Qi (2024) y (2026) | Dos años distintos | Unificar (la lista dice 2026) y revisar su pertinencia |
| Berke et al. (2019) | La lista omite a Huenerfauth | Berke, Albusays, Seita y Huenerfauth (2019) |
| Butler et al. (2019) "WER > 10 % → abandono" | El artículo no lo afirma | Retirar o citar la fuente correcta |

**En la lista pero no citadas (verificar o retirar):** Hosseini et al. (2020), Olwal et al. (2020), Orzan et al. (2025), Seita et al. (2022).

**Formato:** completar los DOI en APA 7 (Butler: 10.1145/3308561.3353772; Berke 2019: 10.1145/3290607.3312921; Bhardwaj: 10.3390/app12094419, etc.), ordenar alfabéticamente (Brooke está al final) y aplicar sangría francesa.

**Nuevas referencias que introduce este plan** (verificar cada una antes de citarla): Hart & Staveland (1988); Hart (2006); Paas (1992); Bangor, Kortum & Miller (2009); Sauro (2011); Braun & Clarke (2006); Wilcoxon (1945); Zadeh (1965); Mamdani & Assilian (1975); Levenshtein (1966).

---

## 10. CRONOGRAMA 2.0

Horizonte: **08/09/2026 – 02/12/2026 (13 semanas)**. La tabla reemplaza a la Tabla 3 de la tesis y elimina los encabezados de mes repetidos. Las semanas S1 a S8 conservan las fechas de la tabla actual y se marca su estado real. La columna "Estado" está actualizada al 03/11/2026.

| Fase | Semana | Fechas | Actividad | Entregable | Estado al 03/11 |
|---|---|---|---|---|---|
| 1. Fundamentación | S1–S3 | 08/09–28/09 | Análisis de trabajos similares; Cap. II; Cap. III (generalidades, esquema, arquitectura) | Borrador de Cap. II–III | Hecho (en corrección por la 2.ª revisión) |
| 2. Desarrollo | S4–S7 | 29/09–26/10 | Módulos 1–5, motor difuso Mamdani, diccionarios, migración del micrófono a USB-C | APK + 68 pruebas unitarias OK | Hecho |
| 3. Pruebas en laboratorio | S7–S8 | 20/10–02/11 | Benchmark con corpus sintético, comparación con la regla booleana, barrido de umbral, verificación FCL | E01–E03 | Hecho (resultados sintéticos, dentro de la muestra) |
| 4. **Verificación técnica en el dispositivo** | S9 | **03/11** | ASR-01 a 04, HW-01/02, PERF-01 y CLASE-45 en la tableta objetivo, con el receptor USB-C | E04–E09 + hoja de registro llena | **Hoy** |
| 5. **Permisos** | S9–S10 | 03/11–14/11 | Cartas a la dirección de la IE y a SAANEE (03–04/11); consentimiento de docentes; consentimiento de apoderados y asentimiento de estudiantes; consultar si la UNSA exige comité de ética | E13 | Pendiente: **las cartas salen el 03–04/11** |
| 6. Ajustes de código | S9 | 03/11 | Registro por enunciado (crudo, corregido, tiempos) + CSV + interruptor oculto T0/T2 + SUS guardado + script de análisis | APK de evaluación + `evaluar_sesiones.py` | **Hecho** (falta VER-01 en la tableta) |
| 7. Redacción de correcciones | S9–S10 | 04/11–12/11 | Cap. I–III según este plan (prioridad: 1.4, 1.5, 1.6, 1.9, 1.11, 3.1, 3.2); Anexos A–H | Tesis corregida | Pendiente |
| 8. Instrumentos | S10 | 07/11–12/11 | Juicio de 3 expertos (por correo, 48 h) y ajustes; congelar versión del código (tag) | Instrumentos validados + E14 | Pendiente |
| 9. **Piloto** | S10–S11 | [fecha programada] | Sesión piloto: grabación del docente (corpus de desarrollo), prueba de instrumentos con 1 estudiante, medición de ruido, ficha de observación | Corpus dev + ajustes del protocolo | **Programado** |
| 10. **Evaluación en vivo** | S11–S12 | 16/11–24/11 | Sesiones en aula con ≥ 2 docentes (corpus de prueba) y 3–8 estudiantes: T0/T2 contrabalanceado, NASA-TLX, fatiga, comprensión, SUS, entrevistas | Datos crudos + audio | Pendiente (depende de la fase 5) |
| 11. Referencia humana | S11–S12 | 17/11–25/11 | Transcripción manual por 2 revisores, en paralelo a la fase 10 | Referencias con consenso | Pendiente |
| 12. **Resultados** | S12–S13 | 24/11–28/11 | Cálculo T0/T1/T2, estadística, análisis temático, triangulación; redacción del Cap. IV, conclusiones y recomendaciones | Cap. IV + E12 | Pendiente |
| 13. Cierre | S13 | 29/11–02/12 | Resumen, abstract, índices, referencias, revisión con el asesor y entrega | Tesis final | Entrega 02/12 |

**Reglas de secuencia:** la fase 4 (offline) y la 9 (piloto) van **antes** de la 10. Los parámetros se congelan **antes** de procesar el corpus de prueba. La fase 11 corre en paralelo a la 10: cada sesión grabada se transcribe ese mismo día o al siguiente.

**Hitos que no se pueden mover:**
- 04/11: cartas entregadas.
- 06/11: APK de evaluación listo.
- 14/11: permisos recibidos.
- 24/11: fin de la recolección.
- 28/11: Cap. IV completo.
- 02/12: entrega.

**Plan de contingencia si los permisos para menores no llegan el 14/11:**
1. La **evaluación algorítmica no depende de estudiantes**. Basta con grabar a los docentes con su consentimiento. Las hipótesis HE1 a HE4 se pueden contrastar igual.
2. Para usabilidad y carga cognitiva (HE5 y HE6), aplicar el protocolo con los estudiantes que sí tengan autorización, aunque sean solo 2 o 3, y reportarlo como estudio de casos.
3. Si no hay ninguno, dejar los instrumentos validados y el protocolo en la tesis y declarar HE5 y HE6 como **no contrastadas por falta de autorización**. No sustituir a los estudiantes sordos por oyentes sin decirlo, porque cambiaría la población.

**Presupuesto:** debe cubrir los mismos 3 meses (ver 1.6.3).

---

## 11. LISTA DE VERIFICACIÓN FINAL ANTES DE ENTREGAR

**Consistencia técnica**
- [ ] No queda "Bluetooth SCO" en ninguna parte (D1): texto, tablas, figuras, árbol de módulos, código mostrado, requisitos de hardware.
- [ ] No queda "Whisper" ni "API comercial" en ninguna parte (D3); el presupuesto se recalculó sin esa fila.
- [ ] No queda "jFuzzyLogic" como motor en tiempo de ejecución ni "OpenNLP" como etiquetador real.
- [ ] El modelo descrito es trivariado (C, D, S) en el Cap. I, II y III y en las figuras.
- [ ] La confianza C se describe como una por hipótesis, con el resultado de ASR-02.
- [ ] El criterio de sustitución tiene tres condiciones, iguales en todo el documento.
- [ ] Contraste: 19.6:1 (amarillo/negro) o se cambia a blanco/negro para 21:1.

**Metodología**
- [ ] WER ≤ 10 %, exactitud 1 − WER, reducción relativa y precisión semántica aparecen definidas por separado, y "WER del 90 %" no aparece en ninguna parte (incluida la Tabla 2).
- [ ] Hay hipótesis de implementación separadas de las de desempeño.
- [ ] La variable dependiente tiene 3 o 4 dimensiones con fórmula, instrumento y procedimiento.
- [ ] La fatiga solo aparece si tiene un instrumento (D2).
- [ ] Existe la sección 1.11 Diseño con corpus, partición dev/test, referencia, T0/T1/T2 y estadística.
- [ ] La FCR y la lista de falsas correcciones están previstas.
- [ ] El alcance usa distritos con casos y muestreo intencional, y distingue lo previsto de lo realizado.

**Evidencia y honestidad**
- [ ] Cada afirmación en pasado sobre el funcionamiento del prototipo remite a una evidencia E01–E14.
- [ ] Los resultados sintéticos, si se mencionan, están rotulados como "corpus simulado, dentro de la muestra".
- [ ] Los beneficios educativos están redactados como expectativas.

**Forma**
- [ ] Numeración según el asesor (1.7 a 1.14).
- [ ] Índices de figuras, tablas y fórmulas llenos; la fórmula del WER aparece en el 3.6.1 (hoy falta, p. 40).
- [ ] Todas las citas del texto están en la lista y viceversa; APA 7 con DOI.
- [ ] Cronograma y presupuesto con el mismo periodo, con fechas y estado real.
- [ ] Anexos A–H presentes.
- [ ] Sin frases cortadas (hipótesis general) ni rótulos sueltos ("2020", "2015").
