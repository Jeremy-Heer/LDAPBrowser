package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.model.SearchResultEntry;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
* Directory Search sub-tab for searching users, groups, and all entries across multiple environments
*/
public class DirectorySearchSubTab extends VerticalLayout {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  // Parent tab reference for environment dropdown
  private DirectorySearchTab parentTab;

  // Sub-tabs for basic and advanced search
  private TabSheet searchTabSheet;
  private Tab basicTab;
  private Tab advancedTab;

  // Basic search components
  private VerticalLayout basicSearchLayout;
  private TextField nameField;
  private ComboBox<SearchType> typeComboBox;
  private Button searchButton;

  // Advanced search components
  private VerticalLayout advancedSearchLayout;
  private AdvancedSearchBuilder advancedSearchBuilder;
  private Button advancedSearchButton;

  // Results components
  private Grid<SearchResultEntry> resultsGrid;
  private Span resultCountLabel;
  private VerticalLayout resultsContainer;
  private HorizontalLayout paginationLayout;

  // Comparison functionality
  private final Set<SearchResultEntry> selectedForComparison = new HashSet<>();
  private final Map<SearchResultEntry, Checkbox> comparisonCheckboxes = new HashMap<>();
  private Checkbox selectAllCheckbox;
  private Button compareButton;
  private Span comparisonCountLabel;
  private Consumer<List<SearchResultEntry>> comparisonCallback;
  private java.util.function.BiConsumer<List<SearchResultEntry>, String> searchResultsCallback;

  // Pagination
  private static final int RESULTS_PER_PAGE = 100;
  private List<SearchResultEntry> allResults = new ArrayList<>();
  private int currentPage = 0;
  private Button prevPageButton;
  private Button nextPageButton;
  private Span pageInfoLabel;

  // Search types
  private enum SearchType {
    USER("User", "(&(objectClass=person)(uid=%s))"),
    GROUP("Group", "(&(|(objectClass=groupOfUniqueNames)(objectClass=groupofURLs)(objectClass=groupOfNames))(cn=%s))"),
    ALL("All", "(|(cn=%s)(uid=%s))");

    private final String label;
    private final String filterTemplate;

    SearchType(String label, String filterTemplate) {
      this.label = label;
      this.filterTemplate = filterTemplate;
    }

    public String getLabel() { return label; }
    public String getFilterTemplate() { return filterTemplate; }

    @Override
    public String toString() { return label; }
  }

  public DirectorySearchSubTab(LdapService ldapService, ConfigurationService configurationService,
  InMemoryLdapService inMemoryLdapService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    initializeComponents();
    setupLayout();
  }

  /**
  * Set the parent tab to access the environment dropdown
  */
  public void setParentTab(DirectorySearchTab parentTab) {
    this.parentTab = parentTab;
  }

  private void initializeComponents() {
    // Create the search tab sheet
    searchTabSheet = new TabSheet();
    searchTabSheet.setSizeFull();

    // Basic search tab
    basicTab = new Tab("Basic");
    basicSearchLayout = createBasicSearchLayout();
    searchTabSheet.add(basicTab, basicSearchLayout);

    // Advanced search tab
    advancedTab = new Tab("Advanced");
    advancedSearchLayout = createAdvancedSearchLayout();
    searchTabSheet.add(advancedTab, advancedSearchLayout);

    // Set Basic as the default selected tab
    searchTabSheet.setSelectedTab(basicTab);

    // Initialize results grid and other common components
    initializeResultsComponents();
  }

  private VerticalLayout createBasicSearchLayout() {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(true);
    layout.setSpacing(true);
    layout.addClassName("basic-search-layout");

    // Search field
    nameField = new TextField("Name");
    nameField.setPlaceholder("Enter search term...");
    nameField.setWidthFull();
    nameField.addValueChangeListener(e -> updateSearchButton());

    // Type selector
    typeComboBox = new ComboBox<>("Type");
    typeComboBox.setItems(SearchType.values());
    typeComboBox.setValue(SearchType.ALL);
    typeComboBox.setItemLabelGenerator(SearchType::getLabel);
    typeComboBox.setWidthFull();

    // Search button
    searchButton = new Button("Search", new Icon(VaadinIcon.SEARCH));
    searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    searchButton.setEnabled(false);
    searchButton.addClickListener(e -> performBasicSearch());

    // Search form layout
    HorizontalLayout searchForm = new HorizontalLayout();
    searchForm.setDefaultVerticalComponentAlignment(Alignment.END);
    searchForm.setSpacing(true);
    searchForm.setWidthFull();

    VerticalLayout nameFieldContainer = new VerticalLayout();
    nameFieldContainer.setPadding(false);
    nameFieldContainer.setSpacing(false);
    nameFieldContainer.add(nameField);
    nameFieldContainer.setFlexGrow(1, nameField);

    VerticalLayout typeFieldContainer = new VerticalLayout();
    typeFieldContainer.setPadding(false);
    typeFieldContainer.setSpacing(false);
    typeFieldContainer.add(typeComboBox);
    typeFieldContainer.setWidth("150px");

    searchForm.add(nameFieldContainer, typeFieldContainer, searchButton);
    searchForm.setFlexGrow(1, nameFieldContainer);

    layout.add(searchForm);
    return layout;
  }

