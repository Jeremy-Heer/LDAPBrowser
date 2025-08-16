package com.example.ldapbrowser.service;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* Service for LDAP operations using UnboundID SDK
*/
@Service
public class LdapService {

  private final Map<String, LDAPConnection> connections = new HashMap<>();
  private final LoggingService loggingService;

  public LdapService(LoggingService loggingService) {
    this.loggingService = loggingService;
  }

  /**
  * Test connection to LDAP server
  */
  public boolean testConnection(LdapServerConfig config) {
    try {
      LDAPConnection connection = createConnection(config);
      connection.close();
      return true;
    } catch (Exception e) {
    return false;
  }
}

/**
* Connect to LDAP server
*/
public void connect(LdapServerConfig config) throws LDAPException {
  try {
    loggingService.logInfo("CONNECTION", "Attempting to connect to " + config.getName() + " (" + config.getHost() + ":" + config.getPort() + ")");
    LDAPConnection connection = createConnection(config);
    connections.put(config.getId(), connection);
    config.setConnection(connection);
    loggingService.logConnection(config.getName(), "Successfully connected");
  } catch (LDAPException e) {
  loggingService.logConnectionError(config.getName(), "Connection failed", e.getMessage());
  throw e;
}
}

/**
* Disconnect from LDAP server
*/
public void disconnect(String serverId) {
  LDAPConnection connection = connections.remove(serverId);
  if (connection != null && connection.isConnected()) {
    // Find the server name for logging
    String serverName = "Server " + serverId;
    connection.close();
    loggingService.logConnection(serverName, "Disconnected");
  }
}

/**
* Check if connected to a server
*/
public boolean isConnected(String serverId) {
  LDAPConnection connection = connections.get(serverId);
  return connection != null && connection.isConnected();
}

/**
* Browse LDAP entries under a given DN
*/
public List<LdapEntry> browseEntries(String serverId, String baseDn) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  // Optimize: Only request essential attributes for browsing
  // We need objectClass for display logic and icon determination
  SearchRequest searchRequest = new SearchRequest(
  baseDn,
  SearchScope.ONE,
  Filter.createPresenceFilter("objectClass"),
  "objectClass", "cn", "ou", "dc" // Only essential attributes for display
  );
  searchRequest.setSizeLimit(100); // Limit to 100 entries for performance

  SearchResult searchResult = connection.search(searchRequest);

  List<LdapEntry> entries = new ArrayList<>();
  for (SearchResultEntry entry : searchResult.getSearchEntries()) {
    LdapEntry ldapEntry = new LdapEntry(entry);
    // Check if entry has children
    ldapEntry.setHasChildren(hasChildren(connection, entry.getDN()));
    entries.add(ldapEntry);
  }

  // Log if we hit the size limit
  if (searchResult.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
    // Size limit exceeded - this is expected for large directories
  }

  // Sort entries by display name
  entries.sort(Comparator.comparing(LdapEntry::getDisplayName));

  return entries;
}

/**
* Browse LDAP entries with metadata about size limits
*/
public BrowseResult browseEntriesWithMetadata(String serverId, String baseDn) throws LDAPException {
  return browseEntriesWithMetadata(serverId, baseDn, 0, 100);
}

