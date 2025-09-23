package com.ldapweb.ldapbrowser.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive OID lookup table providing descriptions for LDAP OIDs
 * organized by category. Based on the LDAP OID Reference Guide from ldap.com.
 * 
 * This utility can be used throughout the application to provide human-readable
 * descriptions for LDAP controls, extended operations, attribute types,
 * object classes, and other OID-identified elements.
 */
public final class OidLookupTable {

    // =================================================
    // LDAP CONTROLS
    // =================================================
    
    private static final Map<String, String> CONTROL_DESCRIPTIONS = new HashMap<>();
    static {
        // Standard LDAP Controls
        CONTROL_DESCRIPTIONS.put("1.2.826.0.1.3344810.2.3", "Matched Values Request Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113556.1.4.319", "Simple Paged Results Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113556.1.4.473", "Server-Side Sort Request Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113556.1.4.474", "Server-Side Sort Response Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113556.1.4.805", "Tree Delete Request Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113556.1.4.841", "DirSync Request Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113556.1.4.1413", "Permissive Modify Request Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113556.1.4.1504", "Attribute Scoped Query Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.7.1", "LCUP Sync Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.7.2", "LCUP Sync Update Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.7.3", "LCUP Sync Done Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.12", "LDAP Assertion Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.13.1", "LDAP Pre-Read Request and Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.13.2", "LDAP Post-Read Request and Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.21.2", "Transaction Specification Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.1.22", "LDAP Don't Use Copy Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113549.6.0.0", "Signed Operation Request Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113549.6.0.1", "Demand Signed Result Request Control");
        CONTROL_DESCRIPTIONS.put("1.2.840.113549.6.0.2", "Signed Result Response Control");
        
        // Sun/Oracle Directory Server Controls
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.8.5.1", "Password Policy Request and Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.9.5.2", "Get Effective Rights Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.9.5.8", "Account Usable Request and Response Control");
        
        // OpenLDAP Controls
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.9.1.1", "LDAP Content Synchronization Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.9.1.2", "LDAP Content Synchronization State Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.9.1.3", "LDAP Content Synchronization Done Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.10.1", "LDAP Subentries Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.10.2", "LDAP No-Op Request Control");
        
        // Netscape/iPlanet Controls
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.2", "ManageDsaIT Request Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.3", "Persistent Search Request Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.4", "Password Expired Response Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.5", "Password Expiring Response Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.7", "Entry Change Notification Response Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.9", "Virtual List View Request Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.10", "Virtual List View Response Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.12", "Proxied Authorization (v1) Request Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.15", "Authorization Identity Response Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.16", "Authorization Identity Request Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.17", "Real Attributes Only Request Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.18", "Proxied Authorization (v2) Request Control");
        CONTROL_DESCRIPTIONS.put("2.16.840.1.113730.3.4.19", "Virtual Attributes Only Request Control");
        
        // Draft/Experimental Controls
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.1466.29539.10", "LDAP Triggered Search Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.1466.29539.13", "LDAP Triggered Search Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.7628.5.101.1", "LDAP Subentries Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.21008.108.63.1", "LDAP Session Tracking Control");
        
        // Ping Identity Directory Server Controls
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.5.2", "Replication Repair Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.1", "Batched Transaction Specification Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.2", "Intermediate Client Request and Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.3", "Retain Identity Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.4", "Interactive Transaction Specification Request and Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.5", "Ignore NO-USER-MODIFICATION Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.6", "Get Authorization Entry Request and Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.9", "Join Request and Result Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.12", "Extended Schema Info Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.28", "Assured Replication Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.29", "Assured Replication Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.36", "Matching Entry Count Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.37", "Matching Entry Count Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.52", "Uniqueness Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.53", "Uniqueness Response Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.64", "JSON-formatted Request Control");
        CONTROL_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.5.65", "JSON-formatted Response Control");
    }
    
    // =================================================
    // EXTENDED OPERATIONS
    // =================================================
    
