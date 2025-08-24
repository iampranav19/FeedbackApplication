package com.feedback.ui.views.actionitems;

import com.feedback.model.ActionItem;
import com.feedback.model.Feedback;
import com.feedback.model.User;
import com.feedback.service.ActionItemService;
import com.feedback.service.AuthenticationService;
import com.feedback.service.FeedbackService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "action-items", layout = MainLayout.class)
@PageTitle("Action Items | Feedback System")
@PermitAll
public class ActionItemView extends VerticalLayout {

    private final ActionItemService actionItemService;
    private final FeedbackService feedbackService;
    private final UserService userService;
    private final AuthenticationService authenticationService;
    
    private final Grid<ActionItem> grid = new Grid<>(ActionItem.class);
    private final ComboBox<String> statusFilter = new ComboBox<>("Status");
    private final ComboBox<String> priorityFilter = new ComboBox<>("Priority");
    
    private Tab assignedToMeTab;
    private Tab createdByMeTab;
    private Tab allItemsTab; // Only for admins
    
    private User currentUser;

    public ActionItemView(ActionItemService actionItemService, 
                          FeedbackService feedbackService,
                          UserService userService,
                          AuthenticationService authenticationService) {
        this.actionItemService = actionItemService;
        this.feedbackService = feedbackService;
        this.userService = userService;
        this.authenticationService = authenticationService;
        
        System.out.println("ActionItemView: Constructor started");
        
        // Spring Security ensures authentication - just get the current user
        this.currentUser = authenticationService.getCurrentUser();
        
        // Safety check - if user is null, something is wrong with authentication
        if (this.currentUser == null) {
            System.err.println("CRITICAL: Current user is null in ActionItemView!");
            add(new Span("Authentication error. Please refresh the page and try again."));
            return;
        }
        
        System.out.println("ActionItemView: User authenticated: " + currentUser.getFullName());

        addClassName("action-item-view");
        setSizeFull();

        try {
            configureGrid();
            configureFilters();
            
            // Create user info card
            Div userInfoCard = createUserInfoCard();
            
            Tabs tabs = createTabs();
            HorizontalLayout filterLayout = createFilterLayout();

            add(
                    new H2("Action Items"),
                    userInfoCard,
                    tabs,
                    filterLayout,
                    getToolbar(),
                    grid
            );

            // Default to showing assigned to me items
            showAssignedToMe();
            
            System.out.println("ActionItemView: Successfully initialized for user: " + currentUser.getFullName());
        } catch (Exception e) {
            System.err.println("Error initializing ActionItemView: " + e.getMessage());
            e.printStackTrace();
            add(new Span("Error loading action items. Please try again."));
        }
    }