/**
* Browse LDAP entries with metadata about size limits and paging support
*/
public BrowseResult browseEntriesWithMetadata(String serverId, String baseDn, int page, int pageSize) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  // Optimize: Only request essential attributes for browsing
  SearchRequest searchRequest = new SearchRequest(
  baseDn,
  SearchScope.ONE,
  Filter.createPresenceFilter("objectClass"),
  "objectClass", "cn", "ou", "dc" // Only essential attributes for display
  );
  
  // For paging, we need to get more entries to determine total count and calculate pages
  // We'll use a larger size limit and handle paging client-side for better performance
  searchRequest.setSizeLimit(Math.max(1000, (page + 2) * pageSize)); // Get enough for current page + next page detection

  try {
    SearchResult searchResult = connection.search(searchRequest);

    List<LdapEntry> allEntries = new ArrayList<>();
    for (SearchResultEntry entry : searchResult.getSearchEntries()) {
      LdapEntry ldapEntry = new LdapEntry(entry);
      // Check if entry has children
      ldapEntry.setHasChildren(hasChildren(connection, entry.getDN()));
      allEntries.add(ldapEntry);
    }

    // Sort entries by display name
    allEntries.sort(Comparator.comparing(LdapEntry::getDisplayName));

    // Calculate pagination
    int totalEntries = allEntries.size();
    boolean sizeLimitExceeded = searchResult.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED;
    
    // If size limit exceeded, we know there are more entries than we retrieved
    if (sizeLimitExceeded) {
      totalEntries = -1; // Indicate unknown total count
    }
    
    // Extract page entries
    int startIndex = page * pageSize;
    int endIndex = Math.min(startIndex + pageSize, allEntries.size());
    
    List<LdapEntry> pageEntries;
    if (startIndex >= allEntries.size()) {
      pageEntries = new ArrayList<>(); // Empty page
    } else {
      pageEntries = new ArrayList<>(allEntries.subList(startIndex, endIndex));
    }
    
    // Determine if there are more pages
    boolean hasNextPage = (endIndex < allEntries.size()) || sizeLimitExceeded;
    boolean hasPrevPage = page > 0;
    
    return new BrowseResult(pageEntries, sizeLimitExceeded, totalEntries, page, pageSize, hasNextPage, hasPrevPage);

  } catch (LDAPSearchException e) {
  // Handle size limit exceeded gracefully by returning partial results
  if (e.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
    List<LdapEntry> allEntries = new ArrayList<>();
    // Get the partial results from the search exception
    for (SearchResultEntry entry : e.getSearchEntries()) {
      LdapEntry ldapEntry = new LdapEntry(entry);
      // Check if entry has children
      ldapEntry.setHasChildren(hasChildren(connection, entry.getDN()));
      allEntries.add(ldapEntry);
    }

    // Sort entries by display name
    allEntries.sort(Comparator.comparing(LdapEntry::getDisplayName));

    // Calculate pagination for partial results
    int startIndex = page * pageSize;
    int endIndex = Math.min(startIndex + pageSize, allEntries.size());
    
    List<LdapEntry> pageEntries;
    if (startIndex >= allEntries.size()) {
      pageEntries = new ArrayList<>(); // Empty page
    } else {
      pageEntries = new ArrayList<>(allEntries.subList(startIndex, endIndex));
    }
    
    boolean hasNextPage = (endIndex < allEntries.size()) || true; // Always true when size limit exceeded
    boolean hasPrevPage = page > 0;

    return new BrowseResult(pageEntries, true, -1, page, pageSize, hasNextPage, hasPrevPage);
  } else {
  // Re-throw other types of LDAP exceptions
  throw e;
}
}
}

/**
* Result wrapper for browse operations
*/
public static class BrowseResult {
  private final List<LdapEntry> entries;
  private final boolean sizeLimitExceeded;
  private final int entryCount;
  private final int currentPage;
  private final int pageSize;
  private final boolean hasNextPage;
  private final boolean hasPrevPage;

  public BrowseResult(List<LdapEntry> entries, boolean sizeLimitExceeded, int entryCount) {
    this(entries, sizeLimitExceeded, entryCount, 0, entries.size(), false, false);
  }

  public BrowseResult(List<LdapEntry> entries, boolean sizeLimitExceeded, int entryCount, 
                     int currentPage, int pageSize, boolean hasNextPage, boolean hasPrevPage) {
    this.entries = entries;
    this.sizeLimitExceeded = sizeLimitExceeded;
    this.entryCount = entryCount;
    this.currentPage = currentPage;
    this.pageSize = pageSize;
    this.hasNextPage = hasNextPage;
    this.hasPrevPage = hasPrevPage;
  }

  public List<LdapEntry> getEntries() { return entries; }
  public boolean isSizeLimitExceeded() { return sizeLimitExceeded; }
  public int getEntryCount() { return entryCount; }
  public int getCurrentPage() { return currentPage; }
  public int getPageSize() { return pageSize; }
  public boolean hasNextPage() { return hasNextPage; }
  public boolean hasPrevPage() { return hasPrevPage; }
  public int getTotalPages() { 
    if (entryCount < 0) return -1; // Unknown total
    return (int) Math.ceil((double) entryCount / pageSize); 
  }
}

