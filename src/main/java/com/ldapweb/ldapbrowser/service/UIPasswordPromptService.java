package com.ldapweb.ldapbrowser.service;

import com.ldapweb.ldapbrowser.ui.components.PasswordPromptDialog;
import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service that provides password prompting functionality for the UI.
 * This service implements the PasswordPromptCallback interface to bridge
 * between the service layer and UI layer for password prompting.
 */
@Service
public class UIPasswordPromptService implements PasswordPromptCallback {

  @Override
  public CompletableFuture<String> promptForPassword(String serverName, String serverId) {
    CompletableFuture<String> future = new CompletableFuture<>();
    
    // Ensure we're on the UI thread
    UI currentUI = UI.getCurrent();
    if (currentUI == null) {
      future.completeExceptionally(new IllegalStateException("No UI context available for password prompt"));
      return future;
    }

    // Create and show the password prompt dialog
    currentUI.access(() -> {
      PasswordPromptDialog dialog = new PasswordPromptDialog(serverName);
      
      dialog.setPasswordConsumer(password -> {
        // Complete the future with the entered password
        future.complete(password);
      });
      
      dialog.setCancelCallback(() -> {
        // Complete the future with null to indicate cancellation
        future.complete(null);
      });
      
      dialog.open();
    });

    return future;
  }
}