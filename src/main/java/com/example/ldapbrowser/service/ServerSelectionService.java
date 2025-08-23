package com.example.ldapbrowser.service;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds the currently selected server for the user session and notifies listeners on change.
 */
@Service
@VaadinSessionScope
public class ServerSelectionService {

    private LdapServerConfig selected;
    private final List<Consumer<LdapServerConfig>> listeners = new ArrayList<>();

    public LdapServerConfig getSelected() {
        return selected;
    }

    public void setSelected(LdapServerConfig selected) {
        this.selected = selected;
        // Notify listeners
        for (Consumer<LdapServerConfig> l : List.copyOf(listeners)) {
            try {
                l.accept(selected);
            } catch (Exception ignored) {
            }
        }
    }

    public void addListener(Consumer<LdapServerConfig> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Consumer<LdapServerConfig> listener) {
        listeners.remove(listener);
    }
}
