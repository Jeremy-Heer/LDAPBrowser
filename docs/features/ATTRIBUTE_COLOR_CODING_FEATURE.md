# LDAP Browser - Attribute Color Coding Feature

## Overview

This feature enhances the LDAP entry details view by implementing color-coded attribute names based on their schema classification. When viewing an entry's attributes, different font colors are used to indicate the attribute's role and type according to the LDAP schema definition.

## Color Scheme

The following color coding is applied to attribute names in the entry details view:

### 🔴 **Red** - Required Attributes ("must")
- **Color**: `#d32f2f` (Red)
- **Description**: Attributes that are listed as "must" (required) in any of the objectClasses for the given entry
- **Tooltip**: "Required attribute (must)"

### 🔵 **Blue** - Optional Attributes ("may") 
- **Color**: `#1976d2` (Blue)
- **Description**: Attributes that are listed as "may" (optional) in any of the objectClasses for the given entry
- **Tooltip**: "Optional attribute (may)"

### 🟠 **Orange** - Operational Attributes
- **Color**: `#f57c00` (Orange)
- **Description**: Attributes defined as operational (system-generated) when operational attributes are chosen to be displayed
- **Tooltip**: "Operational attribute"
- **Note**: Only visible when the "Show operational attributes" checkbox is enabled

### ⚫ **Default** - Regular Attributes
- **Color**: Default text color
- **Description**: Attributes that don't fall into the above categories or when schema information is unavailable

## Implementation Details

### Files Modified

- **`AttributeEditor.java`**: Main component for editing LDAP entry attributes
  - Added new imports for schema classes
  - Modified attribute column to use `ComponentRenderer` with styled `Span` components
  - Added color classification logic based on schema information

### Key Methods Added

#### `createAttributeNameComponent(AttributeRow row)`
Creates a styled Span component for attribute names with appropriate color coding based on schema classification.

#### `classifyAttribute(String attributeName, Schema schema)`
Analyzes an attribute against the entry's object classes to determine if it's required, optional, operational, or regular.

#### `isOperationalAttributeBySchema(String attributeName, Schema schema)`
Enhanced operational attribute detection using both heuristic patterns and schema usage information.

#### `AttributeClassification` enum
Defines the four classification types:
- `REQUIRED` - Must attributes from object classes
- `OPTIONAL` - May attributes from object classes  
- `OPERATIONAL` - System/operational attributes
- `REGULAR` - Unclassified attributes

### Schema Integration

The feature integrates with the LDAP schema to:

1. **Retrieve Object Class Definitions**: Uses the entry's `objectClass` values to get schema definitions
2. **Extract Required Attributes**: Collects all "must" attributes from object classes
3. **Extract Optional Attributes**: Collects all "may" attributes from object classes
4. **Check Operational Status**: Uses `AttributeUsage.DIRECTORY_OPERATION` from schema plus heuristic patterns

### Error Handling

- Graceful fallback when schema information is unavailable
- Exception handling ensures UI remains functional even with schema access issues
- Default styling applied when classification fails

## Usage

The color coding is automatically applied when viewing entry details:

1. **Navigate** to an LDAP entry in the directory tree
2. **View** the entry details in the attribute editor
3. **Observe** color-coded attribute names based on their schema classification
4. **Toggle** "Show operational attributes" to see/hide operational attributes in orange

## Benefits

### For LDAP Administrators
- **Quick Identification**: Instantly identify required vs optional attributes
- **Schema Compliance**: Easily see which attributes are mandated by object classes
- **Operational Awareness**: Distinguish system-generated from user-managed attributes

### For Directory Developers
- **Schema Understanding**: Visual representation of schema relationships
- **Debugging Aid**: Quickly identify attribute classification issues
- **Documentation**: Self-documenting interface for schema structure

## Technical Considerations

### Performance
- Schema lookup is cached per connection
- Classification is computed on-demand for visible entries only
- Minimal performance impact on attribute rendering

### Compatibility
- Works with any LDAP server that provides schema information
- Graceful degradation when schema is unavailable
- Compatible with existing operational attribute detection logic

### Accessibility
- Color coding is supplemented with tooltips for clarity
- Maintains text readability with appropriate contrast ratios
- Screen reader compatible with semantic markup

## Future Enhancements

Potential improvements could include:

1. **Customizable Colors**: User preference for color scheme
2. **Additional Classifications**: Support for other schema properties (e.g., single-valued, collective)
3. **Schema Inheritance**: Visual indication of inherited attributes from superior object classes
4. **Attribute Descriptions**: Tooltips with schema descriptions from attribute type definitions
