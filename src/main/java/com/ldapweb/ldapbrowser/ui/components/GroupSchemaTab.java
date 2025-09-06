package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.util.SchemaCompareUtil;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.AttributeSyntaxDefinition;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleUseDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;
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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Schema comparison tab for group operations. Displays per-server checksums of
 * schema elements
 * across all servers in the selected group with an equality indicator.
 */
public class GroupSchemaTab extends VerticalLayout {

  private final LdapService ldapService;

  private final TabSheet tabSheet = new TabSheet();
  private final Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
  private final Span statusLabel = new Span();
  private final TextField searchField = new TextField();

  private Set<LdapServerConfig> environments = new HashSet<>();
  private List<LdapServerConfig> sortedServers = new ArrayList<>();

  // Grids per schema component
  private Grid<RowModel> ocGrid;
  private Grid<RowModel> atGrid;
  private Grid<RowModel> mrGrid;
  private Grid<RowModel> mruGrid;
  private Grid<RowModel> synGrid;

  // Data providers for filtering
  private ListDataProvider<RowModel> ocDataProvider;
  private ListDataProvider<RowModel> atDataProvider;
  private ListDataProvider<RowModel> mrDataProvider;
  private ListDataProvider<RowModel> mruDataProvider;
  private ListDataProvider<RowModel> synDataProvider;

  // Split layout for details view
  private SplitLayout splitLayout;
  private VerticalLayout detailsPanel;

  /**
   * A Vaadin UI component tab that displays LDAP schema information, including
   * object classes,
   * attribute types, matching rules, matching rule use, and syntaxes. Provides
   * controls for
   * refreshing schema data and displays status information.
   *
   * <p>
   * This tab uses grids to present different schema elements and organizes them
   * into
   * separate tabs for easy navigation. The header includes an icon and title, and
   * controls
   * allow users to refresh the displayed data.
   * </p>
   *
   * @param ldapService the service used to retrieve LDAP schema information
   */
  public GroupSchemaTab(LdapService ldapService) {
    this.ldapService = ldapService;
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    // Title
    Icon icon = new Icon(VaadinIcon.BRIEFCASE);
    icon.setColor("#1976d2");
    icon.setSize("20px");
    H3 title = new H3("Schema");
    title.getStyle().set("margin", "0");
    HorizontalLayout header = new HorizontalLayout(icon, title);
    header.setDefaultVerticalComponentAlignment(Alignment.CENTER);

    // Search field
    searchField.setPlaceholder("Search schema elements...");
    searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(e -> applyFilter(e.getValue()));
    searchField.setWidth("300px");

    refreshButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    refreshButton.addClickListener(e -> loadAndRender());

    statusLabel.getStyle().set("font-style", "italic").set("color", "#666");

    HorizontalLayout controls = new HorizontalLayout(searchField, refreshButton, statusLabel);
    controls.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    controls.setWidthFull();
    controls.setJustifyContentMode(JustifyContentMode.BETWEEN);

    // Tabs
    tabSheet.setSizeFull();
    ocGrid = createGrid();
    atGrid = createGrid();
    mrGrid = createGrid();
    mruGrid = createGrid();
    synGrid = createGrid();

    // Initialize data providers
    ocDataProvider = new ListDataProvider<>(new ArrayList<>());
    atDataProvider = new ListDataProvider<>(new ArrayList<>());
    mrDataProvider = new ListDataProvider<>(new ArrayList<>());
    mruDataProvider = new ListDataProvider<>(new ArrayList<>());
    synDataProvider = new ListDataProvider<>(new ArrayList<>());

    ocGrid.setDataProvider(ocDataProvider);
    atGrid.setDataProvider(atDataProvider);
    mrGrid.setDataProvider(mrDataProvider);
    mruGrid.setDataProvider(mruDataProvider);
    synGrid.setDataProvider(synDataProvider);

    tabSheet.add(new Tab("Object Classes"), ocGrid);
    tabSheet.add(new Tab("Attribute Types"), atGrid);
    tabSheet.add(new Tab("Matching Rules"), mrGrid);
    tabSheet.add(new Tab("Matching Rule Use"), mruGrid);
    tabSheet.add(new Tab("Syntaxes"), synGrid);

    // Details panel
    detailsPanel = new VerticalLayout();
    detailsPanel.setSpacing(true);
    detailsPanel.setPadding(true);
    detailsPanel.setVisible(false);

    // Split layout
    splitLayout = new SplitLayout(tabSheet, detailsPanel);
    splitLayout.setSizeFull();
    splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
    splitLayout.setSplitterPosition(70);

    add(header, controls, splitLayout);
    setFlexGrow(1, splitLayout);
  }

