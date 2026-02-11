#!/usr/bin/env python3
"""
Generuje default.json z poprawnym formatem dla wszystkich providerów fontów.

Pozycjonowanie Y strzałek przez ascent:
- Tło GUI (ex_furnace.png) ma ascent=13, height=125
- Strzałki muszą być w różnych rzędach GUI, co wymaga różnych ascentów
- Ujemne ascenty przesuwają glyph w dół względem baseline tekstu
"""
import json
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUTPUT_FILE = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/font/default.json")

# Ascenty dla 3 rzędów strzałek
# Linia 0 (ascent=-24) jest idealna, dostosowuję pozostałe proporcjonalnie
# Rzędy GUI są rozstawione co ~18px, ale może potrzeba korekty
ARROW_ASCENTS = [-24, -40, -56]  # Linia 0 idealna, reszta o 16px niżej każda

providers = [
    {"type": "bitmap", "file": "minecraft:gui/plethora/ex_crafting.png", "ascent": 13, "height": 125, "chars": ["\uE001"]},
    {"type": "bitmap", "file": "minecraft:gui/plethora/ex_furnace.png", "ascent": 13, "height": 125, "chars": ["\uE002"]},
    {"type": "bitmap", "file": "minecraft:gui/plethora/lit_progress.png", "ascent": 7, "height": 14, "chars": ["\uE010"]},
    {"type": "bitmap", "file": "minecraft:gui/plethora/smelt_progress.png", "ascent": 6, "height": 12, "chars": ["\uE020"]},
]

# Arrow files: arrow_empty, arrow_1 ... arrow_22
arrow_files = ["arrow_empty.png"] + [f"arrow_{i}.png" for i in range(1, 23)]

# 3 sets of arrow chars - each with DIFFERENT ascent for different Y position
# Line 0: E030-E046 (row 1 in GUI)
# Line 1: E050-E066 (row 2 in GUI)
# Line 2: E070-E086 (row 3 in GUI)
line_bases = [0xE030, 0xE050, 0xE070]
for line_idx, base_char in enumerate(line_bases):
    ascent = ARROW_ASCENTS[line_idx]
    for i, fname in enumerate(arrow_files):
        char = chr(base_char + i)
        providers.append({
            "type": "bitmap",
            "file": f"minecraft:font/{fname}",
            "ascent": ascent,
            "height": 12,
            "chars": [char]
        })

# Space provider with negative AND positive space characters
providers.append({
    "type": "space",
    "advances": {
        # Negative spaces (cofanie kursora w lewo)
        "\uF801": -1,
        "\uF802": -2,
        "\uF804": -4,
        "\uF808": -2,   # Legacy dla crafting GUI
        "\uF809": -8,
        "\uF80A": -16,
        "\uF80B": -32,
        "\uF80C": -64,
        "\uF80D": -128,
        # Positive spaces (przesuwanie kursora w prawo)
        "\uF811": 1,
        "\uF812": 2,
        "\uF814": 4,
        "\uF818": 8,
        "\uF819": 16,
        "\uF81A": 32,
        "\uF81B": 64,
        "\uF81C": 128
    }
})

data = {"providers": providers}

with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print(f"Written {len(providers)} providers to {OUTPUT_FILE}")
print(f"  - 4 base textures")
print(f"  - {3*23} arrow chars (3 lines x 23 frames)")
print(f"    Line 0 (E030-E046): ascent={ARROW_ASCENTS[0]}")
print(f"    Line 1 (E050-E066): ascent={ARROW_ASCENTS[1]}")
print(f"    Line 2 (E070-E086): ascent={ARROW_ASCENTS[2]}")
print(f"  - 1 space provider with 9 advances")
