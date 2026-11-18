#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Agrega al diccionario del área Ciencia y Tecnología los términos de 1.º, 2.º y 3.º de secundaria (CNEB, ciclos VI y
VII), con sus áreas temáticas (clusters) para el factor S. Complementa a scripts/agregar_quimica_ciclos.py (química
de 3.º a 5.º), que ya cubría átomo, enlace, funciones, reacciones, soluciones, química orgánica y ciclos.

Competencias del área (CNEB, MINEDU 2016) y temas de los textos escolares de CyT 1.º-3.º que cubren estos clusters:
  - Indaga mediante métodos científicos: hipótesis, variables, instrumentos de laboratorio y medición.
  - Explica el mundo físico (materia y energía; mecanismos de los seres vivos; biodiversidad, Tierra y universo):
      * Materia y sus propiedades, estados y cambios de estado; modelos atómicos (1.º-2.º).
      * Física: movimiento, fuerzas, energía, calor y temperatura, electricidad y magnetismo, ondas, sonido y luz,
        presión y fluidos (1.º-3.º). Antes NO había ningún término de física.
      * Tierra y universo: sistema solar, estructura de la Tierra, sismos y fenómenos del Perú (1.º-2.º).
      * Seres vivos: clasificación (vertebrados, invertebrados, plantas sin semilla), nutrición y salud,
        enfermedades y prevención, órganos de los sentidos, reproducción humana y adolescencia (1.º-3.º).
      * Ambiente: contaminación, cambio climático y sostenibilidad (1.º-3.º).
  - Diseña y construye soluciones tecnológicas: prototipo, tecnología.

Por qué en biologia.json: el código interno "BIOLOGIA" es el del área completa (CaptionsViewModel.SUBJECT_CODE).

Criterios para entrar (los mismos de agregar_quimica_ciclos.py, para evitar falsas correcciones):
  - Una sola palabra y al menos 5 letras.
  - Sin vecinos comunes: se excluyen los términos con similitud D >= 0.70 frente a una palabra frecuente del
    español (EXCLUIDOS: se dejan en las listas como constancia y el script los salta; decididos con scripts/revisar_vecinos_terminos.py --modulo agregar_cyt_primero_tercero).
  - Sin palabras muy frecuentes y vagas ("trabajo", "cambio", "carga"): siempre identicas a sí mismas, solo
    sumarían ruido a la memoria de clase.

