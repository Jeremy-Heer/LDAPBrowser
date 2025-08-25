package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
* Panel for searching LDAP entries
*/
public class SearchPanel extends VerticalLayout {

private final LdapService ldapService;
private LdapServerConfig serverConfig;

private TextField baseDnField;
private TextField filterField;
private TextField returnAttributesField;
private ComboBox<SearchScope> scopeComboBox;
private Button searchButton;
private Button clearButton;

private final List<Consumer<SearchResult>> searchListeners = new ArrayList<>();

/**
* Data class to hold search criteria and results
*/
public static class SearchResult {
private final List<LdapEntry> entries;
private final List<String> requestedAttributes;

public SearchResult(List<LdapEntry> entries, List<String> requestedAttributes) {
this.entries = entries;
this.requestedAttributes = requestedAttributes;
}

public List<LdapEntry> getEntries() { return entries; }
public List<String> getRequestedAttributes() { return requestedAttributes; }
}

public SearchPanel(LdapService ldapService) {
this.ldapService = ldapService;

initializeComponents();
setupLayout();
}

private void initializeComponents() {
baseDnField = new TextField("Search Base DN");
baseDnField.setPlaceholder("e.g., ou=users,dc=example,dc=com");
baseDnField.setWidthFull();

filterField = new TextField("LDAP Filter");
filterField.setPlaceholder("e.g., (objectClass=person)");
filterField.setValue("(objectClass=*)");
filterField.setWidthFull();

returnAttributesField = new TextField("Return Attributes");
returnAttributesField.setPlaceholder("e.g., cn,mail,telephoneNumber (leave empty for all attributes)");
returnAttributesField.setWidthFull();
returnAttributesField.setHelperText("Comma-separated list of attributes to return. Leave empty to return all attributes. Use '+' for operational attributes.");
returnAttributesField.setClearButtonVisible(true);

scopeComboBox = new ComboBox<>("Search Scope");
scopeComboBox.setItems(SearchScope.BASE, SearchScope.ONE, SearchScope.SUB);
scopeComboBox.setItemLabelGenerator(scope -> {
switch (scope.intValue()) {
 case 0: return "Base (base object only)";
 case 1: return "One Level (immediate children)";
 case 2: return "Subtree (all descendants)";
 default: return scope.getName();
}
});
scopeComboBox.setValue(SearchScope.SUB);
scopeComboBox.setWidth("200px");

searchButton = new Button("Search", new Icon(VaadinIcon.SEARCH));
searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
searchButton.addClickListener(e -> performSearch());

clearButton = new Button("Clear", new Icon(VaadinIcon.CLOSE));
clearButton.addClickListener(e -> clearResults());

// Enable search on Enter key
filterField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> performSearch());
baseDnField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> performSearch());
returnAttributesField.addKeyPressListener(com.vaadin.flow.component.Key.ENTER, e -> performSearch());
}

private void setupLayout() {
setPadding(false);
setSpacing(true);

// Search controls
HorizontalLayout searchControls = new HorizontalLayout();
searchControls.setWidthFull();
searchControls.setDefaultVerticalComponentAlignment(Alignment.END);

searchControls.add(scopeComboBox, searchButton, clearButton);

add(baseDnField, filterField, returnAttributesField, searchControls);
}

public void setServerConfig(LdapServerConfig serverConfig) {
this.serverConfig = serverConfig;
if (serverConfig != null && serverConfig.getBaseDn() != null) {
baseDnField.setValue(serverConfig.getBaseDn());
}
}

private void performSearch() {
if (serverConfig == null) {
showError("Please connect to a server first.");
return;
}

String baseDn = baseDnField.getValue();
String filter = filterField.getValue();
String returnAttributesText = returnAttributesField.getValue();
SearchScope scope = scopeComboBox.getValue();

if (baseDn == null || baseDn.trim().isEmpty()) {
showError("Please enter a search base DN.");
return;
}

if (filter == null || filter.trim().isEmpty()) {
showError("Please enter a search filter.");
return;
}

if (scope == null) {
showError("Please select a search scope.");
return;
}

// Parse return attributes
List<String> requestedAttributes = new ArrayList<>();
if (returnAttributesText != null && !returnAttributesText.trim().isEmpty()) {
String[] attrs = returnAttributesText.split(",");
for (String attr : attrs) {
 String trimmed = attr.trim();
 if (!trimmed.isEmpty()) {
 // Basic validation for attribute names (allow alphanumeric, hyphens, and dots)
 if (trimmed.matches("[a-zA-Z0-9\\-\\.\\+]+")) {
 requestedAttributes.add(trimmed);
 } else {
 showError("Invalid attribute name: " + trimmed + ". Attribute names should contain only letters, numbers, hyphens, and dots.");
 return;
 }
}
}
}

try {
List<LdapEntry> results;
if (requestedAttributes.isEmpty()) {
// Use the existing method that returns all attributes
results = ldapService.searchEntries(
serverConfig.getId(),
baseDn.trim(),
filter.trim(),
scope
);
} else {
// Use a new method that accepts specific attributes
results = ldapService.searchEntries(
serverConfig.getId(),
baseDn.trim(),
filter.trim(),
scope,
requestedAttributes.toArray(new String[0])
);
}

// Create search result with requested attributes
SearchResult searchResult = new SearchResult(results, requestedAttributes);

// Notify listeners
searchListeners.forEach(listener -> listener.accept(searchResult));

if (results.isEmpty()) {
showInfo("No entries found matching the search criteria.");
} else {
showSuccess("Found " + results.size() + " entries.");
}

} catch (LDAPException e) {
showError("Search failed: " + e.getMessage());
}
}

private void clearResults() {
// Notify listeners with empty search result
SearchResult emptyResult = new SearchResult(new ArrayList<>(), new ArrayList<>());
searchListeners.forEach(listener -> listener.accept(emptyResult));
showInfo("Search results cleared.");
}

public void addSearchListener(Consumer<SearchResult> listener) {
searchListeners.add(listener);
}

public void removeSearchListener(Consumer<SearchResult> listener) {
searchListeners.remove(listener);
}

public void setBaseDn(String baseDn) {
baseDnField.setValue(baseDn != null ? baseDn : "");
}

public void setFilter(String filter) {
filterField.setValue(filter != null ? filter : "(objectClass=*)");
}

public void setScope(SearchScope scope) {
scopeComboBox.setValue(scope != null ? scope : SearchScope.SUB);
}

public void setReturnAttributes(String attributes) {
returnAttributesField.setValue(attributes != null ? attributes : "");
}

private void showSuccess(String message) {
Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
}

private void showError(String message) {
Notification notification = Notification.show(message, 5000, Notification.Position.BOTTOM_END);
notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
}

private void showInfo(String message) {
Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
}
}