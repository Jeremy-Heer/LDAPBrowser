package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.AttributeSyntaxDefinition;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleUseDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* Component for browsing LDAP schema information including object classes,
* attribute types, matching rules, matching rule use, and syntaxes.
*/
public class SchemaBrowser extends VerticalLayout {

  private final LdapService ldapService;
  @SuppressWarnings("unused")
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  // Environment selection removed; driven by ServerSelectionService

  // Server configuration
  private LdapServerConfig serverConfig;

  // Schema data
  private Schema schema;

  // UI Components
  private Tabs schemaTabs;
  private TextField searchField;
  private Button refreshButton;
  private Button addObjectClassButton;
  private Button addAttributeTypeButton;

  // Schema type grids
  private Grid<ObjectClassDefinition> objectClassGrid;
  private Grid<AttributeTypeDefinition> attributeTypeGrid;
  private Grid<MatchingRuleDefinition> matchingRuleGrid;
  private Grid<MatchingRuleUseDefinition> matchingRuleUseGrid;
  private Grid<AttributeSyntaxDefinition> syntaxGrid;

  // Details panels
  private VerticalLayout detailsPanel;
  private VerticalLayout gridContainer;

  // Current view state
  private String currentView = "objectClasses";
  private String currentFilter = "";

  /**
   * Constructs a new {@code SchemaBrowser} component.
   * Initializes the schema browser with the provided LDAP service, configuration service,
   * in-memory LDAP service, and server selection service. Sets up the UI components and layout,
   * and registers a listener for environment selection events.
   *
   * @param ldapService the service for interacting with LDAP servers
   * @param configurationService the service for managing application configuration
   * @param inMemoryLdapService the service for handling in-memory LDAP operations
   * @param selectionService the service for managing server selection and environment changes
   */
  public SchemaBrowser(LdapService ldapService, ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService, ServerSelectionService selectionService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    initializeComponents();
    setupLayout();
    selectionService.addListener(this::onEnvironmentSelected);
  }

  /**
   * Initializes the components for the Schema Browser.
   */
  private void initializeComponents() {
    // Updated indentation to 2 spaces
    // Applied double indentation for line wraps exceeding 100 columns

    // Environment dropdown removed

    // Search controls
    searchField = new TextField();
    searchField.setPlaceholder("Search schema elements...");
    searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(e -> {
      currentFilter = e.getValue();
      filterCurrentView();
    });
    searchField.setWidth("300px");

    refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
    refreshButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    refreshButton.addClickListener(e -> loadSchema());

    // Add schema element buttons
    addObjectClassButton = new Button("Add Object Class", new Icon(VaadinIcon.PLUS));
    addObjectClassButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
    addObjectClassButton.addClickListener(e -> openAddObjectClassDialog());
    addObjectClassButton.setEnabled(false);

    addAttributeTypeButton = new Button("Add Attribute Type", new Icon(VaadinIcon.PLUS));
    addAttributeTypeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
    addAttributeTypeButton.addClickListener(e -> openAddAttributeTypeDialog());
    addAttributeTypeButton.setEnabled(false);

    // Schema tabs
    schemaTabs = new Tabs();
    Tab objectClassTab = new Tab("Object Classes");
    Tab attributeTypeTab = new Tab("Attribute Types");
    Tab matchingRuleTab = new Tab("Matching Rules");
    Tab matchingRuleUseTab = new Tab("Matching Rule Use");
    Tab syntaxTab = new Tab("Syntaxes");

    schemaTabs.add(objectClassTab, attributeTypeTab, matchingRuleTab, matchingRuleUseTab,
        syntaxTab);
    schemaTabs.addSelectedChangeListener(e -> {
      Tab selectedTab = e.getSelectedTab();
      if (selectedTab == objectClassTab) {
        currentView = "objectClasses";
        showObjectClasses();
        updateAddButtons();
      } else if (selectedTab == attributeTypeTab) {
        currentView = "attributeTypes";
        showAttributeTypes();
        updateAddButtons();
      } else if (selectedTab == matchingRuleTab) {
        currentView = "matchingRules";
        showMatchingRules();
        updateAddButtons();
      } else if (selectedTab == matchingRuleUseTab) {
        currentView = "matchingRuleUse";
        showMatchingRuleUse();
        updateAddButtons();
      } else if (selectedTab == syntaxTab) {
        currentView = "syntaxes";
        showSyntaxes();
        updateAddButtons();
      }
    });

    // Initialize grids
    initializeObjectClassGrid();
    initializeAttributeTypeGrid();
    initializeMatchingRuleGrid();
    initializeMatchingRuleUseGrid();
    initializeSyntaxGrid();

    // Details panel
    detailsPanel = new VerticalLayout();
    detailsPanel.setSizeFull();
    detailsPanel.setPadding(true);
    detailsPanel.setSpacing(true);
    detailsPanel.addClassName("schema-details");
    detailsPanel.add(new Span("Select a schema element to view details"));
  }

  /**
   * Adds a class name to the object class grid and sets up its columns.
   */
  private void initializeObjectClassGrid() {
    objectClassGrid = new Grid<>();
    objectClassGrid.setSizeFull();
    objectClassGrid.addClassName("schema-grid");

    objectClassGrid.addColumn(ObjectClassDefinition::getNameOrOID)
        .setHeader("Name")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    objectClassGrid.addColumn(oc -> oc.getDescription() != null ? oc.getDescription() : "")
        .setHeader("Description")
        .setFlexGrow(3)
        .setResizable(true)
        .setSortable(true);

    objectClassGrid.addColumn(oc -> 
      oc.getObjectClassType() != null 
        ? oc.getObjectClassType().getName() 
        : ""
      )
        .setHeader("Type")
        .setFlexGrow(1)
        .setResizable(true)
        .setSortable(true);

    objectClassGrid.addColumn(oc -> oc.isObsolete() ? "Yes" : "No")
        .setHeader("Obsolete")
        .setFlexGrow(1)
        .setResizable(true)
        .setSortable(true);

    // Schema File column from X-Schema-file extension (when available)
    objectClassGrid.addColumn(oc -> {
      try {
        Map<String, String[]> ext = oc.getExtensions();
        if (ext != null) {
          String[] vals = ext.get("X-SCHEMA-FILE");
          if (vals == null) vals = ext.get("X-Schema-File");
          if (vals == null) vals = ext.get("X-Schema-file");
          if (vals == null) vals = ext.get("x-schema-file");
          if (vals != null && vals.length > 0) {
            return String.join(", ", vals);
          }
        }
      } catch (Exception ignored) {}
      return "";
    })
        .setHeader("Schema File")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    objectClassGrid.asSingleSelect().addValueChangeListener(e -> {
      if (e.getValue() != null) {
        showObjectClassDetails(e.getValue());
      }
    });
  }

