package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for managing LDAP server connections
 */
public class ConnectionDialog extends Dialog {

  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  private Grid<LdapServerConfig> serverGrid;
  private Button addButton;
  private Button editButton;
  private Button deleteButton;
  private Button testButton;

  private final List<Consumer<LdapServerConfig>> saveListeners = new ArrayList<>();

  public ConnectionDialog(ConfigurationService configurationService, 
                         InMemoryLdapService inMemoryLdapService) {
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;

    initializeComponents();
    setupLayout();
    refreshGrid();
  }

  private void initializeComponents() {
    setHeaderTitle("Manage LDAP Servers");
    setWidth("800px");
    setHeight("600px");
    setResizable(true);

    serverGrid = new Grid<>(LdapServerConfig.class, false);
    serverGrid.setSizeFull();

    // Configure grid columns
    serverGrid.addColumn(LdapServerConfig::getName)
        .setHeader("Name")
        .setFlexGrow(1);

    serverGrid.addColumn(config -> config.getHost() + ":" + config.getPort())
        .setHeader("Host:Port")
        .setFlexGrow(1);

    serverGrid.addColumn(LdapServerConfig::getBindDn)
        .setHeader("Bind DN")
        .setFlexGrow(2);

    serverGrid.addColumn(new ComponentRenderer<>(this::createSecurityInfo))
        .setHeader("Security")
        .setFlexGrow(0)
        .setWidth("100px");

    serverGrid.addSelectionListener(event -> updateButtons());

    addButton = new Button("Add", new Icon(VaadinIcon.PLUS));
    addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addButton.addClickListener(e -> openServerDialog(null));

    editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
    editButton.addClickListener(e -> {
      serverGrid.getSelectedItems().stream()
          .findFirst()
          .ifPresent(this::openServerDialog);
    });
    editButton.setEnabled(false);

    deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH));
    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(e -> {
      serverGrid.getSelectedItems().stream()
          .findFirst()
          .ifPresent(this::confirmDelete);
    });
    deleteButton.setEnabled(false);

    testButton = new Button("Test", new Icon(VaadinIcon.CHECK_CIRCLE));
    testButton.addClickListener(e -> {
      serverGrid.getSelectedItems().stream()
          .findFirst()
          .ifPresent(this::testConnection);
    });
    testButton.setEnabled(false);
  }

  private void setupLayout() {
    VerticalLayout layout = new VerticalLayout();
    layout.setSizeFull();
    layout.setPadding(false);

    // Buttons layout
    HorizontalLayout buttonLayout = new HorizontalLayout();
    buttonLayout.add(addButton, editButton, deleteButton, testButton);

    layout.add(buttonLayout, serverGrid);
    layout.setFlexGrow(1, serverGrid);

    add(layout);

    // Footer buttons
    Button closeButton = new Button("Close", e -> close());
    getFooter().add(closeButton);
  }

  private VerticalLayout createSecurityInfo(LdapServerConfig config) {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(false);

    if (config.isUseSSL()) {
      layout.add(new Icon(VaadinIcon.LOCK));
    } else if (config.isUseStartTLS()) {
      layout.add(new Icon(VaadinIcon.SHIELD));
    } else {
      layout.add(new Icon(VaadinIcon.UNLOCK));
    }

    return layout;
  }

  private void updateButtons() {
    boolean hasSelection = !serverGrid.getSelectedItems().isEmpty();
    editButton.setEnabled(hasSelection);
    deleteButton.setEnabled(hasSelection);
    testButton.setEnabled(hasSelection);
  }

  private void refreshGrid() {
    serverGrid.setItems(configurationService.getAllConfigurations());
  }

  private void openServerDialog(LdapServerConfig config) {
    MultiGroupServerConfigDialog dialog = new MultiGroupServerConfigDialog(
        config, configurationService, inMemoryLdapService);
    dialog.addSaveListener(savedConfig -> {
      configurationService.saveConfiguration(savedConfig);
      refreshGrid();
      fireServerSaved(savedConfig);
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
      refreshGrid();
      showSuccess("Server configuration deleted.");
    });
    dialog.open();
  }

  private void testConnection(LdapServerConfig config) {
    // This would require injecting the LdapService, which we'll skip for now
    showInfo("Connection test would be performed here.");
  }

  public void addSaveListener(Consumer<LdapServerConfig> listener) {
    saveListeners.add(listener);
  }

  private void fireServerSaved(LdapServerConfig config) {
    saveListeners.forEach(listener -> listener.accept(config));
  }

  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showInfo(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
  }
}