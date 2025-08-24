package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.util.SchemaCompareUtil;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.AttributeSyntaxDefinition;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleUseDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
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
 * Schema comparison tab for group operations. Displays per-server checksums of schema elements
 * across all servers in the selected group with an equality indicator.
 */
public class GroupSchemaTab extends VerticalLayout {

  private final LdapService ldapService;

  private final TabSheet tabSheet = new TabSheet();
  private final Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
  private final Span statusLabel = new Span();

  private Set<LdapServerConfig> environments = new HashSet<>();
  private List<LdapServerConfig> sortedServers = new ArrayList<>();

  // Grids per schema component
  private Grid<RowModel> ocGrid;
  private Grid<RowModel> atGrid;
  private Grid<RowModel> mrGrid;
  private Grid<RowModel> mruGrid;
  private Grid<RowModel> synGrid;

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

    refreshButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    refreshButton.addClickListener(e -> loadAndRender());

    statusLabel.getStyle().set("font-style", "italic").set("color", "#666");

    HorizontalLayout controls = new HorizontalLayout(refreshButton, statusLabel);
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

    tabSheet.add(new Tab("Object Classes"), ocGrid);
    tabSheet.add(new Tab("Attribute Types"), atGrid);
    tabSheet.add(new Tab("Matching Rules"), mrGrid);
    tabSheet.add(new Tab("Matching Rule Use"), mruGrid);
    tabSheet.add(new Tab("Syntaxes"), synGrid);

    add(header, controls, tabSheet);
    setFlexGrow(1, tabSheet);
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

    // Fetch schemas with an all-or-none decision: if any server lacks support, don't use the control anywhere
    for (LdapServerConfig cfg : sortedServers) {
      String serverName = displayName(cfg);
      try {
        // Connections ensured in previous loop; fetch schema honoring group-wide decision
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
      statusLabel.setText("Loaded with " + errors + " error(s). Extended schema info control " + (allSupportExtended ? "used" : "disabled for consistency") + ".");
      Notification n = Notification.show("Some servers failed to load schema.");
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    } else {
    statusLabel.setText("Schema loaded. Extended schema info control " + (schemas.isEmpty() ? "n/a" : (allSupportExtended ? "used" : "disabled for consistency")) + ".");
    }
  }

  private void setGridsEmpty() {
    ocGrid.setItems(List.of());
    atGrid.setItems(List.of());
    mrGrid.setItems(List.of());
    mruGrid.setItems(List.of());
    synGrid.setItems(List.of());
  }

  private void renderObjectClasses(Map<String, Schema> schemas) {
    setupColumns(ocGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.OBJECT_CLASS);
    ocGrid.setItems(toModels(rows, schemas.keySet()));
  }

  private void renderAttributeTypes(Map<String, Schema> schemas) {
    setupColumns(atGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.ATTRIBUTE_TYPE);
    atGrid.setItems(toModels(rows, schemas.keySet()));
  }

  private void renderMatchingRules(Map<String, Schema> schemas) {
    setupColumns(mrGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.MATCHING_RULE);
    mrGrid.setItems(toModels(rows, schemas.keySet()));
  }

  private void renderMatchingRuleUse(Map<String, Schema> schemas) {
    setupColumns(mruGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.MATCHING_RULE_USE);
    mruGrid.setItems(toModels(rows, schemas.keySet()));
  }

  private void renderSyntaxes(Map<String, Schema> schemas) {
    setupColumns(synGrid, schemas.keySet());
    Map<String, Map<String, String>> rows = buildRowsFor(schemas, ComponentType.SYNTAX);
    synGrid.setItems(toModels(rows, schemas.keySet()));
  }

  private void setupColumns(Grid<RowModel> grid, Collection<String> serverNames) {
    grid.removeAllColumns();
    grid.addColumn(RowModel::getName).setHeader("Name").setAutoWidth(true).setFrozen(true);
    for (String server : serverNames) {
      grid.addColumn(row -> row.getChecksum(server)).setHeader(server).setAutoWidth(true);
    }
    grid.addColumn(row -> row.isEqualAcross(serverNames) ? "Equal" : "Unequal")
        .setHeader("Status").setAutoWidth(true);
  }

  private enum ComponentType { OBJECT_CLASS, ATTRIBUTE_TYPE, MATCHING_RULE, MATCHING_RULE_USE, SYNTAX }

  private Map<String, Map<String, String>> buildRowsFor(Map<String, Schema> schemas, ComponentType type) {
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

  // Row model for the grid with dynamic per-server checksum map
  public static class RowModel {
    private final String name;
    private final Map<String, String> checksums; // serverName -> checksum

    public RowModel(String name, Map<String, String> checksums) {
      this.name = name;
      this.checksums = checksums;
    }

    public String getName() { return name; }

    public String getChecksum(String serverName) {
      return checksums.getOrDefault(serverName, "");
    }

    public boolean isEqualAcross(Collection<String> serverNames) {
      String ref = null;
      for (String s : serverNames) {
        String v = checksums.get(s);
        if (v == null) continue;
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
