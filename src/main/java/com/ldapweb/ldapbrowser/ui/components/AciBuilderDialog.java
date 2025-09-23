package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.util.OidLookupTable;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.Schema;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Dialog for building Access Control Instructions (ACIs) using PingDirectory syntax.
 * Guides users through selecting each ACI component to construct a valid ACI.
 */
public class AciBuilderDialog extends Dialog {

  private Consumer<String> onAciBuilt;
  private LdapService ldapService;
  private String serverId;
  private LdapServerConfig serverConfig;
  
  // Target container for multiple targets
  private VerticalLayout targetsContainer;
  private List<TargetComponent> targets;
  
  // ACL components
  private TextField aclDescriptionField;
  private RadioButtonGroup<String> allowDenyGroup;
  private CheckboxGroup<String> permissionsGroup;
  
  // Bind rule components
  private ComboBox<String> bindRuleTypeCombo;
  private TextField bindRuleValueField;
  private DnSelectorField bindRuleDnField;
  private VerticalLayout bindRuleContainer;
  private Span autoPrependLabel;
  
  // Preview area
  private TextArea aciPreviewArea;
  
  // Action buttons
  private Button buildButton;
  private Button cancelButton;

  /**
   * Constructs a new AciBuilderDialog.
   *
   * @param onAciBuilt callback when ACI is successfully built
   * @param ldapService service for LDAP operations
   * @param serverId the server ID for LDAP operations
   */
  public AciBuilderDialog(Consumer<String> onAciBuilt, LdapService ldapService, String serverId) {
    this(onAciBuilt, ldapService, serverId, null);
  }

  /**
   * Constructs a new AciBuilderDialog.
   *
   * @param onAciBuilt callback when ACI is successfully built
   * @param ldapService service for LDAP operations
   * @param serverId the server ID for LDAP operations
   * @param serverConfig the server configuration for LDAP operations (optional)
   */
  public AciBuilderDialog(Consumer<String> onAciBuilt, LdapService ldapService, String serverId, LdapServerConfig serverConfig) {
    this.onAciBuilt = onAciBuilt;
    this.ldapService = ldapService;
    this.serverId = serverId;
    this.serverConfig = serverConfig;
    this.targets = new ArrayList<>();
    initUI();
    setupEventHandlers();
    // Add initial target after UI is fully initialized
    addNewTarget();
    updatePreview();
  }

  /**
   * Inner class representing a single target component with dynamic controls
   */
  private class TargetComponent extends VerticalLayout {
    private ComboBox<String> targetTypeCombo;
    private Div dynamicControlsContainer;
    private Button removeButton;
    
    // Dynamic controls based on target type
    private ComboBox<String> extopCombo;
    private ComboBox<String> targetControlCombo;
    private TextField requestCriteriaField;
    private DnSelectorField targetDnField;
    private MultiSelectComboBox<String> targetAttrCombo;
    private TextField targetFilterField;
    private TextField targetTrFiltersField;
    private ComboBox<String> scopeCombo;
    
    public TargetComponent() {
      setPadding(false);
      setSpacing(true);
      getStyle().set("border", "1px solid var(--lumo-contrast-20pct)")
          .set("border-radius", "var(--lumo-border-radius-m)")
          .set("padding", "var(--lumo-space-m)")
          .set("margin-bottom", "var(--lumo-space-s)");
      
      initTargetControls();
    }
    
    private void initTargetControls() {
      HorizontalLayout header = new HorizontalLayout();
      header.setJustifyContentMode(JustifyContentMode.BETWEEN);
      header.setAlignItems(Alignment.CENTER);
      
      targetTypeCombo = new ComboBox<>("Target Type");
      targetTypeCombo.setItems("target", "targetattr", "targetfilter", "targettrfilters", 
                              "extop", "targetcontrol", "requestcriteria", "scope");
      targetTypeCombo.setRequired(true);
      targetTypeCombo.addValueChangeListener(event -> updateDynamicControls());
      
      removeButton = new Button(new Icon(VaadinIcon.TRASH));
      removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      removeButton.addClickListener(event -> removeTarget());
      removeButton.setTooltipText("Remove this target");
      
      header.add(targetTypeCombo, removeButton);
      
      dynamicControlsContainer = new Div();
      dynamicControlsContainer.getStyle().set("margin-top", "var(--lumo-space-s)");
      
      add(header, dynamicControlsContainer);
    }
    
