#!/usr/bin/env python3
"""
Targeted script to fix specific indentation issues in the remaining problematic files
"""
import os
import re

def fix_mainview_java():
    """Fix specific indentation issues in MainView.java"""
    file_path = '/home/jheer/Documents/git/vaadin/FirstMock/src/main/java/com/example/ldapbrowser/ui/MainView.java'
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Fix the constructor parameter indentation
    content = content.replace(
        '  InMemoryLdapService inMemoryLdapService, LoggingService loggingService) {',
        '      InMemoryLdapService inMemoryLdapService, LoggingService loggingService) {'
    )
    
    # Fix the if-else chain indentation
    lines = content.split('\n')
    fixed_lines = []
    in_nested_if_chain = False
    
    for i, line in enumerate(lines):
        stripped = line.strip()
        
        # Detect start of the problematic if-else chain
        if 'showDirectorySearch();' in line:
            in_nested_if_chain = True
        elif in_nested_if_chain and stripped.startswith('}') and not stripped.startswith('} else'):
            in_nested_if_chain = False
        
        if in_nested_if_chain:
            if stripped.startswith('} else if'):
                fixed_lines.append('      } else if' + stripped[9:])
            elif stripped.startswith('}'):
                fixed_lines.append('    }')
            elif stripped.startswith('show'):
                fixed_lines.append('        ' + stripped)
            else:
                fixed_lines.append(line)
        else:
            fixed_lines.append(line)
    
    content = '\n'.join(fixed_lines)
    
    # Write back
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Fixed MainView.java")

def fix_directory_search_subtab():
    """Fix specific indentation issues in DirectorySearchSubTab.java"""
    file_path = '/home/jheer/Documents/git/vaadin/FirstMock/src/main/java/com/example/ldapbrowser/ui/components/DirectorySearchSubTab.java'
    
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    fixed_lines = []
    for line in lines:
        stripped = line.strip()
        
        # Skip empty lines
        if not stripped:
            fixed_lines.append('')
            continue
        
        # For method signatures that are split across lines, ensure proper continuation indentation
        if (stripped.startswith('private') or stripped.startswith('public') or stripped.startswith('protected')) and not stripped.endswith('{') and not stripped.endswith(';'):
            # Method declaration - use 2 spaces
            fixed_lines.append('  ' + stripped)
        elif line.startswith('    ') and not stripped.startswith('//') and stripped:
            # Statement inside method - use 4 spaces
            fixed_lines.append('    ' + stripped)
        elif line.startswith('  ') and (stripped.startswith('private') or stripped.startswith('public') or stripped.startswith('protected')):
            # Class member or method - use 2 spaces
            fixed_lines.append('  ' + stripped)
        else:
            # Determine proper indentation based on context
            if stripped.startswith('}'):
                # Closing brace - determine level based on content
                if 'class' in stripped or stripped == '}':
                    fixed_lines.append('}')  # Class level
                else:
                    fixed_lines.append('  }')  # Method level
            elif stripped.startswith('public class') or stripped.startswith('private class'):
                fixed_lines.append(stripped)  # Top level
            elif any(stripped.startswith(keyword) for keyword in ['private', 'public', 'protected']):
                fixed_lines.append('  ' + stripped)  # Class member
            else:
                # Default to 4-space indentation for method content
                if stripped and not line.startswith('package') and not line.startswith('import'):
                    fixed_lines.append('    ' + stripped)
                else:
                    fixed_lines.append(stripped)
    
    # Write back
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(fixed_lines))
    
    print(f"Fixed DirectorySearchSubTab.java")

def main():
    fix_mainview_java()
    fix_directory_search_subtab()
    print("Completed targeted fixes for problematic files")

if __name__ == '__main__':
    main()
