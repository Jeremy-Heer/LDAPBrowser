package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.example.ldapbrowser.ui.components.BulkOperationsTab;
import com.example.ldapbrowser.ui.components.DashboardTab;
import com.example.ldapbrowser.ui.components.DirectorySearchTab;
import com.example.ldapbrowser.ui.components.ExportTab;
import com.example.ldapbrowser.ui.components.LogsTab;
import com.example.ldapbrowser.ui.components.SchemaBrowser;
import com.example.ldapbrowser.ui.components.ServersTab;
import com.example.ldapbrowser.ui.components.EnvironmentRefreshListener;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.unboundid.ldap.sdk.LDAPException;

@Route(value = "legacy", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Legacy")
@AnonymousAllowed
public class LegacyView extends VerticalLayout implements EnvironmentRefreshListener {

    private final LdapService ldapService;
    private final ConfigurationService configurationService;
    private final InMemoryLdapService inMemoryLdapService;
    private final LoggingService loggingService;

    private Tabs mainTabs;
    private ServersTab serversTab;
    private DashboardTab dashboardTab;
    private SchemaBrowser schemaBrowser;
    private ExportTab reportsTab;
    private BulkOperationsTab bulkOperationsTab;
    private LogsTab logsTab;
    private DirectorySearchTab directorySearchTab;
    private VerticalLayout contentContainer;

    public LegacyView(LdapService ldapService, ConfigurationService configurationService,
                      InMemoryLdapService inMemoryLdapService, LoggingService loggingService) {
        this.ldapService = ldapService;
        this.configurationService = configurationService;
        this.inMemoryLdapService = inMemoryLdapService;
        this.loggingService = loggingService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        initializeComponents();
        setupLayout();
        refreshServerList();
    }

    private void initializeComponents() {
        mainTabs = new Tabs();
        Tab directorySearchTabComponent = new Tab("Directory Search");
        Tab dashboardTabComponent = new Tab("LDAP Browser");
        Tab schemaTabComponent = new Tab("Schema");
        Tab reportsTabComponent = new Tab("Reports");
        Tab bulkOperationsTabComponent = new Tab("Bulk Operations");
        Tab logsTabComponent = new Tab("Logs");
        Tab serversTabComponent = new Tab("Settings");
        mainTabs.add(directorySearchTabComponent, dashboardTabComponent, schemaTabComponent,
                reportsTabComponent, bulkOperationsTabComponent, logsTabComponent, serversTabComponent);

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

        dashboardTab = new DashboardTab(ldapService, configurationService, inMemoryLdapService);
        directorySearchTab = new DirectorySearchTab(ldapService, configurationService, inMemoryLdapService);
        schemaBrowser = new SchemaBrowser(ldapService, configurationService, inMemoryLdapService);
        reportsTab = new ExportTab(ldapService, loggingService, configurationService, inMemoryLdapService);
        bulkOperationsTab = new BulkOperationsTab(ldapService, loggingService, configurationService, inMemoryLdapService);
        logsTab = new LogsTab(loggingService);

        contentContainer = new VerticalLayout();
        contentContainer.setSizeFull();
        contentContainer.setPadding(false);
        contentContainer.setSpacing(false);

        contentContainer.add(directorySearchTab);
    }

    private void setupLayout() {
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(false);
        mainContent.setSpacing(false);

        HorizontalLayout tabsHeader = new HorizontalLayout();
        tabsHeader.setWidthFull();
        tabsHeader.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        tabsHeader.setPadding(true);
        tabsHeader.addClassName("tabs-header");
        tabsHeader.add(mainTabs);

        mainContent.add(tabsHeader, contentContainer);
        mainContent.setFlexGrow(1, contentContainer);

        add(mainContent);
        setFlexGrow(1, mainContent);
    }

    private void showServers() {
        swapContent(serversTab);
    }

    private void showDashboard() {
        swapContent(dashboardTab);
    }

    private void showDirectorySearch() {
        swapContent(directorySearchTab);
    }

    private void showSchema() {
        swapContent(schemaBrowser);
    }

    private void showReports() {
        swapContent(reportsTab);
    }

    private void showBulkOperations() {
        swapContent(bulkOperationsTab);
    }

    private void showLogs() {
        swapContent(logsTab);
    }

    private void swapContent(Component c) {
        contentContainer.removeAll();
        contentContainer.add(c);
    }

    @Override
    public void onEnvironmentChange() {
        refreshEnvironmentDropdowns();
    }

    public void refreshEnvironmentDropdowns() {
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

            reportsTab.setServerConfig(config);
            bulkOperationsTab.setServerConfig(config);

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
