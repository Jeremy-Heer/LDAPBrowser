#!/usr/bin/env python3
"""
Advanced script to fix Java indentation to 2-space standard
Properly handles class structure and nested blocks
"""
import os
import re
import sys

def fix_java_indentation(file_path):
    """Fix indentation in a Java file to use 2-space standard"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        fixed_lines = []
        current_indent_level = 0
        
        for line in lines:
            stripped = line.strip()
            
            # Skip empty lines
            if not stripped:
                fixed_lines.append('')
                continue
            
            # Count braces to determine indent level changes
            opens = stripped.count('{')
            closes = stripped.count('}')
            
            # Adjust indentation level for closing braces at start of line
            if stripped.startswith('}'):
                current_indent_level -= closes
                current_indent_level = max(0, current_indent_level)
            
            # Apply current indentation
            indent = '  ' * current_indent_level  # 2 spaces per level
            fixed_line = indent + stripped
            fixed_lines.append(fixed_line)
            
            # Adjust indentation level for opening braces
            if not stripped.startswith('}'):
                current_indent_level += opens
                current_indent_level = max(0, current_indent_level)
            
            # Adjust for closing braces not at start of line  
            if not stripped.startswith('}') and closes > 0:
                current_indent_level -= closes
                current_indent_level = max(0, current_indent_level)
        
        # Write back to file
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(fixed_lines))
        
        print(f"Fixed indentation in {file_path}")
        return True
        
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def find_java_files(directory):
    """Find all Java files in directory and subdirectories"""
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files

def main():
    # Find all Java files in src directory
    src_dir = '/home/jheer/Documents/git/vaadin/FirstMock/src'
    java_files = find_java_files(src_dir)
    
    print(f"Found {len(java_files)} Java files")
    
    success_count = 0
    for file_path in java_files:
        if fix_java_indentation(file_path):
            success_count += 1
    
    print(f"Successfully fixed {success_count} out of {len(java_files)} files")

if __name__ == '__main__':
    main()
