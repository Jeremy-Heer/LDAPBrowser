# Schema Enhanced Selectors Feature

## Overview
This feature enhances the Schema tab's "Add Object Class" and "Add Attribute Type" dialogs by providing intelligent selectors that allow users to choose existing schema elements instead of manually typing them. This improves usability, reduces errors, and speeds up schema modification tasks.

## Enhanced Features

### 1. Add Object Class Dialog Enhancements

#### Superior Classes Selector
- **Multi-Select ComboBox**: Choose from existing object classes in the schema
- **Placeholder**: "Choose from existing object classes..."
- **Helper Text**: "Select from existing object classes or type new ones"
- **Alternative**: Manual text area for typing (one per line)
- **Behavior**: 
  - Loads all available object class names from the current schema
  - Allows custom values to be entered
  - Combines selections from both selector and manual entry
  - Sorted alphabetically for easy browsing

#### Required Attributes (MUST) Selector
- **Multi-Select ComboBox**: Choose from existing attribute types in the schema
- **Placeholder**: "Choose from existing attributes..."
- **Helper Text**: "Select from existing attribute types or type new ones"
- **Alternative**: Manual text area for typing (one per line)
- **Behavior**: 
  - Loads all available attribute type names from the current schema
  - Allows custom values to be entered
  - Combines selections from both selector and manual entry
  - Sorted alphabetically for easy browsing

#### Optional Attributes (MAY) Selector
- **Multi-Select ComboBox**: Choose from existing attribute types in the schema
- **Placeholder**: "Choose from existing attributes..."
- **Helper Text**: "Select from existing attribute types or type new ones"
- **Alternative**: Manual text area for typing (one per line)
- **Behavior**: 
  - Loads all available attribute type names from the current schema
  - Allows custom values to be entered
  - Combines selections from both selector and manual entry
  - Sorted alphabetically for easy browsing

### 2. Add Attribute Type Dialog Enhancements

#### Syntax OID Selector
- **ComboBox with Custom Values**: Choose from existing syntax OIDs in the schema
- **Placeholder**: "Choose from available syntaxes..."
- **Helper Text**: "Select from existing syntaxes or enter custom OID"
- **Alternative**: Manual text field for typing
- **Behavior**: 
  - Pre-filled with default DirectoryString syntax (1.3.6.1.4.1.1466.115.121.1.15)
  - Loads all available syntax OIDs from the current schema
  - Allows custom values to be entered
  - Uses the selector value if available, otherwise falls back to manual field

#### Superior Type Selector
- **ComboBox with Custom Values**: Choose from existing attribute types in the schema
- **Placeholder**: "Choose from existing attribute types..."
- **Helper Text**: "Select from existing attribute types or enter custom name"
- **Alternative**: Manual text field for typing
- **Behavior**: 
  - Loads all available attribute type names from the current schema
  - Allows custom values to be entered
  - Uses the selector value if available, otherwise falls back to manual field

#### Equality Matching Rule Selector
- **ComboBox with Custom Values**: Choose from existing matching rules in the schema
- **Placeholder**: "Choose from existing matching rules..."
- **Helper Text**: "Select from existing matching rules or enter custom name"
- **Alternative**: Manual text field for typing
- **Behavior**: 
  - Loads all available matching rule names from the current schema
  - Allows custom values to be entered
  - Uses the selector value if available, otherwise falls back to manual field

#### Ordering Matching Rule Selector
- **ComboBox with Custom Values**: Choose from existing matching rules in the schema
- **Placeholder**: "Choose from existing matching rules..."
- **Helper Text**: "Select from existing matching rules or enter custom name"
- **Alternative**: Manual text field for typing
- **Behavior**: 
  - Loads all available matching rule names from the current schema
  - Allows custom values to be entered
  - Uses the selector value if available, otherwise falls back to manual field

#### Substring Matching Rule Selector
- **ComboBox with Custom Values**: Choose from existing matching rules in the schema
- **Placeholder**: "Choose from existing matching rules..."
- **Helper Text**: "Select from existing matching rules or enter custom name"
- **Alternative**: Manual text field for typing
- **Behavior**: 
  - Loads all available matching rule names from the current schema
  - Allows custom values to be entered
  - Uses the selector value if available, otherwise falls back to manual field

