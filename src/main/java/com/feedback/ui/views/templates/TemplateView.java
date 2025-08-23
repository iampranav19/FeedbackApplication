package com.feedback.ui.views.templates;

import com.feedback.model.FeedbackTemplate;
import com.feedback.service.FeedbackTemplateService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Route(value = "templates", layout = MainLayout.class)
@PageTitle("Feedback Templates | Feedback System")
public class TemplateView extends VerticalLayout {

    private final FeedbackTemplateService templateService;
    private final Grid<FeedbackTemplate> grid = new Grid<>(FeedbackTemplate.class);
    private final Binder<FeedbackTemplate> binder = new Binder<>(FeedbackTemplate.class);
    private FeedbackTemplate currentTemplate = null;

    private final TextField name = new TextField("Template Name");
    private final TextField description = new TextField("Description");
    private final TextArea instructions = new TextArea("Instructions");
    private final TextArea questions = new TextArea("Questions (one per line)");
    private final Checkbox active = new Checkbox("Active");

    public TemplateView(FeedbackTemplateService templateService) {
        this.templateService = templateService;
        addClassName("template-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(
                new H2("Feedback Templates"),
                getToolbar(),
                grid
        );

        updateList();
    }

    private void configureGrid() {
        grid.addClassName("template-grid");
        grid.setSizeFull();
        grid.setColumns("name", "description");
        grid.addColumn(template -> template.isActive() ? "Yes" : "No").setHeader("Active");
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        
        grid.asSingleSelect().addValueChangeListener(e -> openTemplateForm(e.getValue()));
    }

    private HorizontalLayout getToolbar() {
        Button addTemplateButton = new Button("Add Template");
        addTemplateButton.addClickListener(e -> openTemplateForm(new FeedbackTemplate()));

        HorizontalLayout toolbar = new HorizontalLayout(addTemplateButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void configureForm() {
        name.setRequired(true);
        description.setRequired(true);
        instructions.setWidthFull();
        questions.setWidthFull();
        questions.setHeight("200px");
        
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
                      template.setQuestions(questionList);
                  }
              );
    }

    private void openTemplateForm(FeedbackTemplate templateParam) {
        if (templateParam == null) {
            return;
        }
        
        FeedbackTemplate template = templateParam;
        
        // If this is an existing template (has ID), load it completely from the service
        if (template.getId() != null) {
            // Fetch the complete template with questions within a transaction
            FeedbackTemplate loadedTemplate = templateService.findTemplateById(template.getId()).orElse(null);
            if (loadedTemplate == null) {
                Notification.show("Template no longer exists");
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
        dialog.setHeaderTitle(finalTemplate.getId() == null ? "Add Template" : "Edit Template");
        dialog.setWidth("600px");
        
        FormLayout formLayout = new FormLayout();
        formLayout.add(name, description, instructions, questions, active);
        formLayout.setColspan(instructions, 2);
        formLayout.setColspan(questions, 2);
        
        Button saveButton = new Button("Save");
        saveButton.addClickListener(e -> {
            if (binder.validate().isOk()) {
                templateService.saveTemplate(currentTemplate);
                dialog.close();
                updateList();
                Notification.show("Template saved successfully");
            }
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> dialog.close());
        
        Button deleteButton = new Button("Delete");
        deleteButton.setVisible(finalTemplate.getId() != null);
        deleteButton.addClickListener(e -> {
            if (finalTemplate.getId() != null) {
                templateService.deleteTemplate(finalTemplate.getId());
                dialog.close();
                updateList();
                Notification.show("Template deleted");
            }
        });
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton, deleteButton);
        buttonLayout.setSpacing(true);
        
        dialog.add(formLayout, buttonLayout);
        dialog.open();
    }

    private void updateList() {
        grid.setItems(templateService.findAllTemplates());
    }
}