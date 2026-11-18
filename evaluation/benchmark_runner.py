#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
SISTEMA DE VALIDACIÓN EXPERIMENTAL Y BENCHMARKING DE WER Y KER
Tesis: Mejora De La Precisión De Sistemas Speech-to-Text Mediante Técnicas De
       Lógica Difusa Para Transcripción Automática Como Apoyo A Estudiantes
       Sordos En Educación Secundaria.
Autor: Miguel Angel Tinta Aguilar - Universidad Nacional de San Agustín (UNSA)
=============================================================================
"""

import json
import os
import re
import time
import unicodedata

# -----------------------------------------------------------------------------
# 1. Normalización y Métrica Fonética Hispana
# -----------------------------------------------------------------------------
def strip_accents(text: str) -> str:
    nfkd = unicodedata.normalize('NFD', text)
    return "".join([c for c in nfkd if not unicodedata.combining(c)])

def normalize_text(text: str) -> str:
    s = strip_accents(text.lower())
    s = re.sub(r'[^a-z0-9\s]', ' ', s)
    s = re.sub(r'\s+', ' ', s).strip()
    return s

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

# -----------------------------------------------------------------------------
# 2. Funciones de Membresía e Inferencia Difusa Mamdani Tri-Variable (27 Reglas)
# -----------------------------------------------------------------------------
def trapezoid(x: float, a: float, b: float, c: float, d: float) -> float:
    # Hombros verticales (a == b o c == d): idéntico a MembershipFunction.Trapezoidal en Kotlin
    if a == b and x <= b:
        return 1.0
    if c == d and x >= c:
        return 1.0
    if x <= a or x >= d:
        return 0.0
    if b <= x <= c:
        return 1.0
    if a < x < b:
        return (x - a) / (b - a)
    if c < x < d:
        return (d - x) / (d - c)
    return 0.0

def triangle(x: float, a: float, b: float, c: float) -> float:
    if x <= a or x >= c:
        return 0.0
    if x == b:
        return 1.0
    if a < x < b:
        return (x - a) / (b - a)
    if b < x < c:
        return (c - x) / (c - b)
    return 0.0

def evaluate_mamdani(confianza: float, similitud: float, semantica: float = 0.50) -> float:
    """
    Evalúa las 27 reglas difusas Mamdani con tres variables lingüísticas de entrada:
      1. Confianza Acústica (C)
      2. Similitud Léxico-Fonética (D)
      3. Coherencia Semántica por Topic Clusters (S)
    Defuzzificación por Centroide de Área (COG) con 100 pasos de integración numérica.
    """
    c = max(0.0, min(1.0, confianza))
    d = max(0.0, min(1.0, similitud))
    s = max(0.0, min(1.0, semantica))

    # Fuzzificación de C
    mu_c_baja = trapezoid(c, 0.0, 0.0, 0.35, 0.60)
    mu_c_media = triangle(c, 0.35, 0.60, 0.80)
    mu_c_alta = trapezoid(c, 0.65, 0.85, 1.0, 1.0)

    # Fuzzificación de D
    mu_d_baja = trapezoid(d, 0.0, 0.0, 0.40, 0.65)
    mu_d_media = triangle(d, 0.45, 0.70, 0.85)
    mu_d_alta = trapezoid(d, 0.75, 0.90, 1.0, 1.0)

    # Fuzzificación de S
    mu_s_baja = trapezoid(s, 0.0, 0.0, 0.25, 0.45)
    mu_s_media = triangle(s, 0.30, 0.50, 0.70)
    mu_s_alta = trapezoid(s, 0.55, 0.75, 1.0, 1.0)

    # --- Grupo 1: C IS Baja ---
    r1 = min(mu_c_baja, mu_d_alta, mu_s_alta)    # J Alta
    r2 = min(mu_c_baja, mu_d_alta, mu_s_media)   # J Alta
    r3 = min(mu_c_baja, mu_d_alta, mu_s_baja)    # J Media
    r4 = min(mu_c_baja, mu_d_media, mu_s_alta)   # J Alta
    r5 = min(mu_c_baja, mu_d_media, mu_s_media)  # J Media
    r6 = min(mu_c_baja, mu_d_media, mu_s_baja)   # J Baja
    r7 = min(mu_c_baja, mu_d_baja, mu_s_alta)    # J Media
    r8 = min(mu_c_baja, mu_d_baja, mu_s_media)   # J Baja
    r9 = min(mu_c_baja, mu_d_baja, mu_s_baja)    # J Baja

    # --- Grupo 2: C IS Media ---
    r10 = min(mu_c_media, mu_d_alta, mu_s_alta)  # J Alta
    r11 = min(mu_c_media, mu_d_alta, mu_s_media) # J Alta
    r12 = min(mu_c_media, mu_d_alta, mu_s_baja)  # J Media
    r13 = min(mu_c_media, mu_d_media, mu_s_alta) # J Alta
    r14 = min(mu_c_media, mu_d_media, mu_s_media)# J Media
    r15 = min(mu_c_media, mu_d_media, mu_s_baja) # J Baja
    r16 = min(mu_c_media, mu_d_baja, mu_s_alta)  # J Media
    r17 = min(mu_c_media, mu_d_baja, mu_s_media) # J Baja
    r18 = min(mu_c_media, mu_d_baja, mu_s_baja)  # J Baja

    # --- Grupo 3: C IS Alta ---
    r19 = min(mu_c_alta, mu_d_alta, mu_s_alta)   # J Media
    r20 = min(mu_c_alta, mu_d_alta, mu_s_media)  # J Media
    r21 = min(mu_c_alta, mu_d_alta, mu_s_baja)   # J Baja
    r22 = min(mu_c_alta, mu_d_media, mu_s_alta)  # J Media
    r23 = min(mu_c_alta, mu_d_media, mu_s_media) # J Baja
    r24 = min(mu_c_alta, mu_d_media, mu_s_baja)  # J Baja
    r25 = min(mu_c_alta, mu_d_baja, mu_s_alta)   # J Baja
    r26 = min(mu_c_alta, mu_d_baja, mu_s_media)  # J Baja
    r27 = min(mu_c_alta, mu_d_baja, mu_s_baja)   # J Baja

    # Agregación de consecuentes (s-norma MAX)
    activa_alta = max(r1, r2, r4, r10, r11, r13)
    activa_media = max(r3, r5, r7, r12, r14, r16, r19, r20, r22)
    activa_baja = max(r6, r8, r9, r15, r17, r18, r21, r23, r24, r25, r26, r27)

    # Defuzzificación Centroide COG (100 pasos)
    steps = 100
    step_size = 1.0 / steps
    numerator = 0.0
    denominator = 0.0
    for i in range(steps + 1):
        y = i * step_size
        mu_y_alta = min(activa_alta, trapezoid(y, 0.60, 0.80, 1.0, 1.0))
        mu_y_media = min(activa_media, triangle(y, 0.35, 0.55, 0.75))
        mu_y_baja = min(activa_baja, trapezoid(y, 0.0, 0.0, 0.30, 0.50))
        mu_combined = max(mu_y_alta, mu_y_media, mu_y_baja)
        numerator += y * mu_combined
        denominator += mu_combined

    cog = (numerator / denominator) if denominator > 0.00001 else 0.0
    return max(0.0, min(1.0, cog))

# -----------------------------------------------------------------------------
# 3. Topic Clusters y Coherencia Semántica
# -----------------------------------------------------------------------------
# Debe coincidir con DecisionEvaluator.contextMatchThreshold (Kotlin)
CONTEXT_MATCH_THRESHOLD = 0.80

def load_clusters(clusters_dir: str) -> dict:
    clusters = {}
    for sub in ["biologia", "quimica", "historia", "geografia"]:
        path = os.path.join(clusters_dir, f"{sub}_clusters.json")
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                clusters[sub.upper()] = json.load(f)
    return clusters

# Umbral nítido de decisión (= DecisionEvaluator.decisionThreshold)
DECISION_THRESHOLD = 0.65

# --- Filtro de candidatos (espejo de OpenNlpPosTagger.isCandidateNoun) ---------
STOPWORDS_ES = {'o', 'y', 'nuestra', 'al', 'esa', 'tambien', 'ese', 'a', 'dias', 'pero', 'aquellos', 'como', 'mediante', 'sea', 'e', 'mal', 'el', 'nosotros', 'para', 'es', 'de', 'unas', 'ustedes', 'tan', 'tardes', 'nuestras', 'que', 'son', 'esas', 'sin', 'una', 'sean', 'ni', 'ellas', 'tras', 'por', 'quien', 'del', 'menos', 'mi', 'fue', 'aquel', 'con', 'si', 'cuando', 'esos', 'pues', 'en', 'aquella', 'nuestros', 'la', 'mis', 'porque', 'tu', 'donde', 'no', 'ellos', 'un', 'u', 'nuestro', 'aquellas', 'ya', 'durante', 'estos', 'sino', 'cual', 'estas', 'mas', 'unos', 'muy', 'fueron', 'este', 'era', 'sobre', 'ella', 'las', 'su', 'bien', 'tus', 'hola', 'eran', 'sus', 'esta', 'buenos', 'yo', 'los', 'tampoco'}
NOUN_SUFFIXES = ('cion', 'ción', 'dad', 'fera', 'fisis', 'geno', 'grafia', 'grafía', 'ismo', 'ista', 'lisis', 'logia', 'logía', 'metria', 'metría', 'miento', 'plasto', 'sfera', 'sion', 'sis', 'sión', 'soma', 'tad', 'trofia')

def is_candidate_noun(word: str) -> bool:
    clean = re.sub(r"[^a-záéíóúüñ]", "", word.lower().strip())
    if len(clean) < 3:
        return False
    if clean in STOPWORDS_ES:
        return False
    if word[:1].isupper():
        return True
    if any(clean.endswith(x) for x in NOUN_SUFFIXES):
        return True
    return len(clean) >= 4

def is_inflection_of(word: str, term: str) -> bool:
    """Plural/singular regular ('gametos'/'gameto'): la palabra ya está bien escrita (espejo de isInflectionOf)."""
    w, t = strip_accents(word.lower()), strip_accents(term.lower())
    if w == t:
        return False
    return w == t + "s" or w == t + "es" or t == w + "s" or t == w + "es"

# Selección de candidato por los tres factores (espejo de DecisionEvaluator)
MAX_CANDIDATES = 3
CANDIDATE_WINDOW = 0.12
SUBSTITUTION_MIN_SIMILARITY = 0.70
RERANK_MIN_SIMILARITY = 0.70

# --- Términos partidos por el ASR (espejo de DecisionEvaluator.findSplitTerms) ---
# Vosk solo escribe palabras de su vocabulario: un término que no conoce sale en pedazos
# ("citoplasma" -> "sito plasma", "procariota" -> "pero carlota"). Se prueban uniones de 2-3 palabras seguidas.
SPAN_MODE = "morfema"      # None = desactivado | "estricto" | "morfema" (ver find_split_terms)
SPAN_MAX_WORDS = 3
SPAN_MIN_CHARS = 6          # "y una" -> "yuna" no se une
SPAN_MIN_SIMILARITY = 0.80  # = CONTEXT_MATCH_THRESHOLD: la unión debe parecerse claramente a un término
SPAN_MIN_GAIN = 0.05        # y explicar mejor que cualquiera de sus pedazos por separado
# C neutra (= AndroidSpeechEngine.NEUTRAL_CONFIDENCE) para UNIONES cuyo término el ASR no puede escribir: si lo
# partió en pedazos es porque no lo conocía, y la confianza en esos pedazos no es evidencia en contra del término.
# NO se aplica a palabras enteras: "saber" escrita con seguridad sí es evidencia en contra de "suber".
OOV_NEUTRAL_CONFIDENCE = 0.65


def clean_token(word: str) -> str:
    return re.sub(r"^[^\wáéíóúüñ]+|[^\wáéíóúüñ]+$", "", word.strip())


def plural_of(term: str) -> str:
    """Espejo de DecisionEvaluator.pluralOf: la aguda con tilde la pierde (infección -> infecciones)."""
    if strip_accents(term[-1:].lower()) in "aeiou":
        return term + "s"
    if len(term) >= 2 and term[-1].lower() in "ns" and term[-2].lower() in "áéíóú":
        term = term[:-2] + strip_accents(term[-2]) + term[-1]
    return term + "es"


def span_similarity(joined: str, term: str) -> float:
    """D de una unión contra un término, admitiendo su plural ("game tos" -> gametos)."""
    sim = calculate_similarity(joined, term)
    if joined.lower().endswith("s") and not term.lower().endswith("s"):
        sim = max(sim, calculate_similarity(joined, plural_of(term)))
    return sim


def similarity_upper_bound(s1: str, s2: str) -> float:
    """Cota superior EXACTA de calculate_similarity por diferencia de largos (Levenshtein >= |len1 - len2|).
    Espejo de LevenshteinMatcher.combinedSimilarityUpperBound."""
    c1, c2 = strip_accents(s1.lower()), strip_accents(s2.lower())
    p1, p2 = to_hispanic_phonetic(s1), to_hispanic_phonetic(s2)
    m, mp = max(len(c1), len(c2)), max(len(p1), len(p2))
    lex = 1.0 if m == 0 else 1.0 - abs(len(c1) - len(c2)) / m
    phon = 1.0 if mp == 0 else 1.0 - abs(len(p1) - len(p2)) / mp
    return 0.60 * lex + 0.40 * phon


def _span_may_reach(joined, term):
    """Una unión solo se acepta con D >= 0.80 y sus alternativas con D >= 0.70: un término que por largo no llega
    a 0.70 (ni en plural) no cambia el resultado. Mismo descarte que DecisionEvaluator.scanSpan."""
    if similarity_upper_bound(joined, term) >= RERANK_MIN_SIMILARITY:
        return True
    return joined.lower().endswith("s") and not term.lower().endswith("s") and \
        similarity_upper_bound(joined, plural_of(term)) >= RERANK_MIN_SIMILARITY


_RANK_CACHE = {}


def rank_terms(text, dict_terms, similarity=calculate_similarity):
    """Mejor término por D y candidatos cercanos para el rerank (misma regla para palabras y uniones).
    Memorizado por (texto, diccionario, función): mismo resultado, sin recalcular las palabras repetidas."""
    key = (text, id(dict_terms), similarity.__name__)
    hit = _RANK_CACHE.get(key)
    if hit is not None and hit[0] is dict_terms:
        return hit[1]
    pool = enumerate(dict_terms)
    if similarity is span_similarity:
        pool = ((idx, d) for idx, d in pool if _span_may_reach(text, d["term"]))
    scored = sorted(
        ((similarity(text, d["term"]), idx, d["term"]) for idx, d in pool),
        key=lambda t: (-t[0], t[1])
    )
    if not scored:
        result = (None, 0.0, [])
        _RANK_CACHE[key] = (dict_terms, result)
        return result
    best_sim, _, best_term = scored[0]
    top = [(t, sm) for sm, _, t in scored[:MAX_CANDIDATES] if sm >= 0.50]
    alternatives = [
        (t, sm) for k, (t, sm) in enumerate(top)
        if k == 0 or (sm >= best_sim - CANDIDATE_WINDOW and sm >= RERANK_MIN_SIMILARITY)
    ]
    result = (best_term, best_sim, alternatives)
    _RANK_CACHE[key] = (dict_terms, result)
    return result


def effective_confidence(conf, term, asr_oov):
    if asr_oov and term.lower() in asr_oov:
        return min(conf, OOV_NEUTRAL_CONFIDENCE)
    return conf


def evaluate_candidates(candidates, conf, asr_oov, matched_terms, sub_clusters, lesson):
    """Rerank por (C, D, S): gana el mayor y*; empate -> mayor D."""
    chosen, chosen_y, chosen_s, chosen_d = candidates[0][0], -1.0, 0.0, candidates[0][1]
    for cand, sim in candidates:
        s_c, _ = calculate_semantic_coherence(cand, matched_terms, sub_clusters, lesson)
        y_c = evaluate_mamdani(effective_confidence(conf, cand, asr_oov), sim, s_c)
        if y_c > chosen_y + 1e-9 or (abs(y_c - chosen_y) <= 1e-9 and sim > chosen_d):
            chosen, chosen_y, chosen_s, chosen_d = cand, y_c, s_c, sim
    return chosen, chosen_y, chosen_s, chosen_d


def surface_form(spoken, term):
    """Espejo de DecisionEvaluator.surfaceForm: plural si lo dicho termina en "s" y se parece más al plural
    ("arcanos" -> alcanos, "game tos" -> gametos). Solo la forma escrita, no la decisión."""
    if spoken.lower().endswith("s") and not term.lower().endswith("s") and \
            calculate_similarity(spoken, plural_of(term)) > calculate_similarity(spoken, term):
        return plural_of(term)
    return term


def span_surface(span, term):
    return surface_form(span["joined"], term)


# Reglas del 18/11/2026 (uniones de palabras vacías, prefijos sueltos, plural en palabra suelta). False = comportamiento
# anterior, para medir el "antes" (evaluar_quimica_ciclos.py --sin-reglas-18nov).
REGLAS_18NOV = True

# Prefijos que Vosk deja sueltos delante de un término ("in saturado", "oxi acetilénica"). Espejo de PREFIX_PIECES.
PREFIX_PIECES = {"in", "sin", "des", "sub", "di", "tri", "per", "bi", "oxi", "ex"}

_EXACT_KEYS = {}


def exact_term_keys(dict_terms):
    """Términos (y plurales) sin tildes y en minúsculas. Espejo de DecisionEvaluator.exactTermKeys."""
    key = id(dict_terms)
    if key not in _EXACT_KEYS:
        keys = set()
        for d in dict_terms:
            keys.add(strip_accents(d["term"].lower()))
            keys.add(strip_accents(plural_of(d["term"]).lower()))
        _EXACT_KEYS[key] = (dict_terms, keys)
    return _EXACT_KEYS[key][1]


def has_sentence_support(term, matched_terms, clusters):
    """Otro término de la oración comparte cluster con term. Espejo de DecisionEvaluator.hasSentenceSupport."""
    lower = term.lower()
    mine = [c for c in clusters if lower in {t.lower() for t in c.get("terms", [])}]
    return any(other != lower and any(other in {t.lower() for t in c.get("terms", [])} for c in mine)
               for other in matched_terms)


def is_proper_part(piece_term: str, term: str) -> bool:
    """"plasma" es parte de "citoplasma"; "fotosíntesis" NO es parte propia de sí mismo ("la fotosintesis")."""
    p, t = strip_accents(piece_term.lower()), strip_accents(term.lower())
    return len(p) < len(t) and p in t


def find_split_terms(pre_scans, dict_terms, mode=SPAN_MODE):
    """Uniones de 2-3 palabras seguidas que se parecen a un término mucho más que sus pedazos.

    Si un pedazo YA es un término bien escrito ("plasma" en "sito plasma"):
      - "estricto": no se une (protege frases correctas como "el núcleo lo protege");
      - "morfema": se une solo si ese término forma parte del término unido ("plasma" ⊂ "citoplasma").
        En ese caso no se aplica la C neutra de los términos desconocidos: el ASR escribió con seguridad una
        palabra real, así que la duda tiene que venir de su propia confianza.
    Se eligen las uniones de mayor D sin solaparse. Devuelve {índice inicial: unión}.
    """
    found = []
    n = len(pre_scans)
    for i in range(n):
        for size in range(2, SPAN_MAX_WORDS + 1):
            if i + size > n:
                break
            items = pre_scans[i:i + size]
            words = [clean_token(it["word"]) for it in items]
            joined = "".join(words)
            if len(strip_accents(joined)) < SPAN_MIN_CHARS:
                continue
            # Solo palabras vacías ("al que no" -> alqueno): solo si forman el término EXACTO (o su plural); la
            # evaluación exige además otro término del mismo cluster en la oración. Espejo de findSplitTerms.
            function_only = not any(is_candidate_noun(w) for w in words)
            if function_only and (not REGLAS_18NOV or strip_accents(joined.lower()) not in exact_term_keys(dict_terms)):
                continue
            exact = [j for j, it in enumerate(items) if it["best_term"] and
                     (it["best_sim"] >= 0.999 or is_inflection_of(it["word"], it["best_term"]))]
            if exact and mode == "estricto":
                continue
            # Con un pedazo ya exacto, la unión no puede empezar ni terminar en una palabra vacía: pegada al término
            # desde afuera formaba otro ("de óxido" -> dióxido, "metano el" -> metanol). En medio sí se admite: es el
            # pegamento de los pedazos de Vosk ("sean o bacterias" -> cianobacterias). Espejo de findSplitTerms.
            # Excepción: un prefijo que el ASR separó ("in saturado" -> insaturado, "oxi acetilénica").
            if exact and any(j not in exact and not is_candidate_noun(words[j]) and
                             (not REGLAS_18NOV or strip_accents(words[j].lower()) not in PREFIX_PIECES)
                             for j in (0, len(items) - 1)):
                continue
            best_term, best_sim, alternatives = rank_terms(joined, dict_terms, span_similarity)
            if best_sim < SPAN_MIN_SIMILARITY:
                continue
            if exact and not all(is_proper_part(items[j]["best_term"], best_term) for j in exact):
                continue
            pieces = [it["best_sim"] for j, it in enumerate(items) if j not in exact]
            if best_sim < max(pieces, default=0.0) + SPAN_MIN_GAIN:
                continue
            found.append({
                "start": i, "len": size, "joined": joined, "term": best_term, "sim": best_sim,
                "alternatives": alternatives, "conf": min(it["conf"] for it in items),
                "has_exact_piece": bool(exact),
                "function_only": function_only,
            })
    found.sort(key=lambda sp: (-sp["sim"], -sp["len"], sp["start"]))
    chosen, used = {}, set()
    for sp in found:
        covered = set(range(sp["start"], sp["start"] + sp["len"]))
        if covered & used:
            continue
        chosen[sp["start"]] = sp
        used |= covered
    return chosen


# --- Memoria de la clase (espejo exacto de LessonContext.kt) -----------------
MACRO_CREDIT = 0.50  # = DecisionEvaluator.MACRO_CREDIT
LESSON_HALF_LIFE_S = 45.0  # = LessonContext.DEFAULT_HALF_LIFE_MS / 1000
LESSON_MIN_EVIDENCE = 0.5
LESSON_FULL_EVIDENCE = 3.0
LESSON_FLOOR = 0.20
LESSON_WEIGHT = 0.40

class LessonContext:
    """Pesos por cluster con decaimiento exponencial. El reloj es simulado (self.now, en segundos)."""
    def __init__(self):
        self.weights = {}
        self.last = None
        self.now = 0.0
        self.macro_by_cluster = {}

    def _decay(self):
        if self.last is None:
            return 1.0
        return 0.5 ** (max(0.0, self.now - self.last) / LESSON_HALF_LIFE_S)

    def record_terms(self, term_cluster_ids):
        if not term_cluster_ids:
            return
        f = self._decay()
        if f < 1.0:
            self.weights = {k: v * f for k, v in self.weights.items() if v * f >= 0.01}
        self.last = self.now
        for ids in term_cluster_ids:
            for cid in ids:
                self.weights[cid] = self.weights.get(cid, 0.0) + 1.0 / len(ids)

    def affinity(self, ids):
        """-> (value, strength, best_id) o None"""
        if not self.weights or not ids:
            return None
        dominant = max(self.weights.values())
        evidence = dominant * self._decay()
        if evidence < LESSON_MIN_EVIDENCE:
            return None
        best, best_id = 0.0, ids[0]
        for cid in ids:
            w = self.weights.get(cid, 0.0)
            if w > best:
                best, best_id = w, cid
        affinity = best / dominant
        if self.macro_by_cluster:
            macro_w = {}
            for cid, w in self.weights.items():
                m = self.macro_by_cluster.get(cid)
                if m:
                    macro_w[m] = macro_w.get(m, 0.0) + w
            dom_macro = max(macro_w.values()) if macro_w else 0.0
            if dom_macro > 0:
                for cid in ids:
                    m = self.macro_by_cluster.get(cid)
                    if not m:
                        continue
                    via = MACRO_CREDIT * macro_w.get(m, 0.0) / dom_macro
                    if via > affinity:
                        affinity = via
                        if best == 0.0:
                            best_id = cid
        value = max(LESSON_FLOOR, min(1.0, affinity))
        return (value, min(1.0, evidence / LESSON_FULL_EVIDENCE), best_id)

    def reset(self):
        self.weights, self.last = {}, None

def combine_semantic_evidence(sentence_score, lesson):
    if sentence_score is None and lesson is None:
        return 0.50
    if sentence_score is None:
        return max(0.0, min(1.0, 0.50 + (lesson[0] - 0.50) * lesson[1]))
    if lesson is None:
        return sentence_score
    w = LESSON_WEIGHT * lesson[1]
    return max(0.0, min(1.0, (1.0 - w) * sentence_score + w * lesson[0]))

_MACRO_CACHE = {}

def term_macros(term: str, clusters: list) -> set:
    """Áreas temáticas de un término a través de sus clusters (con caché por lista de clusters)."""
    key = (id(clusters), term)
    if key not in _MACRO_CACHE:
        _MACRO_CACHE[key] = {c.get("macro_id") for c in clusters
                             if term in {x.lower() for x in c.get("terms", [])} and c.get("macro_id")}
    return _MACRO_CACHE[key]

def calculate_semantic_coherence(candidate_term: str, matched_terms_in_sentence: set, clusters: list, lesson=None) -> tuple:
    """
    Coherencia semántica S in [0.0, 1.0]: co-ocurrencia en la oración (fórmula de la tesis)
    combinada con la memoria de la clase (lesson) si se provee.
    """
    if not clusters:
        return (0.50, None)

    cand_lower = candidate_term.lower()
    matching_clusters = []
    for c in clusters:
        terms_set = {t.lower() for t in c.get("terms", [])}
        if cand_lower in terms_set:
            matching_clusters.append((c, terms_set))

    if not matching_clusters:
        return (0.20, None)

    best_name = matching_clusters[0][0].get("topic_name")
    sentence_score = None
    other_terms = [t for t in matched_terms_in_sentence if t != cand_lower]
    if other_terms:
        best_score = 0.0
        for c, terms_set in matching_clusters:
            macro = c.get("macro_id") or None
            credit = 0.0
            for other in other_terms:
                if other in terms_set:
                    credit += 1.0
                elif macro and macro in term_macros(other, clusters):
                    credit += MACRO_CREDIT
            score = credit / len(other_terms)
            if score >= best_score:
                best_score = score
                best_name = c.get("topic_name")
        sentence_score = max(0.0, min(1.0, best_score))

    lesson_aff = lesson.affinity([c.get("topic_id") for c, _ in matching_clusters]) if lesson else None
    if lesson_aff is not None and sentence_score is None:
        for c, _ in matching_clusters:
            if c.get("topic_id") == lesson_aff[2]:
                best_name = c.get("topic_name")

    return (combine_semantic_evidence(sentence_score, lesson_aff), best_name)

def cluster_ids_for_terms(terms, clusters):
    ids = []
    for t in terms:
        ids.append([c.get("topic_id") for c in clusters if t in {x.lower() for x in c.get("terms", [])}])
    return ids

# -----------------------------------------------------------------------------
# 4. Cálculo de Métricas Formales: WER y KER
# -----------------------------------------------------------------------------
def calculate_wer(hypothesis: str, reference: str) -> tuple:
    """
    Calcula Word Error Rate formal: WER = (S + D + I) / N
    """
    h = [w for w in hypothesis.strip().lower().split() if w]
    r = [w for w in reference.strip().lower().split() if w]
    n = len(r)
    if n == 0:
        return (0.0 if len(h) == 0 else 1.0, 0, 0)

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

def contar_termino(termino: list, palabras: list) -> int:
    """Veces que la secuencia de palabras `termino` aparece en `palabras` (términos de una o varias palabras)."""
    k = len(termino)
    return sum(1 for i in range(len(palabras) - k + 1) if palabras[i:i + k] == termino)


def calculate_ker(hypothesis: str, reference: str, keyword_terms: set) -> tuple:
    """
    KER = (N_k - A_k) / N_k   y   Precisión semántica = A_k / N_k = 1 - KER
      N_k = apariciones de términos clave en la referencia
      A_k = cuántas de ellas aparecen también en la hipótesis (como máximo, tantas como en la referencia)
    Se compara palabra por palabra tras normalizar (minúsculas, sin tildes ni puntuación).
    Retorna (ker, precision, A, N).
    """
    referencia = normalize_text(reference).split()
    hipotesis = normalize_text(hypothesis).split()
    n = a = 0
    for termino in {normalize_text(t) for t in keyword_terms} - {""}:
        palabras = termino.split()
        en_ref = contar_termino(palabras, referencia)
        if en_ref:
            n += en_ref
            a += min(en_ref, contar_termino(palabras, hipotesis))
    if n == 0:
        return (0.0, 1.0, 0, 0)
    ker = (n - a) / n
    return (ker, 1.0 - ker, a, n)

# -----------------------------------------------------------------------------
# 5. Ejecución del Arnés Experimental de Benchmarking
# -----------------------------------------------------------------------------
def run_benchmarks(use_memory=True, corpus_name="corpus_arequipa_test.json", write_report=True, use_rerank=True, crisp=None, fixed_conf=None,
                   span_mode=SPAN_MODE, asr_oov=None, details=None):
    base_dir = os.path.dirname(os.path.abspath(__file__))
    assets_dir = os.path.join(base_dir, "..", "app", "src", "main", "assets")
    assets_dict_dir = os.path.join(assets_dir, "dictionaries")
    assets_clusters_dir = os.path.join(assets_dir, "clusters")
    corpus_file = os.path.join(base_dir, corpus_name)

    # Cargar Diccionarios MINEDU
    dictionaries = {}
    dict_term_sets = {}
    for sub in ["biologia", "quimica", "historia", "geografia"]:
        path = os.path.join(assets_dict_dir, f"{sub}.json")
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
                dictionaries[sub.upper()] = data
                dict_term_sets[sub.upper()] = {d["term"] for d in data}

    # Cargar Clusters Temáticos
    clusters = load_clusters(assets_clusters_dir)

    # Cargar Corpus Escolar
    with open(corpus_file, "r", encoding="utf-8") as f:
        corpus = json.load(f)

    print("==========================================================================================================")
    print("      BANCO DE PRUEBAS EXPERIMENTAL: EVALUACIÓN DE PRECISIÓN WER Y KER (MODELO TRI-VARIABLE 27 REGLAS)")
    print("  Tesis UNSA - Sistema Speech-to-Text Asistido por Lógica Difusa On-Device con Topic Clusters Semánticos")
    print("==========================================================================================================\n")

    total_ref_words = 0
    total_raw_errors = 0
    total_fuzzy_errors = 0
    total_substitutions_made = 0

    total_ref_keywords = 0
    total_raw_correct_keywords = 0
    total_fuzzy_correct_keywords = 0

    semantic_scores = []
    latencies = []

    # Métricas discriminadas por materia
    subject_stats = {}

    header = f"{'ID':<3} | {'Materia':<10} | {'WER Crudo':<9} | {'WER Dif':<9} | {'Red WER':<8} | {'KER Crudo':<9} | {'KER Dif':<9} | {'Prec Sem':<9} | {'S Med':<5} | {'Lat (ms)'}"
    print(header)
    print("-" * len(header))

    lesson = LessonContext() if use_memory else None
    if lesson is not None:
        lesson.macro_by_cluster = {c["topic_id"]: c["macro_id"] for cl_list in clusters.values() for c in cl_list if c.get("macro_id")}
    last_subject = None

    for item in corpus:
        sub = item["subject"]
        if lesson is not None:
            if sub != last_subject:
                lesson.reset()
            last_subject = sub
            lesson.now += item.get("gap_s", 8)
        ground_truth = item["ground_truth"]
        raw_asr = item["raw_asr"]
        confidences = item.get("word_confidences", [])
        dict_terms = dictionaries.get(sub, [])
        sub_clusters = clusters.get(sub, [])
        keyword_set = dict_term_sets.get(sub, set())

        t_start = time.perf_counter()
        raw_words = raw_asr.split()

        # -------------------------------------------------------------
        # PASADA 1: Pre-escaneo Léxico de la Oración (D >= 0.50)
        # -------------------------------------------------------------
        pre_scans = []
        for i, word in enumerate(raw_words):
            conf = fixed_conf if fixed_conf is not None else (confidences[i] if i < len(confidences) else 0.55)
            best_term = None
            best_sim = 0.0
            alternatives = []

            if is_candidate_noun(word) and dict_terms:
                # Misma regla que rank_terms (y memorizada): mejor por D y candidatos cercanos
                best_term, best_sim, alternatives = rank_terms(word, dict_terms)

            pre_scans.append({
                "word": word,
                "conf": conf,
                "best_term": best_term,
                "best_sim": best_sim,
                "alternatives": alternatives,
            })

        matched_terms = {
            item["best_term"].lower()
            for item in pre_scans
            if item["best_term"] and item["best_sim"] >= CONTEXT_MATCH_THRESHOLD
        }

        # -------------------------------------------------------------
        # PASADA 1b: Palabras partidas por el ASR ("sito plasma" -> citoplasma)
        # -------------------------------------------------------------
        spans = find_split_terms(pre_scans, dict_terms, span_mode) if (span_mode and dict_terms) else {}
        if spans:
            # La unión REEMPLAZA a sus pedazos como evidencia del tema: "plasma" (de "sito plasma") no debe votar
            # en contra de "citoplasma", y el término unido cuenta como una palabra con D >= 0.80
            covered = {j for sp in spans.values() for j in range(sp["start"], sp["start"] + sp["len"])}
            matched_terms = {
                it["best_term"].lower() for j, it in enumerate(pre_scans)
                if j not in covered and it["best_term"] and it["best_sim"] >= CONTEXT_MATCH_THRESHOLD
            } | {sp["term"].lower() for sp in spans.values() if sp["sim"] >= CONTEXT_MATCH_THRESHOLD}

        # -------------------------------------------------------------
        # PASADA 2: Inferencia Difusa Mamdani Tri-Variable (C, D, S)
        # -------------------------------------------------------------
        corrected_words = []
        item_sem_scores = []
        resolved_terms = set()

        k = 0
        while k < len(pre_scans):
            item_scan = pre_scans[k]
            span = spans.get(k)
            if span is not None:
                if crisp is not None:
                    if span["sim"] >= crisp[0] and span["conf"] <= crisp[1]:
                        corrected_words.append(span["surface"])
                        total_substitutions_made += 1
                        item_sem_scores.append(0.5)
                        k += span["len"]
                        continue
                else:
                    span_cands = span["alternatives"] if use_rerank else span["alternatives"][:1]
                    if span["function_only"]:
                        span_cands = [c for c in span_cands if has_sentence_support(c[0], matched_terms, sub_clusters)]
                    chosen, chosen_y, chosen_s, chosen_d = (None, -1.0, 0.0, 0.0) if not span_cands else \
                        evaluate_candidates(span_cands, span["conf"], None if span["has_exact_piece"] else asr_oov,
                                            matched_terms, sub_clusters, lesson)
                    if chosen is not None and chosen_y >= DECISION_THRESHOLD and chosen_d >= SUBSTITUTION_MIN_SIMILARITY:
                        corrected_words.append(span_surface(span, chosen))
                        total_substitutions_made += 1
                        resolved_terms.add(chosen.lower())
                        item_sem_scores.append(chosen_s)
                        k += span["len"]
                        continue
                # Unión rechazada: sus palabras se evalúan una por una, como siempre

            word = item_scan["word"]
            conf = item_scan["conf"]
            best_term = item_scan["best_term"]
            best_sim = item_scan["best_sim"]

            if crisp is not None:
                # Línea base BOOLEANA (sin lógica difusa): sustituir el mejor por D si D >= tD y C <= tC
                if best_term and best_sim >= crisp[0] and conf <= crisp[1]:
                    corrected_words.append(best_term)
                    total_substitutions_made += 1
                    item_sem_scores.append(0.5)
                else:
                    corrected_words.append(word)
            elif best_term and best_sim >= 0.50:
                candidates = item_scan["alternatives"] if (use_rerank and len(item_scan["alternatives"]) > 1) \
                    else [(best_term, best_sim)]
                # Palabra entera: su C es evidencia acústica real aunque el término no esté en el vocabulario
                # del ASR ("saber" con C alta no debe volverse "suber"). La C neutra solo aplica a uniones.
                chosen, chosen_y, chosen_s, chosen_d = evaluate_candidates(
                    candidates, conf, None, matched_terms, sub_clusters, lesson)
                item_sem_scores.append(chosen_s)

                already_correct = is_inflection_of(word, chosen)
                if chosen_y >= DECISION_THRESHOLD and chosen_d >= SUBSTITUTION_MIN_SIMILARITY and not already_correct:
                    corrected_words.append(surface_form(clean_token(word) or word, chosen) if REGLAS_18NOV else chosen)
                    total_substitutions_made += 1
                    resolved_terms.add(chosen.lower())
                else:
                    corrected_words.append(word)
                    if chosen_d >= 0.999 or already_correct:
                        resolved_terms.add(chosen.lower())
            else:
                corrected_words.append(word)
            k += 1

        t_latency = (time.perf_counter() - t_start) * 1000.0
        if lesson is not None:
            lesson.record_terms(cluster_ids_for_terms(resolved_terms, sub_clusters))
        latencies.append(t_latency)

        mean_s = sum(item_sem_scores) / len(item_sem_scores) if item_sem_scores else 0.50
        semantic_scores.append(mean_s)

        corrected_text = " ".join(corrected_words)
        if details is not None:
            details.append({"id": item["id"], "raw": raw_asr, "corrected": corrected_text, "truth": ground_truth})

        # Cálculo de WER
        raw_wer, raw_dist, n_words = calculate_wer(raw_asr, ground_truth)
        fuzzy_wer, fuzzy_dist, _ = calculate_wer(corrected_text, ground_truth)

        total_ref_words += n_words
        total_raw_errors += raw_dist
        total_fuzzy_errors += fuzzy_dist

        rel_wer_reduction = ((raw_wer - fuzzy_wer) / raw_wer * 100.0) if raw_wer > 0 else 0.0

        # Cálculo de KER
        raw_ker, raw_prec, raw_kw_c, total_kw = calculate_ker(raw_asr, ground_truth, keyword_set)
        fuzzy_ker, fuzzy_prec, fuzzy_kw_c, _ = calculate_ker(corrected_text, ground_truth, keyword_set)

        total_ref_keywords += total_kw
        total_raw_correct_keywords += raw_kw_c
        total_fuzzy_correct_keywords += fuzzy_kw_c

        # Acumular por materia
        if sub not in subject_stats:
            subject_stats[sub] = {
                "ref_words": 0, "raw_err": 0, "fuzzy_err": 0,
                "ref_kw": 0, "raw_kw_c": 0, "fuzzy_kw_c": 0, "s_scores": []
            }
        subject_stats[sub]["ref_words"] += n_words
        subject_stats[sub]["raw_err"] += raw_dist
        subject_stats[sub]["fuzzy_err"] += fuzzy_dist
        subject_stats[sub]["ref_kw"] += total_kw
        subject_stats[sub]["raw_kw_c"] += raw_kw_c
        subject_stats[sub]["fuzzy_kw_c"] += fuzzy_kw_c
        subject_stats[sub]["s_scores"].append(mean_s)

        print(f"{item['id']:<3} | {sub:<10} | {raw_wer*100:>7.1f}% | {fuzzy_wer*100:>7.1f}% | {rel_wer_reduction:>6.1f}% | {raw_ker*100:>7.1f}% | {fuzzy_ker*100:>7.1f}% | {fuzzy_prec*100:>7.1f}% | {mean_s:>5.2f} | {t_latency:>6.2f} ms")

    print("-" * len(header))

    # Métricas Globales
    global_raw_wer = (total_raw_errors / total_ref_words) * 100.0 if total_ref_words > 0 else 0.0
    global_fuzzy_wer = (total_fuzzy_errors / total_ref_words) * 100.0 if total_ref_words > 0 else 0.0
    global_rel_wer_reduction = ((global_raw_wer - global_fuzzy_wer) / global_raw_wer * 100.0) if global_raw_wer > 0 else 0.0

    global_raw_ker = ((total_ref_keywords - total_raw_correct_keywords) / total_ref_keywords * 100.0) if total_ref_keywords > 0 else 0.0
    global_fuzzy_ker = ((total_ref_keywords - total_fuzzy_correct_keywords) / total_ref_keywords * 100.0) if total_ref_keywords > 0 else 0.0
    global_semantic_precision = (total_fuzzy_correct_keywords / total_ref_keywords * 100.0) if total_ref_keywords > 0 else 100.0
    global_rel_ker_reduction = ((global_raw_ker - global_fuzzy_ker) / global_raw_ker * 100.0) if global_raw_ker > 0 else 0.0

    avg_latency = sum(latencies) / len(latencies) if latencies else 0.0
    avg_s = sum(semantic_scores) / len(semantic_scores) if semantic_scores else 0.50

    print("\n======================= RESULTADOS GLOBALES CONSOLIDADOS =======================")
    print(f"Total palabras de referencia evaluadas (N)     : {total_ref_words}")
    print(f"Total palabras clave curriculares evaluadas    : {total_ref_keywords}")
    print(f"Total sustituciones silenciosas realizadas      : {total_substitutions_made}")
    print("--------------------------------------------------------------------------------")
    print(f"WER Promedio ASR Crudo (Línea Base)            : {global_raw_wer:.2f}%")
    print(f"WER Promedio con Capa Difusa On-Device         : {global_fuzzy_wer:.2f}%")
    print(f"REDUCCIÓN RELATIVA DE WER (ΔWER_rel)           : {global_rel_wer_reduction:.2f}%")
    print("--------------------------------------------------------------------------------")
    print(f"KER Promedio ASR Crudo (Pretest)               : {global_raw_ker:.2f}%")
    print(f"KER Promedio con Capa Difusa (Postest)         : {global_fuzzy_ker:.2f}%")
    print(f"REDUCCIÓN RELATIVA DE KER (ΔKER_rel)           : {global_rel_ker_reduction:.2f}%")
    print(f"PRECISIÓN SEMÁNTICA FINAL (1 - KER)            : {global_semantic_precision:.2f}% (Meta de Tesis >= 90%)")
    print("--------------------------------------------------------------------------------")
    print(f"Coherencia Semántica Media (S)                 : {avg_s:.2f}")
    print(f"Latencia Media de Procesamiento                : {avg_latency:.2f} ms (Meta < 1000 ms)")
    print("================================================================================")

    # Desglose por Materia
    print("\n-------------------------- RENDIMIENTO POR ASIGNATURA --------------------------")
    print(f"{'Materia':<12} | {'WER Crudo':<10} | {'WER Difuso':<11} | {'KER Crudo':<10} | {'KER Difuso':<11} | {'Prec Sem':<9} | {'S Prom'}")
    print("-" * 80)
    for sub, stats in subject_stats.items():
        sub_raw_wer = (stats["raw_err"] / stats["ref_words"]) * 100.0 if stats["ref_words"] > 0 else 0.0
        sub_fuz_wer = (stats["fuzzy_err"] / stats["ref_words"]) * 100.0 if stats["ref_words"] > 0 else 0.0
        sub_raw_ker = ((stats["ref_kw"] - stats["raw_kw_c"]) / stats["ref_kw"]) * 100.0 if stats["ref_kw"] > 0 else 0.0
        sub_fuz_ker = ((stats["ref_kw"] - stats["fuzzy_kw_c"]) / stats["ref_kw"]) * 100.0 if stats["ref_kw"] > 0 else 0.0
        sub_prec = (stats["fuzzy_kw_c"] / stats["ref_kw"]) * 100.0 if stats["ref_kw"] > 0 else 100.0
        sub_s = sum(stats["s_scores"]) / len(stats["s_scores"]) if stats["s_scores"] else 0.50
        print(f"{sub:<12} | {sub_raw_wer:>8.1f}% | {sub_fuz_wer:>9.1f}% | {sub_raw_ker:>8.1f}% | {sub_fuz_ker:>9.1f}% | {sub_prec:>7.1f}% | {sub_s:>6.2f}")
    print("================================================================================\n")

    # Guardar reporte persistente en Markdown para la tesis
    metrics = {
        "wer_raw": global_raw_wer, "wer_fuzzy": global_fuzzy_wer,
        "ker_raw": global_raw_ker, "ker_fuzzy": global_fuzzy_ker,
        "subs": total_substitutions_made, "s_mean": avg_s,
    }
    if not write_report:
        return metrics

    report_path = os.path.join(base_dir, "reporte_benchmark_wer_ker.md")
    with open(report_path, "w", encoding="utf-8") as f:
        f.write("# Reporte Experimental de Benchmarking: WER y KER (Modelo Tri-Variable Mamdani)\n\n")
        f.write("**Fecha de Ejecución:** Evaluación experimental automatizada\n")
        f.write("**Modelo:** Mamdani 27 reglas $(C, D, S)$ con Defuzzificación COG (100 pasos)\n\n")
        f.write("## 1. Resumen Global Consolidado\n\n")
        f.write("| Métrica | Valor |\n")
        f.write("|---|---|\n")
        f.write(f"| Total Palabras de Referencia ($N$) | {total_ref_words} |\n")
        f.write(f"| Total Palabras Clave Curriculares | {total_ref_keywords} |\n")
        f.write(f"| Total Sustituciones Silenciosas | {total_substitutions_made} |\n")
        f.write(f"| WER Promedio ASR Crudo (Línea Base) | {global_raw_wer:.2f}% |\n")
        f.write(f"| WER Promedio con Capa Difusa | {global_fuzzy_wer:.2f}% |\n")
        f.write(f"| **Reducción Relativa de WER ($\\Delta\\text{{WER}}_{{\\text{{rel}}}}$)** | **{global_rel_wer_reduction:.2f}%** |\n")
        f.write(f"| KER Promedio ASR Crudo (Pretest) | {global_raw_ker:.2f}% |\n")
        f.write(f"| KER Promedio con Capa Difusa (Postest) | {global_fuzzy_ker:.2f}% |\n")
        f.write(f"| **Reducción Relativa de KER ($\\Delta\\text{{KER}}_{{\\text{{rel}}}}$)** | **{global_rel_ker_reduction:.2f}%** |\n")
        f.write(f"| **Precisión Semántica Final ($1 - \\text{{KER}}$)** | **{global_semantic_precision:.2f}%** |\n")
        f.write(f"| Coherencia Semántica Media ($S$) | {avg_s:.2f} |\n")
        f.write(f"| Latencia Media de Procesamiento | {avg_latency:.2f} ms |\n\n")
        f.write("## 2. Rendimiento Discriminado por Asignatura\n\n")
        f.write("| Asignatura | WER Crudo | WER Difuso | KER Crudo | KER Difuso | Precisión Semántica | S Promedio |\n")
        f.write("|---|---|---|---|---|---|---|\n")
        for sub, stats in subject_stats.items():
            sub_raw_wer = (stats["raw_err"] / stats["ref_words"]) * 100.0 if stats["ref_words"] > 0 else 0.0
            sub_fuz_wer = (stats["fuzzy_err"] / stats["ref_words"]) * 100.0 if stats["ref_words"] > 0 else 0.0
            sub_raw_ker = ((stats["ref_kw"] - stats["raw_kw_c"]) / stats["ref_kw"]) * 100.0 if stats["ref_kw"] > 0 else 0.0
            sub_fuz_ker = ((stats["ref_kw"] - stats["fuzzy_kw_c"]) / stats["ref_kw"]) * 100.0 if stats["ref_kw"] > 0 else 0.0
            sub_prec = (stats["fuzzy_kw_c"] / stats["ref_kw"]) * 100.0 if stats["ref_kw"] > 0 else 100.0
            sub_s = sum(stats["s_scores"]) / len(stats["s_scores"]) if stats["s_scores"] else 0.50
            f.write(f"| {sub} | {sub_raw_wer:.1f}% | {sub_fuz_wer:.1f}% | {sub_raw_ker:.1f}% | {sub_fuz_ker:.1f}% | {sub_prec:.1f}% | {sub_s:.2f} |\n")

    print(f"Reporte de benchmark exportado con éxito a: {report_path}\n")
    return metrics

def run_ablation():
    """Compara variantes del factor S / selección de candidato. Todas usan el modelo trivariado (C, D, S)."""
    import contextlib, io
    variants = (
        ("S solo oración (tesis)", dict(use_memory=False, use_rerank=False)),
        ("+ memoria de clase", dict(use_memory=True, use_rerank=False)),
        ("+ memoria + rerank", dict(use_memory=True, use_rerank=True)),
    )
    print("\nABLACIÓN DEL FACTOR SEMÁNTICO Y LA SELECCIÓN DE CANDIDATO (modelo trivariado en todas las variantes)")
    print(f"{'Corpus':<28} {'Variante':<24} {'WER dif':>8} {'KER dif':>8} {'Sustit.':>8} {'S medio':>8}")
    for corpus in ("corpus_arequipa_test.json", "corpus_clase_continua.json"):
        for name, kw in variants:
            with contextlib.redirect_stdout(io.StringIO()):
                m = run_benchmarks(corpus_name=corpus, write_report=False, **kw)
            print(f"{corpus:<28} {name:<24} {m['wer_fuzzy']:>7.2f}% {m['ker_fuzzy']:>7.2f}% {m['subs']:>8} {m['s_mean']:>8.2f}")
        print(f"{'':<28} {'(ASR crudo)':<24} {m['wer_raw']:>7.2f}% {m['ker_raw']:>7.2f}%")

def run_crisp_baseline():
    """Compara el sistema difuso contra la MEJOR regla booleana (D >= tD y C <= tC) de una malla de umbrales.
    Se hace en dos escenarios: confianzas sintéticas informativas (favorecen a la regla booleana, porque el corpus
    marca bajo C justo en las palabras erradas) y confianza neutra 0.65 (lo que entrega Android en la práctica)."""
    import contextlib, io
    corpora = ("corpus_arequipa_test.json", "corpus_clase_continua.json", "corpus_control_cotidiano.json")

    def measure(**kw):
        out = {}
        for c in corpora:
            with contextlib.redirect_stdout(io.StringIO()):
                out[c] = run_benchmarks(corpus_name=c, write_report=False, **kw)
        return out

    def score(m):  # minimizar WER total; cada falso positivo del control pesa como 5 puntos
        return sum(m[c]["wer_fuzzy"] for c in corpora) + 5 * m[corpora[2]]["subs"]

    def row(name, m):
        print(f"{name:<44} {m[corpora[0]]['wer_fuzzy']:>12.2f}% {m[corpora[1]]['wer_fuzzy']:>12.2f}% {m[corpora[2]]['wer_fuzzy']:>11.2f}% {m[corpora[2]]['subs']:>11}")

    for titulo, fc in (("A) Confianzas sintéticas informativas (favorece a la booleana)", None),
                       ("B) Confianza neutra 0.65 en todas las palabras (caso realista)", 0.65)):
        print(f"\n{titulo}")
        print(f"{'Sistema':<44} {'WER arequipa':>13} {'WER continua':>13} {'WER control':>12} {'FP control':>11}")
        best = None
        for td in (0.60, 0.70, 0.75, 0.80, 0.85, 0.90):
            for tc in ((0.50, 0.60, 0.70, 0.80, 1.00) if fc is None else (1.00,)):
                m = measure(crisp=(td, tc), fixed_conf=fc)
                if best is None or score(m) < score(best[2]):
                    best = (td, tc, m)
        raw = measure(crisp=(2.0, 0.0), fixed_conf=fc)
        row("ASR crudo (sin corrección)", {c: dict(v, wer_fuzzy=v["wer_raw"], subs=0) for c, v in raw.items()})
        row(f"Booleana óptima (D>={best[0]:.2f}, C<={best[1]:.2f})", best[2])
        row("Difusa (C,D,S) solo oración", measure(use_memory=False, use_rerank=False, fixed_conf=fc))
        row("Difusa (C,D,S) + memoria + rerank", measure(fixed_conf=fc))

def run_threshold_sweep():
    """Sensibilidad al umbral de decisión y* (modelo completo: memoria + rerank)."""
    import contextlib, io, sys
    global DECISION_THRESHOLD
    original = DECISION_THRESHOLD
    print("\nSENSIBILIDAD AL UMBRAL DE DECISIÓN y*")
    print(f"{'Umbral':>7} | {'Corpus':<28} | {'WER dif':>8} {'KER dif':>8} {'Sustit.':>8}")
    for th in (0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80):
        DECISION_THRESHOLD = th
        for corpus in ("corpus_arequipa_test.json", "corpus_clase_continua.json", "corpus_control_cotidiano.json"):
            with contextlib.redirect_stdout(io.StringIO()):
                m = run_benchmarks(corpus_name=corpus, write_report=False)
            extra = "  <- falsos positivos" if corpus.startswith("corpus_control") else ""
            print(f"{th:>7.2f} | {corpus:<28} | {m['wer_fuzzy']:>7.2f}% {m['ker_fuzzy']:>7.2f}% {m['subs']:>8}{extra}")
    DECISION_THRESHOLD = original

def run_vosk_evaluation(write_report=True):
    """Capa difusa con salida REAL de Vosk: palabra a palabra (antes) vs. uniones de palabras partidas + C neutra
    para términos fuera del vocabulario de Vosk (ahora). Desarrollo = se usó para ajustar; prueba = no se miró hasta
    el final; controles = cualquier sustitución es una falsa corrección."""
    import contextlib
    import io
    base = os.path.dirname(os.path.abspath(__file__))
    with open(os.path.join(base, "vosk_fase0", "oov_terminos.json"), encoding="utf-8") as f:
        oov = set(json.load(f))
    filas = [
        ("TTS + Vosk, desarrollo", "corpus_vosk_tts_desarrollo.json"),
        ("TTS + Vosk, prueba (no usado para ajustar)", "corpus_vosk_tts_prueba.json"),
        ("Control: OpenSLR 73 + Vosk (cotidiano)", "corpus_vosk_slr73_control.json"),
        ("Control: clase correcta con C alta", "corpus_control_clase_c_alta.json"),
    ]
    variantes = [("Palabra a palabra (antes)", {"span_mode": None}),
                 ("Uniones + C neutra en uniones (ahora)", {"span_mode": SPAN_MODE, "asr_oov": oov})]
    resultados = []
    for etiqueta, corpus in filas:
        for nombre, kw in variantes:
            with contextlib.redirect_stdout(io.StringIO()):
                m = run_benchmarks(corpus_name=os.path.join(base, corpus), write_report=False, **kw)
            resultados.append((etiqueta, nombre, m))
    cab = f"{'Corpus':44} {'Variante':38} {'WER crudo':>9} {'WER dif':>8} {'KER crudo':>9} {'KER dif':>8} {'Sustit.':>7}"
    print(cab)
    print("-" * len(cab))
    for etiqueta, nombre, m in resultados:
        print(f"{etiqueta:44} {nombre:38} {m['wer_raw']:8.2f}% {m['wer_fuzzy']:7.2f}% {m['ker_raw']:8.2f}% {m['ker_fuzzy']:7.2f}% {m['subs']:7}")
    if not write_report:
        return resultados
    ruta = os.path.join(base, "reporte_vosk_palabras_partidas.md")
    with open(ruta, "w", encoding="utf-8") as f:
        f.write("# Capa difusa con salida real de Vosk: términos partidos\n\n")
        with open(os.path.join(base, "..", "app", "src", "main", "assets", "dictionaries", "biologia.json"),
                  encoding="utf-8") as fd:
            n_terminos = len(json.load(fd))
        f.write(f"Vosk (`vosk-model-small-es-0.42`) no puede escribir {len(oov)} de los {n_terminos} términos del "
                f"diccionario de Ciencia y Tecnología ({100 * len(oov) / n_terminos:.0f} %) y los parte en varias palabras (\"sito plasma\" -> citoplasma). La capa difusa evalúa ahora "
                "uniones de 2-3 palabras seguidas (D >= 0.80 y mejor que sus pedazos) y, si el término no está en el "
                "vocabulario de Vosk, usa C neutra (0.65) en la unión. Las 27 reglas Mamdani no cambian.\n\n")
        f.write("| Corpus | Variante | WER crudo | WER difuso | KER crudo | KER difuso | Sustituciones |\n|---|---|---|---|---|---|---|\n")
        for etiqueta, nombre, m in resultados:
            f.write(f"| {etiqueta} | {nombre} | {m['wer_raw']:.2f} % | {m['wer_fuzzy']:.2f} % | {m['ker_raw']:.2f} % | "
                    f"{m['ker_fuzzy']:.2f} % | {m['subs']} |\n")
        f.write("\nCorpus TTS: frases de clase sintetizadas con Piper (voces es_MX-ald, es_MX-claude, es_AR-daniela) y "
                "transcritas por Vosk (`vosk_fase0/generar_corpus_tts.py`). La voz sintética es más limpia que un docente "
                "en aula (WER optimista); los errores por vocabulario sí son representativos. Control OpenSLR 73: "
                "Guevara-Rukoz et al. (2020), CC BY-SA 4.0.\n\n"
                "Nota: «Sustituciones» del benchmark cuenta también palabras reemplazadas por sí mismas (p. ej. "
                "«núcleo»->«núcleo»); la app no las cuenta. WER y KER no se ven afectados.\n\n"
                "## Alternativas evaluadas y descartadas\n\n"
                "| Alternativa | Resultado | Motivo del descarte |\n|---|---|---|\n"
                "| C neutra (0.65) también en palabras sueltas cuyo término no está en el vocabulario de Vosk | "
                "6 falsas correcciones en el control OpenSLR 73 («saber»->«suber», «manera»->«monera», "
                "«planta»->«plantae», «aplicación»->«replicación») | Una palabra entera escrita con C alta sí es "
                "evidencia acústica; la C neutra solo se justifica cuando el ASR partió la palabra |\n"
                "| Unir aunque un pedazo ya sea el término completo («la fotosintesis») | Absorbía el artículo "
                "vecino (arequipa: WER 1.94 % -> 3.88 %) | El término bien escrito debe ser parte PROPIA del unido "
                "(«plasma» ⊂ «citoplasma») |\n"
                "| Regla 19 (C Alta, D Alta, S Alta) con consecuente Alta en vez de Media, para corregir «cédula» "
                "con C alta | Desarrollo: KER 45.8 % -> 43.2 %, pero 5 de 18 frases correctas del control de clase "
                "cambiaron «celular» por «célula» | Falsas correcciones en lenguaje de clase correcto; se mantienen "
                "las 27 reglas originales |\n")
    print(f"\nReporte: {ruta}")
    return resultados


if __name__ == "__main__":
    import sys
    if "--booleana" in sys.argv:
        run_crisp_baseline()
    elif "--umbral" in sys.argv:
        run_threshold_sweep()
    elif "--ablacion" in sys.argv:
        run_ablation()
    elif "--vosk" in sys.argv:
        run_vosk_evaluation()
    else:
        run_benchmarks(use_memory="--sin-memoria" not in sys.argv, use_rerank="--sin-rerank" not in sys.argv)
