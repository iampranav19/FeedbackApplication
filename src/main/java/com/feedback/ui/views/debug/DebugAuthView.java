package com.feedback.ui.views.debug;

import com.feedback.model.User;
import com.feedback.service.AuthenticationService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Debug view to troubleshoot authentication issues.
 * Access via: /debug-auth
 * Remove this in production!
 */
@Route(value = "debug-auth")
@PageTitle("Debug Authentication | Feedback System")
@PermitAll
public class DebugAuthView extends VerticalLayout {

    private final AuthenticationService authenticationService;

    public DebugAuthView(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        
        System.out.println("DebugAuthView: Constructor called");

        add(new H1("Authentication Debug Information"));
        
        try {
            // Check VaadinSession
            H3 sessionHeader = new H3("VaadinSession Information:");
            add(sessionHeader);
            
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                add(new Paragraph("✅ VaadinSession exists"));
                
                Object sessionUser = session.getAttribute(AuthenticationService.CURRENT_USER_SESSION_ATTRIBUTE);
                if (sessionUser != null && sessionUser instanceof User) {
                    User user = (User) sessionUser;
                    add(new Paragraph("✅ User found in VaadinSession: " + user.getFullName()));
                    add(new Paragraph("   Email: " + user.getEmail()));
                    add(new Paragraph("   Role: " + user.getRole().getName()));
                    add(new Paragraph("   Active: " + user.isActive()));
                    add(new Paragraph("   Super Admin: " + user.isSuperAdmin()));
                } else {
                    add(new Paragraph("❌ No user found in VaadinSession"));
                }
            } else {
                add(new Paragraph("❌ No VaadinSession available"));
            }
            
            // Check AuthenticationService
            H3 authServiceHeader = new H3("AuthenticationService Information:");
            add(authServiceHeader);
            
            if (authenticationService.isAuthenticated()) {
                User user = authenticationService.getCurrentUser();
                if (user != null) {
                    add(new Paragraph("✅ AuthenticationService: User authenticated"));
                    add(new Paragraph("   User: " + user.getFullName()));
                    add(new Paragraph("   Email: " + user.getEmail()));
                    add(new Paragraph("   Role: " + user.getRole().getName()));
                    add(new Paragraph("   Super Admin: " + user.isSuperAdmin()));
                    add(new Paragraph("   Can Manage Users: " + authenticationService.canManageUsers()));
                } else {
                    add(new Paragraph("⚠️ AuthenticationService says authenticated but user is null"));
                }
            } else {
                add(new Paragraph("❌ AuthenticationService: Not authenticated"));
            }
            
            // Check Spring Security Context
            H3 springSecurityHeader = new H3("Spring Security Context:");
            add(springSecurityHeader);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                add(new Paragraph("✅ Spring Security: Authenticated"));
                add(new Paragraph("   Principal Type: " + authentication.getPrincipal().getClass().getSimpleName()));
                
                if (authentication.getPrincipal() instanceof User) {
                    User user = (User) authentication.getPrincipal();
                    add(new Paragraph("   User: " + user.getFullName()));
                    add(new Paragraph("   Email: " + user.getEmail()));
                    add(new Paragraph("   Role: " + user.getRole().getName()));
                } else {
                    add(new Paragraph("   Principal: " + authentication.getPrincipal()));
                }
                
                add(new Paragraph("   Authorities: " + authentication.getAuthorities()));
                add(new Paragraph("   Is Authenticated: " + authentication.isAuthenticated()));
            } else {
                add(new Paragraph("❌ Spring Security: Not authenticated"));
                if (authentication != null) {
                    add(new Paragraph("   Authentication object exists but not authenticated"));
                    add(new Paragraph("   Principal: " + authentication.getPrincipal()));
                    add(new Paragraph("   Principal Type: " + authentication.getPrincipal().getClass().getSimpleName()));
                } else {
                    add(new Paragraph("   No Authentication object in SecurityContext"));
                }
            }
            
        } catch (Exception e) {
            add(new Paragraph("❌ Error during debug: " + e.getMessage()));
            e.printStackTrace();
        }
        
        add(new Paragraph("This debug view should be removed in production!"));
        
        System.out.println("DebugAuthView: Debug information displayed");
    }
}