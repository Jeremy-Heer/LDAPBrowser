package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.model.SearchResultEntry;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Directory Search tab for searching users, groups, and all entries across multiple environments
 */
public class DirectorySearchTab extends VerticalLayout {
    
    private final LdapService ldapService;
    private final ConfigurationService configurationService;
    private final InMemoryLdapService inMemoryLdapService;
    
    // Environment selection
    private EnvironmentDropdown environmentDropdown;
    
    // Search components
    private TextField nameField;
    private ComboBox<SearchType> typeComboBox;
    private Button searchButton;
    
    // Results components
    private Grid<SearchResultEntry> resultsGrid;
    private Span resultCountLabel;
    private VerticalLayout resultsContainer;
    private HorizontalLayout paginationLayout;
    
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
    
    public DirectorySearchTab(LdapService ldapService, ConfigurationService configurationService, 
                              InMemoryLdapService inMemoryLdapService) {
        this.ldapService = ldapService;
        this.configurationService = configurationService;
        this.inMemoryLdapService = inMemoryLdapService;
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        // Environment dropdown for multi-select
        environmentDropdown = new EnvironmentDropdown(ldapService, configurationService, inMemoryLdapService, true);
        
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
        searchButton.addClickListener(e -> performSearch());
        
        // Results grid
        resultsGrid = new Grid<>(SearchResultEntry.class, false);
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
        }).setHeader("Type").setWidth("60px").setFlexGrow(0);
        
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
        
        // Results container
        resultsContainer = new VerticalLayout();
        resultsContainer.setPadding(false);
        resultsContainer.setSpacing(true);
        resultsContainer.add(resultCountLabel, resultsGrid, paginationLayout);
        resultsContainer.setFlexGrow(1, resultsGrid);
        resultsContainer.setSizeFull();
    }
    
    private void setupLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("directory-search-tab");
        
        // Title with icon
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        titleLayout.setSpacing(true);
        
        Icon searchIcon = new Icon(VaadinIcon.SEARCH);
        searchIcon.setSize("20px");
        searchIcon.getStyle().set("color", "#2196f3");
        
        H3 title = new H3("Directory Search");
        title.addClassNames(LumoUtility.Margin.NONE);
        title.getStyle().set("color", "#333");
        
        titleLayout.add(searchIcon, title);
        
        // Environment dropdown
        HorizontalLayout environmentLayout = new HorizontalLayout();
        environmentLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.END);
        environmentLayout.setSpacing(true);
        
        // Add server icon to environment section
        Icon serverIcon = new Icon(VaadinIcon.SERVER);
        serverIcon.setSize("16px");
        serverIcon.getStyle().set("color", "#666");
        serverIcon.getStyle().set("margin-top", "26px"); // Align with combo box
        
        environmentLayout.add(serverIcon, environmentDropdown.getMultiSelectComponent());
        
        // Search form
        HorizontalLayout searchForm = new HorizontalLayout();
        searchForm.setDefaultVerticalComponentAlignment(Alignment.END);
        searchForm.setSpacing(true);
        searchForm.setWidthFull();
        
        // Create a container for the form fields to control their flex behavior
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
        
        // Add components to layout
        add(titleLayout, environmentLayout, searchForm, new Hr(), resultsContainer);
        setFlexGrow(1, resultsContainer);
    }
    
    private void updateSearchButton() {
        String searchTerm = nameField.getValue();
        Set<LdapServerConfig> selectedEnvironments = environmentDropdown.getSelectedEnvironments();
        searchButton.setEnabled(searchTerm != null && !searchTerm.trim().isEmpty() && !selectedEnvironments.isEmpty());
    }
    
    private void performSearch() {
        String searchTerm = nameField.getValue();
        SearchType searchType = typeComboBox.getValue();
        Set<LdapServerConfig> selectedEnvironments = environmentDropdown.getSelectedEnvironments();
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            showError("Please enter a search term");
            return;
        }
        
        if (selectedEnvironments.isEmpty()) {
            showError("Please select at least one environment");
            return;
        }
        
        try {
            // Build the search filter based on the selected type
            String filter = buildSearchFilter(searchTerm.trim(), searchType);
            
            List<SearchResultEntry> allResults = new ArrayList<>();
            
            // Search across all selected environments
            for (LdapServerConfig environment : selectedEnvironments) {
                if (environment.getBaseDn() == null || environment.getBaseDn().trim().isEmpty()) {
                    showError("No base DN configured for environment: " + environment.getName());
                    continue;
                }
                
                try {
                    // Perform the search
                    List<LdapEntry> results = ldapService.searchEntries(
                        environment.getId(),
                        environment.getBaseDn(),
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
            displayResults(allResults, searchTerm, searchType);
            
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
    
    private void displayResults(List<SearchResultEntry> results, String searchTerm, SearchType searchType) {
        this.allResults = new ArrayList<>(results);
        currentPage = 0;
        
        updateResultsDisplay();
        
        resultsGrid.setVisible(true);
        
        // Update result count
        String countText = String.format("Found %d %s matching '%s'", 
            results.size(), 
            results.size() == 1 ? "entry" : "entries",
            searchTerm);
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
        allResults.clear();
        currentPage = 0;
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
        attributeEditor.editEntry(entry);
        
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
    
    public void refreshEnvironments() {
        if (environmentDropdown != null) {
            environmentDropdown.refreshEnvironments();
        }
    }
}
