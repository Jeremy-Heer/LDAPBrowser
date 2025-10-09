package com.ldapweb.ldapbrowser.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
        + "and managing LDAP directories. Connect to your LDAP servers, navigate through "
        + "directory structures, search for entries, and manage your LDAP data all from "
        + "one intuitive interface.");
    intro.getStyle().set("max-width", "800px").set("font-size", "var(--lumo-font-size-l)");

    // Quick Start section
    H2 quickStartHeader = new H2("Quick Start");
    quickStartHeader.getStyle().set("color", "var(--lumo-primary-color)");
    
    VerticalLayout quickStartSteps = new VerticalLayout();
    quickStartSteps.setSpacing(true);
    quickStartSteps.setPadding(false);
    quickStartSteps.add(
        createQuickStartStep("1", "Configure Server", 
            "Go to Settings to add your LDAP server configuration"),
        createQuickStartStep("2", "Connect", 
            "Select your server from the sidebar to connect"),
        createQuickStartStep("3", "Browse", 
            "Navigate the directory tree and explore your data"));

    // Feature cards section
    H2 featuresHeader = new H2("Key Features");
    featuresHeader.getStyle().set("color", "var(--lumo-primary-color)");
    
    HorizontalLayout featureCards = new HorizontalLayout();
    featureCards.setWidthFull();
    featureCards.setJustifyContentMode(JustifyContentMode.CENTER);
    featureCards.add(
        createFeatureCard(
            VaadinIcon.CONNECT,
            "LDAP Browser",
            "Navigate directory trees, view and edit entry attributes with an intuitive interface."),
        createFeatureCard(
            VaadinIcon.SEARCH,
            "Advanced Search",
            "Powerful search capabilities with filters, scopes, and result sorting."),
        createFeatureCard(
            VaadinIcon.FILE_TREE,
            "Schema Explorer",
            "Browse object classes, attributes, syntaxes, and matching rules."));

    H2 tipsHeader = new H2("Quick Tips");
    tipsHeader.getStyle().set("color", "var(--lumo-primary-color)");
    
    VerticalLayout tipsList = new VerticalLayout();
    tipsList.setSpacing(false);
    tipsList.setPadding(false);
    tipsList.add(
        createTipItem("Use server groups to organize multiple LDAP servers"),
        createTipItem("Right-click entries in the tree for context menu options"),
        createTipItem("Click Help in the sidebar for detailed usage instructions"),
        createTipItem("Test your setup with internal servers before connecting to production"));

    // Add all components
    add(
        header,
        intro,
        quickStartHeader,
        quickStartSteps,
        featuresHeader,
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

  /**
   * Creates a quick start step with a number, title, and description.
   *
   * @param stepNumber The step number
   * @param title The step title
   * @param description The step description
   * @return A horizontal layout representing the step
   */
  private HorizontalLayout createQuickStartStep(String stepNumber, String title, String description) {
    HorizontalLayout stepItem = new HorizontalLayout();
    stepItem.setSpacing(true);
    stepItem.setPadding(false);
    stepItem.setAlignItems(Alignment.CENTER);
    stepItem.getStyle().set("margin-bottom", "var(--lumo-space-s)");

    // Step number circle
    Div numberCircle = new Div();
    numberCircle.setText(stepNumber);
    numberCircle.getStyle()
        .set("background-color", "var(--lumo-primary-color)")
        .set("color", "var(--lumo-primary-contrast-color)")
        .set("border-radius", "50%")
        .set("width", "32px")
        .set("height", "32px")
        .set("display", "flex")
        .set("align-items", "center")
        .set("justify-content", "center")
        .set("font-weight", "bold")
        .set("font-size", "var(--lumo-font-size-s)")
        .set("flex-shrink", "0");

    // Step content
    VerticalLayout stepContent = new VerticalLayout();
    stepContent.setSpacing(false);
    stepContent.setPadding(false);
    stepContent.getStyle().set("margin", "0");

    H3 stepTitle = new H3(title);
    stepTitle.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-m)");

    Paragraph stepDescription = new Paragraph(description);
    stepDescription.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

    stepContent.add(stepTitle, stepDescription);
    stepItem.add(numberCircle, stepContent);
    
    return stepItem;
  }
}
