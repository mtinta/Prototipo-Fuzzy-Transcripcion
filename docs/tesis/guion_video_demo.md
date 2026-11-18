# Guion del video de demostración del prototipo (máx. 8 minutos)

**Duración estimada:** 7:30. Lo que va entre comillas es lo que dices. Lo que va en *cursiva* es lo que haces.

| # | Escena | Tiempo | Qué se ve |
|---|---|---|---|
| 1 | Saludo | 0:00 – 0:20 | Cámara |
| 2 | Arquitectura | 0:20 – 1:40 | Figura de arquitectura + cámara en una esquina |
| 3 | Condiciones de funcionamiento | 1:40 – 2:30 | Teléfono |
| 4 | Pantalla principal | 2:30 – 3:30 | Teléfono (micrófono encendido) |
| 5 | Condición T0, sin corrección | 3:30 – 4:45 | Teléfono |
| 6 | Condición T2, con corrección | 4:45 – 5:40 | Teléfono |
| 7 | Latencia, correcciones y S | 5:40 – 6:25 | Teléfono |
| 8 | Historial: C, D, S e y* | 6:25 – 7:30 | Teléfono |

---

## Antes de grabar

- **App actualizada** con los cambios de hoy (`./gradlew installDebug`): el historial con un solo botón para volver, el contador de correcciones y la S.
- **Teléfono en vertical.** En horizontal la app oculta los botones y la barra de valores. Brillo alto y modo No molestar.
- **Al empezar:** modo avión apagado (lo enciendes en cámara), receptor USB-C desconectado, transmisor del SX26 encendido y prendido en la camisa.
- **App abierta** en la pantalla principal, en condición T2 (la que trae por defecto) y con la pantalla limpia (botón de papelera).
- **Grabar la pantalla del teléfono** con el grabador de pantalla del propio teléfono. `scrcpy` necesita el puerto USB (lo ocupa el receptor) o wifi (se apaga con el modo avión). Graba la cámara aparte con OBS y junta los dos videos al editar. La cámara debe verse durante todo el video.
- **En la PC:** la figura de arquitectura del Capítulo III, abierta a pantalla completa.
- **Ensayo:** di las tres frases una vez en T2 antes de grabar. Las primeras frases después de abrir la app tardan más.

### Palabras que no debes decir con el micrófono encendido

En T2, la app las corrige mal (se comprobó con el motor real):

| Si dices | La app escribe |
|---|---|
| aplicación | replicación → di **"la app"** |
| celular | célula → di **"el teléfono"** |
| nuclear | núcleo |
| espera | espora |
| cédula de identidad | célula de identidad |

---

## Las frases de la demostración

En cada frase dices **a propósito** una palabra equivocada que existe en español y se parece al término correcto. Así simulas un error del reconocedor y la demostración se puede repetir.

| # | Dices exactamente | T0 muestra | T2 muestra |
|---|---|---|---|
| F1 | "La membrana de la **cédula** controla el paso de sustancias." | cédula | **célula** |
| F2 | "Cada gen tiene un **alero** dominante y un **alero** recesivo." | alero | **alelo** (2 veces) |
| F3 | "La savia elaborada baja por el **flema** desde la hoja." | flema | **floema** |

Se probaron con el motor y los diccionarios reales de la app, con C = 0.65 como en tu TECNO KJ5. Valores esperados al terminar T2:

- **Barra principal:** Corr: 4 · S: alrededor de 84 %. La latencia depende del teléfono: en las pruebas, la capa difusa tardó 23 ms en promedio.
- **Historial** (lo más reciente sale arriba):

| Corrección | C | D | S | y* | Tema |
|---|---|---|---|---|---|
| flema → floema | 65 % | 83 % | 68 % | 0.79 | Botánica y Tejidos Biológicos |
| alero → alelo (2 veces) | 65 % | 80 % | 84 % | 0.82 | Genética y Herencia |
| cédula → célula | 65 % | 83 % | 100 % | 0.83 | Citología General y Transporte Celular |

**Frases de reserva.** Son homófonos: el reconocedor puede escribirlas bien por sí solo y entonces no habrá nada que corregir. Si las usas, cambian Corr y S.
- "El espermatozoide fecunda al **óbulo** y se forma el cigoto." (óbulo → óvulo)
- "La **encima** digestiva rompe la proteína en el estómago." (encima → enzima)

---

## Guion

### 1. Saludo (0:00 – 0:20) · cámara

> "Buenos días. Soy Miguel Ángel Tinta Aguilar y presento el prototipo de mi tesis: una app Android que subtitula en tiempo real la voz del docente para estudiantes con discapacidad auditiva, y que corrige con lógica difusa los términos técnicos que el reconocedor de voz entiende mal."

### 2. Arquitectura (0:20 – 1:40) · figura de arquitectura de fondo

*Señala cada capa de la figura cuando la nombres.*

