package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Component for editing LDAP entry attributes
 */
public class AttributeEditor extends VerticalLayout {
    
    private final LdapService ldapService;
    private LdapServerConfig serverConfig;
    private LdapEntry currentEntry;
    private LdapEntry fullEntry; // Cache of full entry with all attributes
    
    // Removed redundant titleLabel field
    private Span dnLabel;
    private Button copyDnButton;
    private HorizontalLayout entryTypeDisplay;
    private Checkbox showOperationalAttributesCheckbox;
    private Grid<AttributeRow> attributeGrid;
    private Grid.Column<AttributeRow> attributeColumn;
    private Button addAttributeButton;
    private Button saveButton;
    private Button refreshButton;
    private Button deleteEntryButton;
    
    public AttributeEditor(LdapService ldapService) {
        this.ldapService = ldapService;
        
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        // Removed redundant titleLabel as it's already shown in DashboardTab header
        dnLabel = new Span();
        dnLabel.getStyle().set("font-family", "monospace");
        dnLabel.getStyle().set("word-break", "break-all");
        
        // Copy DN button
        copyDnButton = new Button(new Icon(VaadinIcon.COPY));
        copyDnButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        copyDnButton.getElement().setAttribute("title", "Copy DN to clipboard");
        copyDnButton.addClickListener(e -> copyDnToClipboard());
        copyDnButton.setEnabled(false); // Initially disabled
        
        // Initialize entry type display
        entryTypeDisplay = new HorizontalLayout();
        entryTypeDisplay.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        entryTypeDisplay.setPadding(false);
        entryTypeDisplay.setSpacing(true);
        entryTypeDisplay.getStyle().set("margin-bottom", "8px");
        
        // Operational attributes checkbox
        showOperationalAttributesCheckbox = new Checkbox("Show operational attributes");
        showOperationalAttributesCheckbox.setValue(false);
        showOperationalAttributesCheckbox.addValueChangeListener(e -> refreshAttributeDisplay());
        
        attributeGrid = new Grid<>(AttributeRow.class, false);
        attributeGrid.setSizeFull();
        
        // Configure attribute grid columns
        attributeColumn = attributeGrid.addColumn(AttributeRow::getName)
            .setHeader("Attribute")
            .setFlexGrow(1)
            .setSortable(true);
        
        attributeGrid.addColumn(new ComponentRenderer<>(this::createValueComponent))
            .setHeader("Values")
            .setFlexGrow(2);
        
        attributeGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
            .setHeader("Actions")
            .setFlexGrow(0)
            .setWidth("160px");
        
        addAttributeButton = new Button("Add Attribute", new Icon(VaadinIcon.PLUS));
        addAttributeButton.addClickListener(e -> openAddAttributeDialog());
        
        saveButton = new Button("Save Changes", new Icon(VaadinIcon.CHECK));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveChanges());
        
        refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
        refreshButton.addClickListener(e -> refreshEntry());
        
        deleteEntryButton = new Button("Delete Entry", new Icon(VaadinIcon.TRASH));
        deleteEntryButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteEntryButton.addClickListener(e -> confirmDeleteEntry());
        
