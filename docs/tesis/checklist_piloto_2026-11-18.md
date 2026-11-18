# Lista para el piloto del 18/11/2026 (motor Vosk)

Complementa la Fase B de `plan_de_pruebas_ejecucion_v3.md`. Ese plan es anterior a Vosk: aquí van los comandos actualizados.

## Estado del software (revisado el 17/11)

| Verificación | Resultado |
|---|---|
| Pruebas unitarias (`./gradlew testDebugUnitTest`) | 90 de 90 OK (paridad Kotlin↔Python en 776 frases) |
| APK debug | Compila; firma SHA-256 `7e3ac082…` (la del teléfono: se instala encima sin borrar datos) |
| APK SHA-256 (17/11, con química) | `e9009103a18b1491f299bdc1ea3087c5ab00c550f54c9d052da8ea7533d97fa8` |
| Diccionario | **974 términos en 39 áreas temáticas** (18/11): los 650 anteriores (biología + química, `scripts/agregar_quimica_ciclos.py`) + 324 de CyT de 1.º a 3.º, con física y Tierra y universo (`scripts/agregar_cyt_primero_tercero.py`, ver `evaluation/reporte_cyt_primero_tercero.md`). Respaldo del de 650: `evaluation/respaldo_diccionario_650/` |
| Pruebas tras ampliar el diccionario | 100 de 100 OK (paridad Kotlin↔Python regenerada). APK SHA-256 `16862b3a9f09d67fcd0b0d2855fbfd08a99d61a17f30a57701d00550b900fdcc` |
| Diccionario (18/11, después de esta revisión) | Ampliado a **974 términos y 39 áreas** con CyT de 1.º a 3.º (física, Tierra y universo, materia, salud…; `scripts/agregar_cyt_primero_tercero.py`, `evaluation/reporte_cyt_primero_tercero.md`). Solo si se instala un APK compilado después de esto, el logcat dirá 974 términos, 39 clusters; el APK `e9009103…` sigue con 650 |
| Carga del diccionario | Corregido: la app usa siempre el del APK (antes podía quedarse con la copia de la primera instalación en Room) |
| `verificar_fcl.py` | OK (diferencia máx. 3.3e-16) |
| `ejecutor_pruebas_calidad.py` | 4 de 4 OK |
| `benchmark_runner.py --vosk` | Reproduce el reporte del 15/11 (prueba: WER 37.5 → 31.0 %; controles sin cambios) |
| `evaluar_sesiones.py` | **Fallaba** al leer `biologia_vosk_oov.json`; corregido el 17/11 y probado con los CSV del 04/11 |
| Grabación del audio del docente (17/11) | Nueva: con Vosk, la app guarda en `evaluacion/audio/` un WAV 16 kHz (el mismo audio que reconoce Vosk) con el nombre del CSV + `_a01`, `_a02`… e `indice_audio.csv` para ubicar cada enunciado. Interruptor en el panel (activado por defecto). 6 pruebas nuevas OK. **Cambia la APK: el SHA-256 de arriba ya no vale** |
| Datos en carpeta pública, una por sesión (18/11) | La app guarda todo en `Documentos/PrototipoFuzzy/` (visible en el explorador y por USB; no se borra al desinstalar): una carpeta `sesion_<fecha>_<participante>_<condición>/` con el CSV, `_meta.txt`, los WAV e `indice_audio.csv`; SUS y NASA-TLX en la raíz. Al abrirse copia ahí, ordenado por sesión, lo de la carpeta privada anterior (el original queda). `evaluar_sesiones.py` lee ambos formatos. **Cambia la APK** |
| Modo alumno (18/11) | Nuevo: panel → BLOQUEAR PARA EL ALUMNO. Horizontal fija, app fijada (Inicio/Recientes/Atrás no salen), barras ocultas, pantalla siempre encendida mientras escucha; el alumno solo desplaza el texto (ya no lo devuelve al final si subió a releer). Salir: pulsación larga en la esquina superior derecha + PIN **2580**. Probado en el emulador |
| Vosk en el Tecno Spark 20 con SX26 | **Nunca probado.** Solo en el emulador. Es el riesgo principal: ensayarlo esta noche |

