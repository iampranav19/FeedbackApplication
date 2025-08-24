package com.feedback.ui.views.feedback;

import com.feedback.model.Feedback;
import com.feedback.model.FeedbackTemplate;
import com.feedback.model.PrivacyLevel;
import com.feedback.model.User;
import com.feedback.service.AuthenticationService;
import com.feedback.service.FeedbackService;
import com.feedback.service.FeedbackTemplateService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDateTime;
import java.util.List;

@Route(value = "feedback-form", layout = MainLayout.class)
@PageTitle("Give Feedback | Feedback System")
@PermitAll
public class FeedbackFormView extends VerticalLayout {

    private final FeedbackService feedbackService;
    private final UserService userService;
    private final FeedbackTemplateService templateService;
    private final AuthenticationService authenticationService;

    private final ComboBox<User> recipient = new ComboBox<>("Recipient");
    private final ComboBox<String> category = new ComboBox<>("Category");
    private final TextArea content = new TextArea("Feedback");
    private final ComboBox<PrivacyLevel> privacyLevel = new ComboBox<>("Privacy Level");
    private final ComboBox<FeedbackTemplate> templateSelector = new ComboBox<>("Use Template");
    
    // For structured feedback based on templates
    private final VerticalLayout templateQuestionsLayout = new VerticalLayout();
    private final TextArea[] questionAnswers = new TextArea[10]; // Max 10 questions per template
    
    private User currentUser;

    public FeedbackFormView(FeedbackService feedbackService, 
                          UserService userService,
                          FeedbackTemplateService templateService,
                          AuthenticationService authenticationService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
        this.templateService = templateService;
        this.authenticationService = authenticationService;
        
        System.out.println("FeedbackFormView: Constructor started");
        
        // Spring Security ensures authentication - just get the current user
        this.currentUser = authenticationService.getCurrentUser();
        
        // Safety check - if user is null, something is wrong with authentication
        if (this.currentUser == null) {
            System.err.println("CRITICAL: Current user is null in FeedbackFormView!");
            add(new Span("Authentication error. Please refresh the page and try again."));
            return;
        }
        
        System.out.println("FeedbackFormView: User authenticated: " + currentUser.getFullName());

        addClassName("feedback-form-view");
        setSizeFull();

        try {
            configureForm();
            
            // Welcome message
            Div welcomeCard = createWelcomeCard();

            add(
                    new H2("Give Feedback"),
                    welcomeCard,
                    recipient,
                    category,
                    new HorizontalLayout(privacyLevel, templateSelector),
                    content,
                    templateQuestionsLayout,
                    createButtonLayout()
            );
            
            System.out.println("FeedbackFormView: Successfully created form for user: " + currentUser.getFullName());
        } catch (Exception e) {
            System.err.println("Error initializing FeedbackFormView: " + e.getMessage());
            e.printStackTrace();
            add(new Span("Error loading feedback form. Please try again."));
        }
    }
    
