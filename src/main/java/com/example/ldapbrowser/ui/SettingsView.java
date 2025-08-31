package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.example.ldapbrowser.ui.components.EnvironmentRefreshListener;
import com.example.ldapbrowser.ui.components.ExternalServersTab;
import com.example.ldapbrowser.ui.components.InternalServersTab;
import com.example.ldapbrowser.ui.components.LogsTab;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * View for managing application settings, including external servers, internal
 * servers, and logs.
 */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@AnonymousAllowed
public class SettingsView extends VerticalLayout implements EnvironmentRefreshListener {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final LoggingService loggingService;

  private TabSheet tabSheet;
  private ExternalServersTab externalServersTab;
  private InternalServersTab internalServersTab;
  private LogsTab logsTab;

  /**
   * Constructs the SettingsView with the required services.
   *
   * @param ldapService          the LDAP service
   * @param configurationService the configuration service
   * @param inMemoryLdapService  the in-memory LDAP service
   * @param loggingService       the logging service
   */
  public SettingsView(LdapService ldapService,
      ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      LoggingService loggingService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.loggingService = loggingService;

    setSizeFull();
    setPadding(false);
    setSpacing(false);

    initTabs();
  }

  private void initTabs() {
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    Tab externalTab = new Tab("External Servers");
    externalServersTab = new ExternalServersTab(ldapService, configurationService, this);
    addTab(externalTab, externalServersTab);

    Tab internalTab = new Tab("Internal Servers");
    internalServersTab = new InternalServersTab(ldapService, this, inMemoryLdapService);
    addTab(internalTab, internalServersTab);

    Tab logsTabT = new Tab("Logs");
    logsTab = new LogsTab(loggingService);
    addTab(logsTabT, logsTab);

    add(tabSheet);
    setFlexGrow(1, tabSheet);
  }

  private void addTab(Tab tab, Component content) {
    tabSheet.add(tab, content);
  }

  @Override
  public void onEnvironmentChange() {
    if (externalServersTab != null) {
      externalServersTab.refreshServerList();
    }
    if (internalServersTab != null) {
      internalServersTab.refreshServerList();
    }
    // Also refresh the drawer server list in MainLayout so changes are immediately
    // visible
    MainLayout mainLayout = findMainLayout();
    if (mainLayout != null) {
      mainLayout.refreshServerListInDrawer();
    }
  }

  private MainLayout findMainLayout() {
    // Try walking up the parent chain first
    java.util.Optional<com.vaadin.flow.component.Component> parent = getParent();
    while (parent.isPresent()) {
      com.vaadin.flow.component.Component c = parent.get();
      if (c instanceof MainLayout) {
        return (MainLayout) c;
      }
      parent = c.getParent();
    }
    // Fallback: look among UI's direct children
    return getUI()
        .flatMap(ui -> ui.getChildren()
            .filter(c -> c instanceof MainLayout)
            .map(c -> (MainLayout) c)
            .findFirst())
        .orElse(null);
  }
}
