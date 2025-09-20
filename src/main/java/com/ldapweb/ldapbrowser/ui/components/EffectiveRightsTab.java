package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.unboundidds.controls.GetEffectiveRightsRequestControl;
import com.unboundid.ldap.sdk.unboundidds.controls.EffectiveRightsEntry;
import com.unboundid.ldap.sdk.unboundidds.controls.AttributeRight;
import com.unboundid.ldap.sdk.unboundidds.controls.EntryRight;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.ldap.sdk.ExtendedResult;
import javax.net.ssl.SSLSocketFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab component for checking effective rights using GetEffectiveRightsRequestControl.
 * Allows users to search for entries and view their effective access rights.
 */
public class EffectiveRightsTab extends VerticalLayout {

  private LdapServerConfig serverConfig;
  
  // Form fields
  private TextField searchBaseField;
  private ComboBox<SearchScope> scopeComboBox;
  private TextField searchFilterField;
  private TextField attributesField;
  private TextField effectiveRightsForField;
  private TextField searchSizeLimitField;
  private Button searchButton;
  
  // Results display
  private Grid<EffectiveRightsResult> resultsGrid;
  private ProgressBar progressBar;
  private Div loadingContainer;
  private Div resultsContainer;

  /**
   * Constructs a new EffectiveRightsTab.
   *
   * @param ldapService the LDAP service (not used - we create direct connections)
   */
  public EffectiveRightsTab(LdapService ldapService) {
    initUI();
  }

  private void initUI() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    H3 title = new H3("Effective Rights");
    add(title);

    Div description = new Div();
    description.setText("Check effective access rights for entries using GetEffectiveRightsRequestControl.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    add(description);

    initSearchForm();
    initResultsSection();
  }

  private void initSearchForm() {
    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("600px", 2)
    );

    // Search Base
    searchBaseField = new TextField("Search Base");
    searchBaseField.setPlaceholder("dc=example,dc=com");
    searchBaseField.setWidthFull();

    // Search Scope
    scopeComboBox = new ComboBox<>("Search Scope");
    scopeComboBox.setItems(SearchScope.BASE, SearchScope.ONE, SearchScope.SUB);
    scopeComboBox.setValue(SearchScope.SUB);
    scopeComboBox.setItemLabelGenerator(scope -> {
      if (scope == SearchScope.BASE) return "Base";
      if (scope == SearchScope.ONE) return "One Level";
      if (scope == SearchScope.SUB) return "Subtree";
      return scope.getName();
    });

    // Search Filter
    searchFilterField = new TextField("Search Filter");
    searchFilterField.setPlaceholder("(objectClass=*)");
    searchFilterField.setValue("(objectClass=*)");
    searchFilterField.setWidthFull();

    // Attributes
    attributesField = new TextField("Attributes");
    attributesField.setPlaceholder("* (all attributes) or comma-separated list");
    attributesField.setValue("*");
    attributesField.setWidthFull();

    // Effective Rights For
    effectiveRightsForField = new TextField("Effective Rights For");
    effectiveRightsForField.setPlaceholder("Target user/entity DN (e.g., uid=admin,dc=example,dc=com)");
    effectiveRightsForField.setWidthFull();
    effectiveRightsForField.setRequired(true);
    effectiveRightsForField.setHelperText("The 'dn: ' prefix will be automatically added");
    effectiveRightsForField.setClearButtonVisible(true);

    // Search Size Limit
    searchSizeLimitField = new TextField("Search Size Limit");
    searchSizeLimitField.setPlaceholder("Maximum number of entries to return");
    searchSizeLimitField.setValue("100");
    searchSizeLimitField.setWidthFull();
    searchSizeLimitField.setHelperText("Maximum number of entries to return from LDAP search (0 = no limit)");
    searchSizeLimitField.setClearButtonVisible(true);

    // Search Button
    searchButton = new Button("Search", new Icon(VaadinIcon.SEARCH));
    searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    searchButton.addClickListener(e -> performSearch());

