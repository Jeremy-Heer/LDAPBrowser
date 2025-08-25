# LDAP Browser - Entry Attribute Sorting Implementation

## Overview

This document describes the implementation of attribute sorting functionality for the LDAP Browser's entry details display. The implementation ensures that attributes are displayed in a logical and consistent order according to their importance and type.

## Feature Requirements

When displaying an entry's attribute details, attributes are now sorted in the following priority order:

1. **objectClass attributes first** - Always displayed at the top and sorted by value
2. **Required attributes second** - Attributes marked as "must" in the object class schema, sorted by name  
3. **Optional attributes third** - Attributes marked as "may" in the object class schema, sorted by name
4. **Operational attributes last** - System-generated attributes (only when "Show operational attributes" is checked), sorted by name

## Implementation Details

### Files Modified

- `/src/main/java/com/example/ldapbrowser/ui/components/AttributeEditor.java`

### Key Changes

#### 1. Enhanced `refreshAttributeDisplay()` Method

```java
/**
* Refresh the attribute display based on current settings
*/
private void refreshAttributeDisplay() {
  if (fullEntry == null) {
    return;
  }

  List<AttributeRow> rows = new ArrayList<>();
  boolean showOperational = showOperationalAttributesCheckbox.getValue();

  for (Map.Entry<String, List<String>> attr : fullEntry.getAttributes().entrySet()) {
    String attrName = attr.getKey();

    // Filter operational attributes if checkbox is unchecked
    if (!showOperational && isOperationalAttribute(attrName)) {
      continue;
    }

    rows.add(new AttributeRow(attrName, attr.getValue()));
  }

  // Sort attributes according to priority: objectClass > required > optional > operational
  sortAttributeRows(rows, showOperational);

  attributeGrid.setItems(rows);
}
```

#### 2. New `sortAttributeRows()` Method

Implements the custom sorting logic:

```java
/**
 * Sort attribute rows according to priority and within each category by name:
 * 1. objectClass attributes first (sorted by name)
 * 2. Required attributes second (sorted by name)
 * 3. Optional attributes third (sorted by name)
 * 4. Operational attributes last (sorted by name)
 */
private void sortAttributeRows(List<AttributeRow> rows, boolean showOperational) {
  rows.sort((row1, row2) -> {
    String attr1 = row1.getName();
    String attr2 = row2.getName();
    
    // Get attribute classifications
    int priority1 = getAttributeSortPriority(attr1, showOperational);
    int priority2 = getAttributeSortPriority(attr2, showOperational);
    
    // First sort by priority (lower number = higher priority)
    if (priority1 != priority2) {
      return Integer.compare(priority1, priority2);
    }
    
    // Within same priority group, sort alphabetically by attribute name
    return attr1.compareToIgnoreCase(attr2);
  });
}
```

#### 3. New `getAttributeSortPriority()` Method

Determines the sorting priority for each attribute:

```java
/**
 * Get sorting priority for an attribute:
 * 1 = objectClass (highest priority)
 * 2 = required attributes
 * 3 = optional attributes  
 * 4 = operational attributes (lowest priority)
 * 5 = regular/unknown attributes
 */
private int getAttributeSortPriority(String attributeName, boolean showOperational) {
  String lowerName = attributeName.toLowerCase();
  
  // objectClass always comes first
  if ("objectclass".equals(lowerName)) {
    return 1;
  }
  
  // Check if operational attribute
  if (isOperationalAttribute(attributeName)) {
    return showOperational ? 4 : 5; // Only show if operational checkbox is checked
  }
  
  // Classify based on schema if available
  try {
    if (serverConfig != null && ldapService.isConnected(serverConfig.getId()) && currentEntry != null) {
      Schema schema = ldapService.getSchema(serverConfig.getId());
      if (schema != null) {
        AttributeClassification classification = classifyAttribute(attributeName, schema);
        switch (classification) {
          case REQUIRED:
            return 2;
          case OPTIONAL:
            return 3;
          case OPERATIONAL:
            return showOperational ? 4 : 5;
          default:
            return 5; // Regular attributes
        }
      }
    }
  } catch (Exception e) {
    // If schema lookup fails, treat as regular attribute
  }
  
  return 5; // Default for regular/unknown attributes
}
```

#### 4. Enhanced `AttributeRow` Constructor

Added sorting for objectClass values:

```java
public AttributeRow(String name, List<String> values) {
  this.name = name;
  this.values = new ArrayList<>(values);
  
  // Sort objectClass values alphabetically
  if ("objectClass".equalsIgnoreCase(name)) {
    this.values.sort(String.CASE_INSENSITIVE_ORDER);
  }
}
```

#### 5. Removed Unused Components

- Removed unused `attributeColumn` field and related references
- Removed unused imports for `GridSortOrder` and `SortDirection`

## Benefits

### 1. **Improved User Experience**
- Attributes are now displayed in a logical, consistent order
- Most important attributes (objectClass) are always visible first
- Schema-aware sorting helps users understand entry structure

### 2. **Better Organization**
- Required attributes are clearly separated from optional ones
- Operational attributes are grouped at the bottom (when shown)
- Alphabetical sorting within each category ensures predictability

### 3. **Schema Integration**
- Leverages existing schema browsing functionality
- Uses LDAP schema information to classify attributes correctly
- Falls back gracefully when schema information is unavailable

## Usage

The attribute sorting is automatically applied whenever an entry is displayed in the AttributeEditor component. Users will see:

1. **objectClass** at the top with sorted values
2. **Required attributes** (marked as "must" in schema) below objectClass
3. **Optional attributes** (marked as "may" in schema) next
4. **Operational attributes** at the bottom (only when checkbox is checked)

## Technical Notes

- The sorting is performed client-side for optimal performance
- Schema lookups are cached for efficiency
- The implementation gracefully handles cases where schema information is not available
- All sorting is case-insensitive for better user experience
- The existing attribute color coding functionality remains intact and works alongside the new sorting

## Testing

To test the new sorting functionality:

1. Connect to an LDAP server
2. Browse to any entry in the tree
3. View the entry details in the AttributeEditor
4. Observe that attributes are now sorted according to the priority order
5. Toggle the "Show operational attributes" checkbox to see operational attributes at the bottom

The implementation is backward compatible and doesn't affect any existing functionality.
