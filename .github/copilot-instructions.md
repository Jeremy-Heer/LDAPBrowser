# LDAP Browser - Copilot Coding Agent Instructions

## Project Overview

**LDAP Browser** is a comprehensive Java web application for browsing, searching, and managing LDAP directories. Built with Spring Boot and Vaadin, it provides a modern tabbed interface featuring both directory management and schema exploration capabilities.

### High-Level Repository Information
- **Project Type**: Java Spring Boot web application
- **Primary Language**: Java 17+
- **Framework**: Spring Boot 3.2.0, Vaadin 24.3.0
- **LDAP SDK**: UnboundID LDAP SDK 7.0.3
- **Build Tool**: Maven
- **Repository Size**: ~47 Java source files, comprehensive feature set
- **Target Runtime**: Java 17+ with embedded Tomcat
- **Package Format**: Self-contained JAR (~51MB) with all dependencies

### Key Features
- **Dashboard Tab**: Directory tree browsing, entry management, advanced search
- **Schema Tab**: Object classes, attribute types, matching rules, syntaxes browser
- **Multi-server Support**: Configure and connect to multiple LDAP servers
- **Security**: SSL/TLS and StartTLS support
- **Entry Management**: Full CRUD operations on LDAP entries and attributes

## Build Instructions

### Prerequisites
- Java 17 or higher (Java 21+ recommended)
- Maven 3.6 or higher
- No additional runtime dependencies required

### Essential Build Commands

**ALWAYS run commands in repository root directory (`/home/runner/work/LDAPBrowser/LDAPBrowser`)**

#### 1. Clean Build Environment
```bash
mvn clean
```
- **Time**: ~7 seconds
- **Always run before starting fresh builds**

#### 2. Compile and Validate
```bash
mvn compile
```
- **Time**: ~55 seconds (first run), ~10 seconds (subsequent)
- **Downloads dependencies on first run**
- **Runs checkstyle validation automatically (758 warnings expected, build still succeeds)**

#### 3. Run Tests
```bash
mvn test
```
- **Time**: ~19 seconds
- **Runs 4 tests**: Main application context test + 3 UI component tests
- **All tests should pass**

#### 4. Code Quality Checks
```bash
mvn checkstyle:check
```
- **Time**: ~6 seconds
- **758 checkstyle violations are expected (formatting only, not functional)**
- **Build succeeds because failOnViolation=false**
- **Uses Google checkstyle rules**

#### 5. Production Build
```bash
mvn clean package -Pproduction -DskipTests
```
- **Time**: ~25 seconds
- **Creates self-contained JAR**: `target/ldap-browser-1.0-SNAPSHOT.jar` (~51MB)
- **Includes all dependencies and embedded Tomcat**
- **Ready for deployment**

#### 6. Development with Live Reload
```bash
./dev-reload.sh
```
- **Alternative**: `mvn spring-boot:run -Dspring.profiles.active=development`
- **Enables automatic restart on Java file changes**
- **Runs on http://localhost:8080**

### Critical Build Notes
- **Always run `mvn clean` before production builds**
- **Checkstyle warnings are expected and don't indicate functional issues**
- **Use `-DskipTests` for faster production builds**
- **Frontend assets are pre-compiled in `src/main/bundles/`**
- **Development profile enables Spring DevTools for live reload**

### Build Troubleshooting
- If build fails with missing dependencies: Run `mvn clean compile` first
- If Vaadin frontend compilation fails: Ensure Node.js environment is clean
- If tests fail: Check LDAP connection timeout settings in application properties
- If JAR is missing after build: Verify Maven successfully completed package phase

## Project Layout and Architecture

