package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.service.LoggingService;
import com.ldapweb.ldapbrowser.service.LoggingService.LogEntry;
import com.ldapweb.ldapbrowser.service.LoggingService.LogLevel;
import com.ldapweb.ldapbrowser.service.LoggingService.LogStats;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Logs tab for viewing application logs.
 */
public class LogsTab extends VerticalLayout {

  private final LoggingService loggingService;

  // UI Components
  private Grid<LogEntry> logsGrid;
  private ListDataProvider<LogEntry> dataProvider;

  // Filters
  private ComboBox<LogLevel> levelFilter;
  private ComboBox<String> categoryFilter;
  private TextField searchFilter;

  // Statistics
  private Span totalLogsSpan;
  private Span errorCountSpan;
  private Span warningCountSpan;
  private Span infoCountSpan;
  private Span debugCountSpan;

  // Controls
  private Button refreshButton;
  private Button clearLogsButton;
  private Button exportLogsButton;
  private Button configButton;
  private Anchor downloadLink;
  // Debug capture toggle
  private com.vaadin.flow.component.checkbox.Checkbox debugCaptureCheckbox;
  // Listener runnable to refresh when logs update
  private final Runnable logUpdateListener = this::safeRefreshFromListener;

  /**
   * Constructs a new LogsTab with the given LoggingService.
   *
   * @param loggingService the logging service to use
   */
  public LogsTab(LoggingService loggingService) {
    this.loggingService = loggingService;
    initializeComponents();
    setupLayout();
    refreshLogs();
    updateStatistics();
  }

