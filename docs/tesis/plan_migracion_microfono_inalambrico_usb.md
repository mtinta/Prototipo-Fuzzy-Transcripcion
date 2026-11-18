# Plan de Implementación: Migración de Audio a Micrófono de Solapa Inalámbrico (SX26 / Maxtron 2.4 GHz)
**Proyecto:** Prototipo de Subtitulado Inteligente On-Device para Estudiantes con Discapacidad Auditiva (UNSA)  
**Autor:** Miguel Angel Tinta Aguilar  
**Asesor:** Dr. Karim Guevara Puente de la Vega  
**Fecha:** Octubre 2026  

---

## 1. Justificación Técnica y Beneficios del Cambio de Hardware

El proyecto migra del perfil **Bluetooth SCO** tradicional a un sistema de micrófonos inalámbricos digitales de 2.4 GHz con receptor dongle USB-C (modelos **SX26 Wireless** o **Maxtron Wireless de solapa**).

### Cuadro Comparativo: Bluetooth SCO vs. Micrófono Inalámbrico 2.4 GHz (SX26 / Maxtron)

| Criterio Técnico | Arquitectura Anterior (Bluetooth SCO) | Nueva Arquitectura (SX26 / Maxtron USB-C) | Impacto en la Calidad del Sistema |
| :--- | :--- | :--- | :--- |
| **Frecuencia de Muestreo** | 8 kHz / 16 kHz (perfil llamadas de voz) | **44.1 kHz / 48 kHz (PCM Digital nativo)** | Mayor nitidez en consonantes fonéticas ("s", "c", "z", "b", "v"). |
| **Enlace Físico** | Inalámbrico Bluetooth hacia el SoC | Dongle receptor USB-C Plug & Play | Sin fallos de emparejamiento ni desconexión por timeout. |
| **Latencia de Audio** | 120 – 180 ms (negociación de códec SCO) | **$< 20\text{ ms}$ (enlace RF directo 2.4 GHz)** | Reducción de latencia end-to-end en el teleprompter. |
| **Modo de Audio Android** | Requiere `MODE_IN_COMMUNICATION` | Opera en **`MODE_NORMAL` estándar** | Evita la degradación del DSP telefónico y filtros restrictivos. |
| **Permisos de Android** | Requiere `BLUETOOTH_CONNECT` (Android 12+) | **Ningún permiso especial de red/BT requerido** | Cumple con máxima privacidad y compatibilidad de Android. |
| **Autonomía Docente** | Micrófono sujeto al stack BT del móvil | Batería independiente en emisor (8+ horas) | Soporta jornadas escolares completas sin cortes. |

---

## 2. Alcance del Plan de Implementación

El plan comprende las siguientes 4 fases técnicas:
1. **Fase 1 (Arquitectura & Abstracción de Audio):** Creación de `ExternalAudioDeviceManager.kt` para detección reactiva de dongles USB Audio y auriculares con micrófono mediante `AudioDeviceCallback`.
2. **Fase 2 (Refactorización de Servicios y ViewModel):** Sustitución de `BluetoothScoManager` en `AudioForegroundService` y actualización de los estados en `CaptionsViewModel` y UI.
3. **Fase 3 (Depuración de Manifiesto y Permisos):** Limpieza de permisos de Bluetooth obsoletos en `AndroidManifest.xml` y habilitación de filtros de intent para dispositivos USB (`android.hardware.usb.host`).
4. **Fase 4 (Actualización de SQA y Plan de Pruebas):** Actualización de los casos de prueba [TC-AUDIO-01] y [TC-AUDIO-02] en el Plan de Calidad de Software y verificación en hardware real.

---

## 3. Desglose de Actividades y Tickets de Implementación

### TICKET-MIC-01: Creación del Módulo `ExternalAudioDeviceManager.kt`
* **Ubicación:** `app/src/main/java/com/unsa/dhh/captions/service/ExternalAudioDeviceManager.kt`
* **Responsabilidad:**
  - Registrar un `AudioDeviceCallback` sobre `AudioManager` (API 23+).
  - Escuchar eventos `onAudioDevicesAdded` y `onAudioDevicesRemoved`.
  - Detectar si el dispositivo de entrada conectado corresponde a:
    - `AudioDeviceInfo.TYPE_USB_DEVICE`
    - `AudioDeviceInfo.TYPE_USB_HEADSET`
    - `AudioDeviceInfo.TYPE_WIRED_HEADSET` (en caso de dongle 3.5mm TRRS)
  - Exponer un flujo reactivo `StateFlow<AudioInputSource>` con estados:
    - `INTERNAL_MIC` (Micrófono integrado del teléfono)
    - `WIRELESS_USB_MIC` (Receptor SX26 / Maxtron conectado)
    - `WIRED_HEADSET_MIC` (Micrófono alámbrico 3.5mm)
  - Ejecutar fallback automático transparente: Si se retira el dongle USB-C, conmuta automáticamente a `INTERNAL_MIC` sin detener la grabación.

