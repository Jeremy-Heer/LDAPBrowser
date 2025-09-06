package com.ldapweb.ldapbrowser.util;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleUseDefinition;
import com.unboundid.ldap.sdk.schema.AttributeSyntaxDefinition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.HashMap;

public class SchemaCompareUtilTest {

    @Test
    public void testObjectClassCanonicalWithExtensions() throws LDAPException {
        Map<String, String[]> extensions = new HashMap<>();
        extensions.put("X-ORIGIN", new String[]{"RFC2798"});
        extensions.put("X-DESCRIPTION", new String[]{"inetOrgPerson"});
        
        ObjectClassDefinition def = new ObjectClassDefinition("( 2.16.840.1.113730.3.2.2 NAME 'inetOrgPerson' " +
            "SUP organizationalPerson STRUCTURAL " +
            "MAY ( audio $ businessCategory $ carLicense $ departmentNumber $ displayName $ " +
            "employeeNumber $ employeeType $ givenName $ homePhone $ homePostalAddress $ " +
            "initials $ jpegPhoto $ labeledURI $ mail $ manager $ mobile $ o $ pager $ " +
            "photo $ roomNumber $ secretary $ uid $ userCertificate $ x500uniqueIdentifier $ " +
            "preferredLanguage $ userSMIMECertificate $ userPKCS12 ) " +
            "X-ORIGIN 'RFC2798' X-DESCRIPTION 'inetOrgPerson' )");
        
        String canonicalWithExtensions = SchemaCompareUtil.canonical(def, true);
        String canonicalWithoutExtensions = SchemaCompareUtil.canonical(def, false);
        
        // Verify that the version with extensions contains extension information
        assertTrue(canonicalWithExtensions.contains("x-origin"));
        assertTrue(canonicalWithExtensions.contains("x-description"));
        
        // Verify that the version without extensions does not contain extension information
        assertFalse(canonicalWithoutExtensions.contains("x-origin"));
        assertFalse(canonicalWithoutExtensions.contains("x-description"));
        
        // Verify that both contain core information
        assertTrue(canonicalWithExtensions.contains("oid=2.16.840.1.113730.3.2.2"));
        assertTrue(canonicalWithoutExtensions.contains("oid=2.16.840.1.113730.3.2.2"));
        
        // Verify that the versions are different
        assertNotEquals(canonicalWithExtensions, canonicalWithoutExtensions);
    }

    @Test
    public void testAttributeTypeCanonicalWithExtensions() throws LDAPException {
        AttributeTypeDefinition def = new AttributeTypeDefinition("( 2.5.4.3 NAME 'cn' " +
            "SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch " +
            "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-ORIGIN 'RFC4519' )");
        
        String canonicalWithExtensions = SchemaCompareUtil.canonical(def, true);
        String canonicalWithoutExtensions = SchemaCompareUtil.canonical(def, false);
        
        // Verify that the version with extensions contains extension information
        assertTrue(canonicalWithExtensions.contains("x-origin"));
        
        // Verify that the version without extensions does not contain extension information
        assertFalse(canonicalWithoutExtensions.contains("x-origin"));
        
        // Verify that both contain core information
        assertTrue(canonicalWithExtensions.contains("oid=2.5.4.3"));
        assertTrue(canonicalWithoutExtensions.contains("oid=2.5.4.3"));
        
        // Verify that the versions are different
        assertNotEquals(canonicalWithExtensions, canonicalWithoutExtensions);
    }

    @Test
    public void testDefaultBehaviorIncludesExtensions() throws LDAPException {
        AttributeTypeDefinition def = new AttributeTypeDefinition("( 2.5.4.3 NAME 'cn' " +
            "SUP name EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch " +
            "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-ORIGIN 'RFC4519' )");
        
        String canonicalDefault = SchemaCompareUtil.canonical(def);
        String canonicalWithExtensions = SchemaCompareUtil.canonical(def, true);
        
        // Verify that default behavior includes extensions
        assertEquals(canonicalDefault, canonicalWithExtensions);
        assertTrue(canonicalDefault.contains("x-origin"));
    }

    @Test
    public void testMatchingRuleCanonicalWithExtensions() throws LDAPException {
        MatchingRuleDefinition def = new MatchingRuleDefinition("( 2.5.13.2 NAME 'caseIgnoreMatch' " +
            "SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 X-ORIGIN 'RFC4517' )");
        
        String canonicalWithExtensions = SchemaCompareUtil.canonical(def, true);
        String canonicalWithoutExtensions = SchemaCompareUtil.canonical(def, false);
        
        assertTrue(canonicalWithExtensions.contains("x-origin"));
        assertFalse(canonicalWithoutExtensions.contains("x-origin"));
        assertNotEquals(canonicalWithExtensions, canonicalWithoutExtensions);
    }

    @Test
    public void testMatchingRuleUseCanonicalWithExtensions() throws LDAPException {
        MatchingRuleUseDefinition def = new MatchingRuleUseDefinition("( 2.5.13.2 " +
            "APPLIES ( cn $ commonName ) X-ORIGIN 'RFC4517' )");
        
        String canonicalWithExtensions = SchemaCompareUtil.canonical(def, true);
        String canonicalWithoutExtensions = SchemaCompareUtil.canonical(def, false);
        
        assertTrue(canonicalWithExtensions.contains("x-origin"));
        assertFalse(canonicalWithoutExtensions.contains("x-origin"));
        assertNotEquals(canonicalWithExtensions, canonicalWithoutExtensions);
    }

    @Test
    public void testAttributeSyntaxCanonicalWithExtensions() throws LDAPException {
        AttributeSyntaxDefinition def = new AttributeSyntaxDefinition("( 1.3.6.1.4.1.1466.115.121.1.15 " +
            "DESC 'Directory String' X-ORIGIN 'RFC4517' )");
        
        String canonicalWithExtensions = SchemaCompareUtil.canonical(def, true);
        String canonicalWithoutExtensions = SchemaCompareUtil.canonical(def, false);
        
        assertTrue(canonicalWithExtensions.contains("x-origin"));
        assertFalse(canonicalWithoutExtensions.contains("x-origin"));
        assertNotEquals(canonicalWithExtensions, canonicalWithoutExtensions);
    }
}
