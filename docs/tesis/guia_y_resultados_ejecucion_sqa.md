# Guía y Resultados de Ejecución: Plan de Calidad de Software (SQA)
**Proyecto:** Prototipo Fuzzy Transcripción On-Device  
**Asignatura:** Biología  
**Fecha de Ejecución:** Octubre 2026  

---

## 1. Resultados de las Pruebas Ejecutadas Automáticamente

Las pruebas de calidad de software que no requieren intervención motriz o hardware físico externo fueron ejecutadas en el entorno local con un **100% de éxito**.

### Resumen de Resultados de Pruebas Automatizadas

```
======================================================================
 RESUMEN DE EJECUCIÓN DE PRUEBAS AUTOMATIZADAS (PASS / FAIL)
======================================================================
 - TC-DOM-01 (Integridad de Biología y Topic Clusters) : PASÓ (PASS)
 - TC-ACC-01 (Contraste Cromático WCAG 2.1 AAA)        : PASÓ (PASS)
 - TC-PERF-01 (Latencia Inferencia Difusa Mamdani COG) : PASÓ (PASS)
 - TC-AUDIO-02 (Lógica Fallback USB SX26/Maxtron -> Mic) : PASÓ (PASS)
 - SUITE UNITARIA ANDROID (Gradle testDebugUnitTest)   : PASÓ (PASS)
======================================================================
```

### Detalle de Mediciones Obtenidas

| Caso de Prueba | Métrica Evaluada | Umbral Exigido | Resultado Real Medido | Estado |
| :--- | :--- | :--- | :--- | :---: |
| **TC-DOM-01** | Cobertura Léxica Biología | 100% de términos MINEDU | **403 / 403 términos (100%)** en 12 clusters temáticos | **APROBADO** |
| **TC-ACC-01** | Contraste Amarillo (`#FFFF00`) / Negro | $\ge 7.0:1$ (WCAG AAA) | **$19.56:1$** (Supera en un 279.4% la norma) | **APROBADO** |
| **TC-ACC-01** | Contraste Cian (`#00FFFF`) / Negro | $\ge 7.0:1$ (WCAG AAA) | **$16.75:1$** (Supera en un 239.3% la norma) | **APROBADO** |
| **TC-PERF-01**| Latencia de Inferencia Difusa | $\le 100.0\text{ ms}$ | **$0.0333\text{ ms}$** (P99: $0.0380\text{ ms}$) | **APROBADO** |
| **TC-AUDIO-02**| Lógica de Transición y Fallback | Fallback instantáneo en $\le 300\text{ ms}$ | Eventos USB detectados y conmutación a mic interno sin crash | **APROBADO** |

---

## 2. Comandos para Re-ejecutar las Pruebas Automatizadas

Puedes volver a ejecutar estas verificaciones en cualquier momento desde la terminal con los siguientes comandos:

```bash
# 1. Ejecutar las verificaciones de calidad de software (WCAG AAA, Inferencia, Clusters y Fallback)
python3 evaluation/ejecutor_pruebas_calidad.py

# 2. Ejecutar la suite completa de pruebas unitarias Android (KSP, Compose, Room, Evaluator, ExternalAudioDeviceManager)
export JAVA_HOME=/opt/android-studio/jbr
./gradlew testDebugUnitTest
```

---

## 3. Protocolo de Ejecución Manual en Dispositivo Físico (Hardware & Conectividad)

Para las pruebas que requieren el smartphone y el micrófono inalámbrico de solapa, sigue este procedimiento paso a paso para recopilar evidencias para tu asesor de tesis:

### Protocolo A: Prueba de Recepción y Fallback de Micrófono Inalámbrico USB-C (TC-AUDIO-01 y TC-AUDIO-02)

1. **Preparación:**
   - Ten a la mano el micrófono de solapa inalámbrico (SX26 Wireless o Maxtron 2.4 GHz) y su dongle receptor USB-C.
   - Conecta el teléfono a tu computadora mediante cable USB o Wi-Fi ADB con Depuración USB activa.
