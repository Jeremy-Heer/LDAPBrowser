# New Entry Template Feature

## Overview
Added an optional template dropdown to the New Entry sub-tab in the LDAP Browser tab that allows users to quickly populate common LDAP entry types with their required attributes.

## Feature Details

### Location
- **Tab**: LDAP Browser
- **Sub-tab**: New Entry
- **Component**: Template dropdown (positioned between DN field and attribute table)

### Template Options
The dropdown includes the following predefined templates:

1. **None** (default) - No template applied
2. **User** - inetOrgPerson objectClass with minimum required attributes
3. **Group** - groupOfUniqueNames objectClass with minimum required attributes  
4. **Dynamic Group** - groupOfUniqueNames with memberURL attribute
5. **OU** - organizationalUnit objectClass with minimum required attributes

### Template Attribute Mappings

#### User Template (inetOrgPerson)
- `objectClass`: inetOrgPerson
- `cn`: (empty - user input required)
- `sn`: (empty - user input required) 
- `uid`: (empty - user input required)
- `mail`: (empty - user input required)

#### Group Template (groupOfUniqueNames)
- `objectClass`: groupOfUniqueNames
- `cn`: (empty - user input required)
- `uniqueMember`: (empty - user input required)

#### Dynamic Group Template
- `objectClass`: groupOfUniqueNames
- `cn`: (empty - user input required)
- `uniqueMember`: (empty - user input required)
- `memberURL`: (empty - user input required)

#### OU Template (organizationalUnit)
- `objectClass`: organizationalUnit
- `ou`: (empty - user input required)

## Implementation Details

### Files Modified
- `/src/main/java/com/example/ldapbrowser/ui/components/NewEntryTab.java`

### New Components Added
- `ComboBox<String> templateComboBox` - Template selection dropdown
- `applyTemplate(String template)` method - Applies selected template attributes
- `addTemplateAttribute(String name, String value)` method - Helper method to add template attributes

### Behavior
1. When a template is selected, existing user-entered attributes are preserved
2. Template attributes are added to the attribute table if they don't already exist
3. Empty row is automatically added at the end for additional custom attributes
4. "Clear All" button resets template dropdown to "None"
5. Template attributes show with empty values requiring user input for the values

### User Experience
1. User navigates to LDAP Browser tab
2. Clicks on "New Entry" sub-tab  
3. Selects desired template from "Entry Template (Optional)" dropdown
4. Required attributes for the selected template are automatically populated
5. User fills in the attribute values and DN
6. User can add additional custom attributes as needed
7. User clicks "Create Entry" to create the LDAP entry

## Technical Notes
- Template selection preserves any existing user-entered attributes
- Duplicate attribute names are prevented when applying templates
- Templates provide a starting point but don't restrict additional attributes
- All LDAP objectClass and attribute validation still applies during entry creation