    private void updateDynamicControls() {
      dynamicControlsContainer.removeAll();
      
      String targetType = targetTypeCombo.getValue();
      if (targetType == null) return;
      
      switch (targetType) {
        case "extop":
          extopCombo = new ComboBox<>("Extended Operation OID");
          extopCombo.setHelperText("Select supported extended operation");
          loadExtendedOperations();
          extopCombo.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(extopCombo);
          break;
          
        case "targetcontrol":
          targetControlCombo = new ComboBox<>("Control OID");
          targetControlCombo.setHelperText("Select supported control");
          loadSupportedControls();
          targetControlCombo.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(targetControlCombo);
          break;
          
        case "requestcriteria":
          requestCriteriaField = new TextField("Request Criteria");
          requestCriteriaField.setPlaceholder("e.g., critical-request");
          requestCriteriaField.setHelperText("Request criteria identifier");
          requestCriteriaField.setWidthFull();
          requestCriteriaField.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(requestCriteriaField);
          break;
          
        case "target":
          targetDnField = new DnSelectorField("Target DN", ldapService);
          if (serverConfig != null) {
            targetDnField.setServerConfig(serverConfig);
          }
          targetDnField.setHelperText("Base DN for the subtree to which this ACI applies");
          targetDnField.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(targetDnField);
          break;
          
        case "targetattr":
          targetAttrCombo = new MultiSelectComboBox<>("Target Attributes");
          targetAttrCombo.setHelperText("Select attributes (* = all user attrs, + = all operational attrs, multiple values separated by ||)");
          loadSchemaAttributes();
          targetAttrCombo.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(targetAttrCombo);
          break;
          
        case "targetfilter":
          targetFilterField = new TextField("Target Filter");
          targetFilterField.setPlaceholder("e.g., (objectClass=person)");
          targetFilterField.setHelperText("LDAP filter to restrict entries within the scope");
          targetFilterField.setWidthFull();
          targetFilterField.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(targetFilterField);
          break;
          
        case "targettrfilters":
          targetTrFiltersField = new TextField("Target TR Filters");
          targetTrFiltersField.setPlaceholder("e.g., ldap:///ou=people,dc=example,dc=com??one?(objectClass=person)");
          targetTrFiltersField.setHelperText("Target tree filters for complex matching");
          targetTrFiltersField.setWidthFull();
          targetTrFiltersField.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(targetTrFiltersField);
          break;
          
        case "scope":
          scopeCombo = new ComboBox<>("LDAP Scope");
          scopeCombo.setItems("base", "one", "sub", "subordinate");
          scopeCombo.setValue("sub");
          scopeCombo.setHelperText("Search scope for LDAP operations");
          scopeCombo.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(scopeCombo);
          break;
      }
    }
    
    private void loadExtendedOperations() {
      if (extopCombo == null) return;
      
      try {
        Entry rootDSE = ldapService.getRootDSE(serverId);
        if (rootDSE != null) {
          String[] supportedExtensions = rootDSE.getAttributeValues("supportedExtension");
          if (supportedExtensions != null) {
            List<String> extOps = Arrays.asList(supportedExtensions);
            extopCombo.setItems(extOps);
            
            extopCombo.setItems(extOps);
            extopCombo.setRenderer(new ComponentRenderer<>(oid -> {
              Span span = new Span(getExtOpDescription(oid));
              return span;
            }));
            extopCombo.setWidth("500px"); // Make wider to show OID + description
          } else {
            // Fallback with common extended operations
            List<String> commonExtOps = Arrays.asList(
                "1.3.6.1.4.1.42.2.27.9.5.1", // Start TLS
                "1.3.6.1.4.1.1466.20037",    // Password Modify
                "1.3.6.1.4.1.4203.1.11.1"    // Cancel
            );
            extopCombo.setItems(commonExtOps);
            extopCombo.setRenderer(new ComponentRenderer<>(oid -> {
              Span span = new Span(getExtOpDescription(oid));
              return span;
            }));
            extopCombo.setWidth("500px"); // Make wider to show OID + description
          }
        }
      } catch (LDAPException e) {
        // Fallback to common extended operations on error
        List<String> commonExtOps = Arrays.asList(
            "1.3.6.1.4.1.42.2.27.9.5.1", // Start TLS
            "1.3.6.1.4.1.1466.20037",    // Password Modify
            "1.3.6.1.4.1.4203.1.11.1"    // Cancel
        );
        extopCombo.setItems(commonExtOps);
        extopCombo.setRenderer(new ComponentRenderer<>(oid -> {
          Span span = new Span(getExtOpDescription(oid));
          return span;
        }));
        extopCombo.setWidth("500px"); // Make wider to show OID + description
      }
    }
    