Los clusters existentes listados en AMPLIAR_CLUSTERS solo GANAN términos (unión); no se quita ninguno.
Idempotente. Ejecutar desde la raíz del repositorio:
  python3 scripts/agregar_cyt_primero_tercero.py
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
NUEVOS_CLUSTERS = [
    # --- Indagación científica y tecnología (competencias 1 y 3, todos los grados) ---
    ("indagacion_cientifica", "Indagación Científica, Laboratorio y Medición", "indagacion_y_tecnologia", [
        "hipótesis", "variable", "experimento", "experimental", "observación", "indagación", "medición",
        "conclusión", "laboratorio", "microscopio", "probeta", "pipeta", "bureta", "matraz", "mechero",
        "balanza", "termómetro", "cronómetro", "dinamómetro", "vernier", "magnitud", "científico", "prototipo",
        "tecnología", "tecnológico", "instrumento",
    ]),

    # --- Materia (1.º-2.º) ---
    ("materia_propiedades_estados", "Materia: Propiedades, Estados y Cambios de Estado", "quimica_inorganica", [
        "materia", "volumen", "densidad", "sólido", "líquido", "gaseoso", "fusión", "solidificación",
        "vaporización", "ebullición", "licuación", "licuefacción", "sublimación", "condensación", "evaporación",
        "compresibilidad", "viscosidad", "elasticidad", "dureza", "maleabilidad", "ductilidad", "extensiva",
        "intensiva", "coloide", "suspensión", "centrifugación", "cristalización", "imantación", "compuesto",
        "decantación", "filtración", "destilación", "tamizado", "homogénea", "heterogénea", "sustancia",
        "elemento",
    ]),
    ("modelos_atomicos", "Modelos Atómicos y Partículas Subatómicas", "quimica_inorganica", [
        "dalton", "thomson", "rutherford", "schrödinger", "demócrito", "electrón", "electrones", "protón",
        "neutrón", "núcleo", "nucleón", "partícula", "subatómica", "radiactividad", "radiactivo", "isótopo",
        "átomo", "atómico", "cuántico", "orbital", "mendeléiev",
    ]),

    # --- Física (1.º-3.º) ---
    ("cinematica", "Movimiento: Cinemática", "fisica", [
        "movimiento", "trayectoria", "desplazamiento", "distancia", "rapidez", "velocidad", "aceleración",
        "desaceleración", "rectilíneo", "gravedad", "posición", "vector", "vectorial", "escalar", "cinemática",
        "kilómetro", "cronómetro", "magnitud",
    ]),
    ("dinamica_fuerzas", "Fuerzas y Leyes de Newton", "fisica", [
        "fuerza", "newton", "inercia", "rozamiento", "fricción", "tensión", "gravitatoria", "gravitación",
        "equilibrio", "resultante", "dinámica", "palanca", "polea", "torque", "empuje", "dinamómetro",
        "gravedad", "aceleración",
    ]),
    ("trabajo_energia", "Trabajo, Energía y Potencia", "fisica", [
        "energía", "cinética", "potencial", "mecánica", "térmica", "eléctrica", "nuclear", "eólica",
        "hidráulica", "geotérmica", "hidroeléctrica", "fotovoltaico", "solar", "renovable", "conservación",
        "potencia", "joule", "vatio", "kilovatio", "eficiencia", "combustible", "biomasa", "petróleo",
        "rendimiento",
    ]),
    ("calor_temperatura", "Calor y Temperatura", "fisica", [
        "calor", "temperatura", "termómetro", "celsius", "kelvin", "fahrenheit", "dilatación", "conducción",
        "convección", "radiación", "caloría", "aislante", "conductor", "térmica", "equilibrio",
    ]),
    ("electricidad_magnetismo", "Electricidad y Magnetismo", "fisica", [
        "electricidad", "eléctrica", "eléctrico", "corriente", "voltaje", "resistencia", "amperio", "voltio",
        "ohmio", "circuito", "paralelo", "conductor", "aislante", "electrón", "electrostática", "electrización",
        "inducción", "coulomb", "magnetismo", "magnético", "electroimán", "brújula", "generador", "interruptor",
        "fusible", "batería",
    ]),
    ("ondas_sonido_luz", "Ondas, Sonido y Luz", "fisica", [
        "frecuencia", "amplitud", "longitud", "periodo", "vibración", "sonido", "sonora", "decibel",
        "decibelio", "hertz", "reflexión", "refracción", "difracción", "espejo", "lente", "cóncavo", "convexo",
        "cóncava", "convexa",  # femeninas: sin ellas 'una cóncava' pasaba a 'una cóncavo' (CyT #70)
        "espectro", "prisma", "ultravioleta", "infrarrojo", "electromagnética", "luminoso", "fotón",
        "transversal", "longitudinal",
    ]),
    ("presion_fluidos", "Presión e Hidrostática", "fisica", [
        "presión", "pascal", "arquímedes", "empuje", "flotación", "hidrostática", "atmosférica", "barómetro",
        "manómetro", "fluido", "hidráulica", "torricelli", "densidad", "volumen",
    ]),

    # --- Tierra y universo (1.º-2.º) ---
    ("universo_sistema_solar", "El Universo y el Sistema Solar", "tierra_y_universo", [
        "universo", "galaxia", "estrella", "planeta", "satélite", "venus", "marte", "júpiter", "saturno",
        "urano", "neptuno", "plutón", "asteroide", "cometa", "meteorito", "meteoro", "órbita", "rotación",
        "traslación", "eclipse", "solsticio", "equinoccio", "telescopio", "astronomía", "nebulosa",
        "constelación", "gravitación", "heliocéntrico", "geocéntrico", "copérnico", "galileo", "kepler",
        "cosmos", "mercurio",
    ]),
    ("estructura_tierra", "Estructura y Dinámica de la Tierra", "tierra_y_universo", [
        "corteza", "núcleo", "litosfera", "astenosfera", "mesosfera", "geosfera", "tectónica", "sismo",
        "terremoto", "volcán", "magma", "epicentro", "hipocentro", "cordillera", "mineral", "ígnea",
        "sedimentaria", "metamórfica", "relieve", "erosión", "meteorización", "tsunami", "maremoto", "huaico",
        "friaje", "atmósfera", "troposfera", "estratosfera", "hidrosfera", "sismógrafo", "richter", "magnitud",
    ]),

    # --- Seres vivos y salud (1.º-3.º) ---
    ("clasificacion_seres_vivos", "Niveles de Organización y Clasificación de los Seres Vivos", "botanica_y_ecologia", [
        "organismo", "órgano", "tejido", "vertebrado", "invertebrado", "mamífero", "reptil", "anfibio",
        "artrópodo", "molusco", "insecto", "arácnido", "crustáceo", "equinodermo", "anélido", "cnidario",
        "porífero", "briofita", "helecho", "musgo", "pteridofita", "irritabilidad", "unicelular",
        "pluricelular", "multicelular", "microorganismo", "carnívoro", "herbívoro", "omnívoro", "ovíparo",
        "vivíparo", "linneo", "binomial", "célula", "población", "comunidad", "ecosistema", "biosfera",
    ]),
    ("nutricion_salud", "Alimentación, Nutrición y Salud", "fisiologia_humana", [
        "nutriente", "nutrición", "alimento", "alimentación", "vitamina", "mineral", "caloría", "dieta",
        "anemia", "desnutrición", "obesidad", "diabetes", "hipertensión", "sedentarismo", "anorexia", "bulimia",
        "colesterol", "proteína", "carbohidrato", "lípido", "glucosa", "hierro", "calcio", "digestión",
        "metabolismo",
    ]),
    ("enfermedades_prevencion", "Enfermedades Infecciosas y Prevención", "fisiologia_humana", [
        "enfermedad", "infección", "infeccioso", "contagio", "epidemia", "pandemia", "tuberculosis", "dengue",
        "malaria", "paludismo", "hepatitis", "sarampión", "influenza", "antibiótico", "vacuna", "vacunación",
        "inmunización", "microorganismo", "higiene", "desinfección", "bacteria", "virus", "parásito", "sífilis",
        "gonorrea", "papiloma", "inmunodeficiencia", "inmunidad", "anticuerpo", "patógeno",
    ]),
    ("organos_sentidos", "Órganos de los Sentidos", "fisiologia_humana", [
        "retina", "córnea", "pupila", "cristalino", "tímpano", "cóclea", "papila", "gustativa", "olfato",
        "olfativo", "receptor", "estímulo", "nervio", "audición", "visión", "miopía", "hipermetropía",
        "astigmatismo", "auditivo", "óptico", "sensorial", "neurona", "cerebelo",
    ]),
    ("reproduccion_humana", "Reproducción Humana y Adolescencia", "genetica_y_herencia", [
        "pubertad", "adolescencia", "menstruación", "menstrual", "ovario", "testículo", "útero", "ovulación",
        "embarazo", "anticonceptivo", "gestación", "estrógeno", "testosterona", "progesterona", "próstata",
        "vagina", "escroto", "endometrio", "eyaculación", "fecundación", "cigoto", "embrión", "placenta",
        "espermatozoide", "óvulo", "hormona",
    ]),
    ("ambiente_sostenibilidad", "Ambiente, Cambio Climático y Sostenibilidad", "botanica_y_ecologia", [
        "contaminación", "contaminante", "deforestación", "reciclaje", "residuo", "climático",
        "desertificación", "sostenible", "sostenibilidad", "extinción", "conservación", "reforestación",
        "glaciar", "deshielo", "relave", "minería", "pesticida", "plaguicida", "fertilizante", "depredador",
        "alimentaria", "pirámide", "calentamiento", "invernadero", "ozono", "biodegradable", "biodiversidad",
        "ecosistema",
    ]),
]

