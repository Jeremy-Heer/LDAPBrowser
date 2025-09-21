package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * A custom field component for selecting Distinguished Names (DNs) from an LDAP directory.
 * Combines a text field for manual entry with a browse button that opens a tree browser dialog.
 */
public class DnSelectorField extends CustomField<String> {

    private final LdapService ldapService;
    private TextField textField;
    private Button browseButton;
    private LdapServerConfig serverConfig;
    private boolean allowManualEntry = true;
    private LdapTreeBrowser.FilterType filterType = LdapTreeBrowser.FilterType.ALL;

    /**
     * Create a DN selector field with default label
     *
     * @param ldapService The LDAP service to use
     */
    public DnSelectorField(LdapService ldapService) {
        this("Distinguished Name", ldapService);
    }

    /**
     * Create a DN selector field with custom label
     *
     * @param label The field label
     * @param ldapService The LDAP service to use
     */
    public DnSelectorField(String label, LdapService ldapService) {
        this.ldapService = ldapService;
        setLabel(label);
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        // Create text field for DN display/entry
        textField = new TextField();
        textField.setPlaceholder("e.g., ou=users,dc=example,dc=com");
        textField.setWidthFull();

        // Create browse button
        browseButton = new Button(new Icon(VaadinIcon.FOLDER_OPEN));
        browseButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        browseButton.setTooltipText("Browse for DN");
        browseButton.getStyle().set("color", "#4a90e2");
    }

    private void setupLayout() {
        // Create horizontal layout with text field and browse button
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.END);
        
        layout.add(textField, browseButton);
        layout.setFlexGrow(1, textField);
        
        // Add some spacing between text field and button
        browseButton.getStyle().set("margin-left", "var(--lumo-space-xs)");
        
        add(layout);
    }

    private void setupEventHandlers() {
        // Text field value change handler
        textField.addValueChangeListener(event -> {
            updateValue();
        });

        // Browse button click handler
        browseButton.addClickListener(event -> {
            openBrowserDialog();
        });

        // Disable text field if manual entry is not allowed
        textField.setEnabled(allowManualEntry);
    }

    private void openBrowserDialog() {
        if (serverConfig == null) {
            return;
        }

        // Create dialog
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Select Distinguished Name");
        dialog.setWidth("600px");
        dialog.setHeight("500px");
        dialog.setResizable(true);

        // Create tree browser in selector mode
        LdapTreeBrowser treeBrowser = new LdapTreeBrowser(ldapService, LdapTreeBrowser.Mode.SELECTOR);
        treeBrowser.setServerConfig(serverConfig);
        treeBrowser.setCompactMode(true);
        treeBrowser.setFilterType(filterType);
        treeBrowser.setSizeFull();

        // Load the tree
        treeBrowser.loadRootDSE();

        // Create buttons
        Button selectButton = new Button("Select", event -> {
            String selectedDn = treeBrowser.getSelectedDn();
            if (selectedDn != null && !selectedDn.trim().isEmpty()) {
                textField.setValue(selectedDn);
                updateValue();
                dialog.close();
            }
        });
        selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        selectButton.setEnabled(false); // Initially disabled

        Button cancelButton = new Button("Cancel", event -> dialog.close());

        // Enable select button when an entry is selected
        treeBrowser.addSelectionListener(selectionEvent -> {
            LdapEntry selectedEntry = selectionEvent.getSelectedEntry();
            boolean hasValidSelection = selectedEntry != null && 
                !selectedEntry.getDn().isEmpty() && 
                !"Root DSE".equals(selectedEntry.getRdn());
            selectButton.setEnabled(hasValidSelection);
        });

        // Add components to dialog
        Div content = new Div();
        content.setSizeFull();
        content.add(treeBrowser);
        
        dialog.add(content);
        dialog.getFooter().add(cancelButton, selectButton);

        dialog.open();
    }

    @Override
    protected String generateModelValue() {
        return textField.getValue();
    }

    @Override
    protected void setPresentationValue(String value) {
        textField.setValue(value != null ? value : "");
    }

    /**
     * Set the server configuration for LDAP operations
     *
     * @param serverConfig The server configuration
     */
    public void setServerConfig(LdapServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        // Enable/disable browse button based on server config
        browseButton.setEnabled(serverConfig != null);
    }

    /**
     * Set whether manual DN entry is allowed
     *
     * @param allowManualEntry True to allow manual entry
     */
    public void setAllowManualEntry(boolean allowManualEntry) {
        this.allowManualEntry = allowManualEntry;
        textField.setEnabled(allowManualEntry);
    }

    /**
     * Set the filter type for the tree browser
     *
     * @param filterType The filter type to use
     */
    public void setFilterType(LdapTreeBrowser.FilterType filterType) {
        this.filterType = filterType;
    }

    /**
     * Get whether manual entry is allowed
     *
     * @return True if manual entry is allowed
     */
    public boolean isAllowManualEntry() {
        return allowManualEntry;
    }

    /**
     * Get the current filter type
     *
     * @return The current filter type
     */
    public LdapTreeBrowser.FilterType getFilterType() {
        return filterType;
    }

    /**
     * Set a placeholder text for the text field
     *
     * @param placeholder The placeholder text
     */
    public void setPlaceholder(String placeholder) {
        textField.setPlaceholder(placeholder);
    }

    /**
     * Get the placeholder text
     *
     * @return The placeholder text
     */
    public String getPlaceholder() {
        return textField.getPlaceholder();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        textField.setReadOnly(readOnly);
        browseButton.setEnabled(!readOnly && serverConfig != null);
    }
}