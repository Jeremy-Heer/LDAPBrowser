package com.ldapweb.ldapbrowser.ui;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.ldapweb.ldapbrowser.service.ServerSelectionService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View for displaying servers within a specific group under the Servers section.
 * This view shows all servers that belong to the specified group.
 */
@Route(value = "servers/:group", layout = MainLayout.class)
@PageTitle("Server Group")
@AnonymousAllowed
public class ServersGroupView extends VerticalLayout implements BeforeEnterObserver {

  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  private final ServerSelectionService selectionService;
  
  private String groupName;
  private H2 groupTitle;
  private VerticalLayout serversList;

  /**
   * Constructs the ServersGroupView with the required services.
   *
   * @param configurationService the configuration service
   * @param inMemoryLdapService the in-memory LDAP service
   * @param selectionService the server selection service
   */
  public ServersGroupView(ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService,
      ServerSelectionService selectionService) {
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.selectionService = selectionService;
    
    initializeComponents();
  }

  /**
   * Initializes the UI components.
   */
  private void initializeComponents() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    
    groupTitle = new H2();
    serversList = new VerticalLayout();
    serversList.setSpacing(true);
    serversList.setPadding(false);
    
    add(groupTitle, serversList);
  }

  /**
   * Handles the before-enter event to extract the group name and display servers.
   *
   * @param event the before-enter event
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String encodedGroup = event.getRouteParameters().get("group").orElse("");
    
    if (encodedGroup.isEmpty()) {
      event.forwardTo(ServersView.class);
      return;
    }
    
    try {
      this.groupName = URLDecoder.decode(encodedGroup, StandardCharsets.UTF_8);
    } catch (Exception e) {
      this.groupName = encodedGroup;
    }
    
    updateContent();
  }

  /**
   * Updates the content to display servers in the specified group.
   */
  private void updateContent() {
    groupTitle.setText("Server Group: " + groupName);
    serversList.removeAll();
    
    // Get all servers in this group
    List<LdapServerConfig> serversInGroup = configurationService.getAllConfigurations().stream()
        .filter(cfg -> cfg.getGroups().contains(groupName))
        .collect(Collectors.toList());
    
    // Add internal servers in this group
    List<LdapServerConfig> internalServersInGroup = 
        inMemoryLdapService.getAllInMemoryServers().stream()
        .filter(cfg -> 
          inMemoryLdapService.isServerRunning(cfg.getId()) && cfg.getGroups().contains(groupName)
          ).collect(Collectors.toList());
    if (serversInGroup.isEmpty() && internalServersInGroup.isEmpty()) {
      serversList.add(new Paragraph("No servers found in group '" + groupName + "'."));
      return;
    }
    
    // Display external servers
    if (!serversInGroup.isEmpty()) {
      H2 externalTitle = new H2("External Servers");
      externalTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
      serversList.add(externalTitle);
      
      for (LdapServerConfig server : serversInGroup) {
        Div serverCard = createServerCard(server, false);
        serversList.add(serverCard);
      }
    }
    
    // Display internal servers
    if (!internalServersInGroup.isEmpty()) {
      H2 internalTitle = new H2("Internal Servers");
      internalTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
      serversList.add(internalTitle);
      
      for (LdapServerConfig server : internalServersInGroup) {
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
    
    // Make card clickable to select server
    card.addClickListener(e -> {
      selectionService.setSelected(server);
      getUI().ifPresent(ui -> ui.navigate(ServersView.class));
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

  /**
   * Gets the current group name.
   *
   * @return the group name
   */
  public String getGroupName() {
    return groupName;
  }
}
