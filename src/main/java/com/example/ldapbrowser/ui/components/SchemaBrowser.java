package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.AttributeSyntaxDefinition;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleUseDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* Component for browsing LDAP schema information including object classes,
* attribute types, matching rules, matching rule use, and syntaxes
*/
public class SchemaBrowser extends VerticalLayout {
 
 private final LdapService ldapService;
 private final ConfigurationService configurationService;
 private final InMemoryLdapService inMemoryLdapService;
 
 // Environment selection
 private EnvironmentDropdown environmentDropdown;
 
 // Server configuration
 private LdapServerConfig serverConfig;
 
 // Schema data
 private Schema schema;
 
 // UI Components
 private Tabs schemaTabs;
 private TextField searchField;
 private Button refreshButton;
 
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
 
 public SchemaBrowser(LdapService ldapService, ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService) {
  this.ldapService = ldapService;
  this.configurationService = configurationService;
  this.inMemoryLdapService = inMemoryLdapService;
  initializeComponents();
  setupLayout();
 }
 
 private void initializeComponents() {
  // Environment dropdown for single-select
  environmentDropdown = new EnvironmentDropdown(ldapService, configurationService, inMemoryLdapService, false);
  environmentDropdown.addSingleSelectionListener(this::onEnvironmentSelected);
  
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
  
  // Schema tabs
  schemaTabs = new Tabs();
  Tab objectClassTab = new Tab("Object Classes");
  Tab attributeTypeTab = new Tab("Attribute Types");
  Tab matchingRuleTab = new Tab("Matching Rules");
  Tab matchingRuleUseTab = new Tab("Matching Rule Use");
  Tab syntaxTab = new Tab("Syntaxes");
  
  schemaTabs.add(objectClassTab, attributeTypeTab, matchingRuleTab, matchingRuleUseTab, syntaxTab);
  schemaTabs.addSelectedChangeListener(e -> {
   Tab selectedTab = e.getSelectedTab();
   if (selectedTab == objectClassTab) {
    currentView = "objectClasses";
    showObjectClasses();
   } else if (selectedTab == attributeTypeTab) {
    currentView = "attributeTypes";
    showAttributeTypes();
   } else if (selectedTab == matchingRuleTab) {
    currentView = "matchingRules";
    showMatchingRules();
   } else if (selectedTab == matchingRuleUseTab) {
    currentView = "matchingRuleUse";
    showMatchingRuleUse();
   } else if (selectedTab == syntaxTab) {
    currentView = "syntaxes";
    showSyntaxes();
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
   
  objectClassGrid.addColumn(oc -> oc.getObjectClassType() != null ? oc.getObjectClassType().getName() : "")
   .setHeader("Type")
   .setFlexGrow(1)
   .setResizable(true)
   .setSortable(true);
   
  objectClassGrid.addColumn(oc -> oc.isObsolete() ? "Yes" : "No")
   .setHeader("Obsolete")
   .setFlexGrow(1)
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
  
  // Environment dropdown
  HorizontalLayout environmentLayout = new HorizontalLayout();
  environmentLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.END);
  environmentLayout.setPadding(true);
  environmentLayout.add(environmentDropdown.getSingleSelectComponent());
  
  // Top controls
  HorizontalLayout controlsLayout = new HorizontalLayout();
  controlsLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
  controlsLayout.setPadding(true);
  controlsLayout.setSpacing(true);
  controlsLayout.addClassName("schema-controls");
  
  Icon schemaIcon = new Icon(VaadinIcon.COGS);
  schemaIcon.setSize("16px");
  schemaIcon.getStyle().set("color", "#4a90e2");
  
  H3 title = new H3("Schema Browser");
  title.addClassNames(LumoUtility.Margin.NONE);
  title.getStyle().set("font-size", "1.1em").set("font-weight", "600").set("color", "#333");
  
  controlsLayout.add(schemaIcon, title);
  controlsLayout.setFlexGrow(1, new Span()); // Spacer
  controlsLayout.add(searchField, refreshButton);
  
  // Main content with tabs
  VerticalLayout contentLayout = new VerticalLayout();
  contentLayout.setSizeFull();
  contentLayout.setPadding(false);
  contentLayout.setSpacing(false);
  
  contentLayout.add(schemaTabs);
  
  // Split layout for grid and details
  SplitLayout splitLayout = new SplitLayout();
  splitLayout.setSizeFull();
  splitLayout.setSplitterPosition(70); // 70% for grid, 30% for details
  
  // Grid container
  gridContainer = new VerticalLayout();
  gridContainer.setSizeFull();
  gridContainer.setPadding(false);
  gridContainer.setSpacing(false);
  gridContainer.addClassName("schema-grid-container");
  
  // Initially show object classes
  gridContainer.add(objectClassGrid);
  
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
  H3 detailsTitle = new H3("Details");
  detailsTitle.addClassNames(LumoUtility.Margin.NONE);
  detailsTitle.getStyle().set("font-size", "0.9em").set("font-weight", "600").set("color", "#333");
  detailsHeader.add(detailsIcon, detailsTitle);
  
  detailsContainer.add(detailsHeader, detailsPanel);
  detailsContainer.setFlexGrow(1, detailsPanel);
  
  splitLayout.addToPrimary(gridContainer);
  splitLayout.addToSecondary(detailsContainer);
  
  contentLayout.add(splitLayout);
  contentLayout.setFlexGrow(1, splitLayout);
  
  add(environmentLayout, controlsLayout, contentLayout);
  setFlexGrow(1, contentLayout);
 }
 
 private void onEnvironmentSelected(LdapServerConfig environment) {
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
    showSuccess("Schema loaded successfully");
   } else {
    showError("No schema information available");
   }
  } catch (LDAPException e) {
   showError("Failed to load schema: " + e.getMessage());
   schema = null;
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
 
 private List<ObjectClassDefinition> filterObjectClasses(Collection<ObjectClassDefinition> objectClasses) {
  if (currentFilter == null || currentFilter.trim().isEmpty()) {
   return new ArrayList<>(objectClasses);
  }
  
  String filter = currentFilter.toLowerCase().trim();
  return objectClasses.stream()
   .filter(oc -> 
    (oc.getNameOrOID() != null && oc.getNameOrOID().toLowerCase().contains(filter)) ||
    (oc.getDescription() != null && oc.getDescription().toLowerCase().contains(filter)) ||
    (oc.getOID() != null && oc.getOID().toLowerCase().contains(filter))
   )
   .sorted(Comparator.comparing(ObjectClassDefinition::getNameOrOID))
   .collect(Collectors.toList());
 }
 
 private List<AttributeTypeDefinition> filterAttributeTypes(Collection<AttributeTypeDefinition> attributeTypes) {
  if (currentFilter == null || currentFilter.trim().isEmpty()) {
   return new ArrayList<>(attributeTypes);
  }
  
  String filter = currentFilter.toLowerCase().trim();
  return attributeTypes.stream()
   .filter(at -> 
    (at.getNameOrOID() != null && at.getNameOrOID().toLowerCase().contains(filter)) ||
    (at.getDescription() != null && at.getDescription().toLowerCase().contains(filter)) ||
    (at.getOID() != null && at.getOID().toLowerCase().contains(filter))
   )
   .sorted(Comparator.comparing(AttributeTypeDefinition::getNameOrOID))
   .collect(Collectors.toList());
 }
 
 private List<MatchingRuleDefinition> filterMatchingRules(Collection<MatchingRuleDefinition> matchingRules) {
  if (currentFilter == null || currentFilter.trim().isEmpty()) {
   return new ArrayList<>(matchingRules);
  }
  
  String filter = currentFilter.toLowerCase().trim();
  return matchingRules.stream()
   .filter(mr -> 
    (mr.getNameOrOID() != null && mr.getNameOrOID().toLowerCase().contains(filter)) ||
    (mr.getDescription() != null && mr.getDescription().toLowerCase().contains(filter)) ||
    (mr.getOID() != null && mr.getOID().toLowerCase().contains(filter))
   )
   .sorted(Comparator.comparing(MatchingRuleDefinition::getNameOrOID))
   .collect(Collectors.toList());
 }
 
 private List<MatchingRuleUseDefinition> filterMatchingRuleUse(Collection<MatchingRuleUseDefinition> matchingRuleUses) {
  if (currentFilter == null || currentFilter.trim().isEmpty()) {
   return new ArrayList<>(matchingRuleUses);
  }
  
  String filter = currentFilter.toLowerCase().trim();
  return matchingRuleUses.stream()
   .filter(mru -> 
    (mru.getOID() != null && mru.getOID().toLowerCase().contains(filter)) ||
    (mru.getDescription() != null && mru.getDescription().toLowerCase().contains(filter))
   )
   .sorted(Comparator.comparing(MatchingRuleUseDefinition::getOID))
   .collect(Collectors.toList());
 }
 
 private List<AttributeSyntaxDefinition> filterSyntaxes(Collection<AttributeSyntaxDefinition> syntaxes) {
  if (currentFilter == null || currentFilter.trim().isEmpty()) {
   return new ArrayList<>(syntaxes);
  }
  
  String filter = currentFilter.toLowerCase().trim();
  return syntaxes.stream()
   .filter(syn -> 
    (syn.getOID() != null && syn.getOID().toLowerCase().contains(filter)) ||
    (syn.getDescription() != null && syn.getDescription().toLowerCase().contains(filter))
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
  addDetailRow(details, "Type", objectClass.getObjectClassType() != null ? 
   objectClass.getObjectClassType().getName() : "");
  addDetailRow(details, "Obsolete", objectClass.isObsolete() ? "Yes" : "No");
  
  // Superior classes
  if (objectClass.getSuperiorClasses() != null && objectClass.getSuperiorClasses().length > 0) {
   addDetailRow(details, "Superior Classes", String.join(", ", objectClass.getSuperiorClasses()));
  }
  
  // Required attributes
  if (objectClass.getRequiredAttributes() != null && objectClass.getRequiredAttributes().length > 0) {
   addDetailRow(details, "Required Attributes", String.join(", ", objectClass.getRequiredAttributes()));
  }
  
  // Optional attributes
  if (objectClass.getOptionalAttributes() != null && objectClass.getOptionalAttributes().length > 0) {
   addDetailRow(details, "Optional Attributes", String.join(", ", objectClass.getOptionalAttributes()));
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
  addDetailRow(details, "Syntax OID", attributeType.getSyntaxOID());
  addDetailRow(details, "Obsolete", attributeType.isObsolete() ? "Yes" : "No");
  addDetailRow(details, "Single Value", attributeType.isSingleValued() ? "Yes" : "No");
  addDetailRow(details, "Collective", attributeType.isCollective() ? "Yes" : "No");
  addDetailRow(details, "No User Modification", attributeType.isNoUserModification() ? "Yes" : "No");
  
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
 
 public void clear() {
  schema = null;
  objectClassGrid.setItems();
  attributeTypeGrid.setItems();
  matchingRuleGrid.setItems();
  matchingRuleUseGrid.setItems();
  syntaxGrid.setItems();
  detailsPanel.removeAll();
  detailsPanel.add(new Span("Select a schema element to view details"));
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
  if (environmentDropdown != null) {
   environmentDropdown.refreshEnvironments();
  }
 }
}
