package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.StreamResource;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFReader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* Search sub-tab for bulk operations on search results
*/
public class BulkSearchTab extends VerticalLayout {
  
  // LDAP Control OIDs
  private static final String NO_OPERATION_CONTROL_OID = "1.3.6.1.4.1.4203.1.10.2";
  private static final String PERMISSIVE_MODIFY_CONTROL_OID = "1.2.840.113556.1.4.1413";
  
  private final LdapService ldapService;
  private final LoggingService loggingService;
  
  // Server configuration
  private LdapServerConfig serverConfig;
  
  // UI Components
  private TextField searchBaseField;
  private TextArea searchFilterField;
  private Checkbox continueOnErrorCheckbox;
  private Checkbox permissiveModifyCheckbox;
  private Checkbox noOperationCheckbox;
  private ComboBox<String> operationModeCombo;
  private TextArea ldifTemplateArea;
  private Button runButton;
  
  // Progress and download
  private ProgressBar progressBar;
  private VerticalLayout progressContainer;
  private Anchor downloadLink;
  
  public BulkSearchTab(LdapService ldapService, LoggingService loggingService) {
    this.ldapService = ldapService;
    this.loggingService = loggingService;
    
    initializeComponents();
    setupLayout();
  }
  
  private void initializeComponents() {
    // Search fields
    searchBaseField = new TextField("Search Base");
    searchBaseField.setWidthFull();
    searchBaseField.setPlaceholder("dc=example,dc=com");
    
    searchFilterField = new TextArea("Search Filter");
    searchFilterField.setWidthFull();
    searchFilterField.setHeight("100px");
    searchFilterField.setPlaceholder("(objectClass=person)");
    
    // Option checkboxes
    continueOnErrorCheckbox = new Checkbox("Continue on error");
    continueOnErrorCheckbox.setValue(false);
    
    permissiveModifyCheckbox = new Checkbox("Permissive modify request control");
    permissiveModifyCheckbox.setValue(false);
    
    noOperationCheckbox = new Checkbox("No operation request control");
    noOperationCheckbox.setValue(false);
    
    // Operation mode selector
    operationModeCombo = new ComboBox<>("Operation Mode");
    operationModeCombo.setItems("Execute Change", "Create LDIF");
    operationModeCombo.setValue("Execute Change");
    operationModeCombo.setWidthFull();
    
    // LDIF Template
    ldifTemplateArea = new TextArea("LDIF Template");
    ldifTemplateArea.setWidthFull();
    ldifTemplateArea.setHeight("150px");
    ldifTemplateArea.setValue("changetype: modify\nreplace: userpassword\nuserpassword: Secret123");
    ldifTemplateArea.setPlaceholder("changetype: modify\nreplace: userpassword\nuserpassword: Secret123");
    
    // Run button
    runButton = new Button("Run", new Icon(VaadinIcon.PLAY));
    runButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    runButton.addClickListener(e -> performBulkOperation());
    
    // Progress components
    progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    
    progressContainer = new VerticalLayout();
    progressContainer.setPadding(false);
    progressContainer.setSpacing(true);
    progressContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    progressContainer.add(new Span("Processing bulk operation..."), progressBar);
    progressContainer.setVisible(false);
    
    // Download link
    downloadLink = new Anchor();
    downloadLink.getElement().setAttribute("download", true);
    downloadLink.setVisible(false);
    downloadLink.add(new Button("Download LDIF", new Icon(VaadinIcon.DOWNLOAD)));
  }
  
  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("bulk-search-tab");
    
    // Main content layout
    VerticalLayout contentLayout = new VerticalLayout();
    contentLayout.setPadding(true);
    contentLayout.setSpacing(true);
    contentLayout.addClassName("bulk-search-field-group");
    
    // Options layout
    HorizontalLayout optionsLayout = new HorizontalLayout();
    optionsLayout.setWidthFull();
    optionsLayout.setSpacing(true);
    optionsLayout.add(continueOnErrorCheckbox, permissiveModifyCheckbox, noOperationCheckbox);
    
    // Action layout
    HorizontalLayout actionLayout = new HorizontalLayout();
    actionLayout.setDefaultVerticalComponentAlignment(Alignment.END);
    actionLayout.setSpacing(true);
    actionLayout.add(operationModeCombo, runButton);
    
    contentLayout.add(
      new H4("Bulk Search Operations"),
      new Span("Perform bulk operations on LDAP search results"),
      searchBaseField,
      searchFilterField,
      optionsLayout,
      ldifTemplateArea,
      actionLayout,
      progressContainer,
      downloadLink
    );
    
