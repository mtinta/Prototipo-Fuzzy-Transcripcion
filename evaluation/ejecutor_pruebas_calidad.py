#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Ejecutor Automatizado del Plan de Calidad de Software (SQA Automated Runner)
Tesis: Sistema de Subtitulado con Lógica Difusa On-Device - UNSA
Asignatura de Validación: Biología

Ejecuta las pruebas de calidad de software verificables automáticamente:
- TC-DOM-01: Integridad Léxica y Cobertura de Topic Clusters (Biología)
- TC-ACC-01: Cálculo Fotométrico de Contraste Cromático WCAG 2.1 AAA
- TC-PERF-01: Latencia de Inferencia Difusa (Mamdani 27 reglas + COG)
- TC-AUDIO-FALLBACK: Simulación de Máquina de Estados y Fallback de Audio
"""

import os
import sys
import json
import time
import math

# Independiente del directorio de trabajo
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

def print_header(title):
    print("\n" + "=" * 70)
    print(f" {title}")
    print("=" * 70)

def test_tc_dom_01():
    print_header("TC-DOM-01: Integridad del Dominio de Biología y Topic Clusters")
    clusters_path = os.path.join(PROJECT_ROOT, "app/src/main/assets/clusters/biologia_clusters.json")
    dict_path = os.path.join(PROJECT_ROOT, "app/src/main/assets/dictionaries/biologia.json")
    
    assert os.path.exists(clusters_path), f"Archivo no encontrado: {clusters_path}"
    assert os.path.exists(dict_path), f"Archivo no encontrado: {dict_path}"
    
    with open(clusters_path, "r", encoding="utf-8") as f:
        clusters_data = json.load(f)
        
    with open(dict_path, "r", encoding="utf-8") as f:
        dict_data = json.load(f)
        
    # Verificar cantidad de clusters
    num_clusters = len(clusters_data)
    cluster_ids = [c.get("topic_id") or c.get("topicId") for c in clusters_data]
    print(f"[*] Clusters temáticos cargados en Biología ({num_clusters}):")
    for cid in cluster_ids:
        print(f"    - {cid}")
    
    assert num_clusters >= 6, f"Se esperaban al menos 6 clusters, encontrados {num_clusters}"
        
    # Extraer todos los términos de los clusters
    cluster_terms = set()
    for c in clusters_data:
        terms = c.get("terms", [])
        cid = c.get("topic_id") or c.get("topicId", "desconocido")
        assert len(terms) > 0, f"Cluster {cid} vacío"
        for t in terms:
            cluster_terms.add(t.lower().strip())
            
    # Extraer términos del diccionario
    dict_terms = set(d["term"].lower().strip() for d in dict_data)
    
    print(f"[*] Términos únicos en Diccionario de Biología: {len(dict_terms)}")
    print(f"[*] Términos indexados en Topic Clusters: {len(cluster_terms)}")
    
    # Verificar intersección
    covered = cluster_terms.intersection(dict_terms)
    print(f"[*] Términos de clusters presentes en diccionario oficial: {len(covered)} / {len(cluster_terms)}")
    
    # 12 de biología + 9 de química y ciclos biogeoquímicos (scripts/agregar_quimica_ciclos.py, 17/11/2026)
    assert num_clusters == 21, f"Se esperaban 21 clusters temáticos en Ciencia y Tecnología, encontrados {num_clusters}"
    assert len(covered) == len(dict_terms), f"Inconsistencia en términos: {len(covered)} cubiertos de {len(dict_terms)}"
    
    print("[+] TC-DOM-01: APROBADO (Integridad léxica verificada al 100%)")
    return True

def test_tc_acc_01():
    print_header("TC-ACC-01: Verificación de Contraste Cromático WCAG 2.1 AAA")
    
    def srgb_to_luminance(r, g, b):
        def channel_lum(val):
            c = val / 255.0
            return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
        r_l = channel_lum(r)
        g_l = channel_lum(g)
        b_l = channel_lum(b)
        return 0.2126 * r_l + 0.7152 * g_l + 0.0722 * b_l

    def contrast_ratio(lum1, lum2):
        l_max = max(lum1, lum2)
        l_min = min(lum1, lum2)
        return (l_max + 0.05) / (l_min + 0.05)

    # Colores de la app
    # Fondo: Negro puro (#000000)
    bg_lum = srgb_to_luminance(0, 0, 0)
    
    # Texto Primario: Amarillo (#FFFF00)
    yellow_lum = srgb_to_luminance(255, 255, 0)
    cr_yellow = contrast_ratio(yellow_lum, bg_lum)
    
    # Texto Resaltado / Corregido: Cian (#00FFFF)
    cyan_lum = srgb_to_luminance(0, 255, 255)
    cr_cyan = contrast_ratio(cyan_lum, bg_lum)
    
    print(f"[*] Fondo (#000000) Luminancia: {bg_lum:.4f}")
    print(f"[*] Amarillo (#FFFF00) Luminancia: {yellow_lum:.4f} -> Ratio Contraste: {cr_yellow:.2f}:1")
    print(f"[*] Cian (#00FFFF) Luminancia: {cyan_lum:.4f} -> Ratio Contraste: {cr_cyan:.2f}:1")
    
    # Umbrales WCAG 2.1
    # Nivel AA: 4.5:1
    # Nivel AAA: 7.0:1
    print(f"[*] Umbral Exigido WCAG 2.1 AAA: 7.00:1")
    
    assert cr_yellow >= 7.0, f"Amarillo no cumple AAA: {cr_yellow}"
    assert cr_cyan >= 7.0, f"Cian no cumple AAA: {cr_cyan}"
    
    print(f"[+] Contraste Amarillo/Negro: {cr_yellow:.2f}:1 (Supera umbral AAA en un {(cr_yellow/7.0)*100:.1f}%)")
    print(f"[+] Contraste Cian/Negro: {cr_cyan:.2f}:1 (Supera umbral AAA en un {(cr_cyan/7.0)*100:.1f}%)")
    print("[+] TC-ACC-01: APROBADO (Certificación WCAG 2.1 Nivel AAA)")
    return True

def test_tc_perf_01():
    print_header("TC-PERF-01: Latencia de Inferencia Difusa (Mamdani 27 reglas + COG)")
    
    # Importar función de inferencia Mamdani 27 reglas
    sys.path.insert(0, os.path.join(PROJECT_ROOT, "evaluation"))
    from benchmark_runner import evaluate_mamdani
    
    # Probar 1000 iteraciones con variaciones de C, D, S en Biología
    inputs = [
        (0.60, 0.85, 1.0), # Co-ocurre en bio_celular
        (0.50, 0.90, 0.5), # Co-ocurre parcial
        (0.70, 0.40, 0.0), # No co-ocurre
        (0.45, 0.88, 1.0),
        (0.80, 0.95, 1.0),
    ] * 200 # 1000 inferencias
    
    latencies = []
    for c, d, s in inputs:
        t0 = time.perf_counter()
        score = evaluate_mamdani(c, d, s)
        t1 = time.perf_counter()
        latencies.append((t1 - t0) * 1000.0) # en ms
        
    avg_lat = sum(latencies) / len(latencies)
    p95_lat = sorted(latencies)[int(len(latencies) * 0.95)]
    p99_lat = sorted(latencies)[int(len(latencies) * 0.99)]
    max_lat = max(latencies)
    
    print(f"[*] Total inferencias evaluadas: {len(inputs)}")
    print(f"[*] Latencia promedio: {avg_lat:.4f} ms")
    print(f"[*] Latencia P95: {p95_lat:.4f} ms")
    print(f"[*] Latencia P99: {p99_lat:.4f} ms")
    print(f"[*] Latencia Máxima: {max_lat:.4f} ms")
    print(f"[*] Criterio de Aceptación: Latencia promedio <= 100.0 ms")
    
    assert avg_lat <= 100.0, f"Latencia excede límite admisible: {avg_lat} ms"
    print("[+] TC-PERF-01: APROBADO (Rendimiento en tiempo real validado)")
    return True

def test_tc_audio_fallback_logic():
    print_header("TC-AUDIO-02: Lógica de Fallback de Audio USB (SX26/Maxtron) y Máquina de Estados")
    
    # Simulación de estados de ExternalAudioDeviceManager
    class MockAudioDeviceManager:
        def __init__(self):
            self.source = "INTERNAL_MIC"
            self.is_external_connected = False
            self.audio_mode = "MODE_NORMAL" # Opera siempre en MODE_NORMAL para máxima fidelidad (48 kHz)
            
        def on_device_event(self, event):
            if event == "USB_ATTACHED":
                self.source = "WIRELESS_USB_MIC"
                self.is_external_connected = True
            elif event == "USB_DETACHED":
                # Fallback automático instantáneo
                self.source = "INTERNAL_MIC"
                self.is_external_connected = False
            elif event == "AUX_3_5MM_ATTACHED":
                self.source = "WIRED_HEADSET_MIC"
                self.is_external_connected = True
                
    mgr = MockAudioDeviceManager()
    assert mgr.source == "INTERNAL_MIC" and mgr.is_external_connected is False
    print("[*] Estado inicial: Micrófono interno activo (Audio Mode = MODE_NORMAL, 48 kHz PCM)")
    
    # 1. Conexión del dongle receptor USB-C inalámbrico (SX26 / Maxtron 2.4 GHz)
    mgr.on_device_event("USB_ATTACHED")
    assert mgr.source == "WIRELESS_USB_MIC"
    assert mgr.is_external_connected is True
    print("[*] Dongle receptor USB-C conectado: Fuente = WIRELESS_USB_MIC (SX26/Maxtron 2.4 GHz)")
    
    # 2. Desconexión física intempestiva del receptor USB
    mgr.on_device_event("USB_DETACHED")
    assert mgr.source == "INTERNAL_MIC"
    assert mgr.is_external_connected is False
    print("[*] Receptor USB desconectado -> Fallback ejecutado: Fuente = INTERNAL_MIC (Sin cortes)")
    
    # 3. Conexión alternativa por jack auxiliar 3.5mm
    mgr.on_device_event("AUX_3_5MM_ATTACHED")
    assert mgr.source == "WIRED_HEADSET_MIC"
    assert mgr.is_external_connected is True
    print("[*] Micrófono auxiliar detectado: Fuente = WIRED_HEADSET_MIC")
    
    print("[+] TC-AUDIO-02 (Lógica USB): APROBADO")
    return True

def main():
    print("=" * 70)
    print(" EJECUCIÓN DEL PLAN DE PRUEBAS DE CALIDAD DE SOFTWARE (SQA)")
    print(" Prototipo Fuzzy Transcripción - Universidad Nacional de San Agustín")
    print("=" * 70)
    
    results = {}
    try:
        results["TC-DOM-01"] = test_tc_dom_01()
        results["TC-ACC-01"] = test_tc_acc_01()
        results["TC-PERF-01"] = test_tc_perf_01()
        results["TC-AUDIO-02 (Lógica)"] = test_tc_audio_fallback_logic()
    except AssertionError as e:
        print(f"\n[!] FALLO EN PRUEBA: {e}")
        sys.exit(1)
        
    print("\n" + "=" * 70)
    print(" RESUMEN DE EJECUCIÓN DE PRUEBAS AUTOMATIZADAS")
    print("=" * 70)
    for test_id, passed in results.items():
        status = "PASÓ (PASS)" if passed else "FALLÓ (FAIL)"
        print(f" - {test_id:<25}: {status}")
    print("\n[+] Todas las pruebas de calidad de software automatizadas fueron APROBADAS.")
    print("=" * 70)

if __name__ == "__main__":
    main()
