package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.SearchResultEntry;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Entry Comparison sub-tab for comparing LDAP entries across environments
 */
public class EntryComparisonTab extends VerticalLayout {

  private List<SearchResultEntry> comparisonEntries = new ArrayList<>();
  private Grid<ComparisonRow> comparisonGrid;
  private Span titleLabel;
  private Button clearButton;
  private VerticalLayout contentLayout;
  private VerticalLayout placeholderLayout;
  private MultiSelectComboBox<String> hideAttributesComboBox;
  private Checkbox includeOperationalAttributesCheckbox;
  private Set<String> hiddenAttributes = new HashSet<>();

  // Data model for comparison rows
  public static class ComparisonRow {
    private String attributeName;
    private Map<String, List<String>> valuesByEnvironment;

    public ComparisonRow(String attributeName) {
      this.attributeName = attributeName;
      this.valuesByEnvironment = new LinkedHashMap<>();
    }

    public String getAttributeName() {
      return attributeName;
    }

    public Map<String, List<String>> getValuesByEnvironment() {
      return valuesByEnvironment;
    }

    public void addEnvironmentValues(String environment, List<String> values) {
      valuesByEnvironment.put(environment, values != null ? values : new ArrayList<>());
    }

    public String getFormattedValues(String environment) {
      List<String> values = valuesByEnvironment.get(environment);
      if (values == null || values.isEmpty()) {
        return "";
      }
      return String.join("\n", values);
    }
  }

  public EntryComparisonTab() {
    initializeComponents();
    setupLayout();
  }

  private void initializeComponents() {
    // Title and controls
    titleLabel = new Span("No entries selected for comparison");
    titleLabel.getStyle().set("font-weight", "bold").set("color", "#333");

    clearButton = new Button("Clear Comparison", new Icon(VaadinIcon.TRASH));
    clearButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    clearButton.setVisible(false);
    clearButton.addClickListener(e -> clearComparison());

    // Hide Attributes multiselect dropdown
    hideAttributesComboBox = new MultiSelectComboBox<>();
    hideAttributesComboBox.setLabel("Hide Attributes");
    hideAttributesComboBox.setWidth("300px");
    hideAttributesComboBox.setVisible(false);
    hideAttributesComboBox.addSelectionListener(e -> {
      hiddenAttributes = new HashSet<>(e.getAllSelectedItems());
      refreshComparisonGrid();
    });

    // Include Operational Attributes checkbox
    includeOperationalAttributesCheckbox = new Checkbox("Include operational attributes");
    includeOperationalAttributesCheckbox.setValue(false);
    includeOperationalAttributesCheckbox.setVisible(false);
    includeOperationalAttributesCheckbox.addValueChangeListener(e -> refreshComparisonGrid());

    // Comparison grid
    comparisonGrid = new Grid<>(ComparisonRow.class, false);
    comparisonGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    comparisonGrid.setSizeFull();
    comparisonGrid.setVisible(false);

    // Attribute Name column (fixed)
    comparisonGrid.addColumn(ComparisonRow::getAttributeName)
        .setHeader("Attribute Name")
        .setWidth("200px")
        .setFlexGrow(0)
        .setSortable(true)
        .setComparator(Comparator.comparing(ComparisonRow::getAttributeName));

    // Content layout
    contentLayout = new VerticalLayout();
    contentLayout.setPadding(false);
    contentLayout.setSpacing(true);
    contentLayout.setSizeFull();
    
    // Controls layout for checkbox and dropdown
    HorizontalLayout controlsLayout = new HorizontalLayout();
    controlsLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    controlsLayout.setSpacing(true);
    controlsLayout.add(includeOperationalAttributesCheckbox, hideAttributesComboBox);
    
    contentLayout.add(controlsLayout, comparisonGrid);
    contentLayout.setFlexGrow(0, controlsLayout);
    contentLayout.setFlexGrow(1, comparisonGrid);
    contentLayout.setVisible(false);

    // Placeholder layout
    setupPlaceholderContent();
  }

