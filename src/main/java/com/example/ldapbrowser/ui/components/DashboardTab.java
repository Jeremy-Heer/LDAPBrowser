package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.ui.components.SearchPanel.SearchResult;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

/**
 * Dashboard tab containing the LDAP browser, attribute editor, and search functionality
 */
public class DashboardTab extends VerticalLayout {
    
    private final LdapService ldapService;
    
    // Main content components
    private LdapTreeGrid treeGrid;
    private AttributeEditor attributeEditor;
    private SearchPanel searchPanel;
    private VerticalLayout searchResultsPanel;
    
    // Tabbed interface components
    private TabSheet tabSheet;
    private Tab entryDetailsTab;
    
    // Server configuration (removed unused field)
    
    public DashboardTab(LdapService ldapService) {
        this.ldapService = ldapService;
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        // Main content components
        treeGrid = new LdapTreeGrid(ldapService);
        treeGrid.asSingleSelect().addValueChangeListener(event -> {
            LdapEntry selectedEntry = event.getValue();
            onEntrySelected(selectedEntry);
        });
        
        attributeEditor = new AttributeEditor(ldapService);
        
        searchPanel = new SearchPanel(ldapService);
        searchPanel.addSearchListener(this::onSearchResults);
        
        searchResultsPanel = new VerticalLayout();
        searchResultsPanel.setPadding(false);
        searchResultsPanel.setSpacing(false);
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
        
        browserHeader.add(treeIcon, browserTitle);
        browserHeader.setFlexGrow(1, browserTitle);
        
        // Apply tree styling and completely remove all spacing
        treeGrid.addClassName("ldap-tree");
        treeGrid.getStyle().set("margin", "0px");
        treeGrid.getStyle().set("padding", "0px");
        treeGrid.getStyle().set("border-top", "none");
        
        leftSidebar.add(browserHeader, treeGrid);
        leftSidebar.setFlexGrow(1, treeGrid);
        leftSidebar.getStyle().set("gap", "0px"); // Remove any gap between components
        
        // Create tabbed interface for Entry Details and Search
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        
        // Entry Details Tab
        entryDetailsTab = new Tab("Entry Details");
        VerticalLayout entryDetailsPanel = createEntryDetailsPanel();
        tabSheet.add(entryDetailsTab, entryDetailsPanel);
        
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
        mainHorizontalSplit.addToSecondary(tabSheet);
        
        add(mainHorizontalSplit);
        setFlexGrow(1, mainHorizontalSplit);
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
        searchVerticalSplit.setSplitterPosition(60); // 60% for search panel, 40% for results to ensure all search controls are visible
        searchVerticalSplit.addToPrimary(searchContainer);
        searchVerticalSplit.addToSecondary(resultsContainer);
        
        searchTabPanel.add(searchVerticalSplit);
        searchTabPanel.setFlexGrow(1, searchVerticalSplit);
        
        return searchTabPanel;
    }
    
    private void onEntrySelected(LdapEntry entry) {
        // Don't display placeholder entries in the attribute editor
        if (entry != null && !entry.getDn().startsWith("_placeholder_")) {
            attributeEditor.editEntry(entry);
            // Automatically switch to Entry Details tab when an entry is selected
            tabSheet.setSelectedTab(entryDetailsTab);
        } else {
            attributeEditor.clear();
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
            com.vaadin.flow.component.grid.Grid<LdapEntry> resultsGrid = 
                new com.vaadin.flow.component.grid.Grid<>();
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
                    if (dn1 == null && dn2 == null) return 0;
                    if (dn1 == null) return -1;
                    if (dn2 == null) return 1;
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
                        if (name1 == null && name2 == null) return 0;
                        if (name1 == null) return -1;
                        if (name2 == null) return 1;
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
                      if (objectClass1 == null) objectClass1 = "";
                      if (objectClass2 == null) objectClass2 = "";
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
        treeGrid.setServerConfig(serverConfig);
        attributeEditor.setServerConfig(serverConfig);
        searchPanel.setServerConfig(serverConfig);
    }
    
    public void loadRootDSEWithNamingContexts() {
        try {
            treeGrid.loadRootDSEWithNamingContexts();
        } catch (Exception e) {
            // Error will be handled by the tree grid component
        }
    }
    
    public void clear() {
        treeGrid.clear();
        attributeEditor.clear();
        searchResultsPanel.removeAll();
        searchResultsPanel.add(new Span("No search results"));
    }
}
