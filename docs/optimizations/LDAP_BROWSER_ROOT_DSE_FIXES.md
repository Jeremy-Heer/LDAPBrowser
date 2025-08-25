# LDAP Browser Root DSE Label and Spacing Fixes

## Summary
Fixed the missing Root DSE label and reduced unnecessary spacing between the title and tree content.

## Issues Fixed

### 1. Missing Root DSE Label
**Problem**: The Root DSE entry was showing an empty label because its DN is empty ("") and the display logic was only showing full DNs.

**Solution**: 
- **File**: `src/main/java/com/example/ldapbrowser/ui/components/LdapTreeGrid.java`
- **Method**: `getEntryDisplayName(LdapEntry entry)`
- **Fix**: Added special case handling for Root DSE entries

```java
// Special case for Root DSE - show label instead of empty DN
if (entry.getDn().isEmpty() || "Root DSE".equals(entry.getRdn())) {
    return "Root DSE";
}
```

### 2. Reduced Extra Spacing
**Problem**: There was excessive spacing between the "LDAP Browser" header and the tree content.

**Solutions Applied**:

#### A. MainView Layout Optimization
- **File**: `src/main/java/com/example/ldapbrowser/ui/MainView.java`
- **Changes**:
  - Added explicit margin and padding removal for the tree grid
  - Set `margin-top: 0px` and `padding-top: 0px` on the tree grid

#### B. TreeGrid Styling Improvements  
- **File**: `src/main/java/com/example/ldapbrowser/ui/components/LdapTreeGrid.java`
- **Changes**:
  - Added explicit margin and padding reset: `margin: 0px` and `padding: 0px`
  - This ensures no default spacing is applied to the grid component

## Technical Details

### Root DSE Detection Logic
The fix handles Root DSE entries in two ways:
1. **Empty DN**: `entry.getDn().isEmpty()` - Standard Root DSE case
2. **RDN Match**: `"Root DSE".equals(entry.getRdn())` - Explicitly labeled Root DSE entries

### Spacing Optimization
The spacing reduction targets multiple levels:
- **Tree Grid Container**: Removed margins and padding from the grid itself
- **Layout Integration**: Ensured the tree grid connects directly to the header without gaps

## Result
✅ **Root DSE Label**: Now displays "Root DSE" text instead of being blank  
✅ **Compact Layout**: Reduced spacing between header and tree content  
✅ **Icon Preserved**: Root DSE continues to show the database icon  
✅ **Full DN Display**: Other entries still show their complete Distinguished Names  

## Visual Improvements
- Clean, compact appearance with minimal spacing
- Root DSE is clearly identifiable with both icon and label
- Consistent with professional LDAP browser interfaces
- Maintains all previously implemented functionality