> "El sistema tiene cinco módulos, agrupados en las cuatro capas de esta figura.
>
> La primera capa es la captura. El módulo uno toma el audio: el docente usa un micrófono de solapa inalámbrico, cuyo receptor tipo C se conecta al teléfono del estudiante. Si el receptor se desconecta, la app pasa sola al micrófono del teléfono. El módulo dos convierte la voz en texto con el reconocedor de Android, sin conexión, y entrega también la confianza del reconocimiento, C.
>
> La segunda capa es el razonamiento difuso, el módulo tres. Primero se eligen las palabras que podrían ser términos técnicos. Para cada una se calcula la similitud D con el término más parecido del diccionario de Ciencia y Tecnología, y la coherencia temática S, que mide si ese término encaja con el tema de la frase y de la clase. Con C, D y S, un motor Mamdani de 27 reglas calcula el grado de justificación, y-estrella.
>
> La tercera capa es la decisión, el módulo cuatro. La palabra se sustituye solo si y-estrella llega a 0.65 y la similitud a 0.70. Este módulo también guarda el historial de correcciones y un registro de cada frase para la evaluación.
>
> La cuarta capa es la presentación, el módulo cinco: muestra el texto final en formato teleprompter, en amarillo sobre negro. Todo el procesamiento ocurre dentro del teléfono."

### 3. Condiciones de funcionamiento (1:40 – 2:30) · teléfono

*Baja la barra de ajustes rápidos y activa el modo avión.*

> "El prototipo funciona en dos condiciones. La primera es sin internet: activo el modo avión, así que no hay wifi ni datos móviles. El reconocimiento de voz se hace en el propio teléfono, y eso importa porque muchas aulas no tienen una conexión estable."

*Conecta el receptor USB-C frente a la cámara, para que se vea. La franja de la fuente de audio se pone verde y muestra "SX26 inalámbrico · 2.4 GHz RF".*

> "La segunda es con el micrófono inalámbrico. Conecto el receptor tipo C al teléfono y la app lo detecta sola: la franja se pone verde e indica el micrófono de 2.4 gigahercios. Desde ahora, el teléfono escucha la voz del docente a través del micrófono de solapa, aunque el docente esté a varios metros."

### 4. Pantalla principal (2:30 – 3:30) · teléfono, micrófono encendido

*Pulsa INICIAR MICRÓFONO. Desde aquí tu voz aparece en la zona negra, así que no hace falta decir que funciona: se ve. Señala cada parte cuando la nombres.*

> "Inicio el micrófono. Todo lo que digo aparece aquí, en letras amarillas sobre fondo negro.
>
> Arriba está el área de Ciencia y Tecnología y los botones para agrandar o reducir la letra.
>
> La franja verde indica que el audio llega desde el micrófono inalámbrico.
>
> Este botón inicia o detiene la escucha. Al lado se ven la latencia, el número de correcciones y el valor S, que explicaré más adelante.
>
> No hay colores ni subrayados, así el estudiante solo se concentra en leer.
>
> Arriba a la derecha están los íconos para exportar los apuntes en texto o en PDF, el historial de correcciones y el cuestionario de usabilidad."

*Pulsa A+ dos veces y A− una vez para mostrar el cambio de tamaño. Pulsa DETENER MICRÓFONO.*

### 5. Condición T0, sin corrección (3:30 – 4:45) · teléfono

*Mantén pulsado el título "Prueba" y se abre el Panel del investigador. Apaga "Mostrar corrección difusa"; el texto cambia a "Condición T0: se muestra el texto crudo del ASR". Pulsa APLICAR E INICIAR SESIÓN y la pantalla se limpia.*

> "Para comparar uso el panel del investigador, que se abre con una pulsación larga en el título. Apago la corrección difusa: esta es la condición T0, que muestra el texto tal como lo entrega el reconocedor de voz.
>
> Voy a leer tres frases de una clase de biología. En cada una digo a propósito una palabra equivocada que se parece al término correcto, como cuando el reconocedor se equivoca."

*Pulsa INICIAR MICRÓFONO. Lee F1, F2 y F3 con unos 2 segundos de pausa entre frases. Pulsa DETENER MICRÓFONO.*

> "Las tres frases tienen un error: cédula en lugar de célula, alero en lugar de alelo y flema en lugar de floema. Lo difícil es que las tres palabras existen en español, así que un corrector ortográfico no las marcaría. Solo el tema de la frase delata el error. En T0 no se aplica ninguna corrección, por eso los contadores siguen en cero, y el estudiante leería este texto equivocado."

*Si el reconocedor escribe bien una de las palabras por sí solo, dilo ("aquí el reconocedor acertó solo") y sigue. Corr bajará en una.*

### 6. Condición T2, con corrección (4:45 – 5:40) · teléfono

*Abre otra vez el panel con una pulsación larga en "Prueba". Enciende "Mostrar corrección difusa" ("Condición T2…") y pulsa APLICAR E INICIAR SESIÓN.*

