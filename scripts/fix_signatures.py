#!/usr/bin/env python3
import os

files = [
    "src/map/services/change_map_service.rs",
    "src/map/models/zone.rs",
    "src/map/managers/zone_manager.rs"
]

target = "&mut crate::network::session::AsyncSession"
replacement = "&crate::network::session::SessionArc"

for path in files:
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        new_content = content.replace(target, replacement)
        if new_content != content:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated {path}")
        else:
            print(f"No changes for {path}")
    else:
        print(f"File not found: {path}")