  public void setEnvironments(Set<LdapServerConfig> envs) {
    this.environments = envs != null ? new HashSet<>(envs) : Collections.emptySet();
    this.sortedServers = new ArrayList<>(this.environments);
    // Sort by display name
    this.sortedServers.sort(Comparator.comparing(cfg -> displayName(cfg)));
    loadAndRender();
  }

  private Grid<RowModel> createGrid() {
    Grid<RowModel> grid = new Grid<>(RowModel.class, false);
    grid.setSizeFull();
    
    // Add selection listener to show details
    grid.asSingleSelect().addValueChangeListener(e -> {
      if (e.getValue() != null) {
        showSchemaElementDetails(e.getValue());
      } else {
        hideDetails();
      }
    });
    
    return grid;
  }

  private void loadAndRender() {
    if (sortedServers.isEmpty()) {
      statusLabel.setText("No servers in this group.");
      setGridsEmpty();
      return;
    }

    statusLabel.setText("Loading schema from " + sortedServers.size() + " servers...");

    Map<String, Schema> schemas = new LinkedHashMap<>(); // serverName -> Schema
    int errors = 0;

    // Determine group-wide support for the extended schema info request control
    final String EXTENDED_SCHEMA_INFO_OID = "1.3.6.1.4.1.30221.2.5.12";
    boolean allSupportExtended = true;

    for (LdapServerConfig cfg : sortedServers) {
      try {
        if (!ldapService.isConnected(cfg.getId())) {
          ldapService.connect(cfg);
        }
        boolean supported = ldapService.isControlSupported(cfg.getId(), EXTENDED_SCHEMA_INFO_OID);
        if (!supported) {
          allSupportExtended = false;
        }
      } catch (LDAPException e) {
        // Treat errors as lack of support to keep comparisons consistent
        allSupportExtended = false;
      }
    }

    // Fetch schemas with an all-or-none decision: if any server lacks support,
    // don't use the control
    for (LdapServerConfig cfg : sortedServers) {
      String serverName = displayName(cfg);
      try {
        // Connections ensured in previous loop; fetch schema honoring group-wide
        // decision
        Schema schema = ldapService.getSchema(cfg.getId(), allSupportExtended);
        schemas.put(serverName, schema);
      } catch (LDAPException e) {
        errors++;
        schemas.put(serverName, null);
      }
    }

    // Build data for each component
    renderObjectClasses(schemas);
    renderAttributeTypes(schemas);
    renderMatchingRules(schemas);
    renderMatchingRuleUse(schemas);
    renderSyntaxes(schemas);

    if (errors > 0) {
      statusLabel.setText(
          "Loaded with " + errors + " error(s). Extended schema info control " +
              (allSupportExtended ? "used" : "disabled for consistency") + ".");
      Notification n = Notification.show("Some servers failed to load schema.");
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    } else {
      statusLabel.setText(
          "Schema loaded. Extended schema info control " +
              (schemas.isEmpty()
                  ? "n/a"
                  : (allSupportExtended
                      ? "used"
                      : "disabled for consistency"))
              +
              ".");
    }
  }

  private void setGridsEmpty() {
    ocDataProvider.getItems().clear();
    atDataProvider.getItems().clear();
    mrDataProvider.getItems().clear();
    mruDataProvider.getItems().clear();
    synDataProvider.getItems().clear();
    
    ocDataProvider.refreshAll();
    atDataProvider.refreshAll();
    mrDataProvider.refreshAll();
    mruDataProvider.refreshAll();
    synDataProvider.refreshAll();
  }

