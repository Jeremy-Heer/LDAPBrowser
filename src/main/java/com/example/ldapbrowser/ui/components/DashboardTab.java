package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.example.ldapbrowser.ui.components.SearchPanel.SearchResult;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

/**
 * Dashboard tab containing the LDAP browser, attribute editor, and search
 * functionality
 */
public class DashboardTab extends VerticalLayout {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private LdapServerConfig serverConfig;

  // Environment selection
  // Removed environment dropdown; selection driven by ServerSelectionService

  // Main content components
  private LdapTreeGrid treeGrid;
  private AttributeEditor attributeEditor;
  private SearchPanel searchPanel;
  private VerticalLayout searchResultsPanel;

  // Tabbed interface components
  private TabSheet tabSheet;
  private Tab entryDetailsTab;
  private NewEntryTab newEntryTab;

  // Private naming contexts option
  private Checkbox includePrivateNamingContextsCheckbox;
  private ContextMenu settingsContextMenu;

  public DashboardTab(LdapService ldapService, ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService, ServerSelectionService selectionService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    initializeComponents();
    setupLayout();
    selectionService.addListener(this::onEnvironmentSelected);
  }

  private void initializeComponents() {
    // Environment dropdown removed

    // Main content components
    treeGrid = new LdapTreeGrid(ldapService);
    treeGrid.asSingleSelect().addValueChangeListener(event -> {
      LdapEntry selectedEntry = event.getValue();
      onEntrySelected(selectedEntry);
    });

    attributeEditor = new AttributeEditor(ldapService);

    newEntryTab = new NewEntryTab(ldapService, this);

    searchPanel = new SearchPanel(ldapService);
    searchPanel.addSearchListener(this::onSearchResults);

    searchResultsPanel = new VerticalLayout();
    searchResultsPanel.setPadding(false);
    searchResultsPanel.setSpacing(false);

    // Initialize private naming contexts checkbox
    includePrivateNamingContextsCheckbox = new Checkbox("Include private naming contexts");
    includePrivateNamingContextsCheckbox.addValueChangeListener(event -> {
      // Reload the tree when the checkbox state changes
      if (treeGrid != null) {
        try {
          treeGrid.loadRootDSEWithNamingContexts(event.getValue());
        } catch (Exception e) {
          // Error will be handled by the tree grid component
        }
      }
    });
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(false);
    setSpacing(false);

    // Create left sidebar with LDAP browser
    VerticalLayout leftSidebar = new VerticalLayout();
    leftSidebar.setSizeFull();
    leftSidebar.setPadding(false);
    leftSidebar.setSpacing(false);
    leftSidebar.addClassName("ds-panel");

    // Left sidebar header with Directory Studio styling
    HorizontalLayout browserHeader = new HorizontalLayout();
    browserHeader.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    browserHeader.setPadding(true);
    browserHeader.addClassName("ds-panel-header");
    browserHeader.getStyle().set("margin-bottom", "0px");

    Icon treeIcon = new Icon(VaadinIcon.TREE_TABLE);
    treeIcon.setSize("16px");
    treeIcon.getStyle().set("color", "#4a90e2");

    H3 browserTitle = new H3("LDAP Browser");
    browserTitle.addClassNames(LumoUtility.Margin.NONE);
    browserTitle.getStyle().set("font-size", "0.9em").set("font-weight", "600").set("color", "#333");

    // Add refresh button
    Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    refreshButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    refreshButton.setTooltipText("Refresh LDAP Browser");
    refreshButton.addClickListener(e -> refreshLdapBrowser());
    refreshButton.getStyle().set("color", "#4a90e2");

    // Add settings button with cog icon
    Button settingsButton = new Button(new Icon(VaadinIcon.COG));
    settingsButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    settingsButton.setTooltipText("LDAP Browser Settings");
    settingsButton.getStyle().set("color", "#4a90e2");

    // Create context menu for settings
    settingsContextMenu = new ContextMenu(settingsButton);
    settingsContextMenu.setOpenOnClick(true);

    // Add the checkbox to the context menu
    VerticalLayout settingsContent = new VerticalLayout();
    settingsContent.setPadding(false);
    settingsContent.setSpacing(false);
    settingsContent.getStyle().set("padding", "var(--lumo-space-s)");
    settingsContent.add(includePrivateNamingContextsCheckbox);
    settingsContextMenu.add(settingsContent);

    browserHeader.add(treeIcon, browserTitle, settingsButton, refreshButton);
    browserHeader.setFlexGrow(1, browserTitle);

    // Apply tree styling and completely remove all spacing
    treeGrid.addClassName("ldap-tree");
    treeGrid.getStyle().set("margin", "0px");
    treeGrid.getStyle().set("padding", "0px");
    treeGrid.getStyle().set("border-top", "none");

    leftSidebar.add(browserHeader, treeGrid);
    leftSidebar.setFlexGrow(1, treeGrid);
    leftSidebar.getStyle().set("gap", "0px");
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    // Create horizontal layout for tabs and environment dropdown
    VerticalLayout rightPanel = new VerticalLayout();
    rightPanel.setSizeFull();
    rightPanel.setPadding(false);
    rightPanel.setSpacing(false);

    // Add tabsheet to right panel (no environment header)
    rightPanel.add(tabSheet);
    rightPanel.setFlexGrow(1, tabSheet);

    // Entry Details Tab
    entryDetailsTab = new Tab("Entry Details");
    VerticalLayout entryDetailsPanel = createEntryDetailsPanel();
    tabSheet.add(entryDetailsTab, entryDetailsPanel);

    // New Entry Tab
    Tab newEntryTabComponent = new Tab("New Entry");
    tabSheet.add(newEntryTabComponent, newEntryTab);

    // Search Tab
    Tab searchTab = new Tab("Search");
    VerticalLayout searchTabPanel = createSearchTabPanel();
    tabSheet.add(searchTab, searchTabPanel);

    // Set Entry Details as the default selected tab
    tabSheet.setSelectedTab(entryDetailsTab);

    // Create main split layout (horizontal)
    SplitLayout mainHorizontalSplit = new SplitLayout();
    mainHorizontalSplit.setSizeFull();
    mainHorizontalSplit.setSplitterPosition(25); // 25% for left sidebar
    mainHorizontalSplit.addToPrimary(leftSidebar);
    mainHorizontalSplit.addToSecondary(rightPanel);

    add(mainHorizontalSplit);
    setFlexGrow(1, mainHorizontalSplit);
  }

