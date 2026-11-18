#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Mide el efecto de los términos de química y ciclos biogeoquímicos (scripts/agregar_quimica_ciclos.py).

Corre la capa difusa (espejo Python de la app, benchmark_runner.py) sobre los corpus TTS + Vosk de química y los
corpus anteriores, con la C real de Vosk y con C fija 0.65 (peor caso: motor Android o palabra dudosa). Para cada
palabra que la capa cambia dice si fue una corrección VERDADERA (la palabra nueva está en la referencia y la
original no) o FALSA (la palabra nueva no está en la referencia).

  python3 evaluar_quimica_ciclos.py            # tabla resumen
  python3 evaluar_quimica_ciclos.py --detalle  # además, cada falsa corrección
  python3 evaluar_quimica_ciclos.py --solo CyT  # solo los corpus cuyo nombre contiene el texto
Desde el 2026-11-17 incluye también los corpus de CyT 1.º-3.º (scripts/agregar_cyt_primero_tercero.py), y desde el
2026-11-18 los de nomenclatura química y unidades (scripts/agregar_quimica_nomenclatura_unidades.py).
Usa el diccionario y la lista OOV vigentes (app/src/main/assets, vosk_fase0/oov_terminos.json).
"""
import argparse
import contextlib
import difflib
import io
import json
import os
from collections import Counter

import benchmark_runner
from benchmark_runner import SPAN_MODE, normalize_text, run_benchmarks

AQUI = os.path.dirname(os.path.abspath(__file__))
CORPUS = [
    ("Nomenclatura y unidades TTS + Vosk, clase", "corpus_vosk_tts_nomenclatura_prueba.json"),
    ("Nomenclatura y unidades TTS + Vosk, control", "corpus_vosk_tts_nomenclatura_control.json"),
    ("CyT 1.º-3.º TTS + Vosk, clase", "corpus_vosk_tts_cyt_prueba.json"),
    ("CyT 1.º-3.º TTS + Vosk, control", "corpus_vosk_tts_cyt_control.json"),
    ("Química TTS + Vosk, clase", "corpus_vosk_tts_quimica_prueba.json"),
    ("Química TTS + Vosk, control", "corpus_vosk_tts_quimica_control.json"),
    ("Biología TTS + Vosk, desarrollo", "corpus_vosk_tts_desarrollo.json"),
    ("Biología TTS + Vosk, prueba", "corpus_vosk_tts_prueba.json"),
    ("Control OpenSLR 73 (cotidiano)", "corpus_vosk_slr73_control.json"),
    ("Control clase correcta, C alta", "corpus_control_clase_c_alta.json"),
]


def cambios(raw, corregido, verdad):
    """Palabras que la capa cambió, clasificadas contra la referencia (multiconjunto de palabras)."""
    r, c = normalize_text(raw).split(), normalize_text(corregido).split()
    ref = Counter(normalize_text(verdad).split())
    verdaderas, falsas = [], []
    for op, i1, i2, j1, j2 in difflib.SequenceMatcher(a=r, b=c, autojunk=False).get_opcodes():
        if op == "equal":
            continue
        antes, despues = " ".join(r[i1:i2]), " ".join(c[j1:j2])
        if all(ref[w] > 0 for w in c[j1:j2]) and not all(ref[w] > 0 for w in r[i1:i2]):
            verdaderas.append((antes, despues))
        elif any(ref[w] == 0 for w in c[j1:j2]):
            falsas.append((antes, despues))
    return verdaderas, falsas


def medir(corpus, fixed_conf):
    with open(os.path.join(AQUI, "vosk_fase0", "oov_terminos.json"), encoding="utf-8") as f:
        oov = set(json.load(f))
    det = []
    with contextlib.redirect_stdout(io.StringIO()):
        m = run_benchmarks(corpus_name=os.path.join(AQUI, corpus), write_report=False, details=det,
                           span_mode=SPAN_MODE, asr_oov=oov, fixed_conf=fixed_conf)
    ver, fal = [], []
    for d in det:
        v, f = cambios(d["raw"], d["corrected"], d["truth"])
        ver += v
        fal += [(d["id"], a, b) for a, b in f]
    return m, ver, fal


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--detalle", action="store_true")
    ap.add_argument("--solo", default="", help="medir solo los corpus cuyo nombre contiene este texto")
    ap.add_argument("--sin-reglas-18nov", action="store_true",
                    help="medir sin las reglas del 18/11 (palabras vacías, prefijos sueltos, plural): el 'antes'")
    args = ap.parse_args()
    benchmark_runner.REGLAS_18NOV = not args.sin_reglas_18nov
    cab = f"{'Corpus':34} {'C':10} {'WER crudo':>9} {'WER dif':>8} {'KER crudo':>9} {'KER dif':>8} {'Verdad.':>7} {'Falsas':>6}"
    print(cab)
    print("-" * len(cab))
    todas = []
    for nombre, corpus in CORPUS:
        if args.solo not in nombre:
            continue
        for etiqueta, fc in (("Vosk", None), ("fija 0.65", 0.65)):
            m, ver, fal = medir(corpus, fc)
            print(f"{nombre:34} {etiqueta:10} {m['wer_raw']:8.2f}% {m['wer_fuzzy']:7.2f}% {m['ker_raw']:8.2f}% "
                  f"{m['ker_fuzzy']:7.2f}% {len(ver):7} {len(fal):6}")
            todas += [(nombre, etiqueta, *x) for x in fal]
    if args.detalle and todas:
        print("\nFalsas correcciones (corpus, C, id, antes -> después):")
        for nombre, etiqueta, i, a, b in todas:
            print(f"  {nombre} [{etiqueta}] #{i}: «{a}» -> «{b}»")


if __name__ == "__main__":
    main()
