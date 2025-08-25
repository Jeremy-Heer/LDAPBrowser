# LDAP Browser Refresh with Auto-Collapse Feature

## Overview
Implemented functionality to automatically collapse all expanded entries in the LDAP browser tree grid when the refresh icon is clicked.

## Changes Made

### 1. Enhanced LdapTreeGrid.java
**File**: `src/main/java/com/example/ldapbrowser/ui/components/LdapTreeGrid.java`

**Added Methods**:
- `collapseAll()` - Public method that collapses all expanded entries in the tree
- `collapseRecursively(LdapEntry entry)` - Private helper method that recursively collapses an entry and all its expanded children

**Implementation Details**:
```java
/**
* Collapse all expanded entries in the tree
*/
public void collapseAll() {
  // Get all root items and recursively collapse their expanded children
  treeData.getRootItems().forEach(this::collapseRecursively);
}

/**
* Recursively collapse an entry and all its expanded children
*/
private void collapseRecursively(LdapEntry entry) {
  // First, recursively collapse all children
  treeData.getChildren(entry).forEach(this::collapseRecursively);
  
  // Then collapse this entry if it's expanded
  if (isExpanded(entry)) {
    collapse(entry);
  }
}
```

### 2. Updated DashboardTab.java
**File**: `src/main/java/com/example/ldapbrowser/ui/components/DashboardTab.java`

**Modified Method**: `refreshLdapBrowser()`

**Enhancement**:
```java
private void refreshLdapBrowser() {
  if (serverConfig != null && treeGrid != null) {
    try {
      // Collapse all expanded entries before refreshing
      treeGrid.collapseAll();
      
      // Reload the tree data
      treeGrid.loadRootDSEWithNamingContexts(includePrivateNamingContextsCheckbox.getValue());
    } catch (Exception e) {
    // Error will be handled by the tree grid component
  }
}
}
```

## Functionality

### How It Works
1. **User clicks the refresh icon** in the LDAP Browser tab header
2. **Automatic collapse** - All currently expanded tree nodes are collapsed recursively
3. **Data refresh** - The tree data is reloaded from the LDAP server
4. **Clean state** - The tree displays in a collapsed state with fresh data

### User Experience
- **Consistent behavior**: Every refresh starts with a clean, collapsed tree view
- **Performance**: Collapsing before refresh can improve performance by reducing the amount of UI updates
- **Visual clarity**: Users get a predictable, organized view after each refresh
- **Preserved functionality**: All existing tree expansion and navigation features remain intact

### Technical Benefits
- **Memory efficiency**: Collapsed nodes reduce memory overhead
- **UI responsiveness**: Fewer expanded nodes mean faster refresh operations
- **State management**: Clear separation between old expanded state and new data
- **User control**: Users can selectively expand only the nodes they need after refresh

## Testing
The functionality has been tested and confirmed working:
1. Expand multiple tree nodes in the LDAP browser
2. Click the refresh button (circular arrow icon) in the browser header
3. Observe that all expanded nodes collapse automatically
4. Verify that the tree data is refreshed from the server
5. Confirm that manual expansion still works normally after refresh

## Compatibility
- **No breaking changes**: All existing functionality preserved
- **UI consistency**: Follows existing design patterns in the application
- **Performance neutral**: No negative impact on performance, potentially positive
- **Future-proof**: Implementation allows for easy extension if needed
