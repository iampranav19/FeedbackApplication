package com.feedback.ui.views.dashboard;

import com.feedback.model.Feedback;
import com.feedback.model.User;
import com.feedback.service.ActionItemService;
import com.feedback.service.FeedbackService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | Feedback System")
public class DashboardView extends VerticalLayout {

    private final FeedbackService feedbackService;
    private final UserService userService;
    private final ActionItemService actionItemService;
    private User currentUser; // In a real app, this would be the logged-in user

    public DashboardView(FeedbackService feedbackService, UserService userService, ActionItemService actionItemService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
        this.actionItemService = actionItemService;
        
        // For the demo, we'll use the admin user as the current user
        this.currentUser = userService.findUserById(1L).orElse(null);

        addClassName("dashboard-view");
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        
        add(new H2("Feedback Dashboard"));
        
        // Create a horizontal layout for stats instead of using the Board component
        HorizontalLayout statsLayout = new HorizontalLayout(
            createFeedbackStatCard("Total Feedback", feedbackService.findAllFeedback().size()),
            createFeedbackStatCard("Unread Feedback", 
                                   feedbackService.countUnreadFeedback(currentUser.getId())),
            createFeedbackStatCard("Pending Actions", 
                                  actionItemService.countActiveActionItems(currentUser.getId()))
        );
        statsLayout.setWidthFull();
        statsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        statsLayout.setSpacing(true);
        
        add(statsLayout);
        
        add(createStatusDistributionChart());
        
        // Add recent feedback section
        add(createRecentFeedbackSection());
        
        // Add action items summary
        add(createActionItemsSummary());
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
        layout.getStyle()
              .set("background-color", "var(--lumo-contrast-5pct)")
              .set("border-radius", "var(--lumo-border-radius-m)")
              .set("padding", "var(--lumo-space-m)")
              .set("box-shadow", "var(--lumo-box-shadow-xs)")
              .set("min-width", "200px");
        
        return layout;
    }
    
    private Component createStatusDistributionChart() {
        // Instead of using Chart component, create a visualization using standard components
        VerticalLayout chartLayout = new VerticalLayout();
        chartLayout.setWidth("100%");
        
        H3 title = new H3("Feedback Status Distribution");
        chartLayout.add(title);
        
        // For MVP, we'll just count statuses
        List<Feedback> allFeedback = feedbackService.findAllFeedback();
        Map<String, Integer> statusCounts = new HashMap<>();
        
        for (Feedback feedback : allFeedback) {
            statusCounts.put(feedback.getStatus(), 
                             statusCounts.getOrDefault(feedback.getStatus(), 0) + 1);
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
            block.setHeight("80px");  // Fixed height for simplicity
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
        chartLayout.getStyle()
                  .set("background-color", "var(--lumo-contrast-5pct)")
                  .set("border-radius", "var(--lumo-border-radius-m)")
                  .set("padding", "var(--lumo-space-m)")
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
        grid.addColumn(feedback -> feedback.getSender().getFullName()).setHeader("From").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getRecipient().getFullName()).setHeader("To").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getCategory()).setHeader("Category").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getStatus()).setHeader("Status").setAutoWidth(true);
        
        // Add action button
        grid.addComponentColumn(feedback -> {
            Button viewButton = new Button("View");
            viewButton.addClickListener(e -> {
                // In a real app, this would open the feedback dialog
                // For the MVP, we'll just navigate to the feedback list
                viewButton.getUI().ifPresent(ui -> 
                    ui.navigate("feedback-list")
                );
            });
            return viewButton;
        }).setHeader("Actions").setAutoWidth(true);
        
        // Get recent feedback (up to 5 items)
        List<Feedback> recentFeedback = feedbackService.findAllFeedback().stream()
                .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());
        
        grid.setItems(recentFeedback);
        
        // Add a "View All" button
        Button viewAllButton = new Button("View All Feedback", VaadinIcon.LIST.create());
        viewAllButton.addClickListener(e -> 
            viewAllButton.getUI().ifPresent(ui -> 
                ui.navigate("feedback-list")
            )
        );
        
        layout.add(title, grid, viewAllButton);
        
        // Add some styling
        layout.getStyle()
              .set("background-color", "var(--lumo-contrast-5pct)")
              .set("border-radius", "var(--lumo-border-radius-m)")
              .set("padding", "var(--lumo-space-m)")
              .set("box-shadow", "var(--lumo-box-shadow-xs)");
        
        return layout;
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
        summaryLayout.add(
            createActionTypeCard("Open", 8, "var(--lumo-primary-color)"),
            createActionTypeCard("In Progress", 5, "var(--lumo-success-color)"),
            createActionTypeCard("Completed", 3, "var(--lumo-contrast-color)"),
            createActionTypeCard("Overdue", 2, "var(--lumo-error-color)")
        );
        
        // Add a "View All" button
        Button viewAllButton = new Button("View All Action Items", VaadinIcon.TASKS.create());
        viewAllButton.addClickListener(e -> 
            viewAllButton.getUI().ifPresent(ui -> 
                ui.navigate("action-items")
            )
        );
        
        layout.add(title, summaryLayout, viewAllButton);
        
        // Add some styling
        layout.getStyle()
              .set("background-color", "var(--lumo-contrast-5pct)")
              .set("border-radius", "var(--lumo-border-radius-m)")
              .set("padding", "var(--lumo-space-m)")
              .set("box-shadow", "var(--lumo-box-shadow-xs)");
        
        return layout;
    }
    
    private Component createActionTypeCard(String type, int count, String color) {
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Span countSpan = new Span(String.valueOf(count));
        countSpan.getStyle()
                 .set("font-size", "2em")
                 .set("font-weight", "bold")
                 .set("color", color);
        
        Span typeSpan = new Span(type);
        
        layout.add(countSpan, typeSpan);
        
        // Add some styling
        layout.getStyle()
              .set("background-color", "var(--lumo-base-color)")
              .set("border-radius", "var(--lumo-border-radius-s)")
              .set("padding", "var(--lumo-space-s)")
              .set("border-left", "4px solid " + color);
        
        return layout;
    }
    
    private String formatDate(Feedback feedback) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return feedback.getCreatedAt().format(formatter);
    }
}