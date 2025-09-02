package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab component for displaying entry access control information.
 * Shows entries with ACI attributes.
 */
public class EntryAccessControlTab extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private Grid<EntryAciInfo> aciGrid;

  /**
   * Constructs a new EntryAccessControlTab.
   *
   * @param ldapService the LDAP service for executing searches
   */
  public EntryAccessControlTab(LdapService ldapService) {
    this.ldapService = ldapService;
    initUI();
  }

  private void initUI() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    H3 title = new H3("Entry Access Control");
    add(title);

    Div description = new Div();
    description.setText("Entries with Access Control Instructions (ACIs) defined at the entry level.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    add(description);

    // Grid for displaying entry DN and ACI values
    aciGrid = new Grid<>(EntryAciInfo.class, false);
    aciGrid.addColumn(EntryAciInfo::getDn)
        .setHeader("Entry DN")
        .setAutoWidth(true)
        .setFlexGrow(1);
    
    aciGrid.addColumn(EntryAciInfo::getAciValue)
        .setHeader("Access Control Instruction (ACI)")
        .setAutoWidth(true)
        .setFlexGrow(2);

    aciGrid.setSizeFull();
    add(aciGrid);
    setFlexGrow(1, aciGrid);
  }

  /**
   * Sets the server configuration for this tab.
   *
   * @param config the server configuration
   */
  public void setServerConfig(LdapServerConfig config) {
    this.serverConfig = config;
  }

  /**
   * Loads entry access control data from the LDAP server.
   */
  public void loadData() {
    if (serverConfig == null) {
      showError("No server selected");
      return;
    }

    if (!ldapService.isConnected(serverConfig.getId())) {
      showError("Not connected to server: " + serverConfig.getName());
      return;
    }

    try {
      // Get the default search base from the server configuration
      String baseDn = getDefaultSearchBase();
      if (baseDn == null) {
        showError("No default search base configured for server");
        return;
      }

      // Search for entries with ACIs
      List<LdapEntry> entries = ldapService.searchEntries(
          serverConfig.getId(),
          baseDn,
          "(aci=*)",
          SearchScope.SUB,
          "aci"
      );

      List<EntryAciInfo> aciInfoList = new ArrayList<>();

      for (LdapEntry entry : entries) {
        List<String> acis = entry.getAttributeValues("aci");
        if (acis != null) {
          for (String aci : acis) {
            aciInfoList.add(new EntryAciInfo(entry.getDn(), aci));
          }
        }
      }

      aciGrid.setItems(aciInfoList);

      if (aciInfoList.isEmpty()) {
        showInfo("No entry access control instructions found");
      } else {
        showSuccess("Found " + aciInfoList.size() + " entry ACI(s)");
      }

    } catch (LDAPException e) {
      showError("Failed to search for entry ACIs: " + e.getMessage());
      aciGrid.setItems(new ArrayList<>());
    }
  }

  private String getDefaultSearchBase() {
    if (serverConfig == null) {
      return null;
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
        return namingContexts.get(0); // Use the first naming context
      }
    } catch (LDAPException e) {
      // Fall back to common defaults
    }
    
    // Common fallbacks based on server type or host
    String host = serverConfig.getHost().toLowerCase();
    if (host.contains("example")) {
      return "dc=example,dc=com";
    }
    
    return "dc=local"; // Last resort fallback
  }

  private void showError(String message) {
    Notification n = Notification.show(message, 5000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private void showInfo(String message) {
    Notification n = Notification.show(message, 3000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
  }

  private void showSuccess(String message) {
    Notification n = Notification.show(message, 3000, Notification.Position.TOP_END);
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  /**
   * Data class for entry ACI information.
   */
  public static class EntryAciInfo {
    private final String dn;
    private final String aciValue;

    public EntryAciInfo(String dn, String aciValue) {
      this.dn = dn;
      this.aciValue = aciValue;
    }

    public String getDn() {
      return dn;
    }

    public String getAciValue() {
      return aciValue;
    }
  }
}