/**
* Search LDAP entries
*/
public List<LdapEntry> searchEntries(String serverId, String baseDn, String filter, SearchScope scope) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  try {
    loggingService.logDebug("SEARCH", "Starting search - Server: " + serverId + ", Base: " + baseDn + ", Filter: " + filter);

    SearchRequest searchRequest = new SearchRequest(baseDn, scope, Filter.create(filter));
    searchRequest.setSizeLimit(1000); // Limit results to prevent overwhelming UI

    SearchResult searchResult = connection.search(searchRequest);

    List<LdapEntry> entries = new ArrayList<>();
    for (SearchResultEntry entry : searchResult.getSearchEntries()) {
      entries.add(new LdapEntry(entry));
    }

    loggingService.logSearch("Server " + serverId, baseDn, filter, entries.size());
    return entries;
  } catch (LDAPException e) {
  loggingService.logSearchError("Server " + serverId, baseDn, filter, e.getMessage());
  throw e;
}
}

/**
* Search LDAP entries with specific return attributes
*/
public List<LdapEntry> searchEntries(String serverId, String baseDn, String filter, SearchScope scope, String... attributes) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  try {
    String attrsList = attributes.length > 0 ? String.join(",", attributes) : "all";
    loggingService.logDebug("SEARCH", "Starting search with attributes - Server: " + serverId + ", Base: " + baseDn + ", Filter: " + filter + ", Attributes: " + attrsList);

    // Always ensure DN is available (though it's always returned by default)
    String[] finalAttributes = attributes;
    if (attributes.length > 0) {
      // Add "+" to get operational attributes if not already present
      boolean hasOperational = false;
      for (String attr : attributes) {
        if ("+".equals(attr)) {
          hasOperational = true;
          break;
        }
      }

      if (!hasOperational) {
        // Create new array with operational attributes included
        finalAttributes = new String[attributes.length + 1];
        System.arraycopy(attributes, 0, finalAttributes, 0, attributes.length);
        finalAttributes[attributes.length] = "+";
      }
    }

    SearchRequest searchRequest = new SearchRequest(baseDn, scope, Filter.create(filter), finalAttributes);
    searchRequest.setSizeLimit(1000); // Limit results to prevent overwhelming UI

    SearchResult searchResult = connection.search(searchRequest);

    List<LdapEntry> entries = new ArrayList<>();
    for (SearchResultEntry entry : searchResult.getSearchEntries()) {
      entries.add(new LdapEntry(entry));
    }

    loggingService.logSearch("Server " + serverId, baseDn, filter, entries.size());
    return entries;
  } catch (LDAPException e) {
  loggingService.logSearchError("Server " + serverId, baseDn, filter, e.getMessage());
  throw e;
}
}

/**
* Get a specific LDAP entry by DN - returns all attributes for entry details view
*/
public LdapEntry getEntry(String serverId, String dn) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  // For entry details, we want ALL attributes including operational attributes
  SearchRequest searchRequest = new SearchRequest(
  dn,
  SearchScope.BASE,
  Filter.createPresenceFilter("objectClass"),
  "*", "+" // Request all user attributes (*) and operational attributes (+)
  );

  SearchResult searchResult = connection.search(searchRequest);

  if (searchResult.getEntryCount() > 0) {
    return new LdapEntry(searchResult.getSearchEntries().get(0));
  }

  return null;
}

/**
* Get only DN of entries matching filter - optimized for bulk operations
*/
public List<String> getDNsOnly(String serverId, String baseDn, String filter, SearchScope scope) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  try {
    loggingService.logDebug("SEARCH", "DN-only search - Server: " + serverId + ", Base: " + baseDn + ", Filter: " + filter);

    // Optimize: Request no attributes, only DN (which is always returned)
    SearchRequest searchRequest = new SearchRequest(baseDn, scope, Filter.create(filter), "1.1");
    searchRequest.setSizeLimit(1000); // Limit results to prevent overwhelming UI

    SearchResult searchResult = connection.search(searchRequest);

    List<String> dns = new ArrayList<>();
    for (SearchResultEntry entry : searchResult.getSearchEntries()) {
      dns.add(entry.getDN());
    }

    loggingService.logSearch("Server " + serverId, baseDn, filter, dns.size());
    return dns;
  } catch (LDAPException e) {
  loggingService.logSearchError("Server " + serverId, baseDn, filter, e.getMessage());
  throw e;
}
}

