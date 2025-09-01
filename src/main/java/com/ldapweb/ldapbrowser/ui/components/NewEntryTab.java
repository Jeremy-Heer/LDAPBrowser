package com.ldapweb.ldapbrowser.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.unboundid.ldap.sdk.LDAPException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * New Entry tab for creating new LDAP entries
 */
public class NewEntryTab extends VerticalLayout {

  private final LdapService ldapService;
  private final DashboardTab dashboardTab;
  private LdapServerConfig serverConfig;

  // UI Components
  private TextField dnField;
  private ComboBox<String> templateComboBox;
  private Grid<AttributeRow> attributeGrid;
  private Button addRowButton;
  private Button createButton;
  private Button clearButton;
  private VerticalLayout entryLinkContainer;

  // Data
  private List<AttributeRow> attributeRows;

  public NewEntryTab(LdapService ldapService, DashboardTab dashboardTab) {
    this.ldapService = ldapService;
    this.dashboardTab = dashboardTab;
    this.attributeRows = new ArrayList<>();

    initializeComponents();
    setupLayout();

    // Add initial empty row
    addEmptyRow();
  }

  private void initializeComponents() {
    // DN field
    dnField = new TextField("Distinguished Name (DN)");
    dnField.setWidthFull();
    dnField.setPlaceholder("e.g., cn=John Doe,ou=people,dc=example,dc=com");
    dnField.setRequired(true);
    dnField.setRequiredIndicatorVisible(true);

    // Template dropdown
    templateComboBox = new ComboBox<>("Entry Template (Optional)");
    templateComboBox.setWidthFull();
    templateComboBox.setItems("None", "User", "Group", "Dynamic Group", "OU");
    templateComboBox.setValue("None");
    templateComboBox.setPlaceholder("Select a template to auto-populate attributes");
    templateComboBox.addValueChangeListener(e -> applyTemplate(e.getValue()));

    // Attribute grid
    attributeGrid = new Grid<>(AttributeRow.class, false);
    attributeGrid.setSizeFull();
    attributeGrid.setHeight("400px");

    // Configure grid columns
    attributeGrid.addColumn(new ComponentRenderer<>(this::createAttributeNameField))
        .setHeader("Attribute Name")
        .setFlexGrow(1);

    attributeGrid.addColumn(new ComponentRenderer<>(this::createAttributeValueField))
        .setHeader("Attribute Value")
        .setFlexGrow(2);

    attributeGrid.addColumn(new ComponentRenderer<>(this::createActionButtons))
        .setHeader("Actions")
        .setFlexGrow(0)
        .setWidth("100px");

    // Action buttons
    addRowButton = new Button("Add Row", new Icon(VaadinIcon.PLUS));
    addRowButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    addRowButton.addClickListener(e -> addEmptyRow());

    createButton = new Button("Create Entry", new Icon(VaadinIcon.CHECK));
    createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    createButton.addClickListener(e -> createEntry());

    clearButton = new Button("Clear All", new Icon(VaadinIcon.ERASER));
    clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    clearButton.addClickListener(e -> clearAll());

    // Entry link container (for showing links to created entries)
    entryLinkContainer = new VerticalLayout();
    entryLinkContainer.setPadding(false);
    entryLinkContainer.setSpacing(true);
    entryLinkContainer.setVisible(false); // Initially hidden
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("new-entry-tab");

    // Title with icon
    HorizontalLayout titleLayout = new HorizontalLayout();
    titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    titleLayout.setSpacing(true);

    Icon newEntryIcon = new Icon(VaadinIcon.PLUS_CIRCLE);
    newEntryIcon.setSize("20px");
    newEntryIcon.getStyle().set("color", "#4caf50");

    H3 title = new H3("Create New LDAP Entry");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle().set("color", "#333");

    titleLayout.add(newEntryIcon, title);

    // Info text
    Span infoText = new Span("Fill in the DN and attributes to create a new LDAP entry. " +
        "At minimum, you need to specify the objectClass attribute.");
    infoText.getStyle().set("color", "#666").set("font-style", "italic").set("margin-bottom", "16px");

    // Button layout
    HorizontalLayout buttonLayout = new HorizontalLayout();
    buttonLayout.setSpacing(true);
    buttonLayout.add(addRowButton, clearButton, createButton);

    add(titleLayout, infoText, dnField, templateComboBox, attributeGrid, buttonLayout, entryLinkContainer);
    setFlexGrow(1, attributeGrid);
  }

