package com.feedback.ui.views.test;

import com.feedback.service.AuthenticationService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;

@Route(value = "test-dashboard")
@PageTitle("Test Dashboard | Feedback System")
@PermitAll
public class TestDashboard extends VerticalLayout {

    private final AuthenticationService authenticationService;

    public TestDashboard(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        
        System.out.println("TestDashboard constructor called");

        add(new H1("Test Dashboard - No Layout"));
        
        try {
            // Check VaadinSession
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                add(new Paragraph("✅ VaadinSession exists"));
                
                Object sessionUser = session.getAttribute(AuthenticationService.CURRENT_USER_SESSION_ATTRIBUTE);
                if (sessionUser != null) {
                    add(new Paragraph("✅ User found in session: " + sessionUser.toString()));
                } else {
                    add(new Paragraph("❌ No user in session"));
                }
            } else {
                add(new Paragraph("❌ No VaadinSession"));
            }
            
            // Check authentication service
            if (authenticationService.isAuthenticated()) {
                var user = authenticationService.getCurrentUser();
                if (user != null) {
                    add(new Paragraph("✅ Authentication Service: Authenticated"));
                    add(new Paragraph("✅ User: " + user.getFullName()));
                    add(new Paragraph("Email: " + user.getEmail()));
                    add(new Paragraph("Role: " + user.getRole().getName()));
                    add(new Paragraph("Active: " + user.isActive()));
                    add(new Paragraph("Super Admin: " + user.isSuperAdmin()));
                } else {
                    add(new Paragraph("⚠️ Authenticated but user is null"));
                }
            } else {
                add(new Paragraph("❌ Authentication Service: Not authenticated"));
            }
            
        } catch (Exception e) {
            add(new Paragraph("❌ Error checking authentication: " + e.getMessage()));
            e.printStackTrace();
        }
        
        add(new Paragraph("This is a simple test page without MainLayout"));
        add(new Paragraph("If you see this page loading properly, the authentication system is working."));
        
        System.out.println("TestDashboard constructor completed successfully");
    }
}