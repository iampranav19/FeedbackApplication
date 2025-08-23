package com.feedback.ui.views.feedback;

import com.feedback.model.ActionItem;
import com.feedback.model.Feedback;
import com.feedback.model.PrivacyLevel;
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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "feedback-list", layout = MainLayout.class)
@PageTitle("View Feedback | Feedback System")
public class FeedbackListView extends VerticalLayout {

    private final FeedbackService feedbackService;
    private final UserService userService;
    private final ActionItemService actionItemService;
    private final Grid<Feedback> grid = new Grid<>(Feedback.class);
    private Tab receivedTab;
    private Tab sentTab;
    private User currentUser; // In a real app, this would be the logged-in user

    public FeedbackListView(FeedbackService feedbackService, UserService userService, ActionItemService actionItemService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
        this.actionItemService = actionItemService;
        
        // For the MVP, we'll use the admin user as the current user
        this.currentUser = userService.findUserById(1L).orElse(null);

        addClassName("feedback-list-view");
        setSizeFull();

        configureGrid();
        
        HorizontalLayout filterLayout = new HorizontalLayout();
        ComboBox<String> statusFilter = new ComboBox<>("Status");
        statusFilter.setItems("All", "Open", "Acknowledged", "In Progress", "Completed");
        statusFilter.setValue("All");
        statusFilter.addValueChangeListener(e -> updateList());
        
        filterLayout.add(statusFilter);

        Tabs tabs = createTabs();
        
        add(
                new H2("Feedback"),
                tabs,
                filterLayout,
                grid
        );

        // Default to showing received feedback
        showReceivedFeedback();
    }