  private void onEnvironmentSelected(LdapServerConfig environment) {
    this.serverConfig = environment;

    // Update components with new environment
    if (environment != null) {
      treeGrid.setServerConfig(environment);
      attributeEditor.setServerConfig(environment);
      newEntryTab.setServerConfig(environment);
      searchPanel.setServerConfig(environment);

      // Auto-refresh the tree grid for the newly selected environment
      // Schedule the refresh to happen after potential connection establishment
      getUI().ifPresent(ui -> {
        // Use a small delay to allow the connection to establish
        ui.getElement().executeJs(
            "setTimeout(() => { $0.$server.loadRootDSEForNewEnvironment(); }, 300)",
            getElement());
      });
    } else {
      clear();
    }
  }

  /**
   * Public method callable from client-side JavaScript to load the tree for a new
   * environment
   */
  @ClientCallable
  public void loadRootDSEForNewEnvironment() {
    if (serverConfig != null) {
      loadRootDSEWithNamingContexts();
    }
  }

  /**
   * Create the Entry Details panel content (previously the center panel)
   */
  private VerticalLayout createEntryDetailsPanel() {
    VerticalLayout entryDetailsPanel = new VerticalLayout();
    entryDetailsPanel.setSizeFull();
    entryDetailsPanel.setPadding(false);
    entryDetailsPanel.setSpacing(false);
    entryDetailsPanel.addClassName("ds-panel");

    // Apply attribute editor styling and remove gaps
    attributeEditor.addClassName("attribute-grid");
    attributeEditor.getStyle().set("margin", "0").set("padding", "0");

    entryDetailsPanel.add(attributeEditor);
    entryDetailsPanel.setFlexGrow(1, attributeEditor);
    entryDetailsPanel.getStyle().set("gap", "0px"); // Remove any gap

    return entryDetailsPanel;
  }

