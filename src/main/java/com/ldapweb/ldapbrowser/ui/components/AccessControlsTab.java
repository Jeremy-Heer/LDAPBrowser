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
  private EffectiveRightsTab effectiveRightsTab;

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

    Tab effectiveRightsTabComponent = new Tab("Effective Rights");
    effectiveRightsTab = new EffectiveRightsTab(ldapService);
    tabSheet.add(effectiveRightsTabComponent, effectiveRightsTab);

    // Add selection listener to load data only when tab is selected
    tabSheet.addSelectedChangeListener(event -> {
      if (serverConfig == null) {
        return;
      }
      
      var selectedTab = event.getSelectedTab();
      if (selectedTab == globalAccessControlTabComponent) {
        globalAccessControlTab.loadData();
      } else if (selectedTab == entryAccessControlTabComponent) {
        entryAccessControlTab.loadData();
      } else if (selectedTab == effectiveRightsTabComponent) {
        effectiveRightsTab.loadData();
      }
    });

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
    effectiveRightsTab.setServerConfig(config);
  }

  /**
   * Loads data for the currently selected access control sub-tab.
   */
  public void loadData() {
    if (serverConfig == null || tabSheet == null) {
      return;
    }

    // Only load data for the currently selected tab
    var selectedTab = tabSheet.getSelectedTab();
    if (selectedTab != null) {
      String tabLabel = selectedTab.getLabel();
      switch (tabLabel) {
        case "Global Access Control":
          globalAccessControlTab.loadData();
          break;
        case "Entry Access Control":
          entryAccessControlTab.loadData();
          break;
        case "Effective Rights":
          effectiveRightsTab.loadData();
          break;
      }
    }
  }
}
