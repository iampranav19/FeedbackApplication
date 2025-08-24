package com.feedback.ui.views.login;

import com.feedback.service.AuthenticationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login | Feedback System")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthenticationService authenticationService;
    
    private final EmailField email = new EmailField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final Button loginButton = new Button("Login");
    
    private boolean hasRedirected = false; // Prevent infinite redirects

    public LoginView(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        
        System.out.println("LoginView: Initializing login view");
        
        addClassName("login-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        createLoginForm();
    }

    private void createLoginForm() {
        H1 title = new H1("Feedback System");
        title.getStyle().set("color", "var(--lumo-primary-color)");
        
        H2 subtitle = new H2("Please sign in");
        
        // Create login card
        VerticalLayout loginCard = new VerticalLayout();
        loginCard.setWidth("400px");
        loginCard.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-l)")
                .set("padding", "var(--lumo-space-l)");

        configureForm();
        
        // Add some demo credentials info
        Div demoInfo = new Div();
        demoInfo.getStyle().set("margin-top", "var(--lumo-space-m)");
        Paragraph demoText = new Paragraph("Demo Credentials:");
        demoText.getStyle().set("font-weight", "bold").set("margin", "0");
        Paragraph credText = new Paragraph("Super Admin: pranav@company.com / test123");
        credText.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "0");
        demoInfo.add(demoText, credText);
        
        loginCard.add(subtitle, email, password, loginButton, demoInfo);
        loginCard.setAlignItems(FlexComponent.Alignment.STRETCH);
        
        add(title, loginCard);
    }

    private void configureForm() {
        email.setPlaceholder("Enter your email");
        email.setRequired(true);
        email.setErrorMessage("Please enter a valid email address");
        email.setWidthFull();

        password.setPlaceholder("Enter your password");
        password.setRequired(true);
        password.setWidthFull();

        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(e -> attemptLogin());
        
        // Allow login with Enter key
        password.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> attemptLogin());
    }

    private void attemptLogin() {
        String emailValue = email.getValue();
        String passwordValue = password.getValue();

        if (emailValue.isEmpty() || passwordValue.isEmpty()) {
            showError("Please enter both email and password");
            return;
        }

        System.out.println("LoginView: Attempting login for: " + emailValue);

        try {
            if (authenticationService.authenticate(emailValue, passwordValue)) {
                System.out.println("LoginView: Authentication successful for: " + emailValue);
                
                showSuccess("Login successful!");
                
                // Use getUI() to ensure we have the current UI context
                // Add a small delay to ensure the success message is visible
                UI currentUI = UI.getCurrent();
                if (currentUI != null) {
                    hasRedirected = true; // Prevent any other redirects
                    currentUI.access(() -> {
                        try {
                            Thread.sleep(500); // Small delay for user feedback
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        currentUI.navigate("");
                        currentUI.push();
                    });
                } else {
                    // Fallback - direct navigation
                    UI.getCurrent().navigate("");
                }
                
            } else {
                System.out.println("LoginView: Authentication failed for: " + emailValue);
                showError("Invalid email or password");
                password.clear();
                password.focus();
            }
        } catch (Exception e) {
            System.err.println("LoginView: Login error: " + e.getMessage());
            e.printStackTrace();
            showError("Login error: " + e.getMessage());
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // FIXED: Add protection against infinite redirects
        if (hasRedirected) {
            System.out.println("LoginView: Already redirected, skipping authentication check");
            return;
        }
        
        // Check if user is already authenticated
        try {
            if (authenticationService.isAuthenticated()) {
                System.out.println("LoginView: User already authenticated, redirecting to dashboard");
                hasRedirected = true; // Mark as redirected
                beforeEnterEvent.forwardTo("");
                return;
            } else {
                System.out.println("LoginView: User not authenticated, showing login form");
            }
        } catch (Exception e) {
            System.err.println("LoginView: Error checking authentication: " + e.getMessage());
            e.printStackTrace();
            // Continue to show login form on error
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 2000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}