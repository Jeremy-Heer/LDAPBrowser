package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.ui.components.ServersTab;
import com.example.ldapbrowser.ui.components.DashboardTab;
import com.example.ldapbrowser.ui.components.SchemaBrowser;
import com.example.ldapbrowser.ui.components.ExportTab;
import com.example.ldapbrowser.ui.components.BulkOperationsTab;
import com.example.ldapbrowser.ui.components.LogsTab;
import com.example.ldapbrowser.ui.components.DirectorySearchTab;
import com.example.ldapbrowser.ui.components.EnvironmentRefreshListener;
import com.example.ldapbrowser.service.LoggingService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.unboundid.ldap.sdk.LDAPException;

/**
* Main LDAP Browser UI with tabbed interface - Apache Directory Studio inspired layout
*/
// Deprecated: kept for reference during migration. Not exposed as a route.
public class MainView extends AppLayout implements EnvironmentRefreshListener {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final LoggingService loggingService;
  private final ServerSelectionService selectionService;

  // Main content components
  private Tabs mainTabs;
  private ServersTab serversTab;
  private DashboardTab dashboardTab;
  private SchemaBrowser schemaBrowser;
  private ExportTab reportsTab;
  private BulkOperationsTab bulkOperationsTab;
  private LogsTab logsTab;
  private DirectorySearchTab directorySearchTab;
  private VerticalLayout contentContainer;

  public MainView(LdapService ldapService, ConfigurationService configurationService,
  InMemoryLdapService inMemoryLdapService, LoggingService loggingService, ServerSelectionService selectionService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.loggingService = loggingService;
    this.selectionService = selectionService;

    initializeComponents();
    setupLayout();
    refreshServerList();
  }

  private void initializeComponents() {
    // Main tabs - Directory Search tab first, Servers renamed to Settings and moved to far right
    mainTabs = new Tabs();
    Tab directorySearchTabComponent = new Tab("Directory Search");
    Tab dashboardTabComponent = new Tab("LDAP Browser");
    Tab schemaTabComponent = new Tab("Schema");
    Tab reportsTabComponent = new Tab("Reports");
    Tab bulkOperationsTabComponent = new Tab("Bulk Operations");
    Tab logsTabComponent = new Tab("Logs");
    Tab serversTabComponent = new Tab("Settings");
    mainTabs.add(directorySearchTabComponent, dashboardTabComponent, schemaTabComponent, reportsTabComponent, bulkOperationsTabComponent, logsTabComponent, serversTabComponent);

    mainTabs.addSelectedChangeListener(e -> {
      Tab selectedTab = e.getSelectedTab();
      if (selectedTab == directorySearchTabComponent) {
        showDirectorySearch();
      } else if (selectedTab == dashboardTabComponent) {
      showDashboard();
    } else if (selectedTab == schemaTabComponent) {
    showSchema();
  } else if (selectedTab == reportsTabComponent) {
  showReports();
} else if (selectedTab == bulkOperationsTabComponent) {
showBulkOperations();
} else if (selectedTab == logsTabComponent) {
showLogs();
} else if (selectedTab == serversTabComponent) {
showServers();
}
});

// Tab content components
serversTab = new ServersTab(ldapService, configurationService, this, inMemoryLdapService);
serversTab.setConnectionListener(this::connectToServer);
serversTab.setDisconnectionListener(this::disconnectFromServer);

dashboardTab = new DashboardTab(ldapService, configurationService, inMemoryLdapService, selectionService);
directorySearchTab = new DirectorySearchTab(ldapService, configurationService, inMemoryLdapService, selectionService);
schemaBrowser = new SchemaBrowser(ldapService, configurationService, inMemoryLdapService, selectionService);
reportsTab = new ExportTab(ldapService, loggingService, configurationService, inMemoryLdapService);
bulkOperationsTab = new BulkOperationsTab(ldapService, loggingService, configurationService, inMemoryLdapService);
logsTab = new LogsTab(loggingService);

// Content container
contentContainer = new VerticalLayout();
contentContainer.setSizeFull();
contentContainer.setPadding(false);
contentContainer.setSpacing(false);

// Initially show directory search tab
contentContainer.add(directorySearchTab);
}

private void setupLayout() {
  // Create main content layout with tabs
  VerticalLayout mainContent = new VerticalLayout();
  mainContent.setSizeFull();
  mainContent.setPadding(false);
  mainContent.setSpacing(false);

  // Tabs header
  HorizontalLayout tabsHeader = new HorizontalLayout();
  tabsHeader.setWidthFull();
  tabsHeader.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
  tabsHeader.setPadding(true);
  tabsHeader.addClassName("tabs-header");

  tabsHeader.add(mainTabs);

  mainContent.add(tabsHeader, contentContainer);
  mainContent.setFlexGrow(1, contentContainer);

  // Set up the AppLayout with no navbar - just content
  setContent(mainContent);
}

private void showServers() {
  contentContainer.removeAll();
  contentContainer.add(serversTab);
}

private void showDashboard() {
  contentContainer.removeAll();
  contentContainer.add(dashboardTab);
}

private void showDirectorySearch() {
  contentContainer.removeAll();
  contentContainer.add(directorySearchTab);
}

private void showSchema() {
  contentContainer.removeAll();
  contentContainer.add(schemaBrowser);
}

private void showReports() {
  contentContainer.removeAll();
  contentContainer.add(reportsTab);
}

private void showBulkOperations() {
  contentContainer.removeAll();
  contentContainer.add(bulkOperationsTab);
}

private void showLogs() {
  contentContainer.removeAll();
  contentContainer.add(logsTab);
}

@Override
public void onEnvironmentChange() {
  // Refresh tabs that depend on environment/server changes
  if (dashboardTab != null) {
    dashboardTab.refreshEnvironments();
  }
  if (directorySearchTab != null) {
    directorySearchTab.refreshEnvironments();
  }
  if (schemaBrowser != null) {
    schemaBrowser.refreshEnvironments();
  }
  if (reportsTab != null) {
    reportsTab.refreshEnvironments();
  }
  if (bulkOperationsTab != null) {
    bulkOperationsTab.refreshEnvironments();
  }
}

private void refreshServerList() {
  serversTab.refreshServerList();
  updateConnectionButtons();
}

private void updateConnectionButtons() {
  serversTab.updateConnectionButtons();
}

private void connectToServer(LdapServerConfig config) {
  if (config == null) {
    showError("Please select a server to connect to.");
    return;
  }

  try {
    ldapService.connect(config);
    updateConnectionButtons();

    // Update reports and bulk operations tabs with server config
    reportsTab.setServerConfig(config);
    bulkOperationsTab.setServerConfig(config);

    // Automatically load Root DSE and naming contexts for dashboard
    dashboardTab.loadRootDSEWithNamingContexts();

    showSuccess("Connected to " + config.getName());
  } catch (LDAPException e) {
  showError("Failed to connect: " + e.getMessage());
}
}

private void disconnectFromServer() {
  LdapServerConfig config = serversTab.getSelectedServer();
  if (config != null) {
    ldapService.disconnect(config.getId());
    updateConnectionButtons();

    directorySearchTab.clear();
    reportsTab.clear();
    bulkOperationsTab.clear();

    showInfo("Disconnected from " + config.getName());
  }
}

private void showSuccess(String message) {
  Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
  notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
}

private void showError(String message) {
  Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
  notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
}

private void showInfo(String message) {
  Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
  notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
}
}