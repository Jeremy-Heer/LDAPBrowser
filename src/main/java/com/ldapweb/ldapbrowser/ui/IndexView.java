package com.ldapweb.ldapbrowser.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Legacy index route that forwards to the WelcomeView.
 */
@Route(value = "index", layout = MainLayout.class)
@PageTitle("LDAP Browser")
@AnonymousAllowed
public class IndexView extends Div implements BeforeEnterObserver {

  /**
   * Constructs a new IndexView.
   */
  public IndexView() {
    setVisible(false);
  }

  /**
   * Handles the before-enter event to forward to WelcomeView by default.
   *
   * @param event the before-enter event
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // Forward to welcome view as the default
    event.forwardTo(WelcomeView.class);
  }
}
