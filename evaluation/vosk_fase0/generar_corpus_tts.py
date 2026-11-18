#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Corpus de biología con errores REALES de Vosk: frases de clase -> voz sintética (Piper, sin conexión) -> Vosk.

Por qué: los corpus anteriores simulan errores al estilo de Android (una palabra mal escrita con C = 0.45). Vosk se
equivoca distinto: no conoce el 58 % de los términos del diccionario y los parte en varias palabras
("procariota" -> "pero carlota"). Para ajustar la capa difusa hacen falta esos errores tal como salen de Vosk.

Limitación (declararla en la tesis): la voz sintética es más limpia que un docente en aula, así que el WER de este
corpus es optimista. Los errores por vocabulario (palabras que Vosk no conoce) dependen del vocabulario del modelo,
no de la voz, y por eso sí son representativos.

  .venv/bin/python generar_corpus_tts.py

Salidas (en evaluation/): corpus_vosk_tts_desarrollo.json (guion + frases de desarrollo) y corpus_vosk_tts_prueba.json.
Mismo formato que los demás corpus de benchmark_runner.py.
"""

import json
import os
import re
import sys
import tempfile
import wave

AQUI = os.path.dirname(os.path.abspath(__file__))
EVALUACION = os.path.dirname(AQUI)
sys.path.insert(0, AQUI)
from vosk_fase0 import frases_guion, transcribir  # noqa: E402

VOCES = ["es_MX-ald-medium", "es_MX-claude-high", "es_AR-daniela-high"]
MODELO_VOSK = os.path.join(AQUI, "modelos", "vosk-model-small-es-0.42")
CARPETA_VOCES = os.path.join(AQUI, "modelos", "piper")


def referencia(frase):
    """Minúsculas, sin puntuación, CON tildes (el WER las ignora; los términos se escriben con tilde)."""
    t = re.sub(r"[^\wáéíóúüñ\s]", " ", frase.lower())
    return re.sub(r"\s+", " ", t).strip()


def main():
    from piper import PiperVoice
    from vosk import Model, SetLogLevel

    SetLogLevel(-1)
    modelo = Model(MODELO_VOSK)
    with open(os.path.join(AQUI, "frases_biologia_tts.json"), encoding="utf-8") as f:
        frases = json.load(f)
    conjuntos = {
        "desarrollo": frases_guion() + frases["desarrollo"],
        "prueba": frases["prueba"],
    }

    for nombre, lista in conjuntos.items():
        corpus = []
        for voz in VOCES:
            piper = PiperVoice.load(os.path.join(CARPETA_VOCES, voz + ".onnx"))
            for k, frase in enumerate(lista):
                with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
                    with wave.open(tmp.name, "wb") as wf:
                        piper.synthesize_wav(frase, wf)
                    enunciados, _, _ = transcribir(modelo, tmp.name)
                palabras = [p for e in enunciados for p in e]
                corpus.append({
                    "id": len(corpus) + 1,
                    "subject": "BIOLOGIA",
                    "ground_truth": referencia(frase),
                    "raw_asr": " ".join(p["palabra"] for p in palabras),
                    "word_confidences": [round(p["conf"], 4) for p in palabras],
                    # Una "clase" por voz: 8 s entre frases; 10 min al cambiar de voz (la memoria de clase se olvida)
                    "gap_s": 600 if k == 0 else 8,
                    "nota": f"TTS {voz} + Vosk vosk-model-small-es-0.42 ({nombre})",
                })
                print(f"[{nombre}] {voz} {k + 1}/{len(lista)}: {corpus[-1]['raw_asr']}")
        salida = os.path.join(EVALUACION, f"corpus_vosk_tts_{nombre}.json")
        with open(salida, "w", encoding="utf-8") as f:
            json.dump(corpus, f, ensure_ascii=False, indent=1)
        print(f"-> {salida} ({len(corpus)} frases)")


if __name__ == "__main__":
    main()
