"""Verifica que app/src/main/assets/fuzzy_rules.fcl (anexo formal de la tesis) define el MISMO sistema
que el motor ejecutado en la app (JFuzzyEngine.kt, validado contra paridad_mamdani.json).

Intérprete mínimo de FCL: términos lineales por tramos, AND=MIN, ACT=MIN, ACCU=MAX, COG discreto de 100 pasos.
Uso: python3 verificar_fcl.py      (salida 0 = equivalentes)
"""
import json, os, re, sys

BASE = os.path.dirname(os.path.abspath(__file__))
FCL = os.path.join(BASE, "..", "app", "src", "main", "assets", "fuzzy_rules.fcl")
REF = os.path.join(BASE, "paridad_mamdani.json")


def parse_fcl(text):
    text = re.sub(r"//.*", "", text)
    variables = {}
    for kind, name, body in re.findall(r"(FUZZIFY|DEFUZZIFY)\s+(\w+)(.*?)END_(?:DE)?FUZZIFY", text, re.S):
        terms = {}
        for tname, pts in re.findall(r"TERM\s+(\w+)\s*:=\s*((?:\([^)]*\)\s*)+);", body):
            terms[tname] = [tuple(map(float, p.split(","))) for p in re.findall(r"\(([^)]*)\)", pts)]
        variables[name] = terms
    rules = [
        (m[0], m[1], m[2], m[3])
        for m in re.findall(
            r"RULE\s+\d+\s*:\s*IF\s+confianza\s+IS\s+(\w+)\s+AND\s+similitud\s+IS\s+(\w+)\s+AND\s+semantica\s+IS\s+(\w+)\s+THEN\s+justificacion\s+IS\s+(\w+)",
            text,
        )
    ]
    return variables, rules


def mu(points, x):
    if x <= points[0][0]:
        return points[0][1]
    if x >= points[-1][0]:
        return points[-1][1]
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        if x0 <= x <= x1:
            return y1 if x1 == x0 else y0 + (y1 - y0) * (x - x0) / (x1 - x0)
    return 0.0


def evaluate(variables, rules, c, d, s):
    act = {"Baja": 0.0, "Media": 0.0, "Alta": 0.0}
    for tc, td, ts, out in rules:
        w = min(mu(variables["confianza"][tc], c), mu(variables["similitud"][td], d), mu(variables["semantica"][ts], s))
        act[out] = max(act[out], w)
    num = den = 0.0
    for i in range(101):
        y = i / 100
        m = max(min(act[t], mu(variables["justificacion"][t], y)) for t in act)
        num += y * m
        den += m
    return num / den if den > 1e-5 else 0.0


def main():
    variables, rules = parse_fcl(open(FCL, encoding="utf-8").read())
    assert len(rules) == 27, f"se esperaban 27 reglas, hay {len(rules)}"
    combos = {(r[0], r[1], r[2]) for r in rules}
    assert len(combos) == 27, "la base de reglas no cubre las 27 combinaciones sin repetir"
    ref = json.load(open(REF))
    worst = max(abs(evaluate(variables, rules, c, d, s) - y) for c, d, s, y in ref)
    print(f"FCL: {len(rules)} reglas, base completa. {len(ref)} puntos comparados. Diferencia máxima = {worst:.2e}")
    if worst > 1e-9:
        print("FALLA: el FCL NO coincide con el motor de la app")
        sys.exit(1)
    print("OK: fuzzy_rules.fcl es equivalente al motor ejecutado por la app")


if __name__ == "__main__":
    main()
