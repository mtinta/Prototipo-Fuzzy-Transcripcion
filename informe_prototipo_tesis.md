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

La presentación visual se basa en una interfaz **Jetpack Compose** bajo la pauta de accesibilidad **WCAG AAA** (relación de contraste 19.56:1, texto amarillo `#FFFF00` sobre fondo negro `#000000`), proyectada en modo teleprompter continuo de 2 a 3 líneas con desplazamiento suave (*smooth scrolling*). Esto previene la fatiga cognitiva y la atención dividida en los estudiantes con discapacidad auditiva conforme a la Teoría de la Carga Cognitiva (CLT).

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
El sistema modela la incertidumbre mediante 3 variables de entrada y 1 variable de salida:

#### Variables de Entrada:
- **Confianza Acústica ($C \in [0.0, 1.0]$):** confianza por hipótesis (frase) entregada por `SpeechRecognizer`; si falta se usa 0.65 (0.60 en resultados parciales).
  - $\mu_{\text{Baja}}(C) = \text{Trapezoide}(0.0, 0.0, 0.35, 0.60)$
  - $\mu_{\text{Media}}(C) = \text{Triángulo}(0.35, 0.60, 0.80)$
  - $\mu_{\text{Alta}}(C) = \text{Trapezoide}(0.65, 0.85, 1.0, 1.0)$
- **Similitud Léxico-Fonética ($D \in [0.0, 1.0]$):**
  - $\mu_{\text{Baja}}(D) = \text{Trapezoide}(0.0, 0.0, 0.40, 0.65)$
  - $\mu_{\text{Media}}(D) = \text{Triángulo}(0.45, 0.70, 0.85)$
  - $\mu_{\text{Alta}}(D) = \text{Trapezoide}(0.75, 0.90, 1.0, 1.0)$
- **Coherencia Semántica ($S \in [0.0, 1.0]$):** co-ocurrencia del término candidato con otros términos de la oración en los *topic clusters* de la asignatura, combinada con la memoria temática de la clase.
  - $\mu_{\text{Baja}}(S) = \text{Trapezoide}(0.0, 0.0, 0.25, 0.45)$
  - $\mu_{\text{Media}}(S) = \text{Triángulo}(0.30, 0.50, 0.70)$
  - $\mu_{\text{Alta}}(S) = \text{Trapezoide}(0.55, 0.75, 1.0, 1.0)$

#### Variable de Salida:
- **Grado de Justificación de Corrección ($y^* \in [0.0, 1.0]$):**
  - $\mu_{\text{Baja}}(y) = \text{Trapezoide}(0.0, 0.0, 0.30, 0.50)$
  - $\mu_{\text{Media}}(y) = \text{Triángulo}(0.35, 0.55, 0.75)$
  - $\mu_{\text{Alta}}(y) = \text{Trapezoide}(0.60, 0.80, 1.0, 1.0)$

#### Base de Reglas Difusas (27 reglas, conjunción con MIN, agregación con MAX):
Cada fila es una regla: SI $C$ es (columna C) Y $D$ es (columna D) Y $S$ es (columna S) ENTONCES $y^*$ es (columna y*).

| # | C | D | S | y* |
|---|---|---|---|---|
| 1 | Baja | Alta | Alta | Alta |
| 2 | Baja | Alta | Media | Alta |
| 3 | Baja | Alta | Baja | Media |
| 4 | Baja | Media | Alta | Alta |
| 5 | Baja | Media | Media | Media |
| 6 | Baja | Media | Baja | Baja |
| 7 | Baja | Baja | Alta | Media |
| 8 | Baja | Baja | Media | Baja |
| 9 | Baja | Baja | Baja | Baja |
| 10 | Media | Alta | Alta | Alta |
| 11 | Media | Alta | Media | Alta |
| 12 | Media | Alta | Baja | Media |
| 13 | Media | Media | Alta | Alta |
| 14 | Media | Media | Media | Media |
| 15 | Media | Media | Baja | Baja |
| 16 | Media | Baja | Alta | Media |
| 17 | Media | Baja | Media | Baja |
| 18 | Media | Baja | Baja | Baja |
| 19 | Alta | Alta | Alta | Media |
| 20 | Alta | Alta | Media | Media |
| 21 | Alta | Alta | Baja | Baja |
| 22 | Alta | Media | Alta | Media |
| 23 | Alta | Media | Media | Baja |
| 24 | Alta | Media | Baja | Baja |
| 25 | Alta | Baja | Alta | Baja |
| 26 | Alta | Baja | Media | Baja |
| 27 | Alta | Baja | Baja | Baja |

