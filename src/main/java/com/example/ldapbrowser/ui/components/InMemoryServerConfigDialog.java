package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
* Dialog for configuring UnboundID In-Memory Directory Servers
*/
public class InMemoryServerConfigDialog extends Dialog {

  private final LdapServerConfig config;
  private final boolean isNew;

  private TextField nameField;
  private IntegerField portField;
  private TextField groupField;
  private TextField baseDnField;
  private TextField bindDnField;
  private PasswordField passwordField;
  private Checkbox useSSLCheckbox;
  private Checkbox generateTestDataCheckbox;

  private final List<Consumer<LdapServerConfig>> saveListeners = new ArrayList<>();

  public InMemoryServerConfigDialog(LdapServerConfig config) {
    this.config = config != null ? config : new LdapServerConfig();
    this.isNew = config == null;

    if (isNew && this.config.getId() == null) {
      this.config.setId(UUID.randomUUID().toString());
      // Set default values for in-memory server
      this.config.setHost("localhost");
      this.config.setPort(findAvailablePort());
      this.config.setUseSSL(false);
      this.config.setUseStartTLS(false);
    }

    initializeComponents();
    setupLayout();
    populateFields();
  }

  private void initializeComponents() {
    setHeaderTitle(isNew ? "Add In-Memory LDAP Server" : "Edit In-Memory LDAP Server");
    setWidth("600px");
    setResizable(true);

    nameField = new TextField("Server Name");
    nameField.setRequiredIndicatorVisible(true);
    nameField.setWidthFull();
    nameField.setPlaceholder("e.g., Test LDAP Server");

    portField = new IntegerField("Port");
    portField.setValue(isNew ? findAvailablePort() : config.getPort());
    portField.setMin(1024);
    portField.setMax(65535);
    portField.setWidthFull();
    portField.setHelperText("Port for the in-memory LDAP server");

  groupField = new TextField("Group");
  groupField.setPlaceholder("Optional: Servers menu group");
  groupField.setWidthFull();

    baseDnField = new TextField("Base DN");
    baseDnField.setRequiredIndicatorVisible(true);
    baseDnField.setWidthFull();
    baseDnField.setPlaceholder("e.g., dc=example,dc=com");
    baseDnField.setHelperText("Root distinguished name for the directory tree");

    bindDnField = new TextField("Admin Bind DN");
    bindDnField.setWidthFull();
    bindDnField.setPlaceholder("e.g., cn=admin,dc=example,dc=com");
    bindDnField.setHelperText("Optional: DN for administrative bind (if empty, allows anonymous access)");

    passwordField = new PasswordField("Admin Password");
    passwordField.setWidthFull();
    passwordField.setHelperText("Password for the admin bind DN");

    useSSLCheckbox = new Checkbox("Use SSL/TLS");

    generateTestDataCheckbox = new Checkbox("Generate Test Data");
    generateTestDataCheckbox.setValue(true);
  }

  private void setupLayout() {
    VerticalLayout mainLayout = new VerticalLayout();
    mainLayout.setPadding(false);
    mainLayout.setSpacing(true);

    // Information section
    VerticalLayout infoSection = new VerticalLayout();
    infoSection.setPadding(false);
    infoSection.setSpacing(false);

    H4 infoTitle = new H4("In-Memory LDAP Server Configuration");
    infoTitle.getStyle().set("margin-top", "0");

    Span infoText = new Span("This will create a UnboundID In-Memory Directory Server that runs locally and can be used for testing purposes. The server will be automatically configured with the specified settings.");
    infoText.getStyle()
    .set("color", "#666")
    .set("font-size", "14px")
    .set("margin-bottom", "20px");

    infoSection.add(infoTitle, infoText);

    // Form layout
    FormLayout formLayout = new FormLayout();
    formLayout.setWidthFull();

    formLayout.add(
    nameField,
    portField,
  groupField,
    baseDnField,
    bindDnField,
    passwordField,
    useSSLCheckbox,
    generateTestDataCheckbox
    );

    // Set responsive steps
    formLayout.setResponsiveSteps(
    new FormLayout.ResponsiveStep("0", 1),
    new FormLayout.ResponsiveStep("400px", 2)
    );

    // Span checkboxes across columns
    formLayout.setColspan(useSSLCheckbox, 2);
    formLayout.setColspan(generateTestDataCheckbox, 2);

    mainLayout.add(infoSection, formLayout);
    add(mainLayout);

    // Footer buttons
    Button saveButton = new Button("Create Server", e -> save());
    if (!isNew) {
      saveButton.setText("Update Server");
    }
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(cancelButton, saveButton);
  }

  private void populateFields() {
    if (!isNew) {
      nameField.setValue(config.getName() != null ? config.getName() : "");
      portField.setValue(config.getPort());
  groupField.setValue(config.getGroup() != null ? config.getGroup() : "");
      baseDnField.setValue(config.getBaseDn() != null ? config.getBaseDn() : "");
      bindDnField.setValue(config.getBindDn() != null ? config.getBindDn() : "");
      passwordField.setValue(config.getPassword() != null ? config.getPassword() : "");
      useSSLCheckbox.setValue(config.isUseSSL());
    } else {
    // Set some reasonable defaults for new servers
    baseDnField.setValue("dc=example,dc=com");
    bindDnField.setValue("cn=admin,dc=example,dc=com");
    passwordField.setValue("admin123");
  }
}

private void save() {
  // Validate required fields
  if (nameField.getValue() == null || nameField.getValue().trim().isEmpty()) {
    nameField.setInvalid(true);
    nameField.setErrorMessage("Server name is required");
    return;
  }

  if (baseDnField.getValue() == null || baseDnField.getValue().trim().isEmpty()) {
    baseDnField.setInvalid(true);
    baseDnField.setErrorMessage("Base DN is required");
    return;
  }

  // Clear validation errors
  nameField.setInvalid(false);
  baseDnField.setInvalid(false);

  // Update config
  config.setName(nameField.getValue().trim());
  config.setHost("localhost"); // Always localhost for in-memory servers
  config.setPort(portField.getValue());
  config.setGroup(groupField.getValue() != null ? groupField.getValue().trim() : null);
  config.setBaseDn(baseDnField.getValue().trim());
  config.setBindDn(bindDnField.getValue());
  config.setPassword(passwordField.getValue());
  config.setUseSSL(useSSLCheckbox.getValue());
  config.setUseStartTLS(false); // Not applicable for in-memory servers

  // Fire save event
  saveListeners.forEach(listener -> listener.accept(config));
}

public void addSaveListener(Consumer<LdapServerConfig> listener) {
  saveListeners.add(listener);
}

/**
* Find an available port starting from 10389 (common LDAP test port)
*/
private int findAvailablePort() {
  return 10389; // For simplicity, we'll use a fixed port
  // In a real implementation, you might want to check for port availability
}
}