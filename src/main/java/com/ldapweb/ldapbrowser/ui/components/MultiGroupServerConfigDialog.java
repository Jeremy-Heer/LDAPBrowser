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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Enhanced server configuration dialog with support for multiple group membership.
 */
public class MultiGroupServerConfigDialog extends Dialog {

  private final LdapServerConfig config;
  private final boolean isNew;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  // Basic server configuration fields
  private TextField nameField;
  private TextField hostField;
  private IntegerField portField;
  private TextField bindDnField;
  private PasswordField passwordField;
  private Checkbox useSslCheckbox;
  private Checkbox useStartTlsCheckbox;
  private TextField baseDnField;

  // Group management components
  private MultiSelectListBox<String> groupListBox;
  private TextField newGroupField;
  private Button addGroupButton;

  private final List<Consumer<LdapServerConfig>> saveListeners = new ArrayList<>();

  /**
   * Constructs a new dialog for configuring an LDAP server with multiple group support.
   *
   * @param config the server configuration to edit, or null to create a new one
   * @param configurationService the configuration service for external servers
   * @param inMemoryLdapService the service for internal servers
   */
  public MultiGroupServerConfigDialog(LdapServerConfig config, 
                                    ConfigurationService configurationService,
                                    InMemoryLdapService inMemoryLdapService) {
    this.config = config != null ? config : new LdapServerConfig();
    this.isNew = config == null;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;

    if (isNew && this.config.getId() == null) {
      this.config.setId(UUID.randomUUID().toString());
    }

    initializeComponents();
    setupLayout();
    populateFields();
  }

  private void initializeComponents() {
    setHeaderTitle(isNew ? "Add LDAP Server" : "Edit LDAP Server");
    setWidth("600px");
    setHeight("700px");
    setResizable(true);

    // Basic server configuration fields
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

    useSslCheckbox = new Checkbox("Use SSL");
    useSslCheckbox.addValueChangeListener(e -> {
      if (e.getValue()) {
        useStartTlsCheckbox.setValue(false);
        if (portField.getValue() == 389) {
          portField.setValue(636);
        }
      } else if (portField.getValue() == 636) {
        portField.setValue(389);
      }
    });

    useStartTlsCheckbox = new Checkbox("Use StartTLS");
    useStartTlsCheckbox.addValueChangeListener(e -> {
      if (e.getValue()) {
        useSslCheckbox.setValue(false);
      }
    });

    baseDnField = new TextField("Base DN");
    baseDnField.setWidthFull();
    baseDnField.setPlaceholder("e.g., dc=example,dc=com");

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

    // Basic configuration form
    FormLayout basicForm = new FormLayout();
    basicForm.setWidthFull();
    basicForm.add(
        nameField,
        hostField,
        portField,
        bindDnField,
        passwordField,
        useSslCheckbox,
        useStartTlsCheckbox,
        baseDnField);

    basicForm.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1),
        new FormLayout.ResponsiveStep("400px", 2));

    basicForm.setColspan(useSslCheckbox, 1);
    basicForm.setColspan(useStartTlsCheckbox, 1);

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

    mainLayout.add(basicForm, groupSection);

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
    configurationService.getAllConfigurations().forEach(cfg -> {
      cfg.getGroups().forEach(group -> {
        if (!group.trim().isEmpty()) {
          allGroups.add(group.trim());
        }
      });
    });

    // Get groups from internal servers
    inMemoryLdapService.getAllInMemoryServers().forEach(cfg -> {
      cfg.getGroups().forEach(group -> {
        if (!group.trim().isEmpty()) {
          allGroups.add(group.trim());
        }
      });
    });

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
      Set<String> selectedGroups = new HashSet<>(groupListBox.getSelectedItems());
      selectedGroups.add(trimmedGroup);
      groupListBox.select(trimmedGroup);
      
      // Clear the input field
      newGroupField.clear();
      newGroupField.focus();
    }
  }

  private void populateFields() {
    if (!isNew) {
      nameField.setValue(config.getName() != null ? config.getName() : "");
      hostField.setValue(config.getHost() != null ? config.getHost() : "");
      portField.setValue(config.getPort());
      bindDnField.setValue(config.getBindDn() != null ? config.getBindDn() : "");
      passwordField.setValue(config.getPassword() != null ? config.getPassword() : "");
      useSslCheckbox.setValue(config.isUseSSL());
      useStartTlsCheckbox.setValue(config.isUseStartTLS());
      baseDnField.setValue(config.getBaseDn() != null ? config.getBaseDn() : "");
      
      // Select the groups this server belongs to
      config.getGroups().forEach(groupListBox::select);
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

    // Update basic config
    config.setName(nameField.getValue().trim());
    config.setHost(hostField.getValue().trim());
    config.setPort(portField.getValue());
    config.setBindDn(bindDnField.getValue());
    config.setPassword(passwordField.getValue());
    config.setUseSSL(useSslCheckbox.getValue());
    config.setUseStartTLS(useStartTlsCheckbox.getValue());
    config.setBaseDn(baseDnField.getValue());

    // Update groups
    Set<String> selectedGroups = new HashSet<>(groupListBox.getSelectedItems());
    config.setGroups(selectedGroups);

    // Fire save event
    saveListeners.forEach(listener -> listener.accept(config));
  }

  public void addSaveListener(Consumer<LdapServerConfig> listener) {
    saveListeners.add(listener);
  }
}