  // Ensure the checkbox reflects the current service state when the component is
  // attached
  @Override
  public void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    try {
      // Sync checkbox to current service value
      debugCaptureCheckbox.setValue(loggingService.isDebugCaptureEnabled());
      // Re-register listener in case the component was detached and re-attached
      loggingService.removeLogUpdateListener(logUpdateListener);
      loggingService.addLogUpdateListener(logUpdateListener);
    } catch (Exception ignored) {
      // ignored
    }
  }

  private void initializeComponents() {
    // Grid for displaying logs
    logsGrid = new Grid<>(LogEntry.class, false);
    logsGrid.setHeight("500px");
    logsGrid.setWidthFull();
    logsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT,
        GridVariant.LUMO_WRAP_CELL_CONTENT);

    // Configure grid columns
    logsGrid.addColumn(LogEntry::getFormattedTimestamp)
        .setHeader("Timestamp")
        .setWidth("160px")
        .setFlexGrow(0)
        .setSortable(true);

    logsGrid.addColumn(entry -> entry.getLevel().toString())
        .setHeader("Level")
        .setWidth("80px")
        .setFlexGrow(0)
        .setSortable(true)
        .setClassNameGenerator(entry -> {
          switch (entry.getLevel()) {
            case ERROR:
              return "log-level-error";
            case WARNING:
              return "log-level-warning";
            case INFO:
              return "log-level-info";
            case DEBUG:
              return "log-level-debug";
            default:
              return "";
          }
        });

    logsGrid.addColumn(LogEntry::getCategory)
        .setHeader("Category")
        .setWidth("120px")
        .setFlexGrow(0)
        .setSortable(true);

    logsGrid.addColumn(LogEntry::getMessage)
        .setHeader("Message")
        .setFlexGrow(2)
        .setResizable(true)
        .setAutoWidth(false)
        .setSortable(true);

    logsGrid.addColumn(LogEntry::getDetails)
        .setHeader("Details")
        .setWidth("300px")
        .setFlexGrow(1)
        .setResizable(true)
        .setAutoWidth(false)
        .setSortable(true);

    // Create data provider
    dataProvider = new ListDataProvider<>(loggingService.getAllLogs());
    logsGrid.setDataProvider(dataProvider);

    // Level filter
    levelFilter = new ComboBox<>("Level Filter");
    levelFilter.setItems(LogLevel.values());
    levelFilter.setPlaceholder("All levels");
    levelFilter.setClearButtonVisible(true);
    levelFilter.addValueChangeListener(e -> applyFilters());

    // Category filter
    categoryFilter = new ComboBox<>("Category Filter");
    categoryFilter.setPlaceholder("All categories");
    categoryFilter.setClearButtonVisible(true);
    categoryFilter.addValueChangeListener(e -> applyFilters());

    // Search filter
    searchFilter = new TextField("Search");
    searchFilter.setPlaceholder("Search in messages...");
    searchFilter.setClearButtonVisible(true);
    searchFilter.setValueChangeMode(ValueChangeMode.LAZY);
    searchFilter.addValueChangeListener(e -> applyFilters());
    searchFilter.setPrefixComponent(new Icon(VaadinIcon.SEARCH));

    // Control buttons
    refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
    refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    refreshButton.addClickListener(e -> {
      refreshLogs();
      updateStatistics();
      updateCategoryFilter();
      showSuccess("Logs refreshed - Total: " + loggingService.getAllLogs().size() + " entries");
    });

    clearLogsButton = new Button("Clear Logs", new Icon(VaadinIcon.TRASH));
    clearLogsButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    clearLogsButton.addClickListener(e -> clearLogs());

    exportLogsButton = new Button("Export Logs", new Icon(VaadinIcon.DOWNLOAD));
    exportLogsButton.addClickListener(e -> exportLogs());

    // Download link (hidden initially)
    downloadLink = new Anchor();
    downloadLink.setVisible(false);
    downloadLink.getElement().setAttribute("download", true);

    configButton = new Button("Settings", new Icon(VaadinIcon.COG));
    configButton.addClickListener(e -> showConfigDialog());

    // Debug capture checkbox (initialize to current service state so it remains
    // checked when the tab is recreated) and avoid showing a notification
    // when the state didn't actually change.
    debugCaptureCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Debug LDAP & System");
    debugCaptureCheckbox.getElement().setProperty("title", "Enable detailed LDAP modification logging and system error capture");
    // set initial value from the service before wiring the listener to avoid
    // triggering the listener on construction
    debugCaptureCheckbox.setValue(loggingService.isDebugCaptureEnabled());
    debugCaptureCheckbox.addValueChangeListener(e -> {
      boolean enabled = Boolean.TRUE.equals(e.getValue());
      boolean already = loggingService.isDebugCaptureEnabled();
      // If there's no actual change, just refresh stats and return
      if (enabled == already) {
        updateStatistics();
        return;
      }
      loggingService.setDebugCaptureEnabled(enabled);
      updateStatistics();
      showSuccess(enabled ? "Debug logging enabled (LDAP modifications & system errors)" : "Debug logging disabled");
    });

    // Statistics spans
    totalLogsSpan = new Span();
    errorCountSpan = new Span();
    errorCountSpan.addClassName("log-stat-error");
    warningCountSpan = new Span();
    warningCountSpan.addClassName("log-stat-warning");
    infoCountSpan = new Span();
    infoCountSpan.addClassName("log-stat-info");
    debugCountSpan = new Span();
    debugCountSpan.addClassName("log-stat-debug");
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("logs-container");

    // Title with icon
    HorizontalLayout titleLayout = new HorizontalLayout();
    titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    titleLayout.setSpacing(true);

    Icon logsIcon = new Icon(VaadinIcon.FILE_TEXT);
    logsIcon.setSize("20px");
    logsIcon.getStyle().set("color", "#4a90e2");

    H3 title = new H3("Application Logs");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle().set("color", "#333");

    titleLayout.add(logsIcon, title);

    // Statistics section
    HorizontalLayout statsLayout = new HorizontalLayout();
    statsLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    statsLayout.setSpacing(true);
    statsLayout.addClassName("logs-stats");

    H4 statsTitle = new H4("Statistics:");
    statsTitle.addClassName(LumoUtility.Margin.NONE);

    statsLayout.add(statsTitle, totalLogsSpan, errorCountSpan,
        warningCountSpan, infoCountSpan, debugCountSpan);

    // Filters section
    HorizontalLayout filtersLayout = new HorizontalLayout();
    filtersLayout.setDefaultVerticalComponentAlignment(Alignment.END);
    filtersLayout.setSpacing(true);
    filtersLayout.setWidthFull();
    filtersLayout.addClassName("logs-filters");

    filtersLayout.add(levelFilter, categoryFilter, searchFilter);
    filtersLayout.setFlexGrow(1, searchFilter);

    // Controls section
    HorizontalLayout controlsLayout = new HorizontalLayout();
    controlsLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    controlsLayout.setSpacing(true);
    controlsLayout.addClassName("logs-controls");

    controlsLayout.add(refreshButton, clearLogsButton, exportLogsButton, downloadLink, configButton, debugCaptureCheckbox);

    // Combine filters and controls
    HorizontalLayout topActionsLayout = new HorizontalLayout();
    topActionsLayout.setWidthFull();
    topActionsLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    topActionsLayout.add(filtersLayout, controlsLayout);

    // Add all components
    add(titleLayout, statsLayout, topActionsLayout, logsGrid);
    setFlexGrow(1, logsGrid);

    // Register for updates so the tab auto-refreshes when new logs arrive
    loggingService.addLogUpdateListener(logUpdateListener);
  }

  private void refreshLogs() {
    List<LogEntry> logs = loggingService.getAllLogs();
    dataProvider.getItems().clear();
    dataProvider.getItems().addAll(logs);
    dataProvider.refreshAll();

    // Scroll to bottom to show latest logs
    if (!logs.isEmpty()) {
      logsGrid.scrollToEnd();
    }
  }

  private void updateStatistics() {
    LogStats stats = loggingService.getLogStats();

    totalLogsSpan.setText("Total: " + stats.getTotalLogs());
    errorCountSpan.setText("Errors: " + stats.getErrorCount());
    warningCountSpan.setText("Warnings: " + stats.getWarningCount());
    infoCountSpan.setText("Info: " + stats.getInfoCount());
    debugCountSpan.setText("Debug: " + stats.getDebugCount());
  }

  private void updateCategoryFilter() {
    List<String> categories = loggingService.getAllLogs().stream()
        .map(LogEntry::getCategory)
        .distinct()
        .sorted()
        .collect(Collectors.toList());

    String currentValue = categoryFilter.getValue();
    categoryFilter.setItems(categories);
    if (currentValue != null && categories.contains(currentValue)) {
      categoryFilter.setValue(currentValue);
    }
  }

  private void applyFilters() {
    dataProvider.clearFilters();

    // Level filter
    if (levelFilter.getValue() != null) {
      dataProvider.addFilter(entry -> entry.getLevel() == levelFilter.getValue());
    }

    // Category filter
    if (categoryFilter.getValue() != null && !categoryFilter.getValue().isEmpty()) {
      dataProvider.addFilter(entry ->
          entry.getCategory().equalsIgnoreCase(categoryFilter.getValue()));
    }

    // Search filter
    if (searchFilter.getValue() != null && !searchFilter.getValue().trim().isEmpty()) {
      String searchTerm = searchFilter.getValue().toLowerCase().trim();
      dataProvider.addFilter(entry ->
          entry.getMessage().toLowerCase().contains(searchTerm)
          || (entry.getDetails() != null && entry.getDetails().toLowerCase().contains(searchTerm)));
    }

    dataProvider.refreshAll();
  }

  private void clearLogs() {
    // Show confirmation dialog
    com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog =
        new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
    dialog.setHeader("Clear All Logs");
    dialog.setText(
        "Are you sure you want to clear all application logs? This action cannot be undone."
    );
    dialog.setCancelable(true);
    dialog.setConfirmText("Clear");
    dialog.addConfirmListener(e -> {
      loggingService.clearLogs();
      refreshLogs();
      updateStatistics();
      showSuccess("All logs cleared");
    });
    dialog.open();
  }

  private void showConfigDialog() {
    com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Log Settings");
    dialog.setWidth("400px");
    dialog.setModal(true);
    dialog.setCloseOnEsc(true);
    dialog.setCloseOnOutsideClick(false);

    // Create form layout
    com.vaadin.flow.component.formlayout.FormLayout formLayout = 
        new com.vaadin.flow.component.formlayout.FormLayout();
    formLayout.setResponsiveSteps(new com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep("0", 1));

    // Max log entries field
    com.vaadin.flow.component.textfield.IntegerField maxEntriesField = 
        new com.vaadin.flow.component.textfield.IntegerField("Maximum Log Entries");
    maxEntriesField.setValue(loggingService.getMaxLogEntries());
    maxEntriesField.setMin(1);
    maxEntriesField.setMax(100000);
    maxEntriesField.setStep(100);
    maxEntriesField.setWidth("100%");
    maxEntriesField.setHelperText("Controls how many log entries are kept in memory. Older entries are automatically removed.");

    formLayout.add(maxEntriesField);

    // Current statistics info
    LogStats stats = loggingService.getLogStats();
    com.vaadin.flow.component.html.Div statsDiv = new com.vaadin.flow.component.html.Div();
    statsDiv.getStyle().set("margin-top", "16px").set("padding", "12px")
        .set("background-color", "var(--lumo-contrast-5pct)")
        .set("border-radius", "var(--lumo-border-radius-m)");
    
    com.vaadin.flow.component.html.Span statsLabel = new com.vaadin.flow.component.html.Span("Current Status:");
    statsLabel.getStyle().set("font-weight", "bold").set("display", "block");
    
    com.vaadin.flow.component.html.Span currentCount = new com.vaadin.flow.component.html.Span(
        "• Log entries in memory: " + stats.getTotalLogs());
    currentCount.getStyle().set("display", "block");
    
    com.vaadin.flow.component.html.Span currentLimit = new com.vaadin.flow.component.html.Span(
        "• Current limit: " + loggingService.getMaxLogEntries());
    currentLimit.getStyle().set("display", "block");

    statsDiv.add(statsLabel, currentCount, currentLimit);

    VerticalLayout content = new VerticalLayout(formLayout, statsDiv);
    content.setPadding(false);
    content.setSpacing(true);
    dialog.add(content);

    // Buttons
    Button saveButton = new Button("Save", e -> {
      try {
        Integer newValue = maxEntriesField.getValue();
        if (newValue != null && newValue > 0) {
          int oldValue = loggingService.getMaxLogEntries();
          loggingService.setMaxLogEntries(newValue);
          
          // Refresh logs and stats to reflect any changes
          refreshLogs();
          updateStatistics();
          
          showSuccess(String.format("Max log entries updated: %d → %d", oldValue, newValue));
          dialog.close();
        } else {
          showWarning("Please enter a valid number greater than 0");
        }
      } catch (Exception ex) {
        showWarning("Invalid value: " + ex.getMessage());
      }
    });
    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    Button cancelButton = new Button("Cancel", e -> dialog.close());

    HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
    buttonLayout.setJustifyContentMode(JustifyContentMode.END);
    buttonLayout.getStyle().set("margin-top", "16px");

    dialog.getFooter().add(buttonLayout);
    dialog.open();
  }

  private void exportLogs() {
    List<LogEntry> logs = loggingService.getAllLogs();
    if (logs.isEmpty()) {
      showWarning("No logs to export");
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Timestamp,Level,Category,Message,Details\n");

    for (LogEntry entry : logs) {
      sb.append("\"");
      sb.append(entry.getFormattedTimestamp());
      sb.append("\",");
      sb.append("\"");
      sb.append(entry.getLevel());
      sb.append("\",");
      sb.append("\"");
      sb.append(entry.getCategory());
      sb.append("\",");
      sb.append("\"");
      sb.append(entry.getMessage().replace("\"", "\"\""));
      sb.append("\",");
      sb.append("\"");
      sb.append(entry.getDetails() != null ? entry.getDetails().replace("\"", "\"\"") : "");
      sb.append("\"");
      sb.append("\n");
    }

    try {
      // Create download link with timestamp in filename
      String timestamp = java.time.LocalDateTime.now()
          .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
      String filename = "logs_export_" + timestamp + ".csv";
      
      StreamResource resource = new StreamResource(filename,
          () -> new java.io.ByteArrayInputStream(
              sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      
      resource.setContentType("text/csv");
      resource.setCacheTime(0);

      // Update download link
      downloadLink.setHref(resource);
      downloadLink.getElement().setAttribute("download", filename);
      downloadLink.setText("Download " + filename);
      downloadLink.setVisible(true);

      showSuccess("Logs exported successfully (" + logs.size() + " entries). Click the download link to save the file.");
      
    } catch (Exception e) {
      showWarning("Failed to export logs: " + e.getMessage());
    }
  }

  /**
   * Clears all filters and refreshes the logs grid.
   */
  public void clear() {
    levelFilter.clear();
    categoryFilter.clear();
    searchFilter.clear();
    dataProvider.clearFilters();
    dataProvider.refreshAll();
  }

  // Ensure we remove the listener if the component is detached
  @Override
  public void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    try {
      loggingService.removeLogUpdateListener(logUpdateListener);
    } catch (Exception ignored) {
      // ignored
    }
  }

  // Called from the logging listener - must access UI thread safely
  private void safeRefreshFromListener() {
    // Use UI access if available
    getUI().ifPresent(ui -> ui.access(() -> {
      try {
        refreshLogs();
        updateStatistics();
        updateCategoryFilter();
      } catch (Exception ignored) {
        // ignored
      }
    }));
  }

  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showWarning(String message) {
    Notification notification = Notification.show(message, 4000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
  }
}