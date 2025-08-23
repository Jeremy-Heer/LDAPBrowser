package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.example.ldapbrowser.ui.components.DirectorySearchTab;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "group-search/:group", layout = MainLayout.class)
@PageTitle("Group Search")
@AnonymousAllowed
public class GroupSearchView extends VerticalLayout implements BeforeEnterObserver {

    private final LdapService ldapService;
    private final ConfigurationService configurationService;
    private final InMemoryLdapService inMemoryLdapService;
    private final ServerSelectionService selectionService;

    private TabSheet tabSheet;
    private DirectorySearchTab directorySearchTab;
    private String groupName;

    public GroupSearchView(LdapService ldapService,
                           ConfigurationService configurationService,
                           InMemoryLdapService inMemoryLdapService,
                           LoggingService loggingService,
                           ServerSelectionService selectionService) {
        this.ldapService = ldapService;
        this.configurationService = configurationService;
        this.inMemoryLdapService = inMemoryLdapService;
        this.selectionService = selectionService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        initTabs();
    }

    private void initTabs() {
        tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        Tab directorySearchTabComponent = new Tab("Directory Search");
        directorySearchTab = new DirectorySearchTab(ldapService, configurationService, inMemoryLdapService, selectionService);
        tabSheet.add(directorySearchTabComponent, directorySearchTab);

        add(tabSheet);
        setFlexGrow(1, tabSheet);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.groupName = event.getRouteParameters().get("group").orElse("");
        if (groupName == null || groupName.isBlank()) {
            Notification.show("No group specified", 3000, Notification.Position.TOP_END);
            return;
        }

        // Build the environment set for this group from both external and running internal servers
        Set<LdapServerConfig> groupServers = new HashSet<>();
        List<LdapServerConfig> external = configurationService.getAllConfigurations().stream()
                .filter(c -> groupName.equalsIgnoreCase(safe(c.getGroup())))
                .collect(Collectors.toList());
        groupServers.addAll(external);

        // Add internal running servers matching the group
        for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
            if (inMemoryLdapService.isServerRunning(cfg.getId()) && groupName.equalsIgnoreCase(safe(cfg.getGroup()))) {
                groupServers.add(cfg);
            }
        }

        // Provide the environments to the DirectorySearchTab and refresh UI state
        directorySearchTab.setEnvironmentSupplier(() -> groupServers);
        directorySearchTab.refreshEnvironments();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Expose the active group name for layout/context display.
     */
    public String getGroupName() {
        return groupName;
    }
}
