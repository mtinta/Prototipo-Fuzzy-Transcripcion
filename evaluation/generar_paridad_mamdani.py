"""Genera evaluation/paridad_mamdani.json: salida de referencia del motor Python en una malla (C, D, S).
La prueba MamdaniParityTest (Kotlin) exige que JFuzzyEngine produzca los mismos valores."""
import json, os
import benchmark_runner as b

pts = [round(i / 10, 1) for i in range(11)] + [0.05, 0.35, 0.45, 0.65, 0.85, 0.95]
rows = [[c, d, s, b.evaluate_mamdani(c, d, s)] for c in pts for d in pts for s in pts]
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "paridad_mamdani.json")
json.dump(rows, open(out, "w"))
print(len(rows), "puntos ->", out)
