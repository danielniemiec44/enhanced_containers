#!/usr/bin/env python3
"""
Generator tekstur strzałek na podstawie smelt_progress.png.
Dzieli oryginalną teksturę 22x12 na dwie połówki 16x16 i tworzy 22 poziomy postępu.
"""

from PIL import Image
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
SOURCE_TEXTURE = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/textures/gui/plethora/smelt_progress.png")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "src/main/resources/assets/enhanced_containers/textures/item/furnace")
MODELS_DIR = os.path.join(PROJECT_ROOT, "src/main/resources/assets/enhanced_containers/models/item")

# Kolory
GRAY = (139, 139, 139, 255)  # Pusty/kontur


def load_source_texture():
    """Ładuje oryginalną teksturę smelt_progress.png."""
    return Image.open(SOURCE_TEXTURE)


def create_arrow_halves_with_progress(source, level, max_level=22):
    """
    Tworzy PEŁNĄ teksturę strzałki z postępem (nie dzieloną na pół).
    Używamy jednego slotu z scale żeby ominąć scissor test.
    level=0: całkowicie przezroczysta
    level=max_level: cała oryginalna (wypełniona)
    """
    src_width, src_height = source.size  # 22x12
    
    # Tworzymy płótno 16x16 (jeden slot) - tekstura będzie powiększona przez scale
    canvas = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    
    # Skalujemy teksturę 22x12 do ~14x10 żeby zmieściła się w 16x16
    # Zostawiamy margines 1px
    scale_x = 14 / src_width
    scale_y = 10 / src_height
    scale = min(scale_x, scale_y)
    
    new_width = int(src_width * scale)
    new_height = int(src_height * scale)
    
    # Wycentruj w 16x16
    x_offset = (16 - new_width) // 2
    y_offset = (16 - new_height) // 2
    
    # Oblicz ile pikseli wypełnić (od lewej)
    if level <= 0:
        fill_end_x = -1
    else:
        fill_pixels = int(round((src_width * level) / max_level))
        fill_end_x = fill_pixels - 1
    
    # Skopiuj piksele ze skalowaniem (nearest neighbor)
    for dy in range(new_height):
        for dx in range(new_width):
            # Mapuj z nowej pozycji do źródłowej
            sx = int(dx / scale)
            sy = int(dy / scale)
            
            if sx < src_width and sy < src_height:
                src_pixel = source.getpixel((sx, sy))
                
                # Jeśli piksel jest widoczny I jest w wypełnionym obszarze
                if src_pixel[3] > 0 and sx <= fill_end_x:
                    canvas.putpixel((x_offset + dx, y_offset + dy), src_pixel)
    
    # Zwracamy tę samą teksturę dla "left" (główna) i pustą dla "right"
    empty = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    return canvas, empty


def write_model_json(model_name, texture_path):
    """Zapisuje plik modelu JSON z scale i translation dla jednego slotu."""
    os.makedirs(MODELS_DIR, exist_ok=True)
    data = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": texture_path
        }
    }
    
    # Tylko dla lewej połówki (głównej) dodajemy display z scale
    if "arrow_left" in model_name:
        data["display"] = {
            "gui": {
                "rotation": [0, 0, 0],
                "translation": [8, 0, 0],  # przesuń w prawo żeby wystawać na sąsiedni slot
                "scale": [1.8, 1.8, 1.0]   # powiększ żeby objąć oba sloty
            }
        }
    # arrow_right będzie pustą teksturą, bez display
    
    path = os.path.join(MODELS_DIR, f"{model_name}.json")
    with open(path, "w") as f:
        json.dump(data, f, indent=2)


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    print(f"Ładuję teksturę: {SOURCE_TEXTURE}")
    source = load_source_texture()
    print(f"Rozmiar: {source.size}")
    print()
    
    print("Generuję tekstury strzałek...")
    
    for level in range(23):  # 0-22
        left, right = create_arrow_halves_with_progress(source, level, 22)
        
        if level == 0:
            left_name = "arrow_left_empty"
            right_name = "arrow_right_empty"
        else:
            left_name = f"arrow_left_{level}"
            right_name = f"arrow_right_{level}"
        
        # Zapisz tekstury
        left.save(os.path.join(OUTPUT_DIR, f"{left_name}.png"))
        right.save(os.path.join(OUTPUT_DIR, f"{right_name}.png"))
        
        # Zapisz modele JSON
        write_model_json(left_name, f"enhanced_containers:item/furnace/{left_name}")
        write_model_json(right_name, f"enhanced_containers:item/furnace/{right_name}")
        
        print(f"  ✓ {left_name} / {right_name}")
    
    print()
    print("✅ Wygenerowano 46 tekstur strzałek z smelt_progress.png!")


if __name__ == "__main__":
    main()
