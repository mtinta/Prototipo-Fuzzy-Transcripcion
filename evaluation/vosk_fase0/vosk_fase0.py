#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
FASE 0: ¿La confianza acústica C de Vosk sirve para el motor difuso?
Tesis: Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De
       Lógica Difusa ... (UNSA)
=============================================================================

Contexto: con el ASR local de Android, CONFIDENCE_SCORES llega siempre en 0.0 y la app usa
C = 0.65 fija (258/258 enunciados del piloto). Este script comprueba, EN LA PC y antes de tocar
la app, si Vosk entrega una C por palabra que:

  V1. SEPARE EN CONJUNTOS: que las palabras mal reconocidas salgan de "Alta" (C < 0.85).
      Que las correctas se concentren en "Alta" es lo esperado, no un problema.
  V2. INFORME: sea más baja en las palabras mal reconocidas que en las correctas (AUROC).
  V3. IMPORTE: cambie las decisiones de la capa difusa frente a C = 0.65 fija
      (se reutiliza benchmark_runner.py, el espejo Python verificado del motor Kotlin).

La decodificación de Vosk es determinista: el mismo audio y el mismo modelo dan las mismas
confianzas en la PC y en Android, así que esta medición es representativa del teléfono
(salvo la latencia, que aquí NO se evalúa).

