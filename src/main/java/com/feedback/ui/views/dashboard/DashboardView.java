package com.feedback.ui.views.dashboard;

import com.feedback.model.Feedback;
import com.feedback.model.PrivacyLevel;
import com.feedback.model.User;
import com.feedback.service.ActionItemService;
import com.feedback.service.AuthenticationService;
import com.feedback.service.FeedbackService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | Feedback System")
@PermitAll
public class DashboardView extends VerticalLayout {
	private final FeedbackService feedbackService;
	private final UserService userService;
	private final ActionItemService actionItemService;
	private final AuthenticationService authenticationService;
	private User currentUser;

	private boolean hasRedirected = false; // Prevent infinite redirects

	public DashboardView(FeedbackService feedbackService, UserService userService, ActionItemService actionItemService,
			AuthenticationService authenticationService) {
		this.feedbackService = feedbackService;
		this.userService = userService;
		this.actionItemService = actionItemService;
		this.authenticationService = authenticationService;

		System.out.println("DashboardView: Constructor started");

		// FIXED: More defensive authentication check
		this.currentUser = authenticationService.getCurrentUser();

		if (this.currentUser == null) {
			System.err.println("DashboardView: Current user is null - authentication issue detected");

			// Instead of immediately redirecting, show error message and provide login link
			if (!hasRedirected) {
				hasRedirected = true;

				// Show error message with login option
				VerticalLayout errorLayout = new VerticalLayout();
				errorLayout.setAlignItems(FlexComponent.Alignment.CENTER);
				errorLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
				errorLayout.setSizeFull();

				H2 errorTitle = new H2("Authentication Required");
				Span errorMessage = new Span("Please log in to access the dashboard.");

				Button loginButton = new Button("Go to Login");
				loginButton.addClickListener(e -> {
					try {
						// Clear any existing session data
						authenticationService.logout();
						UI.getCurrent().navigate("login");
					} catch (Exception ex) {
						System.err.println("Error navigating to login: " + ex.getMessage());
						UI.getCurrent().getPage().reload();
					}
				});

				errorLayout.add(errorTitle, errorMessage, loginButton);
				add(errorLayout);
				return;
			} else {
				// Already tried to redirect, show minimal error
				add(new Span("Authentication error. Please refresh the page."));
				return;
			}
		}

		System.out.println("DashboardView loaded for user: " + currentUser.getFullName());

		addClassName("dashboard-view");
		setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

		add(new H2("Feedback Dashboard"));

		// Create dashboard content
		createDashboardContent();

		System.out.println("DashboardView: Successfully initialized");
	}

	private void createDashboardContent() {
		try {
			// Create stats layout - FIXED: Use privacy-aware count for total feedback
			HorizontalLayout statsLayout = new HorizontalLayout(
					createFeedbackStatCard("Visible Feedback", feedbackService.findVisibleFeedbackForUser(currentUser.getId()).size()),
					createFeedbackStatCard("Unread Feedback", feedbackService.countUnreadFeedback(currentUser.getId())),
					createFeedbackStatCard("Pending Actions",
							actionItemService.countActiveActionItems(currentUser.getId())));
			statsLayout.setWidthFull();
			statsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
			statsLayout.setSpacing(true);

			add(statsLayout);
			add(createStatusDistributionChart());
			add(createRecentFeedbackSection());
			add(createActionItemsSummary());

		} catch (Exception e) {
			// Better error handling
			System.err.println("Error creating dashboard content: " + e.getMessage());
			e.printStackTrace();

			// Show user-friendly error message
			VerticalLayout errorLayout = new VerticalLayout();
			errorLayout.setAlignItems(FlexComponent.Alignment.CENTER);

			Span errorMessage = new Span("Error loading dashboard content. Please try refreshing the page.");
			Button refreshButton = new Button("Refresh Page");
			refreshButton.addClickListener(e2 -> UI.getCurrent().getPage().reload());

			errorLayout.add(errorMessage, refreshButton);
			add(errorLayout);
		}
	}

	private Component createFeedbackStatCard(String title, long value) {
		VerticalLayout layout = new VerticalLayout();
		layout.addClassName("feedback-stat-card");
		layout.setAlignItems(FlexComponent.Alignment.CENTER);

		H3 titleComponent = new H3(title);
		Span valueComponent = new Span(String.valueOf(value));
		valueComponent.getStyle().set("font-size", "2.5em");

		layout.add(titleComponent, valueComponent);

		// Add a visual indicator
		Icon icon;
		if (title.contains("Unread")) {
			icon = VaadinIcon.ENVELOPE.create();
			icon.setColor("var(--lumo-primary-color)");
		} else if (title.contains("Actions")) {
			icon = VaadinIcon.TASKS.create();
			icon.setColor("var(--lumo-error-color)");
		} else {
			icon = VaadinIcon.CHART.create();
			icon.setColor("var(--lumo-success-color)");
		}

		layout.addComponentAsFirst(icon);

		// Add some styling
		layout.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
				.set("border-radius", "var(--lumo-border-radius-m)").set("padding", "var(--lumo-space-m)")
				.set("box-shadow", "var(--lumo-box-shadow-xs)").set("min-width", "200px");

		return layout;
	}

