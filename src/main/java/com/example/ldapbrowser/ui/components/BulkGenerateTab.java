package com.example.ldapbrowser.ui.components;

import com.example.ldapbrowser.model.LdapEntry;
import com.example.ldapbrowser.model.LdapServerConfig;
import com.example.ldapbrowser.service.LdapService;
import com.example.ldapbrowser.service.LoggingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFReader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Generate sub-tab for bulk generation of LDAP entries
 */
public class BulkGenerateTab extends VerticalLayout {

  private final LdapService ldapService;
  private final LoggingService loggingService;

  // Server configuration
  private LdapServerConfig serverConfig;

  // UI Components
  private IntegerField countStartField;
  private IntegerField countEndField;
  private TextArea ldifTemplateArea;
  private TextArea ldifPreviewArea;
  private Button loadButton;

  // Progress
  private ProgressBar progressBar;
  private VerticalLayout progressContainer;

  public BulkGenerateTab(LdapService ldapService, LoggingService loggingService) {
    this.ldapService = ldapService;
    this.loggingService = loggingService;

    initializeComponents();
    setupLayout();
  }

  private void initializeComponents() {
    // Count fields
    countStartField = new IntegerField("Count Start");
    countStartField.setWidthFull();
    countStartField.setPlaceholder("1");
    countStartField.setMin(1);
    countStartField.setValue(1);
    countStartField.setStepButtonsVisible(true);

    countEndField = new IntegerField("Count End");
    countEndField.setWidthFull();
    countEndField.setPlaceholder("100");
    countEndField.setMin(1);
    countEndField.setValue(100);
    countEndField.setStepButtonsVisible(true);

    // LDIF Template
    ldifTemplateArea = new TextArea("LDIF Template");
    ldifTemplateArea.setWidthFull();
    ldifTemplateArea.setHeight("200px");
    ldifTemplateArea.setValue("dn: uid=user.{COUNT},ou=people,dc=example,dc=com\n" +
        "changetype: add\n" +
        "objectClass: inetOrgPerson\n" +
        "objectClass: organizationalPerson\n" +
        "objectClass: person\n" +
        "objectClass: top\n" +
        "uid: user.{COUNT}\n" +
        "cn: user.{COUNT}\n" +
        "sn: user.{COUNT}");
    ldifTemplateArea.setPlaceholder("Enter LDIF template with {COUNT} placeholder");

    // Add value change listener to update preview
    countStartField.addValueChangeListener(e -> updatePreview());
    countEndField.addValueChangeListener(e -> updatePreview());
    ldifTemplateArea.addValueChangeListener(e -> updatePreview());

    // LDIF Preview
    ldifPreviewArea = new TextArea("LDIF Preview");
    ldifPreviewArea.setWidthFull();
    ldifPreviewArea.setHeight("150px");
    ldifPreviewArea.setReadOnly(true);
    ldifPreviewArea.setHelperText("Preview of the first few LDIF entries to be generated");

    // Load button
    loadButton = new Button("Load", new Icon(VaadinIcon.UPLOAD));
    loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loadButton.addClickListener(e -> performBulkGenerate());

    // Progress components
    progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);

    progressContainer = new VerticalLayout();
    progressContainer.setPadding(false);
    progressContainer.setSpacing(true);
    progressContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    progressContainer.add(new Span("Generating and importing entries..."), progressBar);
    progressContainer.setVisible(false);

