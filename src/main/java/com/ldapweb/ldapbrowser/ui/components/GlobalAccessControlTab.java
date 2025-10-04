package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab component for displaying global access control information.
 * Shows ds-cfg-global-aci attributes from the Access Control Handler.
 */
public class GlobalAccessControlTab extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private Grid<GlobalAciInfo> aciGrid;
  private ProgressBar progressBar;
  private Div loadingContainer;
  private boolean dataLoaded = false;
  private TextField searchField;
  private List<GlobalAciInfo> allAciInfo = new ArrayList<>();
  private Div aciDetailsPanel;
  private GlobalAciInfo selectedAci;

  /**
   * Constructs a new GlobalAccessControlTab.
   *
   * @param ldapService the LDAP service for executing searches
   */
  public GlobalAccessControlTab(LdapService ldapService) {
    this.ldapService = ldapService;
    initUI();
  }

  private void initUI() {
    setSizeFull();
    setPadding(false);
    setSpacing(false);

    // Create loading indicator (for both panes)
    progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    
    loadingContainer = new Div();
    loadingContainer.add(progressBar, new Div("Loading global access control information..."));
    loadingContainer.getStyle().set("text-align", "center");
    loadingContainer.getStyle().set("padding", "20px");
    loadingContainer.setVisible(false);
    add(loadingContainer);

    // Main split layout - two vertical panes
    SplitLayout mainLayout = new SplitLayout();
    mainLayout.setSizeFull();
    mainLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
    mainLayout.setSplitterPosition(60); // 60% for left pane, 40% for right pane

    // LEFT PANE: Control panel with title, buttons, description, search, and grid
    VerticalLayout leftPane = new VerticalLayout();
    leftPane.setSizeFull();
    leftPane.setPadding(true);
    leftPane.setSpacing(true);

    // Header with title and refresh button
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setJustifyContentMode(JustifyContentMode.BETWEEN);
    header.setAlignItems(Alignment.CENTER);
    
    H3 title = new H3("Global Access Control");
    title.getStyle().set("margin", "0");
    
    Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create());
    refreshButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    refreshButton.addClickListener(event -> refreshData());
    
    header.add(title, refreshButton);
    leftPane.add(header);

    // Description
    Div description = new Div();
    description.setText("Global Access Control Instructions (ACIs) from Access Control Handler.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    leftPane.add(description);

    // Search field for filtering ACI rules
    searchField = new TextField();
    searchField.setPlaceholder("Search global ACIs by name or content...");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setWidthFull();
    searchField.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    searchField.addValueChangeListener(event -> filterAciGrid());
    leftPane.add(searchField);

    // Grid for displaying ACI entries
    aciGrid = new Grid<>(GlobalAciInfo.class, false);
    aciGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
    aciGrid.setAllRowsVisible(false);
    aciGrid.setPageSize(50);
    aciGrid.getStyle()
        .set("border", "1px solid var(--lumo-contrast-20pct)")
        .set("border-radius", "var(--lumo-border-radius-m)");
    setupGridColumns();
    aciGrid.setSizeFull();
    aciGrid.addSelectionListener(event -> {
      selectedAci = event.getFirstSelectedItem().orElse(null);
      updateDetailsPanel();
    });

    leftPane.add(aciGrid);
    leftPane.setFlexGrow(1, aciGrid); // Grid takes remaining space

    // RIGHT PANE: ACI details
    aciDetailsPanel = new Div();
    aciDetailsPanel.setHeightFull();
    aciDetailsPanel.getStyle()
        .set("padding", "var(--lumo-space-m)")
        .set("overflow-y", "auto")
        .set("min-width", "300px") // Ensure minimum width for readability
        .set("background", "white"); // Match schema browser style
    
    createDetailsPanel();

    mainLayout.addToPrimary(leftPane);
    mainLayout.addToSecondary(aciDetailsPanel);

    add(mainLayout);
    setFlexGrow(1, mainLayout);
  }

  /**
   * Sets the server configuration for this tab.
   *
   * @param config the server configuration
   */
  public void setServerConfig(LdapServerConfig config) {
    this.serverConfig = config;
    this.dataLoaded = false; // Reset data loaded flag when server changes
    aciGrid.setItems(new ArrayList<>()); // Clear existing data
  }

  /**
   * Sets up the grid columns with the new ACI structure.
   */
  private void setupGridColumns() {
    // Name column (first and leftmost)
    aciGrid.addColumn(GlobalAciInfo::getName)
        .setHeader("Name")
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortable(true)
        .setResizable(true);

    // Resources column
    aciGrid.addColumn(GlobalAciInfo::getResources)
        .setHeader("Resources")
        .setAutoWidth(true)
        .setFlexGrow(2)
        .setSortable(false)
        .setResizable(true);

    // Rights column
    aciGrid.addColumn(GlobalAciInfo::getRights)
        .setHeader("Rights")
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortable(false)
        .setResizable(true);

    // Clients column
    aciGrid.addColumn(GlobalAciInfo::getClients)
        .setHeader("Clients")
        .setAutoWidth(true)
        .setFlexGrow(2)
        .setSortable(false)
        .setResizable(true);
  }

  /**
   * Creates the initial details panel layout.
   */
  private void createDetailsPanel() {
    updateDetailsPanel();
  }

  /**
   * Updates the details panel with the selected ACI information.
   */
  private void updateDetailsPanel() {
    aciDetailsPanel.removeAll();

    if (selectedAci == null) {
      Span placeholder = new Span("Select a global ACI to view details");
      placeholder.getStyle()
          .set("color", "var(--lumo-secondary-text-color)")
          .set("font-style", "italic");
      aciDetailsPanel.add(placeholder);
      return;
    }

    com.ldapweb.ldapbrowser.util.AciParser.ParsedAci parsedAci = selectedAci.getParsedAci();

    // Header with title (no action buttons for read-only)
    H4 aciTitle = new H4(parsedAci.getName());
    aciTitle.getStyle().set("margin", "0").set("margin-bottom", "var(--lumo-space-m)");
    aciDetailsPanel.add(aciTitle);

    // Resources section
    if (!parsedAci.getTargets().isEmpty()) {
      addDetailRow("Resources", String.join(", ", parsedAci.getTargets()));
    } else {
      addDetailRow("Resources", "All resources");
    }

    // Rights section
    String rightsValue = parsedAci.getAllowOrDeny().toUpperCase();
    if (!parsedAci.getPermissions().isEmpty()) {
      rightsValue += " (" + String.join(", ", parsedAci.getPermissions()) + ")";
    }
    addDetailRow("Rights", rightsValue);

    // Clients section
    if (!parsedAci.getBindRules().isEmpty()) {
      addDetailRow("Clients", String.join(", ", parsedAci.getBindRules()));
    } else {
      addDetailRow("Clients", "Any client");
    }

    // Raw Definition section
    HorizontalLayout rawSection = new HorizontalLayout();
    rawSection.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.START);
    rawSection.setSpacing(true);
    rawSection.setWidthFull();
    rawSection.getStyle().set("margin-top", "var(--lumo-space-l)");
    
    Span rawLabel = new Span("Raw Definition:");
    rawLabel.getStyle().set("font-weight", "bold").set("min-width", "150px");
    
    TextArea rawTextArea = new TextArea();
    rawTextArea.setValue(parsedAci.getRawAci());
    rawTextArea.setReadOnly(true);
    rawTextArea.setWidthFull();
    rawTextArea.setMinHeight("100px");
    rawTextArea.getStyle()
        .set("font-family", "monospace")
        .set("font-size", "var(--lumo-font-size-s)");
    
    rawSection.add(rawLabel, rawTextArea);
    rawSection.setFlexGrow(1, rawTextArea);
    
    aciDetailsPanel.add(rawSection);
  }

  /**
   * Adds a detail row with label and value, matching schema browser styling.
   */
  private void addDetailRow(String label, String value) {
    if (value != null && !value.trim().isEmpty()) {
      HorizontalLayout row = new HorizontalLayout();
      row.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.START);
      row.setSpacing(true);
      row.getStyle().set("margin-bottom", "var(--lumo-space-s)");

      Span labelSpan = new Span(label + ":");
      labelSpan.getStyle().set("font-weight", "bold").set("min-width", "150px");

      Span valueSpan = new Span(value);
      valueSpan.getStyle().set("word-wrap", "break-word");

      row.add(labelSpan, valueSpan);
      row.setFlexGrow(1, valueSpan);

      aciDetailsPanel.add(row);
    }
  }

  /**
   * Filters the ACI grid based on the search field value.
   */
  private void filterAciGrid() {
    String searchTerm = searchField.getValue().toLowerCase().trim();
    
    if (searchTerm.isEmpty()) {
      aciGrid.setItems(allAciInfo);
    } else {
      List<GlobalAciInfo> filteredList = allAciInfo.stream()
          .filter(aci -> 
              aci.getName().toLowerCase().contains(searchTerm) ||
              aci.getAciValue().toLowerCase().contains(searchTerm) ||
              aci.getResources().toLowerCase().contains(searchTerm) ||
              aci.getRights().toLowerCase().contains(searchTerm) ||
              aci.getClients().toLowerCase().contains(searchTerm)
          )
          .collect(Collectors.toList());
      aciGrid.setItems(filteredList);
    }
  }

  /**
   * Loads global access control data from the LDAP server.
   */
  public void loadData() {
    if (serverConfig == null) {
      showError("No server selected");
      return;
    }

    if (!ldapService.isConnected(serverConfig.getId())) {
      showError("Not connected to server: " + serverConfig.getName());
      return;
    }

    // Only load once per server configuration
    if (dataLoaded) {
      return;
    }

    // Show loading indicator
    loadingContainer.setVisible(true);
    aciGrid.setVisible(false);

    try {
      // Search for global ACIs in the Access Control Handler
      List<LdapEntry> entries = ldapService.searchEntries(
          serverConfig.getId(),
          "cn=Access Control Handler,cn=config",
          "(objectClass=*)",
          SearchScope.BASE,
          "ds-cfg-global-aci"
      );

      List<GlobalAciInfo> aciInfoList = new ArrayList<>();

      for (LdapEntry entry : entries) {
        List<String> acis = entry.getAttributeValues("ds-cfg-global-aci");
        if (acis != null) {
          for (String aci : acis) {
            aciInfoList.add(new GlobalAciInfo(aci));
          }
        }
      }

      // Update UI
      loadingContainer.setVisible(false);
      aciGrid.setVisible(true);
      allAciInfo.clear();
      allAciInfo.addAll(aciInfoList);
      filterAciGrid(); // Apply any current filter
      dataLoaded = true;

      if (aciInfoList.isEmpty()) {
        showInfo("No global access control instructions found");
      } else {
        showSuccess("Found " + aciInfoList.size() + " global ACI(s)");
      }

    } catch (LDAPException e) {
      loadingContainer.setVisible(false);
      aciGrid.setVisible(true);
      allAciInfo.clear();
      aciGrid.setItems(new ArrayList<>());
      showError("Failed to search for global ACIs: " + e.getMessage());
    }
  }

  /**
   * Refreshes the global ACI data from the server.
   */
  private void refreshData() {
    dataLoaded = false;
    loadData();
  }

  private void showError(String message) {
    Notification n = Notification.show(message, 5000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private void showInfo(String message) {
    Notification n = Notification.show(message, 3000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
  }

  private void showSuccess(String message) {
    Notification n = Notification.show(message, 3000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  /**
   * Data class for global ACI information.
   */
  public static class GlobalAciInfo {
    private final String aciValue;
    private final com.ldapweb.ldapbrowser.util.AciParser.ParsedAci parsedAci;

    public GlobalAciInfo(String aciValue) {
      this.aciValue = aciValue;
      this.parsedAci = com.ldapweb.ldapbrowser.util.AciParser.parseAci(aciValue);
    }

    public String getAciValue() {
      return aciValue;
    }

    public com.ldapweb.ldapbrowser.util.AciParser.ParsedAci getParsedAci() {
      return parsedAci;
    }

    public String getName() {
      return parsedAci.getName();
    }

    public String getResources() {
      return parsedAci.getResourcesString();
    }

    public String getRights() {
      return parsedAci.getRightsString();
    }

    public String getClients() {
      return parsedAci.getClientsString();
    }
  }
}