  private void renderObjectClasses(Map<String, Schema> schemas) {
    setupColumns(ocGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.OBJECT_CLASS);
    List<RowModel> models = toModels(rows, schemas.keySet());
    ocDataProvider.getItems().clear();
    ocDataProvider.getItems().addAll(models);
    ocDataProvider.refreshAll();
  }

  private void renderAttributeTypes(Map<String, Schema> schemas) {
    setupColumns(atGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.ATTRIBUTE_TYPE);
    List<RowModel> models = toModels(rows, schemas.keySet());
    atDataProvider.getItems().clear();
    atDataProvider.getItems().addAll(models);
    atDataProvider.refreshAll();
  }

  private void renderMatchingRules(Map<String, Schema> schemas) {
    setupColumns(mrGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.MATCHING_RULE);
    List<RowModel> models = toModels(rows, schemas.keySet());
    mrDataProvider.getItems().clear();
    mrDataProvider.getItems().addAll(models);
    mrDataProvider.refreshAll();
  }

  private void renderMatchingRuleUse(Map<String, Schema> schemas) {
    setupColumns(mruGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.MATCHING_RULE_USE);
    List<RowModel> models = toModels(rows, schemas.keySet());
    mruDataProvider.getItems().clear();
    mruDataProvider.getItems().addAll(models);
    mruDataProvider.refreshAll();
  }

  private void renderSyntaxes(Map<String, Schema> schemas) {
    setupColumns(synGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.SYNTAX);
    List<RowModel> models = toModels(rows, schemas.keySet());
    synDataProvider.getItems().clear();
    synDataProvider.getItems().addAll(models);
    synDataProvider.refreshAll();
  }

  private void setupColumns(Grid<RowModel> grid, Collection<String> serverNames) {
    grid.removeAllColumns();
    grid.addColumn(RowModel::getName)
        .setHeader("Name")
        .setAutoWidth(true)
        .setFrozen(true)
        .setSortable(true)
        .setComparator(Comparator.comparing(RowModel::getName));
        
    for (String server : serverNames) {
      grid.addColumn(row -> row.getChecksum(server))
          .setHeader(server)
          .setAutoWidth(true)
          .setSortable(true)
          .setComparator(Comparator.comparing(row -> row.getChecksum(server)));
    }
    
    grid.addColumn(row -> row.isEqualAcross(serverNames) ? "Equal" : "Unequal")
        .setHeader("Status")
        .setAutoWidth(true)
        .setSortable(true)
        .setComparator(Comparator.comparing(row -> row.isEqualAcross(serverNames)));
  }

  private enum ComponentType {
    OBJECT_CLASS, ATTRIBUTE_TYPE, MATCHING_RULE, MATCHING_RULE_USE, SYNTAX
  }