  /**
   * Create the Search tab panel content (previously the bottom panel)
   */
  private VerticalLayout createSearchTabPanel() {
    VerticalLayout searchTabPanel = new VerticalLayout();
    searchTabPanel.setSizeFull();
    searchTabPanel.setPadding(false);
    searchTabPanel.setSpacing(false);
    searchTabPanel.addClassName("ds-panel");

    // Search panel at the top
    VerticalLayout searchContainer = new VerticalLayout();
    searchContainer.setWidthFull();
    searchContainer.setPadding(false);
    searchContainer.setSpacing(false);
    searchContainer.addClassName("ds-panel");

    HorizontalLayout searchHeader = new HorizontalLayout();
    searchHeader.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    searchHeader.setPadding(true);
    searchHeader.addClassName("ds-panel-header");

    Icon searchIcon = new Icon(VaadinIcon.SEARCH);
    searchIcon.setSize("14px");
    H3 searchTitle = new H3("Search");
    searchTitle.addClassNames(LumoUtility.Margin.NONE);
    searchTitle.getStyle().set("font-size", "0.9em").set("font-weight", "600").set("color", "#333");
    searchHeader.add(searchIcon, searchTitle);

    searchContainer.add(searchHeader, searchPanel);
    searchContainer.setFlexGrow(1, searchPanel);

    // Search results panel underneath
    VerticalLayout resultsContainer = new VerticalLayout();
    resultsContainer.setSizeFull();
    resultsContainer.setPadding(false);
    resultsContainer.setSpacing(false);
    resultsContainer.addClassName("ds-panel");

    HorizontalLayout resultsHeader = new HorizontalLayout();
    resultsHeader.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    resultsHeader.setPadding(true);
    resultsHeader.addClassName("ds-panel-header");

    Icon resultsIcon = new Icon(VaadinIcon.LIST_UL);
    resultsIcon.setSize("14px");
    H3 resultsTitle = new H3("Search Results");
    resultsTitle.addClassNames(LumoUtility.Margin.NONE);
    resultsTitle.getStyle().set("font-size", "0.9em").set("font-weight", "600").set("color", "#333");
    resultsHeader.add(resultsIcon, resultsTitle);

    searchResultsPanel.setSizeFull();
    searchResultsPanel.setPadding(true);
    searchResultsPanel.add(new Span("No search results"));

    resultsContainer.add(resultsHeader, searchResultsPanel);
    resultsContainer.setFlexGrow(1, searchResultsPanel);

    // Use vertical split layout for search panel (top) and results (bottom)
    SplitLayout searchVerticalSplit = new SplitLayout();
    searchVerticalSplit.setSizeFull();
    searchVerticalSplit.setOrientation(SplitLayout.Orientation.VERTICAL);
    searchVerticalSplit.setSplitterPosition(60);
    searchVerticalSplit.addToPrimary(searchContainer);
    searchVerticalSplit.addToSecondary(resultsContainer);

    searchTabPanel.add(searchVerticalSplit);
    searchTabPanel.setFlexGrow(1, searchVerticalSplit);

    return searchTabPanel;
  }