  private VerticalLayout createAdvancedSearchLayout() {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(true);
    layout.setSpacing(true);
    layout.addClassName("advanced-search-layout");

    // Advanced search builder
    advancedSearchBuilder = new AdvancedSearchBuilder();
    
    // Listen for filter changes to update search button
    advancedSearchBuilder.getElement().addEventListener("filter-changed", e -> updateAdvancedSearchButton());

    // Advanced search button
    advancedSearchButton = new Button("Search", new Icon(VaadinIcon.SEARCH));
    advancedSearchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    advancedSearchButton.addClickListener(e -> performAdvancedSearch());

    HorizontalLayout buttonLayout = new HorizontalLayout();
    buttonLayout.setJustifyContentMode(JustifyContentMode.END);
    buttonLayout.add(advancedSearchButton);

    layout.add(advancedSearchBuilder, buttonLayout);
    layout.setFlexGrow(1, advancedSearchBuilder);
    return layout;
  }

  private void initializeResultsComponents() {
    // Results grid
    resultsGrid = new Grid<>(SearchResultEntry.class, false);

    // Create select all checkbox for the header
    selectAllCheckbox = new Checkbox();
    selectAllCheckbox.addValueChangeListener(event -> {
      boolean selectAll = event.getValue();
      if (selectAll) {
        // Select all visible entries (up to 10)
        List<SearchResultEntry> visibleEntries = getCurrentPageEntries();
        int toSelect = Math.min(10, visibleEntries.size());
        selectedForComparison.clear();
        for (int i = 0; i < toSelect; i++) {
          selectedForComparison.add(visibleEntries.get(i));
        }
        if (toSelect < visibleEntries.size()) {
          showError("You can select up to 10 entries for comparison. Selected first 10 entries.");
        }
      } else {
        selectedForComparison.clear();
      }
      updateAllCheckboxes();
      updateComparisonControls();
    });

    // Compare column with checkbox
    resultsGrid.addComponentColumn(entry -> {
      Checkbox checkbox = new Checkbox();
      checkbox.setValue(selectedForComparison.contains(entry));
      checkbox.addValueChangeListener(event -> {
        if (event.getValue()) {
          if (selectedForComparison.size() < 10) {
            selectedForComparison.add(entry);
          } else {
            checkbox.setValue(false);
            showError("You can select up to 10 entries for comparison");
            return;
          }
        } else {
          selectedForComparison.remove(entry);
        }
        comparisonCheckboxes.put(entry, checkbox);
        updateComparisonControls();
        updateSelectAllCheckbox();
      });
      comparisonCheckboxes.put(entry, checkbox);
      return checkbox;
    }).setHeader(selectAllCheckbox).setWidth("80px").setFlexGrow(0);

    resultsGrid.addColumn(entry -> {
      // Determine icon based on object classes
      List<String> objectClasses = entry.getAttributeValues("objectClass");
      if (objectClasses.contains("person") || objectClasses.contains("inetOrgPerson")) {
        return "👤"; // User icon
      } else if (objectClasses.contains("groupOfUniqueNames") ||
          objectClasses.contains("groupofURLs") ||
          objectClasses.contains("groupOfNames")) {
        return "👥"; // Group icon
      } else if (objectClasses.contains("organizationalUnit")) {
        return "📁"; // Folder icon
      } else {
        return "📄"; // Generic entry icon
      }
    }).setWidth("60px").setFlexGrow(0);

    resultsGrid.addColumn(SearchResultEntry::getDisplayName)
        .setHeader("Name")
        .setSortable(true);

    resultsGrid.addColumn(SearchResultEntry::getDn)
        .setHeader("Distinguished Name")
        .setSortable(true);

    resultsGrid.addColumn(entry -> String.join(", ", entry.getAttributeValues("objectClass")))
        .setHeader("Object Classes")
        .setSortable(true);

    // Add environment column
    resultsGrid.addColumn(SearchResultEntry::getEnvironmentName)
        .setHeader("Environment")
        .setSortable(true);

    resultsGrid.setSizeFull();
    resultsGrid.setVisible(false);

    // Add click listener for entry details popup
    resultsGrid.addItemClickListener(event -> {
      SearchResultEntry selectedEntry = event.getItem();
      if (selectedEntry != null) {
        showEntryDetailsDialog(selectedEntry.getEntry(), selectedEntry.getEnvironment());
      }
    });

    // Result count label
    resultCountLabel = new Span();
    resultCountLabel.getStyle().set("font-style", "italic");
    resultCountLabel.setVisible(false);

    // Pagination controls
    prevPageButton = new Button("Previous", new Icon(VaadinIcon.ARROW_LEFT));
    prevPageButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    prevPageButton.setEnabled(false);
    prevPageButton.addClickListener(e -> navigateToPage(currentPage - 1));

    nextPageButton = new Button("Next", new Icon(VaadinIcon.ARROW_RIGHT));
    nextPageButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    nextPageButton.setEnabled(false);
    nextPageButton.addClickListener(e -> navigateToPage(currentPage + 1));

    pageInfoLabel = new Span();
    pageInfoLabel.getStyle().set("font-style", "italic").set("color", "#666");

    paginationLayout = new HorizontalLayout();
    paginationLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    paginationLayout.setSpacing(true);
    paginationLayout.add(prevPageButton, pageInfoLabel, nextPageButton);
    paginationLayout.setVisible(false);

    // Comparison controls
    comparisonCountLabel = new Span("0 entries selected for comparison");
    comparisonCountLabel.getStyle().set("font-style", "italic").set("color", "#666");

    compareButton = new Button("Compare Selected", new Icon(VaadinIcon.TWIN_COL_SELECT));
    compareButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    compareButton.setEnabled(false);
    compareButton.addClickListener(e -> performComparison());

    HorizontalLayout comparisonControls = new HorizontalLayout();
    comparisonControls.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    comparisonControls.setSpacing(true);
    comparisonControls.add(comparisonCountLabel, compareButton);
    comparisonControls.setVisible(false);
    comparisonControls.addClassName("comparison-controls");

    // Results container
    resultsContainer = new VerticalLayout();
    resultsContainer.setPadding(false);
    resultsContainer.setSpacing(true);
    resultsContainer.add(resultCountLabel, resultsGrid, paginationLayout, comparisonControls);
    resultsContainer.setFlexGrow(1, resultsGrid);
    resultsContainer.setSizeFull();
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("directory-search-sub-tab");

    // Add only the search tab sheet (results will be displayed in dedicated tab)
    add(searchTabSheet);
    setFlexGrow(1, searchTabSheet);
  }

