package com.example.ldapbrowser.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Home view displayed at the root route.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home")
@AnonymousAllowed
public class HomeView extends VerticalLayout {

  /**
   * Creates the Home view and initializes its layout and actions.
   */
  public HomeView() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);

    add(new H1("Hello, world"));

    Button openServers = new Button(
        "Open Servers",
        e -> UI.getCurrent().navigate(ServersView.class)
    );
    add(openServers);
  }
}
