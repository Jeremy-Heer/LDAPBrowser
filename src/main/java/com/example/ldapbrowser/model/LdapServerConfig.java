package com.example.ldapbrowser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unboundid.ldap.sdk.LDAPConnection;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Model class representing an LDAP server connection configuration
 */
public class LdapServerConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private String group; // Kept for backward compatibility
  private Set<String> groups = new HashSet<>(); // New field for multiple groups
  private String host;
  private int port;
  private String bindDn;
  private String password;
  private boolean useSSL;
  private boolean useStartTLS;
  private String baseDn;

  @JsonIgnore
  private LDAPConnection connection;

  public LdapServerConfig() {
    this.port = 389; // Default LDAP port
    this.useSSL = false;
    this.useStartTLS = false;
  }

  public LdapServerConfig(String id, String name, String host, int port, String bindDn, String password) {
    this();
    this.id = id;
    this.name = name;
    this.host = host;
    this.port = port;
    this.bindDn = bindDn;
    this.password = password;
  }

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroup() {
    // Return the first group for backward compatibility, or the legacy group field
    if (!groups.isEmpty()) {
      return groups.iterator().next();
    }
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
    // Also add to groups set if not null/empty
    if (group != null && !group.trim().isEmpty()) {
      this.groups.add(group.trim());
    }
  }

  /**
   * Gets all groups this server belongs to.
   * @return Set of group names
   */
  public Set<String> getGroups() {
    Set<String> result = new HashSet<>(groups);
    // Include legacy group if not already in the set
    if (group != null && !group.trim().isEmpty()) {
      result.add(group.trim());
    }
    return result;
  }

  /**
   * Sets all groups this server belongs to.
   * @param groups Set of group names
   */
  public void setGroups(Set<String> groups) {
    this.groups = groups != null ? new HashSet<>(groups) : new HashSet<>();
    // Set the first group as the legacy group for backward compatibility
    if (!this.groups.isEmpty()) {
      this.group = this.groups.iterator().next();
    }
  }

  /**
   * Adds a group to this server's group membership.
   * @param groupName the group name to add
   */
  public void addGroup(String groupName) {
    if (groupName != null && !groupName.trim().isEmpty()) {
      this.groups.add(groupName.trim());
      // If no legacy group is set, use this as the legacy group
      if (this.group == null || this.group.trim().isEmpty()) {
        this.group = groupName.trim();
      }
    }
  }

  /**
   * Removes a group from this server's group membership.
   * @param groupName the group name to remove
   */
  public void removeGroup(String groupName) {
    if (groupName != null) {
      this.groups.remove(groupName.trim());
      // If removing the legacy group, update it to another group or null
      if (groupName.trim().equals(this.group)) {
        this.group = this.groups.isEmpty() ? null : this.groups.iterator().next();
      }
    }
  }

  /**
   * Checks if this server belongs to the specified group.
   * @param groupName the group name to check
   * @return true if the server belongs to the group
   */
  public boolean belongsToGroup(String groupName) {
    if (groupName == null) {
      return false;
    }
    return getGroups().contains(groupName.trim());
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getBindDn() {
    return bindDn;
  }

  public void setBindDn(String bindDn) {
    this.bindDn = bindDn;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isUseSSL() {
    return useSSL;
  }

  public void setUseSSL(boolean useSSL) {
    this.useSSL = useSSL;
  }

  public boolean isUseStartTLS() {
    return useStartTLS;
  }

  public void setUseStartTLS(boolean useStartTLS) {
    this.useStartTLS = useStartTLS;
  }

  public String getBaseDn() {
    return baseDn;
  }

  public void setBaseDn(String baseDn) {
    this.baseDn = baseDn;
  }

  public LDAPConnection getConnection() {
    return connection;
  }

  public void setConnection(LDAPConnection connection) {
    this.connection = connection;
  }

  @Override
  public String toString() {
    return name + " (" + host + ":" + port + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    LdapServerConfig that = (LdapServerConfig) obj;
    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}