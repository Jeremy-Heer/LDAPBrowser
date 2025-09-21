package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Reusable LDAP tree browser component that can operate in different modes
 * for both browsing and DN selection purposes.
 */
public class LdapTreeBrowser extends VerticalLayout {

    /**
     * Operating modes for the tree browser
     */
    public enum Mode {
        /** Full browser mode with all controls and functionality */
        BROWSER,
        /** Simplified selector mode focused on DN selection */
        SELECTOR
    }

    /**
     * Filter types for showing different types of entries
     */
    public enum FilterType {
        /** Show all entries */
        ALL,
        /** Show only container entries (organizationalUnit, container, etc.) */
        CONTAINERS_ONLY,
        /** Show only leaf entries (users, devices, etc.) */
        ENTRIES_ONLY
    }

    /**
     * Event fired when an entry is selected in the tree
     */
    public static class SelectionEvent extends ComponentEvent<LdapTreeBrowser> {
        private final LdapEntry selectedEntry;

        public SelectionEvent(LdapTreeBrowser source, LdapEntry selectedEntry, boolean fromClient) {
            super(source, fromClient);
            this.selectedEntry = selectedEntry;
        }

        public LdapEntry getSelectedEntry() {
            return selectedEntry;
        }

        public String getSelectedDn() {
            return selectedEntry != null ? selectedEntry.getDn() : null;
        }
    }

    private final LdapService ldapService;
    private final Mode mode;
    
    // Core components
    private LdapTreeGrid treeGrid;
    private HorizontalLayout headerLayout;
    private Button refreshButton;
    private Button settingsButton;
    private ContextMenu settingsContextMenu;
    private Checkbox includePrivateNamingContextsCheckbox;
    
    // Configuration
    private LdapServerConfig serverConfig;
    private FilterType filterType = FilterType.ALL;
    private boolean compactMode = false;
    private boolean showPrivateNamingContexts = false;

    /**
     * Create a new LDAP tree browser in BROWSER mode
     *
     * @param ldapService The LDAP service to use
     */
    public LdapTreeBrowser(LdapService ldapService) {
        this(ldapService, Mode.BROWSER);
    }

    /**
     * Create a new LDAP tree browser with specified mode
     *
     * @param ldapService The LDAP service to use
     * @param mode The operating mode
     */
    public LdapTreeBrowser(LdapService ldapService, Mode mode) {
        this.ldapService = ldapService;
        this.mode = mode;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // Create the tree grid
        treeGrid = new LdapTreeGrid(ldapService);
        
        // Apply tree styling
        treeGrid.addClassName("ldap-tree");
        treeGrid.getStyle().set("margin", "0px");
        treeGrid.getStyle().set("padding", "0px");
        treeGrid.getStyle().set("border-top", "none");

        // Initialize private naming contexts checkbox first (needed by header creation)
        includePrivateNamingContextsCheckbox = new Checkbox("Include private naming contexts");
        includePrivateNamingContextsCheckbox.setValue(showPrivateNamingContexts);
        includePrivateNamingContextsCheckbox.addValueChangeListener(event -> {
            showPrivateNamingContexts = event.getValue();
            refreshTree();
        });

        // Create header components based on mode
        if (mode == Mode.BROWSER) {
            createBrowserHeader();
        } else {
            createSelectorHeader();
        }
    }

    private void createBrowserHeader() {
        headerLayout = new HorizontalLayout();
        headerLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        headerLayout.setPadding(true);
        headerLayout.addClassName("ds-panel-header");
        headerLayout.getStyle().set("margin-bottom", "0px");

        Icon treeIcon = new Icon(VaadinIcon.TREE_TABLE);
        treeIcon.setSize("16px");
        treeIcon.getStyle().set("color", "#4a90e2");

        H3 browserTitle = new H3("LDAP Browser");
        browserTitle.addClassNames(LumoUtility.Margin.NONE);
        browserTitle.getStyle().set("font-size", "0.9em")
            .set("font-weight", "600")
            .set("color", "#333");

        // Add refresh button
        Icon refreshIcon = new Icon(VaadinIcon.REFRESH);
        refreshButton = new Button(refreshIcon);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        refreshButton.setTooltipText("Refresh LDAP Browser");
        refreshButton.getStyle().set("color", "#4a90e2");

        // Add settings button with cog icon
        Icon settingsIcon = new Icon(VaadinIcon.COG);
        settingsButton = new Button(settingsIcon);
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

        headerLayout.add(treeIcon, browserTitle, settingsButton, refreshButton);
        headerLayout.setFlexGrow(1, browserTitle);
    }