  private Map<String, Map<String, String>> buildRowsFor(
      Map<String, Schema> schemas, ComponentType type) {
    // name -> (serverName -> checksum or "MISSING"/"ERROR")
    Map<String, Map<String, String>> rows = new LinkedHashMap<>();
    Set<String> allNames = new TreeSet<>();

    for (Map.Entry<String, Schema> e : schemas.entrySet()) {
      Schema schema = e.getValue();
      if (schema == null) {
        continue;
      }
      switch (type) {
        case OBJECT_CLASS -> {
          for (ObjectClassDefinition d : schema.getObjectClasses()) {
            String name = d.getNameOrOID();
            allNames.add(name);
          }
        }
        case ATTRIBUTE_TYPE -> {
          for (AttributeTypeDefinition d : schema.getAttributeTypes()) {
            String name = d.getNameOrOID();
            allNames.add(name);
          }
        }
        case MATCHING_RULE -> {
          for (MatchingRuleDefinition d : schema.getMatchingRules()) {
            String name = d.getNameOrOID();
            allNames.add(name);
          }
        }
        case MATCHING_RULE_USE -> {
          for (MatchingRuleUseDefinition d : schema.getMatchingRuleUses()) {
            String name = d.getNameOrOID();
            allNames.add(name);
          }
        }
        case SYNTAX -> {
          for (AttributeSyntaxDefinition d : schema.getAttributeSyntaxes()) {
            String name = d.getOID();
            allNames.add(name);
          }
        }
      }
    }

    for (String name : allNames) {
      Map<String, String> checksums = new LinkedHashMap<>();
      for (Map.Entry<String, Schema> e : schemas.entrySet()) {
        String server = e.getKey();
        Schema schema = e.getValue();
        if (schema == null) {
          checksums.put(server, "ERROR");
          continue;
        }
        String sum = switch (type) {
          case OBJECT_CLASS -> {
            ObjectClassDefinition d = schema.getObjectClass(name);
            yield d != null ? checksum(SchemaCompareUtil.canonical(d)) : "MISSING";
          }
          case ATTRIBUTE_TYPE -> {
            AttributeTypeDefinition d = schema.getAttributeType(name);
            yield d != null ? checksum(SchemaCompareUtil.canonical(d)) : "MISSING";
          }
          case MATCHING_RULE -> {
            MatchingRuleDefinition d = schema.getMatchingRule(name);
            yield d != null ? checksum(SchemaCompareUtil.canonical(d)) : "MISSING";
          }
          case MATCHING_RULE_USE -> {
            MatchingRuleUseDefinition d = schema.getMatchingRuleUse(name);
            yield d != null ? checksum(SchemaCompareUtil.canonical(d)) : "MISSING";
          }
          case SYNTAX -> {
            AttributeSyntaxDefinition d = schema.getAttributeSyntax(name);
            yield d != null ? checksum(SchemaCompareUtil.canonical(d)) : "MISSING";
          }
        };
        checksums.put(server, sum);
      }
      rows.put(name, checksums);
    }

    return rows;
  }

  private List<RowModel> toModels(Map<String, Map<String, String>> rows, Collection<String> servers) {
    List<RowModel> list = new ArrayList<>();
    for (Map.Entry<String, Map<String, String>> e : rows.entrySet()) {
      RowModel m = new RowModel(e.getKey(), e.getValue());
      list.add(m);
    }
    // Sort by name
    list.sort(Comparator.comparing(RowModel::getName));
    return list;
  }

  // Canonicalization handled by SchemaCompareUtil

