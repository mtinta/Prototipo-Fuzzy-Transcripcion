# Guía para Exponer la Tesis y Banco de Preguntas del Jurado

**Para quién es:** el tesista, para preparar la sustentación. No es para entregar al jurado.
**Cómo usarla:** lee la sección 1 (dónde eres fuerte y dónde te pueden atacar), ensaya la 2 y la 3, memoriza la 4 y la 5, y estudia las respuestas de la 6 en voz alta. La sección 7 es lo que conviene hacer *antes* de la defensa.

---

## 1. Diagnóstico honesto: fortalezas y puntos débiles

**Lo que puedes defender con evidencia reproducible**

- El sistema **funciona de extremo a extremo**: compila, 68 pruebas automáticas pasan, el benchmark se ejecuta con un comando.
- **No modifica lenguaje cotidiano:** 0 sustituciones en 12 frases de control, y pruebas que lo exigen incluso con un tema activo y confianza baja.
- **Cada decisión es explicable:** el historial muestra palabra original → corregida con C, D, S, y* y tema.
- El anexo formal (`fuzzy_rules.fcl`) **es exactamente** el motor que ejecuta la app (verificado en 4 913 puntos, diferencia máxima 3·10⁻¹⁶).
- Los números del informe son **los que salen del código**, no cifras sueltas.

**Lo que NO puedes afirmar (y un jurado atento puede descubrir)**

| Debilidad | Por qué importa | Cómo responder |
|---|---|---|
| Los corpus son **sintéticos** (8, 13 y 12 frases con errores inventados) | La reducción de WER no prueba nada sobre voz real | Decirlo tú primero: "es una validación de factibilidad; la validación en campo es el siguiente paso" |
| Las mejoras se **decidieron mirando los mismos corpus** donde se miden | Resultado "dentro de la muestra": puede no generalizar | Declararlo. Ver pregunta E5 |
| La **comparación contra una regla booleana** es el ataque directo al título de la tesis | Con confianzas sintéticas hay empate técnico; con confianza neutra gana lo difuso, pero ambas se ajustaron sobre los mismos datos | Ver pregunta B1. No lo escondas |
| La **confianza C** que da Android es una por frase, y a veces no existe | El factor C puede ser casi constante | Ver pregunta D3 |
| Precisión Semántica (1−KER) = **93.55 %** solo en el corpus sintético y dentro de la muestra | Cumple la meta de 90 %, pero es una validación preliminar | Presentarlo como factibilidad, no como cumplimiento de la meta (pregunta E3) |
| **Sin pruebas con estudiantes sordos** ni SUS aplicado | La hipótesis de ergonomía no está contrastada | Ver pregunta F2 |
| **Sin pruebas con el SX26 real** hasta ahora | El enrutamiento del audio no está verificado | Ver pregunta D1 y el plan de pruebas |

La estrategia más sólida en la defensa es **nombrar tú estas limitaciones antes de que las pregunten**: transmite control del tema y resta fuerza al ataque.

---

## 2. Mensaje central (memorízalo)

> "Los reconocedores de voz fallan justo en el vocabulario técnico que un estudiante sordo necesita leer. Propongo una capa ligera, que corre en el teléfono, que corrige en silencio esas palabras **solo cuando tres señales coinciden**: el reconocedor duda (C), la palabra se parece mucho a un término del currículo (D) y el término encaja con el tema que se está explicando (S). Es explicable, no usa red y, en un banco de pruebas controlado, redujo el error de palabras de 18 % a 2 % sin tocar lenguaje cotidiano."

Las tres ideas que el jurado debe llevarse: **(1)** corrige solo con evidencia triple, **(2)** es auditable y offline, **(3)** la validación en campo es la etapa siguiente.

---

## 3. Estructura de la exposición (≈ 25 min + preguntas)

