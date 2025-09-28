package com.ldapweb.ldapbrowser.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

import java.util.function.Consumer;

/**
 * Dialog for prompting user to enter password during LDAP connection.
 * The password is only stored in memory and not persisted.
 */
public class PasswordPromptDialog extends Dialog {

  private PasswordField passwordField;
  private Consumer<String> passwordConsumer;
  private Runnable cancelCallback;

  public PasswordPromptDialog(String serverName) {
    setHeaderTitle("Enter Password");
    setWidth("400px");
    setModal(true);
    setDraggable(false);
    setResizable(false);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(false);

    initializeComponents(serverName);
    setupLayout();
  }

  private void initializeComponents(String serverName) {
    passwordField = new PasswordField("Password");
    passwordField.setRequiredIndicatorVisible(true);
    passwordField.setWidthFull();
    passwordField.focus();
    passwordField.setPlaceholder("Enter connection password");

    // Allow confirming with Enter key
    passwordField.addKeyPressListener(e -> {
      if (e.getKey().getKeys().contains("Enter")) {
        confirm();
      }
    });
  }

  private void setupLayout() {
    VerticalLayout layout = new VerticalLayout();
    layout.setPadding(false);
    layout.setSpacing(true);

    Icon serverIcon = new Icon(VaadinIcon.SERVER);
    serverIcon.setSize("24px");
    serverIcon.getStyle().set("color", "var(--lumo-primary-color)");

    H4 title = new H4("Authentication Required");
    title.getStyle().set("margin", "0 0 10px 0");

    Span description = new Span("Please enter the password to connect to this LDAP server. The password will only be stored in memory during this session.");
    description.getStyle().set("font-size", "var(--lumo-font-size-s)")
                          .set("color", "var(--lumo-secondary-text-color)")
                          .set("margin-bottom", "15px")
                          .set("line-height", "1.4");

    layout.add(title, description, passwordField);
    add(layout);

    // Footer buttons
    Button connectButton = new Button("Connect", e -> confirm());
    connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectButton.setIcon(new Icon(VaadinIcon.PLUG));

    Button cancelButton = new Button("Cancel", e -> cancel());
    cancelButton.setIcon(new Icon(VaadinIcon.CLOSE));

    getFooter().add(cancelButton, connectButton);
  }

  private void confirm() {
    String password = passwordField.getValue();
    if (password == null || password.trim().isEmpty()) {
      passwordField.setInvalid(true);
      passwordField.setErrorMessage("Password is required");
      passwordField.focus();
      return;
    }

    passwordField.setInvalid(false);
    if (passwordConsumer != null) {
      passwordConsumer.accept(password);
    }
    close();
  }

  private void cancel() {
    if (cancelCallback != null) {
      cancelCallback.run();
    }
    close();
  }

  /**
   * Sets the callback to be called when password is entered and confirmed.
   * @param passwordConsumer callback that receives the entered password
   */
  public void setPasswordConsumer(Consumer<String> passwordConsumer) {
    this.passwordConsumer = passwordConsumer;
  }

  /**
   * Sets the callback to be called when the dialog is cancelled.
   * @param cancelCallback callback to run on cancel
   */
  public void setCancelCallback(Runnable cancelCallback) {
    this.cancelCallback = cancelCallback;
  }

  @Override
  public void open() {
    super.open();
    // Focus the password field when dialog opens
    passwordField.focus();
  }
}