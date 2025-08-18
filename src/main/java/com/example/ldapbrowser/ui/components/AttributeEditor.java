package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.AttributeUsage;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* Component for editing LDAP entry attributes
*/
public class AttributeEditor extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private LdapEntry currentEntry;
  private LdapEntry fullEntry; // Cache of full entry with all attributes
  private Schema cachedSchema; // Cache schema to avoid multiple LDAP calls

  // Removed redundant titleLabel field
  private Span dnLabel;
  private Button copyDnButton;
  private HorizontalLayout entryTypeDisplay;
  private Checkbox showOperationalAttributesCheckbox;
  private Grid<AttributeRow> attributeGrid;
  private Button addAttributeButton;
  private Button saveButton;
  private Button refreshButton;
  private Button deleteEntryButton;
  private boolean hasPendingChanges = false;

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
    attributeGrid.addColumn(new ComponentRenderer<Span, AttributeRow>(this::createAttributeNameComponent))
    .setHeader("Attribute")
    .setFlexGrow(1)
    .setSortable(true)
    .setComparator(AttributeRow::getName);

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
    // Initialize pending changes state
    clearPendingChanges();
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

    // Action buttons with operational attributes checkbox on the right
    HorizontalLayout buttonLayout = new HorizontalLayout();
    buttonLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    buttonLayout.setPadding(false);
    buttonLayout.setSpacing(true);
    buttonLayout.add(addAttributeButton, saveButton, refreshButton, deleteEntryButton);
    
    // Add spacer and checkbox on the right
    Span spacer = new Span();
    buttonLayout.add(spacer, showOperationalAttributesCheckbox);
    buttonLayout.setFlexGrow(1, spacer);

    add(header, buttonLayout, attributeGrid);
    setFlexGrow(1, attributeGrid);
  }

  public void setServerConfig(LdapServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  public void editEntry(LdapEntry entry) {
    this.currentEntry = entry;
    this.fullEntry = entry; // Store the full entry
    this.cachedSchema = null; // Clear cached schema for new entry
    clearPendingChanges(); // Clear any pending changes when loading new entry

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
   * Optimized method to edit entry with pre-fetched schema information.
   * This version avoids multiple schema lookups during rendering.
   */
  public void editEntryWithSchema(LdapEntry entry, Schema schema) {
    this.currentEntry = entry;
    this.fullEntry = entry; // Store the full entry
    this.cachedSchema = schema; // Cache the schema to avoid repeated lookups
    clearPendingChanges(); // Clear any pending changes when loading new entry

    if (entry != null) {
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
    // Use schema-based detection for consistency with styling
    if (!showOperational && isOperationalAttributeComprehensive(attrName)) {
      continue;
    }

    rows.add(new AttributeRow(attrName, attr.getValue()));
  }

  // Sort attributes according to priority: objectClass > required > optional > operational
  sortAttributeRows(rows, showOperational);

  attributeGrid.setItems(rows);
}

/**
 * Sort attribute rows according to priority and within each category by name:
 * 1. objectClass attributes first (sorted by name)
 * 2. Required attributes second (sorted by name)
 * 3. Optional attributes third (sorted by name)
 * 4. Operational attributes last (sorted by name)
 */
private void sortAttributeRows(List<AttributeRow> rows, boolean showOperational) {
  rows.sort((row1, row2) -> {
    String attr1 = row1.getName();
    String attr2 = row2.getName();
    
    // Get attribute classifications
    int priority1 = getAttributeSortPriority(attr1, showOperational);
    int priority2 = getAttributeSortPriority(attr2, showOperational);
    
    // First sort by priority (lower number = higher priority)
    if (priority1 != priority2) {
      return Integer.compare(priority1, priority2);
    }
    
    // Within same priority group, sort alphabetically by attribute name
    return attr1.compareToIgnoreCase(attr2);
  });
}

/**
 * Get sorting priority for an attribute:
 * 1 = objectClass (highest priority)
 * 2 = required attributes
 * 3 = optional attributes  
 * 4 = operational attributes (lowest priority)
 * 5 = regular/unknown attributes
 */
private int getAttributeSortPriority(String attributeName, boolean showOperational) {
  String lowerName = attributeName.toLowerCase();
  
  // objectClass always comes first
  if ("objectclass".equals(lowerName)) {
    return 1;
  }
  
  // Check if operational attribute
  if (isOperationalAttribute(attributeName)) {
    return showOperational ? 4 : 5; // Only show if operational checkbox is checked
  }
  
  // Classify based on cached schema if available
  try {
    if (cachedSchema != null && currentEntry != null) {
      AttributeClassification classification = classifyAttribute(attributeName, cachedSchema);
      switch (classification) {
        case REQUIRED:
          return 2;
        case OPTIONAL:
          return 3;
        case OPERATIONAL:
          return showOperational ? 4 : 5;
        default:
          return 5; // Regular attributes
      }
    }
  } catch (Exception e) {
    // If schema classification fails, treat as regular attribute
  }
  
  return 5; // Default for regular/unknown attributes
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
  lowerName.contains("timestamp") ||
  // Additional common operational attributes
  lowerName.equals("entrydn") ||
  lowerName.equals("modifiersname") ||
  lowerName.equals("creatorsname") ||
  lowerName.equals("modifytimestamp") ||
  lowerName.equals("createtimestamp") ||
  lowerName.equals("contextcsn") ||
  lowerName.equals("numsubordinates") ||
  lowerName.equals("subordinatecount") ||
  lowerName.startsWith("operational") ||
  lowerName.startsWith("ds") ||
  lowerName.startsWith("ads-") ||
  lowerName.startsWith("ibm-") ||
  lowerName.startsWith("sun-") ||
  lowerName.startsWith("oracle-") ||
  lowerName.startsWith("microsoft-") ||
  lowerName.startsWith("novell-");
}

/**
 * Comprehensive check for operational attributes using both pattern matching and schema
 */
private boolean isOperationalAttributeComprehensive(String attributeName) {
  // First check using pattern-based detection
  if (isOperationalAttribute(attributeName)) {
    return true;
  }
  
  // Then check schema if available
  if (cachedSchema != null) {
    return isOperationalAttributeBySchema(attributeName, cachedSchema);
  }
  
  return false;
}

/**
 * Create a styled component for attribute name based on schema classification
 */
private Span createAttributeNameComponent(AttributeRow row) {
  Span nameSpan = new Span(row.getName());
  
  try {
    if (cachedSchema != null && currentEntry != null) {
      AttributeClassification classification = classifyAttribute(row.getName(), cachedSchema);
      
      switch (classification) {
        case REQUIRED:
          nameSpan.getStyle().set("color", "#d32f2f"); // Red for required (must)
          nameSpan.getElement().setAttribute("title", "Required attribute (must)");
          break;
        case OPTIONAL:
          nameSpan.getStyle().set("color", "#1976d2"); // Blue for optional (may)
          nameSpan.getElement().setAttribute("title", "Optional attribute (may)");
          break;
        case OPERATIONAL:
          if (showOperationalAttributesCheckbox.getValue()) {
            nameSpan.getStyle().set("color", "#f57c00"); // Orange for operational
            nameSpan.getElement().setAttribute("title", "Operational attribute");
          }
          break;
        case REGULAR:
        default:
          // Default color for regular attributes not defined in object classes
          break;
      }
    }
  } catch (Exception e) {
    // If schema classification fails, just display with default styling
  }
  
  return nameSpan;
}

/**
 * Classification types for attributes
 */
private enum AttributeClassification {
  REQUIRED,   // Must attributes from object classes
  OPTIONAL,   // May attributes from object classes
  OPERATIONAL, // Operational/system attributes
  REGULAR     // Regular attributes not classified by object class
}

/**
 * Classify an attribute based on schema information
 */
private AttributeClassification classifyAttribute(String attributeName, Schema schema) {
  // Check if it's operational first
  if (isOperationalAttributeBySchema(attributeName, schema)) {
    return AttributeClassification.OPERATIONAL;
  }
  
  // Get object classes for the current entry
  List<String> objectClasses = currentEntry.getAttributeValues("objectClass");
  if (objectClasses == null || objectClasses.isEmpty()) {
    return AttributeClassification.REGULAR;
  }
  
  Set<String> requiredAttributes = new HashSet<>();
  Set<String> optionalAttributes = new HashSet<>();
  
  // Collect required and optional attributes from all object classes
  for (String objectClassName : objectClasses) {
    ObjectClassDefinition objectClass = schema.getObjectClass(objectClassName);
    if (objectClass != null) {
      // Add required attributes
      String[] required = objectClass.getRequiredAttributes();
      if (required != null) {
        for (String attr : required) {
          requiredAttributes.add(attr.toLowerCase());
        }
      }
      
      // Add optional attributes
      String[] optional = objectClass.getOptionalAttributes();
      if (optional != null) {
        for (String attr : optional) {
          optionalAttributes.add(attr.toLowerCase());
        }
      }
    }
  }
  
  String lowerAttributeName = attributeName.toLowerCase();
  
  // Check if it's a required attribute
  if (requiredAttributes.contains(lowerAttributeName)) {
    return AttributeClassification.REQUIRED;
  }
  
  // Check if it's an optional attribute
  if (optionalAttributes.contains(lowerAttributeName)) {
    return AttributeClassification.OPTIONAL;
  }
  
  return AttributeClassification.REGULAR;
}

/**
 * Check if an attribute is operational based on schema information
 */
private boolean isOperationalAttributeBySchema(String attributeName, Schema schema) {
  // First check using our existing operational attribute detection
  if (isOperationalAttribute(attributeName)) {
    return true;
  }
  
  // Check schema for attribute type usage
  AttributeTypeDefinition attributeType = schema.getAttributeType(attributeName);
  if (attributeType != null) {
    AttributeUsage usage = attributeType.getUsage();
    return usage != null && usage == AttributeUsage.DIRECTORY_OPERATION;
  }
  
  return false;
}

/**
 * Get all available object classes from schema for type-ahead functionality
 */
private List<String> getAvailableObjectClasses() {
  List<String> objectClasses = new ArrayList<>();
  
  if (cachedSchema != null) {
    try {
      for (ObjectClassDefinition objectClass : cachedSchema.getObjectClasses()) {
        objectClasses.add(objectClass.getNameOrOID());
        
        // Also add alternative names if available
        String[] names = objectClass.getNames();
        if (names != null) {
          for (String name : names) {
            if (!objectClasses.contains(name)) {
              objectClasses.add(name);
            }
          }
        }
      }
      
      // Sort alphabetically for better user experience
      objectClasses.sort(String.CASE_INSENSITIVE_ORDER);
    } catch (Exception e) {
      // If schema access fails, return empty list
    }
  }
  
  return objectClasses;
}

/**
 * Get all valid attributes (required and optional) for current object classes
 */
private List<String> getValidAttributesForCurrentEntry() {
  Set<String> validAttributes = new HashSet<>();
  
  if (cachedSchema != null && currentEntry != null) {
    try {
      // Get object classes for the current entry
      List<String> objectClasses = currentEntry.getAttributeValues("objectClass");
      if (objectClasses != null) {
        for (String objectClassName : objectClasses) {
          ObjectClassDefinition objectClass = cachedSchema.getObjectClass(objectClassName);
          if (objectClass != null) {
            // Add required attributes
            String[] required = objectClass.getRequiredAttributes();
            if (required != null) {
              for (String attr : required) {
                validAttributes.add(attr);
              }
            }
            
            // Add optional attributes
            String[] optional = objectClass.getOptionalAttributes();
            if (optional != null) {
              for (String attr : optional) {
                validAttributes.add(attr);
              }
            }
          }
        }
      }
      
      // Also add commonly used attributes that might not be in schema
      validAttributes.add("cn");
      validAttributes.add("sn");
      validAttributes.add("givenName");
      validAttributes.add("mail");
      validAttributes.add("telephoneNumber");
      validAttributes.add("description");
      validAttributes.add("displayName");
      validAttributes.add("memberOf");
      validAttributes.add("member");
      validAttributes.add("uniqueMember");
      
    } catch (Exception e) {
      // If schema access fails, provide basic attributes
      validAttributes.add("cn");
      validAttributes.add("sn");
      validAttributes.add("description");
    }
  }
  
  // Convert to sorted list
  List<String> result = new ArrayList<>(validAttributes);
  result.sort(String.CASE_INSENSITIVE_ORDER);
  return result;
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
  cachedSchema = null; // Clear cached schema
  clearPendingChanges(); // Clear any pending changes
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

  /**
   * Mark that there are pending changes that need to be saved
   */
  private void markPendingChanges() {
    hasPendingChanges = true;
    updateSaveButtonAppearance();
  }

  /**
   * Clear pending changes indicator
   */
  private void clearPendingChanges() {
    hasPendingChanges = false;
    updateSaveButtonAppearance();
  }

  /**
   * Update save button appearance based on pending changes
   */
  private void updateSaveButtonAppearance() {
    if (hasPendingChanges) {
      saveButton.setText("Save Changes *");
      saveButton.getStyle().set("font-weight", "bold");
      saveButton.getStyle().set("background-color", "#ff6b35");
      saveButton.getStyle().set("color", "white");
      if (!saveButton.getThemeNames().contains(ButtonVariant.LUMO_PRIMARY.getVariantName())) {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      }
    } else {
      saveButton.setText("Save Changes");
      saveButton.getStyle().remove("font-weight");
      saveButton.getStyle().remove("background-color");
      saveButton.getStyle().remove("color");
      // Keep primary theme for consistency
    }
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

  // Use ComboBox with type-ahead for attribute name
  ComboBox<String> nameField = new ComboBox<>("Attribute Name");
  nameField.setWidthFull();
  nameField.setAllowCustomValue(true);
  nameField.setItems(getValidAttributesForCurrentEntry());
  nameField.setPlaceholder("Type to search attributes...");
  
  // Enable filtering as user types
  nameField.addCustomValueSetListener(event -> {
    String customValue = event.getDetail();
    nameField.setValue(customValue);
  });

  TextArea valueArea = new TextArea("Values (one per line)");
  valueArea.setWidthFull();
  valueArea.setHeight("200px");
  
  // Update valueArea and dialog title when attribute is selected
  nameField.addValueChangeListener(event -> {
    String selectedAttribute = event.getValue();
    if (selectedAttribute != null && !selectedAttribute.trim().isEmpty()) {
      List<String> existingValues = currentEntry.getAttributeValues(selectedAttribute.trim());
      if (existingValues != null && !existingValues.isEmpty()) {
        dialog.setHeaderTitle("Add Values to Existing Attribute: " + selectedAttribute);
        valueArea.setLabel("New values to add (one per line) - Existing: " + existingValues.size() + " value(s)");
        valueArea.setPlaceholder("Enter new values to add to the existing " + existingValues.size() + " value(s)...");
      } else {
        dialog.setHeaderTitle("Add New Attribute: " + selectedAttribute);
        valueArea.setLabel("Values (one per line)");
        valueArea.setPlaceholder("Enter values for the new attribute...");
      }
    } else {
      dialog.setHeaderTitle("Add Attribute");
      valueArea.setLabel("Values (one per line)");
      valueArea.setPlaceholder("");
    }
  });

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

    // Check if attribute already exists and append values instead of replacing
    String attributeName = name.trim();
    List<String> existingValues = currentEntry.getAttributeValues(attributeName);
    
    if (existingValues != null && !existingValues.isEmpty()) {
      // Attribute exists, merge with existing values
      List<String> mergedValues = new ArrayList<>(existingValues);
      
      // Add new values that don't already exist (case-insensitive comparison)
      for (String newValue : values) {
        boolean exists = mergedValues.stream()
          .anyMatch(existing -> existing.equalsIgnoreCase(newValue));
        if (!exists) {
          mergedValues.add(newValue);
        }
      }
      
      currentEntry.setAttributeValues(attributeName, mergedValues);
      markPendingChanges();
      showSuccess("Added " + (mergedValues.size() - existingValues.size()) + " new value(s) to existing attribute '" + attributeName + "'.");
    } else {
      // New attribute, set values directly
      currentEntry.setAttributeValues(attributeName, values);
      markPendingChanges();
      showSuccess("Added new attribute '" + attributeName + "' with " + values.size() + " value(s).");
    }

    // Refresh display without clearing pending changes
    refreshAttributeDisplay();

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
  // Special handling for objectClass attribute
  if ("objectClass".equalsIgnoreCase(row.getName())) {
    openEditObjectClassDialog(row);
    return;
  }
  
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
    markPendingChanges();
    
    // Refresh display without clearing pending changes
    refreshAttributeDisplay();

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

/**
 * Special dialog for editing objectClass attribute with type-ahead functionality
 */
private void openEditObjectClassDialog(AttributeRow row) {
  Dialog dialog = new Dialog();
  dialog.setHeaderTitle("Edit Object Classes");

  // Create a vertical layout for multiple object class selections
  VerticalLayout objectClassLayout = new VerticalLayout();
  objectClassLayout.setPadding(false);
  objectClassLayout.setSpacing(true);
  objectClassLayout.setWidthFull();

  // Current values as editable combo boxes
  List<ComboBox<String>> comboBoxes = new ArrayList<>();
  List<String> availableObjectClasses = getAvailableObjectClasses();
  
  // Add existing values
  for (String existingValue : row.getValues()) {
    ComboBox<String> objectClassCombo = createObjectClassComboBox(availableObjectClasses);
    objectClassCombo.setValue(existingValue);
    
    Button removeButton = new Button(new Icon(VaadinIcon.MINUS));
    removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    removeButton.addClickListener(e -> {
      objectClassLayout.remove(objectClassCombo.getParent().get());
      comboBoxes.remove(objectClassCombo);
    });
    
    HorizontalLayout rowLayout = new HorizontalLayout(objectClassCombo, removeButton);
    rowLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    rowLayout.setFlexGrow(1, objectClassCombo);
    
    objectClassLayout.add(rowLayout);
    comboBoxes.add(objectClassCombo);
  }
  
  // Add button for new object classes
  Button addObjectClassButton = new Button("Add Object Class", new Icon(VaadinIcon.PLUS));
  addObjectClassButton.addClickListener(e -> {
    ComboBox<String> newObjectClassCombo = createObjectClassComboBox(availableObjectClasses);
    
    Button removeButton = new Button(new Icon(VaadinIcon.MINUS));
    removeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    removeButton.addClickListener(removeEvent -> {
      objectClassLayout.remove(newObjectClassCombo.getParent().get());
      comboBoxes.remove(newObjectClassCombo);
    });
    
    HorizontalLayout rowLayout = new HorizontalLayout(newObjectClassCombo, removeButton);
    rowLayout.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
    rowLayout.setFlexGrow(1, newObjectClassCombo);
    
    // Insert before the add button
    objectClassLayout.addComponentAtIndex(objectClassLayout.getComponentCount() - 1, rowLayout);
    comboBoxes.add(newObjectClassCombo);
  });

  objectClassLayout.add(addObjectClassButton);

  Button saveButton = new Button("Save", e -> {
    List<String> newValues = new ArrayList<>();
    
    for (ComboBox<String> comboBox : comboBoxes) {
      String value = comboBox.getValue();
      if (value != null && !value.trim().isEmpty()) {
        newValues.add(value.trim());
      }
    }

    if (newValues.isEmpty()) {
      showError("At least one object class is required.");
      return;
    }

    // Remove duplicates and sort
    Set<String> uniqueValues = new HashSet<>(newValues);
    List<String> finalValues = new ArrayList<>(uniqueValues);
    finalValues.sort(String.CASE_INSENSITIVE_ORDER);

    // Update current entry
    currentEntry.setAttributeValues(row.getName(), finalValues);
    markPendingChanges();
    
    // Refresh display without clearing pending changes
    refreshAttributeDisplay();

    dialog.close();
  });
  saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

  Button cancelButton = new Button("Cancel", e -> dialog.close());

  VerticalLayout mainLayout = new VerticalLayout(objectClassLayout);
  mainLayout.setWidth("500px");
  mainLayout.setMaxHeight("400px");

  dialog.add(mainLayout);
  dialog.getFooter().add(cancelButton, saveButton);

  dialog.open();
}

/**
 * Create a ComboBox for object class selection with type-ahead
 */
private ComboBox<String> createObjectClassComboBox(List<String> availableObjectClasses) {
  ComboBox<String> comboBox = new ComboBox<>();
  comboBox.setWidthFull();
  comboBox.setAllowCustomValue(true);
  comboBox.setItems(availableObjectClasses);
  comboBox.setPlaceholder("Type to search object classes...");
  
  // Enable custom value input
  comboBox.addCustomValueSetListener(event -> {
    String customValue = event.getDetail();
    comboBox.setValue(customValue);
  });
  
  return comboBox;
}

private void deleteAttribute(AttributeRow row) {
  ConfirmDialog dialog = new ConfirmDialog();
  dialog.setHeader("Delete Attribute");
  dialog.setText("Are you sure you want to delete the attribute '" + row.getName() + "'?");
  dialog.setCancelable(true);
  dialog.setConfirmText("Delete");
  dialog.addConfirmListener(e -> {
    currentEntry.getAttributes().remove(row.getName());
    markPendingChanges();
    
    // Refresh display without clearing pending changes
    refreshAttributeDisplay();
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
    clearPendingChanges();
    showSuccess("Entry saved successfully.");
    
    // Automatically refresh the entry to sync with server state and prevent 
    // operational attribute conflicts on subsequent saves
    refreshEntry();

  } catch (LDAPException e) {
  showError("Failed to save entry: " + e.getMessage());
}
}

private List<Modification> createModifications(LdapEntry original, LdapEntry modified) {
  List<Modification> modifications = new ArrayList<>();

  // Find removed attributes (excluding operational attributes)
  for (String attrName : original.getAttributeNames()) {
    if (!modified.getAttributeNames().contains(attrName) && !isOperationalAttributeComprehensive(attrName)) {
      modifications.add(new Modification(ModificationType.DELETE, attrName));
    }
  }

  // Find added/modified attributes (excluding operational attributes)
  for (String attrName : modified.getAttributeNames()) {
    // Skip operational attributes - they are managed by the LDAP server
    if (isOperationalAttributeComprehensive(attrName)) {
      continue;
    }
    
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
    // Use getEntryWithSchema to preserve attribute formatting
    LdapService.EntryWithSchema entryWithSchema = ldapService.getEntryWithSchema(serverConfig.getId(), currentEntry.getDn());
    if (entryWithSchema != null) {
      editEntryWithSchema(entryWithSchema.getEntry(), entryWithSchema.getSchema());
      clearPendingChanges();
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
    
    // Sort objectClass values alphabetically
    if ("objectClass".equalsIgnoreCase(name)) {
      this.values.sort(String.CASE_INSENSITIVE_ORDER);
    }
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