| Min | Diapositiva | Qué decir | Evidencia a mostrar |
|---|---|---|---|
| 0-2 | 1. Problema | Estudiantes sordos dependen del subtitulado; el ASR falla con terminología ("cédula" por "célula"). Un error en un término clave cambia el significado. | Ejemplo real de error del corpus |
| 2-4 | 2. Pregunta, objetivo, hipótesis | Lee la pregunta de investigación y la hipótesis; indica que hay una hipótesis general y tres específicas. | Texto de tu capítulo I |
| 4-6 | 3. Solución en una imagen | Diagrama del flujo: micrófono → ASR → corrección difusa → teleprompter. | Diagrama de la guía del código §1 |
| 6-9 | 4. Las tres variables | C, D, S con un ejemplo numérico: "la sedula tiene nucleo y citoplasma". | Tabla de `guia_del_codigo.md` §3 |
| 9-11 | 5. Motor Mamdani | 27 reglas (3×3×3), AND=min, centroide; decisión y* ≥ 0.65. Mostrar 2 reglas de ejemplo. | `fuzzy_rules.fcl`; mención a la verificación FCL≡app |
| 11-13 | 6. Factor semántico | Topic Clusters (12 en Biología) + memoria de la clase que decae con el tiempo. | Esquema de pesos por cluster |
| 13-14 | 7. Salvaguardas | Tres condiciones para sustituir: y* ≥ 0.65, D ≥ 0.70, no ser plural/singular. Por qué: se encontraron falsos positivos. | Caso "produce"→"profase" |
| 14-16 | 8. Hardware y arquitectura | Micrófono de solapa inalámbrico SX26 (receptor USB-C), servicio en primer plano, fallback al micrófono interno. | Insignia de fuente de audio |
| 16-20 | 9. **Demo en vivo** | Ver sección 4. | La app |
| 20-23 | 10. Resultados | Tabla del banco de pruebas con **sus advertencias**. | Sección 5 |
| 23-24 | 11. Limitaciones | Corpus sintético, comparación booleana, confianza C, falta de pruebas con usuarios. | Sección 1 |
| 24-25 | 12. Conclusiones y trabajo futuro | Qué se logró y los 4 pasos siguientes (sección 8). | – |

Regla de oro: **no más de 12 diapositivas, pocas palabras, una idea por diapositiva.** Tu mejor material es la demo y el ejemplo numérico.

---

## 4. Demo en vivo

**Preparación (la noche anterior):** instala el APK, concede permisos, selecciona **Biología**, pulsa "Limpiar" (reinicia la memoria de clase; el orden de las frases importa porque la memoria arrastra lo anterior), deja el panel de pruebas visible. Ten el teléfono cargado y una captura de pantalla de respaldo de cada paso.

**Guion**

1. **Fuente de audio.** Muestra la insignia (micrófono del teléfono). Conecta el receptor del SX26: la insignia cambia y aparece el aviso. *Mensaje: detección automática, sin configurar nada.* (Solo si probaste esto con el aparato real; si no, omítelo y no lo prometas.)
2. **Corrección con evidencia.** En el panel de pruebas inyecta, en este orden: `la cedula y la mitocondria` (es el ejemplo que muestra la propia app) y luego `la sedula tiene nucleo y citoplasma`. Resultados verificados: "la célula y la mitocondria" y "la célula tiene núcleo y citoplasma". Abre el **Historial de Correcciones** y muestra C, D, S, y* y el tema. *Mensaje: nada es caja negra.*
3. **El contexto decide.** Inyecta solo `sedula`. Con el tema de célula ya activo, el sistema **prefiere no corregir** antes que elegir "médula" (resultado verificado: queda "sedula"). Explica que "sedula" está igual de cerca de "médula" y "célula", y que en una sesión recién limpiada el sistema sí la convertiría en "médula": es la ambigüedad que el contexto mitiga pero no elimina. *No muestres este último caso salvo que te lo pregunten.*
4. **No toca lo cotidiano.** Inyecta `ayer fuimos al parque con mis amigos y jugamos futbol`. Debe salir idéntico. *Mensaje: seguridad ante todo.*
5. **(Opcional, impresiona)** Inyecta `el sitoplasma contiene el riposoma y protege el nuleolo` → "el citoplasma contiene el ribosoma y protege el nucleolo", y luego solo `mitocis` → "mitosis" (la clase viene de hablar de la célula).
6. **Teleprompter.** Muestra el texto amarillo sobre negro, 2-3 líneas. Contraste 19.56:1 (el estándar AAA exige 7:1).
7. **(Opcional)** Desconecta el receptor: fallback al micrófono interno con aviso.

