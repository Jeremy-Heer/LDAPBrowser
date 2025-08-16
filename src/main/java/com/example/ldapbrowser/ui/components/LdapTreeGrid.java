package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.unboundid.ldap.sdk.LDAPException;

import java.util.List;

/**
* Tree grid component for browsing LDAP entries
*/
public class LdapTreeGrid extends TreeGrid<LdapEntry> {
 
 private final LdapService ldapService;
 private LdapServerConfig serverConfig;
 private TreeData<LdapEntry> treeData;
 private TreeDataProvider<LdapEntry> dataProvider;
 
 public LdapTreeGrid(LdapService ldapService) {
  this.ldapService = ldapService;
  
  initializeGrid();
 }
 
 private void initializeGrid() {
  setSizeFull();
  setSelectionMode(Grid.SelectionMode.SINGLE);
  
  // Initialize tree data
  treeData = new TreeData<>();
  dataProvider = new TreeDataProvider<>(treeData);
  setDataProvider(dataProvider);
  
  // Add an icon column without header
  addComponentColumn(this::createIconComponent)
   .setHeader("")
   .setWidth("40px")
   .setFlexGrow(0)
   .setSortable(false)
   .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
  
  // Configure the hierarchy column to show full DN without header and no sorting
  addHierarchyColumn(this::getEntryDisplayName)
   .setHeader("")
   .setFlexGrow(1)
   .setResizable(true)
   .setSortable(false);
  
  // Style the tree grid
  addClassName("ldap-tree-grid");
  getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
  getStyle().set("margin", "0px");
  getStyle().set("padding", "0px");
  
  // Add keyboard navigation
  getElement().setAttribute("tabindex", "0");
  getElement().addEventListener("keydown", e -> {
   String key = e.getEventData().getString("event.key");
   if ("Enter".equals(key) || " ".equals(key)) {
    LdapEntry selectedEntry = asSingleSelect().getValue();
    if (selectedEntry != null && selectedEntry.isHasChildren()) {
     if (isExpanded(selectedEntry)) {
      collapse(selectedEntry);
     } else {
      expandEntry(selectedEntry);
     }
    }
   }
  }).addEventData("event.key");
  
  // Add expand listener for lazy loading and selection
  addExpandListener(event -> {
   event.getItems().forEach(item -> {
    if (isExpanded(item)) {
     loadChildren(item);
     // When an item is expanded, also select it to show its details
     select(item);
    }
   });
  });
  
  // Add collapse listener for selection
  addCollapseListener(event -> {
   event.getItems().forEach(item -> {
    // When an item is collapsed, also select it to show its details
    select(item);
   });
  });
  
  // Add selection listener
  asSingleSelect().addValueChangeListener(event -> {
   LdapEntry selectedEntry = event.getValue();
   if (selectedEntry != null) {
    fireSelectionEvent(selectedEntry);
   }
  });
 }
 
 /**
 * Create a placeholder entry for showing expand toggles
 */
 private LdapEntry createPlaceholderEntry() {
  LdapEntry placeholder = new LdapEntry();
  placeholder.setDn("_placeholder_" + System.currentTimeMillis() + "_" + Math.random());
  placeholder.setRdn("Loading...");
  placeholder.setHasChildren(false);
  return placeholder;
 }

 /**
 * Create an icon component for each entry type
 */
 private Icon createIconComponent(LdapEntry entry) {
  return getIconForEntry(entry);
 }