## Esta noche

### 1. Respaldar e instalar (10 min)
```sh
cd ~/Projects/"Prototipo fuzzy"/Prototipo-Fuzzy-Transcripcion
alias adb=~/Android/Sdk/platform-tools/adb
adb pull /sdcard/Android/data/com.unsa.dhh.captions/files/evaluacion/ ~/respaldo_telefono_2026-11-17/
adb install -r app/build/outputs/apk/debug/app-debug.apk      # NUNCA desinstalar
sha256sum app/build/outputs/apk/debug/app-debug.apk | tee docs/tesis/evidencias/apk_sha256_piloto.txt
```
Si sale `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: no desinstalar; revisar la clave (`~/.config/.android/debug.keystore`).

### 2. Ensayo en casa con el SX26 (30 min)
Panel (pulsación larga en "Prueba"): participante `INV`, etiqueta `ensayo`, motor **Vosk**, T2.

| ✔ | Prueba | Qué debe pasar |
|---|---|---|
| ☐ | Modo avión; abrir la app | "Preparando el modelo…" desaparece. Anotar segundos: ____ |
| ☐ | `adb logcat -d -s CaptionsViewModel:I \| grep "Diccionario activo"` | Dice **974 términos, 39 clusters** (si dice 650 o 403, el teléfono sigue con un diccionario viejo) |
| ☐ | Dictar "el cloruro de sodio es una sal y el metano un hidrocarburo" | Aparece en el historial algún término de química si Vosk lo partió; ninguna palabra común cambió |
| ☐ | Receptor SX26 conectado antes de iniciar | La barra dice "SX26 inalámbrico" |
| ☐ | Leer bloques 1 y 2 del guion con pausas | Una línea por frase; texto definitivo ≈ 1 s tras terminar |
| ☐ | Desconectar el receptor, hablar, reconectar | Sigue con el micrófono del teléfono y vuelve al SX26 sin tocar nada |
| ☐ | Cambiar a T0 → Aplicar → leer una frase | Pantalla limpia; muestra el texto crudo |
| ☐ | 10 min con un video de clase a 1 m del transmisor | No se cierra; anotar batería y si se calienta |
| ☐ | Mientras escucha: el pie dice "● Grabando audio" | En el panel aparece `Grabando: sesion_…_a01.wav (en la carpeta de la sesión)` |
| ☐ | Al terminar: `adb shell ls -l /sdcard/Documents/PrototipoFuzzy/sesion_*/` (o en el teléfono: Archivos → Documentos → PrototipoFuzzy) | Un WAV por tramo que crece ~1.9 MB por minuto, e `indice_audio.csv` |
| ☐ | Panel → BLOQUEAR PARA EL ALUMNO → aceptar "Fijar" en el aviso del sistema | Gira a horizontal y desaparece la etiqueta roja **SIN BLOQUEAR** (arriba a la izquierda); Inicio, Recientes, Atrás y la barra de notificaciones no sacan de la app |
| ☐ | Girar el teléfono con el bloqueo puesto | Sigue en horizontal |
| ☐ | Dejar el teléfono 5 min sin tocar (con silencio) | La pantalla NO se apaga |
| ☐ | Gesto de desfijar del sistema (mantener Atrás+Recientes, o deslizar arriba y mantener) | Pide el PIN/patrón del TELÉFONO (si no lo pide, revisar Ajustes, sección 3) |
| ☐ | Pulsación larga en la esquina superior derecha → PIN 2580 → ✓ | Se desbloquea y abre el panel |
| ☐ | SUS de prueba | Guarda sin mostrar el puntaje |

Luego:
```sh
adb pull /sdcard/Documents/PrototipoFuzzy/ ensayo/
grep -c "Vosk local" ensayo/sesion_*ensayo*.csv     # perfil_asr debe ser Vosk
python3 evaluation/evaluar_sesiones.py ensayo/ --plantilla --recortar   # ventanas en el WAV + un clip por enunciado
```
**Si Vosk falla en el teléfono:** el motor **Android** da subtítulos (funcionó el 04/11, con C fija 0.65) pero **no graba audio**, y no hay otro equipo para grabar. Avisar esta misma noche.

### 3. Teléfono
- ☐ HiOS: *Batería →* la app en **Sin restricciones** y permitida en *Gestión de inicio de apps*.
- ☐ Bloqueo de pantalla del teléfono con PIN o patrón (que el alumno NO conozca).
- ☐ *Ajustes → Seguridad → Fijar aplicaciones / Fijación de pantalla* (el nombre varía en HiOS): **activado**, y **"Pedir PIN/patrón para dejar de fijar"** activado. Sin esto, el alumno podría desfijar la app con el gesto del sistema.
- ☐ ≥ 1 GB libres (la grabación ocupa ~115 MB por hora; con menos de 200 MB se detiene sola y los subtítulos siguen). Brillo medio. *No molestar* activado.
- ☐ Teléfono y transmisor (y el de respaldo) al 100 %.
- ☐ **Un solo equipo:** la app graba Y transcribe con el mismo micrófono (no hay conflicto: Vosk abre el micrófono una vez y la grabación usa una copia). No abrir ninguna otra app de grabación en el teléfono: le quitaría el audio a la app.

## Mañana en el aula

- Modo avión y *No molestar* en el teléfono de la app.
- Panel: participante `E01` (o `D01` si no hay estudiante), etiqueta **`piloto`**, motor **Vosk**, mitad 1 en **T0**, mitad 2 en **T2** (cambiar en el panel → Aplicar).
- Motor **Vosk** obligatorio: con el motor Android no se graba audio (la pantalla lo avisa) y sin audio no hay referencia ni WER.
- No pulsar DETENER salvo incidente: con el micrófono detenido tampoco se graba.
- Con todo listo: panel → **BLOQUEAR PARA EL ALUMNO** → "Fijar" en el aviso del sistema. Para el NASA-TLX de mitad de clase y el cambio a T2: esquina superior derecha (pulsación larga) → PIN **2580** → girar a vertical para el cuestionario → panel → T2 → APLICAR → BLOQUEAR de nuevo.
- Si el alumno apaga la pantalla con el botón de encendido, la transcripción y la grabación siguen; al encender pide el PIN del teléfono (lo pone el investigador) y la app sigue fijada. El docente dice *"Inicio de sesión"* al empezar y después del cambio a T2.
- La clase debe ser de un tema de biología (el diccionario es de biología).
- Sin laptop conectada: el SX26 ocupa el USB-C y el CSV guarda lo necesario.
- No cambiar nada del código ni del diccionario entre hoy y la congelación.

| Si pasa esto | Hacer |
|---|---|
| El texto deja de aparecer | DETENER e INICIAR micrófono; anotar la hora |
| La app se cierra | Abrirla, panel con los mismos datos y Aplicar (abre un archivo nuevo; está bien) |
| La barra dice "Mic. del teléfono" con el SX26 puesto | Desconectar el receptor, esperar 3 s, reconectar |
| Al bloquear sale el aviso "La pantalla NO quedó fijada" | Desbloquear (esquina + PIN), BLOQUEAR otra vez y tocar "Fijar"/"Entendido" en el aviso del sistema |

Anotar en la ficha: hora de cada incidente, frases cortadas a la mitad, correcciones equivocadas que veas y si el texto llega tarde.

## Al volver (el mismo día)
```sh
adb pull /sdcard/Documents/PrototipoFuzzy/ ~/datos_2026-11-18/
# Una carpeta por sesión: CSV + _meta.txt + WAV + indice_audio.csv. SUS y NASA-TLX en la raíz.
python3 evaluation/evaluar_sesiones.py ~/datos_2026-11-18/ --plantilla --recortar
```
- Respaldar `~/datos_2026-11-18/` en un segundo lugar. **Audio y formularios tienen datos de menores: fuera del repositorio.**
- Llenar la hoja 12.2 del plan v3 y anotar qué ajustar antes de la congelación (B3–B4).
