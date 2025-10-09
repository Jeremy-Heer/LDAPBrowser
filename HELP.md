# LDAP Browser Help

This comprehensive guide will help you navigate and use all features of the LDAP Browser application effectively.

## Getting Started

### 1. Connect to an LDAP Server

Start by configuring and connecting to your LDAP server:

- Navigate to Settings to add a new server configuration
- Enter server details (host, port, bind DN, password)
- Test the connection to ensure it works
- Select the server from the sidebar to connect

### 2. Browse Directory Structure

Once connected, explore your LDAP directory:

- Use the LDAP Browser tab to navigate the directory tree
- Click on entries to view and edit their attributes
- Expand nodes to see child entries
- Use the refresh button to update the view

## Navigation

### Sidebar Navigation

The left sidebar provides quick access to:

- **Home** - Welcome page with quick tips
- **Server Selection** - Dropdown to choose active server
- **Group Selection** - Filter servers by group
- **Internal Servers** - In-memory test servers
- **Settings** - Configuration and server management
- **Help** - This help page

### Tab Interface

When connected to a server, you'll see multiple tabs:

- **LDAP Browser** - Main directory browsing interface
- **Search** - Advanced search capabilities
- **Schema** - Browse LDAP schema components
- **Access** - Access control and permissions
- **Reports** - Export and reporting tools
- **Bulk Operations** - Batch operations on entries

## LDAP Browser Features

### Directory Tree Navigation

Navigate through your LDAP directory structure:

- Click the expand icon (►) to show child entries
- Click on any entry to select it and view details
- Use the refresh button to reload the current level
- Right-click for context menu options

### Entry Details & Attribute Editing

View and modify LDAP entry attributes:

- Select an entry to view its attributes in the details panel
- Click 'Edit' to modify attribute values
- Add new attributes using the '+ Add Attribute' button
- Remove attributes with the delete button
- Save changes or cancel to revert

### Creating New Entries

Add new entries to your directory:

- Select a parent entry where you want to add a child
- Use the 'New Entry' tab
- Choose object classes for the new entry
- Fill in required attributes
- Click 'Create Entry' to save

## Search Functionality

### Basic Search

Find entries quickly using search:

- Go to the Search tab
- Enter search base DN (or leave blank for root)
- Choose search scope (Base, One Level, Subtree)
- Enter search filter (e.g., '(cn=john*)' for names starting with 'john')
- Click 'Search' to execute

### Advanced Search

Use advanced search options:

- Combine multiple filters with AND/OR logic
- Use wildcards (* and ?) in search terms
- Limit results with size and time limits
- Sort results by specific attributes
- Export search results

### Search Filter Examples

Common LDAP filter patterns:

- `(objectClass=person)` - Find all person objects
- `(cn=john*)` - Names starting with 'john'
- `(mail=*@example.com)` - Email addresses from example.com
- `(&(objectClass=user)(department=IT))` - IT department users
- `(|(cn=admin)(cn=administrator))` - Either admin or administrator

## Schema Browser

### Understanding LDAP Schema

Explore your LDAP schema components:

- **Object Classes** - Structural templates for entries
- **Attributes** - Properties that entries can have
- **Syntaxes** - Data types and validation rules
- **Matching Rules** - How attributes are compared
- Use schema info to understand entry structure

### Object Class Details

View object class information:

- **Required attributes** - Must be present in entries
- **Optional attributes** - May be present in entries
- **Parent classes** - Inheritance hierarchy
- **Abstract vs Structural vs Auxiliary classes**

## Server Management

### External LDAP Servers

Configure connections to external LDAP servers:

- Go to Settings > External LDAP Servers
- Click 'Add Server' to create new configuration
- Fill in connection details (host, port, SSL, credentials)
- Test connection before saving
- Group servers for better organization

### Internal Test Servers

Create in-memory LDAP servers for testing:

- Go to Settings > Internal LDAP Servers
- Create servers with sample data
- Useful for testing and demonstrations
- Automatically appear in server selection

## Troubleshooting

### Connection Issues

If you can't connect to your LDAP server:

- Verify server host and port are correct
- Check if SSL/TLS settings match server configuration
- Ensure credentials (bind DN and password) are valid
- Check network connectivity and firewall settings
- Verify the server is running and accepting connections

### Search Problems

If searches don't return expected results:

- Check search base DN is correct
- Verify search scope (base, one, sub) is appropriate
- Test search filters with simple patterns first
- Ensure you have proper permissions to search
- Check size and time limits

### Common Error Messages

Understanding common LDAP errors:

- **'Invalid Credentials'** - Wrong username/password
- **'No Such Object'** - DN doesn't exist
- **'Insufficient Access Rights'** - Permission denied
- **'Size Limit Exceeded'** - Too many search results
- **'Time Limit Exceeded'** - Search took too long
