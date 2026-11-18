#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Revisa si los términos candidatos de scripts/agregar_quimica_ciclos.py se parecen a palabras COMUNES del español.

Una palabra común con D >= 0.70 frente a un término puede acabar "corregida" por ese término (p. ej. "todo" -> "yodo"),
que es una falsa corrección: cambia lo que lee el estudiante sin avisar. Este script lista esos vecinos para decidir
qué términos excluir.

Lista de frecuencias: FrequencyWords (Hermit Dave, subtítulos de OpenSubtitles 2018, español, 50 000 palabras),
https://github.com/hermitdave/FrequencyWords/blob/master/content/2018/es/es_50k.txt (formato "palabra frecuencia").

  python3 scripts/revisar_vecinos_terminos.py es_50k.txt [--top 20000] [--modulo agregar_cyt_primero_tercero]
"""
import argparse
import importlib
import json
import os
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(RAIZ, "evaluation"))
sys.path.insert(0, os.path.join(RAIZ, "scripts"))
from benchmark_runner import calculate_similarity, is_candidate_noun, is_inflection_of, strip_accents  # noqa: E402

UMBRAL = 0.70  # = SUBSTITUTION_MIN_SIMILARITY: por debajo nunca se sustituye


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("frecuencias")
    ap.add_argument("--top", type=int, default=20000, help="cuántas palabras más frecuentes revisar")
    ap.add_argument("--modulo", default="agregar_quimica_ciclos",
                    help="script de scripts/ cuyos NUEVOS_CLUSTERS (y AMPLIAR_CLUSTERS) se revisan")
    args = ap.parse_args()
    modulo = importlib.import_module(args.modulo)

    with open(os.path.join(RAIZ, "app", "src", "main", "assets", "dictionaries", "biologia.json"), encoding="utf-8") as f:
        existentes = {d["term"].lower() for d in json.load(f)}
    candidatos = [t for _, _, _, ts in modulo.NUEVOS_CLUSTERS for t in ts]
    candidatos += [t for ts in getattr(modulo, "AMPLIAR_CLUSTERS", {}).values() for t in ts]
    candidatos = list(dict.fromkeys(candidatos))
    todos = existentes | {t.lower() for t in candidatos}
    todos_sin_tilde = {strip_accents(t) for t in todos}

    palabras = []
    with open(args.frecuencias, encoding="utf-8") as f:
        for rango, linea in enumerate(f, 1):
            if rango > args.top:
                break
            w = linea.split()[0].lower()
            if not w.isalpha() or not is_candidate_noun(w) or strip_accents(w) in todos_sin_tilde:
                continue
            if any(is_inflection_of(w, t) for t in todos):
                continue
            palabras.append((rango, w))

    riesgo = {}
    for t in candidatos:
        if t.lower() in existentes:
            continue  # ya estaba en el diccionario: no lo decide esta revisión
        lt = len(strip_accents(t))
        vecinos = []
        for rango, w in palabras:
            if abs(len(strip_accents(w)) - lt) > 0.35 * max(lt, len(w)):
                continue
            d = calculate_similarity(w, t)
            if d >= UMBRAL:
                vecinos.append((rango, w, round(d, 2)))
        if vecinos:
            riesgo[t] = vecinos

    print(f"{len(palabras)} palabras comunes revisadas (top {args.top}); candidatos nuevos: "
          f"{sum(1 for t in candidatos if t.lower() not in existentes)}; con vecinos D >= {UMBRAL}: {len(riesgo)}\n")
    for t, vs in sorted(riesgo.items(), key=lambda kv: min(r for r, _, _ in kv[1])):
        print(f"{t:22s} " + ", ".join(f"{w} (#{r}, D={d})" for r, w, d in vs[:8]))


if __name__ == "__main__":
    main()
