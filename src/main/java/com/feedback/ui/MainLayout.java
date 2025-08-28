package com.feedback.ui;

import com.feedback.service.AuthenticationService;
import com.feedback.ui.profile.ProfileView;
import com.feedback.ui.views.actionitems.ActionItemView;
import com.feedback.ui.views.analytics.AnalyticsView;
import com.feedback.ui.views.appreciation.WallOfAppreciationView;
import com.feedback.ui.views.dashboard.DashboardView;
import com.feedback.ui.views.feedback.FeedbackFormView;
import com.feedback.ui.views.feedback.FeedbackListView;
import com.feedback.ui.views.templates.TemplateView;
import com.feedback.ui.views.users.UserView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    private final AuthenticationService authenticationService;
    
    public MainLayout(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Feedback System");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.MEDIUM
        );

        // User info and logout
        com.feedback.model.User currentUser = authenticationService.getCurrentUser();
        
        HorizontalLayout header;
        if (currentUser != null) {
            Span welcomeText = new Span("Welcome, " + currentUser.getFirstName());
            welcomeText.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
            
            Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
            logoutButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            logoutButton.addClickListener(e -> {
                authenticationService.logout();
                UI.getCurrent().navigate("login");
            });
            
            VerticalLayout userInfo = new VerticalLayout();
            userInfo.setSpacing(false);
            userInfo.setPadding(false);
            userInfo.add(welcomeText);
            
            HorizontalLayout userSection = new HorizontalLayout();
            userSection.setAlignItems(FlexComponent.Alignment.CENTER);
            userSection.add(userInfo, logoutButton);
            
            header = new HorizontalLayout(new DrawerToggle(), logo);
            header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setWidth("100%");
            header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);
            
            // Add user section to the right
            header.add(userSection);
        } else {
            header = new HorizontalLayout(new DrawerToggle(), logo);
            header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            header.setWidth("100%");
            header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);
        }

        addToNavbar(header);
    }

    private void createDrawer() {
        com.feedback.model.User currentUser = authenticationService.getCurrentUser();
        
        SideNav nav = new SideNav();
        
        if (currentUser != null) {
            // Main navigation items for all users
            nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
            nav.addItem(new SideNavItem("Give Feedback", FeedbackFormView.class, VaadinIcon.EDIT.create()));
            nav.addItem(new SideNavItem("My Feedback", FeedbackListView.class, VaadinIcon.LIST.create()));
            
            // NEW: Wall of Appreciation - visible to all authenticated users
            SideNavItem wallItem = new SideNavItem("Wall of Appreciation", WallOfAppreciationView.class, VaadinIcon.HEART.create());
            // Add some special styling to make it stand out
            wallItem.getElement().getStyle().set("--lumo-primary-color", "#e91e63");
            nav.addItem(wallItem);
            
            nav.addItem(new SideNavItem("Action Items", ActionItemView.class, VaadinIcon.TASKS.create()));
            nav.addItem(new SideNavItem("Analytics", AnalyticsView.class, VaadinIcon.CHART.create()));
            nav.addItem(new SideNavItem("My Profile", ProfileView.class, VaadinIcon.USER.create()));
            
            // Admin/Manager only sections
            String roleName = currentUser.getRole().getName();
            if ("SUPER_ADMIN".equals(roleName) || "ADMIN".equals(roleName) || "MANAGER".equals(roleName)) {
                // Add separator
                nav.addItem(new SideNavItem("", "", null)); // Spacer
                
                // Templates management
                nav.addItem(new SideNavItem("Templates", TemplateView.class, VaadinIcon.CLIPBOARD_TEXT.create()));
            }
            
            // Super Admin / Admin only
            if ("SUPER_ADMIN".equals(roleName) || "ADMIN".equals(roleName)) {
                nav.addItem(new SideNavItem("User Management", UserView.class, VaadinIcon.USERS.create()));
            }
        } else {
            // For non-authenticated users (shouldn't really happen due to security)
            nav.addItem(new SideNavItem("Login", "login", VaadinIcon.SIGN_IN.create()));
        }

        addToDrawer(nav);
    }
}