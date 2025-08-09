# LDAP Browser - Tabbed Interface with Schema Browser

## Overview
The LDAP Browser now features a modern tabbed interface with two main sections:
1. **Dashboard** - Contains the original LDAP browser functionality
2. **Schema** - New schema browser for exploring LDAP schema information

## New Features

### Tabbed Interface
- Clean, professional tab layout across the top of the application
- Seamless switching between Dashboard and Schema views
- Connection controls remain accessible from both tabs

### Dashboard Tab
The Dashboard tab contains all the original LDAP browser functionality:
- **LDAP Tree Browser** - Browse directory entries in a hierarchical tree view
- **Entry Details** - View and edit attributes of selected entries
- **Search Panel** - Perform advanced LDAP searches with custom filters
- **Search Results** - View search results with configurable return attributes

### Schema Tab
The new Schema tab provides comprehensive schema browsing capabilities:

#### Schema Element Types
1. **Object Classes**
   - View all object classes in the LDAP schema
   - Display name, description, type (structural/auxiliary/abstract), and obsolete status
   - Show superior classes, required attributes, optional attributes, and extensions

2. **Attribute Types**
   - Browse all attribute types with detailed information
   - View syntax OID, usage, and matching rules
   - Display single-value, collective, and modification restrictions

3. **Matching Rules**
   - Explore matching rules used for attribute comparisons
   - View associated syntax OIDs and descriptions

4. **Matching Rule Use**
   - See how matching rules are applied to specific attribute types
   - View applicable attribute types for each matching rule

5. **Syntaxes**
   - Browse all supported attribute syntaxes
   - View OID and description for each syntax

#### Schema Browser Features
- **Search Functionality** - Filter schema elements by name, OID, or description
- **Detailed Views** - Click any schema element to view comprehensive details
- **Professional Layout** - Grid view with resizable columns and professional styling
- **Real-time Filtering** - Instant search results as you type

## Connection Management
- **Unified Connection Bar** - Connection controls work across both tabs
- **Automatic Schema Loading** - Schema information is automatically loaded when connecting
- **Server-specific Schema** - Each LDAP server's schema is browsed independently

## User Interface Improvements
- **Apache Directory Studio Styling** - Professional look and feel
- **Responsive Layout** - Flexible split panes and resizable columns
- **Enhanced Visual Hierarchy** - Clear separation between different functional areas
- **Improved Navigation** - Intuitive tab switching and panel organization

## Usage Instructions

### Using the Dashboard Tab
1. Select an LDAP server from the dropdown
2. Click "Connect" to establish connection
3. Browse the directory tree in the left panel
4. Select entries to view details in the center panel
5. Use the search panel to perform custom searches

### Using the Schema Tab
1. Ensure you're connected to an LDAP server
2. Switch to the "Schema" tab
3. Use the schema type tabs to browse different elements:
   - Object Classes
   - Attribute Types
   - Matching Rules
   - Matching Rule Use
   - Syntaxes
4. Use the search field to filter results
5. Click any element to view detailed information in the right panel

## Technical Implementation

### Component Architecture
- **MainView** - Main application layout with tabbed interface
- **DashboardTab** - Wrapper component containing original LDAP browser functionality
- **SchemaBrowser** - New component for schema exploration
- **Shared Services** - LdapService provides both directory and schema access

### Schema Data Access
- Uses UnboundID LDAP SDK's Schema class
- Provides access to all standard schema element types
- Real-time filtering and sorting capabilities
- Detailed information extraction for each schema element

### CSS Styling
- Professional tab styling with hover effects
- Grid-based layouts for schema browsing
- Consistent styling across both tabs
- Responsive design for different screen sizes

## Benefits
1. **Enhanced Functionality** - Schema browsing capability previously unavailable
2. **Improved Organization** - Logical separation of directory browsing and schema exploration
3. **Professional Interface** - Modern tabbed layout similar to enterprise LDAP tools
4. **Comprehensive Schema Access** - Full visibility into LDAP server schema definitions
5. **Better User Experience** - Intuitive navigation and clear visual hierarchy

This update significantly enhances the LDAP Browser's capabilities while maintaining the existing functionality that users rely on.