  private void initializeAttributeTypeGrid() {
    attributeTypeGrid = new Grid<>();
    attributeTypeGrid.setSizeFull();
    attributeTypeGrid.addClassName("schema-grid");

    attributeTypeGrid.addColumn(AttributeTypeDefinition::getNameOrOID)
        .setHeader("Name")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    attributeTypeGrid.addColumn(at -> at.getDescription() != null ? at.getDescription() : "")
        .setHeader("Description")
        .setFlexGrow(3)
        .setResizable(true)
        .setSortable(true);

    attributeTypeGrid.addColumn(at -> at.getSyntaxOID() != null ? at.getSyntaxOID() : "")
        .setHeader("Syntax OID")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    attributeTypeGrid.addColumn(at -> at.isObsolete() ? "Yes" : "No")
        .setHeader("Obsolete")
        .setFlexGrow(1)
        .setResizable(true)
        .setSortable(true);

    // Schema File column from X-Schema-file extension (when available)
    attributeTypeGrid.addColumn(at -> {
      try {
        Map<String, String[]> ext = at.getExtensions();
        if (ext != null) {
          String[] vals = ext.get("X-SCHEMA-FILE");
          if (vals == null) vals = ext.get("X-Schema-File");
          if (vals == null) vals = ext.get("X-Schema-file");
          if (vals == null) vals = ext.get("x-schema-file");
          if (vals != null && vals.length > 0) {
            return String.join(", ", vals);
          }
        }
      } catch (Exception ignored) {}
      return "";
    })
        .setHeader("Schema File")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    attributeTypeGrid.asSingleSelect().addValueChangeListener(e -> {
      if (e.getValue() != null) {
        showAttributeTypeDetails(e.getValue());
      }
    });
  }

  private void initializeMatchingRuleGrid() {
    matchingRuleGrid = new Grid<>();
    matchingRuleGrid.setSizeFull();
    matchingRuleGrid.addClassName("schema-grid");

    matchingRuleGrid.addColumn(MatchingRuleDefinition::getNameOrOID)
        .setHeader("Name")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    matchingRuleGrid.addColumn(mr -> mr.getDescription() != null ? mr.getDescription() : "")
        .setHeader("Description")
        .setFlexGrow(3)
        .setResizable(true)
        .setSortable(true);

    matchingRuleGrid.addColumn(mr -> mr.getSyntaxOID() != null ? mr.getSyntaxOID() : "")
        .setHeader("Syntax OID")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    matchingRuleGrid.addColumn(mr -> mr.isObsolete() ? "Yes" : "No")
        .setHeader("Obsolete")
        .setFlexGrow(1)
        .setResizable(true)
        .setSortable(true);

    matchingRuleGrid.asSingleSelect().addValueChangeListener(e -> {
      if (e.getValue() != null) {
        showMatchingRuleDetails(e.getValue());
      }
    });
  }

  private void initializeMatchingRuleUseGrid() {
    matchingRuleUseGrid = new Grid<>();
    matchingRuleUseGrid.setSizeFull();
    matchingRuleUseGrid.addClassName("schema-grid");

    matchingRuleUseGrid.addColumn(MatchingRuleUseDefinition::getOID)
        .setHeader("OID")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    matchingRuleUseGrid.addColumn(mru -> mru.getDescription() != null ? mru.getDescription() : "")
        .setHeader("Description")
        .setFlexGrow(3)
        .setResizable(true)
        .setSortable(true);

    matchingRuleUseGrid.addColumn(mru -> mru.isObsolete() ? "Yes" : "No")
        .setHeader("Obsolete")
        .setFlexGrow(1)
        .setResizable(true)
        .setSortable(true);

    matchingRuleUseGrid.asSingleSelect().addValueChangeListener(e -> {
      if (e.getValue() != null) {
        showMatchingRuleUseDetails(e.getValue());
      }
    });
  }

  private void initializeSyntaxGrid() {
    syntaxGrid = new Grid<>();
    syntaxGrid.setSizeFull();
    syntaxGrid.addClassName("schema-grid");

    syntaxGrid.addColumn(AttributeSyntaxDefinition::getOID)
        .setHeader("OID")
        .setFlexGrow(2)
        .setResizable(true)
        .setSortable(true);

    syntaxGrid.addColumn(syn -> syn.getDescription() != null ? syn.getDescription() : "")
        .setHeader("Description")
        .setFlexGrow(4)
        .setResizable(true)
        .setSortable(true);

    syntaxGrid.asSingleSelect().addValueChangeListener(e -> {
      if (e.getValue() != null) {
        showSyntaxDetails(e.getValue());
      }
    });
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(false);
    setSpacing(false);

    // Create left panel with schema browser controls and search results
    VerticalLayout leftPanel = new VerticalLayout();
    leftPanel.setSizeFull();
    leftPanel.setPadding(false);
    leftPanel.setSpacing(false);
    leftPanel.addClassName("ds-panel");

    // Left panel header with Schema Browser title, search field and refresh button
    HorizontalLayout schemaHeader = new HorizontalLayout();
    schemaHeader.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    schemaHeader.setPadding(true);
    schemaHeader.addClassName("ds-panel-header");
    schemaHeader.getStyle().set("margin-bottom", "0px");

    Icon schemaIcon = new Icon(VaadinIcon.COGS);
    schemaIcon.setSize("16px");
    schemaIcon.getStyle().set("color", "#4a90e2");

    H3 title = new H3("Schema Browser");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle()
      .set("font-size", "0.9em")
      .set("font-weight", "600")
      .set("color", "#333");

    schemaHeader.add(
      schemaIcon,
      title,
      searchField,
      addObjectClassButton,
      addAttributeTypeButton,
      refreshButton
    );
    schemaHeader.setFlexGrow(1, title);

    // Schema tabs container
    VerticalLayout schemaTabsContainer = new VerticalLayout();
    schemaTabsContainer.setSizeFull();
    schemaTabsContainer.setPadding(false);
    schemaTabsContainer.setSpacing(false);

    schemaTabsContainer.add(schemaTabs);

    // Grid container for search results
    gridContainer = new VerticalLayout();
    gridContainer.setSizeFull();
    gridContainer.setPadding(false);
    gridContainer.setSpacing(false);
    gridContainer.addClassName("schema-grid-container");

    // Initially show object classes
    gridContainer.add(objectClassGrid);

    schemaTabsContainer.add(gridContainer);
    schemaTabsContainer.setFlexGrow(1, gridContainer);

    // Apply grid styling and remove spacing
    gridContainer.getStyle().set("margin", "0px");
    gridContainer.getStyle().set("padding", "0px");

    leftPanel.add(schemaHeader, schemaTabsContainer);
    leftPanel.setFlexGrow(1, schemaTabsContainer);
    leftPanel.getStyle().set("gap", "0px"); // Remove any gap between components

    // Create right panel with environment dropdown and details
    VerticalLayout rightPanel = new VerticalLayout();
    rightPanel.setSizeFull();
    rightPanel.setPadding(false);
    rightPanel.setSpacing(false);

    // Environment dropdown layout - positioned at the top right
    HorizontalLayout environmentLayout = new HorizontalLayout();
    environmentLayout.setWidthFull();
    environmentLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    environmentLayout.setPadding(false);
    environmentLayout.getStyle().set("padding-left", "var(--lumo-space-m)");
    environmentLayout.getStyle().set("padding-right", "var(--lumo-space-m)");
    environmentLayout.getStyle().set("padding-top", "var(--lumo-space-xs)");
    environmentLayout.getStyle().set("padding-bottom", "var(--lumo-space-xs)");

    // Add spacer to push environment dropdown to the right
    Span spacer = new Span();
    // environment selection UI removed
    environmentLayout.setFlexGrow(1, spacer);

    // Details container
    VerticalLayout detailsContainer = new VerticalLayout();
    detailsContainer.setSizeFull();
    detailsContainer.setPadding(false);
    detailsContainer.setSpacing(false);
    detailsContainer.addClassName("schema-details-container");

    HorizontalLayout detailsHeader = new HorizontalLayout();
    detailsHeader.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    detailsHeader.setPadding(true);
    detailsHeader.addClassName("ds-panel-header");

    Icon detailsIcon = new Icon(VaadinIcon.INFO_CIRCLE);
    detailsIcon.setSize("14px");
    H3 detailsTitle = new H3("Schema Details");
    detailsTitle.addClassNames(LumoUtility.Margin.NONE);
    detailsTitle.getStyle()
      .set("font-size", "0.9em")
      .set("font-weight", "600")
      .set("color", "#333");
    detailsHeader.add(detailsIcon, detailsTitle);

    detailsContainer.add(detailsHeader, detailsPanel);
    detailsContainer.setFlexGrow(1, detailsPanel);

    // Add environment layout and details to right panel
    rightPanel.add(environmentLayout, detailsContainer);
    rightPanel.setFlexGrow(1, detailsContainer);

    // Create main split layout (horizontal)
    SplitLayout mainHorizontalSplit = new SplitLayout();
    mainHorizontalSplit.setSizeFull();
    mainHorizontalSplit.setSplitterPosition(70); // 70% for left panel, 30% for details
    mainHorizontalSplit.addToPrimary(leftPanel);
    mainHorizontalSplit.addToSecondary(rightPanel);

    add(mainHorizontalSplit);
    setFlexGrow(1, mainHorizontalSplit);
  }private void onEnvironmentSelected(LdapServerConfig environment) {
    this.serverConfig = environment;
    if (environment != null) {
      loadSchema();
    } else {
      clear();
    }
  }

