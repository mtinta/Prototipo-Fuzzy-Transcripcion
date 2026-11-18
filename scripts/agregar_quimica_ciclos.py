#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Agrega al diccionario del área Ciencia y Tecnología los términos de química inorgánica, química orgánica y ciclos
biogeoquímicos de secundaria (CNEB, 3.º a 5.º), con sus áreas temáticas (clusters) para el factor S.

Por qué en biologia.json: el código interno "BIOLOGIA" es el del área completa (CaptionsViewModel.SUBJECT_CODE); la
app, las pruebas, la paridad Kotlin<->Python, el benchmark y la lista OOV de Vosk leen ese archivo. Cada término
nuevo queda identificado por su cluster (macro_id quimica_inorganica, quimica_organica o botanica_y_ecologia).

Criterios para entrar a la lista (evitan falsas correcciones):
  - Una sola palabra (las uniones de palabras partidas por Vosk ya cubren "hidro carburo" -> hidrocarburo).
  - Al menos 5 letras: las palabras de menos de 4 no se evalúan y un término corto atrae palabras comunes
    ("sala" -> "sal").
  - Sin vecinos comunes: se excluyeron términos cuya similitud D con una palabra frecuente del español es >= 0.70
    (ver EXCLUIDOS). Revisión: scripts/revisar_vecinos_terminos.py.

Idempotente: no duplica términos ni clusters ya presentes. Ejecutar desde la raíz del repositorio:
  python3 scripts/agregar_quimica_ciclos.py
