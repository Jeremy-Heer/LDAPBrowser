#!/usr/bin/env python3
"""
Script to check for unused imports in Java files
"""
import os
import re
import sys

def get_java_files(directory):
    """Get all Java files in the directory recursively"""
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files

def analyze_java_file(file_path):
    """Analyze a Java file for unused imports"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return []

    lines = content.split('\n')
    imports = []
    unused_imports = []
    
    # Extract import statements
    for i, line in enumerate(lines):
        line = line.strip()
        if line.startswith('import ') and not line.startswith('import static'):
            # Extract the import
            import_match = re.match(r'import\s+([^;]+);', line)
            if import_match:
                full_import = import_match.group(1)
                # Get the simple class name (last part after the dot)
                simple_name = full_import.split('.')[-1]
                imports.append({
                    'line_number': i + 1,
                    'full_import': full_import,
                    'simple_name': simple_name,
                    'import_line': line
                })
    
    # Check if each import is used
    for imp in imports:
        simple_name = imp['simple_name']
        
        # Create a regex pattern to find usage of the class
        # Look for the class name followed by word boundary (not part of another word)
        pattern = r'\b' + re.escape(simple_name) + r'\b'
        
        # Count occurrences, excluding the import line itself
        content_without_imports = '\n'.join([line for i, line in enumerate(lines) 
                                           if not (line.strip().startswith('import ') and 
                                                 simple_name in line)])
        
        matches = re.findall(pattern, content_without_imports)
        
        if len(matches) == 0:
            unused_imports.append(imp)
    
    return unused_imports

def main():
    if len(sys.argv) > 1:
        directory = sys.argv[1]
    else:
        directory = 'src/main/java'
    
    if not os.path.exists(directory):
        print(f"Directory {directory} does not exist")
        sys.exit(1)
    
    java_files = get_java_files(directory)
    total_unused = 0
    files_with_unused = 0
    
    for file_path in java_files:
        unused_imports = analyze_java_file(file_path)
        if unused_imports:
            files_with_unused += 1
            print(f"\n{file_path}:")
            for imp in unused_imports:
                print(f"  Line {imp['line_number']}: {imp['import_line']}")
                total_unused += 1
    
    print(f"\nSummary:")
    print(f"  Total Java files: {len(java_files)}")
    print(f"  Files with unused imports: {files_with_unused}")
    print(f"  Total unused imports: {total_unused}")
    
    if total_unused > 0:
        print(f"\nFound {total_unused} unused imports in {files_with_unused} files")
        return 1
    else:
        print("No unused imports found!")
        return 0

if __name__ == "__main__":
    sys.exit(main())