**Plan B si falla la app:** ejecuta en la terminal `python3 evaluation/benchmark_runner.py` (corre en segundos) y muestra la tabla; o usa tus capturas. Practica la transición para que no se note pánico.

> **Precaución:** el panel de pruebas inyecta frases con confianza fija 0.45. No digas que es "voz real": di "frase simulada con ruido de reconocimiento".

---

## 5. Cifras que puedes citar (siempre con su calificativo)

| Qué | Valor | Calificativo obligatorio |
|---|---|---|
| WER del ASR crudo → con capa difusa (8 frases) | 18.45 % → 1.94 % (−89.5 % relativo) | Corpus sintético de 8 frases de Biología; mejoras decididas mirando este mismo corpus |
| WER en clase continua (13 frases) | 17.24 % → 1.72 % | Corpus sintético |
| KER (términos clave) | 61.29 % → 6.45 % (precisión semántica 93.55 %) | Cumple 90 % **solo en este corpus sintético**, dentro de la muestra |
| Falsos positivos | 0 en 12 frases cotidianas | Corpus de control pequeño (≈110 palabras) |
| Con confianza neutra 0.65 (caso realista) | 4.85 % → 2.91 % al sumar memoria y rerank | Cada componente suma: 4.85 → 3.88 → 2.91 % |
| Mejor regla booleana (ajustada sobre los mismos datos) | Conf. sintética: 2.91 % / 0.00 %. Conf. neutra: 4.85 % / 3.45 % | Frente al difuso completo (1.94 / 1.72 y 2.91 / 1.72): empate técnico con conf. sintética; gana lo difuso con conf. neutra |
| Latencia de la capa difusa | ≈ 7 ms por oración en JVM de escritorio | **No medida en teléfono todavía** |
| Contraste del teleprompter | 19.56 : 1 | AAA exige ≥ 7:1 |
| Umbral y* | 0.65 | Resultados idénticos entre 0.60 y 0.75; con ≤ 0.55 aparece 1 falso positivo |
| Verificación FCL ↔ app | diferencia máx. 3·10⁻¹⁶ en 4 913 puntos | – |
| Pruebas automáticas | 58 pasan | No sustituyen la prueba con hardware real |

**No cites:** "23.16 % → 7.37 %", "92.63 %" ni "21:1" (cifras de versiones anteriores del informe, ya corregidas). Tampoco cites números de este documento sin su calificativo "corpus sintético, dentro de la muestra".

---

## 6. Banco de preguntas y respuestas modelo

> Las respuestas son guías: aprende la **idea**, no las palabras.

### A. Problema y justificación

**A1. ¿Por qué no usar simplemente un mejor reconocedor de voz?**
Los reconocedores generales no conocen el vocabulario curricular de secundaria ni el acento de aula. Una capa posterior es barata, funciona con cualquier ASR instalado y es auditable. Es complementaria: si en el futuro el ASR mejora, la capa deja de corregir porque la confianza sube y las reglas lo impiden.

**A2. ¿Por qué no sesgar el reconocedor con una lista de palabras o usar un modelo grande (Whisper/LLM)?**
Son alternativas válidas y conviene compararlas en trabajo futuro (versiones recientes de Android permiten pasar términos de sesgo al reconocedor; verifícalo antes de citarlo). Mi restricción de diseño fue **ejecutar en teléfonos de gama media, sin red y con latencia baja**, y que cada corrección se pueda explicar a un docente. No evalué esas alternativas, y lo reconozco como limitación.

