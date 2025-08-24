package com.feedback.ui.views.templates;

import com.feedback.model.FeedbackTemplate;
import com.feedback.service.AuthenticationService;
import com.feedback.service.FeedbackTemplateService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Route(value = "templates", layout = MainLayout.class)
@PageTitle("Feedback Templates | Feedback System")
@PermitAll  // Changed from @RolesAllowed
public class TemplateView extends VerticalLayout {

    private final FeedbackTemplateService templateService;
    private final AuthenticationService authenticationService;
    private final Grid<FeedbackTemplate> grid = new Grid<>(FeedbackTemplate.class);
    private final Binder<FeedbackTemplate> binder = new Binder<>(FeedbackTemplate.class);
    private FeedbackTemplate currentTemplate = null;

    private final TextField name = new TextField("Template Name");
    private final TextField description = new TextField("Description");
    private final TextArea instructions = new TextArea("Instructions");
    private final TextArea questions = new TextArea("Questions (one per line)");
    private final Checkbox active = new Checkbox("Active");

    public TemplateView(FeedbackTemplateService templateService, AuthenticationService authenticationService) {
        this.templateService = templateService;
        this.authenticationService = authenticationService;
        
        System.out.println("TemplateView: Constructor started");
        
        // MANUAL AUTHORIZATION CHECK
        com.feedback.model.User currentUser = authenticationService.getCurrentUser();
        if (currentUser == null) {
            System.out.println("TemplateView: No authenticated user, redirecting to login");
            showError("Please log in to access this page");
            UI.getCurrent().navigate("login");
            return;
        }
        
        // Check if user has required role for template management
        String roleName = currentUser.getRole().getName();
        if (!"SUPER_ADMIN".equals(roleName) && !"ADMIN".equals(roleName) && !"MANAGER".equals(roleName)) {
            System.out.println("TemplateView: User " + currentUser.getFullName() + " with role " + roleName + " does not have access");
            showError("Access denied. You need admin or manager privileges to access this page.");
            UI.getCurrent().navigate("");
            return;
        }
        
        System.out.println("TemplateView: Access granted for user: " + currentUser.getFullName() + " (Role: " + roleName + ")");
        
        addClassName("template-view");
        setSizeFull();

        try {
            configureGrid();
            configureForm();
            
            // Create info card
            Div infoCard = createInfoCard();

            add(
                    new H2("Feedback Templates"),
                    infoCard,
                    getToolbar(),
                    grid
            );

            updateList();
            
            System.out.println("TemplateView: Successfully initialized");
        } catch (Exception e) {
            System.err.println("Error initializing TemplateView: " + e.getMessage());
            e.printStackTrace();
            add(new Span("Error loading templates. Please try again."));
        }
    }
    
    private Div createInfoCard() {
        Div infoCard = new Div();
        infoCard.getStyle()
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("border", "1px solid var(--lumo-primary-color-50pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        H3 infoTitle = new H3("Feedback Template Management");
        infoTitle.getStyle().set("margin-top", "0");
        
        Span infoText = new Span("Create and manage structured feedback templates to guide users in providing " +
                                "constructive and comprehensive feedback. Templates help ensure consistency and " +
                                "completeness in feedback collection.");
        
        // Usage stats
        long totalTemplates = templateService.findAllTemplates().size();
        long activeTemplates = templateService.findActiveTemplates().size();
        
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setSpacing(true);
        statsLayout.add(
            createStatSpan("Total Templates", totalTemplates),
            createStatSpan("Active Templates", activeTemplates)
        );
        
        VerticalLayout content = new VerticalLayout(infoTitle, infoText, statsLayout);
        content.setPadding(false);
        content.setSpacing(false);
        
        infoCard.add(content);
        return infoCard;
    }
    
    private Span createStatSpan(String label, long value) {
        Span statSpan = new Span(label + ": " + value);
        statSpan.getStyle()
            .set("background-color", "var(--lumo-base-color)")
            .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
            .set("border-radius", "var(--lumo-border-radius-s)")
            .set("font-weight", "bold")
            .set("font-size", "var(--lumo-font-size-s)");
        return statSpan;
    }

    private void configureGrid() {
        grid.addClassName("template-grid");
        grid.setSizeFull();
        
        // Remove default columns and add custom ones
        grid.removeAllColumns();
        
        grid.addColumn(FeedbackTemplate::getName)
            .setHeader("Template Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(FeedbackTemplate::getDescription)
            .setHeader("Description").setAutoWidth(true);
        
        // Show question count
        grid.addColumn(template -> template.getQuestions().size())
            .setHeader("Questions").setAutoWidth(true);
        
        // Active status with visual indicator
        grid.addComponentColumn(template -> {
            Span activeSpan = new Span(template.isActive() ? "Active" : "Inactive");
            if (template.isActive()) {
                activeSpan.getStyle()
                    .set("color", "var(--lumo-success-color)")
                    .set("font-weight", "bold");
            } else {
                activeSpan.getStyle()
                    .set("color", "var(--lumo-error-color)")
                    .set("font-weight", "bold");
            }
            return activeSpan;
        }).setHeader("Status").setAutoWidth(true);
        
        // Actions column
        grid.addComponentColumn(template -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            Button viewButton = new Button("View");
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            viewButton.addClickListener(e -> openTemplatePreviewDialog(template));
            
            Button editButton = new Button("Edit");
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            editButton.addClickListener(e -> openTemplateForm(template));
            
            Button toggleButton = new Button(template.isActive() ? "Deactivate" : "Activate");
            toggleButton.addThemeVariants(ButtonVariant.LUMO_SMALL, 
                template.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS);
            toggleButton.addClickListener(e -> {
                template.setActive(!template.isActive());
                templateService.saveTemplate(template);
                updateList();
                showSuccess("Template " + (template.isActive() ? "activated" : "deactivated") + " successfully");
            });
            
            actions.add(viewButton, editButton, toggleButton);
            actions.setSpacing(true);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                openTemplatePreviewDialog(e.getValue());
            }
        });
    }

