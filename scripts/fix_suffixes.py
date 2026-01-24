#!/usr/bin/env python3
import os

ROOT = "/home/ahwuocdz/GameNro"
files_to_check = [
    "src/network/controller.rs",
    "src/network/mod.rs"
]

def clean_file(rel_path):
    path = os.path.join(ROOT, rel_path)
    if not os.path.exists(path):
        print(f"File not found: {path}")
        return

    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    # Remove _arc suffixes - simple string replace is safer than regex for this specific case
    # as we know we want to remove the suffix from function calls
    new_content = new_content.replace("_arc(", "(")
    new_content = new_content.replace("_arc (", " (")
    # Also handle ::<TurboFish>_arc( if any? Unlikely for methods.
    
    # Remove duplicate import
    lines = new_content.splitlines()
    final_lines = []
    import_removed = False
    for line in lines:
        if "use crate::network::session::SessionArc;" in line and not line.strip().startswith("//"):
            # Only remove if we suspect it's the duplicate generated one. 
            # In mod.rs and controller.rs we saw it was invalid/duplicate.
            print(f"Removing import line in {rel_path}")
            continue
        final_lines.append(line)
        
    new_content = "\n".join(final_lines)
    # Ensure trailing newline if original had it
    if content.endswith("\n") and not new_content.endswith("\n"):
        new_content += "\n"
    
    if new_content != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed {rel_path}")
    else:
        print(f"No changes for {rel_path}")

if __name__ == "__main__":
    for f in files_to_check:
        clean_file(f)
