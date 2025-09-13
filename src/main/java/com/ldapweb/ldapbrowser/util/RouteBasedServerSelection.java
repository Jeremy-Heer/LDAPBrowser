package com.ldapweb.ldapbrowser.util;

import com.ldapweb.ldapbrowser.model.LdapServerConfig;
import com.ldapweb.ldapbrowser.service.ConfigurationService;
import com.ldapweb.ldapbrowser.service.InMemoryLdapService;
import com.vaadin.flow.component.UI;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to extract server configurations from route parameters
 * for multi-tab friendly operation.
 */
public class RouteBasedServerSelection {

  /**
   * Extracts the server ID from the current route if it follows the pattern "servers/{serverId}".
   * 
   * @return Optional containing the server ID if found, empty otherwise
   */
  public static Optional<String> getCurrentServerIdFromRoute() {
    if (UI.getCurrent() == null) {
      return Optional.empty();
    }
    
    String location = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    if (location.startsWith("servers/")) {
      String serverId = location.substring("servers/".length());
      // Handle sub-routes like "servers/{id}/some-sub-path"
      int slashIndex = serverId.indexOf('/');
      if (slashIndex > 0) {
        serverId = serverId.substring(0, slashIndex);
      }
      return serverId.isEmpty() ? Optional.empty() : Optional.of(serverId);
    }
    
    return Optional.empty();
  }

  /**
   * Extracts the group name from the current route if it follows the pattern "group-search/{groupName}".
   * 
   * @return Optional containing the group name if found, empty otherwise
   */
  public static Optional<String> getCurrentGroupFromRoute() {
    if (UI.getCurrent() == null) {
      return Optional.empty();
    }
    
    String location = UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    if (location.startsWith("group-search/")) {
      String groupName = location.substring("group-search/".length());
      // Handle sub-routes and decode URL encoding
      int slashIndex = groupName.indexOf('/');
      if (slashIndex > 0) {
        groupName = groupName.substring(0, slashIndex);
      }
      return groupName.isEmpty() ? Optional.empty() : Optional.of(normalizeGroup(groupName));
    }
    
    return Optional.empty();
  }

  /**
   * Gets the server configuration for the current route's server ID.
   * 
   * @param configurationService service to look up external servers
   * @param inMemoryLdapService service to look up internal servers
   * @return Optional containing the server configuration if found
   */
  public static Optional<LdapServerConfig> getCurrentServerFromRoute(
      ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService) {
    
    return getCurrentServerIdFromRoute()
        .flatMap(serverId -> findServerById(serverId, configurationService, inMemoryLdapService));
  }

  /**
   * Gets the server configurations for the current route's group name.
   * 
   * @param configurationService service to look up external servers
   * @param inMemoryLdapService service to look up internal servers
   * @return Set of server configurations in the group
   */
  public static Set<LdapServerConfig> getCurrentGroupServersFromRoute(
      ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService) {
    
    return getCurrentGroupFromRoute()
        .map(groupName -> getServersInGroup(groupName, configurationService, inMemoryLdapService))
        .orElse(Set.of());
  }

  /**
   * Finds a server configuration by its ID.
   * 
   * @param serverId the ID of the server to find
   * @param configurationService service to look up external servers
   * @param inMemoryLdapService service to look up internal servers
   * @return Optional containing the server if found
   */
  public static Optional<LdapServerConfig> findServerById(String serverId,
      ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService) {
    
    if (serverId == null || serverId.trim().isEmpty()) {
      return Optional.empty();
    }

    // First try to find in external servers
    Optional<LdapServerConfig> externalServer = configurationService.getAllConfigurations().stream()
        .filter(config -> serverId.equals(config.getId()))
        .findFirst();
    
    if (externalServer.isPresent()) {
      return externalServer;
    }
    
    // If not found in external servers, try internal servers
    return inMemoryLdapService.getRunningInMemoryServers().stream()
        .filter(config -> serverId.equals(config.getId()))
        .findFirst();
  }

  /**
   * Gets all servers that belong to the specified group.
   * 
   * @param groupName the name of the group
   * @param configurationService service to look up external servers
   * @param inMemoryLdapService service to look up internal servers
   * @return set of servers in the group
   */
  public static Set<LdapServerConfig> getServersInGroup(String groupName,
      ConfigurationService configurationService,
      InMemoryLdapService inMemoryLdapService) {
    
    if (groupName == null || groupName.trim().isEmpty()) {
      return Set.of();
    }

    Set<LdapServerConfig> groupServers = new java.util.HashSet<>();
    final String normalized = normalizeGroup(groupName);
    
    // Check external servers
    groupServers.addAll(configurationService.getAllConfigurations().stream()
        .filter(c -> c.getGroups().stream()
            .anyMatch(group -> normalized.equalsIgnoreCase(normalizeGroup(group))))
        .collect(Collectors.toSet()));

    // Add internal running servers matching the group
    for (LdapServerConfig cfg : inMemoryLdapService.getAllInMemoryServers()) {
      if (inMemoryLdapService.isServerRunning(cfg.getId())
          && cfg.getGroups().stream()
              .anyMatch(group -> normalized.equalsIgnoreCase(normalizeGroup(group)))) {
        groupServers.add(cfg);
      }
    }
    
    return groupServers;
  }

  /**
   * Normalize group names for comparison.
   * 
   * @param s the group name to normalize
   * @return normalized group name
   */
  private static String normalizeGroup(String s) {
    if (s == null) {
      return "";
    }
    String value = s;
    try {
      // URL decode in case the route segment contained encoded spaces (%20)
      value = java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8.name());
    } catch (Exception ignored) {
      // ignore and continue with original value
    }
    // Replace '+' (common in some encodings) with space and collapse whitespace
    value = value.replace('+', ' ').trim().replaceAll("\\s+", " ");
    return value;
  }
}