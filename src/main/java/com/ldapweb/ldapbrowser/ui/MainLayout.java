package com.ldapweb.ldapbrowser.ui;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Root application layout with a side drawer and a top navbar used as view
 * header.
 */
@AnonymousAllowed
public class MainLayout extends AppLayout {

  /**
   * Title of the current view displayed in the navbar.
   */
  private final H1 viewTitle = new H1();

  /**
   * Context title displayed in the navbar, e.g., current server or group.
   */
  private final H1 contextTitle = new H1();

  /**
   * Connection status chip displayed in the navbar.
   */
  private final HorizontalLayout connectionChip = new HorizontalLayout();

  /**
   * Service for managing application configuration.
   */
  private final ConfigurationService configurationService;

  /**
   * Service for managing in-memory LDAP servers.
   */
  private final InMemoryLdapService inMemoryLdapService;

  /**
   * Service for managing server selection.
   */
  private final ServerSelectionService selectionService;

  /**
   * Service for interacting with LDAP servers.
   */
  private final LdapService ldapService;

  /**
   * Index of server items in the drawer for quick access.
   */
  private final Map<String, SideNavItem> serverItemIndex = new HashMap<>();

  /**
   * Side navigation component for the drawer.
   */
  private final SideNav nav;

  /**
   * Root item for the servers section in the drawer.
   */
  private final SideNavItem serversRoot;

  /**
   * Root item for the group search section in the drawer.
   */
  private final SideNavItem groupSearchRoot;

  /**
   * Constructs the MainLayout with the required services.
   *
   * @param configurationService the configuration service
   * @param inMemoryLdapService  the in-memory LDAP service
   * @param selectionService     the server selection service
   * @param ldapService          the LDAP service
   */
  public MainLayout(ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      ServerSelectionService selectionService,
      LdapService ldapService) {
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;
    this.ldapService = ldapService;

    // Navbar content (to the side of the drawer toggle)
    final DrawerToggle toggle = new DrawerToggle();
    viewTitle.getStyle().set("font-size", "var(--lumo-font-size-l)")
        .set("margin", "0");
    contextTitle.getStyle().set("font-size", "var(--lumo-font-size-m)")
        .set("margin", "0 0 0 var(--lumo-space-m)")
        .set("color", "var(--lumo-secondary-text-color)");

    setupConnectionChip();
    addToNavbar(toggle, viewTitle, contextTitle, connectionChip);

    // Drawer content
    nav = new SideNav();

    // Servers accordion-like group
    serversRoot = new SideNavItem("Servers", ServersView.class);
    serversRoot.setPrefixComponent(new Icon(VaadinIcon.SERVER));
    populateServers(serversRoot);
    nav.addItem(serversRoot);

    // Group Operations - shows only groups, clicking goes to group search view
    groupSearchRoot = new SideNavItem("Group Operations", (String) null);
    groupSearchRoot.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    populateGroups(groupSearchRoot);
    nav.addItem(groupSearchRoot);

    // Settings link
    SideNavItem settingsItem = new SideNavItem("Settings", SettingsView.class);
    settingsItem.setPrefixComponent(new Icon(VaadinIcon.COG));
    nav.addItem(settingsItem);

    Scroller scroller = new Scroller(nav);
    scroller.addClassName(LumoUtility.Padding.SMALL);

    addToDrawer(scroller);

    setPrimarySection(Section.DRAWER);
  }

  /**
   * Updates the view title and context title after navigation.
   */
  @Override
  protected void afterNavigation() {
    super.afterNavigation();
    viewTitle.setText(getCurrentPageTitle());

    // Update context title depending on current view
    if (getContent() instanceof GroupSearchView gsv) {
      String grp = gsv.getGroupName();
      contextTitle.setText(grp != null && !grp.isBlank()
          ? ("Group: " + grp)
          : "Group Operations");
      // In group mode, hide single-server connection chip to avoid confusion
      connectionChip.setVisible(false);
    } else {
      connectionChip.setVisible(true);
    }
  }

  /**
   * Retrieves the title of the current page.
   *
   * @return the current page title, or an empty string if none is set
   */
  private String getCurrentPageTitle() {
    if (getContent() == null) {
      return "";
    }
    PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
    return title != null ? title.value() : "";
  }

