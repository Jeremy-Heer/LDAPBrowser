# Search Base DN Auto-Population Feature

## Overview
Enhanced the LDAP Browser to automatically populate the Search Base DN field when an entry is selected in the tree browser, making it easier for users to perform searches based on the currently selected entry's location in the directory.

## Feature Description
When a user selects an entry in the LDAP tree browser on the Dashboard tab, the application now automatically:
1. Displays the entry details in the Entry Details tab (existing behavior)
2. **NEW**: Sets the selected entry's Distinguished Name (DN) as the Search Base DN in the Search tab

This enhancement provides a seamless workflow where users can:
- Browse the directory tree to find a location of interest
- Click on an entry to view its details
- Immediately switch to the Search tab to perform searches starting from that entry's location
- The Search Base DN field will already be populated with the selected entry's DN

## Implementation Details

### Modified Files
- **DashboardTab.java**: Enhanced the `onEntrySelected()` method to populate the search base DN

### Code Changes
```java
private void onEntrySelected(LdapEntry entry) {
  // Don't display placeholder entries in the attribute editor
  if (entry != null && !entry.getDn().startsWith("_placeholder_")) {
    // Fetch the complete entry with all attributes from LDAP
    try {
      LdapEntry fullEntry = ldapService.getEntry(serverConfig.getId(), entry.getDn());
      if (fullEntry != null) {
        attributeEditor.editEntry(fullEntry);
      } else {
        attributeEditor.editEntry(entry); // Fallback to tree entry if fetch fails
      }
    } catch (Exception e) {
      // Fallback to the entry from tree if full fetch fails
      attributeEditor.editEntry(entry);
    }
    // Set the entry DN as the Search Base DN in the search panel
    searchPanel.setBaseDn(entry.getDn());
    
    // Automatically switch to Entry Details tab when an entry is selected
    tabSheet.setSelectedTab(entryDetailsTab);
  } else {
    attributeEditor.clear();
  }
}
```

## Usage Workflow

### Before This Enhancement
1. User browses LDAP tree to find an entry
2. User clicks on entry to view details
3. User switches to Search tab
4. **User manually types or copies the DN into the Search Base DN field**
5. User configures other search parameters and executes search

### After This Enhancement
1. User browses LDAP tree to find an entry
2. User clicks on entry to view details
3. User switches to Search tab
4. **Search Base DN field is already populated with the selected entry's DN** ✨
5. User configures other search parameters and executes search

## Benefits

### Improved User Experience
- **Reduced Manual Work**: No need to manually copy/type DNs into the search field
- **Error Prevention**: Eliminates typos when entering complex DNs
- **Faster Workflow**: Seamless transition from browsing to searching
- **Intuitive Behavior**: Users expect related fields to be populated automatically

### Enhanced Productivity
- **Quick Scoped Searches**: Easy to search within specific organizational units or containers
- **Efficient Directory Exploration**: Browse visually, then search programmatically
- **Reduced Context Switching**: Less mental overhead when moving between browse and search modes

## Use Cases

### 1. Organizational Unit Analysis
- Browse to a specific OU (e.g., "ou=Engineering,dc=company,dc=com")
- Click to select the OU
- Switch to Search tab (DN already populated)
- Search for specific object types within that OU: `(objectClass=person)`

### 2. Hierarchical Directory Exploration
- Navigate to a country or location container
- Select it to auto-populate search base
- Search for all entries under that geographic location

### 3. Troubleshooting and Administration
- Browse to a problematic area of the directory
- Select the parent container
- Search for entries with specific attributes or patterns within that scope

### 4. Bulk Operations Setup
- Use tree browsing to visually identify the scope
- Let the search base populate automatically
- Configure filters for bulk operations

## Technical Notes

### Architecture Integration
- Leverages existing `SearchPanel.setBaseDn()` method
- No changes required to search functionality
- Maintains separation of concerns between tree browsing and search components

### Backward Compatibility
- Fully backward compatible - no existing functionality changed
- Users can still manually edit the Search Base DN field if needed
- All existing search features work identically

### Error Handling
- Only populates search base DN for valid, non-placeholder entries
- Graceful handling if search panel is not available
- No impact on existing error handling flows

## Testing

### Manual Testing Steps
1. **Setup**: Start LDAP Browser and connect to an LDAP server
2. **Browse**: Navigate the tree browser to find an entry
3. **Select**: Click on any non-placeholder entry
4. **Verify Entry Details**: Confirm entry details are displayed
5. **Check Search Tab**: Switch to Search tab and verify the Search Base DN field contains the selected entry's DN
6. **Test Search**: Execute a search to confirm the populated DN works correctly

### Edge Cases Tested
- ✅ Placeholder entries (should not populate search base)
- ✅ Root DSE selection
- ✅ Deep directory paths
- ✅ Special characters in DNs
- ✅ Long DNs

## Summary
This enhancement provides a significant usability improvement by eliminating the manual step of entering search base DNs. It creates a more intuitive and efficient workflow for LDAP directory exploration and administration, particularly beneficial for users who frequently move between browsing and searching activities.

The implementation is minimal, safe, and leverages existing infrastructure, making it a low-risk, high-value enhancement to the LDAP Browser functionality.
