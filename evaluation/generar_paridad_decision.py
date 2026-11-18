#!/usr/bin/env python3
"""Genera evaluation/paridad_decision.json: texto corregido que produce el benchmark (Python) en cada frase de los
corpus. La prueba DecisionParityTest (Kotlin) exige que DecisionEvaluator produzca exactamente el mismo texto,
con la misma memoria de clase (mismos intervalos gap_s) y la misma lista de términos fuera del vocabulario de Vosk.
Regenerar tras cambiar el algoritmo:  python3 generar_paridad_decision.py"""
import contextlib
import io
import json
import os

import benchmark_runner as b

AQUI = os.path.dirname(os.path.abspath(__file__))
CORPUS = [  # (archivo, ¿salida de Vosk? -> usa la lista de términos fuera de su vocabulario)
    ("corpus_vosk_tts_desarrollo.json", True),
    ("corpus_vosk_tts_prueba.json", True),
    ("corpus_vosk_slr73_control.json", True),
    ("corpus_control_clase_c_alta.json", True),
    ("corpus_vosk_tts_quimica_prueba.json", True),
    ("corpus_vosk_tts_quimica_control.json", True),
    ("corpus_vosk_tts_cyt_prueba.json", True),
    ("corpus_vosk_tts_cyt_control.json", True),
    ("corpus_vosk_tts_nomenclatura_prueba.json", True),
    ("corpus_vosk_tts_nomenclatura_control.json", True),
    ("corpus_arequipa_test.json", False),
    ("corpus_clase_continua.json", False),
    ("corpus_control_cotidiano.json", False),
]


def main():
    with open(os.path.join(AQUI, "vosk_fase0", "oov_terminos.json"), encoding="utf-8") as f:
        oov = set(json.load(f))
    out = []
    for nombre, es_vosk in CORPUS:
        ruta = os.path.join(AQUI, nombre)
        with open(ruta, encoding="utf-8") as f:
            corpus = json.load(f)
        det = []
        with contextlib.redirect_stdout(io.StringIO()):
            b.run_benchmarks(corpus_name=ruta, write_report=False, details=det, asr_oov=oov if es_vosk else None)
        out.append({"corpus": nombre, "oov": es_vosk, "items": [
            {"gap_s": it.get("gap_s", 8), "words": it["raw_asr"].split(), "confs": it.get("word_confidences", []),
             "expected": d["corrected"]}
            for it, d in zip(corpus, det)
        ]})
    with open(os.path.join(AQUI, "paridad_decision.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False)
    print(sum(len(c["items"]) for c in out), "frases ->", os.path.join(AQUI, "paridad_decision.json"))


if __name__ == "__main__":
    main()
