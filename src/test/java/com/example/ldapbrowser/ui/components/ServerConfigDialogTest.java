package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
* Test class for ServerConfigDialog functionality
*/
public class ServerConfigDialogTest {

 @Test
 public void testNewServerConfigDialog() {
  // Test creating a new server config dialog
  ServerConfigDialog dialog = new ServerConfigDialog(null);
  assertNotNull(dialog);
 }

 @Test
 public void testEditServerConfigDialog() {
  // Test editing an existing server config
  LdapServerConfig config = new LdapServerConfig();
  config.setId("test-id");
  config.setName("Test Server");
  config.setHost("localhost");
  config.setPort(389);
  
  ServerConfigDialog dialog = new ServerConfigDialog(config);
  assertNotNull(dialog);
 }

 @Test
 public void testServerConfigProperties() {
  // Test LdapServerConfig model
  LdapServerConfig config = new LdapServerConfig();
  
  config.setName("Test Server");
  config.setHost("ldap.example.com");
  config.setPort(636);
  config.setBindDn("cn=admin,dc=example,dc=com");
  config.setPassword("secret");
  config.setUseSSL(true);
  config.setUseStartTLS(false);
  config.setBaseDn("dc=example,dc=com");
  
  assertEquals("Test Server", config.getName());
  assertEquals("ldap.example.com", config.getHost());
  assertEquals(636, config.getPort());
  assertEquals("cn=admin,dc=example,dc=com", config.getBindDn());
  assertEquals("secret", config.getPassword());
  assertTrue(config.isUseSSL());
  assertFalse(config.isUseStartTLS());
  assertEquals("dc=example,dc=com", config.getBaseDn());
 }
}
