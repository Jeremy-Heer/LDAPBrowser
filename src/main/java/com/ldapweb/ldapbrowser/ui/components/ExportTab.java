package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.service.LoggingService;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Export tab for exporting LDAP search results in various formats
 */
public class ExportTab extends VerticalLayout {

  private final LdapService ldapService;
  private final LoggingService loggingService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  // Environment selection
  // Removed environment dropdown; server is provided by container view

  // Server configuration
  private LdapServerConfig serverConfig;
  private Set<LdapServerConfig> groupServers;

  // UI Components
  private ComboBox<String> itemSelectionMode;
  private VerticalLayout modeContainer;

  // Search Mode Components
  private VerticalLayout searchModeLayout;
  private TextField searchBaseField;
  private TextArea searchFilterField;
  private TextField returnAttributesField;
  private ComboBox<String> outputFormatCombo;
  private Button exportButton;

  // Input CSV Mode Components
  private VerticalLayout csvModeLayout;
  private TextField csvSearchBaseField;
  private TextArea csvSearchFilterField;
  private TextField csvReturnAttributesField;
  private ComboBox<String> csvOutputFormatCombo;
  private Checkbox excludeHeaderCheckbox;
  private Checkbox quotedValuesCheckbox;
  private Upload csvUpload;
  private MemoryBuffer csvBuffer;
  private VerticalLayout csvPreviewContainer;
  private Grid<Map<String, String>> csvPreviewGrid;
  // CSV data and settings
  private List<Map<String, String>> csvData;
  private String rawCsvContent; // Store the raw CSV content for reprocessing
  private List<String> csvColumnOrder; // Maintain original CSV column order
  private Button csvExportButton;

  // Progress and download
  private ProgressBar progressBar;
  private VerticalLayout progressContainer;
  private Anchor downloadLink;

  public ExportTab(LdapService ldapService, LoggingService loggingService,
      ConfigurationService configurationService, InMemoryLdapService inMemoryLdapService) {
    this.ldapService = ldapService;
    this.loggingService = loggingService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.csvData = new ArrayList<>();
    this.csvColumnOrder = new ArrayList<>();

    // Initialize environment dropdown
    // Environment dropdown removed

    initializeComponents();
    setupLayout();
  }

  private void initializeComponents() {
    // Item Selection Mode
    itemSelectionMode = new ComboBox<>("Item Selection Mode");
    itemSelectionMode.setItems("Search", "Input CSV");
    itemSelectionMode.setValue("Search");
    itemSelectionMode.addValueChangeListener(e -> switchMode(e.getValue()));

    // Progress components
    progressBar = new ProgressBar();
    progressBar.setVisible(false);
    progressContainer = new VerticalLayout();
    progressContainer.setVisible(false);
    progressContainer.addClassName("export-progress");
    progressContainer.add(new Span("Processing export..."), progressBar);

    // Initialize search mode components
    initializeSearchModeComponents();

    // Initialize CSV mode components
    initializeCsvModeComponents();

    // Mode container
    modeContainer = new VerticalLayout();
    modeContainer.setPadding(false);
    modeContainer.setSpacing(false);
    modeContainer.add(searchModeLayout);
  }

  private void initializeSearchModeComponents() {
    searchModeLayout = new VerticalLayout();
    searchModeLayout.setPadding(true);
    searchModeLayout.setSpacing(true);
    searchModeLayout.addClassName("export-field-group");

    // Search fields
    searchBaseField = new TextField("Search Base");
    searchBaseField.setWidthFull();
    searchBaseField.setPlaceholder("dc=example,dc=com");

    searchFilterField = new TextArea("Search Filter");
    searchFilterField.setWidthFull();
    searchFilterField.setHeight("100px");
    searchFilterField.setPlaceholder("(objectClass=person)");

    returnAttributesField = new TextField("Return Attributes");
    returnAttributesField.setWidthFull();
    returnAttributesField.setPlaceholder("cn,mail,telephoneNumber (comma-separated, leave empty for all)");

    outputFormatCombo = new ComboBox<>("Output Format");
    outputFormatCombo.setItems("CSV", "JSON", "LDIF", "DN List");
    outputFormatCombo.setValue("CSV");

    exportButton = new Button("Export", new Icon(VaadinIcon.DOWNLOAD));
    exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    exportButton.addClickListener(e -> performSearchExport());

    HorizontalLayout formatLayout = new HorizontalLayout();
    formatLayout.setDefaultVerticalComponentAlignment(Alignment.END);
    formatLayout.add(outputFormatCombo, exportButton);

    searchModeLayout.add(
        new H4("Search Export"),
        searchBaseField,
        searchFilterField,
        returnAttributesField,
        formatLayout);
  }

