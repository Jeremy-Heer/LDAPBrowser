package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Root application layout with a side drawer and a top navbar used as view header.
 */
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final H1 viewTitle = new H1();
    private final H1 contextTitle = new H1();
    private final HorizontalLayout connectionChip = new HorizontalLayout();

    private final ConfigurationService configurationService;
    private final InMemoryLdapService inMemoryLdapService;
    private final ServerSelectionService selectionService;
    private final LdapService ldapService;

    private final Map<String, SideNavItem> serverItemIndex = new HashMap<>();
    private final SideNav nav;
    private final SideNavItem serversRoot;
    private final SideNavItem groupSearchRoot;

    public MainLayout(ConfigurationService configurationService,
                      InMemoryLdapService inMemoryLdapService,
                      ServerSelectionService selectionService,
                      LdapService ldapService) {
        this.configurationService = configurationService;
        this.inMemoryLdapService = inMemoryLdapService;
        this.selectionService = selectionService;
        this.ldapService = ldapService;
        // Navbar content (to the side of the drawer toggle)
        DrawerToggle toggle = new DrawerToggle();
    viewTitle.getStyle().set("font-size", "var(--lumo-font-size-l)")
        .set("margin", "0");
    contextTitle.getStyle().set("font-size", "var(--lumo-font-size-m)")
        .set("margin", "0 0 0 var(--lumo-space-m)")
        .set("color", "var(--lumo-secondary-text-color)");

        setupConnectionChip();
        addToNavbar(toggle, viewTitle, contextTitle, connectionChip);

    // Drawer content
    nav = new SideNav();
    // Servers accordion-like group
    serversRoot = new SideNavItem("Servers", ServersView.class);
        serversRoot.setPrefixComponent(new Icon(VaadinIcon.SERVER));
        populateServers(serversRoot);
        nav.addItem(serversRoot);

    // Group Search - shows only groups, clicking goes to group search view
    groupSearchRoot = new SideNavItem("Group Search", (String) null);
    groupSearchRoot.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
    populateGroups(groupSearchRoot);
    nav.addItem(groupSearchRoot);

        // Settings link
        SideNavItem settingsItem = new SideNavItem("Settings", SettingsView.class);
        settingsItem.setPrefixComponent(new Icon(VaadinIcon.COG));
        nav.addItem(settingsItem);

        Scroller scroller = new Scroller(nav);
        scroller.addClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller);

        setPrimarySection(Section.DRAWER);
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
        // Update context title depending on current view
        if (getContent() instanceof GroupSearchView gsv) {
            String grp = gsv.getGroupName();
            contextTitle.setText(grp != null && !grp.isBlank() ? ("Group: " + grp) : "Group Search");
            // In group mode, hide single-server connection chip to avoid confusion
            connectionChip.setVisible(false);
        } else {
            connectionChip.setVisible(true);
        }
    }

    private String getCurrentPageTitle() {
        if (getContent() == null) {
            return "";
        }
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title != null ? title.value() : "";
    }

    private void populateServers(SideNavItem root) {
        root.removeAll();
        serverItemIndex.clear();

        // Unified groups map (external + internal)
        Map<String, SideNavItem> groups = new HashMap<>();
        SideNavItem ungrouped = new SideNavItem("Ungrouped", (String) null);
        ungrouped.setPrefixComponent(new Icon(VaadinIcon.FOLDER));

        // External servers
        for (LdapServerConfig cfg : configurationService.getAllConfigurations()) {
            String group = cfg.getGroup() != null && !cfg.getGroup().isBlank() ? cfg.getGroup().trim() : null;
            SideNavItem parent = group == null ? ungrouped : groups.computeIfAbsent(group, g -> {
                SideNavItem grp = new SideNavItem(g, (String) null);
                grp.setPrefixComponent(new Icon(VaadinIcon.FOLDER_OPEN));
                return grp;
            });

            String name = cfg.getName() != null ? cfg.getName() : cfg.getHost();
            SideNavItem item = new SideNavItem(name, "/select/" + cfg.getId());
            item.setPrefixComponent(new Icon(VaadinIcon.DATABASE));
            parent.addItem(item);
            serverItemIndex.put(cfg.getId(), item);
        }

        // Internal running servers
        for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
            if (inMemoryLdapService.isServerRunning(cfg.getId())) {
                String group = cfg.getGroup() != null && !cfg.getGroup().isBlank() ? cfg.getGroup().trim() : null;
                SideNavItem parent = group == null ? ungrouped : groups.computeIfAbsent(group, g -> {
                    SideNavItem grp = new SideNavItem(g, (String) null);
                    grp.setPrefixComponent(new Icon(VaadinIcon.FOLDER_OPEN));
                    return grp;
                });

                String name = (cfg.getName() != null ? cfg.getName() : cfg.getHost()) + " (internal)";
                SideNavItem item = new SideNavItem(name, "/select/" + cfg.getId());
                item.setPrefixComponent(new Icon(VaadinIcon.CUBE));
                parent.addItem(item);
                serverItemIndex.put(cfg.getId(), item);
            }
        }

        // Add groups sorted by name, then ungrouped if any
        groups.keySet().stream().sorted().forEach(k -> root.addItem(groups.get(k)));
        if (!ungrouped.getItems().isEmpty()) {
            root.addItem(ungrouped);
        }
    }

    private void populateGroups(SideNavItem root) {
        root.removeAll();
        // Collect groups from external and running internal servers
        Set<String> groups = new TreeSet<>();
        for (LdapServerConfig cfg : configurationService.getAllConfigurations()) {
            if (cfg.getGroup() != null && !cfg.getGroup().isBlank()) {
                groups.add(cfg.getGroup().trim());
            }
        }
        for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
            if (inMemoryLdapService.isServerRunning(cfg.getId())) {
                if (cfg.getGroup() != null && !cfg.getGroup().isBlank()) {
                    groups.add(cfg.getGroup().trim());
                }
            }
        }

        // Add a nav item for each group; route to GroupSearchView
        for (String group : groups) {
            String encoded = URLEncoder.encode(group, StandardCharsets.UTF_8);
            SideNavItem item = new SideNavItem(group, "/group-search/" + encoded);
            item.setPrefixComponent(new Icon(VaadinIcon.FOLDER_OPEN));
            root.addItem(item);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Show current server selection in the navbar context area
        LdapServerConfig selected = selectionService.getSelected();
        updateSelectionUI(selected);
        selectionService.addListener(this::updateSelectionUI);
    }

    private void updateSelectionUI(LdapServerConfig cfg) {
        String suffix = cfg != null ? (cfg.getName() != null ? cfg.getName() : cfg.getHost()) : "None";
        contextTitle.setText("Server: " + suffix);
        updateConnectionChip(cfg);
        updateDrawerHighlight(cfg);
    }

    private void setupConnectionChip() {
        connectionChip.setSpacing(true);
        connectionChip.setPadding(false);
        connectionChip.getStyle().set("margin-left", "var(--lumo-space-m)");
    connectionChip.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        connectionChip.setVisible(true);
        // Initial content
        updateConnectionChip(null);
    }

    private void updateConnectionChip(LdapServerConfig cfg) {
        connectionChip.removeAll();
        Icon dot = new Icon(VaadinIcon.CIRCLE);
        dot.setSize("10px");
        boolean connected = cfg != null && ldapService.isConnected(cfg.getId());
        dot.getStyle().set("color", connected ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
        Span text = new Span(connected ? "Connected" : "Disconnected");
        text.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        connectionChip.add(dot, text);
    }

    private void updateDrawerHighlight(LdapServerConfig cfg) {
        // Clear previous highlights
        for (SideNavItem item : serverItemIndex.values()) {
            item.setSuffixComponent(null);
        }
        if (cfg != null) {
            SideNavItem selectedItem = serverItemIndex.get(cfg.getId());
            if (selectedItem != null) {
                Icon check = new Icon(VaadinIcon.CHECK);
                check.setSize("16px");
                check.getStyle().set("color", "var(--lumo-success-color)");
                selectedItem.setSuffixComponent(check);
            }
        }
    }

    public void refreshServerListInDrawer() {
        populateServers(serversRoot);
    populateGroups(groupSearchRoot);
        // Re-apply highlight and connection chip based on current selection
        updateSelectionUI(selectionService.getSelected());
    }
}
