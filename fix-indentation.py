#!/usr/bin/env python3
"""
Script to fix Java indentation from 4 spaces to 2 spaces
"""
import os
import re
import sys

def fix_indentation(file_path):
    """Fix indentation in a Java file from 4 spaces to 2 spaces"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Split into lines
        lines = content.split('\n')
        fixed_lines = []
        
        for line in lines:
            # Count leading spaces
            leading_spaces = len(line) - len(line.lstrip(' '))
            
            # If line has leading spaces, convert 4-space to 2-space indentation
            if leading_spaces > 0:
                # Calculate new indentation (divide by 2)
                new_leading_spaces = leading_spaces // 2
                fixed_line = ' ' * new_leading_spaces + line.lstrip(' ')
                fixed_lines.append(fixed_line)
            else:
                fixed_lines.append(line)
        
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
        if fix_indentation(file_path):
            success_count += 1
    
    print(f"Successfully fixed {success_count} out of {len(java_files)} files")

if __name__ == '__main__':
    main()
