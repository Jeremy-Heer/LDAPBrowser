# LDAP Search Return Attributes Feature

## Overview
The LDAP Browser now includes the ability to specify which attributes to return in search results, providing more focused and efficient searches.

## New Features

### 1. Return Attributes Field
- **Location**: Search pane, below the LDAP Filter field
- **Purpose**: Specify comma-separated list of attributes to return
- **Example**: `cn,mail,telephoneNumber,department`
- **Default**: Empty (returns all attributes)

### 2. Enhanced Search Results Display
- **DN Column**: Always displayed as the first column
- **Requested Attributes**: Displayed as subsequent columns when specified
- **Default Attributes**: When no specific attributes requested, shows DN, Name, and Object Class
- **Multi-value Support**: Multiple attribute values are joined with semicolons
- **Resizable Columns**: All columns can be resized for better viewing

### 3. Validation
- **Attribute Name Validation**: Ensures attribute names contain only valid characters
- **Error Handling**: Clear error messages for invalid attribute names

## Usage Examples

### Basic Search (All Attributes)
1. Enter search base DN: `ou=users,dc=example,dc=com`
2. Enter filter: `(objectClass=person)`
3. Leave "Return Attributes" field empty
4. Click "Search"

**Result**: Shows DN, Name, and Object Class columns with all attributes available for detailed view.

### Focused Search (Specific Attributes)
1. Enter search base DN: `ou=users,dc=example,dc=com`
2. Enter filter: `(objectClass=person)`
3. Enter return attributes: `cn,mail,telephoneNumber`
4. Click "Search"

**Result**: Shows DN column followed by cn, mail, and telephoneNumber columns.

### Advanced Attributes
- Use `+` to include operational attributes: `cn,mail,+`
- Use `*` to include all user attributes: `*,createTimestamp`

## Technical Implementation

### Backend Changes
- **LdapService**: Added `searchEntries` overload accepting specific attributes
- **SearchPanel**: Added `SearchResult` class to encapsulate results and requested attributes
- **Validation**: Added basic attribute name validation using regex

### Frontend Changes
- **Dynamic Columns**: Search results grid dynamically creates columns based on requested attributes
- **User Feedback**: Added informational text showing result count and requested attributes
- **Column Resizing**: All columns are now resizable for better usability

## Benefits
1. **Performance**: Reduced network traffic by only returning needed attributes
2. **Clarity**: Focused view of specific data without overwhelming detail
3. **Flexibility**: Support for both focused and comprehensive searches
4. **Usability**: Clear indication of what attributes were requested vs. returned

## Error Handling
- Invalid attribute names are rejected with clear error messages
- LDAP errors are caught and displayed to the user
- Empty results show appropriate "no results found" message

## Compatibility
- Backward compatible: existing searches without specified attributes work as before
- Forward compatible: supports standard and operational LDAP attributes
- Cross-platform: works with all LDAP server types (Active Directory, OpenLDAP, etc.)