2. **Monitoreo en Vivo con ADB:**
   Abre una terminal en tu PC y filtra los logs del manejador de audio:
   ```bash
   adb logcat -s ExternalAudioDevMgr AndroidSpeechEngine AudioForegroundService
   ```
3. **Paso a Paso de la Prueba:**
   - Abre la app y conecta el dongle receptor USB-C al smartphone.
   - Enciende el micrófono transmisor de solapa del docente.
   - En la terminal de logcat deberás observar de inmediato:
     ```
     ExternalAudioDevMgr: Receptor USB-C detectado (SX26 / Maxtron 2.4 GHz): USB Audio Device
     ExternalAudioDevMgr: Fuente de audio activa: Micrófono inalámbrico de solapa (SX26/Maxtron 2.4 GHz)
     ```
   - En la pantalla de la app, el badge se actualiza a: `[Micrófono inalámbrico de solapa (SX26/Maxtron 2.4 GHz)] [2.4 GHz RF]`.
   - Presiona **"Iniciar Transcripción"**.
   - Habla frente al micrófono de solapa: *"La respiración celular se produce en las mitocondrias"*. Observa cómo el texto aparece con alta nitidez en el teleprompter.
   - **Prueba de Fallback:** Desconecta físicamente el dongle receptor USB-C del teléfono mientras hablas.
   - En la terminal de logcat deberás observar en menos de 300 ms:
     ```
     ExternalAudioDevMgr: Receptor USB desconectado físicamente. Activando fallback a micrófono interno.
     ExternalAudioDevMgr: Micrófono interno del teléfono activado (Modo Normal).
     ```
   - Habla directamente al teléfono: *"Las células vegetales poseen cloroplastos"*.
   - **Criterio de Éxito:** La transcripción continuó sin interrupción y la app nunca se cerró.

---

### Protocolo B: Prueba de Autonomía 100% Offline / Sin Conexión (TC-NET-01)

1. En el teléfono móvil, activa el **Modo Avión** (desactivando simultáneamente Wi-Fi y Datos Móviles).
2. Conecta el receptor USB-C del micrófono SX26 (no requiere Bluetooth ni red).
3. Inicia la transcripción y dicta 5 oraciones del tema de Biología Celular:
   - *"El ADN contiene adenina, timina, guanina y citosina."*
   - *"La mitosis genera dos células hijas diploides."*
4. Finaliza la sesión con el botón "Detener".
5. Ve a la pantalla de **Historial de Sesiones**.
6. **Criterio de Éxito:**
   - Las oraciones se transcribieron y se corrigieron mediante lógica difusa.
   - La sesión quedó guardada en la base de datos local SQLite (Room) con sus estadísticas de WER y KER.
   - Ninguna pantalla mostró error de conexión ni diálogo de red.

---

### Protocolo C: Prueba de Silencios Prolongados y Continuidad (TC-ROB-01 y TC-ROB-02)

1. Con la transcripción iniciada, di una frase y luego guarda **silencio total durante 25 segundos**.
2. Observa el logcat:
   ```
   AndroidSpeechEngine: onError: SpeechRecognizer error 6 (SPEECH_TIMEOUT) o 7 (NO_MATCH)
   AndroidSpeechEngine: Reiniciando escucha continua tras silencio pedagógico (delay 350ms)...
   ```
3. Vuelve a hablar con normalidad.
4. **Criterio de Éxito:** El sistema no se congela; se auto-recupera silenciosamente y continúa recibiendo audio sin obligar al usuario a reiniciar la app.

---

### Protocolo D: Medición de Consumo de Memoria RAM (TC-PERF-02)

Con la app ejecutando una sesión de transcripción, corre en la terminal:
```bash
adb shell dumpsys meminfo com.unsa.dhh.captions | grep -E "TOTAL|Native Heap|Dalvik Heap"
```
* **Criterio de Éxito:** El campo `TOTAL PSS:` debe situarse entre **80 MB y 140 MB**, lo cual representa menos del 4% de la memoria de un teléfono de 4 GB RAM.
