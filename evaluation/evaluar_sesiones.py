#!/usr/bin/env python3
"""
=============================================================================
EVALUACIÓN CON VOZ REAL: análisis de las sesiones exportadas por la app
Tesis: Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De
       Lógica Difusa ... (UNSA) — Protocolo, Sección 7 del plan de subsanación 2.0
=============================================================================

Entrada (carpeta descargada con `adb pull /sdcard/Documents/PrototipoFuzzy/`; versiones antiguas: `.../files/evaluacion/`):
  sesion_<...>/sesion_<fecha>_<participante>_<condicion>.csv   una fila por enunciado (crudo Y corregido)
  sus_respuestas.csv                               respuestas SUS + 4 complementarias (sin puntaje; se calcula aquí)
  nasa_tlx_respuestas.csv                          respuestas NASA-TLX, una fila por mitad (la media se calcula aquí)
  sesion_<...>/<sesión>_aNN.wav + indice_audio.csv  grabación del docente (opcional): la plantilla indica el WAV
  (también lee el formato anterior: CSV sueltos en la raíz y WAV en audio/)
                                                   y el segundo de cada enunciado; --recortar crea un clip por enunciado
Entrada manual:
  referencias/<mismo nombre que la sesión>.txt     transcripción humana (ver --plantilla)
  usuarios.csv (opcional)                          NASA-TLX en papel, solo si no se usó la app (--plantilla-usuarios)

Condiciones comparadas sobre EXACTAMENTE la misma salida del ASR:
  T0 = texto crudo del ASR | T1 = diccionario + umbral nítido | T2 = capa difusa (lo que calculó la app)

Uso típico:
  python3 evaluar_sesiones.py --plantilla datos/            # crea referencias/*.txt para transcribir
  python3 evaluar_sesiones.py --plantilla --recortar datos/ # además, referencias/clips/<sesión>_<idx>.wav
  python3 evaluar_sesiones.py datos/                        # evalúa y escribe el reporte
  python3 evaluar_sesiones.py datos/ --usuarios datos/usuarios.csv

Solo usa la biblioteca estándar de Python (no requiere numpy ni scipy).
"""

import argparse
import csv
import glob
import json
import math
import os
import random
import re
import statistics
import sys
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from benchmark_runner import (  # noqa: E402  (espejo Python del motor, verificado contra Kotlin)
    calculate_ker, calculate_similarity, is_candidate_noun, normalize_text,
)

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "dictionaries")
REF_LINE = re.compile(r"^\s*(\d{1,4})\s*\|\s?(.*)$")
EMPTY_MARK = {"", "-", "<vacio>", "<vacío>", "∅"}

# ----------------------------------------------------------------------------- utilidades


def tokens(text):
    return normalize_text(text).split()