  public void updateSearchButton() {
    // Update both basic and advanced search buttons
    updateBasicSearchButton();
    updateAdvancedSearchButton();
  }

  private void updateBasicSearchButton() {
    String searchTerm = nameField != null ? nameField.getValue() : "";
    Set<LdapServerConfig> selectedEnvironments = parentTab != null ? 
      parentTab.getSelectedEnvironments() : new HashSet<>();
    if (searchButton != null) {
      searchButton.setEnabled(searchTerm != null && !searchTerm.trim().isEmpty() && !selectedEnvironments.isEmpty());
    }
  }

  private void updateAdvancedSearchButton() {
    Set<LdapServerConfig> selectedEnvironments = parentTab != null ? 
      parentTab.getSelectedEnvironments() : new HashSet<>();
    String filter = advancedSearchBuilder != null ? advancedSearchBuilder.getGeneratedFilter() : "";
    if (advancedSearchButton != null) {
      advancedSearchButton.setEnabled(!selectedEnvironments.isEmpty() && !filter.trim().isEmpty());
    }
  }

  private void performBasicSearch() {
    String searchTerm = nameField.getValue();
    SearchType searchType = typeComboBox.getValue();
    performSearch(searchTerm, searchType, null);
  }

  private void performAdvancedSearch() {
    // Get the manually edited filter or the generated one
    String customFilter = advancedSearchBuilder.getEditedFilter();
    String customSearchBase = advancedSearchBuilder.getSearchBase();
    
    performAdvancedSearchWithResults(customFilter, customSearchBase);
  }
  
