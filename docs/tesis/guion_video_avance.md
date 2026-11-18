# Guion del video de avance del prototipo (máx. 8 minutos)

**Requisitos de la entrega:** video de 8 minutos como máximo, con la cámara encendida, que muestre las funcionalidades de la solución propuesta del Capítulo III. El enlace va en un segundo PDF y debe tener permiso de descarga.

**Duración objetivo:** unos 7:30, para tener margen.

---

## 1. Preparación (antes de grabar)

### Montaje de la grabación
- **Pantalla del teléfono en la PC:** `scrcpy` (`sudo pacman -S scrcpy`), con el teléfono conectado por USB y la depuración USB activada.
  - Si el puerto USB-C del teléfono está ocupado por el receptor del micrófono, usa `scrcpy` por Wi-Fi: `adb tcpip 5555` y luego `scrcpy --tcpip=<IP del teléfono>`.
  - **Ojo:** en modo avión se corta el Wi-Fi. Para la escena de modo avión, graba el teléfono con la cámara o activa el modo avión y vuelve a encender solo el Wi-Fi (el reconocimiento sigue sin datos móviles). Explícalo en voz alta si lo haces.
- **Grabación:** OBS Studio (`sudo pacman -S obs-studio`).
  - Escena 1: cámara web a pantalla completa (presentación y cierre).
  - Escena 2: ventana de `scrcpy` en grande y la cámara web en una esquina (todo el resto del video).
  - **La cámara debe verse encendida durante todo el video.**
- **Audio:** usa el micrófono de solapa SX26 conectado al teléfono para la demo. Para la narración, el micrófono de la PC o auriculares sirven.

### Estado del teléfono
- App instalada, permiso de micrófono concedido, paquete de español sin conexión descargado.
- Historial de correcciones vacío (sesión nueva).
- Brillo alto, orientación horizontal, notificaciones silenciadas (modo No molestar).
- Receptor USB-C a la mano, **sin conectar** al empezar.

### Ventanas abiertas en la PC
- La tesis en PDF, abierta en la Figura de arquitectura y en la Figura del flujo de decisión (Capítulo III).
- Una terminal en `Prototipo-Fuzzy-Transcripcion/` con el resultado de las pruebas unitarias **ya ejecutado** (`./gradlew test`). No lo ejecutes en vivo: tarda.
- Una segunda terminal en `Prototipo-Fuzzy-Transcripcion/evaluation/`, lista para el plan B (`python3 interactive_tester.py`).

---

## 2. Guion por escenas

Los tiempos son acumulados. El texto entre comillas es lo que dices; lo que va en *cursiva* es lo que haces.

### Escena 1 — Presentación (0:00 – 0:35) · cámara a pantalla completa

> "Buenos días. Soy Miguel Ángel Tinta Aguilar y presento el avance del prototipo de mi tesis: un sistema de subtitulado en tiempo real para teléfonos Android que apoya a estudiantes con discapacidad auditiva en clases de Ciencia y Tecnología de secundaria. Su característica principal es una capa de corrección basada en lógica difusa Mamdani que revisa la salida del reconocedor de voz y corrige los términos técnicos mal reconocidos. En este video muestro, en el teléfono, cada una de las funcionalidades de la propuesta del Capítulo III."

### Escena 2 — Cómo funciona (0:35 – 1:30) · Figura de arquitectura + cámara en la esquina

*Muestra la figura de arquitectura y señala cada capa mientras hablas.*

> "El sistema tiene cuatro capas. Primero, la captura: el docente usa un micrófono de solapa inalámbrico cuyo receptor USB-C se conecta al teléfono del estudiante. Segundo, el reconocedor de voz de Android convierte la voz en texto; cuando el equipo tiene el modelo de español descargado, funciona sin internet. Tercero, la capa difusa: para cada palabra candidata calcula tres valores, la confianza del reconocedor C, la similitud con un término del diccionario D y la coherencia con el tema de la clase S, y con 27 reglas decide si conviene sustituirla. Cuarto, la interfaz muestra el texto en formato teleprompter, en amarillo sobre negro. Todo el procesamiento propio se hace en el teléfono."

### Escena 3 — Captura de audio, Módulo 1 (1:30 – 2:20) · teléfono

*Abre la app y conecta el receptor USB-C en cámara, para que se vea.*

> "Conecto el receptor del micrófono inalámbrico. La aplicación lo detecta y lo selecciona como fuente de audio; aquí se ve el aviso de micrófono de solapa conectado."

