package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFReader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
* Import tab for importing LDAP data from LDIF and CSV files
*/
public class ImportTab extends VerticalLayout {
 
 // LDAP Control OIDs
 private static final String NO_OPERATION_CONTROL_OID = "1.3.6.1.4.1.4203.1.10.2";
 private static final String PERMISSIVE_MODIFY_CONTROL_OID = "1.2.840.113556.1.4.1413";
 
 private final LdapService ldapService;
 private final LoggingService loggingService;
 
 // Server configuration
 private LdapServerConfig serverConfig;
 
 // UI Components
 private ComboBox<String> importModeSelector;
 private VerticalLayout modeContainer;
 
 // LDIF Mode Components
 private VerticalLayout ldifModeLayout;
 private Upload ldifUpload;
 private MemoryBuffer ldifBuffer;
 private Checkbox ldifContinueOnError;
 private Checkbox ldifPermissiveModify;
 private Checkbox ldifNoOperation;
 private Button ldifImportButton;
 
 // CSV Mode Components
 private VerticalLayout csvModeLayout;
 private Upload csvUpload;
 private MemoryBuffer csvBuffer;
 private Checkbox csvExcludeHeader;
 private Checkbox csvQuotedValues;
 private Checkbox csvContinueOnError;
 private Checkbox csvPermissiveModify;
 private Checkbox csvNoOperation;
 private VerticalLayout csvPreviewContainer;
 private Grid<Map<String, String>> csvPreviewGrid;
 private ComboBox<String> dnMethodSelector;
 private VerticalLayout dnMethodContainer;
 private TextField searchBaseField;
 private TextField searchFilterField;
 private TextArea ldifTemplateArea;
 private TextArea previewLdifArea;
 private Button csvImportButton;
 
 // CSV data and settings
 private List<Map<String, String>> csvData;
 private String rawCsvContent;
 private String rawLdifContent;
 private List<String> csvColumnOrder; // Maintain original CSV column order
 
 // Progress
 private ProgressBar progressBar;
 private VerticalLayout progressContainer;
 
 public ImportTab(LdapService ldapService, LoggingService loggingService) {
  this.ldapService = ldapService;
  this.loggingService = loggingService;
  this.csvData = new ArrayList<>();
  this.csvColumnOrder = new ArrayList<>();
  initializeComponents();
  setupLayout();
 }
 
 private void initializeComponents() {
  // Import Mode Selector
  importModeSelector = new ComboBox<>("Import Type");
  importModeSelector.setItems("Input LDIF", "Input CSV");
  importModeSelector.setValue("Input LDIF");
  importModeSelector.addValueChangeListener(e -> switchMode(e.getValue()));
  
  // Initialize mode-specific components
  initializeLdifModeComponents();
  initializeCsvModeComponents();
  
  // Progress components
  progressBar = new ProgressBar();
  progressBar.setIndeterminate(true);
  
  progressContainer = new VerticalLayout();
  progressContainer.setPadding(false);
  progressContainer.setSpacing(true);
  progressContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
  progressContainer.add(new Span("Processing import..."), progressBar);
  progressContainer.setVisible(false);
 }
 
 private void initializeLdifModeComponents() {
  ldifModeLayout = new VerticalLayout();
  ldifModeLayout.setPadding(true);
  ldifModeLayout.setSpacing(true);
  ldifModeLayout.addClassName("import-field-group");
  
  // LDIF Upload
  ldifBuffer = new MemoryBuffer();
  ldifUpload = new Upload(ldifBuffer);
  ldifUpload.setAcceptedFileTypes("text/ldif", ".ldif", "text/plain", ".txt");
  ldifUpload.setMaxFiles(1);
  ldifUpload.setWidthFull();
  ldifUpload.setDropLabel(new Span("Drop LDIF file here or click to browse"));
  
  ldifUpload.addSucceededListener(event -> {
   try {
    processLdifFile();
   } catch (Exception ex) {
    showError("Error processing LDIF file: " + ex.getMessage());
   }
  });
  
  // LDIF Options
  ldifContinueOnError = new Checkbox("Continue on error");
  ldifContinueOnError.setValue(false);
  
  ldifPermissiveModify = new Checkbox("Permissive modify request control");
  ldifPermissiveModify.setValue(false);
  
  ldifNoOperation = new Checkbox("No operation request control");
  ldifNoOperation.setValue(false);
  
  ldifImportButton = new Button("Import LDIF", new Icon(VaadinIcon.UPLOAD));
  ldifImportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  ldifImportButton.addClickListener(e -> performLdifImport());
  ldifImportButton.setEnabled(false);
  
  ldifModeLayout.add(
   new H4("Input LDIF Import"),
   new Span("Upload an LDIF file to import LDAP entries"),
   ldifUpload,
   ldifContinueOnError,
   ldifPermissiveModify,
   ldifNoOperation,
   ldifImportButton
  );
 }
 