    private static final Map<String, String> EXTENDED_OPERATION_DESCRIPTIONS = new HashMap<>();
    static {
        // Standard Extended Operations
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.8", "Cancel Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.17.1", "Start LBURP Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.17.2", "Start LBURP Extended Response");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.17.3", "End LBURP Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.17.4", "End LBURP Extended Response");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.17.5", "LBURP Update Extended Response");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.17.6", "LBURP Update Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.19", "LDAP Turn Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.21.1", "Start Transaction Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.1.21.3", "End Transaction Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.1466.20037", "StartTLS Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.11.1", "Password Modify Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.11.3", "\"Who Am I?\" Extended Request");
        
        // Ping Identity Directory Server Extended Operations
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.6.1", "Password Policy State Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.6.2", "Get Connection ID Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.6.20", "Get Subtree Accessibility Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.6.21", "Get Subtree Accessibility Extended Response");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.1", "Start Batched Transaction Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.2", "End Batched Transaction Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.3", "Start Interactive Transaction Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.4", "End Interactive Transaction Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.15", "Validate TOTP Password Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.16", "Validate TOTP Password Extended Response");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.62", "Generate Password Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.63", "Generate Password Extended Response");
        
        // Draft Extended Operations
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.5515.3.1", "Copy Subtree Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.5515.3.2", "Copy Subtree Extended Response");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.5515.3.3", "Delete Subtree Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.5515.3.4", "Delete Subtree Extended Response");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.5515.3.5", "Update Subtree Extended Request");
        EXTENDED_OPERATION_DESCRIPTIONS.put("1.3.6.1.4.1.5515.3.6", "Update Subtree Extended Response");
    }
    
    // =================================================
    // STANDARD ATTRIBUTE TYPES
    // =================================================
    
    private static final Map<String, String> ATTRIBUTE_TYPE_DESCRIPTIONS = new HashMap<>();
    static {
        // X.500 Standard Attribute Types (2.5.4.*)
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.0", "objectClass Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.1", "aliasedObjectName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.2", "knowledgeInformation Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.3", "cn (Common Name) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.4", "sn (Surname) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.5", "serialNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.6", "c (Country) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.7", "l (Locality) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.8", "st (State or Province) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.9", "street Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.10", "o (Organization) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.11", "ou (Organizational Unit) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.12", "title Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.13", "description Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.14", "searchGuide Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.15", "businessCategory Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.16", "postalAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.17", "postalCode Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.18", "postOfficeBox Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.19", "physicalDeliveryOfficeName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.20", "telephoneNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.21", "telexNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.22", "teletexTerminalIdentifier Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.23", "facsimileTelephoneNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.24", "x121Address Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.25", "internationaliSDNNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.26", "registeredAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.27", "destinationIndicator Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.28", "preferredDeliveryMethod Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.29", "presentationAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.30", "supportedApplicationContext Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.31", "member Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.32", "owner Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.33", "roleOccupant Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.34", "seeAlso Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.35", "userPassword Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.36", "userCertificate Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.37", "cACertificate Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.38", "authorityRevocationList Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.39", "certificateRevocationList Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.40", "crossCertificatePair Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.41", "name Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.42", "givenName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.43", "initials Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.44", "generationQualifier Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.45", "x500UniqueIdentifier Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.46", "dnQualifier Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.47", "enhancedSearchGuide Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.48", "protocolInformation Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.49", "distinguishedName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.50", "uniqueMember Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.51", "houseIdentifier Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.52", "supportedAlgorithms Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.53", "deltaRevocationList Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.54", "dmdName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.4.65", "pseudonym Attribute Type");
        
        // RFC 1274 Attribute Types (0.9.2342.19200300.100.1.*)
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.1", "uid Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.2", "textEncodedORAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.3", "mail Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.4", "info Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.5", "drink Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.6", "roomNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.7", "photo Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.8", "userClass Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.9", "host Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.10", "manager Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.11", "documentIdentifier Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.12", "documentTitle Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.13", "documentVersion Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.14", "documentAuthor Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.15", "documentLocation Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.20", "homePhone Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.21", "secretary Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.22", "otherMailbox Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.23", "lastModifiedTime Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.24", "lastModifiedBy Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.25", "dc (Domain Component) Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.26", "aRecord Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.27", "mDRecord Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.28", "mxRecord Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.29", "nSRecord Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.30", "sOARecord Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.31", "cNAMERecord Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.37", "associatedDomain Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.38", "associatedName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.39", "homePostalAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.40", "personalTitle Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.41", "mobile Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.42", "pager Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.43", "co Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.44", "uniqueIdentifier Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.45", "organizationalStatus Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.46", "janetMailbox Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.47", "mailPreferenceOption Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.48", "buildingName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.49", "dSAQuality Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.50", "singleLevelQuality Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.51", "subtreeMinimumQuality Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.52", "subtreeMaximumQuality Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.53", "personalSignature Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.54", "dITRedirect Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.55", "audio Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.56", "documentPublisher Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.60", "jpegPhoto Attribute Type");
        
        // RFC 2307 (NIS Schema) Attribute Types
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.0", "uidNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.1", "gidNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.2", "gecos Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.3", "homeDirectory Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.4", "loginShell Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.5", "shadowLastChange Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.6", "shadowMin Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.7", "shadowMax Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.8", "shadowWarning Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.9", "shadowInactive Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.10", "shadowExpire Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.11", "shadowFlag Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.12", "memberUid Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.13", "memberNisNetgroup Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.14", "nisNetgroupTriple Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.15", "ipServicePort Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.16", "ipServiceProtocol Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.17", "ipProtocolNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.18", "oncRpcNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.19", "ipHostNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.20", "ipNetworkNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.21", "ipNetmaskNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.22", "macAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.23", "bootParameter Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.24", "bootFile Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.26", "nisMapName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.27", "nisMapEntry Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.28", "nisPublicKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.29", "nisSecretKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.30", "nisDomain Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.31", "automountMapName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.32", "automountKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.1.1.33", "automountInformation Attribute Type");
        
        // RFC 2798 (inetOrgPerson) Attribute Types
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.1", "carLicense Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.2", "departmentNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.3", "employeeNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.4", "employeeType Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.39", "preferredLanguage Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.216", "userPKCS12 Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.241", "displayName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("0.9.2342.19200300.100.1.60", "jpegPhoto Attribute Type");
        
        // LDAP Operational Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.18.1", "createTimestamp Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.18.2", "modifyTimestamp Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.18.3", "creatorsName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.18.4", "modifiersName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.18.9", "hasSubordinates Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.18.10", "subschemaSubentry Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.16.4", "entryUUID Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.20", "entryDN Attribute Type");
        
        // Schema Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.1", "dITStructureRules Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.2", "dITContentRules Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.4", "matchingRules Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.5", "attributeTypes Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.6", "objectClasses Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.7", "nameForms Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.8", "matchingRuleUse Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.9", "structuralObjectClass Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.5.21.10", "governingStructureRule Attribute Type");
        
        // Root DSE Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.1", "administratorsAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.5", "namingContexts Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.6", "altServer Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.7", "supportedExtension Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.13", "supportedControl Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.14", "supportedSASLMechanisms Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.15", "supportedLDAPVersion Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.16", "ldapSyntaxes Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.3.5", "supportedFeatures Attribute Type");
        
        // Additional General Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.4", "vendorName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.5", "vendorVersion Attribute Type");
        
        // PKCS#9 Attribute Types
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.1", "emailAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.2", "unstructuredName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.3", "contentType Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.4", "messageDigest Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.5", "signingTime Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.6", "counterSignature Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.7", "challengePassword Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.8", "unstructuredAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.25.1", "pKCS15Token Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.25.2", "encryptedPrivateKeyInfo Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.1.9.25.5", "pKCS7PDU Attribute Type");
        
        // RFC 2649 Signed Directory Operations
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.6.2.0", "changes Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.6.2.1", "originalObject Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113549.6.2.2", "signedDirectoryOperationSupport Attribute Type");
        
        // Microsoft Active Directory Calendar Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.478", "calCalURI Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.479", "calFBURL Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.480", "calCAPURI Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.481", "calCalAdrURI Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.482", "calOtherCalURIs Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.483", "calOtherFBURLs Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.484", "calOtherCAPURIs Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.2.840.113556.1.4.485", "calOtherCalAdrURIs Attribute Type");
        
        // UDDI Attributes (extensive set)
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.1", "uddiBusinessKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.2", "uddiAuthorizedName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.3", "uddiOperator Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.4", "uddiName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.5", "uddiDescription Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.6", "uddiDiscoveryURLs Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.7", "uddiUseType Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.8", "uddiPersonName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.9", "uddiPhone Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.10", "uddiEMail Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.11", "uddiSortCode Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.12", "uddiTModelKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.13", "uddiAddressLine Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.14", "uddiIdentifierBag Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.15", "uddiCategoryBag Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.16", "uddiKeyedReference Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.17", "uddiServiceKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.18", "uddiBindingKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.19", "uddiAccessPoint Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.20", "uddiHostingRedirector Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.21", "uddiInstanceDescription Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.22", "uddiInstanceParms Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.23", "uddiOverviewDescription Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.24", "uddiOverviewURL Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.25", "uddiFromKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.26", "uddiToKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.27", "uddiUUID Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.28", "uddiIsHidden Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.29", "uddiIsProjection Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.30", "uddiLang Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.31", "uddiv3BusinessKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.32", "uddiv3ServiceKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.33", "uddiv3BindingKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.34", "uddiv3TModelKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.35", "uddiv3DigitalSignature Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.36", "uddiv3NodeId Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.37", "uddiv3EntityModificationTime Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.38", "uddiv3SubscriptionKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.39", "uddiv3SubscriptionFilter Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.40", "uddiv3NotificationInterval Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.41", "uddiv3MaxEntities Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.42", "uddiv3ExpiresAfter Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.43", "uddiv3BriefResponse Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.44", "uddiv3EntityKey Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.45", "uddiv3EntityCreationTime Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.1.10.4.46", "uddiv3EntityDeletionTime Attribute Type");
        
        // Java/CORBA Naming Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.6", "javaClassName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.7", "javaCodebase Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.8", "javaSerializedData Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.10", "javaFactory Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.11", "javaReferenceAddress Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.12", "javaDoc Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.13", "javaClassNames Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.14", "corbaIor Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.1.15", "corbaRepositoryId Attribute Type");
        
        // Personal Identity Attributes (RFC 2985)
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.5.5.7.9.1", "dateOfBirth Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.5.5.7.9.2", "placeOfBirth Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.5.5.7.9.3", "gender Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.5.5.7.9.4", "countryOfCitizenship Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.5.5.7.9.5", "countryOfResidence Attribute Type");
        
        // Printer Attributes (RFC 3712) 
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1107", "printer-xri-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1108", "printer-aliases Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1109", "printer-charset-configured Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1110", "printer-job-priority-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1111", "printer-job-k-octets-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1112", "printer-current-operator Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1113", "printer-service-person Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1114", "printer-delivery-orientation-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1115", "printer-stacking-order-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1116", "printer-output-features-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1117", "printer-media-local-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1118", "printer-copies-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1119", "printer-natural-language-configured Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1120", "printer-print-quality-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1121", "printer-resolution-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1122", "printer-media-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1123", "printer-sides-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1124", "printer-number-up-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1125", "printer-finishings-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1126", "printer-pages-per-minute-color Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1127", "printer-pages-per-minute Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1128", "printer-compression-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1129", "printer-color-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1130", "printer-document-format-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1131", "printer-charset-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1132", "printer-multiple-document-jobs-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1133", "printer-ipp-versions-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1134", "printer-more-info Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1135", "printer-name Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1136", "printer-location Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1137", "printer-generated-natural-language-supported Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1138", "printer-make-and-model Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1139", "printer-info Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.18.0.2.4.1140", "printer-uri Attribute Type");
        
        // Miscellaneous Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.250.1.41", "labeledURL Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.250.1.57", "labeledURI Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.453.16.2.103", "numSubordinates Attribute Type");
        
        // Authorization/Authentication Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.3.3", "supportedAuthPasswordSchemes Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.3.4", "authPassword Attribute Type");
        
        // SLP Service Directory Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.1.1", "template-major-version-number Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.1.2", "template-minor-version-number Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.1.3", "template-url-syntax Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.1.4", "service-advert-service-type Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.1.5", "service-advert-scopes Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.1.6", "service-advert-url-authenticator Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.1.7", "service-advert-attribute-authenticator Attribute Type");
        
        // Subentry Attributes  
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.7628.5.4.1", "inheritable Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("1.3.6.1.4.1.7628.5.4.2", "blockInheritance Attribute Type");
        
        // Netscape/iPlanet Directory Server Attributes
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.1", "carLicense Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.2", "departmentNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.3", "employeeNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.4", "employeeType Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.5", "changeNumber Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.6", "targetDN Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.7", "changeType Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.8", "changes Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.9", "newRDN Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.10", "deleteOldRDN Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.11", "newSuperior Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.34", "ref Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.35", "changelog Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.39", "preferredLanguage Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.40", "userSMIMECertificate Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.55", "aci Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.198", "memberURL Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.216", "userPKCS12 Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.241", "displayName Attribute Type");
        ATTRIBUTE_TYPE_DESCRIPTIONS.put("2.16.840.1.113730.3.1.542", "nsUniqueId Attribute Type");
    }
    
    // =================================================
    // OBJECT CLASSES
    // =================================================
    
    private static final Map<String, String> OBJECT_CLASS_DESCRIPTIONS = new HashMap<>();
    static {
        // RFC 1274 Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.3", "pilotObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.4", "pilotPerson Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.5", "account Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.6", "document Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.7", "room Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.9", "documentSeries Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.13", "domain Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.14", "rFC822localPart Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.15", "dNSDomain Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.17", "domainRelatedObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.18", "friendlyCountry Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.19", "simpleSecurityObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.20", "pilotOrganization Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.21", "pilotDSA Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("0.9.2342.19200300.100.4.22", "qualityLabelledData Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.1466.344", "dcObject Object Class");
        
        // RFC 2307 (NIS Schema) Object Classes  
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.0", "posixAccount Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.1", "shadowAccount Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.2", "posixGroup Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.3", "ipService Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.4", "ipProtocol Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.5", "oncRpc Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.6", "ipHost Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.7", "ipNetwork Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.8", "nisNetgroup Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.9", "nisMap Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.10", "nisObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.11", "ieee802Device Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.12", "bootableDevice Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.14", "nisKeyObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.15", "nisDomainObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.16", "automountMap Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.1.2.17", "automount Object Class");
        
        // UDDI Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.1", "uddiBusinessEntity Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.2", "uddiContact Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.3", "uddiAddress Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.4", "uddiBusinessService Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.5", "uddiBindingTemplate Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.6", "uddiTModelInstanceInfo Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.7", "uddiTModel Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.8", "uddiPublisherAssertion Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.9", "uddiv3Subscription Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.10.6.10", "uddiv3EntityObituary Object Class");
        
        // PKCS#9 Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.2.840.113549.1.9.24.1", "pkcsEntity Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.2.840.113549.1.9.24.2", "naturalPerson Object Class");
        
        // RFC 2649 Signed Directory Operations Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.2.840.113549.6.1.0", "signedAuditTrail Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.2.840.113549.6.1.2", "zombieObject Object Class");
        
        // Calendar Object Class
        OBJECT_CLASS_DESCRIPTIONS.put("1.2.840.113556.1.5.87", "calEntry Object Class");
        
        // Java/CORBA Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.1", "javaContainer Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.4", "javaObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.5", "javaSerializedObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.7", "javaNamingReference Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.8", "javaMarshalledObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.9", "corbaObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.10", "corbaContainer Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.4.2.11", "corbaObjectReference Object Class");
        
        // Password Policy Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.42.2.27.8.2.1", "pwdPolicy Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.4.7", "authPasswordObject Object Class");
        
        // Miscellaneous Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.250.3.15", "labeledURIObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.1.3.1", "uidObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.1466.101.120.111", "extensibleObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.5322.13.1.1", "namedObject Object Class");
        
        // SLP Service Directory Object Class
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.6252.2.27.6.2.1", "slpService Object Class");
        
        // Subentry Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.7628.5.6.1.1", "inheritableLDAPSubEntry Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.16.840.1.113719.2.142.6.1.1", "ldapSubEntry Object Class");
        
        // Ping Identity Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.2.900", "untypedObject Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.2.645", "ubidPerson Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.2.646", "ubidPersonAux Object Class");
        
        // Printer Object Classes (RFC 3712)
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.18.0.2.6.253", "printerLPR Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.18.0.2.6.254", "slpServicePrinter Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.18.0.2.6.255", "printerService Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.18.0.2.6.256", "printerIPP Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.18.0.2.6.257", "printerServiceAuxClass Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("1.3.18.0.2.6.258", "printerAbstract Object Class");
        
        // Standard Object Classes (Extended)
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.0", "top Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.1", "alias Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.2", "country Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.3", "locality Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.4", "organization Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.5", "organizationalUnit Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.6", "person Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.7", "organizationalPerson Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.8", "organizationalRole Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.9", "groupOfNames Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.10", "residentialPerson Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.11", "applicationProcess Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.12", "applicationEntity Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.13", "dSA Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.14", "device Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.15", "strongAuthenticationUser Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.16", "certificationAuthority Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.16.2", "certificationAuthority-V2 Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.17", "groupOfUniqueNames Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.18", "userSecurityInformation Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.19", "cRLDistributionPoint Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.20", "dmd Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.21", "pkiUser Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.22", "pkiCA Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.6.23", "deltaCRL Object Class");
        
        // LDAP Schema Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("2.5.20.1", "subschema Object Class");
        
        // RFC 2798 Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("2.16.840.1.113730.3.2.2", "inetOrgPerson Object Class");
        
        // Netscape/iPlanet Object Classes
        OBJECT_CLASS_DESCRIPTIONS.put("2.16.840.1.113730.3.2.1", "changeLogEntry Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.16.840.1.113730.3.2.6", "referral Object Class");
        OBJECT_CLASS_DESCRIPTIONS.put("2.16.840.1.113730.3.2.33", "groupOfURLs Object Class");
        
        // Dynamic Groups and Other Extensions
        OBJECT_CLASS_DESCRIPTIONS.put("1.2.826.0.1.3458854.2.1.1", "groupOfEntries Object Class");
    }
    
    // =================================================
    // MATCHING RULES
    // =================================================
    
    private static final Map<String, String> MATCHING_RULE_DESCRIPTIONS = new HashMap<>();
    static {
        // Standard Matching Rules
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.0", "objectIdentifierMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.1", "distinguishedNameMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.2", "caseIgnoreMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.3", "caseIgnoreOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.4", "caseIgnoreSubstringsMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.5", "caseExactMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.6", "caseExactOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.7", "caseExactSubstringsMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.8", "numericStringMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.9", "numericStringOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.10", "numericStringSubstringsMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.11", "caseIgnoreListMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.12", "caseIgnoreListSubstringsMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.13", "booleanMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.14", "integerMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.15", "integerOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.16", "bitStringMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.17", "octetStringMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.18", "octetStringOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.19", "octetStringSubstringsMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.20", "telephoneNumberMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.21", "telephoneNumberSubstringsMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.23", "uniqueMemberMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.27", "generalizedTimeMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.28", "generalizedTimeOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.29", "integerFirstComponentMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.30", "objectIdentifierFirstComponentMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.31", "directoryStringFirstComponentMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.32", "wordMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("2.5.13.33", "keywordMatch Matching Rule");
        
        // RFC 4517 Matching Rules
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.109.114.1", "caseExactIA5Match Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.109.114.2", "caseIgnoreIA5Match Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.109.114.3", "caseIgnoreIA5SubstringsMatch Matching Rule");
        
        // Ping Identity Matching Rules
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.1", "compactTimestampMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.2", "compactTimestampOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.3", "ldapURLMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.4", "hexStringMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.5", "hexStringOrderingMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.6", "valueCountEquals Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.7", "valueCountDoesNotEqual Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.8", "valueCountGreaterThan Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.9", "valueCountGreaterThanOrEqualTo Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.10", "valueCountLessThan Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.11", "valueCountLessThanOrEqualTo Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.12", "jsonObjectExactMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.13", "jsonObjectFilterExtensibleMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.14", "relativeTimeExtensibleMatch Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.15", "jsonObjectCaseSensitiveNamesCaseSensitiveValues Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.16", "jsonObjectCaseInsensitiveNamesCaseSensitiveValues Matching Rule");
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.4.17", "jsonObjectCaseInsensitiveNamesCaseInsensitiveValues Matching Rule");
        
        // Draft Matching Rules
        MATCHING_RULE_DESCRIPTIONS.put("1.3.6.1.4.1.1466.29539.10.1", "dnSubordinateTo Matching Rule");
    }
    
    // =================================================
    // LDAP SYNTAXES
    // =================================================
    
    private static final Map<String, String> SYNTAX_DESCRIPTIONS = new HashMap<>();
    static {
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.3", "Attribute Type Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.6", "Bit String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.7", "Boolean Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.11", "Country String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.12", "DN Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.15", "Directory String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.24", "Generalized Time Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.26", "IA5 String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.27", "INTEGER Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.28", "JPEG Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.36", "Numeric String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.37", "Object Class Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.38", "OID Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.40", "Octet String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.41", "Postal Address Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.44", "Printable String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.50", "Telephone Number Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.53", "UTC Time Syntax");
        
        // Additional RFC 4517 Syntaxes
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.4", "Audio Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.5", "Binary Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.8", "Certificate Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.9", "Certificate List Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.10", "Certificate Pair Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.13", "Data Quality Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.14", "Delivery Method Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.16", "DIT Content Rule Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.17", "DIT Structure Rule Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.18", "DL Submit Permission Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.19", "DSA Quality Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.20", "DSE Type Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.21", "Enhanced Guide Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.22", "Facsimile Telephone Number Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.23", "Fax Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.25", "Guide Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.29", "Master And Shadow Access Points Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.30", "Matching Rule Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.31", "Matching Rule Use Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.32", "Mail Preference Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.33", "MHS OR Address Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.34", "Name And Optional UID Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.35", "Name Form Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.39", "Other Mailbox Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.42", "Protocol Information Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.43", "Presentation Address Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.45", "Subtree Specification Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.46", "Supplier Information Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.47", "Supplier Or Consumer Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.48", "Supplier And Consumer Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.49", "Supported Algorithm Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.51", "Teletex Terminal Identifier Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.52", "Telex Number Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.54", "LDAP Syntax Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.55", "Modify Rights Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.56", "LDAP Schema Definition Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.57", "LDAP Schema Description Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.1466.115.121.1.58", "Substring Assertion Syntax");
        
        // Additional Standard Syntaxes
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.1.16.1", "UUID Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.1.15.1", "Relative Subtree Specification Syntax");
        
        // Ping Identity Custom Syntaxes
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.3.1", "Hex String Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.3.2", "Compact Timestamp Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.3.3", "JSON Object Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.3.4", "LDAP URL Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.3.5", "Relative Time Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.3.6", "Log Level Syntax");
        SYNTAX_DESCRIPTIONS.put("1.3.6.1.4.1.30221.1.3.7", "Log Category Syntax");
    }
    
    // =================================================
    // FEATURES
    // =================================================
    
    private static final Map<String, String> FEATURE_DESCRIPTIONS = new HashMap<>();
    static {
        FEATURE_DESCRIPTIONS.put("1.3.6.1.1.14", "LDAP Modify-Increment Extension Feature");
        FEATURE_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.5.1", "All Operational Attributes Feature");
        FEATURE_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.5.2", "Requesting Attributes by Object Class Feature");
        FEATURE_DESCRIPTIONS.put("1.3.6.1.4.1.4203.1.5.3", "LDAP Absolute True and False Filters Feature");
        FEATURE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.12.1", "Excluding Attributes from Search Result Entries Feature");
        FEATURE_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.12.2", "Subordinate Subtree Search Scope Feature");
    }
    
    // =================================================
    // ADMINISTRATIVE ALERTS
    // =================================================
    
    private static final Map<String, String> ALERT_DESCRIPTIONS = new HashMap<>();
    static {
        // Ping Identity Directory Server Administrative Alerts
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.1", "Directory Server Backend Database Disk Space Full");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.2", "Directory Server Backend Database Disk Space Low");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.3", "Directory Server Backend Database Corrupt");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.4", "Directory Server Backend Database Malformed");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.5", "Directory Server Backend Database Unavailable");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.6", "Directory Server Backend Database Restored");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.7", "Directory Server Backend Database Exported");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.8", "Directory Server Backend Database Imported");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.9", "Directory Server Backend Database Backup Begin");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.10", "Directory Server Backend Database Backup End");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.11", "Directory Server Backend Database Backup Error");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.12", "Directory Server Backend Database Verify Begin");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.13", "Directory Server Backend Database Verify End");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.14", "Directory Server Backend Database Verify Error");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.15", "Directory Server Backend Database Rebuild Begin");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.16", "Directory Server Backend Database Rebuild End");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.17", "Directory Server Backend Database Rebuild Error");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.18", "Directory Server Connection Handler Connection Limit Exceeded");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.19", "Directory Server Connection Handler Connection Limit Normal");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.20", "Directory Server Connection Handler Refused Connection");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.21", "Directory Server Connection Handler Connection Lost");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.22", "Directory Server Connection Handler Unbind Request");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.23", "Directory Server Extended Operation Processing Error");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.24", "Directory Server LDIF Import Begin");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.25", "Directory Server LDIF Import End");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.26", "Directory Server LDIF Import Error");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.27", "Directory Server LDIF Export Begin");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.28", "Directory Server LDIF Export End");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.29", "Directory Server LDIF Export Error");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.30", "Directory Server Log Disk Space Full");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.31", "Directory Server Log Disk Space Low");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.32", "Directory Server JVM Memory Full");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.33", "Directory Server JVM Memory Low");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.34", "Directory Server Replication Server Connection Lost");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.35", "Directory Server Replication Server Connection Established");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.36", "Directory Server Replication Conflict Detected");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.37", "Directory Server Replication Conflict Resolved");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.38", "Directory Server Replication Replay Queue Full");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.39", "Directory Server Replication Replay Queue Normal");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.40", "Directory Server Schema Inconsistency Detected");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.41", "Directory Server Schema Consistency Restored");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.42", "Directory Server Configuration Change");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.43", "Directory Server Administrative User Access Denied");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.44", "Directory Server Password Policy Warning");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.45", "Directory Server Password Policy Account Locked");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.46", "Directory Server Password Policy Account Unlocked");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.47", "Directory Server Backend Database Read Only");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.48", "Directory Server Backend Database Read Write");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.49", "Directory Server Backend Database Connection Pool Full");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.50", "Directory Server Backend Database Connection Pool Normal");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.51", "Directory Server Monitor Entry Update Failed");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.52", "Directory Server Monitor Entry Update Succeeded");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.53", "Directory Server Backend Database Lock Timeout");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.54", "Directory Server Backend Database Lock Normal");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.55", "Directory Server SSL Context Creation Failed");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.56", "Directory Server SSL Context Creation Succeeded");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.57", "Directory Server Certificate Expiring");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.58", "Directory Server Certificate Expired");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.59", "Directory Server Certificate Renewed");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.60", "Directory Server Trust Store Update");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.61", "Directory Server Key Store Update");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.62", "Directory Server Startup Complete");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.63", "Directory Server Shutdown Beginning");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.64", "Directory Server Shutdown Complete");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.65", "Directory Server Access Control Evaluation Error");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.66", "Directory Server Connection Limit Approached");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.67", "Directory Server Thread Pool Congestion");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.68", "Directory Server Thread Pool Normal");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.69", "Directory Server Entry Cache Full");
        ALERT_DESCRIPTIONS.put("1.3.6.1.4.1.30221.2.6.70", "Directory Server Entry Cache Normal");
    }

    // =================================================
    // PUBLIC UTILITY METHODS
    // =================================================
    
    /**
     * Gets the description for a control OID.
     * 
     * @param oid The OID to look up
     * @return The description, or null if not found
     */
    public static String getControlDescription(String oid) {
        return CONTROL_DESCRIPTIONS.get(oid);
    }
    
    /**
     * Gets the description for an extended operation OID.
     * 
     * @param oid The OID to look up
     * @return The description, or null if not found
     */
    public static String getExtendedOperationDescription(String oid) {
        return EXTENDED_OPERATION_DESCRIPTIONS.get(oid);
    }
    
    /**
     * Gets the description for an attribute type OID.
     * 
     * @param oid The OID to look up
     * @return The description, or null if not found
     */
    public static String getAttributeTypeDescription(String oid) {
        return ATTRIBUTE_TYPE_DESCRIPTIONS.get(oid);
    }
    
    /**
     * Gets the description for an object class OID.
     * 
     * @param oid The OID to look up
     * @return The description, or null if not found
     */
    public static String getObjectClassDescription(String oid) {
        return OBJECT_CLASS_DESCRIPTIONS.get(oid);
    }
    
    /**
     * Gets the description for a matching rule OID.
     * 
     * @param oid The OID to look up
     * @return The description, or null if not found
     */
    public static String getMatchingRuleDescription(String oid) {
        return MATCHING_RULE_DESCRIPTIONS.get(oid);
    }
    
    /**
     * Gets the description for a syntax OID.
     * 
     * @param oid The OID to look up
     * @return The description, or null if not found
     */
    public static String getSyntaxDescription(String oid) {
        return SYNTAX_DESCRIPTIONS.get(oid);
    }
    
    /**
     * Gets the description for a feature OID.
     * 
     * @param oid The OID to look up
     * @return The description, or null if not found
     */
    public static String getFeatureDescription(String oid) {
        return FEATURE_DESCRIPTIONS.get(oid);
    }
    
    public static String getAlertDescription(String oid) {
        return ALERT_DESCRIPTIONS.get(oid);
    }
    
    /**
     * Gets a description for any OID by searching all categories.
     * 
     * @param oid The OID to look up
     * @return The description with category prefix, or the OID itself if not found
     */
    public static String getAnyDescription(String oid) {
        String desc = getControlDescription(oid);
        if (desc != null) return "[Control] " + desc;
        
        desc = getExtendedOperationDescription(oid);
        if (desc != null) return "[Extended Op] " + desc;
        
        desc = getAttributeTypeDescription(oid);
        if (desc != null) return "[Attribute] " + desc;
        
        desc = getObjectClassDescription(oid);
        if (desc != null) return "[Object Class] " + desc;
        
        desc = getMatchingRuleDescription(oid);
        if (desc != null) return "[Matching Rule] " + desc;
        
        desc = getSyntaxDescription(oid);
        if (desc != null) return "[Syntax] " + desc;
        
        desc = getFeatureDescription(oid);
        if (desc != null) return "[Feature] " + desc;
        
        desc = getAlertDescription(oid);
        if (desc != null) return "[Alert] " + desc;
        
        return oid; // Return original OID if no description found
    }
    
    /**
     * Formats an OID with its description for display purposes.
     * 
     * @param oid The OID
     * @param category The specific category to search (controls, extensions, etc.)
     * @return Formatted string like "1.2.3.4 - Description" or just the OID if no description
     */
    public static String formatOidWithDescription(String oid, OidCategory category) {
        String description = null;
        
        switch (category) {
            case CONTROL:
                description = getControlDescription(oid);
                break;
            case EXTENDED_OPERATION:
                description = getExtendedOperationDescription(oid);
                break;
            case ATTRIBUTE_TYPE:
                description = getAttributeTypeDescription(oid);
                break;
            case OBJECT_CLASS:
                description = getObjectClassDescription(oid);
                break;
            case MATCHING_RULE:
                description = getMatchingRuleDescription(oid);
                break;
            case SYNTAX:
                description = getSyntaxDescription(oid);
                break;
            case FEATURE:
                description = getFeatureDescription(oid);
                break;
            case ALERT:
                description = getAlertDescription(oid);
                break;
            case ANY:
                return formatOidWithAnyDescription(oid);
        }
        
        if (description != null) {
            return oid + " - " + description;
        }
        return oid;
    }
    
    /**
     * Formats an OID with its description, searching all categories.
     * 
     * @param oid The OID
     * @return Formatted string like "1.2.3.4 - [Category] Description" or just the OID
     */
    public static String formatOidWithAnyDescription(String oid) {
        String description = getAnyDescription(oid);
        if (!description.equals(oid)) {
            return oid + " - " + description;
        }
        return oid;
    }
    
    /**
     * Gets all control OIDs.
     * 
     * @return Map of control OIDs to descriptions
     */
    public static Map<String, String> getAllControls() {
        return new HashMap<>(CONTROL_DESCRIPTIONS);
    }
    
    /**
     * Gets all extended operation OIDs.
     * 
     * @return Map of extended operation OIDs to descriptions
     */
    public static Map<String, String> getAllExtendedOperations() {
        return new HashMap<>(EXTENDED_OPERATION_DESCRIPTIONS);
    }
    
    /**
     * Enumeration of OID categories for targeted lookups.
     */
    public enum OidCategory {
        CONTROL,
        EXTENDED_OPERATION,
        ATTRIBUTE_TYPE,
        OBJECT_CLASS,
        MATCHING_RULE,
        SYNTAX,
        FEATURE,
        ALERT,
        ANY
    }

    // Private constructor to prevent instantiation
    private OidLookupTable() {
        throw new UnsupportedOperationException("This utility class cannot be instantiated");
    }
}