  /**
   * Populates the servers section in the drawer.
   *
   * @param root the root item for the servers section
   */
  private void populateServers(SideNavItem root) {
    root.removeAll();
    serverItemIndex.clear();

    // Unified groups map (external + internal)
    Map<String, SideNavItem> groups = new HashMap<>();
    SideNavItem ungrouped = new SideNavItem("Ungrouped", (String) null);
    ungrouped.setPrefixComponent(new Icon(VaadinIcon.FOLDER));

    // External servers
    for (LdapServerConfig cfg : configurationService.getAllConfigurations()) {
      Set<String> serverGroups = cfg.getGroups();
      
      if (serverGroups.isEmpty()) {
        // Server belongs to no groups - add to ungrouped
        String name = cfg.getName() != null ? cfg.getName() : cfg.getHost();
        SideNavItem item = new SideNavItem(name, "/select/" + cfg.getId());
        item.setPrefixComponent(new Icon(VaadinIcon.DATABASE));
        ungrouped.addItem(item);
        serverItemIndex.put(cfg.getId(), item);
      } else {
        // Server belongs to one or more groups - add to each group
        for (String groupName : serverGroups) {
          if (groupName != null && !groupName.trim().isEmpty()) {
            String trimmedGroup = groupName.trim();
            SideNavItem parent = groups.computeIfAbsent(trimmedGroup, g -> {
              SideNavItem grp = new SideNavItem(g, (String) null);
              grp.setPrefixComponent(new Icon(VaadinIcon.FOLDER_OPEN));
              return grp;
            });

            String name = cfg.getName() != null ? cfg.getName() : cfg.getHost();
            SideNavItem item = new SideNavItem(name, "/select/" + cfg.getId());
            item.setPrefixComponent(new Icon(VaadinIcon.DATABASE));
            parent.addItem(item);
            // Only store in index once (use first group's item for highlighting)
            if (!serverItemIndex.containsKey(cfg.getId())) {
              serverItemIndex.put(cfg.getId(), item);
            }
          }
        }
      }
    }

    // Internal running servers
    for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
      if (inMemoryLdapService.isServerRunning(cfg.getId())) {
        Set<String> serverGroups = cfg.getGroups();
        
        if (serverGroups.isEmpty()) {
          // Server belongs to no groups - add to ungrouped
          String name = (cfg.getName() != null ? cfg.getName() : cfg.getHost()) + " (internal)";
          SideNavItem item = new SideNavItem(name, "/select/" + cfg.getId());
          item.setPrefixComponent(new Icon(VaadinIcon.CUBE));
          ungrouped.addItem(item);
          serverItemIndex.put(cfg.getId(), item);
        } else {
          // Server belongs to one or more groups - add to each group
          for (String groupName : serverGroups) {
            if (groupName != null && !groupName.trim().isEmpty()) {
              String trimmedGroup = groupName.trim();
              SideNavItem parent = groups.computeIfAbsent(trimmedGroup, g -> {
                SideNavItem grp = new SideNavItem(g, (String) null);
                grp.setPrefixComponent(new Icon(VaadinIcon.FOLDER_OPEN));
                return grp;
              });

              String name = (cfg.getName() != null ? cfg.getName() : cfg.getHost()) + " (internal)";
              SideNavItem item = new SideNavItem(name, "/select/" + cfg.getId());
              item.setPrefixComponent(new Icon(VaadinIcon.CUBE));
              parent.addItem(item);
              // Only store in index once (use first group's item for highlighting)
              if (!serverItemIndex.containsKey(cfg.getId())) {
                serverItemIndex.put(cfg.getId(), item);
              }
            }
          }
        }
      }
    }

