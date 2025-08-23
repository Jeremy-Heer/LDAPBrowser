package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.example.ldapbrowser.ui.components.BulkOperationsTab;
import com.example.ldapbrowser.ui.components.DashboardTab;
import com.example.ldapbrowser.ui.components.DirectorySearchTab;
import com.example.ldapbrowser.ui.components.ExportTab;
import com.example.ldapbrowser.ui.components.SchemaBrowser;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import java.util.Optional;

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
    private ExportTab reportsTab;
    private BulkOperationsTab bulkOperationsTab;

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

    Tab directorySearchTabComponent = new Tab("Directory Search");
    directorySearchTab = new DirectorySearchTab(ldapService, configurationService, inMemoryLdapService, selectionService);
        tabSheet.add(directorySearchTabComponent, directorySearchTab);

        Tab dashboardTabComponent = new Tab("LDAP Browser");
    dashboardTab = new DashboardTab(ldapService, configurationService, inMemoryLdapService, selectionService);
        tabSheet.add(dashboardTabComponent, dashboardTab);

        Tab schemaTabComponent = new Tab("Schema");
    schemaBrowser = new SchemaBrowser(ldapService, configurationService, inMemoryLdapService, selectionService);
        tabSheet.add(schemaTabComponent, schemaBrowser);

        Tab reportsTabComponent = new Tab("Reports");
        reportsTab = new ExportTab(ldapService, loggingService, configurationService, inMemoryLdapService);
        tabSheet.add(reportsTabComponent, reportsTab);

        Tab bulkOperationsTabComponent = new Tab("Bulk Operations");
        bulkOperationsTab = new BulkOperationsTab(ldapService, loggingService, configurationService, inMemoryLdapService);
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
            Notification n = Notification.show("Select a server from the drawer to begin", 3000, Notification.Position.TOP_END);
            n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        try {
            if (!ldapService.isConnected(config.getId())) {
                ldapService.connect(config);
            }
        } catch (Exception e) {
            Notification n = Notification.show("Failed to connect: " + e.getMessage(), 5000, Notification.Position.TOP_END);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

    // Update tabs that need the server config explicitly
    // LDAP Browser and Schema require the active server to be set
    dashboardTab.setServerConfig(config);
    schemaBrowser.setServerConfig(config);
        reportsTab.setServerConfig(config);
        bulkOperationsTab.setServerConfig(config);

        // Trigger refreshes where needed
        dashboardTab.loadRootDSEWithNamingContexts();
    directorySearchTab.refreshEnvironments();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> sid = event.getLocation().getQueryParameters().getParameters().getOrDefault("sid", java.util.List.of()).stream().findFirst();
        if (sid.isPresent()) {
            String id = sid.get();
            // Try to resolve from external first
            LdapServerConfig cfg = configurationService.getAllConfigurations().stream()
                    .filter(c -> id.equals(c.getId()))
                    .findFirst()
                    .orElseGet(() -> inMemoryLdapService.getAllInMemoryServers().stream()
                            .filter(c -> id.equals(c.getId()))
                            .findFirst().orElse(null));
            if (cfg != null) {
                selectionService.setSelected(cfg);
            }
        }
    }
}
