package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.LdapService;
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
 * Connections tab for managing LDAP server connections
 */
public class ConnectionsTab extends VerticalLayout {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;

  // Server management controls
  private Grid<LdapServerConfig> serverGrid;
  private Button addServerButton;
  private Button editServerButton;
  private Button deleteServerButton;
  private Button testConnectionButton;

  // Event listeners
  private Consumer<LdapServerConfig> connectionListener;
  private Runnable disconnectionListener;

  public ConnectionsTab(LdapService ldapService, ConfigurationService configurationService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;

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

    serverGrid.addColumn(LdapServerConfig::getBindDn)
        .setHeader("Bind DN")
        .setFlexGrow(2)
        .setSortable(true);

    serverGrid.addColumn(new ComponentRenderer<>(this::createSecurityInfo))
        .setHeader("Security")
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

    testConnectionButton = new Button("Test", new Icon(VaadinIcon.CHECK_CIRCLE));
    testConnectionButton.addClickListener(e -> {
      serverGrid.getSelectedItems().stream()
          .findFirst()
          .ifPresent(this::testConnection);
    });
    testConnectionButton.setEnabled(false);
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("connections-tab");

    // Title with icon
    HorizontalLayout titleLayout = new HorizontalLayout();
    titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    titleLayout.setSpacing(true);

    Icon connectionIcon = new Icon(VaadinIcon.CONNECT);
    connectionIcon.setSize("20px");
    connectionIcon.getStyle().set("color", "#4a90e2");

    H3 title = new H3("LDAP Connections");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle().set("color", "#333");

    titleLayout.add(connectionIcon, title);

    // Server Management section (removed redundant connection section)
    VerticalLayout serverManagementSection = new VerticalLayout();
    serverManagementSection.setPadding(false);
    serverManagementSection.setSpacing(true);
    serverManagementSection.addClassName("server-management-section");

    H4 managementTitle = new H4("Manage LDAP Servers");
    managementTitle.addClassNames(LumoUtility.Margin.NONE);
    managementTitle.getStyle().set("color", "#333").set("margin-top", "10px");

    // Server management buttons layout
    HorizontalLayout managementButtonLayout = new HorizontalLayout();
    managementButtonLayout.setSpacing(true);
    managementButtonLayout.add(addServerButton, editServerButton, deleteServerButton, testConnectionButton);

    serverManagementSection.add(
        managementTitle,
        new Span("Manage LDAP server configurations and connections:"),
        managementButtonLayout,
        serverGrid);

    add(titleLayout, serverManagementSection);
    setFlexGrow(1, serverManagementSection);
  }

  public void refreshServerList() {
    serverGrid.setItems(configurationService.getAllConfigurations());
  }

  public void updateConnectionButtons() {
    // Refresh the grid to update connection status in action buttons
    serverGrid.getDataProvider().refreshAll();
  }

  private void updateServerManagementButtons() {
    boolean hasSelection = !serverGrid.getSelectedItems().isEmpty();
    editServerButton.setEnabled(hasSelection);
    deleteServerButton.setEnabled(hasSelection);
    testConnectionButton.setEnabled(hasSelection);
  }

  private VerticalLayout createSecurityInfo(LdapServerConfig config) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);

    if (config.isUseSSL()) {
      Icon icon = new Icon(VaadinIcon.LOCK);
      icon.getStyle().set("color", "green");
      layout.add(icon);
    } else if (config.isUseStartTLS()) {
      Icon icon = new Icon(VaadinIcon.SHIELD);
      icon.getStyle().set("color", "orange");
      layout.add(icon);
    } else {
      Icon icon = new Icon(VaadinIcon.UNLOCK);
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

    if (connected) {
      Button disconnectBtn = new Button("Disconnect", new Icon(VaadinIcon.CLOSE_CIRCLE));
      disconnectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
      disconnectBtn.addClickListener(e -> disconnectFromServer(config));
      actionsLayout.add(disconnectBtn);
    } else {
      Button connectBtn = new Button("Connect", new Icon(VaadinIcon.CONNECT));
      connectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
      connectBtn.addClickListener(e -> connectToServer(config));
      actionsLayout.add(connectBtn);
    }

    return actionsLayout;
  }

  private void openServerDialog(LdapServerConfig config) {
    ServerConfigDialog dialog = new ServerConfigDialog(config);
    dialog.addSaveListener(savedConfig -> {
      configurationService.saveConfiguration(savedConfig);
      refreshServerList();
      showSuccess(config == null ? "Server configuration added." : "Server configuration updated.");
      dialog.close();
    });
    dialog.open();
  }

  private void confirmDelete(LdapServerConfig config) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Delete Server Configuration");
    dialog.setText("Are you sure you want to delete the server configuration '" + config.getName() + "'?");
    dialog.setCancelable(true);
    dialog.setConfirmText("Delete");
    dialog.addConfirmListener(e -> {
      configurationService.deleteConfiguration(config.getId());
      refreshServerList();
      showSuccess("Server configuration deleted.");
    });
    dialog.open();
  }

  private void testConnection(LdapServerConfig config) {
    // Simple test - try to connect temporarily
    try {
      ldapService.connect(config);
      ldapService.disconnect(config.getId());
      showSuccess("Connection test successful for " + config.getName());
    } catch (Exception e) {
      showError("Connection test failed for " + config.getName() + ": " + e.getMessage());
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
    // Return the currently connected server or null
    return configurationService.getAllConfigurations().stream()
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