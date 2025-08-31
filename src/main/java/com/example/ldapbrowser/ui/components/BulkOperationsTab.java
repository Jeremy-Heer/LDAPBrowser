package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Bulk Operations tab containing Import, Search, and Generate sub-tabs
 */
public class BulkOperationsTab extends VerticalLayout {

  private final LdapService ldapService;
  private final LoggingService loggingService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;

  // Environment selection removed; server is provided by container view

  // Sub-tabs
  private TabSheet tabSheet;
  private Tab importTab;
  private Tab searchTab;
  private Tab generateTab;
  private Tab groupMembershipsTab;

  // Components
  private ImportTab importTabContent;
  private BulkSearchTab searchTabContent;
  private BulkGenerateTab generateTabContent;
  private BulkGroupMembershipsTab groupMembershipsTabContent;

  public BulkOperationsTab(LdapService ldapService, LoggingService loggingService,
      ConfigurationService configurationService, InMemoryLdapService inMemoryLdapService) {
    this.ldapService = ldapService;
    this.loggingService = loggingService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;

    initializeComponents();
    setupLayout();
  }

  private void initializeComponents() {
    // Environment dropdown removed

    // Create sub-tabs
    tabSheet = new TabSheet();
    tabSheet.setSizeFull();

    // Import tab (existing LDAP Import functionality)
    importTab = new Tab("Import");
    importTabContent = new ImportTab(ldapService, loggingService);
    tabSheet.add(importTab, importTabContent);

    // Search tab (new bulk search operations)
    searchTab = new Tab("Search");
    searchTabContent = new BulkSearchTab(ldapService, loggingService);
    tabSheet.add(searchTab, searchTabContent);

    // Generate tab (new bulk generate operations)
    generateTab = new Tab("Generate");
    generateTabContent = new BulkGenerateTab(ldapService, loggingService);
    tabSheet.add(generateTab, generateTabContent);

    // Group Memberships tab (new bulk group memberships operations)
    groupMembershipsTab = new Tab("Group Memberships");
    groupMembershipsTabContent = new BulkGroupMembershipsTab(ldapService, loggingService);
    tabSheet.add(groupMembershipsTab, groupMembershipsTabContent);

    // Set Import as the default selected tab
    tabSheet.setSelectedTab(importTab);
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("bulk-operations-tab");

    // No environment selection UI at the top

    // Title with icon
    HorizontalLayout titleLayout = new HorizontalLayout();
    titleLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    titleLayout.setSpacing(true);

    Icon bulkIcon = new Icon(VaadinIcon.COGS);
    bulkIcon.setSize("20px");
    bulkIcon.getStyle().set("color", "#28a745");

    H3 title = new H3("Bulk Operations");
    title.addClassNames(LumoUtility.Margin.NONE);
    title.getStyle().set("color", "#333");

    titleLayout.add(bulkIcon, title);

    add(titleLayout, tabSheet);
    setFlexGrow(1, tabSheet);
  }

  public void setServerConfig(LdapServerConfig serverConfig) {
    importTabContent.setServerConfig(serverConfig);
    searchTabContent.setServerConfig(serverConfig);
    generateTabContent.setServerConfig(serverConfig);
    groupMembershipsTabContent.setServerConfig(serverConfig);
  }

  /**
   * Refresh the environment dropdown when environments change
   */
  public void refreshEnvironments() {
    // No environment dropdown to refresh
  }

  public void clear() {
    importTabContent.clear();
    searchTabContent.clear();
    generateTabContent.clear();
    groupMembershipsTabContent.clear();
  }
}