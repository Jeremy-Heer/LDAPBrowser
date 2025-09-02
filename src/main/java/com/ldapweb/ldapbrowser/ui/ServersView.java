package com.ldapweb.ldapbrowser.ui;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.service.LoggingService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.ldapweb.ldapbrowser.ui.components.AccessControlsTab;
import com.ldapweb.ldapbrowser.ui.components.BulkOperationsTab;
import com.ldapweb.ldapbrowser.ui.components.DashboardTab;
import com.ldapweb.ldapbrowser.ui.components.DirectorySearchTab;
import com.ldapweb.ldapbrowser.ui.components.ExportTab;
import com.ldapweb.ldapbrowser.ui.components.SchemaBrowser;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Optional;

/**
 * View for managing LDAP servers, including directory search, schema browsing, and bulk operations.
 */
@Route(value = "servers", layout = MainLayout.class)
@PageTitle("Servers")
@AnonymousAllowed
public class ServersView extends VerticalLayout implements BeforeEnterObserver {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final LoggingService loggingService;
  private final ServerSelectionService selectionService;

  private TabSheet tabSheet;
  private DirectorySearchTab directorySearchTab;
  private DashboardTab dashboardTab;
  private SchemaBrowser schemaBrowser;
  private AccessControlsTab accessControlsTab;
  private ExportTab reportsTab;
  private BulkOperationsTab bulkOperationsTab;

  /**
   * Constructs the ServersView with the required services.
   *
   * @param ldapService the LDAP service
   * @param configurationService the configuration service
   * @param inMemoryLdapService the in-memory LDAP service
   * @param loggingService the logging service
   * @param selectionService the server selection service
   */
  public ServersView(LdapService ldapService,
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
    bindSelection();
  }

  private void initTabs() {
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();
    
    // Add a tab selection listener to lazily load data only when a tab is selected
    tabSheet.addSelectedChangeListener(event -> {
      if (selectionService.getSelected() == null) {
        return;
      }
      
      Tab selectedTab = event.getSelectedTab();
      if (selectedTab != null) {
        String tabLabel = selectedTab.getLabel();
        if ("LDAP Browser".equals(tabLabel)) {
          dashboardTab.loadRootDSEWithNamingContexts();
        } else if ("Access".equals(tabLabel)) {
          accessControlsTab.loadData();
        } else if ("Schema".equals(tabLabel)) {
          schemaBrowser.loadSchema();
        }
      }
    });

    Tab directorySearchTabComponent = new Tab("Directory Search");
    directorySearchTab = new DirectorySearchTab(ldapService, configurationService,
     inMemoryLdapService, selectionService, loggingService);
    tabSheet.add(directorySearchTabComponent, directorySearchTab);

    Tab dashboardTabComponent = new Tab("LDAP Browser");
    dashboardTab = new DashboardTab(ldapService, configurationService, inMemoryLdapService,
        selectionService);
    tabSheet.add(dashboardTabComponent, dashboardTab);

    Tab schemaTabComponent = new Tab("Schema");
    schemaBrowser = new SchemaBrowser(ldapService, configurationService, inMemoryLdapService,
        selectionService);
    tabSheet.add(schemaTabComponent, schemaBrowser);

    Tab accessControlsTabComponent = new Tab("Access");
    accessControlsTab = new AccessControlsTab(ldapService);
    tabSheet.add(accessControlsTabComponent, accessControlsTab);

    Tab reportsTabComponent = new Tab("Reports");
    reportsTab = new ExportTab(ldapService, loggingService, configurationService,
        inMemoryLdapService);
    tabSheet.add(reportsTabComponent, reportsTab);

    Tab bulkOperationsTabComponent = new Tab("Bulk Operations");
    bulkOperationsTab = new BulkOperationsTab(ldapService, loggingService, configurationService,
        inMemoryLdapService);
    tabSheet.add(bulkOperationsTabComponent, bulkOperationsTab);

    add(tabSheet);
    setFlexGrow(1, tabSheet);
  }

  private void bindSelection() {
    LdapServerConfig current = selectionService.getSelected();
    applySelection(current);
    selectionService.addListener(this::applySelection);
  }

  private void applySelection(LdapServerConfig config) {
    if (config == null) {
      Notification n = Notification.show(
          "Select a server from the drawer to begin", 3000, Notification.Position.TOP_END);
      n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
      return;
    }

    try {
      if (!ldapService.isConnected(config.getId())) {
        ldapService.connect(config);
      }
    } catch (Exception e) {
      Notification n = Notification.show(
          "Failed to connect: " + e.getMessage(), 5000, Notification.Position.TOP_END);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
      return;
    }

    // Update tabs that need the server config explicitly 
    // but don't trigger immediate data loading
    dashboardTab.setServerConfig(config);
    schemaBrowser.setServerConfig(config);
    accessControlsTab.setServerConfig(config);
    reportsTab.setServerConfig(config);
    bulkOperationsTab.setServerConfig(config);
    directorySearchTab.refreshEnvironments();
    
    // Only load data for the currently active tab
    Tab selectedTab = tabSheet.getSelectedTab();
    if (selectedTab != null) {
      String tabLabel = selectedTab.getLabel();
      if ("LDAP Browser".equals(tabLabel)) {
        dashboardTab.loadRootDSEWithNamingContexts();
      } else if ("Access".equals(tabLabel)) {
        accessControlsTab.loadData();
      } else if ("Schema".equals(tabLabel)) {
        // Load schema when the Schema tab is selected
        schemaBrowser.loadSchema();
      }
    }
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<String> sid = event.getLocation().getQueryParameters().getParameters()
        .getOrDefault("sid", java.util.List.of()).stream().findFirst();
    if (sid.isPresent()) {
      String id = sid.get();
      // Try to resolve from external first
      LdapServerConfig cfg = configurationService.getAllConfigurations().stream()
          .filter(c -> id.equals(c.getId()))
          .findFirst()
          .orElseGet(() -> inMemoryLdapService.getAllInMemoryServers().stream()
              .filter(c -> id.equals(c.getId()))
              .findFirst()
              .orElse(null));
      if (cfg != null) {
        selectionService.setSelected(cfg);
      }
    }
  }
}