  public void setServerConfig(LdapServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    loadSchema();
  }

  private void loadSchema() {
    if (serverConfig == null) {
      showError("No server selected");
      return;
    }

    // Check if connected to the server
    if (!ldapService.isConnected(serverConfig.getId())) {
      // Try to connect first
      try {
        ldapService.connect(serverConfig);
        showSuccess("Connected to " + serverConfig.getName());
      } catch (Exception e) {
        showError("Failed to connect to server " + serverConfig.getName() + ": " + e.getMessage());
        schema = null;
        return;
      }
    }

    try {
      schema = ldapService.getSchema(serverConfig.getId());
      if (schema != null) {
        showObjectClasses(); // Default view
        updateAddButtons();
        showSuccess("Schema loaded successfully");
      } else {
        showError("No schema information available");
        updateAddButtons();
      }
    } catch (LDAPException e) {
      showError("Failed to load schema: " + e.getMessage());
      schema = null;
      updateAddButtons();
    }
  }

  private void showObjectClasses() {
    if (schema == null) return;

    clearGridContainer();
    getGridContainer().add(objectClassGrid);

    Collection<ObjectClassDefinition> objectClasses = schema.getObjectClasses();
    List<ObjectClassDefinition> filtered = filterObjectClasses(objectClasses);
    objectClassGrid.setItems(filtered);
  }

  private void showAttributeTypes() {
    if (schema == null) return;

    clearGridContainer();
    getGridContainer().add(attributeTypeGrid);

    Collection<AttributeTypeDefinition> attributeTypes = schema.getAttributeTypes();
    List<AttributeTypeDefinition> filtered = filterAttributeTypes(attributeTypes);
    attributeTypeGrid.setItems(filtered);
  }

  private void showMatchingRules() {
    if (schema == null) return;

    clearGridContainer();
    getGridContainer().add(matchingRuleGrid);

    Collection<MatchingRuleDefinition> matchingRules = schema.getMatchingRules();
    List<MatchingRuleDefinition> filtered = filterMatchingRules(matchingRules);
    matchingRuleGrid.setItems(filtered);
  }

  private void showMatchingRuleUse() {
    if (schema == null) return;

    clearGridContainer();
    getGridContainer().add(matchingRuleUseGrid);

    Collection<MatchingRuleUseDefinition> matchingRuleUses = schema.getMatchingRuleUses();
    List<MatchingRuleUseDefinition> filtered = filterMatchingRuleUse(matchingRuleUses);
    matchingRuleUseGrid.setItems(filtered);
  }

  private void showSyntaxes() {
    if (schema == null) return;

    clearGridContainer();
    getGridContainer().add(syntaxGrid);

    Collection<AttributeSyntaxDefinition> syntaxes = schema.getAttributeSyntaxes();
    List<AttributeSyntaxDefinition> filtered = filterSyntaxes(syntaxes);
    syntaxGrid.setItems(filtered);
  }

  private VerticalLayout getGridContainer() {
    return gridContainer;
  }

  private void clearGridContainer() {
    getGridContainer().removeAll();
  }

  /**
   * Filters the current view based on the selected schema type.
   */
  private void filterCurrentView() {
    switch (currentView) {
      case "objectClasses":
        showObjectClasses();
        break;
      case "attributeTypes":
        showAttributeTypes();
        break;
      case "matchingRules":
        showMatchingRules();
        break;
      case "matchingRuleUse":
        showMatchingRuleUse();
        break;
      case "syntaxes":
        showSyntaxes();
        break;
    }
  }

  private List<ObjectClassDefinition> filterObjectClasses(
      Collection<ObjectClassDefinition> objectClasses) {
    if (currentFilter == null || currentFilter.trim().isEmpty()) {
      return new ArrayList<>(objectClasses);
    }

    String filter = currentFilter.toLowerCase().trim();
    return objectClasses.stream()
      .filter(oc ->
        (oc.getNameOrOID() != null &&
          oc.getNameOrOID().toLowerCase().contains(filter)) ||
        (oc.getDescription() != null &&
          oc.getDescription().toLowerCase().contains(filter)) ||
        (oc.getOID() != null &&
          oc.getOID().toLowerCase().contains(filter))
      )
      .sorted(Comparator.comparing(ObjectClassDefinition::getNameOrOID))
      .collect(Collectors.toList());
  }

  private List<AttributeTypeDefinition> filterAttributeTypes(
      Collection<AttributeTypeDefinition> attributeTypes) {
    if (currentFilter == null || currentFilter.trim().isEmpty()) {
      return new ArrayList<>(attributeTypes);
    }

    String filter = currentFilter.toLowerCase().trim();
    return attributeTypes.stream()
      .filter(at ->
        (at.getNameOrOID() != null &&
          at.getNameOrOID().toLowerCase().contains(filter)) ||
        (at.getDescription() != null &&
          at.getDescription().toLowerCase().contains(filter)) ||
        (at.getOID() != null &&
          at.getOID().toLowerCase().contains(filter))
      )
      .sorted(Comparator.comparing(AttributeTypeDefinition::getNameOrOID))
      .collect(Collectors.toList());
  }

  private List<MatchingRuleDefinition> filterMatchingRules(
      Collection<MatchingRuleDefinition> matchingRules) {
    if (currentFilter == null || currentFilter.trim().isEmpty()) {
      return new ArrayList<>(matchingRules);
    }

    String filter = currentFilter.toLowerCase().trim();
    return matchingRules.stream()
      .filter(mr ->
        (mr.getNameOrOID() != null &&
          mr.getNameOrOID().toLowerCase().contains(filter)) ||
        (mr.getDescription() != null &&
          mr.getDescription().toLowerCase().contains(filter)) ||
        (mr.getOID() != null &&
          mr.getOID().toLowerCase().contains(filter))
      )
      .sorted(Comparator.comparing(MatchingRuleDefinition::getNameOrOID))
      .collect(Collectors.toList());
  }

  private List<MatchingRuleUseDefinition> filterMatchingRuleUse(
      Collection<MatchingRuleUseDefinition> matchingRuleUses) {
    if (currentFilter == null || currentFilter.trim().isEmpty()) {
      return new ArrayList<>(matchingRuleUses);
    }

    String filter = currentFilter.toLowerCase().trim();
    return matchingRuleUses.stream()
      .filter(mru ->
        (mru.getOID() != null && mru.getOID().toLowerCase().contains(filter)) ||
        (mru.getDescription() != null &&
          mru.getDescription().toLowerCase().contains(filter))
      )
      .sorted(Comparator.comparing(MatchingRuleUseDefinition::getOID))
      .collect(Collectors.toList());
  }

