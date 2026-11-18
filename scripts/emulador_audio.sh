#!/usr/bin/env bash
# Reproduce grabaciones como si fueran el micrófono del emulador de Android (PipeWire).
#
#   bash scripts/emulador_audio.sh grabacion1.m4a [grabacion2.wav ...]
#
# Requisitos: emulador arrancado con -allow-host-audio, "Virtual microphone uses host audio input" activado
# (Extended controls > Microphone) y la app ESCUCHANDO (INICIAR MICRÓFONO). Necesita pw-link, pactl y ffmpeg.
#
# Qué hace: crea una fuente de audio temporal, desconecta la entrada del emulador de su fuente actual
# (micrófono / EasyEffects), le conecta la fuente temporal, reproduce los archivos con una pausa entre uno
# y otro (3 s) y, al terminar (o con Ctrl+C), deja las conexiones como estaban.
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Uso: bash $0 audio1 [audio2 ...]" >&2
    exit 1
fi
for f in "$@"; do
    [ -f "$f" ] || { echo "No existe: $f" >&2; exit 1; }
done
for cmd in pw-link pactl ffmpeg; do
    command -v "$cmd" >/dev/null || { echo "Falta $cmd" >&2; exit 1; }
done

QL=qemu-system-x86_64:input_FL
QR=qemu-system-x86_64:input_FR
SRC=emu_archivo
TMP=$(mktemp -d)
FIFO="$TMP/mic.fifo"
MOD=""

# Fuentes conectadas ahora mismo a un puerto de entrada del emulador
fuentes_de() {
    pw-link -l | awk -v q="$1" '$0 == q { f = 1; next } f && /^  \|<- / { sub(/^  \|<- /, ""); print; next } { f = 0 }'
}

echo "Esperando la entrada de audio del emulador (pulsa INICIAR MICRÓFONO en la app)..."
for _ in $(seq 60); do
    pw-link -i | grep -qx "$QL" && break
    sleep 1
done
pw-link -i | grep -qx "$QL" || { echo "No encuentro la entrada del emulador. ¿Está la app escuchando?" >&2; exit 1; }

mapfile -t PREV_L < <(fuentes_de "$QL")
mapfile -t PREV_R < <(fuentes_de "$QR")

limpiar() {
    pw-link -d "$SRC:capture_MONO" "$QL" 2>/dev/null || true
    pw-link -d "$SRC:capture_MONO" "$QR" 2>/dev/null || true
    [ -n "$MOD" ] && pactl unload-module "$MOD" 2>/dev/null || true
    for s in "${PREV_L[@]}"; do [ -n "$s" ] && pw-link "$s" "$QL" 2>/dev/null || true; done
    for s in "${PREV_R[@]}"; do [ -n "$s" ] && pw-link "$s" "$QR" 2>/dev/null || true; done
    rm -rf "$TMP"
    echo "Conexiones de audio restauradas."
}
trap limpiar EXIT

MOD=$(pactl load-module module-pipe-source source_name=$SRC file="$FIFO" \
    format=s16le rate=16000 channels=1 source_properties=device.description=MicEmuladorArchivo)
for _ in $(seq 20); do
    pw-link -o | grep -qx "$SRC:capture_MONO" && break
    sleep 0.5
done

for s in "${PREV_L[@]}"; do [ -n "$s" ] && pw-link -d "$s" "$QL" 2>/dev/null || true; done
for s in "${PREV_R[@]}"; do [ -n "$s" ] && pw-link -d "$s" "$QR" 2>/dev/null || true; done
pw-link "$SRC:capture_MONO" "$QL"
pw-link "$SRC:capture_MONO" "$QR"

n=0
{
    for f in "$@"; do
        n=$((n + 1))
        echo "[$n/$#] $(basename "$f")" >&2
        ffmpeg -nostdin -loglevel error -re -i "$f" -f s16le -ar 16000 -ac 1 -
        # 3 s de "silencio" con ruido de fondo muy bajo (≈ -60 dB). Con 2 s Vosk a veces no cierra el enunciado (su regla
        # sin condiciones exige 2.0 s de silencio) y une varias frases hasta su límite de 20 s
        ffmpeg -nostdin -loglevel error -f lavfi -i "anoisesrc=color=pink:amplitude=0.001:sample_rate=16000:duration=3" \
            -f s16le -ac 1 -
    done
} > "$FIFO"

sleep 3 # margen para que llegue el último resultado final
echo "Listo. Revisa: adb logcat -d -s VoskSpeechEngine:I | grep ASR02"
