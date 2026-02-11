#!/usr/bin/env python3
import json

json_path = '/Users/danielniemiec/Documents/Projekty2026/enhanced_containers/src/main/resources/assets/minecraft/font/default.json'

with open(json_path, 'r') as f:
    data = json.load(f)

# Usuń WSZYSTKIE fire providers z ascent != -78
new_providers = []
removed = 0
for p in data['providers']:
    file_name = p.get('file', '')
    ascent = p.get('ascent', 0)
    
    # Sprawdź czy to fire provider (plik zawiera "fire_")
    is_fire = 'font/fire_' in file_name
    
    if is_fire:
        # Zachowaj tylko z ascent=-78
        if ascent == -78:
            new_providers.append(p)
        else:
            removed += 1
            print(f"Usuwam: {file_name} ascent={ascent}")
    else:
        new_providers.append(p)

data['providers'] = new_providers

with open(json_path, 'w') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

fire_count = sum(1 for p in new_providers if 'font/fire_' in p.get('file', ''))
print(f"\nUsunięto: {removed}")
print(f"Pozostało fire providers: {fire_count}")