    private String getExtOpDescription(String oid) {
      String description = OidLookupTable.getExtendedOperationDescription(oid);
      return description != null ? oid + " - " + description : oid;
    }
    
    private void loadSupportedControls() {
      if (targetControlCombo == null) return;
      
      try {
        Entry rootDSE = ldapService.getRootDSE(serverId);
        if (rootDSE != null) {
          String[] supportedControls = rootDSE.getAttributeValues("supportedControl");
          if (supportedControls != null) {
            List<String> controls = Arrays.asList(supportedControls);
            targetControlCombo.setItems(controls);
            targetControlCombo.setRenderer(new ComponentRenderer<>(oid -> {
              Span span = new Span(getControlDescription(oid));
              return span;
            }));
            targetControlCombo.setWidth("500px"); // Make wider to show OID + description
          } else {
            // Fallback with common controls
            List<String> commonControls = Arrays.asList(
                "1.2.840.113556.1.4.319", // Paged Results
                "1.2.840.113556.1.4.473", // Sort
                "1.3.6.1.1.12",           // Assertion
                "2.16.840.1.113730.3.4.2" // ManageDsaIT
            );
            targetControlCombo.setItems(commonControls);
            targetControlCombo.setRenderer(new ComponentRenderer<>(oid -> {
              Span span = new Span(getControlDescription(oid));
              return span;
            }));
            targetControlCombo.setWidth("500px"); // Make wider to show OID + description
          }
        }
      } catch (LDAPException e) {
        // Fallback to common controls on error
        List<String> commonControls = Arrays.asList(
            "1.2.840.113556.1.4.319", // Paged Results
            "1.2.840.113556.1.4.473", // Sort
            "1.3.6.1.1.12",           // Assertion
            "2.16.840.1.113730.3.4.2" // ManageDsaIT
        );
        targetControlCombo.setItems(commonControls);
        targetControlCombo.setRenderer(new ComponentRenderer<>(oid -> {
          Span span = new Span(getControlDescription(oid));
          return span;
        }));
        targetControlCombo.setWidth("500px"); // Make wider to show OID + description
      }
    }
    
    private String getControlDescription(String oid) {
      String description = OidLookupTable.getControlDescription(oid);
      return description != null ? oid + " - " + description : oid;
    }
    
    private void loadSchemaAttributes() {
      if (targetAttrCombo == null) return;
      
      try {
        Schema schema = ldapService.getSchema(serverId);
        if (schema != null) {
          Collection<AttributeTypeDefinition> attributeTypes = schema.getAttributeTypes();
          List<String> attributeNames = attributeTypes.stream()
              .map(AttributeTypeDefinition::getNameOrOID)
              .sorted()
              .collect(Collectors.toList());
          
          // Add special wildcard values at the beginning
          List<String> allAttributes = new ArrayList<>();
          allAttributes.add("*");  // All user attributes
          allAttributes.add("+");  // All operational attributes
          allAttributes.addAll(attributeNames);
          
          targetAttrCombo.setItems(allAttributes);
        }
      } catch (LDAPException e) {
        // Fallback to common attributes on error, including wildcards
        List<String> commonAttrs = Arrays.asList(
            "*", "+",  // Special wildcard values
            "cn", "sn", "givenName", "mail", "uid", "objectClass", "userPassword",
            "member", "memberOf", "description", "displayName", "telephoneNumber"
        );
        targetAttrCombo.setItems(commonAttrs);
      }
    }
    
