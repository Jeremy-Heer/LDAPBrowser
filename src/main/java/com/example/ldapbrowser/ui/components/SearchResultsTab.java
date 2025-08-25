package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.SearchResultEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
* Search Results tab for displaying search results from both basic and advanced searches
*/
public class SearchResultsTab extends VerticalLayout {

private static final int RESULTS_PER_PAGE = 50;

private final LdapService ldapService;

// Results display components
private Grid<SearchResultEntry> resultsGrid;
private Span resultCountLabel;
private VerticalLayout resultsContainer;

// Pagination components
private HorizontalLayout paginationLayout;
private Button prevPageButton;
private Button nextPageButton;
private Span pageInfoLabel;

// Comparison components
private HorizontalLayout comparisonControls;
private Button compareSelectedButton;
private Button clearSelectionButton;
private Checkbox selectAllCheckbox;

// Data management
private List<SearchResultEntry> allResults = new ArrayList<>();
private List<SearchResultEntry> selectedForComparison = new ArrayList<>();
private int currentPage = 0;

// Callback for comparison
private Consumer<List<SearchResultEntry>> comparisonCallback;

public SearchResultsTab(LdapService ldapService) {
 this.ldapService = ldapService;
 initializeComponents();
 setupLayout();
}

private void initializeComponents() {
 // Result count label
 resultCountLabel = new Span();
 resultCountLabel.setVisible(false);
 resultCountLabel.getStyle().set("font-weight", "bold");
 resultCountLabel.getStyle().set("color", "var(--lumo-primary-text-color)");
 resultCountLabel.getStyle().set("margin-bottom", "10px");

 // Results grid
 resultsGrid = new Grid<>(SearchResultEntry.class, false);
 setupResultsGrid();

 // Pagination components
 setupPagination();

 // Comparison components
 setupComparisonControls();

 // Results container
 resultsContainer = new VerticalLayout();
 resultsContainer.setPadding(false);
 resultsContainer.setSpacing(true);
 resultsContainer.add(resultCountLabel, resultsGrid, paginationLayout, comparisonControls);
 resultsContainer.setFlexGrow(1, resultsGrid);
 resultsContainer.setSizeFull();
 resultsContainer.setVisible(false); // Initially hidden
}

private void setupResultsGrid() {
 resultsGrid.setSizeFull();
 resultsGrid.addClassName("search-results-grid");

 // Create select all checkbox for the header
 selectAllCheckbox = new Checkbox();
 selectAllCheckbox.addValueChangeListener(event -> {
 boolean selectAll = event.getValue();
 if (selectAll) {
  // Select all visible entries (up to 10)
  List<SearchResultEntry> visibleEntries = getCurrentPageEntries();
  int toSelect = Math.min(10, visibleEntries.size());
  selectedForComparison.clear();
  for (int i = 0; i < toSelect; i++) {
  selectedForComparison.add(visibleEntries.get(i));
  }
  if (toSelect < visibleEntries.size()) {
  showError("You can select up to 10 entries for comparison. Selected first 10 entries.");
  }
 } else {
  selectedForComparison.clear();
 }
 updateAllCheckboxes();
 updateComparisonControls();
 });

 // Compare column with checkbox
 resultsGrid.addComponentColumn(entry -> {
 Checkbox checkbox = new Checkbox();
 checkbox.setValue(selectedForComparison.contains(entry));
 checkbox.addValueChangeListener(event -> {
  if (event.getValue()) {
  if (selectedForComparison.size() >= 10) {
   checkbox.setValue(false);
   showError("You can only select up to 10 entries for comparison.");
   return;
  }
  selectedForComparison.add(entry);
  } else {
  selectedForComparison.remove(entry);
  }
  updateComparisonControls();
  updateSelectAllCheckbox();
 });
 return checkbox;
 }).setHeader(selectAllCheckbox).setWidth("60px").setFlexGrow(0);

 // Distinguished Name column
 resultsGrid.addColumn(entry -> entry.getEntry().getDn())
  .setHeader("Distinguished Name")
  .setAutoWidth(true)
  .setFlexGrow(1);

 // Environment column
 resultsGrid.addColumn(entry -> entry.getEnvironment().getName())
  .setHeader("Environment")
  .setAutoWidth(true)
  .setFlexGrow(0);

 // Actions column
 resultsGrid.addComponentColumn(entry -> {
 Button viewButton = new Button("View", new Icon(VaadinIcon.EYE));
 viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
 viewButton.addClickListener(e -> viewEntry(entry));
 return viewButton;
 }).setHeader("Actions").setWidth("100px").setFlexGrow(0);

 // Set up grid item click handler for row selection
 resultsGrid.addItemClickListener(event -> {
 if (event.getClickCount() == 2) {
  viewEntry(event.getItem());
 }
 });
}

private void setupPagination() {
 prevPageButton = new Button("Previous", new Icon(VaadinIcon.ANGLE_LEFT));
 prevPageButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
 prevPageButton.addClickListener(e -> previousPage());

 nextPageButton = new Button("Next", new Icon(VaadinIcon.ANGLE_RIGHT));
 nextPageButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
 nextPageButton.addClickListener(e -> nextPage());

 pageInfoLabel = new Span();
 pageInfoLabel.getStyle().set("margin", "0 10px");

 paginationLayout = new HorizontalLayout();
 paginationLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
 paginationLayout.setJustifyContentMode(JustifyContentMode.CENTER);
 paginationLayout.add(prevPageButton, pageInfoLabel, nextPageButton);
 paginationLayout.setVisible(false);
}

private void setupComparisonControls() {
 compareSelectedButton = new Button("Compare Selected", new Icon(VaadinIcon.TWIN_COL_SELECT));
 compareSelectedButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
 compareSelectedButton.setEnabled(false);
 compareSelectedButton.addClickListener(e -> compareSelected());

 clearSelectionButton = new Button("Clear Selection", new Icon(VaadinIcon.CLOSE_SMALL));
 clearSelectionButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
 clearSelectionButton.setEnabled(false);
 clearSelectionButton.addClickListener(e -> clearSelection());

 comparisonControls = new HorizontalLayout();
 comparisonControls.setDefaultVerticalComponentAlignment(Alignment.CENTER);
 comparisonControls.setSpacing(true);
 comparisonControls.add(
 new Span("Selected: "),
 compareSelectedButton,
 clearSelectionButton
 );
 comparisonControls.setVisible(false);
}

private void setupLayout() {
 setSizeFull();
 setPadding(true);
 setSpacing(true);
 addClassName("search-results-tab");

 // Add results container
 add(resultsContainer);
 setFlexGrow(1, resultsContainer);
}

/**
* Display search results
*/
public void displayResults(List<SearchResultEntry> results, String searchDescription) {
 this.allResults = new ArrayList<>(results);
 currentPage = 0;
 selectedForComparison.clear();

 updateResultsDisplay();

 resultsContainer.setVisible(true);
 resultsGrid.setVisible(true);

 // Update result count
 String countText = String.format("Found %d %s for %s",
 results.size(),
 results.size() == 1 ? "entry" : "entries",
 searchDescription);
 resultCountLabel.setText(countText);
 resultCountLabel.setVisible(true);

 // Show pagination if more than one page
 paginationLayout.setVisible(results.size() > RESULTS_PER_PAGE);

 if (results.isEmpty()) {
 showInfo("No entries found matching your search criteria");
 } else {
 showSuccess(String.format("Found %d %s", results.size(), results.size() == 1 ? "entry" : "entries"));
 }
}

/**
* Clear all results
*/
public void clear() {
 allResults.clear();
 selectedForComparison.clear();
 currentPage = 0;
 resultsGrid.setItems(new ArrayList<>());
 resultCountLabel.setVisible(false);
 paginationLayout.setVisible(false);
 comparisonControls.setVisible(false);
 resultsContainer.setVisible(false);
}

private void updateResultsDisplay() {
 List<SearchResultEntry> currentPageEntries = getCurrentPageEntries();
 resultsGrid.setItems(currentPageEntries);
 updatePaginationInfo();
 updateComparisonControls();
 updateAllCheckboxes();
}

private List<SearchResultEntry> getCurrentPageEntries() {
 int start = currentPage * RESULTS_PER_PAGE;
 int end = Math.min(start + RESULTS_PER_PAGE, allResults.size());
 return allResults.subList(start, end);
}

private void updatePaginationInfo() {
 if (allResults.isEmpty()) {
 pageInfoLabel.setText("No results");
 prevPageButton.setEnabled(false);
 nextPageButton.setEnabled(false);
 return;
 }

 int totalPages = (int) Math.ceil((double) allResults.size() / RESULTS_PER_PAGE);
 int start = currentPage * RESULTS_PER_PAGE + 1;
 int end = Math.min((currentPage + 1) * RESULTS_PER_PAGE, allResults.size());

 pageInfoLabel.setText(String.format("Showing %d-%d of %d (Page %d of %d)",
 start, end, allResults.size(), currentPage + 1, totalPages));

 prevPageButton.setEnabled(currentPage > 0);
 nextPageButton.setEnabled(currentPage < totalPages - 1);
}

private void previousPage() {
 if (currentPage > 0) {
 currentPage--;
 updateResultsDisplay();
 }
}

private void nextPage() {
 int totalPages = (int) Math.ceil((double) allResults.size() / RESULTS_PER_PAGE);
 if (currentPage < totalPages - 1) {
 currentPage++;
 updateResultsDisplay();
 }
}

private void updateComparisonControls() {
 int selectedCount = selectedForComparison.size();
 compareSelectedButton.setEnabled(selectedCount >= 2);
 clearSelectionButton.setEnabled(selectedCount > 0);
 comparisonControls.setVisible(selectedCount > 0);

 // Update the "Selected: " text
 if (selectedCount > 0) {
 comparisonControls.getComponentAt(0).getElement().setText(
  String.format("Selected: %d %s", selectedCount, selectedCount == 1 ? "entry" : "entries"));
 }
}

private void updateAllCheckboxes() {
 resultsGrid.getDataProvider().refreshAll();
}

private void updateSelectAllCheckbox() {
 List<SearchResultEntry> visibleEntries = getCurrentPageEntries();
 boolean allSelected = !visibleEntries.isEmpty() && 
 visibleEntries.stream().allMatch(selectedForComparison::contains);
 selectAllCheckbox.setValue(allSelected);
}

private void compareSelected() {
 if (selectedForComparison.size() < 2) {
 showError("Please select at least 2 entries for comparison.");
 return;
 }

 if (comparisonCallback != null) {
 comparisonCallback.accept(new ArrayList<>(selectedForComparison));
 }
}

private void clearSelection() {
 selectedForComparison.clear();
 updateAllCheckboxes();
 updateComparisonControls();
 updateSelectAllCheckbox();
}

private void viewEntry(SearchResultEntry entry) {
 try {
 LdapServerConfig environment = entry.getEnvironment();
 
 com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
 dialog.setHeaderTitle("Entry Details - " + environment.getName());
 dialog.setWidth("800px");
 dialog.setHeight("600px");

 // Create an attribute editor for display-only mode
 AttributeEditor attributeEditor = new AttributeEditor(ldapService);
 attributeEditor.setServerConfig(environment);
 
 // Try to fetch complete entry with schema for optimized display
 try {
  LdapService.EntryWithSchema entryWithSchema = ldapService.getEntryWithSchema(environment.getId(), entry.getEntry().getDn());
  if (entryWithSchema != null) {
  attributeEditor.editEntryWithSchema(entryWithSchema.getEntry(), entryWithSchema.getSchema());
  } else {
  attributeEditor.editEntry(entry.getEntry());
  }
 } catch (Exception e) {
  // Fallback to regular method if optimized fetch fails
  attributeEditor.editEntry(entry.getEntry());
 }

 dialog.add(attributeEditor);
 
 // Close button
 Button closeButton = new Button("Close", VaadinIcon.CLOSE.create());
 closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
 closeButton.addClickListener(e -> dialog.close());
 dialog.getFooter().add(closeButton);
 
 dialog.open();
 } catch (Exception ex) {
 showError("Failed to load entry details: " + ex.getMessage());
 }
}

private void showError(String message) {
 Notification.show(message, 3000, Notification.Position.TOP_CENTER)
 .addThemeVariants(NotificationVariant.LUMO_ERROR);
}

private void showInfo(String message) {
 Notification.show(message, 3000, Notification.Position.TOP_CENTER)
 .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
}

private void showSuccess(String message) {
 Notification.show(message, 3000, Notification.Position.TOP_CENTER)
 .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
}

/**
* Set callback for entry comparison
*/
public void setComparisonCallback(Consumer<List<SearchResultEntry>> callback) {
 this.comparisonCallback = callback;
}
}
