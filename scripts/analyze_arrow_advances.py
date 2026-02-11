#!/usr/bin/env python3
"""Analizuje rzeczywiste szerokości tekstur strzałek dla Zero-Width Wrapper."""
from PIL import Image
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
FONT_DIR = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/textures/font")

def get_rightmost_pixel(img):
    """Znajdź najbardziej prawy nieprzezroczysty piksel (0-indexed)"""
    width, height = img.size
    rightmost = -1
    for y in range(height):
        for x in range(width):
            pixel = img.getpixel((x, y))
            if len(pixel) >= 4 and pixel[3] > 0:
                if x > rightmost:
                    rightmost = x
    return rightmost

def main():
    files = ["arrow_empty.png"] + [f"arrow_{i}.png" for i in range(1, 23)]
    
    print("Level | Plik             | Rightmost X | Advance")
    print("-" * 55)
    
    advances = []
    for level, fname in enumerate(files):
        path = os.path.join(FONT_DIR, fname)
        if os.path.exists(path):
            img = Image.open(path)
            rightmost = get_rightmost_pixel(img)
            # MC advance = rightmost_x + 1 (bo 0-indexed) + 1 (spacing) = rightmost + 2
            # Ale w praktyce MC używa: szerokość widocznej części + 1
            advance = rightmost + 2 if rightmost >= 0 else 1
            advances.append(advance)
            print(f"{level:5} | {fname:16} | {rightmost:11} | {advance}")
        else:
            print(f"{level:5} | {fname:16} | NOT FOUND")
            advances.append(1)
    
    print("\n// Wklej do FurnaceGUITitleFormatter.java:")
    print("private static final int[] ARROW_ADVANCES = {")
    print("    " + ", ".join(map(str, advances)))
    print("};")

if __name__ == "__main__":
    main()