# Términos que también se suman a clusters YA existentes (unión): el tema ya estaba, faltaban palabras de 1.º-2.º.
AMPLIAR_CLUSTERS = {
    "sistemas_digestion_respiracion_excrecion": ["digestión", "nutrición", "excreción", "orina", "faringe",
                                                 "laringe", "saliva", "glándula"],
    "sistema_circulatorio_inmune": ["sangre", "circulación", "corazón", "presión"],
    "ecologia_evolucion_taxonomia": ["depredador", "herbívoro", "carnívoro", "omnívoro", "extinción"],
    "botanica_tejidos": ["helecho", "musgo", "briofita", "pteridofita", "semilla", "fruto"],
    # Sin esto, 'nutrientes' solo votaba por nutrición humana y se perdía 'autorización' -> eutrofización.
    "ciclos_biogeoquimicos": ["nutriente"],
}

# REGLA (medida): un término que ya estaba en un cluster anterior NO se repite en los clusters nuevos. Repetirlo lo
# reparte entre k clusters (aporta 1/k a cada uno) y debilita el tema original: con 'célula', 'bacteria', 'proteína'
# o 'núcleo' repetidos se perdían correcciones de biología y química. Se dejan en las listas como referencia del
# currículo; main() los salta. Los clusters de la misma área (macro_id) siguen recibiendo crédito parcial.

