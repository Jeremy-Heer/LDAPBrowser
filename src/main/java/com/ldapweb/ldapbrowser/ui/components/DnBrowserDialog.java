package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.unboundid.ldap.sdk.LDAPException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for browsing and selecting DN from LDAP directory tree.
 */
public class DnBrowserDialog extends Dialog {
  
  private final LdapService ldapService;
  private final LdapServerConfig serverConfig;
  private final Consumer<String> onDnSelected;
  
  private TextField currentDnField;
  private Grid<LdapEntry> entriesGrid;
  private Button selectButton;
  private Button cancelButton;
  private String currentBaseDn;
  
  /**
   * Constructs a new DN Browser Dialog.
   *
   * @param ldapService the LDAP service
   * @param serverConfig the server configuration
   * @param onDnSelected callback when DN is selected
   */
  public DnBrowserDialog(LdapService ldapService, LdapServerConfig serverConfig, 
                        Consumer<String> onDnSelected) {
    this.ldapService = ldapService;
    this.serverConfig = serverConfig;
    this.onDnSelected = onDnSelected;
    initUI();
    loadInitialData();
  }
  
  private void initUI() {
    setHeaderTitle("Browse Directory Tree");
    setModal(true);
    setDraggable(true);
    setResizable(true);
    setWidth("700px");
    setHeight("600px");
    
    VerticalLayout content = new VerticalLayout();
    content.setPadding(false);
    content.setSpacing(true);
    
    // Description
    Div description = new Div();
    description.setText("Browse the directory tree and select an entry DN.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    content.add(description);
    
    // Current DN field
    currentDnField = new TextField("Current Base DN");
    currentDnField.setReadOnly(true);
    currentDnField.setWidthFull();
    content.add(currentDnField);
    
    // Entries grid
    entriesGrid = new Grid<>(LdapEntry.class, false);
    entriesGrid.addColumn(LdapEntry::getRdn)
        .setHeader("Name (RDN)")
        .setAutoWidth(true)
        .setFlexGrow(1);
    
    entriesGrid.addColumn(entry -> entry.isHasChildren() ? "Container" : "Entry")
        .setHeader("Type")
        .setAutoWidth(true);
    
    entriesGrid.addColumn(LdapEntry::getDn)
        .setHeader("Distinguished Name")
        .setAutoWidth(true)
        .setFlexGrow(2);
    
    // Double-click to navigate
    entriesGrid.addItemDoubleClickListener(event -> {
      LdapEntry entry = event.getItem();
      if (entry.isHasChildren()) {
        navigateTo(entry.getDn());
      }
    });
    
    // Single click to select
    entriesGrid.addSelectionListener(event -> {
      selectButton.setEnabled(!event.getAllSelectedItems().isEmpty());
    });
    
    entriesGrid.setSizeFull();
    content.add(entriesGrid);
    content.setFlexGrow(1, entriesGrid);
    
    add(content);
    
    // Footer buttons
    selectButton = new Button("Select DN", event -> selectCurrentDn());
    selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    selectButton.setEnabled(false);
    
    Button parentButton = new Button("Up", event -> navigateToParent());
    
    cancelButton = new Button("Cancel", event -> close());
    
    getFooter().add(parentButton, cancelButton, selectButton);
  }
  
  private void loadInitialData() {
    try {
      // Get default search base
      String baseDn = getDefaultSearchBase();
      navigateTo(baseDn);
    } catch (Exception e) {
      showError("Failed to load directory tree: " + e.getMessage());
    }
  }
  
  private String getDefaultSearchBase() {
    if (serverConfig == null) {
      return "";
    }
    
    // Try to get default search base from server config
    String searchBase = serverConfig.getBaseDn();
    if (searchBase != null && !searchBase.trim().isEmpty()) {
      return searchBase.trim();
    }
    
    // Try to get naming contexts from the server
    try {
      List<String> namingContexts = ldapService.getNamingContexts(serverConfig.getId());
      if (!namingContexts.isEmpty()) {
        return namingContexts.get(0);
      }
    } catch (LDAPException e) {
      // Fall back to common defaults
    }
    
    // Common fallbacks
    String host = serverConfig.getHost().toLowerCase();
    if (host.contains("example")) {
      return "dc=example,dc=com";
    }
    
    return "dc=local";
  }
  
  private void navigateTo(String dn) {
    try {
      currentBaseDn = dn;
      currentDnField.setValue(dn);
      
      // Load child entries
      List<LdapEntry> entries = ldapService.browseEntries(serverConfig.getId(), dn);
      entriesGrid.setItems(entries);
      
    } catch (LDAPException e) {
      showError("Failed to browse DN '" + dn + "': " + e.getMessage());
      entriesGrid.setItems(new ArrayList<>());
    }
  }
  
  private void navigateToParent() {
    if (currentBaseDn == null || currentBaseDn.isEmpty()) {
      return;
    }
    
    // Parse parent DN
    int firstComma = currentBaseDn.indexOf(',');
    if (firstComma > 0) {
      String parentDn = currentBaseDn.substring(firstComma + 1).trim();
      navigateTo(parentDn);
    }
  }
  
  private void selectCurrentDn() {
    LdapEntry selectedEntry = entriesGrid.asSingleSelect().getValue();
    if (selectedEntry != null && onDnSelected != null) {
      onDnSelected.accept(selectedEntry.getDn());
    } else if (currentBaseDn != null && onDnSelected != null) {
      // Allow selecting the current base DN even if no entry is selected
      onDnSelected.accept(currentBaseDn);
    }
    close();
  }
  
  private void showError(String message) {
    Notification n = Notification.show(message, 5000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }
}