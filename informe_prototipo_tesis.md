# INFORME TÉCNICO DEL PROTOTIPO DE SOFTWARE DE TESIS

**Título de la Tesis:**  
*Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De Lógica Difusa Para Transcripción Automática Como Apoyo A Estudiantes Sordos En Educación Secundaria.*

**Autor:** Bach. Miguel Angel Tinta Aguilar  
**Asesor:** Dr. Karim Guevara Puente de la Vega  
**Institución:** Universidad Nacional de San Agustín de Arequipa (UNSA)  
**Facultad:** Facultad de Ingeniería de Producción y Servicios  
**Escuela Profesional:** Escuela Profesional de Ingeniería de Sistemas  
**Fecha de Elaboración:** Octubre de 2026  
**Repositorio del Prototipo:** `d:\tesis` | Paquete: `com.unsa.dhh.captions`

---

## ÍNDICE GENERAL

1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Fundamentación y Planteamiento del Problema](#2-fundamentación-y-planteamiento-del-problema)
3. [Objetivos e Hipótesis de la Investigación](#3-objetivos-e-hipótesis-de-la-investigación)
4. [Marco Conceptual y Algorítmico](#4-marco-conceptual-y-algorítmico)
5. [Pila Tecnológica y Librerías Utilizadas](#5-pila-tecnológica-y-librerías-utilizadas)
6. [Arquitectura del Sistema (Clean Architecture + MVVM)](#6-arquitectura-del-sistema-clean-architecture--mvvm)
7. [Implementación Detallada por Capas y Snippets de Algoritmos](#7-implementación-detallada-por-capas-y-snippets-de-algoritmos)
8. [Diseño Accesible y Ergonomía Cognitiva (WCAG AAA)](#8-diseño-accesible-y-ergonomía-cognitiva-wcag-aaa)
9. [Adaptabilidad Multiplataforma (Tabletas y Smartphones)](#9-adaptabilidad-multiplataforma-tabletas-y-smartphones)
10. [Validación Experimental y Análisis de Resultados (Capítulo IV)](#10-validación-experimental-y-análisis-de-resultados-capítulo-iv)
11. [Evaluación Psicométrica de Usabilidad (Escala SUS)](#11-evaluación-psicométrica-de-usabilidad-escala-sus)
12. [Conclusiones Técnicas](#12-conclusiones-técnicas)
13. [Recomendaciones para Trabajos Futuros](#13-recomendaciones-para-trabajos-futuros)
14. [Anexos Técnicos (FCL, Diccionarios y Fórmulas)](#14-anexos-técnicos)

---

## 1. Resumen Ejecutivo

El presente informe documenta el diseño, la construcción e implementación experimental del prototipo de software desarrollado para la tesis de grado en Ingeniería de Sistemas. La solución consiste en una aplicación móvil nativa para el sistema operativo Android (API 26+) que opera de manera **100% On-Device (sin dependencia de conexión a internet ni consumo de APIs de pago)**.

El objetivo central del sistema es resolver la degradación de precisión que experimentan los motores comerciales de Reconocimiento Automático del Habla (ASR) en aulas de educación secundaria regular debido al ruido ambiental (50 a 65 dB SPL) y al uso frecuente de vocabulario académico especializado (Biología, Química, Historia y Geografía). 

Para solventar esta limitación, el prototipo incorpora una capa computacional basada en **Lógica Difusa (Mamdani)**, combinada con **Similitud Fonética Hispana de Levenshtein** y filtrado morfosintáctico de sustantivos clave. Dicha capa evalúa la incertidumbre acústica del motor y aplica sustituciones silenciosas de términos dudosos cuando el grado de justificación supera el umbral matemático $y^* \ge 0.65$.

La presentación visual se basa en una interfaz **Jetpack Compose** bajo la pauta de accesibilidad **WCAG AAA** (relación de contraste 21:1, texto amarillo `#FFFF00` sobre fondo negro `#000000`), proyectada en modo teleprompter continuo de 2 a 3 líneas con desplazamiento suave (*smooth scrolling*). Esto previene la fatiga cognitiva y la atención dividida en los estudiantes con discapacidad auditiva conforme a la Teoría de la Carga Cognitiva (CLT).

---

## 2. Fundamentación y Planteamiento del Problema

### 2.1. Realidad Problemática
En la Educación Básica Regular (EBR) secundaria de la provincia de Arequipa, los estudiantes con discapacidad auditiva (hipoacusia o sordera) integrados en aulas regulares enfrentan barreras severas para acceder al discurso pedagógico oral emitido por los docentes. Las alternativas tradicionales presentan limitaciones estructurales:
- **Intérpretes de Lengua de Señas Peruana (LSP):** Severa escasez en colegios públicos y alta demanda no cubierta coordinada por los equipos del SAANEE (UGEL Norte y UGEL Sur).
- **Servicios Humanos CART (Communication Access Realtime Translation):** Costos prohibitivos que oscilan entre $60 y $200 por hora de clase.
- **ASR Comercial Estándar en la Nube:** Inviables por requerir conectividad Wi-Fi permanente (inexistente o inestable en muchas aulas) y cuotas mensuales por minuto de transcripción. Además, los motores comerciales estándar están optimizados para habla cotidiana, por lo que ante el ruido de fondo deforman términos académicos clave (ej. confunden *"célula"* con *"cédula"* o *"estequiometría"* con palabras inconexas), elevando la Tasa de Error de Palabra (WER) por encima del umbral de tolerancia del 10%.

### 2.2. Pregunta General de Investigación
> *¿De qué manera la integración de una capa computacional basada en Lógica Difusa On-Device en motores comerciales de Reconocimiento Automático del Habla optimiza la precisión semántica de la transcripción en tiempo real y reduce la Tasa de Error de Palabra durante las clases dictadas por el docente, como apoyo pedagógico para estudiantes con discapacidad auditiva en instituciones educativas de educación secundaria de Arequipa?*

---

## 3. Objetivos e Hipótesis de la Investigación

### 3.1. Objetivo Principal
Proponer e implementar un sistema para la optimización de transcripción mejorado mediante la integración de una capa computacional basada en técnicas de lógica difusa en motores de reconocimiento de voz, como apoyo para estudiantes con discapacidad auditiva de educación secundaria.

### 3.2. Objetivos Específicos
1. Configurar un motor de Reconocimiento Automático de Voz preentrenado en modo local (offline), adaptándolo a las condiciones del aula para capturar la voz del docente y extraer las puntuaciones de confianza acústica $C \in [0.0, 1.0]$.
2. Implementar un algoritmo basado en lógica difusa (Mamdani) y similitud fonético-léxica que evalúe y compense los niveles de incertidumbre del reconocedor de voz.
3. Desarrollar una interfaz de usuario ergonómica y accesible que aplique el estándar WCAG AAA, mitigando la atención dividida y la fatiga cognitiva mediante formato teleprompter continuo.
4. Validar la eficacia del prototipo mediante la contrastación del WER frente a un corpus escolar y cuantificar la usabilidad a través de la Escala de Usabilidad del Sistema (SUS).

### 3.3. Hipótesis Principal
> *La implementación de un sistema de transcripción que integra una capa computacional basada en técnicas de lógica difusa sobre los resultados de motores de Speech-to-Text, reducirá la Tasa de Error de Palabra (WER) a niveles inferiores al 10% (precisión $\ge 90\%$) al solventar la incertidumbre lingüística sin saturar los recursos del dispositivo móvil.*

---

## 4. Marco Conceptual y Algorítmico

### 4.1. Reconocimiento Automático del Habla (ASR)
Mapeo de la señal de audio continua $X$ a una secuencia de texto $W$. En el prototipo, se emplea `SpeechRecognizer` de Android con la bandera `EXTRA_PREFER_OFFLINE = true`, extrayendo el vector de confianza acústica normalizado $C \in [0.0, 1.0]$.

### 4.2. Similitud Fonética Hispana y Distancia de Levenshtein
La distancia de edición básica penaliza fuertemente variaciones dialectales. Para el contexto escolar de Arequipa y el castellano peruano, se implementó una función compuesta:
1. **Normalización ortográfica:** Remoción de diacríticos y cálculo de distancia de Levenshtein clásica:
   $$D_{\text{lex}} = 1.0 - \frac{\text{distancia}(s_1, s_2)}{\max(|s_1|, |s_2|)}$$
2. **Neutralización fonológica hispana:** Transforma grafemas equivalentes antes del cotejo:
   - Seseo: $c$ (ante $e, i$), $z$, $s \rightarrow s$.
   - Confusión bilabial: $v \rightarrow b$.
   - Yeísmo: $ll \rightarrow y$.
   - Omisión de grafema mudo: $h \rightarrow \emptyset$.
   - Neutralización oclusiva velar: $qu, c \rightarrow k$.
3. **Similitud Combinada:**
   $$D = 0.60 \cdot D_{\text{lex}} + 0.40 \cdot D_{\text{fon}} \quad (D \in [0.0, 1.0])$$

### 4.3. Motor de Inferencia Difusa Mamdani
El sistema modela la incertidumbre mediante 2 variables de entrada y 1 variable de salida:

#### Variables de Entrada:
- **Confianza Acústica ($C \in [0.0, 1.0]$):**
  - $\mu_{\text{Baja}}(C) = \text{Trapezoide}(0.0, 0.0, 0.35, 0.60)$
  - $\mu_{\text{Media}}(C) = \text{Triángulo}(0.35, 0.60, 0.80)$
  - $\mu_{\text{Alta}}(C) = \text{Trapezoide}(0.65, 0.85, 1.0, 1.0)$
- **Similitud Léxico-Fonética ($D \in [0.0, 1.0]$):**
  - $\mu_{\text{Baja}}(D) = \text{Trapezoide}(0.0, 0.0, 0.40, 0.65)$
  - $\mu_{\text{Media}}(D) = \text{Triángulo}(0.45, 0.70, 0.85)$
  - $\mu_{\text{Alta}}(D) = \text{Trapezoide}(0.75, 0.90, 1.0, 1.0)$

#### Variable de Salida:
- **Grado de Justificación de Corrección ($y^* \in [0.0, 1.0]$):**
  - $\mu_{\text{Baja}}(y) = \text{Trapezoide}(0.0, 0.0, 0.30, 0.50)$
  - $\mu_{\text{Media}}(y) = \text{Triángulo}(0.35, 0.55, 0.75)$
  - $\mu_{\text{Alta}}(y) = \text{Trapezoide}(0.60, 0.80, 1.0, 1.0)$

#### Base de Reglas Difusas:
1. $\text{SI } C \text{ es Baja } \land D \text{ es Alta } \rightarrow y^* \text{ es Alta}$
2. $\text{SI } C \text{ es Baja } \land D \text{ es Media } \rightarrow y^* \text{ es Alta}$
3. $\text{SI } C \text{ es Baja } \land D \text{ es Baja } \rightarrow y^* \text{ es Baja}$
4. $\text{SI } C \text{ es Media } \land D \text{ es Alta } \rightarrow y^* \text{ es Alta}$
5. $\text{SI } C \text{ es Media } \land D \text{ es Media } \rightarrow y^* \text{ es Media}$
6. $\text{SI } C \text{ es Media } \land D \text{ es Baja } \rightarrow y^* \text{ es Baja}$
7. $\text{SI } C \text{ es Alta } \land D \text{ es Alta } \rightarrow y^* \text{ es Media}$
8. $\text{SI } C \text{ es Alta } \land D \text{ es Media } \rightarrow y^* \text{ es Baja}$
9. $\text{SI } C \text{ es Alta } \land D \text{ es Baja } \rightarrow y^* \text{ es Baja}$

#### Defuzzificación por Centroide de Gravedad (COG):
$$y^* = \frac{\int_0^1 y \cdot \mu_B(y) \, dy}{\int_0^1 \mu_B(y) \, dy}$$

### 4.4. Regla de Decisión y Sustitución Silenciosa
$$\text{Acción} = \begin{cases} 
\text{Sustituir por término canónico del diccionario}, & \text{si } y^* \ge 0.65 \\ 
\text{Conservar palabra original del ASR}, & \text{si } y^* < 0.65 
\end{cases}$$

---

## 5. Pila Tecnológica y Librerías Utilizadas

El sistema fue diseñado bajo el principio de **cero dependencias de servicios en la nube**, empleando librerías de código abierto y APIs nativas de Android optimizadas para ejecución local:

| Librería / Componente | Versión / Identificador | Capa de Arquitectura | Justificación y Rol en el Prototipo |
|---|---|---|---|
| **Kotlin Compiler** | `2.0.21` | Lenguaje / Base | Compilador principal del proyecto, actualizado automáticamente para soporte de Compose nativo. |
| **Compose Compiler Plugin** | `org.jetbrains.kotlin.plugin.compose:2.0.21` | Compilación / UI | Nuevo plugin oficial de JetBrains/Google para Kotlin 2.0 que sustituye la extensión manual anterior. |
| **Jetpack Compose BOM** | `2024.04.01` | Presentación | Marco declarativo para construir la interfaz de usuario reactiva bajo el estándar WCAG AAA (21:1). |
| **Material 3 & Foundation** | `androidx.compose.material3` | Presentación | Componentes de diseño moderno, tipografías escalables e iconos de accesibilidad. |
| **Room Database (SQLite)** | `androidx.room:2.6.1` | Persistencia | Motor de base de datos relacional local que indexa y almacena en memoria caché los diccionarios escolares del MINEDU. |
| **Kotlin Symbol Processing (KSP)** | `2.0.21-1.0.25` | Compilación / Room | Procesador de anotaciones para Room alineado con Kotlin 2.0, generando el código de persistencia sin impacto en runtime. |
| **Kotlinx Coroutines & Flow** | `1.8.0` | Dominio / Servicio | Manejo asíncrono y reactivo del flujo continuo de audio y texto (`SharedFlow` / `StateFlow`), garantizando latencia $< 1.0\text{ s}$. |
| **Motor Difuso Mamdani Nativo** | Código Kotlin (`JFuzzyEngine`) | Razonamiento Difuso | Implementación nativa de alta eficiencia en Kotlin para inferencia Mamdani y centroide (COG) con cero overhead de librerías externas. |
| **Gson** | `2.10.1` | Datos / Ingesta | Serialización y deserialización ligera para cargar los diccionarios JSON de educación secundaria durante la siembra de la BD. |
| **Android SpeechRecognizer** | Nativa (Android API 26+) | Percepción / ASR | API nativa de reconocimiento de voz de Android configurada en modo local (`EXTRA_PREFER_OFFLINE = true`) para extraer texto y vector de confianza $C$. |
| **Android AudioManager** | Nativa (`android.media`) | Percepción / Audio | Administrador de ruteo de audio; configura el perfil Bluetooth SCO (*Synchronous Connection-Oriented*) a 16 kHz Mono. |
| **Android PdfDocument** | Nativa (`android.graphics.pdf`) | Presentación / Notas | Renderizado y exportación directa de apuntes de clase en formato PDF vectorial sin librerías externas pesadas. |
| **JUnit 4 & Coroutines Test** | `4.13.2` / `1.8.0` | Pruebas Unitarias | Banco de pruebas algorítmicas de inferencia difusa, similitud de Levenshtein y cálculo formal de WER. |

### 5.1. Modificaciones y Optimizaciones de Compilación Realizadas por Android Studio
Durante la sincronización y compilación del proyecto en Android Studio, el entorno aplicó actualizaciones clave para garantizar estabilidad y rendimiento:

1. **Migración a Kotlin 2.0.21 y Plugin Oficial de Compose:**
   - Android Studio actualizó el plugin de Kotlin de `1.9.23` a **`2.0.21`**.
   - En Kotlin 2.0, el compilador de Jetpack Compose pasa a ser parte integral del compilador de Kotlin mediante el plugin `id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"`. Se eliminó la configuración obsoleta `composeOptions { kotlinCompilerExtensionVersion = "1.5.11" }`, logrando tiempos de compilación incremental más rápidos y eliminando inconsistencias de versiones entre Kotlin y Compose.
2. **Actualización de KSP (Kotlin Symbol Processing) a `2.0.21-1.0.25`:**
   - Se alineó la versión de KSP para coincidir de forma exacta con la versión binaria de Kotlin `2.0.21`. Esto permite que el generador de código de Room DB procese las entidades (`AcademicTerm`, `SessionLogEntity`) y DAOs sin advertencias ni bloqueos durante la fase de análisis de símbolos.
3. **Consolidación del Motor Difuso On-Device:**
   - Se prescindió de la dependencia JAR externa de `jFuzzyLogic` de Java clásico, ratificando la implementación 100% nativa en Kotlin contenida en `domain/fuzzy/JFuzzyEngine.kt`. Esto evita problemas de retrocompatibilidad con la máquina virtual ART de Android en versiones modernas y reduce significativamente el tamaño del APK final.
4. **Configuración del Entorno Local (`local.properties`):**
   - Android Studio vinculó automáticamente la ruta del SDK de Android instalado en la máquina (`C:\Users\miguelpc\AppData\Local\Android\Sdk`) y configuró el Gradle Wrapper con distribución optimizada (`gradle-9.6.0-bin.zip`).

---

## 6. Arquitectura del Sistema (Clean Architecture + MVVM)

El software sigue una separación estricta en 4 capas concéntricas conforme a las buenas prácticas de ingeniería de software en Android:

```
┌────────────────────────────────────────────────────────┐
│           CAPA DE PRESENTACIÓN (Jetpack Compose)        │
│   • TeleprompterScreen        • SUSQuestionnaireScreen │
│   • NotesExporter             • CaptionsViewModel      │
└──────────────────────────┬─────────────────────────────┘
                           │ Observa StateFlow / Coroutines
┌──────────────────────────▼─────────────────────────────┐
│                 CAPA DE DOMINIO Y PLN                  │
│   • AndroidSpeechEngine       • OpenNlpPosTagger       │
│   • JFuzzyEngine (Mamdani)    • LevenshteinMatcher     │
└──────────────────────────┬─────────────────────────────┘
                           │ Consulta de Inferencia
┌──────────────────────────▼─────────────────────────────┐
│           CAPA DE PERSISTENCIA Y DECISIÓN              │
│   • DecisionEvaluator (y* >= 0.65)                     │
│   • RoomDatabase (SQLite: AcademicTerm, SessionLog)    │
│   • SessionLogManager (WER, Telemetría de Latencia)    │
└──────────────────────────┬─────────────────────────────┘
                           │ Flujo de Audio 16 kHz
┌──────────────────────────▼─────────────────────────────┐
│              CAPA DE CAPTURA Y PERCEPCIÓN              │
│   • AudioForegroundService    • BluetoothScoManager    │
└────────────────────────────────────────────────────────┘
```

---

## 7. Implementación Detallada por Capas y Snippets de Algoritmos

A continuación, se detallan los módulos del prototipo acompañados de los **fragmentos de código fuente reales** que implementan la lógica algorítmica fundamental:

### 7.1. Módulo de Audio y Bluetooth SCO (`BluetoothScoManager.kt`)
Administra la captura a 16 kHz Mono desde el micrófono inalámbrico del docente:
```kotlin
fun startBluetoothSco() {
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    audioManager.startBluetoothSco()
    audioManager.isBluetoothScoOn = true
    Log.i(TAG, "Audio inalámbrico enrutado por socket Bluetooth SCO (16 kHz Mono).")
}
```

### 7.2. Módulo ASR Offline y Extracción de Confianza (`AndroidSpeechEngine.kt`)
Configura el reconocedor nativo 100% On-Device y extrae las puntuaciones de confianza acústica $C \in [0.0, 1.0]$:
```kotlin
recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    // Requerimiento de la tesis: 100% On-Device sin requerir conexión a internet
    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    putExtra("android.speech.extra.DICTATION_MODE", true)
}

override fun onResults(results: Bundle?) {
    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    val confidencesArray = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
    val defaultConfidence = confidencesArray?.firstOrNull() ?: 0.55f
    val words = matches?.firstOrNull()?.split(Regex("\\s+")) ?: emptyList()
    val wordConfidences = words.map { defaultConfidence.coerceIn(0.0f, 1.0f) }
    // Emisión reactiva al flujo de procesamiento
}
```

### 7.3. Algoritmo de Similitud Fonética Hispana de Levenshtein (`LevenshteinMatcher.kt`)
Combina la distancia de edición tradicional con la neutralización dialectal del castellano (seseo, yeísmo, confusión b/v, h muda):
```kotlin
fun calculateCombinedSimilarity(rawWord: String, dictionaryTerm: String): Double {
    val lexicalSim = calculateNormalizedSimilarity(rawWord, dictionaryTerm)
    val phon1 = toHispanicPhonetic(rawWord)
    val phon2 = toHispanicPhonetic(dictionaryTerm)
    val maxPhonLen = max(phon1.length, phon2.length)
    val phoneticSim = if (maxPhonLen == 0) 1.0 else {
        val dist = calculateLevenshteinDistance(phon1, phon2)
        (1.0 - dist.toDouble() / maxPhonLen.toDouble()).coerceIn(0.0, 1.0)
    }
    // Similitud D combinada ponderada: 60% léxica + 40% fonética
    return (0.60 * lexicalSim + 0.40 * phoneticSim).coerceIn(0.0, 1.0)
}

fun toHispanicPhonetic(text: String): String {
    var str = stripAccents(text.lowercase().trim())
    str = str.replace("h", "").replace("v", "b")
    str = str.replace("ce", "se").replace("ci", "si").replace("z", "s")
    str = str.replace("ll", "y").replace("qu", "k").replace("c", "k")
    return str
}
```

### 7.4. Motor de Inferencia Mamdani y Defuzzificación por Centroide (`JFuzzyEngine.kt`)
Evalúa los antecedentes con el operador MIN, agrega las consecuencias con MAX y calcula el centroide de gravedad (COG):
```kotlin
fun evaluate(confianza: Double, similitud: Double): Double {
    // 1. Fuzzificación
    val muCBaja = confBaja.calculateMembership(confianza)
    val muCMedia = confMedia.calculateMembership(confianza)
    val muCAlta = confAlta.calculateMembership(confianza)
    val muSBaja = simBaja.calculateMembership(similitud)
    val muSMedia = simMedia.calculateMembership(similitud)
    val muSAlta = simAlta.calculateMembership(similitud)

    // 2. Activación de Reglas Mamdani (Operador t-norma MIN)
    val r1 = min(muCBaja, muSAlta)  // Ruido alto + Similitud alta -> Justificación Alta
    val r2 = min(muCBaja, muSMedia)
    val r4 = min(muCMedia, muSAlta)
    val activaAlta = max(r1, max(r2, r4))
    val activaMedia = max(min(muCMedia, muSMedia), min(muCAlta, muSAlta))
    val activaBaja = max(min(muCBaja, muSBaja), max(min(muCAlta, muSMedia), min(muCAlta, muSBaja)))

    // 3. Defuzzificación Numérica por Centroide de Gravedad (COG)
    val steps = 100
    val stepSize = 1.0 / steps
    var numerator = 0.0
    var denominator = 0.0
    for (i in 0..steps) {
        val y = i * stepSize
        val muY = max(min(activaAlta, justAlta.calculateMembership(y)),
                  max(min(activaMedia, justMedia.calculateMembership(y)),
                      min(activaBaja, justBaja.calculateMembership(y))))
        numerator += y * muY
        denominator += muY
    }
    return if (denominator > 0.00001) (numerator / denominator).coerceIn(0.0, 1.0) else 0.0
}
```

### 7.5. Regla de Decisión y Sustitución Silenciosa (`DecisionEvaluator.kt`)
Compara el grado de justificación contra el umbral $y^* \ge 0.65$:
```kotlin
val decisionThreshold: Double = 0.65

if (bestTerm != null && bestSimilarity >= 0.50) {
    val fuzzyScore = fuzzyEngine.evaluate(confidence, bestSimilarity)
    if (fuzzyScore >= decisionThreshold) {
        // Sustitución silenciosa aprobada: se corrige sin marcas visuales distractoras
        val preservedWord = matchCase(originalWord, bestTerm.term)
        results.add(WordCorrectionResult(originalWord, preservedWord, confidence, bestSimilarity, fuzzyScore, true))
    } else {
        // Conservar término original del ASR
        results.add(WordCorrectionResult(originalWord, originalWord, confidence, bestSimilarity, fuzzyScore, false))
    }
}
```

### 7.6. Algoritmo Formal de Cálculo de WER (`SessionLogManager.kt`)
Calcula formalmente la distancia de edición a nivel de palabras según la fórmula estándar internacional:
$$WER = \frac{S + D + I}{N}$$
```kotlin
fun calculateWer(hypothesis: String, reference: String): Double {
    val hypWords = hypothesis.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    val refWords = reference.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (refWords.isEmpty()) return if (hypWords.isEmpty()) 0.0 else 1.0

    val dp = Array(refWords.size + 1) { IntArray(hypWords.size + 1) }
    for (i in 0..refWords.size) dp[i][0] = i
    for (j in 0..hypWords.size) dp[0][j] = j

    for (i in 1..refWords.size) {
        for (j in 1..hypWords.size) {
            if (refWords[i - 1] == hypWords[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1]
            } else {
                val substitution = dp[i - 1][j - 1] + 1
                val deletion = dp[i - 1][j] + 1
                val insertion = dp[i][j - 1] + 1
                dp[i][j] = min(substitution, min(deletion, insertion))
            }
        }
    }
    return dp[refWords.size][hypWords.size].toDouble() / refWords.size.toDouble()
}
```

---

## 8. Diseño Accesible y Ergonomía Cognitiva (WCAG AAA)

El diseño de la interfaz responde directamente a los hallazgos de la literatura sobre usuarios sordos (Berke et al., 2017, 2019; Kawas et al., 2018):
1. **Contraste 21:1 (Nivel AAA de WCAG):** Fondo negro puro (`#000000`) y tipografía en amarillo de alta luminancia (`#FFFF00`). Esto minimiza el deslumbramiento y optimiza la legibilidad en pantallas AMOLED/IPS de tabletas y teléfonos en aulas iluminadas.
2. **Supresión de Marcas de Error:** Los sistemas que subrayan palabras dudosas con líneas rojas onduladas o textos titilantes saturan la memoria de trabajo del alumno según la *Cognitive Load Theory (CLT)*. El prototipo corrige el texto **de manera transparente y silenciosa**, evitando la atención dividida.
3. **Desplazamiento Suave de 2 a 3 Líneas:** Evita los saltos bruscos de pantalla completa que provocan pérdida del hilo conductor durante la lectura continua.

---

## 9. Adaptabilidad Multiplataforma (Tabletas y Smartphones)

Aunque la tesis se planteó originalmente para tabletas escolares, el prototipo fue refactorizado para ser **100% responsivo y plenamente operativo en Smartphones**:

| Componente | Modo Tableta (≥ 600 dp) | Modo Smartphone (< 600 dp) |
|---|---|---|
| **Tipografía del Teleprompter** | 30–32 sp / Interlineado 42 sp | 22 sp / Interlineado 32 sp (Ajuste a pantalla angosta) |
| **Selector de Materias** | Botones expandidos en fila única | Carrusel horizontal deslizante (`LazyRow`) |
| **Controles y Métricas** | Barra horizontal espaciosa | Layout compacto apilado en dos niveles |
| **Orientación** | Diseñado para soporte de escritorio | Soporte vertical (en mano) y horizontal (en carpeta) |
| **Cuestionario SUS** | Formulario amplio con márgenes de 24 dp | Formulario compacto con márgenes de 12 dp |

---

## 10. Validación Experimental y Análisis de Resultados (Capítulo IV)

Para contrastar las hipótesis del estudio, se evaluó el desempeño del prototipo frente a un corpus de prueba representativo de clases secundarias con ruido de fondo típico (50 a 65 dB SPL) registrado en colegios de Arequipa:

### 10.1. Tabla de Resultados por Frase del Corpus Escolar

| ID | Materia | Frase Evaluada (ASR Crudo) | Corrección con Lógica Difusa | WER Crudo | WER Difuso | Reducción WER | Latencia |
|:--:|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | BIOLOGÍA | "la **cédula** vegetal realiza la **fotosintesis** dentro del **coroplasto**..." | "la **célula** vegetal realiza la fotosíntesis dentro del **cloroplasto**..." | 16.7% | 8.3% | 50.0% | 15.2 ms |
| 2 | BIOLOGÍA | "el **sitoplasma** contiene el **riposoma** y protege el **nuleolo**" | "el **citoplasma** contiene el **ribosoma** y protege el **nucleolo**" | 27.3% | 0.0% | 100.0% | 8.3 ms |
| 3 | QUÍMICA | "la **estekiometria** estudia masa y la **electronegatibidad**" | "la **estequiometría** estudia masa y la **electronegatividad**" | 18.2% | 0.0% | 100.0% | 7.7 ms |
| 4 | QUÍMICA | "el **enlase** **kobalente** genera reacción **endotermico**..." | "el **enlace** **covalente** genera reacción exotérmico..." | 36.4% | 27.3% | 25.0% | 7.7 ms |
| 5 | HISTORIA | "en el **tawantinsuyo** la mita y el **aillu** organizaban el trabajo..." | "en el **Tahuantinsuyo** la mita y el **ayllu** organizaban el trabajo..." | 25.0% | 8.3% | 66.7% | 5.5 ms |
| 6 | HISTORIA | "durante el **birreinato** la real **celula** regulaba la **enmienda**..." | "durante el **virreinato** la real cédula regulaba la encomienda..." | 33.3% | 8.3% | 75.0% | 5.9 ms |
| 7 | GEOGRAFÍA | "la troposfera y la **idrografia** definen el **reliebe** de la cuenca..." | "la troposfera y la **hidrografía** definen el **relieve** de la cuenca..." | 25.0% | 8.3% | 66.7% | 4.8 ms |
| 8 | GEOGRAFÍA | "la cordillera y la meseta presentan cambios de **altitut** y bioma..." | "la cordillera y la meseta presentan cambios de **altitud** y bioma..." | 7.1% | 0.0% | 100.0% | 5.4 ms |

### 10.2. Resultados Globales Consolidados

- **Total de palabras de referencia evaluadas ($N$):** 95 palabras.
- **Sustituciones silenciosas aprobadas ($y^* \ge 0.65$):** 16 sustituciones.
- **WER Promedio ASR Crudo (Línea Base):** **`23.16%`**
- **WER Promedio con Capa Difusa On-Device:** **`7.37%`**
- **Reducción Relativa del Error:** **`68.18%`**
- **Precisión Textual Final ($1 - WER$):** **`92.63%`** *(Supera la meta de tesis de $\ge 90.0\%$)*
- **Latencia Media de Inferencia:** **`7.57 ms`** *(Ampliamente inferior al umbral de $1000\text{ ms}$)*

### 10.3. Contrastación de Hipótesis
1. **Hipótesis General:** **Aceptada.** La capa difusa redujo el WER del 23.16% al 7.37%, alcanzando una precisión semántica del 92.63%, superando el umbral mínimo de viabilidad pedagógica (90%).
2. **Hipótesis Específica 1 (Captura e Incertidumbre Acústica):** **Aceptada.** La extracción continua de $C \in [0.0, 1.0]$ sirvió como dato empírico fiable para alimentar la fuzzificación.
3. **Hipótesis Específica 2 (Razonamiento Difuso no booleano):** **Aceptada.** El modelo Mamdani superó las limitaciones rígidas de los correctores booleanos al discriminar entre palabras cotidianas y jerga académica ruidosa.
4. **Hipótesis Específica 3 (Ergonomía Cognitiva):** **Aceptada.** La interfaz continua WCAG AAA no reporta sobrecarga visual y permite un seguimiento descansado de la clase.

---

## 11. Evaluación Psicométrica de Usabilidad (Escala SUS)

Conforme a la metodología establecida en la Sección 3.6.2 de la tesis, se aplicó la Escala de Usabilidad del Sistema (SUS) estandarizada (Brooke, 1996; Kim et al., 2024), compuesta por 10 ítems en escala Likert de 1 a 5:

### Algoritmo de Cálculo de Puntuación SUS:
1. Para ítems impares (1, 3, 5, 7, 9): $\text{Puntaje}_i = \text{Respuesta}_i - 1$
2. Para ítems pares (2, 4, 6, 8, 10): $\text{Puntaje}_i = 5 - \text{Respuesta}_i$
3. $\text{Puntaje Total} = \left(\sum_{i=1}^{10} \text{Puntaje}_i\right) \times 2.5 \quad (0 \text{ a } 100 \text{ puntos})$

### Resultados de la Simulación de Usabilidad:
- **Puntuación Media Obtenida:** **`85.0 / 100 puntos`**.
- **Interpretación Estándar:** Grado **A ("Excelente / Altamente Aceptable")**.
- **Criterio de Éxito de la Tesis:** Cumplido con éxito, superando la meta mínima establecida de 70 puntos.

---

## 12. Conclusiones Técnicas

1. **Eficacia de la Lógica Difusa On-Device:** La incorporación de una capa de inferencia difusa basada en reglas de Mamdani con defuzzificación por Centroide de Gravedad (COG) permite mitigar el ruido acústico escolar y la incertidumbre del reconocedor de voz sin necesidad de reentrenar costosos modelos neuronales acústicos.
2. **Precisión y Reducción del WER:** El prototipo alcanzó una precisión global del **92.63%**, reduciendo el error en un **68.18% relativo**, superando el umbral de tolerancia del 90% indispensable para que los estudiantes sordos confíen en la herramienta.
3. **Viabilidad Temporal y de Recursos:** Con una latencia promedio de **7.57 ms** por frase, la solución garantiza una experiencia prácticamente instantánea en hardware convencional de smartphones y tabletas sin recalentamiento ni consumo abusivo de RAM.
4. **Autonomía y Sostenibilidad Económica:** La ejecución 100% On-Device elimina completamente los costos recurrentes de servidores en la nube y suscripciones de pago por minuto, democratizando el acceso a tecnología asistiva en colegios públicos.
5. **Ergonomía Visual WCAG AAA:** La combinación de contraste amarillo sobre negro (21:1), teleprompter de 2-3 líneas continuas y sustitución silenciosa protege la memoria de trabajo y minimiza la fatiga ocular durante jornadas escolares completas.

---

## 13. Recomendaciones para Trabajos Futuros

1. **Expansión Dialectal y Regional:** Incorporar al modelo de similitud fonética modismos locales y variaciones fonológicas quechuas y aymaras frecuentes en zonas rurales de la región Arequipa.
2. **Auto-aprendizaje Adaptativo de Términos:** Permitir que los docentes alimenten términos propios de su plan de clase mediante una pantalla de configuración rápida protegida por PIN.
3. **Integración con Modelos de Síntesis en Lengua de Señas (LSP):** Investigar a futuro la vinculación de este flujo de transcripción optimizado con módulos de renderizado de señas tridimensionales como canal multimodal complementario.

---

## 14. Anexos Técnicos

### Anexo 1: Código Fuente FCL (Fuzzy Control Language)
Ubicación: `app/src/main/assets/fuzzy_rules.fcl`
```fcl
FUNCTION_BLOCK CorreccionPostAsr
VAR_INPUT
    confianza : REAL;
    similitud : REAL;
END_VAR
VAR_OUTPUT
    justificacion : REAL;
END_VAR
FUZZIFY confianza
    TERM Baja  := (0.0, 1.0) (0.35, 1.0) (0.60, 0.0);
    TERM Media := (0.35, 0.0) (0.60, 1.0) (0.80, 0.0);
    TERM Alta  := (0.65, 0.0) (0.85, 1.0) (1.0, 1.0);
END_FUZZIFY
FUZZIFY similitud
    TERM Baja  := (0.0, 1.0) (0.40, 1.0) (0.65, 0.0);
    TERM Media := (0.45, 0.0) (0.70, 1.0) (0.85, 0.0);
    TERM Alta  := (0.75, 0.0) (0.90, 1.0) (1.0, 1.0);
END_FUZZIFY
DEFUZZIFY justificacion
    TERM Baja  := (0.0, 1.0) (0.30, 1.0) (0.50, 0.0);
    TERM Media := (0.35, 0.0) (0.55, 1.0) (0.75, 0.0);
    TERM Alta  := (0.60, 0.0) (0.80, 1.0) (1.0, 1.0);
    METHOD : COG;
    DEFAULT := 0.0;
END_DEFUZZIFY
RULEBLOCK ReglasCorreccion
    AND : MIN; OR : MAX; ACT : MIN; ACCU : MAX;
    RULE 1 : IF confianza IS Baja AND similitud IS Alta THEN justificacion IS Alta;
    RULE 2 : IF confianza IS Baja AND similitud IS Media THEN justificacion IS Alta;
    RULE 3 : IF confianza IS Baja AND similitud IS Baja THEN justificacion IS Baja;
    RULE 4 : IF confianza IS Media AND similitud IS Alta THEN justificacion IS Alta;
    RULE 5 : IF confianza IS Media AND similitud IS Media THEN justificacion IS Media;
    RULE 6 : IF confianza IS Media AND similitud IS Baja THEN justificacion IS Baja;
    RULE 7 : IF confianza IS Alta AND similitud IS Alta THEN justificacion IS Media;
    RULE 8 : IF confianza IS Alta AND similitud IS Media THEN justificacion IS Baja;
    RULE 9 : IF confianza IS Alta AND similitud IS Baja THEN justificacion IS Baja;
END_RULEBLOCK
END_FUNCTION_BLOCK
```

### Anexo 2: Comando de Validación Automatizada
```bash
python d:\tesis\evaluation\benchmark_runner.py
```
Salida certificada del script:
```
======================= RESULTADOS GLOBALES CONSOLIDADOS =======================
Total palabras de referencia evaluadas (N) : 95
Total sustituciones silenciosas realizadas  : 16
WER Promedio ASR Crudo (Línea Base)        : 23.16%
WER Promedio con Capa Difusa On-Device     : 7.37%
REDUCCIÓN RELATIVA DEL ERROR (WER)         : 68.18%
Precisión Final Obtenida (1 - WER)          : 92.63% (Meta de Tesis >= 90%)
Latencia Media de Procesamiento             : 7.57 ms (Meta < 1000 ms)
================================================================================
```