  private TextField createAttributeNameField(AttributeRow row) {
    TextField nameField = new TextField();
    nameField.setWidthFull();
    nameField.setPlaceholder("e.g., objectClass, cn, mail");
    nameField.setValue(row.getAttributeName() != null ? row.getAttributeName() : "");
    nameField.addValueChangeListener(e -> row.setAttributeName(e.getValue()));
    return nameField;
  }

  private TextField createAttributeValueField(AttributeRow row) {
    TextField valueField = new TextField();
    valueField.setWidthFull();
    valueField.setPlaceholder("Enter attribute value");
    valueField.setValue(row.getAttributeValue() != null ? row.getAttributeValue() : "");
    valueField.addValueChangeListener(e -> row.setAttributeValue(e.getValue()));
    return valueField;
  }

  private HorizontalLayout createActionButtons(AttributeRow row) {
    Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(e -> removeRow(row));
    deleteButton.getElement().setAttribute("title", "Remove row");

    HorizontalLayout layout = new HorizontalLayout(deleteButton);
    layout.setSpacing(false);
    layout.setPadding(false);

    return layout;
  }

  private void addEmptyRow() {
    AttributeRow newRow = new AttributeRow();
    attributeRows.add(newRow);
    refreshGrid();
  }

  private void removeRow(AttributeRow row) {
    attributeRows.remove(row);
    // Ensure at least one row remains
    if (attributeRows.isEmpty()) {
      addEmptyRow();
    } else {
      refreshGrid();
    }
  }

  private void refreshGrid() {
    attributeGrid.setItems(attributeRows);
    attributeGrid.getDataProvider().refreshAll();
  }

  private void createEntry() {
    if (serverConfig == null) {
      showError("Please connect to an LDAP server first");
      return;
    }

    String dn = dnField.getValue();
    if (dn == null || dn.trim().isEmpty()) {
      showError("Distinguished Name (DN) is required");
      return;
    }

    // Collect attributes
    Map<String, List<String>> attributes = new LinkedHashMap<>();
    boolean hasValidAttributes = false;

    for (AttributeRow row : attributeRows) {
      String name = row.getAttributeName();
      String value = row.getAttributeValue();

      if (name != null && !name.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
        name = name.trim();
        value = value.trim();

        // Add to attributes map (supporting multi-valued attributes)
        attributes.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        hasValidAttributes = true;
      }
    }

    if (!hasValidAttributes) {
      showError("At least one attribute with a valid name and value is required");
      return;
    }

    // Check if objectClass is specified
    if (!attributes.containsKey("objectClass")) {
      showError("The 'objectClass' attribute is required for LDAP entries");
      return;
    }

    try {
      // Create LdapEntry
      LdapEntry newEntry = new LdapEntry();
      newEntry.setDn(dn.trim());
      newEntry.setAttributes(attributes);

      // Add the entry using LdapService
      ldapService.addEntry(serverConfig.getId(), newEntry);

      showSuccess("Entry created successfully: " + dn);

      // Create link to the newly created entry
      createEntryLink(dn.trim());

    } catch (LDAPException e) {
      showError("Failed to create entry: " + e.getMessage());
    } catch (Exception e) {
      showError("Unexpected error: " + e.getMessage());
    }
  }

