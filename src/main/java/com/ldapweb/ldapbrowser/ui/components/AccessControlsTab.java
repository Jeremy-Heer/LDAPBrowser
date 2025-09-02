package com.ldapweb.ldapbrowser.ui.components;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;

/**
 * Access Controls tab component containing all access control sub-tabs.
 * This is used as a tab within the ServersView.
 */
public class AccessControlsTab extends VerticalLayout {

  private final LdapService ldapService;
  private LdapServerConfig serverConfig;
  
  private TabSheet tabSheet;
  private GlobalAccessControlTab globalAccessControlTab;
  private EntryAccessControlTab entryAccessControlTab;
  private ResourceLimitsTab resourceLimitsTab;
  private PrivilegesTab privilegesTab;

  /**
   * Constructs a new AccessControlsTab.
   *
   * @param ldapService the LDAP service for executing searches
   */
  public AccessControlsTab(LdapService ldapService) {
    this.ldapService = ldapService;
    initUI();
  }

  private void initUI() {
    setSizeFull();
    setPadding(false);
    setSpacing(false);

    initHeader();
    initTabs();
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

  /**
   * Sets the server configuration for this tab and all sub-tabs.
   *
   * @param config the server configuration
   */
  public void setServerConfig(LdapServerConfig config) {
    this.serverConfig = config;
    
    // Update all sub-tabs with server config
    globalAccessControlTab.setServerConfig(config);
    entryAccessControlTab.setServerConfig(config);
    resourceLimitsTab.setServerConfig(config);
    privilegesTab.setServerConfig(config);
  }

  /**
   * Loads data for all access control sub-tabs.
   */
  public void loadData() {
    if (serverConfig == null) {
      return;
    }

    // Trigger data loading for all sub-tabs
    globalAccessControlTab.loadData();
    entryAccessControlTab.loadData();
    resourceLimitsTab.loadData();
    privilegesTab.loadData();
  }
}