  private void performAdvancedSearchWithResults(String customFilter, String customSearchBase) {
    Set<LdapServerConfig> selectedEnvironments = parentTab != null ? 
      parentTab.getSelectedEnvironments() : new HashSet<>();

    if (selectedEnvironments.isEmpty()) {
      showError("Please select at least one environment");
      return;
    }

    if (customFilter == null || customFilter.trim().isEmpty()) {
      showError("Please configure your search criteria");
      return;
    }

    try {
      List<SearchResultEntry> allResults = new ArrayList<>();
      String filter = customFilter.trim();

      // Search across all selected environments
      for (LdapServerConfig environment : selectedEnvironments) {
        // Ensure connection to each environment before searching
        try {
          if (!ldapService.isConnected(environment.getId())) {
            ldapService.connect(environment);
          }
        } catch (LDAPException ex) {
          String msg = "Failed to connect to environment '" + environment.getName() + "': " + ex.getMessage();
          System.err.println(msg);
          showError(msg);
          continue; // Skip this environment
        }

        String searchBase;
        
        if (customSearchBase != null && !customSearchBase.trim().isEmpty()) {
          searchBase = customSearchBase.trim();
        } else {
          searchBase = environment.getBaseDn();
        }

        try {
          // Use searchEntries method with correct parameters
          List<LdapEntry> environmentResults = ldapService.searchEntries(
            environment.getId(), searchBase, filter, SearchScope.SUB
          );

          // Convert to SearchResultEntry objects
          for (LdapEntry entry : environmentResults) {
            allResults.add(new SearchResultEntry(entry, environment));
          }
  } catch (LDAPException ex) {
          String errorMsg = "Search failed for environment '" + environment.getName() + 
            "': " + ex.getMessage();
          System.err.println(errorMsg);
          showError(errorMsg);
        }
      }

      // Use the existing displayResults method for main grid
      String searchDescription = "advanced search: " + filter;
      displayResults(allResults, searchDescription);

    } catch (Exception ex) {
      String errorMsg = "Search failed: " + ex.getMessage();
      showError(errorMsg);
      System.err.println(errorMsg);
      ex.printStackTrace();
    }
  }

  private void performSearch(String searchTerm, SearchType searchType, String customFilter) {
    performSearch(searchTerm, searchType, customFilter, null);
  }