  private List<AttributeSyntaxDefinition> filterSyntaxes(
      Collection<AttributeSyntaxDefinition> syntaxes) {
    if (currentFilter == null || currentFilter.trim().isEmpty()) {
      return new ArrayList<>(syntaxes);
    }

    String filter = currentFilter.toLowerCase().trim();
    return syntaxes.stream()
      .filter(syn ->
        (syn.getOID() != null && syn.getOID().toLowerCase().contains(filter)) ||
        (syn.getDescription() != null &&
          syn.getDescription().toLowerCase().contains(filter))
      )
      .sorted(Comparator.comparing(AttributeSyntaxDefinition::getOID))
      .collect(Collectors.toList());
  }

  private void showObjectClassDetails(ObjectClassDefinition objectClass) {
    detailsPanel.removeAll();

    VerticalLayout details = new VerticalLayout();
    details.setSpacing(true);
    details.setPadding(true);

    // Header
    H3 header = new H3("Object Class: " + objectClass.getNameOrOID());
    header.getStyle().set("margin-bottom", "16px");
    details.add(header);

    // Basic information
    addDetailRow(details, "OID", objectClass.getOID());
    addDetailRow(details, "Names", objectClass.getNames() != null ?
        String.join(", ", Arrays.asList(objectClass.getNames())) : "");
    addDetailRow(details, "Description", objectClass.getDescription());
    // Schema file (extension)
    String ocSchemaFile = getSchemaFileFromExtensions(objectClass.getExtensions());
    addDetailRow(details, "Schema File", ocSchemaFile);
    addDetailRow(details, "Type", objectClass.getObjectClassType() != null ?
        objectClass.getObjectClassType().getName() : "");
    addDetailRow(details, "Obsolete", objectClass.isObsolete() ? "Yes" : "No");

    // Superior classes
    if (objectClass.getSuperiorClasses() != null
      && objectClass.getSuperiorClasses().length > 0) {
      addDetailRow(details, "Superior Classes",
        String.join(", ", objectClass.getSuperiorClasses()));
    }

    // Required attributes
    if (objectClass.getRequiredAttributes() != null
      && objectClass.getRequiredAttributes().length > 0) {
      addDetailRow(details, "Required Attributes",
        String.join(", ", objectClass.getRequiredAttributes()));
    }

    // Optional attributes
    if (objectClass.getOptionalAttributes() != null
      && objectClass.getOptionalAttributes().length > 0) {
      addDetailRow(details, "Optional Attributes",
        String.join(", ", objectClass.getOptionalAttributes()));
    }

    // Extensions (additional properties)
    if (objectClass.getExtensions() != null && !objectClass.getExtensions().isEmpty()) {
      StringBuilder extensions = new StringBuilder();
      for (Map.Entry<String, String[]> entry : objectClass.getExtensions().entrySet()) {
        if (extensions.length() > 0) extensions.append(", ");
        extensions.append(entry.getKey()).append("=").append(String.join(",", entry.getValue()));
      }
      addDetailRow(details, "Extensions", extensions.toString());
    }

    detailsPanel.add(details);
  }

  private void showAttributeTypeDetails(AttributeTypeDefinition attributeType) {
    detailsPanel.removeAll();

    VerticalLayout details = new VerticalLayout();
    details.setSpacing(true);
    details.setPadding(true);

    // Header
    H3 header = new H3("Attribute Type: " + attributeType.getNameOrOID());
    header.getStyle().set("margin-bottom", "16px");
    details.add(header);

    // Basic information
    addDetailRow(details, "OID", attributeType.getOID());
    addDetailRow(details, "Names", attributeType.getNames() != null ?
        String.join(", ", Arrays.asList(attributeType.getNames())) : "");
    addDetailRow(details, "Description", attributeType.getDescription());
    // Schema file (extension)
    String atSchemaFile = getSchemaFileFromExtensions(attributeType.getExtensions());
    addDetailRow(details, "Schema File", atSchemaFile);
    addDetailRow(details, "Syntax OID", attributeType.getSyntaxOID());
    addDetailRow(details, "Obsolete", attributeType.isObsolete() ? "Yes" : "No");
    addDetailRow(details, "Single Value", attributeType.isSingleValued() ? "Yes" : "No");
    addDetailRow(details, "Collective", attributeType.isCollective() ? "Yes" : "No");
    addDetailRow(details, "No User Modification",
      attributeType.isNoUserModification() ? "Yes" : "No");

    // Usage
    if (attributeType.getUsage() != null) {
      addDetailRow(details, "Usage", attributeType.getUsage().getName());
    }

    // Superior type
    if (attributeType.getSuperiorType() != null) {
      addDetailRow(details, "Superior Type", attributeType.getSuperiorType());
    }

    // Equality matching rule
    if (attributeType.getEqualityMatchingRule() != null) {
      addDetailRow(details, "Equality Matching Rule", attributeType.getEqualityMatchingRule());
    }

    // Ordering matching rule
    if (attributeType.getOrderingMatchingRule() != null) {
      addDetailRow(details, "Ordering Matching Rule", attributeType.getOrderingMatchingRule());
    }

    // Substring matching rule
    if (attributeType.getSubstringMatchingRule() != null) {
      addDetailRow(details, "Substring Matching Rule", attributeType.getSubstringMatchingRule());
    }

    detailsPanel.add(details);
  }

  // Helper to read the X-Schema-file extension (supports common casings)
  private String getSchemaFileFromExtensions(Map<String, String[]> extensions) {
    if (extensions == null || extensions.isEmpty()) return null;
    String[] keys = new String[] {
      "X-SCHEMA-FILE", "X-Schema-File", "X-Schema-file", "x-schema-file"
    };
    for (String k : keys) {
      String[] vals = extensions.get(k);
      if (vals != null && vals.length > 0) {
        return String.join(", ", vals);
      }
    }
    return null;
  }

  private void showMatchingRuleDetails(MatchingRuleDefinition matchingRule) {
    detailsPanel.removeAll();

    VerticalLayout details = new VerticalLayout();
    details.setSpacing(true);
    details.setPadding(true);

    // Header
    H3 header = new H3("Matching Rule: " + matchingRule.getNameOrOID());
    header.getStyle().set("margin-bottom", "16px");
    details.add(header);

    // Basic information
    addDetailRow(details, "OID", matchingRule.getOID());
    addDetailRow(details, "Names", matchingRule.getNames() != null ?
        String.join(", ", Arrays.asList(matchingRule.getNames())) : "");
    addDetailRow(details, "Description", matchingRule.getDescription());
    addDetailRow(details, "Syntax OID", matchingRule.getSyntaxOID());
    addDetailRow(details, "Obsolete", matchingRule.isObsolete() ? "Yes" : "No");

    detailsPanel.add(details);
  }