/**
* Check if an entry exists by DN - no attributes returned
*/
public boolean entryExists(String serverId, String dn) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  try {
    // Optimize: Request no attributes, only check existence
    SearchRequest searchRequest = new SearchRequest(
    dn,
    SearchScope.BASE,
    Filter.createPresenceFilter("objectClass"),
    "1.1" // Request no attributes
    );

    SearchResult searchResult = connection.search(searchRequest);
    return searchResult.getEntryCount() > 0;
  } catch (LDAPException e) {
  if (e.getResultCode() == ResultCode.NO_SUCH_OBJECT) {
    return false;
  }
  throw e;
}
}

/**
* Get entry with minimal attributes for display purposes
*/
public LdapEntry getEntryMinimal(String serverId, String dn) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  // Optimize: Only request essential attributes for display
  SearchRequest searchRequest = new SearchRequest(
  dn,
  SearchScope.BASE,
  Filter.createPresenceFilter("objectClass"),
  "objectClass", "cn", "ou", "dc", "uid", "mail" // Essential display attributes
  );

  SearchResult searchResult = connection.search(searchRequest);

  if (searchResult.getEntryCount() > 0) {
    return new LdapEntry(searchResult.getSearchEntries().get(0));
  }

  return null;
}

/**
* Modify an LDAP entry
*/
public void modifyEntry(String serverId, String dn, List<Modification> modifications) throws LDAPException {
  modifyEntry(serverId, dn, modifications, null);
}

/**
* Modify an LDAP entry with optional controls
*/
public void modifyEntry(String serverId, String dn, List<Modification> modifications, List<Control> controls) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  try {
    loggingService.logDebug("MODIFY", "Modifying entry - Server: " + serverId + ", DN: " + dn + ", Modifications: " + modifications.size() +
    (controls != null ? ", Controls: " + controls.size() : ""));
    ModifyRequest modifyRequest = new ModifyRequest(dn, modifications);

    if (controls != null && !controls.isEmpty()) {
      modifyRequest.setControls(controls.toArray(new Control[0]));
    }

    connection.modify(modifyRequest);
    loggingService.logModification("Server " + serverId, dn, "MODIFY");
  } catch (LDAPException e) {
  loggingService.logModificationError("Server " + serverId, dn, "MODIFY", e.getMessage());
  throw e;
}
}

/**
* Add a new LDAP entry
*/
public void addEntry(String serverId, LdapEntry entry) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  try {
    loggingService.logDebug("MODIFY", "Adding entry - Server: " + serverId + ", DN: " + entry.getDn());
    Collection<Attribute> attributes = entry.getAttributes().entrySet().stream()
    .map(attr -> new Attribute(attr.getKey(), attr.getValue()))
    .collect(Collectors.toList());

    AddRequest addRequest = new AddRequest(entry.getDn(), attributes);
    connection.add(addRequest);
    loggingService.logModification("Server " + serverId, entry.getDn(), "ADD");
  } catch (LDAPException e) {
  loggingService.logModificationError("Server " + serverId, entry.getDn(), "ADD", e.getMessage());
  throw e;
}
}

/**
* Delete an LDAP entry
*/
public void deleteEntry(String serverId, String dn) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);

  try {
    loggingService.logDebug("MODIFY", "Deleting entry - Server: " + serverId + ", DN: " + dn);
    DeleteRequest deleteRequest = new DeleteRequest(dn);
    connection.delete(deleteRequest);
    loggingService.logModification("Server " + serverId, dn, "DELETE");
  } catch (LDAPException e) {
  loggingService.logModificationError("Server " + serverId, dn, "DELETE", e.getMessage());
  throw e;
}
}

/**
* Get LDAP schema information
*/
public Schema getSchema(String serverId) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);
  return connection.getSchema();
}

/**
* Get root DSE information
*/
public Entry getRootDSE(String serverId) throws LDAPException {
  LDAPConnection connection = getConnection(serverId);
  return connection.getRootDSE();
}