  private void applyTemplate(String template) {
    if (template == null || "None".equals(template)) {
      return;
    }

    // Clear existing attributes but keep non-empty user-entered ones
    List<AttributeRow> existingRows = new ArrayList<>();
    for (AttributeRow row : attributeRows) {
      if (row.getAttributeName() != null && !row.getAttributeName().trim().isEmpty() &&
          row.getAttributeValue() != null && !row.getAttributeValue().trim().isEmpty()) {
        existingRows.add(row);
      }
    }

    attributeRows.clear();
    attributeRows.addAll(existingRows);

    // Add template-specific attributes
    switch (template) {
      case "User":
        addTemplateAttribute("objectClass", "inetOrgPerson");
        addTemplateAttribute("cn", "");
        addTemplateAttribute("sn", "");
        addTemplateAttribute("uid", "");
        addTemplateAttribute("mail", "");
        break;

      case "Group":
        addTemplateAttribute("objectClass", "groupOfUniqueNames");
        addTemplateAttribute("cn", "");
        addTemplateAttribute("uniqueMember", "");
        break;

      case "Dynamic Group":
        addTemplateAttribute("objectClass", "groupOfUniqueNames");
        addTemplateAttribute("cn", "");
        addTemplateAttribute("uniqueMember", "");
        addTemplateAttribute("memberURL", "");
        break;

      case "OU":
        addTemplateAttribute("objectClass", "organizationalUnit");
        addTemplateAttribute("ou", "");
        break;
    }

    // Add an empty row at the end for additional attributes
    addEmptyRow();
    refreshGrid();
  }

  private void addTemplateAttribute(String name, String value) {
    // Check if attribute already exists to avoid duplicates
    boolean exists = attributeRows.stream()
        .anyMatch(row -> name.equals(row.getAttributeName()));

    if (!exists) {
      attributeRows.add(new AttributeRow(name, value));
    }
  }

  private void createEntryLink(String dn) {
    // Create a clickable link/span for the created entry
    HorizontalLayout linkLayout = new HorizontalLayout();
    linkLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    linkLayout.setSpacing(true);
    linkLayout.getStyle().set("margin-top", "10px");

    Icon linkIcon = new Icon(VaadinIcon.EXTERNAL_LINK);
    linkIcon.setSize("16px");
    linkIcon.getStyle().set("color", "#2196f3");

    Span linkText = new Span("Created entry: " + dn);
    linkText.getStyle()
        .set("color", "#2196f3")
        .set("cursor", "pointer")
        .set("text-decoration", "underline")
        .set("font-weight", "500");

    // Make the entire layout clickable
    linkLayout.getStyle().set("cursor", "pointer");
    linkLayout.addClickListener(e -> {
      // Navigate to the created entry in the Entry Details tab
      dashboardTab.showEntryDetails(dn);
    });

    linkLayout.add(linkIcon, linkText);

    // Add to the container and make it visible
    entryLinkContainer.add(linkLayout);
    entryLinkContainer.setVisible(true);
  }

  private void clearAll() {
    dnField.clear();
    templateComboBox.setValue("None");
    attributeRows.clear();
    addEmptyRow(); // Add one empty row

    // Clear entry links
    entryLinkContainer.removeAll();
    entryLinkContainer.setVisible(false);
  }

  public void setServerConfig(LdapServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  public void clear() {
    clearAll();
  }

  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 4000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showError(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  /**
   * Data class for attribute rows in the grid
   */
  public static class AttributeRow {
    private String attributeName;
    private String attributeValue;

    public AttributeRow() {
    }

    public AttributeRow(String attributeName, String attributeValue) {
      this.attributeName = attributeName;
      this.attributeValue = attributeValue;
    }

    public String getAttributeName() {
      return attributeName;
    }

    public void setAttributeName(String attributeName) {
      this.attributeName = attributeName;
    }

    public String getAttributeValue() {
      return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
      this.attributeValue = attributeValue;
    }
  }
}