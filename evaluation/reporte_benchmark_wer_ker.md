# Reporte Experimental de Benchmarking: WER y KER (Modelo Tri-Variable Mamdani)

**Fecha de Ejecución:** Evaluación experimental automatizada
**Modelo:** Mamdani 27 reglas $(C, D, S)$ con Defuzzificación COG (100 pasos)

## 1. Resumen Global Consolidado

| Métrica | Valor |
|---|---|
| Total Palabras de Referencia ($N$) | 103 |
| Total Palabras Clave Curriculares | 31 |
| Total Sustituciones Silenciosas | 21 |
| WER Promedio ASR Crudo (Línea Base) | 18.45% |
| WER Promedio con Capa Difusa | 1.94% |
| **Reducción Relativa de WER ($\Delta\text{WER}_{\text{rel}}$)** | **89.47%** |
| KER Promedio ASR Crudo (Pretest) | 61.29% |
| KER Promedio con Capa Difusa (Postest) | 6.45% |
| **Reducción Relativa de KER ($\Delta\text{KER}_{\text{rel}}$)** | **89.47%** |
| **Precisión Semántica Final ($1 - \text{KER}$)** | **93.55%** |
| Coherencia Semántica Media ($S$) | 0.50 |
| Latencia Media de Procesamiento | 73.98 ms |

## 2. Rendimiento Discriminado por Asignatura

| Asignatura | WER Crudo | WER Difuso | KER Crudo | KER Difuso | Precisión Semántica | S Promedio |
|---|---|---|---|---|---|---|
| BIOLOGIA | 18.4% | 1.9% | 61.3% | 6.5% | 93.5% | 0.50 |
