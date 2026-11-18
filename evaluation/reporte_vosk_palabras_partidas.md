# Capa difusa con salida real de Vosk: términos partidos

Vosk (`vosk-model-small-es-0.42`) no puede escribir 232 de los 403 términos del diccionario de biología (58 %) y los parte en varias palabras ("sito plasma" -> citoplasma). La capa difusa evalúa ahora uniones de 2-3 palabras seguidas (D >= 0.80 y mejor que sus pedazos) y, si el término no está en el vocabulario de Vosk, usa C neutra (0.65) en la unión. Las 27 reglas Mamdani no cambian.

| Corpus | Variante | WER crudo | WER difuso | KER crudo | KER difuso | Sustituciones |
|---|---|---|---|---|---|---|
| TTS + Vosk, desarrollo | Palabra a palabra (antes) | 33.99 % | 33.46 % | 63.37 % | 60.07 % | 23 |
| TTS + Vosk, desarrollo | Uniones + C neutra en uniones (ahora) | 33.99 % | 27.28 % | 63.37 % | 45.42 % | 72 |
| TTS + Vosk, prueba (no usado para ajustar) | Palabra a palabra (antes) | 37.48 % | 37.28 % | 72.62 % | 70.83 % | 12 |
| TTS + Vosk, prueba (no usado para ajustar) | Uniones + C neutra en uniones (ahora) | 37.48 % | 30.97 % | 72.62 % | 57.74 % | 43 |
| Control: OpenSLR 73 + Vosk (cotidiano) | Palabra a palabra (antes) | 7.31 % | 7.31 % | 0.00 % | 0.00 % | 0 |
| Control: OpenSLR 73 + Vosk (cotidiano) | Uniones + C neutra en uniones (ahora) | 7.31 % | 7.31 % | 0.00 % | 0.00 % | 0 |
| Control: clase correcta con C alta | Palabra a palabra (antes) | 0.00 % | 0.00 % | 0.00 % | 0.00 % | 0 |
| Control: clase correcta con C alta | Uniones + C neutra en uniones (ahora) | 0.00 % | 0.00 % | 0.00 % | 0.00 % | 0 |

Corpus TTS: frases de clase sintetizadas con Piper (voces es_MX-ald, es_MX-claude, es_AR-daniela) y transcritas por Vosk (`vosk_fase0/generar_corpus_tts.py`). La voz sintética es más limpia que un docente en aula (WER optimista); los errores por vocabulario sí son representativos. Control OpenSLR 73: Guevara-Rukoz et al. (2020), CC BY-SA 4.0.

Nota: «Sustituciones» del benchmark cuenta también palabras reemplazadas por sí mismas (p. ej. «núcleo»->«núcleo»); la app no las cuenta. WER y KER no se ven afectados.

## Alternativas evaluadas y descartadas

| Alternativa | Resultado | Motivo del descarte |
|---|---|---|
| C neutra (0.65) también en palabras sueltas cuyo término no está en el vocabulario de Vosk | 6 falsas correcciones en el control OpenSLR 73 («saber»->«suber», «manera»->«monera», «planta»->«plantae», «aplicación»->«replicación») | Una palabra entera escrita con C alta sí es evidencia acústica; la C neutra solo se justifica cuando el ASR partió la palabra |
| Unir aunque un pedazo ya sea el término completo («la fotosintesis») | Absorbía el artículo vecino (arequipa: WER 1.94 % -> 3.88 %) | El término bien escrito debe ser parte PROPIA del unido («plasma» ⊂ «citoplasma») |
| Regla 19 (C Alta, D Alta, S Alta) con consecuente Alta en vez de Media, para corregir «cédula» con C alta | Desarrollo: KER 45.8 % -> 43.2 %, pero 5 de 18 frases correctas del control de clase cambiaron «celular» por «célula» | Falsas correcciones en lenguaje de clase correcto; se mantienen las 27 reglas originales |