 private void initializeCsvModeComponents() {
  csvModeLayout = new VerticalLayout();
  csvModeLayout.setPadding(true);
  csvModeLayout.setSpacing(true);
  csvModeLayout.addClassName("import-field-group");
  
  // CSV Upload
  csvBuffer = new MemoryBuffer();
  csvUpload = new Upload(csvBuffer);
  csvUpload.setAcceptedFileTypes("text/csv", ".csv");
  csvUpload.setMaxFiles(1);
  csvUpload.setWidthFull();
  csvUpload.setDropLabel(new Span("Drop CSV file here or click to browse"));
  
  csvUpload.addSucceededListener(event -> {
   try {
    processCsvFile();
   } catch (Exception ex) {
    showError("Error processing CSV file: " + ex.getMessage());
   }
  });
  
  // CSV Options
  csvExcludeHeader = new Checkbox("Exclude first row (header row)");
  csvExcludeHeader.setValue(false);
  csvExcludeHeader.addValueChangeListener(e -> {
   if (csvData != null && !csvData.isEmpty()) {
    try {
     processCsvFile();
    } catch (Exception ex) {
     showError("Error reprocessing CSV file: " + ex.getMessage());
    }
   }
  });
  
  csvQuotedValues = new Checkbox("Values are surrounded by quotes");
  csvQuotedValues.setValue(true);
  csvQuotedValues.addValueChangeListener(e -> {
   if (csvData != null && !csvData.isEmpty()) {
    try {
     processCsvFile();
    } catch (Exception ex) {
     showError("Error reprocessing CSV file: " + ex.getMessage());
    }
   }
  });
  
  csvContinueOnError = new Checkbox("Continue on error");
  csvContinueOnError.setValue(false);
  
  csvPermissiveModify = new Checkbox("Permissive modify request control");
  csvPermissiveModify.setValue(false);
  
  csvNoOperation = new Checkbox("No operation request control");
  csvNoOperation.setValue(false);
  
  // CSV Preview
  csvPreviewContainer = new VerticalLayout();
  csvPreviewContainer.setPadding(false);
  csvPreviewContainer.setSpacing(true);
  csvPreviewContainer.setVisible(false);
  
  csvPreviewGrid = new Grid<>();
  csvPreviewGrid.setHeight("200px");
  csvPreviewGrid.setWidthFull();
  csvPreviewGrid.addClassName("csv-preview-grid");
  
  csvPreviewContainer.add(
   new H4("File Preview"),
   csvPreviewGrid
  );
  
  // DN Method Selector
  dnMethodSelector = new ComboBox<>("DN Method");
  dnMethodSelector.setItems("CSV Column", "CSV Column and Search");
  dnMethodSelector.setValue("CSV Column");
  dnMethodSelector.setHelperText("Select how to determine the DN for each entry");
  dnMethodSelector.addValueChangeListener(e -> switchDnMethod(e.getValue()));
  
  dnMethodContainer = new VerticalLayout();
  dnMethodContainer.setPadding(false);
  dnMethodContainer.setSpacing(true);
  
  // Search fields for "CSV Column and Search" method
  searchBaseField = new TextField("Search Base");
  searchBaseField.setWidthFull();
  searchBaseField.setPlaceholder("dc=example,dc=com");
  searchBaseField.setVisible(false);
  
  searchFilterField = new TextField("Filter");
  searchFilterField.setWidthFull();
  searchFilterField.setValue("(&(objectClass=person)(uid={C1}))");
  searchFilterField.setHelperText("Use {C1}, {C2}, {C3}, etc. to reference CSV columns");
  searchFilterField.setVisible(false);
  searchFilterField.addValueChangeListener(e -> updatePreviewLdif());
  
  dnMethodContainer.add(searchBaseField, searchFilterField);
  
  // LDIF Template
  ldifTemplateArea = new TextArea("LDIF Template");
  ldifTemplateArea.setWidthFull();
  ldifTemplateArea.setHeight("150px");
  ldifTemplateArea.setValue("dn: {DN}\nchangetype: modify\nreplace: userpassword\nuserpassword: {C2}");
  ldifTemplateArea.setHelperText("Use {DN} for the computed DN and {C1}, {C2}, etc. for CSV columns");
  ldifTemplateArea.addValueChangeListener(e -> updatePreviewLdif());
  
  // Preview LDIF
  previewLdifArea = new TextArea("Preview LDIF");
  previewLdifArea.setWidthFull();
  previewLdifArea.setHeight("150px");
  previewLdifArea.setReadOnly(true);
  previewLdifArea.setHelperText("Preview of the LDIF to be generated");
  
  csvImportButton = new Button("Import CSV", new Icon(VaadinIcon.UPLOAD));
  csvImportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  csvImportButton.addClickListener(e -> performCsvImport());
  csvImportButton.setEnabled(false);
  
  csvModeLayout.add(
   new H4("Input CSV Import"),
   new Span("Upload a CSV file to import LDAP entries"),
   csvUpload,
   csvExcludeHeader,
   csvQuotedValues,
   csvContinueOnError,
   csvPermissiveModify,
   csvNoOperation,
   csvPreviewContainer,
   dnMethodSelector,
   dnMethodContainer,
   ldifTemplateArea,
   previewLdifArea,
   csvImportButton
  );
 }
 