    private HorizontalLayout getToolbar() {
        Button addTemplateButton = new Button("Add Template");
        addTemplateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTemplateButton.addClickListener(e -> openTemplateForm(new FeedbackTemplate()));

        Button importTemplateButton = new Button("Import Default Templates");
        importTemplateButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        importTemplateButton.addClickListener(e -> {
            templateService.initializeDefaultTemplates();
            updateList();
            showSuccess("Default templates imported successfully");
        });

        HorizontalLayout toolbar = new HorizontalLayout(addTemplateButton, importTemplateButton);
        toolbar.addClassName("toolbar");
        toolbar.setSpacing(true);
        return toolbar;
    }

    private void configureForm() {
        name.setRequired(true);
        description.setRequired(true);
        instructions.setWidthFull();
        instructions.setHeight("100px");
        questions.setWidthFull();
        questions.setHeight("200px");
        questions.setPlaceholder("Enter each question on a new line...");
        
        // Custom binding for questions list
        binder.forField(name).bind(FeedbackTemplate::getName, FeedbackTemplate::setName);
        binder.forField(description).bind(FeedbackTemplate::getDescription, FeedbackTemplate::setDescription);
        binder.forField(instructions).bind(FeedbackTemplate::getInstructions, FeedbackTemplate::setInstructions);
        binder.forField(active).bind(FeedbackTemplate::isActive, FeedbackTemplate::setActive);
        
        // We need custom binding for the questions since they're stored as a list
        binder.forField(questions)
              .bind(
                  template -> String.join("\n", template.getQuestions()),
                  (template, value) -> {
                      List<String> questionList = new ArrayList<>(
                          Arrays.asList(value.split("\n"))
                      );
                      // Remove empty lines
                      questionList.removeIf(q -> q.trim().isEmpty());
                      template.setQuestions(questionList);
                  }
              );
    }
    
