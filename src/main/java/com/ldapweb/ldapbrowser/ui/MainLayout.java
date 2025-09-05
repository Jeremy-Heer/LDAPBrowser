package com.ldapweb.ldapbrowser.ui;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.ldapweb.ldapbrowser.ui.SettingsView;
import com.ldapweb.ldapbrowser.ui.WelcomeView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Root application layout with a side drawer and a top navbar used as view
 * header.
 */
@AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver {

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
   * ComboBox for selecting external LDAP servers.
   */
  private final ComboBox<LdapServerConfig> serversComboBox = new ComboBox<>();

  /**
   * ComboBox for selecting server groups.
   */
  private final ComboBox<String> groupsComboBox = new ComboBox<>();

  /**
   * ComboBox for selecting running internal servers.
   */
  private final ComboBox<LdapServerConfig> internalServersComboBox = new ComboBox<>();

  /**
   * Service for managing application configuration.
   */
  private final ConfigurationService configurationService;

  /**
   * Service for managing server selection.
   */
  private final ServerSelectionService selectionService;

  /**
   * Service for interacting with LDAP servers.
   */
  private final LdapService ldapService;

  /**
   * Service for managing in-memory LDAP servers.
   */
  private final InMemoryLdapService inMemoryLdapService;

  /**
   * Side navigation component for the drawer.
   */
  private final SideNav nav;

  /**
   * Drawer content layout for dynamic updates.
   */
  private VerticalLayout drawerContent;


  /**
   * Constructs the MainLayout with the required services.
   *
   * @param configurationService the configuration service
   * @param selectionService     the server selection service
   * @param ldapService          the LDAP service
   * @param inMemoryLdapService  the in-memory LDAP service
   */
  public MainLayout(ConfigurationService configurationService,
      ServerSelectionService selectionService,
      LdapService ldapService,
      InMemoryLdapService inMemoryLdapService) {
    this.configurationService = configurationService;
    this.selectionService = selectionService;
    this.ldapService = ldapService;
    this.inMemoryLdapService = inMemoryLdapService;

    // Navbar content (to the side of the drawer toggle)
    final DrawerToggle toggle = new DrawerToggle();
    viewTitle.getStyle().set("font-size", "var(--lumo-font-size-l)")
        .set("margin", "0");
    contextTitle.getStyle().set("font-size", "var(--lumo-font-size-m)")
        .set("margin", "0 0 0 var(--lumo-space-m)")
        .set("color", "var(--lumo-secondary-text-color)");

    setupConnectionChip();
    setupServersComboBox();
    setupGroupsComboBox();
    setupInternalServersComboBox();
    addToNavbar(toggle, viewTitle, contextTitle, connectionChip);

    // Drawer content
    drawerContent = new VerticalLayout();
    drawerContent.setPadding(false);
    drawerContent.setSpacing(false);
    
    nav = new SideNav();
    
    // Home link
    SideNavItem homeItem = new SideNavItem("Home", WelcomeView.class);
    homeItem.setPrefixComponent(new Icon(VaadinIcon.HOME));
    nav.addItem(homeItem);

    // Add servers ComboBox after Home
    serversComboBox.getStyle().set("margin", "var(--lumo-space-s)");
    serversComboBox.setWidthFull();
    
    // Add groups ComboBox after servers
    groupsComboBox.getStyle().set("margin", "var(--lumo-space-s)");
    groupsComboBox.setWidthFull();
    
    // Add internal servers ComboBox conditionally
    internalServersComboBox.getStyle().set("margin", "var(--lumo-space-s)");
    internalServersComboBox.setWidthFull();
    
    // Settings link
    SideNavItem settingsItem = new SideNavItem("Settings", SettingsView.class);
    settingsItem.setPrefixComponent(new Icon(VaadinIcon.COG));
    nav.addItem(settingsItem);

    // Add combo boxes to drawer
    drawerContent.add(nav, serversComboBox, groupsComboBox);
    updateInternalServersVisibility();
    
    Scroller scroller = new Scroller(drawerContent);
    scroller.addClassName(LumoUtility.Padding.SMALL);

    addToDrawer(scroller);

    setPrimarySection(Section.DRAWER);
  }

  /**
   * Populates the servers ComboBox with external servers only.
   */
  private void populateServersComboBox() {
    serversComboBox.setItems(configurationService.getAllConfigurations());
  }

  /**
   * Populates the groups ComboBox with all available groups.
   */
  private void populateGroupsComboBox() {
    groupsComboBox.setItems(configurationService.getAllGroups());
  }

  /**
   * Populates the internal servers ComboBox with running internal servers.
   */
  private void populateInternalServersComboBox() {
    internalServersComboBox.setItems(inMemoryLdapService.getRunningInMemoryServers());
  }

  /**
   * Updates the visibility of the internal servers ComboBox based on running servers.
   */
  private void updateInternalServersVisibility() {
    boolean hasRunningInternalServers = !inMemoryLdapService.getRunningInMemoryServers().isEmpty();
    
    // Check if the combo box is currently in the drawer
    boolean isCurrentlyInDrawer = drawerContent.getChildren()
        .anyMatch(component -> component == internalServersComboBox);
    
    if (hasRunningInternalServers && !isCurrentlyInDrawer) {
      // Add the combo box after the groups combo box (index 3: nav, servers, groups, internal servers)
      drawerContent.addComponentAtIndex(3, internalServersComboBox);
    } else if (!hasRunningInternalServers && isCurrentlyInDrawer) {
      // Remove the combo box
      drawerContent.remove(internalServersComboBox);
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
    // Note: Manual highlighting removed - Vaadin handles navigation highlighting automatically
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
   * Sets up the servers ComboBox for the drawer.
   */
  private void setupServersComboBox() {
    serversComboBox.setPlaceholder("Select Server");
    serversComboBox.setItemLabelGenerator(config -> 
        config.getName() != null ? config.getName() : config.getHost());
    serversComboBox.setClearButtonVisible(true);
    
    // Populate with external servers only
    populateServersComboBox();
    
    // Handle selection changes
    serversComboBox.addValueChangeListener(event -> {
      LdapServerConfig selectedServer = event.getValue();
      if (selectedServer != null) {
        // Navigate to the server details view
        UI.getCurrent().navigate("servers/" + selectedServer.getId());
      }
    });
  }

  /**
   * Sets up the groups ComboBox for the drawer.
   */
  private void setupGroupsComboBox() {
    groupsComboBox.setPlaceholder("Select Group");
    groupsComboBox.setClearButtonVisible(true);
    
    // Populate with all available groups
    populateGroupsComboBox();
    
    // Handle selection changes
    groupsComboBox.addValueChangeListener(event -> {
      String selectedGroup = event.getValue();
      if (selectedGroup != null && !selectedGroup.trim().isEmpty()) {
        // Navigate to the group search view
        UI.getCurrent().navigate("group-search/" + selectedGroup);
      }
    });
  }

  /**
   * Sets up the internal servers ComboBox for the drawer.
   */
  private void setupInternalServersComboBox() {
    internalServersComboBox.setPlaceholder("Select Internal Server");
    internalServersComboBox.setItemLabelGenerator(config -> 
        config.getName() != null ? config.getName() : config.getHost());
    internalServersComboBox.setClearButtonVisible(true);
    
    // Populate with running internal servers
    populateInternalServersComboBox();
    
    // Handle selection changes
    internalServersComboBox.addValueChangeListener(event -> {
      LdapServerConfig selectedServer = event.getValue();
      if (selectedServer != null) {
        // Navigate to the server details view
        UI.getCurrent().navigate("servers/" + selectedServer.getId());
      }
    });
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
   * Updates the connection chip to show group connection status.
   *
   * @param location the current route location
   */
  private void updateConnectionChipForGroup(String location) {
    if (!location.startsWith("group-search/")) {
      return;
    }

    String groupName = location.substring("group-search/".length());
    // Decode URL encoding if needed (basic decoding for spaces)
    groupName = groupName.replace("%20", " ").replace("+", " ");
    // Normalize multiple spaces to single space and trim
    groupName = groupName.trim().replaceAll("\\s+", " ");

    // Get servers in this group using the same logic as GroupSearchView
    Set<LdapServerConfig> groupServers = getServersInGroup(groupName);
    
    connectionChip.removeAll();
    Icon dot = new Icon(VaadinIcon.CIRCLE);
    dot.setSize("10px");
    
    if (groupServers.isEmpty()) {
      // No servers in group
      dot.getStyle().set("color", "var(--lumo-contrast-30pct)");
      Span text = new Span("No Servers");
      text.getStyle().set("font-size", "var(--lumo-font-size-s)")
          .set("color", "var(--lumo-secondary-text-color)");
      connectionChip.add(dot, text);
      return;
    }

    // Count connected servers
    int totalServers = groupServers.size();
    int connectedServers = (int) groupServers.stream()
        .filter(server -> ldapService.isConnected(server.getId()))
        .count();

    // Set status based on connection results
    String statusText;
    String color;
    
    if (connectedServers == 0) {
      color = "var(--lumo-error-color)";
      statusText = "None Connected";
    } else if (connectedServers == totalServers) {
      color = "var(--lumo-success-color)";
      statusText = "All Connected";
    } else {
      color = "var(--lumo-warning-color)";
      statusText = connectedServers + "/" + totalServers + " Connected";
    }
    
    dot.getStyle().set("color", color);
    Span text = new Span(statusText);
    text.getStyle().set("font-size", "var(--lumo-font-size-s)")
        .set("color", "var(--lumo-secondary-text-color)");
    connectionChip.add(dot, text);
  }

  /**
   * Gets all servers that belong to the specified group.
   * Uses the same logic as GroupSearchView to find servers.
   *
   * @param groupName the name of the group
   * @return set of servers in the group
   */
  private Set<LdapServerConfig> getServersInGroup(String groupName) {
    Set<LdapServerConfig> groupServers = new HashSet<>();
    final String normalized = normalizeGroup(groupName);
    
    // Check external servers - look at all groups each server belongs to
    List<LdapServerConfig> external = configurationService.getAllConfigurations()
        .stream()
        .filter(c -> c.getGroups().stream()
            .anyMatch(group -> normalized.equalsIgnoreCase(normalizeGroup(group))))
        .collect(Collectors.toList());
    groupServers.addAll(external);

    // Add internal running servers matching the group - check all groups
    for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
      if (inMemoryLdapService.isServerRunning(cfg.getId())
          && cfg.getGroups().stream()
              .anyMatch(group -> normalized.equalsIgnoreCase(normalizeGroup(group)))) {
        groupServers.add(cfg);
      }
    }
    
    return groupServers;
  }

  /**
   * Normalize group names for comparison.
   * - URL-decode the incoming route parameter
   * - Treat '+' as space
   * - Collapse multiple whitespace to a single space
   * - Trim
   */
  private String normalizeGroup(String s) {
    if (s == null) {
      return "";
    }
    String value = s;
    try {
      // URL decode in case the route segment contained encoded spaces (%20)
      value = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name());
    } catch (Exception ignored) {
      // ignore and continue with original value
    }
    // Replace '+' (common in some encodings) with space and collapse whitespace
    value = value.replace('+', ' ').trim().replaceAll("\\s+", " ");
    return value;
  }

  /**
   * Refreshes the server list in the ComboBox.
   */
  public void refreshServerList() {
    populateServersComboBox();
    populateGroupsComboBox();
    populateInternalServersComboBox();
    updateInternalServersVisibility();
    // Re-apply selection state based on current selection
    updateSelectionUi(selectionService.getSelected());
  }

  /**
   * Handles navigation events to clear server selection when navigating away from server routes.
   *
   * @param event the navigation event
   */
  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    String location = event.getLocation().getPath();
    
    // Clear ComboBox selection if not on a server route
    if (!location.startsWith("servers/")) {
      // Temporarily disable the value change listener to prevent navigation loop
      serversComboBox.setValue(null);
      internalServersComboBox.setValue(null);
    } else {
      // If on a server route, try to set the ComboBox to match the current server
      updateComboBoxFromRoute(location);
    }
    
    // Clear group ComboBox selection if not on a group-search route
    if (!location.startsWith("group-search/")) {
      groupsComboBox.setValue(null);
    } else {
      // If on a group-search route, try to set the ComboBox to match the current group
      updateGroupComboBoxFromRoute(location);
      // Update context title for group view
      updateContextTitleForGroup(location);
    }
    
    // Update context title for server view if not on group-search route
    if (!location.startsWith("group-search/")) {
      // Restore server context title if we have a selected server
      LdapServerConfig selectedServer = selectionService.getSelected();
      updateSelectionUi(selectedServer);
    } else {
      // For group routes, update connection chip to show group status
      updateConnectionChipForGroup(location);
    }
  }

  /**
   * Updates the context title for group view.
   *
   * @param location the current route location
   */
  private void updateContextTitleForGroup(String location) {
    if (location.startsWith("group-search/")) {
      String groupName = location.substring("group-search/".length());
      // Decode URL encoding if needed (basic decoding for spaces)
      groupName = groupName.replace("%20", " ").replace("+", " ");
      // Normalize multiple spaces to single space and trim
      groupName = groupName.trim().replaceAll("\\s+", " ");
      
      contextTitle.setText("Group: " + groupName);
    }
  }

  /**
   * Updates the ComboBox selection to match the current server route.
   *
   * @param location the current route location
   */
  private void updateComboBoxFromRoute(String location) {
    if (location.startsWith("servers/")) {
      String serverId = location.substring("servers/".length());
      
      // First try to find in external servers
      configurationService.getAllConfigurations().stream()
          .filter(config -> serverId.equals(config.getId()))
          .findFirst()
          .ifPresentOrElse(config -> {
            // Only update if different to avoid triggering navigation
            if (!config.equals(serversComboBox.getValue())) {
              serversComboBox.setValue(config);
            }
          }, () -> {
            // If not found in external servers, try internal servers
            inMemoryLdapService.getRunningInMemoryServers().stream()
                .filter(config -> serverId.equals(config.getId()))
                .findFirst()
                .ifPresent(config -> {
                  // Only update if different to avoid triggering navigation
                  if (!config.equals(internalServersComboBox.getValue())) {
                    internalServersComboBox.setValue(config);
                  }
                });
          });
    }
  }

  /**
   * Updates the group ComboBox selection to match the current group route.
   *
   * @param location the current route location
   */
  private void updateGroupComboBoxFromRoute(String location) {
    if (location.startsWith("group-search/")) {
      String groupName = location.substring("group-search/".length());
      // Decode URL encoding if needed (basic decoding for spaces)
      groupName = groupName.replace("%20", " ");
      
      // Only update if different to avoid triggering navigation
      if (!groupName.equals(groupsComboBox.getValue())) {
        groupsComboBox.setValue(groupName);
      }
    }
  }
}