> "Ahora enciendo la corrección difusa: esta es la condición T2. Repito exactamente las mismas frases."

*Pulsa INICIAR MICRÓFONO. Lee F1, F2 y F3 igual que antes. Pulsa DETENER MICRÓFONO.*

> "Con la capa difusa, el texto sale corregido: célula, alelo y floema. La corrección es silenciosa: el estudiante no ve marcas ni alternativas, solo el texto final. Y el resto de cada frase queda intacto."

### 7. Latencia, correcciones y S (5:40 – 6:25) · teléfono

*Señala la barra que está junto al botón: "Lat: __ms · Corr: 4 · S: __%".*

> "Esta barra resume tres valores de la sesión.
>
> Lat es la latencia de la capa difusa: el tiempo promedio que tarda en revisar cada frase. Aquí son [lee el valor] milisegundos. La meta de la tesis es menos de 100, así que el estudiante no lo nota.
>
> Corr es el número de palabras corregidas: cuatro, porque alero aparecía dos veces.
>
> S es la coherencia temática media de los términos técnicos de la sesión. Va de cero a cien por ciento e indica qué tanto encajan esos términos con el tema de la clase. Cien por ciento significa que los otros términos de la frase pertenecen al mismo tema, cincuenta por ciento es un valor neutro porque falta contexto, y un valor bajo indica que el término no encaja. Aquí marca [lee el valor] por ciento: las correcciones se hicieron con un contexto temático fuerte."

### 8. Historial de correcciones: C, D, S e y* (6:25 – 7:30) · teléfono

*Toca el ícono del historial, el tercero de arriba a la derecha (el de "abc" con un visto). Señala la tarjeta "Sustituciones: 4 · Umbral y* ≥ 0.65". Baja hasta la tarjeta **cédula → célula**, que es la última porque lo más reciente sale arriba.*

> "El historial muestra cada corrección: tachada, la palabra que entendió el reconocedor y, al lado, el término corregido. Debajo están el tema detectado y cuatro valores. Tomo la primera frase: cédula pasó a célula.
>
> C es la confianza acústica, es decir, qué tan seguro está el reconocedor de lo que oyó. Este teléfono no informa ese valor, así que la app usa uno neutro de 65 por ciento. Cuando la confianza es alta, el sistema corrige con más cautela.
>
> D es la similitud: qué tanto se parece la palabra oída al término del diccionario, por cómo se escribe y por cómo suena. Cédula y célula solo difieren en una letra, por eso D es 83 por ciento.
>
> S es la coherencia temática de esta corrección. En la misma frase aparece membrana, que pertenece al mismo tema, citología. Por eso S es cien por ciento.
>
> Y-estrella es la salida del motor difuso: el grado de justificación de la corrección, entre cero y uno, que resulta de aplicar las 27 reglas a C, D y S. Aquí vale 0.83. Como supera el umbral de 0.65 y la similitud supera 0.70, la palabra se sustituye. Si y-estrella fuera menor, la app respetaría lo que dijo el docente."

*Pulsa VOLVER A LA TRANSCRIPCIÓN.*

> "Con esto termina la demostración. Muchas gracias."

---

## Referencia rápida: cómo se calcula cada valor

Por si te lo preguntan; no hace falta decirlo en el video.

- **C (confianza acústica):** viene de `CONFIDENCE_SCORES` del reconocedor de Android, con un valor por frase. Si el motor no la entrega (tu TECNO entrega 0), la app usa 0.65.
- **D (similitud):** D = 0.6 · D_ortográfica + 0.4 · D_fonética. La fonética iguala las letras que suenan igual en el español del Perú (c/z/s, b/v, ll/y, h muda). Solo es candidata si D ≥ 0.50, y solo se sustituye si D ≥ 0.70.
- **S (coherencia temática):** el diccionario está organizado en 12 grupos temáticos dentro de 4 áreas. Por cada candidato se cuentan los otros términos de la frase con D ≥ 0.80: suman 1 punto si son del mismo grupo y 0.5 si son de otro grupo de la misma área, y el total se divide entre el número de esos términos. Ese valor se mezcla con la memoria de la clase, que pesa hasta un 40 %. Sin contexto, S = 0.50 (neutro).
- **S de la barra principal:** el promedio de S de los términos técnicos de la sesión (palabras corregidas o con D ≥ 0.80). Las palabras comunes no cuentan.
- **y\* (grado de justificación):** sale de un motor Mamdani con 27 reglas (3 niveles de C × 3 de D × 3 de S), operador MIN y defuzzificación por centroide. Se sustituye si y\* ≥ 0.65 y D ≥ 0.70.
- **Lat:** el tiempo de la capa difusa por frase final (`t_fin corrección − t_inicio corrección`). En PERF-01 dio 23 ms de media y 37 ms en el percentil 95 (meta: < 100 ms).