  private void onEntrySelected(LdapEntry entry) {
    // Don't display placeholder entries or pagination entries in the attribute
    // editor
    if (entry != null && !entry.getDn().startsWith("_placeholder_") &&
        entry.getAttributeValues("isPagination").isEmpty()) {
      // Fetch the complete entry with schema information in one optimized call
      try {
        LdapService.EntryWithSchema entryWithSchema = ldapService.getEntryWithSchema(serverConfig.getId(),
            entry.getDn());
        if (entryWithSchema != null) {
          attributeEditor.editEntryWithSchema(entryWithSchema.getEntry(), entryWithSchema.getSchema());
        } else {
          // Fallback to regular method if optimized call fails
          LdapEntry fullEntry = ldapService.getEntry(serverConfig.getId(), entry.getDn());
          if (fullEntry != null) {
            attributeEditor.editEntry(fullEntry);
          } else {
            attributeEditor.editEntry(entry); // Fallback to tree entry if fetch fails
          }
        }
      } catch (Exception e) {
        // Fallback to the entry from tree if optimized fetch fails
        try {
          LdapEntry fullEntry = ldapService.getEntry(serverConfig.getId(), entry.getDn());
          if (fullEntry != null) {
            attributeEditor.editEntry(fullEntry);
          } else {
            attributeEditor.editEntry(entry);
          }
        } catch (Exception fallbackException) {
          attributeEditor.editEntry(entry);
        }
      }
      // Set the entry DN as the Search Base DN in the search panel
      searchPanel.setBaseDn(entry.getDn());

      // Automatically switch to Entry Details tab when an entry is selected
      tabSheet.setSelectedTab(entryDetailsTab);
    } else {
      attributeEditor.clear();
    }
  }

  /**
   * Load an entry by DN and switch to the Entry Details tab
   * 
   * @param dn The distinguished name of the entry to load
   */
  public void showEntryDetails(String dn) {
    if (dn == null || dn.trim().isEmpty()) {
      return;
    }

    try {
      LdapEntry entry = ldapService.getEntry(serverConfig.getId(), dn);
      if (entry != null) {
        attributeEditor.editEntry(entry);
        // Switch to Entry Details tab
        tabSheet.setSelectedTab(entryDetailsTab);
      }
    } catch (Exception e) {
      // Handle error silently or show notification
      System.err.println("Failed to load entry: " + dn + ", Error: " + e.getMessage());
    }
  }

