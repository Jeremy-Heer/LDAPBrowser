package com.example.ldapbrowser.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unboundid.ldap.sdk.LDAPConnection;

/**
* Model class representing an LDAP server connection configuration
*/
public class LdapServerConfig {

  private String id;
  private String name;
  private String group;
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
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
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
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    LdapServerConfig that = (LdapServerConfig) obj;
    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}