    private Div createWelcomeCard() {
        Div welcomeCard = new Div();
        welcomeCard.getStyle()
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("border", "1px solid var(--lumo-primary-color-50pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        Span welcomeText = new Span("Welcome, " + currentUser.getFirstName() + "! ");
        welcomeText.getStyle().set("font-weight", "bold");
        
        Span instructionText = new Span("Use this form to provide constructive feedback to your colleagues. " +
                "Remember, good feedback is specific, actionable, and focuses on behaviors rather than personality.");
        
        welcomeCard.add(welcomeText, instructionText);
        return welcomeCard;
    }

    private void configureForm() {
        System.out.println("FeedbackFormView: Configuring form...");
        
        // Only show users that the current user can give feedback to
        List<User> availableRecipients = userService.getUsersForFeedback(currentUser);
        recipient.setItems(availableRecipients);
        recipient.setItemLabelGenerator(user -> user.getFullName() + " (" + user.getRole().getName() + ")");
        recipient.setRequired(true);
        recipient.setHelperText("Select the person you want to give feedback to");

        category.setItems("Performance", "Leadership", "Communication", "Teamwork", "Technical Skills", 
                         "Project Management", "Innovation", "Collaboration", "Other");
        category.setRequired(true);
        category.setValue("Performance"); // Default selection

        content.setMinLength(10);
        content.setMaxLength(1000);
        content.setPlaceholder("Your feedback here... Be specific and constructive.");
        content.setRequired(true);
        content.setWidthFull();
        content.setHeight("200px");
        content.setHelperText("Minimum 10 characters, maximum 1000 characters");

        privacyLevel.setItems(PrivacyLevel.values());
        privacyLevel.setItemLabelGenerator(PrivacyLevel::getDescription);
        privacyLevel.setValue(PrivacyLevel.PRIVATE);
        privacyLevel.setRequired(true);
        privacyLevel.setHelperText("Choose who can see this feedback");
        
        // Template selector
        List<FeedbackTemplate> templates = templateService.findActiveTemplates();
        templateSelector.setItems(templates);
        templateSelector.setItemLabelGenerator(template -> 
            template.getName() + " - " + template.getDescription());
        templateSelector.setHelperText("Optional: Use a structured template for your feedback");
        templateSelector.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                applyTemplate(event.getValue());
            } else {
                clearTemplateQuestions();
            }
        });
        
        // Initially hide the template questions layout
        templateQuestionsLayout.setVisible(false);
        
        System.out.println("FeedbackFormView: Form configuration completed");
    }
    
    private void applyTemplate(FeedbackTemplate template) {
        // Clear any existing questions
        templateQuestionsLayout.removeAll();
        
        // Show template info
        Div templateInfo = new Div();
        templateInfo.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        Span templateTitle = new Span("Using Template: " + template.getName());
        templateTitle.getStyle().set("font-weight", "bold");
        
        templateInfo.add(templateTitle);
        
        // Show the instructions
        if (template.getInstructions() != null && !template.getInstructions().isEmpty()) {
            Paragraph instructions = new Paragraph(template.getInstructions());
            instructions.getStyle().set("font-style", "italic")
                                  .set("color", "var(--lumo-secondary-text-color)");
            templateInfo.add(instructions);
        }
        
        templateQuestionsLayout.add(templateInfo);
        
        // Add each question with a text area for the answer
        List<String> questions = template.getQuestions();
        for (int i = 0; i < questions.size() && i < questionAnswers.length; i++) {
            String question = questions.get(i);
            
            // Create a text area for the answer
            TextArea answer = new TextArea();
            answer.setLabel("Question " + (i + 1) + ": " + question);
            answer.setWidthFull();
            answer.setMinHeight("80px");
            answer.setRequired(true);
            answer.setHelperText("Please provide a detailed response");
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
        
        // Clear question answers array
        for (int i = 0; i < questionAnswers.length; i++) {
            questionAnswers[i] = null;
        }
    }

    private HorizontalLayout createButtonLayout() {
        Button submitButton = new Button("Submit Feedback");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClickListener(click -> {
            if (validateForm()) {
                saveFeedback();
            }
        });
        
        Button clearButton = new Button("Clear Form");
        clearButton.addClickListener(click -> clearForm());
        
        Button previewButton = new Button("Preview");
        previewButton.addClickListener(click -> previewFeedback());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(submitButton, clearButton, previewButton);
        buttonLayout.setSpacing(true);
        
        return buttonLayout;
    }

    private boolean validateForm() {
        if (recipient.isEmpty()) {
            showError("Please select a recipient");
            return false;
        }

        if (category.isEmpty()) {
            showError("Please select a category");
            return false;
        }

        if (templateSelector.getValue() == null) {
            // Traditional free-form feedback validation
            if (content.isEmpty() || content.getValue().length() < 10) {
                showError("Please provide feedback with at least 10 characters");
                return false;
            }
        } else {
            // Template-based feedback validation
            FeedbackTemplate template = templateSelector.getValue();
            List<String> questions = template.getQuestions();
            
            for (int i = 0; i < questions.size() && i < questionAnswers.length; i++) {
                if (questionAnswers[i] == null || questionAnswers[i].isEmpty()) {
                    showError("Please answer all template questions");
                    questionAnswers[i].focus();
                    return false;
                }
                
                if (questionAnswers[i].getValue().length() < 5) {
                    showError("Please provide more detailed responses (minimum 5 characters per question)");
                    questionAnswers[i].focus();
                    return false;
                }
            }
        }

        if (privacyLevel.isEmpty()) {
            showError("Please select a privacy level");
            return false;
        }
        
        // Check if user can give feedback to the selected recipient
        if (!authenticationService.canGiveFeedbackTo(recipient.getValue())) {
            showError("You cannot give feedback to this user");
            return false;
        }

        return true;
    }
    
    private void previewFeedback() {
        if (!validateForm()) {
            return;
        }
        
        // Create preview dialog
        com.vaadin.flow.component.dialog.Dialog previewDialog = new com.vaadin.flow.component.dialog.Dialog();
        previewDialog.setHeaderTitle("Feedback Preview");
        previewDialog.setWidth("600px");
        
        VerticalLayout previewContent = new VerticalLayout();
        previewContent.add(new Span("To: " + recipient.getValue().getFullName()));
        previewContent.add(new Span("Category: " + category.getValue()));
        previewContent.add(new Span("Privacy: " + privacyLevel.getValue().getDescription()));
        
        String feedbackText = getFeedbackContent();
        TextArea previewText = new TextArea("Feedback Content");
        previewText.setValue(feedbackText);
        previewText.setReadOnly(true);
        previewText.setWidthFull();
        previewText.setHeight("300px");
        
        previewContent.add(previewText);
        
        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> previewDialog.close());
        
        previewContent.add(closeButton);
        previewDialog.add(previewContent);
        previewDialog.open();
    }
    
    private String getFeedbackContent() {
        if (templateSelector.getValue() == null) {
            return content.getValue();
        } else {
            // Format template responses
            StringBuilder contentBuilder = new StringBuilder();
            FeedbackTemplate template = templateSelector.getValue();
            List<String> questions = template.getQuestions();
            
            contentBuilder.append("Template: ").append(template.getName()).append("\n\n");
            
            for (int i = 0; i < questions.size() && i < questionAnswers.length; i++) {
                if (questionAnswers[i] != null) {
                    contentBuilder.append(questions.get(i)).append("\n");
                    contentBuilder.append(questionAnswers[i].getValue()).append("\n\n");
                }
            }
            
            return contentBuilder.toString();
        }
    }

    private void saveFeedback() {
        try {
            Feedback feedback = new Feedback();
            feedback.setSender(currentUser);
            feedback.setRecipient(recipient.getValue());
            feedback.setCategory(category.getValue());
            feedback.setContent(getFeedbackContent());
            feedback.setPrivacyLevel(privacyLevel.getValue());
            feedback.setCreatedAt(LocalDateTime.now());
            feedback.setRead(false);
            feedback.setStatus("Open");

            feedbackService.saveFeedback(feedback);
            
            showSuccess("Feedback submitted successfully! The recipient will be notified.");
            clearForm();
            
            // Navigate back to dashboard after successful submission
            UI.getCurrent().navigate("");
            
        } catch (Exception e) {
            System.err.println("Error saving feedback: " + e.getMessage());
            e.printStackTrace();
            showError("Error submitting feedback: " + e.getMessage());
        }
    }

    private void clearForm() {
        recipient.clear();
        category.setValue("Performance");
        content.clear();
        privacyLevel.setValue(PrivacyLevel.PRIVATE);
        templateSelector.clear();
        clearTemplateQuestions();
        content.setVisible(true);
        recipient.focus();
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