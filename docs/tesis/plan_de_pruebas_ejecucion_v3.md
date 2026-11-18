# PLAN DE PRUEBAS DE EJECUCIÓN 3.0 — GUÍA PASO A PASO
**Tesis:** Mejora de la precisión de sistemas Speech-to-Text mediante lógica difusa (UNSA)  
**Responsable:** Bach. Miguel Angel Tinta Aguilar  
**Versión:** 04/11/2026 (actualizada) · Entrega de la tesis: 02/12/2026  
**Sustituye a:** `plan_de_pruebas_ejecucion_v2.md`. Alineado con la tesis corregida (`tesis_latex/`): ámbito de la **ciudad de Arequipa**, área de **Ciencia y Tecnología**, pruebas en **teléfono (Tecno Spark 20, Android 13) y tableta**, y los objetivos e hipótesis específicos OE1–OE7 y HE1–HE7, emparejados uno a uno.

**Cambios del 04/11:** la transcripción de referencia la hace **una sola persona**; la precisión semántica se valida con **un solo juez** (el docente del área, sin κ de Cohen); los instrumentos los revisa **el docente** (sin juicio de expertos); no hay mediación en LSP; las letras de los anexos y del Capítulo IV se actualizaron; se añadieron los pasos de `adb` y los ajustes del Tecno Spark 20.

---

## ÍNDICE