  private void showMatchingRuleUseDetails(MatchingRuleUseDefinition matchingRuleUse) {
    detailsPanel.removeAll();

    VerticalLayout details = new VerticalLayout();
    details.setSpacing(true);
    details.setPadding(true);

    // Header
    H3 header = new H3("Matching Rule Use: " + matchingRuleUse.getOID());
    header.getStyle().set("margin-bottom", "16px");
    details.add(header);

    // Basic information
    addDetailRow(details, "OID", matchingRuleUse.getOID());
    addDetailRow(details, "Names", matchingRuleUse.getNames() != null ?
        String.join(", ", Arrays.asList(matchingRuleUse.getNames())) : "");
    addDetailRow(details, "Description", matchingRuleUse.getDescription());
    addDetailRow(details, "Obsolete", matchingRuleUse.isObsolete() ? "Yes" : "No");

    // Applicable attribute types
    if (matchingRuleUse.getApplicableAttributeTypes() != null &&
          matchingRuleUse.getApplicableAttributeTypes().length > 0) {
      addDetailRow(details, "Applicable Attribute Types",
            String.join(", ", matchingRuleUse.getApplicableAttributeTypes()));
    }

    detailsPanel.add(details);
  }

  private void showSyntaxDetails(AttributeSyntaxDefinition syntax) {
    detailsPanel.removeAll();

    VerticalLayout details = new VerticalLayout();
    details.setSpacing(true);
    details.setPadding(true);

    // Header
    H3 header = new H3("Syntax: " + syntax.getOID());
    header.getStyle().set("margin-bottom", "16px");
    details.add(header);

    // Basic information
    addDetailRow(details, "OID", syntax.getOID());
    addDetailRow(details, "Description", syntax.getDescription());

    detailsPanel.add(details);
  }

