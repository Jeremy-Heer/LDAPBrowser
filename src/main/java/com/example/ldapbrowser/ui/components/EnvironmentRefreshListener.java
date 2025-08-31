package com.example.ldapbrowser.ui.components;

/**
 * Interface for components that need to be notified when internal server
 * environments change.
 */
@FunctionalInterface
public interface EnvironmentRefreshListener {
  /**
   * Called when internal server environments change (start/stop).
   */
  void onEnvironmentChange();
}