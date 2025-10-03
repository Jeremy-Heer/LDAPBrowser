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
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
  private List<BindRule> bindRules;
  private VerticalLayout bindRulesContainer;
  private RadioButtonGroup<String> bindRuleCombinationGroup;
  private Button addBindRuleButton;
  
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
    this.bindRules = new ArrayList<>();
    initUI();
    setupEventHandlers();
    // Add initial target after UI is fully initialized
    addNewTarget();
    // Add initial bind rule
    addNewBindRule();
    updatePreview();
  }

  /**
   * Populates the ACI Builder dialog with values from an existing ACI string.
   * This method attempts to parse the ACI and populate the form fields accordingly.
   *
   * @param aciString the existing ACI string to parse and populate
   */
  public void populateFromAci(String aciString) {
    if (aciString == null || aciString.trim().isEmpty()) {
      return;
    }
    
    try {
      // Basic ACI parsing - this is a simplified version
      // A full parser would need to handle all the complex ACI syntax
      
      // Extract ACL description
      if (aciString.contains("acl \"")) {
        int start = aciString.indexOf("acl \"") + 5;
        int end = aciString.indexOf("\"", start);
        if (end > start) {
          String description = aciString.substring(start, end);
          if (aclDescriptionField != null) {
            aclDescriptionField.setValue(description);
          }
        }
      }
      
      // Extract allow/deny and permissions
      if (aciString.contains(" allow ")) {
        if (allowDenyGroup != null) {
          allowDenyGroup.setValue("allow");
        }
        // Extract permissions - look for pattern "allow (permissions)"
        int allowIndex = aciString.indexOf(" allow ");
        String afterAllow = aciString.substring(allowIndex + 7); // Skip " allow "
        if (afterAllow.startsWith("(") && afterAllow.contains(")")) {
          int start = 1; // Skip the opening parenthesis
          int end = afterAllow.indexOf(")");
          if (end > start) {
            String permsStr = afterAllow.substring(start, end);
            if (permissionsGroup != null) {
              Set<String> permissions = Arrays.stream(permsStr.split(","))
                  .map(String::trim)
                  .filter(p -> !p.isEmpty())
                  .collect(Collectors.toSet());
              permissionsGroup.setValue(permissions);
            }
          }
        }
      } else if (aciString.contains(" deny ")) {
        if (allowDenyGroup != null) {
          allowDenyGroup.setValue("deny");
        }
        // Extract permissions - look for pattern "deny (permissions)"
        int denyIndex = aciString.indexOf(" deny ");
        String afterDeny = aciString.substring(denyIndex + 6); // Skip " deny "
        if (afterDeny.startsWith("(") && afterDeny.contains(")")) {
          int start = 1; // Skip the opening parenthesis
          int end = afterDeny.indexOf(")");
          if (end > start) {
            String permsStr = afterDeny.substring(start, end);
            if (permissionsGroup != null) {
              Set<String> permissions = Arrays.stream(permsStr.split(","))
                  .map(String::trim)
                  .filter(p -> !p.isEmpty())
                  .collect(Collectors.toSet());
              permissionsGroup.setValue(permissions);
            }
          }
        }
      }
      
      // Extract bind rules - improved implementation to handle multiple bind rules
      bindRules.clear();
      bindRulesContainer.removeAll();
      
      // Parse all bind rule types - changed from if-else if to independent if statements
      // to allow multiple bind rule types to be parsed from the same ACI string
      
      // Parse userdn bind rules
      parseBindRulesOfType(aciString, "userdn", 0);
      
      // Parse groupdn bind rules  
      parseBindRulesOfType(aciString, "groupdn", 0);
      
      // Parse roledn bind rules
      parseBindRulesOfType(aciString, "roledn", 0);
      
      // Parse other bind rule types
      parseBindRulesOfType(aciString, "authmethod", 0);
      parseBindRulesOfType(aciString, "ip", 0);
      parseBindRulesOfType(aciString, "dns", 0);
      parseBindRulesOfType(aciString, "dayofweek", 0);
      parseBindRulesOfType(aciString, "timeofday", 0);
      parseBindRulesOfType(aciString, "userattr", 0);
      parseBindRulesOfType(aciString, "secure", 0);
      
      // If no bind rules found, add a default one
      if (bindRules.isEmpty()) {
        addNewBindRule();
      }
      
      // Parse all target types from the ACI string
      parseTargetsFromAci(aciString);
      
      // Update the preview after populating
      updatePreview();
      
    } catch (Exception e) {
      // If parsing fails, just log it and continue - the user can still use the builder
      System.err.println("Failed to parse ACI for auto-population: " + e.getMessage());
    }
  }

  /**
   * Helper method to parse bind rules of a specific type from the ACI string.
   * This method can find and parse multiple instances of the same bind rule type.
   *
   * @param aciString the ACI string to parse
   * @param bindRuleType the type of bind rule to look for (e.g., "userdn", "groupdn", "roledn")
   * @param typeLength the length of the bind rule type string plus the equals and quote characters (not used anymore)
   */
  private void parseBindRulesOfType(String aciString, String bindRuleType, int typeLength) {
    String searchPattern = bindRuleType + "=\"";
    int searchStart = 0;
    
    // Look for all instances of this bind rule type
    while (true) {
      int start = aciString.indexOf(searchPattern, searchStart);
      if (start == -1) {
        break; // No more instances found
      }
      
      // Move past the pattern to the start of the value
      int valueStart = start + searchPattern.length();
      int end = aciString.indexOf("\"", valueStart);
      
      if (end > valueStart) {
        String value = aciString.substring(valueStart, end);
        
        // Handle ldap:/// prefix for DN-based bind rules
        if ((bindRuleType.equals("userdn") || bindRuleType.equals("groupdn") || bindRuleType.equals("roledn")) 
            && value.startsWith("ldap:///")) {
          value = value.substring(8);
        }
        
        // Check if this bind rule is negated by looking backwards for "not " before the bind rule type
        boolean negated = false;
        String beforeBindRule = aciString.substring(Math.max(0, start - 10), start);
        if (beforeBindRule.toLowerCase().contains("not ")) {
          negated = true;
        }
        
        // Create and add the bind rule
        BindRule bindRule = new BindRule(bindRuleType, value, negated);
        bindRules.add(bindRule);
        BindRuleComponent component = new BindRuleComponent(bindRule, this::removeBindRule, this::updatePreview);
        bindRulesContainer.add(component);
      }
      
      // Move search start past this match to find the next one
      searchStart = valueStart;
    }
  }

  /**
   * Helper method to parse all target types from the ACI string.
   * This method can find and parse multiple target types in the same ACI.
   */
  private void parseTargetsFromAci(String aciString) {
    // Define all supported target types and their patterns
    // Put targetscope before scope to prioritize the new syntax
    String[] targetTypes = {"targetattr", "extop", "target", "targetfilter", "targettrfilters", 
                           "targetcontrol", "requestcriteria", "targetscope", "scope"};
    
    int targetIndex = 0;
    
    for (String targetType : targetTypes) {
      String pattern = "(" + targetType + "=\"";
      if (aciString.contains(pattern)) {
        int start = aciString.indexOf(pattern) + pattern.length();
        int end = aciString.indexOf("\")", start);
        if (end > start) {
          String value = aciString.substring(start, end);
          
          // Ensure we have enough target components
          while (targets.size() <= targetIndex) {
            addNewTarget();
          }
          
          // Set the target type and value
          TargetComponent target = targets.get(targetIndex);
          if (target.targetTypeCombo != null) {
            // Map old "scope" to new "targetscope" for backward compatibility
            String mappedTargetType = "scope".equals(targetType) ? "targetscope" : targetType;
            target.targetTypeCombo.setValue(mappedTargetType);
            // Trigger the value change to show the controls
            target.updateDynamicControls();
            
            // Set the specific value based on target type
            switch (targetType) {
              case "targetattr":
                if (target.targetAttrCombo != null) {
                  Set<String> attrSet = Arrays.stream(value.split("\\|\\|"))
                      .map(String::trim)
                      .filter(attr -> !attr.isEmpty())
                      .collect(Collectors.toSet());
                  target.targetAttrCombo.setValue(attrSet);
                }
                break;
              case "extop":
                if (target.extopCombo != null) {
                  target.extopCombo.setValue(value);
                }
                break;
              case "target":
                if (target.targetDnField != null) {
                  target.targetDnField.setValue(value);
                }
                break;
              case "targetfilter":
                if (target.targetFilterField != null) {
                  target.targetFilterField.setValue(value);
                }
                break;
              case "targetcontrol":
                if (target.targetControlCombo != null) {
                  target.targetControlCombo.setValue(value);
                }
                break;
              case "requestcriteria":
                if (target.requestCriteriaField != null) {
                  target.requestCriteriaField.setValue(value);
                }
                break;
              case "scope":
              case "targetscope":
                if (target.scopeCombo != null) {
                  target.scopeCombo.setValue(value);
                }
                break;
            }
            
            targetIndex++;
          }
        }
      }
    }
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
                              "extop", "targetcontrol", "requestcriteria", "targetscope");
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
          
        case "targetscope":
          scopeCombo = new ComboBox<>("Target Scope");
          scopeCombo.setItems("base", "onelevel", "subtree", "subordinate");
          scopeCombo.setValue("subtree");
          scopeCombo.setHelperText("Scope for ACI target (base=entry only, onelevel=immediate children, subtree=entry and descendants, subordinate=descendants only)");
          scopeCombo.addValueChangeListener(event -> updatePreview());
          dynamicControlsContainer.add(scopeCombo);
          // Trigger preview update since we set a default value
          updatePreview();
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
          if (targetDnField.getValue() != null && !targetDnField.getValue().trim().isEmpty()) {
            String targetValue = targetDnField.getValue().trim();
            // Check if ldap:/// prefix is already present
            if (!targetValue.startsWith("ldap:///")) {
              targetValue = "ldap:///" + targetValue;
            }
            return "(target=\"" + targetValue + "\")";
          }
          return "";
              
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
              
        case "targetscope":
          return scopeCombo.getValue() != null && !scopeCombo.getValue().trim().isEmpty() ?
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
        case "targetscope":
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
    
    H3 title = new H3("3. Bind Rules");
    title.getStyle().set("margin", "0");
    section.add(title);
    
    Span info = new Span("Specify who this ACI applies to (the requester identification). Multiple bind rules can be combined using AND/OR operators.");
    info.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");
    section.add(info);

    // Combination type selection
    HorizontalLayout combinationLayout = new HorizontalLayout();
    combinationLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    combinationLayout.setSpacing(true);
    
    Span combinationLabel = new Span("Combine bind rules using:");
    combinationLabel.getStyle().set("font-weight", "500");
    
    bindRuleCombinationGroup = new RadioButtonGroup<>();
    bindRuleCombinationGroup.setItems("and", "or");
    bindRuleCombinationGroup.setValue("and");
    bindRuleCombinationGroup.addValueChangeListener(e -> updatePreview());
    
    combinationLayout.add(combinationLabel, bindRuleCombinationGroup);
    section.add(combinationLayout);
    
    // Container for multiple bind rules
    bindRulesContainer = new VerticalLayout();
    bindRulesContainer.setPadding(false);
    bindRulesContainer.setSpacing(true);
    section.add(bindRulesContainer);
    
    // Add new bind rule button
    addBindRuleButton = new Button("Add Bind Rule", VaadinIcon.PLUS.create());
    addBindRuleButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
    addBindRuleButton.addClickListener(e -> addNewBindRule());
    section.add(addBindRuleButton);

    return section;
  }

  private void addNewBindRule() {
    BindRule bindRule = new BindRule("userdn", "", false);
    bindRules.add(bindRule);
    
    BindRuleComponent bindRuleComponent = new BindRuleComponent(bindRule, this::removeBindRule, this::updatePreview);
    bindRulesContainer.add(bindRuleComponent);
    
    updatePreview();
  }
  
  private void removeBindRule(BindRuleComponent component) {
    bindRulesContainer.remove(component);
    // Find and remove the corresponding BindRule from the list
    BindRule toRemove = null;
    for (BindRule bindRule : bindRules) {
      if (component.getBindRule() == bindRule) {
        toRemove = bindRule;
        break;
      }
    }
    if (toRemove != null) {
      bindRules.remove(toRemove);
    }
    updatePreview();
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
  }
  
  private void updatePreview() {
    // Don't update preview if UI is not fully initialized
    if (aciPreviewArea == null) {
      return;
    }
    
    try {
      String aci = buildAciString();
      aciPreviewArea.setValue(aci);
      boolean isValid = isValidAci();
      if (buildButton != null) {
        buildButton.setEnabled(isValid);
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
    
    // Add bind rules
    if (!bindRules.isEmpty()) {
      List<String> bindRuleStrings = new ArrayList<>();
      for (BindRule bindRule : bindRules) {
        if (bindRule.getValue() != null && !bindRule.getValue().trim().isEmpty()) {
          bindRuleStrings.add(bindRule.toString());
        }
      }
      
      if (!bindRuleStrings.isEmpty()) {
        if (bindRuleStrings.size() == 1) {
          aci.append(bindRuleStrings.get(0));
        } else {
          String combination = bindRuleCombinationGroup.getValue();
          aci.append(String.join(" " + combination + " ", bindRuleStrings));
        }
      }
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
    
    // Check that at least one valid bind rule is specified
    boolean hasValidBindRule = bindRules.stream()
        .anyMatch(bindRule -> bindRule.getValue() != null && !bindRule.getValue().trim().isEmpty());
    if (!hasValidBindRule) {
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
  
  /**
   * Component for editing a single bind rule.
   */
  private class BindRuleComponent extends HorizontalLayout {
    private final BindRule bindRule;
    private final ComboBox<String> typeCombo;
    private final TextField valueField;
    private final DnSelectorField dnField;
    private final Checkbox negatedCheckbox;
    private final Button removeButton;
    private final Consumer<BindRuleComponent> onRemove;
    private final Runnable onUpdate;
    private VerticalLayout fieldContainer;
    
    public BindRuleComponent(BindRule bindRule, Consumer<BindRuleComponent> onRemove, Runnable onUpdate) {
      this.bindRule = bindRule;
      this.onRemove = onRemove;
      this.onUpdate = onUpdate;
      
      setAlignItems(FlexComponent.Alignment.END);
      setSpacing(true);
      setWidthFull();
      
      // Negation checkbox
      negatedCheckbox = new Checkbox("NOT");
      negatedCheckbox.setValue(bindRule.isNegated());
      negatedCheckbox.addValueChangeListener(e -> {
        bindRule.setNegated(e.getValue());
        onUpdate.run();
      });
      add(negatedCheckbox);
      
      // Type combo
      typeCombo = new ComboBox<>("Type");
      typeCombo.setItems("userdn", "groupdn", "roledn", "authmethod", "ip", "dns", 
                         "dayofweek", "timeofday", "userattr", "secure");
      typeCombo.setValue(bindRule.getType());
      typeCombo.setRequired(true);
      typeCombo.addValueChangeListener(e -> {
        bindRule.setType(e.getValue());
        updateFieldType();
        onUpdate.run();
      });
      add(typeCombo);
      
      // Field container for value field
      fieldContainer = new VerticalLayout();
      fieldContainer.setPadding(false);
      fieldContainer.setSpacing(false);
      fieldContainer.setWidthFull();
      
      // Create both field types
      valueField = new TextField("Value");
      valueField.setWidthFull();
      valueField.addValueChangeListener(e -> {
        bindRule.setValue(e.getValue());
        onUpdate.run();
      });
      
      dnField = new DnSelectorField("Value", ldapService);
      dnField.setServerConfig(serverConfig);
      dnField.setWidthFull();
      dnField.addValueChangeListener(e -> {
        bindRule.setValue(e.getValue());
        onUpdate.run();
      });
      
      updateFieldType();
      add(fieldContainer);
      
      // Remove button
      removeButton = new Button(VaadinIcon.TRASH.create());
      removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      removeButton.addClickListener(e -> onRemove.accept(this));
      add(removeButton);
      
      setFlexGrow(1, fieldContainer);
    }
    
    private void updateFieldType() {
      fieldContainer.removeAll();
      
      String type = typeCombo.getValue();
      boolean isDnType = "userdn".equals(type) || "groupdn".equals(type) || "roledn".equals(type);
      
      if (isDnType) {
        fieldContainer.add(dnField);
        if ("userdn".equals(type)) {
          dnField.setPlaceholder("self or uid=admin,ou=people,dc=example,dc=com");
        } else if ("groupdn".equals(type)) {
          dnField.setPlaceholder("cn=group,ou=groups,dc=example,dc=com");
        } else if ("roledn".equals(type)) {
          dnField.setPlaceholder("cn=role,ou=roles,dc=example,dc=com");
        }
        // Set the value on the DN field
        dnField.setValue(bindRule.getValue() != null ? bindRule.getValue() : "");
      } else {
        fieldContainer.add(valueField);
        if ("authmethod".equals(type)) {
          valueField.setPlaceholder("simple, sasl, etc.");
        } else if ("ip".equals(type)) {
          valueField.setPlaceholder("192.168.1.1 or 192.168.1.0/24");
        } else if ("dns".equals(type)) {
          valueField.setPlaceholder("*.example.com");
        } else {
          valueField.setPlaceholder("Value for " + type);
        }
        // Set the value on the text field
        valueField.setValue(bindRule.getValue() != null ? bindRule.getValue() : "");
      }
    }
    
    public BindRule getBindRule() {
      return bindRule;
    }
  }
  
  /**
   * Represents a single bind rule in an ACI.
   */
  private static class BindRule {
    private String type;
    private String value;
    private boolean negated;
    
    public BindRule(String type, String value, boolean negated) {
      this.type = type;
      this.value = value;
      this.negated = negated;
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public boolean isNegated() { return negated; }
    public void setNegated(boolean negated) { this.negated = negated; }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (negated) {
        sb.append("not ");
      }
      sb.append(type).append("=\"");
      if (type.equals("userdn") || type.equals("groupdn") || type.equals("roledn")) {
        if (!value.startsWith("ldap:///")) {
          sb.append("ldap:///");
        }
      }
      sb.append(value).append("\"");
      return sb.toString();
    }
  }
}
