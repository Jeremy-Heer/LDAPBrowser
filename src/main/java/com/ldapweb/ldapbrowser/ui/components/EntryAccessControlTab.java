package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.util.AciParser;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.UI;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab component for displaying entry access control information.
 * Shows entries with ACI attributes.
 */
public class EntryAccessControlTab extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private Grid<EntryAciInfo> aciGrid;
  private ProgressBar progressBar;
  private Div loadingContainer;
  private boolean dataLoaded = false;
  private TextField searchField;
  private List<EntryAciInfo> allAciInfo = new ArrayList<>();
  private Div aciDetailsPanel;
  private EntryAciInfo selectedAci;

  /**
   * Constructs a new EntryAccessControlTab.
   *
   * @param ldapService the LDAP service for executing searches
   */
  public EntryAccessControlTab(LdapService ldapService) {
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
    loadingContainer.add(progressBar, new Div("Loading entry access control information..."));
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

    // Header with title and buttons on the same row
    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setJustifyContentMode(JustifyContentMode.BETWEEN);
    header.setAlignItems(Alignment.CENTER);
    
    H3 title = new H3("Entry Access Control");
    title.getStyle().set("margin", "0");
    
    // Button group for actions
    HorizontalLayout buttonGroup = new HorizontalLayout();
    buttonGroup.setSpacing(true);
    
    Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create());
    refreshButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
    refreshButton.addClickListener(event -> refreshData());
    
    Button addAciButton = new Button("Add New ACI", VaadinIcon.PLUS.create());
    addAciButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addAciButton.addClickListener(event -> openAddAciDialog());
    
    buttonGroup.add(refreshButton, addAciButton);
    
    header.add(title, buttonGroup);
    leftPane.add(header);

    // Description
    Div description = new Div();
    description.setText("Entries with Access Control Instructions (ACIs) defined at the entry level.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    leftPane.add(description);

    // Search field for filtering ACI rules
    searchField = new TextField();
    searchField.setPlaceholder("Search ACIs by DN or rule content...");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setWidthFull();
    searchField.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    searchField.addValueChangeListener(event -> filterAciGrid());
    leftPane.add(searchField);

    // Grid for displaying ACI entries
    aciGrid = new Grid<>(EntryAciInfo.class, false);
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
   * Sets up the grid columns with the new ACI structure.
   */
  private void setupGridColumns() {
    // Name column (first and leftmost)
    aciGrid.addColumn(EntryAciInfo::getName)
        .setHeader("Name")
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortable(true)
        .setResizable(true);

    // Entry DN column
    aciGrid.addColumn(EntryAciInfo::getDn)
        .setHeader("Entry DN")
        .setAutoWidth(true)
        .setFlexGrow(2)
        .setSortable(true)
        .setResizable(true);

    // Resources column
    aciGrid.addColumn(EntryAciInfo::getResources)
        .setHeader("Resources")
        .setAutoWidth(true)
        .setFlexGrow(2)
        .setSortable(false)
        .setResizable(true);

    // Rights column
    aciGrid.addColumn(EntryAciInfo::getRights)
        .setHeader("Rights")
        .setAutoWidth(true)
        .setFlexGrow(1)
        .setSortable(false)
        .setResizable(true);

    // Clients column
    aciGrid.addColumn(EntryAciInfo::getClients)
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
      Span placeholder = new Span("Select an ACI to view details");
      placeholder.getStyle()
          .set("color", "var(--lumo-secondary-text-color)")
          .set("font-style", "italic");
      aciDetailsPanel.add(placeholder);
      return;
    }

    AciParser.ParsedAci parsedAci = selectedAci.getParsedAci();

    // Header with title and action buttons
    HorizontalLayout detailsHeader = new HorizontalLayout();
    detailsHeader.setWidthFull();
    detailsHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);
    detailsHeader.setAlignItems(Alignment.CENTER);
    detailsHeader.getStyle().set("margin-bottom", "var(--lumo-space-m)");

    H4 aciTitle = new H4(parsedAci.getName());
    aciTitle.getStyle().set("margin", "0");

    // Action buttons (Edit and Delete)
    HorizontalLayout actionButtons = new HorizontalLayout();
    actionButtons.setSpacing(false);

    Button editButton = new Button(new Icon(VaadinIcon.EDIT));
    editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    editButton.setTooltipText("Edit ACI");
    editButton.addClickListener(event -> openEditAciDialog(selectedAci));

    Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    deleteButton.setTooltipText("Delete ACI");
    deleteButton.addClickListener(event -> deleteAci(selectedAci));

    actionButtons.add(editButton, deleteButton);

    detailsHeader.add(aciTitle, actionButtons);
    aciDetailsPanel.add(detailsHeader);

    // Entry DN section
    addDetailRow("Entry DN", selectedAci.getDn());

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
   * Loads entry access control data from the LDAP server.
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
      // Get the default search base from the server configuration
      String baseDn = getDefaultSearchBase();
      if (baseDn == null) {
        loadingContainer.setVisible(false);
        aciGrid.setVisible(true);
        showError("No default search base configured for server");
        return;
      }

      // Search for entries with ACIs
      List<LdapEntry> entries = ldapService.searchEntries(
          serverConfig.getId(),
          baseDn,
          "(aci=*)",
          SearchScope.SUB,
          "aci"
      );

      List<EntryAciInfo> aciInfoList = new ArrayList<>();

      for (LdapEntry entry : entries) {
        List<String> acis = entry.getAttributeValues("aci");
        if (acis != null) {
          for (String aci : acis) {
            aciInfoList.add(new EntryAciInfo(entry.getDn(), aci));
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
        showInfo("No entry access control instructions found");
      } else {
        showSuccess("Found " + aciInfoList.size() + " entry ACI(s)");
      }

    } catch (LDAPException e) {
      loadingContainer.setVisible(false);
      aciGrid.setVisible(true);
      allAciInfo.clear();
      aciGrid.setItems(new ArrayList<>());
      showError("Failed to search for entry ACIs: " + e.getMessage());
    }
  }

  private String getDefaultSearchBase() {
    if (serverConfig == null) {
      return null;
    }
    
    // Try to get default search base from server config
    String searchBase = serverConfig.getBaseDn();
    if (searchBase != null && !searchBase.trim().isEmpty()) {
      return searchBase.trim();
    }
    
    // Try to get naming contexts from the server
    try {
      List<String> namingContexts = ldapService.getNamingContexts(serverConfig.getId());
      if (!namingContexts.isEmpty()) {
        return namingContexts.get(0); // Use the first naming context
      }
    } catch (LDAPException e) {
      // Fall back to common defaults
    }
    
    // Common fallbacks based on server type or host
    String host = serverConfig.getHost().toLowerCase();
    if (host.contains("example")) {
      return "dc=example,dc=com";
    }
    
    return "dc=local"; // Last resort fallback
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
   * Opens the Add ACI dialog.
   */
  private void openAddAciDialog() {
    if (serverConfig == null) {
      showError("No server selected");
      return;
    }

    if (!ldapService.isConnected(serverConfig.getId())) {
      showError("Not connected to server: " + serverConfig.getName());
      return;
    }

    AddAciDialog dialog = new AddAciDialog(ldapService, serverConfig, this::handleAciAdded);
    dialog.open();
  }

  /**
   * Opens the Edit ACI dialog with pre-populated data.
   */
  private void openEditAciDialog(EntryAciInfo aciInfo) {
    if (serverConfig == null) {
      showError("No server selected");
      return;
    }

    if (!ldapService.isConnected(serverConfig.getId())) {
      showError("Not connected to server: " + serverConfig.getName());
      return;
    }

    // Create edit dialog with existing ACI data
    AddAciDialog dialog = new AddAciDialog(ldapService, serverConfig, 
        (targetDn, newAci) -> handleAciEdited(aciInfo, targetDn, newAci), 
        aciInfo);
    dialog.open();
  }

  /**
   * Handles the addition of a new ACI.
   */
  private void handleAciAdded(String targetDn, String aci) {
    try {
      // Add the ACI to the specified DN
      ldapService.addAttributeValue(serverConfig.getId(), targetDn, "aci", aci);
      
      // Refresh the data to show the new ACI
      refreshData();
      
      showSuccess("ACI added successfully to " + targetDn);
    } catch (LDAPException e) {
      showError("Failed to add ACI: " + e.getMessage());
    }
  }

  /**
   * Handles the editing of an existing ACI.
   */
  private void handleAciEdited(EntryAciInfo originalAci, String targetDn, String newAci) {
    try {
      // If DN changed, this is a move operation (delete from old, add to new)
      if (!originalAci.getDn().equals(targetDn)) {
        // Remove from original DN
        Modification deleteModification = new Modification(ModificationType.DELETE, "aci", originalAci.getAciValue());
        List<Modification> deleteModifications = List.of(deleteModification);
        ldapService.modifyEntry(serverConfig.getId(), originalAci.getDn(), deleteModifications);
        
        // Add to new DN
        ldapService.addAttributeValue(serverConfig.getId(), targetDn, "aci", newAci);
        showSuccess("ACI moved from " + originalAci.getDn() + " to " + targetDn);
      } else {
        // Same DN, replace the ACI value
        List<Modification> modifications = new ArrayList<>();
        
        // Delete old ACI value
        modifications.add(new Modification(ModificationType.DELETE, "aci", originalAci.getAciValue()));
        
        // Add new ACI value
        modifications.add(new Modification(ModificationType.ADD, "aci", newAci));
        
        ldapService.modifyEntry(serverConfig.getId(), targetDn, modifications);
        showSuccess("ACI updated successfully on " + targetDn);
      }
      
      // Refresh the data to show the updated ACI
      refreshData();
      
    } catch (LDAPException e) {
      showError("Failed to update ACI: " + e.getMessage());
    }
  }

  /**
   * Refreshes the ACI data from the server.
   */
  private void refreshData() {
    dataLoaded = false;
    loadData();
  }

  /**
   * Handles deleting an existing ACI.
   *
   * @param aciInfo the ACI information to delete
   */
  private void deleteAci(EntryAciInfo aciInfo) {
    if (serverConfig == null) {
      showError("Server configuration not available");
      return;
    }

    // Create confirmation dialog
    Dialog confirmDialog = new Dialog();
    confirmDialog.setHeaderTitle("Delete ACI");
    
    VerticalLayout content = new VerticalLayout();
    content.add(new Div("Are you sure you want to delete this ACI?"));
    content.add(new Div("Entry: " + aciInfo.getDn()));
    content.add(new Div("ACI: " + aciInfo.getAciValue()));
    
    HorizontalLayout buttons = new HorizontalLayout();
    
    Button deleteButton = new Button("Delete", event -> {
      try {
        Modification deleteModification = new Modification(ModificationType.DELETE, "aci", aciInfo.getAciValue());
        List<Modification> modifications = List.of(deleteModification);
        ldapService.modifyEntry(serverConfig.getId(), aciInfo.getDn(), modifications);
        showSuccess("ACI deleted successfully");
        refreshData();
        confirmDialog.close();
      } catch (LDAPException e) {
        showError("Failed to delete ACI: " + e.getMessage());
      }
    });
    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
    
    Button cancelButton = new Button("Cancel", event -> confirmDialog.close());
    
    buttons.add(cancelButton, deleteButton);
    
    content.add(buttons);
    confirmDialog.add(content);
    confirmDialog.open();
  }

  /**
   * Filters the ACI grid based on the search field value.
   * Searches in both DN and ACI Value columns.
   */
  private void filterAciGrid() {
    String searchText = searchField.getValue();
    if (searchText == null || searchText.trim().isEmpty()) {
      // Show all ACI entries
      aciGrid.setItems(allAciInfo);
    } else {
      // Filter entries containing the search text in DN or ACI value (case-insensitive)
      String lowerCaseFilter = searchText.toLowerCase().trim();
      List<EntryAciInfo> filteredItems = allAciInfo.stream()
        .filter(aciInfo -> 
          aciInfo.getDn().toLowerCase().contains(lowerCaseFilter) ||
          aciInfo.getAciValue().toLowerCase().contains(lowerCaseFilter)
        )
        .collect(Collectors.toList());
      
      aciGrid.setItems(filteredItems);
    }
  }

  /**
   * Dialog for adding new ACIs to entries.
   */
  private static class AddAciDialog extends Dialog {
    private final LdapService ldapService;
    private final LdapServerConfig serverConfig;
    private final java.util.function.BiConsumer<String, String> onAciAdded;
    private final EntryAciInfo editingAci; // null for add mode, populated for edit mode
    
    private DnSelectorField targetDnField;
    private Button buildAciButton;
    private TextField aciField;
    private Button addButton;
    private Button cancelButton;
    private Button copyLdifButton;

    // Constructor for adding new ACI
    public AddAciDialog(LdapService ldapService, LdapServerConfig serverConfig,
                       java.util.function.BiConsumer<String, String> onAciAdded) {
      this(ldapService, serverConfig, onAciAdded, null);
    }

    // Constructor for editing existing ACI or adding new ACI
    public AddAciDialog(LdapService ldapService, LdapServerConfig serverConfig,
                       java.util.function.BiConsumer<String, String> onAciAdded,
                       EntryAciInfo editingAci) {
      this.ldapService = ldapService;
      this.serverConfig = serverConfig;
      this.onAciAdded = onAciAdded;
      this.editingAci = editingAci;
      initUI();
    }

    private void initUI() {
      boolean isEditMode = editingAci != null;
      setHeaderTitle(isEditMode ? "Edit Access Control Instruction" : "Add Access Control Instruction");
      setModal(true);
      setDraggable(true);
      setResizable(true);
      setWidth("600px");

      VerticalLayout content = new VerticalLayout();
      content.setPadding(false);
      content.setSpacing(true);

      // Description
      Div description = new Div();
      description.setText(isEditMode ? 
          "Edit the Access Control Instruction (ACI) for the selected entry." :
          "Add a new Access Control Instruction (ACI) to a specific entry.");
      description.getStyle().set("color", "var(--lumo-secondary-text-color)")
          .set("margin-bottom", "var(--lumo-space-m)");
      content.add(description);

      // Target DN section
      H4 targetTitle = new H4("Target Entry");
      targetTitle.getStyle().set("margin", "var(--lumo-space-m) 0 var(--lumo-space-s) 0");
      content.add(targetTitle);

      // Use existing DnSelectorField component
      targetDnField = new DnSelectorField("Entry DN", ldapService);
      targetDnField.setPlaceholder("cn=user,ou=people,dc=example,dc=com");
      targetDnField.setHelperText("Distinguished Name of the entry to add the ACI to");
      targetDnField.setServerConfig(serverConfig);
      targetDnField.setWidthFull();
      
      // Pre-populate if in edit mode
      if (isEditMode) {
        targetDnField.setValue(editingAci.getDn());
      }
      
      content.add(targetDnField);

      // ACI section
      H4 aciTitle = new H4("Access Control Instruction");
      aciTitle.getStyle().set("margin", "var(--lumo-space-m) 0 var(--lumo-space-s) 0");
      content.add(aciTitle);

      HorizontalLayout aciLayout = new HorizontalLayout();
      aciLayout.setWidthFull();
      aciLayout.setAlignItems(Alignment.END);
      
      aciField = new TextField("ACI");
      aciField.setPlaceholder("(targetattr=\"userPassword\")(version 3.0; acl \"Example ACI\"; allow (write) userdn=\"ldap:///self\";)");
      aciField.setHelperText("Complete ACI string following PingDirectory syntax");
      aciField.setWidthFull();
      
      // Pre-populate if in edit mode
      if (isEditMode) {
        aciField.setValue(editingAci.getAciValue());
      }
      
      buildAciButton = new Button("Build ACI", VaadinIcon.COG.create());
      buildAciButton.addClickListener(event -> openAciBuilder());
      
      aciLayout.add(aciField, buildAciButton);
      aciLayout.setFlexGrow(1, aciField);
      content.add(aciLayout);

      add(content);

      // Footer buttons
      addButton = new Button(isEditMode ? "Update ACI" : "Add ACI", event -> addAci());
      addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      addButton.setEnabled(false);
      
      copyLdifButton = new Button("Copy as LDIF", VaadinIcon.COPY.create());
      copyLdifButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      copyLdifButton.addClickListener(event -> copyAciAsLdif());
      copyLdifButton.setTooltipText(isEditMode ? 
          "Copy as LDIF for updating the existing ACI" : 
          "Copy as LDIF for adding the ACI to the target entry");
      copyLdifButton.setEnabled(false);
      
      cancelButton = new Button("Cancel", event -> close());
      
      getFooter().add(cancelButton, copyLdifButton, addButton);

      // Enable add button when both fields have values
      targetDnField.addValueChangeListener(event -> updateAddButtonState());
      aciField.addValueChangeListener(event -> updateAddButtonState());
      
      // Update button state for edit mode
      if (isEditMode) {
        updateAddButtonState();
      }
    }

    private void openAciBuilder() {
      AciBuilderDialog aciBuilder = new AciBuilderDialog(builtAci -> {
        aciField.setValue(builtAci);
        updateAddButtonState();
      }, ldapService, serverConfig.getId(), serverConfig);
      
      // If there's an existing ACI value, populate the builder with it
      String existingAci = aciField.getValue().trim();
      if (!existingAci.isEmpty()) {
        aciBuilder.populateFromAci(existingAci);
      }
      
      aciBuilder.open();
    }

    private void updateAddButtonState() {
      boolean canAdd = !targetDnField.getValue().trim().isEmpty() && 
                      !aciField.getValue().trim().isEmpty();
      addButton.setEnabled(canAdd);
      copyLdifButton.setEnabled(canAdd);
    }

    private void addAci() {
      String targetDn = targetDnField.getValue().trim();
      String aci = aciField.getValue().trim();
      
      if (targetDn.isEmpty() || aci.isEmpty()) {
        showError("Please specify both target DN and ACI");
        return;
      }
      
      if (onAciAdded != null) {
        onAciAdded.accept(targetDn, aci);
      }
      close();
    }

    /**
     * Copies the ACI as LDIF format to the clipboard.
     */
    private void copyAciAsLdif() {
      String targetDn = targetDnField.getValue().trim();
      String aci = aciField.getValue().trim();
      
      if (targetDn.isEmpty() || aci.isEmpty()) {
        showError("Please specify both target DN and ACI before copying as LDIF");
        return;
      }
      
      try {
        String ldif;
        boolean isEditMode = editingAci != null;
        
        if (isEditMode) {
          // Generate update LDIF that replaces the old ACI with the new one
          ldif = generateUpdateLdif(targetDn, editingAci.getAciValue(), aci);
        } else {
          // Generate add LDIF for new ACI
          ldif = generateAddLdif(targetDn, aci);
        }
        
        // Copy to clipboard using browser API
        UI.getCurrent().getPage().executeJs(
            "navigator.clipboard.writeText($0).then(() => {" +
            "  console.log('LDIF copied to clipboard');" +
            "}, (err) => {" +
            "  console.error('Failed to copy to clipboard:', err);" +
            "});", ldif);
        
        String message = isEditMode ? "Update LDIF copied to clipboard" : "Add LDIF copied to clipboard";
        showSuccess(message);
      } catch (Exception e) {
        showError("Failed to copy LDIF: " + e.getMessage());
      }
    }
    
    /**
     * Generates LDIF for adding a new ACI to an entry.
     */
    private String generateAddLdif(String targetDn, String aci) {
      StringBuilder ldif = new StringBuilder();
      ldif.append("# LDIF to add new ACI to entry\n");
      ldif.append("dn: ").append(targetDn).append("\n");
      ldif.append("changetype: modify\n");
      ldif.append("add: aci\n");
      ldif.append("aci: ").append(aci).append("\n");
      ldif.append("-\n");
      return ldif.toString();
    }
    
    /**
     * Generates LDIF for updating an existing ACI.
     */
    private String generateUpdateLdif(String targetDn, String oldAci, String newAci) {
      StringBuilder ldif = new StringBuilder();
      ldif.append("# LDIF to update existing ACI\n");
      ldif.append("dn: ").append(targetDn).append("\n");
      ldif.append("changetype: modify\n");
      ldif.append("delete: aci\n");
      ldif.append("aci: ").append(oldAci).append("\n");
      ldif.append("-\n");
      ldif.append("add: aci\n");
      ldif.append("aci: ").append(newAci).append("\n");
      ldif.append("-\n");
      return ldif.toString();
    }
    
    private static void showSuccess(String message) {
      Notification n = Notification.show(message, 3000, Notification.Position.TOP_END);
      n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void showError(String message) {
      Notification n = Notification.show(message, 5000, Notification.Position.TOP_END);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  /**
   * Data class for entry ACI information.
   */
  public static class EntryAciInfo {
    private final String dn;
    private final String aciValue;
    private final AciParser.ParsedAci parsedAci;

    public EntryAciInfo(String dn, String aciValue) {
      this.dn = dn;
      this.aciValue = aciValue;
      this.parsedAci = AciParser.parseAci(aciValue);
    }

    public String getDn() {
      return dn;
    }

    public String getAciValue() {
      return aciValue;
    }

    public AciParser.ParsedAci getParsedAci() {
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
