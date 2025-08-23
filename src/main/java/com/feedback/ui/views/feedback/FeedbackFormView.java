package com.feedback.ui.views.feedback;

import com.feedback.model.Feedback;
import com.feedback.model.FeedbackTemplate;
import com.feedback.model.PrivacyLevel;
import com.feedback.model.User;
import com.feedback.service.FeedbackService;
import com.feedback.service.FeedbackTemplateService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.util.List;

@Route(value = "feedback-form", layout = MainLayout.class)
@PageTitle("Give Feedback | Feedback System")
public class FeedbackFormView extends VerticalLayout {

    private final FeedbackService feedbackService;
    private final UserService userService;
    private final FeedbackTemplateService templateService;

    private final ComboBox<User> recipient = new ComboBox<>("Recipient");
    private final ComboBox<String> category = new ComboBox<>("Category");
    private final TextArea content = new TextArea("Feedback");
    private final ComboBox<PrivacyLevel> privacyLevel = new ComboBox<>("Privacy Level");
    private final ComboBox<FeedbackTemplate> templateSelector = new ComboBox<>("Use Template");
    
    // For structured feedback based on templates
    private final VerticalLayout templateQuestionsLayout = new VerticalLayout();
    private final TextArea[] questionAnswers = new TextArea[10]; // Max 10 questions per template

    public FeedbackFormView(FeedbackService feedbackService, 
                          UserService userService,
                          FeedbackTemplateService templateService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
        this.templateService = templateService;

        addClassName("feedback-form-view");
        setSizeFull();

        configureForm();

        add(
                new H2("Give Feedback"),
                recipient,
                category,
                new HorizontalLayout(privacyLevel, templateSelector),
                content,
                templateQuestionsLayout,
                createButtonLayout()
        );
    }

    private void configureForm() {
        recipient.setItems(userService.findAllUsers());
        recipient.setItemLabelGenerator(User::getFullName);
        recipient.setRequired(true);

        category.setItems("Performance", "Leadership", "Communication", "Teamwork", "Technical Skills", "Other");
        category.setRequired(true);

        content.setMinLength(10);
        content.setMaxLength(1000);
        content.setPlaceholder("Your feedback here...");
        content.setRequired(true);
        content.setWidthFull();
        content.setHeight("200px");

        privacyLevel.setItems(PrivacyLevel.values());
        privacyLevel.setItemLabelGenerator(PrivacyLevel::getDescription);
        privacyLevel.setValue(PrivacyLevel.PRIVATE);
        privacyLevel.setRequired(true);
        
        // Template selector
        templateSelector.setItems(templateService.findActiveTemplates());
        templateSelector.setItemLabelGenerator(FeedbackTemplate::getName);
        templateSelector.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                applyTemplate(event.getValue());
            } else {
                clearTemplateQuestions();
            }
        });
        
        // Initially hide the template questions layout
        templateQuestionsLayout.setVisible(false);
    }
    
    private void applyTemplate(FeedbackTemplate template) {
        // Clear any existing questions
        templateQuestionsLayout.removeAll();
        
        // Show the instructions
        if (template.getInstructions() != null && !template.getInstructions().isEmpty()) {
            Paragraph instructions = new Paragraph(template.getInstructions());
            instructions.getStyle().set("font-style", "italic");
            templateQuestionsLayout.add(instructions);
        }
        
        // Add each question with a text area for the answer
        List<String> questions = template.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            
            // Create a text area for the answer
            TextArea answer = new TextArea(question);
            answer.setWidthFull();
            answer.setMinHeight("100px");
            questionAnswers[i] = answer;
            
            templateQuestionsLayout.add(answer);
        }
        
        // Make template questions visible and hide the main content area
        templateQuestionsLayout.setVisible(true);
        content.setVisible(false);
    }
    
    private void clearTemplateQuestions() {
        templateQuestionsLayout.removeAll();
        templateQuestionsLayout.setVisible(false);
        content.setVisible(true);
    }

    private Button createButtonLayout() {
        Button submitButton = new Button("Submit Feedback");
        submitButton.addClickListener(click -> {
            if (validateForm()) {
                saveFeedback();
            }
        });
        
        return submitButton;
    }

    private boolean validateForm() {
        if (recipient.isEmpty()) {
            Notification.show("Please select a recipient");
            return false;
        }

        if (category.isEmpty()) {
            Notification.show("Please select a category");
            return false;
        }

        if (templateSelector.getValue() == null) {
            // Traditional free-form feedback validation
            if (content.isEmpty() || content.getValue().length() < 10) {
                Notification.show("Please provide feedback with at least 10 characters");
                return false;
            }
        } else {
            // Template-based feedback validation
            FeedbackTemplate template = templateSelector.getValue();
            List<String> questions = template.getQuestions();
            
            for (int i = 0; i < questions.size(); i++) {
                if (questionAnswers[i] == null || questionAnswers[i].isEmpty()) {
                    Notification.show("Please answer all template questions");
                    return false;
                }
            }
        }

        if (privacyLevel.isEmpty()) {
            Notification.show("Please select a privacy level");
            return false;
        }

        return true;
    }

    private void saveFeedback() {
        Feedback feedback = new Feedback();
        // In a real system, this would be the logged-in user
        // For the MVP, we'll use the admin user
        User currentUser = userService.findUserById(1L).orElse(null);
        
        feedback.setSender(currentUser);
        feedback.setRecipient(recipient.getValue());
        feedback.setCategory(category.getValue());
        
        // Determine the content based on whether a template is being used
        if (templateSelector.getValue() == null) {
            feedback.setContent(content.getValue());
        } else {
            // Format template responses
            StringBuilder contentBuilder = new StringBuilder();
            FeedbackTemplate template = templateSelector.getValue();
            List<String> questions = template.getQuestions();
            
            contentBuilder.append("Template: ").append(template.getName()).append("\n\n");
            
            for (int i = 0; i < questions.size(); i++) {
                contentBuilder.append(questions.get(i)).append("\n");
                contentBuilder.append(questionAnswers[i].getValue()).append("\n\n");
            }
            
            feedback.setContent(contentBuilder.toString());
        }
        
        feedback.setPrivacyLevel(privacyLevel.getValue());
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setRead(false);
        feedback.setStatus("Open");

        feedbackService.saveFeedback(feedback);
        
        Notification.show("Feedback submitted successfully");
        clearForm();
    }

    private void clearForm() {
        recipient.clear();
        category.clear();
        content.clear();
        privacyLevel.setValue(PrivacyLevel.PRIVATE);
        templateSelector.clear();
        clearTemplateQuestions();
        content.setVisible(true);
    }
}