	private Component createStatusDistributionChart() {
		// FIXED: Use privacy-aware feedback instead of all feedback
		VerticalLayout chartLayout = new VerticalLayout();
		chartLayout.setWidth("100%");

		H3 title = new H3("Feedback Status Distribution");
		chartLayout.add(title);

		// Get only feedback that the current user is authorized to see
		List<Feedback> visibleFeedback = feedbackService.findVisibleFeedbackForUser(currentUser.getId());
		Map<String, Integer> statusCounts = new HashMap<>();

		for (Feedback feedback : visibleFeedback) {
			statusCounts.put(feedback.getStatus(), statusCounts.getOrDefault(feedback.getStatus(), 0) + 1);
		}

		// If we don't have any feedback yet, add some default statuses
		if (statusCounts.isEmpty()) {
			statusCounts.put("Open", 5);
			statusCounts.put("Acknowledged", 3);
			statusCounts.put("In Progress", 2);
			statusCounts.put("Completed", 1);
		}

		// Calculate total for percentages
		int total = statusCounts.values().stream().mapToInt(Integer::intValue).sum();

		// Define colors for different statuses
		Map<String, String> statusColors = new HashMap<>();
		statusColors.put("Open", "var(--lumo-primary-color)");
		statusColors.put("Acknowledged", "var(--lumo-success-color)");
		statusColors.put("In Progress", "var(--lumo-contrast-color)");
		statusColors.put("Completed", "var(--lumo-tertiary-color)");

		// Create horizontal layout for the visualization
		HorizontalLayout statusLayout = new HorizontalLayout();
		statusLayout.setWidthFull();
		statusLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

		// Create a proportional bar for each status
		for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
			String status = entry.getKey();
			int count = entry.getValue();
			double percentage = total > 0 ? (count * 100.0 / total) : 0;

			// Create a vertical layout for each status
			VerticalLayout statusBlock = new VerticalLayout();
			statusBlock.setSpacing(false);
			statusBlock.setPadding(false);
			statusBlock.setAlignItems(FlexComponent.Alignment.CENTER);

			// Create a colored block proportional to the count
			Div block = new Div();
			block.setWidth("80px");
			block.setHeight("80px"); // Fixed height for simplicity
			block.getStyle().set("background-color", statusColors.getOrDefault(status, "var(--lumo-primary-color)"));
			block.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
			block.getStyle().set("display", "flex");
			block.getStyle().set("align-items", "center");
			block.getStyle().set("justify-content", "center");

			// Add count in the middle of the block
			Span countSpan = new Span(String.valueOf(count));
			countSpan.getStyle().set("color", "white");
			countSpan.getStyle().set("font-weight", "bold");
			countSpan.getStyle().set("font-size", "1.5em");
			block.add(countSpan);

			// Add status name and percentage
			Span statusName = new Span(status);
			Span percentSpan = new Span(String.format("%.1f%%", percentage));

			statusBlock.add(block, statusName, percentSpan);
			statusLayout.add(statusBlock);
		}

		chartLayout.add(statusLayout);

		// Add some styling
		chartLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
				.set("border-radius", "var(--lumo-border-radius-m)").set("padding", "var(--lumo-space-m)")
				.set("box-shadow", "var(--lumo-box-shadow-xs)");