    // Add groups sorted by name, then ungrouped if any
    groups.keySet().stream().sorted().forEach(k -> root.addItem(groups.get(k)));
    if (!ungrouped.getItems().isEmpty()) {
      root.addItem(ungrouped);
    }
  }

  /**
   * Populates the groups section in the drawer.
   *
   * @param root the root item for the groups section
   */
  private void populateGroups(SideNavItem root) {
    root.removeAll();
    // Collect groups from external and running internal servers
    Set<String> groups = new TreeSet<>();
    
    for (LdapServerConfig cfg : configurationService.getAllConfigurations()) {
      cfg.getGroups().forEach(group -> {
        if (group != null && !group.trim().isEmpty()) {
          groups.add(group.trim());
        }
      });
    }
    
    for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
      if (inMemoryLdapService.isServerRunning(cfg.getId())) {
        cfg.getGroups().forEach(group -> {
          if (group != null && !group.trim().isEmpty()) {
            groups.add(group.trim());
          }
        });
      }
    }

    // Add a nav item for each group; route to GroupSearchView
    for (String group : groups) {
      String encoded = URLEncoder.encode(group, StandardCharsets.UTF_8);
      SideNavItem item = new SideNavItem(group, "/group-search/" + encoded);
      item.setPrefixComponent(new Icon(VaadinIcon.FOLDER_OPEN));
      root.addItem(item);
    }
  }

  /**
   * Handles actions to perform when the layout is attached to the UI.
   *
   * @param attachEvent the attach event
   */
  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    // Show current server selection in the navbar context area
    LdapServerConfig selected = selectionService.getSelected();
    updateSelectionUi(selected);
    selectionService.addListener(this::updateSelectionUi);
  }

  /**
   * Updates the UI to reflect the currently selected server.
   *
   * @param cfg the currently selected server configuration
   */
  private void updateSelectionUi(LdapServerConfig cfg) {
    String suffix = cfg != null
        ? (cfg.getName() != null ? cfg.getName() : cfg.getHost())
        : "None";
    contextTitle.setText("Server: " + suffix);
    updateConnectionChip(cfg);
    updateDrawerHighlight(cfg);
  }

  /**
   * Sets up the connection status chip in the navbar.
   */
  private void setupConnectionChip() {
    connectionChip.setSpacing(true);
    connectionChip.setPadding(false);
    connectionChip.getStyle().set("margin-left", "var(--lumo-space-m)");
    connectionChip.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    connectionChip.setVisible(true);
    // Initial content
    updateConnectionChip(null);
  }

  /**
   * Updates the connection status chip based on the given server configuration.
   *
   * @param cfg the server configuration to update the chip for
   */
  private void updateConnectionChip(LdapServerConfig cfg) {
    connectionChip.removeAll();
    Icon dot = new Icon(VaadinIcon.CIRCLE);
    dot.setSize("10px");
    boolean connected = cfg != null && ldapService.isConnected(cfg.getId());
    dot.getStyle().set("color",
        connected ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
    Span text = new Span(connected ? "Connected" : "Disconnected");
    text.getStyle().set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");
    connectionChip.add(dot, text);
  }

  /**
   * Highlights the currently selected server in the drawer.
   *
   * @param cfg the currently selected server configuration
   */
  private void updateDrawerHighlight(LdapServerConfig cfg) {
    // Clear previous highlights from all items
    clearAllHighlights();
    
    if (cfg != null) {
      // Find and highlight all instances of the selected server
      highlightServerInstances(cfg.getId());
    }
  }
  
  /**
   * Clears highlights from all server items in the drawer.
   */
  private void clearAllHighlights() {
    // Clear highlights from main servers section
    clearHighlightsFromNavItem(serversRoot);
  }
  
  /**
   * Recursively clears highlights from all items in a nav item tree.
   */
  private void clearHighlightsFromNavItem(SideNavItem navItem) {
    navItem.setSuffixComponent(null);
    for (SideNavItem child : navItem.getItems()) {
      clearHighlightsFromNavItem(child);
    }
  }
  
  /**
   * Highlights all instances of a server with the given ID in the drawer.
   */
  private void highlightServerInstances(String serverId) {
    if (serverId == null) return;
    
    // Find and highlight all instances of this server
    highlightServerInNavItem(serversRoot, serverId);
  }
  
  /**
   * Recursively searches for and highlights server instances in a nav item tree.
   */
  private void highlightServerInNavItem(SideNavItem navItem, String serverId) {
    // Check if this item's path matches the server selection path
    String path = navItem.getPath();
    if (path != null && path.equals("/select/" + serverId)) {
      Icon check = new Icon(VaadinIcon.CHECK);
      check.setSize("16px");
      check.getStyle().set("color", "var(--lumo-success-color)");
      navItem.setSuffixComponent(check);
    }
    
    // Recursively check children
    for (SideNavItem child : navItem.getItems()) {
      highlightServerInNavItem(child, serverId);
    }
  }

  /**
   * Refreshes the server list and group list in the drawer.
   */
  public void refreshServerListInDrawer() {
    populateServers(serversRoot);
    populateGroups(groupSearchRoot);
    // Re-apply highlight and connection chip based on current selection
    updateSelectionUi(selectionService.getSelected());
  }
}