  private void setupPlaceholderContent() {
    placeholderLayout = new VerticalLayout();
    placeholderLayout.setPadding(true);
    placeholderLayout.setSpacing(true);
    placeholderLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    placeholderLayout.getStyle().set("color", "#666");

    Icon icon = new Icon(VaadinIcon.TWIN_COL_SELECT);
    icon.setSize("48px");
    icon.getStyle().set("color", "#ccc");

    H4 title = new H4("Entry Comparison");
    title.getStyle().set("color", "#333");

    Span description = new Span(
        "Compare LDAP entries across different environments to identify differences and similarities.");
    description.getStyle().set("color", "#666").set("font-style", "italic").set("text-align", "center");

    Span instructions = new Span("To get started:");
    instructions.getStyle().set("color", "#333").set("font-weight", "bold").set("margin-top", "20px");

    VerticalLayout stepsList = new VerticalLayout();
    stepsList.setPadding(false);
    stepsList.setSpacing(false);
    stepsList.setDefaultHorizontalComponentAlignment(Alignment.START);

    Span step1 = new Span("1. Go to the Search tab");
    Span step2 = new Span("2. Perform a search to find entries");
    Span step3 = new Span("3. Check the boxes in the 'Compare' column (2-10 entries)");
    Span step4 = new Span("4. Click the 'Compare Selected' button");

    step1.getStyle().set("color", "#555");
    step2.getStyle().set("color", "#555");
    step3.getStyle().set("color", "#555");
    step4.getStyle().set("color", "#555");

    stepsList.add(step1, step2, step3, step4);

    placeholderLayout.add(icon, title, description, instructions, stepsList);
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("entry-comparison-tab");

    // Header with title and controls
    HorizontalLayout headerLayout = new HorizontalLayout();
    headerLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    headerLayout.setWidthFull();
    headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    headerLayout.add(titleLabel, clearButton);

    add(headerLayout, placeholderLayout, contentLayout);
    setFlexGrow(1, placeholderLayout);
    setFlexGrow(1, contentLayout);
  }

  /**
   * Set the entries to compare
   */
  public void setComparisonEntries(List<SearchResultEntry> entries) {
    this.comparisonEntries = new ArrayList<>(entries);
    if (entries.isEmpty()) {
      showPlaceholder();
    } else {
      buildComparisonTable();
      showComparisonTable();
    }
  }

  private void showPlaceholder() {
    titleLabel.setText("No entries selected for comparison");
    clearButton.setVisible(false);
    hideAttributesComboBox.setVisible(false);
    includeOperationalAttributesCheckbox.setVisible(false);
    contentLayout.setVisible(false);
    placeholderLayout.setVisible(true);
    setFlexGrow(1, placeholderLayout);
    setFlexGrow(0, contentLayout);
  }

  private void showComparisonTable() {
    titleLabel.setText(String.format("Comparing %d entries", comparisonEntries.size()));
    clearButton.setVisible(true);
    hideAttributesComboBox.setVisible(true);
    includeOperationalAttributesCheckbox.setVisible(true);
    placeholderLayout.setVisible(false);
    contentLayout.setVisible(true);
    setFlexGrow(0, placeholderLayout);
    setFlexGrow(1, contentLayout);
  }

  private void buildComparisonTable() {
    // Clear existing columns except the first one (Attribute Name)
    while (comparisonGrid.getColumns().size() > 1) {
      comparisonGrid.removeColumn(comparisonGrid.getColumns().get(1));
    }

    // Collect all unique attribute names for the hide attributes dropdown
    Set<String> allAttributes = new TreeSet<>();
    for (SearchResultEntry entry : comparisonEntries) {
      allAttributes.addAll(entry.getAttributes().keySet());
    }

    // Update hide attributes dropdown with all available attributes (excluding operational if not included)
    Set<String> visibleAttributes = new TreeSet<>();
    for (String attributeName : allAttributes) {
      if (includeOperationalAttributesCheckbox.getValue() || !isOperationalAttribute(attributeName)) {
        visibleAttributes.add(attributeName);
      }
    }
    hideAttributesComboBox.setItems(visibleAttributes);
    
    // Filter hidden attributes to only include those that are currently visible
    Set<String> filteredHiddenAttributes = new HashSet<>();
    for (String hiddenAttr : hiddenAttributes) {
      if (visibleAttributes.contains(hiddenAttr)) {
        filteredHiddenAttributes.add(hiddenAttr);
      }
    }
    hiddenAttributes = filteredHiddenAttributes;
    hideAttributesComboBox.select(hiddenAttributes);

    // Add dynamic columns for each entry/environment
    for (int i = 0; i < comparisonEntries.size(); i++) {
      SearchResultEntry entry = comparisonEntries.get(i);
      String columnHeader = String.format("%s\n(%s)",
          entry.getEnvironmentName(),
          truncateDn(entry.getDn()));

      final int entryIndex = i;
      comparisonGrid.addColumn(new ComponentRenderer<>(row -> {
        String formattedValues = row.getFormattedValues(getEnvironmentKey(entryIndex));
        if (formattedValues.isEmpty()) {
          return new Span("");
        }

        Div container = new Div();
        container.getStyle()
            .set("white-space", "pre-line")
            .set("word-wrap", "break-word")
            .set("line-height", "1.4");

        container.setText(formattedValues);
        return container;
      }))
          .setHeader(columnHeader)
          .setFlexGrow(1)
          .setResizable(true);
    }

    // Build and display comparison rows
    refreshComparisonGrid();
    comparisonGrid.setVisible(true);
  }