  private void performSearch(String searchTerm, SearchType searchType, String customFilter, String customSearchBase) {
    Set<LdapServerConfig> selectedEnvironments = parentTab != null ? 
      parentTab.getSelectedEnvironments() : new HashSet<>();

    if (selectedEnvironments.isEmpty()) {
      showError("Please select at least one environment");
      return;
    }

    String filter;
    String searchDescription;

    // Determine if this is basic or advanced search
    if (customFilter != null && !customFilter.trim().isEmpty()) {
      // Advanced search
      filter = customFilter.trim();
      searchDescription = "custom filter: " + filter;
    } else {
      // Basic search
      if (searchTerm == null || searchTerm.trim().isEmpty()) {
        showError("Please enter a search term");
        return;
      }
      filter = buildSearchFilter(searchTerm.trim(), searchType);
      searchDescription = searchType.getLabel().toLowerCase() + " matching '" + searchTerm + "'";
    }

    try {
      List<SearchResultEntry> allResults = new ArrayList<>();

      // Search across all selected environments
      for (LdapServerConfig environment : selectedEnvironments) {
        // Ensure connection to each environment before searching
        try {
          if (!ldapService.isConnected(environment.getId())) {
            ldapService.connect(environment);
          }
        } catch (LDAPException ex) {
          showError("Failed to connect to environment " + environment.getName() + ": " + ex.getMessage());
          continue; // Skip this environment
        }

        String searchBase;
        
        // Determine search base
        if (customSearchBase != null && !customSearchBase.trim().isEmpty()) {
          searchBase = customSearchBase.trim();
        } else {
          searchBase = environment.getBaseDn();
        }
        
        if (searchBase == null || searchBase.trim().isEmpty()) {
          showError("No base DN configured for environment: " + environment.getName());
          continue;
        }

        try {
          // Perform the search
          List<LdapEntry> results = ldapService.searchEntries(
            environment.getId(),
            searchBase,
            filter,
            SearchScope.SUB // Search entire subtree
          );

          // Convert to SearchResultEntry objects
          for (LdapEntry entry : results) {
            allResults.add(new SearchResultEntry(entry, environment));
          }

        } catch (LDAPException e) {
          showError("Search failed for environment " + environment.getName() + ": " + e.getMessage());
        }
      }

      // Display results
      displayResults(allResults, searchDescription);

    } catch (Exception e) {
      showError("Search failed: " + e.getMessage());
    }
  }

private String buildSearchFilter(String searchTerm, SearchType searchType) {
  switch (searchType) {
    case USER:
    return String.format(searchType.getFilterTemplate(), searchTerm);
    case GROUP:
    return String.format(searchType.getFilterTemplate(), searchTerm);
    case ALL:
    // For "All" type, we need to substitute the search term twice
    return String.format(searchType.getFilterTemplate(), searchTerm, searchTerm);
    default:
    throw new IllegalArgumentException("Unknown search type: " + searchType);
  }
}

private void displayResults(List<SearchResultEntry> results, String searchDescription) {
  // If we have a search results callback, use it to display results in the dedicated tab
  if (searchResultsCallback != null) {
    searchResultsCallback.accept(results, searchDescription);
    
    // Still show notifications for user feedback
    if (results.isEmpty()) {
      showInfo("No entries found matching your search criteria");
    } else {
      showSuccess(String.format("Found %d %s", results.size(), results.size() == 1 ? "entry" : "entries"));
    }
    return;
  }

  // Fallback to local display if no callback is set (backward compatibility)
  this.allResults = new ArrayList<>(results);
  currentPage = 0;

  updateResultsDisplay();

  resultsGrid.setVisible(true);

  // Update result count
  String countText = String.format("Found %d %s for %s",
    results.size(),
    results.size() == 1 ? "entry" : "entries",
    searchDescription);
  resultCountLabel.setText(countText);
  resultCountLabel.setVisible(true);

  // Show pagination if more than one page
  paginationLayout.setVisible(results.size() > RESULTS_PER_PAGE);

  if (results.isEmpty()) {
    showInfo("No entries found matching your search criteria");
  } else {
    showSuccess(String.format("Found %d %s", results.size(), results.size() == 1 ? "entry" : "entries"));
  }
}

public void clear() {
  if (nameField != null) {
    nameField.clear();
  }
  if (typeComboBox != null) {
    typeComboBox.setValue(SearchType.ALL);
  }
  if (advancedSearchBuilder != null) {
    advancedSearchBuilder.clear();
  }
  if (resultsGrid != null) {
    resultsGrid.setItems(new ArrayList<>());
    resultsGrid.setVisible(false);
  }
  if (resultCountLabel != null) {
    resultCountLabel.setVisible(false);
  }
  if (paginationLayout != null) {
    paginationLayout.setVisible(false);
  }
  // Clear comparison selection
  selectedForComparison.clear();
  comparisonCheckboxes.clear();
  updateComparisonControls();
  allResults.clear();
  currentPage = 0;
  updateSearchButton();
}

public void refreshEnvironments() {
  // Environment dropdown is now handled by the parent tab
  // This method is kept for compatibility but does nothing
  updateSearchButton();
}

private void showSuccess(String message) {
  Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
  notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
}

private void showError(String message) {
  Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
  notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
}

private void showInfo(String message) {
  Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
  notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
}

private void updateResultsDisplay() {
  int totalPages = (int) Math.ceil((double) allResults.size() / RESULTS_PER_PAGE);
  int startIndex = currentPage * RESULTS_PER_PAGE;
  int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, allResults.size());

  List<SearchResultEntry> pageResults = allResults.subList(startIndex, endIndex);
  resultsGrid.setItems(pageResults);

  // Update pagination controls
  prevPageButton.setEnabled(currentPage > 0);
  nextPageButton.setEnabled(currentPage < totalPages - 1);

  if (totalPages > 1) {
    pageInfoLabel.setText(String.format("Page %d of %d (%d-%d of %d entries)",
    currentPage + 1, totalPages, startIndex + 1, endIndex, allResults.size()));
  } else {
  pageInfoLabel.setText("");
}

// Update comparison controls
updateComparisonControls();

// Update select all checkbox state
updateSelectAllCheckbox();
}

private void navigateToPage(int page) {
  int totalPages = (int) Math.ceil((double) allResults.size() / RESULTS_PER_PAGE);
  if (page >= 0 && page < totalPages) {
    currentPage = page;
    updateResultsDisplay();
  }
}

