package com.ldapweb.ldapbrowser.ui;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Lightweight route used to select a server by id and forward to ServersView.
 */
@Route(value = "server/:sid", layout = MainLayout.class)
@PageTitle("Server")
@AnonymousAllowed
public class ServerView extends Div implements BeforeEnterObserver {

  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;

  /**
   * Constructs a new ServerView.
   *
   * @param configurationService the configuration service
   * @param inMemoryLdapService  the in-memory LDAP service
   * @param selectionService     the server selection service
   */
  public ServerView(ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      ServerSelectionService selectionService) {
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;
    setVisible(false);
  }

  /**
   * Handles the before-enter event to select a server by ID and forward to
   * ServersView.
   *
   * @param event the before-enter event
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String id = event.getRouteParameters().get("sid").orElse(null);
    System.out.println("ServerView: Processing server ID: " + id);
    
    if (id != null) {
      LdapServerConfig cfg = configurationService.getAllConfigurations().stream()
          .filter(c -> id.equals(c.getId()))
          .findFirst()
          .orElseGet(() -> inMemoryLdapService.getAllInMemoryServers().stream()
              .filter(c -> id.equals(c.getId()))
              .findFirst()
              .orElse(null));
      
      if (cfg != null) {
        selectionService.setSelected(cfg);
        // Forward directly to the ServersView with the server ID parameter
        event.forwardTo("servers/" + id);
      } else {
        // If server not found, forward to the servers list view
        event.forwardTo("servers");
      }
    } else {
      // If no id is provided, forward to the servers list view
      event.forwardTo("servers");
    }
  }
}
