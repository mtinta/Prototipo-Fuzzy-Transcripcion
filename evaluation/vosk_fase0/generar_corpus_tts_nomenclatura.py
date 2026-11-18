#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Corpus de nomenclatura química, hidrocarburos y unidades con errores reales de Vosk (voz sintética Piper -> Vosk), para medir los
términos agregados por scripts/agregar_quimica_nomenclatura_unidades.py. Mismo método y limitaciones que generar_corpus_tts.py.

  .venv/bin/python generar_corpus_tts_nomenclatura.py

Salidas (en evaluation/): corpus_vosk_tts_nomenclatura_prueba.json (clase) y corpus_vosk_tts_nomenclatura_control.json
(frases correctas con palabras comunes parecidas a los términos: cualquier sustitución es falsa corrección).
"""
import json
import os
import sys
import tempfile
import wave

AQUI = os.path.dirname(os.path.abspath(__file__))
EVALUACION = os.path.dirname(AQUI)
sys.path.insert(0, AQUI)
from generar_corpus_tts import CARPETA_VOCES, MODELO_VOSK, VOCES, referencia  # noqa: E402
from vosk_fase0 import transcribir  # noqa: E402


def main():
    from piper import PiperVoice
    from vosk import Model, SetLogLevel

    SetLogLevel(-1)
    modelo = Model(MODELO_VOSK)
    with open(os.path.join(AQUI, "frases_quimica_nomenclatura_tts.json"), encoding="utf-8") as f:
        frases = json.load(f)

    for nombre in ("prueba", "control"):
        corpus = []
        for voz in VOCES:
            piper = PiperVoice.load(os.path.join(CARPETA_VOCES, voz + ".onnx"))
            for k, frase in enumerate(frases[nombre]):
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
                    "gap_s": 600 if k == 0 else 8,
                    "nota": f"TTS {voz} + Vosk vosk-model-small-es-0.42 (nomenclatura química y unidades {nombre})",
                })
                print(f"[{nombre}] {voz} {k + 1}/{len(frases[nombre])}: {corpus[-1]['raw_asr']}")
        salida = os.path.join(EVALUACION, f"corpus_vosk_tts_nomenclatura_{nombre}.json")
        with open(salida, "w", encoding="utf-8") as f:
            json.dump(corpus, f, ensure_ascii=False, indent=1)
        print(f"-> {salida} ({len(corpus)} frases)")


if __name__ == "__main__":
    main()