### TICKET-MIC-02: Refactorización de `AudioForegroundService.kt`
* **Ubicación:** `app/src/main/java/com/unsa/dhh/captions/service/AudioForegroundService.kt`
* **Cambios:**
  - Reemplazar la instancia de `BluetoothScoManager` por `ExternalAudioDeviceManager`.
  - En `startServiceExecution()`, inicializar el monitoreo de dispositivos de audio sin invocar `startBluetoothSco()`.
  - Mantener el modo de audio en `AudioManager.MODE_NORMAL`.
  - Actualizar la notificación persistente para reflejar el estado: *"Micrófono Inalámbrico SX26/Maxtron Activo"*.

### TICKET-MIC-03: Actualización de la Interfaz de Usuario y ViewModel
* **Ubicación:**
  - `app/src/main/java/com/unsa/dhh/captions/presentation/viewmodel/CaptionsViewModel.kt`
  - `app/src/main/java/com/unsa/dhh/captions/presentation/ui/TeleprompterScreen.kt`
* **Cambios:**
  - Agregar estado observable `audioSourceState` (`INTERNAL_MIC` vs `WIRELESS_USB_MIC`).
  - Sustituir el icono de Bluetooth por un indicador de **"Micrófono Inalámbrico USB Activo"** (icono de micrófono con ondas o dongle USB).
  - Mostrar alerta flotante breve (Toast/Snackbar) cuando el receptor sea conectado o desconectado:
    - Conectado: *"Micrófono de solapa inalámbrico SX26/Maxtron conectado"* (Verde/Cian).
    - Desconectado: *"Dongle USB desconectado. Usando micrófono integrado"* (Amarillo/Alerta).

### TICKET-MIC-04: Limpieza de Permisos en `AndroidManifest.xml`
* **Ubicación:** `app/src/main/AndroidManifest.xml`
* **Cambios:**
  - Eliminar permisos que ya no son requeridos:
    - `<uses-permission android:name="android.permission.BLUETOOTH" />`
    - `<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />`
    - `<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />`
  - Conservar únicamente:
    - `RECORD_AUDIO` (Esencial para captura)
    - `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_MICROPHONE`
    - `POST_NOTIFICATIONS` y `WAKE_LOCK`
  - Declarar compatibilidad con hardware USB host opcional:
    ```xml
    <uses-feature android:name="android.hardware.usb.host" android:required="false" />
    ```

### TICKET-MIC-05: Actualización del Plan de Calidad de Software (SQA)
* **Ubicación:** `docs/tesis/plan_de_pruebas_calidad_software.md` y `evaluation/ejecutor_pruebas_calidad.py`
* **Cambios:**
  - **TC-AUDIO-01:** Reemplazar "Negociación Bluetooth SCO" por **"Detección y Enlace de Receptor USB-C 2.4 GHz (SX26 / Maxtron)"** con tiempo de detección $\le 500\text{ ms}$.
  - **TC-AUDIO-02:** Reemplazar "Pérdida de cobertura Bluetooth" por **"Desconexión Física del Dongle USB-C y Fallback Inmediato al Micrófono Interno"**.
  - Actualizar el runner automatizado `ejecutor_pruebas_calidad.py` para simular la nueva máquina de estados USB.

---

## 4. Matriz de Riesgos y Medidas de Mitigación

| Riesgo Técnico Identificado | Nivel de Impacto | Probabilidad | Medida de Mitigación Implementada |
| :--- | :---: | :---: | :--- |
| El puerto USB-C del teléfono está ocupado por el receptor y no permite cargar la batería durante clases muy largas. | Medio | Baja | Los receptores SX26 y Maxtron incorporan un puerto **USB-C Pass-Through** en el lateral del dongle, permitiendo conectar el cargador simultáneamente. |
| Modelos de teléfonos antiguos que no rutean automáticamente audio USB a `SpeechRecognizer`. | Medio | Baja | Implementar selección explícita mediante `AudioRecord.setPreferredDevice(usbDevice)` disponible desde Android 6.0 (API 23). |
| Desconexión accidental del dongle por movimiento del teléfono en el pupitre. | Bajo | Media | Fallback inmediato en $\le 300\text{ ms}$ al micrófono interno sin pausar ni corromper el stream de audio. |

---

## 5. Cronograma Estimado de Ejecución

| Fase / Ticket | Tarea Principal | Tiempo Estimado | Responsable |
| :--- | :--- | :---: | :--- |
| **Ticket 01** | Implementar `ExternalAudioDeviceManager.kt` y tests unitarios | 45 min | Desarrollador / Agente |
| **Ticket 02** | Integrar en `AudioForegroundService.kt` y eliminar Bluetooth SCO | 30 min | Desarrollador / Agente |
| **Ticket 03** | Actualizar `CaptionsViewModel.kt` y UI Teleprompter con indicador USB | 30 min | Desarrollador / Agente |
| **Ticket 04** | Limpiar `AndroidManifest.xml` de permisos Bluetooth obsoletos | 15 min | Desarrollador / Agente |
| **Ticket 05** | Actualizar documentación de tesis, plan de pruebas SQA y runner | 30 min | Desarrollador / Agente |
| **TOTAL** | **Migración completa y validada** | **2.5 horas** | |

---

## 6. Siguientes Pasos Inmediatos

¿Deseas que procedamos a implementar de inmediato los Tickets 01 al 05 para dejar la arquitectura de audio 100% actualizada con soporte para micrófonos inalámbricos USB-C SX26 y Maxtron?
