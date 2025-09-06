package com.ldapweb.ldapbrowser.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Welcome page for the LDAP Browser application.
 * This view serves as the landing page for the application, providing an
 * overview
 * and basic instructions for new users.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Welcome to LDAP Browser")
@AnonymousAllowed
public class WelcomeView extends VerticalLayout {

  /**
   * Constructs the welcome view with instructions and getting started content.
   */
  public WelcomeView() {
    addClassName("welcome-view");
    setSpacing(false);
    setPadding(true);
    setHeightFull();

    // Create the header
    H1 header = new H1("Welcome to LDAP Browser");
    header.getStyle().set("margin-top", "0");

    // Introduction paragraph
    Paragraph intro = new Paragraph(
        "LDAP Browser is a comprehensive web application for browsing, searching, "
        + "and managing LDAP directories. Navigate through your directory structure, "
        + "search for entries, and manage your LDAP data all from one central interface.");
    intro.getStyle().set("max-width", "800px");

    // Feature cards section
    HorizontalLayout featureCards = new HorizontalLayout();
    featureCards.setWidthFull();
    featureCards.setJustifyContentMode(JustifyContentMode.CENTER);
    featureCards.add(
        createFeatureCard(
            VaadinIcon.CONNECT,
            "Connect to a Server",
            "Select a server from the sidebar to connect and start browsing the directory."),
        createFeatureCard(
            VaadinIcon.SEARCH,
            "Search Directory",
            "Use the search tab to find entries based on various criteria."),
        createFeatureCard(
            VaadinIcon.FILE_TREE,
            "Browse Schema",
            "Explore object classes, attributes, and other schema elements."));

    VerticalLayout tipsList = new VerticalLayout();
    tipsList.setSpacing(false);
    tipsList.setPadding(false);
    tipsList.add(
        createTipItem("Use the dashboard tab to browse the directory tree."),
        createTipItem("The schema browser allows you to explore LDAP schema components."),
        createTipItem("Select a group from the sidebar to focus on servers in that group."),
        createTipItem("Click on an entry in the tree to view and edit its attributes."));

    // Getting started section (moved closer to usage)
    H2 gettingStarted = new H2("Getting Started");

    // Tips section (moved closer to usage)
    H2 tipsHeader = new H2("Quick Tips");

    // Add all components
    add(
        header,
        intro,
        gettingStarted,
        featureCards,
        tipsHeader,
        tipsList);
  }

  /**
    * Creates a feature card with an icon, title, and description.
    * 
    *
    * @param icon        The Vaadin icon for the feature
    * @param title       The feature title
    * @param description A short description of the feature
    * @return A div component representing the feature card
   */
  private Div createFeatureCard(VaadinIcon icon, String title, String description) {
    Div card = new Div();
    card.addClassName("feature-card");
    card.getStyle()
        .set("background-color", "var(--lumo-base-color)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("box-shadow", "var(--lumo-box-shadow-xs)")
        .set("padding", "var(--lumo-space-m)")
        .set("margin", "var(--lumo-space-s)")
        .set("max-width", "300px");

    Icon iconComponent = new Icon(icon);
    iconComponent.getStyle()
        .set("width", "48px")
        .set("height", "48px")
        .set("color", "var(--lumo-primary-color)");

    H2 titleComponent = new H2(title);
    titleComponent.getStyle()
        .set("margin-top", "var(--lumo-space-s)")
        .set("margin-bottom", "var(--lumo-space-xs)");

    Paragraph descriptionComponent = new Paragraph(description);

    card.add(iconComponent, titleComponent, descriptionComponent);

    return card;
  }

  /**
    * Creates a tip list item with a bullet point.
    * 
    *
    * @param tipText The text of the tip
    * @return A horizontal layout representing the tip item
   */
  private HorizontalLayout createTipItem(String tipText) {
    HorizontalLayout tipItem = new HorizontalLayout();
    tipItem.setSpacing(true);
    tipItem.setPadding(false);
    tipItem.setAlignItems(Alignment.CENTER);

    Icon bulletIcon = new Icon(VaadinIcon.CIRCLE);
    bulletIcon.getStyle()
        .set("color", "var(--lumo-primary-color)")
        .set("width", "8px")
        .set("height", "8px");

    Paragraph text = new Paragraph(tipText);
    text.getStyle().set("margin", "0");

    tipItem.add(bulletIcon, text);
    return tipItem;
  }
}