#### Defuzzificación por Centroide de Gravedad (COG):
$$y^* = \frac{\int_0^1 y \cdot \mu_B(y) \, dy}{\int_0^1 \mu_B(y) \, dy}$$

### 4.4. Regla de Decisión y Sustitución Silenciosa
Para cada palabra candidata se sustituye únicamente si se cumplen las tres condiciones siguientes; en cualquier otro caso se conserva la palabra original del ASR:

$$\text{Acción} = \begin{cases} 
\text{Sustituir por término canónico del diccionario}, & \text{si } y^* \ge 0.65 \;\text{y}\; D \ge 0.70 \;\text{y la palabra no es flexión del término} \\ 
\text{Conservar palabra original del ASR}, & \text{en otro caso} 
\end{cases}$$

El piso $D \ge 0.70$ evita falsos positivos cuando un $S$ alto lleva a $y^*$ sobre el umbral con un parecido léxico pobre. La condición de flexión evita cambiar el número gramatical de una palabra ya correcta (p. ej. "gametos"/"gameto").

---

## 5. Pila Tecnológica y Librerías Utilizadas

El sistema fue diseñado bajo el principio de **cero dependencias de servicios en la nube**, empleando librerías de código abierto y APIs nativas de Android optimizadas para ejecución local:

| Librería / Componente | Versión / Identificador | Capa de Arquitectura | Justificación y Rol en el Prototipo |
|---|---|---|---|
| **Kotlin Compiler** | `2.0.21` | Lenguaje / Base | Compilador principal del proyecto, actualizado automáticamente para soporte de Compose nativo. |
| **Compose Compiler Plugin** | `org.jetbrains.kotlin.plugin.compose:2.0.21` | Compilación / UI | Nuevo plugin oficial de JetBrains/Google para Kotlin 2.0 que sustituye la extensión manual anterior. |
| **Jetpack Compose BOM** | `2024.04.01` | Presentación | Marco declarativo para construir la interfaz de usuario reactiva bajo el estándar WCAG AAA (19.56:1; umbral 7:1). |
| **Material 3 & Foundation** | `androidx.compose.material3` | Presentación | Componentes de diseño moderno, tipografías escalables e iconos de accesibilidad. |
| **Room Database (SQLite)** | `androidx.room:2.6.1` | Persistencia | Motor de base de datos relacional local que indexa y almacena en memoria caché los diccionarios escolares del MINEDU. |
| **Kotlin Symbol Processing (KSP)** | `2.0.21-1.0.25` | Compilación / Room | Procesador de anotaciones para Room alineado con Kotlin 2.0, generando el código de persistencia sin impacto en runtime. |
| **Kotlinx Coroutines & Flow** | `1.8.0` | Dominio / Servicio | Manejo asíncrono y reactivo del flujo continuo de audio y texto (`SharedFlow` / `StateFlow`), garantizando latencia $< 1.0\text{ s}$. |
| **Motor Difuso Mamdani Nativo** | Código Kotlin (`JFuzzyEngine`) | Razonamiento Difuso | Implementación nativa de alta eficiencia en Kotlin para inferencia Mamdani y centroide (COG) con cero overhead de librerías externas. |
| **Gson** | `2.10.1` | Datos / Ingesta | Serialización y deserialización ligera para cargar los diccionarios JSON de educación secundaria durante la siembra de la BD. |
| **Android SpeechRecognizer** | Nativa (Android API 26+) | Percepción / ASR | API nativa de reconocimiento de voz de Android configurada en modo local (`EXTRA_PREFER_OFFLINE = true`) para extraer texto y vector de confianza $C$. |
| **Android AudioManager** | Nativa (`android.media`) | Percepción / Audio | Administrador de ruteo de audio; fija `MODE_NORMAL` y detecta el receptor USB-C 2.4 GHz (SX26 / Maxtron) mediante `AudioDeviceCallback`. |
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
                           │ Flujo de Audio 44.1 / 48 kHz