  private void onSearchResults(SearchResult searchResult) {
    // Clear previous results
    searchResultsPanel.removeAll();

    List<LdapEntry> results = searchResult.getEntries();
    List<String> requestedAttributes = searchResult.getRequestedAttributes();

    if (results.isEmpty()) {
      searchResultsPanel.add(new Span("No search results found"));
    } else {
      // Create a simple grid to display search results
      com.vaadin.flow.component.grid.Grid<LdapEntry> resultsGrid = new com.vaadin.flow.component.grid.Grid<>();
      resultsGrid.setSizeFull();
      resultsGrid.addClassName("search-results-grid");

      // Always show DN as the first column
      resultsGrid.addColumn(LdapEntry::getDn)
          .setHeader("Distinguished Name")
          .setFlexGrow(2)
          .setResizable(true)
          .setSortable(true)
          .setComparator((entry1, entry2) -> {
            String dn1 = entry1.getDn();
            String dn2 = entry2.getDn();
            if (dn1 == null && dn2 == null)
              return 0;
            if (dn1 == null)
              return -1;
            if (dn2 == null)
              return 1;
            return dn1.compareToIgnoreCase(dn2);
          });

      // Add columns for requested attributes if any were specified
      if (!requestedAttributes.isEmpty()) {
        for (String attribute : requestedAttributes) {
          resultsGrid.addColumn(entry -> {
            List<String> values = entry.getAttributeValues(attribute);
            if (values.isEmpty()) {
              return "";
            } else if (values.size() == 1) {
              return values.get(0);
            } else {
              // For multiple values, join with semicolon
              return String.join("; ", values);
            }
          }).setHeader(attribute)
              .setFlexGrow(1)
              .setResizable(true)
              .setSortable(true)
              .setComparator((entry1, entry2) -> {
                List<String> values1 = entry1.getAttributeValues(attribute);
                List<String> values2 = entry2.getAttributeValues(attribute);
                String value1 = values1.isEmpty() ? "" : values1.get(0);
                String value2 = values2.isEmpty() ? "" : values2.get(0);
                return value1.compareToIgnoreCase(value2);
              });
        }

        // Add info text about requested attributes
        Span infoText = new Span("Showing " + results.size() + " entries with requested attributes: " +
            String.join(", ", requestedAttributes));
        infoText.getStyle().set("font-style", "italic").set("color", "#666").set("margin-bottom", "8px");
        searchResultsPanel.add(infoText);
      } else {
        // If no specific attributes requested, show name and object class as before
        resultsGrid.addColumn(LdapEntry::getDisplayName)
            .setHeader("Name")
            .setFlexGrow(1)
            .setResizable(true)
            .setSortable(true)
            .setComparator((entry1, entry2) -> {
              String name1 = entry1.getDisplayName();
              String name2 = entry2.getDisplayName();
              if (name1 == null && name2 == null)
                return 0;
              if (name1 == null)
                return -1;
              if (name2 == null)
                return 1;
              return name1.compareToIgnoreCase(name2);
            });

        resultsGrid.addColumn(entry -> {
          String objectClass = entry.getFirstAttributeValue("objectClass");
          return objectClass != null ? objectClass : "";
        }).setHeader("Object Class")
            .setFlexGrow(1)
            .setResizable(true)
            .setSortable(true)
            .setComparator((entry1, entry2) -> {
              String objectClass1 = entry1.getFirstAttributeValue("objectClass");
              String objectClass2 = entry2.getFirstAttributeValue("objectClass");
              if (objectClass1 == null)
                objectClass1 = "";
              if (objectClass2 == null)
                objectClass2 = "";
              return objectClass1.compareToIgnoreCase(objectClass2);
            });

        // Add info text about all attributes
        Span infoText = new Span("Showing " + results.size() + " entries with all attributes");
        infoText.getStyle().set("font-style", "italic").set("color", "#666").set("margin-bottom", "8px");
        searchResultsPanel.add(infoText);
      }

      resultsGrid.setItems(results);

      // Add selection listener to show entry details
      resultsGrid.asSingleSelect().addValueChangeListener(event -> {
        LdapEntry selectedEntry = event.getValue();
        if (selectedEntry != null) {
          onEntrySelected(selectedEntry);
        }
      });

      searchResultsPanel.add(resultsGrid);
      searchResultsPanel.setFlexGrow(1, resultsGrid);
    }
  }

  public void setServerConfig(LdapServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    treeGrid.setServerConfig(serverConfig);
    attributeEditor.setServerConfig(serverConfig);
    newEntryTab.setServerConfig(serverConfig);
    searchPanel.setServerConfig(serverConfig);
  }

  public void loadRootDSEWithNamingContexts() {
    try {
      treeGrid.loadRootDSEWithNamingContexts(includePrivateNamingContextsCheckbox.getValue());
    } catch (Exception e) {
      // Error will be handled by the tree grid component
    }
  }

  public void clear() {
    treeGrid.clear();
    attributeEditor.clear();
    newEntryTab.clear();
    searchResultsPanel.removeAll();
    searchResultsPanel.add(new Span("No search results"));
  }

  public void refreshEnvironments() {
    // No environment dropdown; server selection is handled via
    // ServerSelectionService listeners
  }

  /**
   * Refresh the LDAP browser tree
   */
  private void refreshLdapBrowser() {
    if (serverConfig != null && treeGrid != null) {
      try {
        // Collapse all expanded entries before refreshing
        treeGrid.collapseAll();

        // Reload the tree data
        treeGrid.loadRootDSEWithNamingContexts(includePrivateNamingContextsCheckbox.getValue());
      } catch (Exception e) {
        // Error will be handled by the tree grid component
      }
    }
  }
}