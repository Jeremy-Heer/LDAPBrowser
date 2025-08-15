package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.ConfigurationService;
import com.example.ldapbrowser.service.InMemoryLdapService;
import com.example.ldapbrowser.service.LdapService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
* Environment dropdown component that shows all available external and internal LDAP servers.
* Supports both single and multi-selection modes.
*/
public class EnvironmentDropdown {
  
  private final LdapService ldapService;
  private final ConfigurationService configurationService;
  private final InMemoryLdapService inMemoryLdapService;
  
  private boolean multiSelect;
  private ComboBox<LdapServerConfig> singleSelectCombo;
  private MultiSelectComboBox<LdapServerConfig> multiSelectCombo;
  
  private Consumer<LdapServerConfig> singleSelectionListener;
  private Consumer<Set<LdapServerConfig>> multiSelectionListener;
  
  public EnvironmentDropdown(LdapService ldapService, ConfigurationService configurationService, 
               InMemoryLdapService inMemoryLdapService, boolean multiSelect) {
    this.ldapService = ldapService;
    this.configurationService = configurationService;
    this.inMemoryLdapService = inMemoryLdapService;
    this.multiSelect = multiSelect;
    
    initializeComponents();
    refreshEnvironments();
  }
  
  private void initializeComponents() {
    if (multiSelect) {
      multiSelectCombo = new MultiSelectComboBox<>();
      multiSelectCombo.setLabel("Environments");
      multiSelectCombo.setItemLabelGenerator(this::formatServerName);
      multiSelectCombo.setWidth("300px");
      multiSelectCombo.addSelectionListener(e -> {
        if (multiSelectionListener != null) {
          multiSelectionListener.accept(e.getAllSelectedItems());
        }
        // Auto-connect to selected environments
        connectToSelectedEnvironments(e.getAllSelectedItems());
      });
    } else {
      singleSelectCombo = new ComboBox<>();
      singleSelectCombo.setLabel("Environment");
      singleSelectCombo.setItemLabelGenerator(this::formatServerName);
      singleSelectCombo.setPrefixComponent(new Icon(VaadinIcon.SERVER));
      singleSelectCombo.setWidth("300px");
      singleSelectCombo.addValueChangeListener(e -> {
        if (singleSelectionListener != null && e.getValue() != null) {
          singleSelectionListener.accept(e.getValue());
        }
        // Auto-connect to selected environment
        if (e.getValue() != null) {
          connectToEnvironment(e.getValue());
        }
      });
    }
  }
  
  private String formatServerName(LdapServerConfig config) {
    // Use just the Name field as requested
    return config.getName() != null ? config.getName() : config.getHost() + ":" + config.getPort();
  }
  
  public void refreshEnvironments() {
    List<LdapServerConfig> allEnvironments = new ArrayList<>();
    
    // Add external servers
    allEnvironments.addAll(configurationService.getAllConfigurations());
    
    // Add internal servers that are started
    List<LdapServerConfig> internalServers = inMemoryLdapService.getAllInMemoryServers()
      .stream()
      .filter(server -> inMemoryLdapService.isServerRunning(server.getId()))
      .collect(Collectors.toList());
    allEnvironments.addAll(internalServers);
    
    if (multiSelect) {
      multiSelectCombo.setItems(allEnvironments);
    } else {
      singleSelectCombo.setItems(allEnvironments);
    }
  }
  
  private void connectToEnvironment(LdapServerConfig config) {
    if (!ldapService.isConnected(config.getId())) {
      try {
        ldapService.connect(config);
      } catch (Exception e) {
        // Connection failed - show error notification
        Notification notification = Notification.show(
          "Failed to connect to " + config.getName() + ": " + e.getMessage(), 
          5000, 
          Notification.Position.BOTTOM_END
        );
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
      }
    }
  }
  
  private void connectToSelectedEnvironments(Set<LdapServerConfig> environments) {
    for (LdapServerConfig config : environments) {
      connectToEnvironment(config);
    }
  }
  
  // Single select methods
  public ComboBox<LdapServerConfig> getSingleSelectComponent() {
    return singleSelectCombo;
  }
  
  public LdapServerConfig getSelectedEnvironment() {
    return multiSelect ? null : (singleSelectCombo != null ? singleSelectCombo.getValue() : null);
  }
  
  public void setSelectedEnvironment(LdapServerConfig config) {
    if (!multiSelect && singleSelectCombo != null) {
      singleSelectCombo.setValue(config);
    }
  }
  
  public Registration addSingleSelectionListener(Consumer<LdapServerConfig> listener) {
    this.singleSelectionListener = listener;
    return () -> this.singleSelectionListener = null;
  }
  
  // Multi select methods
  public MultiSelectComboBox<LdapServerConfig> getMultiSelectComponent() {
    return multiSelectCombo;
  }
  
  /**
  * Get the multi-select component wrapped with an icon for consistency with single-select
  */
  public Component getMultiSelectComponentWithIcon() {
    if (!multiSelect || multiSelectCombo == null) {
      return multiSelectCombo;
    }
    
    // Create a wrapper div that mimics the structure of a ComboBox with prefix
    Div wrapper = new Div();
    wrapper.getStyle()
      .set("position", "relative")
      .set("width", "100%");
    
    // Create icon positioned like a prefix component
    Icon serverIcon = new Icon(VaadinIcon.SERVER);
    serverIcon.setSize("var(--lumo-icon-size-m)");
    serverIcon.getStyle()
      .set("position", "absolute")
      .set("left", "var(--lumo-space-s)")
      .set("top", "calc(var(--lumo-text-field-size) - var(--lumo-icon-size-m))")
      .set("z-index", "10")
      .set("color", "var(--lumo-contrast-60pct)")
      .set("pointer-events", "none");
    
    // Add left padding to the multiselect combo to make space for the icon
    multiSelectCombo.getStyle().set("padding-left", "calc(var(--lumo-space-s) + var(--lumo-icon-size-m) + var(--lumo-space-xs))");
    
    wrapper.add(serverIcon, multiSelectCombo);
    
    return wrapper;
  }
  
  public Set<LdapServerConfig> getSelectedEnvironments() {
    return multiSelect ? (multiSelectCombo != null ? multiSelectCombo.getSelectedItems() : Set.of()) : Set.of();
  }
  
  public void setSelectedEnvironments(Set<LdapServerConfig> configs) {
    if (multiSelect && multiSelectCombo != null) {
      multiSelectCombo.select(configs);
    }
  }
  
  public Registration addMultiSelectionListener(Consumer<Set<LdapServerConfig>> listener) {
    this.multiSelectionListener = listener;
    return () -> this.multiSelectionListener = null;
  }
  
  public void clear() {
    if (multiSelect && multiSelectCombo != null) {
      multiSelectCombo.clear();
    } else if (!multiSelect && singleSelectCombo != null) {
      singleSelectCombo.clear();
    }
  }
  
  public boolean isMultiSelect() {
    return multiSelect;
  }
}