  private void addDetailRow(VerticalLayout parent, String label, String value) {
    if (value != null && !value.trim().isEmpty()) {
      HorizontalLayout row = new HorizontalLayout();
      row.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.START);
      row.setSpacing(true);

      Span labelSpan = new Span(label + ":");
      labelSpan.getStyle().set("font-weight", "bold").set("min-width", "150px");

      Span valueSpan = new Span(value);
      valueSpan.getStyle().set("word-wrap", "break-word");

      row.add(labelSpan, valueSpan);
      row.setFlexGrow(1, valueSpan);

      parent.add(row);
    }
  }

  /**
   * Clears the current schema and resets all UI components to their default state.
   * <p>
   * This includes:
   * <ul>
   *   <li>Setting the schema to {@code null}</li>
   *   <li>Clearing all grids (object classes, attribute types, matching rules,
   *       matching rule uses, syntaxes)</li>
   *   <li>Removing all components from the details panel and displaying a default message</li>
   *   <li>Updating the state of add buttons</li>
   * </ul>
   */
  public void clear() {
    schema = null;
    objectClassGrid.setItems();
    attributeTypeGrid.setItems();
    matchingRuleGrid.setItems();
    matchingRuleUseGrid.setItems();
    syntaxGrid.setItems();
    detailsPanel.removeAll();
    detailsPanel.add(new Span("Select a schema element to view details"));
    updateAddButtons();
  }

  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showError(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  public void refreshEnvironments() {
    // No environment dropdown to refresh
  }

  /**
  * Update visibility of add buttons based on current view and server capabilities.
  */
  private void updateAddButtons() {
    boolean hasSchema = schema != null && serverConfig != null;
    boolean canAddSchema = hasSchema && canModifySchema();

    if ("objectClasses".equals(currentView)) {
      addObjectClassButton.setVisible(true);
      addAttributeTypeButton.setVisible(false);
    } else if ("attributeTypes".equals(currentView)) {
      addObjectClassButton.setVisible(false);
      addAttributeTypeButton.setVisible(true);
    } else {
      addObjectClassButton.setVisible(false);
      addAttributeTypeButton.setVisible(false);
    }

    addObjectClassButton.setEnabled(canAddSchema);
    addAttributeTypeButton.setEnabled(canAddSchema);
  }

  /**
  * Check if the current server supports schema modifications.
  */
  private boolean canModifySchema() {
    if (serverConfig == null) {
      return false;
    }

    // For in-memory servers, always allow schema modifications
    if (inMemoryLdapService.isInMemoryServer(serverConfig.getId())) {
      return true;
    }

    // For external servers, check if they support schema modifications
    try {
      return ldapService.supportsSchemaModification(serverConfig.getId());
    } catch (Exception e) {
      return false;
    }
  }

  /**
  * Get available object class names from schema for selectors.
  */
  private List<String> getAvailableObjectClassNames() {
    List<String> objectClassNames = new ArrayList<>();
    if (schema != null) {
      try {
        Collection<ObjectClassDefinition> objectClasses = schema.getObjectClasses();
        for (ObjectClassDefinition oc : objectClasses) {
          String name = oc.getNameOrOID();
          if (name != null && !name.trim().isEmpty()) {
            objectClassNames.add(name);
          }
        }
        objectClassNames.sort(String.CASE_INSENSITIVE_ORDER);
      } catch (Exception e) {
        // If schema access fails, return empty list
      }
    }
    return objectClassNames;
  }

  /**
  * Get available attribute type names from schema for selectors.
  */
  private List<String> getAvailableAttributeTypeNames() {
    List<String> attributeNames = new ArrayList<>();
    if (schema != null) {
      try {
        Collection<AttributeTypeDefinition> attributeTypes = schema.getAttributeTypes();
        for (AttributeTypeDefinition at : attributeTypes) {
          String name = at.getNameOrOID();
          if (name != null && !name.trim().isEmpty()) {
            attributeNames.add(name);
          }
        }
        attributeNames.sort(String.CASE_INSENSITIVE_ORDER);
      } catch (Exception e) {
        // If schema access fails, return empty list
      }
    }
    return attributeNames;
  }

  /**
  * Get available matching rule names from schema for selectors.
  */
  private List<String> getAvailableMatchingRuleNames() {
    List<String> matchingRuleNames = new ArrayList<>();
    if (schema != null) {
      try {
        Collection<MatchingRuleDefinition> matchingRules = schema.getMatchingRules();
        for (MatchingRuleDefinition mr : matchingRules) {
          String name = mr.getNameOrOID();
          if (name != null && !name.trim().isEmpty()) {
            matchingRuleNames.add(name);
          }
        }
        matchingRuleNames.sort(String.CASE_INSENSITIVE_ORDER);
      } catch (Exception e) {
        // If schema access fails, return empty list
      }
    }
    return matchingRuleNames;
  }

  /**
  * Get available syntax OIDs from schema for selectors.
  */
  private List<String> getAvailableSyntaxOIDs() {
    List<String> syntaxOIDs = new ArrayList<>();
    if (schema != null) {
      try {
        Collection<AttributeSyntaxDefinition> syntaxes = schema.getAttributeSyntaxes();
        for (AttributeSyntaxDefinition syn : syntaxes) {
          String oid = syn.getOID();
          if (oid != null && !oid.trim().isEmpty()) {
            syntaxOIDs.add(oid);
          }
        }
        syntaxOIDs.sort(String.CASE_INSENSITIVE_ORDER);
      } catch (Exception e) {
        // If schema access fails, return empty list
      }
    }
    return syntaxOIDs;
  }

  /**
  * Create a multi-select component with existing items from schema.
  */
  private MultiSelectComboBox<String> createSchemaMultiSelect(String label, String placeholder,
      List<String> items) {
    MultiSelectComboBox<String> multiSelect = new MultiSelectComboBox<>(label);
    multiSelect.setPlaceholder(placeholder);
    multiSelect.setItems(items);
    multiSelect.setWidth("100%");
    return multiSelect;
  }

  /**
  * Create a combobox component with existing items from schema.
  */
  private ComboBox<String> createSchemaComboBox(String label, String placeholder,
      List<String> items) {
    ComboBox<String> comboBox = new ComboBox<>(label);
    comboBox.setPlaceholder(placeholder);
    comboBox.setItems(items);
    comboBox.setWidth("100%");
    comboBox.setAllowCustomValue(true);
    comboBox.addCustomValueSetListener(e -> {
      String customValue = e.getDetail();
      if (customValue != null && !customValue.trim().isEmpty()) {
        comboBox.setValue(customValue);
      }
    });
    return comboBox;
  }

  /**
  * Open dialog for adding a new object class.
  */
  private void openAddObjectClassDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Add Object Class");
    dialog.setWidth("700px");
    dialog.setHeight("800px");

    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

    // Add warning for external servers.
    if (serverConfig != null && !inMemoryLdapService.isInMemoryServer(serverConfig.getId())) {
      Span warningSpan = new Span(
          "⚠️ Adding schema elements to external LDAP servers may require administrator "
          + "privileges and could affect production systems.");
      warningSpan.getStyle().set("color", "#ff6b35");
      warningSpan.getStyle().set("font-size", "0.875rem");
      warningSpan.getStyle().set("margin-bottom", "1rem");
      warningSpan.getStyle().set("display", "block");
      formLayout.add(warningSpan);
    }

    // Basic fields
    TextField nameField = new TextField("Name*");
    nameField.setRequired(true);

    TextField oidField = new TextField("OID*");
    oidField.setRequired(true);
    oidField.setHelperText("Object identifier (e.g., 1.2.3.4.5.6.7.8)");

    TextField descriptionField = new TextField("Description");

    ComboBox<String> typeComboBox = new ComboBox<>("Type");
    typeComboBox.setItems("STRUCTURAL", "AUXILIARY", "ABSTRACT");
    typeComboBox.setValue("STRUCTURAL");

    Checkbox obsoleteCheckbox = new Checkbox("Obsolete");

    // Get available schema elements for selectors
    List<String> availableObjectClasses = getAvailableObjectClassNames();
    List<String> availableAttributes = getAvailableAttributeTypeNames();

    // Superior classes - enhanced with selector
    MultiSelectComboBox<String> superiorClassesSelector = createSchemaMultiSelect(
        "Superior Classes (Select)", 
        "Choose from existing object classes...", 
        availableObjectClasses
    );
    superiorClassesSelector.setHelperText("Select from existing object classes or type new ones");

    TextArea superiorClassesField = new TextArea("Superior Classes (Manual Entry)");
    superiorClassesField.setHelperText(
        "One per line (e.g., top, person) - Alternative to selector above");
    superiorClassesField.setHeight("60px");

    // Required attributes - enhanced with selector
    MultiSelectComboBox<String> requiredAttributesSelector = createSchemaMultiSelect(
        "Required Attributes (MUST) - Select", 
        "Choose from existing attributes...", 
        availableAttributes
    );
    requiredAttributesSelector.setHelperText(
        "Select from existing attribute types or type new ones");

    TextArea requiredAttributesField = new TextArea("Required Attributes (MUST) - Manual Entry");
    requiredAttributesField.setHelperText(
        "One per line (e.g., cn, sn, objectClass) - Alternative to selector above");
    requiredAttributesField.setHeight("80px");

    // Optional attributes - enhanced with selector
    MultiSelectComboBox<String> optionalAttributesSelector = createSchemaMultiSelect(
        "Optional Attributes (MAY) - Select", 
        "Choose from existing attributes...", 
        availableAttributes
    );
    optionalAttributesSelector.setHelperText(
        "Select from existing attribute types or type new ones");

    TextArea optionalAttributesField = new TextArea("Optional Attributes (MAY) - Manual Entry");
    optionalAttributesField.setHelperText(
        "One per line (e.g., mail, telephoneNumber) - Alternative to selector above");
    optionalAttributesField.setHeight("80px");

    formLayout.add(nameField, oidField, descriptionField, typeComboBox, obsoleteCheckbox,
        superiorClassesSelector, superiorClassesField,
        requiredAttributesSelector, requiredAttributesField,
        optionalAttributesSelector, optionalAttributesField);

    // Buttons
    Button saveButton = new Button("Add Object Class", e -> {
      if (validateAndSaveObjectClass(dialog, nameField, oidField, descriptionField,
          typeComboBox, obsoleteCheckbox, 
          superiorClassesSelector, superiorClassesField,
          requiredAttributesSelector, requiredAttributesField,
          optionalAttributesSelector, optionalAttributesField)) {
        dialog.close();
      }
    });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> dialog.close());

    dialog.add(formLayout);
    dialog.getFooter().add(cancelButton, saveButton);
    dialog.open();
  }

  /**
  * Open dialog for adding a new attribute type.
  */
  private void openAddAttributeTypeDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle("Add Attribute Type");
    dialog.setWidth("700px");
    dialog.setHeight("750px");

    FormLayout formLayout = new FormLayout();
    formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

    // Add warning for external servers
    if (serverConfig != null && !inMemoryLdapService.isInMemoryServer(serverConfig.getId())) {
      Span warningSpan = new Span(
          "⚠️ Adding schema elements to external LDAP servers may require administrator "
          + "privileges and could affect production systems.");
      warningSpan.getStyle().set("color", "#ff6b35");
      warningSpan.getStyle().set("font-size", "0.875rem");
      warningSpan.getStyle().set("margin-bottom", "1rem");
      warningSpan.getStyle().set("display", "block");
      formLayout.add(warningSpan);
    }

    // Basic fields
    TextField nameField = new TextField("Name*");
    nameField.setRequired(true);

    TextField oidField = new TextField("OID*");
    oidField.setRequired(true);
    oidField.setHelperText("Object identifier (e.g., 1.2.3.4.5.6.7.8)");

    TextField descriptionField = new TextField("Description");

    // Get available schema elements for selectors
    List<String> availableSyntaxes = getAvailableSyntaxOIDs();
    List<String> availableAttributes = getAvailableAttributeTypeNames();
    List<String> availableMatchingRules = getAvailableMatchingRuleNames();

    // Syntax OID - enhanced with selector
    ComboBox<String> syntaxOidSelector = createSchemaComboBox(
        "Syntax OID* (Select)", 
        "Choose from available syntaxes...", 
        availableSyntaxes
    );
    syntaxOidSelector.setRequired(true);
    syntaxOidSelector.setValue("1.3.6.1.4.1.1466.115.121.1.15"); // DirectoryString
    syntaxOidSelector.setHelperText("Select from existing syntaxes or enter custom OID");

    TextField syntaxOidField = new TextField("Syntax OID* (Manual Entry)");
    syntaxOidField.setRequired(true);
    syntaxOidField.setValue("1.3.6.1.4.1.1466.115.121.1.15"); // DirectoryString
    syntaxOidField.setHelperText(
        "Common: 1.3.6.1.4.1.1466.115.121.1.15 (Directory String) - Alternative to selector above");

    // Superior Type - enhanced with selector
    ComboBox<String> superiorTypeSelector = createSchemaComboBox(
        "Superior Type (Select)", 
        "Choose from existing attribute types...", 
        availableAttributes
    );
    superiorTypeSelector.setHelperText(
        "Select from existing attribute types or enter custom name");

    TextField superiorTypeField = new TextField("Superior Type (Manual Entry)");
    superiorTypeField.setHelperText(
        "Inherited attribute type (optional) - Alternative to selector above");

    ComboBox<String> usageComboBox = new ComboBox<>("Usage");
    usageComboBox.setItems("USER_APPLICATIONS", "DIRECTORY_OPERATION", "DISTRIBUTED_OPERATION",
        "DSA_OPERATION");
    usageComboBox.setValue("USER_APPLICATIONS");

    // Checkboxes
    Checkbox singleValuedCheckbox = new Checkbox("Single Valued");
    Checkbox obsoleteCheckbox = new Checkbox("Obsolete");
    Checkbox collectiveCheckbox = new Checkbox("Collective");
    Checkbox noUserModificationCheckbox = new Checkbox("No User Modification");

    // Matching rules - enhanced with selectors
    ComboBox<String> equalityMatchingRuleSelector = createSchemaComboBox(
        "Equality Matching Rule (Select)", 
        "Choose from existing matching rules...", 
        availableMatchingRules
    );
    equalityMatchingRuleSelector.setHelperText(
        "Select from existing matching rules or enter custom name");

    TextField equalityMatchingRuleField = new TextField("Equality Matching Rule (Manual Entry)");
    equalityMatchingRuleField.setHelperText("Alternative to selector above");

    ComboBox<String> orderingMatchingRuleSelector = createSchemaComboBox(
        "Ordering Matching Rule (Select)", 
        "Choose from existing matching rules...", 
        availableMatchingRules
    );
    orderingMatchingRuleSelector.setHelperText(
        "Select from existing matching rules or enter custom name");

    TextField orderingMatchingRuleField = new TextField("Ordering Matching Rule (Manual Entry)");
    orderingMatchingRuleField.setHelperText("Alternative to selector above");

    ComboBox<String> substringMatchingRuleSelector = createSchemaComboBox(
        "Substring Matching Rule (Select)", 
        "Choose from existing matching rules...", 
        availableMatchingRules
    );
    substringMatchingRuleSelector.setHelperText(
        "Select from existing matching rules or enter custom name");

    TextField substringMatchingRuleField = new TextField("Substring Matching Rule (Manual Entry)");
    substringMatchingRuleField.setHelperText("Alternative to selector above");

    formLayout.add(nameField, oidField, descriptionField, 
        syntaxOidSelector, syntaxOidField,
        superiorTypeSelector, superiorTypeField,
        usageComboBox, singleValuedCheckbox, obsoleteCheckbox, collectiveCheckbox,
        noUserModificationCheckbox, 
        equalityMatchingRuleSelector, equalityMatchingRuleField,
        orderingMatchingRuleSelector, orderingMatchingRuleField,
        substringMatchingRuleSelector, substringMatchingRuleField);

    // Buttons
    Button saveButton = new Button("Add Attribute Type", e -> {
      if (validateAndSaveAttributeType(dialog, nameField, oidField, descriptionField,
          syntaxOidSelector, syntaxOidField,
          superiorTypeSelector, superiorTypeField, usageComboBox,
          singleValuedCheckbox, obsoleteCheckbox, collectiveCheckbox,
          noUserModificationCheckbox, 
          equalityMatchingRuleSelector, equalityMatchingRuleField,
          orderingMatchingRuleSelector, orderingMatchingRuleField,
          substringMatchingRuleSelector, substringMatchingRuleField)) {
        dialog.close();
      }
    });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> dialog.close());

    dialog.add(formLayout);
    dialog.getFooter().add(cancelButton, saveButton);
    dialog.open();
  }

  /**
   * Validate and save new object class.
   */
  private boolean validateAndSaveObjectClass(Dialog dialog, TextField nameField, TextField oidField,
      TextField descriptionField, ComboBox<String> typeComboBox, Checkbox obsoleteCheckbox,
      MultiSelectComboBox<String> superiorClassesSelector, TextArea superiorClassesField,
      MultiSelectComboBox<String> requiredAttributesSelector, TextArea requiredAttributesField,
      MultiSelectComboBox<String> optionalAttributesSelector, TextArea optionalAttributesField) {
    // Validate required fields
    if (nameField.getValue() == null || nameField.getValue().trim().isEmpty()) {
      showError("Name is required");
      nameField.focus();
      return false;
    }

    if (oidField.getValue() == null || oidField.getValue().trim().isEmpty()) {
      showError("OID is required");
      oidField.focus();
      return false;
    }

    // Check if OID already exists
    if (schema.getObjectClass(oidField.getValue()) != null) {
      showError("An object class with this OID already exists");
      oidField.focus();
      return false;
    }

    // Check if name already exists
    if (schema.getObjectClass(nameField.getValue()) != null) {
      showError("An object class with this name already exists");
      nameField.focus();
      return false;
    }

    try {
      // Build object class definition string
      StringBuilder objectClassDef = new StringBuilder();
      objectClassDef.append("( ").append(oidField.getValue());
      objectClassDef.append(" NAME '").append(nameField.getValue()).append("'");

      if (descriptionField.getValue() != null && !descriptionField.getValue().trim().isEmpty()) {
        objectClassDef.append(" DESC '").append(descriptionField.getValue().trim()).append("'");
      }

      if (obsoleteCheckbox.getValue()) {
        objectClassDef.append(" OBSOLETE");
      }

      // Superior classes - combine selector and manual entry
      Set<String> allSuperiorClasses = new HashSet<>();

      // Add from selector
      if (superiorClassesSelector.getValue() != null && !superiorClassesSelector.getValue()
          .isEmpty()) {
        allSuperiorClasses.addAll(superiorClassesSelector.getValue());
      }

      // Add from manual entry
      String manualSuperiorClasses = superiorClassesField.getValue();
      if (manualSuperiorClasses != null && !manualSuperiorClasses.trim().isEmpty()) {
        String[] manualSuperiors = manualSuperiorClasses.split("\n");
        for (String superior : manualSuperiors) {
          if (superior.trim().length() > 0) {
            allSuperiorClasses.add(superior.trim());
          }
        }
      }

      if (!allSuperiorClasses.isEmpty()) {
        List<String> superiorList = new ArrayList<>(allSuperiorClasses);
        if (superiorList.size() == 1) {
          objectClassDef.append(" SUP ").append(superiorList.get(0));
        } else if (superiorList.size() > 1) {
          objectClassDef.append(" SUP ( ");
          for (int i = 0; i < superiorList.size(); i++) {
            if (i > 0) {
              objectClassDef.append(" $ ");
            }
            objectClassDef.append(superiorList.get(i));
          }
          objectClassDef.append(" )");
        }
      }

      // Object class type
      if (typeComboBox.getValue() != null) {
        objectClassDef.append(" ").append(typeComboBox.getValue());
      }

      // Required attributes - combine selector and manual entry
      Set<String> allRequiredAttributes = new HashSet<>();

      // Add from selector
      if (requiredAttributesSelector.getValue() != null && !requiredAttributesSelector.getValue()
          .isEmpty()) {
        allRequiredAttributes.addAll(requiredAttributesSelector.getValue());
      }

      // Add from manual entry
      String manualRequiredAttributes = requiredAttributesField.getValue();
      if (manualRequiredAttributes != null && !manualRequiredAttributes.trim().isEmpty()) {
        String[] manualMusts = manualRequiredAttributes.split("\n");
        for (String must : manualMusts) {
          if (must.trim().length() > 0) {
            allRequiredAttributes.add(must.trim());
          }
        }
      }

      if (!allRequiredAttributes.isEmpty()) {
        List<String> mustList = new ArrayList<>(allRequiredAttributes);
        if (mustList.size() == 1) {
          objectClassDef.append(" MUST ").append(mustList.get(0));
        } else if (mustList.size() > 1) {
          objectClassDef.append(" MUST ( ");
          for (int i = 0; i < mustList.size(); i++) {
            if (i > 0) {
              objectClassDef.append(" $ ");
            }
            objectClassDef.append(mustList.get(i));
          }
          objectClassDef.append(" )");
        }
      }

      // Optional attributes - combine selector and manual entry
      Set<String> allOptionalAttributes = new HashSet<>();

      // Add from selector
      if (optionalAttributesSelector.getValue() != null && !optionalAttributesSelector.getValue()
          .isEmpty()) {
        allOptionalAttributes.addAll(optionalAttributesSelector.getValue());
      }

      // Add from manual entry
      String manualOptionalAttributes = optionalAttributesField.getValue();
      if (manualOptionalAttributes != null && !manualOptionalAttributes.trim().isEmpty()) {
        String[] manualMays = manualOptionalAttributes.split("\n");
        for (String may : manualMays) {
          if (may.trim().length() > 0) {
            allOptionalAttributes.add(may.trim());
          }
        }
      }

      if (!allOptionalAttributes.isEmpty()) {
        List<String> mayList = new ArrayList<>(allOptionalAttributes);
        if (mayList.size() == 1) {
          objectClassDef.append(" MAY ").append(mayList.get(0));
        } else if (mayList.size() > 1) {
          objectClassDef.append(" MAY ( ");
          for (int i = 0; i < mayList.size(); i++) {
            if (i > 0) {
              objectClassDef.append(" $ ");
            }
            objectClassDef.append(mayList.get(i));
          }
          objectClassDef.append(" )");
        }
      }

      objectClassDef.append(" )");

      // Add to appropriate server type
      if (inMemoryLdapService.isInMemoryServer(serverConfig.getId())) {
        // Add to in-memory server
        inMemoryLdapService.addObjectClassToSchema(serverConfig.getId(), objectClassDef.toString());
      } else {
        // Add to external LDAP server
        ldapService.addObjectClassToSchema(serverConfig.getId(), objectClassDef.toString());
      }

      // Reload schema
      loadSchema();

      showSuccess("Object class '" + nameField.getValue() + "' added successfully");
      return true;

    } catch (Exception e) {
      showError("Failed to add object class: " + e.getMessage());
      return false;
    }
  }

  /**
  * Validate and save new attribute type.
  */
  private boolean validateAndSaveAttributeType(Dialog dialog, TextField nameField,
      TextField oidField,
      TextField descriptionField, 
      ComboBox<String> syntaxOidSelector, TextField syntaxOidField,
      ComboBox<String> superiorTypeSelector, TextField superiorTypeField, 
      ComboBox<String> usageComboBox,
      Checkbox singleValuedCheckbox, Checkbox obsoleteCheckbox,
      Checkbox collectiveCheckbox, Checkbox noUserModificationCheckbox,
      ComboBox<String> equalityMatchingRuleSelector, TextField equalityMatchingRuleField,
      ComboBox<String> orderingMatchingRuleSelector, TextField orderingMatchingRuleField,
      ComboBox<String> substringMatchingRuleSelector, TextField substringMatchingRuleField) {
    // Validate required fields
    if (nameField.getValue() == null || nameField.getValue().trim().isEmpty()) {
      showError("Name is required");
      nameField.focus();
      return false;
    }

    if (oidField.getValue() == null || oidField.getValue().trim().isEmpty()) {
      showError("OID is required");
      oidField.focus();
      return false;
    }

    // Get syntax OID from selector or manual field
    String syntaxOid = syntaxOidSelector.getValue();
    if (syntaxOid == null || syntaxOid.trim().isEmpty()) {
      syntaxOid = syntaxOidField.getValue();
    }

    if (syntaxOid == null || syntaxOid.trim().isEmpty()) {
      showError("Syntax OID is required");
      syntaxOidSelector.focus();
      return false;
    }

    // Check if OID already exists
    if (schema.getAttributeType(oidField.getValue()) != null) {
      showError("An attribute type with this OID already exists");
      oidField.focus();
      return false;
    }

    // Check if name already exists
    if (schema.getAttributeType(nameField.getValue()) != null) {
      showError("An attribute type with this name already exists");
      nameField.focus();
      return false;
    }

    try {
      // Build attribute type definition string
      StringBuilder attributeDef = new StringBuilder();
      attributeDef.append("( ").append(oidField.getValue());

      attributeDef.append(" NAME '").append(nameField.getValue()).append("'");

      if (descriptionField.getValue() != null && !descriptionField.getValue().trim().isEmpty()) {
        attributeDef.append(" DESC '").append(descriptionField.getValue().trim()).append("'");
      }

      if (obsoleteCheckbox.getValue()) {
        attributeDef.append(" OBSOLETE");
      }

      // Superior type - combine selector and manual entry
      String superiorType = superiorTypeSelector.getValue();
      if (superiorType == null || superiorType.trim().isEmpty()) {
        superiorType = superiorTypeField.getValue();
      }
      if (superiorType != null && !superiorType.trim().isEmpty()) {
        attributeDef.append(" SUP ").append(superiorType.trim());
      }

      // Equality matching rule - combine selector and manual entry
      String equalityMatchingRule = equalityMatchingRuleSelector.getValue();
      if (equalityMatchingRule == null || equalityMatchingRule.trim().isEmpty()) {
        equalityMatchingRule = equalityMatchingRuleField.getValue();
      }
      if (equalityMatchingRule != null && !equalityMatchingRule.trim().isEmpty()) {
        attributeDef.append(" EQUALITY ").append(equalityMatchingRule.trim());
      }

      // Ordering matching rule - combine selector and manual entry
      String orderingMatchingRule = orderingMatchingRuleSelector.getValue();
      if (orderingMatchingRule == null || orderingMatchingRule.trim().isEmpty()) {
        orderingMatchingRule = orderingMatchingRuleField.getValue();
      }
      if (orderingMatchingRule != null && !orderingMatchingRule.trim().isEmpty()) {
        attributeDef.append(" ORDERING ").append(orderingMatchingRule.trim());
      }

      // Substring matching rule - combine selector and manual entry
      String substringMatchingRule = substringMatchingRuleSelector.getValue();
      if (substringMatchingRule == null || substringMatchingRule.trim().isEmpty()) {
        substringMatchingRule = substringMatchingRuleField.getValue();
      }
      if (substringMatchingRule != null && !substringMatchingRule.trim().isEmpty()) {
        attributeDef.append(" SUBSTR ").append(substringMatchingRule.trim());
      }

      attributeDef.append(" SYNTAX ").append(syntaxOid.trim());

      if (singleValuedCheckbox.getValue()) {
        attributeDef.append(" SINGLE-VALUE");
      }

      if (collectiveCheckbox.getValue()) {
        attributeDef.append(" COLLECTIVE");
      }

      if (noUserModificationCheckbox.getValue()) {
        attributeDef.append(" NO-USER-MODIFICATION");
      }

      if (usageComboBox.getValue() != null && !"USER_APPLICATIONS".equals(usageComboBox
          .getValue())) {
        attributeDef.append(" USAGE ").append(usageComboBox.getValue().toLowerCase()
            .replace("_", ""));
      }

      attributeDef.append(" )");

      // Add to appropriate server type
      if (inMemoryLdapService.isInMemoryServer(serverConfig.getId())) {
        // Add to in-memory server
        inMemoryLdapService.addAttributeTypeToSchema(serverConfig.getId(), attributeDef.toString());
      } else {
        // Add to external LDAP server
        ldapService.addAttributeTypeToSchema(serverConfig.getId(), attributeDef.toString());
      }

      // Reload schema
      loadSchema();

      showSuccess("Attribute type '" + nameField.getValue() + "' added successfully");
      return true;

    } catch (Exception e) {
      showError("Failed to add attribute type: " + e.getMessage());
      return false;
    }
  }
}