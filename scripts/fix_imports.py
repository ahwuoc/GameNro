#!/usr/bin/env python3
"""
Script to fix missing SessionArc imports in Rust files.
"""

import os
import re

ROOT = "/home/ahwuocdz/GameNro/src"

def fix_imports_in_file(filepath: str) -> bool:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False

    # Check if SessionArc is used
    if "SessionArc" not in content:
        return False
        
    # Check if already imported or defined
    if "use crate::network::session::SessionArc;" in content or \
       "use crate::network::session::{AsyncSession, SessionArc};" in content or \
       "type SessionArc =" in content or \
       "pub type SessionArc =" in content:
        return False
        
    original_content = content
    
    # Try to add to existing AsyncSession import
    if "use crate::network::session::AsyncSession;" in content:
        content = content.replace(
            "use crate::network::session::AsyncSession;", 
            "use crate::network::session::{AsyncSession, SessionArc};"
        )
    elif "use crate::network::session::{AsyncSession};" in content:
        content = content.replace(
            "use crate::network::session::{AsyncSession};", 
            "use crate::network::session::{AsyncSession, SessionArc};"
        )
    # Check for other valid replacement points
    elif "use crate::network::session::" in content:
         # simple regex to find explicit imports from session module
         # e.g. use crate::network::session::{foo, bar};
         def repl(match):
             existing = match.group(1)
             if "SessionArc" not in existing:
                 return f"use crate::network::session::{{{existing}, SessionArc}};"
             return match.group(0)
             
         content = re.sub(r"use crate::network::session::\{([^}]+)\};", repl, content)
         
         # Fallback: if we didn't change anything yet, verify if we need to add standalone import
         if content == original_content:
             # Just add it after the last use crate::... specific line or at top of imports
             if "use crate::network::session::" in content:
                 # It might be `use crate::network::session;`
                 if "use crate::network::session;" in content:
                      content = content.replace("use crate::network::session;", "use crate::network::session;\nuse crate::network::session::SessionArc;")
    else:
        # No session session import found, try to insert with other network imports
        if "use crate::network::" in content:
             content = re.sub(r"(use crate::network::[^;]+;)", r"\1\nuse crate::network::session::SessionArc;", content, count=1)
        elif "use crate::" in content:
             content = re.sub(r"(use crate::[^;]+;)", r"\1\nuse crate::network::session::SessionArc;", content, count=1)
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    
    return False

def main():
    print("Scanning for missing SessionArc imports...")
    count = 0
    for root, dirs, files in os.walk(ROOT):
        if 'target' in root:
            continue
        for file in files:
            if file.endswith('.rs'):
                filepath = os.path.join(root, file)
                # Skip the session definition file itself
                if file == "session.rs" and "network" in root:
                    continue
                
                if fix_imports_in_file(filepath):
                    print(f"Fixed imports in: {os.path.relpath(filepath, ROOT)}")
                    count += 1
    
    print(f"Fixed {count} files.")

if __name__ == "__main__":
    main()