**A3. ¿Por qué estudiantes sordos y no oyentes?**
Para un estudiante sordo el texto *es* el canal: un término mal escrito no tiene un respaldo auditivo. Un error en "mitosis/meiosis" puede cambiar el concepto estudiado.

**A4. ¿De dónde salen el diccionario y los clusters?**
*(Prepara esto bien: ver sección 8.)* Los 403 términos de Biología corresponden al vocabulario curricular de secundaria; los clusters los agrupan por tema. Debes poder decir quién los elaboró y quién los validó (idealmente un docente de Biología). Reconoce que Química, Historia y Geografía tienen solo 13 a 22 términos y son demostrativos.

### B. Lógica difusa y diseño

**B1. ¿Por qué lógica difusa y no un simple "si la similitud es alta y la confianza es baja, corrige"?**
*(Es la pregunta más peligrosa. No la esquives.)*
Hice esa comparación. Busqué la mejor regla booleana "D ≥ x y C ≤ y" en una malla de umbrales y la comparé con mi sistema en dos escenarios. Con la confianza **neutra** 0.65, que es lo que Android entrega en la práctica, el sistema difuso tiene menos error en ambos corpus (2.91 % frente a 4.85 % y 1.72 % frente a 3.45 %). Con las confianzas **sintéticas** que yo asigné hay un empate técnico: gana el difuso en un corpus y la booleana en el otro. Eso último era esperable: mis confianzas sintéticas marcan "baja" justo en las palabras erradas, lo que le regala la respuesta a una regla basada en C. Debo ser honesto con dos reservas: los umbrales de la booleana se ajustaron sobre los mismos datos (es un rival optimista) y yo también mejoré el sistema difuso mirando esos mismos datos. Lo que sí tiene la lógica difusa y la regla booleana no es contexto: combina tres señales imprecisas, decide entre "médula" y "célula" por el tema, y cada decisión se explica. La contrastación definitiva requiere voz real; por eso la hipótesis específica 2 la dejo **pendiente**.

**B2. ¿Por qué Mamdani y no Sugeno?**
Mamdani usa consecuentes lingüísticos ("justificación Alta"), lo que permite explicar la regla a un docente. Sugeno sería más eficiente, pero el costo del motor es del orden de microsegundos, no es el cuello de botella.

**B3. ¿Cómo se eligieron las funciones de pertenencia y el umbral 0.65?**
Por diseño experto, sin optimización automática. Para el umbral hice un barrido (0.50 a 0.80) con un corpus de control de lenguaje cotidiano: con ≤ 0.55 aparece un falso positivo, con ≥ 0.60 ninguno. Mantuve 0.65 como margen de seguridad. Un cambio equivocado en silencio es peor que una omisión, por eso privilegio precisión sobre cobertura. No hice un análisis de sensibilidad de las funciones de pertenencia, y es trabajo futuro.

**B4. ¿Por qué 27 reglas?**
3 términos × 3 variables = 3³ combinaciones; la base es completa, sin huecos ni contradicciones (se verifica automáticamente).

**B5. ¿Por qué no usó la librería jFuzzyLogic si lo dice la tesis?**
El anexo formal está en FCL y la app ejecuta una implementación equivalente en Kotlin, sin dependencias. Escribí un verificador que interpreta el FCL y lo compara con el motor de la app en 4 913 puntos: diferencia máxima 3·10⁻¹⁶. Si prefiere que cite la librería, el FCL se puede cargar directamente con ella.

**B6. ¿Qué pasa si C, D o S valen exactamente 0 o 1?**
Los hombros de las funciones de pertenencia tratan los bordes correctamente. Hubo un error de bordes en el espejo de Python (y* caía a 0 con D=1.0); lo encontré con una prueba de paridad Kotlin↔Python y lo corregí.

### C. Factor semántico y memoria

**C1. ¿Qué es S exactamente?**
Qué tan coherente es un término candidato con el tema. Combina dos evidencias: cuántos otros términos de la misma oración pertenecen a su cluster, y cuánto se ha hablado de ese tema en los últimos segundos (memoria de la clase, con decaimiento exponencial).

