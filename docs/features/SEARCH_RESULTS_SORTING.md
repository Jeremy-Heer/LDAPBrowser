# Search Results Sorting Enhancement

## Overview
Added sorting capabilities to all grid columns throughout the LDAP Browser application to improve usability and data navigation.

## Changes Made

### 1. Search Results Grid (DashboardTab.java)
- **Distinguished Name column**: Added sorting with case-insensitive string comparison
- **Dynamic attribute columns**: Added sorting for each requested attribute column with case-insensitive comparison 
- **Name column**: Added sorting for entry display names with null handling
- **Object Class column**: Added sorting for object class values with null handling

### 2. Export Tab CSV Preview Grid (ExportTab.java)
- **All CSV columns**: Added sorting with case-insensitive string comparison and null handling

### 3. Schema Browser Grids (SchemaBrowser.java)
- **Object Class Grid**: Added sorting for Name, Description, Type, and Obsolete columns
- **Attribute Type Grid**: Added sorting for Name, Description, Syntax OID, and Obsolete columns
- **Matching Rule Grid**: Added sorting for Name, Description, Syntax OID, and Obsolete columns
- **Matching Rule Use Grid**: Added sorting for OID, Description, and Obsolete columns
- **Syntax Grid**: Added sorting for OID and Description columns

### 4. Connections Tab Grid (ConnectionsTab.java)
- **Name column**: Added sorting for server configuration names
- **Host:Port column**: Added sorting for host and port combinations
- **Bind DN column**: Added sorting for bind distinguished names

### 5. Attribute Editor Grid (AttributeEditor.java)
- **Attribute column**: Added sorting for attribute names

## Technical Implementation

### Sorting Features
- All string comparisons use `compareToIgnoreCase()` for case-insensitive sorting
- Proper null handling to prevent NullPointerException
- Consistent sorting behavior across all grids
- Boolean columns use `Boolean.compare()` for proper true/false ordering

### User Experience
- Users can now click on any column header to sort the data
- Visual indicators show the current sort direction (ascending/descending)
- Multi-column sorting is supported where applicable
- Improved data navigation for large result sets

## Benefits
1. **Enhanced Usability**: Users can quickly find specific entries by sorting on relevant columns
2. **Better Data Management**: Large search results and schema information are easier to navigate
3. **Consistent Interface**: All grids throughout the application now have consistent sorting behavior
4. **Performance**: Client-side sorting provides immediate response for better user experience

## Testing
- All grids maintain their existing functionality
- Sorting works correctly with various data types (strings, booleans, numeric values)
- Null values are handled gracefully
- Application compiles and runs without errors
