package com.ldapweb.ldapbrowser.service;

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
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.unboundid.asn1.ASN1OctetString;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service for LDAP operations using UnboundID SDK
 */
@Service
public class LdapService {

  private final Map<String, LDAPConnection> connections = new HashMap<>();
  private final Map<String, String> inMemoryPasswords = new HashMap<>(); // Store session passwords for prompt-enabled servers
  private final Map<String, byte[]> pagingCookies = new HashMap<>(); // Store paging cookies for LDAP paged search
  private final Map<String, Integer> currentPages = new HashMap<>(); // Track current page for each search context
  private final LoggingService loggingService;
  private PasswordPromptCallback passwordPromptCallback;

  public LdapService(LoggingService loggingService) {
    this.loggingService = loggingService;
  }

  /**
   * Sets the callback for prompting passwords when needed.
   * @param callback the password prompt callback
   */
  public void setPasswordPromptCallback(PasswordPromptCallback callback) {
    this.passwordPromptCallback = callback;
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
   * Test LDAP bind authentication with provided DN and password
   * 
   * @param serverId the server ID to test bind against
   * @param bindDn the DN to bind as
   * @param password the password for the bind DN
   * @return TestBindResult containing success status and message
   */
  public TestBindResult testBind(String serverId, String bindDn, String password) {
    try {
      LDAPConnection connection = getConnection(serverId);
      
      // Create a temporary connection for testing the bind
      String host = connection.getConnectedAddress();
      int port = connection.getConnectedPort();
      boolean useSSL = connection.getSSLSession() != null;
      
      // Create a temporary connection with the test credentials
      LDAPConnection testConnection = null;
      try {
        if (useSSL) {
          SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
          SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
          testConnection = new LDAPConnection(socketFactory, host, port);
        } else {
          testConnection = new LDAPConnection(host, port);
        }
        
        // Attempt to bind with the provided credentials
        BindRequest bindRequest = new SimpleBindRequest(bindDn, password);
        BindResult bindResult = testConnection.bind(bindRequest);
        
        if (bindResult.getResultCode() == ResultCode.SUCCESS) {
          loggingService.logInfo("TEST_BIND", "Successful test bind for DN: " + bindDn);
          return new TestBindResult(true, "Authentication successful for DN: " + bindDn);
        } else {
          loggingService.logInfo("TEST_BIND", "Failed test bind for DN: " + bindDn + " - " + bindResult.getDiagnosticMessage());
          return new TestBindResult(false, "Authentication failed: " + bindResult.getDiagnosticMessage());
        }
        
      } finally {
        if (testConnection != null && testConnection.isConnected()) {
          testConnection.close();
        }
      }
      
    } catch (LDAPException e) {
      loggingService.logError("TEST_BIND", "Test bind error for DN: " + bindDn + " - " + e.getMessage());
      return new TestBindResult(false, "Authentication error: " + e.getMessage());
    } catch (Exception e) {
      loggingService.logError("TEST_BIND", "Unexpected error during test bind for DN: " + bindDn + " - " + e.getMessage());
      return new TestBindResult(false, "Unexpected error: " + e.getMessage());
    }
  }

  /**
   * Result class for test bind operations
   */
  public static class TestBindResult {
    private final boolean success;
    private final String message;

    public TestBindResult(boolean success, String message) {
      this.success = success;
      this.message = message;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }
  }

  /**
   * Connect to LDAP server
   */
  public void connect(LdapServerConfig config) throws LDAPException {
    try {
      loggingService.logInfo("CONNECTION",
          "Attempting to connect to " + config.getName() + " (" + config.getHost() + ":" + config.getPort() + ")");
      
      // Check if this server requires password prompting
      if (config.isPromptForPassword() && 
          config.getBindDn() != null && !config.getBindDn().trim().isEmpty()) {
        throw new LDAPException(ResultCode.AUTH_METHOD_NOT_SUPPORTED,
            "Password required for authentication. Use connectAsync for servers with password prompting enabled.");
      }
      
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
   * Connect to LDAP server with password prompting support.
   * This method will prompt for password if the server configuration requires it.
   * 
   * @param config the server configuration
   * @param onSuccess callback called on successful connection
   * @param onError callback called on connection failure
   */
  public void connectWithPrompt(LdapServerConfig config, Runnable onSuccess, java.util.function.Consumer<String> onError) {
    try {
      loggingService.logInfo("CONNECTION",
          "Attempting to connect to " + config.getName() + " (" + config.getHost() + ":" + config.getPort() + ")");

      if (config.isPromptForPassword() && (config.getPassword() == null || config.getPassword().trim().isEmpty())) {
        // Need to prompt for password
        if (passwordPromptCallback == null) {
          onError.accept("Password prompting not supported in this context");
          return;
        }

        // Check if we already have a password in memory for this server
        String inMemoryPassword = inMemoryPasswords.get(config.getId());
        if (inMemoryPassword != null) {
          // Use the in-memory password
          connectWithPassword(config, inMemoryPassword, onSuccess, onError);
        } else {
          // Prompt for password
          passwordPromptCallback.promptForPassword(config.getName(), config.getId())
              .thenAccept(password -> {
                if (password != null) {
                  // Store password in memory for this session
                  inMemoryPasswords.put(config.getId(), password);
                  connectWithPassword(config, password, onSuccess, onError);
                } else {
                  onError.accept("Password prompt was cancelled");
                }
              })
              .exceptionally(ex -> {
                onError.accept("Error prompting for password: " + ex.getMessage());
                return null;
              });
        }
      } else {
        // Use configured password or anonymous bind
        connectWithPassword(config, config.getPassword(), onSuccess, onError);
      }
    } catch (Exception e) {
      loggingService.logConnectionError(config.getName(), "Connection failed", e.getMessage());
      onError.accept(e.getMessage());
    }
  }

  private void connectWithPassword(LdapServerConfig config, String password, Runnable onSuccess, Consumer<String> onError) {
    try {
      LDAPConnection connection = createConnectionWithPassword(config, password);
      connections.put(config.getId(), connection);
      config.setConnection(connection);
      loggingService.logConnection(config.getName(), "Successfully connected");
      if (onSuccess != null) {
        onSuccess.run();
      }
    } catch (LDAPException e) {
      loggingService.logConnectionError(config.getName(), "Connection failed", e.getMessage());
      onError.accept(e.getMessage());
    }
  }

  /**
   * Disconnect from LDAP server
   */
  public void disconnect(String serverId) {
    LDAPConnection connection = connections.remove(serverId);
    // Also remove any in-memory password for this server
    inMemoryPasswords.remove(serverId);
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
      // Set hasChildren based on object classes/DN patterns to show expanders by
      // default
      // Actual children check will be done lazily when expander is clicked
      ldapEntry.setHasChildren(shouldShowExpanderForEntry(ldapEntry));
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
  public BrowseResult browseEntriesWithMetadata(String serverId, String baseDn, int page, int pageSize)
      throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    // Create a unique key for this search context
    String searchKey = serverId + ":" + baseDn;

    try {
      List<LdapEntry> allEntries = new ArrayList<>();
      boolean hasMorePages = false;
      boolean sizeLimitExceeded = false;

      // Get current stored page and cookie for this search context
      Integer storedPage = currentPages.get(searchKey);
      byte[] cookie = pagingCookies.get(searchKey);

      // Handle different navigation scenarios
      if (page == 0) {
        // First page - start fresh
        pagingCookies.remove(searchKey);
        currentPages.put(searchKey, 0);
        cookie = null;
      } else if (storedPage == null || storedPage != page - 1) {
        // We don't have the right cookie position - need to iterate from beginning
        // This happens when jumping to arbitrary pages or after a refresh
        return browseEntriesWithPagingIteration(serverId, baseDn, page, pageSize);
      }
      // else: we have the right cookie for sequential navigation (page = storedPage +
      // 1)

      // Create the paged search control
      SimplePagedResultsControl pagedControl;
      if (cookie != null) {
        pagedControl = new SimplePagedResultsControl(pageSize, new ASN1OctetString(cookie), false);
      } else {
        pagedControl = new SimplePagedResultsControl(pageSize, false);
      }

      // Optimize: Only request essential attributes for browsing
      SearchRequest searchRequest = new SearchRequest(
          baseDn,
          SearchScope.ONE,
          Filter.createPresenceFilter("objectClass"),
          "objectClass", "cn", "ou", "dc" // Only essential attributes for display
      );

      // Add the paged results control
      searchRequest.addControl(pagedControl);

      // Don't set a size limit when using paged search controls
      // The page size in the control handles this

      SearchResult searchResult = connection.search(searchRequest);

      // Extract entries from this page
      for (SearchResultEntry entry : searchResult.getSearchEntries()) {
        LdapEntry ldapEntry = new LdapEntry(entry);
        // Set hasChildren based on object classes/DN patterns to show expanders by
        // default
        // Actual children check will be done lazily when expander is clicked
        ldapEntry.setHasChildren(shouldShowExpanderForEntry(ldapEntry));
        allEntries.add(ldapEntry);
      }

      // Sort entries by display name
      allEntries.sort(Comparator.comparing(LdapEntry::getDisplayName));

      // Check for paged results response control to get the cookie for next page
      SimplePagedResultsControl responseControl = null;
      for (Control control : searchResult.getResponseControls()) {
        if (control instanceof SimplePagedResultsControl) {
          responseControl = (SimplePagedResultsControl) control;
          break;
        }
      }

      if (responseControl != null) {
        ASN1OctetString cookieOctetString = responseControl.getCookie();
        if (cookieOctetString != null && cookieOctetString.getValueLength() > 0) {
          // Store cookie for next page
          byte[] nextCookie = cookieOctetString.getValue();
          pagingCookies.put(searchKey, nextCookie);
          currentPages.put(searchKey, page);
          hasMorePages = true;
        } else {
          // No more pages
          pagingCookies.remove(searchKey);
          currentPages.remove(searchKey);
          hasMorePages = false;
        }
      }

      // Calculate pagination info
      boolean hasPrevPage = page > 0;
      boolean hasNextPage = hasMorePages;

      return new BrowseResult(allEntries, sizeLimitExceeded, -1, page, pageSize, hasNextPage, hasPrevPage);

    } catch (LDAPSearchException e) {
      // Handle size limit exceeded or other search errors
      if (e.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
        // Fallback to partial results
        List<LdapEntry> partialEntries = new ArrayList<>();
        for (SearchResultEntry entry : e.getSearchEntries()) {
          LdapEntry ldapEntry = new LdapEntry(entry);
          ldapEntry.setHasChildren(shouldShowExpanderForEntry(ldapEntry));
          partialEntries.add(ldapEntry);
        }

        partialEntries.sort(Comparator.comparing(LdapEntry::getDisplayName));

        return new BrowseResult(partialEntries, true, -1, page, pageSize, true, page > 0);
      } else {
        // Re-throw other types of LDAP exceptions
        throw e;
      }
    }
  }

  /**
   * Helper method to iterate through pages when jumping to a specific page
   * This is necessary because LDAP paged search doesn't support jumping to
   * arbitrary pages
   */
  private BrowseResult browseEntriesWithPagingIteration(String serverId, String baseDn, int targetPage, int pageSize)
      throws LDAPException {
    // Clear any existing state and start from page 0
    String searchKey = serverId + ":" + baseDn;
    pagingCookies.remove(searchKey);
    currentPages.remove(searchKey);

    // Iterate through pages until we reach the target page
    boolean hasMorePages = true;
    BrowseResult lastResult = null;

    for (int currentPage = 0; currentPage <= targetPage && hasMorePages; currentPage++) {
      lastResult = browseEntriesWithMetadata(serverId, baseDn, currentPage, pageSize);

      if (currentPage == targetPage) {
        // This is our target page
        return lastResult;
      }

      hasMorePages = lastResult.hasNextPage();
    }

    // If we reach here, the target page doesn't exist
    return new BrowseResult(new ArrayList<>(), false, -1, targetPage, pageSize, false, targetPage > 0);
  }

  /**
   * Clear all paging cookies for a specific server
   */
  public void clearPagingState(String serverId) {
    pagingCookies.entrySet().removeIf(entry -> entry.getKey().startsWith(serverId + ":"));
    currentPages.entrySet().removeIf(entry -> entry.getKey().startsWith(serverId + ":"));
  }

  /**
   * Clear paging cookies for a specific search context
   */
  public void clearPagingState(String serverId, String baseDn) {
    String searchKey = serverId + ":" + baseDn;
    pagingCookies.remove(searchKey);
    currentPages.remove(searchKey);
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

    public List<LdapEntry> getEntries() {
      return entries;
    }

    public boolean isSizeLimitExceeded() {
      return sizeLimitExceeded;
    }

    public int getEntryCount() {
      return entryCount;
    }

    public int getCurrentPage() {
      return currentPage;
    }

    public int getPageSize() {
      return pageSize;
    }

    public boolean hasNextPage() {
      return hasNextPage;
    }

    public boolean hasPrevPage() {
      return hasPrevPage;
    }

    public int getTotalPages() {
      if (entryCount < 0)
        return -1; // Unknown total
      return (int) Math.ceil((double) entryCount / pageSize);
    }
  }

  /**
   * Search LDAP entries
   */
  public List<LdapEntry> searchEntries(String serverId, String baseDn, String filter, SearchScope scope)
      throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      loggingService.logDebug("SEARCH",
          "Starting search - Server: " + serverId + ", Base: " + baseDn + ", Filter: " + filter);

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
  public List<LdapEntry> searchEntries(String serverId, String baseDn, String filter, SearchScope scope,
      String... attributes) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      String attrsList = attributes.length > 0 ? String.join(",", attributes) : "all";
      loggingService.logDebug("SEARCH", "Starting search with attributes - Server: " + serverId + ", Base: " + baseDn
          + ", Filter: " + filter + ", Attributes: " + attrsList);

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
   * Get a specific LDAP entry by DN - returns all attributes for entry details
   * view
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
   * Result class for optimized entry details fetching that includes both entry
   * and schema
   */
  public static class EntryWithSchema {
    private final LdapEntry entry;
    private final Schema schema;

    public EntryWithSchema(LdapEntry entry, Schema schema) {
      this.entry = entry;
      this.schema = schema;
    }

    public LdapEntry getEntry() {
      return entry;
    }

    public Schema getSchema() {
      return schema;
    }
  }

  /**
   * Optimized method to get entry details with schema information in a single
   * optimized call.
   * This reduces LDAP queries when displaying entry details by fetching both
   * entry and schema
   * information together, instead of making separate calls for each attribute
   * classification.
   */
  public EntryWithSchema getEntryWithSchema(String serverId, String dn) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    // Fetch both entry and schema in an optimized way
    // First get the entry with all attributes
    SearchRequest searchRequest = new SearchRequest(
        dn,
        SearchScope.BASE,
        Filter.createPresenceFilter("objectClass"),
        "*", "+" // Request all user attributes (*) and operational attributes (+)
    );

    SearchResult searchResult = connection.search(searchRequest);

    if (searchResult.getEntryCount() == 0) {
      return null;
    }

    LdapEntry ldapEntry = new LdapEntry(searchResult.getSearchEntries().get(0));

    // Get schema information (may include extended info when supported)
    Schema schema = getSchema(serverId);

    return new EntryWithSchema(ldapEntry, schema);
  }

  /**
   * Get only DN of entries matching filter - optimized for bulk operations
   */
  public List<String> getDNsOnly(String serverId, String baseDn, String filter, SearchScope scope)
      throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      loggingService.logDebug("SEARCH",
          "DN-only search - Server: " + serverId + ", Base: " + baseDn + ", Filter: " + filter);

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
  public void modifyEntry(String serverId, String dn, List<Modification> modifications, List<Control> controls)
      throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      loggingService.logDebug("MODIFY",
          "Modifying entry - Server: " + serverId + ", DN: " + dn + ", Modifications: " + modifications.size() +
              (controls != null ? ", Controls: " + controls.size() : ""));

      // Log detailed modification debug information when debug capture is enabled
      if (loggingService.isDebugCaptureEnabled()) {
        for (Modification mod : modifications) {
          String operation = mod.getModificationType().toString();
          String attributeName = mod.getAttributeName();
          String[] newValues = mod.getValues();
          
          if (newValues != null && newValues.length > 0) {
            loggingService.logModificationDebug("Server " + serverId, dn, operation, 
                attributeName, null, String.join(", ", newValues));
          } else {
            loggingService.logModificationDebug("Server " + serverId, dn, operation, 
                attributeName, null, null);
          }
        }
      }

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
      
      // Log detailed add debug information when debug capture is enabled
      if (loggingService.isDebugCaptureEnabled()) {
        StringBuilder details = new StringBuilder();
        details.append("DN: ").append(entry.getDn()).append("\n");
        details.append("Attributes:\n");
        for (Map.Entry<String, List<String>> attr : entry.getAttributes().entrySet()) {
          details.append("  ").append(attr.getKey()).append(": ").append(String.join(", ", attr.getValue())).append("\n");
        }
        loggingService.logDebug("MODIFY", "ADD operation on Server " + serverId, details.toString());
      }
      
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
      
      // Log detailed delete debug information when debug capture is enabled
      if (loggingService.isDebugCaptureEnabled()) {
        loggingService.logModificationDebug("Server " + serverId, dn, "DELETE", "entry", null, null);
      }
      
      DeleteRequest deleteRequest = new DeleteRequest(dn);
      connection.delete(deleteRequest);
      loggingService.logModification("Server " + serverId, dn, "DELETE");
    } catch (LDAPException e) {
      loggingService.logModificationError("Server " + serverId, dn, "DELETE", e.getMessage());
      throw e;
    }
  }

