package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Advanced search builder component for constructing complex LDAP search
 * filters
 * Supports Filter Units, Logical Filter Groupings, and Nesting
 */
@CssImport("./styles/advanced-search.css")
public class AdvancedSearchBuilder extends VerticalLayout {

  private final LdapService ldapService;

  // Search base field
  private DnSelectorField searchBaseField;

  // Filter groups container
  private VerticalLayout filterGroupsContainer;
  private List<FilterGroup> filterGroups;

  // Root logical operator for combining filter groups
  private ComboBox<LogicalOperator> rootLogicalOperator;
  private HorizontalLayout rootOperatorLayout;

  // Result filter field
  private TextArea resultFilterField;

  // Generated filter
  private TextArea generatedFilterArea;

  // Control buttons
  private Button addFilterGroupButton;

  // Common LDAP attributes for dropdown
  private static final String[] COMMON_ATTRIBUTES = {
      "cn", "uid", "mail", "sn", "givenName", "telephoneNumber",
      "objectClass", "ou", "dc", "memberOf", "member", "uniqueMember",
      "description", "title", "department", "employeeNumber"
  };

  // LDAP filter operators
  private enum FilterOperator {
    EQUALS("=", "equals"),
    NOT_EQUALS("!=", "not equals"),
    GREATER_EQUAL(">=", "greater than or equal"),
    LESS_EQUAL("<=", "less than or equal"),
    STARTS_WITH("starts", "starts with"),
    ENDS_WITH("ends", "ends with"),
    CONTAINS("contains", "contains"),
    EXISTS("exists", "exists"),
    NOT_EXISTS("!exists", "does not exist");

    private final String symbol;
    private final String description;

    FilterOperator(String symbol, String description) {
      this.symbol = symbol;
      this.description = description;
    }