    private void createSelectorHeader() {
        headerLayout = new HorizontalLayout();
        headerLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        headerLayout.setPadding(true);
        headerLayout.addClassName("ds-panel-header");
        headerLayout.getStyle().set("margin-bottom", "0px");

        Icon treeIcon = new Icon(VaadinIcon.TREE_TABLE);
        treeIcon.setSize("14px");
        treeIcon.getStyle().set("color", "#4a90e2");

        H3 selectorTitle = new H3("Select Entry");
        selectorTitle.addClassNames(LumoUtility.Margin.NONE);
        selectorTitle.getStyle().set("font-size", "0.85em")
            .set("font-weight", "600")
            .set("color", "#333");

        // Compact refresh button
        Icon refreshIcon = new Icon(VaadinIcon.REFRESH);
        refreshButton = new Button(refreshIcon);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        refreshButton.setTooltipText("Refresh");
        refreshButton.getStyle().set("color", "#4a90e2");

        headerLayout.add(treeIcon, selectorTitle, refreshButton);
        headerLayout.setFlexGrow(1, selectorTitle);
    }

    private void setupLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("ds-panel");

        // Add header if present
        if (headerLayout != null) {
            add(headerLayout);
        }

        // Add tree grid
        add(treeGrid);
        setFlexGrow(1, treeGrid);
        
        // Remove gaps
        getStyle().set("gap", "0px");

        // Apply compact styling if requested
        if (compactMode) {
            applyCompactStyling();
        }
    }

    private void setupEventHandlers() {
        // Forward tree grid selection events
        treeGrid.asSingleSelect().addValueChangeListener(event -> {
            LdapEntry selectedEntry = event.getValue();
            
            // Skip placeholder and pagination entries
            if (selectedEntry != null && 
                !selectedEntry.getDn().startsWith("_placeholder_") &&
                selectedEntry.getAttributeValues("isPagination").isEmpty()) {
                
                // Fire selection event
                fireEvent(new SelectionEvent(this, selectedEntry, event.isFromClient()));
            }
        });

        // Setup refresh button handler
        if (refreshButton != null) {
            refreshButton.addClickListener(e -> refreshTree());
        }
    }

    /**
     * Add a selection listener for when entries are selected
     *
     * @param listener The listener to add
     * @return Registration for removing the listener
     */
    public Registration addSelectionListener(ComponentEventListener<SelectionEvent> listener) {
        return addListener(SelectionEvent.class, listener);
    }

    /**
     * Set the server configuration
     *
     * @param serverConfig The server configuration
     */
    public void setServerConfig(LdapServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        treeGrid.setServerConfig(serverConfig);
    }

    /**
     * Load the root DSE with naming contexts
     */
    public void loadRootDSE() {
        if (treeGrid != null) {
            try {
                treeGrid.loadRootDSEWithNamingContexts(showPrivateNamingContexts);
            } catch (Exception e) {
                // Error will be handled by the tree grid component
            }
        }
    }

    /**
     * Refresh the tree data
     */
    public void refreshTree() {
        if (serverConfig != null && treeGrid != null) {
            try {
                // Collapse all expanded entries before refreshing
                treeGrid.collapseAll();
                // Reload the tree data
                treeGrid.loadRootDSEWithNamingContexts(showPrivateNamingContexts);
            } catch (Exception e) {
                // Error will be handled by the tree grid component
            }
        }
    }

    /**
     * Clear the tree data
     */
    public void clear() {
        if (treeGrid != null) {
            treeGrid.clear();
        }
    }

    /**
     * Set whether to use compact mode styling
     *
     * @param compact True for compact mode
     */
    public void setCompactMode(boolean compact) {
        this.compactMode = compact;
        if (compact) {
            applyCompactStyling();
        } else {
            removeCompactStyling();
        }
    }

    /**
     * Set the filter type for entries to display
     *
     * @param filterType The filter type
     */
    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
        // TODO: Implement filtering in LdapTreeGrid if needed
    }

    /**
     * Set whether to show private naming contexts
     *
     * @param show True to show private naming contexts
     */
    public void setShowPrivateNamingContexts(boolean show) {
        this.showPrivateNamingContexts = show;
        if (includePrivateNamingContextsCheckbox != null) {
            includePrivateNamingContextsCheckbox.setValue(show);
        }
    }

    /**
     * Get the currently selected entry
     *
     * @return The selected entry or null
     */
    public LdapEntry getSelectedEntry() {
        return treeGrid != null ? treeGrid.asSingleSelect().getValue() : null;
    }

    /**
     * Get the currently selected DN
     *
     * @return The selected DN or null
     */
    public String getSelectedDn() {
        LdapEntry selected = getSelectedEntry();
        return selected != null ? selected.getDn() : null;
    }

    /**
     * Get the operating mode
     *
     * @return The current mode
     */
    public Mode getMode() {
        return mode;
    }

    private void applyCompactStyling() {
        // Reduce header size
        if (headerLayout != null) {
            headerLayout.getStyle().set("padding", "var(--lumo-space-xs)");
        }
        
        // Make tree grid more compact
        treeGrid.getStyle().set("font-size", "0.9em");
        
        // Reduce overall padding
        getStyle().set("padding", "0");
    }

    private void removeCompactStyling() {
        // Restore normal header size
        if (headerLayout != null) {
            headerLayout.getStyle().remove("padding");
        }
        
        // Restore normal tree grid font size
        treeGrid.getStyle().remove("font-size");
        
        // Restore normal padding
        getStyle().remove("padding");
    }
}