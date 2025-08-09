# LDAP Browser

A comprehensive LDAP Browser application built with Vaadin and the UnboundID LDAP SDK. This application provides a user-friendly interface for browsing, searching, and managing LDAP directories with an integrated schema browser.

## Features

### Dashboard
- **Multiple LDAP Server Support**: Configure and connect to multiple LDAP servers
- **Tree-based Directory Browsing**: Navigate LDAP directory structure with expandable tree view
- **Advanced Search**: Search LDAP entries with customizable filters and scopes
- **Entry Management**: View, edit, add, and delete LDAP entries
- **Attribute Editing**: Full CRUD operations on LDAP attributes
- **Security Options**: Support for SSL/TLS and StartTLS connections
- **Connection Management**: Save and manage multiple server configurations

### Schema Browser
- **Object Classes**: Browse and explore all object classes with details on types, attributes, and inheritance
- **Attribute Types**: View attribute definitions including syntax, matching rules, and usage restrictions
- **Matching Rules**: Explore comparison rules used for attribute matching and sorting
- **Matching Rule Use**: See how matching rules are applied to specific attribute types
- **Syntaxes**: Browse all supported attribute syntaxes and their definitions
- **Real-time Search**: Filter schema elements by name, OID, or description
- **Detailed Views**: Click any schema element to view comprehensive technical details

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2.0**
- **Vaadin 24.3.0** - Modern web UI framework
- **UnboundID LDAP SDK 6.0.11** - Java LDAP client library
- **Maven** - Build and dependency management

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Access to an LDAP server (OpenLDAP, Active Directory, Apache Directory Server, etc.)

### Building and Running

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd ldap-browser
   ```

2. **Build the application:**
   ```bash
   mvn clean compile
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application:**
   Open your web browser and navigate to `http://localhost:8080`

### Production Build

To create a production-ready build:

```bash
mvn clean package -Pproduction
java -jar target/ldap-browser-1.0-SNAPSHOT.jar
```

## Usage Guide

### Application Interface

The LDAP Browser features a modern tabbed interface with two main sections:

#### Dashboard Tab
Contains the original LDAP directory browsing and management functionality:
- LDAP tree browser for hierarchical navigation
- Entry details panel for viewing and editing attributes  
- Search panel for advanced LDAP queries
- Search results with configurable return attributes

#### Schema Tab  
Provides comprehensive LDAP schema exploration:
- **Object Classes**: View structural, auxiliary, and abstract classes
- **Attribute Types**: Explore attribute definitions and constraints
- **Matching Rules**: Browse comparison and sorting rules
- **Matching Rule Use**: See rule applications to attributes
- **Syntaxes**: Review all supported data syntaxes
- **Search & Filter**: Real-time filtering across all schema elements

### 1. Server Configuration

1. Click the **"Manage Servers"** button to open the server management dialog
2. Click **"Add"** to create a new server configuration
3. Fill in the server details:
   - **Name**: A friendly name for the server
   - **Host**: LDAP server hostname or IP address
   - **Port**: LDAP server port (usually 389 for LDAP, 636 for LDAPS)
   - **Bind DN**: Distinguished name for authentication (leave empty for anonymous)
   - **Password**: Password for the bind DN
   - **Use SSL**: Enable for LDAPS connections
   - **Use StartTLS**: Enable for StartTLS encryption
   - **Base DN**: Default base DN for browsing and searching

4. Click **"Save"** to store the configuration

### 2. Connecting to LDAP Server

1. Select a server from the dropdown list
2. Click **"Connect"** to establish connection
3. The application will attempt to connect and authenticate
4. Once connected, both Dashboard and Schema tabs will be populated
5. The connection status is shown in the top-right corner

### 3. Using the Dashboard Tab

#### Browsing Directory Structure
1. Ensure you're on the **Dashboard** tab
2. The directory tree will automatically load Root DSE and naming contexts
3. Expand tree nodes by clicking the arrow icons
4. Children are loaded lazily when nodes are expanded
5. Click on any entry to view its attributes in the right panel

#### Searching LDAP Entries

1. In the search panel, enter:
   - **Search Base DN**: Starting point for the search
   - **LDAP Filter**: Search filter (e.g., `(objectClass=person)`)
   - **Return Attributes**: Comma-separated list of attributes to return (optional)
   - **Search Scope**: Choose from Base, One Level, or Subtree
2. Click **"Search"** to execute the search
3. Results will be displayed in the search results panel with:
   - **DN** as the first column
   - **Requested attributes** as subsequent columns (if specified)
   - **Default view** (Name, Object Class) if no attributes specified
4. Click **"Clear"** to clear search results

#### Return Attributes Examples
- Leave empty: Returns all attributes
- `cn,mail,telephoneNumber`: Returns only these specific attributes
- `cn,mail,+`: Returns specified attributes plus operational attributes
- `*,createTimestamp`: Returns all user attributes plus specific operational attribute

### 4. Using the Schema Tab