    private void removeTarget() {
      targets.remove(this);
      targetsContainer.remove(this);
      updatePreview();
    }
    
    public String getTargetString() {
      String targetType = targetTypeCombo.getValue();
      if (targetType == null) return "";
      
      switch (targetType) {
        case "extop":
          return extopCombo.getValue() != null && !extopCombo.getValue().trim().isEmpty() ?
              "(extop=\"" + extopCombo.getValue().trim() + "\")" : "";
              
        case "targetcontrol":
          return targetControlCombo.getValue() != null && !targetControlCombo.getValue().trim().isEmpty() ?
              "(targetcontrol=\"" + targetControlCombo.getValue().trim() + "\")" : "";
              
        case "requestcriteria":
          return requestCriteriaField.getValue() != null && !requestCriteriaField.getValue().trim().isEmpty() ?
              "(requestcriteria=\"" + requestCriteriaField.getValue().trim() + "\")" : "";
              
        case "target":
          return targetDnField.getValue() != null && !targetDnField.getValue().trim().isEmpty() ?
              "(target=\"ldap:///" + targetDnField.getValue().trim() + "\")" : "";
              
        case "targetattr":
          Set<String> attrs = targetAttrCombo.getValue();
          return attrs != null && !attrs.isEmpty() ?
              "(targetattr=\"" + String.join("||", attrs) + "\")" : "";
              
        case "targetfilter":
          return targetFilterField.getValue() != null && !targetFilterField.getValue().trim().isEmpty() ?
              "(targetfilter=\"" + targetFilterField.getValue().trim() + "\")" : "";
              
        case "targettrfilters":
          return targetTrFiltersField.getValue() != null && !targetTrFiltersField.getValue().trim().isEmpty() ?
              "(targettrfilters=\"" + targetTrFiltersField.getValue().trim() + "\")" : "";
              
        case "scope":
          return scopeCombo.getValue() != null && !scopeCombo.getValue().equals("sub") ?
              "(targetscope=\"" + scopeCombo.getValue() + "\")" : "";
              
        default:
          return "";
      }
    }
    
    public boolean hasValue() {
      String targetType = targetTypeCombo.getValue();
      if (targetType == null) return false;
      
      switch (targetType) {
        case "extop":
          return extopCombo.getValue() != null && !extopCombo.getValue().trim().isEmpty();
        case "targetcontrol":
          return targetControlCombo.getValue() != null && !targetControlCombo.getValue().trim().isEmpty();
        case "requestcriteria":
          return requestCriteriaField.getValue() != null && !requestCriteriaField.getValue().trim().isEmpty();
        case "target":
          return targetDnField.getValue() != null && !targetDnField.getValue().trim().isEmpty();
        case "targetattr":
          Set<String> attrs = targetAttrCombo.getValue();
          return attrs != null && !attrs.isEmpty();
        case "targetfilter":
          return targetFilterField.getValue() != null && !targetFilterField.getValue().trim().isEmpty();
        case "targettrfilters":
          return targetTrFiltersField.getValue() != null && !targetTrFiltersField.getValue().trim().isEmpty();
        case "scope":
          return scopeCombo.getValue() != null;
        default:
          return false;
      }
    }
  }

  private void initUI() {
    setHeaderTitle("ACI Builder");
    setModal(true);
    setDraggable(false);
    setResizable(true);
    setWidth("800px");
    setHeight("700px");

    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);

    // Add description
    Div description = new Div();
    description.setText("Build an Access Control Instruction (ACI) using PingDirectory syntax.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    content.add(description);

    // Target section
    content.add(createTargetSection());
    content.add(new Hr());
    
    // ACL section
    content.add(createAclSection());
    content.add(new Hr());
    
    // Bind rule section
    content.add(createBindRuleSection());
    content.add(new Hr());
    
    // Preview section
    content.add(createPreviewSection());

    add(content);

    // Footer buttons
    buildButton = new Button("Build ACI", event -> buildAci());
    buildButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    
    cancelButton = new Button("Cancel", event -> close());
    
    getFooter().add(cancelButton, buildButton);
  }

