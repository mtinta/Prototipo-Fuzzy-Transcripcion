#!/usr/bin/env sh
# Descarga el modelo Vosk de español (≈40 MB) en app/src/main/assets/vosk-model-es (no se versiona en git).
# El archivo uuid le indica a StorageService.unpack si debe volver a copiar el modelo al actualizar la app:
# si cambias de modelo, cambia también el contenido de uuid.
set -eu
cd "$(dirname "$0")/.."
MODEL=vosk-model-small-es-0.42
DEST=app/src/main/assets/vosk-model-es
if [ -f "$DEST/am/final.mdl" ]; then
    echo "Ya existe $DEST"
    exit 0
fi
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
curl -fL -o "$TMP/modelo.zip" "https://alphacephei.com/vosk/models/$MODEL.zip"
unzip -q "$TMP/modelo.zip" -d "$TMP"
mkdir -p "$DEST"
cp -r "$TMP/$MODEL/." "$DEST/"
echo "$MODEL" > "$DEST/uuid"
echo "Modelo listo en $DEST"
