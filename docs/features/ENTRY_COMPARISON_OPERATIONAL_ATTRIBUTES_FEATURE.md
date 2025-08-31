# Entry Comparison Operational Attributes Feature

## Overview
Added an option to include operational attributes in the Entry Comparison tab. This enhancement allows users to compare system-generated attributes alongside regular user attributes when comparing LDAP entries across different environments.

## Features Implemented

### 1. Include Operational Attributes Checkbox
- **Location**: Entry Comparison tab, above the comparison grid in the controls area
- **Purpose**: Toggle the inclusion of operational attributes in the comparison
- **Default State**: Unchecked (operational attributes are hidden by default)
- **Behavior**: When checked, operational attributes are included in the comparison grid

### 2. Operational Attribute Detection
- **Comprehensive Detection**: Uses the same operational attribute detection logic as the AttributeEditor component
- **Pattern-Based Recognition**: Detects operational attributes using common naming patterns:
  - Attributes starting with "create", "modify"
  - Common operational attributes: `entryUUID`, `entryCSN`, `hasSubordinates`, etc.
  - Directory-specific attributes: starting with "ds-", "nsds-", "ads-"
  - Vendor-specific attributes: IBM, Sun, Oracle, Microsoft, Novell prefixes
  - Timestamp-related attributes

### 3. Dynamic Filtering
- **Real-time Updates**: Grid refreshes automatically when checkbox state changes
- **Smart Dropdown Filtering**: The "Hide Attributes" dropdown only shows attributes that are currently visible
- **Preserved Selections**: Hidden attribute selections are maintained when switching between operational attribute modes

## User Workflow

### Without Operational Attributes (Default)
1. Select entries for comparison from the Search tab
2. Switch to Entry Comparison tab
3. View comparison grid showing only user attributes
4. Regular LDAP attributes like `cn`, `mail`, `objectClass` are displayed

### With Operational Attributes
1. Select entries for comparison from the Search tab
2. Switch to Entry Comparison tab
3. Check the "Include operational attributes" checkbox
4. View expanded comparison grid including both user and operational attributes
5. See system-generated attributes like `createTimestamp`, `modifyTimestamp`, `entryUUID`

## Technical Implementation

### Components Modified
- **EntryComparisonTab.java**: Added operational attribute filtering functionality

### New Components Added
- **Checkbox**: `includeOperationalAttributesCheckbox` for toggling operational attributes
- **Detection Method**: `isOperationalAttribute()` for identifying operational attributes
- **Enhanced Layout**: Controls layout to accommodate the new checkbox

### Key Features
- **Performance**: Efficient filtering without requiring additional LDAP queries
- **Consistency**: Uses the same operational attribute detection logic as other components
- **User Experience**: Clear visual indication of what attributes are included
- **Flexibility**: Users can switch between modes without losing their current comparison

## Benefits

### 1. Troubleshooting Enhancement
- **System Analysis**: Allows comparison of system-generated attributes for troubleshooting
- **Timestamp Comparison**: Compare creation and modification times across environments
- **UUID Tracking**: Track entry UUIDs across different LDAP systems

### 2. Administrative Insight
- **Operational Visibility**: See how different LDAP servers handle operational attributes
- **Replication Analysis**: Compare replication-related attributes across environments
- **Audit Trail**: View system-maintained audit information

### 3. Development Support
- **Schema Validation**: Verify operational attribute consistency across environments
- **Migration Planning**: Understand operational attribute differences during migrations
- **Integration Testing**: Compare system behavior across different LDAP implementations

## User Interface Design

### Layout Changes
- **Horizontal Controls Layout**: Checkbox and dropdown are arranged horizontally for better space utilization
- **Consistent Styling**: Checkbox follows the same design patterns as other form controls
- **Clear Labeling**: Descriptive label clearly indicates the checkbox purpose

### Visual Feedback
- **Dynamic Grid Updates**: Grid immediately reflects checkbox state changes
- **Preserved State**: Selection states are maintained when toggling operational attributes
- **Smart Filtering**: Hide attributes dropdown adapts to show only relevant options

## Future Enhancements

Potential improvements that could be added:
- **Operational Attribute Highlighting**: Visual distinction for operational vs. user attributes
- **Attribute Categories**: Group attributes by type (user, operational, vendor-specific)
- **Advanced Filtering**: Filter by specific operational attribute types
- **Export Options**: Include/exclude operational attributes in export functionality

## Compatibility

- **Backward Compatible**: Existing comparison functionality remains unchanged
- **LDAP Standard**: Works with standard LDAP operational attributes
- **Cross-Platform**: Compatible with Active Directory, OpenLDAP, and other LDAP implementations
- **Performance**: No impact on comparison performance when operational attributes are disabled
