#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Lista los términos del diccionario de biología que el modelo Vosk NO puede escribir (fuera de su vocabulario).

Vosk avisa "Ignoring word missing in vocabulary" por cada palabra de una gramática que no conoce; se le pasa el
diccionario completo como gramática y se recogen esos avisos. Regenerar si cambia el modelo o el diccionario:

  .venv/bin/python generar_oov.py

Salidas:
  vosk_fase0/oov_terminos.json                               (benchmark y análisis)
  app/src/main/assets/dictionaries/biologia_vosk_oov.json    (la app: C neutra en uniones, ver DecisionEvaluator)
"""
import json
import os
import tempfile

from vosk import KaldiRecognizer, Model, SetLogLevel

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(AQUI))
DICCIONARIO = os.path.join(RAIZ, "app", "src", "main", "assets", "dictionaries", "biologia.json")
MODELO = os.path.join(AQUI, "modelos", "vosk-model-small-es-0.42")
SALIDAS = [
    os.path.join(AQUI, "oov_terminos.json"),
    os.path.join(RAIZ, "app", "src", "main", "assets", "dictionaries", "biologia_vosk_oov.json"),
]


def main():
    with open(DICCIONARIO, encoding="utf-8") as f:
        terminos = sorted({d["term"].lower() for d in json.load(f)})
    SetLogLevel(0)
    # Los avisos de Kaldi salen por stderr. Van a un ARCHIVO temporal, no a un pipe: con más de ~64 KB de avisos
    # (diccionario de >1000 términos) el pipe se llenaba sin que nadie lo leyera y Vosk quedaba bloqueado.
    with tempfile.TemporaryFile() as registro:
        stderr = os.dup(2)
        os.dup2(registro.fileno(), 2)
        try:
            modelo = Model(MODELO)
            KaldiRecognizer(modelo, 16000, json.dumps(terminos + ["[unk]"], ensure_ascii=False))
        finally:
            os.dup2(stderr, 2)
            os.close(stderr)
        registro.seek(0)
        log = registro.read().decode("utf-8", errors="replace")
    faltan = sorted({l.split("'")[1] for l in log.splitlines() if "missing" in l.lower() and "'" in l})
    for salida in SALIDAS:
        with open(salida, "w", encoding="utf-8") as f:
            json.dump(faltan, f, ensure_ascii=False, indent=0)
    print(f"{len(faltan)} de {len(terminos)} términos fuera del vocabulario de Vosk -> {', '.join(SALIDAS)}")


if __name__ == "__main__":
    main()