# Candidatos descartados y por qué. No volver a añadirlos sin medir falsas correcciones en los controles.
EXCLUIDOS = {
    "trabajo": "palabra muy frecuente y vaga (memoria de clase)", "cambio": "muy frecuente y vaga",
    "carga": "muy frecuente (verbo cargar)", "uniforme": "vecino 'informe'; 'uniforme' escolar",
    "manto": "vecinos 'tanto', 'santo', 'mando'", "falla": "verbo fallar", "masa": "menos de 5 letras",
    "peso": "menos de 5 letras", "onda": "menos de 5 letras", "imán": "menos de 5 letras",
    "roca": "menos de 5 letras", "lava": "menos de 5 letras", "luna": "menos de 5 letras",
    "iris": "menos de 5 letras", "pila": "menos de 5 letras", "watt": "menos de 5 letras", "raíz": "menos de 5 letras",
    # Regla de la revisión de vecinos (es_50k, top 20 000): fuera si una palabra entre las 5000 más frecuentes
    # tiene D >= 0.80 con el término. Se pierden términos del currículo (fuerza, calor, presión, planeta), pero
    # Vosk ya los escribe bien (están en su vocabulario) y como destino de corrección atraerían palabras comunes.
    "lente": "vecino 'gente' (#145, D=0.8), 'mente' (#758, D=0.8)",
    "sismo": "vecino 'mismo' (#149, D=0.8)",
    "fuerza": "vecino 'fuera' (#160, D=0.83)",
    "magma": "vecino 'mamá' (#196, D=0.8), 'magia' (#1688, D=0.8)",
    "marte": "vecino 'parte' (#211, D=0.8), 'darte' (#1199, D=0.8)",
    "sonora": "vecino 'señora' (#300, D=0.83)",
    "materia": "vecino 'material' (#2195, D=0.88), 'mataría' (#3896, D=0.86)",
    "espejo": "vecino 'espero' (#329, D=0.83)",
    "solar": "vecino 'sola' (#508, D=0.8), 'volar' (#1533, D=0.8)",
    "tensión": "vecino 'atención' (#693, D=0.8)",
    "alimento": "vecino 'aliento' (#3358, D=0.88)",
    "fotón": "vecino 'foto' (#767, D=0.8), 'fotos' (#894, D=0.8)",
    "difracción": "vecino 'dirección' (#843, D=0.8)",
    "prisma": "vecino 'prisa' (#883, D=0.83), 'prima' (#3666, D=0.83)",
    "térmica": "vecino 'termina' (#1806, D=0.86)",
    "malaria": "vecino 'mataría' (#3896, D=0.86)",
    "visión": "vecino 'misión' (#1048, D=0.83)",
    "retina": "vecino 'reina' (#1084, D=0.83), 'rutina' (#4188, D=0.83)",
    "presión": "vecino 'prisión' (#1087, D=0.86)",
    "torque": "vecino 'porqué' (#1088, D=0.82), 'toque' (#2047, D=0.82)",
    "polea": "vecino 'pelea' (#1116, D=0.8)",
    "córnea": "vecino 'corea' (#4990, D=0.83)",
    "urano": "vecino 'grano' (#3714, D=0.8)",
    "gestación": "vecino 'estación' (#1179, D=0.89)",
    "saliva": "vecino 'salida' (#1200, D=0.83), 'salía' (#4458, D=0.83)",
    "calor": "vecino 'color' (#1207, D=0.8), 'valor' (#1308, D=0.8)",
    "voltio": "vecino 'volvió' (#1238, D=0.83)",
    "escalar": "vecino 'escapar' (#1384, D=0.86), 'escala' (#4493, D=0.86)",
    "planeta": "vecino 'planta' (#2372, D=0.86)",
    "sólido": "vecino 'salido' (#1419, D=0.83)",
    "conservación": "vecino 'conversación' (#1465, D=0.83)",
    "convección": "vecino 'colección' (#3858, D=0.8)",
    "escroto": "vecino 'escrito' (#1490, D=0.86)",
    "enfermedad": "vecino 'enfermera' (#1916, D=0.8)",
    "conducción": "vecino 'condición' (#2825, D=0.8)",
    "kelvin": "vecino 'kevin' (#1822, D=0.83)",
    "vatio": "vecino 'vacío' (#2247, D=0.8), 'patio' (#2963, D=0.8)",
    "vagina": "vecino 'página' (#2381, D=0.83)",
    "partícula": "vecino 'particular' (#2675, D=0.9)",
    "musgo": "vecino 'museo' (#3190, D=0.8)",
    "vacunación": "vecino 'acusación' (#4874, D=0.8)",
    "amperio": "vecino 'imperio' (#3513, D=0.86)",
    "excreción": "vecino 'expresión' (#3739, D=0.82)",
    "resistencia": "vecino 'existencia' (#3848, D=0.82), 'residencia' (#4142, D=0.82)",
    "posición": "vecino 'exposición' (#4846, D=0.8), 'posesión' (#4937, D=0.8)",
    "influenza": "vecino 'influencia' (#3941, D=0.84)",
    "radiación": "vecino 'graduación' (#4638, D=0.8)",
    "vector": "vecino 'victor' (#4042, D=0.83), 'sector' (#4632, D=0.83)",
    "dieta": "vecino 'diera' (#4056, D=0.8)",
    "fruto": "vecino 'fruta' (#4693, D=0.8)",
    # Medidos con evaluation/evaluar_quimica_ciclos.py (corpus TTS + Vosk, C real de Vosk, 2026-11-18):
    "dalton": "término exacto bloquea la unión 'dalton mismo' -> daltonismo (biología prueba #26, #66)",
    "organismo": "'organismos' pasa a ser evidencia de otro tema y se pierde 'precario tas' -> procariotas (#57)",
    "mecánica": "'mecánicos' -> 'mecánica' en el control de química (#32); vecino 'mecánico' D=0.88",
    "meteoro": "roba la unión 'meteor estación' -> meteorización (química prueba #55, #115)",
    "meteorito": "se pierde 'meteórica acción' -> meteorización (química prueba #175)",
    "decibel": "'reciben' -> 'decibel' en clase de física (CyT #370); queda 'decibelio'",
    "eyaculación": "'adulación' (de 'ovulación') -> 'eyaculación' (CyT #432): error inaceptable en el aula",
}