 private void setupLayout() {
  setSizeFull();
  setPadding(true);
  setSpacing(true);
  addClassName("import-tab");
  
  // Title with icon
  HorizontalLayout titleLayout = new HorizontalLayout();
  titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
  titleLayout.setSpacing(true);
  
  Icon importIcon = new Icon(VaadinIcon.UPLOAD);
  importIcon.setSize("20px");
  importIcon.getStyle().set("color", "#28a745");
  
  H3 title = new H3("LDAP Import");
  title.addClassNames(LumoUtility.Margin.NONE);
  title.getStyle().set("color", "#333");
  
  titleLayout.add(importIcon, title);
  
  // Mode container
  modeContainer = new VerticalLayout();
  modeContainer.setPadding(false);
  modeContainer.setSpacing(false);
  modeContainer.setSizeFull();
  
  // Initially show LDIF mode
  modeContainer.add(ldifModeLayout);
  
  add(titleLayout, importModeSelector, modeContainer, progressContainer);
  setFlexGrow(1, modeContainer);
 }
 
 private void switchMode(String mode) {
  modeContainer.removeAll();
  
  if ("Input LDIF".equals(mode)) {
   modeContainer.add(ldifModeLayout);
  } else if ("Input CSV".equals(mode)) {
   modeContainer.add(csvModeLayout);
  }
 }
 
 private void switchDnMethod(String method) {
  boolean showSearchFields = "CSV Column and Search".equals(method);
  searchBaseField.setVisible(showSearchFields);
  searchFilterField.setVisible(showSearchFields);
  updatePreviewLdif();
 }
 
