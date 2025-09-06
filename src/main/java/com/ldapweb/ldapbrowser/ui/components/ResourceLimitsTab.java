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
 * Tab component for displaying resource limits information.
 * Shows entries with resource limit attributes.
 */
public class ResourceLimitsTab extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  private Grid<ResourceLimitInfo> limitsGrid;
  private boolean dataLoaded = false;

  // Resource limit attributes to search for
  private static final String[] RESOURCE_LIMIT_ATTRIBUTES = {
      "ds-rlim-size-limit",
      "ds-rlim-time-limit", 
      "ds-rlim-lookthrough-limit",
      "ds-rlim-idle-time-limit",
      "ds-rlim-ldap-join-size-limit"
  };

  /**
   * Constructs a new ResourceLimitsTab.
   *
   * @param ldapService the LDAP service for executing searches
   */
  public ResourceLimitsTab(LdapService ldapService) {
    this.ldapService = ldapService;
    initUi();
  }

  private void initUi() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    H3 title = new H3("Resource Limits");
    add(title);

    Div description = new Div();
    description.setText(
        "Entries with resource limit configurations (size, time, lookthrough, idle time, "
        + "join size limits)."
    );
    description.getStyle().set("color", "var(--lumo-secondary-text-color)")
        .set("margin-bottom", "var(--lumo-space-m)");
    add(description);

    // Grid for displaying entry DN, attribute name, and values
    limitsGrid = new Grid<>(ResourceLimitInfo.class, false);
    limitsGrid.addColumn(ResourceLimitInfo::getDn)
        .setHeader("Entry DN")
        .setAutoWidth(true)
        .setFlexGrow(2);
    
    limitsGrid.addColumn(ResourceLimitInfo::getAttributeName)
        .setHeader("Limit Type")
        .setAutoWidth(true)
        .setFlexGrow(1);

    limitsGrid.addColumn(ResourceLimitInfo::getAttributeValue)
        .setHeader("Limit Value")
        .setAutoWidth(true)
        .setFlexGrow(1);

    limitsGrid.setSizeFull();
    add(limitsGrid);
    setFlexGrow(1, limitsGrid);
  }

  /**
   * Sets the server configuration for this tab.
   *
   * @param config the server configuration
   */
  public void setServerConfig(LdapServerConfig config) {
    this.serverConfig = config;
    this.dataLoaded = false; // Reset data loaded flag when server changes
    limitsGrid.setItems(new ArrayList<>()); // Clear existing data
  }

  /**
   * Loads resource limits data from the LDAP server.
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

    // Only load once per server configuration
    if (dataLoaded) {
      return;
    }

    try {
      // Get the default search base from the server configuration
      String baseDn = getDefaultSearchBase();
      if (baseDn == null) {
        showError("No default search base configured for server");
        return;
      }

      // Build the filter for resource limit attributes
      StringBuilder filterBuilder = new StringBuilder("(|");
      for (String attr : RESOURCE_LIMIT_ATTRIBUTES) {
        filterBuilder.append("(").append(attr).append("=*)");
      }
      filterBuilder.append(")");
      String filter = filterBuilder.toString();

      // Search for entries with resource limits
      List<LdapEntry> entries = ldapService.searchEntries(
          serverConfig.getId(),
          baseDn,
          filter,
          SearchScope.SUB,
          RESOURCE_LIMIT_ATTRIBUTES
      );

      List<ResourceLimitInfo> limitInfoList = new ArrayList<>();

      for (LdapEntry entry : entries) {
        for (String attrName : RESOURCE_LIMIT_ATTRIBUTES) {
          List<String> values = entry.getAttributeValues(attrName);
          if (values != null && !values.isEmpty()) {
            for (String value : values) {
              limitInfoList.add(new ResourceLimitInfo(entry.getDn(), attrName, value));
            }
          }
        }
      }

      limitsGrid.setItems(limitInfoList);
      dataLoaded = true;

      if (limitInfoList.isEmpty()) {
        showInfo("No resource limit configurations found");
      } else {
        showSuccess("Found " + limitInfoList.size() + " resource limit configuration(s)");
      }

    } catch (LDAPException e) {
      showError("Failed to search for resource limits: " + e.getMessage());
      limitsGrid.setItems(new ArrayList<>());
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
   * Data class for resource limit information.
   */
  public static class ResourceLimitInfo {
    private final String dn;
    private final String attributeName;
    private final String attributeValue;

    /**
     * Constructs a new {@code ResourceLimitInfo} instance with the specified
     * distinguished name (DN), attribute name, and attribute value.
     *
     * @param dn the distinguished name associated with the resource limit
     * @param attributeName the name of the attribute representing the resource
     *                      limit
     * @param attributeValue the value of the resource limit attribute
     */
    public ResourceLimitInfo(String dn, String attributeName, String attributeValue) {
      this.dn = dn;
      this.attributeName = attributeName;
      this.attributeValue = attributeValue;
    }

    public String getDn() {
      return dn;
    }

    public String getAttributeName() {
      return attributeName;
    }

    public String getAttributeValue() {
      return attributeValue;
    }
  }
}