  private VerticalLayout createTargetSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    
    H3 title = new H3("1. Target Specification");
    title.getStyle().set("margin", "0");
    section.add(title);
    
    Span info = new Span("Specify what the ACI applies to (at least one target is required)");
    info.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");
    section.add(info);

    // Container for target components
    targetsContainer = new VerticalLayout();
    targetsContainer.setSpacing(true);
    targetsContainer.setPadding(false);
    section.add(targetsContainer);

    // Button to add targets
    Button addTargetButton = new Button("Add Target");
    addTargetButton.addClickListener(e -> addNewTarget());
    section.add(addTargetButton);

    return section;
  }

  private void addNewTarget() {
    TargetComponent targetComponent = new TargetComponent();
    targetsContainer.add(targetComponent);
    targets.add(targetComponent);
    updatePreview();
  }

  private VerticalLayout createAclSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    
    H3 title = new H3("2. Access Control Rule");
    title.getStyle().set("margin", "0");
    section.add(title);
    
    Span info = new Span("Define the access control rule with description and permissions");
    info.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");
    section.add(info);

    FormLayout form = new FormLayout();
    
    aclDescriptionField = new TextField("ACL Description");
    aclDescriptionField.setPlaceholder("e.g., Allow users to update their own password");
    aclDescriptionField.setHelperText("Human-readable description of this access control rule");
    aclDescriptionField.setRequired(true);
    form.add(aclDescriptionField);
    
    allowDenyGroup = new RadioButtonGroup<>();
    allowDenyGroup.setLabel("Access Type");
    allowDenyGroup.setItems("allow", "deny");
    allowDenyGroup.setValue("allow");
    allowDenyGroup.setRequired(true);
    form.add(allowDenyGroup);
    
    permissionsGroup = new CheckboxGroup<>();
    permissionsGroup.setLabel("Permissions");
    permissionsGroup.setItems("read", "search", "compare", "write", "selfwrite", 
                              "add", "delete", "import", "export", "proxy", "all");
    permissionsGroup.setHelperText("Select one or more permissions to grant or deny");
    permissionsGroup.setRequired(true);
    form.add(permissionsGroup);

    section.add(form);
    return section;
  }

  private VerticalLayout createBindRuleSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    
    H3 title = new H3("3. Bind Rule");
    title.getStyle().set("margin", "0");
    section.add(title);
    
    Span info = new Span("Specify who this ACI applies to (the requester identification)");
    info.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");
    section.add(info);

    FormLayout form = new FormLayout();
    
    bindRuleTypeCombo = new ComboBox<>("Bind Rule Type");
    bindRuleTypeCombo.setItems("userdn", "groupdn", "roledn", "authmethod", "ip", "dns", 
                               "dayofweek", "timeofday", "userattr", "secure");
    bindRuleTypeCombo.setValue("userdn");
    bindRuleTypeCombo.setRequired(true);
    form.add(bindRuleTypeCombo);
    
    // Create container for bind rule value field
    bindRuleContainer = new VerticalLayout();
    bindRuleContainer.setPadding(false);
    bindRuleContainer.setSpacing(false);
    
    // Auto-prepend info label (initially visible for DN types)
    autoPrependLabel = new Span("Note: 'ldap:///' will be automatically prepended");
    autoPrependLabel.getStyle().set("color", "var(--lumo-primary-text-color)")
        .set("font-size", "var(--lumo-font-size-xs)")
        .set("font-weight", "500")
        .set("margin-bottom", "var(--lumo-space-xs)");
    bindRuleContainer.add(autoPrependLabel);
    
    // Create both field types - we'll show/hide them based on bind rule type
    bindRuleValueField = new TextField("Bind Rule Value");
    bindRuleValueField.setPlaceholder("e.g., self or simple");
    bindRuleValueField.setHelperText("Value for the selected bind rule type");
    bindRuleValueField.setRequired(true);
    bindRuleValueField.setWidthFull();
    
    // Create DN selector field (for DN-based bind rules)
    bindRuleDnField = new DnSelectorField("Bind Rule Value", ldapService);
    bindRuleDnField.setServerConfig(serverConfig);
    bindRuleDnField.setPlaceholder("e.g., uid=admin,ou=people,dc=example,dc=com");
    bindRuleDnField.setHelperText("Select or enter a DN for the bind rule");
    bindRuleDnField.setWidthFull();
    
    // Initially add the DN field (since default is "userdn")
    bindRuleContainer.add(bindRuleDnField);
    
    form.add(bindRuleContainer);
    form.setColspan(bindRuleContainer, 2);

    section.add(form);
    return section;
  }

  private VerticalLayout createPreviewSection() {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    
    H3 title = new H3("ACI Preview");
    title.getStyle().set("margin", "0");
    section.add(title);
    
    aciPreviewArea = new TextArea();
    aciPreviewArea.setLabel("Generated ACI");
    aciPreviewArea.setReadOnly(true);
    aciPreviewArea.setWidth("100%");
    aciPreviewArea.setHeight("120px");
    aciPreviewArea.getStyle().set("font-family", "monospace");
    section.add(aciPreviewArea);
    
    return section;
  }

  private void setupEventHandlers() {
    // Update preview when any field changes
    aclDescriptionField.addValueChangeListener(event -> updatePreview());
    allowDenyGroup.addValueChangeListener(event -> updatePreview());
    permissionsGroup.addValueChangeListener(event -> updatePreview());
    bindRuleTypeCombo.addValueChangeListener(event -> {
      updateBindRuleHelperText();
      updatePreview();
    });
    bindRuleValueField.addValueChangeListener(event -> updatePreview());
    bindRuleDnField.addValueChangeListener(event -> updatePreview());
    
    // Update helper text for bind rule value based on type
    updateBindRuleHelperText();
  }

  private void updateBindRuleHelperText() {
    String type = bindRuleTypeCombo.getValue();
    if (type != null) {
      boolean isDnType = type.equals("userdn") || type.equals("groupdn") || type.equals("roledn");
      
      // Show/hide auto-prepend label for DN types
      if (autoPrependLabel != null) {
        autoPrependLabel.setVisible(isDnType);
      }
      
      // Switch between DN field and regular text field based on type
      if (bindRuleContainer != null) {
        // Remove both fields first
        bindRuleContainer.remove(bindRuleValueField);
        bindRuleContainer.remove(bindRuleDnField);
        
        if (isDnType) {
          // Use DN selector field for DN-based bind rules
          bindRuleContainer.add(bindRuleDnField);
          switch (type) {
            case "userdn":
              bindRuleDnField.setHelperText("e.g., self, uid=admin,ou=people,dc=example,dc=com");
              bindRuleDnField.setPlaceholder("self");
              break;
            case "groupdn":
              bindRuleDnField.setHelperText("e.g., cn=administrators,ou=groups,dc=example,dc=com");
              bindRuleDnField.setPlaceholder("cn=group,ou=groups,dc=example,dc=com");
              break;
            case "roledn":
              bindRuleDnField.setHelperText("e.g., cn=admin-role,ou=roles,dc=example,dc=com");
              bindRuleDnField.setPlaceholder("cn=role,ou=roles,dc=example,dc=com");
              break;
          }
        } else {
          // Use regular text field for non-DN bind rules
          bindRuleContainer.add(bindRuleValueField);
          switch (type) {
            case "authmethod":
              bindRuleValueField.setHelperText("e.g., simple, sasl");
              bindRuleValueField.setPlaceholder("simple");
              break;
            case "ip":
              bindRuleValueField.setHelperText("e.g., 192.168.1.*, 10.0.0.0/24");
              bindRuleValueField.setPlaceholder("192.168.1.*");
              break;
            case "dns":
              bindRuleValueField.setHelperText("e.g., *.example.com, localhost");
              bindRuleValueField.setPlaceholder("*.example.com");
              break;
            case "secure":
              bindRuleValueField.setHelperText("true or false");
              bindRuleValueField.setPlaceholder("true");
              break;
            default:
              bindRuleValueField.setHelperText("Value for the selected bind rule type");
              bindRuleValueField.setPlaceholder("");
              break;
          }
        }
      }
    }
  }
  
  private String getBindRuleValue() {
    String type = bindRuleTypeCombo.getValue();
    if (type != null) {
      boolean isDnType = type.equals("userdn") || type.equals("groupdn") || type.equals("roledn");
      if (isDnType) {
        String value = bindRuleDnField.getValue();
        return value != null ? value : "";
      } else {
        String value = bindRuleValueField.getValue();
        return value != null ? value : "";
      }
    }
    return "";
  }

  private void updatePreview() {
    // Don't update preview if UI is not fully initialized
    if (aciPreviewArea == null) {
      return;
    }
    
    try {
      String aci = buildAciString();
      aciPreviewArea.setValue(aci);
      if (buildButton != null) {
        buildButton.setEnabled(isValidAci());
      }
    } catch (Exception e) {
      aciPreviewArea.setValue("Invalid ACI configuration: " + e.getMessage());
      if (buildButton != null) {
        buildButton.setEnabled(false);
      }
    }
  }

  private String buildAciString() {
    StringBuilder aci = new StringBuilder();
    
    // Build target components from TargetComponent instances
    List<String> targetStrings = new ArrayList<>();
    
    for (TargetComponent target : targets) {
      String targetString = target.getTargetString();
      if (targetString != null && !targetString.isEmpty()) {
        targetStrings.add(targetString);
      }
    }
    
    // Add targets to ACI
    for (String target : targetStrings) {
      aci.append(target);
    }
    
    // Add version and ACL
    aci.append("(version 3.0; ");
    
    if (!aclDescriptionField.getValue().trim().isEmpty()) {
      aci.append("acl \"").append(aclDescriptionField.getValue().trim()).append("\"; ");
    }
    
    // Add allow/deny and permissions
    String allowDeny = allowDenyGroup.getValue();
    Set<String> permissions = permissionsGroup.getValue();
    
    if (allowDeny != null && !permissions.isEmpty()) {
      aci.append(allowDeny).append(" (");
      aci.append(String.join(",", permissions));
      aci.append(") ");
    }
    
    // Add bind rule
    String bindType = bindRuleTypeCombo.getValue();
    String bindValue = getBindRuleValue();
    
    if (bindType != null && bindValue != null && !bindValue.trim().isEmpty()) {
      aci.append(bindType).append("=\"");
      if (bindType.equals("userdn") || bindType.equals("groupdn") || bindType.equals("roledn")) {
        if (!bindValue.startsWith("ldap:///")) {
          aci.append("ldap:///");
        }
      }
      aci.append(bindValue.trim()).append("\"");
    }
    
    aci.append(";)");
    
    return aci.toString();
  }

  private boolean isValidAci() {
    // Check required fields
    if (aclDescriptionField.getValue().trim().isEmpty()) {
      return false;
    }
    
    if (allowDenyGroup.getValue() == null) {
      return false;
    }
    
    if (permissionsGroup.getValue().isEmpty()) {
      return false;
    }
    
    if (bindRuleTypeCombo.getValue() == null || getBindRuleValue().trim().isEmpty()) {
      return false;
    }
    
    // Check that at least one target is specified
    boolean hasTarget = targets.stream().anyMatch(target -> target.hasValue());
    
    return hasTarget;
  }

  private void buildAci() {
    if (!isValidAci()) {
      showError("Please fill in all required fields and specify at least one target.");
      return;
    }
    
    try {
      String aci = buildAciString();
      if (onAciBuilt != null) {
        onAciBuilt.accept(aci);
      }
      close();
      showSuccess("ACI built successfully");
    } catch (Exception e) {
      showError("Failed to build ACI: " + e.getMessage());
    }
  }

  private void showError(String message) {
    Notification n = Notification.show(message, 5000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private void showSuccess(String message) {
    Notification n = Notification.show(message, 3000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
}
