package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.ldapweb.ldapbrowser.model.LdapEntry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import java.util.List;

/**
 * Tab component for displaying global access control information.
 * Shows ds-cfg-global-aci attributes from the Access Control Handler.
 */
public class GlobalAccessControlTab extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private Grid<String> aciGrid;

  /**
   * Constructs a new GlobalAccessControlTab.
   *
   * @param ldapService the LDAP service for executing searches
   */
  public GlobalAccessControlTab(LdapService ldapService) {
    this.ldapService = ldapService;
    initUI();
  }

  private void initUI() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    H3 title = new H3("Global Access Control");
    add(title);

    Div description = new Div();
    description.setText("Global Access Control Instructions (ACIs) from the Access Control Handler configuration.");
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    add(description);

    // Grid for displaying ACI values
    aciGrid = new Grid<>(String.class, false);
    aciGrid.addColumn(aci -> aci)
        .setHeader("Access Control Instruction (ACI)")
        .setAutoWidth(true)
        .setFlexGrow(1);

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
   * Loads global access control data from the LDAP server.
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
      // Search for global ACIs in the Access Control Handler
      List<LdapEntry> entries = ldapService.searchEntries(
          serverConfig.getId(),
          "cn=Access Control Handler,cn=config",
          "(objectClass=*)",
          SearchScope.BASE,
          "ds-cfg-global-aci"
      );

      List<String> aciValues = new ArrayList<>();

      for (LdapEntry entry : entries) {
        List<String> acis = entry.getAttributeValues("ds-cfg-global-aci");
        if (acis != null) {
          aciValues.addAll(acis);
        }
      }

      aciGrid.setItems(aciValues);

      if (aciValues.isEmpty()) {
        showInfo("No global access control instructions found");
      } else {
        showSuccess("Found " + aciValues.size() + " global ACI(s)");
      }

    } catch (LDAPException e) {
      showError("Failed to search for global ACIs: " + e.getMessage());
      aciGrid.setItems(new ArrayList<>());
    }
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
}
