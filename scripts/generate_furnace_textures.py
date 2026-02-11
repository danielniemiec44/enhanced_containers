#!/usr/bin/env python3
"""
Generator tekstur postępu pieca w stylu vanilla Minecraft.

Tworzy tekstury 16x16 dla:
- Fire (ogień): 14 tekstur (empty + 13 poziomów wypełnienia od dołu)
- Arrow (strzałka): 23 tekstury (empty + 22 poziomy wypełnienia od lewej)

Kolory i kształty bazują na vanilla Minecraft furnace GUI.
"""

from PIL import Image, ImageDraw
import os
import json

# Katalog docelowy
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUTPUT_DIR_GUI = os.path.join(PROJECT_ROOT, "src/main/resources/assets/enhanced_containers/textures/gui/furnace")
OUTPUT_DIR_ITEM = os.path.join(PROJECT_ROOT, "src/main/resources/assets/enhanced_containers/textures/item/furnace")
OUTPUT_DIRS = [OUTPUT_DIR_GUI, OUTPUT_DIR_ITEM]

MODELS_DIR = os.path.join(PROJECT_ROOT, "src/main/resources/assets/enhanced_containers/models/item")

# Kolory vanilla Minecraft furnace
# Fire - pomarańczowo-żółty płomień
FIRE_OUTLINE = (180, 100, 30, 255)    # Ciemny pomarańczowy
FIRE_FILL = (255, 150, 50, 255)       # Jasny pomarańczowy
FIRE_HIGHLIGHT = (255, 220, 100, 255) # Żółty highlight
FIRE_EMPTY = (80, 80, 80, 255)        # Szary dla pustego

# Arrow - biała strzałka na szarym tle
ARROW_OUTLINE = (139, 139, 139, 255)  # Szary kontur
ARROW_FILL = (255, 255, 255, 255)     # Biały wypełniony
ARROW_EMPTY = (100, 100, 100, 255)    # Ciemniejszy szary


def create_fire_sprite(level, max_level=13):
    """
    Tworzy sprite ognia w stylu vanilla.
    
    W vanilla furnace ogień to 14x14 pikseli, wypełnia się od dołu do góry.
    Tutaj robimy 16x16 dla item texture.
    
    level: 0 = pusty kontur, 1-13 = wypełnienie od dołu
    """
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Wymiary płomienia (wycentrowany w 16x16)
    fire_left = 3
    fire_right = 12
    fire_top = 2
    fire_bottom = 14
    fire_height = fire_bottom - fire_top
    
    # Ile pikseli wypełnić od dołu
    if level <= 0:
        fill_height = 0
    else:
        fill_height = int((fire_height * level) / max_level)
    
    fill_start_y = fire_bottom - fill_height
    
    # Kształt płomienia - szerszy na dole, węższy na górze
    for y in range(fire_top, fire_bottom + 1):
        # Proporcja od góry (0.0 na górze, 1.0 na dole)
        progress = (y - fire_top) / fire_height
        
        # Szerokość na tej wysokości
        base_width = fire_right - fire_left
        width = int(2 + progress * (base_width - 2))
        
        center_x = (fire_left + fire_right) // 2
        x_start = center_x - width // 2
        x_end = center_x + width // 2
        
        if y >= fill_start_y and level > 0:
            # Wypełniony ogień
            for x in range(x_start, x_end + 1):
                # Gradient - jaśniejszy w środku
                dist_from_center = abs(x - center_x)
                if dist_from_center == 0:
                    draw.point((x, y), fill=FIRE_HIGHLIGHT)
                elif dist_from_center <= width // 4:
                    draw.point((x, y), fill=FIRE_FILL)
                else:
                    draw.point((x, y), fill=FIRE_OUTLINE)
        else:
            # Pusty kontur
            draw.point((x_start, y), fill=FIRE_EMPTY)
            draw.point((x_end, y), fill=FIRE_EMPTY)
    
    # Czubek płomienia (zawsze rysowany gdy wypełniony)
    if level >= max_level - 1:
        center_x = (fire_left + fire_right) // 2
        draw.point((center_x, fire_top - 1), fill=FIRE_HIGHLIGHT)
    
    return img