0. [Mapa: qué prueba sustenta cada hipótesis](#0-mapa-qué-prueba-sustenta-cada-hipótesis)
1. [Calendario de pruebas](#1-calendario-de-pruebas)
2. [La app en una página](#2-la-app-en-una-página)
3. [Material y preparación](#3-material-y-preparación)
4. [Fase G — Gestión: permisos y ética](#4-fase-g--gestión-permisos-y-ética)
5. [Fase P — Preparación de materiales e instrumentos](#5-fase-p--preparación-de-materiales-e-instrumentos)
6. [Fase A — Pruebas técnicas en los dos dispositivos](#6-fase-a--pruebas-técnicas-en-los-dos-dispositivos)
7. [Fase B — Piloto y congelación de parámetros](#7-fase-b--piloto-y-congelación-de-parámetros)
8. [Fase C — Evaluación en vivo](#8-fase-c--evaluación-en-vivo)
9. [Fase D — Después de cada sesión: descarga y referencia humana](#9-fase-d--después-de-cada-sesión-descarga-y-referencia-humana)
10. [Fase E — Análisis y resultados](#10-fase-e--análisis-y-resultados)
11. [Qué hacer si una prueba falla](#11-qué-hacer-si-una-prueba-falla)
12. [Hojas de registro](#12-hojas-de-registro)
13. [Lista final antes de escribir el Capítulo IV](#13-lista-final-antes-de-escribir-el-capítulo-iv)

---

## 0. MAPA: QUÉ PRUEBA SUSTENTA CADA HIPÓTESIS

Cada fila indica qué hay que hacer, qué evidencia queda y en qué fase se hace. Si una hipótesis no tiene evidencia al final, se declara como **no contrastada**.

| Hipótesis / objetivo | Qué se mide | Prueba | Evidencia que queda | Fase | Responde con |
|---|---|---|---|---|---|
| **OE1 / HE1** Corpus y diccionarios: referencia hecha a mano; desarrollo y prueba sin grabaciones compartidas | Diccionario (403 términos); minutos de corpus | Corpus de desarrollo y de prueba; transcripción manual con segunda escucha | `referencias/` | P, B, C, D | Cap. IV |
| **OE2 / HE2** Reconocimiento: sin conexión, confianza y captura continua 45 min | `perfil_asr`, validez de `CONFIDENCE_SCORES`; cierres, batería, temperatura | ASR-01, ASR-02 en cada dispositivo; CLASE-45 | Capturas con modo avión; `ASR02.txt`; columna `perfil_asr`; registro + batería/temperatura | A, B | Tabla 1.4, Tabla 3.1, Anexo B |
| **OE3 / HE3** Corrección: equivalencia con la FCL; p95 capa < 100 ms | Diferencia máxima FCL–motor; `ms_capa_difusa` | `verificar_fcl.py`; pruebas unitarias; PERF-01 + sesiones | Salida de `verificar_fcl.py`; CSV | A, C, E | Cap. IV |
| **OE4 / HE4** Interfaz: contraste ≥ 7:1; p95 latencia < 1 s | Contraste del tema; `ms_fin_voz_a_publicado` | PERF-01 + sesiones; verificación con video | CSV; video | A, C, E | Cap. IV |
| **OE5 / HE5** Evaluación de la capa: (a) WER T2 < T0, reducción ≥ 30 %, FCR ≤ 1 %; (b) precisión semántica ≥ 90 %; (c) T2 ≥ T1; (d) meta secundaria WER T2 ≤ 10 % | WER, CER, KER, TCR, FCR; Wilcoxon; WER y FCR de T1 | Corpus de prueba + referencias; lista de términos clave; T1 ajustada solo con el corpus de desarrollo | `reporte_evaluacion.md`, `sustituciones_erroneas.csv`, `terminos_clave.txt` | P, B, C, D, E | Cap. IV; Anexo I |
| **OE6 / HE6** Usabilidad: SUS ≥ 68 | SUS por participante y dispositivo; 4 ítems complementarios | SUS en la app tras la sesión | `sus_respuestas.csv` | B, C, E | Cap. IV; Anexo I |
| **OE7 / HE7** Carga cognitiva (NASA-TLX, exploratoria) | NASA-TLX Raw (6 subescalas) | Comparación T0 frente a T2, intrasujeto | `nasa_tlx_respuestas.csv` (app) | C, E | Cap. IV; Anexo I |
| Precisión semántica válida | Relación entre el juicio del docente y el KER | Validación con un juez (docente del área) | Hoja del juez | E | Cap. IV |
| Instrumentos comprensibles | Revisión del docente + piloto | Revisión de instrumentos | Notas de la revisión; cambios | P, B | — |
| Entrevistas | Temas y triangulación | Entrevistas semiestructuradas | Transcripciones, matriz | C, E | Cap. IV |
| Ruido del aula | L_Aeq de fondo: aula en silencio y con alumnos presentes mientras el docente no habla | Sonómetro | Ficha de observación (Anexo C) | B, C | Cap. IV (condición del entorno) |
| Viabilidad operativa | Autorizaciones, instalación, incidencias | Gestión + ficha de observación | Cartas, permisos | G | Cap. 1.6.2 |

---

## 1. CALENDARIO DE PRUEBAS

Alineado con el cronograma de la tesis (08/09–02/12/2026). Los hitos en **negrita** no se pueden mover.

| Fecha | Qué | Fase | Entregable |
|---|---|---|---|
| **31/10–05/11** | Pruebas técnicas en teléfono **y** tableta | A | Hoja 12.1 por dispositivo + evidencias |
| **03–04/11** | Cartas a las direcciones de las IE; consulta sobre comité de ética | G | Cartas entregadas (acuse) |
| 04–06/11 | Lista de términos clave con el docente; materiales impresos | P | `terminos_clave.txt` |
| 07–12/11 | Revisión de los instrumentos por el docente | P | Notas de la revisión |
| **por el piloto** | Piloto: corpus de desarrollo, prueba de instrumentos con 1 estudiante, ruido | B | CSV y audio del piloto |
| tras el piloto | Congelación de parámetros (etiqueta de versión) | B | Hash en la tesis |
| **14/11** | Permisos recibidos | G | Autorizaciones firmadas |
| 16–24/11 | Evaluación en vivo: corpus de prueba y estudiantes | C | CSV, audio, formularios |
| 17–25/11 | Referencias humanas (1 transcriptor), en paralelo | D | Referencias revisadas |
| 24–28/11 | Análisis y redacción del Capítulo IV | E | Reporte; Cap. IV |
| **02/12** | Entrega | — | Tesis final |

---

## 2. LA APP EN UNA PÁGINA

| Función | Para qué sirve | Cómo se usa |
|---|---|---|
| **Registro por enunciado (CSV)** | Comparar las mismas transcripciones antes y después de la capa difusa | Automático. Guarda texto crudo, corregido y mostrado, confianza, perfil ASR, tiempos, `tipo_dispositivo`, `escala_texto` y el detalle de D, S e y* por palabra |
| **Interruptor T0/T2** | Comparación intrasujeto (HE7) | Panel del investigador. En ambas condiciones el CSV guarda los dos textos |
| **Panel del investigador** | Código del participante, etiqueta, condición | **Pulsación larga sobre el título "Prueba"** (solo en vertical). Interruptor **"Mostrar corrección difusa"**: encendido = T2, apagado = T0. "APLICAR E INICIAR SESIÓN" limpia la pantalla y abre un archivo nuevo |
| **Área fija "Ciencia y Tecnología"** | La investigación se limita a esta área | No hay selector de asignatura. El diccionario interno se llama `BIOLOGIA` |
| **A− / A+** | El estudiante ajusta el tamaño de letra (80–160 %, pasos de 10 %) | Botones a la derecha de la etiqueta "Ciencia y Tecnología". Se registra en `escala_texto` |
| **SUS** | HE6 | Icono de cuestionario; obliga a responder los 10 ítems; no muestra el puntaje; guarda también el tipo de dispositivo |
| **Línea `ASR02` en logcat** | ASR-02 | `adb logcat -s AndroidSpeechEngine:I \| grep ASR02` |

**Archivos en el dispositivo:** `/sdcard/Documents/PrototipoFuzzy/` (una carpeta por sesión; en Android 8–10: `/sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/`)
```
sesion_<fecha>_<participante>_<condicion>.csv       una fila por enunciado
sesion_<fecha>_<participante>_<condicion>_meta.txt  equipo, versión, tipo de dispositivo
sus_respuestas.csv                                  acumulativo (incluye tipo_dispositivo)
```
**Una sesión = un archivo = una condición.** Cambiar de condición, de participante o de etiqueta abre un archivo nuevo.

---

## 3. MATERIAL Y PREPARACIÓN

### 3.1 Lista de material (revisar antes de cada salida)

- [ ] **Teléfono** y **tableta** Android cargados al 100 %, cargadores y cable USB-C/OTG
- [ ] Receptor USB-C del micrófono 2.4 GHz + transmisor de solapa cargado (y transmisor de respaldo)
- [ ] Laptop con `adb` y el repositorio (si falta `adb`: `sudo pacman -S android-tools`)
- [ ] **Grabador de audio independiente** (otro celular). La app no graba audio
- [ ] App de sonómetro (p. ej., NIOSH SLM) en el celular grabador; verificada con un sonido conocido
- [ ] Formularios impresos: ficha de observación (Anexo C) y guía de entrevista (Anexo D). El NASA-TLX (Anexo E) y el SUS (Anexo A) se responden en la app
- [ ] Consentimientos y asentimientos **firmados** (formato propio; ya no va como anexo de la tesis): sin ellos no se registra al participante
- [ ] Cartilla de inducción para docente y estudiante
- [ ] Hojas de registro de la Sección 12

### 3.2 Compilar e instalar (en los dos dispositivos)

**Antes de instalar, en el dispositivo:**
- [ ] Depuración USB: *Ajustes → Acerca del teléfono →* tocar 7 veces **Número de compilación**; luego *Sistema → Opciones de desarrollador → Depuración USB*. Conectar y aceptar el aviso.
- [ ] Paquete de voz sin conexión: app *Google → Ajustes → Voz → Reconocimiento de voz sin conexión* → **Español (Estados Unidos)**. Es el que usa la app (es-US).
- [ ] **Solo Tecno / HiOS:** *Ajustes → Batería →* la app en **Sin restricciones** y permitirla en *Gestión de inicio de apps*. Si no, HiOS puede cerrar la captura en segundo plano y CLASE-45 fallará.

```bash
cd "/home/miguel/Projects/Prototipo fuzzy/Prototipo-Fuzzy-Transcripcion"
./gradlew testDebugUnitTest assembleDebug        # deben pasar las 71 pruebas
sha256sum app/build/outputs/apk/debug/app-debug.apk | tee docs/tesis/evidencias/apk_sha256.txt
adb devices                                      # conectar un dispositivo a la vez, o usar -s <serie>
adb -s <serie> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serie> shell pm grant com.unsa.dhh.captions android.permission.RECORD_AUDIO
```

### 3.3 Etiquetas de sesión

| Etiqueta | Uso | Cuenta como |
|---|---|---|
| `prueba`, `offline`, `asr02` | Pruebas técnicas de la Fase A | No se analiza la precisión (sin referencia) |
| `dev` o `piloto` | Corpus de **desarrollo** | Ajuste de la línea base T1 |
| `clase1`, `clase2`… | Corpus de **prueba** | Resultados de la tesis |

Códigos: docentes `D01, D02…`; estudiantes `E01, E02…`. **Nunca nombres.**

---

## 4. FASE G — GESTIÓN: PERMISOS Y ÉTICA

Es la fase que más tarda y bloquea las demás. **Empezar hoy.** Plazo de respuesta: 14/11.

### G1. Qué documentos se necesitan

| Documento | A quién | Contenido mínimo | Estado |
|---|---|---|---|
| Carta de presentación y solicitud de autorización | Dirección de cada IE candidata | Objetivo, qué se hará, cuántas sesiones, duración, qué datos se recogen, garantía de anonimato. Pedir además la verificación de los criterios de inclusión de los estudiantes y apoyo para contactar a las familias | ☐ |
| Consentimiento del docente | Cada docente grabado | Grabación de voz solo con fines de investigación, código en lugar de nombre | ☐ |
| Consentimiento del apoderado | Padre, madre o apoderado de cada estudiante | Formato propio (ya no va como anexo de la tesis) | ☐ |
| Asentimiento del estudiante | Cada estudiante | Versión en lenguaje simple con pictogramas | ☐ |
| Consulta sobre comité de ética | Escuela Profesional / Facultad | Preguntar si un estudio con menores y datos de voz requiere aprobación y qué documentos exige | ☐ |

### G2. Instituciones candidatas

Partir del Anexo H de la tesis: los 28 estudiantes están en 25 IE de nueve distritos. Prioridad: distritos con más casos (Cerro Colorado, Alto Selva Alegre, Arequipa) y las IE con dos o más estudiantes. Enviar cartas a **más IE de las necesarias** (al menos 6): algunas no responderán a tiempo.

| IE | Distrito | UGEL | Estudiantes (censo) | Carta enviada | Respuesta | Autorizada | Participantes |
|---|---|---|---|---|---|---|---|
| | | | | | | | |
| | | | | | | | |

### G3. Qué hacer según la respuesta

- **Autorizan** → verificar con la dirección y el docente de aula los estudiantes que cumplen el criterio (lectura funcional en castellano) y programar sesiones.
- **No responden el 10/11** → llamar o visitar. A los 14/11, usar solo las IE autorizadas.
- **Menos de 3 estudiantes autorizados el 14/11** → ver la contingencia de la Sección 11.

---

## 5. FASE P — PREPARACIÓN DE MATERIALES E INSTRUMENTOS

### P1. Lista de términos clave (para el KER)
- [ ] Partir de `app/src/main/assets/dictionaries/biologia.json` (648 términos desde el 17/11: biología, química y ciclos biogeoquímicos).
- [ ] El docente de Ciencia y Tecnología revisa la lista: quita términos que no usará en la clase y añade los de la sesión.
- [ ] Guardar como `terminos_clave.txt` (un término por línea). Admite términos de varias palabras.
- [ ] **Hacerlo antes de transcribir las referencias.** Si se ajusta mirando los resultados, el KER pierde validez.

### P2. Revisión de los instrumentos por el docente (07–12/11)
- [ ] El docente de Ciencia y Tecnología revisa el SUS (Anexo A), la guía de entrevista (Anexo D), el NASA-TLX (Anexo E) y el asentimiento.
- [ ] Por cada ítem, pedirle si es claro y adecuado para los estudiantes, y anotar sus comentarios.
- [ ] Registrar los cambios hechos (hoja 12.4).

### P3. Materiales del piloto
- [ ] Guion de sesión de la Sección 7 impreso.
- [ ] Cartilla visual de inducción (docente: colocación del micrófono; estudiante: iniciar la escucha, A−/A+, qué hacer si el texto se detiene).
- [ ] Frase de sincronización definida: *"Inicio de sesión"* (la dice el docente al empezar y tras cada cambio de condición).
- [ ] Sonómetro probado.

---

## 6. FASE A — PRUEBAS TÉCNICAS EN LOS DOS DISPOSITIVOS

**Repetir toda la fase en el teléfono y en la tableta.** Los resultados pueden diferir (versión de Android, paquete de voz). Duración: unas 2 h 30 min por dispositivo. Orden fijo: las primeras pruebas deciden si tiene sentido seguir con ese equipo. Las frases de cada prueba están en `evaluation/guion_docente_biologia.md` (tabla "Qué frases usar en cada prueba").

**Teléfono:** Tecno Spark 20, Android 13 (HiOS 13.5.0), Servicios de voz de Google 0928.02 (ya registrado en la tesis). **Tableta:** pendiente.

Antes de empezar, en el panel del investigador: participante `INV`, etiqueta `prueba`, condición T2. En la laptop, dejar corriendo:
```bash
mkdir -p docs/tesis/evidencias
adb -s <serie> logcat -s AndroidSpeechEngine:I CaptionsViewModel:I ExternalAudioDeviceManager:I \
    > docs/tesis/evidencias/logcat_<telefono|tableta>_03-11.txt
```

### A0. Datos del equipo (5 min) → E04
- [ ] `adb -s <serie> shell getprop ro.product.model` y `ro.build.version.release`
- [ ] Versión de "Servicios de voz de Google" y de "Google" (Ajustes > Apps). Captura.
- [ ] Ajustes > Idioma > Voz > Reconocimiento sin conexión: ¿español instalado? Captura.

### A1. VER-01 Funciones de la app (10 min)
| Paso | Resultado esperado | OK |
|---|---|---|
| Pulsación larga sobre "Prueba" | Se abre el panel del investigador | ☐ |
| `INV` / `prueba` / T2 → APLICAR | Pantalla limpia; el panel muestra la ruta `…/sesion_…_INV_T2_CORREGIDO.csv` | ☐ |
| Dictar 3 frases; `adb shell ls …/evaluacion/` | Existe el CSV y su `_meta.txt` | ☐ |
| `adb pull` del CSV | 3 filas con `texto_crudo` y `texto_corregido` | ☐ |
| Cambiar a T0 → APLICAR → dictar "la cedula y la mitocondria" | En pantalla aparece el **crudo**; el CSV nuevo tiene la versión corregida en `texto_corregido` | ☐ |
| Ver la parte superior | "Ciencia y Tecnología" fijo (sin selector) y botones A− / A+ | ☐ |
| A+ tres veces y A− una vez | 100 % → 130 % → 120 %; en el CSV `escala_texto` = 1.2 | ☐ |
| Girar a horizontal | Texto a pantalla completa; **mantiene el tamaño propio del dispositivo**; `tipo_dispositivo` correcto en el CSV y el `_meta` | ☐ |
| Cuestionario SUS | El botón dice "Responde las 10 preguntas (0/10)"; al guardar, "¡Gracias!" sin puntaje | ☐ |
| `adb shell cat …/sus_respuestas.csv` | Una fila con `INV`, el tipo de dispositivo y el puntaje | ☐ |

**Criterio de parada:** si algo de A1 falla, no seguir: la evaluación depende de esto.

### A2. HW-01 Micrófono USB (10 min) → E07
- [ ] Conectar el receptor **antes** de iniciar la escucha. `adb shell dumpsys audio | grep -iE "usb|input" | head -20`
- [ ] La app muestra "Micrófono de solapa inalámbrico…" (aviso) y el indicador de fuente externa.
- [ ] Dictar con el transmisor a 15–20 cm de la boca, de espaldas al equipo y a 5 m de distancia: transcribe.
- [ ] Captura + salida de `dumpsys`.

### A3. ASR-01 Sin conexión (15 min) → E04 · **decisiva**
- [ ] Modo avión activo, Wi-Fi, datos y Bluetooth apagados. Captura de la barra de estado.
- [ ] Etiqueta `offline`. Dictar las 5 primeras frases de `evaluation/guion_docente_biologia.md`.
- [ ] En el CSV, `perfil_asr` debe decir "reconocimiento local del equipo" o "sin conexión preferido". **Si dice "en línea", FALLA.**
- [ ] Captura con el texto transcrito y el icono del avión.

### A4. ASR-02 Confianza (15 min) → E05 · **decisiva para la variable independiente**
- [ ] En modo avión, etiqueta `asr02`, dictar las 20 frases del guion (Bloques 1 a 4).
- [ ] Contar en `confianza_api` cuántas filas tienen valores entre 0 y 1 y cuántas `NA`, `-1` o `0`. También queda en el log (líneas `ASR02`).
- [ ] Anotar n válidas / 20, mínimo, máximo y si varía entre frases.
- [ ] **Interpretación:** válidas y variables → C informa; fija o `NA` → C = 0.65 constante y la comparación T1/T2 (HE5c) pasa a ser la prueba central.

### A5. ASR-03 Continuidad (10 min) → E06
- [ ] Grabar con el celular grabador mientras se leen los Bloques 1 a 4 seguidos, **unos 3 minutos sin pausas**.
- [ ] Contar las palabras perdidas en los cortes entre enunciados. Criterio: < 2 %. Anotar si suena un pitido en cada reinicio.

### A6. ASR-04 Silencio (5 min) → E06
- [ ] 60 s de silencio y luego hablar. Criterio: retoma solo en < 1 s (`ms_inicio_voz_a_primer_parcial` de esa fila).

### A7. HW-02 Desconexión (5 min) → E07
- [ ] Con la escucha activa, desconectar el receptor: aparece el aviso y sigue con el micrófono interno, sin cerrarse. Reconectar: vuelve al externo. `fuente_audio` del CSV refleja el cambio.

### A8. PERF-01 Rendimiento (15 min) → E09
- [ ] Durante 5 minutos de lectura: `adb shell dumpsys meminfo com.unsa.dhh.captions | grep -E "TOTAL|Java Heap|Native Heap"` (al inicio, a los 2 y a los 5 minutos).
- [ ] En el CSV: `ms_capa_difusa` (p95 < 100 ms) y `ms_fin_voz_a_publicado` (p95 < 1000 ms).

### A8b. Verificación externa de la latencia con video (20 min) → E09
La app mide el tiempo desde que termina la voz hasta que el texto se entrega a la interfaz; el video comprueba que lo que ve el estudiante coincide.
- [ ] Con otro celular, grabar a **60 fps** el equipo (pantalla visible) y, en el mismo encuadre, la fuente de sonido. 10 frases.
- [ ] Para cada frase, avanzar cuadro a cuadro (VLC, `ffmpeg` o cualquier editor): cuadro donde termina la última palabra (onda de audio o boca) → primer cuadro en que el texto de esa frase aparece. Cada cuadro son 16.7 ms.
- [ ] Anotar las 10 diferencias y compararlas con `ms_fin_voz_a_publicado` de las mismas frases. Diferencia sistemática grande → anotarla como limitación (el reconocedor, no la app, decide cuándo declara "fin de voz").

### A9. CLASE-45 Autonomía (45 min; si hoy no hay tiempo, en el piloto) → E08
- [ ] `adb shell dumpsys battery | grep -E "level|temperature"` al inicio y al final.
- [ ] 45 minutos de una clase real o de un video de una clase de Ciencia y Tecnología reproducido a 1 m del transmisor.
- [ ] Criterio: sin cierres ni bloqueos. Anotar el minuto de cualquier incidente.

### A10. Cierre de la fase (15 min)
- [ ] `adb pull …/evaluacion/ docs/tesis/evidencias/datos_<dispositivo>_03-11/`
- [ ] `python3 evaluation/evaluar_sesiones.py docs/tesis/evidencias/datos_<dispositivo>_03-11/` → secciones 3 y 4 del reporte (latencia y confianza, ya separadas por dispositivo).
- [ ] Llenar la hoja 12.1 y renombrar las capturas E04_…, E05_…, etc.
- [ ] Con los resultados se llenan la Tabla 1.4 (1.6.1), la Tabla 3.1 de requisitos, el Anexo B, HE2 y el párrafo sobre la confianza en 1.5.1.

---

## 7. FASE B — PILOTO Y CONGELACIÓN DE PARÁMETROS

**Objetivos:** (1) corpus de **desarrollo** (1 docente, ≥ 15 min); (2) probar los instrumentos con 1 estudiante; (3) medir ruido; (4) ensayar tiempos; (5) CLASE-45 si no se hizo; (6) **congelar los parámetros**.

### B1. Antes (día anterior)
- [ ] Hora confirmada con el docente; tema de la clase conocido (contenido de **biología** del área de Ciencia y Tecnología, para que el diccionario aplique).
- [ ] Términos clave listos (P1).
- [ ] Consentimiento del docente, del apoderado y asentimiento del estudiante **firmados**.
- [ ] Dispositivo y transmisor cargados, grabador con espacio, sonómetro.
- [ ] Decidir qué dispositivo usará el estudiante y **mantenerlo en las dos mitades**.

### B2. En el aula
| Min | Acción | Registro |
|---|---|---|
| −15 | Medir ruido 2 min con el aula en silencio y anotar L_Aeq en dB(A) | Ficha de observación |
| −10 | Colocar el transmisor al docente (15–20 cm de la boca); conectar el receptor; colocar el equipo sobre la carpeta del estudiante; panel: `E01` / `piloto` / condición de la mitad 1 | Ficha |
| −5 | Inducción de 10 min al estudiante (cartilla) | Ficha + formulario |
| 0 | Iniciar grabador y escucha. El docente dice la **frase de sincronización** | — |
| 0–20 | Mitad 1 (en el piloto: **T0**) | Hora de inicio y fin |
| 20 | **NASA-TLX en la app** (≈ 3 min), botón del cerebro. Se responde **antes** de cambiar de condición: la app guarda la condición activa | App |
| 27 | Panel: cambiar a **T2** → APLICAR. El docente repite la frase de sincronización | — |
| 27–47 | Mitad 2 con **T2** | — |
| 47 | **NASA-TLX en la app** y después **SUS en la app** (14 preguntas) | App |
| 55 | Entrevista breve al estudiante (5 min) y al docente (5 min) | Guía de entrevista |
| 60 | Medir ruido de fondo 2 min con los alumnos presentes y el docente en pausa (p. ej., durante un trabajo individual). No medir mientras el docente habla: el sonómetro registraría su voz, no el ruido | Ficha |

### B3. Después del piloto (el mismo día)
- [ ] Descargar datos y grabación (Fase D).
- [ ] Anotar qué falló o tardó más de lo previsto y ajustar la Fase C.
- [ ] Preguntar al estudiante qué ítems de los formularios no entendió. Esto es el **piloto de comprensión de los instrumentos**: registrar los cambios (hoja 12.4).
- [ ] Transcribir la referencia del piloto (Fase D) y ejecutar el análisis: con la etiqueta `piloto`, este corpus ajusta los umbrales de T1.
- [ ] Revisar en `sustituciones_erroneas.csv` del piloto si hay falsas correcciones. El umbral τ = 0.65 y el piso D ≥ 0.70 se mantienen salvo que el piloto muestre un problema claro; si se cambian, anotarlo y justificarlo con esos datos (el barrido de la tabla de sensibilidad del umbral del Capítulo 3 es con corpus simulados).

### B4. Congelación de parámetros (después del piloto, antes de la primera sesión de prueba)
Desde aquí **no se modifican** el código, los diccionarios ni los umbrales. Si hay un error grave, se corrige y se **repite la congelación**, anotándolo.
```bash
cd "/home/miguel/Projects/Prototipo fuzzy/Prototipo-Fuzzy-Transcripcion"
git status --short                                  # debe estar limpio o con los cambios confirmados
git add -A && git commit -m "Versión congelada para la evaluación"   # solo si hay cambios pendientes
git tag tesis-eval-v1 && git rev-parse HEAD | tee docs/tesis/evidencias/hash_congelado.txt
sha256sum app/build/outputs/apk/debug/app-debug.apk app/src/main/assets/dictionaries/biologia.json \
          app/src/main/assets/fuzzy_rules.fcl | tee -a docs/tesis/evidencias/hash_congelado.txt
```
- [ ] Escribir el hash y la fecha en la tesis (Sección 1.11 y Capítulo IV).
- [ ] Reinstalar **ese mismo APK** en los dos dispositivos.

---

## 8. FASE C — EVALUACIÓN EN VIVO

### C1. Metas mínimas
| Elemento | Mínimo | Ideal |
|---|---|---|
| Docentes en el corpus de prueba | 2 (distintos del piloto) | 3 |
| Habla total de prueba | 30 min | ≥ 45 min |
| Estudiantes con ambas condiciones | 3 | **≥ 6** |

> **Por qué 6 estudiantes:** con Wilcoxon exacto, el menor $p$ posible con 5 participantes es 0.0625. Con 5 o menos, HE7 no puede llegar a $p<0.05$ aunque todos mejoren; solo se reporta de forma descriptiva. Con 6, el mínimo es 0.031.

### C2. Asignación de condiciones (contrabalanceo)
| Participante | Mitad 1 | Mitad 2 |
|---|---|---|
| E01 | T0 | T2 |
| E02 | T2 | T0 |
| E03 | T0 | T2 |
| E04 | T2 | T0 |
| E05 | T0 | T2 |
| E06 | T2 | T0 |

- **Cada estudiante usa el mismo dispositivo (teléfono o tableta) en las dos mitades.** Si no, el cambio de pantalla se mezcla con el efecto de la corrección.
- Si dos estudiantes comparten aula, cada uno usa su propio equipo y su propia condición.
- El estudiante no sabe qué condición ve; se le dice solo "parte 1" y "parte 2".

### C3. Guion de cada sesión
Igual que B2, con estos cambios:
- Etiqueta `clase1`, `clase2`…; participante = código del **estudiante**.
- Condiciones según C2. Anotar en la ficha el dispositivo usado.
- Si no hay estudiante (solo corpus del docente): participante `D0x`, condición T2, sin formularios.

### C4. Qué NO hacer durante la evaluación
- No cambiar el código, los diccionarios ni los umbrales.
- No mirar el CSV ni la pantalla mientras se transcribe la referencia.
- No explicar al estudiante qué es la "corrección".
- No borrar archivos del dispositivo antes de descargarlos **y** respaldarlos.

---

## 9. FASE D — DESPUÉS DE CADA SESIÓN: DESCARGA Y REFERENCIA HUMANA

### D1. Descarga y respaldo (10 min, el mismo día)
```bash
adb -s <serie> pull /sdcard/Documents/PrototipoFuzzy/ datos_<fecha>/
cp <grabacion>.m4a datos_<fecha>/<nombre_de_la_sesion>/<nombre_de_la_sesion>.m4a   # mismo nombre que el CSV
python3 evaluation/evaluar_sesiones.py datos_<fecha>/ --plantilla    # crea datos_<fecha>/referencias/*.txt
```
Respaldar `datos_<fecha>/` en un segundo lugar. **El audio y los formularios contienen datos de menores: no subirlos al repositorio.**

### D2. Transcripción de referencia (una persona)
1. Abrir `referencias/<sesion>.txt` y el audio. **No abrir el CSV ni la pantalla de la app.**
2. Cada línea `NNN | ` es un enunciado, con su ventana de tiempo aproximada (segundos desde la frase de sincronización). Escribir lo que **dijo** el docente, palabra por palabra:
   - números en letras ("dos mil", no "2000");
   - sin muletillas ("eh", "este") ni repeticiones involuntarias;
   - palabra ininteligible → `[inaudible]`;
   - si no se dijo nada en esa ventana → `-`.
3. Si las ventanas no cuadran: borrar las líneas `NNN |` y escribir la clase como texto continuo; el script la reparte.

### D3. Segunda escucha de revisión
1. Al terminar, la misma persona vuelve a escuchar el audio completo con la transcripción delante y corrige lo que falte o sobre.
2. Esa versión revisada queda en `referencias/` y es la referencia.
3. Como hay un solo transcriptor, no se calcula el acuerdo entre revisores; se declara como limitación.

**Tiempo:** de 4 a 6 minutos de trabajo por minuto de audio (unas 4 horas para 45 min de audio). Distribuirlas durante la semana de evaluación.

---

## 10. FASE E — ANÁLISIS Y RESULTADOS

### E1. Reunir los datos
En `datos_final/`: todos los `sesion_*.csv`, `sus_respuestas.csv`, `nasa_tlx_respuestas.csv`, `referencias/` y `terminos_clave.txt`. Los dos cuestionarios salen de la app con el mismo `adb pull`; no hay que transcribir nada.

La app solo guarda las respuestas, sin puntajes; el SUS y la media del NASA-TLX los calcula el script.

`sus_respuestas.csv`: `fecha, participante, etiqueta, tipo_dispositivo, item1…item10 (1–5), comp_legibilidad, comp_velocidad, comp_comodidad, comp_distraccion (1–5, sin invertir)`.

`nasa_tlx_respuestas.csv`: `fecha, participante, etiqueta, tipo_dispositivo, condicion (T0/T2), orden (1/2), tlx_1…tlx_6 (0–100)`. Si un estudiante respondió dos veces la misma condición, el script usa la última y lo avisa en el reporte. Solo si alguna sesión se hizo en papel: `--plantilla-usuarios` crea `usuarios.csv` y se pasa con `--usuarios`.

### E2. Ejecutar
```bash
python3 evaluation/evaluar_sesiones.py datos_final/ \
        --terminos-clave datos_final/terminos_clave.txt
```
Genera `reporte_evaluacion.md`, `detalle_enunciados.csv` (referencia, T0, T1, T2 y WER por enunciado) y `sustituciones_erroneas.csv` (todas las falsas correcciones y cambios inútiles). El reporte indica la lista de términos clave usada y separa latencia y SUS por dispositivo.

### E3. Dónde va cada resultado en el Capítulo IV
El Capítulo IV está vacío por ahora (solo el título); las tablas se crearán al redactarlo.

| Sección del reporte | Hipótesis | Tabla de la tesis |
|---|---|---|
| 1. Precisión (WER, CER, KER, TCR, FCR) | HE5 | Cap. IV (por crear) |
| 1. Reducción relativa, IC 95 %, Wilcoxon | HE5 (a, c, d) | Cap. IV (por crear) |
| Sustituciones erróneas | HE5 (FCR) | Anexo I, tabla de sustituciones |
| 2. Precisión — desarrollo | — | Solo ajuste de T1; no son resultados |
| 3. Desempeño temporal (por dispositivo) | HE3, HE4 | Cap. IV (por crear) |
| 4. Confianza y perfil ASR | HE2 | Tabla 1.4, Anexo B y hoja 12.1 |
| 5. SUS (con dispositivo) | HE6 | Cap. IV; Anexo I, valores individuales |
| 5. NASA-TLX | HE7 | Cap. IV; Anexo I, valores individuales |
| Entrevistas (análisis temático) | — | Cap. IV (por crear) |

### E4. Análisis temático de las entrevistas
- [ ] Transcribir las entrevistas (códigos, sin nombres).
- [ ] Seis fases: familiarización, codificación inicial, búsqueda de temas, revisión, definición y redacción (Braun y Clarke, 2006).
- [ ] Matriz de triangulación: cada tema, una cita representativa y la métrica con que se cruza (FCR, latencia, SUS, NASA-TLX).

### E5. Validación con un juez (precisión semántica)
- [ ] De `detalle_enunciados.csv`, elegir al azar 60 enunciados en los que T0 y T2 difieran.
- [ ] Mostrar al docente del área, sin decirle cuál es cuál, la referencia y la versión T2.
- [ ] Clasificar: *conserva el significado*, *lo distorsiona* o *lo pierde*. Calcular la relación de su juicio con el KER. No hay κ de Cohen: con un solo juez no hay concordancia entre evaluadores (limitación declarada en la tesis).
- [ ] Si no da tiempo, declararlo como limitación.

---

## 11. QUÉ HACER SI UNA PRUEBA FALLA

| Falla | Consecuencia para la tesis | Acción |
|---|---|---|
| ASR-01: no transcribe en modo avión | **No** se puede afirmar funcionamiento local en ese equipo | Instalar el paquete de español sin conexión y repetir. Si sigue, probar otro equipo; si ninguno pasa, redactar 1.6.1 y el alcance como "requiere conexión en [equipo]" |
| Un dispositivo pasa y el otro no | El alcance se limita al que pasa | Usar solo ese en la evaluación y declararlo |
| `perfil_asr` = "en línea" en sesiones de evaluación | Esos enunciados no fueron locales | Reportar cuántos; no ocultarlos |
| ASR-02: confianza siempre `NA` o fija | C no discrimina | Declararlo en 1.5.1; HE5c pasa a ser la prueba central |
| ASR-03: pérdida ≥ 2 % | Afecta la continuidad | Reportar el porcentaje; proponer mejora como trabajo futuro |
| HW-01: no reconoce el receptor | No hay captura externa | Otro cable OTG o el otro dispositivo; en último caso, micrófono interno y declararlo |
| CLASE-45: se cierra o se calienta | Afecta la viabilidad operativa | Anotar el minuto; repetir con brillo medio y cargador conectado |
| Video de latencia difiere mucho de `ms_fin_voz_a_publicado` | La latencia percibida es mayor que la medida | Reportar ambas y explicar la diferencia |
| Menos de 3 estudiantes autorizados el 14/11 | HE6 y HE7 sin muestra suficiente | Evaluación algorítmica con docentes (no depende de menores); HE6 y HE7 como estudio de casos o declaradas no contrastadas. No sustituir por estudiantes oyentes sin decirlo |
| Un docente rechaza la grabación | Menos corpus | Buscar otro docente; el mínimo son 2 en el corpus de prueba |
| Error grave en el código tras la congelación | Hay que repetir | Corregir, repetir la congelación (B4) y reprocesar; anotarlo en la tesis |

---

## 12. HOJAS DE REGISTRO

### 12.1 Pruebas técnicas (Fase A) — una hoja por dispositivo
Dispositivo: ☐ Teléfono ☐ Tableta · Modelo: ______________ Android: _____ Speech Services: _________ Fecha: ___/___/2026  
*(Teléfono: Tecno Spark 20 · Android 13 (HiOS 13.5.0) · Speech Services 0928.02)*

| Caso | Resultado (OK / FALLA) | Valor medido | Evidencia (archivo) | Observaciones |
|---|---|---|---|---|
| A0 Datos del equipo | | | E04_equipo.png | |
| VER-01 Funciones de la app | | | VER01_* | |
| HW-01 Micrófono USB | | | E07_dumpsys.txt | |
| ASR-01 Sin conexión | | perfil_asr = | E04_modo_avion.png | |
| ASR-02 Confianza | | válidas __/20; rango __–__ | E05_asr02.csv | |
| ASR-03 Continuidad | | pérdida __ % | E06_continuidad | |
| ASR-04 Silencio | | __ ms | E06_silencio | |
| HW-02 Desconexión | | | E07_desconexion.png | |
| PERF-01 Rendimiento | | p95 capa __ ms; p95 latencia __ ms; RAM __ MB | E09_* | |
| Video de latencia | | media de las 10 diferencias __ ms | E09_video | |
| CLASE-45 | | batería __ % → __ %; temp __ → __ °C | E08_* | |

### 12.2 Sesiones (Fases B y C)
| Fecha | IE | Docente | Estudiante | Dispositivo | Etiqueta | Orden | Archivos CSV | Audio | Ruido dB(A) silencio / clase | Formularios | Incidencias |
|---|---|---|---|---|---|---|---|---|---|---|---|
| | | D01 | E__ | | piloto | T0 / T2 | | | / | ☐ | |
| | | | | | | | | | / | ☐ | |
| | | | | | | | | | / | ☐ | |

### 12.3 Referencias humanas
| Sesión | Min de audio | Transcripción (fecha) | Segunda escucha (fecha) | Observaciones |
|---|---|---|---|---|
| | | | | |

### 12.4 Revisión de los instrumentos por el docente
| Instrumento | Comentarios del docente (claridad / adecuación) | Cambios realizados |
|---|---|---|
| SUS | | |
| NASA-TLX | | |
| Guía de entrevista | | |
| Asentimiento | | |

### 12.5 Cuestionarios en la app (resumen)
**SUS:** 10 ítems (1–5) + 4 complementarios: legibilidad, velocidad, comodidad y distracción (inverso).  
**NASA-TLX (0–100, pasos de 5):** 1 demanda mental · 2 demanda física · 3 demanda temporal · 4 desempeño (0 = perfecto, 100 = fracaso) · 5 esfuerzo · 6 frustración.  

---

## 13. LISTA FINAL ANTES DE ESCRIBIR EL CAPÍTULO IV

- [ ] Hoja 12.1 completa **para los dos dispositivos**, con evidencias E04–E09.
- [ ] Parámetros congelados antes del corpus de prueba, con hash anotado en la tesis (E14).
- [ ] `terminos_clave.txt` fijado antes de transcribir las referencias.
- [ ] Referencias transcritas a mano y revisadas con una segunda escucha.
- [ ] Ningún enunciado de evaluación con `perfil_asr` en línea sin reportar.
- [ ] `reporte_evaluacion.md` generado con el corpus de prueba, no con el de desarrollo.
- [ ] SUS y NASA-TLX (T0 y T2) de cada participante en `sus_respuestas.csv` y `nasa_tlx_respuestas.csv`, con el dispositivo registrado.
- [ ] Autorizaciones, consentimientos y asentimientos archivados (sin datos personales en el repositorio).
- [ ] Cada hipótesis tiene su evidencia o está declarada como **no contrastada** con el motivo.
- [ ] Los marcadores `\pend` de la tesis se sustituyen **solo** con datos de este plan; ninguno se llena con una estimación.
