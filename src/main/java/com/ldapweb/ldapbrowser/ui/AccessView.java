package com.ldapweb.ldapbrowser.ui;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.ldapweb.ldapbrowser.ui.components.GlobalAccessControlTab;
import com.ldapweb.ldapbrowser.ui.components.EntryAccessControlTab;
import com.ldapweb.ldapbrowser.ui.components.ResourceLimitsTab;
import com.ldapweb.ldapbrowser.ui.components.PrivilegesTab;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Optional;

/**
 * View for managing LDAP access controls, including global access controls, 
 * entry access controls, resource limits, and privileges.
 */
@Route(value = "access", layout = MainLayout.class)
@PageTitle("Access Controls")
@AnonymousAllowed
public class AccessView extends VerticalLayout implements BeforeEnterObserver {

  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;

  private TabSheet tabSheet;
  private GlobalAccessControlTab globalAccessControlTab;
  private EntryAccessControlTab entryAccessControlTab;
  private ResourceLimitsTab resourceLimitsTab;
  private PrivilegesTab privilegesTab;

  /**
   * Constructs the AccessView with the required services.
   *
   * @param ldapService the LDAP service
   * @param configurationService the configuration service
   * @param inMemoryLdapService the in-memory LDAP service
   * @param selectionService the server selection service
   */
  public AccessView(LdapService ldapService,
      ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      ServerSelectionService selectionService) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;

    setSizeFull();
    setPadding(false);
    setSpacing(false);

    initHeader();
    initTabs();
    bindSelection();
  }

  private void initHeader() {
    HorizontalLayout header = new HorizontalLayout();
    header.setAlignItems(Alignment.CENTER);
    header.setPadding(true);
    header.setSpacing(true);

    Icon accessIcon = new Icon(VaadinIcon.SHIELD);
    accessIcon.setSize("24px");
    accessIcon.getStyle().set("color", "var(--lumo-primary-color)");

    H2 title = new H2("Access Controls");
    title.getStyle().set("margin", "0");

    header.add(accessIcon, title);
    add(header);
  }

  private void initTabs() {
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    Tab globalAccessControlTabComponent = new Tab("Global Access Control");
    globalAccessControlTab = new GlobalAccessControlTab(ldapService);
    tabSheet.add(globalAccessControlTabComponent, globalAccessControlTab);

    Tab entryAccessControlTabComponent = new Tab("Entry Access Control");
    entryAccessControlTab = new EntryAccessControlTab(ldapService);
    tabSheet.add(entryAccessControlTabComponent, entryAccessControlTab);

    Tab resourceLimitsTabComponent = new Tab("Resource Limits");
    resourceLimitsTab = new ResourceLimitsTab(ldapService);
    tabSheet.add(resourceLimitsTabComponent, resourceLimitsTab);

    Tab privilegesTabComponent = new Tab("Privileges");
    privilegesTab = new PrivilegesTab(ldapService);
    tabSheet.add(privilegesTabComponent, privilegesTab);

    add(tabSheet);
    setFlexGrow(1, tabSheet);
  }

  private void bindSelection() {
    LdapServerConfig current = selectionService.getSelected();
    applySelection(current);
    selectionService.addListener(this::applySelection);
  }

  private void applySelection(LdapServerConfig config) {
    if (config == null) {
      Notification n = Notification.show(
          "Select a server from the drawer to begin", 3000, Notification.Position.TOP_END);
      n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
      return;
    }

    try {
      if (!ldapService.isConnected(config.getId())) {
        ldapService.connect(config);
      }
    } catch (Exception e) {
      Notification n = Notification.show(
          "Failed to connect: " + e.getMessage(), 5000, Notification.Position.TOP_END);
      n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    // Update tabs with server config
    globalAccessControlTab.setServerConfig(config);
    entryAccessControlTab.setServerConfig(config);
    resourceLimitsTab.setServerConfig(config);
    privilegesTab.setServerConfig(config);

    // Trigger data loading
    globalAccessControlTab.loadData();
    entryAccessControlTab.loadData();
    resourceLimitsTab.loadData();
    privilegesTab.loadData();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Optional<String> sid = event.getLocation().getQueryParameters().getParameters()
        .getOrDefault("sid", java.util.List.of()).stream().findFirst();
    if (sid.isPresent()) {
      String id = sid.get();
      // Try to resolve from external first
      LdapServerConfig cfg = configurationService.getAllConfigurations().stream()
          .filter(c -> id.equals(c.getId()))
          .findFirst()
          .orElseGet(() -> inMemoryLdapService.getAllInMemoryServers().stream()
              .filter(c -> id.equals(c.getId()))
              .findFirst()
              .orElse(null));
      if (cfg != null) {
        selectionService.setSelected(cfg);
      }
    }
  }
}
