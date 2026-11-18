#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Amplía el diccionario de CyT con NOMENCLATURA química (inorgánica y orgánica), geometría molecular y UNIDADES de
medida (18/11/2026, pedido tras el piloto: "alqueno", "insaturado", "enlaces covalentes", "oxiacetilénico",
"trigonal", "kilojoule"). Complementa a agregar_quimica_ciclos.py y agregar_cyt_primero_tercero.py, que ya traían
ácido, anhídrido, óxido, alcano, alqueno, alquino, alcohol, aldehído, cetona, benceno, joule, pascal, newton...

Todos los términos van a clusters EXISTENTES (AMPLIAR_CLUSTERS): así co-ocurren con los términos que ya estaban
(ácido + sulfúrico, alcano + ciclohexano, energía + kilojoule) y suman al factor S. No se crea ningún cluster y no
se repite ningún término que ya tuviera cluster (regla medida en agregar_cyt_primero_tercero.py).

Mismos criterios para entrar: una sola palabra de al menos 5 letras y sin vecinos comunes peligrosos
(scripts/revisar_vecinos_terminos.py --modulo agregar_quimica_nomenclatura_unidades; EXCLUIDOS dice por qué).

ORDEN: correr DESPUÉS de agregar_quimica_ciclos.py y agregar_cyt_primero_tercero.py (esos reescriben sus clusters;
si se vuelven a correr, hay que correr este otra vez). Idempotente.
  python3 scripts/agregar_quimica_nomenclatura_unidades.py
