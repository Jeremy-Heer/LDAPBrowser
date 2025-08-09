package com.example.ldapbrowser.service;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocketFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for LDAP operations using UnboundID SDK
 */
@Service
public class LdapService {
    
    private final Map<String, LDAPConnection> connections = new HashMap<>();
    
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
        LDAPConnection connection = createConnection(config);
        connections.put(config.getId(), connection);
        config.setConnection(connection);
    }
    
    /**
     * Disconnect from LDAP server
     */
    public void disconnect(String serverId) {
        LDAPConnection connection = connections.remove(serverId);
        if (connection != null && connection.isConnected()) {
            connection.close();
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
        
        SearchRequest searchRequest = new SearchRequest(
            baseDn,
            SearchScope.ONE,
            Filter.createPresenceFilter("objectClass")
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
        LDAPConnection connection = getConnection(serverId);
        
        SearchRequest searchRequest = new SearchRequest(
            baseDn,
            SearchScope.ONE,
            Filter.createPresenceFilter("objectClass")
        );
        searchRequest.setSizeLimit(100); // Limit to 100 entries for performance
        
        try {
            SearchResult searchResult = connection.search(searchRequest);
            
            List<LdapEntry> entries = new ArrayList<>();
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                LdapEntry ldapEntry = new LdapEntry(entry);
                // Check if entry has children
                ldapEntry.setHasChildren(hasChildren(connection, entry.getDN()));
                entries.add(ldapEntry);
            }
            
            // Sort entries by display name
            entries.sort(Comparator.comparing(LdapEntry::getDisplayName));
            
            boolean sizeLimitExceeded = searchResult.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED;
            
            return new BrowseResult(entries, sizeLimitExceeded, entries.size());
            
        } catch (LDAPSearchException e) {
            // Handle size limit exceeded gracefully by returning partial results
            if (e.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                List<LdapEntry> entries = new ArrayList<>();
                // Get the partial results from the search exception
                for (SearchResultEntry entry : e.getSearchEntries()) {
                    LdapEntry ldapEntry = new LdapEntry(entry);
                    // Check if entry has children
                    ldapEntry.setHasChildren(hasChildren(connection, entry.getDN()));
                    entries.add(ldapEntry);
                }
                
                // Sort entries by display name
                entries.sort(Comparator.comparing(LdapEntry::getDisplayName));
                
                return new BrowseResult(entries, true, entries.size());
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
        
        public BrowseResult(List<LdapEntry> entries, boolean sizeLimitExceeded, int entryCount) {
            this.entries = entries;
            this.sizeLimitExceeded = sizeLimitExceeded;
            this.entryCount = entryCount;
        }
        
        public List<LdapEntry> getEntries() { return entries; }
        public boolean isSizeLimitExceeded() { return sizeLimitExceeded; }
        public int getEntryCount() { return entryCount; }
    }
    
    /**
     * Search LDAP entries
     */
    public List<LdapEntry> searchEntries(String serverId, String baseDn, String filter, SearchScope scope) throws LDAPException {
        LDAPConnection connection = getConnection(serverId);
        
        SearchRequest searchRequest = new SearchRequest(baseDn, scope, Filter.create(filter));
        searchRequest.setSizeLimit(1000); // Limit results to prevent overwhelming UI
        
        SearchResult searchResult = connection.search(searchRequest);
        
        List<LdapEntry> entries = new ArrayList<>();
        for (SearchResultEntry entry : searchResult.getSearchEntries()) {
            entries.add(new LdapEntry(entry));
        }
        
        return entries;
    }
    
    /**
     * Search LDAP entries with specific return attributes
     */
    public List<LdapEntry> searchEntries(String serverId, String baseDn, String filter, SearchScope scope, String... attributes) throws LDAPException {
        LDAPConnection connection = getConnection(serverId);
        
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
        
        return entries;
    }
    
    /**
     * Get a specific LDAP entry by DN
     */
    public LdapEntry getEntry(String serverId, String dn) throws LDAPException {
        LDAPConnection connection = getConnection(serverId);
        
        SearchRequest searchRequest = new SearchRequest(
            dn,
            SearchScope.BASE,
            Filter.createPresenceFilter("objectClass")
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
        LDAPConnection connection = getConnection(serverId);
        
        ModifyRequest modifyRequest = new ModifyRequest(dn, modifications);
        connection.modify(modifyRequest);
    }
    
    /**
     * Add a new LDAP entry
     */
    public void addEntry(String serverId, LdapEntry entry) throws LDAPException {
        LDAPConnection connection = getConnection(serverId);
        
        Collection<Attribute> attributes = entry.getAttributes().entrySet().stream()
            .map(attr -> new Attribute(attr.getKey(), attr.getValue()))
            .collect(Collectors.toList());
        
        AddRequest addRequest = new AddRequest(entry.getDn(), attributes);
        connection.add(addRequest);
    }
    
    /**
     * Delete an LDAP entry
     */
    public void deleteEntry(String serverId, String dn) throws LDAPException {
        LDAPConnection connection = getConnection(serverId);
        
        DeleteRequest deleteRequest = new DeleteRequest(dn);
        connection.delete(deleteRequest);
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
     * Load Root DSE and naming contexts for browsing
     */
    public List<LdapEntry> loadRootDSEWithNamingContexts(String serverId) throws LDAPException {
        List<LdapEntry> entries = new ArrayList<>();
        
        // Add Root DSE entry
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
                // Add the naming context itself as a root entry
                LdapEntry contextEntry = getEntry(serverId, context);
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
            SearchRequest searchRequest = new SearchRequest(
                dn,
                SearchScope.ONE,
                Filter.createPresenceFilter("objectClass")
            );
            searchRequest.setSizeLimit(1);
            searchRequest.setTimeLimitSeconds(5); // Add timeout to prevent hanging
            
            SearchResult result = connection.search(searchRequest);
            boolean hasChildren = result.getEntryCount() > 0;
            return hasChildren;
        } catch (LDAPException e) {
            // If we can't determine, assume it might have children for organizational units and containers
            // This ensures expanders are shown even if there's a permission issue
            try {
                SearchRequest entryRequest = new SearchRequest(
                    dn,
                    SearchScope.BASE,
                    Filter.createPresenceFilter("objectClass")
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
}
