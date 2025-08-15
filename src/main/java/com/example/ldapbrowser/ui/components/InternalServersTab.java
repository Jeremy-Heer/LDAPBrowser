package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.function.Consumer;

/**
* Internal servers tab for managing UnboundID In-Memory Directory Servers
*/
public class InternalServersTab extends VerticalLayout {
  
  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final EnvironmentRefreshListener environmentRefreshListener;
  
  // Server management controls
  private Grid<LdapServerConfig> serverGrid;
  private Button addServerButton;
  private Button editServerButton;
  private Button deleteServerButton;
  private Button stopServerButton;
  
  // Event listeners
  private Consumer<LdapServerConfig> connectionListener;
  private Runnable disconnectionListener;
  
  public InternalServersTab(LdapService ldapService, ConfigurationService configurationService, 
               EnvironmentRefreshListener environmentRefreshListener,
               InMemoryLdapService inMemoryLdapService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.environmentRefreshListener = environmentRefreshListener;
    
    initializeComponents();
    setupLayout();
    refreshServerList();
  }
  
  private void initializeComponents() {
    // Server management components
    serverGrid = new Grid<>(LdapServerConfig.class, false);
    serverGrid.setHeight("400px");
    serverGrid.setWidthFull();
    
    // Configure grid columns
    serverGrid.addColumn(LdapServerConfig::getName)
      .setHeader("Name")
      .setFlexGrow(1)
      .setSortable(true);
    
    serverGrid.addColumn(config -> config.getHost() + ":" + config.getPort())
      .setHeader("Host:Port")
      .setFlexGrow(1)
      .setSortable(true);
    
    serverGrid.addColumn(LdapServerConfig::getBaseDn)
      .setHeader("Base DN")
      .setFlexGrow(2)
      .setSortable(true);
    
    serverGrid.addColumn(config -> "In-Memory")
      .setHeader("Type")
      .setFlexGrow(0)
      .setWidth("100px");
    
    // Add server status column
    serverGrid.addColumn(new ComponentRenderer<>(this::createServerStatus))
      .setHeader("Status")
      .setFlexGrow(0)
      .setWidth("100px");
    
    // Add connection actions column
    serverGrid.addColumn(new ComponentRenderer<>(this::createConnectionActions))
      .setHeader("Connection")
      .setFlexGrow(0)
      .setWidth("150px");
    
    serverGrid.addSelectionListener(event -> updateServerManagementButtons());
    
    addServerButton = new Button("Add Server", new Icon(VaadinIcon.PLUS));
    addServerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addServerButton.addClickListener(e -> openServerDialog(null));
    
    editServerButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
    editServerButton.addClickListener(e -> {
      serverGrid.getSelectedItems().stream()
        .findFirst()
        .ifPresent(this::openServerDialog);
    });
    editServerButton.setEnabled(false);
    
    deleteServerButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
    deleteServerButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    deleteServerButton.addClickListener(e -> {
      serverGrid.getSelectedItems().stream()
        .findFirst()
        .ifPresent(this::confirmDelete);
    });
    deleteServerButton.setEnabled(false);
    
    stopServerButton = new Button("Stop", new Icon(VaadinIcon.STOP));
    stopServerButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    stopServerButton.addClickListener(e -> {
      serverGrid.getSelectedItems().stream()
        .findFirst()
        .ifPresent(this::stopServer);
    });
    stopServerButton.setEnabled(false);
  }
  
  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("internal-servers-tab");
    
    // Title with icon
    HorizontalLayout titleLayout = new HorizontalLayout();
    titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    titleLayout.setSpacing(true);
    
    Icon serverIcon = new Icon(VaadinIcon.SERVER);
    serverIcon.setSize("20px");
    serverIcon.getStyle().set("color", "#ff6b35");
    
    H3 title = new H3("Internal LDAP Servers");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle().set("color", "#333");
    
    titleLayout.add(serverIcon, title);
    
    // Server Management section
    VerticalLayout serverManagementSection = new VerticalLayout();
    serverManagementSection.setPadding(false);
    serverManagementSection.setSpacing(true);
    serverManagementSection.addClassName("server-management-section");
    
    H4 managementTitle = new H4("Manage In-Memory LDAP Servers");
    managementTitle.addClassNames(LumoUtility.Margin.NONE);
    managementTitle.getStyle().set("color", "#333").set("margin-top", "10px");
    
    // Server management buttons layout
    HorizontalLayout managementButtonLayout = new HorizontalLayout();
    managementButtonLayout.setSpacing(true);
    managementButtonLayout.add(addServerButton, editServerButton, deleteServerButton, stopServerButton);
    
    serverManagementSection.add(
      managementTitle,
      new Span("Create and manage UnboundID In-Memory Directory Servers with test data:"),
      managementButtonLayout,
      serverGrid
    );
    
