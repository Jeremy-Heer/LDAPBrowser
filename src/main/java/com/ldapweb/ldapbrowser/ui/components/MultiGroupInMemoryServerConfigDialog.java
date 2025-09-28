package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Enhanced dialog for configuring UnboundID In-Memory Directory Servers with multiple group support.
 */
public class MultiGroupInMemoryServerConfigDialog extends Dialog {

  private final LdapServerConfig config;
  private final boolean isNew;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  // Form fields
  private TextField nameField;
  private IntegerField portField;
  private TextField baseDnField;
  private TextField bindDnField;
  private PasswordField passwordField;
  private Checkbox promptForPasswordCheckbox;
  private Checkbox useSSLCheckbox;
  private Checkbox generateTestDataCheckbox;

  // Group management components
  private MultiSelectListBox<String> groupListBox;
  private TextField newGroupField;
  private Button addGroupButton;

  private final List<Consumer<LdapServerConfig>> saveListeners = new ArrayList<>();

  public MultiGroupInMemoryServerConfigDialog(LdapServerConfig config,
                                            ConfigurationService configurationService,
                                            InMemoryLdapService inMemoryLdapService) {
    this.config = config != null ? config : new LdapServerConfig();
    this.isNew = config == null;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;

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
    setHeight("700px");
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

    baseDnField = new TextField("Base DN");
    baseDnField.setRequiredIndicatorVisible(true);
    baseDnField.setWidthFull();
    baseDnField.setPlaceholder("e.g., dc=example,dc=com");
    baseDnField.setHelperText("Root DN for the directory tree");

    bindDnField = new TextField("Admin DN");
    bindDnField.setRequiredIndicatorVisible(true);
    bindDnField.setWidthFull();
    bindDnField.setPlaceholder("e.g., cn=admin,dc=example,dc=com");
    bindDnField.setHelperText("Administrator bind DN");

    passwordField = new PasswordField("Admin Password");
    passwordField.setRequiredIndicatorVisible(true);
    passwordField.setWidthFull();
    passwordField.setHelperText("Password for administrator account");

    promptForPasswordCheckbox = new Checkbox("Prompt for admin password");
    promptForPasswordCheckbox.getStyle().set("font-size", "var(--lumo-font-size-s)");
    promptForPasswordCheckbox.addValueChangeListener(e -> {
      boolean promptEnabled = e.getValue();
      passwordField.setVisible(!promptEnabled);
      passwordField.setRequiredIndicatorVisible(!promptEnabled);
      if (promptEnabled) {
        passwordField.clear();
      }
    });

    useSSLCheckbox = new Checkbox("Enable SSL/TLS");

    generateTestDataCheckbox = new Checkbox("Generate Test Data");
    generateTestDataCheckbox.setValue(true);

    // Group management components
    groupListBox = new MultiSelectListBox<>();
    groupListBox.setHeight("150px");
    groupListBox.setWidthFull();
    groupListBox.setRenderer(new ComponentRenderer<>(this::createGroupItem));

    newGroupField = new TextField();
    newGroupField.setPlaceholder("Enter new group name");
    newGroupField.setWidthFull();

    addGroupButton = new Button("Add Group", new Icon(VaadinIcon.PLUS));
    addGroupButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    addGroupButton.addClickListener(e -> addNewGroup());

    // Enable adding group by pressing Enter
    newGroupField.addKeyPressListener(e -> {
      if (e.getKey().getKeys().contains("Enter")) {
        addNewGroup();
      }
    });

    populateAvailableGroups();
  }

  private void setupLayout() {
    VerticalLayout mainLayout = new VerticalLayout();
    mainLayout.setPadding(false);
    mainLayout.setSpacing(true);

    // Server configuration section
    H4 serverTitle = new H4("Server Configuration");
    serverTitle.getStyle().set("margin", "0");

    FormLayout serverForm = new FormLayout();
    serverForm.setWidthFull();
    serverForm.add(
        nameField,
        portField,
        baseDnField,
        bindDnField,
        passwordField,
        promptForPasswordCheckbox,
        useSSLCheckbox,
        generateTestDataCheckbox);

    serverForm.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("400px", 2));

    serverForm.setColspan(promptForPasswordCheckbox, 2);
    serverForm.setColspan(useSSLCheckbox, 1);
    serverForm.setColspan(generateTestDataCheckbox, 1);

    // Group management section
    VerticalLayout groupSection = new VerticalLayout();
    groupSection.setPadding(false);
    groupSection.setSpacing(true);

    H4 groupTitle = new H4("Group Membership");
    groupTitle.getStyle().set("margin", "0");

    Span groupDescription = new Span("Select multiple groups this server belongs to. Servers appear in all selected groups in the navigation menu.");
    groupDescription.getStyle().set("font-size", "var(--lumo-font-size-s)")
                              .set("color", "var(--lumo-secondary-text-color)");

    HorizontalLayout addGroupLayout = new HorizontalLayout();
    addGroupLayout.setSpacing(true);
    addGroupLayout.setWidthFull();
    addGroupLayout.add(newGroupField, addGroupButton);
    addGroupLayout.setFlexGrow(1, newGroupField);

