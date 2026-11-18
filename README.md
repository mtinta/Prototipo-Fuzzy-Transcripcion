# Prototipo de Tesis: Mejora de la Precisión Speech-to-Text mediante Lógica Difusa On-Device

**Título de la Tesis:** *Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De Lógica Difusa Para Transcripción Automática Como Apoyo A Estudiantes Sordos En Educación Secundaria.*  
**Bachiller:** Miguel Angel Tinta Aguilar  
**Asesor:** Dr. Karim Guevara Puente de la Vega  
**Institución:** Universidad Nacional de San Agustín de Arequipa (UNSA) — Escuela Profesional de Ingeniería de Sistemas  
**Paquete:** `com.unsa.dhh.captions`

---

## 🏛️ Estructura del Proyecto (Capítulo III)

```text
d:\tesis\
├── app\
│   ├── src\main\
│   │   ├── AndroidManifest.xml                  # Permisos Audio (RECORD_AUDIO), Foreground Service, USB host opcional
│   │   ├── assets\
│   │   │   ├── fuzzy_rules.fcl                  # Definición formal FCL de reglas difusas Mamdani
│   │   │   └── dictionaries\                    # Diccionarios MINEDU Educación Secundaria
│   │   │       ├── biologia.json
│   │   │       ├── quimica.json
│   │   │       ├── historia.json
│   │   │       └── geografia.json
│   │   └── java\com\unsa\dhh\captions\
│   │       ├── MainActivity.kt                  # Actividad principal y orquestación
│   │       ├── service\                         # [Módulo 1: Captura Micrófono Inalámbrico USB & Foreground]
│   │       │   ├── AudioForegroundService.kt
│   │       │   └── ExternalAudioDeviceManager.kt
│   │       ├── domain\
│   │       │   ├── asr\                         # [Módulo 2: Reconocimiento ASR Local Offline]
│   │       │   │   ├── SpeechEngine.kt              # Interfaz común + selector de motor
│   │       │   │   ├── VoskSpeechEngine.kt          # Motor predeterminado: Vosk, C por palabra
│   │       │   │   ├── VoskSupport.kt               # Lectura del JSON de Vosk y reloj del audio
│   │       │   │   ├── AndroidSpeechEngine.kt       # Alternativa: SpeechRecognizer (C fija 0.65)
│   │       │   │   └── SpeechState.kt
│   │       │   ├── nlp\                         # [Módulo 3: Etiquetado Morfosintáctico]
│   │       │   │   └── OpenNlpPosTagger.kt
│   │       │   └── fuzzy\                       # [Módulo 3: Motor Difuso Mamdani & Levenshtein]
│   │       │       ├── JFuzzyEngine.kt
│   │       │       ├── LevenshteinMatcher.kt
│   │       │       └── MembershipFunctions.kt
│   │       ├── data\                            # [Módulo 4: Persistencia Room DB & Decisión]
│   │       │   ├── local\
│   │       │   │   ├── RoomDatabase.kt
│   │       │   │   ├── dao\
│   │       │   │   │   ├── DictionaryDao.kt
│   │       │   │   │   └── SessionLogDao.kt
│   │       │   │   └── entities\
│   │       │   │       ├── AcademicTerm.kt
│   │       │   │       └── SessionLogEntity.kt
│   │       │   ├── decision\
│   │       │   │   └── DecisionEvaluator.kt     # Regla y* >= 0.65 (Sustitución Silenciosa)
│   │       │   └── telemetry\
│   │       │       └── SessionLogManager.kt     # Telemetría y cálculo formal de WER
│   │       └── presentation\                    # [Módulo 5: Presentación Accesible Jetpack Compose]
│   │           ├── TeleprompterScreen.kt        # Teleprompter 2-3 líneas continuo (WCAG AAA)
│   │           ├── NotesExporter.kt             # Exportación de notas a .txt y .pdf
│   │           ├── SUSQuestionnaireScreen.kt    # Cuestionario SUS (Anexo A de la tesis)
│   │           ├── theme\
│   │           │   ├── Color.kt
│   │           │   └── WCAGHighContrastTheme.kt # 19.56:1 (#FFFF00 sobre #000000)
│   │           └── viewmodel\
│   │               └── CaptionsViewModel.kt
│   └── src\test\java\com\unsa\dhh\captions\     # Pruebas Unitarias Algorítmicas
│       ├── FuzzyEngineTest.kt
│       ├── LevenshteinMatcherTest.kt
│       └── WerCalculatorTest.kt
└── evaluation\                                  # [Capítulo IV: Banco de Validación Experimental]
    ├── benchmark_runner.py                      # Ejecutor de métricas de precisión y latencia
    ├── corpus_arequipa_test.json                # Corpus de aula escolar con ruido y Ground Truth
    ├── interactive_tester.py                    # Prueba interactiva
    └── run_evaluation.bat                       # Script de ejecución con un clic
```

