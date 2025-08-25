package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
* Dialog for managing LDAP server connections
*/
public class ConnectionDialog extends Dialog {

private final ConfigurationService configurationService;

private Grid<LdapServerConfig> serverGrid;
private Button addButton;
private Button editButton;
private Button deleteButton;
private Button testButton;

private final List<Consumer<LdapServerConfig>> saveListeners = new ArrayList<>();

public ConnectionDialog(ConfigurationService configurationService) {
this.configurationService = configurationService;

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
ServerConfigDialog dialog = new ServerConfigDialog(config);
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

/**
* Dialog for editing individual server configurations
*/
private static class ServerConfigDialog extends Dialog {

private final LdapServerConfig config;
private final boolean isNew;

private TextField nameField;
private TextField hostField;
private IntegerField portField;
private TextField bindDnField;
private PasswordField passwordField;
private Checkbox useSSLCheckbox;
private Checkbox useStartTLSCheckbox;
private TextField baseDnField;

private final List<Consumer<LdapServerConfig>> saveListeners = new ArrayList<>();

public ServerConfigDialog(LdapServerConfig config) {
this.config = config != null ? config : new LdapServerConfig();
this.isNew = config == null;

if (isNew && this.config.getId() == null) {
this.config.setId(UUID.randomUUID().toString());
}

initializeComponents();
setupLayout();
populateFields();
}

private void initializeComponents() {
setHeaderTitle(isNew ? "Add LDAP Server" : "Edit LDAP Server");
setWidth("500px");
setResizable(true);

nameField = new TextField("Name");
nameField.setRequiredIndicatorVisible(true);
nameField.setWidthFull();

hostField = new TextField("Host");
hostField.setRequiredIndicatorVisible(true);
hostField.setWidthFull();

portField = new IntegerField("Port");
portField.setValue(389);
portField.setMin(1);
portField.setMax(65535);
portField.setWidthFull();

bindDnField = new TextField("Bind DN");
bindDnField.setWidthFull();
bindDnField.setPlaceholder("e.g., cn=admin,dc=example,dc=com");

passwordField = new PasswordField("Password");
passwordField.setWidthFull();

useSSLCheckbox = new Checkbox("Use SSL");
useSSLCheckbox.addValueChangeListener(e -> {
if (e.getValue()) {
 useStartTLSCheckbox.setValue(false);
 if (portField.getValue() == 389) {
 portField.setValue(636);
 }
} else if (portField.getValue() == 636) {
portField.setValue(389);
}
});

useStartTLSCheckbox = new Checkbox("Use StartTLS");
useStartTLSCheckbox.addValueChangeListener(e -> {
if (e.getValue()) {
useSSLCheckbox.setValue(false);
}
});

baseDnField = new TextField("Base DN");
baseDnField.setWidthFull();
baseDnField.setPlaceholder("e.g., dc=example,dc=com");
}

private void setupLayout() {
FormLayout formLayout = new FormLayout();
formLayout.setWidthFull();

formLayout.add(
nameField,
hostField,
portField,
bindDnField,
passwordField,
useSSLCheckbox,
useStartTLSCheckbox,
baseDnField
);

// Set responsive steps
formLayout.setResponsiveSteps(
new FormLayout.ResponsiveStep("0", 1),
new FormLayout.ResponsiveStep("400px", 2)
);

// Span security checkboxes across columns
formLayout.setColspan(useSSLCheckbox, 1);
formLayout.setColspan(useStartTLSCheckbox, 1);

add(formLayout);

// Footer buttons
Button saveButton = new Button("Save", e -> save());
saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

Button cancelButton = new Button("Cancel", e -> close());

getFooter().add(cancelButton, saveButton);
}

private void populateFields() {
if (!isNew) {
nameField.setValue(config.getName() != null ? config.getName() : "");
hostField.setValue(config.getHost() != null ? config.getHost() : "");
portField.setValue(config.getPort());
bindDnField.setValue(config.getBindDn() != null ? config.getBindDn() : "");
passwordField.setValue(config.getPassword() != null ? config.getPassword() : "");
useSSLCheckbox.setValue(config.isUseSSL());
useStartTLSCheckbox.setValue(config.isUseStartTLS());
baseDnField.setValue(config.getBaseDn() != null ? config.getBaseDn() : "");
}
}

private void save() {
// Validate required fields
if (nameField.getValue() == null || nameField.getValue().trim().isEmpty()) {
nameField.setInvalid(true);
nameField.setErrorMessage("Name is required");
return;
}

if (hostField.getValue() == null || hostField.getValue().trim().isEmpty()) {
hostField.setInvalid(true);
hostField.setErrorMessage("Host is required");
return;
}

// Clear validation errors
nameField.setInvalid(false);
hostField.setInvalid(false);

// Update config
config.setName(nameField.getValue().trim());
config.setHost(hostField.getValue().trim());
config.setPort(portField.getValue());
config.setBindDn(bindDnField.getValue());
config.setPassword(passwordField.getValue());
config.setUseSSL(useSSLCheckbox.getValue());
config.setUseStartTLS(useStartTLSCheckbox.getValue());
config.setBaseDn(baseDnField.getValue());

// Fire save event
saveListeners.forEach(listener -> listener.accept(config));
}

public void addSaveListener(Consumer<LdapServerConfig> listener) {
saveListeners.add(listener);
}
}
}