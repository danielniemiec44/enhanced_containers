#!/usr/bin/env python3
"""
Aktualizuje font providers dla ognia - zmienia height z 16 na 14.
"""

import json
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
FONT_FILE = os.path.join(PROJECT_ROOT, "src/main/resources/assets/minecraft/font/default.json")

def update_fire_height():
    with open(FONT_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    updated = 0
    for provider in data['providers']:
        if provider.get('type') == 'bitmap':
            file_path = provider.get('file', '')
            if 'font/fire_' in file_path:
                if provider.get('height') != 14:
                    old_height = provider.get('height')
                    provider['height'] = 14
                    print(f"Zmieniono height: {file_path} ({old_height} -> 14)")
                    updated += 1
    
    with open(FONT_FILE, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    
    print(f"\nZaktualizowano {updated} providerów")

if __name__ == "__main__":
    update_fire_height()