  private void refreshComparisonGrid() {
    // Collect all unique attribute names
    Set<String> allAttributes = new TreeSet<>();
    for (SearchResultEntry entry : comparisonEntries) {
      allAttributes.addAll(entry.getAttributes().keySet());
    }

    // Build comparison rows, filtering out hidden attributes and operational attributes if not included
    List<ComparisonRow> comparisonRows = new ArrayList<>();
    for (String attributeName : allAttributes) {
      // Skip if this attribute is in the hidden set
      if (hiddenAttributes.contains(attributeName)) {
        continue;
      }

      // Skip operational attributes if the checkbox is not checked
      if (!includeOperationalAttributesCheckbox.getValue() && isOperationalAttribute(attributeName)) {
        continue;
      }

      ComparisonRow row = new ComparisonRow(attributeName);

      for (int i = 0; i < comparisonEntries.size(); i++) {
        SearchResultEntry entry = comparisonEntries.get(i);
        List<String> values = entry.getAttributeValues(attributeName);
        row.addEnvironmentValues(getEnvironmentKey(i), values);
      }

      comparisonRows.add(row);
    }

    comparisonGrid.setItems(comparisonRows);
  }

  /**
   * Check if an attribute is operational (system-generated)
   */
  private boolean isOperationalAttribute(String attributeName) {
    String lowerName = attributeName.toLowerCase();
    return lowerName.startsWith("create")
        || lowerName.startsWith("modify")
        || lowerName.equals("entryuuid")
        || lowerName.equals("entrydd")
        || lowerName.equals("entrycsn")
        || lowerName.equals("hassubordinates")
        || lowerName.equals("subschemasubentry")
        || lowerName.equals("pwdchangedtime")
        || lowerName.equals("pwdaccountlockedtime")
        || lowerName.equals("pwdfailuretime")
        || lowerName.equals("structuralobjectclass")
        || lowerName.startsWith("ds-")
        || lowerName.startsWith("nsds-")
        || lowerName.contains("timestamp")
        // Additional common operational attributes
        || lowerName.equals("entrydn")
        || lowerName.equals("modifiersname")
        || lowerName.equals("creatorsname")
        || lowerName.equals("modifytimestamp")
        || lowerName.equals("createtimestamp")
        || lowerName.equals("contextcsn")
        || lowerName.equals("numsubordinates")
        || lowerName.equals("subordinatecount")
        || lowerName.startsWith("operational")
        || lowerName.startsWith("ds")
        || lowerName.startsWith("ads-")
        || lowerName.startsWith("ibm-")
        || lowerName.startsWith("sun-")
        || lowerName.startsWith("oracle-")
        || lowerName.startsWith("microsoft-")
        || lowerName.startsWith("novell-");
  }

  private String getEnvironmentKey(int index) {
    return "env_" + index;
  }

  private String truncateDn(String dn) {
    if (dn == null || dn.length() <= 40) {
      return dn;
    }
    return dn.substring(0, 37) + "...";
  }

  private void clearComparison() {
    comparisonEntries.clear();
    hiddenAttributes.clear();
    hideAttributesComboBox.clear();
    comparisonGrid.setItems(new ArrayList<>());
    showPlaceholder();
  }

  /**
   * Clear the comparison tab content.
   */
  public void clear() {
    clearComparison();
  }
}