  /**
   * Add a value to an attribute of an LDAP entry.
   *
   * @param serverId the server ID
   * @param dn the distinguished name of the entry
   * @param attributeName the name of the attribute
   * @param attributeValue the value to add
   * @throws LDAPException if the operation fails
   */
  public void addAttributeValue(String serverId, String dn, String attributeName, String attributeValue) 
      throws LDAPException {
    try {
      loggingService.logDebug("MODIFY", 
          "Adding attribute value - Server: " + serverId + ", DN: " + dn + 
          ", Attribute: " + attributeName + ", Value: [value]");
      
      // Create modification to add the attribute value
      Modification modification = new Modification(ModificationType.ADD, attributeName, attributeValue);
      List<Modification> modifications = List.of(modification);
      
      modifyEntry(serverId, dn, modifications);
      
    } catch (LDAPException e) {
      loggingService.logModificationError("Server " + serverId, dn, 
          "ADD_ATTRIBUTE_VALUE (" + attributeName + ")", e.getMessage());
      throw e;
    }
  }

  /**
   * Get LDAP schema information
   */
  public Schema getSchema(String serverId) throws LDAPException {
    // Default behavior: prefer extended schema info when available per-server
    return getSchema(serverId, true);
  }

  /**
   * Get LDAP schema information with optional extended schema info control.
   * When useExtended is false, the request will not include the extended schema
   * info request control even if the server supports it.
   */
  public Schema getSchema(String serverId, boolean useExtended) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);
    // Try to request extended schema info (e.g., X-Schema-file) when the server
    // supports it
    final String EXTENDED_SCHEMA_INFO_OID = "1.3.6.1.4.1.30221.2.5.12";
    try {
      String schemaDN = getSchemaSubentryDN(serverId);
      if (schemaDN != null && !schemaDN.isEmpty()) {
        SearchRequest req = new SearchRequest(
            schemaDN,
            SearchScope.BASE,
            Filter.createPresenceFilter("objectClass"),
            // Request all common schema attributes
            "attributeTypes", "objectClasses", "ldapSyntaxes", "matchingRules",
            "matchingRuleUse", "dITContentRules", "nameForms", "dITStructureRules");
        try {
          if (useExtended && isControlSupported(serverId, EXTENDED_SCHEMA_INFO_OID)) {
            // Non-critical control: server may ignore if unsupported
            req.addControl(new Control(EXTENDED_SCHEMA_INFO_OID, false));
          }
        } catch (Exception ignored) {
        }

        SearchResult sr = connection.search(req);
        if (sr.getEntryCount() > 0) {
          SearchResultEntry entry = sr.getSearchEntries().get(0);
          try {
            // Build Schema directly from the schema subentry
            return new Schema(entry);
          } catch (Throwable t) {
            // Last resort fallback below
          }
        }
      }
    } catch (LDAPException e) {
      // Fall back to standard retrieval on error
    }
    return connection.getSchema();
  }

  /**
   * Add an object class to the schema of an external LDAP server
   */
  public void addObjectClassToSchema(String serverId, String objectClassDefinition) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      // Get the schema subentry DN from root DSE
      String schemaDN = getSchemaSubentryDN(serverId);
      if (schemaDN == null) {
        throw new LDAPException(ResultCode.NO_SUCH_OBJECT, "Cannot determine schema subentry DN");
      }

      // Create modification to add the object class
      Modification modification = new Modification(ModificationType.ADD, "objectClasses", objectClassDefinition);

      // Apply the modification
      ModifyRequest modifyRequest = new ModifyRequest(schemaDN, modification);
      connection.modify(modifyRequest);

      loggingService.logModification("Server " + serverId, schemaDN, "ADD_OBJECT_CLASS");
    } catch (LDAPException e) {
      loggingService.logModificationError("Server " + serverId, "schema", "ADD_OBJECT_CLASS", e.getMessage());
      throw e;
    }
  }

  /**
   * Add an attribute type to the schema of an external LDAP server
   */
  public void addAttributeTypeToSchema(String serverId, String attributeTypeDefinition) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      // Get the schema subentry DN from root DSE
      String schemaDN = getSchemaSubentryDN(serverId);
      if (schemaDN == null) {
        throw new LDAPException(ResultCode.NO_SUCH_OBJECT, "Cannot determine schema subentry DN");
      }

      // Create modification to add the attribute type
      Modification modification = new Modification(ModificationType.ADD, "attributeTypes", attributeTypeDefinition);

      // Apply the modification
      ModifyRequest modifyRequest = new ModifyRequest(schemaDN, modification);
      connection.modify(modifyRequest);

      loggingService.logModification("Server " + serverId, schemaDN, "ADD_ATTRIBUTE_TYPE");
    } catch (LDAPException e) {
      loggingService.logModificationError("Server " + serverId, "schema", "ADD_ATTRIBUTE_TYPE", e.getMessage());
      throw e;
    }
  }

  /**
   * Get the schema subentry DN from the root DSE
   */
  private String getSchemaSubentryDN(String serverId) throws LDAPException {
    Entry rootDSE = getRootDSE(serverId);

    if (rootDSE != null) {
      // Try common attributes for schema subentry
      String[] possibleAttributes = { "subschemaSubentry", "schemaNamingContext", "schemaSubentry" };

      for (String attr : possibleAttributes) {
        String schemaDN = rootDSE.getAttributeValue(attr);
        if (schemaDN != null && !schemaDN.trim().isEmpty()) {
          return schemaDN;
        }
      }
    }

    // Fallback to common default
    return "cn=schema";
  }

  /**
   * Check if the server supports schema modifications
   */
  public boolean supportsSchemaModification(String serverId) {
    try {
      Entry rootDSE = getRootDSE(serverId);
      if (rootDSE != null) {
        // Check for schema modification support indicators
        String[] supportedFeatures = rootDSE.getAttributeValues("supportedFeatures");
        if (supportedFeatures != null) {
          for (String feature : supportedFeatures) {
            // Check for various schema modification OIDs
            if ("1.3.6.1.4.1.4203.1.5.1".equals(feature) || // All Operational Attributes
                "1.3.6.1.4.1.42.2.27.9.5.4".equals(feature)) { // Sun DS Schema Modification
              return true;
            }
          }
        }

        // Check if schema subentry exists and is writable
        String schemaDN = getSchemaSubentryDN(serverId);
        if (schemaDN != null) {
          try {
            LdapEntry schemaEntry = getEntry(serverId, schemaDN);
            return schemaEntry != null;
          } catch (LDAPException e) {
            // Schema entry not accessible
            return false;
          }
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Modify an existing object class in the schema of an external LDAP server
   */
  public void modifyObjectClassInSchema(String serverId, String oldObjectClassDefinition, String newObjectClassDefinition) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      // Get the schema subentry DN from root DSE
      String schemaDN = getSchemaSubentryDN(serverId);
      if (schemaDN == null) {
        throw new LDAPException(ResultCode.NO_SUCH_OBJECT, "Cannot determine schema subentry DN");
      }

      // Log debug information if debug capture is enabled
      loggingService.logSchemaModificationDebug("Server " + serverId, schemaDN, "MODIFY_OBJECT_CLASS", 
          "objectClass", oldObjectClassDefinition, newObjectClassDefinition);

      // Create modifications to delete the old object class and add the new one
      Modification deleteModification = new Modification(ModificationType.DELETE, "objectClasses", oldObjectClassDefinition);
      Modification addModification = new Modification(ModificationType.ADD, "objectClasses", newObjectClassDefinition);

      // Apply both modifications in a single operation
      ModifyRequest modifyRequest = new ModifyRequest(schemaDN, deleteModification, addModification);
      connection.modify(modifyRequest);

      loggingService.logModification("Server " + serverId, schemaDN, "MODIFY_OBJECT_CLASS");
    } catch (LDAPException e) {
      loggingService.logModificationError("Server " + serverId, "schema", "MODIFY_OBJECT_CLASS", e.getMessage());
      throw e;
    }
  }

  /**
   * Modify an existing attribute type in the schema of an external LDAP server
   */
  public void modifyAttributeTypeInSchema(String serverId, String oldAttributeTypeDefinition, String newAttributeTypeDefinition) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);

    try {
      // Get the schema subentry DN from root DSE
      String schemaDN = getSchemaSubentryDN(serverId);
      if (schemaDN == null) {
        throw new LDAPException(ResultCode.NO_SUCH_OBJECT, "Cannot determine schema subentry DN");
      }

      // Log debug information if debug capture is enabled
      loggingService.logSchemaModificationDebug("Server " + serverId, schemaDN, "MODIFY_ATTRIBUTE_TYPE", 
          "attributeType", oldAttributeTypeDefinition, newAttributeTypeDefinition);

      // Create modifications to delete the old attribute type and add the new one
      Modification deleteModification = new Modification(ModificationType.DELETE, "attributeTypes", oldAttributeTypeDefinition);
      Modification addModification = new Modification(ModificationType.ADD, "attributeTypes", newAttributeTypeDefinition);

      // Apply both modifications in a single operation
      ModifyRequest modifyRequest = new ModifyRequest(schemaDN, deleteModification, addModification);
      connection.modify(modifyRequest);

      loggingService.logModification("Server " + serverId, schemaDN, "MODIFY_ATTRIBUTE_TYPE");
    } catch (LDAPException e) {
      loggingService.logModificationError("Server " + serverId, "schema", "MODIFY_ATTRIBUTE_TYPE", e.getMessage());
      throw e;
    }
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
   * Load Root DSE and naming contexts for browsing with optional private naming
   * contexts
   */
  public List<LdapEntry> loadRootDSEWithNamingContexts(String serverId, boolean includePrivateNamingContexts)
      throws LDAPException {
    List<LdapEntry> entries = new ArrayList<>();

    // Add Root DSE entry - get all attributes for Root DSE as it contains important
    // server info
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
      // Check if we should prompt for password
      if (config.isPromptForPassword()) {
        // Check if we have an in-memory password for this server
        String storedPassword = inMemoryPasswords.get(config.getId());
        if (storedPassword == null) {
          // No password available and prompting is enabled - attempt anonymous bind or prompt
          if (passwordPromptCallback != null) {
            // We have a callback, but we can't use it in the synchronous createConnection method
            // This should be handled by the calling code using connectWithPrompt instead
            connection.close();
            throw new LDAPException(ResultCode.AUTH_METHOD_NOT_SUPPORTED,
                "Password required for authentication. Use connectWithPrompt for servers with password prompting enabled.");
          } else {
            // No callback available, attempt anonymous bind
            // Don't perform any bind operation for anonymous access
          }
        } else {
          // Use the stored in-memory password
          BindRequest bindRequest = new SimpleBindRequest(config.getBindDn(), storedPassword);
          BindResult bindResult = connection.bind(bindRequest);
          if (bindResult.getResultCode() != ResultCode.SUCCESS) {
            connection.close();
            throw new LDAPException(bindResult.getResultCode(),
                "Bind failed: " + bindResult.getDiagnosticMessage());
          }
        }
      } else {
        // Normal password-based authentication
        String password = config.getPassword();
        if (password == null || password.isEmpty()) {
          // No password provided for bind DN - attempt anonymous bind
          // Don't perform any bind operation for anonymous access
        } else {
          BindRequest bindRequest = new SimpleBindRequest(config.getBindDn(), password);
          BindResult bindResult = connection.bind(bindRequest);
          if (bindResult.getResultCode() != ResultCode.SUCCESS) {
            connection.close();
            throw new LDAPException(bindResult.getResultCode(),
                "Bind failed: " + bindResult.getDiagnosticMessage());
          }
        }
      }
    }

    return connection;
  }

  private LDAPConnection createConnectionWithPassword(LdapServerConfig config, String password) throws LDAPException {
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
      // We have a bind DN - we need a password too
      if (password == null || password.trim().isEmpty()) {
        connection.close();
        throw new LDAPException(ResultCode.INVALID_CREDENTIALS, 
            "Simple bind operations are not allowed to contain a bind DN without a password");
      }
      
      BindRequest bindRequest = new SimpleBindRequest(config.getBindDn(), password);
      BindResult bindResult = connection.bind(bindRequest);
      if (bindResult.getResultCode() != ResultCode.SUCCESS) {
        connection.close();
        throw new LDAPException(bindResult.getResultCode(),
            "Bind failed: " + bindResult.getDiagnosticMessage());
      }
    }
    // If no bind DN is specified, use anonymous connection (no bind needed)

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
      // SIZE_LIMIT_EXCEEDED (ResultCode 4) indicates children exist but server is
      // limiting response
      if (e.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
        // If size limit exceeded, that means there ARE children - return true
        // immediately
        return true;
      }
      // For other LDAP exceptions, fall through to fallback logic
    }

    // Fallback logic: If we can't determine, assume it might have children for
    // organizational units and containers
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
            // Assume these types typically have children - be more specific with
            // organizationalUnit
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

        // Additional fallback: if DN starts with "ou=" it's likely an organizational
        // unit
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

  /**
   * Determine if an entry should show an expander based on its object classes and
   * DN pattern
   * This is used for the initial display without doing an LDAP search
   */
  private boolean shouldShowExpanderForEntry(LdapEntry entry) {
    // Check DN pattern first - if it starts with "ou=" it's likely an
    // organizational unit
    String dn = entry.getDn();
    if (dn != null && dn.toLowerCase().startsWith("ou=")) {
      return true;
    }

    // Check all object classes to determine if this entry should have an expander
    // But exclude person/user types even if they contain organizational patterns
    boolean hasPersonClass = false;
    boolean hasContainerClass = false;

    List<String> objectClasses = entry.getAttributeValues("objectClass");
    for (String oc : objectClasses) {
      String lowerOc = oc.toLowerCase();

      // Check if this is a person/user entry
      if (lowerOc.contains("person") || lowerOc.contains("user") ||
          lowerOc.contains("inetorgperson") || lowerOc.contains("posixaccount")) {
        hasPersonClass = true;
      }

      // Check if this is a container/organizational entry
      if (lowerOc.equals("organizationalunit") ||
          lowerOc.contains("organizationalunit") ||
          lowerOc.equals("organization") ||
          lowerOc.contains("organization") ||
          lowerOc.contains("container") ||
          lowerOc.contains("domain") ||
          lowerOc.contains("dcobject") ||
          lowerOc.contains("builtindomain")) {
        hasContainerClass = true;
      }
    }

    // If it's a person/user, don't show expander regardless of other classes
    if (hasPersonClass && !hasContainerClass) {
      return false;
    }

    // If it has container classes, show expander
    if (hasContainerClass) {
      return true;
    }

    return false;
  }

  /**
   * Check if an entry actually has children by performing an LDAP search
   * This is called lazily when an expander is clicked
   */
  public boolean checkHasChildren(String serverId, String dn) throws LDAPException {
    LDAPConnection connection = getConnection(serverId);
    return hasChildren(connection, dn);
  }
}