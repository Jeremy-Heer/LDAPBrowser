# Schema Modification Feature

This document describes the enhanced schema modification functionality that allows adding new object classes and attribute types to both in-memory and external LDAP servers.

## Overview

The LDAP Browser now supports adding custom schema elements to LDAP servers through an intuitive user interface. This feature works with both in-memory test servers and external production LDAP servers (where supported).

## Features

### 1. Add Object Class
- **Access**: Schema Browser → Object Classes tab → "Add Object Class" button
- **Supported Fields**:
  - **Name*** (required): Object class name
  - **OID*** (required): Unique object identifier
  - **Description**: Human-readable description
  - **Type**: STRUCTURAL, AUXILIARY, or ABSTRACT
  - **Obsolete**: Flag to mark as obsolete
  - **Superior Classes**: Parent object classes (one per line)
  - **Required Attributes (MUST)**: Mandatory attributes (one per line)
  - **Optional Attributes (MAY)**: Optional attributes (one per line)

### 2. Add Attribute Type
- **Access**: Schema Browser → Attribute Types tab → "Add Attribute Type" button
- **Supported Fields**:
  - **Name*** (required): Attribute type name
  - **OID*** (required): Unique object identifier
  - **Syntax OID*** (required): LDAP syntax identifier
  - **Description**: Human-readable description
  - **Superior Type**: Parent attribute type
  - **Usage**: USER_APPLICATIONS, DIRECTORY_OPERATION, etc.
  - **Flags**: Single Valued, Obsolete, Collective, No User Modification
  - **Matching Rules**: Equality, Ordering, Substring

## Server Support

### In-Memory Servers
- ✅ **Full Support**: All schema modifications are supported
- ✅ **Immediate Effect**: Changes take effect immediately
- ✅ **No Restrictions**: Can add any valid schema elements

### External LDAP Servers
- ⚠️ **Limited Support**: Depends on server implementation and permissions
- ⚠️ **Administrator Privileges**: May require special permissions
- ⚠️ **Production Impact**: Could affect live systems

## Supported External LDAP Servers

The feature automatically detects schema modification support by checking:

1. **Root DSE Features**:
   - All Operational Attributes (1.3.6.1.4.1.4203.1.5.1)
   - Sun DS Schema Modification (1.3.6.1.4.1.42.2.27.9.5.4)

2. **Schema Subentry Access**:
   - Checks if schema subentry is accessible
   - Verifies write permissions

### Known Compatible Servers
- **OpenLDAP**: Supports schema modifications via cn=schema
- **Apache Directory Server**: Full schema modification support
- **OpenDJ/ForgeRock DS**: Comprehensive schema management
- **389 Directory Server**: Schema modification capabilities

### Server-Specific Notes

#### OpenLDAP
```ldif
# Schema modifications target: cn=schema
# Requires: olcAccess permissions for schema modification
```

#### Active Directory
```
❌ Not Supported: AD uses a different schema model
Alternative: Use AD Schema Extensions tools
```

#### IBM Security Directory Server
```ldif
# Schema modifications target: cn=schema
# Requires: Administrator privileges
```

## Technical Implementation

### Schema Definition Format
The tool generates RFC-compliant LDAP schema definitions:

**Object Class Example**:
```
( 1.2.3.4.5.6.7.8 NAME 'customPerson' 
  DESC 'Custom person object class' 
  SUP person STRUCTURAL 
  MUST ( cn $ sn ) 
  MAY ( customAttribute $ description ) )
```

**Attribute Type Example**:
```
( 1.2.3.4.5.6.7.9 NAME 'customAttribute' 
  DESC 'Custom attribute for testing' 
  SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 
  SINGLE-VALUE )
```

### Modification Process

#### In-Memory Servers
1. Validate schema definition syntax
2. Add to server configuration
3. Refresh schema immediately

#### External Servers
1. Validate schema definition syntax
2. Determine schema subentry DN from Root DSE
3. Create LDAP modify operation with ADD modification type
4. Apply modification to schema subentry
5. Refresh schema cache

### Schema Subentry Detection
The system checks these Root DSE attributes in order:
1. `subschemaSubentry`
2. `schemaNamingContext`
3. `schemaSubentry`
4. Fallback: `cn=schema`

## User Interface

### Button Visibility
- Buttons appear only on Object Classes and Attribute Types tabs
- Automatically hidden for unsupported schema types
- Disabled when no server is connected or schema modification not supported

### Validation
- **Syntax Validation**: Ensures LDAP-compliant definitions
- **Duplicate Detection**: Prevents conflicts with existing schema elements
- **Required Field Validation**: Enforces mandatory fields
- **Real-time Feedback**: Immediate error reporting

### Security Warnings
External server dialogs include prominent warnings:
> ⚠️ Adding schema elements to external LDAP servers may require administrator privileges and could affect production systems.

## Error Handling

### Common Errors
1. **Insufficient Privileges**: User lacks schema modification rights
2. **Duplicate OID/Name**: Schema element already exists
3. **Invalid Syntax**: Malformed schema definition
4. **Server Not Supported**: Server doesn't allow schema modifications
5. **Connection Issues**: Network or authentication problems

### Error Messages
- Clear, actionable error descriptions
- Includes server-specific guidance where possible
- Suggests alternative approaches for unsupported servers

## Best Practices

### Development/Testing
1. **Use In-Memory Servers**: For development and testing
2. **Test First**: Validate schema elements before production
3. **Backup Schemas**: Keep copies of original schemas

### Production Usage
1. **Plan Carefully**: Schema changes can affect applications
2. **Test in Staging**: Validate in non-production environment
3. **Monitor Impact**: Watch for application compatibility issues
4. **Documentation**: Document all custom schema additions

### OID Management
1. **Use Enterprise OIDs**: Obtain proper OID arc for organization
2. **Avoid Conflicts**: Don't reuse existing OIDs
3. **Documentation**: Maintain OID registry

## Troubleshooting

### Schema Not Loading
1. Check server connection
2. Verify user permissions
3. Check LDAP server logs

### Modifications Failing
1. Verify administrator privileges
2. Check schema subentry permissions
3. Validate syntax with LDAP tools
4. Review server-specific requirements

### Button Not Appearing
1. Ensure schema is loaded
2. Check server compatibility
3. Verify tab selection (Object Classes/Attribute Types only)

## Future Enhancements

### Planned Features
- [ ] Schema element editing/modification
- [ ] Schema element deletion
- [ ] Import/export schema definitions
- [ ] Schema validation tools
- [ ] Bulk schema operations

### Advanced Features
- [ ] Schema dependency analysis
- [ ] Schema migration tools
- [ ] Multi-server schema synchronization
- [ ] Schema versioning

## API Reference

### LdapService Methods
```java
// Add object class to external server
void addObjectClassToSchema(String serverId, String objectClassDefinition)

// Add attribute type to external server  
void addAttributeTypeToSchema(String serverId, String attributeTypeDefinition)

// Check schema modification support
boolean supportsSchemaModification(String serverId)

// Get schema subentry DN
String getSchemaSubentryDN(String serverId)
```

### InMemoryLdapService Methods
```java
// Add object class to in-memory server
void addObjectClassToSchema(String serverId, String objectClassDefinition)

// Add attribute type to in-memory server
void addAttributeTypeToSchema(String serverId, String attributeTypeDefinition)

// Check if server is in-memory
boolean isInMemoryServer(String serverId)
```

## Conclusion

The enhanced schema modification feature provides a powerful and user-friendly way to extend LDAP schemas for both development and production use cases. The implementation follows LDAP standards and includes appropriate safeguards for production environments.
