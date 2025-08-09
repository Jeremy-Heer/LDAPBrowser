package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.ui.components.ServersTab;
import com.example.ldapbrowser.ui.components.DashboardTab;
import com.example.ldapbrowser.ui.components.SchemaBrowser;
import com.example.ldapbrowser.ui.components.ExportTab;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * Main LDAP Browser UI with tabbed interface - Apache Directory Studio inspired layout
 */
@Route("")
@PageTitle("LDAP Browser")
public class MainView extends AppLayout {
    
    private final LdapService ldapService;
    private final ConfigurationService configurationService;
    
    // Main content components
    private Tabs mainTabs;
    private ServersTab serversTab;
    private DashboardTab dashboardTab;
    private SchemaBrowser schemaBrowser;
    private ExportTab exportTab;
    private VerticalLayout contentContainer;
    
    // Global status display
    private Span connectionStatus;
    private HorizontalLayout statusContainer;
    
    public MainView(LdapService ldapService, ConfigurationService configurationService) {
        this.ldapService = ldapService;
        this.configurationService = configurationService;
        
        initializeComponents();
        setupLayout();
        refreshServerList();
    }
    
    private void initializeComponents() {
        // Global connection status display
        connectionStatus = new Span("Disconnected");
        connectionStatus.getStyle()
            .set("background-color", "#f44336")  // Red background for disconnected
            .set("color", "white")
            .set("padding", "4px 12px")
            .set("border-radius", "12px")
            .set("font-weight", "bold")
            .set("font-size", "0.9em");
        
        statusContainer = new HorizontalLayout();
        statusContainer.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        statusContainer.setSpacing(true);
        statusContainer.add(new Span("Status:"), connectionStatus);
        statusContainer.getStyle()
            .set("position", "fixed")
            .set("top", "10px")
            .set("right", "20px")
            .set("z-index", "1000");
        
        // Main tabs - Servers tab first
        mainTabs = new Tabs();
        Tab serversTabComponent = new Tab("Servers");
        Tab dashboardTabComponent = new Tab("Dashboard");
        Tab schemaTabComponent = new Tab("Schema");
        Tab exportTabComponent = new Tab("Export");
        mainTabs.add(serversTabComponent, dashboardTabComponent, schemaTabComponent, exportTabComponent);
        
        mainTabs.addSelectedChangeListener(e -> {
            Tab selectedTab = e.getSelectedTab();
            if (selectedTab == serversTabComponent) {
                showServers();
            } else if (selectedTab == dashboardTabComponent) {
                showDashboard();
            } else if (selectedTab == schemaTabComponent) {
                showSchema();
            } else if (selectedTab == exportTabComponent) {
                showExport();
            }
        });
        
        // Tab content components
        serversTab = new ServersTab(ldapService, configurationService);
        serversTab.setConnectionListener(this::connectToServer);
        serversTab.setDisconnectionListener(this::disconnectFromServer);
        
        dashboardTab = new DashboardTab(ldapService);
        schemaBrowser = new SchemaBrowser(ldapService);
        exportTab = new ExportTab(ldapService);
        
        // Content container
        contentContainer = new VerticalLayout();
        contentContainer.setSizeFull();
        contentContainer.setPadding(false);
        contentContainer.setSpacing(false);
        
        // Initially show servers tab
        contentContainer.add(serversTab);
    }
    
    private void setupLayout() {
        // Create main content layout with tabs and connection status
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(false);
        mainContent.setSpacing(false);
        
        // Tabs header without connection status
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
        
        // Add global status container to the UI (positioned fixed)
        getElement().appendChild(statusContainer.getElement());
    }
    
    private void showServers() {
        contentContainer.removeAll();
        contentContainer.add(serversTab);
    }
    
    private void showDashboard() {
        contentContainer.removeAll();
        contentContainer.add(dashboardTab);
    }
    
    private void showSchema() {
        contentContainer.removeAll();
        contentContainer.add(schemaBrowser);
    }
    
    private void showExport() {
        contentContainer.removeAll();
        contentContainer.add(exportTab);
    }
    
    private void refreshServerList() {
        serversTab.refreshServerList();
        updateConnectionButtons();
    }
    
    private void updateConnectionButtons() {
        LdapServerConfig selected = serversTab.getSelectedServer();
        boolean connected = selected != null && ldapService.isConnected(selected.getId());
        
        serversTab.updateConnectionButtons();
        
        // Update the global status display
        updateGlobalConnectionStatus(selected, connected);
    }
    
    private void updateGlobalConnectionStatus(LdapServerConfig selected, boolean connected) {
        if (connected && selected != null) {
            connectionStatus.setText("Connected to " + selected.getName());
            connectionStatus.getStyle()
                .set("background-color", "#4caf50")  // Green background for connected
                .set("color", "white");
        } else {
            connectionStatus.setText("Disconnected");
            connectionStatus.getStyle()
                .set("background-color", "#f44336")  // Red background for disconnected
                .set("color", "white");
        }
    }
    
    private void connectToServer(LdapServerConfig config) {
        if (config == null) {
            showError("Please select a server to connect to.");
            return;
        }
        
        try {
            ldapService.connect(config);
            updateConnectionButtons();
            
            // Update both dashboard and schema tabs with server config
            dashboardTab.setServerConfig(config);
            schemaBrowser.setServerConfig(config);
            exportTab.setServerConfig(config);
            
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
            
            dashboardTab.clear();
            schemaBrowser.clear();
            exportTab.clear();
            
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