  private void initializeCsvModeComponents() {
    csvModeLayout = new VerticalLayout();
    csvModeLayout.setPadding(true);
    csvModeLayout.setSpacing(true);
    csvModeLayout.addClassName("export-field-group");

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

    // CSV Search fields
    csvSearchBaseField = new TextField("Search Base");
    csvSearchBaseField.setWidthFull();
    csvSearchBaseField.setPlaceholder("dc=example,dc=com");

    csvSearchFilterField = new TextArea("Search Filter");
    csvSearchFilterField.setWidthFull();
    csvSearchFilterField.setHeight("100px");
    csvSearchFilterField.setPlaceholder("(&(objectClass=person)(uid={C1})(sn={C2}))");
    csvSearchFilterField.setHelperText("Use {C1}, {C2}, {C3}, etc. to reference CSV columns");

    csvReturnAttributesField = new TextField("Return Attributes");
    csvReturnAttributesField.setWidthFull();
    csvReturnAttributesField.setPlaceholder("cn,mail,telephoneNumber (comma-separated, leave empty for all)");

    csvOutputFormatCombo = new ComboBox<>("Output Format");
    csvOutputFormatCombo.setItems("CSV", "JSON", "LDIF", "DN List");
    csvOutputFormatCombo.setValue("CSV");

    excludeHeaderCheckbox = new Checkbox("Exclude first row (header row)");
    excludeHeaderCheckbox.setValue(false);
    excludeHeaderCheckbox.getStyle().set("margin-top", "8px");
    excludeHeaderCheckbox.addValueChangeListener(e -> {
      if (csvData != null && !csvData.isEmpty()) {
        try {
          processCsvFile(); // Reprocess the file with new settings
        } catch (Exception ex) {
          showError("Error reprocessing CSV file: " + ex.getMessage());
        }
      }
    });

    quotedValuesCheckbox = new Checkbox("Values are surrounded by quotes");
    quotedValuesCheckbox.setValue(true);
    quotedValuesCheckbox.getStyle().set("margin-bottom", "8px");
    quotedValuesCheckbox.addValueChangeListener(e -> {
      if (csvData != null && !csvData.isEmpty()) {
        try {
          processCsvFile(); // Reprocess the file with new settings
        } catch (Exception ex) {
          showError("Error reprocessing CSV file: " + ex.getMessage());
        }
      }
    });

    csvExportButton = new Button("Export", new Icon(VaadinIcon.DOWNLOAD));
    csvExportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    csvExportButton.addClickListener(e -> performCsvExport());
    csvExportButton.setEnabled(false);

    HorizontalLayout formatAndExportLayout = new HorizontalLayout();
    formatAndExportLayout.setDefaultVerticalComponentAlignment(Alignment.END);
    formatAndExportLayout.add(csvOutputFormatCombo, csvExportButton);

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
        csvPreviewGrid);

    csvModeLayout.add(
        new H4("Input CSV Export"),
        new Span("Upload a CSV file to use as input for batch LDAP searches"),
        csvUpload,
        excludeHeaderCheckbox,
        quotedValuesCheckbox,
        csvPreviewContainer,
        csvSearchBaseField,
        csvSearchFilterField,
        csvReturnAttributesField,
        formatAndExportLayout);
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("export-container");

    // Environment selection at the top
    HorizontalLayout environmentSection = new HorizontalLayout();
    environmentSection.setSpacing(true);
    environmentSection.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    // Environment selection UI removed
    add(environmentSection);

    // Title with icon
    HorizontalLayout titleLayout = new HorizontalLayout();
    titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    titleLayout.setSpacing(true);

    Icon exportIcon = new Icon(VaadinIcon.DOWNLOAD);
    exportIcon.setSize("20px");
    exportIcon.getStyle().set("color", "#4a90e2");

    H3 title = new H3("Export");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle().set("color", "#333");

    titleLayout.add(exportIcon, title);

    // Mode selector with styling
    itemSelectionMode.addClassName("export-mode-selector");

    // Add section styling to mode containers
    modeContainer.addClassName("export-section");

    add(titleLayout, itemSelectionMode, modeContainer, progressContainer);