Después: regenerar la lista OOV (evaluation/vosk_fase0/generar_oov.py) y la paridad (generar_paridad_decision.py).
"""
import json
import os
import re
import unicodedata

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DICCIONARIO = os.path.join(RAIZ, "app", "src", "main", "assets", "dictionaries", "biologia.json")
CLUSTERS = os.path.join(RAIZ, "app", "src", "main", "assets", "clusters", "biologia_clusters.json")

# (topic_id, topic_name, macro_id, términos). Un término puede estar en varios clusters.
# Los términos que YA existen en biologia.json (p. ej. fosfato, eutrofización) solo se añaden al cluster.
NUEVOS_CLUSTERS = [
    ("materia_atomo_tabla_periodica", "Materia, Átomo y Tabla Periódica", "quimica_inorganica", [
        "átomo", "protón", "neutrón", "electrones", "atómico", "isótopo", "isóbaro", "isótono", "orbital", "subnivel",
        "cuántico", "cuántica", "configuración", "electronegatividad", "electroafinidad", "ionización",
        "metaloide", "halógeno", "alcalino", "alcalinotérreo", "lantánido", "actínido", "elemento",
        "hidrógeno", "helio", "litio", "berilio", "carbono", "nitrógeno", "oxígeno", "flúor",
        "magnesio", "aluminio", "silicio", "fósforo", "azufre", "argón", "potasio", "calcio", "hierro",
        "manganeso", "níquel", "cobalto", "mercurio", "uranio", "zinc", "molécula", "molecular", "sustancia",
        "homogénea", "heterogénea", "decantación", "filtración", "destilación", "tamizado",
    ]),
    ("enlace_quimico", "Enlace Químico y Estructura Molecular", "quimica_inorganica", [
        "enlace", "covalente", "iónico", "metálico", "electrovalente", "polar", "apolar", "dipolo", "catión", "anión",
        "octeto", "lewis", "valencia", "electronegatividad", "molécula", "molecular", "intermolecular",
        "electrones", "orbital", "órbita",
    ]),
    ("funciones_inorganicas", "Funciones Químicas Inorgánicas y Nomenclatura", "quimica_inorganica", [
        "óxido", "hidróxido", "peróxido", "hidruro", "anhídrido", "oxácido", "hidrácido", "oxisal",
        "haloidea", "nomenclatura", "atomicidad", "ácido", "sulfato", "nitrato", "nitrito", "carbonato",
        "bicarbonato", "cloruro", "sulfuro", "fosfato", "hipoclorito", "permanganato", "amoníaco", "amonio",
        "oxidación", "valencia", "metaloide", "anión", "catión",
    ]),
    ("reacciones_estequiometria", "Reacciones Químicas y Estequiometría", "quimica_inorganica", [
        "reacción", "reactante", "reactivo", "descomposición", "desplazamiento", "combustión", "neutralización",
        "precipitación", "precipitado", "oxidación", "reducción", "redox", "oxidante", "reductor",
        "catalizador", "endotérmica", "endotérmico", "exotérmica", "exotérmico", "entalpía", "balanceo", "balance", "coeficiente", "estequiometría",
        "molaridad", "avogadro", "limitante", "rendimiento", "lavoisier",
    ]),
    ("soluciones_acidos_bases", "Soluciones, Ácidos y Bases", "quimica_inorganica", [
        "solución", "soluto", "solvente", "disolución", "concentración", "saturada", "insaturada",
        "sobresaturada", "solubilidad", "indicador", "fenolftaleína", "tornasol", "acidez", "alcalinidad",
        "alcalino", "electrolito", "disociación", "hidronio", "hidroxilo", "arrhenius",
        "neutralización", "ácido", "hidróxido", "molaridad",
    ]),
    ("carbono_hidrocarburos", "El Carbono e Hidrocarburos", "quimica_organica", [
        "carbono", "carbón", "tetravalencia", "tetravalente", "hibridación", "covalente", "cuaternario", "alótropo", "grafito",
        "diamante", "fullereno", "isómero", "isomería", "hidrocarburo", "alcano", "alqueno", "alquino",
        "cicloalcano", "aromático", "benceno", "tolueno", "naftaleno", "metano", "etano", "propano",
        "butano", "pentano", "hexano", "heptano", "octano", "eteno", "etileno", "acetileno", "propeno",
        "metilo", "etilo", "alquilo", "saturado", "insaturado", "parafina", "petróleo", "gasolina",
        "octanaje", "craqueo", "destilación", "combustión", "kekulé",
    ]),
    ("funciones_organicas", "Funciones Orgánicas Oxigenadas y Nitrogenadas", "quimica_organica", [
        "alcohol", "aldehído", "cetona", "carboxílico", "etanol", "metanol", "propanol", "fenol",
        "formaldehído", "acetona", "glicerol", "glicerina", "hidroxilo", "carbonilo", "carboxilo",
        "esterificación", "saponificación", "fermentación", "nitrilo", "urea", "anilina", "aminoácido",
        "grupo", "isómero",
    ]),
    ("polimeros_biomoleculas_organicas", "Polímeros y Compuestos Orgánicos de Uso Diario", "quimica_organica", [
        "polímero", "monómero", "polimerización", "polietileno", "nailon", "caucho", "plástico",
        "biodegradable", "combustible", "petróleo", "glucosa", "celulosa", "almidón", "proteína", "lípido",
        "triglicérido", "saponificación", "detergente",
    ]),
    ("ciclos_biogeoquimicos", "Ciclos Biogeoquímicos", "botanica_y_ecologia", [
        "biogeoquímico", "hidrológico", "evaporación", "condensación", "precipitación", "infiltración",
        "escorrentía", "transpiración", "evapotranspiración", "sublimación", "percolación", "acuífero",
        "nitrificación", "desnitrificación", "amonificación", "fijación", "nitrogenasa", "rhizobium",
        "leguminosa", "nódulo", "nitrato", "nitrito", "amonio", "amoníaco", "reservorio", "sedimentación",
        "meteorización", "erosión", "combustión", "combustible", "fósil", "carbón", "dióxido", "monóxido", "carbonato",
        "sulfato", "fosfato", "fosforita", "guano", "eutrofización", "invernadero", "calentamiento",
        "biosfera", "atmósfera", "hidrosfera", "litosfera", "mineralización", "descomposición",
        "descomponedor", "gusano", "fotosíntesis", "respiración", "quimiosíntesis", "carbono", "nitrógeno", "oxígeno",
        "fósforo", "azufre", "ozono", "productor", "consumidor", "biomasa",
    ]),
]

# Candidatos descartados y por qué (vecino común con D >= 0.70, o demasiado cortos). No volver a añadirlos sin
# medir falsas correcciones en los controles.
EXCLUIDOS = {
    "funcional": "vecino 'funciona' (D=0.89, verbo muy frecuente)",
    "mezcla": "vecino 'mezclar' (D=0.86, verbo de uso en clase)",
    "titulación": "vecino 'situación' (D=0.80, muy frecuente) y poco usado en secundaria",
    "molalidad": "vecino 'moralidad' (D=0.89) y poco usado en secundaria",
    "sal": "menos de 5 letras ('sala' -> 'sal')", "gas": "menos de 5 letras", "ion": "menos de 5 letras",
    "mol": "menos de 5 letras", "base": "menos de 5 letras ('pase', 'fase')", "yodo": "vecino 'todo'",
    "cloro": "vecino 'claro' (D=0.80)", "cobre": "vecino 'sobre'", "plata": "vecino 'planta'",
    "cromo": "vecino 'como'", "plomo": "vecino 'plano'", "boro": "vecino 'toro', 'coro'",
    "estaño": "vecino 'extraño'", "éster": "vecino 'este'", "éter": "vecino 'éste'",
    "sodio": "'odio' -> 'sodio' en el control OpenSLR 73 con C de Vosk (17/11)",
    "periodicidad": "'periódica' -> 'periodicidad' con C 0.65; poco usado en secundaria",
    "amina": "vecino 'lámina'", "amida": "vecino 'amiga'", "ciclo": "vecino 'cielo' (clase del ciclo del agua)",
}

# Palabras científicas que se agregan como término para PROTEGERLAS: una palabra idéntica a un término nunca se
# sustituye. Sin ellas, "carbón" podía acabar en "carbono", "órbita" en "orbital" o "flor" en "flúor".
PROTECTORES = {"carbón", "molecular", "órbita", "polar", "cuántica", "balance", "flor", "gusano", "reacción",
               "atómico"}
# "flor" es de botánica: va también al cluster existente de tejidos vegetales.
CLUSTER_DE_FLOR = "botanica_tejidos"

ADJETIVOS = {
    "cuántico", "homogénea", "heterogénea", "covalente", "iónico", "metálico", "electrovalente", "apolar",
    "intermolecular", "atómico", "tetravalente", "endotérmica", "exotérmica", "saturada", "insaturada", "sobresaturada", "alcalino",
    "alcalinotérreo", "cuaternario", "aromático", "saturado", "insaturado", "carboxílico", "funcional",
    "biodegradable", "biogeoquímico", "hidrológico", "haloidea", "limitante", "reductor", "oxidante",
    "cuántica", "molecular", "polar", "endotérmico", "exotérmico",
}
NOMBRES_PROPIOS = {"lewis", "avogadro", "lavoisier", "arrhenius", "kekulé", "rhizobium"}


def sin_tildes(texto):
    return "".join(c for c in unicodedata.normalize("NFD", texto) if not unicodedata.combining(c))


def fonetica(texto):
    """Mismo formato que el campo phonetic de biologia.json (informativo: el matcher calcula la suya)."""
    s = sin_tildes(texto.lower().strip()).replace("h", "").replace("v", "b")
    s = s.replace("ce", "se").replace("ci", "si").replace("z", "s").replace("ll", "y")
    s = s.replace("qu", "k").replace("c", "k").replace("x", "ks")
    return re.sub(r"\s+", "", s)


def main():
    with open(DICCIONARIO, encoding="utf-8") as f:
        diccionario = json.load(f)
    with open(CLUSTERS, encoding="utf-8") as f:
        clusters = json.load(f)

    existentes = {d["term"].lower() for d in diccionario}
    nuevos = []
    for _, _, _, terminos in NUEVOS_CLUSTERS:
        for t in terminos:
            if t in EXCLUIDOS:
                raise SystemExit(f"'{t}' está en EXCLUIDOS: {EXCLUIDOS[t]}")
            if t.lower() in existentes:
                continue
            existentes.add(t.lower())
            categoria = "PROPN" if t in NOMBRES_PROPIOS else ("ADJ" if t in ADJETIVOS else "NOUN")
            nuevos.append({"term": t, "lemma": t, "subject": "BIOLOGIA", "category": categoria,
                           "phonetic": fonetica(t)})
    if "flor" not in existentes:
        existentes.add("flor")
        nuevos.append({"term": "flor", "lemma": "flor", "subject": "BIOLOGIA", "category": "NOUN",
                       "phonetic": fonetica("flor")})
    faltan = PROTECTORES - existentes
    if faltan:
        raise SystemExit(f"Faltan términos protectores en NUEVOS_CLUSTERS: {sorted(faltan)}")
    # Al final y sin reordenar: ante un empate de D gana el primer término de la lista, y así los términos
    # existentes conservan sus desempates.
    diccionario.extend(nuevos)

    por_id = {c["topic_id"]: c for c in clusters}
    flor = por_id.get(CLUSTER_DE_FLOR)
    if flor is not None and "flor" not in flor["terms"]:
        flor["terms"].append("flor")
    for topic_id, nombre, macro, terminos in NUEVOS_CLUSTERS:
        unicos = list(dict.fromkeys(terminos))
        if topic_id in por_id:
            por_id[topic_id].update(topic_name=nombre, macro_id=macro, terms=unicos)
        else:
            clusters.append({"topic_id": topic_id, "topic_name": nombre, "macro_id": macro, "terms": unicos})

    with open(DICCIONARIO, "w", encoding="utf-8") as f:
        json.dump(diccionario, f, ensure_ascii=False, indent=2)
    with open(CLUSTERS, "w", encoding="utf-8") as f:
        json.dump(clusters, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"{len(nuevos)} términos nuevos; diccionario: {len(diccionario)} términos; clusters: {len(clusters)}")


if __name__ == "__main__":
    main()
