package com.feedback.ui.views.analytics;

import com.feedback.model.Feedback;
import com.feedback.model.User;
import com.feedback.service.ActionItemService;
import com.feedback.service.FeedbackService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "analytics", layout = MainLayout.class)
@PageTitle("Analytics | Feedback System")
public class AnalyticsView extends VerticalLayout {

    private final FeedbackService feedbackService;
    private final UserService userService;
    private final ActionItemService actionItemService;
    
    private final VerticalLayout chartsLayout = new VerticalLayout();
    private Tab feedbackVolumeTab;
    private Tab feedbackByDepartmentTab;
    private Tab feedbackByStatusTab;
    private Tab actionItemsTab;

    public AnalyticsView(FeedbackService feedbackService, 
                         UserService userService,
                         ActionItemService actionItemService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
        this.actionItemService = actionItemService;
        
        addClassName("analytics-view");
        setSizeFull();
        
        Tabs tabs = createTabs();
        
        add(
                new H2("Feedback Analytics"),
                tabs,
                createFilters(),
                chartsLayout
        );
        
        // Default to showing feedback volume
        showFeedbackVolumeChart();
    }
    
    private Tabs createTabs() {
        feedbackVolumeTab = new Tab("Feedback Volume");
        feedbackByDepartmentTab = new Tab("Feedback by Department");
        feedbackByStatusTab = new Tab("Feedback by Status");
        actionItemsTab = new Tab("Action Items");
        
        Tabs tabs = new Tabs(feedbackVolumeTab, feedbackByDepartmentTab, feedbackByStatusTab, actionItemsTab);
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(feedbackVolumeTab)) {
                showFeedbackVolumeChart();
            } else if (event.getSelectedTab().equals(feedbackByDepartmentTab)) {
                showFeedbackByDepartmentChart();
            } else if (event.getSelectedTab().equals(feedbackByStatusTab)) {
                showFeedbackByStatusChart();
            } else if (event.getSelectedTab().equals(actionItemsTab)) {
                showActionItemsChart();
            }
        });
        
        return tabs;
    }
    
    private HorizontalLayout createFilters() {
        ComboBox<String> timeRangeFilter = new ComboBox<>("Time Range");
        timeRangeFilter.setItems("Last 7 Days", "Last 30 Days", "Last 90 Days", "All Time");
        timeRangeFilter.setValue("All Time");
        
        ComboBox<String> typeFilter = new ComboBox<>("Feedback Type");
        typeFilter.setItems("All Types", "Performance", "Leadership", "Communication", "Teamwork", "Technical Skills", "Other");
        typeFilter.setValue("All Types");
        
        HorizontalLayout filters = new HorizontalLayout(timeRangeFilter, typeFilter);
        filters.setSpacing(true);
        
        return filters;
    }
    
    private void showFeedbackVolumeChart() {
        chartsLayout.removeAll();
        
        // Get all feedback
        List<Feedback> allFeedback = feedbackService.findAllFeedback();
        
        // Group feedback by month
        Map<Month, Long> feedbackByMonth = allFeedback.stream()
                .filter(f -> f.getCreatedAt().getYear() == LocalDate.now().getYear())
                .collect(Collectors.groupingBy(
                        f -> f.getCreatedAt().getMonth(),
                        Collectors.counting()
                ));
        
        // Create a visualization using free components
        VerticalLayout volumeLayout = new VerticalLayout();
        volumeLayout.setWidth("100%");
        volumeLayout.add(new H3("Feedback Volume by Month"));
        
        // Create month bars
        HorizontalLayout monthBars = new HorizontalLayout();
        monthBars.setWidthFull();
        monthBars.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        monthBars.getStyle().set("padding", "var(--lumo-space-m)");
        
        // Find maximum value for scaling
        long maxCount = feedbackByMonth.values().stream().mapToLong(Long::longValue).max().orElse(10);
        
        // Add a visualization for each month
        for (Month month : Month.values()) {
            long count = feedbackByMonth.getOrDefault(month, 0L);
            VerticalLayout monthLayout = new VerticalLayout();
            monthLayout.setSpacing(false);
            monthLayout.setPadding(false);
            monthLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            
            // Display the count
            Span countLabel = new Span(String.valueOf(count));
            
            // Create a visual representation using a div with height proportional to count
            Div bar = new Div();
            bar.setWidth("30px");
            
            // Calculate height as percentage of max (minimum 10px)
            int heightPx = count > 0 ? (int)(200 * count / maxCount) : 10;
            bar.setHeight(heightPx + "px");
            bar.getStyle().set("background-color", "var(--lumo-primary-color)");
            
            // Add month label
            Span monthLabel = new Span(month.toString().substring(0, 3));
            
            monthLayout.add(countLabel, bar, monthLabel);
            monthBars.add(monthLayout);
        }
        
        volumeLayout.add(monthBars);
        
        // Create category visualization
        VerticalLayout categoryLayout = new VerticalLayout();
        categoryLayout.setWidth("100%");
        categoryLayout.add(new H3("Feedback by Category"));
        
        // Group feedback by category
        Map<String, Long> feedbackByCategory = allFeedback.stream()
                .collect(Collectors.groupingBy(
                        Feedback::getCategory,
                        Collectors.counting()
                ));
        
        // Find total for percentage calculation
        long total = feedbackByCategory.values().stream().mapToLong(Long::longValue).sum();
        
        // Create a grid or bar visualization for categories
        for (Map.Entry<String, Long> entry : feedbackByCategory.entrySet()) {
            HorizontalLayout categoryRow = new HorizontalLayout();
            categoryRow.setWidthFull();
            
            String category = entry.getKey();
            long count = entry.getValue();
            double percentage = total > 0 ? (count * 100.0 / total) : 0;
            
            // Category label
            Span categoryLabel = new Span(category);
            categoryLabel.setWidth("150px");
            
            // Progress bar to represent percentage
            ProgressBar progressBar = new ProgressBar();
            progressBar.setValue(percentage / 100);
            progressBar.setWidth("60%");
            
            // Percentage label
            Span percentLabel = new Span(String.format("%.1f%% (%d)", percentage, count));
            
            categoryRow.add(categoryLabel, progressBar, percentLabel);
            categoryLayout.add(categoryRow);
        }
        
        // Add all layouts to the main layout
        chartsLayout.add(volumeLayout, categoryLayout);
    }
    
    private void showFeedbackByDepartmentChart() {
        chartsLayout.removeAll();
        
        VerticalLayout departmentLayout = new VerticalLayout();
        departmentLayout.setWidth("100%");
        departmentLayout.add(new H3("Feedback by Department"));
        
        // Get all feedback
        List<Feedback> allFeedback = feedbackService.findAllFeedback();
        
        // Group feedback by recipient's department
        Map<String, Long> feedbackByDepartment = allFeedback.stream()
                .filter(f -> f.getRecipient() != null && f.getRecipient().getDepartment() != null)
                .collect(Collectors.groupingBy(
                        f -> f.getRecipient().getDepartment().getName(),
                        Collectors.counting()
                ));
        
        // Find maximum value for scaling
        long maxCount = feedbackByDepartment.values().stream().mapToLong(Long::longValue).max().orElse(10);
        
        // Create a bar visualization for departments
        Grid<Map.Entry<String, Long>> grid = new Grid<>();
        grid.setItems(feedbackByDepartment.entrySet());
        
        grid.addColumn(Map.Entry::getKey).setHeader("Department").setAutoWidth(true);
        grid.addColumn(Map.Entry::getValue).setHeader("Count").setAutoWidth(true);
        
        grid.addComponentColumn(entry -> {
            long count = entry.getValue();
            double ratio = (double) count / maxCount;
            
            // Create progress bar to visualize the count
            ProgressBar bar = new ProgressBar();
            bar.setValue(ratio);
            bar.setWidth("200px");
            
            return bar;
        }).setHeader("Distribution");
        
        departmentLayout.add(grid);
        
        // Add visualization for feedback given by department
        VerticalLayout givenByDeptLayout = new VerticalLayout();
        givenByDeptLayout.setWidth("100%");
        givenByDeptLayout.add(new H3("Feedback Given by Department"));
        
        // Group feedback by sender's department
        Map<String, Long> feedbackGivenByDepartment = allFeedback.stream()
                .filter(f -> f.getSender() != null && f.getSender().getDepartment() != null)
                .collect(Collectors.groupingBy(
                        f -> f.getSender().getDepartment().getName(),
                        Collectors.counting()
                ));
        
        long totalGiven = feedbackGivenByDepartment.values().stream().mapToLong(Long::longValue).sum();
        
        // Create a horizontal bar for each department
        for (Map.Entry<String, Long> entry : feedbackGivenByDepartment.entrySet()) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            
            String dept = entry.getKey();
            long count = entry.getValue();
            double percentage = totalGiven > 0 ? (count * 100.0 / totalGiven) : 0;
            
            // Department label
            Span deptLabel = new Span(dept);
            deptLabel.setWidth("150px");
            
            // Progress bar
            ProgressBar progressBar = new ProgressBar();
            progressBar.setValue(percentage / 100);
            progressBar.setWidth("60%");
            
            // Percentage and count
            Span countLabel = new Span(String.format("%.1f%% (%d)", percentage, count));
            
            row.add(deptLabel, progressBar, countLabel);
            givenByDeptLayout.add(row);
        }
        
        chartsLayout.add(departmentLayout, givenByDeptLayout);
    }
    
    private void showFeedbackByStatusChart() {
        chartsLayout.removeAll();
        
        VerticalLayout statusLayout = new VerticalLayout();
        statusLayout.setWidth("100%");
        statusLayout.add(new H3("Feedback by Status"));
        
        // Get all feedback
        List<Feedback> allFeedback = feedbackService.findAllFeedback();
        
        // Group feedback by status
        Map<String, Long> feedbackByStatus = allFeedback.stream()
                .collect(Collectors.groupingBy(
                        Feedback::getStatus,
                        Collectors.counting()
                ));
        
        long total = feedbackByStatus.values().stream().mapToLong(Long::longValue).sum();
        
        // Create status visualization
        HorizontalLayout statusBars = new HorizontalLayout();
        statusBars.setWidthFull();
        statusBars.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        
        // Define colors for different statuses
        Map<String, String> statusColors = new HashMap<>();
        statusColors.put("Open", "var(--lumo-primary-color)");
        statusColors.put("Acknowledged", "var(--lumo-success-color)");
        statusColors.put("In Progress", "var(--lumo-contrast-color)");
        statusColors.put("Completed", "var(--lumo-tertiary-color)");
        statusColors.put("Closed", "var(--lumo-secondary-color)");
        
        // Create a colored box for each status
        for (Map.Entry<String, Long> entry : feedbackByStatus.entrySet()) {
            String status = entry.getKey();
            long count = entry.getValue();
            double percentage = total > 0 ? (count * 100.0 / total) : 0;
            
            VerticalLayout statusBox = new VerticalLayout();
            statusBox.setSpacing(false);
            statusBox.setPadding(false);
            statusBox.setAlignItems(FlexComponent.Alignment.CENTER);
            
            // Create a colored box
            Div box = new Div();
            box.setWidth("100px");
            box.setHeight("100px");
            box.getStyle().set("background-color", statusColors.getOrDefault(status, "var(--lumo-primary-color)"));
            box.getStyle().set("display", "flex");
            box.getStyle().set("align-items", "center");
            box.getStyle().set("justify-content", "center");
            box.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            
            // Add count in the middle of the box
            Span countLabel = new Span(String.valueOf(count));
            countLabel.getStyle().set("color", "white");
            countLabel.getStyle().set("font-size", "1.5em");
            countLabel.getStyle().set("font-weight", "bold");
            box.add(countLabel);
            
            // Add status and percentage below
            Span statusLabel = new Span(status);
            Span percentLabel = new Span(String.format("%.1f%%", percentage));
            
            statusBox.add(box, statusLabel, percentLabel);
            statusBars.add(statusBox);
        }
        
        statusLayout.add(statusBars);
        
        // Add response time visualization
        VerticalLayout responseTimeLayout = new VerticalLayout();
        responseTimeLayout.setWidth("100%");
        responseTimeLayout.add(new H3("Average Response Time by Department"));
        
        // Create a bar chart for response times (mock data)
        Grid<Map.Entry<String, Double>> responseGrid = new Grid<>();
        
        // Mock data
        Map<String, Double> responseTimes = new HashMap<>();
        responseTimes.put("IT", 2.1);
        responseTimes.put("HR", 1.5);
        responseTimes.put("Sales", 3.2);
        
        responseGrid.setItems(responseTimes.entrySet());
        responseGrid.addColumn(Map.Entry::getKey).setHeader("Department");
        responseGrid.addColumn(entry -> String.format("%.1f days", entry.getValue())).setHeader("Response Time");
        
        responseGrid.addComponentColumn(entry -> {
            double time = entry.getValue();
            // Scale to a reasonable value (assuming max is around 5 days)
            double ratio = time / 5.0;
            
            ProgressBar bar = new ProgressBar();
            bar.setValue(ratio);
            bar.setWidth("200px");
            
            return bar;
        }).setHeader("Visualization");
        
        responseTimeLayout.add(responseGrid);
        
        chartsLayout.add(statusLayout, responseTimeLayout);
    }
    
    private void showActionItemsChart() {
        chartsLayout.removeAll();
        
        VerticalLayout actionItemsLayout = new VerticalLayout();
        actionItemsLayout.setWidth("100%");
        actionItemsLayout.add(new H3("Action Items by Status"));
        
        // Mock data - in a real implementation we would use actionItemService
        Map<String, Integer> actionItemsByStatus = new HashMap<>();
        actionItemsByStatus.put("Open", 10);
        actionItemsByStatus.put("In Progress", 7);
        actionItemsByStatus.put("Completed", 5);
        actionItemsByStatus.put("Cancelled", 2);
        
        int totalItems = actionItemsByStatus.values().stream().mapToInt(Integer::intValue).sum();
        
        // Create a visualization for action items by status
        for (Map.Entry<String, Integer> entry : actionItemsByStatus.entrySet()) {
            String status = entry.getKey();
            int count = entry.getValue();
            double percentage = totalItems > 0 ? (count * 100.0 / totalItems) : 0;
            
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            
            // Status label
            Span statusLabel = new Span(status);
            statusLabel.setWidth("150px");
            
            // Progress bar
            ProgressBar progressBar = new ProgressBar();
            progressBar.setValue(percentage / 100);
            progressBar.setWidth("60%");
            
            // Count and percentage
            Span countLabel = new Span(String.format("%d (%.1f%%)", count, percentage));
            
            row.add(statusLabel, progressBar, countLabel);
            actionItemsLayout.add(row);
        }
        
        // Add visualization for action items by priority
        VerticalLayout priorityLayout = new VerticalLayout();
        priorityLayout.setWidth("100%");
        priorityLayout.add(new H3("Action Items by Priority"));
        
        // Mock data
        Map<String, Integer> actionItemsByPriority = new HashMap<>();
        actionItemsByPriority.put("High", 8);
        actionItemsByPriority.put("Medium", 12);
        actionItemsByPriority.put("Low", 4);
        
        // Define colors for priorities
        Map<String, String> priorityColors = new HashMap<>();
        priorityColors.put("High", "var(--lumo-error-color)");
        priorityColors.put("Medium", "var(--lumo-primary-color)");
        priorityColors.put("Low", "var(--lumo-success-color)");
        
        HorizontalLayout priorityBars = new HorizontalLayout();
        priorityBars.setWidthFull();
        priorityBars.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        
        // Create visualization for each priority
        for (Map.Entry<String, Integer> entry : actionItemsByPriority.entrySet()) {
            String priority = entry.getKey();
            int count = entry.getValue();
            
            VerticalLayout box = new VerticalLayout();
            box.setSpacing(false);
            box.setPadding(false);
            box.setAlignItems(FlexComponent.Alignment.CENTER);
            
            // Create box with count
            Div countBox = new Div();
            countBox.setWidth("80px");
            countBox.setHeight("80px");
            countBox.getStyle().set("background-color", priorityColors.getOrDefault(priority, "var(--lumo-primary-color)"));
            countBox.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            countBox.getStyle().set("display", "flex");
            countBox.getStyle().set("align-items", "center");
            countBox.getStyle().set("justify-content", "center");
            
            Span countLabel = new Span(String.valueOf(count));
            countLabel.getStyle().set("color", "white");
            countLabel.getStyle().set("font-size", "1.5em");
            countLabel.getStyle().set("font-weight", "bold");
            countBox.add(countLabel);
            
            Span priorityLabel = new Span(priority);
            
            box.add(countBox, priorityLabel);
            priorityBars.add(box);
        }
        
        priorityLayout.add(priorityBars);
        
        // Add department visualization
        VerticalLayout deptLayout = new VerticalLayout();
        deptLayout.setWidth("100%");
        deptLayout.add(new H3("Action Items by Department"));
        
        // Mock data
        Map<String, Integer> actionItemsByDepartment = new HashMap<>();
        actionItemsByDepartment.put("IT", 12);
        actionItemsByDepartment.put("HR", 8);
        actionItemsByDepartment.put("Sales", 4);
        
        // Find maximum for scaling
        int maxDeptCount = actionItemsByDepartment.values().stream().mapToInt(Integer::intValue).max().orElse(10);
        
        // Create a grid visualization
        Grid<Map.Entry<String, Integer>> deptGrid = new Grid<>();
        deptGrid.setItems(actionItemsByDepartment.entrySet());
        
        deptGrid.addColumn(Map.Entry::getKey).setHeader("Department");
        deptGrid.addColumn(Map.Entry::getValue).setHeader("Count");
        
        deptGrid.addComponentColumn(entry -> {
            int count = entry.getValue();
            double ratio = (double) count / maxDeptCount;
            
            ProgressBar bar = new ProgressBar();
            bar.setValue(ratio);
            bar.setWidth("200px");
            
            return bar;
        }).setHeader("Distribution");
        
        deptLayout.add(deptGrid);
        
        // Add all layouts to the main layout
        chartsLayout.add(actionItemsLayout, priorityLayout, deptLayout);
    }
}