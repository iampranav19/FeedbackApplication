package com.feedback.ui.views.actionitems;

import com.feedback.model.ActionItem;
import com.feedback.model.Feedback;
import com.feedback.model.User;
import com.feedback.service.ActionItemService;
import com.feedback.service.FeedbackService;
import com.feedback.service.UserService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Route(value = "action-items", layout = MainLayout.class)
@PageTitle("Action Items | Feedback System")
public class ActionItemView extends VerticalLayout {

    private final ActionItemService actionItemService;
    private final FeedbackService feedbackService;
    private final UserService userService;
    private final Grid<ActionItem> grid = new Grid<>(ActionItem.class);
    private final Binder<ActionItem> binder = new Binder<>(ActionItem.class);
    
    private Tab assignedToMeTab;
    private Tab createdByMeTab;
    private Tab allItemsTab;
    
    private User currentUser; // In a real app, this would be the logged-in user

    public ActionItemView(ActionItemService actionItemService, 
                          FeedbackService feedbackService,
                          UserService userService) {
        this.actionItemService = actionItemService;
        this.feedbackService = feedbackService;
        this.userService = userService;
        
        // For the demo, we'll use the admin user as the current user
        this.currentUser = userService.findUserById(1L).orElse(null);

        addClassName("action-item-view");
        setSizeFull();

        configureGrid();
        
        Tabs tabs = createTabs();
        
        add(
                new H2("Action Items"),
                tabs,
                getToolbar(),
                grid
        );

        // Default to showing assigned to me items
        showAssignedToMe();
    }
    