 /**
 * Get appropriate icon based on LDAP entry type
 */
 private Icon getIconForEntry(LdapEntry entry) {
  // Handle placeholder entries
  if (entry.getDn().startsWith("_placeholder_")) {
   Icon icon = new Icon(VaadinIcon.ELLIPSIS_DOTS_H);
   icon.getStyle().set("color", "#BDBDBD");
   return icon;
  }
  
  // Check if entry is loading
  if (entry.getAttributeValues("_loading").size() > 0) {
   Icon icon = new Icon(VaadinIcon.SPINNER);
   icon.getStyle().set("color", "#757575");
   icon.getElement().getStyle().set("animation", "spin 2s linear infinite");
   return icon;
  }
  
  // Special case for Root DSE first
  if (entry.getDn().isEmpty() || "Root DSE".equals(entry.getRdn())) {
   Icon icon = new Icon(VaadinIcon.DATABASE);
   icon.getStyle().set("color", "#FF5722"); // Deep orange for Root DSE
   return icon;
  }
  
  List<String> objectClasses = entry.getAttributeValues("objectClass");
  
  // First, check for specific entry types regardless of hasChildren flag
  // This ensures user/person entries get the correct icon even if mistakenly marked as having children
  if (objectClasses != null && !objectClasses.isEmpty()) {
   for (String objectClass : objectClasses) {
    String lowerClass = objectClass.toLowerCase();
    
    // Priority 1: Person/User entries (should always show user icon)
    if (lowerClass.contains("person") || lowerClass.contains("user") || 
     lowerClass.contains("inetorgperson") || lowerClass.contains("posixaccount")) {
     Icon icon = new Icon(VaadinIcon.USER);
     icon.getStyle().set("color", "#2196F3"); // Blue for users
     return icon;
    }
    
    // Priority 2: Group entries (should always show group icon)
    else if (lowerClass.contains("group") || lowerClass.contains("groupofnames") || 
      lowerClass.contains("posixgroup") || lowerClass.contains("groupofuniquenames")) {
     Icon icon = new Icon(VaadinIcon.USERS);
     icon.getStyle().set("color", "#4CAF50"); // Green for groups
     return icon;
    }
    
    // Priority 3: Other specific leaf types
    else if (lowerClass.contains("computer") || lowerClass.contains("device")) {
     Icon icon = new Icon(VaadinIcon.DESKTOP);
     icon.getStyle().set("color", "#607D8B"); // Blue-gray for computers
     return icon;
    } else if (lowerClass.contains("printer")) {
     Icon icon = new Icon(VaadinIcon.PRINT);
     icon.getStyle().set("color", "#795548"); // Brown for printers
     return icon;
    } else if (lowerClass.contains("application") || lowerClass.contains("service")) {
     Icon icon = new Icon(VaadinIcon.COG);
     icon.getStyle().set("color", "#FF9800"); // Orange for services
     return icon;
    } else if (lowerClass.contains("alias")) {
     Icon icon = new Icon(VaadinIcon.LINK);
     icon.getStyle().set("color", "#9C27B0"); // Purple for aliases
     return icon;
    } else if (lowerClass.contains("certificate") || lowerClass.contains("crl")) {
     Icon icon = new Icon(VaadinIcon.DIPLOMA);
     icon.getStyle().set("color", "#E91E63"); // Pink for certificates
     return icon;
    }
   }
  }
  
  // Now check for container types (only if not already identified as specific leaf types)
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
    } else if (lowerClass.contains("builtindomain")) {
     Icon icon = new Icon(VaadinIcon.SERVER);
     icon.getStyle().set("color", "#AB47BC"); // Purple for built-in domains
     return icon;
    }
   }
   
   // Default container icon for entries marked as having children
   Icon icon = new Icon(VaadinIcon.FOLDER);
   icon.getStyle().set("color", "#90A4AE"); // Gray for unknown containers
   return icon;
  }
  
  // Default leaf icon for everything else
  Icon icon = new Icon(VaadinIcon.FILE_TEXT);
  icon.getStyle().set("color", "#757575"); // Gray for unknown entries
  return icon;
 }

 private String getEntryDisplayName(LdapEntry entry) {
  // Handle placeholder entries
  if (entry.getDn().startsWith("_placeholder_")) {
   return "Loading...";
  }
  // Special case for Root DSE - show label instead of empty DN
  if (entry.getDn().isEmpty() || "Root DSE".equals(entry.getRdn())) {
   return "Root DSE";
  }
  // Return the full DN instead of display name
  return entry.getDn();
 }
 
 public void setServerConfig(LdapServerConfig serverConfig) {
  this.serverConfig = serverConfig;
 }
 
 public void loadRootEntries(String baseDn) throws LDAPException {
  if (serverConfig == null) {
   throw new IllegalStateException("Server config not set");
  }
  
  clear();
  
  // Use the metadata version to detect size limits
  LdapService.BrowseResult result = ldapService.browseEntriesWithMetadata(serverConfig.getId(), baseDn);
  List<LdapEntry> rootEntries = result.getEntries();
  
  for (LdapEntry entry : rootEntries) {
   // Ensure all entries get a chance to show expanders by checking their object classes
   ensureHasChildrenFlagIsSet(entry);
   
   treeData.addItem(null, entry);
   
   // Add placeholder children for entries that might have children to show expand toggle
   if (entry.isHasChildren() || shouldShowExpanderForEntry(entry)) {
    // Create a placeholder entry to show the expand arrow
    LdapEntry placeholder = createPlaceholderEntry();
    treeData.addItem(entry, placeholder);
    // Update the hasChildren flag if we're adding a placeholder
    if (!entry.isHasChildren()) {
     entry.setHasChildren(true);
    }
   }
  }
  
  dataProvider.refreshAll();
  
  if (rootEntries.isEmpty()) {
   showNotification("No entries found under: " + baseDn, NotificationVariant.LUMO_PRIMARY);
  } else if (result.isSizeLimitExceeded()) {
   showNotification("Showing first " + rootEntries.size() + " entries only - more than 100 entries found under: " + baseDn, 
    NotificationVariant.LUMO_ERROR);
  }
 }
 
 public void loadRootDSEWithNamingContexts() throws LDAPException {
  loadRootDSEWithNamingContexts(false);
 }
 
 public void loadRootDSEWithNamingContexts(boolean includePrivateNamingContexts) throws LDAPException {
  if (serverConfig == null) {
   throw new IllegalStateException("Server config not set");
  }
  
  clear();
  
  List<LdapEntry> rootEntries = ldapService.loadRootDSEWithNamingContexts(serverConfig.getId(), includePrivateNamingContexts);
  
  for (LdapEntry entry : rootEntries) {
   // Ensure all entries get a chance to show expanders by checking their object classes
   ensureHasChildrenFlagIsSet(entry);
   
   treeData.addItem(null, entry);
   
   // Add placeholder children for entries that might have children to show expand toggle
   if (entry.isHasChildren() || shouldShowExpanderForEntry(entry)) {
    // Check if this entry already has any children (including placeholders)
    List<LdapEntry> entryChildren = treeData.getChildren(entry);
    if (entryChildren.isEmpty()) {
     // Create a placeholder entry to show the expand arrow
     LdapEntry placeholder = createPlaceholderEntry();
     treeData.addItem(entry, placeholder);
    }
    // Update the hasChildren flag if we're adding a placeholder
    if (!entry.isHasChildren()) {
     entry.setHasChildren(true);
    }
   }
  }
  
  dataProvider.refreshAll();
  
  if (rootEntries.isEmpty()) {
   showNotification("No Root DSE or naming contexts found", NotificationVariant.LUMO_PRIMARY);
  } else {
   String message = "Loaded Root DSE and " + (rootEntries.size() - 1) + " naming contexts";
   if (includePrivateNamingContexts) {
    message += " (including private naming contexts)";
   }
   showNotification(message, NotificationVariant.LUMO_SUCCESS);
  }
 }
 
 private void loadChildren(LdapEntry parent) {
  if (serverConfig == null || !parent.isHasChildren()) {
   return;
  }
  
  // Check if we have only placeholder children
  List<LdapEntry> existingChildren = treeData.getChildren(parent);
  boolean hasOnlyPlaceholder = existingChildren.size() == 1 && 
   existingChildren.get(0).getDn().startsWith("_placeholder_");
  
  // If we already have real children loaded, don't reload
  if (!existingChildren.isEmpty() && !hasOnlyPlaceholder) {
   return;
  }
  
  try {
   // Show loading indicator
   getUI().ifPresent(ui -> ui.access(() -> {
    parent.addAttribute("_loading", "true");
    dataProvider.refreshItem(parent);
   }));
   
   // Use the metadata version to detect size limits
   LdapService.BrowseResult result = ldapService.browseEntriesWithMetadata(serverConfig.getId(), parent.getDn());
   List<LdapEntry> children = result.getEntries();
   
   getUI().ifPresent(ui -> ui.access(() -> {
    // Remove loading indicator
    parent.getAttributes().remove("_loading");
    
    // Remove placeholder children if they exist
    if (hasOnlyPlaceholder) {
     treeData.removeItem(existingChildren.get(0));
    }
    
    // Add real children
    for (LdapEntry child : children) {
     // Ensure all child entries get a chance to show expanders
     ensureHasChildrenFlagIsSet(child);
     
     treeData.addItem(parent, child);
     
     // Add placeholder for children that have or might have children
     if (child.isHasChildren() || shouldShowExpanderForEntry(child)) {
      // Check if this child already has any children (including placeholders)
      List<LdapEntry> childChildren = treeData.getChildren(child);
      if (childChildren.isEmpty()) {
       LdapEntry placeholder = createPlaceholderEntry();
       treeData.addItem(child, placeholder);
      }
      // Update the hasChildren flag if we're adding a placeholder
      if (!child.isHasChildren()) {
       child.setHasChildren(true);
      }
     }
    }
    
    dataProvider.refreshItem(parent, true);
    
    if (children.isEmpty()) {
     // If no children found, mark as no longer having children and collapse
     parent.setHasChildren(false);
     dataProvider.refreshItem(parent);
     // Collapse the entry since it has no children
     collapse(parent);
    } else if (result.isSizeLimitExceeded()) {
     // Show warning when size limit is exceeded
     showNotification("Showing first " + children.size() + " entries only - more than 100 children found under " + parent.getDn(), 
      NotificationVariant.LUMO_ERROR);
    } else {
     showNotification("Loaded " + children.size() + " child entries", NotificationVariant.LUMO_SUCCESS);
    }
   }));
   
  } catch (LDAPException e) {
   getUI().ifPresent(ui -> ui.access(() -> {
    // Remove loading indicator
    parent.getAttributes().remove("_loading");
    dataProvider.refreshItem(parent);
    
    showNotification("Failed to load children for " + parent.getDn() + ": " + e.getMessage(), 
     NotificationVariant.LUMO_ERROR);
   }));
  } catch (Exception e) {
   getUI().ifPresent(ui -> ui.access(() -> {
    // Remove loading indicator
    parent.getAttributes().remove("_loading");
    dataProvider.refreshItem(parent);
    
    showNotification("Unexpected error loading children for " + parent.getDn() + ": " + e.getMessage(), 
     NotificationVariant.LUMO_ERROR);
   }));
  }
 }
 
 public void showSearchResults(List<LdapEntry> results) {
  clear();
  
  for (LdapEntry entry : results) {
   treeData.addItem(null, entry);
  }
  
  dataProvider.refreshAll();
  
  if (results.isEmpty()) {
   showNotification("No search results found", NotificationVariant.LUMO_PRIMARY);
  } else {
   showNotification("Found " + results.size() + " entries", NotificationVariant.LUMO_SUCCESS);
  }
 }
 
 public void clear() {
  treeData.clear();
  dataProvider.refreshAll();
 }
 
 public void refreshEntry(LdapEntry entry) {
  dataProvider.refreshItem(entry);
 }
 
 /**
 * Expand a specific entry and load its children if needed
 */
 public void expandEntry(LdapEntry entry) {
  if (entry.isHasChildren() && !isExpanded(entry)) {
   loadChildren(entry);
   expand(entry);
  }
 }
 
 private void fireSelectionEvent(LdapEntry entry) {
  // This will be handled by the selection listener added in the constructor
 }
 
 /**
 * Ensure that entries that typically have children are marked as such
 */
 private void ensureHasChildrenFlagIsSet(LdapEntry entry) {
  if (!entry.isHasChildren() && shouldShowExpanderForEntry(entry)) {
   entry.setHasChildren(true);
  }
 }
 
 /**
 * Determine if an entry should show an expander based on its object classes
 */
 private boolean shouldShowExpanderForEntry(LdapEntry entry) {
  // Check DN pattern first - if it starts with "ou=" it's likely an organizational unit
  String dn = entry.getDn();
  if (dn != null && dn.toLowerCase().startsWith("ou=")) {
   return true;
  }
  
  // Check all object classes to determine if this entry should have an expander
  // But exclude person/user types even if they contain organizational patterns
  boolean hasPersonClass = false;
  boolean hasContainerClass = false;
  
  List<String> objectClasses = entry.getAttributeValues("objectClass");
  for (String oc : objectClasses) {
   String lowerOc = oc.toLowerCase();
   
   // Check if this is a person/user entry
   if (lowerOc.contains("person") || lowerOc.contains("user") || 
    lowerOc.contains("inetorgperson") || lowerOc.contains("posixaccount")) {
    hasPersonClass = true;
   }
   
   // Check if this is a container/organizational entry
   if (lowerOc.equals("organizationalunit") ||
    lowerOc.contains("organizationalunit") ||
    lowerOc.equals("organization") ||
    lowerOc.contains("organization") ||
    lowerOc.contains("container") ||
    lowerOc.contains("domain") ||
    lowerOc.contains("dcobject") ||
    lowerOc.contains("builtindomain")) {
    hasContainerClass = true;
   }
  }
  
  // If it's a person/user, don't show expander regardless of other classes
  if (hasPersonClass && !hasContainerClass) {
   return false;
  }
  
  // If it has container classes, show expander
  if (hasContainerClass) {
   return true;
  }
  
  return false;
 }
 
 private void showNotification(String message, NotificationVariant variant) {
  Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
  notification.addThemeVariants(variant);
 }
}