Uso:
  python vosk_fase0.py audios/toma1.m4a --modelo modelos/vosk-model-small-es-0.42 --guion
  python vosk_fase0.py audios/*.wav --modelo modelos/vosk-model-small-es-0.42 --guion --bloques 1,2

Referencia (para V2, V3 y el WER), por archivo y en este orden de prioridad:
  1. <audio>.txt junto al audio (transcripción humana de ESA grabación).
  2. --guion: las frases de evaluation/guion_docente_biologia.md (todas o las de --bloques).
  Sin referencia solo se describe la distribución de C (sin veredicto).

Salidas (en --salida, por defecto ./resultados/<fecha_hora>/):
  palabras.csv        una fila por palabra: C, tiempos, pertenencias, etiqueta correcta/error
  corpus_vosk.json    salida de Vosk en el formato de corpus de benchmark_runner.py
  reporte_fase0.md    resumen con el veredicto
"""

import argparse
import contextlib
import csv
import io
import json
import math
import os
import re
import shutil
import statistics
import subprocess
import sys
import time
from datetime import datetime

AQUI = os.path.dirname(os.path.abspath(__file__))
EVALUACION = os.path.dirname(AQUI)
sys.path.insert(0, EVALUACION)
from benchmark_runner import (  # noqa: E402
    is_candidate_noun, normalize_text, run_benchmarks, trapezoid, triangle,
)

GUION_MD = os.path.join(EVALUACION, "guion_docente_biologia.md")
SAMPLE_RATE = 16000
CHUNK_BYTES = 4000  # 0.125 s de PCM 16 bits mono, como un búfer típico de AudioRecord
C_FIJA_ANDROID = 0.65  # = AndroidSpeechEngine.NEUTRAL_CONFIDENCE

# Criterios de decisión PROPUESTOS (ajustables; documentar en la tesis los que se usen)
MIN_ERRORES_FUERA_DE_ALTA = 0.40  # V1: al menos 40 % de palabras erróneas con C < 0.85 (μ_Alta < 1)
MIN_AUROC = 0.65               # V2: C separa errores de aciertos mejor que el azar (0.5)
MIN_ERRORES_PARA_AUROC = 10    # V2: con menos errores el AUROC no es fiable
WER_AVISO = 0.35               # aviso: línea base demasiado mala para subtitular


# -----------------------------------------------------------------------------
# Funciones de pertenencia de C (idénticas a fuzzy_rules.fcl / benchmark_runner.py)
# -----------------------------------------------------------------------------
def pertenencias_c(c):
    return (
        trapezoid(c, 0.0, 0.0, 0.35, 0.60),   # Baja
        triangle(c, 0.35, 0.60, 0.80),        # Media
        trapezoid(c, 0.65, 0.85, 1.0, 1.0),   # Alta
    )


ETIQUETAS_C = ("Baja", "Media", "Alta")


# -----------------------------------------------------------------------------
# Audio y Vosk
# -----------------------------------------------------------------------------
def leer_pcm(ruta):
    """Convierte cualquier formato que entienda ffmpeg a PCM 16 kHz, mono, 16 bits."""
    if not shutil.which("ffmpeg"):
        sys.exit("ERROR: falta ffmpeg en el PATH (se usa para leer m4a/ogg/mp3/wav).")
    proc = subprocess.run(
        ["ffmpeg", "-nostdin", "-loglevel", "error", "-i", ruta,
         "-ar", str(SAMPLE_RATE), "-ac", "1", "-f", "s16le", "-"],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
    )
    if proc.returncode != 0:
        sys.exit(f"ERROR: ffmpeg no pudo leer {ruta}: {proc.stderr.decode(errors='replace')}")
    return proc.stdout


def transcribir(modelo, ruta):
    """Alimenta el audio en trozos (como en streaming) y devuelve los enunciados con C por palabra."""
    from vosk import KaldiRecognizer

    pcm = leer_pcm(ruta)
    rec = KaldiRecognizer(modelo, SAMPLE_RATE)
    rec.SetWords(True)

    enunciados = []

    def recoger(js):
        data = json.loads(js)
        palabras = data.get("result") or []
        if palabras:
            enunciados.append([
                {"palabra": p["word"], "conf": float(p["conf"]),
                 "inicio": float(p["start"]), "fin": float(p["end"])}
                for p in palabras
            ])

    t0 = time.perf_counter()
    for i in range(0, len(pcm), CHUNK_BYTES):
        if rec.AcceptWaveform(pcm[i:i + CHUNK_BYTES]):
            recoger(rec.Result())
    recoger(rec.FinalResult())
    segundos_proceso = time.perf_counter() - t0
    segundos_audio = len(pcm) / (2 * SAMPLE_RATE)
    return enunciados, segundos_audio, segundos_proceso


# -----------------------------------------------------------------------------
# Referencias
# -----------------------------------------------------------------------------
def frases_guion(bloques=None):
    """Frases entre comillas de los bloques del guion (líneas '> ...' bajo '### Bloque N')."""
    with open(GUION_MD, encoding="utf-8") as f:
        lineas = f.read().splitlines()
    frases, bloque = [], None
    for linea in lineas:
        m = re.match(r"^###\s+Bloque\s+(\d+)", linea)
        if m:
            bloque = int(m.group(1))
            continue
        if linea.startswith("## "):
            bloque = None
        if bloque is not None and linea.startswith(">"):
            texto = linea.lstrip("> ").strip().strip('"“”')
            if texto and (bloques is None or bloque in bloques):
                frases.append(texto)
    return frases


def referencia_para(ruta_audio, usar_guion, bloques):
    txt = os.path.splitext(ruta_audio)[0] + ".txt"
    if os.path.exists(txt):
        with open(txt, encoding="utf-8") as f:
            return f.read(), os.path.basename(txt)
    if usar_guion:
        return " ".join(frases_guion(bloques)), "guion" + (f" (bloques {sorted(bloques)})" if bloques else "")
    return None, None


def tokens(texto):
    return normalize_text(texto).split()


def token_hyp(palabra):
    return normalize_text(palabra).replace(" ", "")


# -----------------------------------------------------------------------------
# Alineamiento Levenshtein con traza (para etiquetar cada palabra del ASR)
# -----------------------------------------------------------------------------
def alinear(ref, hyp):
    """Devuelve operaciones (tipo, i_ref, j_hyp) con tipo en ok/sub/del/ins."""
    n, m = len(ref), len(hyp)
    dp = [[0] * (m + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        dp[i][0] = i
    for j in range(m + 1):
        dp[0][j] = j
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            costo = 0 if ref[i - 1] == hyp[j - 1] else 1
            dp[i][j] = min(dp[i - 1][j - 1] + costo, dp[i - 1][j] + 1, dp[i][j - 1] + 1)
    ops, i, j = [], n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0 and dp[i][j] == dp[i - 1][j - 1] + (0 if ref[i - 1] == hyp[j - 1] else 1):
            ops.append(("ok" if ref[i - 1] == hyp[j - 1] else "sub", i - 1, j - 1))
            i, j = i - 1, j - 1
        elif i > 0 and dp[i][j] == dp[i - 1][j] + 1:
            ops.append(("del", i - 1, None))
            i -= 1
        else:
            ops.append(("ins", None, j - 1))
            j -= 1
    ops.reverse()
    return ops


# -----------------------------------------------------------------------------
# Estadística (solo biblioteca estándar)
# -----------------------------------------------------------------------------
def percentil(valores, p):
    if not valores:
        return float("nan")
    v = sorted(valores)
    k = (len(v) - 1) * p
    lo, hi = math.floor(k), math.ceil(k)
    return v[lo] + (v[hi] - v[lo]) * (k - lo)


def auroc(conf_correctas, conf_errores):
    """P(C de una palabra correcta > C de una errónea); empates cuentan 0.5 (Mann-Whitney)."""
    if not conf_correctas or not conf_errores:
        return float("nan")
    todos = sorted([(c, 1) for c in conf_correctas] + [(c, 0) for c in conf_errores])
    rangos, i = [0.0] * len(todos), 0
    while i < len(todos):
        j = i
        while j + 1 < len(todos) and todos[j + 1][0] == todos[i][0]:
            j += 1
        for k in range(i, j + 1):
            rangos[k] = (i + j) / 2 + 1
        i = j + 1
    suma = sum(r for r, (_, es_ok) in zip(rangos, todos) if es_ok)
    n1, n0 = len(conf_correctas), len(conf_errores)
    return (suma - n1 * (n1 + 1) / 2) / (n1 * n0)


def histograma(valores, bins=10, ancho=40):
    if not valores:
        return "(sin datos)"
    cuentas = [0] * bins
    for v in valores:
        cuentas[min(int(v * bins), bins - 1)] += 1
    mx = max(cuentas) or 1
    filas = []
    for b, c in enumerate(cuentas):
        barra = "█" * round(c / mx * ancho)
        filas.append(f"{b / bins:.1f}–{(b + 1) / bins:.1f} | {barra} {c} ({c / len(valores):.0%})")
    return "\n".join(filas)


def resumen(valores):
    if not valores:
        return {}
    return {
        "n": len(valores),
        "media": statistics.fmean(valores),
        "desv": statistics.pstdev(valores),
        "min": min(valores), "p10": percentil(valores, 0.10), "p25": percentil(valores, 0.25),
        "p50": percentil(valores, 0.50), "p75": percentil(valores, 0.75), "p90": percentil(valores, 0.90),
        "max": max(valores),
        "frac_1": sum(1 for v in valores if v >= 0.999) / len(valores),
        "frac_fuera_alta": sum(1 for v in valores if v < 0.85) / len(valores),
    }


# -----------------------------------------------------------------------------
# Programa principal
# -----------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description="Fase 0: distribución y utilidad de la confianza C de Vosk.")
    ap.add_argument("audios", nargs="+", help="grabaciones (wav, m4a, ogg, mp3...)")
    ap.add_argument("--modelo", required=True, help="carpeta del modelo Vosk descomprimido")
    ap.add_argument("--guion", action="store_true", help="usar el guion de biología como referencia")
    ap.add_argument("--bloques", help="bloques del guion leídos, p. ej. 1,2 (por defecto, los 4)")
    ap.add_argument("--salida", help="carpeta de resultados (por defecto resultados/<fecha_hora>)")
    args = ap.parse_args()

    try:
        from vosk import Model, SetLogLevel
    except ImportError:
        sys.exit("ERROR: falta el paquete vosk. Ver README.md (pip install -r requirements.txt).")
    if not os.path.isdir(args.modelo):
        sys.exit(f"ERROR: no existe la carpeta del modelo: {args.modelo}")
    # Las referencias <audio>.txt pueden estar en la misma carpeta: no son audio
    args.audios = [a for a in args.audios if not a.lower().endswith((".txt", ".md", ".csv", ".json"))]
    if not args.audios:
        sys.exit("ERROR: no se pasó ningún archivo de audio.")
    bloques = {int(b) for b in args.bloques.split(",")} if args.bloques else None
    salida = os.path.abspath(args.salida or os.path.join(AQUI, "resultados", datetime.now().strftime("%Y%m%d_%H%M%S")))
    os.makedirs(salida, exist_ok=True)

    SetLogLevel(-1)
    print(f"Cargando modelo {args.modelo} ...")
    modelo = Model(args.modelo)

    filas, corpus, info_archivos = [], [], []
    for n_archivo, ruta in enumerate(args.audios):
        print(f"Transcribiendo {ruta} ...")
        enunciados, seg_audio, seg_proc = transcribir(modelo, ruta)
        ref_texto, ref_origen = referencia_para(ruta, args.guion, bloques)
        hyp = [(e, k, p) for e, enun in enumerate(enunciados) for k, p in enumerate(enun)]

        etiqueta = {}
        ref_por_enunciado = {e: [] for e in range(len(enunciados))}
        errores_archivo = n_ref = None
        if ref_texto:
            ref = tokens(ref_texto)
            ops = alinear(ref, [token_hyp(p["palabra"]) for _, _, p in hyp])
            ultimo = 0
            for tipo, i, j in ops:
                if j is not None:
                    etiqueta[j] = tipo
                    ultimo = hyp[j][0]
                if i is not None and enunciados:
                    ref_por_enunciado[ultimo].append(ref[i])
            errores_archivo = sum(1 for t, _, _ in ops if t != "ok")
            n_ref = len(ref)

        for j, (e, k, p) in enumerate(hyp):
            mu = pertenencias_c(p["conf"])
            filas.append({
                "archivo": os.path.basename(ruta), "enunciado": e + 1, "pos": k + 1,
                "palabra": p["palabra"], "conf": round(p["conf"], 4),
                "inicio_s": round(p["inicio"], 2), "fin_s": round(p["fin"], 2),
                "candidata": int(is_candidate_noun(p["palabra"])),
                "mu_baja": round(mu[0], 3), "mu_media": round(mu[1], 3), "mu_alta": round(mu[2], 3),
                "conjunto_dominante": ETIQUETAS_C[max(range(3), key=lambda x: mu[x])],
                "etiqueta": etiqueta.get(j, "NA"),
            })

        prev_inicio = None
        for e, enun in enumerate(enunciados):
            inicio = enun[0]["inicio"]
            gap = 600 if (prev_inicio is None and n_archivo > 0) else (8 if prev_inicio is None else max(1, round(inicio - prev_inicio)))
            prev_inicio = inicio
            if ref_texto:
                corpus.append({
                    "id": len(corpus) + 1, "subject": "BIOLOGIA",
                    "ground_truth": " ".join(ref_por_enunciado[e]),
                    "raw_asr": " ".join(p["palabra"] for p in enun),
                    "word_confidences": [round(p["conf"], 4) for p in enun],
                    "gap_s": gap, "nota": f"Vosk fase 0: {os.path.basename(ruta)} enunciado {e + 1}",
                })

        info_archivos.append({
            "archivo": os.path.basename(ruta), "seg_audio": seg_audio, "seg_proc": seg_proc,
            "enunciados": len(enunciados), "palabras": len(hyp), "referencia": ref_origen,
            "wer": (errores_archivo / n_ref) if n_ref else None,
        })

    # ---- Archivos de salida ----
    csv_path = os.path.join(salida, "palabras.csv")
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        if filas:
            w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
            w.writeheader()
            w.writerows(filas)
    corpus_path = os.path.join(salida, "corpus_vosk.json")
    with open(corpus_path, "w", encoding="utf-8") as f:
        json.dump(corpus, f, ensure_ascii=False, indent=1)

    # ---- Análisis ----
    conf_todas = [r["conf"] for r in filas]
    conf_cand = [r["conf"] for r in filas if r["candidata"]]
    est_todas, est_cand = resumen(conf_todas), resumen(conf_cand)

    con_ref = [r for r in filas if r["etiqueta"] != "NA"]
    ok = [r["conf"] for r in con_ref if r["etiqueta"] == "ok"]
    err = [r["conf"] for r in con_ref if r["etiqueta"] in ("sub", "ins")]
    ok_c = [r["conf"] for r in con_ref if r["candidata"] and r["etiqueta"] == "ok"]
    err_c = [r["conf"] for r in con_ref if r["candidata"] and r["etiqueta"] in ("sub", "ins")]
    auc, auc_c = auroc(ok, err), auroc(ok_c, err_c)

    dominante = {k: sum(1 for r in filas if r["candidata"] and r["conjunto_dominante"] == k) for k in ETIQUETAS_C}

    calibracion = []
    for b in range(10):
        lo, hi = b / 10, (b + 1) / 10
        en_bin = [r for r in con_ref if lo <= r["conf"] < hi or (b == 9 and r["conf"] == 1.0)]
        if en_bin:
            calibracion.append((lo, hi, len(en_bin), statistics.fmean(r["conf"] for r in en_bin),
                                sum(1 for r in en_bin if r["etiqueta"] == "ok") / len(en_bin)))

    sim = None
    if corpus:
        with contextlib.redirect_stdout(io.StringIO()):
            real = run_benchmarks(corpus_name=corpus_path, write_report=False)
            fija = run_benchmarks(corpus_name=corpus_path, write_report=False, fixed_conf=C_FIJA_ANDROID)
        sim = {"real": real, "fija": fija}

    # ---- Veredicto ----
    def fuera_alta(v):
        return sum(1 for c in v if c < 0.85) / len(v) if v else None
    err_fuera, ok_fuera = fuera_alta(err), fuera_alta(ok)
    err_c_fuera, ok_c_fuera = fuera_alta(err_c), fuera_alta(ok_c)
    v1 = None if (not err or len(err) < MIN_ERRORES_PARA_AUROC) else err_fuera >= MIN_ERRORES_FUERA_DE_ALTA
    v2 = None if (not err or len(err) < MIN_ERRORES_PARA_AUROC) else auc >= MIN_AUROC
    v3 = None if sim is None else (sim["real"]["subs"] != sim["fija"]["subs"] or abs(sim["real"]["wer_fuzzy"] - sim["fija"]["wer_fuzzy"]) > 1e-9)
    wers = [i["wer"] for i in info_archivos if i["wer"] is not None]
    wer_medio = statistics.fmean(wers) if wers else None

    if v2 is None:
        veredicto = "INCOMPLETO: falta referencia o hay muy pocos errores para evaluar si C informa (V2)."
    elif v2 and v1:
        veredicto = "CONTINUAR: C varía y separa errores de aciertos. Vale la pena integrar Vosk (fase 1)."
    elif v2 and not v1:
        veredicto = ("CONTINUAR CON RECALIBRACIÓN: C informa, pero las palabras erróneas siguen cayendo en 'Alta'. "
                     "Habría que mover las funciones de pertenencia de C según la tabla de calibración.")
    else:
        veredicto = "NO CONTINUAR (con este modelo): C no distingue palabras erróneas de correctas."

    def f(x, d=3):
        return "NA" if x is None or (isinstance(x, float) and math.isnan(x)) else f"{x:.{d}f}"

    def si_no(v):
        return "—" if v is None else ("CUMPLE" if v else "NO CUMPLE")

    rep = []
    rep.append("# Fase 0 — Confianza acústica C con Vosk\n")
    rep.append(f"- Fecha: {datetime.now():%Y-%m-%d %H:%M}")
    rep.append(f"- Modelo: `{os.path.basename(os.path.normpath(args.modelo))}`")
    rep.append(f"- Archivos: {len(info_archivos)} · palabras: {len(filas)} · candidatas (pasan al motor difuso): {len(conf_cand)}\n")
    rep.append(f"## Veredicto\n\n**{veredicto}**\n")
    rep.append("| Criterio | Umbral propuesto | Resultado | Estado |\n|---|---|---|---|")
    def pct(x):
        return "NA" if x is None else f"{x:.0%}"
    rep.append(f"| V1 separa: palabras erróneas con C < 0.85 (fuera de Alta) | ≥ {MIN_ERRORES_FUERA_DE_ALTA:.0%} | {pct(err_fuera)} (correctas: {pct(ok_fuera)}) | {si_no(v1)} |")
    rep.append(f"| V1 separa: solo candidatas | informativo | {pct(err_c_fuera)} (correctas: {pct(ok_c_fuera)}) | |")
    rep.append(f"| V2 informa: AUROC (C correctas vs. erróneas, todas las palabras) | ≥ {MIN_AUROC} | {f(auc)} (errores = {len(err)}) | {si_no(v2)} |")
    rep.append(f"| V2 informa: AUROC solo candidatas | informativo | {f(auc_c)} (errores = {len(err_c)}) | |")
    rep.append(f"| V3 importa: la C real cambia las decisiones frente a C = {C_FIJA_ANDROID} | informativo | "
               f"{'sí' if v3 else ('no' if v3 is not None else 'NA')} | |")
    rep.append(f"| WER medio de Vosk (sin corrección) | aviso si > {WER_AVISO:.0%} | {f(wer_medio)} | "
               f"{'—' if wer_medio is None else ('AVISO' if wer_medio > WER_AVISO else 'OK')} |\n")

    rep.append("## Distribución de C\n")
    rep.append("| Conjunto | n | media | desv | mín | p10 | p25 | p50 | p75 | p90 | máx | % = 1.0 |\n|---|---|---|---|---|---|---|---|---|---|---|---|")
    for nombre, e in (("Todas las palabras", est_todas), ("Candidatas", est_cand)):
        if e:
            rep.append(f"| {nombre} | {e['n']} | {f(e['media'])} | {f(e['desv'])} | {f(e['min'])} | {f(e['p10'])} | {f(e['p25'])} | "
                       f"{f(e['p50'])} | {f(e['p75'])} | {f(e['p90'])} | {f(e['max'])} | {e['frac_1']:.0%} |")
    rep.append("\nHistograma (candidatas):\n\n```\n" + histograma(conf_cand) + "\n```\n")
    rep.append("Conjunto difuso dominante de C en las candidatas (con el FCL actual):\n")
    rep.append("| Baja | Media | Alta |\n|---|---|---|")
    tot = sum(dominante.values()) or 1
    rep.append(" | ".join(f"{dominante[k]} ({dominante[k] / tot:.0%})" for k in ETIQUETAS_C).join(["| ", " |"]) + "\n")
    rep.append("Con Android, el 100 % de las palabras caía en Media (C = 0.65): solo se activaban las reglas 10–18.\n")

    if con_ref:
        rep.append("## ¿C informa sobre los errores?\n")
        rep.append(f"- C media en palabras correctas: {f(statistics.fmean(ok)) if ok else 'NA'} (n = {len(ok)})")
        rep.append(f"- C media en palabras erróneas (sustituidas o insertadas): {f(statistics.fmean(err)) if err else 'NA'} (n = {len(err)})")
        rep.append("- AUROC = probabilidad de que una palabra correcta tenga más C que una errónea (0.5 = azar, 1.0 = separación perfecta).\n")
        rep.append("Calibración (¿una C de 0.6 acierta ~60 % de las veces?):\n")
        rep.append("| Rango de C | n | C media | % correctas |\n|---|---|---|---|")
        for lo, hi, n, cm, acc in calibracion:
            rep.append(f"| {lo:.1f}–{hi:.1f} | {n} | {cm:.3f} | {acc:.0%} |")
        rep.append("")

    if sim:
        r, fj = sim["real"], sim["fija"]
        rep.append("## Simulación de la capa difusa (benchmark_runner.py sobre la salida de Vosk)\n")
        rep.append("| Métrica | C real de Vosk | C fija 0.65 (como Android) |\n|---|---|---|")
        rep.append(f"| WER crudo | {r['wer_raw']:.2f} % | {fj['wer_raw']:.2f} % |")
        rep.append(f"| WER con capa difusa | {r['wer_fuzzy']:.2f} % | {fj['wer_fuzzy']:.2f} % |")
        rep.append(f"| KER crudo | {r['ker_raw']:.2f} % | {fj['ker_raw']:.2f} % |")
        rep.append(f"| KER con capa difusa | {r['ker_fuzzy']:.2f} % | {fj['ker_fuzzy']:.2f} % |")
        rep.append(f"| Sustituciones realizadas | {r['subs']} | {fj['subs']} |\n")
        rep.append("Ojo: el alineamiento de la referencia con cada enunciado de Vosk es automático; "
                   "si se saltaron frases al leer, el WER sube por las omisiones.\n")

    rep.append("## Archivos\n")
    rep.append("| Archivo | Audio (s) | Proceso en PC (s) | Enunciados | Palabras | Referencia | WER |\n|---|---|---|---|---|---|---|")
    if len(info_archivos) > 20:  # corpus de muchos clips cortos: una fila resumen
        con_wer = [i for i in info_archivos if i["wer"] is not None]
        rep.append(f"| {len(info_archivos)} archivos | {sum(i['seg_audio'] for i in info_archivos):.1f} | "
                   f"{sum(i['seg_proc'] for i in info_archivos):.1f} | {sum(i['enunciados'] for i in info_archivos)} | "
                   f"{sum(i['palabras'] for i in info_archivos)} | {len(con_wer)} con referencia | "
                   f"{f(statistics.fmean(i['wer'] for i in con_wer)) if con_wer else 'NA'} (media por archivo) |")
    for i in (info_archivos if len(info_archivos) <= 20 else []):
        rep.append(f"| {i['archivo']} | {i['seg_audio']:.1f} | {i['seg_proc']:.1f} | {i['enunciados']} | {i['palabras']} | "
                   f"{i['referencia'] or 'sin referencia'} | {f(i['wer'])} |")
    rep.append("\nEl tiempo de proceso es de la PC, no del teléfono: la latencia se mide en la fase 2.\n")

    rep_path = os.path.join(salida, "reporte_fase0.md")
    with open(rep_path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(rep))

    print("\n" + "\n".join(rep[:rep.index("## Distribución de C\n")]))
    print(f"Reporte completo: {rep_path}\nPalabras: {csv_path}\nCorpus: {corpus_path}")


if __name__ == "__main__":
    main()
