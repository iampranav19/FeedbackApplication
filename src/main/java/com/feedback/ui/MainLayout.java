package com.feedback.ui;

import com.feedback.model.User;
import com.feedback.service.AuthenticationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

	private final AuthenticationService authenticationService;

	public MainLayout(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;

		System.out.println("MainLayout: Constructor started");
		
		// Spring Security ensures authentication for protected routes
		createHeader();
		createDrawer();
		
		System.out.println("MainLayout: Successfully created layout");
	}

	private void createHeader() {
		H1 logo = new H1("Feedback System");
		logo.addClassNames("text-l", "m-m");

		HorizontalLayout header = new HorizontalLayout();
		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.expand(logo);
		header.setWidthFull();
		header.addClassNames("py-0", "px-m");

		// Get current user for header display
		User currentUser = authenticationService.getCurrentUser();
		if (currentUser != null) {
			String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "User";
			Span userInfo = new Span("Welcome, " + firstName);
			userInfo.getStyle().set("margin-right", "var(--lumo-space-m)");

			Button logoutButton = new Button("Logout", new Icon(VaadinIcon.SIGN_OUT));
			logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
			logoutButton.addClickListener(e -> logout());

			header.add(logo, userInfo, logoutButton);
			
			System.out.println("MainLayout: Header created for user: " + currentUser.getFullName());
		} else {
			header.add(logo);
			System.out.println("MainLayout: Header created without user info");
		}

		addToNavbar(new DrawerToggle(), header);
	}

	private void logout() {
		try {
			System.out.println("MainLayout: Logout initiated");
			authenticationService.logout();
			// Use proper Vaadin navigation instead of direct page manipulation
			UI.getCurrent().navigate("login");
			System.out.println("MainLayout: Logout completed, navigating to login");
		} catch (Exception ex) {
			System.err.println("MainLayout: Logout error: " + ex.getMessage());
			ex.printStackTrace();
			// Fallback: reload page
			UI.getCurrent().getPage().reload();
		}
	}

	private void createDrawer() {
		User currentUser = authenticationService.getCurrentUser();
		if (currentUser == null) {
			System.out.println("MainLayout: No current user, skipping drawer creation");
			return; // Don't create drawer for unauthenticated users
		}

		System.out.println("MainLayout: Creating drawer for user: " + currentUser.getFullName());

		VerticalLayout navigation = new VerticalLayout();
		navigation.setSizeFull();
		navigation.setPadding(false);
		navigation.setSpacing(false);

		// Core navigation items - these should be accessible to all authenticated users
		addNavigationItem(navigation, "Dashboard", VaadinIcon.DASHBOARD, "");
		addNavigationItem(navigation, "Give Feedback", VaadinIcon.EDIT, "feedback-form");
		addNavigationItem(navigation, "View Feedback", VaadinIcon.LIST, "feedback-list");
		addNavigationItem(navigation, "Action Items", VaadinIcon.TASKS, "action-items");
		addNavigationItem(navigation, "Analytics", VaadinIcon.CHART, "analytics");
		addNavigationItem(navigation, "Profile", VaadinIcon.USER, "profile");

		// Admin section - only show for privileged users
		if (currentUser.isSuperAdmin() || "ADMIN".equals(currentUser.getRole().getName()) || "MANAGER".equals(currentUser.getRole().getName())) {
			navigation.add(new Hr());

			Span adminLabel = new Span("Administration");
			adminLabel.addClassNames("block", "font-medium", "text-s", "text-secondary", "px-m", "py-s");
			adminLabel.getStyle()
					.set("color", "var(--lumo-secondary-text-color)")
					.set("font-weight", "bold")
					.set("text-transform", "uppercase")
					.set("font-size", "var(--lumo-font-size-xs)");
			navigation.add(adminLabel);

			if (currentUser.isSuperAdmin() || "ADMIN".equals(currentUser.getRole().getName())) {
				addNavigationItem(navigation, "Users", VaadinIcon.USERS, "users");
			}
			
			if (currentUser.isSuperAdmin() || "ADMIN".equals(currentUser.getRole().getName()) || "MANAGER".equals(currentUser.getRole().getName())) {
				addNavigationItem(navigation, "Templates", VaadinIcon.FILE_TEXT, "templates");
			}
			
			System.out.println("MainLayout: Added admin navigation items for user role: " + currentUser.getRole().getName());
		}

		addToDrawer(navigation);
		System.out.println("MainLayout: Drawer created successfully");
	}

	private void addNavigationItem(VerticalLayout navigation, String text, VaadinIcon icon, String route) {
		HorizontalLayout item = new HorizontalLayout();
		item.setSpacing(true);
		item.setPadding(false);
		item.setAlignItems(FlexComponent.Alignment.CENTER);
		item.getStyle()
			.set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
			.set("cursor", "pointer")
			.set("border-radius", "var(--lumo-border-radius-m)")
			.set("color", "var(--lumo-body-text-color)")
			.set("text-decoration", "none");

		// Add hover effect
		item.getElement().addEventListener("mouseover", e -> {
			item.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
		});
		item.getElement().addEventListener("mouseout", e -> {
			item.getStyle().remove("background-color");
		});

		// Add click handler
		item.addClickListener(e -> {
			try {
				System.out.println("MainLayout: Navigating to route: " + route);
				UI.getCurrent().navigate(route);
			} catch (Exception ex) {
				System.err.println("MainLayout: Navigation error to " + route + ": " + ex.getMessage());
				ex.printStackTrace();
			}
		});

		Icon itemIcon = icon.create();
		itemIcon.getStyle().set("color", "var(--lumo-contrast-60pct)");
		
		Span label = new Span(text);
		
		item.add(itemIcon, label);
		navigation.add(item);
	}
}