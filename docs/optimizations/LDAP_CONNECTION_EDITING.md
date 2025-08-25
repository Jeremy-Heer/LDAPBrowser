# LDAP Connection Editing Feature

## Overview
Added the ability to edit LDAP connections directly from the Connections tab, replacing the previous placeholder message.

## Changes Made

### 1. Created ServerConfigDialog.java
- **Location**: `src/main/java/com/example/ldapbrowser/ui/components/ServerConfigDialog.java`
- **Purpose**: Standalone dialog for creating and editing LDAP server configurations
- **Features**:
  - Form-based interface with all LDAP connection parameters
  - Validation for required fields (Name and Host)
  - Auto-adjustment of port when SSL is enabled/disabled (389 ↔ 636)
  - Mutual exclusion between SSL and StartTLS options
  - Save/Cancel functionality with proper event handling

### 2. Updated ConnectionsTab.java
- **Modified Method**: `openServerDialog(LdapServerConfig config)`
- **Change**: Replaced placeholder message with actual dialog functionality
- **Removed**: Unused `showInfo()` method
- **Added**: Integration with `ServerConfigDialog` including save listeners and success notifications

### 3. Enhanced User Experience
- **Add Server**: Click "Add Server" button opens dialog for new server creation
- **Edit Server**: Select a server and click "Edit" to modify existing configuration
- **Validation**: Required fields are validated with visual feedback
- **Feedback**: Success notifications inform users when configurations are saved

## Technical Details

### Form Fields
- **Name** (required): Friendly name for the server
- **Host** (required): LDAP server hostname or IP address
- **Port**: Port number (default 389, changes to 636 when SSL enabled)
- **Bind DN**: Distinguished name for authentication
- **Password**: Password for the bind DN
- **Use SSL**: Enable LDAPS connections
- **Use StartTLS**: Enable StartTLS encryption (mutually exclusive with SSL)
- **Base DN**: Default base DN for browsing and searching

### Validation Rules
- Name and Host fields are required
- Port must be between 1 and 65535
- SSL and StartTLS are mutually exclusive
- Port automatically adjusts when SSL is toggled (389 ↔ 636)

### Integration
- Dialog integrates with existing `ConfigurationService` for persistence
- Updates are reflected immediately in the server grid
- Success notifications provide user feedback
- Dialog properly handles both new and existing server configurations

## Testing
- Added unit tests in `ServerConfigDialogTest.java`
- Tests cover dialog creation for new and existing configurations
- Tests validate model properties and behavior

## User Workflow
1. Navigate to the "Connections" tab
2. Click "Add Server" to create a new configuration, or
3. Select an existing server and click "Edit" to modify it
4. Fill in the required fields in the dialog
5. Click "Save" to persist the configuration
6. Configuration appears in the server list and can be used for connections

This implementation removes the placeholder message and provides full LDAP connection management functionality.