    // Initial preview update
    updatePreview();
  }

  private void setupLayout() {
    setSizeFull();
    setPadding(true);
    setSpacing(true);
    addClassName("bulk-generate-tab");

    // Main content layout
    VerticalLayout contentLayout = new VerticalLayout();
    contentLayout.setPadding(true);
    contentLayout.setSpacing(true);
    contentLayout.addClassName("bulk-generate-field-group");

    // Count fields layout
    HorizontalLayout countLayout = new HorizontalLayout();
    countLayout.setWidthFull();
    countLayout.setSpacing(true);
    countLayout.add(countStartField, countEndField);

    // Action layout
    HorizontalLayout actionLayout = new HorizontalLayout();
    actionLayout.setDefaultVerticalComponentAlignment(Alignment.END);
    actionLayout.setSpacing(true);
    actionLayout.add(loadButton);

    contentLayout.add(
        new H4("Bulk Generate Operations"),
        new Span("Generate multiple LDAP entries using a template with count placeholders"),
        countLayout,
        ldifTemplateArea,
        ldifPreviewArea,
        actionLayout,
        progressContainer);

    add(contentLayout);
    setFlexGrow(1, contentLayout);
  }

  private void updatePreview() {
    try {
      Integer startCount = countStartField.getValue();
      Integer endCount = countEndField.getValue();
      String template = ldifTemplateArea.getValue();

      if (startCount == null || endCount == null || template == null || template.trim().isEmpty()) {
        ldifPreviewArea.setValue("");
        return;
      }

      if (startCount > endCount) {
        ldifPreviewArea.setValue("Error: Count Start must be less than or equal to Count End");
        return;
      }

      StringBuilder preview = new StringBuilder();
      int previewCount = Math.min(3, endCount - startCount + 1); // Show first 3 entries

      for (int i = 0; i < previewCount; i++) {
        int currentCount = startCount + i;
        String ldif = template.replace("{COUNT}", String.valueOf(currentCount));
        preview.append(ldif);
        if (i < previewCount - 1) {
          preview.append("\n\n");
        }
      }

      if (endCount - startCount + 1 > 3) {
        preview.append("\n\n... and ").append(endCount - startCount + 1 - 3).append(" more entries");
      }

      ldifPreviewArea.setValue(preview.toString());

    } catch (Exception e) {
      ldifPreviewArea.setValue("Error generating preview: " + e.getMessage());
    }
  }

  private void performBulkGenerate() {
    if (serverConfig == null) {
      showError("Please connect to an LDAP server first");
      return;
    }

    Integer startCount = countStartField.getValue();
    Integer endCount = countEndField.getValue();
    String template = ldifTemplateArea.getValue();

    if (startCount == null) {
      showError("Count Start is required");
      return;
    }

    if (endCount == null) {
      showError("Count End is required");
      return;
    }

    if (startCount > endCount) {
      showError("Count Start must be less than or equal to Count End");
      return;
    }

    if (template == null || template.trim().isEmpty()) {
      showError("LDIF Template is required");
      return;
    }

    int totalEntries = endCount - startCount + 1;

    loggingService.logInfo("BULK_GENERATE", "Starting bulk generation - Server: " + serverConfig.getName() +
        ", Start: " + startCount + ", End: " + endCount + ", Total: " + totalEntries);

    showProgress();

    try {
      int successCount = 0;
      int errorCount = 0;

      for (int currentCount = startCount; currentCount <= endCount; currentCount++) {
        try {
          // Generate LDIF for this count
          String ldif = template.replace("{COUNT}", String.valueOf(currentCount));

          // Parse and execute the LDIF
          byte[] contentBytes = ldif.getBytes(StandardCharsets.UTF_8);
          LDIFReader ldifReader = new LDIFReader(new ByteArrayInputStream(contentBytes));

          try {
            LDIFChangeRecord changeRecord;
            while ((changeRecord = ldifReader.readChangeRecord()) != null) {
              switch (changeRecord.getChangeType()) {
                case ADD:
                  if (changeRecord instanceof com.unboundid.ldif.LDIFAddChangeRecord) {
                    com.unboundid.ldif.LDIFAddChangeRecord addRecord = (com.unboundid.ldif.LDIFAddChangeRecord) changeRecord;

                    LdapEntry newEntry = new LdapEntry();
                    newEntry.setDn(addRecord.getDN());

                    for (com.unboundid.ldap.sdk.Attribute attr : addRecord.getAttributes()) {
                      for (String value : attr.getValues()) {
                        newEntry.addAttribute(attr.getName(), value);
                      }
                    }

                    ldapService.addEntry(serverConfig.getId(), newEntry);
                  }
                  break;

                case MODIFY:
                  if (changeRecord instanceof com.unboundid.ldif.LDIFModifyChangeRecord) {
                    com.unboundid.ldif.LDIFModifyChangeRecord modifyRecord = (com.unboundid.ldif.LDIFModifyChangeRecord) changeRecord;

                    ldapService.modifyEntry(serverConfig.getId(),
                        modifyRecord.getDN(),
                        java.util.Arrays.asList(modifyRecord.getModifications()),
                        null);
                  }
                  break;

                case DELETE:
                  ldapService.deleteEntry(serverConfig.getId(), changeRecord.getDN());
                  break;

                default:
                  throw new Exception("Unsupported change type: " + changeRecord.getChangeType());
              }
            }
          } finally {
            ldifReader.close();
          }

          successCount++;

        } catch (Exception e) {
          errorCount++;
          loggingService.logError("BULK_GENERATE", "Error generating entry for count " + currentCount, e.getMessage());
          // Continue processing remaining entries
        }
      }

      hideProgress();

      if (errorCount > 0) {
        loggingService.logWarning("BULK_GENERATE", "Bulk generation completed with errors - Server: " +
            serverConfig.getName() + ", Successes: " + successCount + ", Errors: " + errorCount);
        showInfo("Bulk generation completed with " + successCount + " successes and " + errorCount + " errors");
      } else {
        loggingService.logInfo("BULK_GENERATE", "Bulk generation completed successfully - Server: " +
            serverConfig.getName() + ", Entries: " + successCount);
        showSuccess("Bulk generation completed successfully. " + successCount + " entries created");
      }

    } catch (Exception e) {
      hideProgress();
      loggingService.logError("BULK_GENERATE", "Bulk generation failed - Server: " + serverConfig.getName(),
          e.getMessage());
      showError("Bulk generation failed: " + e.getMessage());
    }
  }

  private void showProgress() {
    progressContainer.setVisible(true);
    loadButton.setEnabled(false);
  }

  private void hideProgress() {
    progressContainer.setVisible(false);
    loadButton.setEnabled(true);
  }

  public void setServerConfig(LdapServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  public void clear() {
    countStartField.setValue(1);
    countEndField.setValue(100);
    ldifTemplateArea.setValue("dn: uid=user.{COUNT},ou=people,dc=example,dc=com\n" +
        "changetype: add\n" +
        "objectClass: inetOrgPerson\n" +
        "objectClass: organizationalPerson\n" +
        "objectClass: person\n" +
        "objectClass: top\n" +
        "uid: user.{COUNT}\n" +
        "cn: user.{COUNT}\n" +
        "sn: user.{COUNT}");
    hideProgress();
    updatePreview();
  }

  private void showSuccess(String message) {
    Notification notification = Notification.show(message, 3000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private void showError(String message) {
    Notification notification = Notification.show(message, 5000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private void showInfo(String message) {
    Notification notification = Notification.show(message, 4000, Notification.Position.TOP_END);
    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
  }
}