    groupSection.add(groupTitle, groupDescription, groupListBox, addGroupLayout);

    mainLayout.add(serverTitle, serverForm, groupSection);
    add(mainLayout);

    // Footer buttons
    Button saveButton = new Button("Save", e -> save());
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> close());

    getFooter().add(cancelButton, saveButton);
  }

  private HorizontalLayout createGroupItem(String groupName) {
    HorizontalLayout layout = new HorizontalLayout();
    layout.setSpacing(true);
    layout.setWidthFull();
    layout.setDefaultVerticalComponentAlignment(VerticalLayout.Alignment.CENTER);

    Icon groupIcon = new Icon(VaadinIcon.FOLDER_OPEN);
    groupIcon.setSize("16px");
    groupIcon.getStyle().set("color", "var(--lumo-primary-color)");

    Span groupLabel = new Span(groupName);
    
    layout.add(groupIcon, groupLabel);
    return layout;
  }

  private void populateAvailableGroups() {
    Set<String> allGroups = new TreeSet<>();
    
    // Get groups from external servers
    if (configurationService != null) {
      configurationService.getAllConfigurations().forEach(cfg -> {
        cfg.getGroups().forEach(group -> {
          if (!group.trim().isEmpty()) {
            allGroups.add(group.trim());
          }
        });
      });
    }

    // Get groups from internal servers
    if (inMemoryLdapService != null) {
      inMemoryLdapService.getAllInMemoryServers().forEach(cfg -> {
        cfg.getGroups().forEach(group -> {
          if (!group.trim().isEmpty()) {
            allGroups.add(group.trim());
          }
        });
      });
    }

    groupListBox.setItems(allGroups);
  }

  private void addNewGroup() {
    String newGroup = newGroupField.getValue();
    if (newGroup != null && !newGroup.trim().isEmpty()) {
      String trimmedGroup = newGroup.trim();
      
      // Add to available groups if not already present
      Set<String> currentItems = new HashSet<>(groupListBox.getListDataView().getItems().toList());
      if (!currentItems.contains(trimmedGroup)) {
        currentItems.add(trimmedGroup);
        groupListBox.setItems(new TreeSet<>(currentItems));
      }
      
      // Select the new group
      groupListBox.select(trimmedGroup);
      
      // Clear the input field
      newGroupField.clear();
      newGroupField.focus();
    }
  }

  private void populateFields() {
    if (!isNew) {
      nameField.setValue(config.getName() != null ? config.getName() : "");
      portField.setValue(config.getPort());
      baseDnField.setValue(config.getBaseDn() != null ? config.getBaseDn() : "");
      bindDnField.setValue(config.getBindDn() != null ? config.getBindDn() : "");
      passwordField.setValue(config.getPassword() != null ? config.getPassword() : "");
      promptForPasswordCheckbox.setValue(config.isPromptForPassword());
      useSSLCheckbox.setValue(config.isUseSSL());
      
      // Select the groups this server belongs to
      config.getGroups().forEach(groupListBox::select);
    } else {
      // Set defaults for new servers
      baseDnField.setValue("dc=example,dc=com");
      bindDnField.setValue("cn=admin,dc=example,dc=com");
      passwordField.setValue("admin");
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

    if (bindDnField.getValue() == null || bindDnField.getValue().trim().isEmpty()) {
      bindDnField.setInvalid(true);
      bindDnField.setErrorMessage("Admin DN is required");
      return;
    }

    // Validate password field only if not prompting for password
    if (!promptForPasswordCheckbox.getValue() && 
        (passwordField.getValue() == null || passwordField.getValue().trim().isEmpty())) {
      passwordField.setInvalid(true);
      passwordField.setErrorMessage("Admin password is required");
      return;
    }

    // Clear validation errors
    nameField.setInvalid(false);
    baseDnField.setInvalid(false);
    bindDnField.setInvalid(false);
    passwordField.setInvalid(false);

    // Update config
    config.setName(nameField.getValue().trim());
    config.setHost("localhost"); // Always localhost for in-memory servers
    config.setPort(portField.getValue());
    config.setBaseDn(baseDnField.getValue().trim());
    config.setBindDn(bindDnField.getValue().trim());
    config.setPassword(passwordField.getValue());
    config.setPromptForPassword(promptForPasswordCheckbox.getValue());
    config.setUseSSL(useSSLCheckbox.getValue());
    config.setUseStartTLS(false); // Not applicable for in-memory servers

    // Update groups
    Set<String> selectedGroups = new HashSet<>(groupListBox.getSelectedItems());
    config.setGroups(selectedGroups);

    // Fire save event
    saveListeners.forEach(listener -> listener.accept(config));
  }

  private int findAvailablePort() {
    int basePort = 10389;
    for (int port = basePort; port < basePort + 100; port++) {
      if (isPortAvailable(port)) {
        return port;
      }
    }
    return basePort; // Fallback
  }

  private boolean isPortAvailable(int port) {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public void addSaveListener(Consumer<LdapServerConfig> listener) {
    saveListeners.add(listener);
  }
}