        // Initially disable all buttons
        setButtonsEnabled(false);
    }
    
    private void setupLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(true);
        
        // Header with DN and copy button (removed redundant title)
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        
        // DN row with copy button
        HorizontalLayout dnRow = new HorizontalLayout();
        dnRow.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        dnRow.setPadding(false);
        dnRow.setSpacing(true);
        dnRow.add(dnLabel, copyDnButton);
        dnRow.setFlexGrow(1, dnLabel);
        
        header.add(entryTypeDisplay, dnRow);
        
        // Controls layout (operational attributes checkbox)
        HorizontalLayout controlsLayout = new HorizontalLayout();
        controlsLayout.setPadding(false);
        controlsLayout.setSpacing(true);
        controlsLayout.add(showOperationalAttributesCheckbox);
        
        // Action buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(addAttributeButton, saveButton, refreshButton, deleteEntryButton);
        
        add(header, controlsLayout, buttonLayout, attributeGrid);
        setFlexGrow(1, attributeGrid);
    }
    
    public void setServerConfig(LdapServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }
    
    public void editEntry(LdapEntry entry) {
        this.currentEntry = entry;
        this.fullEntry = entry; // Store the full entry
        
        if (entry != null) {
            // Removed redundant titleLabel.setText() call
            dnLabel.setText("DN: " + entry.getDn());
            
            // Update entry type display
            updateEntryTypeDisplay(entry);
            
            // Refresh attribute display based on operational attributes setting
            refreshAttributeDisplay();
            
            setButtonsEnabled(true);
            copyDnButton.setEnabled(true);
            showOperationalAttributesCheckbox.setEnabled(true);
        } else {
            clear();
        }
    }
    
    /**
     * Refresh the attribute display based on current settings
     */
    private void refreshAttributeDisplay() {
        if (fullEntry == null) {
            return;
        }
        
        List<AttributeRow> rows = new ArrayList<>();
        boolean showOperational = showOperationalAttributesCheckbox.getValue();
        
        for (Map.Entry<String, List<String>> attr : fullEntry.getAttributes().entrySet()) {
            String attrName = attr.getKey();
            
            // Filter operational attributes if checkbox is unchecked
            if (!showOperational && isOperationalAttribute(attrName)) {
                continue;
            }
            
            rows.add(new AttributeRow(attrName, attr.getValue()));
        }
        
        attributeGrid.setItems(rows);
        // Sort by attribute name by default
        attributeGrid.sort(List.of(new GridSortOrder<>(attributeColumn, SortDirection.ASCENDING)));
    }
    
    /**
     * Check if an attribute is operational (system-generated)
     */
    private boolean isOperationalAttribute(String attributeName) {
        String lowerName = attributeName.toLowerCase();
        return lowerName.startsWith("create") || 
               lowerName.startsWith("modify") || 
               lowerName.equals("entryuuid") ||
               lowerName.equals("entrydd") ||
               lowerName.equals("entrycsn") ||
               lowerName.equals("hassubordinates") ||
               lowerName.equals("subschemasubentry") ||
               lowerName.equals("pwdchangedtime") ||
               lowerName.equals("pwdaccountlockedtime") ||
               lowerName.equals("pwdfailuretime") ||
               lowerName.equals("structuralobjectclass") ||
               lowerName.startsWith("ds-") ||
               lowerName.startsWith("nsds-") ||
               lowerName.contains("timestamp");
    }
    
    /**
     * Update the entry type display with appropriate icon and description
     */
    private void updateEntryTypeDisplay(LdapEntry entry) {
        entryTypeDisplay.removeAll();
        
        Icon typeIcon = getEntryTypeIcon(entry);
        typeIcon.setSize("18px");
        
        String entryType = getEntryTypeDescription(entry);
        Span typeLabel = new Span(entryType);
        typeLabel.getStyle().set("font-weight", "bold").set("color", "#666");
        
        entryTypeDisplay.add(typeIcon, typeLabel);
    }
    
    /**
     * Get appropriate icon for entry type
     */
    private Icon getEntryTypeIcon(LdapEntry entry) {
        List<String> objectClasses = entry.getAttributeValues("objectClass");
        
        // First check for specific entry types regardless of container status
        if (objectClasses != null && !objectClasses.isEmpty()) {
            for (String objectClass : objectClasses) {
                String lowerClass = objectClass.toLowerCase();
                
                // User/Person entries (prioritize over container status)
                if (lowerClass.contains("person") || lowerClass.contains("user") || 
                    lowerClass.contains("inetorgperson") || lowerClass.contains("posixaccount")) {
                    Icon icon = new Icon(VaadinIcon.USER);
                    icon.getStyle().set("color", "#2196F3"); // Blue for users
                    return icon;
                }
                
                // Group entries
                else if (lowerClass.contains("group") || lowerClass.contains("groupofnames") || 
                         lowerClass.contains("posixgroup") || lowerClass.contains("groupofuniquenames")) {
                    Icon icon = new Icon(VaadinIcon.USERS);
                    icon.getStyle().set("color", "#4CAF50"); // Green for groups
                    return icon;
                }
                
                // Computer/Device entries
                else if (lowerClass.contains("computer") || lowerClass.contains("device")) {
                    Icon icon = new Icon(VaadinIcon.DESKTOP);
                    icon.getStyle().set("color", "#607D8B"); // Blue-gray for computers
                    return icon;
                }
                
                // Printer entries
                else if (lowerClass.contains("printer")) {
                    Icon icon = new Icon(VaadinIcon.PRINT);
                    icon.getStyle().set("color", "#795548"); // Brown for printers
                    return icon;
                }
                
                // Application/Service entries
                else if (lowerClass.contains("application") || lowerClass.contains("service")) {
                    Icon icon = new Icon(VaadinIcon.COG);
                    icon.getStyle().set("color", "#FF9800"); // Orange for services
                    return icon;
                }
            }
        }
        
        // If no specific entry type found, check for container types
        if (entry.isHasChildren() && objectClasses != null && !objectClasses.isEmpty()) {
            for (String objectClass : objectClasses) {
                String lowerClass = objectClass.toLowerCase();
                if (lowerClass.contains("organizationalunit") || lowerClass.contains("ou")) {
                    Icon icon = new Icon(VaadinIcon.FOLDER_OPEN);
                    icon.getStyle().set("color", "#FFA726"); // Orange for OUs
                    return icon;
                } else if (lowerClass.contains("container")) {
                    Icon icon = new Icon(VaadinIcon.FOLDER);
                    icon.getStyle().set("color", "#42A5F5"); // Blue for containers
                    return icon;
                } else if (lowerClass.contains("domain") || lowerClass.contains("dcobject")) {
                    Icon icon = new Icon(VaadinIcon.GLOBE);
                    icon.getStyle().set("color", "#66BB6A"); // Green for domains
                    return icon;
                }
            }
            // Unknown container
            Icon icon = new Icon(VaadinIcon.FOLDER);
            icon.getStyle().set("color", "#90A4AE"); // Gray for unknown containers
            return icon;
        }
        
        // Default for leaf entries with no specific type
        Icon icon = new Icon(VaadinIcon.FILE_TEXT);
        icon.getStyle().set("color", "#757575"); // Gray for unknown entries
        return icon;
    }
    
    /**
     * Get human-readable description of entry type
     */
    private String getEntryTypeDescription(LdapEntry entry) {
        List<String> objectClasses = entry.getAttributeValues("objectClass");
        
        if (objectClasses != null && !objectClasses.isEmpty()) {
            // Check all objectClass values for matches
            for (String objectClass : objectClasses) {
                String lowerClass = objectClass.toLowerCase();
                if (lowerClass.contains("organizationalunit")) {
                    return "Organizational Unit";
                } else if (lowerClass.contains("person") || lowerClass.contains("inetorgperson")) {
                    return "Person";
                } else if (lowerClass.contains("user") || lowerClass.contains("posixaccount")) {
                    return "User Account";
                } else if (lowerClass.contains("group") || lowerClass.contains("groupofnames") || 
                          lowerClass.contains("posixgroup") || lowerClass.contains("groupofuniquenames")) {
                    return "Group";
                } else if (lowerClass.contains("computer") || lowerClass.contains("device")) {
                    return "Computer/Device";
                } else if (lowerClass.contains("container")) {
                    return "Container";
                } else if (lowerClass.contains("domain") || lowerClass.contains("dcobject")) {
                    return "Domain Component";
                } else if (lowerClass.contains("printer")) {
                    return "Printer";
                } else if (lowerClass.contains("application") || lowerClass.contains("service")) {
                    return "Application/Service";
                } else if (lowerClass.contains("alias")) {
                    return "Alias";
                } else if (lowerClass.contains("certificate")) {
                    return "Certificate";
                }
            }
            // If no specific match found, return the most specific objectClass (usually the first one)
            return objectClasses.get(0);
        }
        
        return "LDAP Entry";
    }
    
    public void clear() {
        currentEntry = null;
        fullEntry = null;
        // Removed redundant titleLabel.setText() call
        dnLabel.setText("No entry selected");
        entryTypeDisplay.removeAll();
        attributeGrid.setItems(Collections.emptyList());
        setButtonsEnabled(false);
        copyDnButton.setEnabled(false);
        showOperationalAttributesCheckbox.setEnabled(false);
        showOperationalAttributesCheckbox.setValue(false);
    }
    
    private void setButtonsEnabled(boolean enabled) {
        addAttributeButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        deleteEntryButton.setEnabled(enabled);
    }
    
    private VerticalLayout createValueComponent(AttributeRow row) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        
        for (String value : row.getValues()) {
            Span valueSpan = new Span(value);
            valueSpan.getStyle().set("display", "block");
            valueSpan.getStyle().set("margin-bottom", "4px");
            if (value.length() > 50) {
                valueSpan.getStyle().set("font-size", "smaller");
            }
            layout.add(valueSpan);
        }
        
        return layout;
    }
    
    private HorizontalLayout createActionButtons(AttributeRow row) {
        Button editButton = new Button(new Icon(VaadinIcon.EDIT));
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editButton.addClickListener(e -> openEditAttributeDialog(row));
        editButton.getElement().setAttribute("title", "Edit attribute");
        
        Button copyButton = new Button(new Icon(VaadinIcon.COPY));
        copyButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        copyButton.getElement().setAttribute("title", "Copy options");
        
        // Create context menu for copy options
        ContextMenu copyMenu = new ContextMenu(copyButton);
        copyMenu.setOpenOnClick(true);
        copyMenu.addItem("Copy search filter", e -> copySearchFilter(row))
                .addComponentAsFirst(new Icon(VaadinIcon.SEARCH));
        copyMenu.addItem("Copy value(s)", e -> copyValues(row))
                .addComponentAsFirst(new Icon(VaadinIcon.CLIPBOARD_TEXT));
        copyMenu.addItem("Copy as LDIF", e -> copyAsLdif(row))
                .addComponentAsFirst(new Icon(VaadinIcon.FILE_TEXT));
        
        Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> deleteAttribute(row));
        deleteButton.getElement().setAttribute("title", "Delete attribute");
        
        HorizontalLayout layout = new HorizontalLayout(editButton, copyButton, deleteButton);
        layout.setSpacing(false);
        layout.setPadding(false);
        
        return layout;
    }
    
    private void openAddAttributeDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Attribute");
        
        TextField nameField = new TextField("Attribute Name");
        nameField.setWidthFull();
        
        TextArea valueArea = new TextArea("Values (one per line)");
        valueArea.setWidthFull();
        valueArea.setHeight("200px");
        
        Button saveButton = new Button("Add", e -> {
            String name = nameField.getValue();
            String valuesText = valueArea.getValue();
            
            if (name == null || name.trim().isEmpty()) {
                showError("Attribute name is required.");
                return;
            }
            
            if (valuesText == null || valuesText.trim().isEmpty()) {
                showError("At least one value is required.");
                return;
            }
            
            List<String> values = Arrays.asList(valuesText.split("\n"));
            values.replaceAll(String::trim);
            values.removeIf(String::isEmpty);
            
            if (values.isEmpty()) {
                showError("At least one value is required.");
                return;
            }
            
            // Add to current entry
            currentEntry.setAttributeValues(name.trim(), values);
            editEntry(currentEntry); // Refresh display
            
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        VerticalLayout layout = new VerticalLayout(nameField, valueArea);
        layout.setWidth("400px");
        
        dialog.add(layout);
        dialog.getFooter().add(cancelButton, saveButton);
        
        dialog.open();
    }
    
    private void openEditAttributeDialog(AttributeRow row) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Attribute: " + row.getName());
        
        TextArea valueArea = new TextArea("Values (one per line)");
        valueArea.setWidthFull();
        valueArea.setHeight("200px");
        valueArea.setValue(String.join("\n", row.getValues()));
        
        Button saveButton = new Button("Save", e -> {
            String valuesText = valueArea.getValue();
            
            if (valuesText == null || valuesText.trim().isEmpty()) {
                showError("At least one value is required.");
                return;
            }
            
            List<String> values = Arrays.asList(valuesText.split("\n"));
            values.replaceAll(String::trim);
            values.removeIf(String::isEmpty);
            
            if (values.isEmpty()) {
                showError("At least one value is required.");
                return;
            }
            
            // Update current entry
            currentEntry.setAttributeValues(row.getName(), values);
            editEntry(currentEntry); // Refresh display
            
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        VerticalLayout layout = new VerticalLayout(valueArea);
        layout.setWidth("400px");
        
        dialog.add(layout);
        dialog.getFooter().add(cancelButton, saveButton);
        
        dialog.open();
    }
    
    private void deleteAttribute(AttributeRow row) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Attribute");
        dialog.setText("Are you sure you want to delete the attribute '" + row.getName() + "'?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.addConfirmListener(e -> {
            currentEntry.getAttributes().remove(row.getName());
            editEntry(currentEntry); // Refresh display
        });
        dialog.open();
    }
    
    /**
     * Copy LDAP search filter for the attribute to clipboard
     */
    private void copySearchFilter(AttributeRow row) {
        // For multi-valued attributes, create filters for each value and combine them with OR
        List<String> values = row.getValues();
        String filter;
        
        if (values.size() == 1) {
            // Single value: (attributeName=value)
            filter = "(" + row.getName() + "=" + escapeFilterValue(values.get(0)) + ")";
        } else if (values.size() > 1) {
            // Multiple values: (|(attributeName=value1)(attributeName=value2)...)
            StringBuilder filterBuilder = new StringBuilder("(|");
            for (String value : values) {
                filterBuilder.append("(")
                    .append(row.getName())
                    .append("=")
                    .append(escapeFilterValue(value))
                    .append(")");
            }
            filterBuilder.append(")");
            filter = filterBuilder.toString();
        } else {
            // No values - shouldn't happen but handle gracefully
            filter = "(" + row.getName() + "=*)";
        }
        
        // Copy to clipboard using JavaScript
        getUI().ifPresent(ui -> 
            ui.getPage().executeJs("navigator.clipboard.writeText($0)", filter)
        );
        
        // Show notification
        String message = "LDAP search filter copied to clipboard: " + filter;
        showSuccess(message);
    }
    
    /**
     * Copy attribute values to clipboard
     */
    private void copyValues(AttributeRow row) {
        List<String> values = row.getValues();
        String valueText;
        
        if (values.size() == 1) {
            valueText = values.get(0);
        } else {
            valueText = String.join("\n", values);
        }
        
        // Copy to clipboard using JavaScript
        getUI().ifPresent(ui -> 
            ui.getPage().executeJs("navigator.clipboard.writeText($0)", valueText)
        );
        
        // Show notification
        String message = values.size() == 1 ? 
            "Attribute value copied to clipboard" : 
            "Attribute values copied to clipboard (" + values.size() + " values)";
        showSuccess(message);
    }
    
    /**
     * Copy attribute as LDIF format to clipboard
     */
    private void copyAsLdif(AttributeRow row) {
        StringBuilder ldif = new StringBuilder();
        String attrName = row.getName();
        
        for (String value : row.getValues()) {
            ldif.append(attrName).append(": ").append(value).append("\n");
        }
        
        // Copy to clipboard using JavaScript
        getUI().ifPresent(ui -> 
            ui.getPage().executeJs("navigator.clipboard.writeText($0)", ldif.toString())
        );
        
        // Show notification
        String message = "Attribute copied as LDIF to clipboard";
        showSuccess(message);
    }
    
    /**
     * Escape special characters in LDAP filter values according to RFC 4515
     */
    private String escapeFilterValue(String value) {
        if (value == null) {
            return "";
        }
        
        return value
            .replace("\\", "\\5c")  // Backslash must be first
            .replace("*", "\\2a")   // Asterisk
            .replace("(", "\\28")   // Left parenthesis
            .replace(")", "\\29")   // Right parenthesis
            .replace("\0", "\\00"); // Null character
    }
    
    /**
     * Copy DN to clipboard
     */
    private void copyDnToClipboard() {
        if (currentEntry == null || currentEntry.getDn() == null) {
            showInfo("No DN to copy.");
            return;
        }
        
        String dn = currentEntry.getDn();
        
        // Copy to clipboard using JavaScript
        getUI().ifPresent(ui -> 
            ui.getPage().executeJs("navigator.clipboard.writeText($0)", dn)
        );
        
        // Show notification
        String message = "DN copied to clipboard: " + dn;
        showSuccess(message);
    }
    
    private void saveChanges() {
        if (currentEntry == null || serverConfig == null) {
            return;
        }
        
        try {
            // For simplicity, we'll reload the original entry and create modifications
            // In a real application, you'd track individual changes
            LdapEntry originalEntry = ldapService.getEntry(serverConfig.getId(), currentEntry.getDn());
            
            if (originalEntry == null) {
                showError("Could not load original entry for comparison.");
                return;
            }
            
            List<Modification> modifications = createModifications(originalEntry, currentEntry);
            
            if (modifications.isEmpty()) {
                showInfo("No changes to save.");
                return;
            }
            
            ldapService.modifyEntry(serverConfig.getId(), currentEntry.getDn(), modifications);
            showSuccess("Entry saved successfully.");
            
        } catch (LDAPException e) {
            showError("Failed to save entry: " + e.getMessage());
        }
    }
    
    private List<Modification> createModifications(LdapEntry original, LdapEntry modified) {
        List<Modification> modifications = new ArrayList<>();
        
        // Find removed attributes
        for (String attrName : original.getAttributeNames()) {
            if (!modified.getAttributeNames().contains(attrName)) {
                modifications.add(new Modification(ModificationType.DELETE, attrName));
            }
        }
        
        // Find added/modified attributes
        for (String attrName : modified.getAttributeNames()) {
            List<String> originalValues = original.getAttributeValues(attrName);
            List<String> modifiedValues = modified.getAttributeValues(attrName);
            
            if (originalValues.isEmpty()) {
                // New attribute
                modifications.add(new Modification(ModificationType.ADD, attrName, 
                    modifiedValues.toArray(new String[0])));
            } else if (!originalValues.equals(modifiedValues)) {
                // Modified attribute
                modifications.add(new Modification(ModificationType.REPLACE, attrName, 
                    modifiedValues.toArray(new String[0])));
            }
        }
        
        return modifications;
    }
    
    private void refreshEntry() {
        if (currentEntry == null || serverConfig == null) {
            return;
        }
        
        try {
            LdapEntry refreshedEntry = ldapService.getEntry(serverConfig.getId(), currentEntry.getDn());
            if (refreshedEntry != null) {
                editEntry(refreshedEntry);
                showInfo("Entry refreshed.");
            } else {
                showError("Entry not found.");
                clear();
            }
        } catch (LDAPException e) {
            showError("Failed to refresh entry: " + e.getMessage());
        }
    }
    
    private void confirmDeleteEntry() {
        if (currentEntry == null) {
            return;
        }
        
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Entry");
        dialog.setText("Are you sure you want to delete this entry?\n\nDN: " + currentEntry.getDn());
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.addConfirmListener(e -> deleteEntry());
        dialog.open();
    }
    
    private void deleteEntry() {
        if (currentEntry == null || serverConfig == null) {
            return;
        }
        
        try {
            ldapService.deleteEntry(serverConfig.getId(), currentEntry.getDn());
            showSuccess("Entry deleted successfully.");
            clear();
        } catch (LDAPException e) {
            showError("Failed to delete entry: " + e.getMessage());
        }
    }
    
    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    private void showInfo(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }
    
    /**
     * Data class for attribute rows in the grid
     */
    public static class AttributeRow {
        private String name;
        private List<String> values;
        
        public AttributeRow(String name, List<String> values) {
            this.name = name;
            this.values = new ArrayList<>(values);
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<String> getValues() {
            return values;
        }
        
        public void setValues(List<String> values) {
            this.values = new ArrayList<>(values);
        }
    }
}
