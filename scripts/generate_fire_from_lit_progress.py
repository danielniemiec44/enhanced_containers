#!/usr/bin/env python3
"""
Generuje warianty animacji ognia z oryginalnej tekstury lit_progress.png.
Każdy wariant pokazuje coraz więcej pikseli od dołu (jak w vanilla Minecraft).
"""

from PIL import Image
import os

# Ścieżki
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)

SOURCE_PATH = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/textures/gui/plethora/lit_progress.png")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/textures/font")

def generate_fire_variants():
    # Wczytaj oryginalną teksturę
    source = Image.open(SOURCE_PATH).convert("RGBA")
    width, height = source.size
    print(f"Źródło: {SOURCE_PATH}")
    print(f"Wymiary: {width}x{height}")
    
    # Usuń stare pliki fire_*.png
    for f in os.listdir(OUTPUT_DIR):
        if f.startswith("fire_") and f.endswith(".png"):
            os.remove(os.path.join(OUTPUT_DIR, f))
            print(f"Usunięto stary: {f}")
    
    # Generuj 14 wariantów (0 = pusty, 1-13 = coraz więcej ognia od dołu)
    for level in range(14):
        # Utwórz pustą przezroczystą teksturę
        variant = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        
        if level > 0:
            # Ile linii pokazać od dołu
            lines_to_show = level
            
            # Kopiuj dolne `lines_to_show` linii ze źródła
            # crop(left, upper, right, lower)
            crop_top = height - lines_to_show
            cropped = source.crop((0, crop_top, width, height))
            
            # Wklej na dole wariantu
            paste_y = height - lines_to_show
            variant.paste(cropped, (0, paste_y))
        
        # Nazwa pliku
        if level == 0:
            filename = "fire_empty.png"
        else:
            filename = f"fire_{level}.png"
        
        output_path = os.path.join(OUTPUT_DIR, filename)
        variant.save(output_path)
        print(f"Utworzono: {filename} ({level}/{height-1} linii)")
    
    print(f"\nWygenerowano 14 wariantów ognia z lit_progress.png")

if __name__ == "__main__":
    generate_fire_variants()