    private Tabs createTabs() {
        receivedTab = new Tab("Received Feedback");
        sentTab = new Tab("Sent Feedback");
        
        Tabs tabs = new Tabs(receivedTab, sentTab);
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(receivedTab)) {
                showReceivedFeedback();
            } else {
                showSentFeedback();
            }
        });
        
        return tabs;
    }

    private void configureGrid() {
        grid.addClassName("feedback-grid");
        grid.setSizeFull();
        
        // Remove default columns
        grid.removeAllColumns();
        
        // Add custom columns
        grid.addColumn(feedback -> formatDate(feedback)).setHeader("Date").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getSender().getFullName()).setHeader("From").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getRecipient().getFullName()).setHeader("To").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getCategory()).setHeader("Category").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getStatus()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(feedback -> feedback.getPrivacyLevel().name()).setHeader("Privacy").setAutoWidth(true);
        
        // Add action column with view button
        grid.addComponentColumn(feedback -> {
            Button viewButton = new Button("View");
            viewButton.addClickListener(e -> openFeedbackDialog(feedback));
            return viewButton;
        }).setHeader("Actions").setAutoWidth(true);
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }
    
    private String formatDate(Feedback feedback) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return feedback.getCreatedAt().format(formatter);
    }

    private void openFeedbackDialog(Feedback feedback) {
        // Mark as read if user is the recipient
        if (feedback.getRecipient().getId().equals(currentUser.getId()) && !feedback.isRead()) {
            feedbackService.markAsRead(feedback.getId());
        }
        
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        
        VerticalLayout layout = new VerticalLayout();
        
        H3 title = new H3("Feedback Details");
        
        Paragraph fromParagraph = new Paragraph("From: " + feedback.getSender().getFullName());
        Paragraph toParagraph = new Paragraph("To: " + feedback.getRecipient().getFullName());
        Paragraph categoryParagraph = new Paragraph("Category: " + feedback.getCategory());
        Paragraph dateParagraph = new Paragraph("Date: " + formatDate(feedback));
        Paragraph privacyParagraph = new Paragraph("Privacy: " + feedback.getPrivacyLevel().getDescription());
        
        Span contentLabel = new Span("Feedback:");
        Paragraph contentParagraph = new Paragraph(feedback.getContent());
        contentParagraph.getStyle().set("white-space", "pre-wrap");
        contentParagraph.getStyle().set("border", "1px solid #ddd");
        contentParagraph.getStyle().set("padding", "10px");
        contentParagraph.getStyle().set("background-color", "#f9f9f9");
        
        // Status update controls (only if the user is the recipient)
        HorizontalLayout statusLayout = new HorizontalLayout();
        ComboBox<String> statusComboBox = new ComboBox<>("Status");
        statusComboBox.setItems("Open", "Acknowledged", "In Progress", "Completed");
        statusComboBox.setValue(feedback.getStatus());
        
        Button updateStatusButton = new Button("Update Status");
        updateStatusButton.addClickListener(e -> {
            feedbackService.updateFeedbackStatus(feedback.getId(), statusComboBox.getValue());
            dialog.close();
            updateList();
            Notification.show("Status updated successfully");
        });
        
        // Create Action Item button
        Button createActionItemButton = new Button("Create Action Item");
        createActionItemButton.addClickListener(e -> {
            dialog.close();
            openCreateActionItemDialog(feedback);
        });
        
        statusLayout.add(statusComboBox, updateStatusButton, createActionItemButton);
        // Only show status controls if user is the recipient
        statusLayout.setVisible(feedback.getRecipient().getId().equals(currentUser.getId()));
        
        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> dialog.close());
        
        layout.add(
                title,
                fromParagraph,
                toParagraph,
                categoryParagraph,
                dateParagraph,
                privacyParagraph,
                contentLabel,
                contentParagraph,
                statusLayout,
                closeButton
        );
        
        dialog.add(layout);
        dialog.open();
    }
    
    private void openCreateActionItemDialog(Feedback feedback) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Action Item from Feedback");
        dialog.setWidth("600px");
        
        // Create form fields
        TextField title = new TextField("Title");
        title.setRequired(true);
        title.setValue("Action from feedback: " + feedback.getCategory());
        title.setWidthFull();
        
        TextArea description = new TextArea("Description");
        description.setWidthFull();
        description.setHeight("100px");
        description.setValue("Based on feedback: \n\n" + feedback.getContent().substring(0, Math.min(200, feedback.getContent().length())) + "...");
        
        ComboBox<User> assignedTo = new ComboBox<>("Assigned To");
        assignedTo.setItems(userService.findAllUsers());
        assignedTo.setItemLabelGenerator(User::getFullName);
        assignedTo.setValue(feedback.getRecipient()); // Default to the feedback recipient
        assignedTo.setRequired(true);
        
        DatePicker dueDate = new DatePicker("Due Date");
        dueDate.setValue(LocalDate.now().plusWeeks(1)); // Default to one week from now
        
        ComboBox<String> priority = new ComboBox<>("Priority");
        priority.setItems("Low", "Medium", "High");
        priority.setValue("Medium");
        
        // Create form layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(title, assignedTo, dueDate, priority, description);
        formLayout.setColspan(description, 2);
        
        // Create buttons
        Button saveButton = new Button("Create Action Item");
        saveButton.addClickListener(e -> {
            if (title.isEmpty() || assignedTo.isEmpty()) {
                Notification.show("Please fill in all required fields");
                return;
            }
            
            // Create a new action item
            ActionItem actionItem = new ActionItem();
            actionItem.setTitle(title.getValue());
            actionItem.setDescription(description.getValue());
            actionItem.setFeedback(feedback);
            actionItem.setAssignedTo(assignedTo.getValue());
            actionItem.setDueDate(dueDate.getValue());
            actionItem.setPriority(priority.getValue());
            actionItem.setStatus("Open");
            actionItem.setCreatedBy(currentUser);
            actionItem.setCreatedAt(LocalDateTime.now());
            
            // Use the injected actionItemService directly
            actionItemService.saveActionItem(actionItem);
            dialog.close();
            Notification.show("Action item created");
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        
        dialog.add(formLayout, buttonLayout);
        dialog.open();
    }

    private void showReceivedFeedback() {
        if (currentUser != null) {
            List<Feedback> feedbackList = feedbackService.findFeedbackReceivedByUser(currentUser.getId());
            grid.setItems(feedbackList);
        }
    }

    private void showSentFeedback() {
        if (currentUser != null) {
            List<Feedback> feedbackList = feedbackService.findFeedbackSentByUser(currentUser.getId());
            grid.setItems(feedbackList);
        }
    }

    private void updateList() {
        if (receivedTab.isSelected()) {
            showReceivedFeedback();
        } else {
            showSentFeedback();
        }
    }
}