Después: regenerar la lista OOV (evaluation/vosk_fase0/generar_oov.py) y la paridad (generar_paridad_decision.py).
"""
import json
import os
import re
import unicodedata

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICCIONARIO = os.path.join(RAIZ, "app", "src", "main", "assets", "dictionaries", "biologia.json")
CLUSTERS = os.path.join(RAIZ, "app", "src", "main", "assets", "clusters", "biologia_clusters.json")

# Ningún cluster nuevo (lo usa revisar_vecinos_terminos.py).
NUEVOS_CLUSTERS = []

AMPLIAR_CLUSTERS = {
    # Nomenclatura inorgánica: ácidos por nombre, estados de oxidación (-oso / -ico) y sales.
    "funciones_inorganicas": [
        "sulfúrico", "sulfuroso", "nítrico", "nitroso", "clorhídrico", "carbónico", "fosfórico", "hipocloroso",
        "cloroso", "clórico", "perclórico", "bromhídrico", "yodhídrico", "sulfhídrico", "fluorhídrico",
        "ferroso", "férrico", "cuproso", "cúprico", "mercúrico", "sódico", "potásico", "cálcico",
        "clorato", "perclorato", "clorito", "sulfito", "bromuro", "yoduro", "fluoruro", "carburo",
        "binario", "ternario",
    ],
    # Hidrocarburos: más alcanos/alquenos/alquinos, cíclicos, radicales y derivados del acetileno.
    "carbono_hidrocarburos": [
        "nonano", "decano", "buteno", "penteno", "hexeno", "etino", "propino", "butino",
        "ciclopropano", "ciclobutano", "ciclopentano", "ciclohexano", "alifático", "acíclico", "cíclico",
        "ramificado", "ramificada", "ramificación", "propilo", "butilo", "isopropilo", "vinilo", "fenilo",
        "isooctano", "queroseno", "alquitrán", "acetilénico", "acetilénica", "oxiacetilénico", "oxiacetilénica",
    ],
    # Funciones oxigenadas por nombre IUPAC y común, ácidos orgánicos y reacciones orgánicas.
    "funciones_organicas": [
        "metanal", "etanal", "propanal", "butanal", "propanona", "butanona", "metanoico", "etanoico",
        "propanoico", "butanoico", "acético", "fórmico", "láctico", "cítrico", "butírico", "oleico",
        "palmítico", "esteárico", "acetato", "etílico", "metílico", "alcohólico", "haluro", "halogenuro",
        "hidrogenación", "halogenación", "deshidratación", "hidrólisis", "pirólisis", "vulcanización",
    ],
    # Geometría molecular (con hibridación, que ya estaba en carbono_hidrocarburos).
    "enlace_quimico": ["trigonal", "tetraédrico", "tetraédrica", "piramidal", "geometría"],
    # Unidades de medida (joule, pascal, newton, celsius, hertz, ohmio, caloría y kilovatio ya estaban).
    "trabajo_energia": ["kilojoule", "kilocaloría", "megajoule"],
    "calor_temperatura": ["kelvin"],
    "electricidad_magnetismo": ["voltio", "amperio", "amperímetro", "voltímetro"],
    "presion_fluidos": ["hectopascal", "kilopascal", "milibar"],
    "indagacion_cientifica": ["kilogramo", "miligramo", "mililitro", "centímetro", "milímetro", "decímetro",
                              "micrómetro", "nanómetro", "centímetros"],
}

# Candidatos descartados y por qué. No volver a añadirlos sin medir falsas correcciones en los controles.
EXCLUIDOS = {
    "julio": "es el mes (muy frecuente); la unidad se agrega como 'joule' y 'kilojoule'",
    "kilojulio": "duplicaría a 'kilojoule' (en el aula se dice 'kilojoule'); dos formas reparten el cluster",
    "metil": "Vosk lo escribe 'metí' (verbo); se deja 'metilo', que ya estaba",
    "amino": "vecino 'camino'", "éster": "vecino 'este'", "éter": "vecino 'éste'",
    "lineal": "palabra común (no es solo geometría)", "angular": "palabra común",
    "gramo": "vecino 'grano', 'gramo' ya lo escribe Vosk", "litro": "vecino 'libro'", "metro": "palabra común",
    # Revisión de vecinos (es_50k, top 20 000), misma regla que agregar_cyt_primero_tercero.py: fuera si una palabra
    # entre las 5000 más frecuentes tiene D >= 0.80 con el término. Vosk escribe bien la mayoría de estas.
    "buteno": "vecino 'bueno' (#50, D=0.83)", "propino": "vecino 'propio' (#770, D=0.86)",
    "propilo": "vecino 'propio' (#770, D=0.86)", "voltio": "vecino 'volvió' (#1238, D=0.83)",
    "kelvin": "vecino 'kevin' (#1822, D=0.83; nombre de alumnos)", "milibar": "vecino 'militar' (#1920, D=0.86)",
    "amperio": "vecino 'imperio' (#3513, D=0.86)",
    # Criterio escolar (fuera del top 5000, pero frecuentes en un colegio o con flexión propia):
    "ramificación": "vecino 'calificación' (D=0.83): palabra diaria en el colegio",
    "alcohólico": "'alcohólica' (D=0.90) acabaría cambiada a masculino",
    "cíclico": "'el ciclo' -> 'el cíclico' en la prueba del 18/11 (ciclo del agua, ciclo celular: palabra diaria)",
}

ADJETIVOS = {
    "sulfúrico", "sulfuroso", "nítrico", "nitroso", "clorhídrico", "carbónico", "fosfórico", "hipocloroso", "cloroso",
    "clórico", "perclórico", "bromhídrico", "yodhídrico", "sulfhídrico", "fluorhídrico", "ferroso", "férrico",
    "cuproso", "cúprico", "mercúrico", "sódico", "potásico", "cálcico", "binario", "ternario", "alifático",
    "acíclico", "cíclico", "ramificado", "ramificada", "acetilénico", "acetilénica", "oxiacetilénico",
    "oxiacetilénica", "metanoico", "etanoico", "propanoico", "butanoico", "acético", "fórmico", "láctico",
    "cítrico", "butírico", "oleico", "palmítico", "esteárico", "etílico", "metílico", "alcohólico", "trigonal",
    "tetraédrico", "tetraédrica", "piramidal",
}
NOMBRES_PROPIOS = {"kelvin"}


def sin_tildes(texto):
    return "".join(c for c in unicodedata.normalize("NFD", texto) if not unicodedata.combining(c))


def fonetica(texto):
    """Mismo formato que el campo phonetic de biologia.json (informativo: el matcher calcula la suya)."""
    s = sin_tildes(texto.lower().strip()).replace("h", "").replace("v", "b")
    s = s.replace("ce", "se").replace("ci", "si").replace("z", "s").replace("ll", "y")
    s = s.replace("qu", "k").replace("c", "k").replace("x", "ks")
    return re.sub(r"\s+", "", s)


def candidatos():
    return list(dict.fromkeys(t for ts in AMPLIAR_CLUSTERS.values() for t in ts))


def main():
    with open(DICCIONARIO, encoding="utf-8") as f:
        diccionario = json.load(f)
    with open(CLUSTERS, encoding="utf-8") as f:
        clusters = json.load(f)

    por_id = {c["topic_id"]: c for c in clusters}
    for topic_id in AMPLIAR_CLUSTERS:
        if topic_id not in por_id:
            raise SystemExit(f"AMPLIAR_CLUSTERS: no existe el cluster '{topic_id}'")

    # Términos con cluster ANTES de este script: no se repiten (regla de agregar_cyt_primero_tercero.py).
    propios = set(candidatos())
    ya_agrupados = {t for c in clusters for t in c["terms"]} - propios

    existentes = {d["term"].lower() for d in diccionario}
    nuevos = []
    for t in candidatos():
        if t in EXCLUIDOS:
            continue
        if len(sin_tildes(t)) < 5 or " " in t:
            raise SystemExit(f"'{t}': debe ser una sola palabra de al menos 5 letras")
        if t in ya_agrupados:
            raise SystemExit(f"'{t}' ya tiene cluster: no se repite")
        if t.lower() in existentes:
            continue
        existentes.add(t.lower())
        categoria = "PROPN" if t in NOMBRES_PROPIOS else ("ADJ" if t in ADJETIVOS else "NOUN")
        nuevos.append({"term": t, "lemma": t, "subject": "BIOLOGIA", "category": categoria, "phonetic": fonetica(t)})
    # Al final y sin reordenar: ante un empate de D gana el primer término, y los existentes conservan sus desempates.
    diccionario.extend(nuevos)

    for topic_id, extra in AMPLIAR_CLUSTERS.items():
        terms = por_id[topic_id]["terms"]
        terms.extend(t for t in extra if t not in terms and t not in EXCLUIDOS and t.lower() in existentes)

    with open(DICCIONARIO, "w", encoding="utf-8") as f:
        json.dump(diccionario, f, ensure_ascii=False, indent=2)
    with open(CLUSTERS, "w", encoding="utf-8") as f:
        json.dump(clusters, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"{len(nuevos)} términos nuevos; diccionario: {len(diccionario)} términos; clusters: {len(clusters)}")


if __name__ == "__main__":
    main()
