#!/usr/bin/env python3
"""
Generator tekstur strzałek dla Custom Font (metoda tytułu GUI).
Tworzy 23 klatek animacji (0-22) jako osobne tekstury do użycia w font/default.json.
"""

from PIL import Image
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
SOURCE_TEXTURE = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/textures/gui/plethora/smelt_progress.png")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "src/main/resources/assets/enhanced_containers/textures/font")
FONT_JSON = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/font/default.json")


def create_arrow_frame(source, level, max_level=22):
    """
    Tworzy klatkę animacji strzałki.
    level=0: całkowicie przezroczysta
    level=max_level: cała oryginalna (wypełniona)
    """
    src_width, src_height = source.size  # 22x12
    
    # Tworzymy kopię o tym samym rozmiarze co źródło
    canvas = Image.new('RGBA', (src_width, src_height), (0, 0, 0, 0))
    
    # Oblicz ile pikseli wypełnić (od lewej)
    if level <= 0:
        fill_end_x = -1
    else:
        fill_pixels = int(round((src_width * level) / max_level))
        fill_end_x = fill_pixels - 1
    
    # Skopiuj tylko wypełnione piksele
    for y in range(src_height):
        for x in range(src_width):
            src_pixel = source.getpixel((x, y))
            if src_pixel[3] > 0 and x <= fill_end_x:
                canvas.putpixel((x, y), src_pixel)
    
    return canvas


def update_font_json(font_path):
    """Aktualizuje default.json o nowe znaki strzałek."""
    with open(font_path, 'r') as f:
        font_data = json.load(f)
    
    # Usuń stare wpisy arrow_ jeśli istnieją
    font_data['providers'] = [
        p for p in font_data['providers'] 
        if not (p.get('type') == 'bitmap' and 'arrow_' in p.get('file', ''))
    ]
    
    # Dodaj nowe wpisy dla każdej klatki (Unicode: E030-E046)
    for level in range(23):
        unicode_char = chr(0xE030 + level)  # \uE030 - \uE046
        
        if level == 0:
            filename = "enhanced_containers:font/arrow_empty.png"
        else:
            filename = f"enhanced_containers:font/arrow_{level}.png"
        
        font_data['providers'].append({
            "type": "bitmap",
            "file": filename,
            "ascent": 6,  # Takie samo jak smelt_progress
            "height": 12,  # Takie samo jak smelt_progress
            "chars": [unicode_char]
        })
    
    # Dodaj negative space characters jeśli nie ma
    # Te pozwolą na precyzyjne pozycjonowanie
    has_neg_space = any(
        p.get('type') == 'space' and '\uF801' in p.get('advances', {})
        for p in font_data['providers']
    )
    
    if not has_neg_space:
        # Dodaj różne wielkości negative space
        font_data['providers'].append({
            "type": "space",
            "advances": {
                "\uF801": -1,
                "\uF802": -2,
                "\uF804": -4,
                "\uF808": -8,
                "\uF810": -16,
                "\uF820": -32,
                "\uF840": -64,
                "\uF880": -128
            }
        })
    
    with open(font_path, 'w') as f:
        json.dump(font_data, f, indent=2, ensure_ascii=False)


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    print(f"Ładuję teksturę: {SOURCE_TEXTURE}")
    source = Image.open(SOURCE_TEXTURE)
    print(f"Rozmiar: {source.size}")
    print()
    
    print("Generuję tekstury strzałek dla Custom Font...")
    
    for level in range(23):
        frame = create_arrow_frame(source, level, 22)
        
        if level == 0:
            filename = "arrow_empty.png"
        else:
            filename = f"arrow_{level}.png"
        
        frame.save(os.path.join(OUTPUT_DIR, filename))
        print(f"  ✓ {filename} (Unicode: \\uE{0x030 + level:03X})")
    
    print()
    print("Aktualizuję default.json...")
    update_font_json(FONT_JSON)
    
    print()
    print("✅ Gotowe!")
    print()
    print("Unicode mapping:")
    print("  \\uE030 = arrow_empty (0%)")
    print("  \\uE031 = arrow_1")
    print("  ...")
    print("  \\uE046 = arrow_22 (100%)")
    print()
    print("Negative space:")
    print("  \\uF801 = -1px, \\uF802 = -2px, \\uF804 = -4px")
    print("  \\uF808 = -8px, \\uF810 = -16px, \\uF820 = -32px")


if __name__ == "__main__":
    main()