    formLayout.add(searchBaseField, scopeComboBox);
    formLayout.add(searchFilterField, 2);
    formLayout.add(attributesField, 2);
    formLayout.add(effectiveRightsForField, searchSizeLimitField);

    HorizontalLayout buttonLayout = new HorizontalLayout(searchButton);
    buttonLayout.setJustifyContentMode(JustifyContentMode.END);

    add(formLayout, buttonLayout);
  }

  private void initResultsSection() {
    resultsContainer = new Div();
    resultsContainer.setSizeFull();
    resultsContainer.getStyle().set("display", "flex");
    resultsContainer.getStyle().set("flex-direction", "column");
    resultsContainer.getStyle().set("gap", "10px");
    
    // Create loading indicator
    progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    
    loadingContainer = new Div();
    loadingContainer.add(progressBar, new Div("Searching for effective rights..."));
    loadingContainer.getStyle().set("text-align", "center");
    loadingContainer.getStyle().set("padding", "20px");
    loadingContainer.setVisible(false);
    resultsContainer.add(loadingContainer);

    // Results grid
    resultsGrid = new Grid<>(EffectiveRightsResult.class, false);
    
    // Configure columns explicitly with better sizing
    resultsGrid.addColumn(result -> result.getDn())
        .setHeader("Entry DN")
        .setWidth("300px")
        .setFlexGrow(0)
        .setSortable(false)
        .setResizable(true);
    
    resultsGrid.addColumn(result -> result.getAttributeRights())
        .setHeader("Attribute Rights")
        .setAutoWidth(false)
        .setFlexGrow(1)
        .setSortable(false)
        .setResizable(true);
    
    resultsGrid.addColumn(result -> result.getEntryRights())
        .setHeader("Entry Rights")
        .setWidth("200px")
        .setFlexGrow(0)
        .setSortable(false)
        .setResizable(true);

    // Set grid properties for better visibility
    resultsGrid.setWidthFull();
    resultsGrid.setHeight("500px");
    resultsGrid.setVisible(false);
    resultsGrid.setMultiSort(false);
    
    resultsContainer.add(resultsGrid);

    add(resultsContainer);
    setFlexGrow(1, resultsContainer);
  }

  /**
   * Sets the server configuration for this tab.
   *
   * @param config the server configuration
   */
  public void setServerConfig(LdapServerConfig config) {
    this.serverConfig = config;
    
    // Set default search base from server config
    if (config != null && config.getBaseDn() != null && !config.getBaseDn().isEmpty()) {
      searchBaseField.setValue(config.getBaseDn());
    }
    
    // Clear results and hide grid
    resultsGrid.setItems(new ArrayList<>());
    resultsGrid.setVisible(false);
  }

  /**
   * Loads data for this tab (called when tab is selected).
   */
  public void loadData() {
    // Nothing to load automatically - user initiates search manually
  }

  private void performSearch() {
    if (serverConfig == null) {
      showError("No server selected");
      return;
    }

    String searchBase = searchBaseField.getValue();
    if (searchBase == null || searchBase.trim().isEmpty()) {
      showError("Search base is required");
      return;
    }

    String effectiveRightsFor = effectiveRightsForField.getValue();
    if (effectiveRightsFor == null || effectiveRightsFor.trim().isEmpty()) {
      showError("Effective Rights For DN is required");
      return;
    }

    // Auto-prepend "dn: " if not already present
    final String formattedRightsFor;
    String tempRightsFor = effectiveRightsFor.trim();
    if (!tempRightsFor.toLowerCase().startsWith("dn: ")) {
      formattedRightsFor = "dn: " + tempRightsFor;
    } else {
      formattedRightsFor = tempRightsFor;
    }

    // Parse and validate search size limit
    final int sizeLimit;
    String sizeLimitStr = searchSizeLimitField.getValue();
    if (sizeLimitStr != null && !sizeLimitStr.trim().isEmpty()) {
      try {
        int parsedLimit = Integer.parseInt(sizeLimitStr.trim());
        if (parsedLimit < 0) {
          showError("Search size limit must be 0 or greater");
          return;
        }
        sizeLimit = parsedLimit;
      } catch (NumberFormatException e) {
        showError("Search size limit must be a valid number");
        return;
      }
    } else {
      sizeLimit = 100; // default
    }

    loadingContainer.setVisible(true);
    progressBar.setVisible(true);
    resultsGrid.setVisible(false);

    // Perform search in background thread
    new Thread(() -> {
      try {
        SearchResultWithLimit searchResult = searchEffectiveRights(
            searchBase.trim(),
            scopeComboBox.getValue(),
            searchFilterField.getValue().trim(),
            attributesField.getValue().trim(),
            formattedRightsFor,
            sizeLimit
        );

        List<EffectiveRightsResult> results = searchResult.getResults();
        boolean sizeLimitExceeded = searchResult.isSizeLimitExceeded();

        getUI().ifPresent(ui -> ui.access(() -> {
          loadingContainer.setVisible(false);
          progressBar.setVisible(false);
          
          try {
            // Clear existing items first
            resultsGrid.setItems();
            
            // Set new items 
            resultsGrid.setItems(results);
            
            // Force data provider refresh
            resultsGrid.getDataProvider().refreshAll();
            
            // Show the grid and ensure parent container is visible
            resultsGrid.setVisible(true);
            resultsContainer.setVisible(true);
            
            // Force UI push
            ui.push();
            
            if (sizeLimitExceeded) {
              showError("Search size limit (" + sizeLimit + ") exceeded. Displaying first " + results.size() + " entries found.");
            }
            
            if (results.isEmpty()) {
              if (!sizeLimitExceeded) {
                showInfo("No entries found matching the search criteria");
              }
            } else {
              String successMessage = "Found " + results.size() + " entries with effective rights information";
              if (sizeLimitExceeded) {
                successMessage += " (more entries available - increase size limit to see all)";
              }
              showSuccess(successMessage);
            }
            
          } catch (Exception gridException) {
            System.err.println("ERROR: Exception during grid update: " + gridException.getMessage());
            gridException.printStackTrace();
          }
        }));

      } catch (Exception e) {
        getUI().ifPresent(ui -> ui.access(() -> {
          loadingContainer.setVisible(false);
          progressBar.setVisible(false);
          showError("Search failed: " + e.getMessage());
        }));
      }
    }).start();
  }

  private SearchResultWithLimit searchEffectiveRights(String searchBase, 
      SearchScope scope, String filter, String attributes, String effectiveRightsFor, 
      int sizeLimit) throws LDAPException {
    
    List<EffectiveRightsResult> results = new ArrayList<>();
    boolean sizeLimitExceeded = false;
    LDAPConnection connection = null;
    
    try {
      // Create direct LDAP connection using server config
      connection = createConnection(serverConfig);
      
      // Parse attributes for the search - following the SDK example pattern
      String[] attributeArray;
      if ("*".equals(attributes.trim())) {
        // Request all attributes plus aclRights for effective rights processing
        attributeArray = new String[] { "*", "aclRights" };
      } else {
        // Parse comma-separated attributes and add aclRights for effective rights processing
        List<String> attrList = new ArrayList<>();
        for (String attr : attributes.split(",")) {
          attrList.add(attr.trim());
        }
        // Add aclRights attribute for effective rights processing
        attrList.add("aclRights");
        attributeArray = attrList.toArray(new String[0]);
      }

      // Create search request following the SDK example pattern
      SearchRequest searchRequest = new SearchRequest(
          searchBase,
          scope,
          Filter.create(filter),
          attributeArray
      );
      
      // Set the size limit (0 means no limit)
      searchRequest.setSizeLimit(sizeLimit);
      
      // Add the GetEffectiveRightsRequestControl as shown in the SDK example
      searchRequest.addControl(new GetEffectiveRightsRequestControl(effectiveRightsFor));
      
      // Execute the search
      SearchResult searchResult = connection.search(searchRequest);
      
      // Check if size limit was exceeded - this can be indicated by:
      // 1. Result code being SIZE_LIMIT_EXCEEDED
      // 2. The number of entries returned equals the size limit (and size limit > 0)
      sizeLimitExceeded = (searchResult.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) ||
                          (sizeLimit > 0 && searchResult.getEntryCount() >= sizeLimit);
      
      // Process results using EffectiveRightsEntry as shown in the SDK example
      for (Entry entry : searchResult.getSearchEntries()) {
        processEffectiveRightsEntry(entry, effectiveRightsFor, results);
      }
      
    } catch (LDAPException e) {
      // Handle size limit exceeded exception specifically
      if (e.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
        sizeLimitExceeded = true;
        
        // For SIZE_LIMIT_EXCEEDED, try to get partial results from the exception
        // The UnboundID SDK may include partial results in the exception
        if (e instanceof LDAPSearchException) {
          LDAPSearchException searchException = (LDAPSearchException) e;
          SearchResult partialResult = searchException.getSearchResult();
          if (partialResult != null && partialResult.getEntryCount() > 0) {
            // Process the partial results we did get
            for (Entry entry : partialResult.getSearchEntries()) {
              processEffectiveRightsEntry(entry, effectiveRightsFor, results);
            }
          }
        }
      } else if (e.getResultCode() == ResultCode.UNAVAILABLE_CRITICAL_EXTENSION) {
        throw new LDAPException(ResultCode.UNAVAILABLE_CRITICAL_EXTENSION, 
            "Server does not support GetEffectiveRightsRequestControl (OID: " + 
            GetEffectiveRightsRequestControl.GET_EFFECTIVE_RIGHTS_REQUEST_OID + ")", e);
      } else {
        throw new LDAPException(e.getResultCode(), 
            "Effective rights search failed: " + e.getMessage(), e);
      }
    } finally {
      // Clean up connection
      if (connection != null) {
        try {
          connection.close();
        } catch (Exception e) {
          // Ignore cleanup errors
        }
      }
    }
    
    return new SearchResultWithLimit(results, sizeLimitExceeded);
  }

  /**
   * Processes a single LDAP entry to extract effective rights information.
   */
  private void processEffectiveRightsEntry(Entry entry, String effectiveRightsFor, 
      List<EffectiveRightsResult> results) {
    EffectiveRightsResult result = new EffectiveRightsResult();
    result.setDn(entry.getDN());
    
    // Create EffectiveRightsEntry to check rights information availability
    EffectiveRightsEntry effectiveRightsEntry = new EffectiveRightsEntry(entry);
    
    StringBuilder attributeRights = new StringBuilder();
    StringBuilder entryRights = new StringBuilder();
    
    if (effectiveRightsEntry.rightsInformationAvailable()) {
      // Rights information is available - process attribute rights
      for (Attribute attribute : entry.getAttributes()) {
        String attrName = attribute.getName();
        if (!attrName.equals("aclRights") && !attrName.equals("entryLevelRights")) {
          // Check rights for this attribute
          StringBuilder attrRightsBuilder = new StringBuilder();
          
          if (effectiveRightsEntry.hasAttributeRight(AttributeRight.READ, attrName)) {
            attrRightsBuilder.append("r");
          }
          if (effectiveRightsEntry.hasAttributeRight(AttributeRight.WRITE, attrName)) {
            attrRightsBuilder.append("w");
          }
          if (effectiveRightsEntry.hasAttributeRight(AttributeRight.SELFWRITE_ADD, attrName)) {
            attrRightsBuilder.append("a");
          }
          if (effectiveRightsEntry.hasAttributeRight(AttributeRight.SELFWRITE_DELETE, attrName)) {
            attrRightsBuilder.append("d");
          }
          if (effectiveRightsEntry.hasAttributeRight(AttributeRight.COMPARE, attrName)) {
            attrRightsBuilder.append("c");
          }
          if (effectiveRightsEntry.hasAttributeRight(AttributeRight.SEARCH, attrName)) {
            attrRightsBuilder.append("s");
          }
          
          if (attrRightsBuilder.length() > 0) {
            if (attributeRights.length() > 0) {
              attributeRights.append("; ");
            }
            attributeRights.append(attrName).append(": ").append(attrRightsBuilder.toString());
          }
        }
      }
      
      // Check entry-level rights
      StringBuilder entryRightsBuilder = new StringBuilder();
      if (effectiveRightsEntry.hasEntryRight(EntryRight.ADD)) {
        entryRightsBuilder.append("a");
      }
      if (effectiveRightsEntry.hasEntryRight(EntryRight.DELETE)) {
        entryRightsBuilder.append("d");
      }
      if (effectiveRightsEntry.hasEntryRight(EntryRight.READ)) {
        entryRightsBuilder.append("r");
      }
      if (effectiveRightsEntry.hasEntryRight(EntryRight.WRITE)) {
        entryRightsBuilder.append("w");
      }
      if (effectiveRightsEntry.hasEntryRight(EntryRight.PROXY)) {
        entryRightsBuilder.append("p");
      }
      
      if (entryRightsBuilder.length() > 0) {
        entryRights.append("Entry rights: ").append(entryRightsBuilder.toString());
      } else {
        entryRights.append("No entry rights for: ").append(effectiveRightsFor);
      }
      
      // If no attribute rights found, provide informative message
      if (attributeRights.length() == 0) {
        attributeRights.append("No specific attribute rights available");
      }
      
    } else {
      // No effective rights information was returned
      attributeRights.append("No effective rights information available - control may be unsupported or user lacks privileges");
      entryRights.append("No entry rights information available for: ").append(effectiveRightsFor);
    }
    
    result.setAttributeRights(attributeRights.toString());
    result.setEntryRights(entryRights.toString());
    
    results.add(result);
  }
  
  /**
   * Creates a new LDAP connection using the server configuration.
   * This is a direct connection separate from the connection pool.
   */
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
        throw new LDAPException(ResultCode.CONNECT_ERROR, "StartTLS failed", e);
      }
    }

    // Bind with credentials if provided
    if (config.getBindDn() != null && !config.getBindDn().isEmpty()) {
      try {
        connection.bind(config.getBindDn(), config.getPassword());
      } catch (LDAPException e) {
        connection.close();
        throw e;
      }
    }

    return connection;
  }

  private void showError(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showInfo(String message) {
    Notification notification = Notification.show(message, 4000, Notification.Position.MIDDLE);
    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
  }

  /**
   * Result class for effective rights search results.
   */
  public static class EffectiveRightsResult {
    private String dn;
    private String attributeRights;
    private String entryRights;
    
    public EffectiveRightsResult() {
      // Default constructor
    }

    public String getDn() {
      return dn != null ? dn : "";
    }

    public void setDn(String dn) {
      this.dn = dn;
    }

    public String getAttributeRights() {
      return attributeRights != null ? attributeRights : "";
    }

    public void setAttributeRights(String attributeRights) {
      this.attributeRights = attributeRights;
    }

    public String getEntryRights() {
      return entryRights != null ? entryRights : "";
    }

    public void setEntryRights(String entryRights) {
      this.entryRights = entryRights;
    }
    
    @Override
    public String toString() {
      return "EffectiveRightsResult{" +
          "dn='" + dn + '\'' +
          ", attributeRights='" + attributeRights + '\'' +
          ", entryRights='" + entryRights + '\'' +
          '}';
    }
  }

  /**
   * Helper class to hold search results along with size limit exceeded flag.
   */
  public static class SearchResultWithLimit {
    private final List<EffectiveRightsResult> results;
    private final boolean sizeLimitExceeded;
    
    public SearchResultWithLimit(List<EffectiveRightsResult> results, boolean sizeLimitExceeded) {
      this.results = results;
      this.sizeLimitExceeded = sizeLimitExceeded;
    }
    
    public List<EffectiveRightsResult> getResults() {
      return results;
    }
    
    public boolean isSizeLimitExceeded() {
      return sizeLimitExceeded;
    }
  }
}