    private Div createUserInfoCard() {
        Div infoCard = new Div();
        infoCard.getStyle()
                .set("background", "var(--lumo-success-color-10pct)")
                .set("border", "1px solid var(--lumo-success-color-50pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        HorizontalLayout content = new HorizontalLayout();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setWidthFull();
        content.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        
        Span userInfo = new Span("Managing action items for: " + currentUser.getFullName());
        userInfo.getStyle().set("font-weight", "bold");
        
        // Quick stats
        long assignedCount = actionItemService.findActionItemsByUser(currentUser.getId()).size();
        long activeCount = actionItemService.countActiveActionItems(currentUser.getId());
        
        Span quickStats = new Span("Assigned: " + assignedCount + " | Active: " + activeCount);
        quickStats.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        content.add(userInfo, quickStats);
        infoCard.add(content);
        return infoCard;
    }
    
    private Tabs createTabs() {
        assignedToMeTab = new Tab("Assigned to Me (" + getActionItemCount("assigned") + ")");
        createdByMeTab = new Tab("Created by Me (" + getActionItemCount("created") + ")");
        
        Tabs tabs;
        
        // Only super admins can see all action items
        if (currentUser.isSuperAdmin()) {
            allItemsTab = new Tab("All Items (" + getActionItemCount("all") + ")");
            tabs = new Tabs(assignedToMeTab, createdByMeTab, allItemsTab);
        } else {
            tabs = new Tabs(assignedToMeTab, createdByMeTab);
        }
        
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(assignedToMeTab)) {
                showAssignedToMe();
            } else if (event.getSelectedTab().equals(createdByMeTab)) {
                showCreatedByMe();
            } else if (event.getSelectedTab().equals(allItemsTab)) {
                showAllItems();
            }
        });
        
        return tabs;
    }
    
    private long getActionItemCount(String type) {
        switch (type) {
            case "assigned":
                return actionItemService.findActionItemsByUser(currentUser.getId()).size();
            case "created":
                return actionItemService.findAllActionItems().stream()
                        .filter(item -> item.getCreatedBy() != null && 
                                      item.getCreatedBy().getId().equals(currentUser.getId()))
                        .count();
            case "all":
                return actionItemService.findAllActionItems().size();
            default:
                return 0;
        }
    }

    private void configureGrid() {
        grid.addClassName("action-item-grid");
        grid.setSizeFull();
        
        // Remove default columns
        grid.removeAllColumns();
        
        // Add custom columns
        grid.addColumn(item -> item.getTitle())
            .setHeader("Title").setAutoWidth(true).setSortable(true);
        grid.addColumn(item -> item.getAssignedTo().getFullName())
            .setHeader("Assigned To").setAutoWidth(true).setSortable(true);
        grid.addColumn(item -> item.getCreatedBy() != null ? item.getCreatedBy().getFullName() : "System")
            .setHeader("Created By").setAutoWidth(true).setSortable(true);
        grid.addColumn(item -> formatDate(item.getDueDate()))
            .setHeader("Due Date").setAutoWidth(true).setSortable(true);
        
        // Add status with color coding
        grid.addComponentColumn(item -> {
            Span statusSpan = new Span(item.getStatus());
            switch (item.getStatus()) {
                case "Open":
                    statusSpan.getStyle().set("color", "var(--lumo-primary-color)");
                    break;
                case "In Progress":
                    statusSpan.getStyle().set("color", "var(--lumo-warning-color)");
                    break;
                case "Completed":
                    statusSpan.getStyle().set("color", "var(--lumo-success-color)");
                    break;
                case "Cancelled":
                    statusSpan.getStyle().set("color", "var(--lumo-error-color)");
                    break;
            }
            statusSpan.getStyle().set("font-weight", "bold");
            return statusSpan;
        }).setHeader("Status").setAutoWidth(true);
        
        // Add priority with color coding
        grid.addComponentColumn(item -> {
            Span prioritySpan = new Span(item.getPriority());
            switch (item.getPriority()) {
                case "High":
                    prioritySpan.getStyle()
                        .set("background-color", "var(--lumo-error-color)")
                        .set("color", "white")
                        .set("padding", "2px 8px")
                        .set("border-radius", "4px")
                        .set("font-size", "0.8em");
                    break;
                case "Medium":
                    prioritySpan.getStyle()
                        .set("background-color", "var(--lumo-warning-color)")
                        .set("color", "white")
                        .set("padding", "2px 8px")
                        .set("border-radius", "4px")
                        .set("font-size", "0.8em");
                    break;
                case "Low":
                    prioritySpan.getStyle()
                        .set("background-color", "var(--lumo-success-color)")
                        .set("color", "white")
                        .set("padding", "2px 8px")
                        .set("border-radius", "4px")
                        .set("font-size", "0.8em");
                    break;
            }
            return prioritySpan;
        }).setHeader("Priority").setAutoWidth(true);
        
        // Add action column with buttons
        grid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            Button viewButton = new Button("View");
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            viewButton.addClickListener(e -> openActionItemDialog(item));
            
            Button completeButton = new Button("Complete");
            completeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
            completeButton.setEnabled(!item.getStatus().equals("Completed") && 
                                      (item.getAssignedTo().getId().equals(currentUser.getId()) ||
                                       currentUser.isSuperAdmin()));
            completeButton.addClickListener(e -> {
                actionItemService.completeActionItem(item.getId());
                updateList();
                showSuccess("Action item marked as completed");
            });
            
            actions.add(viewButton, completeButton);
            actions.setSpacing(true);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }
    
    private void configureFilters() {
        statusFilter.setItems("All", "Open", "In Progress", "Completed", "Cancelled");
        statusFilter.setValue("All");
        statusFilter.addValueChangeListener(e -> updateList());
        
        priorityFilter.setItems("All", "High", "Medium", "Low");
        priorityFilter.setValue("All");
        priorityFilter.addValueChangeListener(e -> updateList());
    }
    
    private HorizontalLayout createFilterLayout() {
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setAlignItems(FlexComponent.Alignment.END);
        filterLayout.add(statusFilter, priorityFilter);
        
        Button clearFiltersButton = new Button("Clear Filters");
        clearFiltersButton.addClickListener(e -> {
            statusFilter.setValue("All");
            priorityFilter.setValue("All");
        });
        
        filterLayout.add(clearFiltersButton);
        filterLayout.setSpacing(true);
        return filterLayout;
    }
    
    private HorizontalLayout getToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        
        Button addButton = new Button("Add Action Item");
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openActionItemDialog(new ActionItem()));
        
        toolbar.add(addButton);
        toolbar.setSpacing(true);
        return toolbar;
    }
    
    private void openActionItemDialog(ActionItem item) {
        boolean isNewItem = item.getId() == null;
        
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNewItem ? "Add Action Item" : "Action Item Details");
        dialog.setWidth("700px");
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        
        if (isNewItem) {
            // Show creation form - simplified for brevity
            TextField title = new TextField("Title");
            title.setRequired(true);
            title.setWidthFull();
            
            ComboBox<User> assignedTo = new ComboBox<>("Assigned To");
            assignedTo.setItems(userService.findActiveUsers());
            assignedTo.setItemLabelGenerator(User::getFullName);
            assignedTo.setValue(currentUser);
            assignedTo.setRequired(true);
            
            DatePicker dueDate = new DatePicker("Due Date");
            dueDate.setValue(LocalDate.now().plusWeeks(1));
            
            Button saveButton = new Button("Create Action Item");
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            saveButton.addClickListener(e -> {
                if (!title.isEmpty() && !assignedTo.isEmpty()) {
                    ActionItem newItem = new ActionItem();
                    newItem.setTitle(title.getValue());
                    newItem.setAssignedTo(assignedTo.getValue());
                    newItem.setDueDate(dueDate.getValue());
                    newItem.setStatus("Open");
                    newItem.setPriority("Medium");
                    newItem.setCreatedBy(currentUser);
                    newItem.setCreatedAt(LocalDateTime.now());
                    
                    actionItemService.saveActionItem(newItem);
                    dialog.close();
                    updateList();
                    showSuccess("Action item created successfully");
                }
            });
            
            Button cancelButton = new Button("Cancel");
            cancelButton.addClickListener(e -> dialog.close());
            
            content.add(title, assignedTo, dueDate, new HorizontalLayout(saveButton, cancelButton));
        } else {
            // Show details view - simplified
            content.add(new H3(item.getTitle()));
            content.add(new Span("Assigned to: " + item.getAssignedTo().getFullName()));
            content.add(new Span("Status: " + item.getStatus()));
            content.add(new Span("Priority: " + item.getPriority()));
            
            Button closeButton = new Button("Close");
            closeButton.addClickListener(e -> dialog.close());
            content.add(closeButton);
        }
        
        dialog.add(content);
        dialog.open();
    }
    
    private void showAssignedToMe() {
        List<ActionItem> items = actionItemService.findActionItemsByUser(currentUser.getId());
        applyFiltersAndSetItems(items);
    }
    
    private void showCreatedByMe() {
        List<ActionItem> items = actionItemService.findAllActionItems().stream()
                .filter(item -> item.getCreatedBy() != null && 
                              item.getCreatedBy().getId().equals(currentUser.getId()))
                .toList();
        applyFiltersAndSetItems(items);
    }
    
    private void showAllItems() {
        if (currentUser.isSuperAdmin()) {
            List<ActionItem> items = actionItemService.findAllActionItems();
            applyFiltersAndSetItems(items);
        }
    }
    
    private void applyFiltersAndSetItems(List<ActionItem> items) {
        List<ActionItem> filteredItems = items.stream()
                .filter(item -> {
                    boolean statusMatch = "All".equals(statusFilter.getValue()) || 
                                        item.getStatus().equals(statusFilter.getValue());
                    boolean priorityMatch = "All".equals(priorityFilter.getValue()) || 
                                          item.getPriority().equals(priorityFilter.getValue());
                    return statusMatch && priorityMatch;
                })
                .sorted((i1, i2) -> {
                    // Sort by due date, then by priority
                    if (i1.getDueDate() == null && i2.getDueDate() == null) return 0;
                    if (i1.getDueDate() == null) return 1;
                    if (i2.getDueDate() == null) return -1;
                    return i1.getDueDate().compareTo(i2.getDueDate());
                })
                .toList();
                
        grid.setItems(filteredItems);
    }
    
    private void updateList() {
        if (assignedToMeTab.isSelected()) {
            showAssignedToMe();
        } else if (createdByMeTab.isSelected()) {
            showCreatedByMe();
        } else if (allItemsTab != null && allItemsTab.isSelected()) {
            showAllItems();
        }
    }
    
    private String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "Not set";
    }
    
    private void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}