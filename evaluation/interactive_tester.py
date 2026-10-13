#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Probador Interactivo en Tiempo Real de la Capa Difusa On-Device.
Permite escribir frases de prueba y ver en directo la fuzzificación,
el cálculo de Levenshtein con fonética hispana y la defuzzificación COG.
"""

import sys
import os
import json

# Agregar directorio actual para importar funciones del benchmark
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from benchmark_runner import (
    calculate_similarity,
    evaluate_mamdani,
    strip_accents
)

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    assets_dict_dir = os.path.join(base_dir, "..", "app", "src", "main", "assets", "dictionaries")

    dictionaries = {}
    for sub in ["biologia", "quimica", "historia", "geografia"]:
        path = os.path.join(assets_dict_dir, f"{sub}.json")
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                dictionaries[sub.upper()] = json.load(f)

    print("=====================================================================")
    print("      PROBADOR INTERACTIVO: CAPA COMPUTACIONAL DIFUSA ON-DEVICE      ")
    print("      Tesis UNSA - Sistema Speech-to-Text para Estudiantes DHH       ")
    print("=====================================================================\n")

    subjects = ["BIOLOGIA", "QUIMICA", "HISTORIA", "GEOGRAFIA"]
    current_subject = "BIOLOGIA"

    print("Materias disponibles: " + ", ".join(subjects))
    print("Comandos especiales:")
    print("  :materia <BIOLOGIA|QUIMICA|HISTORIA|GEOGRAFIA> -> Cambiar asignatura")
    print("  :salir                                         -> Salir del probador\n")

    while True:
        try:
            print(f"[{current_subject}] Ingrese texto reconocido por el ASR crudo:")
            user_input = input("> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nSaliendo...")
            break

        if not user_input:
            continue

        if user_input.lower() in [":salir", "exit", "quit"]:
            print("Cerrando sesión de pruebas.")
            break

        if user_input.startswith(":materia"):
            parts = user_input.split()
            if len(parts) > 1 and parts[1].upper() in subjects:
                current_subject = parts[1].upper()
                print(f"-> Materia cambiada a: {current_subject}\n")
            else:
                print("Materia no válida. Opciones: " + ", ".join(subjects))
            continue

        # Proceso difuso
        dict_terms = dictionaries.get(current_subject, [])
        words = user_input.split()
        corrected_words = []
        corrections_detail = []

        for word in words:
            # Asumir nivel de ruido típico de aula para palabras no triviales: C = 0.45
            conf = 0.45
            best_term = None
            best_sim = 0.0

            for d in dict_terms:
                sim = calculate_similarity(word, d["term"])
                if sim > best_sim:
                    best_sim = sim
                    best_term = d["term"]

            if best_term and best_sim >= 0.50:
                y_star = evaluate_mamdani(conf, best_sim)
                if y_star >= 0.65:
                    corrected_words.append(best_term)
                    corrections_detail.append((word, best_term, conf, best_sim, y_star, True))
                else:
                    corrected_words.append(word)
                    corrections_detail.append((word, best_term, conf, best_sim, y_star, False))
            else:
                corrected_words.append(word)

        corrected_text = " ".join(corrected_words)

        print("\n" + "-" * 65)
        print(f"Texto Crudo (Entrada ASR) : \"{user_input}\"")
        print(f"Texto Corregido (Pantalla): \"{corrected_text}\"")
        print("-" * 65)

        if corrections_detail:
            print("Detalle de Evaluación Difusa (Mamdani COG):")
            for orig, cand, c, d, y_star, subst in corrections_detail:
                estado = "APROBADA (y* >= 0.65)" if subst else "RECHAZADA (y* < 0.65)"
                print(f"  • Palabra: '{orig}' -> Candidato: '{cand}'")
                print(f"    Confianza C: {c:.2f} | Similitud D: {d:.2f} | Justificación y*: {y_star:.2f} -> {estado}")
        else:
            print("  (No se detectaron términos técnicos ambiguos para corregir)")
        print("-" * 65 + "\n")

if __name__ == "__main__":
    main()
