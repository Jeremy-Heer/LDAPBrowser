package com.ldapweb.ldapbrowser.service;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for prompting the user for passwords during LDAP connection.
 * This allows the service layer to request passwords from the UI layer without direct coupling.
 */
public interface PasswordPromptCallback {
  
  /**
   * Prompts the user for a password for the specified server.
   * 
   * @param serverName the name of the server requesting authentication
   * @param serverId the ID of the server requesting authentication  
   * @return a CompletableFuture that completes with the entered password, or null if cancelled
   */
  CompletableFuture<String> promptForPassword(String serverName, String serverId);
}