/**
* Check if the LDAP server supports a specific control
*/
public boolean isControlSupported(String serverId, String controlOID) throws LDAPException {
  Entry rootDSE = getRootDSE(serverId);

  if (rootDSE != null) {
    String[] supportedControls = rootDSE.getAttributeValues("supportedControl");
    if (supportedControls != null) {
      for (String supportedControl : supportedControls) {
        if (controlOID.equals(supportedControl)) {
          return true;
        }
      }
    }
  }

  return false;
}

/**
* Get naming contexts from Root DSE
*/
public List<String> getNamingContexts(String serverId) throws LDAPException {
  Entry rootDSE = getRootDSE(serverId);
  List<String> namingContexts = new ArrayList<>();

  if (rootDSE != null) {
    String[] contexts = rootDSE.getAttributeValues("namingContexts");
    if (contexts != null) {
      namingContexts.addAll(Arrays.asList(contexts));
    }
  }

  return namingContexts;
}

/**
* Get private naming contexts from Root DSE
*/
public List<String> getPrivateNamingContexts(String serverId) throws LDAPException {
  Entry rootDSE = getRootDSE(serverId);
  List<String> privateNamingContexts = new ArrayList<>();

  if (rootDSE != null) {
    String[] contexts = rootDSE.getAttributeValues("ds-private-naming-contexts");
    if (contexts != null) {
      privateNamingContexts.addAll(Arrays.asList(contexts));
    }
  }

  return privateNamingContexts;
}

/**
* Load Root DSE and naming contexts for browsing
*/
public List<LdapEntry> loadRootDSEWithNamingContexts(String serverId) throws LDAPException {
  return loadRootDSEWithNamingContexts(serverId, false);
}

/**
* Load Root DSE and naming contexts for browsing with optional private naming contexts
*/
public List<LdapEntry> loadRootDSEWithNamingContexts(String serverId, boolean includePrivateNamingContexts) throws LDAPException {
  List<LdapEntry> entries = new ArrayList<>();

  // Add Root DSE entry - get all attributes for Root DSE as it contains important server info
  Entry rootDSE = getRootDSE(serverId);
  if (rootDSE != null) {
    LdapEntry rootEntry = new LdapEntry(rootDSE);
    rootEntry.setDn(""); // Root DSE has empty DN
    rootEntry.setRdn("Root DSE");
    rootEntry.setHasChildren(false);
    entries.add(rootEntry);
  }

  // Add naming contexts as separate root entries
  List<String> namingContexts = getNamingContexts(serverId);
  for (String context : namingContexts) {
    try {
      // Optimize: Use minimal attributes for naming context entries in tree view
      LdapEntry contextEntry = getEntryMinimal(serverId, context);
      if (contextEntry != null) {
        contextEntry.setHasChildren(true);
        entries.add(contextEntry);
      }
    } catch (LDAPException e) {
    // If we can't browse a naming context, still add it as an entry
    LdapEntry contextEntry = new LdapEntry();
    contextEntry.setDn(context);
    contextEntry.setRdn(context);
    contextEntry.setHasChildren(true);
    contextEntry.addAttribute("objectClass", "organizationalUnit");
    entries.add(contextEntry);
  }
}

// Add private naming contexts if requested
if (includePrivateNamingContexts) {
  List<String> privateNamingContexts = getPrivateNamingContexts(serverId);
  for (String context : privateNamingContexts) {
    try {
      // Optimize: Use minimal attributes for private naming context entries
      LdapEntry contextEntry = getEntryMinimal(serverId, context);
      if (contextEntry != null) {
        contextEntry.setHasChildren(true);
        entries.add(contextEntry);
      }
    } catch (LDAPException e) {
    // If we can't browse a private naming context, still add it as an entry
    LdapEntry contextEntry = new LdapEntry();
    contextEntry.setDn(context);
    contextEntry.setRdn(context);
    contextEntry.setHasChildren(true);
    contextEntry.addAttribute("objectClass", "organizationalUnit");
    entries.add(contextEntry);
  }
}
}

return entries;
}

