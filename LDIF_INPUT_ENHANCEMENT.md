# LDIF Input Enhancement

## Overview
Enhanced the Bulk Operations / Import tab to provide users with three options in the existing "Import Type" dropdown:
1. **Upload LDIF** - Upload an LDIF file from the filesystem (existing functionality)
2. **Enter LDIF** - Type or paste LDIF content directly into a text area (new functionality)  
3. **Input CSV** - Import CSV data (existing functionality)

## Changes Made

### 1. ImportTab.java Modifications

#### Updated Components:
- `ComboBox<String> importModeSelector` - Now includes three options: "Upload LDIF", "Enter LDIF", "Input CSV"
- `TextArea ldifTextArea` - Large text area for entering LDIF content directly
- `VerticalLayout ldifInputContainer` - Container that switches between upload and text entry components

#### New Methods Added:
- `updateLdifImportButtonState()` - Updates the import button state based on available content from either source

#### Modified Methods:
- `initializeComponents()` - Updated Import Type selector to include "Enter LDIF" option
- `initializeLdifModeComponents()` - Simplified to support both upload and text entry without extra selectors
- `switchMode()` - Enhanced to handle three modes: "Upload LDIF", "Enter LDIF", and "Input CSV"
- `processLdifFile()` - Updated to call `updateLdifImportButtonState()`
- `clear()` - Added clearing of the text area

#### Removed Components:
- Extra "LDIF Input Method" selector (simplified to use main Import Type dropdown)

### 2. User Interface Changes

#### Import Type Selector:
- **Upload LDIF**: Shows file upload component (existing functionality)
- **Enter LDIF**: Shows large text area for direct LDIF input (new functionality)  
- **Input CSV**: Shows CSV import interface (existing functionality)

#### Text Area Features:
- Large 300px height text area for LDIF content
- Placeholder text with example LDIF format
- Real-time validation that enables/disables the Import button
- Shown only when "Enter LDIF" is selected

#### Dynamic Content Switching:
- When switching to "Upload LDIF": Shows file upload component, hides text area, clears text content
- When switching to "Enter LDIF": Shows text area, hides upload component, clears uploaded content
- When switching to "Input CSV": Shows CSV import interface

### 3. Functionality

#### LDIF Processing:
- Both upload and text entry methods use the same LDIF processing logic
- Content from either source is stored in `rawLdifContent` variable
- Import button is enabled only when valid content is available from the active source

#### State Management:
- Switching between input methods clears the content from the inactive method
- Import button state updates dynamically based on content availability
- Clear functionality resets both upload and text entry states

## Usage

1. Navigate to the Bulk Operations tab
2. Select import type from the "Import Type" dropdown:
   - **Upload LDIF**: Use file upload (existing workflow)
   - **Enter LDIF**: Type or paste LDIF content in the text area (new workflow)
   - **Input CSV**: Import CSV data (existing workflow)
3. Configure import options (continue on error, controls, etc.)
4. Click "Import LDIF" to process the content

## Example LDIF Content for Testing

```ldif
dn: cn=John Doe,ou=People,dc=example,dc=com
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: John Doe
sn: Doe
givenName: John
mail: john.doe@example.com

dn: cn=Jane Smith,ou=People,dc=example,dc=com
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: Jane Smith
sn: Smith
givenName: Jane
mail: jane.smith@example.com
```

## Benefits

1. **Improved User Experience**: Users can now quickly enter small LDIF snippets without creating temporary files
2. **Simplified Interface**: Uses existing Import Type dropdown without adding extra selectors
3. **Maintains Compatibility**: Existing file upload and CSV import functionality remains unchanged
4. **Real-time Validation**: Import button enables/disables based on content availability
5. **Clear State Management**: Switching between methods provides clean separation of content sources
6. **Consistent Design**: Follows the same pattern as existing CSV import functionality
