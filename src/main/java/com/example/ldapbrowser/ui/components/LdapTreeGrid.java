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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Tree grid component for browsing LDAP entries
*/
public class LdapTreeGrid extends TreeGrid<LdapEntry> {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private TreeData<LdapEntry> treeData;
  private TreeDataProvider<LdapEntry> dataProvider;
  
  // Paging support
  private final Map<String, Integer> entryPageState = new HashMap<>(); // DN -> current page
  private static final int PAGE_SIZE = 100;

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
      // Check if this is a pagination control entry
      if (!selectedEntry.getAttributeValues("isPagination").isEmpty()) {
        handlePaginationClick(selectedEntry);
        // Clear selection to avoid keeping pagination entry selected
        asSingleSelect().clear();
      }
    }
  });
}

/**
* Create a placeholder entry for showing expand toggles
*/
private LdapEntry createPlaceholderEntry() {
  LdapEntry placeholder = new LdapEntry();
  placeholder.setDn("_placeholder_" + System.nanoTime());
  placeholder.addAttribute("displayName", "Loading...");
  placeholder.setHasChildren(false);
  return placeholder;
}

/**
* Create a pagination control entry
*/
private LdapEntry createPaginationControlEntry(String parentDn, int currentPage, boolean hasNextPage, boolean hasPrevPage) {
  // This method is now used to create individual pagination entries
  // We'll call it multiple times to create separate Previous/Next entries
  LdapEntry paginationEntry = new LdapEntry();
  String entryId = "_pagination_" + parentDn.hashCode() + "_" + System.nanoTime();
  paginationEntry.setDn(entryId);
  
  // This will be overridden by the calling method
  paginationEntry.addAttribute("isPagination", "true");
  paginationEntry.addAttribute("parentDn", parentDn);
  paginationEntry.addAttribute("currentPage", String.valueOf(currentPage));
  paginationEntry.addAttribute("hasNext", String.valueOf(hasNextPage));
  paginationEntry.addAttribute("hasPrev", String.valueOf(hasPrevPage));
  paginationEntry.setHasChildren(false);
  
  return paginationEntry;
}

private void addPaginationEntries(LdapEntry parentEntry, String parentDn, int currentPage, boolean hasNextPage, boolean hasPrevPage) {
  // Create Previous entry if available
  if (hasPrevPage) {
    LdapEntry prevEntry = createPaginationControlEntry(parentDn, currentPage, hasNextPage, hasPrevPage);
    prevEntry.setDn("_pagination_prev_" + parentDn.hashCode() + "_" + System.nanoTime());
    prevEntry.addAttribute("displayName", "◀ Previous Page");
    prevEntry.addAttribute("action", "previous");
    treeData.addItem(parentEntry, prevEntry);
  }
  
  // Create Next entry if available
  if (hasNextPage) {
    LdapEntry nextEntry = createPaginationControlEntry(parentDn, currentPage, hasNextPage, hasPrevPage);
    nextEntry.setDn("_pagination_next_" + parentDn.hashCode() + "_" + System.nanoTime());
    nextEntry.addAttribute("displayName", "Next Page ▶");
    nextEntry.addAttribute("action", "next");
    treeData.addItem(parentEntry, nextEntry);
  }
  
  // Create page info entry (non-clickable, just for information)
  if (hasPrevPage || hasNextPage) {
    LdapEntry infoEntry = createPaginationControlEntry(parentDn, currentPage, hasNextPage, hasPrevPage);
    infoEntry.setDn("_pagination_info_" + parentDn.hashCode() + "_" + System.nanoTime());
    infoEntry.addAttribute("displayName", "— Page " + (currentPage + 1) + " —");
    infoEntry.addAttribute("action", "info");
    treeData.addItem(parentEntry, infoEntry);
  }
}

/**
* Handle pagination control clicks
*/
private void handlePaginationClick(LdapEntry paginationEntry) {
  String parentDn = paginationEntry.getFirstAttributeValue("parentDn");
  int currentPage = Integer.parseInt(paginationEntry.getFirstAttributeValue("currentPage"));
  String action = paginationEntry.getFirstAttributeValue("action");
  
  // Don't process info entries
  if ("info".equals(action)) {
    return;
  }
  
  // Find the parent entry
  LdapEntry parentEntry = findEntryByDn(parentDn);
  if (parentEntry == null) {
    return;
  }
  
  // Calculate new page based on action
  int newPage = currentPage;
  if ("next".equals(action)) {
    newPage = currentPage + 1;
  } else if ("previous".equals(action)) {
    newPage = currentPage - 1;
  }
  
  if (newPage != currentPage) {
    entryPageState.put(parentDn, newPage);
    loadChildren(parentEntry, newPage);
    
    // Show brief feedback about the action
    showNotification(String.format("Navigated to %s page (%d)", action, newPage + 1), 
                     NotificationVariant.LUMO_SUCCESS);
  }
}

