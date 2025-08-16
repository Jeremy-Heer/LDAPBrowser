package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.service.LoggingService;
import com.example.ldapbrowser.service.LoggingService.LogEntry;
import com.example.ldapbrowser.service.LoggingService.LogLevel;
import com.example.ldapbrowser.service.LoggingService.LogStats;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;
import java.util.stream.Collectors;

/**
* Logs tab for viewing application logs
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

  public LogsTab(LoggingService loggingService) {
    this.loggingService = loggingService;
    initializeComponents();
    setupLayout();
    refreshLogs();
    updateStatistics();
  }

  private void initializeComponents() {
    // Grid for displaying logs
    logsGrid = new Grid<>(LogEntry.class, false);
    logsGrid.setHeight("500px");
    logsGrid.setWidthFull();
    logsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);

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
        case ERROR: return "log-level-error";
        case WARNING: return "log-level-warning";
        case INFO: return "log-level-info";
        case DEBUG: return "log-level-debug";
        default: return "";
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

    statsLayout.add(statsTitle, totalLogsSpan, errorCountSpan, warningCountSpan, infoCountSpan, debugCountSpan);

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

    controlsLayout.add(refreshButton, clearLogsButton, exportLogsButton);

    // Combine filters and controls
    HorizontalLayout topActionsLayout = new HorizontalLayout();
    topActionsLayout.setWidthFull();
    topActionsLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    topActionsLayout.add(filtersLayout, controlsLayout);

    // Add all components
    add(titleLayout, statsLayout, topActionsLayout, logsGrid);
    setFlexGrow(1, logsGrid);
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
      dataProvider.addFilter(entry -> entry.getCategory().equalsIgnoreCase(categoryFilter.getValue()));
    }

    // Search filter
    if (searchFilter.getValue() != null && !searchFilter.getValue().trim().isEmpty()) {
      String searchTerm = searchFilter.getValue().toLowerCase().trim();
      dataProvider.addFilter(entry ->
      entry.getMessage().toLowerCase().contains(searchTerm) ||
      (entry.getDetails() != null && entry.getDetails().toLowerCase().contains(searchTerm))
      );
    }

    dataProvider.refreshAll();
  }

  private void clearLogs() {
    // Show confirmation dialog
    com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog =
    new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
    dialog.setHeader("Clear All Logs");
    dialog.setText("Are you sure you want to clear all application logs? This action cannot be undone.");
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

  private void exportLogs() {
    List<LogEntry> logs = loggingService.getAllLogs();
    if (logs.isEmpty()) {
      showWarning("No logs to export");
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Timestamp,Level,Category,Message,Details\n");

    for (LogEntry entry : logs) {
      sb.append("\"").append(entry.getFormattedTimestamp()).append("\",");
      sb.append("\"").append(entry.getLevel()).append("\",");
      sb.append("\"").append(entry.getCategory()).append("\",");
      sb.append("\"").append(entry.getMessage().replace("\"", "\"\"")).append("\",");
      sb.append("\"").append(entry.getDetails() != null ? entry.getDetails().replace("\"", "\"\"") : "").append("\"");
      sb.append("\n");
    }

    // Create download link
    com.vaadin.flow.server.StreamResource resource =
    new com.vaadin.flow.server.StreamResource("logs_export.csv",
    () -> new java.io.ByteArrayInputStream(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    resource.setContentType("text/csv");

    com.vaadin.flow.component.html.Anchor downloadLink = new com.vaadin.flow.component.html.Anchor(resource, "");
    downloadLink.getElement().setAttribute("download", true);
    downloadLink.getElement().setAttribute("style", "display: none");

    add(downloadLink);
    downloadLink.getElement().callJsFunction("click");
    remove(downloadLink);

    showSuccess("Logs exported successfully");
  }

  public void clear() {
    levelFilter.clear();
    categoryFilter.clear();
    searchFilter.clear();
    dataProvider.clearFilters();
    dataProvider.refreshAll();
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