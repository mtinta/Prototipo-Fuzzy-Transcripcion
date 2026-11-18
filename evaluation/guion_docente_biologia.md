# GUION DE PRUEBA: CLASE DE CIENCIA Y TECNOLOGÍA (SECUNDARIA)
## Prototipo de tesis: subtitulado en tiempo real con corrección difusa Mamdani

**Objetivo de la prueba:**
Comprobar en el dispositivo que la app capta la voz del docente, corrige los términos técnicos mal reconocidos con el motor difuso y muestra subtítulos continuos, sin cambiar palabras que ya estaban bien.

- **Área curricular:** Ciencia y Tecnología (CNEB), contenidos de biología, 3.° a 4.° de secundaria
- **Tema:** estructura celular, organelos y metabolismo energético
- **Diccionario activo:** Ciencia y Tecnología (648 términos: 403 de biología y 245 de química y ciclos biogeoquímicos, agregados el 17/11/2026; en el código se llama `BIOLOGIA`)
- **Ritmo de lectura:** natural, unas 120 a 140 palabras por minuto, con una pausa de 1 a 2 segundos al final de cada frase para que el reconocedor cierre el enunciado.
- **Total:** 4 bloques de 5 frases = **20 frases**.

---

## QUÉ FRASES USAR EN CADA PRUEBA

Las pruebas se describen en `docs/tesis/plan_de_pruebas_ejecucion_v3.md`, sección 6 (Fase A).

| Prueba | Qué leer | Cómo |
|---|---|---|
| VER-01 (funciones) | 3 frases cualesquiera del Bloque 1 | Con pausas |
| ASR-01 (sin conexión) | Las 5 frases del **Bloque 1** | En modo avión, con pausas |
| ASR-02 (confianza) | Las **20 frases** (Bloques 1 a 4) | En modo avión, con pausas |
| ASR-03 (continuidad) | Bloques 1 a 4 seguidos, unos **3 minutos** | **Sin pausas** entre frases |
| PERF-01 (rendimiento) | Los 4 bloques, dos veces, unos 5 minutos | Con pausas |

---

## INSTRUCCIONES PREVIAS EN EL DISPOSITIVO

1. **Panel del investigador:** mantén pulsado el título **"Prueba"**. Escribe el participante (`INV` en las pruebas técnicas) y la etiqueta de la prueba (`prueba`, `offline`, `asr02`…). Deja encendido **"Mostrar corrección difusa"** (condición T2) y pulsa **APLICAR E INICIAR SESIÓN**.
2. **Área curricular:** arriba debe verse la etiqueta fija **"Ciencia y Tecnología"**. No hay que elegir asignatura.
3. **Micrófono:** conecta el receptor USB-C al teléfono **antes** de iniciar la escucha; la app debe indicar que usa el micrófono externo. Coloca el transmisor de solapa a **15–20 cm de la boca**.
4. **Lectura:** lee cada bloque y observa cómo aparece el texto en amarillo sobre negro.
5. **Al terminar:** abre el **Historial de correcciones** (icono de corrector en la barra superior) para ver, por cada palabra cambiada, la palabra original, la corregida y los valores $C$, $D$, $S$ e $y^*$. Los datos completos quedan en el CSV de la sesión.

---

## GUION DEL DOCENTE PARA LECTURA EN VOZ ALTA

### Bloque 1: Introducción y diferenciación celular
*(Tono explicativo y pausas naturales)*

> "Buenos días, jóvenes. Hoy continuaremos con la unidad de Ciencia y Tecnología.
> Como vimos la clase anterior, la célula es la unidad anatómica y funcional de todos los seres vivos.
> Debemos distinguir claramente entre una célula procariota y una célula eucariota.
> La célula procariota, como la bacteria, carece de un núcleo definido y su material genético está disperso en el citoplasma.
> En cambio, la célula eucariota posee una membrana nuclear que protege el nucleolo y alberga los cromosomas."

---

### Bloque 2: Organelos y metabolismo en la célula vegetal
*(Términos técnicos propensos a errores de reconocimiento)*

> "Ahora observemos con atención la célula vegetal.
> La célula vegetal realiza la fotosíntesis dentro del cloroplasto gracias al pigmento de la clorofila.
> Este proceso permite transformar la energía luminosa en energía química almacenada en la glucosa.
> Además, tanto la célula vegetal como la célula animal poseen mitocondria.
> La mitocondria es el organelo encargado de la respiración celular y la producción de energía en forma de adenosina trifosfato."

---