def align(ref, hyp):
    """Alineamiento de Levenshtein por palabras. Devuelve operaciones (op, i_ref, j_hyp), op en C/S/D/I."""
    n, m = len(ref), len(hyp)
    dp = [[0] * (m + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        dp[i][0] = i
    for j in range(m + 1):
        dp[0][j] = j
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            c = 0 if ref[i - 1] == hyp[j - 1] else 1
            dp[i][j] = min(dp[i - 1][j - 1] + c, dp[i - 1][j] + 1, dp[i][j - 1] + 1)
    ops, i, j = [], n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0 and dp[i][j] == dp[i - 1][j - 1] + (0 if ref[i - 1] == hyp[j - 1] else 1):
            ops.append(("C" if ref[i - 1] == hyp[j - 1] else "S", i - 1, j - 1))
            i, j = i - 1, j - 1
        elif i > 0 and dp[i][j] == dp[i - 1][j] + 1:
            ops.append(("D", i - 1, None))
            i -= 1
        else:
            ops.append(("I", None, j - 1))
            j -= 1
    return ops[::-1]


def edit_counts(ref, hyp):
    ops = align(ref, hyp)
    return (sum(o == "S" for o, _, _ in ops), sum(o == "D" for o, _, _ in ops), sum(o == "I" for o, _, _ in ops))


def char_errors(ref_text, hyp_text):
    r, h = list(" ".join(tokens(ref_text))), list(" ".join(tokens(hyp_text)))
    s, d, i = edit_counts(r, h)
    return s + d + i, len(r)


def pct(x):
    return f"{100 * x:.2f} %"


def quantile(values, q):
    v = sorted(values)
    if not v:
        return float("nan")
    k = (len(v) - 1) * q
    lo, hi = math.floor(k), math.ceil(k)
    return v[lo] + (v[hi] - v[lo]) * (k - lo)


# ----------------------------------------------------------------------------- estadística


def wilcoxon_signed_rank(diffs):
    """Wilcoxon de rangos con signo (bilateral). Exacto (distribución de permutación de signos, válida también
    con empates) si n <= 60; si no, aproximación normal con corrección por empates. Descarta diferencias nulas
    (método de Wilcoxon). Devuelve (n, W+, p, z, r)."""
    d = [x for x in diffs if abs(x) > 1e-12]
    n = len(d)
    if n == 0:
        return 0, 0.0, 1.0, 0.0, 0.0
    order = sorted(range(n), key=lambda k: abs(d[k]))
    ranks = [0.0] * n
    k = 0
    ties = []
    while k < n:
        j = k
        while j + 1 < n and abs(abs(d[order[j + 1]]) - abs(d[order[k]])) < 1e-12:
            j += 1
        avg = (k + j) / 2 + 1
        for t in range(k, j + 1):
            ranks[order[t]] = avg
        if j > k:
            ties.append(j - k + 1)
        k = j + 1
    w_plus = sum(r for r, x in zip(ranks, d) if x > 0)
    mean = n * (n + 1) / 4
    var = n * (n + 1) * (2 * n + 1) / 24 - sum(t ** 3 - t for t in ties) / 48
    z = (w_plus - mean) / math.sqrt(var) if var > 0 else 0.0
    if n <= 60:
        # Distribución exacta de W+ bajo H0 (cada signo ±1 equiprobable) sobre los rangos reales.
        # Los rangos medios son múltiplos de 0.5: se duplican para trabajar con enteros.
        counts = {0: 1}
        for rank in ranks:
            r2 = int(round(2 * rank))
            new = dict(counts)
            for s, c in counts.items():
                new[s + r2] = new.get(s + r2, 0) + c
            counts = new
        total = 2 ** n
        w2 = int(round(2 * w_plus))
        tail = min(w2, int(round(2 * sum(ranks))) - w2)
        lo = sum(c for s, c in counts.items() if s <= tail)
        p = min(1.0, 2 * lo / total)
    else:
        p = math.erfc(abs(z) / math.sqrt(2))
    return n, w_plus, p, z, abs(z) / math.sqrt(n)


def holm(pvalues):
    order = sorted(range(len(pvalues)), key=lambda i: pvalues[i])
    adj, running = [0.0] * len(pvalues), 0.0
    for rank, i in enumerate(order):
        running = max(running, min(1.0, (len(pvalues) - rank) * pvalues[i]))
        adj[i] = running
    return adj


def bootstrap_ci(units, stat, reps=10000, seed=2026):
    """IC 95 % por bootstrap percentil remuestreando enunciados."""
    rnd = random.Random(seed)
    vals = []
    for _ in range(reps):
        sample = [units[rnd.randrange(len(units))] for _ in units]
        v = stat(sample)
        if v is not None:
            vals.append(v)
    return quantile(vals, 0.025), quantile(vals, 0.975)


# ----------------------------------------------------------------------------- carga de datos


def load_sessions(folder):
    sessions = []
    # Formato actual: una carpeta por sesión (sesion_X/sesion_X.csv); anterior: todos los CSV juntos en la raíz
    paths = glob.glob(os.path.join(folder, "sesion_*", "sesion_*.csv")) + glob.glob(os.path.join(folder, "sesion_*.csv"))
    seen = set()
    for path in sorted(paths, key=os.path.basename):
        if os.path.basename(path) in seen:
            continue
        seen.add(os.path.basename(path))
        with open(path, encoding="utf-8", newline="") as f:
            rows = list(csv.DictReader(f))
        if rows:
            sessions.append({"path": path, "name": os.path.splitext(os.path.basename(path))[0], "rows": rows})
    return sessions


def load_dictionaries():
    out = {}
    for p in glob.glob(os.path.join(ASSETS, "*.json")):
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        # biologia_vosk_oov.json es una lista de palabras (vocabulario que Vosk no conoce), no un diccionario.
        if not data or not isinstance(data[0], dict):
            continue
        out[os.path.splitext(os.path.basename(p))[0].upper()] = [d["term"] for d in data]
    return out


def read_reference(session, refs_dir):
    """Devuelve una lista de referencias por enunciado (misma longitud que rows) o None si no hay archivo.
    Formatos: (a) plantilla 'NNN | texto' por enunciado; (b) texto continuo, que se reparte entre enunciados
    alineándolo con el texto crudo del ASR."""
    path = os.path.join(refs_dir, session["name"] + ".txt")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as f:
        lines = [ln.rstrip("\n") for ln in f if not ln.lstrip().startswith("#")]
    rows = session["rows"]
    parsed = [REF_LINE.match(ln) for ln in lines if ln.strip()]
    if parsed and all(parsed):
        refs = {int(m.group(1)): m.group(2).strip() for m in parsed}
        missing = [r["idx"] for r in rows if int(r["idx"]) not in refs]
        if missing:
            sys.exit(f"ERROR {path}: faltan los enunciados {missing[:10]} en la plantilla.")
        return ["" if refs[int(r['idx'])].lower() in EMPTY_MARK else refs[int(r["idx"])] for r in rows]
    return project_continuous(" ".join(lines), [r["texto_crudo"] for r in rows])


def project_continuous(ref_text, raw_texts):
    """Reparte una referencia continua entre enunciados según su alineamiento con el crudo concatenado."""
    ref = tokens(ref_text)
    hyp, owner = [], []
    for k, t in enumerate(raw_texts):
        for w in tokens(t):
            hyp.append(w)
            owner.append(k)
    out = [[] for _ in raw_texts]
    current = 0
    for op, i, j in align(ref, hyp):
        if j is not None:
            current = owner[j]
        if i is not None:
            out[current].append(ref[i])
    return [" ".join(ws) for ws in out]


# ----------------------------------------------------------------------------- condición T1


def crisp_correct(raw_text, terms, t_d, conf=None, t_c=1.0):
    """Línea base nítida (sin lógica difusa): sustituye la palabra candidata por el término más parecido si
    D >= t_d (y C <= t_c cuando el ASR informa confianza). Misma regla que `benchmark_runner --booleana`.
    `conf` es un número (una C por enunciado, Android) o una lista con una C por palabra (Vosk)."""
    out = []
    per_word = conf if isinstance(conf, list) else None
    for k, w in enumerate(raw_text.split()):
        if per_word is not None:
            conf = per_word[k]
        best, best_d = None, 0.0
        if is_candidate_noun(w) and terms:
            for t in terms:
                d = calculate_similarity(w, t)
                if d > best_d:
                    best, best_d = t, d
        ok_conf = conf is None or conf <= t_c
        out.append(best if best and best_d >= t_d and ok_conf and normalize_text(best) != normalize_text(w) else w)
    return " ".join(out)


def is_vosk(row):
    return "vosk" in row.get("perfil_asr", "").lower()


def informative_confidence(row):
    """C del enunciado. Android entrega una C por ALTERNATIVA (la primera es la de la mejor hipótesis);
    Vosk, una por PALABRA (se resume con la media). None si el ASR no informó una confianza válida."""
    v = row.get("confianza_api", "NA").split()
    try:
        vals = [float(x) for x in v]
    except ValueError:
        return None
    if is_vosk(row):
        return statistics.mean(vals) if vals and all(0.0 <= c <= 1.0 for c in vals) else None
    return vals[0] if vals and 0.0 < vals[0] <= 1.0 else None


def word_confidences(row):
    """C para la línea base T1: lista por palabra con Vosk (si cuadra con texto_crudo), si no la del enunciado."""
    if is_vosk(row):
        try:
            vals = [float(x) for x in row.get("confianza_api", "NA").split()]
        except ValueError:
            vals = []
        if vals and len(vals) == len(row.get("texto_crudo", "").split()):
            return vals
    return informative_confidence(row)


# ----------------------------------------------------------------------------- métricas


def change_groups(raw_tokens, cond_tokens):
    """Tramos que la condición cambió respecto al crudo: [(índices en el crudo, palabras nuevas)].
    Una unión de palabras partidas por el ASR ("sito plasma" -> "citoplasma") es UN cambio; las sustituciones
    1 a 1 contiguas se separan, como antes de existir las uniones."""
    groups, cur_r, cur_c = [], [], []
    for op, i, j in align(raw_tokens, cond_tokens):
        if op == "C":
            if cur_r or cur_c:
                groups.append((cur_r, cur_c))
                cur_r, cur_c = [], []
            continue
        if i is not None:
            cur_r.append(i)
        if j is not None:
            cur_c.append(cond_tokens[j])
    if cur_r or cur_c:
        groups.append((cur_r, cur_c))
    out = []
    for rs, cs in groups:
        if len(rs) == len(cs):
            out.extend(([ri], [ci]) for ri, ci in zip(rs, cs))
        else:
            out.append((rs, cs))
    return out


def substitution_analysis(raw, cond, ref):
    """Clasifica cada cambio de la condición respecto al crudo: acierto, falsa corrección (las palabras del crudo
    eran correctas) o cambio inútil (palabra errada cambiada por otra también errada). Admite uniones de
    varias palabras en una (el texto corregido puede tener menos palabras que el crudo)."""
    r, c, g = tokens(raw), tokens(cond), tokens(ref)
    raw_status = {}
    for op, i, j in align(g, r):
        if j is not None:
            raw_status[j] = (op, g[i] if i is not None else None)
    res = {"aciertos": 0, "falsas": 0, "inutiles": 0, "crudo_correctas": 0, "crudo_sust": 0, "detalle": []}
    for j in range(len(r)):
        op = raw_status.get(j, ("I", None))[0]
        res["crudo_correctas"] += op == "C"
        res["crudo_sust"] += op == "S"
    # Igual número de palabras: comparación posición a posición (idéntica al análisis anterior a las uniones)
    groups = [([j], [cw]) for j, (rw, cw) in enumerate(zip(r, c)) if rw != cw] if len(r) == len(c) \
        else change_groups(r, c)
    for rs, cs in groups:
        statuses = [raw_status.get(j, ("I", None)) for j in rs]
        golds = [gold for _, gold in statuses if gold is not None]
        if rs and all(op == "C" for op, _ in statuses):
            kind = "falsa"
        elif cs and cs == golds:
            kind = "acierto"
        else:
            kind = "inutil"
        res[{"falsa": "falsas", "acierto": "aciertos", "inutil": "inutiles"}[kind]] += 1
        res["detalle"].append((kind, " ".join(r[j] for j in rs), " ".join(cs), " ".join(golds)))
    return res


def evaluate(units, keywords_by_subject, label):
    """units: lista de dicts con ref, T0, T1, T2, subject. Devuelve métricas agregadas por condición."""
    out = {}
    for cond in ("T0", "T1", "T2"):
        S = D = I = N = ce = cn = kc = kt = 0
        sub = {"aciertos": 0, "falsas": 0, "inutiles": 0, "crudo_correctas": 0, "crudo_sust": 0, "omitidos": 0}
        for u in units:
            ref_t, hyp_t = tokens(u["ref"]), tokens(u[cond])
            s, d, i = edit_counts(ref_t, hyp_t)
            S, D, I, N = S + s, D + d, I + i, N + len(ref_t)
            e, n = char_errors(u["ref"], u[cond])
            ce, cn = ce + e, cn + n
            _, _, c_ok, c_tot = calculate_ker(u[cond], u["ref"], keywords_by_subject.get(u["subject"], set()))
            kc, kt = kc + c_ok, kt + c_tot
            if cond != "T0":
                a = substitution_analysis(u["T0"], u[cond], u["ref"])
                if a is None:
                    sub["omitidos"] += 1
                else:
                    for k in ("aciertos", "falsas", "inutiles", "crudo_correctas", "crudo_sust"):
                        sub[k] += a[k]
        changed = sub["aciertos"] + sub["falsas"] + sub["inutiles"]
        out[cond] = {
            "N": N, "S": S, "D": D, "I": I,
            "WER": (S + D + I) / N if N else float("nan"),
            "CER": ce / cn if cn else float("nan"),
            "KER": 1 - kc / kt if kt else float("nan"), "kw_total": kt,
            "sust": changed, **sub,
            "TCR": sub["aciertos"] / sub["crudo_sust"] if sub["crudo_sust"] else float("nan"),
            "FCR": sub["falsas"] / sub["crudo_correctas"] if sub["crudo_correctas"] else float("nan"),
            "PREC_SUST": sub["aciertos"] / changed if changed else float("nan"),
        }
    out["label"] = label
    return out


def corpus_wer(sample, cond):
    n = sum(len(tokens(u["ref"])) for u in sample)
    if n == 0:
        return None
    return sum(sum(edit_counts(tokens(u["ref"]), tokens(u[cond]))) for u in sample) / n


def utt_wer(u, cond):
    n = len(tokens(u["ref"]))
    return sum(edit_counts(tokens(u["ref"]), tokens(u[cond]))) / n if n else None


# ----------------------------------------------------------------------------- reporte


def fmt_num(x, digits=1):
    return "—" if x is None or (isinstance(x, float) and math.isnan(x)) else f"{x:.{digits}f}"


def latency_section(all_rows):
    lines = ["## 3. Desempeño temporal (HE4)", ""]
    groups = {}
    for r in all_rows:
        groups.setdefault(r.get("tipo_dispositivo") or "sin dato", []).append(r)
    lines += ["Se reporta por tipo de dispositivo (teléfono / tableta).", ""]
    for device, rows in sorted(groups.items()):
        lines += [f"### Dispositivo: {device} ({len(rows)} enunciados)", ""] + latency_table(rows)
    lines += ["Se descartan los 10 primeros enunciados (calentamiento) cuando hay más de 20. "
              "`t_publicado` = texto entregado a la interfaz; Compose lo dibuja en el siguiente cuadro (~16 ms).", ""]
    return lines


def latency_table(all_rows):
    lines = []

    def col(name):
        vals = []
        for r in all_rows:
            try:
                vals.append(float(r[name]))
            except (KeyError, ValueError):
                pass
        return vals[10:] if len(vals) > 20 else vals  # descarta calentamiento si hay datos suficientes

    lines += ["| Medida | n | Mediana | Media | p95 | Máx | % < umbral |", "|---|---|---|---|---|---|---|"]
    for name, title, thr in (("ms_capa_difusa", "Capa difusa por enunciado (ms)", 100.0),
                             ("ms_fin_voz_a_publicado", "Fin de la voz → texto publicado (ms)", 1000.0),
                             ("ms_inicio_voz_a_primer_parcial", "Inicio de la voz → primer parcial (ms)", 1000.0)):
        v = col(name)
        if not v:
            lines.append(f"| {title} | 0 | — | — | — | — | — |")
            continue
        under = sum(x < thr for x in v) / len(v)
        lines.append(f"| {title} | {len(v)} | {quantile(v, .5):.1f} | {statistics.mean(v):.1f} | "
                     f"{quantile(v, .95):.1f} | {max(v):.1f} | {pct(under)} (< {thr:.0f}) |")
    return lines + [""]


def confidence_section(all_rows):
    vals, na, profiles = [], 0, {}
    for r in all_rows:
        profiles[r.get("perfil_asr", "")] = profiles.get(r.get("perfil_asr", ""), 0) + 1
        c = informative_confidence(r)
        if c is None:
            na += 1
        else:
            vals.append(c)
    lines = ["## 4. Confianza del reconocedor (ASR-02) y perfil usado", "",
             f"- Enunciados: {len(all_rows)}; con confianza válida (0 < C ≤ 1): {len(vals)}; sin confianza: {na}."]
    if len(vals) >= 2:
        lines.append(f"- Confianza válida: media {statistics.mean(vals):.3f}, DE {statistics.stdev(vals):.3f}, "
                     f"mín {min(vals):.3f}, máx {max(vals):.3f}.")
    lines.append("- Perfil de reconocimiento: " + "; ".join(f"{k or '¿?'}: {v}" for k, v in profiles.items()) + ".")
    lines += ["- Si algún enunciado usó el perfil **en línea**, ese tramo NO fue on-device: declararlo.", ""]
    return lines


COMPLEMENTARY_ITEMS = [  # (columna, texto, inverso)
    ("comp_legibilidad", "El texto se leía con facilidad", False),
    ("comp_velocidad", "El texto avanzaba a una velocidad que podía seguir", False),
    ("comp_comodidad", "Me sentí cómodo leyendo durante la clase", False),
    ("comp_distraccion", "La pantalla me distraía de la clase (inverso)", True),
]


def sus_score(row):
    """Puntaje SUS 0-100 a partir de item1..item10 (Brooke, 1996): impares r-1, pares 5-r, suma x 2.5.
    La app solo guarda las respuestas; el puntaje se calcula siempre aquí."""
    r = [int(row[f"item{k}"]) for k in range(1, 11)]
    return 2.5 * sum((v - 1) if k % 2 == 0 else (5 - v) for k, v in enumerate(r))


def read_csv_rows(path):
    with open(path, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def users_section(path, folder):
    lines = ["## 5. Estudio con usuarios (HE6, HE7)", ""]
    sus_path = os.path.join(folder, "sus_respuestas.csv")
    if os.path.exists(sus_path):
        sus = read_csv_rows(sus_path)
        for r in sus:
            r["sus"] = sus_score(r)
        scores = [r["sus"] for r in sus]
        lines += ["### SUS", "", "| Participante | Etiqueta | Dispositivo | SUS |", "|---|---|---|---|"]
        lines += [f"| {r['participante']} | {r['etiqueta']} | {r.get('tipo_dispositivo') or 'sin dato'} | {float(r['sus']):.1f} |" for r in sus]
        if scores:
            sd = statistics.stdev(scores) if len(scores) > 1 else 0.0
            lines += ["", f"n = {len(scores)}; media {statistics.mean(scores):.1f}; DE {sd:.1f}; "
                          f"mediana {statistics.median(scores):.1f}; ≥ 68: {sum(s >= 68 for s in scores)}/{len(scores)}.", ""]
        comp = [r for r in sus if all((r.get(c) or "").strip() for c, _, _ in COMPLEMENTARY_ITEMS)]
        if comp:
            lines += ["### Ítems complementarios (1–5)", "",
                      "El ítem de distracción se invierte (6 − r) para que en todos 5 sea lo favorable.", "",
                      "| Ítem | Mediana | Media | Valores individuales |", "|---|---|---|---|"]
            for col, text, inverse in COMPLEMENTARY_ITEMS:
                vals = [(6 - int(r[col])) if inverse else int(r[col]) for r in comp]
                indiv = ", ".join(f"{r['participante']}={v}" for r, v in zip(comp, vals))
                lines.append(f"| {text} | {statistics.median(vals):.1f} | {statistics.mean(vals):.2f} | {indiv} |")
            lines.append("")
    else:
        lines += ["Sin `sus_respuestas.csv`.", ""]

    # NASA-TLX: primero el CSV en papel (--usuarios, opcional) y encima el de la app; gana la última fila
    # de cada participante y condición.
    sources = [p for p in (path, os.path.join(folder, "nasa_tlx_respuestas.csv")) if p and os.path.exists(p)]
    if not sources:
        return lines + ["Sin respuestas del NASA-TLX (`nasa_tlx_respuestas.csv` o `--usuarios`).", ""]
    by, repeated = {}, []
    for src in sources:
        for r in read_csv_rows(src):
            if not all((r.get(f"tlx_{k}") or "").strip() for k in range(1, 7)):
                continue
            tlx = [float(r[f"tlx_{k}"]) for k in range(1, 7)]
            cond = r["condicion"].strip().upper()[:2]
            entry = by.setdefault(r["participante"], {})
            if cond in entry:
                repeated.append(f"{r['participante']}/{cond}")
            entry[cond] = {"TLX": sum(tlx) / 6, "orden": (r.get("orden") or "").strip()}
    paired = {p: v for p, v in by.items() if "T0" in v and "T2" in v}
    lines += ["### Carga cognitiva (NASA-TLX Raw, T0 frente a T2, intrasujeto)", "",
              f"Fuente: {', '.join(os.path.basename(p) for p in sources)}. "
              f"Participantes con ambas condiciones: {len(paired)}.", ""]
    if repeated:
        lines += [f"Aviso: respuestas repetidas (se usa la última): {', '.join(sorted(set(repeated)))}.", ""]
    lines += ["| Participante | TLX T0 (orden) | TLX T2 (orden) | T2 − T0 |", "|---|---|---|---|"]
    for p, v in sorted(by.items()):
        def cell(c):
            return f"{v[c]['TLX']:.1f} ({v[c]['orden'] or '?'})" if c in v else "—"
        delta = f"{v['T2']['TLX'] - v['T0']['TLX']:+.1f}" if p in paired else "—"
        lines.append(f"| {p} | {cell('T0')} | {cell('T2')} | {delta} |")
    lines += ["", "| Medida | Mediana T0 | Mediana T2 | n≠0 | W+ | p (bilateral) | r |", "|---|---|---|---|---|---|---|"]
    for m in ("TLX",):
        a = [v["T0"][m] for v in paired.values()]
        b = [v["T2"][m] for v in paired.values()]
        if not a:
            continue
        n, w, p, _, r = wilcoxon_signed_rank([y - x for x, y in zip(a, b)])
        lines.append(f"| {m} | {statistics.median(a):.1f} | {statistics.median(b):.1f} | {n} | {w:.1f} | {p:.3f} | {r:.2f} |")
    lines += ["", "Exploratorio: con muestras tan pequeñas se reportan los valores individuales y no se generaliza.", ""]
    return lines


# ----------------------------------------------------------------------------- plantillas


def load_audio_index(folder):
    """Tramos de audio grabados por la app, por sesión: {sesion: [(t_inicio_audio_ms, ruta_wav), ...]} ordenados."""
    out = {}
    # Formato actual: un índice por carpeta de sesión; anterior: uno solo en audio/ o en la raíz
    indexes = sorted(glob.glob(os.path.join(folder, "sesion_*", "indice_audio.csv")))
    for path in (os.path.join(folder, "audio", "indice_audio.csv"), os.path.join(folder, "indice_audio.csv")):
        if os.path.exists(path):
            indexes.append(path)
            break
    seen = set()
    for path in indexes:
        with open(path, encoding="utf-8", newline="") as f:
            for r in csv.DictReader(f):
                wav = os.path.join(os.path.dirname(path), r["archivo_wav"])
                if os.path.exists(wav) and r["archivo_wav"] not in seen:
                    seen.add(r["archivo_wav"])
                    out.setdefault(r["sesion"], []).append((int(r["t_inicio_audio"]), wav))
    for segs in out.values():
        segs.sort()
    return out


def wav_duration(path):
    with wave.open(path, "rb") as w:
        return w.getnframes() / w.getframerate()


def locate_in_audio(segments, begin_ms, end_ms):
    """(ruta_wav, inicio_s, fin_s) del enunciado dentro de su tramo, o None si no se grabó."""
    for start, wav in reversed(segments):
        if start <= begin_ms:
            b, e = (begin_ms - start) / 1000, (end_ms - start) / 1000
            return (wav, b, e) if b <= wav_duration(wav) else None
    return None


def cut_clip(wav_path, begin_s, end_s, out_path, margin_s=0.4):
    with wave.open(wav_path, "rb") as src:
        rate = src.getframerate()
        a = max(0, int((begin_s - margin_s) * rate))
        b = min(src.getnframes(), int((end_s + margin_s) * rate))
        src.setpos(a)
        frames = src.readframes(max(0, b - a))
        params = src.getparams()
    with wave.open(out_path, "wb") as dst:
        dst.setparams(params)
        dst.writeframes(frames)


def write_templates(folder, refs_dir, cut=False):
    os.makedirs(refs_dir, exist_ok=True)
    audio = load_audio_index(folder)
    clips_dir = os.path.join(refs_dir, "clips")
    for s in load_sessions(folder):
        path = os.path.join(refs_dir, s["name"] + ".txt")
        rows = s["rows"]
        segments = audio.get(s["name"], [])
        located = {}
        for r in rows:
            b, e = r.get("t_inicio_voz"), r.get("t_fin_voz")
            if segments and b and e:
                loc = locate_in_audio(segments, int(b), int(e))
                if loc:
                    located[r["idx"]] = loc
        if cut and located:
            os.makedirs(clips_dir, exist_ok=True)
            for idx, (wav, b, e) in located.items():
                cut_clip(wav, b, e, os.path.join(clips_dir, f"{s['name']}_{int(idx):03d}.wav"))
            print(f"{len(located)} clips en {clips_dir} ({s['name']})")
        if os.path.exists(path):
            print(f"Ya existe (no se sobrescribe): {path}")
            continue
        t0 = next((int(r["t_inicio_voz"]) for r in rows if r.get("t_inicio_voz")), None)
        with open(path, "w", encoding="utf-8") as f:
            f.write("# Transcripción de REFERENCIA. Escuche el audio y escriba lo que DIJO el docente, palabra por palabra.\n")
            f.write("# No mire la pantalla de la app ni el CSV. Escriba después de la barra '|'. Si no se dijo nada: '-'.\n")
            if located:
                f.write("# Ventana = archivo WAV de la app y segundos dentro de él (grabación de la propia app).\n")
                f.write(f"# {len(located)}/{len(rows)} enunciados tienen audio grabado por la app.\n")
            else:
                f.write("# Ventana = segundos desde el inicio de la primera frase (aprox.; ajuste con la frase de sincronización).\n")
            for r in rows:
                b, e = r.get("t_inicio_voz"), r.get("t_fin_voz")
                if r["idx"] in located:
                    wav, sb, se = located[r["idx"]]
                    win = f"{os.path.basename(wav)} {sb:.1f}–{se:.1f} s"
                else:
                    win = f"{(int(b) - t0) / 1000:.1f}–{(int(e) - t0) / 1000:.1f} s" if (t0 is not None and b and e) else "s/d"
                    if located:
                        win += " desde la 1.ª frase, sin audio de la app"
                f.write(f"# {r['idx']} [{r['hora']}, {win}]\n{int(r['idx']):03d} | \n")
        print(f"Plantilla creada: {path}")


def write_user_template(path):
    header = ["participante", "condicion", "orden", "tlx_1", "tlx_2", "tlx_3", "tlx_4", "tlx_5", "tlx_6",
              "observaciones"]
    with open(path, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerow(["E01", "T0", "1", "", "", "", "", "", "", ""])
        w.writerow(["E01", "T2", "2", "", "", "", "", "", "", "", "", "", "", "10", ""])
    print(f"Plantilla de instrumentos creada: {path} (tlx_1..6 = demanda mental, física, temporal, desempeño, "
          f"esfuerzo, frustración; 0-100)")


# ----------------------------------------------------------------------------- principal


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("carpeta", help="carpeta descargada del equipo (Documents/PrototipoFuzzy/: una subcarpeta por sesión; también el formato anterior)")
    ap.add_argument("--referencias", help="carpeta de referencias (por defecto <carpeta>/referencias)")
    ap.add_argument("--plantilla", action="store_true", help="crea las plantillas de referencia y termina")
    ap.add_argument("--recortar", action="store_true",
                    help="con --plantilla: corta un WAV por enunciado (grabación de la app) en referencias/clips/")
    ap.add_argument("--plantilla-usuarios", action="store_true", help="crea usuarios.csv de ejemplo y termina")
    ap.add_argument("--usuarios", help="CSV con el NASA-TLX en papel (opcional; la app ya guarda nasa_tlx_respuestas.csv)")
    ap.add_argument("--terminos-clave", help="archivo .txt con la lista cerrada de términos clave (uno por línea), "
                    "revisada por el docente; por defecto, todos los términos del diccionario")
    ap.add_argument("--etiquetas-dev", default="dev,piloto",
                    help="etiquetas de sesión que forman el corpus de DESARROLLO (ajuste de T1); el resto es PRUEBA")
    ap.add_argument("--salida", help="reporte Markdown (por defecto <carpeta>/reporte_evaluacion.md)")
    args = ap.parse_args()

    folder = args.carpeta
    refs_dir = args.referencias or os.path.join(folder, "referencias")
    if args.plantilla:
        return write_templates(folder, refs_dir, cut=args.recortar)
    if args.plantilla_usuarios:
        return write_user_template(os.path.join(folder, "usuarios.csv"))

    sessions = load_sessions(folder)
    if not sessions:
        sys.exit(f"No hay archivos sesion_*.csv en {folder}")
    dictionaries = load_dictionaries()
    keywords = {k: set(v) for k, v in dictionaries.items()}
    keyword_source = "todos los términos del diccionario"
    if args.terminos_clave:
        with open(args.terminos_clave, encoding="utf-8") as f:
            custom = {ln.strip() for ln in f if ln.strip() and not ln.lstrip().startswith("#")}
        keywords = {k: custom for k in keywords}  # una sola área curricular: la lista revisada sustituye al diccionario
        keyword_source = f"lista revisada {os.path.basename(args.terminos_clave)} ({len(custom)} términos)"
    dev_labels = {x.strip().lower() for x in args.etiquetas_dev.split(",") if x.strip()}

    all_rows = [r for s in sessions for r in s["rows"]]
    dev_units, test_units, no_ref = [], [], []
    for s in sessions:
        refs = read_reference(s, refs_dir)
        if refs is None:
            no_ref.append(s["name"])
            continue
        is_dev = s["rows"][0].get("etiqueta", "").strip().lower() in dev_labels
        for r, ref in zip(s["rows"], refs):
            if not tokens(ref) and not tokens(r["texto_crudo"]):
                continue
            unit = {"session": s["name"], "idx": r["idx"], "subject": r["asignatura"], "ref": ref,
                    "T0": r["texto_crudo"], "T2": r["texto_corregido"], "conf": word_confidences(r)}
            (dev_units if is_dev else test_units).append(unit)

    # --- Ajuste de la línea base nítida T1 SOLO con el corpus de desarrollo
    use_conf = any(u["conf"] is not None for u in dev_units + test_units)
    grid = [(td, tc) for td in (0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90)
            for tc in ((0.5, 0.6, 0.7, 0.8, 1.0) if use_conf else (1.0,))]
    tuning = dev_units or test_units  # si no hay desarrollo, se avisa en el reporte
    best = None
    for td, tc in grid:
        for u in tuning:
            u["T1"] = crisp_correct(u["T0"], dictionaries.get(u["subject"], []), td, u["conf"], tc)
        w = corpus_wer(tuning, "T1")
        if w is not None and (best is None or w < best[0] - 1e-12):
            best = (w, td, tc)
    _, t_d, t_c = best if best else (None, 0.75, 1.0)
    for u in dev_units + test_units:
        u["T1"] = crisp_correct(u["T0"], dictionaries.get(u["subject"], []), t_d, u["conf"], t_c)

    rep = ["# Reporte de evaluación con voz real", "",
           f"- Sesiones: {len(sessions)} (sin referencia: {len(no_ref)}). Enunciados con referencia: "
           f"desarrollo {len(dev_units)}, prueba {len(test_units)}.",
           f"- Etiquetas de desarrollo: {', '.join(sorted(dev_labels))}.",
           f"- Línea base nítida T1: D ≥ {t_d:.2f}" + (f" y C ≤ {t_c:.2f}" if use_conf else " (sin confianza informativa)") +
           (" — ajustada con el corpus de DESARROLLO." if dev_units else
            " — **ADVERTENCIA: no hay corpus de desarrollo; T1 se ajustó con el de prueba (favorece a T1).**"),
           "- Normalización: minúsculas, sin tildes ni puntuación (igual para referencia e hipótesis).",
           f"- Términos clave (KER): {keyword_source}. KER = (N_k − A_k) / N_k, con N_k = apariciones de términos clave en la "
           "referencia y A_k = cuántas aparecen también en la transcripción del mismo enunciado.", ""]
    if no_ref:
        rep += ["Sesiones sin referencia (solo cuentan para latencia y confianza): " + ", ".join(no_ref), ""]

    detail_rows, false_rows = [], []
    for name, units in (("PRUEBA", test_units), ("DESARROLLO", dev_units)):
        if not units:
            continue
        m = evaluate(units, keywords, name)
        rep += [f"## {'1' if name == 'PRUEBA' else '2'}. Precisión textual — corpus de {name} ({len(units)} enunciados, "
                f"{m['T0']['N']} palabras de referencia)", "",
                "| Condición | WER | CER | KER | Precisión semántica | Sustituciones | Aciertos | Falsas corr. | Inútiles | TCR | FCR | Precisión de la sustitución |",
                "|---|---|---|---|---|---|---|---|---|---|---|---|"]
        for cond, title in (("T0", "T0 ASR crudo"), ("T1", "T1 Diccionario + umbral"), ("T2", "T2 Capa difusa")):
            x = m[cond]
            kws = "—" if math.isnan(x["KER"]) else pct(x["KER"])
            ps = "—" if math.isnan(x["KER"]) else pct(1 - x["KER"])
            if cond == "T0":
                rep.append(f"| {title} | {pct(x['WER'])} | {pct(x['CER'])} | {kws} | {ps} | — | — | — | — | — | — | — |")
            else:
                f = lambda v: "—" if math.isnan(v) else pct(v)  # noqa: E731
                rep.append(f"| {title} | {pct(x['WER'])} | {pct(x['CER'])} | {kws} | {ps} | {x['sust']} | {x['aciertos']} | "
                           f"{x['falsas']} | {x['inutiles']} | {f(x['TCR'])} | {f(x['FCR'])} | {f(x['PREC_SUST'])} |")
        w0, w2 = m["T0"]["WER"], m["T2"]["WER"]
        rr = (w0 - w2) / w0 if w0 else float("nan")
        lo, hi = bootstrap_ci(units, lambda s: corpus_wer(s, "T2"))
        rr_lo, rr_hi = bootstrap_ci(
            units, lambda s: (lambda a, b: (a - b) / a if a else None)(corpus_wer(s, "T0"), corpus_wer(s, "T2")))
        rep += ["", f"- Reducción relativa del WER (T0→T2): **{pct(rr)}** (IC 95 % bootstrap {pct(rr_lo)} a {pct(rr_hi)}). "
                    f"WER T2: IC 95 % {pct(lo)} a {pct(hi)}.",
                f"- Palabras clave en la referencia: {m['T0']['kw_total']}.",
                f"- Enunciados omitidos en el análisis de sustituciones (cambió el n.º de palabras): {m['T2']['omitidos']}.", ""]
        tests, labels = [], []
        for a, b in (("T0", "T2"), ("T1", "T2")):
            diffs = [utt_wer(u, b) - utt_wer(u, a) for u in units if tokens(u["ref"])]
            tests.append(wilcoxon_signed_rank(diffs))
            labels.append(f"{a} vs {b}")
        adj = holm([t[2] for t in tests])
        rep += ["| Comparación (WER por enunciado) | n (≠0) | W+ | p | p Holm | r |", "|---|---|---|---|---|---|"]
        for lab, t, pa in zip(labels, tests, adj):
            rep.append(f"| {lab} | {t[0]} | {t[1]:.1f} | {t[2]:.4f} | {pa:.4f} | {t[4]:.2f} |")
        rep += ["", "W+ suma los rangos de diferencias positivas (WER_b − WER_a > 0); W+ pequeño indica que b reduce el error.", ""]

        for u in units:
            a = substitution_analysis(u["T0"], u["T2"], u["ref"])
            for kind, rw, cw, gold in (a["detalle"] if a else []):
                if kind != "acierto":
                    false_rows.append([name, u["session"], u["idx"], kind, rw, cw, gold])
            detail_rows.append([name, u["session"], u["idx"], u["subject"], u["ref"], u["T0"], u["T1"], u["T2"],
                                fmt_num(utt_wer(u, "T0"), 4), fmt_num(utt_wer(u, "T1"), 4), fmt_num(utt_wer(u, "T2"), 4)])

    rep += latency_section(all_rows) + confidence_section(all_rows) + users_section(args.usuarios, folder)

    out_md = args.salida or os.path.join(folder, "reporte_evaluacion.md")
    with open(out_md, "w", encoding="utf-8") as f:
        f.write("\n".join(rep) + "\n")
    with open(os.path.join(folder, "detalle_enunciados.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["corpus", "sesion", "idx", "asignatura", "referencia", "T0", "T1", "T2", "wer_T0", "wer_T1", "wer_T2"])
        w.writerows(detail_rows)
    with open(os.path.join(folder, "sustituciones_erroneas.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["corpus", "sesion", "idx", "tipo", "palabra_asr", "palabra_sistema", "palabra_referencia"])
        w.writerows(false_rows)
    print("\n".join(rep))
    print(f"\nReporte: {out_md}\nDetalle: detalle_enunciados.csv, sustituciones_erroneas.csv (en {folder})")


if __name__ == "__main__":
    main()
