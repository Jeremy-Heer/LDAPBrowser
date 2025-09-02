package com.ldapweb.ldapbrowser.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Lightweight route used to select a group by name and forward to GroupSearchView.
 */
@Route(value = "group/:group", layout = MainLayout.class)
@PageTitle("Select Group")
@AnonymousAllowed
public class SelectGroupView extends Div implements BeforeEnterObserver {

  /**
   * Constructs a new SelectGroupView.
   */
  public SelectGroupView() {
    setVisible(false);
  }

  /**
   * Handles the before-enter event to select a group by name and forward to GroupSearchView.
   *
   * @param event the before-enter event
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String group = event.getRouteParameters().get("group").orElse(null);
    if (group != null) {
      // Forward to the existing GroupSearchView with the group parameter
      event.forwardTo("group-search/" + group);
    } else {
      // If no group specified, forward to ServersView
      event.forwardTo(ServersView.class);
    }
  }
}