*Inicia la escucha y di una frase corta.*

> "Ahora simulo una falla: desconecto el receptor en plena transcripción."

*Desconecta el receptor. Se muestra "Receptor USB desconectado… micrófono interno".*

> "La app no se cierra: cambia sola al micrófono interno, avisa al usuario y la transcripción continúa. Vuelvo a conectar el receptor para el resto de la demostración."

### Escena 4 — Reconocimiento sin conexión, Módulo 2 (2:20 – 3:00) · teléfono

*Activa el modo avión desde la barra de notificaciones.*

> "Activo el modo avión: sin Wi-Fi ni datos. Leo una frase de la clase."

*Lee:* "La célula procariota, como la bacteria, carece de un núcleo definido y su material genético está disperso en el citoplasma."

> "El texto aparece igual, porque el reconocimiento se ejecuta en el propio teléfono. Esto es importante porque muchas aulas no tienen una conexión estable."

### Escena 5 — Corrección difusa, Módulos 3 y 4 (3:00 – 4:45) · teléfono

> "Ahora la parte central: la corrección de términos técnicos. Leo frases con términos que el reconocedor suele confundir."

*Lee, con pausas de 1 a 2 segundos entre frases:*
1. "La célula vegetal realiza la fotosíntesis dentro del cloroplasto gracias al pigmento de la clorofila."
2. "El retículo endoplasmático rugoso contiene adherido al ribosoma, encargado de fabricar cada proteína."
3. "Para la digestión celular, la célula utiliza el lisosoma y almacena agua y nutrientes en una gran vacuola."

*Abre el **Historial de Correcciones** (icono de corrector en la barra superior).*

> "En el historial de correcciones se ve, para cada palabra cambiada, lo que entendió el reconocedor, la palabra corregida y los tres valores de entrada C, D y S, junto con la salida y-estrella. La app solo sustituye cuando y-estrella es al menos 0.65 y la similitud es al menos 0.70; en cualquier otro caso respeta lo que dijo el docente."

*Señala una corrección concreta del historial y lee sus valores.*

> "Por ejemplo, aquí el reconocedor escribió [palabra original] y el sistema lo corrigió a [término], con una similitud de [D] y una coherencia de [S], porque en la misma oración aparecen otros términos del mismo tema."

**Control de falsas correcciones** — *lee:* "Recuerden que factores del ambiente como la temperatura o el calor no alteran la secuencia del gen."

> "Esta frase contiene palabras comunes como *calor*, que se parece a *cloro*. El sistema no la cambia: el objetivo es corregir términos técnicos sin estropear las palabras que el reconocedor ya acertó."

**Plan B (si el reconocedor no se equivoca en ninguna palabra).** No fuerces la pronunciación. Di:

> "En esta toma el reconocedor acertó todos los términos, así que no hubo nada que corregir, que es lo esperado. Para mostrar la inferencia uso la implementación de referencia del motor, que es equivalente a la de la app."

*En la terminal ejecuta `python3 interactive_tester.py` y escribe:* `la sedula tiene nucleo y citoplasma`

> "Para *sedula* hay dos candidatos: *médula*, que se parece más en la forma, y *célula*. El sistema elige *célula* porque *núcleo* y *citoplasma* pertenecen al mismo tema. Una regla basada solo en la similitud elegiría *médula*."

### Escena 6 — Interfaz accesible, Módulo 5 (4:45 – 5:40) · teléfono

> "La interfaz está pensada para el estudiante. El texto es amarillo sobre negro, con un contraste de 19.6 a 1, que cumple el nivel AAA de accesibilidad. No hay subrayados ni colores que marquen la incertidumbre: el estudiante solo ve el texto final."

*Pulsa A+ y A− varias veces.*

> "El estudiante puede agrandar o reducir la letra. La interfaz se adapta al tamaño de la pantalla, por lo que también funciona en tabletas. Arriba se ve el área curricular fija, Ciencia y Tecnología."

*Pulsa **Exportar PDF** y abre el archivo generado.*

> "Al terminar la clase, el estudiante puede exportar el texto como apuntes en PDF o TXT, ya con los términos corregidos."

### Escena 7 — Herramientas para la evaluación (5:40 – 6:30) · teléfono

*Mantén pulsado el título "Prueba" para abrir el panel del investigador.*