### Directory Structure
```
src/
├── main/
│   ├── java/com/example/ldapbrowser/
│   │   ├── LdapBrowserApplication.java     # Main Spring Boot application
│   │   ├── AppShell.java                   # Vaadin application shell
│   │   ├── model/                          # Data models
│   │   │   ├── LdapEntry.java              # LDAP entry representation
│   │   │   ├── LdapServerConfig.java       # Server configuration
│   │   │   └── SearchResultEntry.java     # Search result wrapper
│   │   ├── service/                        # Business logic layer
│   │   │   ├── LdapService.java            # Core LDAP operations
│   │   │   ├── ConfigurationService.java  # Server config management
│   │   │   ├── InMemoryLdapService.java    # In-memory LDAP for testing
│   │   │   ├── LoggingService.java         # Application logging
│   │   │   └── ServerSelectionService.java # Server selection state
│   │   ├── ui/                             # User interface layer
│   │   │   ├── MainView.java               # Main tabbed interface
│   │   │   ├── MainLayout.java             # Application layout
│   │   │   ├── components/                 # UI components
│   │   │   │   ├── DashboardTab.java       # Directory browsing tab
│   │   │   │   ├── SchemaBrowser.java      # Schema exploration tab
│   │   │   │   ├── DirectorySearchSubTab.java # Search functionality
│   │   │   │   ├── LdapTreeGrid.java       # Directory tree display
│   │   │   │   ├── SearchPanel.java        # Search form
│   │   │   │   ├── AttributeEditor.java    # Attribute editing dialog
│   │   │   │   └── ConnectionDialog.java   # Server connection dialog
│   │   │   └── [Other views for settings, server management]
│   │   └── util/                           # Utility classes
│   ├── resources/
│   │   ├── application.properties          # Base configuration
│   │   ├── application-development.properties # Dev-specific settings
│   │   ├── application-production.properties  # Production settings
│   │   └── META-INF/                       # Spring Boot metadata
│   └── bundles/                            # Pre-compiled Vaadin frontend
└── test/java/com/example/ldapbrowser/      # Test cases
    ├── LdapBrowserApplicationTests.java    # Main application test
    └── ui/components/                      # Component tests
```

### Key Configuration Files
- **`pom.xml`**: Maven configuration with Spring Boot parent, Vaadin, UnboundID SDK
- **`application*.properties`**: Runtime configuration for different environments
- **`.github/workflows/release.yml`**: GitHub Actions for automated releases
- **`dev-reload.sh`**: Development script with live reload
- **`Dockerfile`**: Container deployment configuration
- **`.gitignore`**: Excludes target/, node_modules/, IDE files, ldap-servers.json

### Architectural Patterns
- **Spring Boot**: Dependency injection, auto-configuration, embedded server
- **Vaadin Flow**: Component-based UI with server-side rendering
- **Service Layer**: Clean separation between UI and business logic
- **Configuration Management**: Externalized server configurations in JSON
- **UnboundID SDK**: Professional LDAP client library for all LDAP operations

### GitHub Actions Pipeline
- **Trigger**: Version tags (v*)
- **Build**: `mvn clean package -Pproduction`
- **Artifacts**: Creates GitHub release with JAR file
- **Requirements**: Java 17, Maven caching enabled
- **Output**: Self-contained JAR ready for deployment

### Critical Dependencies
- **Spring Boot Starter**: Core framework and auto-configuration
- **Vaadin Spring Boot Starter**: UI framework integration
- **UnboundID LDAP SDK**: LDAP client operations
- **Jackson**: JSON processing for configuration files
- **Spring Boot DevTools**: Development-time live reload
- **JUnit 5**: Testing framework

### Validation Steps for Changes
1. **Compile**: `mvn compile` - Verify syntax and dependencies
2. **Test**: `mvn test` - Ensure existing functionality works
3. **Checkstyle**: Check code style (warnings expected)
4. **Production Build**: `mvn clean package -Pproduction -DskipTests`
5. **Manual Testing**: Start application and verify key features
6. **LDAP Connectivity**: Test with real LDAP server if modifying connection logic

### Common Entry Points for Changes
- **New LDAP Operations**: Extend `LdapService.java`
- **UI Components**: Add to `ui/components/` and integrate with main tabs
- **Configuration**: Modify `application*.properties` or `LdapServerConfig.java`
- **Business Logic**: Add services to `service/` package
- **Data Models**: Add to `model/` package

### Code Quality Standards
- **Google Checkstyle**: Enforced but not failing builds
- **JavaDoc**: Required for public methods and classes
- **Import Organization**: Lexicographical order expected
- **Line Length**: 100 characters maximum
- **Error Handling**: Comprehensive exception handling for LDAP operations

### Development Environment Setup
1. Use `./dev-reload.sh` for active development
2. Configure IDE for automatic compilation
3. Browser LiveReload extension recommended for frontend changes
4. Test with both OpenLDAP and Active Directory when possible

### Performance Considerations
- **LDAP Connection Pooling**: Managed by UnboundID SDK
- **Lazy Tree Loading**: Directory entries loaded on-demand
- **Search Result Pagination**: Built-in paging for large result sets
- **Memory Management**: Monitor heap usage with large directories

---

**Trust these instructions and only search for additional information if details are incomplete or found to be incorrect. This repository has been thoroughly analyzed and all build commands have been validated.**