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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Route handler for URLs in the format /servers/{group}/{serverId}.
 * This view displays the server management interface for a specific server
 * within a group context.
 */
@Route(value = "servers/:group/:sid", layout = MainLayout.class)
@PageTitle("Server Management")
@AnonymousAllowed
public class ServersGroupServerView extends VerticalLayout implements BeforeEnterObserver {

  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;
  private final LdapService ldapService;
  private final LoggingService loggingService;

  private TabSheet tabSheet;

  /**
   * Constructs a new ServersGroupServerView.
   *
   * @param configurationService the configuration service
   * @param inMemoryLdapService  the in-memory LDAP service
   * @param selectionService     the server selection service
   * @param ldapService          the LDAP service
   * @param loggingService       the logging service
   */
  public ServersGroupServerView(ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      ServerSelectionService selectionService,
      LdapService ldapService,
      LoggingService loggingService) {
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;
    this.ldapService = ldapService;
    this.loggingService = loggingService;
    
    initializeLayout();
  }

  
  /**
   * Initializes the layout with tabs for server management.
   */
  private void initializeLayout() {
    setSizeFull();
    setPadding(false);
    setSpacing(false);

    tabSheet = new TabSheet();
    tabSheet.setSizeFull();
    add(tabSheet);

    // We'll add the tabs when a server is selected
  }

  /**
   * Handles the before-enter event to select a server by group and ID,
   * then display the server management interface.
   *
   * @param event the before-enter event
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String group = event.getRouteParameters().get("group").orElse(null);
    String id = event.getRouteParameters().get("sid").orElse(null);

    if (id != null) {
      // First try to find in external configurations
      LdapServerConfig cfg = configurationService.getAllConfigurations().stream()
          .filter(c -> id.equals(c.getId()))
          .findFirst()
          .orElseGet(() -> 
              // If not found, try in-memory servers
              inMemoryLdapService.getAllInMemoryServers().stream()
                  .filter(c -> id.equals(c.getId()))
                  .findFirst()
                  .orElse(null));

      if (cfg != null) {
        // Verify the server actually belongs to the specified group (if group is provided)
        if (group != null && !cfg.getGroups().contains(group)) {
          // Server doesn't belong to this group, show error
          Notification.show("Server '" + id + "' not found in group '" + group + "'", 
              3000, Notification.Position.MIDDLE)
              .addThemeVariants(NotificationVariant.LUMO_ERROR);
          return;
        }
        selectionService.setSelected(cfg);
        setupServerTabs(cfg);
      } else {
        // Server not found
        Notification.show("Server '" + id + "' not found", 
            3000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    }
  }

  /**
   * Sets up the tabs for the selected server.
   *
   * @param serverConfig the selected server configuration
   */
  private void setupServerTabs(LdapServerConfig serverConfig) {
    // Clear existing tabs
    removeAll();
    
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    // Directory Search Tab
    DirectorySearchTab directorySearchTab = new DirectorySearchTab(ldapService,
        configurationService, inMemoryLdapService, selectionService, loggingService);
    tabSheet.add("Directory Search", directorySearchTab);

    // Dashboard Tab
    DashboardTab dashboardTab = new DashboardTab(ldapService, configurationService,
        inMemoryLdapService, selectionService);
    tabSheet.add("LDAP Browser", dashboardTab);

    // Schema Browser Tab
    SchemaBrowser schemaBrowser = new SchemaBrowser(ldapService, configurationService,
        inMemoryLdapService, selectionService);
    tabSheet.add("Schema", schemaBrowser);

    // Access Controls Tab
    AccessControlsTab accessControlsTab = new AccessControlsTab(ldapService);
    tabSheet.add("Access", accessControlsTab);

    // Export Tab (Reports)
    ExportTab exportTab = new ExportTab(ldapService, loggingService, configurationService,
        inMemoryLdapService);
    tabSheet.add("Reports", exportTab);

    // Bulk Operations Tab
    BulkOperationsTab bulkOperationsTab = new BulkOperationsTab(ldapService, loggingService,
        configurationService, inMemoryLdapService);
    tabSheet.add("Bulk Operations", bulkOperationsTab);

    add(tabSheet);

    // Try to connect to the server
    try {
      if (!ldapService.isConnected(serverConfig.getId())) {
        ldapService.connect(serverConfig);
      }
    } catch (Exception e) {
      Notification.show("Error connecting to server: " + e.getMessage(), 
          3000, Notification.Position.MIDDLE)
          .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }
}