    private void openTemplatePreviewDialog(FeedbackTemplate template) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Template Preview: " + template.getName());
        dialog.setWidth("700px");
        dialog.setMaxHeight("80vh");
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        
        // Template info section
        VerticalLayout infoSection = new VerticalLayout();
        infoSection.setPadding(false);
        infoSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)");
        
        infoSection.add(
            createPreviewRow("Name:", template.getName()),
            createPreviewRow("Description:", template.getDescription()),
            createPreviewRow("Status:", template.isActive() ? "Active" : "Inactive"),
            createPreviewRow("Number of Questions:", String.valueOf(template.getQuestions().size()))
        );
        
        content.add(infoSection);
        
        // Instructions section
        if (template.getInstructions() != null && !template.getInstructions().isEmpty()) {
            H3 instructionsHeader = new H3("Instructions");
            Div instructionsDiv = new Div();
            instructionsDiv.setText(template.getInstructions());
            instructionsDiv.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-20pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("padding", "var(--lumo-space-m)")
                    .set("background-color", "var(--lumo-base-color)")
                    .set("font-style", "italic");
            
            content.add(instructionsHeader, instructionsDiv);
        }
        
        // Questions section
        H3 questionsHeader = new H3("Questions");
        content.add(questionsHeader);
        
        for (int i = 0; i < template.getQuestions().size(); i++) {
            Div questionDiv = new Div();
            questionDiv.setText((i + 1) + ". " + template.getQuestions().get(i));
            questionDiv.getStyle()
                    .set("border", "1px solid var(--lumo-primary-color-50pct)")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("padding", "var(--lumo-space-s)")
                    .set("margin-bottom", "var(--lumo-space-s)")
                    .set("background-color", "var(--lumo-primary-color-10pct)");
            
            content.add(questionDiv);
        }
        
        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        
        Button editButton = new Button("Edit Template");
        editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        editButton.addClickListener(e -> {
            dialog.close();
            openTemplateForm(template);
        });
        
        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> dialog.close());
        
        buttonLayout.add(editButton, closeButton);
        buttonLayout.setSpacing(true);
        
        content.add(buttonLayout);
        
        dialog.add(content);
        dialog.open();
    }
    
    private HorizontalLayout createPreviewRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        
        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-weight", "bold");
        
        Span valueSpan = new Span(value);
        
        row.add(labelSpan, valueSpan);
        return row;
    }

    private void openTemplateForm(FeedbackTemplate templateParam) {
        if (templateParam == null) {
            return;
        }
        
        FeedbackTemplate template = templateParam;
        boolean isNewTemplate = template.getId() == null;
        
        // If this is an existing template (has ID), load it completely from the service
        if (template.getId() != null) {
            FeedbackTemplate loadedTemplate = templateService.findTemplateById(template.getId()).orElse(null);
            if (loadedTemplate == null) {
                showError("Template no longer exists");
                updateList();
                return;
            }
            template = loadedTemplate;
        }
        
        // Create a final reference to use in lambda expressions
        final FeedbackTemplate finalTemplate = template;
        
        currentTemplate = finalTemplate;
        binder.setBean(finalTemplate);
        
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNewTemplate ? "Add Template" : "Edit Template");
        dialog.setWidth("700px");
        
        // Form layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(name, description, instructions, questions, active);
        formLayout.setColspan(instructions, 2);
        formLayout.setColspan(questions, 2);
        
        // Preview section
        Div previewSection = new Div();
        previewSection.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-top", "var(--lumo-space-m)");
        
        H3 previewTitle = new H3("Preview");
        previewTitle.getStyle().set("margin-top", "0");
        
        Div previewContent = new Div();
        previewContent.setText("Preview will appear here when you enter questions...");
        previewContent.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        previewSection.add(previewTitle, previewContent);
        
        // Update preview when questions change
        questions.addValueChangeListener(e -> {
            String questionsText = e.getValue();
            if (questionsText != null && !questionsText.trim().isEmpty()) {
                String[] questionLines = questionsText.split("\n");
                StringBuilder preview = new StringBuilder();
                int questionNumber = 1;
                for (String line : questionLines) {
                    if (!line.trim().isEmpty()) {
                        preview.append(questionNumber).append(". ").append(line.trim()).append("\n");
                        questionNumber++;
                    }
                }
                previewContent.setText(preview.toString());
                previewContent.getStyle().remove("color");
            } else {
                previewContent.setText("Preview will appear here when you enter questions...");
                previewContent.getStyle().set("color", "var(--lumo-secondary-text-color)");
            }
        });
        
        // Buttons
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            if (binder.validate().isOk()) {
                // Validate that we have at least one question
                if (currentTemplate.getQuestions().isEmpty()) {
                    showError("Please add at least one question");
                    return;
                }
                
                templateService.saveTemplate(currentTemplate);
                dialog.close();
                updateList();
                showSuccess("Template saved successfully");
            } else {
                showError("Please fix the validation errors");
            }
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        
        if (!isNewTemplate && authenticationService.isCurrentUserSuperAdmin()) {
            Button deleteButton = new Button("Delete");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> {
                // Confirmation dialog
                Dialog confirmDialog = new Dialog();
                confirmDialog.setHeaderTitle("Confirm Delete");
                
                VerticalLayout confirmContent = new VerticalLayout();
                confirmContent.add(new Span("Are you sure you want to delete this template?"));
                confirmContent.add(new Span("This action cannot be undone."));
                
                HorizontalLayout confirmButtons = new HorizontalLayout();
                Button confirmDelete = new Button("Delete");
                confirmDelete.addThemeVariants(ButtonVariant.LUMO_ERROR);
                confirmDelete.addClickListener(ce -> {
                    templateService.deleteTemplate(finalTemplate.getId());
                    confirmDialog.close();
                    dialog.close();
                    updateList();
                    showSuccess("Template deleted successfully");
                });
                
                Button cancelDelete = new Button("Cancel");
                cancelDelete.addClickListener(ce -> confirmDialog.close());
                
                confirmButtons.add(confirmDelete, cancelDelete);
                confirmContent.add(confirmButtons);
                
                confirmDialog.add(confirmContent);
                confirmDialog.open();
            });
            buttonLayout.add(deleteButton);
        }
        
        buttonLayout.setSpacing(true);
        
        VerticalLayout dialogContent = new VerticalLayout(formLayout, previewSection, buttonLayout);
        dialogContent.setPadding(false);
        
        dialog.add(dialogContent);
        dialog.open();
    }

    private void updateList() {
        grid.setItems(templateService.findAllTemplates());
    }
    
    private void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}