#### Browsing Schema Elements
1. Switch to the **Schema** tab
2. Use the schema type tabs to browse different elements:
   - **Object Classes**: Structural definitions for LDAP entries
   - **Attribute Types**: Definitions for all available attributes
   - **Matching Rules**: Rules for attribute comparison and sorting
   - **Matching Rule Use**: Applications of matching rules to attributes
   - **Syntaxes**: Data type definitions and formats

#### Search and Filter Schema
1. Use the search field at the top to filter schema elements
2. Search works across names, OIDs, and descriptions
3. Results update in real-time as you type
4. Click any element to view detailed information in the right panel

#### Schema Details
Each schema element provides comprehensive information:
- **Object Classes**: Names, OIDs, types, superior classes, required/optional attributes, extensions
- **Attribute Types**: Syntax OIDs, usage types, matching rules, single-value restrictions
- **Matching Rules**: Associated syntaxes and descriptions
- **Matching Rule Use**: Applicable attribute types
- **Syntaxes**: OIDs and detailed descriptions

### 5. Managing LDAP Entries (Dashboard Tab)

#### Viewing Entry Details
- Select any entry in the tree to view its attributes
- All attribute names and values are displayed in the right panel

#### Editing Attributes
1. Click the **edit icon** next to any attribute
2. Modify values in the dialog (one value per line)
3. Click **"Save"** in the dialog
4. Click **"Save Changes"** in the main panel to commit to LDAP

#### Adding New Attributes
1. Click **"Add Attribute"** button
2. Enter the attribute name and values
3. Click **"Add"** in the dialog
4. Click **"Save Changes"** to commit to LDAP

#### Deleting Attributes
1. Click the **delete icon** next to any attribute
2. Confirm the deletion
3. Click **"Save Changes"** to commit to LDAP

#### Deleting Entries
1. Select an entry and click **"Delete Entry"**
2. Confirm the deletion in the dialog
3. The entry will be permanently removed from LDAP

## Configuration Examples

### OpenLDAP Configuration
- **Host**: localhost
- **Port**: 389
- **Bind DN**: cn=admin,dc=example,dc=com
- **Base DN**: dc=example,dc=com

### Active Directory Configuration
- **Host**: domain.company.com
- **Port**: 389
- **Bind DN**: user@domain.company.com (or CN=User,CN=Users,DC=domain,DC=company,DC=com)
- **Base DN**: DC=domain,DC=company,DC=com

### Apache Directory Server Configuration
- **Host**: localhost
- **Port**: 10389
- **Bind DN**: uid=admin,ou=system
- **Base DN**: ou=system

## Security Features

- **SSL/TLS Support**: Full support for encrypted connections
- **Trust All Certificates**: For development/testing environments
- **StartTLS**: Upgrade plain connections to encrypted
- **Authentication**: Support for simple bind authentication
- **Connection Pooling**: Efficient connection management

## Troubleshooting

### Connection Issues
1. Verify server hostname and port
2. Check firewall settings
3. Ensure LDAP service is running
4. Verify credentials (bind DN and password)
5. Check SSL/TLS settings

### Search Issues
1. Verify base DN exists
2. Check LDAP filter syntax
3. Ensure proper search scope
4. Verify search permissions

### Performance Issues
1. Use more specific search filters
2. Limit search scope when possible
3. Use appropriate base DNs for searches
4. Consider LDAP server indexing

## Development

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/example/ldapbrowser/
│   │       ├── LdapBrowserApplication.java     # Main application class
│   │       ├── model/                          # Data models
│   │       │   ├── LdapEntry.java
│   │       │   └── LdapServerConfig.java
│   │       ├── service/                        # Business logic
│   │       │   ├── LdapService.java
│   │       │   └── ConfigurationService.java
│   │       └── ui/                            # User interface
│   │           ├── MainView.java              # Main tabbed interface
│   │           └── components/
│   │               ├── DashboardTab.java      # Dashboard functionality
│   │               ├── SchemaBrowser.java     # Schema exploration
│   │               ├── LdapTreeGrid.java
│   │               ├── SearchPanel.java
│   │               ├── AttributeEditor.java
│   │               └── ConnectionDialog.java
│   └── resources/
│       └── application.properties              # Application configuration
│   └── frontend/
│       └── themes/
│           └── ldap-browser/
│               └── styles.css                  # Professional styling
```

### Adding Features
1. **New LDAP Operations**: Extend `LdapService.java`
2. **Dashboard Components**: Add to `ui/components/` package and integrate with `DashboardTab.java`
3. **Schema Functionality**: Extend `SchemaBrowser.java` for additional schema exploration
4. **UI Components**: Add to `ui/components/` package
5. **Data Models**: Add to `model/` package
6. **Configuration**: Modify `application.properties`

### New in This Version
- **Tabbed Interface**: Clean separation between directory browsing and schema exploration
- **Comprehensive Schema Browser**: Full access to LDAP schema definitions
- **Enhanced User Experience**: Professional styling inspired by Apache Directory Studio
- **Real-time Schema Search**: Instant filtering across all schema element types
- **Detailed Schema Views**: Complete technical information for each schema element

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review LDAP server documentation
3. Check UnboundID LDAP SDK documentation
4. Submit issues on the project repository
