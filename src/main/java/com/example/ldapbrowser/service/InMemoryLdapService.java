package com.example.ldapbrowser.service;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
* Service for managing UnboundID In-Memory Directory Servers
*/
@Service
@Scope("singleton")
public class InMemoryLdapService {

  private final Map<String, InMemoryDirectoryServer> runningServers = new ConcurrentHashMap<>();
  private final Map<String, LdapServerConfig> serverConfigurations = new ConcurrentHashMap<>();

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String CONFIG_FILE_PATH = "inmemory-servers.json";

  @PostConstruct
  private void loadConfigurations() {
    try {
      File configFile = new File(CONFIG_FILE_PATH);
      if (configFile.exists()) {
        TypeReference<Map<String, LdapServerConfig>> typeRef = new TypeReference<Map<String, LdapServerConfig>>() {};
        Map<String, LdapServerConfig> loadedConfigs = objectMapper.readValue(configFile, typeRef);
        serverConfigurations.putAll(loadedConfigs);
      }
    } catch (IOException e) {
    // Initialize with empty configurations if loading fails
  }
}

@PreDestroy
private void cleanup() {
  // Safety check - don't overwrite file if it has more servers than memory
  File configFile = new File(CONFIG_FILE_PATH);
  if (configFile.exists()) {
    try {
      String fileContent = Files.readString(configFile.toPath());

      TypeReference<Map<String, LdapServerConfig>> typeRef = new TypeReference<Map<String, LdapServerConfig>>() {};
      Map<String, LdapServerConfig> fileConfigs = objectMapper.readValue(fileContent, typeRef);

      if (fileConfigs.size() > serverConfigurations.size()) {
        return; // Don't save - prevent data loss
      }
    } catch (Exception e) {
    // Continue with save if safety check fails
  }
}

saveConfigurationsToDisk();
}

private void saveConfigurationsToDisk() {
  try {
    objectMapper.writeValue(new File(CONFIG_FILE_PATH), new HashMap<>(serverConfigurations));
  } catch (IOException e) {
  // Log error if needed for debugging
}
}

/**
* Start an in-memory LDAP server with the given configuration
*/
public void startServer(LdapServerConfig config) throws LDAPException {
  if (runningServers.containsKey(config.getId())) {
    throw new LDAPException(ResultCode.CONSTRAINT_VIOLATION, "Server is already running");
  }

  try {
    // Create the in-memory directory server configuration
    InMemoryDirectoryServerConfig serverConfig = new InMemoryDirectoryServerConfig(config.getBaseDn());

    // Set up listeners
    List<InMemoryListenerConfig> listenerConfigs = new ArrayList<>();

    if (config.isUseSSL()) {
      // SSL listener
      SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
      InMemoryListenerConfig sslListener = InMemoryListenerConfig.createLDAPSConfig(
      "SSL", config.getPort(), sslUtil.createSSLServerSocketFactory());
      listenerConfigs.add(sslListener);
    } else {
    // Plain LDAP listener
    InMemoryListenerConfig plainListener = InMemoryListenerConfig.createLDAPConfig(
    "LDAP", config.getPort());
    listenerConfigs.add(plainListener);
  }

  serverConfig.setListenerConfigs(listenerConfigs);

  // Set authentication if provided
  if (config.getBindDn() != null && !config.getBindDn().trim().isEmpty()) {
    serverConfig.addAdditionalBindCredentials(config.getBindDn(), config.getPassword());
  }

  // Create and start the server
  InMemoryDirectoryServer server = new InMemoryDirectoryServer(serverConfig);

  // Add initial entries if base DN is provided
  if (config.getBaseDn() != null && !config.getBaseDn().trim().isEmpty()) {
    addInitialEntries(server, config.getBaseDn());
  }

  server.startListening();

  // Store the running server and configuration
  runningServers.put(config.getId(), server);
  serverConfigurations.put(config.getId(), config);

} catch (Exception e) {
throw new LDAPException(ResultCode.LOCAL_ERROR, "Failed to start in-memory server: " + e.getMessage());
}
}

/**
* Stop an in-memory LDAP server
*/
public void stopServer(String serverId) {
  InMemoryDirectoryServer server = runningServers.remove(serverId);
  if (server != null) {
    server.shutDown(true);
  }
}

/**
* Check if a server is running
*/
public boolean isServerRunning(String serverId) {
  InMemoryDirectoryServer server = runningServers.get(serverId);
  return server != null && server.getListenPort() > 0;
}

/**
* Get all in-memory server configurations
*/
public List<LdapServerConfig> getAllInMemoryServers() {
  // If the in-memory map is empty but the file exists, reload from disk
  // This handles cases where DevTools recreated the Bean but lost the in-memory state
  File configFile = new File(CONFIG_FILE_PATH);
  if (serverConfigurations.isEmpty() && configFile.exists()) {
    loadConfigurations();
  }

  return new ArrayList<>(serverConfigurations.values());
}

/**
* Save an in-memory server configuration
*/
public void saveInMemoryServer(LdapServerConfig config) {
  serverConfigurations.put(config.getId(), config);

  // Persist to disk immediately
  saveConfigurationsToDisk();
}

/**
* Delete an in-memory server configuration
*/
public void deleteInMemoryServer(String serverId) {
  // Stop the server if it's running
  stopServer(serverId);
  serverConfigurations.remove(serverId);
  // Persist to disk immediately
  saveConfigurationsToDisk();
}

/**
* Get a running server instance
*/
public InMemoryDirectoryServer getRunningServer(String serverId) {
  return runningServers.get(serverId);
}

/**
* Add initial test entries to the server
*/
private void addInitialEntries(InMemoryDirectoryServer server, String baseDn) throws LDAPException {
  try {
    // Add the base DN entry if it doesn't exist
    if (!server.entryExists(baseDn)) {
      Entry baseEntry = createDomainEntry(baseDn);
      if (baseEntry != null) {
        server.add(baseEntry);
      }
    }

    // Add some test organizational units
    Entry peopleOU = new Entry("ou=people," + baseDn);
    peopleOU.addAttribute("objectClass", "organizationalUnit");
    peopleOU.addAttribute("ou", "people");
    peopleOU.addAttribute("description", "Container for user accounts");
    server.add(peopleOU);

    Entry groupsOU = new Entry("ou=groups," + baseDn);
    groupsOU.addAttribute("objectClass", "organizationalUnit");
    groupsOU.addAttribute("ou", "groups");
    groupsOU.addAttribute("description", "Container for groups");
    server.add(groupsOU);

    // Add some test users
    Entry adminUser = new Entry("uid=admin,ou=people," + baseDn);
    adminUser.addAttribute("objectClass", "inetOrgPerson");
    adminUser.addAttribute("uid", "admin");
    adminUser.addAttribute("cn", "Administrator");
    adminUser.addAttribute("sn", "Admin");
    adminUser.addAttribute("userPassword", "admin123");
    adminUser.addAttribute("mail", "admin@example.com");
    server.add(adminUser);

    Entry jdoeUser = new Entry("uid=jdoe,ou=people," + baseDn);
    jdoeUser.addAttribute("objectClass", "inetOrgPerson");
    jdoeUser.addAttribute("uid", "jdoe");
    jdoeUser.addAttribute("cn", "John Doe");
    jdoeUser.addAttribute("sn", "Doe");
    jdoeUser.addAttribute("givenName", "John");
    jdoeUser.addAttribute("userPassword", "password123");
    jdoeUser.addAttribute("mail", "john.doe@example.com");
    jdoeUser.addAttribute("telephoneNumber", "+1-555-123-4567");
    server.add(jdoeUser);

    Entry jsmithUser = new Entry("uid=jsmith,ou=people," + baseDn);
    jsmithUser.addAttribute("objectClass", "inetOrgPerson");
    jsmithUser.addAttribute("uid", "jsmith");
    jsmithUser.addAttribute("cn", "Jane Smith");
    jsmithUser.addAttribute("sn", "Smith");
    jsmithUser.addAttribute("givenName", "Jane");
    jsmithUser.addAttribute("userPassword", "password456");
    jsmithUser.addAttribute("mail", "jane.smith@example.com");
    jsmithUser.addAttribute("telephoneNumber", "+1-555-987-6543");
    server.add(jsmithUser);

    // Add some test groups
    Entry adminGroup = new Entry("cn=administrators,ou=groups," + baseDn);
    adminGroup.addAttribute("objectClass", "groupOfUniqueNames");
    adminGroup.addAttribute("cn", "administrators");
    adminGroup.addAttribute("description", "System administrators");
    adminGroup.addAttribute("uniqueMember", "uid=admin,ou=people," + baseDn);
    server.add(adminGroup);

    Entry usersGroup = new Entry("cn=users,ou=groups," + baseDn);
    usersGroup.addAttribute("objectClass", "groupOfUniqueNames");
    usersGroup.addAttribute("cn", "users");
    usersGroup.addAttribute("description", "Regular users");
    usersGroup.addAttribute("uniqueMember", "uid=jdoe,ou=people," + baseDn);
    usersGroup.addAttribute("uniqueMember", "uid=jsmith,ou=people," + baseDn);
    server.add(usersGroup);

  } catch (Exception e) {
  throw new LDAPException(ResultCode.LOCAL_ERROR, "Failed to add initial entries: " + e.getMessage());
}
}

/**
* Create a domain component entry
*/
private Entry createDomainEntry(String dn) {
  try {
    DN parsedDN = new DN(dn);
    String[] rdnComponents = parsedDN.getRDN().getAttributeValues();

    if (rdnComponents.length > 0) {
      String value = rdnComponents[0];

      if (parsedDN.getRDN().getAttributeNames()[0].equalsIgnoreCase("dc")) {
        Entry entry = new Entry(dn);
        entry.addAttribute("objectClass", "dcObject");
        entry.addAttribute("objectClass", "organization");
        entry.addAttribute("dc", value);
        entry.addAttribute("o", value + " Organization");
        return entry;
      } else if (parsedDN.getRDN().getAttributeNames()[0].equalsIgnoreCase("o")) {
      Entry entry = new Entry(dn);
      entry.addAttribute("objectClass", "organization");
      entry.addAttribute("o", value);
      return entry;
    }
  }
} catch (Exception e) {
// Ignore and return null
}
return null;
}
}