ADJETIVOS = {
    "experimental", "científico", "tecnológico", "sólido", "líquido", "gaseoso", "extensiva", "intensiva",
    "homogénea", "heterogénea", "subatómica", "radiactivo", "atómico", "cuántico", "rectilíneo", "vectorial",
    "escalar", "gravitatoria", "resultante", "cinética", "potencial", "mecánica", "térmica", "eléctrica",
    "eléctrico", "nuclear", "eólica", "hidráulica", "geotérmica", "hidroeléctrica", "fotovoltaico", "solar",
    "renovable", "aislante", "paralelo", "magnético", "sonora", "cóncavo", "convexo", "cóncava", "convexa",
    "ultravioleta",
    "infrarrojo", "electromagnética", "luminoso", "transversal", "longitudinal", "atmosférica", "heliocéntrico",
    "geocéntrico", "ígnea", "sedimentaria", "metamórfica", "vertebrado", "invertebrado", "unicelular",
    "pluricelular", "multicelular", "carnívoro", "herbívoro", "omnívoro", "ovíparo", "vivíparo", "binomial",
    "infeccioso", "gustativa", "olfativo", "auditivo", "óptico", "sensorial", "menstrual", "anticonceptivo",
    "contaminante", "climático", "sostenible", "alimentaria", "biodegradable",
}
NOMBRES_PROPIOS = {"dalton", "thomson", "rutherford", "schrödinger", "demócrito", "mendeléiev", "newton",
                   "pascal", "arquímedes", "torricelli", "celsius", "kelvin", "fahrenheit", "coulomb", "joule",
                   "hertz", "venus", "marte", "júpiter", "saturno", "urano", "neptuno", "plutón", "copérnico",
                   "galileo", "kepler", "richter", "linneo"}


