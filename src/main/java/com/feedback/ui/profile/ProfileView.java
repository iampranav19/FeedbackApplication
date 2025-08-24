package com.feedback.ui.profile;

import com.feedback.model.User;
import com.feedback.service.AuthenticationService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile | Feedback System")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final AuthenticationService authenticationService;
    
    private final PasswordField currentPassword = new PasswordField("Current Password");
    private final PasswordField newPassword = new PasswordField("New Password");
    private final PasswordField confirmPassword = new PasswordField("Confirm New Password");

    public ProfileView(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        
        System.out.println("ProfileView: Constructor started");
        
        // Spring Security ensures authentication - just get the current user
        User currentUser = authenticationService.getCurrentUser();
        
        // Safety check - if user is null, something is wrong with authentication
        if (currentUser == null) {
            System.err.println("CRITICAL: Current user is null in ProfileView!");
            add(new Span("Authentication error. Please refresh the page and try again."));
            return;
        }
        
        System.out.println("ProfileView: User authenticated: " + currentUser.getFullName());

        addClassName("profile-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);

        try {
            createProfileContent();
            System.out.println("ProfileView: Successfully created profile for user: " + currentUser.getFullName());
        } catch (Exception e) {
            System.err.println("Error initializing ProfileView: " + e.getMessage());
            e.printStackTrace();
            add(new Span("Error loading profile. Please try again."));
        }
    }

    private void createProfileContent() {
        User currentUser = authenticationService.getCurrentUser();
        
        VerticalLayout profileCard = new VerticalLayout();
        profileCard.setWidth("600px");
        profileCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-l)")
                .set("padding", "var(--lumo-space-l)");

        H2 title = new H2("User Profile");
        
        // User Information Section
        VerticalLayout userInfoSection = new VerticalLayout();
        userInfoSection.setPadding(false);
        userInfoSection.setSpacing(false);
        
        H3 infoTitle = new H3("Personal Information");
        
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setPadding(false);
        infoLayout.setSpacing(false);
        infoLayout.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)")
                             .set("border-radius", "var(--lumo-border-radius-m)")
                             .set("padding", "var(--lumo-space-m)")
                             .set("background", "var(--lumo-contrast-5pct)");
        
        infoLayout.add(
                createInfoRow("Full Name", currentUser.getFullName()),
                createInfoRow("Email", currentUser.getEmail()),
                createInfoRow("Username", currentUser.getUsername()),
                createInfoRow("Role", currentUser.getRole().getName()),
                createInfoRow("Department", currentUser.getDepartment() != null ? currentUser.getDepartment().getName() : "Not assigned"),
                createInfoRow("Manager", currentUser.getManager() != null ? currentUser.getManager().getFullName() : "None")
        );
        
        userInfoSection.add(infoTitle, infoLayout);
        
        // Password Change Section
        VerticalLayout passwordSection = createPasswordChangeSection();
        
        profileCard.add(title, userInfoSection, passwordSection);
        add(profileCard);
    }
    
    private HorizontalLayout createInfoRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        
        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle().set("font-weight", "bold");
        
        Span valueSpan = new Span(value != null ? value : "Not set");
        
        row.add(labelSpan, valueSpan);
        return row;
    }

    private VerticalLayout createPasswordChangeSection() {
        VerticalLayout passwordSection = new VerticalLayout();
        passwordSection.setPadding(false);
        
        H3 passwordTitle = new H3("Change Password");
        
        Paragraph passwordHint = new Paragraph("Password must be at least 6 characters long.");
        passwordHint.getStyle().set("color", "var(--lumo-secondary-text-color)")
                                 .set("font-size", "var(--lumo-font-size-s)");
        
        configurePasswordFields();
        
        FormLayout passwordForm = new FormLayout();
        passwordForm.add(currentPassword, newPassword, confirmPassword);
        passwordForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        
        Button changePasswordButton = new Button("Change Password");
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordButton.addClickListener(e -> changePassword());
        
        Button cancelButton = new Button("Clear");
        cancelButton.addClickListener(e -> clearPasswordFields());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(changePasswordButton, cancelButton);
        buttonLayout.setSpacing(true);
        
        passwordSection.add(passwordTitle, passwordHint, passwordForm, buttonLayout);
        return passwordSection;
    }

    private void configurePasswordFields() {
        currentPassword.setRequired(true);
        currentPassword.setWidthFull();
        currentPassword.setPlaceholder("Enter your current password");

        newPassword.setRequired(true);
        newPassword.setWidthFull();
        newPassword.setMinLength(6);
        newPassword.setPlaceholder("Enter new password");
        newPassword.setHelperText("Minimum 6 characters");

        confirmPassword.setRequired(true);
        confirmPassword.setWidthFull();
        confirmPassword.setPlaceholder("Confirm new password");
        
        // Add validation for password confirmation
        confirmPassword.addValueChangeListener(event -> {
            if (!newPassword.isEmpty() && !confirmPassword.getValue().equals(newPassword.getValue())) {
                confirmPassword.setErrorMessage("Passwords do not match");
                confirmPassword.setInvalid(true);
            } else {
                confirmPassword.setInvalid(false);
            }
        });
        
        newPassword.addValueChangeListener(event -> {
            if (!confirmPassword.isEmpty()) {
                confirmPassword.clear();
            }
        });
    }

    private void changePassword() {
        String currentPasswordValue = currentPassword.getValue();
        String newPasswordValue = newPassword.getValue();
        String confirmPasswordValue = confirmPassword.getValue();

        // Validate fields
        if (currentPasswordValue.isEmpty() || newPasswordValue.isEmpty() || confirmPasswordValue.isEmpty()) {
            showError("Please fill in all password fields");
            return;
        }

        if (newPasswordValue.length() < 6) {
            showError("New password must be at least 6 characters long");
            return;
        }

        if (!newPasswordValue.equals(confirmPasswordValue)) {
            showError("New passwords do not match");
            return;
        }

        if (currentPasswordValue.equals(newPasswordValue)) {
            showError("New password must be different from current password");
            return;
        }

        // Attempt to change password
        User currentUser = authenticationService.getCurrentUser();
        if (authenticationService.changePassword(currentUser, currentPasswordValue, newPasswordValue)) {
            showSuccess("Password changed successfully!");
            clearPasswordFields();
        } else {
            showError("Current password is incorrect");
            currentPassword.clear();
            currentPassword.focus();
        }
    }

    private void clearPasswordFields() {
        currentPassword.clear();
        newPassword.clear();
        confirmPassword.clear();
        currentPassword.focus();
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