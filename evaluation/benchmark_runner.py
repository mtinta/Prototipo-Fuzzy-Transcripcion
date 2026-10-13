#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
SISTEMA DE VALIDACIÓN EXPERIMENTAL Y BENCHMARKING DE WER
Tesis: Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De
       Lógica Difusa Para Transcripción Automática Como Apoyo A Estudiantes
       Sordos En Educación Secundaria.
Autor: Miguel Angel Tinta Aguilar - Universidad Nacional de San Agustín (UNSA)
=============================================================================
"""

import json
import os
import time
import unicodedata

# 1. Normalización y Métrica Fonética Hispana
def strip_accents(text: str) -> str:
    nfkd = unicodedata.normalize('NFD', text)
    return "".join([c for c in nfkd if not unicodedata.combining(c)])

def to_hispanic_phonetic(text: str) -> str:
    s = strip_accents(text.lower().strip())
    s = s.replace("h", "")
    s = s.replace("v", "b")
    s = s.replace("ce", "se").replace("ci", "si").replace("z", "s")
    s = s.replace("ll", "y")
    s = s.replace("qu", "k").replace("c", "k")
    return s

def levenshtein_distance(s1: str, s2: str) -> int:
    m, n = len(s1), len(s2)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1):
        dp[i][0] = i
    for j in range(n + 1):
        dp[0][j] = j
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            cost = 0 if s1[i - 1] == s2[j - 1] else 1
            dp[i][j] = min(
                dp[i - 1][j] + 1,       # Deletion
                dp[i][j - 1] + 1,       # Insertion
                dp[i - 1][j - 1] + cost # Substitution
            )
    return dp[m][n]

def calculate_similarity(s1: str, s2: str) -> float:
    c1, c2 = strip_accents(s1.lower()), strip_accents(s2.lower())
    if c1 == c2:
        return 1.0
    max_len = max(len(c1), len(c2))
    if max_len == 0:
        return 1.0
    lex_sim = 1.0 - (levenshtein_distance(c1, c2) / max_len)

    p1, p2 = to_hispanic_phonetic(s1), to_hispanic_phonetic(s2)
    max_phon = max(len(p1), len(p2))
    phon_sim = 1.0 if max_phon == 0 else (1.0 - levenshtein_distance(p1, p2) / max_phon)

    return max(0.0, min(1.0, 0.60 * lex_sim + 0.40 * phon_sim))

# 2. Funciones de Membresía e Inferencia Difusa Mamdani
def trapezoid(x, a, b, c, d):
    if x <= a or x >= d:
        return 0.0
    if b <= x <= c:
        return 1.0
    if a < x < b:
        return 1.0 if b == a else (x - a) / (b - a)
    if c < x < d:
        return 1.0 if d == c else (d - x) / (d - c)
    return 0.0

def triangle(x, a, b, c):
    if x <= a or x >= c:
        return 0.0
    if x == b:
        return 1.0
    if a < x < b:
        return (x - a) / (b - a)
    if b < x < c:
        return (c - x) / (c - b)
    return 0.0

def evaluate_mamdani(confianza: float, similitud: float) -> float:
    c = max(0.0, min(1.0, confianza))
    d = max(0.0, min(1.0, similitud))

    mu_c_baja = trapezoid(c, 0.0, 0.0, 0.35, 0.60)
    mu_c_media = triangle(c, 0.35, 0.60, 0.80)
    mu_c_alta = trapezoid(c, 0.65, 0.85, 1.0, 1.0)

    mu_s_baja = trapezoid(d, 0.0, 0.0, 0.40, 0.65)
    mu_s_media = triangle(d, 0.45, 0.70, 0.85)
    mu_s_alta = trapezoid(d, 0.75, 0.90, 1.0, 1.0)

    # Reglas Mamdani (MIN)
    r1 = min(mu_c_baja, mu_s_alta)
    r2 = min(mu_c_baja, mu_s_media)
    r3 = min(mu_c_baja, mu_s_baja)
    r4 = min(mu_c_media, mu_s_alta)
    r5 = min(mu_c_media, mu_s_media)
    r6 = min(mu_c_media, mu_s_baja)
    r7 = min(mu_c_alta, mu_s_alta)
    r8 = min(mu_c_alta, mu_s_media)
    r9 = min(mu_c_alta, mu_s_baja)

    activa_alta = max(r1, r2, r4)
    activa_media = max(r5, r7)
    activa_baja = max(r3, r6, r8, r9)

    # Defuzzificación Centroide COG
    steps = 100
    num = 0.0
    den = 0.0
    for i in range(steps + 1):
        y = i / steps
        mu_y_alta = min(activa_alta, trapezoid(y, 0.60, 0.80, 1.0, 1.0))
        mu_y_media = min(activa_media, triangle(y, 0.35, 0.55, 0.75))
        mu_y_baja = min(activa_baja, trapezoid(y, 0.0, 0.0, 0.30, 0.50))
        mu_comb = max(mu_y_alta, mu_y_media, mu_y_baja)
        num += y * mu_comb
        den += mu_comb

    return (num / den) if den > 0.0001 else 0.0

# 3. Cálculo Formal de WER: WER = (S + D + I) / N
def calculate_wer(hypothesis: str, reference: str) -> tuple:
    h = [w for w in hypothesis.strip().lower().split() if w]
    r = [w for w in reference.strip().lower().split() if w]
    n = len(r)
    if n == 0:
        return (0.0 if len(h) == 0 else 1.0, 0, 0, 0, 0)

    dp = [[0] * (len(h) + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        dp[i][0] = i
    for j in range(len(h) + 1):
        dp[0][j] = j

    for i in range(1, n + 1):
        for j in range(1, len(h) + 1):
            if strip_accents(r[i - 1]) == strip_accents(h[j - 1]):
                dp[i][j] = dp[i - 1][j - 1]
            else:
                dp[i][j] = min(
                    dp[i - 1][j - 1] + 1, # Substitution
                    dp[i - 1][j] + 1,     # Deletion
                    dp[i][j - 1] + 1      # Insertion
                )
    distance = dp[n][len(h)]
    wer = distance / n
    return (wer, distance, n)

def run_benchmarks():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    assets_dict_dir = os.path.join(base_dir, "..", "app", "src", "main", "assets", "dictionaries")
    corpus_file = os.path.join(base_dir, "corpus_arequipa_test.json")

    # Cargar Diccionarios MINEDU
    dictionaries = {}
    for sub in ["biologia", "quimica", "historia", "geografia"]:
        path = os.path.join(assets_dict_dir, f"{sub}.json")
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                dictionaries[sub.upper()] = json.load(f)

    # Cargar Corpus Escolar
    with open(corpus_file, "r", encoding="utf-8") as f:
        corpus = json.load(f)

    print("==========================================================================")
    print("      BANCO DE PRUEBAS EXPERIMENTAL: EVALUACIÓN DE PRECISIÓN WER")
    print("  Tesis UNSA - Sistema Speech-to-Text Asistido por Lógica Difusa On-Device")
    print("==========================================================================\n")

    total_ref_words = 0
    total_raw_errors = 0
    total_fuzzy_errors = 0
    total_substitutions_made = 0
    latencies = []

    print(f"{'ID':<3} | {'Materia':<10} | {'WER Crudo':<10} | {'WER Difuso':<11} | {'Reducción':<10} | {'Latencia (ms)'}")
    print("-" * 75)

    for item in corpus:
        sub = item["subject"]
        ground_truth = item["ground_truth"]
        raw_asr = item["raw_asr"]
        confidences = item.get("word_confidences", [])
        dict_terms = dictionaries.get(sub, [])

        t_start = time.perf_counter()
        raw_words = raw_asr.split()
        corrected_words = []

        for i, word in enumerate(raw_words):
            conf = confidences[i] if i < len(confidences) else 0.55
            best_term = None
            best_sim = 0.0

            # Buscar mejor candidato en el diccionario de la materia
            for d in dict_terms:
                sim = calculate_similarity(word, d["term"])
                if sim > best_sim:
                    best_sim = sim
                    best_term = d["term"]

            if best_term and best_sim >= 0.50:
                y_star = evaluate_mamdani(conf, best_sim)
                if y_star >= 0.65:
                    corrected_words.append(best_term)
                    total_substitutions_made += 1
                else:
                    corrected_words.append(word)
            else:
                corrected_words.append(word)

        t_latency = (time.perf_counter() - t_start) * 1000.0
        latencies.append(t_latency)

        corrected_text = " ".join(corrected_words)

        raw_wer, raw_dist, n_words = calculate_wer(raw_asr, ground_truth)
        fuzzy_wer, fuzzy_dist, _ = calculate_wer(corrected_text, ground_truth)

        total_ref_words += n_words
        total_raw_errors += raw_dist
        total_fuzzy_errors += fuzzy_dist

        rel_reduction = ((raw_wer - fuzzy_wer) / raw_wer * 100.0) if raw_wer > 0 else 0.0

        print(f"{item['id']:<3} | {sub:<10} | {raw_wer*100:>8.1f}% | {fuzzy_wer*100:>9.1f}% | {rel_reduction:>8.1f}% | {t_latency:>9.2f} ms")

    print("-" * 75)

    global_raw_wer = (total_raw_errors / total_ref_words) * 100.0
    global_fuzzy_wer = (total_fuzzy_errors / total_ref_words) * 100.0
    global_relative_reduction = ((global_raw_wer - global_fuzzy_wer) / global_raw_wer) * 100.0
    avg_latency = sum(latencies) / len(latencies)

    print("\n======================= RESULTADOS GLOBALES CONSOLIDADOS =======================")
    print(f"Total palabras de referencia evaluadas (N) : {total_ref_words}")
    print(f"Total sustituciones silenciosas realizadas  : {total_substitutions_made}")
    print(f"WER Promedio ASR Crudo (Línea Base)        : {global_raw_wer:.2f}%")
    print(f"WER Promedio con Capa Difusa On-Device     : {global_fuzzy_wer:.2f}%")
    print(f"REDUCCIÓN RELATIVA DEL ERROR (WER)         : {global_relative_reduction:.2f}%")
    print(f"Precisión Final Obtenida (1 - WER)          : {(100.0 - global_fuzzy_wer):.2f}% (Meta de Tesis >= 90%)")
    print(f"Latencia Media de Procesamiento             : {avg_latency:.2f} ms (Meta < 1000 ms)")
    print("================================================================================")

if __name__ == "__main__":
    run_benchmarks()