    private Tabs createTabs() {
        assignedToMeTab = new Tab("Assigned to Me");
        createdByMeTab = new Tab("Created by Me");
        allItemsTab = new Tab("All Items");
        
        Tabs tabs = new Tabs(assignedToMeTab, createdByMeTab, allItemsTab);
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(assignedToMeTab)) {
                showAssignedToMe();
            } else if (event.getSelectedTab().equals(createdByMeTab)) {
                showCreatedByMe();
            } else {
                showAllItems();
            }
        });
        
        return tabs;
    }

    private void configureGrid() {
        grid.addClassName("action-item-grid");
        grid.setSizeFull();
        
        // Remove default columns
        grid.removeAllColumns();
        
        // Add custom columns
        grid.addColumn(item -> item.getTitle()).setHeader("Title").setAutoWidth(true);
        grid.addColumn(item -> item.getAssignedTo().getFullName()).setHeader("Assigned To").setAutoWidth(true);
        grid.addColumn(item -> item.getCreatedBy().getFullName()).setHeader("Created By").setAutoWidth(true);
        grid.addColumn(item -> item.getDueDate()).setHeader("Due Date").setAutoWidth(true);
        grid.addColumn(item -> item.getStatus()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(item -> item.getPriority()).setHeader("Priority").setAutoWidth(true);
        
        // Add action column with buttons
        grid.addComponentColumn(item -> {
            Button viewButton = new Button("View");
            viewButton.addClickListener(e -> openActionItemDialog(item));
            
            Button completeButton = new Button("Complete");
            completeButton.setEnabled(!item.getStatus().equals("Completed") && 
                                      item.getAssignedTo().getId().equals(currentUser.getId()));
            completeButton.addClickListener(e -> {
                actionItemService.completeActionItem(item.getId());
                updateList();
                Notification.show("Action item marked as completed");
            });
            
            HorizontalLayout buttons = new HorizontalLayout(viewButton, completeButton);
            return buttons;
        }).setHeader("Actions").setAutoWidth(true);
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }
    
    private HorizontalLayout getToolbar() {
        Button addButton = new Button("Add Action Item");
        addButton.addClickListener(e -> openActionItemDialog(new ActionItem()));
        
        ComboBox<String> statusFilter = new ComboBox<>("Status");
        statusFilter.setItems("All", "Open", "In Progress", "Completed", "Cancelled");
        statusFilter.setValue("All");
        statusFilter.addValueChangeListener(e -> updateList());
        
        HorizontalLayout toolbar = new HorizontalLayout(addButton, statusFilter);
        toolbar.setSpacing(true);
        return toolbar;
    }
    
    private void openActionItemDialog(ActionItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(item.getId() == null ? "Add Action Item" : "Edit Action Item");
        dialog.setWidth("600px");
        
        // Create form fields
        TextField title = new TextField("Title");
        title.setRequired(true);
        
        TextArea description = new TextArea("Description");
        description.setWidthFull();
        description.setHeight("100px");
        
        ComboBox<Feedback> relatedFeedback = new ComboBox<>("Related Feedback");
        relatedFeedback.setItems(feedbackService.findAllFeedback());
        relatedFeedback.setItemLabelGenerator(f -> 
            f.getSender().getFullName() + " -> " + f.getRecipient().getFullName() + " (" + f.getCategory() + ")"
        );
        
        ComboBox<User> assignedTo = new ComboBox<>("Assigned To");
        assignedTo.setItems(userService.findAllUsers());
        assignedTo.setItemLabelGenerator(User::getFullName);
        assignedTo.setRequired(true);
        
        DatePicker dueDate = new DatePicker("Due Date");
        
        ComboBox<String> priority = new ComboBox<>("Priority");
        priority.setItems("Low", "Medium", "High");
        priority.setValue("Medium");
        
        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("Open", "In Progress", "Completed", "Cancelled");
        status.setValue("Open");
        
        // Set initial values if item is not new
        if (item.getId() != null) {
            title.setValue(item.getTitle());
            description.setValue(item.getDescription() != null ? item.getDescription() : "");
            relatedFeedback.setValue(item.getFeedback());
            assignedTo.setValue(item.getAssignedTo());
            dueDate.setValue(item.getDueDate());
            priority.setValue(item.getPriority());
            status.setValue(item.getStatus());
        }
        
        // Create form layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(title, assignedTo, dueDate, priority, status, relatedFeedback, description);
        formLayout.setColspan(description, 2);
        formLayout.setColspan(relatedFeedback, 2);
        
        // Create buttons
        Button saveButton = new Button("Save");
        saveButton.addClickListener(e -> {
            if (title.isEmpty() || assignedTo.isEmpty()) {
                Notification.show("Please fill in all required fields");
                return;
            }
            
            item.setTitle(title.getValue());
            item.setDescription(description.getValue());
            item.setFeedback(relatedFeedback.getValue());
            item.setAssignedTo(assignedTo.getValue());
            item.setDueDate(dueDate.getValue());
            item.setPriority(priority.getValue());
            item.setStatus(status.getValue());
            
            // If it's a new item, set the creator
            if (item.getId() == null) {
                item.setCreatedBy(currentUser);
                item.setCreatedAt(LocalDateTime.now());
            }
            
            // Save the item
            actionItemService.saveActionItem(item);
            dialog.close();
            updateList();
            Notification.show("Action item saved");
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> dialog.close());
        
        Button deleteButton = new Button("Delete");
        deleteButton.setVisible(item.getId() != null);
        deleteButton.addClickListener(e -> {
            actionItemService.deleteActionItem(item.getId());
            dialog.close();
            updateList();
            Notification.show("Action item deleted");
        });
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton, deleteButton);
        buttonLayout.setSpacing(true);
        
        dialog.add(formLayout, buttonLayout);
        dialog.open();
    }
    
    private void showAssignedToMe() {
        if (currentUser != null) {
            grid.setItems(actionItemService.findActionItemsByUser(currentUser.getId()));
        }
    }
    
    private void showCreatedByMe() {
        if (currentUser != null) {
            // In a real app, we would filter by created by
            // For this demo, we'll just show all items
            grid.setItems(actionItemService.findAllActionItems());
        }
    }
    
    private void showAllItems() {
        grid.setItems(actionItemService.findAllActionItems());
    }
    
    private void updateList() {
        if (assignedToMeTab.isSelected()) {
            showAssignedToMe();
        } else if (createdByMeTab.isSelected()) {
            showCreatedByMe();
        } else {
            showAllItems();
        }
    }
}