> **SUSTITUIDO por `plan_de_pruebas_ejecucion_v3.md`** (03/11/2026). Se conserva solo como historial.

# PLAN DE PRUEBAS DE EJECUCIÓN 2.0 — GUÍA PASO A PASO
**Tesis:** Mejora de la precisión de sistemas Speech-to-Text mediante lógica difusa (UNSA)  
**Responsable:** Bach. Miguel Angel Tinta Aguilar  
**Versión:** 03/11/2026 · Entrega de la tesis: 02/12/2026  
**Relación con otros documentos:** aplica el protocolo de la Sección 7 y la matriz de evidencias de la Sección 8 de `plan_maestro_subsanacion_observaciones_v2.md`. Sustituye, para la evaluación, al orden de `plan_de_pruebas_sx26_telefono_tablet.md` (cuyos casos se reutilizan aquí).

---

## ÍNDICE

0. [Qué hay de nuevo en la app (APK del 03/11)](#0-qué-hay-de-nuevo-en-la-app-apk-del-0311)
1. [Material y preparación](#1-material-y-preparación)
2. [Fase A — Pruebas técnicas en el dispositivo (HOY 03/11)](#2-fase-a--pruebas-técnicas-en-el-dispositivo-hoy-0311)
3. [Fase B — Piloto (fecha programada)](#3-fase-b--piloto-fecha-programada)
4. [Fase C — Evaluación en vivo (16–24/11)](#4-fase-c--evaluación-en-vivo-1624-11)
5. [Fase D — Después de cada sesión: descarga y referencia humana](#5-fase-d--después-de-cada-sesión-descarga-y-referencia-humana)
6. [Fase E — Análisis y resultados (24–28/11)](#6-fase-e--análisis-y-resultados-2428-11)
7. [Qué hacer si una prueba falla](#7-qué-hacer-si-una-prueba-falla)
8. [Hojas de registro](#8-hojas-de-registro)

---

## 0. QUÉ HAY DE NUEVO EN LA APP (APK DEL 03/11)

| Función | Para qué sirve en la tesis | Cómo se usa |
|---|---|---|
| **Registro por enunciado (CSV)** | Comparar las **mismas** transcripciones antes y después de la capa difusa (obs. 1.11 y dictamen general) | Automático. Cada enunciado final escribe una fila con texto crudo, texto corregido, texto mostrado, confianza de la API, perfil ASR, marcas de tiempo y detalle de D, S e y* por palabra |
| **Interruptor T0/T2** | Estudio intrasujeto de carga cognitiva y fatiga (HE6) | Panel del investigador. T0 muestra el crudo y T2 el corregido. **En ambas condiciones el CSV guarda los dos textos** |
| **Panel del investigador** (oculto) | Código del participante, etiqueta de sesión, condición, ruta del archivo | **Pulsación larga sobre el título "Prueba"** (solo en vertical). "APLICAR E INICIAR SESIÓN" limpia la pantalla y abre un archivo nuevo |
| **SUS guardado** | HE5 | El icono de cuestionario guarda las 10 respuestas en `sus_respuestas.csv` con el código del participante. Obliga a responder las 10 (antes valían 3 por defecto) y **ya no muestra el puntaje al estudiante** |
| **Línea `ASR02` en logcat** | Prueba de confianza (ASR-02) | `adb logcat -s AndroidSpeechEngine:I \| grep ASR02` |

**Archivos en la tableta:** `/sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/`

```
sesion_<fecha>_<participante>_<condicion>.csv     (una fila por enunciado)
sesion_<fecha>_<participante>_<condicion>_meta.txt (equipo, versión, condición)
sus_respuestas.csv                                 (acumulativo)
```

**Descarga:** `adb pull /sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/ datos_<fecha>/`

**Análisis:** `python3 evaluation/evaluar_sesiones.py datos_<fecha>/` (ver Fase E).

**Una sesión = un archivo = una condición.** Cambiar de condición, de participante o de asignatura abre un archivo nuevo.

---

## 1. MATERIAL Y PREPARACIÓN

### 1.1 Lista de material (revisar antes de cada salida)

- [ ] Tableta objetivo cargada al 100 % + cargador + cable USB-C
- [ ] Receptor USB-C del micrófono SX26/Maxtron + transmisor de solapa cargado (y el segundo transmisor de respaldo)
- [ ] Laptop con `adb` y el repositorio (para descargar datos al terminar)
- [ ] **Grabador de audio independiente** (segundo celular con grabadora) para la referencia humana. La app no graba audio, porque el micrófono lo ocupa el reconocedor
- [ ] App de sonómetro (p. ej., NIOSH SLM) en el celular grabador
- [ ] Formularios impresos: NASA-TLX, escala de fatiga, escala de Paas, prueba de comprensión (2 versiones), guía de entrevista, ficha de observación (Anexos E–G)
- [ ] Consentimientos y asentimientos firmados (sin ellos **no** se registra a ese participante)
- [ ] Cartilla de inducción para el docente y el estudiante
- [ ] Hoja de registro de esta guía (Sección 8)

### 1.2 Instalar el APK nuevo

```bash
cd "/home/miguel/Projects/Prototipo fuzzy/Prototipo-Fuzzy-Transcripcion"
./gradlew assembleDebug                      # ya compilado el 03/11 09:53
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant com.unsa.dhh.captions android.permission.RECORD_AUDIO
```

### 1.3 Etiquetas de sesión (importante para el análisis)

| Etiqueta | Uso | Cuenta como |
|---|---|---|
| `prueba` | Pruebas técnicas de hoy | No se analiza la precisión (sin referencia) |
| `dev` o `piloto` | Corpus de **desarrollo** (piloto) | Ajuste de la línea base T1 |
| `clase1`, `clase2`… | Corpus de **prueba** (evaluación en vivo) | Resultados de la tesis |

Códigos de participante: docentes `D01, D02…`; estudiantes `E01, E02…`. **Nunca nombres.**

---

## 2. FASE A — PRUEBAS TÉCNICAS EN EL DISPOSITIVO (HOY 03/11)

Duración estimada: 2 h 30 min. Hacerlas **en este orden**: las primeras deciden si tiene sentido seguir con ese equipo. Antes de empezar, abrir el panel del investigador y poner participante `INV`, etiqueta `prueba`, condición T2.

En la laptop, dejar corriendo durante toda la fase:
```bash
mkdir -p docs/tesis/evidencias
adb logcat -s AndroidSpeechEngine:I CaptionsViewModel:I ExternalAudioDeviceManager:I > docs/tesis/evidencias/logcat_03-11.txt
```

### A0. Datos del equipo (5 min) → evidencia E04
- [ ] `adb shell getprop ro.product.model` · `adb shell getprop ro.build.version.release`
- [ ] Versión de "Servicios de voz de Google" y de "Google" (Ajustes > Apps). Captura.
- [ ] Ajustes > Sistema > Idioma > Voz > Reconocimiento sin conexión: ¿español instalado? Captura.

### A1. VER-01 Funciones nuevas (10 min)
| Paso | Resultado esperado | OK |
|---|---|---|
| Pulsación larga sobre "Prueba" | Se abre el panel del investigador | ☐ |
| Poner `INV` / `prueba` / T2 → APLICAR | Pantalla limpia; el panel muestra la ruta `…/evaluacion/sesion_…_INV_T2_CORREGIDO.csv` | ☐ |
| Dictar 3 frases y ejecutar `adb shell ls /sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/` | Existe el CSV y su `_meta.txt` | ☐ |
| `adb pull` del CSV y abrirlo | 3 filas, con `texto_crudo` y `texto_corregido` | ☐ |
| Cambiar a T0 → APLICAR → dictar "la cedula y la mitocondria" | En pantalla aparece el **crudo**; el CSV nuevo (`…_T0_SIN_CORRECCION.csv`) tiene en `texto_corregido` la versión corregida | ☐ |
| Ver la parte superior de la pantalla | Muestra "Ciencia y Tecnología" fijo (sin selector de asignaturas) y los botones A− / A+ | ☐ |
| Pulsar A+ tres veces y A− una vez | El texto crece y luego se reduce; el porcentaje pasa 100 % → 130 % → 120 %; en el CSV, `escala_texto` = 1.2 | ☐ |
| Girar el equipo a horizontal | El texto ocupa toda la pantalla y **mantiene el tamaño de teléfono o tableta** (en el CSV/meta, `tipo_dispositivo` correcto) | ☐ |
| Abrir el cuestionario SUS | El botón dice "Responde las 10 preguntas (0/10)" y se habilita al completar; al guardar, aparece "¡Gracias!" sin puntaje | ☐ |
| `adb shell cat …/evaluacion/sus_respuestas.csv` | Una fila con `INV` y el puntaje | ☐ |

Si algo falla aquí, avísame antes de seguir: la evaluación depende de esto.

### A2. HW-01 Micrófono USB (10 min) → E07
- [ ] Conectar el receptor **antes** de iniciar la escucha. `adb shell dumpsys audio | grep -iE "usb|input" | head -20`
- [ ] La app muestra "Micrófono de solapa inalámbrico…" (toast) y el indicador de fuente externa.
- [ ] Dictar con el transmisor a 15–20 cm de la boca, de espaldas a la tableta y a 5 m: transcribe.
- [ ] Captura de pantalla + salida de `dumpsys` en la evidencia.

### A3. ASR-01 Sin conexión (15 min) → E04 · **decisiva**
- [ ] Activar el **modo avión** (Wi-Fi y datos apagados; Bluetooth también). Captura de la barra de estado.
- [ ] Panel: etiqueta `offline`. Dictar las 5 primeras frases de `evaluation/guion_docente_biologia.md`.
- [ ] Revisar en el CSV la columna `perfil_asr`: debe decir "reconocimiento local del equipo" o "sin conexión preferido". **Si dice "en línea", la prueba FALLA.**
- [ ] Captura de la pantalla con el texto transcrito **y** el icono del avión visible.

### A4. ASR-02 Confianza (15 min) → E05 · **decisiva para la variable independiente**
- [ ] Siguiendo en modo avión, etiqueta `asr02`, dictar 20 frases del guion.
- [ ] En el CSV, columna `confianza_api`: contar cuántas filas tienen números entre 0 y 1 y cuántas `NA`, `-1` o `0`. También queda en `logcat_03-11.txt` (líneas `ASR02`).
- [ ] Anotar: n válidas / 20, valor mínimo, máximo y si varía entre frases.
- [ ] **Interpretación:**
  - Válidas y variables → C informa; redactar 1.5.1 con esos números.
  - Siempre `NA` o un valor fijo → C = 0.65 constante; redactar que la decisión depende de D y S, y la comparación T1/T2 se vuelve central.

### A5. ASR-03 Continuidad (10 min) → E06
- [ ] Grabar con el celular grabador mientras se lee el guion **3 minutos sin pausas**.
- [ ] Comparar el texto con el guion: contar las palabras perdidas en los cortes entre enunciados. Criterio: < 2 % del total.
- [ ] Anotar si suena un pitido en cada reinicio.

### A6. ASR-04 Silencio (5 min) → E06
- [ ] 60 s en silencio y luego hablar. Criterio: retoma solo, sin tocar la pantalla, en < 1 s (ver `ms_inicio_voz_a_primer_parcial` de esa fila).

### A7. HW-02 Desconexión (5 min) → E07
- [ ] Con la escucha activa, desconectar el receptor. Debe aparecer el aviso "Dongle USB desconectado…" y seguir transcribiendo con el micrófono interno, sin cerrarse.
- [ ] Reconectar: vuelve al micrófono externo. Columna `fuente_audio` del CSV: refleja el cambio.

### A8. PERF-01 Rendimiento (15 min) → E09
- [ ] Durante una lectura de 5 minutos: `adb shell dumpsys meminfo com.unsa.dhh.captions | grep -E "TOTAL|Java Heap|Native Heap"` (3 veces: al inicio, a los 2 min y a los 5 min).
- [ ] En el CSV de esa sesión: `ms_capa_difusa` (meta p95 < 100 ms) y `ms_fin_voz_a_publicado` (meta p95 < 1000 ms). El script de la Fase E calcula los percentiles.
- [ ] Opcional (verificación externa de latencia): grabar en video a 60 fps la boca del lector y la pantalla en el mismo encuadre durante 10 frases; medir cuadro a cuadro el fin de la frase → aparición del texto.

### A9. CLASE-45 Autonomía (45 min; si no hay tiempo hoy, se hace en el piloto) → E08
- [ ] Batería y temperatura al inicio: `adb shell dumpsys battery | grep -E "level|temperature"`.
- [ ] 45 min de lectura o audio de una clase (puede ser un video de una clase de Biología reproducido por un parlante a 1 m del transmisor).
- [ ] Criterio: sin cierres ni bloqueos; batería y temperatura al final anotadas.

### A10. Cierre de la Fase A (15 min)
- [ ] `adb pull /sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/ docs/tesis/evidencias/datos_03-11/`
- [ ] `python3 evaluation/evaluar_sesiones.py docs/tesis/evidencias/datos_03-11/` → secciones 3 y 4 del reporte (latencia y confianza). Las de precisión quedan vacías porque no hay referencia.
- [ ] Llenar la hoja de registro (8.1) y renombrar las capturas como E04_…, E05_…, etc.
- [ ] Redactar 1.6.1 con los resultados (plantilla en el plan 2.0, sección 1.6.1).

---

## 3. FASE B — PILOTO (FECHA PROGRAMADA)

**Objetivos:** (1) grabar el corpus de **desarrollo** (1 docente, ≥ 15 min de Biología); (2) probar los instrumentos con 1 estudiante; (3) medir el ruido; (4) ensayar los tiempos; (5) CLASE-45 si no se hizo hoy.

### B1. Antes (día anterior)
- [ ] Confirmar la hora con el docente; pedirle el tema de la clase y preparar con él **dos pruebas de comprensión** de 8 a 10 preguntas (versión A para la primera mitad de la clase y versión B para la segunda).
- [ ] Consentimiento del docente firmado (grabación de voz); consentimiento del apoderado y asentimiento del estudiante.
- [ ] Tableta cargada, transmisor cargado, celular grabador con espacio.

### B2. En el aula (cronograma de la sesión)
| Min | Acción | Registro |
|---|---|---|
| −15 | Medir el ruido 2 min con el aula en silencio y anotar L_Aeq en dB(A) | Ficha de observación |
| −10 | Colocar el transmisor al docente (15–20 cm de la boca, en la solapa); conectar el receptor a la tableta; panel: `D01` / `piloto` / **T2** | Ficha |
| −5 | Inducción de 10 min al estudiante (cartilla) y **fatiga (pre)** antes de iniciar la sesión | Ficha + formulario |
| 0 | Iniciar el grabador y la escucha. **El docente dice: "Inicio de sesión piloto"** (frase de sincronización) | — |
| 0–20 | Mitad 1 de la clase con condición asignada (en el piloto: **T0**) | Hora de inicio y fin |
| 20 | Estudiante: NASA-TLX + Paas + fatiga (post) + comprensión A (≈ 7 min) | Formularios |
| 27 | Panel: cambiar a **T2** → APLICAR. Fatiga (pre). El docente repite la frase de sincronización | — |
| 27–47 | Mitad 2 de la clase con **T2** | — |
| 47 | NASA-TLX + Paas + fatiga (post) + comprensión B + **SUS en la app** | Formularios + app |
| 55 | Entrevista breve al estudiante (5 min) y al docente (5 min) | Guía de entrevista |
| 60 | Medir ruido de nuevo con la clase en curso (si es posible, durante la clase) | Ficha |

Para el corpus de desarrollo basta el audio del docente y los CSV; los dos archivos del piloto (T0 y T2) contienen ambos textos.

### B3. Después del piloto (ese mismo día)
- [ ] Descargar los datos y la grabación (Fase D).
- [ ] Anotar qué falló o tardó más de lo previsto y ajustar los tiempos de la Fase C.
- [ ] Revisar con el estudiante si entendió cada ítem de los formularios. Anotar los ítems confusos y ajustarlos (esto cuenta como **piloto de comprensión de los instrumentos**, obs. 1.13).
- [ ] Transcribir la referencia del piloto (Fase D). Luego ejecutar el análisis: con la etiqueta `piloto`, este corpus ajusta la línea base T1.
- [ ] **Congelar parámetros:** desde aquí no se modifican el código, los diccionarios ni los umbrales. `git tag tesis-eval-v1` y anotar el hash (evidencia E14).

---

## 4. FASE C — EVALUACIÓN EN VIVO (16–24/11)

### C1. Metas mínimas
| Elemento | Mínimo | Ideal |
|---|---|---|
| Docentes en el corpus de prueba | 2 (distintos del piloto) | 3 |
| Habla total de prueba | 30 min | ≥ 45 min |
| Estudiantes con ambas condiciones | 3 | **≥ 6** |

> **Por qué 6 estudiantes:** con la prueba de Wilcoxon exacta, el p más pequeño posible con 5 participantes es 0.0625. Con 5 o menos, la hipótesis HE6 **no puede** alcanzar p < 0.05 aunque todos mejoren; solo se reporta de forma descriptiva. Con 6, el mínimo es 0.031.

### C2. Asignación de condiciones (contrabalanceo)
| Participante | Mitad 1 | Mitad 2 |
|---|---|---|
| E01 | T0 | T2 |
| E02 | T2 | T0 |
| E03 | T0 | T2 |
| E04 | T2 | T0 |
| E05 | T0 | T2 |
| E06 | T2 | T0 |

Si dos estudiantes están en la misma aula, cada uno usa su propia tableta y condición. Si solo hay una tableta, una sesión por estudiante.

**Comprensión:** la versión A corresponde siempre a la mitad 1 y la B a la mitad 2, sea cual sea la condición. El estudiante no sabe qué condición ve; las instrucciones solo dicen "parte 1" y "parte 2".

### C3. Guion de cada sesión
Igual que el piloto (B2), con estos cambios:
- Etiqueta de sesión `clase1`, `clase2`…; participante el código del **estudiante** (el docente se anota en la ficha de observación).
- Las condiciones siguen la tabla C2.
- Si en la sesión no hay estudiante (solo corpus del docente): participante `D0x`, condición T2, sin formularios.

### C4. Qué NO hacer durante la evaluación
- No cambiar el código ni los diccionarios (parámetros congelados).
- No mirar el CSV ni la pantalla mientras se transcribe la referencia.
- No explicarle al estudiante qué es la "corrección".
- No borrar archivos de la tableta hasta haberlos descargado **y** respaldado.

---

## 5. FASE D — DESPUÉS DE CADA SESIÓN: DESCARGA Y REFERENCIA HUMANA

### D1. Descarga y respaldo (10 min, el mismo día)
```bash
adb pull /sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/ datos_<fecha>/
cp <grabacion>.m4a datos_<fecha>/audio/<nombre_de_la_sesion>.m4a   # mismo nombre que el CSV
python3 evaluation/evaluar_sesiones.py datos_<fecha>/ --plantilla    # crea datos_<fecha>/referencias/*.txt
```
Respaldar `datos_<fecha>/` en un segundo lugar (USB o nube personal). **El audio y los formularios contienen datos de menores: no subirlos al repositorio.**

### D2. Transcripción de referencia (revisor 1)
1. Abrir `referencias/<sesion>.txt` y el audio. **No abrir el CSV.**
2. La plantilla tiene una línea `NNN | ` por enunciado con su ventana de tiempo aproximada (segundos desde la frase de sincronización). Escribir después de `|` lo que **dijo** el docente, palabra por palabra:
   - números en letras ("dos mil", no "2000");
   - sin muletillas ("eh", "este") ni repeticiones involuntarias;
   - palabra ininteligible → `[inaudible]` (no cuenta como palabra);
   - si en esa ventana no se dijo nada → `-`.
3. Alternativa si las ventanas no cuadran: borrar las líneas `NNN |` y escribir la clase completa como texto continuo. El script reparte el texto entre enunciados automáticamente.

### D3. Revisión (revisor 2) y consenso
1. El revisor 2 escucha el audio con la transcripción del revisor 1 y marca las discrepancias.
2. Antes de corregir, guardar una copia: `referencias_revisor1/`. Así se puede reportar el acuerdo entre revisores (WER entre ambas versiones; meta < 5 %).
3. Las discrepancias se resuelven juntos, escuchando el audio. Esa versión final queda en `referencias/`.

**Tiempo estimado:** de 4 a 6 minutos de trabajo por minuto de audio y por revisor. Para 45 minutos de audio, unas 4 horas cada uno: distribuirlas durante la semana de evaluación.

---

## 6. FASE E — ANÁLISIS Y RESULTADOS (24–28/11)

### E1. Juntar los datos
Copiar en una sola carpeta (`datos_final/`) todos los `sesion_*.csv`, `sus_respuestas.csv` y la carpeta `referencias/`. Llenar los formularios en papel en `usuarios.csv`:
```bash
python3 evaluation/evaluar_sesiones.py datos_final/ --plantilla-usuarios   # crea usuarios.csv de ejemplo
```
Columnas: `participante, condicion (T0/T2), orden (1/2), tlx_1…tlx_6 (0–100), paas (1–9), fatiga_pre, fatiga_post (0–10), comprension_aciertos, comprension_total`.

### E2. Ejecutar
```bash
python3 evaluation/evaluar_sesiones.py datos_final/ --usuarios datos_final/usuarios.csv \
        --terminos-clave datos_final/terminos_clave.txt   # opcional: lista revisada por el docente
```
`terminos_clave.txt` tiene un término por línea. Prepáralo **antes** de transcribir las referencias, partiendo del diccionario y con la revisión del docente. Si no lo pasas, el KER usa los 403 términos del diccionario. El reporte indica qué lista se usó.
Genera:
- `reporte_evaluacion.md` con todas las tablas;
- `detalle_enunciados.csv` con referencia, T0, T1, T2 y WER por enunciado (para el anexo);
- `sustituciones_erroneas.csv` con todas las falsas correcciones y los cambios inútiles (el asesor lo pide expresamente).

### E3. Dónde va cada resultado en el Capítulo IV
| Sección del reporte | Hipótesis | Tabla del Cap. IV |
|---|---|---|
| 1. Precisión — PRUEBA (WER, CER, KER, TCR, FCR) | HE1, HE2, HE3 | 4.1 Precisión textual por condición |
| 1. Reducción relativa + IC 95 % + Wilcoxon | HE1 | 4.2 Contraste de hipótesis |
| Lista de sustituciones erróneas | HE1 (FCR) | Anexo: sustituciones erróneas |
| 2. Precisión — DESARROLLO | — | Solo como ajuste de T1 (no son resultados) |
| 3. Desempeño temporal | HE4 | 4.3 Latencia |
| 4. Confianza y perfil ASR | HI1 | 4.4 Verificación técnica (con la hoja 8.1) |
| 5. SUS | HE5 | 4.5 Usabilidad |
| 5. TLX, Paas, fatiga, comprensión | HE6 | 4.6 Exploratorio: carga cognitiva y fatiga |
| Entrevistas (análisis temático) | — | 4.7 Resultados cualitativos y triangulación |

### E4. Validación con jueces (precisión semántica)
De `detalle_enunciados.csv`, elegir al azar 60 enunciados en los que T0 y T2 difieran. Mostrar a cada juez (docente de Biología y especialista SAANEE), por separado y sin decirle cuál es cuál, la referencia y la versión T2. El juez clasifica cada enunciado en *conserva el significado*, *lo distorsiona* o *lo pierde*. Calcular el κ de Cohen entre jueces. Si no da tiempo, declararlo como limitación.

---

## 7. QUÉ HACER SI UNA PRUEBA FALLA

| Falla | Consecuencia para la tesis | Acción |
|---|---|---|
| ASR-01: no transcribe en modo avión | **No** se puede afirmar funcionamiento on-device en ese equipo | Instalar el paquete de español sin conexión y repetir. Si sigue fallando, probar otro equipo; si ninguno pasa, redactar 1.6.1 y el alcance como "requiere conexión en [equipo]" |
| `perfil_asr` = "en línea" en sesiones de evaluación | Esos enunciados no fueron locales | Reportar cuántos; no ocultarlos |
| ASR-02: confianza siempre `NA` o fija | C no discrimina | Declararlo en 1.5.1. La comparación T1/T2 (HE3) pasa a ser la prueba central del aporte de Mamdani |
| ASR-03: pérdida ≥ 2 % en los reinicios | Afecta la continuidad | Reportar el porcentaje medido; proponer mejora como trabajo futuro |
| HW-01: la tableta no reconoce el receptor | No hay captura externa | Probar otro cable OTG o el teléfono; en último caso, micrófono interno y declararlo |
| CLASE-45: se cierra o se calienta | Afecta la viabilidad operativa | Anotar el minuto y repetir con la pantalla en brillo medio y el cargador conectado |
| Llegan menos de 3 estudiantes autorizados | HE5 y HE6 sin muestra | Contingencia del plan 2.0: evaluación algorítmica con docentes; HE5 y HE6 como estudio de casos o no contrastadas |

---

## 8. HOJAS DE REGISTRO

### 8.1 Pruebas técnicas (Fase A)
Equipo: ______________________ Android: _____ Speech Services: _________ Fecha: 03/11/2026

| Caso | Resultado (OK / FALLA) | Valor medido | Evidencia (archivo) | Observaciones |
|---|---|---|---|---|
| A0 Datos del equipo | | | E04_equipo.png | |
| VER-01 Funciones nuevas | | | VER01_*.png / csv | |
| HW-01 Micrófono USB | | | E07_dumpsys.txt | |
| ASR-01 Sin conexión | | perfil_asr = | E04_modo_avion.png | |
| ASR-02 Confianza | | válidas __/20; rango __–__ | E05_asr02.csv | |
| ASR-03 Continuidad | | pérdida __ % | E06_continuidad | |
| ASR-04 Silencio | | __ ms | E06_silencio | |
| HW-02 Desconexión | | | E07_desconexion.png | |
| PERF-01 Rendimiento | | p95 capa __ ms; p95 latencia __ ms; RAM __ MB | E09_* | |
| CLASE-45 | | batería __ % → __ %; temp __ → __ °C | E08_* | |

### 8.2 Sesiones de evaluación (Fases B y C)
| Fecha | IE | Docente | Estudiante | Etiqueta | Orden (1.ª/2.ª) | Archivos CSV | Audio | Ruido dB(A) silencio / clase | Formularios | Incidencias |
|---|---|---|---|---|---|---|---|---|---|---|
| | | D01 | E__ | piloto | T0 / T2 | | | / | ☐ | |
| | | | | | | | | / | ☐ | |
| | | | | | | | | / | ☐ | |

### 8.3 Referencias humanas
| Sesión | Min de audio | Revisor 1 (fecha) | Revisor 2 (fecha) | WER entre revisores | Consenso (fecha) |
|---|---|---|---|---|---|
| | | | | | |

### 8.4 Formularios en papel (resumen para `usuarios.csv`)
**NASA-TLX (0–100, en pasos de 5):** 1 demanda mental · 2 demanda física · 3 demanda temporal · 4 desempeño (0 = perfecto, 100 = fracaso) · 5 esfuerzo · 6 frustración.  
**Paas (1–9):** "¿Cuánto esfuerzo mental hiciste para seguir la clase?" 1 = muy, muy poco … 9 = muchísimo.  
**Fatiga (0–10):** "¿Qué tan cansado(a) te sientes ahora?" 0 = nada … 10 = muchísimo (con caritas). Se toma antes y después de cada mitad.  
**Comprensión:** n.º de aciertos / n.º de preguntas de la versión (A o B).
