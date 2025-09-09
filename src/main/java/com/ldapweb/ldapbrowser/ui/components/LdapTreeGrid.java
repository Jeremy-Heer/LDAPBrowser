package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.unboundid.ldap.sdk.LDAPException;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
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
  // When loading the Root DSE we may want to include private naming contexts
  // This flag is set when loadRootDSEWithNamingContexts(boolean) is called
  private boolean includePrivateNamingContexts = false;

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

    // Hide the header row completely to remove the resizer
    hideGridHeader();

    // Style the tree grid
    addClassName("ldap-tree-grid");
    addClassName("no-header"); // Add class for CSS styling
    getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
    getStyle().set("margin", "0px");
    getStyle().set("padding", "0px");

    // Add attach listener to ensure header stays hidden when switching tabs
    addAttachListener(event -> {
      // Use a small delay to ensure the DOM is ready
      getUI().ifPresent(ui -> ui.access(() -> {
        ui.getElement().executeJs(
            "setTimeout(() => { if ($0.shadowRoot && $0.shadowRoot.querySelector('thead')) { $0.shadowRoot.querySelector('thead').style.display = 'none'; } }, 50)",
            getElement());
      }));
    });

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
        } else if (selectedEntry.getDn().isEmpty() || "Root DSE".equals(selectedEntry.getRdn())) {
          // If the Root DSE is selected, expand it to show naming contexts (and
          // include private naming contexts depending on flag). This ensures the
          // Root DSE is always the root-most object and its naming contexts are
          // loaded on demand when the user interacts with it.
          expandEntry(selectedEntry);
        }
      }
    });
  }

  /**
   * Hide the grid header to provide a clean tree view
   */
  private void hideGridHeader() {
    getElement().executeJs("this.shadowRoot.querySelector('thead').style.display = 'none'");
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
  private LdapEntry createPaginationControlEntry(String parentDn, int currentPage, boolean hasNextPage,
      boolean hasPrevPage) {
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

  private void addPaginationEntries(LdapEntry parentEntry, String parentDn, int currentPage, boolean hasNextPage,
      boolean hasPrevPage) {
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
  }

  /**
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
    // This ensures user/person entries get the correct icon even if mistakenly
    // marked as having children
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

    // Now check for container types (only if not already identified as specific
    // leaf types)
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
      // Ensure all entries get a chance to show expanders by checking their object
      // classes
      ensureHasChildrenFlagIsSet(entry);

      treeData.addItem(null, entry);

      // Add placeholder children for entries that might have children to show expand
      // toggle
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

    // Remember the preference for private naming contexts so that expand
    // behavior can honor it later when Root DSE is interacted with.
    this.includePrivateNamingContexts = includePrivateNamingContexts;

    // **NEW: Collapse the entire tree before clearing to reset all expansion states**
    // This prevents preservation of undesired expanded states and ensures a clean reload
    collapseAll();

    clear();

    // Load the Root DSE entry with naming contexts as a list where the first
    // entry is expected to be the Root DSE itself (with empty DN)
    List<LdapEntry> rootEntries = ldapService.loadRootDSEWithNamingContexts(serverConfig.getId(),
        includePrivateNamingContexts);

    if (rootEntries.isEmpty()) {
      dataProvider.refreshAll();
      showNotification("No Root DSE or naming contexts found", NotificationVariant.LUMO_PRIMARY);
      return;
    }

    // Find the explicit Root DSE entry (prefer one with empty DN). If not found,
    // fall back to the first entry.
    LdapEntry rootDse = null;
    for (LdapEntry e : rootEntries) {
      if (e.getDn().isEmpty() || "Root DSE".equals(e.getRdn())) {
        rootDse = e;
        break;
      }
    }
    if (rootDse == null) {
      rootDse = rootEntries.get(0);
    }

    // Ensure root DSE shows as a root-most item
    ensureHasChildrenFlagIsSet(rootDse);
    treeData.addItem(null, rootDse);

    // Add a placeholder for Root DSE so the user can expand it to load naming
    // contexts on demand. We'll not pre-add naming contexts as separate roots.
    if (rootDse.isHasChildren() || shouldShowExpanderForEntry(rootDse)) {
      LdapEntry placeholder = createPlaceholderEntry();
      treeData.addItem(rootDse, placeholder);
      if (!rootDse.isHasChildren()) {
        rootDse.setHasChildren(true);
      }
    }

    // After adding the new Root DSE and placeholders, ensure it's not expanded
    // (though collapseAll should handle this, this is a safeguard)
    if (rootDse != null && isExpanded(rootDse)) {
      collapse(rootDse);
    }

    dataProvider.refreshAll();

    String message = "Loaded Root DSE";
    if (includePrivateNamingContexts) {
      message += " (including private naming contexts)";
    }
    showNotification(message, NotificationVariant.LUMO_SUCCESS);
  }

  private void loadChildren(LdapEntry parent) {
    // Get current page for this parent (default to 0)
    int currentPage = entryPageState.getOrDefault(parent.getDn(), 0);
    loadChildren(parent, currentPage);
  }

  private void loadChildren(LdapEntry parent, int page) {
    if (serverConfig == null) {
      return;
    }

    // Store the current page
    entryPageState.put(parent.getDn(), page);

    try {
      // Special-case Root DSE: load naming contexts as its children instead of
      // doing a regular browse. Root DSE is identified by an empty DN or
      // explicit "Root DSE" RDN.
      if (parent.getDn().isEmpty() || "Root DSE".equals(parent.getRdn())) {
        getUI().ifPresent(ui -> ui.access(() -> {
          parent.addAttribute("_loading", "true");
          dataProvider.refreshItem(parent);
        }));

        try {
          // Retrieve naming contexts via existing service methods
          List<String> namingContextDns = ldapService.getNamingContexts(serverConfig.getId());
          List<LdapEntry> namingContexts = new ArrayList<>();

          for (String ctx : namingContextDns) {
            try {
              LdapEntry ctxEntry = ldapService.getEntryMinimal(serverConfig.getId(), ctx);
              if (ctxEntry != null) {
                ctxEntry.setHasChildren(true);
                namingContexts.add(ctxEntry);
              }
            } catch (Exception ignoredCtx) {
              LdapEntry ctxEntry = new LdapEntry();
              ctxEntry.setDn(ctx);
              ctxEntry.setRdn(ctx);
              ctxEntry.setHasChildren(true);
              ctxEntry.addAttribute("objectClass", "organizationalUnit");
              namingContexts.add(ctxEntry);
            }
          }

          if (includePrivateNamingContexts) {
            List<String> privateDns = ldapService.getPrivateNamingContexts(serverConfig.getId());
            for (String ctx : privateDns) {
              try {
                LdapEntry ctxEntry = ldapService.getEntryMinimal(serverConfig.getId(), ctx);
                if (ctxEntry != null) {
                  ctxEntry.setHasChildren(true);
                  namingContexts.add(ctxEntry);
                }
              } catch (Exception ignoredCtx) {
                LdapEntry ctxEntry = new LdapEntry();
                ctxEntry.setDn(ctx);
                ctxEntry.setRdn(ctx);
                ctxEntry.setHasChildren(true);
                ctxEntry.addAttribute("objectClass", "organizationalUnit");
                namingContexts.add(ctxEntry);
              }
            }
          }

          getUI().ifPresent(ui -> ui.access(() -> {
            // Remove loading indicator
            parent.getAttributes().remove("_loading");

            // Remove existing children
            List<LdapEntry> existingChildren = new ArrayList<>(treeData.getChildren(parent));
            for (LdapEntry child : existingChildren) {
              treeData.removeItem(child);
            }

            // Add naming contexts as children under Root DSE
            for (LdapEntry nc : namingContexts) {
              ensureHasChildrenFlagIsSet(nc);
              treeData.addItem(parent, nc);

              if (nc.isHasChildren() || shouldShowExpanderForEntry(nc)) {
                List<LdapEntry> childChildren = treeData.getChildren(nc);
                if (childChildren.isEmpty()) {
                  LdapEntry placeholder = createPlaceholderEntry();
                  treeData.addItem(nc, placeholder);
                }
                if (!nc.isHasChildren()) {
                  nc.setHasChildren(true);
                }
              }
            }

            dataProvider.refreshItem(parent, true);
            showNotification("Loaded " + namingContexts.size() + " naming contexts", NotificationVariant.LUMO_SUCCESS);
          }));
        } catch (Exception ex) {
          getUI().ifPresent(ui -> ui.access(() -> {
            parent.getAttributes().remove("_loading");
            dataProvider.refreshItem(parent);
            showNotification("Failed to load naming contexts: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
          }));
        }

        return;
      }
      // First, check if the parent actually has children (lazy check)
      boolean actuallyHasChildren = ldapService.checkHasChildren(serverConfig.getId(), parent.getDn());

      if (!actuallyHasChildren) {
        // Only remove the expander for entries that definitely shouldn't have one
        // Keep expander for entries that might have children in the future or should
        // always show expander
        getUI().ifPresent(ui -> ui.access(() -> {
          if (!shouldShowExpanderForEntry(parent)) {
            parent.setHasChildren(false);
          }
          // Remove any placeholder children but keep the expander if it should be shown
          List<LdapEntry> existingChildren = new ArrayList<>(treeData.getChildren(parent));
          for (LdapEntry child : existingChildren) {
            treeData.removeItem(child);
          }
          dataProvider.refreshItem(parent, true);
          // showNotification("No child entries found under " + parent.getDn(), NotificationVariant.LUMO_PRIMARY);
        }));
        return;
      } // Show loading indicator
      getUI().ifPresent(ui -> ui.access(() -> {
        parent.addAttribute("_loading", "true");
        dataProvider.refreshItem(parent);
      }));

      // Use the metadata version with paging support
      LdapService.BrowseResult result = ldapService.browseEntriesWithMetadata(serverConfig.getId(), parent.getDn(),
          page, PAGE_SIZE);
      List<LdapEntry> children = result.getEntries();

      getUI().ifPresent(ui -> ui.access(() -> {
        // Remove loading indicator
        parent.getAttributes().remove("_loading");

        // **NEW: Collapse all existing children recursively before removing them**
        // This prevents preservation of undesired expanded states on new children
        List<LdapEntry> existingChildren = new ArrayList<>(treeData.getChildren(parent));
        for (LdapEntry child : existingChildren) {
          collapseRecursively(child);  // Collapse the subtree to reset states
        }

        // Remove all existing children (including placeholders and pagination controls)
        for (LdapEntry child : existingChildren) {
          treeData.removeItem(child);
        }

        if (children.isEmpty()) {
          // No children found, but only remove expander if entry shouldn't have one
          if (!shouldShowExpanderForEntry(parent)) {
            parent.setHasChildren(false);
          }
          // showNotification("No child entries found under " + parent.getDn(), NotificationVariant.LUMO_PRIMARY);
        } else {
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
                result.hasPrevPage());
          }

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

        dataProvider.refreshItem(parent, true);
      }));

    } catch (Exception e) {
      getUI().ifPresent(ui -> ui.access(() -> {
        // Remove loading indicator
        parent.getAttributes().remove("_loading");
        dataProvider.refreshItem(parent);

        showNotification("Failed to load children for " + parent.getDisplayName() + ": " + e.getMessage(),
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
    entryPageState.clear(); // Clear client-side paging state

    // Clear server-side paging state if server config is available
    if (serverConfig != null) {
      ldapService.clearPagingState(serverConfig.getId());
    }
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
   * Collapse all expanded entries in the tree
   */
  public void collapseAll() {
    // Get all root items and recursively collapse their expanded children
    treeData.getRootItems().forEach(this::collapseRecursively);
  }

  /**
   * Recursively collapse an entry and all its expanded children
   */
  private void collapseRecursively(LdapEntry entry) {
    // First, recursively collapse all children
    treeData.getChildren(entry).forEach(this::collapseRecursively);

    // Then collapse this entry if it's expanded
    if (isExpanded(entry)) {
      collapse(entry);
    }
  }

  /**
   * Ensure that entries that typically have children are marked as such
   * Modified to be more aggressive in showing expanders for better browsing
   * experience
   */
  private void ensureHasChildrenFlagIsSet(LdapEntry entry) {
    // Always check if an entry should show expander, regardless of current
    // hasChildren flag
    if (shouldShowExpanderForEntry(entry)) {
      entry.setHasChildren(true);
    }
  }

  /**
   * Determine if an entry should show an expander based on its object classes
   * Modified to be more permissive and always show expanders unless explicitly a
   * leaf entry
   */
  private boolean shouldShowExpanderForEntry(LdapEntry entry) {
    // Skip pagination and placeholder entries
    if (entry.getDn().startsWith("_pagination_") || entry.getDn().startsWith("_placeholder_")) {
      return false;
    }

    List<String> objectClasses = entry.getAttributeValues("objectClass");
    if (objectClasses == null || objectClasses.isEmpty()) {
      // If no object classes, assume it might have children and show expander
      return true;
    }

    // Check for definitely leaf entry types that should NOT show expanders
    boolean isDefinitelyLeaf = false;
    for (String oc : objectClasses) {
      String lowerOc = oc.toLowerCase();

      // These are typically leaf entries that should not show expanders
      if (lowerOc.contains("person") || lowerOc.contains("user") ||
          lowerOc.contains("inetorgperson") || lowerOc.contains("posixaccount") ||
          lowerOc.contains("computer") || lowerOc.contains("device") ||
          lowerOc.contains("printer") || lowerOc.contains("certificate") ||
          lowerOc.contains("alias")) {
        isDefinitelyLeaf = true;
        break;
      }
    }

    // For entries that are definitely leaf entries, don't show expander
    if (isDefinitelyLeaf) {
      return false;
    }

    // For all other entries, show expander to allow browsing
    // This includes organizationalUnit, organization, container, domain, etc.
    // and any unknown object classes that might potentially have children
    return true;
  }

  private void showNotification(String message, NotificationVariant variant) {
    Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(variant);
  }
}