## Technical Implementation

### Helper Methods Added

#### Schema Data Retrieval Methods
- `getAvailableObjectClassNames()`: Returns sorted list of all object class names
- `getAvailableAttributeTypeNames()`: Returns sorted list of all attribute type names
- `getAvailableMatchingRuleNames()`: Returns sorted list of all matching rule names
- `getAvailableSyntaxOIDs()`: Returns sorted list of all syntax OIDs

#### UI Component Creation Methods
- `createSchemaMultiSelect()`: Creates multi-select combobox for multiple selections
- `createSchemaComboBox()`: Creates single-select combobox with custom value support

### Dialog Enhancements

#### Object Class Dialog
- Increased dialog width to 700px and height to 800px to accommodate new components
- Added selectors alongside existing manual text areas
- Enhanced validation logic to combine values from both selectors and manual entries
- Updated method signature to handle new multi-select components

#### Attribute Type Dialog
- Increased dialog width to 700px and height to 750px to accommodate new components
- Added selectors alongside existing manual text fields
- Enhanced validation logic to prioritize selector values over manual entries
- Updated method signature to handle new combo box components

### Data Combination Logic

#### Object Class Creation
The system intelligently combines data from both selectors and manual text entries:
1. Collects values from multi-select components
2. Parses manual text area entries (one per line)
3. Combines both sources into a single set (eliminating duplicates)
4. Generates proper LDAP schema definition syntax

#### Attribute Type Creation
The system uses a priority-based approach:
1. If a selector has a value, use it
2. Otherwise, fall back to the manual text field
3. Generate proper LDAP schema definition syntax

### Error Handling
- Graceful fallback if schema access fails
- Empty lists returned when schema is unavailable
- Existing validation logic preserved

## User Experience Improvements

### Usability Benefits
1. **Faster Input**: Users can quickly select from existing schema elements instead of typing
2. **Error Reduction**: Reduces typos and incorrect references to schema elements
3. **Discovery**: Users can see what schema elements are available
4. **Flexibility**: Still allows manual entry for custom or new elements
5. **Consistency**: Ensures proper naming of schema references

### Accessibility
- Clear labels and helper text for all components
- Logical tab order through form elements
- Placeholder text provides guidance
- Alternative manual entry methods for all selectors

### Performance
- Schema data is loaded once when dialog opens
- Lists are sorted alphabetically for quick scanning
- Minimal impact on existing functionality

## Usage Instructions

### To Add an Object Class with Enhanced Selectors:
1. Navigate to Schema tab
2. Click "Add Object Class" button
3. Fill in required fields (Name, OID)
4. Use the multi-select dropdowns to choose:
   - Superior Classes from existing object classes
   - Required Attributes from existing attribute types
   - Optional Attributes from existing attribute types
5. Alternatively, use the manual text areas if needed
6. Click "Add Object Class" to save

### To Add an Attribute Type with Enhanced Selectors:
1. Navigate to Schema tab → Attribute Types tab
2. Click "Add Attribute Type" button
3. Fill in required fields (Name, OID)
4. Use the dropdowns to choose:
   - Syntax OID from available syntaxes
   - Superior Type from existing attribute types
   - Matching Rules from existing matching rules
5. Alternatively, use the manual text fields if needed
6. Click "Add Attribute Type" to save

## Future Enhancements

### Potential Improvements
1. **Real-time Validation**: Validate references against schema in real-time
2. **Dependency Visualization**: Show relationships between schema elements
3. **Import/Export**: Bulk import/export of schema modifications
4. **Templates**: Pre-defined templates for common object classes and attribute types
5. **Search and Filter**: Add search capability to dropdown lists for large schemas
6. **Tooltips**: Show additional information about schema elements on hover

### Schema Element Details
Consider adding expandable details panels showing:
- Object class inheritance hierarchy
- Attribute type properties and constraints
- Matching rule descriptions and syntax compatibility
- Usage examples and best practices

This feature significantly improves the schema modification experience by making it more intuitive, faster, and less error-prone while maintaining full flexibility for advanced users.