		return chartLayout;
	}

	private Component createRecentFeedbackSection() {
		VerticalLayout layout = new VerticalLayout();
		layout.setWidth("100%");

		H3 title = new H3("Recent Feedback");

		// Create a grid for recent feedback
		Grid<Feedback> grid = new Grid<>();
		grid.setHeight("250px");

		// Add columns to the grid
		grid.addColumn(feedback -> formatDate(feedback)).setHeader("Date").setAutoWidth(true);

		// FIXED: Handle privacy-aware sender display
		grid.addColumn(feedback -> getSenderDisplayName(feedback)).setHeader("From").setAutoWidth(true);

		grid.addColumn(feedback -> getRecipientDisplayName(feedback)).setHeader("To").setAutoWidth(true);
		grid.addColumn(feedback -> feedback.getCategory()).setHeader("Category").setAutoWidth(true);
		grid.addColumn(feedback -> feedback.getStatus()).setHeader("Status").setAutoWidth(true);

		// Add action button
		grid.addComponentColumn(feedback -> {
			Button viewButton = new Button("View");
			viewButton.addClickListener(e -> {
				// In a real app, this would open the feedback dialog
				// For the MVP, we'll just navigate to the feedback list
				viewButton.getUI().ifPresent(ui -> ui.navigate("feedback-list"));
			});
			return viewButton;
		}).setHeader("Actions").setAutoWidth(true);

		// FIXED: Get only recent feedback that the current user is authorized to see
		List<Feedback> recentFeedback = feedbackService.findVisibleFeedbackForUser(currentUser.getId()).stream()
				.sorted(Comparator.comparing(Feedback::getCreatedAt).reversed()).limit(5).collect(Collectors.toList());

		grid.setItems(recentFeedback);

		// Add a "View All" button
		Button viewAllButton = new Button("View All Feedback", VaadinIcon.LIST.create());
		viewAllButton.addClickListener(e -> viewAllButton.getUI().ifPresent(ui -> ui.navigate("feedback-list")));

		layout.add(title, grid, viewAllButton);

		// Add some styling
		layout.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
				.set("border-radius", "var(--lumo-border-radius-m)").set("padding", "var(--lumo-space-m)")
				.set("box-shadow", "var(--lumo-box-shadow-xs)");

		return layout;
	}

	/**
	 * FIXED: Helper method to get the appropriate sender display name based on privacy
	 * level and current user's relationship to the feedback
	 */
	private String getSenderDisplayName(Feedback feedback) {
		// If the feedback is anonymous, hide the sender's name
		if (feedback.getPrivacyLevel() == PrivacyLevel.ANONYMOUS) {
			return "Anonymous";
		}

		// For private feedback, only show sender name if current user is involved
		if (feedback.getPrivacyLevel() == PrivacyLevel.PRIVATE) {
			boolean isCurrentUserInvolved = feedback.getSender().getId().equals(currentUser.getId()) || 
			                               feedback.getRecipient().getId().equals(currentUser.getId());
			if (!isCurrentUserInvolved) {
				// This should not happen since findVisibleFeedbackForUser should filter this out,
				// but adding as additional safety
				return "[Private]";
			}
		}

		// For non-anonymous feedback that the user is authorized to see, show the sender's name
		return feedback.getSender().getFullName();
	}

	/**
	 * FIXED: Helper method to get the appropriate recipient display name based on privacy
	 * level and current user's relationship to the feedback
	 */
	private String getRecipientDisplayName(Feedback feedback) {
		// For private feedback, only show recipient name if current user is involved
		if (feedback.getPrivacyLevel() == PrivacyLevel.PRIVATE) {
			boolean isCurrentUserInvolved = feedback.getSender().getId().equals(currentUser.getId()) || 
			                               feedback.getRecipient().getId().equals(currentUser.getId());
			if (!isCurrentUserInvolved) {
				// This should not happen since findVisibleFeedbackForUser should filter this out,
				// but adding as additional safety
				return "[Private]";
			}
		}

		// For feedback that the user is authorized to see, show the recipient's name
		return feedback.getRecipient().getFullName();
	}

	private Component createActionItemsSummary() {
		VerticalLayout layout = new VerticalLayout();
		layout.setWidth("100%");

		H3 title = new H3("Action Items Summary");

		// Create a simple summary view
		HorizontalLayout summaryLayout = new HorizontalLayout();
		summaryLayout.setWidthFull();
		summaryLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

		// Add some mock data for the MVP
		summaryLayout.add(createActionTypeCard("Open", 8, "var(--lumo-primary-color)"),
				createActionTypeCard("In Progress", 5, "var(--lumo-success-color)"),
				createActionTypeCard("Completed", 3, "var(--lumo-contrast-color)"),
				createActionTypeCard("Overdue", 2, "var(--lumo-error-color)"));

		// Add a "View All" button
		Button viewAllButton = new Button("View All Action Items", VaadinIcon.TASKS.create());
		viewAllButton.addClickListener(e -> viewAllButton.getUI().ifPresent(ui -> ui.navigate("action-items")));

		layout.add(title, summaryLayout, viewAllButton);

		// Add some styling
		layout.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
				.set("border-radius", "var(--lumo-border-radius-m)").set("padding", "var(--lumo-space-m)")
				.set("box-shadow", "var(--lumo-box-shadow-xs)");

		return layout;
	}

	private Component createActionTypeCard(String type, int count, String color) {
		VerticalLayout layout = new VerticalLayout();
		layout.setAlignItems(FlexComponent.Alignment.CENTER);

		Span countSpan = new Span(String.valueOf(count));
		countSpan.getStyle().set("font-size", "2em").set("font-weight", "bold").set("color", color);

		Span typeSpan = new Span(type);

		layout.add(countSpan, typeSpan);

		// Add some styling
		layout.getStyle().set("background-color", "var(--lumo-base-color)")
				.set("border-radius", "var(--lumo-border-radius-s)").set("padding", "var(--lumo-space-s)")
				.set("border-left", "4px solid " + color);

		return layout;
	}

	private String formatDate(Feedback feedback) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		return feedback.getCreatedAt().format(formatter);
	}
}