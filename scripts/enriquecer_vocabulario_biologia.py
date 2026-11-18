#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generador y Validador de Vocabulario Enriquecido de Biología
Alineado con el Prospecto CEPRUNSA / UNSA y CNEB (MINEDU).
"""

import json
import unicodedata
import re

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
    s = s.replace("x", "ks")
    s = re.sub(r'\s+', '', s)
    return s

def main():
    dict_file = "app/src/main/assets/dictionaries/biologia.json"
    clusters_file = "app/src/main/assets/clusters/biologia_clusters.json"

    with open(dict_file, "r", encoding="utf-8") as f:
        current_dict = json.load(f)

    with open(clusters_file, "r", encoding="utf-8") as f:
        current_clusters = json.load(f)

    existing_terms = {item["term"].lower().strip(): item for item in current_dict}
    print(f"[*] Términos base existentes en Biología: {len(existing_terms)}")

    # Nuevo vocabulario pedagógico estructurado por categorías de CNEB / CEPRUNSA
    # Formato: (term, lemma, category, [cluster_ids])
    new_terms_spec = [
        # 1. Citología General y Transporte
        ("bicapa", "bicapa", "NOUN", ["citologia_general"]),
        ("permeabilidad", "permeabilidad", "NOUN", ["citologia_general"]),
        ("turgencia", "turgencia", "NOUN", ["citologia_general"]),
        ("plasmólisis", "plasmólisis", "NOUN", ["citologia_general"]),
        ("endocitosis", "endocitosis", "NOUN", ["citologia_general"]),
        ("exocitosis", "exocitosis", "NOUN", ["citologia_general"]),
        ("fagocitosis", "fagocitosis", "NOUN", ["citologia_general", "sistema_circulatorio_inmune"]),
        ("pinocitosis", "pinocitosis", "NOUN", ["citologia_general"]),
        ("isotónico", "isotónico", "ADJ", ["citologia_general"]),
        ("hipertónico", "hipertónico", "ADJ", ["citologia_general"]),
        ("hipotónico", "hipotónico", "ADJ", ["citologia_general"]),
        ("microtúbulo", "microtúbulo", "NOUN", ["citologia_general", "division_celular"]),
        ("microfilamento", "microfilamento", "NOUN", ["citologia_general"]),
        ("filamento", "filamento", "NOUN", ["citologia_general"]),
        ("plasmodesmo", "plasmodesmo", "NOUN", ["citologia_general", "botanica_tejidos"]),

        # 2. Organelos y Estructuras Subcelulares
        ("rugoso", "rugoso", "ADJ", ["organelos_celulares"]),
        ("liso", "liso", "ADJ", ["organelos_celulares"]),
        ("crista", "crista", "NOUN", ["organelos_celulares", "metabolismo_bioenergetica"]),
        ("matriz", "matriz", "NOUN", ["organelos_celulares", "metabolismo_bioenergetica"]),
        ("dictiosoma", "dictiosoma", "NOUN", ["organelos_celulares"]),
        ("glioxisoma", "glioxisoma", "NOUN", ["organelos_celulares"]),
        ("centrosoma", "centrosoma", "NOUN", ["organelos_celulares", "division_celular"]),
        ("peptidoglicano", "peptidoglicano", "NOUN", ["organelos_celulares", "ecologia_evolucion_taxonomia"]),
        ("pili", "pili", "NOUN", ["organelos_celulares", "ecologia_evolucion_taxonomia"]),
        ("fimbria", "fimbria", "NOUN", ["organelos_celulares", "ecologia_evolucion_taxonomia"]),

        # 3. Genética Molecular y Ácidos Nucleicos
        ("polimerasa", "polimerasa", "NOUN", ["genetica_molecular", "acidos_nucleicos_sintesis"]),
        ("helicasa", "helicasa", "NOUN", ["genetica_molecular", "acidos_nucleicos_sintesis"]),
        ("ligasa", "ligasa", "NOUN", ["genetica_molecular", "acidos_nucleicos_sintesis"]),
        ("topoisomerasa", "topoisomerasa", "NOUN", ["genetica_molecular", "acidos_nucleicos_sintesis"]),
        ("cebador", "cebador", "NOUN", ["genetica_molecular", "acidos_nucleicos_sintesis"]),
        ("telómero", "telómero", "NOUN", ["genetica_molecular", "division_celular"]),
        ("telomerasa", "telomerasa", "NOUN", ["genetica_molecular"]),
        ("centrómero", "centrómero", "NOUN", ["genetica_molecular", "division_celular"]),
        ("cinetocoro", "cinetocoro", "NOUN", ["genetica_molecular", "division_celular"]),
        ("promotor", "promotor", "NOUN", ["acidos_nucleicos_sintesis"]),
        ("intrón", "intrón", "NOUN", ["acidos_nucleicos_sintesis"]),
        ("exón", "exón", "NOUN", ["acidos_nucleicos_sintesis"]),
        ("splicing", "splicing", "NOUN", ["acidos_nucleicos_sintesis"]),
        ("elongación", "elongación", "NOUN", ["acidos_nucleicos_sintesis"]),
        ("terminación", "terminación", "NOUN", ["acidos_nucleicos_sintesis"]),
        ("peptídico", "peptídico", "ADJ", ["acidos_nucleicos_sintesis", "metabolismo_bioenergetica"]),
        ("polipéptido", "polipéptido", "NOUN", ["acidos_nucleicos_sintesis", "metabolismo_bioenergetica"]),

        # 4. Genética Mendeliana y Herencia Humana
        ("mendel", "mendel", "NOUN", ["genetica_molecular"]),
        ("punnett", "punnett", "NOUN", ["genetica_molecular"]),
        ("monohíbrido", "monohíbrido", "NOUN", ["genetica_molecular"]),
        ("dihíbrido", "dihíbrido", "NOUN", ["genetica_molecular"]),
        ("codominancia", "codominancia", "NOUN", ["genetica_molecular"]),
        ("epistasia", "epistasia", "NOUN", ["genetica_molecular"]),
        ("ligamiento", "ligamiento", "NOUN", ["genetica_molecular"]),
        ("autosómico", "autosómico", "ADJ", ["genetica_molecular"]),
        ("gonosoma", "gonosoma", "NOUN", ["genetica_molecular"]),
        ("daltonismo", "daltonismo", "NOUN", ["genetica_molecular"]),
        ("hemofilia", "hemofilia", "NOUN", ["genetica_molecular"]),
        ("trisomía", "trisomía", "NOUN", ["genetica_molecular"]),
        ("mutágeno", "mutágeno", "NOUN", ["genetica_molecular"]),

        # 5. Ciclo Celular, Mitosis y Meiosis
        ("quiasma", "quiasma", "NOUN", ["division_celular"]),
        ("entrecruzamiento", "entrecruzamiento", "NOUN", ["division_celular"]),
        ("acromático", "acromático", "ADJ", ["division_celular"]),
        ("leptonema", "leptonema", "NOUN", ["division_celular"]),
        ("cigonema", "cigonema", "NOUN", ["division_celular"]),
        ("paquinema", "paquinema", "NOUN", ["division_celular"]),
        ("diplonema", "diplonema", "NOUN", ["division_celular"]),
        ("diacinesis", "diacinesis", "NOUN", ["division_celular"]),
        ("placa", "placa", "NOUN", ["division_celular"]),

        # 6. Reproducción y Embriología
        ("espermatogénesis", "espermatogénesis", "NOUN", ["reproduccion_desarrollo"]),
        ("ovogénesis", "ovogénesis", "NOUN", ["reproduccion_desarrollo"]),
        ("espermatogonia", "espermatogonia", "NOUN", ["reproduccion_desarrollo"]),
        ("ovogonia", "ovogonia", "NOUN", ["reproduccion_desarrollo"]),
        ("espermátida", "espermátida", "NOUN", ["reproduccion_desarrollo"]),
        ("acrosoma", "acrosoma", "NOUN", ["reproduccion_desarrollo"]),
        ("pelúcida", "pelúcida", "ADJ", ["reproduccion_desarrollo"]),
        ("mórula", "mórula", "NOUN", ["reproduccion_desarrollo"]),
        ("blástula", "blástula", "NOUN", ["reproduccion_desarrollo"]),
        ("gástrula", "gástrula", "NOUN", ["reproduccion_desarrollo"]),
        ("ectodermo", "ectodermo", "NOUN", ["reproduccion_desarrollo"]),
        ("mesodermo", "mesodermo", "NOUN", ["reproduccion_desarrollo"]),
        ("endodermo", "endodermo", "NOUN", ["reproduccion_desarrollo"]),
        ("placenta", "placenta", "NOUN", ["reproduccion_desarrollo"]),
        ("umbilical", "umbilical", "ADJ", ["reproduccion_desarrollo"]),

        # 7. Bioenergética, Glúcidos, Lípidos y Enzimas
        ("bioelemento", "bioelemento", "NOUN", ["metabolismo_bioenergetica"]),
        ("oligoelemento", "oligoelemento", "NOUN", ["metabolismo_bioenergetica"]),
        ("monosacárido", "monosacárido", "NOUN", ["metabolismo_bioenergetica"]),
        ("disacárido", "disacárido", "NOUN", ["metabolismo_bioenergetica"]),
        ("polisacárido", "polisacárido", "NOUN", ["metabolismo_bioenergetica"]),
        ("fructosa", "fructosa", "NOUN", ["metabolismo_bioenergetica"]),
        ("galactosa", "galactosa", "NOUN", ["metabolismo_bioenergetica"]),
        ("sacarosa", "sacarosa", "NOUN", ["metabolismo_bioenergetica"]),
        ("lactosa", "lactosa", "NOUN", ["metabolismo_bioenergetica"]),
        ("maltosa", "maltosa", "NOUN", ["metabolismo_bioenergetica"]),
        ("glucosídico", "glucosídico", "ADJ", ["metabolismo_bioenergetica"]),
        ("fosfolípido", "fosfolípido", "NOUN", ["metabolismo_bioenergetica", "citologia_general"]),
        ("esteroide", "esteroide", "NOUN", ["metabolismo_bioenergetica"]),
        ("fosforilación", "fosforilación", "NOUN", ["metabolismo_bioenergetica"]),
        ("oxidativa", "oxidativa", "ADJ", ["metabolismo_bioenergetica"]),
        ("krebs", "krebs", "NOUN", ["metabolismo_bioenergetica"]),
        ("electrones", "electrones", "NOUN", ["metabolismo_bioenergetica"]),
        ("quimiosíntesis", "quimiosíntesis", "NOUN", ["metabolismo_bioenergetica", "ecologia_evolucion_taxonomia"]),
        ("grana", "grana", "NOUN", ["metabolismo_bioenergetica"]),
        ("rubisco", "rubisco", "NOUN", ["metabolismo_bioenergetica"]),
        ("coenzima", "coenzima", "NOUN", ["metabolismo_bioenergetica"]),
        ("desnaturalización", "desnaturalización", "NOUN", ["metabolismo_bioenergetica"]),

        # 8. Sistema Nervioso y Endocrino
        ("sináptica", "sináptica", "ADJ", ["sistema_nervioso_endocrino"]),
        ("despolarización", "despolarización", "NOUN", ["sistema_nervioso_endocrino"]),
        ("reflejo", "reflejo", "NOUN", ["sistema_nervioso_endocrino"]),
        ("médula", "médula", "NOUN", ["sistema_nervioso_endocrino", "sistema_circulatorio_inmune"]),
        ("bulbo", "bulbo", "NOUN", ["sistema_nervioso_endocrino"]),
        ("cerebelo", "cerebelo", "NOUN", ["sistema_nervioso_endocrino"]),
        ("corteza", "corteza", "NOUN", ["sistema_nervioso_endocrino", "botanica_tejidos"]),
        ("tiroxina", "tiroxina", "NOUN", ["sistema_nervioso_endocrino"]),
        ("calcitonina", "calcitonina", "NOUN", ["sistema_nervioso_endocrino"]),
        ("paratohormona", "paratohormona", "NOUN", ["sistema_nervioso_endocrino"]),
        ("aldosterona", "aldosterona", "NOUN", ["sistema_nervioso_endocrino", "sistemas_digestion_respiracion_excrecion"]),
        ("cortisol", "cortisol", "NOUN", ["sistema_nervioso_endocrino"]),
        ("oxitocina", "oxitocina", "NOUN", ["sistema_nervioso_endocrino"]),
        ("prolactina", "prolactina", "NOUN", ["sistema_nervioso_endocrino"]),
        ("vasopresina", "vasopresina", "NOUN", ["sistema_nervioso_endocrino", "sistemas_digestion_respiracion_excrecion"]),
        ("neuroglia", "neuroglia", "NOUN", ["sistema_nervioso_endocrino"]),
        ("astrocito", "astrocito", "NOUN", ["sistema_nervioso_endocrino"]),
        ("microglía", "microglía", "NOUN", ["sistema_nervioso_endocrino"]),
        ("oligodendrocito", "oligodendrocito", "NOUN", ["sistema_nervioso_endocrino"]),
        ("schwann", "schwann", "NOUN", ["sistema_nervioso_endocrino"]),

        # 9. Sistema Circulatorio e Inmunológico
        ("miocardio", "miocardio", "NOUN", ["sistema_circulatorio_inmune"]),
        ("endocardio", "endocardio", "NOUN", ["sistema_circulatorio_inmune"]),
        ("pericardio", "pericardio", "NOUN", ["sistema_circulatorio_inmune"]),
        ("sístole", "sístole", "NOUN", ["sistema_circulatorio_inmune"]),
        ("diástole", "diástole", "NOUN", ["sistema_circulatorio_inmune"]),
        ("trombocito", "trombocito", "NOUN", ["sistema_circulatorio_inmune"]),
        ("inmunoglobulina", "inmunoglobulina", "NOUN", ["sistema_circulatorio_inmune"]),
        ("macrófago", "macrófago", "NOUN", ["sistema_circulatorio_inmune"]),
        ("interferón", "interferón", "NOUN", ["sistema_circulatorio_inmune"]),
        ("inmunidad", "inmunidad", "NOUN", ["sistema_circulatorio_inmune"]),

        # 10. Digestión, Respiración y Excreción
        ("quilo", "quilo", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("villosidad", "villosidad", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("epiglotis", "epiglotis", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("pleura", "pleura", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("inspiración", "inspiración", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("espiración", "espiración", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("nefrona", "nefrona", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("bowman", "bowman", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("henle", "henle", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("uretra", "uretra", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("uréter", "uréter", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),
        ("vejiga", "vejiga", "NOUN", ["sistemas_digestion_respiracion_excrecion"]),

        # 11. Botánica y Tejidos Biológicos
        ("parénquima", "parénquima", "NOUN", ["botanica_tejidos"]),
        ("colénquima", "colénquima", "NOUN", ["botanica_tejidos"]),
        ("esclerénquima", "esclerénquima", "NOUN", ["botanica_tejidos"]),
        ("cutícula", "cutícula", "NOUN", ["botanica_tejidos"]),
        ("suber", "suber", "NOUN", ["botanica_tejidos"]),
        ("lenticela", "lenticela", "NOUN", ["botanica_tejidos"]),
        ("traqueida", "traqueida", "NOUN", ["botanica_tejidos"]),
        ("criboso", "criboso", "ADJ", ["botanica_tejidos"]),
        ("epidermis", "epidermis", "NOUN", ["botanica_tejidos"]),
        ("cartílago", "cartílago", "NOUN", ["botanica_tejidos"]),
        ("osteocito", "osteocito", "NOUN", ["botanica_tejidos"]),
        ("condrocito", "condrocito", "NOUN", ["botanica_tejidos"]),
        ("osteoblasto", "osteoblasto", "NOUN", ["botanica_tejidos"]),

        # 12. Ecología, Evolución y Biodiversidad Peruana
        ("ecotono", "ecotono", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("clímax", "clímax", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("productor", "productor", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("consumidor", "consumidor", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("descomponedor", "descomponedor", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("eutrofización", "eutrofización", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("invernadero", "invernadero", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("ozono", "ozono", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("especiación", "especiación", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("alopátrica", "alopátrica", "ADJ", ["ecologia_evolucion_taxonomia"]),
        ("simpátrica", "simpátrica", "ADJ", ["ecologia_evolucion_taxonomia"]),
        ("lamarckismo", "lamarckismo", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("neodarwinismo", "neodarwinismo", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("prion", "prion", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("virión", "virión", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("cianobacteria", "cianobacteria", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("puna", "puna", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("yunga", "yunga", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("quechua", "quechua", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("puya", "puya", "NOUN", ["ecologia_evolucion_taxonomia", "botanica_tejidos"]),
        ("alpaca", "alpaca", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("taruca", "taruca", "NOUN", ["ecologia_evolucion_taxonomia"]),
        ("vulnerable", "vulnerable", "ADJ", ["ecologia_evolucion_taxonomia"])
    ]

    # Procesar adiciones al diccionario
    added_to_dict = 0
    cluster_term_additions = {c["topic_id"]: set(c["terms"]) for c in current_clusters}

    for term, lemma, cat, assigned_clusters in new_terms_spec:
        clean_t = term.strip()
        clean_tl = clean_t.lower()
        if clean_tl not in existing_terms:
            entry = {
                "term": clean_t,
                "lemma": lemma,
                "subject": "BIOLOGIA",
                "category": cat,
                "phonetic": to_hispanic_phonetic(clean_t)
            }
            current_dict.append(entry)
            existing_terms[clean_tl] = entry
            added_to_dict += 1

        # Asignar a clusters
        for cid in assigned_clusters:
            if cid in cluster_term_additions:
                cluster_term_additions[cid].add(clean_t)
            else:
                print(f"[!] Warning: Cluster {cid} no existe en clusters actuales")

    # Actualizar clusters con los nuevos términos ordenados
    for c in current_clusters:
        cid = c["topic_id"]
        c["terms"] = sorted(list(cluster_term_additions[cid]), key=lambda s: strip_accents(s.lower()))

    # Ordenar diccionario alfabéticamente
    current_dict = sorted(current_dict, key=lambda x: strip_accents(x["term"].lower()))

    # Validaciones de consistencia
    dict_term_set = set(x["term"].lower().strip() for x in current_dict)
    all_cluster_terms = set()
    for c in current_clusters:
        for t in c["terms"]:
            all_cluster_terms.add(t.lower().strip())

    missing_in_dict = all_cluster_terms - dict_term_set
    assert len(missing_in_dict) == 0, f"Términos en clusters pero no en diccionario: {missing_in_dict}"

    print(f"[+] Nuevos términos añadidos: {added_to_dict}")
    print(f"[+] Total términos en nuevo Diccionario de Biología: {len(current_dict)}")
    print(f"[+] Total términos únicos en Topic Clusters: {len(all_cluster_terms)}")
    print(f"[+] Cobertura de clusters sobre diccionario: {len(all_cluster_terms.intersection(dict_term_set))} / {len(all_cluster_terms)} (100%)")

    # Guardar archivos
    with open(dict_file, "w", encoding="utf-8") as f:
        json.dump(current_dict, f, ensure_ascii=False, indent=2)

    with open(clusters_file, "w", encoding="utf-8") as f:
        json.dump(current_clusters, f, ensure_ascii=False, indent=2)

    print("[+] Archivos guardados exitosamente en assets.")

if __name__ == "__main__":
    main()