/**
* Find an entry by DN in the tree
*/
private LdapEntry findEntryByDn(String dn) {
  return findEntryByDnRecursive(null, dn);
}

/**
* Recursively find an entry by DN
*/
private LdapEntry findEntryByDnRecursive(LdapEntry parent, String dn) {
  for (LdapEntry child : treeData.getChildren(parent)) {
    if (dn.equals(child.getDn())) {
      return child;
    }
    LdapEntry found = findEntryByDnRecursive(child, dn);
    if (found != null) {
      return found;
    }
  }
  return null;
}/**
* Create an icon component for each entry type
*/
private Icon createIconComponent(LdapEntry entry) {
  // Check if this is a pagination control
  if (!entry.getAttributeValues("isPagination").isEmpty()) {
    Icon paginationIcon = new Icon(VaadinIcon.ELLIPSIS_DOTS_H);
    paginationIcon.getStyle().set("color", "var(--lumo-primary-color)");
    paginationIcon.getStyle().set("cursor", "pointer");
    return paginationIcon;
  }
  
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
  
  // Handle pagination control entries
  if (!entry.getAttributeValues("isPagination").isEmpty()) {
    return entry.getFirstAttributeValue("displayName");
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
  } else {
    String message;
    if (result.hasNextPage()) {
      message = String.format("Loaded %d entries under: %s (more available, expand entries to navigate)", 
        rootEntries.size(), baseDn);
    } else {
      message = "Loaded " + rootEntries.size() + " entries under: " + baseDn;
    }
    showNotification(message, NotificationVariant.LUMO_SUCCESS);
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
  // Get current page for this parent (default to 0)
  int currentPage = entryPageState.getOrDefault(parent.getDn(), 0);
  loadChildren(parent, currentPage);
}

private void loadChildren(LdapEntry parent, int page) {
  if (serverConfig == null || !parent.isHasChildren()) {
    return;
  }

  // Store the current page
  entryPageState.put(parent.getDn(), page);

  try {
    // Show loading indicator
    getUI().ifPresent(ui -> ui.access(() -> {
      parent.addAttribute("_loading", "true");
      dataProvider.refreshItem(parent);
    }));

    // Use the metadata version with paging support
    LdapService.BrowseResult result = ldapService.browseEntriesWithMetadata(serverConfig.getId(), parent.getDn(), page, PAGE_SIZE);
    List<LdapEntry> children = result.getEntries();

    getUI().ifPresent(ui -> ui.access(() -> {
      // Remove loading indicator
      parent.getAttributes().remove("_loading");

      // Remove all existing children (including placeholders and pagination controls)
      List<LdapEntry> existingChildren = new ArrayList<>(treeData.getChildren(parent));
      for (LdapEntry child : existingChildren) {
        treeData.removeItem(child);
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

      // Add pagination controls if needed
      if (result.hasNextPage() || result.hasPrevPage()) {
        addPaginationEntries(
          parent, 
          parent.getDn(), 
          result.getCurrentPage(), 
          result.hasNextPage(), 
          result.hasPrevPage()
        );
      }

      dataProvider.refreshItem(parent, true);

      if (children.isEmpty()) {
        // If no children found, mark as no longer having children and collapse
        parent.setHasChildren(false);
        dataProvider.refreshItem(parent);
        // Collapse the entry since it has no children
        collapse(parent);
        showNotification("No child entries found under " + parent.getDn(), NotificationVariant.LUMO_PRIMARY);
      } else {
        // Show appropriate notification based on paging
        String message;
        if (result.hasNextPage() || result.hasPrevPage()) {
          message = String.format("Loaded page %d (%d entries) for %s - Use pagination controls to navigate", 
            result.getCurrentPage() + 1, children.size(), parent.getDn());
        } else {
          message = "Loaded " + children.size() + " child entries for " + parent.getDn();
        }
        showNotification(message, NotificationVariant.LUMO_SUCCESS);
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
  entryPageState.clear(); // Clear paging state
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