  private String checksum(String text) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < hash.length; i++) {
        sb.append(String.format("%02x", hash[i]));
      }
      // Shorten for display
      return sb.substring(0, 8);
    } catch (Exception e) {
      return "ERR";
    }
  }

  private String displayName(LdapServerConfig cfg) {
    String name = cfg.getName();
    return (name != null && !name.isBlank()) ? name : cfg.getHost();
  }

  /**
   * Applies a search filter to all grids.
   */
  private void applyFilter(String filterText) {
    String filter = filterText == null ? "" : filterText.toLowerCase().trim();
    
    ocDataProvider.setFilter(row -> filter.isEmpty() || 
        row.getName().toLowerCase().contains(filter));
    atDataProvider.setFilter(row -> filter.isEmpty() || 
        row.getName().toLowerCase().contains(filter));
    mrDataProvider.setFilter(row -> filter.isEmpty() || 
        row.getName().toLowerCase().contains(filter));
    mruDataProvider.setFilter(row -> filter.isEmpty() || 
        row.getName().toLowerCase().contains(filter));
    synDataProvider.setFilter(row -> filter.isEmpty() || 
        row.getName().toLowerCase().contains(filter));
  }

  /**
   * Shows detailed schema element information with comparison across servers.
   */
  private void showSchemaElementDetails(RowModel rowModel) {
    detailsPanel.removeAll();
    detailsPanel.setVisible(true);
    
    // Header
    H3 header = new H3("Schema Element: " + rowModel.getName());
    header.getStyle().set("margin-bottom", "16px");
    detailsPanel.add(header);
    
    // Create comparison grid
    Grid<SchemaPropertyRow> comparisonGrid = new Grid<>();
    comparisonGrid.setSizeFull();
    comparisonGrid.setHeight("400px");
    
    // Property column
    comparisonGrid.addColumn(SchemaPropertyRow::getProperty)
        .setHeader("Property")
        .setAutoWidth(true)
        .setFrozen(true)
        .setSortable(true);
    
    // Add columns for each server
    for (String serverName : sortedServers.stream().map(this::displayName).toList()) {
      comparisonGrid.addColumn(row -> row.getValue(serverName))
          .setHeader(serverName)
          .setAutoWidth(true)
          .setResizable(true)
          .setSortable(true);
    }
    
    // Load detailed schema information for this element
    List<SchemaPropertyRow> propertyRows = loadSchemaElementDetails(rowModel);
    comparisonGrid.setItems(propertyRows);
    
    detailsPanel.add(comparisonGrid);
    splitLayout.setSplitterPosition(50);
  }

  /**
   * Hides the details panel.
   */
  private void hideDetails() {
    detailsPanel.setVisible(false);
    splitLayout.setSplitterPosition(100);
  }

  /**
   * Loads detailed schema element information for comparison across servers.
   */
  private List<SchemaPropertyRow> loadSchemaElementDetails(RowModel rowModel) {
    List<SchemaPropertyRow> propertyRows = new ArrayList<>();
    String elementName = rowModel.getName();
    
    // Determine which tab is active to know which type of schema element
    ComponentType type = getCurrentComponentType();
    
    // Collect detailed information from each server
    Map<String, Schema> schemas = new LinkedHashMap<>();
    for (LdapServerConfig cfg : sortedServers) {
      String serverName = displayName(cfg);
      try {
        if (!ldapService.isConnected(cfg.getId())) {
          ldapService.connect(cfg);
        }
        Schema schema = ldapService.getSchema(cfg.getId(), false);
        schemas.put(serverName, schema);
      } catch (LDAPException e) {
        schemas.put(serverName, null);
      }
    }
    
    // Extract properties based on type
    switch (type) {
      case OBJECT_CLASS -> {
        addObjectClassProperties(propertyRows, elementName, schemas);
      }
      case ATTRIBUTE_TYPE -> {
        addAttributeTypeProperties(propertyRows, elementName, schemas);
      }
      case MATCHING_RULE -> {
        addMatchingRuleProperties(propertyRows, elementName, schemas);
      }
      case MATCHING_RULE_USE -> {
        addMatchingRuleUseProperties(propertyRows, elementName, schemas);
      }
      case SYNTAX -> {
        addSyntaxProperties(propertyRows, elementName, schemas);
      }
    }
    
    return propertyRows;
  }

  /**
   * Determines the current component type based on selected tab.
   */
  private ComponentType getCurrentComponentType() {
    Tab selectedTab = tabSheet.getSelectedTab();
    if (selectedTab != null) {
      String tabLabel = selectedTab.getLabel();
      return switch (tabLabel) {
        case "Object Classes" -> ComponentType.OBJECT_CLASS;
        case "Attribute Types" -> ComponentType.ATTRIBUTE_TYPE;
        case "Matching Rules" -> ComponentType.MATCHING_RULE;
        case "Matching Rule Use" -> ComponentType.MATCHING_RULE_USE;
        case "Syntaxes" -> ComponentType.SYNTAX;
        default -> ComponentType.OBJECT_CLASS;
      };
    }
    return ComponentType.OBJECT_CLASS;
  }

  private void addObjectClassProperties(List<SchemaPropertyRow> propertyRows, String elementName, Map<String, Schema> schemas) {
    addProperty(propertyRows, "OID", elementName, schemas, 
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null ? def.getOID() : "N/A";
        });
    
    addProperty(propertyRows, "Names", elementName, schemas,
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null && def.getNames() != null ? 
              String.join(", ", Arrays.asList(def.getNames())) : "N/A";
        });
    
    addProperty(propertyRows, "Description", elementName, schemas,
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null ? (def.getDescription() != null ? def.getDescription() : "N/A") : "N/A";
        });
    
    addProperty(propertyRows, "Type", elementName, schemas,
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null && def.getObjectClassType() != null ? 
              def.getObjectClassType().getName() : "N/A";
        });
    
    addProperty(propertyRows, "Obsolete", elementName, schemas,
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null ? (def.isObsolete() ? "Yes" : "No") : "N/A";
        });
    
    addProperty(propertyRows, "Superior Classes", elementName, schemas,
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null && def.getSuperiorClasses() != null ? 
              String.join(", ", def.getSuperiorClasses()) : "N/A";
        });
    
    // Add individual rows for Required Attributes
    addAttributeListProperties(propertyRows, "Required Attribute", elementName, schemas,
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null && def.getRequiredAttributes() != null ? 
              def.getRequiredAttributes() : new String[0];
        });
    
    // Add individual rows for Optional Attributes  
    addAttributeListProperties(propertyRows, "Optional Attribute", elementName, schemas,
        (schema, name) -> {
          ObjectClassDefinition def = schema.getObjectClass(name);
          return def != null && def.getOptionalAttributes() != null ? 
              def.getOptionalAttributes() : new String[0];
        });
  }

  private void addAttributeTypeProperties(List<SchemaPropertyRow> propertyRows, String elementName, Map<String, Schema> schemas) {
    addProperty(propertyRows, "OID", elementName, schemas,
        (schema, name) -> {
          AttributeTypeDefinition def = schema.getAttributeType(name);
          return def != null ? def.getOID() : "N/A";
        });
    
    addProperty(propertyRows, "Names", elementName, schemas,
        (schema, name) -> {
          AttributeTypeDefinition def = schema.getAttributeType(name);
          return def != null && def.getNames() != null ? 
              String.join(", ", Arrays.asList(def.getNames())) : "N/A";
        });
    
    addProperty(propertyRows, "Description", elementName, schemas,
        (schema, name) -> {
          AttributeTypeDefinition def = schema.getAttributeType(name);
          return def != null ? (def.getDescription() != null ? def.getDescription() : "N/A") : "N/A";
        });
    
    addProperty(propertyRows, "Syntax", elementName, schemas,
        (schema, name) -> {
          AttributeTypeDefinition def = schema.getAttributeType(name);
          return def != null ? (def.getSyntaxOID() != null ? def.getSyntaxOID() : "N/A") : "N/A";
        });
    
    addProperty(propertyRows, "Single Value", elementName, schemas,
        (schema, name) -> {
          AttributeTypeDefinition def = schema.getAttributeType(name);
          return def != null ? (def.isSingleValued() ? "Yes" : "No") : "N/A";
        });
    
    addProperty(propertyRows, "Usage", elementName, schemas,
        (schema, name) -> {
          AttributeTypeDefinition def = schema.getAttributeType(name);
          return def != null && def.getUsage() != null ? 
              def.getUsage().getName() : "N/A";
        });
    
    addProperty(propertyRows, "Equality Matching Rule", elementName, schemas,
        (schema, name) -> {
          AttributeTypeDefinition def = schema.getAttributeType(name);
          return def != null ? (def.getEqualityMatchingRule() != null ? 
              def.getEqualityMatchingRule() : "N/A") : "N/A";
        });
  }

  private void addMatchingRuleProperties(List<SchemaPropertyRow> propertyRows, String elementName, Map<String, Schema> schemas) {
    addProperty(propertyRows, "OID", elementName, schemas,
        (schema, name) -> {
          MatchingRuleDefinition def = schema.getMatchingRule(name);
          return def != null ? def.getOID() : "N/A";
        });
    
    addProperty(propertyRows, "Names", elementName, schemas,
        (schema, name) -> {
          MatchingRuleDefinition def = schema.getMatchingRule(name);
          return def != null && def.getNames() != null ? 
              String.join(", ", Arrays.asList(def.getNames())) : "N/A";
        });
    
    addProperty(propertyRows, "Description", elementName, schemas,
        (schema, name) -> {
          MatchingRuleDefinition def = schema.getMatchingRule(name);
          return def != null ? (def.getDescription() != null ? def.getDescription() : "N/A") : "N/A";
        });
    
    addProperty(propertyRows, "Syntax", elementName, schemas,
        (schema, name) -> {
          MatchingRuleDefinition def = schema.getMatchingRule(name);
          return def != null ? (def.getSyntaxOID() != null ? def.getSyntaxOID() : "N/A") : "N/A";
        });
  }

  private void addMatchingRuleUseProperties(List<SchemaPropertyRow> propertyRows, String elementName, Map<String, Schema> schemas) {
    addProperty(propertyRows, "OID", elementName, schemas,
        (schema, name) -> {
          MatchingRuleUseDefinition def = schema.getMatchingRuleUse(name);
          return def != null ? def.getOID() : "N/A";
        });
    
    addProperty(propertyRows, "Names", elementName, schemas,
        (schema, name) -> {
          MatchingRuleUseDefinition def = schema.getMatchingRuleUse(name);
          return def != null && def.getNames() != null ? 
              String.join(", ", Arrays.asList(def.getNames())) : "N/A";
        });
    
    addProperty(propertyRows, "Description", elementName, schemas,
        (schema, name) -> {
          MatchingRuleUseDefinition def = schema.getMatchingRuleUse(name);
          return def != null ? (def.getDescription() != null ? def.getDescription() : "N/A") : "N/A";
        });
    
    addProperty(propertyRows, "Applies To", elementName, schemas,
        (schema, name) -> {
          MatchingRuleUseDefinition def = schema.getMatchingRuleUse(name);
          return def != null && def.getApplicableAttributeTypes() != null ? 
              String.join(", ", def.getApplicableAttributeTypes()) : "N/A";
        });
  }

  private void addSyntaxProperties(List<SchemaPropertyRow> propertyRows, String elementName, Map<String, Schema> schemas) {
    addProperty(propertyRows, "OID", elementName, schemas,
        (schema, name) -> {
          AttributeSyntaxDefinition def = schema.getAttributeSyntax(name);
          return def != null ? def.getOID() : "N/A";
        });
    
    addProperty(propertyRows, "Description", elementName, schemas,
        (schema, name) -> {
          AttributeSyntaxDefinition def = schema.getAttributeSyntax(name);
          return def != null ? (def.getDescription() != null ? def.getDescription() : "N/A") : "N/A";
        });
  }

  private void addProperty(List<SchemaPropertyRow> propertyRows, String propertyName, String elementName, 
                          Map<String, Schema> schemas, PropertyExtractor extractor) {
    Map<String, String> values = new LinkedHashMap<>();
    
    for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
      String serverName = entry.getKey();
      Schema schema = entry.getValue();
      
      String value;
      if (schema == null) {
        value = "ERROR";
      } else {
        try {
          value = extractor.extract(schema, elementName);
        } catch (Exception e) {
          value = "ERROR";
        }
      }
      values.put(serverName, value);
    }
    
    propertyRows.add(new SchemaPropertyRow(propertyName, values));
  }

  @FunctionalInterface
  private interface PropertyExtractor {
    String extract(Schema schema, String elementName);
  }

  @FunctionalInterface
  private interface AttributeArrayExtractor {
    String[] extract(Schema schema, String elementName);
  }

  /**
   * Adds individual property rows for each attribute in an array.
   * This creates separate rows for better comparison across servers.
   */
  private void addAttributeListProperties(List<SchemaPropertyRow> propertyRows, String propertyPrefix, 
                                         String elementName, Map<String, Schema> schemas, 
                                         AttributeArrayExtractor extractor) {
    // First, collect all unique attributes across all servers
    Set<String> allAttributes = new TreeSet<>();
    
    for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
      Schema schema = entry.getValue();
      if (schema != null) {
        try {
          String[] attributes = extractor.extract(schema, elementName);
          if (attributes != null) {
            for (String attr : attributes) {
              if (attr != null && !attr.trim().isEmpty()) {
                allAttributes.add(attr.trim());
              }
            }
          }
        } catch (Exception e) {
          // Skip errors for individual servers
        }
      }
    }
    
    // Create a row for each unique attribute
    for (String attribute : allAttributes) {
      Map<String, String> values = new LinkedHashMap<>();
      
      for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
        String serverName = entry.getKey();
        Schema schema = entry.getValue();
        
        String value = "N/A";
        if (schema != null) {
          try {
            String[] attributes = extractor.extract(schema, elementName);
            if (attributes != null) {
              // Check if this server has this specific attribute
              boolean hasAttribute = false;
              for (String attr : attributes) {
                if (attr != null && attr.trim().equals(attribute)) {
                  hasAttribute = true;
                  break;
                }
              }
              value = hasAttribute ? "Present" : "Missing";
            }
          } catch (Exception e) {
            value = "ERROR";
          }
        } else {
          value = "ERROR";
        }
        values.put(serverName, value);
      }
      
      propertyRows.add(new SchemaPropertyRow(propertyPrefix + ": " + attribute, values));
    }
  }

  /**
   * Represents a row in the schema comparison grid showing a property and its values across servers.
   */
  public static class SchemaPropertyRow {
    private final String property;
    private final Map<String, String> values;

    public SchemaPropertyRow(String property, Map<String, String> values) {
      this.property = property;
      this.values = values;
    }

    public String getProperty() {
      return property;
    }

    public String getValue(String serverName) {
      return values.getOrDefault(serverName, "N/A");
    }

    public Map<String, String> getValues() {
      return values;
    }
  }

  // Row model for the grid with dynamic per-server checksum map
  /**
   * Represents a row in the group schema tab, containing a name and a mapping of
   * server names to their checksums.
   * <p>
   * This model is used to track checksum values for different servers associated
   * with a particular group or entity.
   * </p>
   *
   * <p>
   * Example usage:
   * 
   * <pre>
   * Map&lt;String, String&gt; checksums = new HashMap&lt;&gt;();
   * checksums.put("server1", "abc123");
   * checksums.put("server2", "abc123");
   * RowModel row = new RowModel("GroupA", checksums);
   * </pre>
   * </p>
   *
   * <ul>
   * <li>{@code getName()} returns the name of the row.</li>
   * <li>{@code getChecksum(String serverName)} retrieves the checksum for a
   * specific server.</li>
   * <li>{@code isEqualAcross(Collection&lt;String&gt; serverNames)} checks if all
   * non-null checksums for the given servers are equal.</li>
   * </ul>
   */
  public static class RowModel {
    private final String name;
    private final Map<String, String> checksums; // serverName -> checksum

    public RowModel(String name, Map<String, String> checksums) {
      this.name = name;
      this.checksums = checksums;
    }

    public String getName() {
      return name;
    }

    public String getChecksum(String serverName) {
      return checksums.getOrDefault(serverName, "");
    }

    /**
     * Checks if all non-null checksum values for the given server names are equal.
     *
     * <p>
     * Iterates through the provided collection of server names, retrieves their
     * corresponding
     * checksum values from the {@code checksums} map, and compares them. If all
     * non-null checksum
     * values are equal, returns {@code true}. If any non-null checksum value
     * differs, returns {@code false}.
     * If no non-null checksum values are found, returns {@code false}.
     * </p>
     *
     * @param serverNames the collection of server names to check
     * @return {@code true} if all non-null checksum values are equal and at least
     *         one exists;
     *         {@code false} otherwise
     */
    public boolean isEqualAcross(Collection<String> serverNames) {
      String ref = null;
      for (String s : serverNames) {
        String v = checksums.get(s);
        if (v == null)
          continue;
        if (ref == null) {
          ref = v;
        } else if (!ref.equals(v)) {
          return false;
        }
      }
      return ref != null;
    }
  }
}
