#!/usr/bin/env python3
"""
Skrypt generujący konfigurację font.json dla Custom Font arrow progress w piecu.

Title Overlay Method:
- Strzałki są renderowane jako znaki w tytule GUI
- To omija scissor test który obcina itemy do granic slotów
- Każda z 3 linii pieca ma osobny zestaw znaków z innym ascent (pozycja Y)

Unicode ranges:
- Linia 0: \uE030 - \uE046 (23 znaki, ascent dla rzędu 1)
- Linia 1: \uE050 - \uE066 (23 znaki, ascent dla rzędu 2)  
- Linia 2: \uE070 - \uE086 (23 znaki, ascent dla rzędu 3)
- Negative space: \uF801-\uF880 (8 znaków dla pozycjonowania X)
- Tło pieca: \uE002
- Tło craftingu: \uE001

Każda linia ma 23 stanów:
- 0 = empty (kontur)
- 1-22 = poziomy wypełnienia
"""

import json
import os

# Konfiguracja
TEXTURE_HEIGHT = 12  # Wysokość tekstury strzałki w pikselach

# Ascent dla każdej linii (kontroluje pozycję Y)
# GUI chest: slot height = 18px, pierwszy rząd zaczyna się na ~8px od góry
# Linia 0 (slot 12): rząd 1
# Linia 1 (slot 21): rząd 2 (18px niżej)
# Linia 2 (slot 30): rząd 3 (36px niżej)
#
# Ascent określa ile pikseli od linii bazowej do góry znaku
# Dla GUI inventory, musimy eksperymentalnie dobrać wartości
# 
# Bazowy ascent dla slotu w rzędzie 1 = ~57 (w zależności od tekstury)
# Każdy kolejny rząd: -18px

LINE_0_ASCENT = 57  # Linia 0 - rząd 1 (najwyżej)
LINE_1_ASCENT = 39  # Linia 1 - rząd 2 (środek)
LINE_2_ASCENT = 21  # Linia 2 - rząd 3 (najniżej)

# Unicode starting points
LINE_0_START = 0xE030  # \uE030 - \uE046
LINE_1_START = 0xE050  # \uE050 - \uE066
LINE_2_START = 0xE070  # \uE070 - \uE086

NUM_FRAMES = 23  # 0 (empty) + 22 levels

# Ścieżka do tekstur (relative to assets/enhanced_containers/)
TEXTURE_PATH_PREFIX = "enhanced_containers:font/arrow_"


def generate_arrow_providers():
    """Generuje providery dla strzałek wszystkich 3 linii"""
    providers = []
    
    lines = [
        (LINE_0_START, LINE_0_ASCENT, "Line 0 (row 1)"),
        (LINE_1_START, LINE_1_ASCENT, "Line 1 (row 2)"),
        (LINE_2_START, LINE_2_ASCENT, "Line 2 (row 3)"),
    ]
    
    for start_unicode, ascent, comment in lines:
        for i in range(NUM_FRAMES):
            if i == 0:
                texture_file = f"{TEXTURE_PATH_PREFIX}empty.png"
            else:
                texture_file = f"{TEXTURE_PATH_PREFIX}{i}.png"
            
            char_code = start_unicode + i
            char = chr(char_code)
            
            provider = {
                "type": "bitmap",
                "file": texture_file,
                "ascent": ascent,
                "height": TEXTURE_HEIGHT,
                "chars": [char]
            }
            providers.append(provider)
    
    return providers


def generate_negative_space_provider():
    """Generuje provider dla negative space characters"""
    return {
        "type": "space",
        "advances": {
            "\uE010": -2,  # Legacy compatibility
            "\uF801": -1,
            "\uF802": -2,
            "\uF804": -4,
            "\uF808": -8,
            "\uF810": -16,
            "\uF820": -32,
            "\uF840": -64,
            "\uF880": -128
        }
    }


def generate_gui_background_providers():
    """Generuje providery dla tła GUI (crafting i furnace)"""
    return [
        {
            "type": "bitmap",
            "file": "minecraft:gui/plethora/ex_crafting.png",
            "ascent": 13,
            "height": 125,
            "chars": ["\uE001"]
        },
        {
            "type": "bitmap",
            "file": "minecraft:gui/plethora/ex_furnace.png",
            "ascent": 13,
            "height": 125,
            "chars": ["\uE002"]
        },
        {
            "type": "bitmap",
            "file": "minecraft:gui/plethora/lit_progress.png",
            "ascent": 7,
            "height": 14,
            "chars": ["\uE003"]
        },
        {
            "type": "bitmap",
            "file": "minecraft:gui/plethora/smelt_progress.png",
            "ascent": 6,
            "height": 12,
            "chars": ["\uE004"]
        }
        # Usunięty stary space provider - teraz jest jeden wspólny na końcu
    ]


def generate_font_json():
    """Generuje kompletny plik font.json"""
    providers = []
    
    # Tła GUI
    providers.extend(generate_gui_background_providers())
    
    # Strzałki dla wszystkich 3 linii
    providers.extend(generate_arrow_providers())
    
    # Negative space
    providers.append(generate_negative_space_provider())
    
    return {"providers": providers}


def main():
    # Znajdź katalog projektu
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_dir = os.path.dirname(script_dir)
    
    # Ścieżka do font.json
    font_json_path = os.path.join(
        project_dir, 
        "src/main/resources/assets/minecraft/font/default.json"
    )
    
    print(f"📁 Generuję font.json dla pieca z 3 liniami strzałek...")
    print(f"   Ścieżka: {font_json_path}")
    
    # Generuj JSON
    font_data = generate_font_json()
    
    # Zapisz
    with open(font_json_path, 'w', encoding='utf-8') as f:
        json.dump(font_data, f, indent=2, ensure_ascii=False)
    
    print(f"\n✅ Wygenerowano font.json!")
    print(f"\n📊 Podsumowanie:")
    print(f"   - Tła GUI: 4 znaki (\\uE001-\\uE004)")
    print(f"   - Strzałki linia 0: \\uE030-\\uE046 (ascent={LINE_0_ASCENT})")
    print(f"   - Strzałki linia 1: \\uE050-\\uE066 (ascent={LINE_1_ASCENT})")
    print(f"   - Strzałki linia 2: \\uE070-\\uE086 (ascent={LINE_2_ASCENT})")
    print(f"   - Negative space: \\uF801-\\uF880")
    print(f"\n💡 Użycie w Java:")
    print(f"   Line 0 empty: '\\uE030', full: '\\uE046'")
    print(f"   Line 1 empty: '\\uE050', full: '\\uE066'")
    print(f"   Line 2 empty: '\\uE070', full: '\\uE086'")


if __name__ == "__main__":
    main()
