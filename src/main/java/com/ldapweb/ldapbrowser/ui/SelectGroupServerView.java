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
 * Lightweight route used to select a server by id within a group and forward to ServersView.
 */
@Route(value = "group/:group/:sid", layout = MainLayout.class)
@PageTitle("Select Server")
@AnonymousAllowed
public class SelectGroupServerView extends Div implements BeforeEnterObserver {

  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;

  /**
   * Constructs a new SelectGroupServerView.
   *
   * @param configurationService the configuration service
   * @param inMemoryLdapService  the in-memory LDAP service
   * @param selectionService     the server selection service
   */
  public SelectGroupServerView(ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      ServerSelectionService selectionService) {
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;
    setVisible(false);
  }

  /**
   * Handles the before-enter event to select a server by ID within a group and
   * forward to ServersView.
   *
   * @param event the before-enter event
   */
  @Override
  @SuppressWarnings("unused")
  public void beforeEnter(BeforeEnterEvent event) {
    String group = event.getRouteParameters().get("group").orElse(null);
    String sid = event.getRouteParameters().get("sid").orElse(null);
    
    if (sid != null) {
      // Find and select the server
      LdapServerConfig cfg = configurationService.getAllConfigurations().stream()
          .filter(c -> sid.equals(c.getId()))
          .findFirst()
          .orElseGet(() -> inMemoryLdapService.getAllInMemoryServers().stream()
              .filter(c -> sid.equals(c.getId()))
              .findFirst()
              .orElse(null));
      if (cfg != null) {
        selectionService.setSelected(cfg);
      }
    }
    
    // Always forward to servers view after selection
    event.forwardTo(ServersView.class);
  }
}
