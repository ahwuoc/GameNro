#!/usr/bin/env python3
"""
Script để refactor tất cả service files từ &mut AsyncSession sang &SessionArc
"""

import os
import re
from pathlib import Path

# Root directory
ROOT = "/home/ahwuocdz/GameNro/src"

# Files to skip (already converted)
SKIP_FILES = [
    "network/controller.rs",
    "network/session.rs", 
    "network/mod.rs",
    "network/split_session.rs",
    "player/player.rs",
]

# Patterns to replace
REPLACEMENTS = [
    # Change function signatures
    (r'session:\s*&mut\s+AsyncSession', r'session: &SessionArc'),
    (r'session:\s*&\'life\d+\s+mut\s+AsyncSession', r'session: &SessionArc'),
    
    # Change direct field access to async method calls
    (r'session\.zoom_level', r'session.get_zoom_level().await'),
    
    # Change get_player() - old returns Option<&Player>, new returns Option<Player>
    (r'session\.get_player\(\)\.cloned\(\)', r'session.get_player().await'),
    (r'session\.get_player_mut\(\)', r'session.get_player().await'),
    (r'session\.get_player\(\)', r'session.get_player().await'),
    
    # Change set_player
    (r'session\.set_player\(([^)]+)\);', r'session.set_player(\1).await;'),
    
    # Change take_player  
    (r'session\.take_player\(\)', r'session.take_player().await'),
    
    # Change set_sent_key
    (r'session\.set_sent_key\(([^)]+)\);', r'session.set_sent_key(\1).await;'),
    
    # Change set_credentials
    (r'session\.set_credentials\(([^)]+),\s*([^)]+)\);', r'session.set_credentials(\1, \2).await;'),
    
    # Change set_user_id
    (r'session\.set_user_id\(([^)]+)\);', r'session.set_user_id(\1).await;'),
    
    # Change get_user_id
    (r'session\.get_user_id\(\)', r'session.get_user_id().await'),
    
    # Change get_username/get_password
    (r'session\.get_username\(\)', r'session.get_username().await'),
    (r'session\.get_password\(\)', r'session.get_password().await'),
    
    # Change set_version
    (r'session\.set_version\(([^)]+)\);', r'session.set_version(\1).await;'),
    
    # Change is_admin  
    (r'session\.is_admin', r'session.is_admin().await'),
]

def add_import_if_needed(content: str) -> str:
    if "SessionArc" in content:
        return content
    
    pattern = r'use crate::network::session::AsyncSession;'
    replacement = r'use crate::network::session::{AsyncSession, SessionArc};'
    
    if pattern in content:
        return content.replace(pattern, replacement)
    
    return content


def process_file(filepath: str) -> bool:
    rel_path = os.path.relpath(filepath, ROOT)
    if rel_path in SKIP_FILES:
        return False
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            original = f.read()
    except:
        return False
    
    if "AsyncSession" not in original:
        return False
    
    content = original
    
    for pattern, replacement in REPLACEMENTS:
        content = re.sub(pattern, replacement, content)
    
    content = add_import_if_needed(content)
    
    if content == original:
        return False
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    
    return True


def main():
    print("Starting refactor...")
    changed_files = []
    
    for root, dirs, files in os.walk(ROOT):
        if 'target' in root:
            continue
            
        for file in files:
            if file.endswith('.rs'):
                filepath = os.path.join(root, file)
                if process_file(filepath):
                    rel_path = os.path.relpath(filepath, ROOT)
                    changed_files.append(rel_path)
                    print(f"  Updated: {rel_path}")
    
    print(f"\nRefactored {len(changed_files)} files")
    print("\nDone! Run 'cargo build' to check for remaining errors.")


if __name__ == "__main__":
    main()
