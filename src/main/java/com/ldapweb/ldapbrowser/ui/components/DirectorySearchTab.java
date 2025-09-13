package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.model.SearchResultEntry;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.service.LoggingService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.ldapweb.ldapbrowser.util.RouteBasedServerSelection;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
* Directory Search tab containing Search and Entry Comparison sub-tabs.
*/
public class DirectorySearchTab extends VerticalLayout {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;
  private final LoggingService loggingService;
  // Optional supplier for environments (e.g., group search). If set, overrides selectionService.
  private java.util.function.Supplier<java.util.Set<LdapServerConfig>> environmentSupplier;

  // Environment dropdown
  // Removed: environment dropdown UI; selection is driven by drawer via ServerSelectionService

  // Sub-tabs
  private TabSheet tabSheet;
  private Tab searchTab;
  private Tab resultsTab;
  private Tab entryComparisonTab;

  // Components
  private DirectorySearchSubTab searchTabContent;
  private SearchResultsTab resultsTabContent;
  private EntryComparisonTab entryComparisonTabContent;

  /**
   * Constructs a new DirectorySearchTab.
   *
   * @param ldapService the LDAP service
   * @param configurationService the configuration service
   * @param inMemoryLdapService the in-memory LDAP service
   * @param selectionService the server selection service (deprecated, kept for compatibility)
   * @param loggingService the logging service
   */
  public DirectorySearchTab(LdapService ldapService, ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService, ServerSelectionService selectionService,
      LoggingService loggingService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;
    this.loggingService = loggingService;

    initializeComponents();
    setupLayout();
    // Note: No longer listening to ServerSelectionService for multi-tab friendliness
    // Search button state is updated via refreshEnvironments() when needed
  }

  private void initializeComponents() {
    // Create sub-tabs
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    // Search tab (existing functionality)
    searchTab = new Tab("Search");
    searchTabContent = new DirectorySearchSubTab(ldapService, configurationService,
        inMemoryLdapService, loggingService);

    // Set parent tab reference for environment dropdown access
    searchTabContent.setParentTab(this);

    tabSheet.add(searchTab, searchTabContent);

    // Results tab (new functionality for displaying search results)
    resultsTab = new Tab("Results");
    resultsTabContent = new SearchResultsTab(ldapService);

    // Set up comparison callback from results tab to switch to comparison tab
    resultsTabContent.setComparisonCallback(this::showComparison);

    tabSheet.add(resultsTab, resultsTabContent);

    // Entry Comparison tab (existing functionality)
    entryComparisonTab = new Tab("Entry Comparison");
    entryComparisonTabContent = new EntryComparisonTab();
    tabSheet.add(entryComparisonTab, entryComparisonTabContent);

    // Set up search result callback to switch to results tab
    searchTabContent.setSearchResultsCallback(this::showSearchResults);

    // Set Search as the default selected tab
    tabSheet.setSelectedTab(searchTab);
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("directory-search-tab");

    // Title with icon and environment dropdown
    HorizontalLayout titleLayout = new HorizontalLayout();
    titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    titleLayout.setSpacing(true);
    titleLayout.setWidthFull();

    // Left side: icon and title
    HorizontalLayout leftSide = new HorizontalLayout();
    leftSide.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    leftSide.setSpacing(true);

    Icon searchIcon = new Icon(VaadinIcon.SEARCH);
    searchIcon.setSize("20px");
    searchIcon.getStyle().set("color", "#2196f3");

    H3 title = new H3("Directory Search");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle().set("color", "#333");

    leftSide.add(searchIcon, title);

    // Right side removed (environment dropdown)

    titleLayout.add(leftSide);
    titleLayout.setFlexGrow(1, leftSide);

    add(titleLayout, tabSheet);
    setFlexGrow(1, tabSheet);
  }

  /**
  * Clear the content of all sub-tabs.
  */
  public void clear() {
    searchTabContent.clear();
    resultsTabContent.clear();
    entryComparisonTabContent.clear();
  }

  /**
  * Refresh environments in the dropdown and search tab.
  */
  public void refreshEnvironments() {
    searchTabContent.refreshEnvironments();
  }

  /**
  * Show search results and switch to results tab.
  */
  private void showSearchResults(List<SearchResultEntry> results, String searchDescription) {
    // Set the results in the results tab
    resultsTabContent.displayResults(results, searchDescription);

    // Switch to the results tab
    tabSheet.setSelectedTab(resultsTab);
  }

  /**
  * Show comparison with the provided entries and switch to comparison tab.
  */
  private void showComparison(List<SearchResultEntry> entries) {
    // Set the entries in the comparison tab
    entryComparisonTabContent.setComparisonEntries(entries);

    // Switch to the comparison tab
    tabSheet.setSelectedTab(entryComparisonTab);
  }

  /**
  * Get the selected environments from the route-based selection or environment supplier.
  * This method is multi-tab friendly as it derives selection from the current route.
  */
  public Set<LdapServerConfig> getSelectedEnvironments() {
    // If we have an explicit environment supplier (e.g., for group searches), use it
    if (environmentSupplier != null) {
      try {
        java.util.Set<LdapServerConfig> envs = environmentSupplier.get();
        return envs != null ? envs : java.util.Collections.emptySet();
      } catch (Exception e) {
        return java.util.Collections.emptySet();
      }
    }
    
    // For single-server routes, get server from current route
    return RouteBasedServerSelection.getCurrentServerFromRoute(configurationService, inMemoryLdapService)
        .map(Set::of)
        .orElse(Collections.emptySet());
  }

  /**
  * Override the environments provider. When set, searches will run against these environments
  * instead of the single selection from ServerSelectionService.
  */
  public void setEnvironmentSupplier(
      java.util.function.Supplier<java.util.Set<LdapServerConfig>> supplier) {
    this.environmentSupplier = supplier;
    // Trigger UI enable/disable updates
    searchTabContent.updateSearchButton();
  }
}