    // Download link (initially hidden)
    downloadLink = new Anchor();
    downloadLink.setVisible(false);
    downloadLink.addClassName("export-download");
    add(downloadLink);
  }

  private void switchMode(String mode) {
    modeContainer.removeAll();
    if ("Search".equals(mode)) {
      modeContainer.add(searchModeLayout);
    } else if ("Input CSV".equals(mode)) {
      modeContainer.add(csvModeLayout);
    }
  }

  private void processCsvFile() throws Exception {
    String content;

    // If this is the first time processing, read from the input stream
    if (rawCsvContent == null) {
      content = new String(csvBuffer.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      rawCsvContent = content; // Store for future reprocessing
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
    int startIndex = excludeHeaderCheckbox.getValue() ? 1 : 0;
    boolean removeQuotes = quotedValuesCheckbox.getValue();

    // Parse CSV data
    boolean isFirstRow = true;
    for (int i = startIndex; i < lines.length; i++) {
      String line = lines[i].trim();
      if (line.isEmpty())
        continue;

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
          .setSortable(true)
          .setComparator((row1, row2) -> {
            String value1 = row1.get(columnName);
            String value2 = row2.get(columnName);
            if (value1 == null)
              value1 = "";
            if (value2 == null)
              value2 = "";
            return value1.compareToIgnoreCase(value2);
          });
    }

    csvPreviewGrid.setItems(csvData);
    csvPreviewContainer.setVisible(true);
    csvExportButton.setEnabled(true);

    String excludeText = excludeHeaderCheckbox.getValue() ? " (header row excluded)" : "";
    String quoteText = quotedValuesCheckbox.getValue() ? " (quotes removed)" : "";
    showSuccess("CSV file processed successfully. " + csvData.size() + " rows loaded" + excludeText + quoteText + ".");
  }

  private void performSearchExport() {
    Set<LdapServerConfig> effectiveServers = getEffectiveServers();
    if (effectiveServers.isEmpty()) {
      showError("Please connect to an LDAP server first");
      return;
    }

    String searchBase = searchBaseField.getValue();
    String searchFilter = searchFilterField.getValue();
    String returnAttrs = returnAttributesField.getValue();
    String format = outputFormatCombo.getValue();

    if (searchBase == null || searchBase.trim().isEmpty()) {
      showError("Search Base is required");
      return;
    }

    if (searchFilter == null || searchFilter.trim().isEmpty()) {
      showError("Search Filter is required");
      return;
    }

    String serverNames = effectiveServers.stream()
        .map(LdapServerConfig::getName)
        .collect(Collectors.joining(", "));
    loggingService.logInfo("EXPORT", "Starting search export - Servers: " + serverNames + ", Base: "
        + searchBase + ", Filter: " + searchFilter + ", Format: " + format);

    showProgress();

    try {
      List<LdapEntry> allLdapEntries = new ArrayList<>();
      List<String> allDnList = new ArrayList<>();
      int totalEntries = 0;

      // Search across all servers
      for (LdapServerConfig server : effectiveServers) {
        try {
          // Ensure connection to each server before searching
          if (!ldapService.isConnected(server.getId())) {
            ldapService.connect(server);
          }

          // Use optimized DN-only search for DN List format
          if ("DN List".equals(format)) {
            List<String> dnList = ldapService.getDNsOnly(server.getId(), searchBase.trim(), 
                searchFilter.trim(), SearchScope.SUB);
            allDnList.addAll(dnList);
            totalEntries += dnList.size();
          } else {
            // Use regular search for other formats
            List<LdapEntry> ldapEntries;
            if (returnAttrs != null && !returnAttrs.trim().isEmpty()) {
              String[] attrs = returnAttrs.split(",");
              for (int i = 0; i < attrs.length; i++) {
                attrs[i] = attrs[i].trim();
              }
              ldapEntries = ldapService.searchEntries(server.getId(), searchBase.trim(), 
                  searchFilter.trim(), SearchScope.SUB, attrs);
            } else {
              ldapEntries = ldapService.searchEntries(server.getId(), searchBase.trim(), 
                  searchFilter.trim(), SearchScope.SUB);
            }
            allLdapEntries.addAll(ldapEntries);
            totalEntries += ldapEntries.size();
          }

        } catch (LDAPException e) {
          loggingService.logError("EXPORT", "Search export failed for server: " + server.getName(), e.getMessage());
          showError("Search failed for server " + server.getName() + ": " + e.getMessage());
          // Continue with other servers
        }
      }

      String exportData;
      String fileName;

      if ("DN List".equals(format)) {
        // Generate DN list export
        exportData = generateDNListExport(allDnList);
        fileName = generateFileName(format);
      } else {
        // Convert LdapEntry to SearchResultEntry for export generation
        List<SearchResultEntry> entries = new ArrayList<>();
        for (LdapEntry ldapEntry : allLdapEntries) {
          // Create a SearchResultEntry from LdapEntry
          Collection<Attribute> attributes = new ArrayList<>();
          for (Map.Entry<String, List<String>> attr : ldapEntry.getAttributes().entrySet()) {
            attributes.add(new Attribute(attr.getKey(), attr.getValue()));
          }
          SearchResultEntry entry = new SearchResultEntry(ldapEntry.getDn(), attributes);
          entries.add(entry);
        }

        exportData = generateExportData(entries, format, getReturnAttributesList(returnAttrs));
        fileName = generateFileName(format);
      }

      createDownloadLink(exportData, fileName, format);
      hideProgress();
      loggingService.logExport(serverNames, fileName, totalEntries);
      showSuccess("Export completed successfully. " + totalEntries + " entries exported from " + 
          effectiveServers.size() + " server(s).");

    } catch (Exception e) {
      hideProgress();
      loggingService.logError("EXPORT", "Search export failed", e.getMessage());
      showError("Search failed: " + e.getMessage());
    }
  }

  private void performCsvExport() {
    Set<LdapServerConfig> effectiveServers = getEffectiveServers();
    if (effectiveServers.isEmpty()) {
      showError("Please connect to an LDAP server first");
      return;
    }

    if (csvData.isEmpty()) {
      showError("Please upload a CSV file first");
      return;
    }

    String searchBase = csvSearchBaseField.getValue();
    String searchFilterTemplate = csvSearchFilterField.getValue();
    String returnAttrs = csvReturnAttributesField.getValue();
    String format = csvOutputFormatCombo.getValue();

    if (searchBase == null || searchBase.trim().isEmpty()) {
      showError("Search Base is required");
      return;
    }

    if (searchFilterTemplate == null || searchFilterTemplate.trim().isEmpty()) {
      showError("Search Filter is required");
      return;
    }

    String serverNames = effectiveServers.stream()
        .map(LdapServerConfig::getName)
        .collect(Collectors.joining(", "));
    loggingService.logInfo("EXPORT", "Starting CSV export - Servers: " + serverNames + ", CSV rows: "
        + csvData.size() + ", Format: " + format);

    showProgress();

    try {
      List<SearchResultEntry> allEntries = new ArrayList<>();

      // Process each row in CSV data across all servers
      for (Map<String, String> row : csvData) {
        String searchFilter = substituteVariables(searchFilterTemplate, row);

        for (LdapServerConfig server : effectiveServers) {
          try {
            // Ensure connection to each server before searching
            if (!ldapService.isConnected(server.getId())) {
              ldapService.connect(server);
            }

            List<LdapEntry> ldapEntries;
            if (returnAttrs != null && !returnAttrs.trim().isEmpty()) {
              String[] attrs = returnAttrs.split(",");
              for (int i = 0; i < attrs.length; i++) {
                attrs[i] = attrs[i].trim();
              }
              ldapEntries = ldapService.searchEntries(server.getId(), searchBase.trim(), searchFilter,
                  SearchScope.SUB, attrs);
            } else {
              ldapEntries = ldapService.searchEntries(server.getId(), searchBase.trim(), searchFilter,
                  SearchScope.SUB);
            }

            // Convert LdapEntry to SearchResultEntry for export generation
            for (LdapEntry ldapEntry : ldapEntries) {
              Collection<Attribute> attributes = new ArrayList<>();
              for (Map.Entry<String, List<String>> attr : ldapEntry.getAttributes().entrySet()) {
                attributes.add(new Attribute(attr.getKey(), attr.getValue()));
              }
              SearchResultEntry entry = new SearchResultEntry(ldapEntry.getDn(), attributes);
              allEntries.add(entry);
            }

          } catch (LDAPException e) {
            loggingService.logError("EXPORT", "CSV export failed for server: " + server.getName(), e.getMessage());
            showError("Search failed for server " + server.getName() + ": " + e.getMessage());
            // Continue with other servers
          }
        }
      }

      String exportData = generateExportData(allEntries, format, getReturnAttributesList(returnAttrs));
      String fileName = generateFileName(format);

      createDownloadLink(exportData, fileName, format);
      hideProgress();
      loggingService.logExport(serverNames, fileName, allEntries.size());
      showSuccess("Export completed successfully. " + allEntries.size() + " entries exported from " + 
          csvData.size() + " searches across " + effectiveServers.size() + " server(s).");

    } catch (Exception e) {
      hideProgress();
      loggingService.logError("EXPORT", "CSV export failed", e.getMessage());
      showError("Search failed: " + e.getMessage());
    }
  }

  private String substituteVariables(String template, Map<String, String> variables) {
    String result = template;
    Pattern pattern = Pattern.compile("\\{(C\\d+)\\}");
    Matcher matcher = pattern.matcher(template);

    while (matcher.find()) {
      String variable = matcher.group(1);
      String value = variables.get(variable);
      if (value != null) {
        result = result.replace("{" + variable + "}", value);
      }
    }

    return result;
  }

  /**
   * Parse a CSV line properly handling quoted values that may contain commas
   */
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

  private List<String> getReturnAttributesList(String returnAttrs) {
    if (returnAttrs == null || returnAttrs.trim().isEmpty()) {
      return new ArrayList<>();
    }

    return Arrays.stream(returnAttrs.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  private String generateExportData(List<SearchResultEntry> entries, String format, List<String> requestedAttrs) {
    switch (format.toUpperCase()) {
      case "CSV":
        return generateCsvData(entries, requestedAttrs);
      case "JSON":
        return generateJsonData(entries, requestedAttrs);
      case "LDIF":
        return generateLdifData(entries, requestedAttrs);
      default:
        return generateCsvData(entries, requestedAttrs);
    }
  }

  /**
   * Generate DN-only export - optimized for bulk operations
   */
  private String generateDNListExport(List<String> dnList) {
    StringBuilder sb = new StringBuilder();
    for (String dn : dnList) {
      sb.append(dn).append("\n");
    }
    return sb.toString();
  }

  private String generateCsvData(List<SearchResultEntry> entries, List<String> requestedAttrs) {
    StringWriter writer = new StringWriter();

    if (entries.isEmpty()) {
      return writer.toString();
    }

    // Determine attributes to export
    Set<String> attributesToExport = new LinkedHashSet<>();
    attributesToExport.add("dn"); // Always include DN

    if (requestedAttrs.isEmpty()) {
      // If no specific attributes requested, collect all unique attributes
      for (SearchResultEntry entry : entries) {
        entry.getAttributes().forEach(attr -> attributesToExport.add(attr.getName()));
      }
    } else {
      attributesToExport.addAll(requestedAttrs);
    }

    // Write header
    writer.write(String.join(",", attributesToExport) + "\n");

    // Write data
    for (SearchResultEntry entry : entries) {
      List<String> values = new ArrayList<>();
      for (String attrName : attributesToExport) {
        if ("dn".equals(attrName)) {
          values.add("\"" + entry.getDN() + "\"");
        } else {
          String[] attrValues = entry.getAttributeValues(attrName);
          if (attrValues != null && attrValues.length > 0) {
            if (attrValues.length == 1) {
              values.add("\"" + attrValues[0] + "\"");
            } else {
              values.add("\"" + String.join("; ", attrValues) + "\"");
            }
          } else {
            values.add("\"\"");
          }
        }
      }
      writer.write(String.join(",", values) + "\n");
    }

    return writer.toString();
  }

  private String generateJsonData(List<SearchResultEntry> entries, List<String> requestedAttrs) {
    StringWriter writer = new StringWriter();
    writer.write("[\n");

    for (int i = 0; i < entries.size(); i++) {
      SearchResultEntry entry = entries.get(i);
      writer.write("  {\n");
      writer.write("    \"dn\": \"" + entry.getDN() + "\"");

      if (requestedAttrs.isEmpty()) {
        // Export all attributes
        entry.getAttributes().forEach(attr -> {
          String[] values = attr.getValues();
          writer.write(",\n    \"" + attr.getName() + "\": ");
          if (values.length == 1) {
            writer.write("\"" + values[0] + "\"");
          } else {
            writer.write("[");
            for (int j = 0; j < values.length; j++) {
              if (j > 0)
                writer.write(", ");
              writer.write("\"" + values[j] + "\"");
            }
            writer.write("]");
          }
        });
      } else {
        // Export only requested attributes
        for (String attrName : requestedAttrs) {
          String[] values = entry.getAttributeValues(attrName);
          if (values != null && values.length > 0) {
            writer.write(",\n    \"" + attrName + "\": ");
            if (values.length == 1) {
              writer.write("\"" + values[0] + "\"");
            } else {
              writer.write("[");
              for (int j = 0; j < values.length; j++) {
                if (j > 0)
                  writer.write(", ");
                writer.write("\"" + values[j] + "\"");
              }
              writer.write("]");
            }
          }
        }
      }

      writer.write("\n  }");
      if (i < entries.size() - 1)
        writer.write(",");
      writer.write("\n");
    }

    writer.write("]\n");
    return writer.toString();
  }

  private String generateLdifData(List<SearchResultEntry> entries, List<String> requestedAttrs) {
    StringWriter writer = new StringWriter();

    for (SearchResultEntry entry : entries) {
      writer.write("dn: " + entry.getDN() + "\n");

      if (requestedAttrs.isEmpty()) {
        // Export all attributes
        entry.getAttributes().forEach(attr -> {
          for (String value : attr.getValues()) {
            writer.write(attr.getName() + ": " + value + "\n");
          }
        });
      } else {
        // Export only requested attributes
        for (String attrName : requestedAttrs) {
          String[] values = entry.getAttributeValues(attrName);
          if (values != null) {
            for (String value : values) {
              writer.write(attrName + ": " + value + "\n");
            }
          }
        }
      }

      writer.write("\n"); // Empty line between entries
    }

    return writer.toString();
  }

  private String generateFileName(String format) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    String timestamp = LocalDateTime.now().format(formatter);
    String extension = format.toLowerCase();
    if ("ldif".equals(extension)) {
      extension = "ldif";
    } else if ("json".equals(extension)) {
      extension = "json";
    } else if ("dn list".equals(extension)) {
      extension = "txt";
    } else {
      extension = "csv";
    }
    return "ldap_export_" + timestamp + "." + extension;
  }

  private void createDownloadLink(String data, String fileName, String format) {
    String mimeType;
    switch (format.toUpperCase()) {
      case "JSON":
        mimeType = "application/json";
        break;
      case "LDIF":
        mimeType = "text/plain";
        break;
      case "DN LIST":
        mimeType = "text/plain";
        break;
      default:
        mimeType = "text/csv";
        break;
    }

    StreamResource resource = new StreamResource(fileName,
        () -> new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    resource.setContentType(mimeType);

    downloadLink.setHref(resource);
    downloadLink.getElement().setAttribute("download", true);
    downloadLink.setText("Download " + fileName);
    downloadLink.setVisible(true);
  }

  private void showProgress() {
    progressBar.setIndeterminate(true);
    progressContainer.setVisible(true);
    exportButton.setEnabled(false);
    csvExportButton.setEnabled(false);
    downloadLink.setVisible(false);
  }

  private void hideProgress() {
    progressContainer.setVisible(false);
    exportButton.setEnabled(true);
    csvExportButton.setEnabled(!csvData.isEmpty());
  }

  public void setServerConfig(LdapServerConfig serverConfig) {
    this.serverConfig = serverConfig;
    this.groupServers = null; // Clear group servers when setting single server
  }

  /**
   * Set multiple servers for group operations
   */
  public void setGroupServers(Set<LdapServerConfig> groupServers) {
    this.groupServers = groupServers;
    this.serverConfig = null; // Clear single server when setting group servers
  }

  /**
   * Get the effective servers to operate on (either single server or group servers)
   */
  private Set<LdapServerConfig> getEffectiveServers() {
    if (groupServers != null && !groupServers.isEmpty()) {
      return groupServers;
    } else if (serverConfig != null) {
      return Set.of(serverConfig);
    } else {
      return Collections.emptySet();
    }
  }

  public void clear() {
    searchBaseField.clear();
    searchFilterField.clear();
    returnAttributesField.clear();
    csvSearchBaseField.clear();
    csvSearchFilterField.clear();
    csvReturnAttributesField.clear();
    csvData.clear();
    csvColumnOrder.clear();
    rawCsvContent = null;
    excludeHeaderCheckbox.setValue(false);
    quotedValuesCheckbox.setValue(true);
    csvPreviewContainer.setVisible(false);
    csvExportButton.setEnabled(false);
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

  /**
   * Refresh the environment dropdown when environments change
   */
  public void refreshEnvironments() {
    // No environment dropdown to refresh here
  }
}