    add(contentLayout);
    setFlexGrow(1, contentLayout);
  }
  
  private void performBulkOperation() {
    if (serverConfig == null) {
      showError("Please connect to an LDAP server first");
      return;
    }
    
    String searchBase = searchBaseField.getValue();
    String searchFilter = searchFilterField.getValue();
    String ldifTemplate = ldifTemplateArea.getValue();
    String operationMode = operationModeCombo.getValue();
    
    if (searchBase == null || searchBase.trim().isEmpty()) {
      showError("Search Base is required");
      return;
    }
    
    if (searchFilter == null || searchFilter.trim().isEmpty()) {
      showError("Search Filter is required");
      return;
    }
    
    if (ldifTemplate == null || ldifTemplate.trim().isEmpty()) {
      showError("LDIF Template is required");
      return;
    }
    
    loggingService.logInfo("BULK_SEARCH", "Starting bulk operation - Server: " + serverConfig.getName() + 
      ", Base: " + searchBase + ", Filter: " + searchFilter + ", Mode: " + operationMode);
    
    showProgress();
    
    try {
      // Perform search to get entries
      List<LdapEntry> entries = ldapService.searchEntries(
        serverConfig.getId(), 
        searchBase.trim(), 
        searchFilter.trim(), 
        SearchScope.SUB
      );
      
      if (entries.isEmpty()) {
        hideProgress();
        showInfo("No entries found matching the search criteria");
        return;
      }
      
      if ("Execute Change".equals(operationMode)) {
        performExecuteChanges(entries, ldifTemplate);
      } else {
        performCreateLdif(entries, ldifTemplate);
      }
      
    } catch (Exception e) {
      hideProgress();
      loggingService.logError("BULK_SEARCH", "Bulk operation failed - Server: " + serverConfig.getName(), e.getMessage());
      showError("Bulk operation failed: " + e.getMessage());
    }
  }
  
  private void performExecuteChanges(List<LdapEntry> entries, String ldifTemplate) throws Exception {
    int successCount = 0;
    int errorCount = 0;
    
    for (LdapEntry entry : entries) {
      try {
        // Generate LDIF for this entry
        String ldif = generateLdifForEntry(entry, ldifTemplate);
        
        // Parse and execute the LDIF
        byte[] contentBytes = ldif.getBytes(StandardCharsets.UTF_8);
        LDIFReader ldifReader = new LDIFReader(new ByteArrayInputStream(contentBytes));
        
        try {
          LDIFChangeRecord changeRecord;
          while ((changeRecord = ldifReader.readChangeRecord()) != null) {
            switch (changeRecord.getChangeType()) {
              case MODIFY:
                if (changeRecord instanceof com.unboundid.ldif.LDIFModifyChangeRecord) {
                  com.unboundid.ldif.LDIFModifyChangeRecord modifyRecord = 
                    (com.unboundid.ldif.LDIFModifyChangeRecord) changeRecord;
                  
                  // Prepare controls based on checkbox selections
                  List<Control> controls = new ArrayList<>();
                  
                  // Check and add No Operation control if requested
                  if (noOperationCheckbox.getValue()) {
                    try {
                      boolean isSupported = ldapService.isControlSupported(serverConfig.getId(), NO_OPERATION_CONTROL_OID);
                      if (isSupported) {
                        // Create No Operation control (OID: 1.3.6.1.4.1.4203.1.10.2)
                        Control noOpControl = new Control(NO_OPERATION_CONTROL_OID, false);
                        controls.add(noOpControl);
                      } else {
                        throw new Exception("LDAP server does not support No Operation request control (OID: " + NO_OPERATION_CONTROL_OID + ")");
                      }
                    } catch (LDAPException e) {
                      throw new Exception("Failed to check control support: " + e.getMessage());
                    }
                  }
                  
                  // Check and add Permissive Modify control if requested
                  if (permissiveModifyCheckbox.getValue()) {
                    try {
                      boolean isSupported = ldapService.isControlSupported(serverConfig.getId(), PERMISSIVE_MODIFY_CONTROL_OID);
                      if (isSupported) {
                        // Create Permissive Modify control (OID: 1.2.840.113556.1.4.1413)
                        Control permissiveModifyControl = new Control(PERMISSIVE_MODIFY_CONTROL_OID, false);
                        controls.add(permissiveModifyControl);
                      } else {
                        throw new Exception("LDAP server does not support Permissive Modify request control (OID: " + PERMISSIVE_MODIFY_CONTROL_OID + ")");
                      }
                    } catch (LDAPException e) {
                      throw new Exception("Failed to check control support: " + e.getMessage());
                    }
                  }
                  
                  // Perform modify with controls
                  ldapService.modifyEntry(serverConfig.getId(), 
                    modifyRecord.getDN(), 
                    Arrays.asList(modifyRecord.getModifications()),
                    controls.isEmpty() ? null : controls);
                }
                break;
                
              case ADD:
                if (changeRecord instanceof com.unboundid.ldif.LDIFAddChangeRecord) {
                  com.unboundid.ldif.LDIFAddChangeRecord addRecord = 
                    (com.unboundid.ldif.LDIFAddChangeRecord) changeRecord;
                  
                  LdapEntry newEntry = new LdapEntry();
                  newEntry.setDn(addRecord.getDN());
                  
                  for (com.unboundid.ldap.sdk.Attribute attr : addRecord.getAttributes()) {
                    for (String value : attr.getValues()) {
                      newEntry.addAttribute(attr.getName(), value);
                    }
                  }
                  
                  ldapService.addEntry(serverConfig.getId(), newEntry);
                }
                break;
                
              case DELETE:
                ldapService.deleteEntry(serverConfig.getId(), changeRecord.getDN());
                break;
                
              default:
                throw new Exception("Unsupported change type: " + changeRecord.getChangeType());
            }
          }
        } finally {
          ldifReader.close();
        }
        
        successCount++;
        
      } catch (Exception e) {
        errorCount++;
        if (!continueOnErrorCheckbox.getValue()) {
          throw e;
        }
        // Log error but continue if continue on error is enabled
        loggingService.logError("BULK_SEARCH", "Error processing entry " + entry.getDn(), e.getMessage());
      }
    }
    
    hideProgress();
    
    if (errorCount > 0) {
      showInfo("Bulk operation completed with " + successCount + " successes and " + errorCount + " errors");
    } else {
      showSuccess("Bulk operation completed successfully. " + successCount + " entries processed");
    }
  }
  
  private void performCreateLdif(List<LdapEntry> entries, String ldifTemplate) throws Exception {
    StringBuilder ldifContent = new StringBuilder();
    
    for (int i = 0; i < entries.size(); i++) {
      LdapEntry entry = entries.get(i);
      String ldif = generateLdifForEntry(entry, ldifTemplate);
      ldifContent.append(ldif);
      
      // Add empty line between LDIF change records (except after the last one)
      if (i < entries.size() - 1) {
        ldifContent.append("\n\n");
      } else {
        ldifContent.append("\n");
      }
    }
    
    // Create download
    createDownloadLink(ldifContent.toString(), "bulk-operation.ldif");
    
    hideProgress();
    showSuccess("LDIF generated successfully for " + entries.size() + " entries");
  }
  
  private String generateLdifForEntry(LdapEntry entry, String ldifTemplate) {
    String ldif = ldifTemplate;
    
    // Replace DN placeholder
    ldif = ldif.replace("{DN}", entry.getDn());
    
    // Replace attribute placeholders
    for (String attrName : entry.getAttributes().keySet()) {
      List<String> values = entry.getAttributes().get(attrName);
      if (!values.isEmpty()) {
        String placeholder = "{" + attrName.toUpperCase() + "}";
        ldif = ldif.replace(placeholder, values.get(0));
      }
    }
    
    // Ensure DN is present
    if (!ldif.startsWith("dn:")) {
      ldif = "dn: " + entry.getDn() + "\n" + ldif;
    }
    
    return ldif;
  }
  
  private void createDownloadLink(String content, String fileName) {
    StreamResource resource = new StreamResource(fileName, () -> 
      new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    
    downloadLink.setHref(resource);
    downloadLink.setVisible(true);
  }
  
  private void showProgress() {
    progressContainer.setVisible(true);
    runButton.setEnabled(false);
    downloadLink.setVisible(false);
  }
  
  private void hideProgress() {
    progressContainer.setVisible(false);
    runButton.setEnabled(true);
  }
  
  public void setServerConfig(LdapServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }
  
  public void clear() {
    searchBaseField.clear();
    searchFilterField.clear();
    ldifTemplateArea.setValue("changetype: modify\nreplace: userpassword\nuserpassword: Secret123");
    continueOnErrorCheckbox.setValue(false);
    permissiveModifyCheckbox.setValue(false);
    noOperationCheckbox.setValue(false);
    operationModeCombo.setValue("Execute Change");
    downloadLink.setVisible(false);
    hideProgress();
  }
  
  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }
  
  private void showError(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
  
  private void showInfo(String message) {
    Notification notification = Notification.show(message, 4000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
  }
}
