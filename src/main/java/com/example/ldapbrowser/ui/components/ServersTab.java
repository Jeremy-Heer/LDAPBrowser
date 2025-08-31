package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import java.util.function.Consumer;

/**
 * Servers tab containing External and Internal LDAP server management.
 */
public class ServersTab extends VerticalLayout {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  // Sub-tabs
  private TabSheet tabSheet;
  private Tab externalTab;
  private Tab internalTab;

  // Components
  private ExternalServersTab externalServersTab;
  private InternalServersTab internalServersTab;

  // Event listeners
  private Consumer<LdapServerConfig> connectionListener;
  private Runnable disconnectionListener;

  /**
   * Constructs a new ServersTab with the given services and listener.
   *
   * @param ldapService the LDAP service
   * @param configurationService the configuration service
   * @param environmentRefreshListener the environment refresh listener
   * @param inMemoryLdapService the in-memory LDAP service
   */
  public ServersTab(LdapService ldapService, ConfigurationService configurationService,
      EnvironmentRefreshListener environmentRefreshListener,
      InMemoryLdapService inMemoryLdapService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;

    initializeComponents(environmentRefreshListener);
    setupLayout();
  }

  private void initializeComponents(EnvironmentRefreshListener environmentRefreshListener) {
    // Create sub-tabs
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    // External servers tab (existing connections functionality)
    externalTab = new Tab("External");
    externalServersTab = new ExternalServersTab(
      ldapService,
      configurationService,
      environmentRefreshListener,
      inMemoryLdapService
    );
    tabSheet.add(externalTab, externalServersTab);

    // Internal servers tab (UnboundID in-memory servers)
    internalTab = new Tab("Internal");
    internalServersTab = new InternalServersTab(
      ldapService,
      environmentRefreshListener,
      inMemoryLdapService,
      configurationService
    );
    tabSheet.add(internalTab, internalServersTab);

    // Set External as the default selected tab
    tabSheet.setSelectedTab(externalTab);

    // Forward events from sub-tabs
    externalServersTab.setConnectionListener(config -> {
      if (connectionListener != null) {
        connectionListener.accept(config);
      }
    });

    externalServersTab.setDisconnectionListener(() -> {
      if (disconnectionListener != null) {
        disconnectionListener.run();
      }
    });

    internalServersTab.setConnectionListener(config -> {
      if (connectionListener != null) {
        connectionListener.accept(config);
      }
    });

    internalServersTab.setDisconnectionListener(() -> {
      if (disconnectionListener != null) {
        disconnectionListener.run();
      }
    });
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(false);
    setSpacing(false);

    add(tabSheet);
    setFlexGrow(1, tabSheet);
  }

  public void refreshServerList() {
    externalServersTab.refreshServerList();
    internalServersTab.refreshServerList();
  }

  public void updateConnectionButtons() {
    externalServersTab.updateConnectionButtons();
    internalServersTab.updateConnectionButtons();
  }

  /**
   * Retrieves the currently selected LDAP server configuration.
   *
   * <p>This method checks both the external and internal servers tabs for a selected server.
   * If a server is selected in the external servers tab, it returns that configuration.
   * Otherwise, it returns the selected server configuration from the internal servers tab.
   *
   * @return the selected {@link LdapServerConfig}, or {@code null} if no server is selected.
   */
  public LdapServerConfig getSelectedServer() {
    // Check both external and internal tabs for connected servers
    LdapServerConfig external = externalServersTab.getSelectedServer();
    if (external != null) {
      return external;
    }
    return internalServersTab.getSelectedServer();
  }

  /**
   * Sets the selected server. This method is legacy and no longer needed.
   *
   * @param config the LDAP server configuration
   */
  public void setSelectedServer(LdapServerConfig config) {
    // This method is legacy and no longer needed
  }

  public void clear() {
    externalServersTab.clear();
    internalServersTab.clear();
  }

  public void setConnectionListener(Consumer<LdapServerConfig> listener) {
    this.connectionListener = listener;
  }

  public void setDisconnectionListener(Runnable listener) {
    this.disconnectionListener = listener;
  }
}