private void showEntryDetailsDialog(LdapEntry entry, LdapServerConfig environment) {
  Dialog dialog = new Dialog();
  dialog.setHeaderTitle("Entry Details - " + environment.getName());
  dialog.setWidth("800px");
  dialog.setHeight("600px");

  // Create an attribute editor for display-only mode
  AttributeEditor attributeEditor = new AttributeEditor(ldapService);
  attributeEditor.setServerConfig(environment);
  
  // Try to fetch complete entry with schema for optimized display
  try {
    LdapService.EntryWithSchema entryWithSchema = ldapService.getEntryWithSchema(environment.getId(), entry.getDn());
    if (entryWithSchema != null) {
      attributeEditor.editEntryWithSchema(entryWithSchema.getEntry(), entryWithSchema.getSchema());
    } else {
      attributeEditor.editEntry(entry);
    }
  } catch (Exception e) {
    // Fallback to regular method if optimized fetch fails
    attributeEditor.editEntry(entry);
  }

  // Make it read-only by disabling editing buttons
  attributeEditor.getChildren().forEach(component -> {
    if (component instanceof HorizontalLayout) {
      HorizontalLayout layout = (HorizontalLayout) component;
      layout.getChildren().forEach(child -> {
        if (child instanceof Button) {
          Button button = (Button) child;
          String text = button.getText();
          if (text != null && (text.contains("Save") || text.contains("Add") || text.contains("Delete"))) {
            button.setVisible(false);
          }
        }
      });
    }
  });

  dialog.add(attributeEditor);

  Button closeButton = new Button("Close", e -> dialog.close());
  closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  dialog.getFooter().add(closeButton);

  dialog.open();
}

/**
* Set the callback for when comparison is requested
*/
public void setComparisonCallback(Consumer<List<SearchResultEntry>> callback) {
  this.comparisonCallback = callback;
}

/**
* Set the callback for when search results are ready to be displayed
*/
public void setSearchResultsCallback(java.util.function.BiConsumer<List<SearchResultEntry>, String> callback) {
  this.searchResultsCallback = callback;
}

/**
* Update the comparison controls based on selected entries
*/
private void updateComparisonControls() {
  int selectedCount = selectedForComparison.size();
  comparisonCountLabel.setText(selectedCount + " entries selected for comparison");
  compareButton.setEnabled(selectedCount >= 2 && selectedCount <= 10);

  // Show/hide comparison controls based on whether grid is visible
  boolean showControls = resultsGrid.isVisible() && !allResults.isEmpty();
  comparisonCountLabel.getParent().ifPresent(parent ->
  ((HorizontalLayout) parent).setVisible(showControls));
}

/**
* Get the current page entries
*/
private List<SearchResultEntry> getCurrentPageEntries() {
  int startIndex = currentPage * RESULTS_PER_PAGE;
  int endIndex = Math.min(startIndex + RESULTS_PER_PAGE, allResults.size());
  return allResults.subList(startIndex, endIndex);
}

/**
* Update all checkboxes to reflect the current selection state
*/
private void updateAllCheckboxes() {
  comparisonCheckboxes.forEach((entry, checkbox) -> {
    checkbox.setValue(selectedForComparison.contains(entry));
  });
}

/**
* Update the select all checkbox based on current selection state
*/
private void updateSelectAllCheckbox() {
  List<SearchResultEntry> currentPageEntries = getCurrentPageEntries();
  if (currentPageEntries.isEmpty()) {
    selectAllCheckbox.setValue(false);
    selectAllCheckbox.setIndeterminate(false);
    return;
  }

  int selectedOnPage = 0;
  for (SearchResultEntry entry : currentPageEntries) {
    if (selectedForComparison.contains(entry)) {
      selectedOnPage++;
    }
  }

  if (selectedOnPage == 0) {
    selectAllCheckbox.setValue(false);
    selectAllCheckbox.setIndeterminate(false);
  } else if (selectedOnPage == currentPageEntries.size()) {
  selectAllCheckbox.setValue(true);
  selectAllCheckbox.setIndeterminate(false);
} else {
selectAllCheckbox.setValue(false);
selectAllCheckbox.setIndeterminate(true);
}
}

/**
* Perform comparison of selected entries
*/
private void performComparison() {
  if (selectedForComparison.size() < 2) {
    showError("Please select at least 2 entries for comparison");
    return;
  }

  if (selectedForComparison.size() > 10) {
    showError("You can only compare up to 10 entries at a time");
    return;
  }

  if (comparisonCallback != null) {
    List<SearchResultEntry> entriesToCompare = new ArrayList<>(selectedForComparison);
    comparisonCallback.accept(entriesToCompare);
  } else {
  showError("Comparison functionality is not available");
}
}
}