---

## 🚀 Cómo Ejecutar la Validación Experimental (Cálculo de WER)

Para ejecutar la contrastación de hipótesis y validar los resultados del Capítulo IV:

```bash
python d:\tesis\evaluation\benchmark_runner.py
```

o ejecutando directamente: `d:\tesis\evaluation\run_evaluation.bat`.

### Resultados Obtenidos en el Corpus Escolar:
- **WER Promedio ASR Crudo (Línea Base):** `23.16%`
- **WER Promedio con Capa Difusa On-Device:** `7.37%`
- **Reducción Relativa del Error (WER):** `68.18%`
- **Precisión Final del Sistema:** `92.63%` *(Supera la meta de tesis de $\ge 90\%$)*
- **Latencia Media de Procesamiento:** `7.57 ms` *(Muy por debajo del límite de $1000\text{ ms}$)*

---

## 📱 Despliegue en Android Studio
1. Abrir la carpeta `d:\tesis` en **Android Studio**.
2. Conectar la tableta Android (API 26+) or un emulador.
3. Compilar y ejecutar (`Run 'app'`).
4. La aplicación iniciará en modo **Teleprompter de Alto Contraste (WCAG AAA)**, solicitará permisos de micrófono y notificaciones, y cargará automáticamente en memoria los diccionarios escolares del MINEDU.


---

## 📚 Documentación y verificación

| Documento | Para qué |
|---|---|
| `docs/tesis/guia_del_codigo.md` | Recorrido del código, parámetros ajustables, tests y limitaciones |
| `docs/tesis/guia_exposicion_y_preguntas_defensa.md` | Cómo exponer la tesis, demo y banco de preguntas del jurado |
| `docs/tesis/factor_semantico_topic_clusters.md` | Factor S, memoria de clase, áreas temáticas y ablación |
| `docs/tesis/plan_de_pruebas_sx26_telefono_tablet.md` | Pruebas en hardware con el micrófono SX26 en teléfono y tablet |

```bash
sh scripts/descargar_modelo_vosk.sh            # una vez: modelo Vosk (~40 MB) en assets, fuera de git
./gradlew testDebugUnitTest assembleDebug      # 78 pruebas unitarias + APK
cd evaluation
python3 benchmark_runner.py                    # WER/KER
python3 benchmark_runner.py --ablacion         # aporte de memoria y rerank
python3 benchmark_runner.py --umbral           # sensibilidad de y* y falsos positivos
python3 benchmark_runner.py --booleana         # difuso vs. mejor regla booleana
python3 benchmark_runner.py --vosk             # salida real de Vosk: términos partidos ("sito plasma" -> citoplasma)
python3 generar_paridad_decision.py            # tras cambiar el algoritmo: fixture de paridad Kotlin <-> Python
python3 verificar_fcl.py                       # fuzzy_rules.fcl ≡ motor de la app
python3 ejecutor_pruebas_calidad.py            # casos SQA automatizables
```