> "Para la evaluación hay un panel oculto que se abre con una pulsación larga. Aquí registro el código del participante y la etiqueta de la sesión, y elijo la condición: con el interruptor *Mostrar corrección difusa* el estudiante ve el texto corregido, y sin él, el texto original del reconocedor. En las dos condiciones la app guarda ambos textos, un archivo CSV con una fila por enunciado: texto crudo, texto corregido, confianza, perfil de reconocimiento, tiempos y los valores de la capa difusa."

*Abre el **Cuestionario SUS** y muestra 2 o 3 preguntas.*

> "Al final de la sesión, el estudiante responde en la misma app el cuestionario de usabilidad SUS. Las diez preguntas son obligatorias y el puntaje no se le muestra."

### Escena 8 — Verificación (6:30 – 7:05) · terminal + cámara en la esquina

*Muestra el resultado ya ejecutado de `./gradlew test`.*

> "El código cuenta con 71 pruebas unitarias y todas pasan. Cubren el motor Mamdani, la similitud, la memoria de la clase, la decisión de sustitución, la fuente de audio y el registro por enunciado. Además, comprobé numéricamente que el motor de la app da los mismos resultados que la especificación en lenguaje FCL del anexo, en 4 913 puntos."

### Escena 9 — Estado y siguientes pasos (7:05 – 7:35) · cámara a pantalla completa

> "En resumen, el prototipo ya implementa las funcionalidades de la propuesta: captura con micrófono inalámbrico y respaldo automático, reconocimiento sin conexión, corrección difusa con 27 reglas, interfaz accesible, exportación de apuntes y registro para la evaluación. La verificación técnica en el teléfono se realizó el 31 de octubre. Los siguientes pasos son el piloto y la evaluación en aulas con docentes y estudiantes. Muchas gracias."

---

## 3. Funcionalidades del Capítulo III y dónde aparecen

| Funcionalidad (Cap. III) | Escena |
|---|---|
| Captura desde receptor USB-C con respaldo en micrófono interno (Módulo 1) | 3 |
| Reconocimiento en español sin conexión (Módulo 2) | 4 |
| Filtro de candidatas, similitud D, coherencia S, motor Mamdani de 27 reglas (Módulo 3) | 5 |
| Criterio de sustitución (y* ≥ 0.65, D ≥ 0.70) e historial de correcciones (Módulo 4) | 5 |
| Registro por enunciado en CSV (Módulo 4) | 7 |
| Interfaz de alto contraste, tamaño de letra ajustable, área fija (Módulo 5) | 6 |
| Exportación de apuntes en TXT y PDF (Módulo 5) | 6 |
| Panel del investigador y condiciones T0/T2 (Módulo 5) | 7 |
| Cuestionario SUS en la app (Módulo 5) | 7 |
| Pruebas unitarias y equivalencia con la especificación FCL | 8 |

---

## 4. Consejos de grabación
- Ensaya una vez completa con cronómetro. Si te pasas de 7:45, acorta las escenas 2 y 7, que son las más habladas.
- Graba por escenas y únelas después. Así, si el reconocedor falla en una toma, solo repites esa escena.
- Lee las frases de la demo con ritmo natural y no exageres la pronunciación para provocar errores.
- No muestres resultados de evaluación en aula: esa fase aún está pendiente según el cronograma.
- Exporta en MP4 (H.264), 1080p. Ocho minutos suelen ocupar entre 150 y 400 MB.

---

## 5. Subida y permisos de descarga (Google Drive)
1. Sube el MP4 a Google Drive.
2. Clic derecho → **Compartir** → en *Acceso general* elige **"Cualquier persona con el enlace"** y el rol **Lector**.
3. En el engranaje (⚙) del cuadro Compartir, deja **marcada** la casilla *"Los lectores y comentadores pueden ver la opción para descargar, imprimir y copiar"*. Así la descarga queda **permitida**.
4. Copia el enlace y ábrelo en una **ventana de incógnito**, sin tu sesión iniciada. Comprueba que se reproduce y que aparece el botón de **Descargar**.
5. Evita YouTube: no permite descargar el video.

---

## 6. Contenido del segundo PDF
- Título de la tesis, autor, asesor, universidad y fecha.
- "Video de avance del prototipo (duración: m:ss)".
- El enlace completo y clicable.
- Una línea: "Acceso: cualquier persona con el enlace; descarga habilitada."
- Opcional: la tabla de la sección 3 de este guion, con el minuto en que aparece cada funcionalidad.
