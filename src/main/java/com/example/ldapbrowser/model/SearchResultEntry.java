package com.example.ldapbrowser.model;

import java.util.List;
import java.util.Map;

/**
* Search result entry that includes information about which environment the result came from
*/
public class SearchResultEntry {
 
 private LdapEntry entry;
 private LdapServerConfig environment;
 
 public SearchResultEntry(LdapEntry entry, LdapServerConfig environment) {
  this.entry = entry;
  this.environment = environment;
 }
 
 public LdapEntry getEntry() {
  return entry;
 }
 
 public void setEntry(LdapEntry entry) {
  this.entry = entry;
 }
 
 public LdapServerConfig getEnvironment() {
  return environment;
 }
 
 public void setEnvironment(LdapServerConfig environment) {
  this.environment = environment;
 }
 
 // Delegate methods to LdapEntry for convenience
 public String getDn() {
  return entry != null ? entry.getDn() : "";
 }
 
 public String getDisplayName() {
  return entry != null ? entry.getDisplayName() : "";
 }
 
 public List<String> getAttributeValues(String attributeName) {
  return entry != null ? entry.getAttributeValues(attributeName) : List.of();
 }
 
 public Map<String, List<String>> getAttributes() {
  return entry != null ? entry.getAttributes() : Map.of();
 }
 
 public String getEnvironmentName() {
  return environment != null ? environment.getName() : "Unknown";
 }
 
 public String getEnvironmentInfo() {
  if (environment != null) {
   return environment.getName() + " (" + environment.getHost() + ":" + environment.getPort() + ")";
  }
  return "Unknown Environment";
 }
}