**C2. ¿Por qué memoria de la clase?**
Porque el docente habla en frases cortas con un solo término técnico; sin memoria, S sería neutro. La memoria hereda el contexto de lo dicho antes.

**C3. ¿La memoria mejora los resultados?**
En los corpus sintéticos suma, aunque con matices. En el escenario realista (confianza neutra) el error baja en pasos: 4.85 % solo con la oración, 3.88 % con memoria, 3.88 % con selección de candidato por los tres factores y 2.91 % con ambas. Sola, la memoria no siempre ayuda: su valor principal es evitar correcciones fuera de tema. Son 8 y 13 frases, así que no afirmo significancia estadística.

**C4. ¿Por qué 45 segundos de vida media?**
Elegí 45 s dentro de una meseta: el resultado es idéntico entre 20 y 90 s y solo empeora con 120 s (se retrasa la adaptación al cambio de tema). Aun así es un valor provisional que debo calibrar con grabaciones reales.

**C5. ¿Qué pasa cuando el docente cambia de tema?**
Las primeras correcciones del tema nuevo se retrasan hasta acumular evidencia. Es un costo medido; con una vida media corta se reduce.

**C6. ¿Cómo evitan que un tema activo "robe" palabras comunes?**
Encontré ese problema al probar: dentro de una clase de mitosis, "produce" se convertía en "profase". Agregué tres salvaguardas: similitud mínima 0.70 para sustituir, ventana de 0.12 para competir y guardia de flexión (no cambiar plural por singular). Cada una tiene su prueba automática.

**C7. ¿Qué son las áreas temáticas?**
Los clusters son finos ("citología general", "organelos celulares") y una oración real mezcla subtemas del mismo bloque ("la célula y la mitocondria"). Cada cluster declara un área (p. ej. "biología celular") y un término del mismo bloque aporta crédito parcial a S. *Esa agrupación es una propuesta mía y debe ser validada por un docente de Biología.*

### D. Hardware, audio y ASR

**D1. ¿El micrófono SX26 realmente llega a la app?**
*(Contesta con precisión.)* La app detecta un receptor USB de audio y cambia de fuente; esa lógica está probada. Pero `SpeechRecognizer` no permite elegir el micrófono: depende de que Android enrute el audio al dispositivo USB. Eso se verifica en hardware con el plan de pruebas (caso HW-02). Si fallara, el plan B es capturar con `AudioRecord` indicando el dispositivo preferido; no lo implementé porque no puedo validarlo sin el aparato.

**D2. ¿Por qué un micrófono inalámbrico de solapa y no el del teléfono?**
Por la relación señal/ruido: el micrófono está a pocos centímetros de la boca del docente aunque el teléfono esté en el pupitre del estudiante. *(Cuidado: no afirmes que la mejora venga de la frecuencia de muestreo. Los reconocedores de voz habitualmente procesan audio a 16 kHz; verifícalo antes de hablar de 44.1/48 kHz.)*

**D3. ¿Qué tan confiable es la confianza C que da Android?**
Android entrega **una puntuación por hipótesis, no por palabra**, y algunos equipos no la entregan; en ese caso el sistema usa 0.65. Por eso evalué también el caso de confianza neutra. Medir su utilidad real es uno de los casos del plan de pruebas (ASR-02).

**D4. ¿Funciona sin internet?**
El reconocedor se configura para preferir el modo sin conexión, y el resto del procesamiento es local. Falta confirmarlo en el aparato con modo avión (caso ASR-01), que además requiere el paquete de voz en español instalado.

**D5. ¿Y en tablets?**
Muchas tablets económicas no traen los servicios de voz; el primer caso de prueba de tablet verifica eso. Si no hay servicios, la limitación es del dispositivo.

**D6. ¿Se pierden palabras entre reinicios del reconocedor?**
Android corta cada escucha y el motor reinicia en 150-350 ms. Puede perder palabras en esos instantes; la medición está en el caso ASR-03.

