package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dialog for editing individual LDAP server configurations
 */
public class ServerConfigDialog extends Dialog {
    
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