private LDAPConnection createConnection(LdapServerConfig config) throws LDAPException {
  LDAPConnection connection;

  if (config.isUseSSL()) {
    // Create SSL connection
    try {
      SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
      SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
      connection = new LDAPConnection(socketFactory, config.getHost(), config.getPort());
    } catch (Exception e) {
    throw new LDAPException(ResultCode.CONNECT_ERROR, "Failed to create SSL connection", e);
  }
} else {
connection = new LDAPConnection(config.getHost(), config.getPort());
}

// Use StartTLS if configured
if (config.isUseStartTLS() && !config.isUseSSL()) {
  try {
    SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
    SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
    ExtendedResult startTLSResult = connection.processExtendedOperation(
    new StartTLSExtendedRequest(socketFactory));
    if (startTLSResult.getResultCode() != ResultCode.SUCCESS) {
      throw new LDAPException(startTLSResult.getResultCode(),
      "StartTLS failed: " + startTLSResult.getDiagnosticMessage());
    }
  } catch (Exception e) {
  connection.close();
  throw new LDAPException(ResultCode.CONNECT_ERROR, "Failed to establish StartTLS", e);
}
}

// Bind to the directory
if (config.getBindDn() != null && !config.getBindDn().trim().isEmpty()) {
  BindRequest bindRequest = new SimpleBindRequest(config.getBindDn(), config.getPassword());
  BindResult bindResult = connection.bind(bindRequest);
  if (bindResult.getResultCode() != ResultCode.SUCCESS) {
    connection.close();
    throw new LDAPException(bindResult.getResultCode(),
    "Bind failed: " + bindResult.getDiagnosticMessage());
  }
}

return connection;
}

private LDAPConnection getConnection(String serverId) throws LDAPException {
  LDAPConnection connection = connections.get(serverId);
  if (connection == null || !connection.isConnected()) {
    throw new LDAPException(ResultCode.SERVER_DOWN, "Not connected to server: " + serverId);
  }
  return connection;
}

private boolean hasChildren(LDAPConnection connection, String dn) {
  try {
    // Optimize: Only check existence of children, no attributes needed
    SearchRequest searchRequest = new SearchRequest(
    dn,
    SearchScope.ONE,
    Filter.createPresenceFilter("objectClass"),
    "1.1" // Request no attributes, we only need to know if entries exist
    );
    searchRequest.setSizeLimit(1);
    searchRequest.setTimeLimitSeconds(5); // Add timeout to prevent hanging

    SearchResult result = connection.search(searchRequest);
    boolean hasChildren = result.getEntryCount() > 0;
    return hasChildren;
  } catch (LDAPException e) {
  // SIZE_LIMIT_EXCEEDED (ResultCode 4) indicates children exist but server is limiting response
  if (e.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
    // If size limit exceeded, that means there ARE children - return true immediately
    return true;
  }
  // For other LDAP exceptions, fall through to fallback logic
}

// Fallback logic: If we can't determine, assume it might have children for organizational units and containers
// This ensures expanders are shown even if there's a permission issue
try {
  // Optimize: Only request objectClass attribute for fallback logic
  SearchRequest entryRequest = new SearchRequest(
  dn,
  SearchScope.BASE,
  Filter.createPresenceFilter("objectClass"),
  "objectClass" // Only need objectClass for determining container types
  );
  SearchResult entryResult = connection.search(entryRequest);
  if (entryResult.getEntryCount() > 0) {
    SearchResultEntry entry = entryResult.getSearchEntries().get(0);
    String[] objectClasses = entry.getAttributeValues("objectClass");
    if (objectClasses != null) {
      for (String oc : objectClasses) {
        String lowerOc = oc.toLowerCase();
        // Assume these types typically have children - be more specific with organizationalUnit
        if (lowerOc.equals("organizationalunit") ||
        lowerOc.contains("organizationalunit") ||
        lowerOc.equals("organization") ||
        lowerOc.contains("organization") ||
        lowerOc.contains("container") ||
        lowerOc.contains("domain") ||
        lowerOc.contains("dcobject") ||
        lowerOc.contains("builtindomain") ||
        dn.toLowerCase().startsWith("ou=")) { // Also check DN pattern
          return true;
        }
      }
    }

    // Additional fallback: if DN starts with "ou=" it's likely an organizational unit
    if (dn.toLowerCase().startsWith("ou=")) {
      return true;
    }
  }
} catch (LDAPException ignored) {
// If we still can't determine, check DN pattern as final fallback
if (dn.toLowerCase().startsWith("ou=")) {
  return true;
}
}
return false;
}
}