def create_arrow_sprite(level, max_level=22):
    """
    Tworzy sprite strzałki postępu w stylu vanilla.
    
    W vanilla furnace strzałka to ~24x17 pikseli, wypełnia się od lewej do prawej.
    Tutaj robimy 16x16 dla item texture.
    
    level: 0 = pusty kontur, 1-22 = wypełnienie od lewej
    """
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Wymiary strzałki
    arrow_left = 1
    arrow_body_right = 10  # Koniec ciała strzałki
    arrow_tip_right = 14   # Koniec grota
    arrow_y_center = 7
    arrow_half_height = 1  # Ciało ma wysokość 3 pikseli
    
    # Ile pikseli wypełnić od lewej (wliczając czubek przy level==max_level)
    total_pixels = arrow_tip_right - arrow_left + 1
    if level <= 0:
        fill_width = 0
    else:
        fill_width = int(round((total_pixels * level) / max_level))

    fill_end_x_inclusive = arrow_left + fill_width - 1
    
    # Ciało strzałki (prostokąt)
    for x in range(arrow_left, arrow_body_right + 1):
        for dy in range(-arrow_half_height, arrow_half_height + 1):
            y = arrow_y_center + dy
            if level > 0 and x <= fill_end_x_inclusive:
                pixel = ARROW_FILL
            else:
                pixel = ARROW_EMPTY

            # Najpierw wypełnienie/"pusta" część, potem kontur na krawędziach
            draw.point((x, y), fill=pixel)
            if abs(dy) == arrow_half_height:
                draw.point((x, y), fill=ARROW_OUTLINE)
    
    # Grot strzałki (trójkąt skierowany w prawo)
    for i in range(4):
        x = arrow_body_right + 1 + i
        if x > 15:
            break
            
        # Grot rozszerza się
        for dy in range(-i - arrow_half_height, i + arrow_half_height + 1):
            y = arrow_y_center + dy
            if 0 <= y <= 15:
                if level > 0 and x <= fill_end_x_inclusive:
                    pixel = ARROW_FILL
                else:
                    pixel = ARROW_EMPTY

                draw.point((x, y), fill=pixel)

                # Kontur na krawędziach grota
                if abs(dy) == i + arrow_half_height:
                    draw.point((x, y), fill=ARROW_OUTLINE)
    
    # Końcówka grota
    tip_x = arrow_body_right + 4
    if tip_x <= 15:
        if level > 0 and tip_x <= fill_end_x_inclusive:
            pixel = ARROW_FILL
        else:
            pixel = ARROW_EMPTY

        draw.point((tip_x, arrow_y_center), fill=pixel)
        # Kontur czubka
        if level <= 0 or tip_x > fill_end_x_inclusive:
            draw.point((tip_x, arrow_y_center), fill=ARROW_OUTLINE)
    
    return img


