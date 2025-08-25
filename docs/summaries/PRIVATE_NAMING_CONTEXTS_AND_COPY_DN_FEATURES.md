# Private Naming Contexts and Copy DN Features

## Overview
This document describes the implementation of two new features in the LDAP Browser:
1. **Private Naming Contexts Option** - Option to include private naming contexts in the Dashboard tab
2. **Copy DN Button** - Copy DN to clipboard functionality in the Entry Details panel

## Features Implemented

### 1. Private Naming Contexts Option

#### Description
Added a checkbox option in the Dashboard tab's LDAP Browser section that allows users to include private naming contexts (`ds-private-naming-contexts`) along with the standard `namingContexts` when browsing the LDAP directory tree.

#### Implementation Details

**Backend Changes:**
- **LdapService.java**: Added `getPrivateNamingContexts()` method to retrieve `ds-private-naming-contexts` from Root DSE
- **LdapService.java**: Enhanced `loadRootDSEWithNamingContexts()` with overloaded method accepting `includePrivateNamingContexts` parameter
- **LdapTreeGrid.java**: Updated `loadRootDSEWithNamingContexts()` with overloaded method supporting private naming contexts flag

**Frontend Changes:**
- **DashboardTab.java**: Added checkbox component for "Include private naming contexts"
- **DashboardTab.java**: Integrated checkbox with automatic tree reload when toggled
- **DashboardTab.java**: Positioned checkbox between browser header and tree for easy access

#### Usage
1. Navigate to the Dashboard tab
2. Look for the "Include private naming contexts" checkbox below the LDAP Browser header
3. Check the box to include private naming contexts in the directory tree
4. Uncheck to show only standard naming contexts
5. Tree automatically reloads when checkbox state changes

#### Technical Notes
- Private naming contexts are retrieved from the `ds-private-naming-contexts` attribute in Root DSE
- This feature is particularly useful for browsing administrative or internal LDAP partitions
- If private naming contexts are not available, the checkbox will have no additional effect
- The feature maintains backward compatibility with existing functionality

### 2. Copy DN Button

#### Description
Added a copy button next to the DN (Distinguished Name) in the Entry Details panel that allows users to copy the entry's DN to the clipboard with a single click.

#### Implementation Details

**Frontend Changes:**
- **AttributeEditor.java**: Added copy DN button with clipboard icon next to the DN display
- **AttributeEditor.java**: Implemented `copyDnToClipboard()` method using JavaScript clipboard API
- **AttributeEditor.java**: Enhanced layout to include DN row with copy button
- **AttributeEditor.java**: Added proper button state management (enabled/disabled)

#### Usage
1. Navigate to the Dashboard tab
2. Select any entry from the LDAP tree browser
3. In the Entry Details panel, locate the DN field
4. Click the copy icon button next to the DN
5. DN is copied to clipboard with success notification
6. Paste the DN wherever needed (e.g., search filters, external tools)

#### Technical Notes
- Uses modern browser clipboard API (`navigator.clipboard.writeText()`)
- Provides user feedback through notifications
- Button is automatically enabled/disabled based on entry selection
- Graceful fallback if clipboard API is not supported by browser
- Button follows the same styling pattern as other action buttons in the interface

## Browser Compatibility
Both features use modern web APIs and are compatible with:
- Chrome 66+
- Firefox 63+
- Safari 13.1+
- Edge 79+

For older browsers, the clipboard functionality may not work, but the application will handle this gracefully with error notifications.

## Files Modified

### Backend Services
- `src/main/java/com/example/ldapbrowser/service/LdapService.java`
- `src/main/java/com/example/ldapbrowser/ui/components/LdapTreeGrid.java`

### Frontend Components
- `src/main/java/com/example/ldapbrowser/ui/components/DashboardTab.java`
- `src/main/java/com/example/ldapbrowser/ui/components/AttributeEditor.java`

## Testing

### Private Naming Contexts
1. Connect to an LDAP server that has private naming contexts
2. Verify checkbox appears in Dashboard tab
3. Toggle checkbox and verify tree reloads with/without private contexts
4. Verify notification messages indicate inclusion of private contexts

### Copy DN Feature
1. Connect to any LDAP server
2. Select various entries in the tree
3. Verify copy button appears and is enabled for selected entries
4. Click copy button and verify DN is copied to clipboard
5. Test pasting the copied DN in external applications
6. Verify success notifications appear after copying

## Benefits
1. **Enhanced Directory Navigation** - Access to private/administrative naming contexts
2. **Improved Workflow** - Easy DN copying for external tool integration
3. **User Experience** - Intuitive interface with immediate feedback
4. **Professional Features** - Enterprise-grade functionality for LDAP administration
5. **Backward Compatibility** - Existing functionality preserved and enhanced

These features significantly enhance the LDAP Browser's capabilities for directory administration and make it more competitive with professional LDAP tools like Apache Directory Studio.
