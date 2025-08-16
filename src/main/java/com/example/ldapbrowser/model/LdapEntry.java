package com.example.ldapbrowser.model;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
* Model class representing an LDAP entry for display in the UI
*/
public class LdapEntry {

  private String dn;
  private String rdn;
  private Map<String, List<String>> attributes;
  private boolean hasChildren;

  public LdapEntry() {
    this.attributes = new LinkedHashMap<>();
  }

  public LdapEntry(Entry entry) {
    this();
    this.dn = entry.getDN();

    // Extract RDN from DN
    String[] dnParts = dn.split(",");
    this.rdn = dnParts.length > 0 ? dnParts[0].trim() : dn;

    // Convert attributes
    for (Attribute attr : entry.getAttributes()) {
      List<String> values = new ArrayList<>();
      for (String value : attr.getValues()) {
        values.add(value);
      }
      attributes.put(attr.getName(), values);
    }
  }

  public String getDn() {
    return dn;
  }

  public void setDn(String dn) {
    this.dn = dn;
  }

  public String getRdn() {
    return rdn;
  }

  public void setRdn(String rdn) {
    this.rdn = rdn;
  }

  public Map<String, List<String>> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, List<String>> attributes) {
    this.attributes = attributes;
  }

  public boolean isHasChildren() {
    return hasChildren;
  }

  public void setHasChildren(boolean hasChildren) {
    this.hasChildren = hasChildren;
  }

  public List<String> getAttributeValues(String attributeName) {
    return attributes.getOrDefault(attributeName, new ArrayList<>());
  }

  public String getFirstAttributeValue(String attributeName) {
    List<String> values = getAttributeValues(attributeName);
    return values.isEmpty() ? null : values.get(0);
  }

  public void addAttribute(String name, String value) {
    attributes.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
  }

  public void setAttributeValues(String name, List<String> values) {
    attributes.put(name, new ArrayList<>(values));
  }

  public Set<String> getAttributeNames() {
    return attributes.keySet();
  }

  public String getDisplayName() {
    // Try to get a friendly display name from common attributes
    String cn = getFirstAttributeValue("cn");
    if (cn != null && !cn.isEmpty()) {
      return cn;
    }

    String uid = getFirstAttributeValue("uid");
    if (uid != null && !uid.isEmpty()) {
      return uid;
    }

    String ou = getFirstAttributeValue("ou");
    if (ou != null && !ou.isEmpty()) {
      return ou;
    }

    // Fall back to RDN
    return rdn;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    LdapEntry ldapEntry = (LdapEntry) obj;
    return Objects.equals(dn, ldapEntry.dn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dn);
  }
}