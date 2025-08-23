package com.example.ldapbrowser.ui;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.ServerSelectionService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Lightweight route used to select a server by id and forward to ServersView.
 */
@Route(value = "select/:sid", layout = MainLayout.class)
@PageTitle("Select Server")
@AnonymousAllowed
public class SelectServerView extends Div implements BeforeEnterObserver {

    private final ConfigurationService configurationService;
    private final InMemoryLdapService inMemoryLdapService;
    private final ServerSelectionService selectionService;

    public SelectServerView(ConfigurationService configurationService,
                            InMemoryLdapService inMemoryLdapService,
                            ServerSelectionService selectionService) {
        this.configurationService = configurationService;
        this.inMemoryLdapService = inMemoryLdapService;
        this.selectionService = selectionService;
        setVisible(false);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get("sid").orElse(null);
        if (id != null) {
            LdapServerConfig cfg = configurationService.getAllConfigurations().stream()
                    .filter(c -> id.equals(c.getId()))
                    .findFirst()
                    .orElseGet(() -> inMemoryLdapService.getAllInMemoryServers().stream()
                            .filter(c -> id.equals(c.getId()))
                            .findFirst().orElse(null));
            if (cfg != null) {
                selectionService.setSelected(cfg);
            }
        }
        // Always forward to servers view after selection
        event.forwardTo(ServersView.class);
    }
}