 private void processLdifFile() throws Exception {
  String content = new String(ldifBuffer.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  rawLdifContent = content;
  
  // Basic LDIF validation
  if (content.trim().isEmpty()) {
   showError("LDIF file is empty");
   return;
  }
  
  // Count entries for validation
  long entryCount = content.lines()
   .filter(line -> line.startsWith("dn:"))
   .count();
  
  ldifImportButton.setEnabled(true);
  showSuccess("LDIF file loaded successfully. Found " + entryCount + " entries.");
 }
 
 private void processCsvFile() throws Exception {
  String content;
  
  // If this is the first time processing, read from the input stream
  if (rawCsvContent == null) {
   content = new String(csvBuffer.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
   rawCsvContent = content;
  } else {
   // Use stored content for reprocessing
   content = rawCsvContent;
  }
  
  String[] lines = content.split("\n");
  
  csvData.clear();
  csvColumnOrder.clear();
  csvPreviewGrid.removeAllColumns();
  
  if (lines.length == 0) {
   showError("CSV file is empty");
   return;
  }
  
  // Skip header row if checkbox is checked
  int startIndex = csvExcludeHeader.getValue() ? 1 : 0;
  boolean removeQuotes = csvQuotedValues.getValue();
  
  // Parse CSV data
  boolean isFirstRow = true;
  for (int i = startIndex; i < lines.length; i++) {
   String line = lines[i].trim();
   if (line.isEmpty()) continue;
   
   List<String> values = parseCSVLine(line, removeQuotes);
   Map<String, String> row = new LinkedHashMap<>();
   
   // On the first row, establish the column order
   if (isFirstRow) {
    for (int j = 0; j < values.size(); j++) {
     String columnName = "C" + (j + 1);
     csvColumnOrder.add(columnName);
    }
    isFirstRow = false;
   }
   
   for (int j = 0; j < values.size(); j++) {
    String columnName = "C" + (j + 1);
    String value = values.get(j);
    row.put(columnName, value);
   }
   csvData.add(row);
  }
  
  if (csvData.isEmpty()) {
   showError("No valid data found in CSV file");
   return;
  }
  
  // Set up preview grid columns using the stored column order
  for (String columnName : csvColumnOrder) {
   csvPreviewGrid.addColumn(row -> row.get(columnName))
    .setHeader(columnName)
    .setFlexGrow(1)
    .setResizable(true)
    .setSortable(true);
  }
  
  csvPreviewGrid.setItems(csvData);
  csvPreviewContainer.setVisible(true);
  csvImportButton.setEnabled(true);
  
  String excludeText = csvExcludeHeader.getValue() ? " (header row excluded)" : "";
  String quoteText = csvQuotedValues.getValue() ? " (quotes removed)" : "";
  
  showSuccess("CSV file processed successfully. " + csvData.size() + " rows loaded" + excludeText + quoteText + ".");
  
  updatePreviewLdif();
 }
 
 private void updatePreviewLdif() {
  if (csvData == null || csvData.isEmpty()) {
   previewLdifArea.setValue("");
   return;
  }
  
  // Take the first row as an example
  Map<String, String> sampleRow = csvData.get(0);
  String template = ldifTemplateArea.getValue();
  String dnMethod = dnMethodSelector.getValue();
  
  try {
   String sampleDn;
   if ("CSV Column".equals(dnMethod)) {
    // Assume C1 contains the DN
    sampleDn = sampleRow.getOrDefault("C1", "cn=sample,dc=example,dc=com");
   } else {
    // For search method, show a placeholder
    sampleDn = "cn=foundUser,ou=people,dc=example,dc=com";
   }
   
   // Replace variables in template
   Map<String, String> variables = new HashMap<>(sampleRow);
   variables.put("DN", sampleDn);
   
   String previewLdif = substituteVariables(template, variables);
   previewLdifArea.setValue(previewLdif);
  } catch (Exception e) {
   previewLdifArea.setValue("Error generating preview: " + e.getMessage());
  }
 }
 
 private List<String> parseCSVLine(String line, boolean removeQuotes) {
  List<String> values = new ArrayList<>();
  boolean inQuotes = false;
  StringBuilder currentValue = new StringBuilder();
  
  for (int i = 0; i < line.length(); i++) {
   char ch = line.charAt(i);
   
   if (ch == '"') {
    if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
     // Handle escaped quotes ("")
     currentValue.append('"');
     i++; // Skip the next quote
    } else {
     // Toggle quote state
     inQuotes = !inQuotes;
     if (!removeQuotes) {
      currentValue.append(ch);
     }
    }
   } else if (ch == ',' && !inQuotes) {
    // End of field
    values.add(currentValue.toString().trim());
    currentValue = new StringBuilder();
   } else {
    currentValue.append(ch);
   }
  }
  
  // Add the last field
  values.add(currentValue.toString().trim());
  
  return values;
 }
 
 private String substituteVariables(String template, Map<String, String> variables) {
  String result = template;
  for (Map.Entry<String, String> entry : variables.entrySet()) {
   String placeholder = "{" + entry.getKey() + "}";
   String value = entry.getValue() != null ? entry.getValue() : "";
   result = result.replace(placeholder, value);
  }
  return result;
 }
 
 private void performLdifImport() {
  if (serverConfig == null) {
   showError("Please connect to an LDAP server first");
   return;
  }
  
  if (rawLdifContent == null || rawLdifContent.trim().isEmpty()) {
   showError("Please upload an LDIF file first");
   return;
  }
  
  loggingService.logInfo("IMPORT", "Starting LDIF import - Server: " + serverConfig.getName());
  showProgress();
  
  try {
   // Parse LDIF content using UnboundID SDK with InputStream
   byte[] contentBytes = rawLdifContent.getBytes(StandardCharsets.UTF_8);
   LDIFReader ldifReader = new LDIFReader(new java.io.ByteArrayInputStream(contentBytes));
   
   int successCount = 0;
   int errorCount = 0;
   
   try {
    while (true) {
     LDIFChangeRecord changeRecord = ldifReader.readChangeRecord();
     if (changeRecord == null) {
      break; // End of file
     }
     
     try {
      // Process the change record
      switch (changeRecord.getChangeType()) {
       case ADD:
        // Convert to LdapEntry and add
        LdapEntry entry = new LdapEntry();
        entry.setDn(changeRecord.getDN());
        
        // Get attributes from the add change record
        if (changeRecord instanceof com.unboundid.ldif.LDIFAddChangeRecord) {
         com.unboundid.ldif.LDIFAddChangeRecord addRecord = 
          (com.unboundid.ldif.LDIFAddChangeRecord) changeRecord;
         
         for (com.unboundid.ldap.sdk.Attribute attr : addRecord.getAttributes()) {
          for (String value : attr.getValues()) {
           entry.addAttribute(attr.getName(), value);
          }
         }
        }
        
        ldapService.addEntry(serverConfig.getId(), entry);
        break;
        
       case MODIFY:
        if (changeRecord instanceof com.unboundid.ldif.LDIFModifyChangeRecord) {
         com.unboundid.ldif.LDIFModifyChangeRecord modifyRecord = 
          (com.unboundid.ldif.LDIFModifyChangeRecord) changeRecord;
         
         // Prepare controls based on checkbox selections
         List<Control> controls = new ArrayList<>();
         
         // Check and add No Operation control if requested
         if (ldifNoOperation.getValue()) {
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
         if (ldifPermissiveModify.getValue()) {
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
        
       case DELETE:
        ldapService.deleteEntry(serverConfig.getId(), changeRecord.getDN());
        break;
        
       default:
        throw new Exception("Unsupported change type: " + changeRecord.getChangeType());
      }
      
      successCount++;
      
     } catch (Exception e) {
      errorCount++;
      if (!ldifContinueOnError.getValue()) {
       throw e;
      }
      // Log error but continue if continue on error is enabled
      System.err.println("Error processing LDIF record " + changeRecord.getDN() + ": " + e.getMessage());
     }
    }
   } finally {
    ldifReader.close();
   }
   
   hideProgress();
   
   if (errorCount > 0) {
    loggingService.logImport(serverConfig.getName(), "LDIF file", successCount);
    loggingService.logWarning("IMPORT", "LDIF import completed with errors - Server: " + serverConfig.getName() + ", Successes: " + successCount + ", Errors: " + errorCount);
    showInfo("LDIF import completed with " + successCount + " successes and " + errorCount + " errors");
   } else {
    loggingService.logImport(serverConfig.getName(), "LDIF file", successCount);
    showSuccess("LDIF import completed successfully. " + successCount + " entries processed");
   }
   
  } catch (Exception e) {
   hideProgress();
   loggingService.logError("IMPORT", "LDIF import failed - Server: " + serverConfig.getName(), e.getMessage());
   showError("LDIF import failed: " + e.getMessage());
  }
 }
 
 private void performCsvImport() {
  if (serverConfig == null) {
   showError("Please connect to an LDAP server first");
   return;
  }
  
  if (csvData.isEmpty()) {
   showError("Please upload a CSV file first");
   return;
  }
  
  showProgress();
  
  try {
   String template = ldifTemplateArea.getValue();
   String dnMethod = dnMethodSelector.getValue();
   int successCount = 0;
   int errorCount = 0;
   
   for (Map<String, String> row : csvData) {
    try {
     String dn;
     if ("CSV Column".equals(dnMethod)) {
      // Use first column as DN
      dn = row.getOrDefault("C1", "");
      if (dn.isEmpty()) {
       throw new Exception("DN column (C1) is empty");
      }
     } else {
      // Search for DN using LDAP search
      String searchFilter = substituteVariables(searchFilterField.getValue(), row);
      String searchBase = searchBaseField.getValue();
      
      // Perform actual LDAP search to find DN
      List<LdapEntry> results = ldapService.searchEntries(
       serverConfig.getId(), searchBase, searchFilter, SearchScope.SUB);
      
      if (results.isEmpty()) {
       throw new Exception("No entry found matching search filter: " + searchFilter);
      } else if (results.size() > 1) {
       throw new Exception("Multiple entries found matching search filter: " + searchFilter);
      }
      
      dn = results.get(0).getDn();
     }
     
     // Generate LDIF for this row
     Map<String, String> variables = new HashMap<>(row);
     variables.put("DN", dn);
     String ldifEntry = substituteVariables(template, variables);
     
     // Process the generated LDIF using UnboundID LDIF parser
     if (ldifEntry != null && !ldifEntry.trim().isEmpty()) {
      byte[] contentBytes = ldifEntry.getBytes(StandardCharsets.UTF_8);
      LDIFReader ldifReader = new LDIFReader(new java.io.ByteArrayInputStream(contentBytes));
      
      try {
       while (true) {
        LDIFChangeRecord changeRecord = ldifReader.readChangeRecord();
        if (changeRecord == null) {
         break; // End of LDIF content
        }
        
        // Process the change record (same logic as LDIF import)
        switch (changeRecord.getChangeType()) {
         case ADD:
          LdapEntry entry = new LdapEntry();
          entry.setDn(changeRecord.getDN());
          
          if (changeRecord instanceof com.unboundid.ldif.LDIFAddChangeRecord) {
           com.unboundid.ldif.LDIFAddChangeRecord addRecord = 
            (com.unboundid.ldif.LDIFAddChangeRecord) changeRecord;
           
           for (com.unboundid.ldap.sdk.Attribute attr : addRecord.getAttributes()) {
            for (String value : attr.getValues()) {
             entry.addAttribute(attr.getName(), value);
            }
           }
          }
          
          ldapService.addEntry(serverConfig.getId(), entry);
          break;
          
         case MODIFY:
          if (changeRecord instanceof com.unboundid.ldif.LDIFModifyChangeRecord) {
           com.unboundid.ldif.LDIFModifyChangeRecord modifyRecord = 
            (com.unboundid.ldif.LDIFModifyChangeRecord) changeRecord;
           
           // Prepare controls based on checkbox selections
           List<Control> controls = new ArrayList<>();
           
           // Check and add No Operation control if requested
           if (csvNoOperation.getValue()) {
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
           if (csvPermissiveModify.getValue()) {
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
     }
     
     successCount++;
     
    } catch (Exception e) {
     errorCount++;
     if (!csvContinueOnError.getValue()) {
      throw e;
     }
     // Log error but continue if continue on error is enabled
     System.err.println("Error processing row: " + e.getMessage());
    }
   }
   
   hideProgress();
   
   if (errorCount > 0) {
    showInfo("CSV import completed with " + successCount + " successes and " + errorCount + " errors");
   } else {
    showSuccess("CSV import completed successfully. " + successCount + " entries processed");
   }
   
  } catch (Exception e) {
   hideProgress();
   showError("CSV import failed: " + e.getMessage());
  }
 }
 
 private void showProgress() {
  progressContainer.setVisible(true);
  ldifImportButton.setEnabled(false);
  csvImportButton.setEnabled(false);
 }
 
 private void hideProgress() {
  progressContainer.setVisible(false);
  ldifImportButton.setEnabled(rawLdifContent != null);
  csvImportButton.setEnabled(!csvData.isEmpty());
 }
 
 public void setServerConfig(LdapServerConfig serverConfig) {
  this.serverConfig = serverConfig;
 }
 
 public void clear() {
  rawLdifContent = null;
  rawCsvContent = null;
  csvData.clear();
  csvColumnOrder.clear();
  ldifImportButton.setEnabled(false);
  csvImportButton.setEnabled(false);
  csvPreviewContainer.setVisible(false);
  previewLdifArea.setValue("");
  ldifTemplateArea.setValue("dn: {DN}\nchangetype: modify\nreplace: userpassword\nuserpassword: {C2}");
  searchFilterField.setValue("(&(objectClass=person)(uid={C1}))");
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
