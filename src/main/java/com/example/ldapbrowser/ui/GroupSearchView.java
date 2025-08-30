package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.example.ldapbrowser.ui.components.DirectorySearchTab;
import com.example.ldapbrowser.ui.components.GroupSchemaTab;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * View for performing group-specific operations, such as directory search and schema comparison.
 */
@Route(value = "group-search/:group", layout = MainLayout.class)
@PageTitle("Group Operations")
@AnonymousAllowed
public class GroupSearchView extends VerticalLayout implements BeforeEnterObserver {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;
  private final LoggingService loggingService;

  private TabSheet tabSheet;
  private DirectorySearchTab directorySearchTab;
  private GroupSchemaTab schemaTabContent;
  private String groupName;

  /**
   * Constructs the GroupSearchView with the required services.
   *
   * @param ldapService the LDAP service
   * @param configurationService the configuration service
   * @param inMemoryLdapService the in-memory LDAP service
   * @param loggingService the logging service
   * @param selectionService the server selection service
   */
  public GroupSearchView(LdapService ldapService,
      ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      LoggingService loggingService,
      ServerSelectionService selectionService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
  this.loggingService = loggingService;
    this.selectionService = selectionService;

    setSizeFull();
    setPadding(false);
    setSpacing(false);

    initTabs();
  }

  private void initTabs() {
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    Tab directorySearchTabComponent = new Tab("Directory Search");
  directorySearchTab = new DirectorySearchTab(ldapService, configurationService,
    inMemoryLdapService, selectionService, loggingService);
    tabSheet.add(directorySearchTabComponent, directorySearchTab);

    // Schema tab for group-wide schema comparison
    Tab schemaTab = new Tab("Schema");
    schemaTabContent = new GroupSchemaTab(ldapService);
    tabSheet.add(schemaTab, schemaTabContent);

    add(tabSheet);
    setFlexGrow(1, tabSheet);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
  this.groupName = event.getRouteParameters().get("group").orElse("");
    if (groupName == null || groupName.isBlank()) {
      Notification.show("No group specified", 3000, Notification.Position.TOP_END);
      return;
    }

    // Build the environment set for this group from both external and running internal servers
    Set<LdapServerConfig> groupServers = new HashSet<>();
  final String normalized = normalizeGroup(groupName);
  List<LdapServerConfig> external = configurationService.getAllConfigurations()
    .stream()
    .filter(c -> normalized.equalsIgnoreCase(normalizeGroup(c.getGroup())))
    .collect(Collectors.toList());
    groupServers.addAll(external);

    // Add internal running servers matching the group
    for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
      if (inMemoryLdapService.isServerRunning(cfg.getId())
          && normalized.equalsIgnoreCase(normalizeGroup(cfg.getGroup()))) {
        groupServers.add(cfg);
      }
    }

    // Provide the environments to the tabs and refresh UI state
    directorySearchTab.setEnvironmentSupplier(() -> groupServers);
    directorySearchTab.refreshEnvironments();
    schemaTabContent.setEnvironments(groupServers);
  }

  /**
   * Normalize group names for comparison.
   * - URL-decode the incoming route parameter
   * - Treat '+' as space
   * - Collapse multiple whitespace to a single space
   * - Trim
   */
  private String normalizeGroup(String s) {
    if (s == null) return "";
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
   * Expose the active group name for layout/context display.
   *
   * @return the active group name
   */
  public String getGroupName() {
    return groupName;
  }
}