def sin_tildes(texto):
    return "".join(c for c in unicodedata.normalize("NFD", texto) if not unicodedata.combining(c))


def fonetica(texto):
    """Mismo formato que el campo phonetic de biologia.json (informativo: el matcher calcula la suya)."""
    s = sin_tildes(texto.lower().strip()).replace("h", "").replace("v", "b")
    s = s.replace("ce", "se").replace("ci", "si").replace("z", "s").replace("ll", "y")
    s = s.replace("qu", "k").replace("c", "k").replace("x", "ks")
    return re.sub(r"\s+", "", s)


def candidatos():
    """Todos los términos que este script pide (sin repetir), en orden."""
    todos = [t for _, _, _, ts in NUEVOS_CLUSTERS for t in ts] + [t for ts in AMPLIAR_CLUSTERS.values() for t in ts]
    return list(dict.fromkeys(todos))


def main():
    with open(DICCIONARIO, encoding="utf-8") as f:
        diccionario = json.load(f)
    with open(CLUSTERS, encoding="utf-8") as f:
        clusters = json.load(f)

    por_id = {c["topic_id"]: c for c in clusters}
    for topic_id in AMPLIAR_CLUSTERS:
        if topic_id not in por_id:
            raise SystemExit(f"AMPLIAR_CLUSTERS: no existe el cluster '{topic_id}'")
    for topic_id, _, _, terminos in NUEVOS_CLUSTERS:
        if len(terminos) != len(set(terminos)):
            raise SystemExit(f"Términos repetidos en '{topic_id}'")

    existentes = {d["term"].lower() for d in diccionario}
    nuevos = []
    for t in candidatos():
        if t in EXCLUIDOS:
            continue  # se deja en la lista del currículo como constancia; no entra al diccionario
        if len(sin_tildes(t)) < 5 or " " in t:
            raise SystemExit(f"'{t}': debe ser una sola palabra de al menos 5 letras")
        if t.lower() in existentes:
            continue
        existentes.add(t.lower())
        categoria = "PROPN" if t in NOMBRES_PROPIOS else ("ADJ" if t in ADJETIVOS else "NOUN")
        nuevos.append({"term": t, "lemma": t, "subject": "BIOLOGIA", "category": categoria,
                       "phonetic": fonetica(t)})
    # Al final y sin reordenar: ante un empate de D gana el primer término de la lista, y así los términos
    # existentes conservan sus desempates.
    diccionario.extend(nuevos)

    # Términos que ya tenían cluster antes de este script (ver REGLA): los de clusters que no son nuevos, sin
    # contar los que este mismo script les sumó con AMPLIAR_CLUSTERS (así una segunda ejecución da lo mismo).
    ids_nuevos = {topic_id for topic_id, _, _, _ in NUEVOS_CLUSTERS}
    ampliados = {t for ts in AMPLIAR_CLUSTERS.values() for t in ts}
    ya_agrupados = {t for c in clusters if c["topic_id"] not in ids_nuevos for t in c["terms"]} - ampliados

    # Un cluster solo lista términos del diccionario (los excluidos no son evidencia del tema).
    for topic_id, extra in AMPLIAR_CLUSTERS.items():
        terms = por_id[topic_id]["terms"]
        terms.extend(t for t in extra if t not in terms and t.lower() in existentes)
    for topic_id, nombre, macro, terminos in NUEVOS_CLUSTERS:
        validos = [t for t in terminos if t.lower() in existentes and t not in ya_agrupados]
        if topic_id in por_id:
            por_id[topic_id].update(topic_name=nombre, macro_id=macro, terms=validos)
        else:
            clusters.append({"topic_id": topic_id, "topic_name": nombre, "macro_id": macro, "terms": validos})

    with open(DICCIONARIO, "w", encoding="utf-8") as f:
        json.dump(diccionario, f, ensure_ascii=False, indent=2)
    with open(CLUSTERS, "w", encoding="utf-8") as f:
        json.dump(clusters, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"{len(nuevos)} términos nuevos; diccionario: {len(diccionario)} términos; clusters: {len(clusters)}")


if __name__ == "__main__":
    main()
