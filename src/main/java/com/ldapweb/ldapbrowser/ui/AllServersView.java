package com.ldapweb.ldapbrowser.ui;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;

/**
 * View for displaying all available LDAP servers.
 * This view shows cards for all configured external and internal servers.
 */
@Route(value = "servers", layout = MainLayout.class)
@PageTitle("All Servers")
@AnonymousAllowed
public class AllServersView extends VerticalLayout {

  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;
  
  private VerticalLayout serversList;

  /**
   * Constructs the AllServersView with the required services.
   *
   * @param configurationService the configuration service
   * @param inMemoryLdapService the in-memory LDAP service
   * @param selectionService the server selection service
   */
  public AllServersView(ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      ServerSelectionService selectionService) {
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;
    
    initializeComponents();
    updateContent();
  }

  /**
   * Initializes the UI components.
   */
  private void initializeComponents() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    
    final H2 pageTitle = new H2("LDAP Servers");
    serversList = new VerticalLayout();
    serversList.setSpacing(true);
    serversList.setPadding(false);
    
    add(pageTitle, serversList);
  }

  /**
   * Updates the content to display all available servers.
   */
  private void updateContent() {
    serversList.removeAll();
    
    // Get all external servers
    List<LdapServerConfig> externalServers = configurationService.getAllConfigurations();
    
    // Get all internal servers
    List<LdapServerConfig> internalServers = inMemoryLdapService.getAllInMemoryServers().stream()
        .filter(cfg -> inMemoryLdapService.isServerRunning(cfg.getId()))
        .toList();
    
    if (externalServers.isEmpty() && internalServers.isEmpty()) {
      serversList.add(new Paragraph("No servers configured. Please add a server configuration."));
      return;
    }
    
    // Display external servers
    if (!externalServers.isEmpty()) {
      H2 externalTitle = new H2("External Servers");
      externalTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
      serversList.add(externalTitle);
      
      for (LdapServerConfig server : externalServers) {
        Div serverCard = createServerCard(server, false);
        serversList.add(serverCard);
      }
    }
    
    // Display internal servers
    if (!internalServers.isEmpty()) {
      H2 internalTitle = new H2("Internal Servers");
      internalTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
      serversList.add(internalTitle);
      
      for (LdapServerConfig server : internalServers) {
        Div serverCard = createServerCard(server, true);
        serversList.add(serverCard);
      }
    }
  }

  /**
   * Creates a card component for displaying server information.
   *
   * @param server the server configuration
   * @param isInternal whether this is an internal server
   * @return the server card component
   */
  private Div createServerCard(LdapServerConfig server, boolean isInternal) {
    Div card = new Div();
    card.addClassName("server-card");
    card.getStyle()
        .set("border", "1px solid var(--lumo-contrast-20pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("padding", "var(--lumo-space-m)")
        .set("margin-bottom", "var(--lumo-space-s)")
        .set("cursor", "pointer")
        .set("background", "var(--lumo-base-color)");
    
    String displayName = server.getName() != null ? server.getName() : server.getHost();
    if (isInternal) {
      displayName += " (internal)";
    }
    
    H2 serverName = new H2(displayName);
    serverName.getStyle().set("margin", "0 0 var(--lumo-space-xs) 0")
        .set("font-size", "var(--lumo-font-size-l)");
    
    Paragraph details = new Paragraph(server.getHost() + ":" + server.getPort());
    details.getStyle().set("margin", "0")
        .set("color", "var(--lumo-secondary-text-color)")
        .set("font-size", "var(--lumo-font-size-s)");
    
    card.add(serverName, details);
    
    // Make card clickable to navigate to server view
    String serverId = server.getId();
    card.addClickListener(e -> {
      selectionService.setSelected(server);
      getUI().ifPresent(ui -> ui.navigate("servers/" + serverId));
    });
    
    // Hover effect
    card.getElement().addEventListener("mouseenter", event -> {
      card.getStyle().set("background", "var(--lumo-contrast-5pct)");
    });
    
    card.getElement().addEventListener("mouseleave", event -> {
      card.getStyle().set("background", "var(--lumo-base-color)");
    });
    
    return card;
  }
}
