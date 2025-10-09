package com.ldapweb.ldapbrowser.ui;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Help page that loads and displays help content from HELP.md markdown file.
 */
@Route(value = "help", layout = MainLayout.class)
@PageTitle("Help - LDAP Browser")
@AnonymousAllowed
public class HelpView extends VerticalLayout {

  /**
   * Constructs the help view by loading and rendering the HELP.md markdown file.
   */
  public HelpView() {
    addClassName("help-view");
    setSpacing(false);
    setPadding(true);
    setHeightFull();

    loadAndRenderMarkdown();
  }

  /**
   * Loads the HELP.md file and renders it as HTML.
   */
  private void loadAndRenderMarkdown() {
    try {
      Path helpFilePath = Paths.get("HELP.md");
      String markdownContent;

      if (Files.exists(helpFilePath)) {
        markdownContent = Files.readString(helpFilePath, StandardCharsets.UTF_8);
      } else {
        markdownContent = getDefaultHelpContent();
      }

      Parser parser = Parser.builder().build();
      Node document = parser.parse(markdownContent);
      HtmlRenderer renderer = HtmlRenderer.builder().build();
      String htmlContent = renderer.render(document);

      Div contentDiv = new Div();
      contentDiv.addClassName("help-content");
      contentDiv.getStyle()
          .set("max-width", "900px")
          .set("margin", "0 auto")
          .set("padding", "var(--lumo-space-l)");

      String styledHtml = "<style>"
          + ".help-content h1 { color: var(--lumo-primary-text-color); "
          + "margin-top: var(--lumo-space-xl); margin-bottom: var(--lumo-space-m); }"
          + ".help-content h2 { color: var(--lumo-primary-text-color); "
          + "margin-top: var(--lumo-space-l); margin-bottom: var(--lumo-space-m); "
          + "border-bottom: 2px solid var(--lumo-primary-color); "
          + "padding-bottom: var(--lumo-space-xs); }"
          + ".help-content h3 { color: var(--lumo-primary-color); "
          + "margin-top: var(--lumo-space-m); margin-bottom: var(--lumo-space-s); }"
          + ".help-content p { margin-bottom: var(--lumo-space-m); line-height: 1.6; }"
          + ".help-content ul { margin-bottom: var(--lumo-space-m); "
          + "padding-left: var(--lumo-space-l); }"
          + ".help-content li { margin-bottom: var(--lumo-space-xs); }"
          + ".help-content code { background-color: var(--lumo-contrast-10pct); "
          + "padding: 2px 6px; border-radius: 3px; font-family: monospace; }"
          + ".help-content strong { color: var(--lumo-primary-text-color); }"
          + "</style>" + htmlContent;

      Html htmlComponent = new Html("<div class=\"help-content\">" + styledHtml + "</div>");
      contentDiv.add(htmlComponent);
      add(contentDiv);

    } catch (IOException e) {
      Notification.show("Error loading help content: " + e.getMessage(),
          5000, Notification.Position.MIDDLE);
      add(new Div(new Html("<p>Unable to load help content. "
          + "Please check the HELP.md file.</p>")));
    }
  }

  /**
   * Returns default help content if the HELP.md file is not found.
   */
  private String getDefaultHelpContent() {
    return "# LDAP Browser Help\n\n"
        + "Help content file (HELP.md) not found. "
        + "Please create the file in the project root.\n\n"
        + "## Quick Start\n\n"
        + "1. Configure your LDAP server connection in Settings\n"
        + "2. Select a server from the dropdown\n"
        + "3. Browse the directory tree in the LDAP Browser tab\n";
  }
}