### E. Evaluación y validez

**E1. ¿Con qué datos evaluó?**
Con tres corpus sintéticos: 8 frases de Biología con errores de reconocimiento simulados, 13 frases de clase continua con cambio de tema y 12 frases cotidianas de control. Son una prueba de factibilidad, no una validación de campo.

**E2. ¿Hay significancia estadística?**
No. Con 8 a 13 frases una palabra mueve varios puntos porcentuales. Para una conclusión estadística haría falta un corpus de voz real (≥ 100 oraciones) y un intervalo de confianza por bootstrap o prueba pareada.

**E3. La meta era 90 % de precisión semántica; ¿la cumple?**
En el corpus sintético, la precisión sobre palabras clave (1−KER) es 93.55 % y la textual 98 %. Pero no lo presento como cumplimiento: los corpus son sintéticos, pequeños, y varias mejoras se decidieron observando esos mismos datos. En una versión anterior del sistema el mismo indicador era 77 % y 84 %. Lo que corresponde afirmar es que la capa es factible y que la meta debe contrastarse con voz real.

**E4. ¿Por qué WER y KER?**
WER mide errores en cualquier palabra; KER mide solo los términos curriculares, que son los que importan al aprendizaje. Un sistema podría bajar el WER y fallar en lo clave.

**E5. ¿Cómo sabe que no sobreajustó?**
No lo puedo descartar y lo declaro. Las áreas temáticas, la memoria de términos resueltos y las salvaguardas se agregaron tras ver fallos en estos mismos corpus; por tanto los resultados son "dentro de la muestra". Para los parámetros hay un respaldo parcial: el umbral y* da lo mismo entre 0.60 y 0.75, y la vida media entre 20 y 90 s, así que no están en un filo. El siguiente paso es separar datos de calibración y de prueba con grabaciones reales.

**E6. Las confianzas del corpus las puso usted.**
Sí, y señalé que eso favorece a la regla booleana. Por eso repetí la evaluación con confianza neutra 0.65, que es lo que Android entrega en la práctica.

### F. Hipótesis y resultados

**F1. ¿Se aceptan las hipótesis?**
Hipótesis general: **sustentada de forma preliminar** en un banco de pruebas sintético (el WER baja de 18 % a 2 % sin tocar lenguaje cotidiano), **pendiente de validación en campo**. Específica 1 (confianza acústica) y específica 3 (ergonomía): **pendientes**: la confianza de Android es por frase y la usabilidad no se ha medido con usuarios. Específica 2 (razonamiento difuso frente a lo booleano): **resultados mixtos**: con confianza neutra el difuso supera a la mejor regla booleana, con confianzas sintéticas hay empate; requiere voz real para concluir.

**F2. ¿Se probó con estudiantes sordos?**
Aún no. El cuestionario SUS está implementado en la app, pero no se ha aplicado. La interfaz cumple contraste AAA (19.56:1), que es una condición necesaria, no una prueba de usabilidad.

**F3. ¿Por qué declara hipótesis como pendientes en vez de aceptarlas?**
Porque la evidencia disponible no las contrasta. Prefiero una conclusión honesta y verificable.

### G. Implementación

**G1. ¿Corre en tiempo real?**
La capa difusa cuesta ≈ 7 ms por oración de 14 palabras contra 403 términos en una JVM de escritorio, y la memoria de la clase ≈ 1 µs. Procesa los resultados en orden con un único hilo de trabajo. En un teléfono será más lento; la medición en dispositivo es el caso PERF-02.

**G2. ¿Qué pasa si el docente deja de hablar o el reconocedor falla?**
El motor reinicia la escucha automáticamente; si el idioma o el paquete offline no están disponibles, se detiene y muestra un mensaje en lugar de reintentar sin fin. La sesión de 90 min se prueba en ROB-01.

**G3. ¿Qué pasa si desconectan el receptor?**
Cambia al micrófono interno y avisa. La lógica de decisión está probada con pruebas unitarias; el comportamiento con el aparato real está en el plan de pruebas.