def create_wide_arrow_halves(level, max_level=22):
    """Tworzy strzałkę jak w vanilla, ale rozbitą na 2 sloty (2×16px).
    
    Rysuje normalną strzałkę 24px szerokości i crop uje na pół.
    Strzałka jest wycentrowana tak, żeby grot był w PRAWEJ połówce.
    """
    img = Image.new('RGBA', (32, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Strzałka wycentrowana - grot musi być w prawej połówce (x > 16)
    # Ciało: x=6-20, Grot: x=21-25
    arrow_left = 6
    arrow_body_right = 20
    arrow_tip_right = 25
    arrow_y_center = 7
    arrow_half_height = 1

    total_pixels = arrow_tip_right - arrow_left + 1
    if level <= 0:
        fill_width = 0
    else:
        fill_width = int(round((total_pixels * level) / max_level))
    fill_end_x_inclusive = arrow_left + fill_width - 1

    # Ciało
    for x in range(arrow_left, arrow_body_right + 1):
        for dy in range(-arrow_half_height, arrow_half_height + 1):
            y = arrow_y_center + dy
            if level > 0 and x <= fill_end_x_inclusive:
                pixel = ARROW_FILL
            else:
                pixel = ARROW_EMPTY

            draw.point((x, y), fill=pixel)
            if abs(dy) == arrow_half_height:
                draw.point((x, y), fill=ARROW_OUTLINE)

    # Grot
    for i in range(4):
        x = arrow_body_right + 1 + i
        if x > 31:
            break

        for dy in range(-i - arrow_half_height, i + arrow_half_height + 1):
            y = arrow_y_center + dy
            if 0 <= y <= 15:
                if level > 0 and x <= fill_end_x_inclusive:
                    pixel = ARROW_FILL
                else:
                    pixel = ARROW_EMPTY

                draw.point((x, y), fill=pixel)
                if abs(dy) == i + arrow_half_height:
                    draw.point((x, y), fill=ARROW_OUTLINE)

    # Czubek
    tip_x = arrow_body_right + 4
    if tip_x <= 31:
        if level > 0 and tip_x <= fill_end_x_inclusive:
            pixel = ARROW_FILL
        else:
            pixel = ARROW_EMPTY
        draw.point((tip_x, arrow_y_center), fill=pixel)
        if level <= 0 or tip_x > fill_end_x_inclusive:
            draw.point((tip_x, arrow_y_center), fill=ARROW_OUTLINE)

    # Normalne crop 16x16
    left = img.crop((0, 0, 16, 16))
    right = img.crop((16, 0, 32, 16))
    return left, right


def write_item_model_json(model_name: str, texture_path: str):
    os.makedirs(MODELS_DIR, exist_ok=True)
    data = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": texture_path
        }
    }
    
    # AGGRESSIVE GUI GAP FIX: Force overlap with extreme translation
    # translation: 8 = half slot width, should push item into neighbor's space
    # scale: 1.05 = 5% bleed to cover any rounding gaps
    if "arrow_left" in model_name:
        data["display"] = {
            "gui": {
                "rotation": [0, 0, 0],
                "translation": [8, 0, 0],
                "scale": [1.05, 1.05, 1.0]
            }
        }
    elif "arrow_right" in model_name:
        data["display"] = {
            "gui": {
                "rotation": [0, 0, 0],
                "translation": [-8, 0, 0],
                "scale": [1.05, 1.05, 1.0]
            }
        }
        
    path = os.path.join(MODELS_DIR, f"{model_name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main():
    for out_dir in OUTPUT_DIRS:
        os.makedirs(out_dir, exist_ok=True)

    print("Generuję tekstury w:")
    for out_dir in OUTPUT_DIRS:
        print(f"- {out_dir}")
    print()
    
    # Generuj tekstury ognia (0-13)
    print("Tekstury ognia (fire):")
    for level in range(14):
        img = create_fire_sprite(level)
        if level == 0:
            filename = "fire_empty.png"
        else:
            filename = f"fire_{level}.png"
        for out_dir in OUTPUT_DIRS:
            filepath = os.path.join(out_dir, filename)
            img.save(filepath)
        print(f"  ✓ {filename}")
    
    print()
    
    # Generuj tekstury strzałki (0-22) - 1-slot (kompatybilność)
    print("Tekstury strzałki (arrow) 1-slot:")
    for level in range(23):
        img = create_arrow_sprite(level)
        if level == 0:
            filename = "arrow_empty.png"
        else:
            filename = f"arrow_{level}.png"
        for out_dir in OUTPUT_DIRS:
            filepath = os.path.join(out_dir, filename)
            img.save(filepath)
        print(f"  ✓ {filename}")

    print()

    # Generuj tekstury strzałki 2-slot (lewa+ prawa połówka) + modele
    print("Tekstury strzałki (arrow) 2-slot halves:")
    for level in range(23):
        left, right = create_wide_arrow_halves(level)
        if level == 0:
            left_name = "arrow_left_empty.png"
            right_name = "arrow_right_empty.png"
            left_model = "arrow_left_empty"
            right_model = "arrow_right_empty"
            left_tex = "enhanced_containers:item/furnace/arrow_left_empty"
            right_tex = "enhanced_containers:item/furnace/arrow_right_empty"
        else:
            left_name = f"arrow_left_{level}.png"
            right_name = f"arrow_right_{level}.png"
            left_model = f"arrow_left_{level}"
            right_model = f"arrow_right_{level}"
            left_tex = f"enhanced_containers:item/furnace/arrow_left_{level}"
            right_tex = f"enhanced_containers:item/furnace/arrow_right_{level}"

        for out_dir in OUTPUT_DIRS:
            left.save(os.path.join(out_dir, left_name))
            right.save(os.path.join(out_dir, right_name))

        write_item_model_json(left_model, left_tex)
        write_item_model_json(right_model, right_tex)

        print(f"  ✓ {left_name} / {right_name}")
    
    print()
    print(f"✅ Wygenerowano {14 + 23 + 46} tekstur!")


if __name__ == "__main__":
    main()
