#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Descarga una MUESTRA del corpus OpenSLR 73 (español peruano, frases leídas, CC BY-SA 4.0)
sin bajar los zips completos (~1.9 GB): lee solo los archivos elegidos mediante peticiones
HTTP por rangos (paquete `remotezip`).

Por cada audio deja <id>.wav y <id>.txt (la transcripción), que vosk_fase0.py usa como referencia.

  .venv/bin/python descargar_slr73.py --n 200
  .venv/bin/python vosk_fase0.py audios/slr73/*.wav --modelo modelos/vosk-model-small-es-0.42

Atribución: Guevara-Rukoz et al. (2020), "Crowdsourcing Latin American Spanish for Low-Resource
Text-to-Speech", LREC 2020. https://www.openslr.org/73/
"""

import argparse
import csv
import os
import random
import sys
import urllib.request
from collections import defaultdict

BASE = "https://www.openslr.org/resources/73"
CONJUNTOS = {"female": "es_pe_female.zip", "male": "es_pe_male.zip"}
AQUI = os.path.dirname(os.path.abspath(__file__))


def leer_indice(genero):
    with urllib.request.urlopen(f"{BASE}/line_index_{genero}.tsv") as r:
        texto = r.read().decode("utf-8")
    return [(fila[0], fila[1]) for fila in csv.reader(texto.splitlines(), delimiter="\t") if len(fila) >= 2]


def main():
    ap = argparse.ArgumentParser(description="Muestra de OpenSLR 73 (español peruano) para la fase 0.")
    ap.add_argument("--n", type=int, default=200, help="número total de frases (mitad mujeres, mitad hombres)")
    ap.add_argument("--semilla", type=int, default=73, help="semilla del muestreo (reproducible)")
    ap.add_argument("--salida", default=os.path.join(AQUI, "audios", "slr73"))
    args = ap.parse_args()

    try:
        from remotezip import RemoteZip
    except ImportError:
        sys.exit("ERROR: falta remotezip (uv pip install -p .venv/bin/python -r requirements.txt).")

    os.makedirs(args.salida, exist_ok=True)
    rnd = random.Random(args.semilla)
    por_genero = {"female": args.n // 2, "male": args.n - args.n // 2}

    for genero, cuantos in por_genero.items():
        indice = leer_indice(genero)
        # Muestreo repartido entre hablantes: primero uno por hablante, luego se repite la ronda
        por_hablante = defaultdict(list)
        for archivo, texto in indice:
            por_hablante["_".join(archivo.split("_")[:2])].append((archivo, texto))
        for lista in por_hablante.values():
            rnd.shuffle(lista)
        hablantes = sorted(por_hablante)
        rnd.shuffle(hablantes)
        elegidos, ronda = [], 0
        while len(elegidos) < cuantos and any(len(v) > ronda for v in por_hablante.values()):
            for h in hablantes:
                if len(elegidos) < cuantos and len(por_hablante[h]) > ronda:
                    elegidos.append(por_hablante[h][ronda])
            ronda += 1

        print(f"{genero}: {len(elegidos)} frases de {len({'_'.join(a.split('_')[:2]) for a, _ in elegidos})} hablantes")
        with RemoteZip(f"{BASE}/{CONJUNTOS[genero]}") as z:
            nombres = {os.path.splitext(os.path.basename(n))[0]: n for n in z.namelist() if n.endswith(".wav")}
            for k, (archivo, texto) in enumerate(elegidos, 1):
                destino = os.path.join(args.salida, archivo + ".wav")
                if not os.path.exists(destino):
                    with z.open(nombres[archivo]) as src, open(destino, "wb") as dst:
                        dst.write(src.read())
                with open(os.path.join(args.salida, archivo + ".txt"), "w", encoding="utf-8") as f:
                    f.write(texto + "\n")
                print(f"  {k}/{len(elegidos)} {archivo}", end="\r")
        print()

    print(f"Listo: {args.salida}")


if __name__ == "__main__":
    main()
