package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab component for displaying privilege information.
 * Shows entries with privilege name attributes.
 */
public class PrivilegesTab extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private Grid<PrivilegeInfo> privilegesGrid;
  private boolean dataLoaded = false;

  /**
   * Constructs a new PrivilegesTab.
   *
   * @param ldapService the LDAP service for executing searches
   */
  public PrivilegesTab(LdapService ldapService) {
    this.ldapService = ldapService;
    initUi();
  }

  private void initUi() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    H3 title = new H3("Privileges");
    add(title);

    Div description = new Div();
    description.setText("Entries with privilege name configurations assigned to users or groups.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    add(description);

    // Grid for displaying entry DN and privilege values
    privilegesGrid = new Grid<>(PrivilegeInfo.class, false);
    privilegesGrid.addColumn(PrivilegeInfo::getDn)
        .setHeader("Entry DN")
        .setAutoWidth(true)
        .setFlexGrow(2);
    
    privilegesGrid.addColumn(PrivilegeInfo::getPrivilegeName)
        .setHeader("Privilege Name")
        .setAutoWidth(true)
        .setFlexGrow(1);

    privilegesGrid.setSizeFull();
    add(privilegesGrid);
    setFlexGrow(1, privilegesGrid);
  }

  /**
   * Sets the server configuration for this tab.
   *
   * @param config the server configuration
   */
  public void setServerConfig(LdapServerConfig config) {
    this.serverConfig = config;
    this.dataLoaded = false; // Reset loading state when server changes
  }

  /**
   * Loads privileges data from the LDAP server.
   */
  public void loadData() {
    if (dataLoaded) {
      return; // Data already loaded
    }
    
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

      // Search for entries with privilege names
      List<LdapEntry> entries = ldapService.searchEntries(
          serverConfig.getId(),
          baseDn,
          "(ds-privilege-name=*)",
          SearchScope.SUB,
          "ds-privilege-name"
      );

      List<PrivilegeInfo> privilegeInfoList = new ArrayList<>();

      for (LdapEntry entry : entries) {
        List<String> privilegeNames = entry.getAttributeValues("ds-privilege-name");
        if (privilegeNames != null) {
          for (String privilegeName : privilegeNames) {
            privilegeInfoList.add(new PrivilegeInfo(entry.getDn(), privilegeName));
          }
        }
      }

      privilegesGrid.setItems(privilegeInfoList);

      if (privilegeInfoList.isEmpty()) {
        showInfo("No privilege configurations found");
      } else {
        showSuccess("Found " + privilegeInfoList.size() + " privilege assignment(s)");
      }
      
      dataLoaded = true; // Mark data as loaded

    } catch (LDAPException e) {
      showError("Failed to search for privileges: " + e.getMessage());
      privilegesGrid.setItems(new ArrayList<>());
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
   * Data class for privilege information.
   */
  public static class PrivilegeInfo {
    private final String dn;
    private final String privilegeName;

    public PrivilegeInfo(String dn, String privilegeName) {
      this.dn = dn;
      this.privilegeName = privilegeName;
    }

    public String getDn() {
      return dn;
    }

    public String getPrivilegeName() {
      return privilegeName;
    }
  }
}