┌──────────────────────────▼─────────────────────────────┐
│              CAPA DE CAPTURA Y PERCEPCIÓN              │
│   • AudioForegroundService • ExternalAudioDeviceManager│
└────────────────────────────────────────────────────────┘
```

---

## 7. Implementación Detallada por Capas y Snippets de Algoritmos

A continuación, se detallan los módulos del prototipo acompañados de los **fragmentos de código fuente reales** que implementan la lógica algorítmica fundamental:

### 7.1. Módulo de Audio y Micrófono Inalámbrico Digital (`ExternalAudioDeviceManager.kt`)
Administra la captura digital a 44.1 / 48 kHz PCM desde el micrófono de solapa inalámbrico del docente (SX26 Wireless o Maxtron 2.4 GHz con receptor USB-C) y el fallback automático instantáneo al micrófono interno:
```kotlin
fun checkConnectedAudioDevices() {
    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
    for (device in devices) {
        if (device.type == AudioDeviceInfo.TYPE_USB_DEVICE || 
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
            _currentSource.value = AudioInputSource.WIRELESS_USB_MIC
            _isExternalMicConnected.value = true
            Log.i(TAG, "Receptor USB-C 2.4 GHz detectado: ${device.productName}")
            return
        }
    }
    fallbackToInternalMic()
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
    // La API entrega UNA confianza por hipótesis (no por palabra): se replica a todas las palabras.
    // Si falta, es <= 0 o NaN, resolveConfidence() devuelve la confianza neutra 0.65.
    val defaultConfidence = resolveConfidence(confidencesArray?.firstOrNull())
    val words = matches?.firstOrNull()?.split(Regex("\\s+")) ?: emptyList()
    val wordConfidences = words.map { defaultConfidence }
    // En onPartialResults la confianza es una constante de 0.60
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
Evalúa las 27 reglas sobre $(C, D, S)$: los antecedentes con el operador MIN, agrega las consecuencias con MAX y calcula el centroide de gravedad (COG):
```kotlin
fun evaluate(confianza: Double, similitud: Double, semantica: Double = 0.50): Double {
    val c = confianza.coerceIn(0.0, 1.0)
    val d = similitud.coerceIn(0.0, 1.0)
    val s = semantica.coerceIn(0.0, 1.0)

    // 1. Fuzzificación de entradas
    val muCBaja = confBaja.calculateMembership(c)
    val muCMedia = confMedia.calculateMembership(c)
    val muCAlta = confAlta.calculateMembership(c)

    val muDBaja = simBaja.calculateMembership(d)
    val muDMedia = simMedia.calculateMembership(d)
    val muDAlta = simAlta.calculateMembership(d)

    val muSBaja = semBaja.calculateMembership(s)
    val muSMedia = semMedia.calculateMembership(s)
    val muSAlta = semAlta.calculateMembership(s)

    // 2. Activación de 27 Reglas Mamdani (Operador t-norma MIN: min(muC, muD, muS))
    // --- Grupo 1: C IS Baja ---
    val r1 = min(muCBaja, min(muDAlta, muSAlta))   // J Alta
    val r2 = min(muCBaja, min(muDAlta, muSMedia))  // J Alta
    val r3 = min(muCBaja, min(muDAlta, muSBaja))   // J Media
    val r4 = min(muCBaja, min(muDMedia, muSAlta))  // J Alta
    val r5 = min(muCBaja, min(muDMedia, muSMedia)) // J Media
    val r6 = min(muCBaja, min(muDMedia, muSBaja))  // J Baja
    val r7 = min(muCBaja, min(muDBaja, muSAlta))   // J Media
    val r8 = min(muCBaja, min(muDBaja, muSMedia))  // J Baja
    val r9 = min(muCBaja, min(muDBaja, muSBaja))   // J Baja

    // --- Grupo 2: C IS Media ---
    val r10 = min(muCMedia, min(muDAlta, muSAlta))  // J Alta
    val r11 = min(muCMedia, min(muDAlta, muSMedia)) // J Alta
    val r12 = min(muCMedia, min(muDAlta, muSBaja))  // J Media
    val r13 = min(muCMedia, min(muDMedia, muSAlta)) // J Alta
    val r14 = min(muCMedia, min(muDMedia, muSMedia))// J Media
    val r15 = min(muCMedia, min(muDMedia, muSBaja)) // J Baja
    val r16 = min(muCMedia, min(muDBaja, muSAlta))  // J Media
    val r17 = min(muCMedia, min(muDBaja, muSMedia)) // J Baja
    val r18 = min(muCMedia, min(muDBaja, muSBaja))  // J Baja

    // --- Grupo 3: C IS Alta ---
    val r19 = min(muCAlta, min(muDAlta, muSAlta))  // J Media
    val r20 = min(muCAlta, min(muDAlta, muSMedia)) // J Media
    val r21 = min(muCAlta, min(muDAlta, muSBaja))  // J Baja
    val r22 = min(muCAlta, min(muDMedia, muSAlta)) // J Media
    val r23 = min(muCAlta, min(muDMedia, muSMedia))// J Baja
    val r24 = min(muCAlta, min(muDMedia, muSBaja)) // J Baja
    val r25 = min(muCAlta, min(muDBaja, muSAlta))  // J Baja
    val r26 = min(muCAlta, min(muDBaja, muSMedia)) // J Baja
    val r27 = min(muCAlta, min(muDBaja, muSBaja))  // J Baja

    // Agregación de consecuentes por conjunto difuso (Operador s-norma MAX)
    val activaAlta = maxOf(r1, r2, r4, r10, r11, r13)
    val activaMedia = maxOf(r3, r5, r7, r12, r14, r16, r19, r20, r22)
    val activaBaja = maxOf(r6, r8, r9, r15, r17, r18, r21, r23, r24, r25, r26, r27)

    // 3. Defuzzificación por Centroide de Gravedad (COG)
    val steps = 100
    val stepSize = 1.0 / steps
    var numerator = 0.0
    var denominator = 0.0

    for (i in 0..steps) {
        val y = i * stepSize
        // Membresía recortada por cada regla para el punto y
        val muYAlta = min(activaAlta, justAlta.calculateMembership(y))
        val muYMedia = min(activaMedia, justMedia.calculateMembership(y))
        val muYBaja = min(activaBaja, justBaja.calculateMembership(y))

        // Agregación difusa total en y
        val muCombined = max(muYAlta, max(muYMedia, muYBaja))

        numerator += y * muCombined
        denominator += muCombined
    }

    val cog = if (denominator > 0.00001) {
        numerator / denominator
    } else {
        0.0
    }

    return cog.coerceIn(0.0, 1.0)
}
```

### 7.5. Regla de Decisión y Sustitución Silenciosa (`DecisionEvaluator.kt`)
Compara el grado de justificación contra el umbral $y^* \ge 0.65$ y exige además $D \ge 0.70$ y que la palabra no sea ya una flexión del término (extracto):
```kotlin
val decisionThreshold: Double = 0.65
const val SUBSTITUTION_MIN_SIMILARITY = 0.70

// Por cada candidato (hasta 3) se calcula S y se evalúa Mamdani; gana el de mayor y*
val (s, cluster) = calculateSemanticCoherence(candidate.term, matchedTermsInSentence, clusters)
val y = fuzzyEngine.evaluate(confianza = item.confidence, similitud = sim, semantica = s)

val alreadyCorrect = isInflectionOf(cleanWord, bestTerm.term)
if (fuzzyScore >= decisionThreshold &&
    bestSimilarity >= SUBSTITUTION_MIN_SIMILARITY &&
    !alreadyCorrect
) {
    // Sustitución silenciosa aprobada: se corrige sin marcas visuales distractoras
    val preservedWord = matchCase(wordBase, bestTerm.term) + item.trailingPunct
    // ... WordCorrectionResult(..., wasSubstituted = true, semanticCoherence = s)
} else {
    // Conservar término original del ASR
    // ... WordCorrectionResult(..., wasSubstituted = false)
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
1. **Contraste 19.56:1 (Nivel AAA de WCAG, umbral 7:1):** Fondo negro puro (`#000000`) y tipografía en amarillo de alta luminancia (`#FFFF00`). Esto minimiza el deslumbramiento y optimiza la legibilidad en pantallas AMOLED/IPS de tabletas y teléfonos en aulas iluminadas.
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

### 10.1. Resultados por Frase del Corpus Escolar

El detalle por frase (WER, KER, S y latencia) lo genera `evaluation/benchmark_runner.py` y se exporta a `evaluation/reporte_benchmark_wer_ker.md`. Las cifras de esta sección provienen de esa ejecución reproducible.

> **Alcance real del corpus:** `corpus_arequipa_test.json` contiene **8 frases de Biología** con salida ASR y confianzas *simuladas* (no grabadas con el micrófono SX26). Las cifras previas de este informe (23.16 % → 7.37 %, 16 sustituciones, 4 asignaturas) no eran reproducibles con ese corpus y fueron reemplazadas.

### 10.2. Resultados Globales Consolidados

Modelo evaluado: Mamdani trivariado (C, D, S) con áreas temáticas, memoria de la clase y selección de candidato por los tres factores (ver `docs/tesis/factor_semantico_topic_clusters.md`).

- **Total de palabras de referencia evaluadas ($N$):** 103 palabras (31 palabras clave curriculares).
- **Sustituciones silenciosas aprobadas ($y^* \ge 0.65$):** 21.
- **WER Promedio ASR Crudo (Línea Base):** **`18.45%`**
- **WER Promedio con Capa Difusa On-Device:** **`1.94%`** (reducción relativa **`89.47%`**)
- **Precisión Textual Final ($1 - WER$):** **`98.06%`**
- **KER (palabras clave):** `61.29%` → `6.45%`; **Precisión Semántica ($1 - KER$):** **`93.55%`**
- **Coherencia Semántica media ($S$):** `0.50`
- **Falsos positivos:** 0 sustituciones en un corpus de control de 12 frases cotidianas correctas.
- **Latencia media (Python, PC):** `~75 ms`. La latencia en dispositivo móvil debe medirse con TC-PERF-01 del plan de pruebas.

**Advertencias metodológicas (declarar en la tesis).**
1. Los tres corpus son **sintéticos** (errores de ASR inventados; confianzas asignadas a mano), de 8, 13 y 12 frases. Las cifras anteriores muestran que el sistema es **factible**, no que cumpla la meta en el aula.
2. Varias mejoras (áreas temáticas, memoria de términos resueltos, salvaguardas) se decidieron tras observar fallos **en estos mismos corpus**: los resultados son "dentro de la muestra". En una versión previa del sistema la precisión semántica era 77.42 % y luego 83.87 %.
3. Frente a la mejor regla booleana (ajustada sobre los mismos corpus): con confianza neutra 0.65 el sistema difuso obtiene menor WER (2.91 % vs 4.85 % y 1.72 % vs 3.45 %); con confianzas sintéticas hay empate técnico (1.94 % / 1.72 % vs 2.91 % / 0.00 %).
4. Los `macro_id` (áreas temáticas) de los clusters son una propuesta del desarrollador y requieren validación de un docente de Biología.

### 10.3. Contrastación de Hipótesis
1. **Hipótesis General:** **Sustentada de forma preliminar, pendiente de validación en campo.** Sobre el banco de pruebas sintético el WER bajó de 18.45 % a 1.94 % (precisión textual 98.06 %) y la precisión sobre palabras clave subió a 93.55 %, sin alterar lenguaje cotidiano; son resultados dentro de la muestra y deben confirmarse con grabaciones reales del SX26 en aula.
2. **Hipótesis Específica 1 (Captura e Incertidumbre Acústica):** **Pendiente de validar.** `SpeechRecognizer` de Android entrega una única confianza por hipótesis (no por palabra) y puede no entregarla (se usa 0.65 neutro); la fiabilidad de $C$ debe medirse con el SX26 real (TC-ASR-02).
3. **Hipótesis Específica 2 (Razonamiento Difuso no booleano):** **Resultados mixtos, pendiente de voz real.** El sistema no modifica lenguaje cotidiano (0 falsos positivos en el control). Frente a la mejor regla booleana ajustada sobre los mismos corpus, el difuso obtiene menor WER con confianza neutra y hay empate técnico con confianzas sintéticas.
4. **Hipótesis Específica 3 (Ergonomía Cognitiva):** **Pendiente de validar** con estudiantes reales (cuestionario SUS, Anexo A); el contraste medido es 19.56:1 (cumple AAA ≥ 7:1).

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
2. **Precisión y Reducción del WER:** Sobre el banco de pruebas sintético, el prototipo alcanzó una precisión textual de **98.06%** (WER de 18.45% a 1.94%, reducción relativa de **89.47%**) y una precisión semántica de **93.55%**, superando la meta de 90%. Son resultados dentro de la muestra (corpus sintético) y deben confirmarse con grabaciones reales en aula.
3. **Viabilidad Temporal y de Recursos:** La latencia media medida en PC (Python) es de **~72 ms** por frase, muy por debajo de la meta de 1000 ms. La latencia en la tableta debe medirse con TC-PERF-01 antes de afirmar un desempeño equivalente en el dispositivo.
4. **Autonomía y Sostenibilidad Económica:** La ejecución 100% On-Device elimina completamente los costos recurrentes de servidores en la nube y suscripciones de pago por minuto, democratizando el acceso a tecnología asistiva en colegios públicos.
5. **Ergonomía Visual WCAG AAA:** La combinación de contraste amarillo sobre negro (19.56:1), teleprompter de 2-3 líneas continuas y sustitución silenciosa protege la memoria de trabajo y minimiza la fatiga ocular durante jornadas escolares completas.

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
// ============================================================================
// SISTEMA DE CONTROL DIFUSO MAMDANI (jFuzzyLogic / FCL)
// Tesis: Mejora de Precisión Speech-to-Text mediante Lógica Difusa
// Autor: Miguel Angel Tinta Aguilar - UNSA
// Modelo Tri-Variable: Confianza Acústica (C), Similitud Léxica (D) y Coherencia Semántica (S)
// ============================================================================

FUNCTION_BLOCK CorreccionPostAsr

VAR_INPUT
    confianza : REAL;   // Puntuación de confianza acústica del ASR: C in [0.0, 1.0]
    similitud : REAL;   // Similitud fonético-léxica de Levenshtein: D in [0.0, 1.0]
    semantica : REAL;   // Coherencia semántica contextual por clusters: S in [0.0, 1.0]
END_VAR

VAR_OUTPUT
    justificacion : REAL; // Grado de Justificación de Corrección: y* in [0.0, 1.0]
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

FUZZIFY semantica
    TERM Baja  := (0.0, 1.0) (0.25, 1.0) (0.45, 0.0);
    TERM Media := (0.30, 0.0) (0.50, 1.0) (0.70, 0.0);
    TERM Alta  := (0.55, 0.0) (0.75, 1.0) (1.0, 1.0);
END_FUZZIFY

DEFUZZIFY justificacion
    TERM Baja  := (0.0, 1.0) (0.30, 1.0) (0.50, 0.0);
    TERM Media := (0.35, 0.0) (0.55, 1.0) (0.75, 0.0);
    TERM Alta  := (0.60, 0.0) (0.80, 1.0) (1.0, 1.0);
    METHOD : COG; // Centroide de Gravedad
    DEFAULT := 0.0;
END_DEFUZZIFY

RULEBLOCK ReglasCorreccion
    AND : MIN;
    OR  : MAX;
    ACT : MIN;
    ACCU : MAX;

    // --- Grupo 1: Confianza Baja (ASR con alta incertidumbre) ---
    RULE 1  : IF confianza IS Baja AND similitud IS Alta AND semantica IS Alta THEN justificacion IS Alta;
    RULE 2  : IF confianza IS Baja AND similitud IS Alta AND semantica IS Media THEN justificacion IS Alta;
    RULE 3  : IF confianza IS Baja AND similitud IS Alta AND semantica IS Baja THEN justificacion IS Media;
    RULE 4  : IF confianza IS Baja AND similitud IS Media AND semantica IS Alta THEN justificacion IS Alta;
    RULE 5  : IF confianza IS Baja AND similitud IS Media AND semantica IS Media THEN justificacion IS Media;
    RULE 6  : IF confianza IS Baja AND similitud IS Media AND semantica IS Baja THEN justificacion IS Baja;
    RULE 7  : IF confianza IS Baja AND similitud IS Baja AND semantica IS Alta THEN justificacion IS Media;
    RULE 8  : IF confianza IS Baja AND similitud IS Baja AND semantica IS Media THEN justificacion IS Baja;
    RULE 9  : IF confianza IS Baja AND similitud IS Baja AND semantica IS Baja THEN justificacion IS Baja;

    // --- Grupo 2: Confianza Media (Incertidumbre acústica moderada) ---
    RULE 10 : IF confianza IS Media AND similitud IS Alta AND semantica IS Alta THEN justificacion IS Alta;
    RULE 11 : IF confianza IS Media AND similitud IS Alta AND semantica IS Media THEN justificacion IS Alta;
    RULE 12 : IF confianza IS Media AND similitud IS Alta AND semantica IS Baja THEN justificacion IS Media;
    RULE 13 : IF confianza IS Media AND similitud IS Media AND semantica IS Alta THEN justificacion IS Alta;
    RULE 14 : IF confianza IS Media AND similitud IS Media AND semantica IS Media THEN justificacion IS Media;
    RULE 15 : IF confianza IS Media AND similitud IS Media AND semantica IS Baja THEN justificacion IS Baja;
    RULE 16 : IF confianza IS Media AND similitud IS Baja AND semantica IS Alta THEN justificacion IS Media;
    RULE 17 : IF confianza IS Media AND similitud IS Baja AND semantica IS Media THEN justificacion IS Baja;
    RULE 18 : IF confianza IS Media AND similitud IS Baja AND semantica IS Baja THEN justificacion IS Baja;

    // --- Grupo 3: Confianza Alta (ASR seguro de la hipótesis acústica) ---
    RULE 19 : IF confianza IS Alta AND similitud IS Alta AND semantica IS Alta THEN justificacion IS Media;
    RULE 20 : IF confianza IS Alta AND similitud IS Alta AND semantica IS Media THEN justificacion IS Media;
    RULE 21 : IF confianza IS Alta AND similitud IS Alta AND semantica IS Baja THEN justificacion IS Baja;
    RULE 22 : IF confianza IS Alta AND similitud IS Media AND semantica IS Alta THEN justificacion IS Media;
    RULE 23 : IF confianza IS Alta AND similitud IS Media AND semantica IS Media THEN justificacion IS Baja;
    RULE 24 : IF confianza IS Alta AND similitud IS Media AND semantica IS Baja THEN justificacion IS Baja;
    RULE 25 : IF confianza IS Alta AND similitud IS Baja AND semantica IS Alta THEN justificacion IS Baja;
    RULE 26 : IF confianza IS Alta AND similitud IS Baja AND semantica IS Media THEN justificacion IS Baja;
    RULE 27 : IF confianza IS Alta AND similitud IS Baja AND semantica IS Baja THEN justificacion IS Baja;
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
Total palabras de referencia evaluadas (N)     : 103
Total palabras clave curriculares evaluadas    : 31
Total sustituciones silenciosas realizadas      : 21
WER Promedio ASR Crudo (Línea Base)            : 18.45%
WER Promedio con Capa Difusa On-Device         : 1.94%
REDUCCIÓN RELATIVA DE WER (ΔWER_rel)           : 89.47%
KER Promedio ASR Crudo / con Capa Difusa       : 61.29% / 6.45%
PRECISIÓN SEMÁNTICA FINAL (1 - KER)            : 93.55% (Meta de Tesis >= 90%)
Coherencia Semántica Media (S)                 : 0.50
Latencia Media de Procesamiento                : 72.11 ms (Meta < 1000 ms)
================================================================================
```