**G4. ¿Por qué Kotlin/Android nativo?**
Acceso directo a `SpeechRecognizer`, servicios en primer plano y gestión de audio USB; ejecución local y consumo bajo.

**G5. ¿Cómo se garantiza que Python (benchmark) y Kotlin (app) calculan lo mismo?**
Hay una prueba de paridad: el benchmark genera una tabla de referencia de 4 913 puntos y un test de Kotlin exige que la app dé los mismos valores.

### H. Preguntas incómodas

**H1. ¿Cuál es la contribución original?**
El diseño y la validación preliminar de una capa de corrección post-ASR tri-variable, explicable y local, orientada a vocabulario curricular peruano, con memoria temática de la clase y salvaguardas contra la corrección indebida. No afirmo superioridad sobre otras técnicas: lo que aporto es un marco evaluable y los primeros resultados de factibilidad.

**H2. ¿Qué haría distinto?**
Empezaría por grabar un corpus real con el micrófono final y dividirlo en calibración y prueba antes de ajustar parámetros.

**H3. ¿Qué pasa si el sistema corrige mal una palabra y el estudiante aprende el término equivocado?**
Es el riesgo principal de una corrección silenciosa. Lo mitigo con: umbral conservador, piso de parecido, no corregir donde el reconocedor está seguro, y un historial auditable de cada cambio. Una mejora futura es marcar visualmente las palabras corregidas para que el docente las revise.

**H4. ¿Funcionaría con otros acentos o regiones?**
El reconocedor está configurado para español de Perú y la normalización fonética contempla seseo y yeísmo, pero no lo probé con hablantes distintos.

**H5. ¿Qué pasa con términos fuera del diccionario?**
No se corrigen. El sistema solo mejora los términos que conoce.

**H6. Esto no demuestra que mejore el aprendizaje.**
Correcto: mide exactitud del texto, no aprendizaje. Medir impacto pedagógico exige un estudio con estudiantes, fuera del alcance de este prototipo.

**H7. ¿Es privado? ¿Qué datos guarda?**
El procesamiento es local. Guarda en el teléfono el registro de palabras y métricas de la sesión; no envía datos. Confirma el comportamiento del reconocedor con el modo avión antes de afirmarlo categóricamente.

---

## 7. Frases de rescate

- *"Es una buena observación; esa es una limitación que declaro en el capítulo de conclusiones."*
- *"Con los datos que tengo no puedo afirmarlo; lo que sí puedo mostrar es…"*
- *"Eso es trabajo futuro; el primer paso concreto sería…"*
- Si no sabes: *"No lo medí. Lo anotaría como una prueba pendiente y se lo puedo indicar con el procedimiento."*

Nunca inventes una cifra. Si no la recuerdas, di "del orden de…" y vuelve a la tabla de la sección 5.

---

## 8. Qué hacer antes de la defensa (en orden de impacto)

1. **Grabar un corpus real** (≥ 100 oraciones de Biología) con el SX26 y el ASR del teléfono, con la transcripción correcta escrita a mano. Es lo que más fortalece la tesis y la única forma de contrastar las hipótesis. Dividir en calibración y prueba antes de ajustar τ y el umbral.
2. **Ejecutar el plan de pruebas en hardware** (`plan_de_pruebas_sx26_telefono_tablet.md`), empezando por HW-02 (enrutamiento) y ASR-01/ASR-02 (modo sin conexión y confianza real).
3. **Documentar el origen del diccionario y los clusters** y conseguir que un docente de Biología los revise.
4. **Aplicar el SUS** a al menos algunos usuarios (docentes o estudiantes).
5. **Ensayar la demo** tres veces y tener el plan B listo.
6. **Releer** `guia_del_codigo.md` §9 (limitaciones) y `factor_semantico_topic_clusters.md` para no contradecirte.

Comandos de apoyo (desde `evaluation/`): `python3 benchmark_runner.py`, `--ablacion`, `--umbral`, `--booleana`, `python3 verificar_fcl.py`.