    add(titleLayout, serverManagementSection);
    setFlexGrow(1, serverManagementSection);
  }
  
  public void refreshServerList() {
    // For internal servers, we'll get them from a specific configuration category
    // or maintain them separately from external servers
    serverGrid.setItems(inMemoryLdapService.getAllInMemoryServers());
  }
  
  public void updateConnectionButtons() {
    // Refresh the grid to update connection status in action buttons
    serverGrid.getDataProvider().refreshAll();
  }
  
  private void updateServerManagementButtons() {
    boolean hasSelection = !serverGrid.getSelectedItems().isEmpty();
    editServerButton.setEnabled(hasSelection);
    deleteServerButton.setEnabled(hasSelection);
    
    // Enable stop button only if server is running
    if (hasSelection) {
      LdapServerConfig selected = serverGrid.getSelectedItems().iterator().next();
      stopServerButton.setEnabled(inMemoryLdapService.isServerRunning(selected.getId()));
    } else {
      stopServerButton.setEnabled(false);
    }
  }
  
  private VerticalLayout createServerStatus(LdapServerConfig config) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);
    
    boolean isRunning = inMemoryLdapService.isServerRunning(config.getId());
    
    if (isRunning) {
      Icon icon = new Icon(VaadinIcon.CHECK_CIRCLE);
      icon.getStyle().set("color", "green");
      layout.add(icon);
    } else {
      Icon icon = new Icon(VaadinIcon.CLOSE_CIRCLE);
      icon.getStyle().set("color", "red");
      layout.add(icon);
    }
    
    return layout;
  }
  
  private HorizontalLayout createConnectionActions(LdapServerConfig config) {
    HorizontalLayout actionsLayout = new HorizontalLayout();
    actionsLayout.setPadding(false);
    actionsLayout.setSpacing(true);
    
    boolean connected = ldapService.isConnected(config.getId());
    boolean serverRunning = inMemoryLdapService.isServerRunning(config.getId());
    
    if (connected) {
      Button disconnectBtn = new Button("Disconnect", new Icon(VaadinIcon.CLOSE_CIRCLE));
      disconnectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
      disconnectBtn.addClickListener(e -> disconnectFromServer(config));
      actionsLayout.add(disconnectBtn);
    } else if (serverRunning) {
      Button connectBtn = new Button("Connect", new Icon(VaadinIcon.CONNECT));
      connectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
      connectBtn.addClickListener(e -> connectToServer(config));
      actionsLayout.add(connectBtn);
    } else {
      Button startBtn = new Button("Start", new Icon(VaadinIcon.PLAY));
      startBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
      startBtn.addClickListener(e -> startServer(config));
      actionsLayout.add(startBtn);
    }
    
    return actionsLayout;
  }
  
  private void openServerDialog(LdapServerConfig config) {
    InMemoryServerConfigDialog dialog = new InMemoryServerConfigDialog(config);
    dialog.addSaveListener(savedConfig -> {
      inMemoryLdapService.saveInMemoryServer(savedConfig);
      refreshServerList();
      showSuccess(config == null ? "In-Memory server configuration added." : "In-Memory server configuration updated.");
      dialog.close();
    });
    dialog.open();
  }
  
  private void confirmDelete(LdapServerConfig config) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete In-Memory Server");
    dialog.setText("Are you sure you want to delete the in-memory server '" + config.getName() + "'? This will stop the server if it's running.");
    dialog.setCancelable(true);
    dialog.setConfirmText("Delete");
    dialog.addConfirmListener(e -> {
      // Stop server if running
      if (inMemoryLdapService.isServerRunning(config.getId())) {
        inMemoryLdapService.stopServer(config.getId());
      }
      inMemoryLdapService.deleteInMemoryServer(config.getId());
      refreshServerList();
      showSuccess("In-Memory server deleted.");
    });
    dialog.open();
  }
  
  private void startServer(LdapServerConfig config) {
    try {
      inMemoryLdapService.startServer(config);
      refreshServerList();
      
      // Notify environment dropdowns about the change
      if (environmentRefreshListener != null) {
        environmentRefreshListener.onEnvironmentChange();
      }
      
      showSuccess("In-Memory server '" + config.getName() + "' started successfully.");
    } catch (Exception e) {
      showError("Failed to start server '" + config.getName() + "': " + e.getMessage());
    }
  }
  
  private void stopServer(LdapServerConfig config) {
    try {
      // Disconnect first if connected
      if (ldapService.isConnected(config.getId())) {
        disconnectFromServer(config);
      }
      
      inMemoryLdapService.stopServer(config.getId());
      refreshServerList();
      
      // Notify environment dropdowns about the change
      if (environmentRefreshListener != null) {
        environmentRefreshListener.onEnvironmentChange();
      }
      
      showSuccess("In-Memory server '" + config.getName() + "' stopped.");
    } catch (Exception e) {
      showError("Failed to stop server '" + config.getName() + "': " + e.getMessage());
    }
  }
  
  private void connectToServer(LdapServerConfig config) {
    if (config != null && connectionListener != null) {
      connectionListener.accept(config);
    }
  }
  
  private void disconnectFromServer(LdapServerConfig config) {
    if (config != null && disconnectionListener != null) {
      // For disconnect, we need the current connected server, so use the callback
      disconnectionListener.run();
    }
  }
  
  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
  
  private void showError(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  public void setConnectionListener(Consumer<LdapServerConfig> listener) {
    this.connectionListener = listener;
  }
  
  public void setDisconnectionListener(Runnable listener) {
    this.disconnectionListener = listener;
  }
  
  public LdapServerConfig getSelectedServer() {
    // Return the currently connected in-memory server or null
    return inMemoryLdapService.getAllInMemoryServers().stream()
      .filter(config -> ldapService.isConnected(config.getId()))
      .findFirst()
      .orElse(null);
  }
  
  public void setSelectedServer(LdapServerConfig config) {
    // No longer needed since we don't have a combo box
    // Connection is handled through the grid actions
  }
  
  public void clear() {
    updateConnectionButtons();
  }
}