    public String getSymbol() {
      return symbol;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  // Logical operators for grouping and nesting
  private enum LogicalOperator {
    AND("&", "AND"),
    OR("|", "OR"),
    NOT("!", "NOT");

    private final String symbol;
    private final String description;

    LogicalOperator(String symbol, String description) {
      this.symbol = symbol;
      this.description = description;
    }

    public String getSymbol() {
      return symbol;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  public AdvancedSearchBuilder(LdapService ldapService) {
    this.ldapService = ldapService;
    this.filterGroups = new ArrayList<>();

    initializeComponents();
    setupLayout();
    addInitialFilterGroup();
  }

  private void initializeComponents() {
    // Search base field
    searchBaseField = new DnSelectorField("Search Base DN", ldapService);
    searchBaseField.setPlaceholder("Leave blank to use default search base for the environment");
    searchBaseField.setWidthFull();
    searchBaseField.getStyle().set("margin-bottom", "10px");

    // Root logical operator for combining filter groups
    rootLogicalOperator = new ComboBox<>("Combine Groups With");
    rootLogicalOperator.setItems(LogicalOperator.values());
    rootLogicalOperator.setValue(LogicalOperator.AND);
    rootLogicalOperator.setWidth("150px");
    rootLogicalOperator.addValueChangeListener(e -> updateGeneratedFilter());

    // Filter groups container
    filterGroupsContainer = new VerticalLayout();
    filterGroupsContainer.setPadding(false);
    filterGroupsContainer.setSpacing(true);
    filterGroupsContainer.addClassName("filter-groups-container");

    // Add filter group button
    addFilterGroupButton = new Button("Add Filter Group", new Icon(VaadinIcon.PLUS));
    addFilterGroupButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    addFilterGroupButton.addClickListener(e -> addFilterGroup());

    // Generated Filter Area (editable)
    generatedFilterArea = new TextArea("Generated LDAP Filter");
    generatedFilterArea.setPlaceholder("(Generated LDAP filter will appear here and can be edited)");
    generatedFilterArea.setWidthFull();
    generatedFilterArea.setHeight("100px");
    generatedFilterArea.getStyle().set("font-family", "monospace");
    generatedFilterArea.getStyle().set("margin-bottom", "10px");
    generatedFilterArea.addValueChangeListener(e -> {
      // Update the internal result filter when manually edited
      if (resultFilterField != null) {
        resultFilterField.setValue(e.getValue());
      }
    });

    // Result filter field (hidden, for internal use and compatibility)
    resultFilterField = new TextArea("Generated LDAP Filter");
    resultFilterField.setWidthFull();
    resultFilterField.setReadOnly(true);
    resultFilterField.setHeight("80px");
    resultFilterField.getStyle().set("font-family", "monospace");
    resultFilterField.setVisible(false);
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(false);
    setSpacing(true);
    addClassName("advanced-search-builder");

    // Instructions
    Span instructions = new Span("Build complex LDAP search filters using filter groups. " +
        "Each group contains one or more filter units. Multiple groups can be combined with logical operators (AND, OR, NOT).");
    instructions.addClassName("instructions");
    instructions.getStyle().set("font-size", "14px").set("color", "#666").set("margin-bottom", "15px");

    // Root logical operator (only show if multiple groups)
    rootOperatorLayout = new HorizontalLayout();
    rootOperatorLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    rootOperatorLayout.add(new Span("Combine filter groups with:"), rootLogicalOperator);
    rootOperatorLayout.setVisible(false); // Initially hidden
    rootOperatorLayout.addClassName("root-operator-layout");

    add(instructions, searchBaseField, rootOperatorLayout,
        filterGroupsContainer, addFilterGroupButton, generatedFilterArea, resultFilterField);
  }

  private void addInitialFilterGroup() {
    addFilterGroup();
  }

  private void addFilterGroup() {
    FilterGroup group = new FilterGroup();
    filterGroups.add(group);
    filterGroupsContainer.add(group);
    updateGeneratedFilter();

    // Show root operator selector if we have more than one group
    updateRootOperatorVisibility();
  }

  private void removeFilterGroup(FilterGroup group) {
    filterGroups.remove(group);
    filterGroupsContainer.remove(group);
    updateGeneratedFilter();

    // Hide root operator selector if we only have one group
    updateRootOperatorVisibility();
  }

  private void updateRootOperatorVisibility() {
    boolean showRootOperator = filterGroups.size() > 1;
    rootOperatorLayout.setVisible(showRootOperator);
  }

  private void updateGeneratedFilter() {
    String filter = buildLdapFilter();
    resultFilterField.setValue(filter);

    // Update the editable generated filter area
    if (generatedFilterArea != null) {
      generatedFilterArea.setValue(filter);
    }

    // Notify listeners that the filter has changed
    fireFilterChangeEvent();
  }

  private void fireFilterChangeEvent() {
    getElement().executeJs("this.dispatchEvent(new CustomEvent('filter-changed', {detail: {filter: $0}}));",
        getGeneratedFilter());
  }

  private String buildLdapFilter() {
    if (filterGroups.isEmpty()) {
      return "";
    }

    List<String> groupFilters = new ArrayList<>();
    for (FilterGroup group : filterGroups) {
      String groupFilter = group.buildFilterExpression();
      if (!groupFilter.isEmpty()) {
        groupFilters.add(groupFilter);
      }
    }

    if (groupFilters.isEmpty()) {
      return "";
    }

    if (groupFilters.size() == 1) {
      return groupFilters.get(0);
    }

    // Multiple groups - combine with root logical operator
    LogicalOperator rootOp = rootLogicalOperator.getValue();
    StringBuilder filter = new StringBuilder();

    if (rootOp == LogicalOperator.NOT) {
      // NOT operator applies to the entire combined expression
      filter.append("(!").append(buildCombinedExpression(groupFilters, LogicalOperator.AND)).append(")");
    } else {
      filter.append(buildCombinedExpression(groupFilters, rootOp));
    }

    return filter.toString();
  }

  private String buildCombinedExpression(List<String> expressions, LogicalOperator operator) {
    if (expressions.size() == 1) {
      return expressions.get(0);
    }

    StringBuilder result = new StringBuilder();
    result.append("(").append(operator.getSymbol());
    for (String expr : expressions) {
      result.append(expr);
    }
    result.append(")");
    return result.toString();
  }

  /**
   * Get the search base DN
   */
  public String getSearchBase() {
    return searchBaseField.getValue();
  }

  /**
   * Set the server configuration for the DN selector field
   */
  public void setServerConfig(LdapServerConfig serverConfig) {
    searchBaseField.setServerConfig(serverConfig);
  }

  /**
   * Get the generated LDAP filter
   */
  public String getGeneratedFilter() {
    return resultFilterField.getValue();
  }

  /**
   * Clear all search criteria
   */
  public void clear() {
    filterGroups.clear();
    filterGroupsContainer.removeAll();
    searchBaseField.clear();
    resultFilterField.clear();
    if (generatedFilterArea != null) {
      generatedFilterArea.clear();
    }
    addInitialFilterGroup();
  }

  /**
   * Get the manually edited LDAP filter from the editable area
   */
  public String getEditedFilter() {
    return generatedFilterArea != null ? generatedFilterArea.getValue() : getGeneratedFilter();
  }

  /**
   * Filter Group - represents a logical grouping of filter units
   */
  private class FilterGroup extends VerticalLayout {

    private ComboBox<LogicalOperator> groupLogicalOperator;
    private VerticalLayout filterUnitsContainer;
    private List<FilterUnit> filterUnits;
    private Button addFilterUnitButton;
    private Button removeGroupButton;
    private Span groupLabel;

    public FilterGroup() {
      this.filterUnits = new ArrayList<>();
      initializeGroupComponents();
      setupGroupLayout();
      addInitialFilterUnit();
    }

    private void initializeGroupComponents() {
      // Group label and controls
      groupLabel = new Span("Filter Group " + (filterGroups.size() + 1));
      groupLabel.getStyle().set("font-weight", "bold").set("color", "#1976d2");

      // Group logical operator (for combining filter units within this group)
      groupLogicalOperator = new ComboBox<>("Combine Units With");
      groupLogicalOperator.setItems(LogicalOperator.AND, LogicalOperator.OR, LogicalOperator.NOT);
      groupLogicalOperator.setValue(LogicalOperator.AND);
      groupLogicalOperator.setWidth("150px");
      groupLogicalOperator.addValueChangeListener(e -> updateGeneratedFilter());

      // Filter units container
      filterUnitsContainer = new VerticalLayout();
      filterUnitsContainer.setPadding(false);
      filterUnitsContainer.setSpacing(true);
      filterUnitsContainer.addClassName("filter-units-container");

      // Add filter unit button
      addFilterUnitButton = new Button("Add Filter Unit", new Icon(VaadinIcon.PLUS_CIRCLE));
      addFilterUnitButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
      addFilterUnitButton.addClickListener(e -> addFilterUnit());
      
      // Force proper styling with inline styles
      addFilterUnitButton.getStyle().set("background", "transparent");
      addFilterUnitButton.getStyle().set("border", "2px solid var(--lumo-primary-color)");
      addFilterUnitButton.getStyle().set("color", "var(--lumo-primary-color)");
      addFilterUnitButton.getStyle().set("border-radius", "var(--lumo-border-radius-s)");

      // Remove group button
      removeGroupButton = new Button("Remove Group", new Icon(VaadinIcon.TRASH));
      removeGroupButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
      removeGroupButton.addClickListener(e -> removeFilterGroup(this));
      
      // Force proper styling with inline styles
      removeGroupButton.getStyle().set("background", "var(--lumo-error-color)");
      removeGroupButton.getStyle().set("border", "2px solid var(--lumo-error-color)");
      removeGroupButton.getStyle().set("color", "var(--lumo-error-contrast-color)");
      removeGroupButton.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
    }

    private void setupGroupLayout() {
      setPadding(true);
      setSpacing(true);
      addClassName("filter-group");
      getStyle().set("border", "2px solid #e3f2fd")
          .set("border-radius", "8px")
          .set("background", "#fafafa")
          .set("margin", "8px 0");

      // Header with group label and controls
      HorizontalLayout header = new HorizontalLayout();
      header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
      header.setWidthFull();
      header.add(groupLabel, groupLogicalOperator, removeGroupButton);
      header.setFlexGrow(1, groupLabel);

      add(header, filterUnitsContainer, addFilterUnitButton);
    }

    private void addInitialFilterUnit() {
      addFilterUnit();
    }

    private void addFilterUnit() {
      FilterUnit unit = new FilterUnit();
      filterUnits.add(unit);
      filterUnitsContainer.add(unit);
      updateGeneratedFilter();
      updateGroupLogicalOperatorVisibility();
    }

    private void removeFilterUnit(FilterUnit unit) {
      filterUnits.remove(unit);
      filterUnitsContainer.remove(unit);
      updateGeneratedFilter();
      updateGroupLogicalOperatorVisibility();
    }

    private void updateGroupLogicalOperatorVisibility() {
      boolean showGroupOperator = filterUnits.size() > 1;
      groupLogicalOperator.setVisible(showGroupOperator);
    }

    public String buildFilterExpression() {
      List<String> unitFilters = new ArrayList<>();
      for (FilterUnit unit : filterUnits) {
        String unitFilter = unit.buildFilterExpression();
        if (!unitFilter.isEmpty()) {
          unitFilters.add(unitFilter);
        }
      }

      if (unitFilters.isEmpty()) {
        return "";
      }

      if (unitFilters.size() == 1) {
        LogicalOperator groupOp = groupLogicalOperator.getValue();
        if (groupOp == LogicalOperator.NOT) {
          return "(!" + unitFilters.get(0) + ")";
        } else {
          return unitFilters.get(0);
        }
      }

      // Multiple units - combine with group logical operator
      LogicalOperator groupOp = groupLogicalOperator.getValue();
      if (groupOp == LogicalOperator.NOT) {
        // NOT applies to the entire combined expression
        return "(!(" + LogicalOperator.AND.getSymbol() + String.join("", unitFilters) + "))";
      } else {
        return "(" + groupOp.getSymbol() + String.join("", unitFilters) + ")";
      }
    }
  }

  /**
   * Filter Unit - represents a single LDAP filter expression like (cn=john)
   */
  private class FilterUnit extends HorizontalLayout {

    private ComboBox<String> attributeCombo;
    private ComboBox<FilterOperator> operatorCombo;
    private TextField valueField;
    private Button removeButton;

    public FilterUnit() {
      initializeUnitComponents();
      setupUnitLayout();
    }

    private void initializeUnitComponents() {
      // Attribute selector
      attributeCombo = new ComboBox<>("Attribute");
      attributeCombo.setItems(Arrays.asList(COMMON_ATTRIBUTES));
      attributeCombo.setAllowCustomValue(true);
      attributeCombo.setWidth("150px");
      attributeCombo.addValueChangeListener(e -> updateGeneratedFilter());
      attributeCombo.addCustomValueSetListener(e -> {
        attributeCombo.setValue(e.getDetail());
        updateGeneratedFilter();
      });

      // Operator selector
      operatorCombo = new ComboBox<>("Operator");
      operatorCombo.setItems(FilterOperator.values());
      operatorCombo.setValue(FilterOperator.EQUALS);
      operatorCombo.setWidth("150px");
      operatorCombo.addValueChangeListener(e -> {
        updateValueFieldVisibility();
        updateGeneratedFilter();
      });

      // Value field
      valueField = new TextField("Value");
      valueField.setWidth("200px");
      valueField.addValueChangeListener(e -> updateGeneratedFilter());

      // Remove button
      removeButton = new Button(new Icon(VaadinIcon.MINUS_CIRCLE));
      removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
      removeButton.addClickListener(e -> {
        // Find parent filter group and remove this unit
        FilterGroup parentGroup = findParentFilterGroup();
        if (parentGroup != null) {
          parentGroup.removeFilterUnit(this);
        }
      });
      
      // Force proper styling with inline styles
      removeButton.getStyle().set("background", "var(--lumo-error-color)");
      removeButton.getStyle().set("border", "2px solid var(--lumo-error-color)");
      removeButton.getStyle().set("color", "var(--lumo-error-contrast-color)");
      removeButton.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
    }

    private void setupUnitLayout() {
      setDefaultVerticalComponentAlignment(Alignment.END);
      setSpacing(true);
      setWidthFull();
      addClassName("filter-unit");
      getStyle().set("padding", "8px")
          .set("border", "1px solid #ddd")
          .set("border-radius", "4px")
          .set("background", "white");

      add(attributeCombo, operatorCombo, valueField, removeButton);
      setFlexGrow(0, attributeCombo, operatorCombo, removeButton);
      setFlexGrow(1, valueField);
    }

    private void updateValueFieldVisibility() {
      FilterOperator operator = operatorCombo.getValue();
      boolean needsValue = operator != FilterOperator.EXISTS && operator != FilterOperator.NOT_EXISTS;
      valueField.setVisible(needsValue);
    }

    private FilterGroup findParentFilterGroup() {
      return filterGroups.stream()
          .filter(group -> group.filterUnits.contains(this))
          .findFirst()
          .orElse(null);
    }

    public String buildFilterExpression() {
      String attribute = attributeCombo.getValue();
      FilterOperator operator = operatorCombo.getValue();
      String value = valueField.getValue();

      if (attribute == null || attribute.trim().isEmpty()) {
        return "";
      }

      switch (operator) {
        case EQUALS:
          return value != null && !value.trim().isEmpty() ? "(" + attribute + "=" + value + ")" : "";
        case NOT_EQUALS:
          return value != null && !value.trim().isEmpty() ? "(!(" + attribute + "=" + value + "))" : "";
        case GREATER_EQUAL:
          return value != null && !value.trim().isEmpty() ? "(" + attribute + ">=" + value + ")" : "";
        case LESS_EQUAL:
          return value != null && !value.trim().isEmpty() ? "(" + attribute + "<=" + value + ")" : "";
        case STARTS_WITH:
          return value != null && !value.trim().isEmpty() ? "(" + attribute + "=" + value + "*)" : "";
        case ENDS_WITH:
          return value != null && !value.trim().isEmpty() ? "(" + attribute + "=*" + value + ")" : "";
        case CONTAINS:
          return value != null && !value.trim().isEmpty() ? "(" + attribute + "=*" + value + "*)" : "";
        case EXISTS:
          return "(" + attribute + "=*)";
        case NOT_EXISTS:
          return "(!(" + attribute + "=*))";
        default:
          return "";
      }
    }
  }
}