### Bloque 3: Sistema de endomembranas y síntesis de proteínas
*(Términos compuestos y fonéticamente complejos)*

> "En el citoplasma encontramos una red compleja de membranas.
> El retículo endoplasmático rugoso contiene adherido al ribosoma, encargado de fabricar cada proteína.
> Luego, las proteínas son empaquetadas en las vesículas del aparato de golgi.
> Para la digestión celular, la célula utiliza el lisosoma y almacena agua y nutrientes en una gran vacuola.
> Todo este equilibrio interno permite mantener la homeostasis en el organismo."

---

### Bloque 4: Genética y herencia (bloque de control)
*(Mezcla términos técnicos con palabras comunes para comprobar que no haya falsas correcciones)*

> "Finalmente, en el núcleo encontramos la molécula de la herencia: el ácido desoxirribonucleico, conocido como ADN.
> El ADN contiene el genoma y define el genotipo y el fenotipo de cada individuo.
> Durante la mitosis, la célula se divide para el crecimiento de los tejidos; mientras que en la meiosis se originan los gametos para la reproducción.
> Recuerden que factores del ambiente como la temperatura o el calor no alteran la secuencia del gen, pero sí pueden influir en su expresión.
> Revisen estos conceptos para la evaluación de la próxima semana."

---

## CÓMO DECIDE LA APP (RESUMEN)

Por cada palabra candidata (se descartan artículos, preposiciones, conectores y palabras de menos de tres letras), el motor difuso combina tres valores entre 0 y 1:

- **$C$, confianza acústica:** la entrega el reconocedor de Android, un valor por enunciado. Si no la entrega, la app usa 0.65 (neutral).
- **$D$, similitud con el término del diccionario:** $D = 0.6\,D_{ort} + 0.4\,D_{fon}$, donde la parte fonética iguala el seseo, el yeísmo, *b/v* y la *h* muda.
- **$S$, coherencia temática:** es alta si en la oración o en lo que va de la clase aparecen otros términos del mismo grupo temático.

Las **27 reglas** Mamdani producen el grado de justificación $y^*$. La palabra se sustituye solo si **$y^* \ge 0.65$, $D \ge 0.70$** y no es un simple plural o singular del término. En cualquier otro caso se conserva la palabra original.

---

## QUÉ OBSERVAR DURANTE LA PRUEBA

Son ejemplos de comportamiento esperado, no resultados garantizados: el reconocedor puede acertar la palabra (y entonces no hay nada que corregir) o equivocarse de otra manera.

| Término dicho | Error posible del reconocedor | Comportamiento esperado |
|---|---|---|
| **célula** | *"cédula"*, *"sedula"* | Se corrige a **célula** si hay otros términos del tema cerca (núcleo, citoplasma) |
| **cloroplasto** | *"coroplasto"*, *"coloroplasto"* | Se corrige a **cloroplasto** (similitud alta) |
| **citoplasma** | *"sitoplasma"* | Se corrige: la parte fonética iguala *s/c* |
| **ribosoma** | *"riposoma"* | Se corrige a **ribosoma** |
| **nucleolo** | *"nuleolo"* | Se corrige a **nucleolo** |
| **fotosíntesis** | *"fotosintesis"* (sin tilde) | Puede quedar sin tilde; el WER ignora las tildes, así que no cuenta como error |
| **calor** (control) | *"calor"* | **No** debe cambiar a *cloro*: si cambia, es una **falsa corrección** y hay que anotarla |
| **la, de, en, para** | — | No pasan por el motor (filtro de candidatas) |

---

## LISTA DE COMPROBACIÓN AL TERMINAR

- [ ] **Latencia:** el texto aparece en menos de 1 segundo después de cada frase (se mide en el CSV, columna `ms_fin_voz_a_publicado`).
- [ ] **Sin marcas:** las palabras corregidas aparecen limpias, sin colores ni subrayados.
- [ ] **Historial de correcciones:** muestra la palabra original, la corregida y $y^* \ge 0.65$.
- [ ] **Falsas correcciones:** ninguna palabra común (*calor*, *forma*, *mesa*…) fue cambiada por un término técnico. Si ocurrió, anotarla.
- [ ] **Apuntes:** "Exportar TXT" o "Exportar PDF" guarda el texto con los términos corregidos.

La app **no calcula el WER**: para eso hace falta la transcripción de referencia y el script `evaluation/evaluar_sesiones.py`. Las pruebas técnicas con este guion no sirven para medir la precisión de la tesis; solo comprueban que la app funciona.
