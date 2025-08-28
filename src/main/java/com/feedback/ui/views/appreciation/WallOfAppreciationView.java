package com.feedback.ui.views.appreciation;

import com.feedback.model.Feedback;
import com.feedback.model.PrivacyLevel;
import com.feedback.model.User;
import com.feedback.service.AuthenticationService;
import com.feedback.service.FeedbackService;
import com.feedback.ui.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "wall-of-appreciation", layout = MainLayout.class)
@PageTitle("Wall of Appreciation | Feedback System")
@PermitAll
public class WallOfAppreciationView extends VerticalLayout {

    private final FeedbackService feedbackService;
    private final AuthenticationService authenticationService;
    
    private final VerticalLayout feedbackCardsLayout = new VerticalLayout();
    private final ComboBox<String> categoryFilter = new ComboBox<>("Filter by Category");
    private final ComboBox<String> sortFilter = new ComboBox<>("Sort by");
    private final TextField searchField = new TextField();
    
    private User currentUser;
    private List<Feedback> allPublicFeedback;
    
    // References to stat card value spans for easy updating
    private Span totalFeedbackValue;
    private Span thisMonthValue;
    private Span activeUsersValue;

    public WallOfAppreciationView(FeedbackService feedbackService, 
                                  AuthenticationService authenticationService) {
        this.feedbackService = feedbackService;
        this.authenticationService = authenticationService;
        
        System.out.println("WallOfAppreciationView: Constructor started");
        
        // Get current user (for authentication, but this view is public)
        this.currentUser = authenticationService.getCurrentUser();
        
        // Safety check
        if (this.currentUser == null) {
            System.err.println("CRITICAL: Current user is null in WallOfAppreciationView!");
            add(new Span("Authentication error. Please refresh the page and try again."));
            return;
        }
        
        System.out.println("WallOfAppreciationView: User authenticated: " + currentUser.getFullName());

        addClassName("wall-of-appreciation-view");
        setSizeFull();

        try {
            createHeader();
            createFilters();
            createFeedbackDisplay();
            loadPublicFeedback();
            
            System.out.println("WallOfAppreciationView: Successfully initialized");
        } catch (Exception e) {
            System.err.println("Error initializing WallOfAppreciationView: " + e.getMessage());
            e.printStackTrace();
            add(new Span("Error loading wall of appreciation. Please try again."));
        }
    }
    
    private void createHeader() {
        // Create a beautiful header section
        VerticalLayout header = new VerticalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("background", "linear-gradient(135deg, var(--lumo-primary-color-10pct), var(--lumo-success-color-10pct))")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("margin-bottom", "var(--lumo-space-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)");
        
        // Main title with icon
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        
        Icon appreciationIcon = VaadinIcon.HEART.create();
        appreciationIcon.setSize("2em");
        appreciationIcon.getStyle().set("color", "var(--lumo-error-color)");
        
        H1 title = new H1("Wall of Appreciation");
        title.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("text-align", "center")
                .set("margin", "0")
                .set("font-weight", "bold");
        
        titleLayout.add(appreciationIcon, title);
        
        // Subtitle
        Paragraph subtitle = new Paragraph("Celebrating great work and positive feedback from our team");
        subtitle.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic")
                .set("margin", "var(--lumo-space-s) 0 0 0");
        
        // Stats section
        HorizontalLayout statsLayout = createStatsLayout();
        
        header.add(titleLayout, subtitle, statsLayout);
        add(header);
    }
    
    private HorizontalLayout createStatsLayout() {
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        statsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        statsLayout.setSpacing(true);
        statsLayout.getStyle().set("margin-top", "var(--lumo-space-m)");
        
        // Create stat cards and store references to value spans
        Div totalFeedbackStat = createStatCard(VaadinIcon.COMMENTS, "Total Appreciations", "0");
        Div thisMonthStat = createStatCard(VaadinIcon.CALENDAR, "This Month", "0");
        Div activeUsersStat = createStatCard(VaadinIcon.USERS, "Active Contributors", "0");
        
        statsLayout.add(totalFeedbackStat, thisMonthStat, activeUsersStat);
        return statsLayout;
    }
    
    private Div createStatCard(VaadinIcon iconType, String label, String value) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("text-align", "center")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("min-width", "120px");
        
        Icon icon = iconType.create();
        icon.setSize("1.5em");
        icon.getStyle().set("color", "var(--lumo-primary-color)");
        
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("display", "block")
                .set("font-size", "1.8em")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-color)");
        
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("display", "block")
                .set("font-size", "0.9em")
                .set("color", "var(--lumo-secondary-text-color)");
        
        // Store references to value spans based on the label
        if (label.equals("Total Appreciations")) {
            totalFeedbackValue = valueSpan;
        } else if (label.equals("This Month")) {
            thisMonthValue = valueSpan;
        } else if (label.equals("Active Contributors")) {
            activeUsersValue = valueSpan;
        }
        
        card.add(icon, valueSpan, labelSpan);
        return card;
    }
    
    private void createFilters() {
        HorizontalLayout filtersLayout = new HorizontalLayout();
        filtersLayout.setWidthFull();
        filtersLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        filtersLayout.setAlignItems(FlexComponent.Alignment.END);
        
        // Left side - filters
        HorizontalLayout leftFilters = new HorizontalLayout();
        leftFilters.setAlignItems(FlexComponent.Alignment.END);
        
        // Category filter
        categoryFilter.setItems("All Categories", "Performance", "Leadership", "Communication", 
                               "Teamwork", "Technical Skills", "Innovation", "Collaboration", "Other");
        categoryFilter.setValue("All Categories");
        categoryFilter.addValueChangeListener(e -> filterAndDisplayFeedback());
        
        // Sort filter
        sortFilter.setItems("Newest First", "Oldest First", "Most Recent Activity");
        sortFilter.setValue("Newest First");
        sortFilter.addValueChangeListener(e -> filterAndDisplayFeedback());
        
        leftFilters.add(categoryFilter, sortFilter);
        
        // Right side - search and add button
        HorizontalLayout rightSection = new HorizontalLayout();
        rightSection.setAlignItems(FlexComponent.Alignment.END);
        
        // Search field
        searchField.setPlaceholder("Search appreciations...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("200px");
        searchField.addValueChangeListener(e -> filterAndDisplayFeedback());
        
        // Add appreciation button
        Button addAppreciationButton = new Button("Give Appreciation", VaadinIcon.PLUS.create());
        addAppreciationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addAppreciationButton.addClickListener(e -> navigateToFeedbackForm());
        
        rightSection.add(searchField, addAppreciationButton);
        
        filtersLayout.add(leftFilters, rightSection);
        
        add(filtersLayout);
    }
    
    private void createFeedbackDisplay() {
        // Container for feedback cards
        feedbackCardsLayout.setWidthFull();
        feedbackCardsLayout.setSpacing(true);
        feedbackCardsLayout.setPadding(false);
        
        // Add loading message initially
        Div loadingMessage = new Div();
        loadingMessage.setText("Loading appreciations...");
        loadingMessage.getStyle()
                .set("text-align", "center")
                .set("padding", "var(--lumo-space-xl)")
                .set("color", "var(--lumo-secondary-text-color)");
        
        feedbackCardsLayout.add(loadingMessage);
        add(feedbackCardsLayout);
    }
    
    private void loadPublicFeedback() {
        try {
            // Get all public feedback
            allPublicFeedback = feedbackService.findAllFeedback().stream()
                    .filter(feedback -> feedback.getPrivacyLevel() == PrivacyLevel.PUBLIC)
                    .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
                    .collect(Collectors.toList());
            
            // Update stats
            updateStats();
            
            // Display filtered feedback
            filterAndDisplayFeedback();
            
        } catch (Exception e) {
            System.err.println("Error loading public feedback: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading appreciations. Please refresh the page.");
        }
    }
    
    private void updateStats() {
        if (allPublicFeedback == null) return;
        
        // Total feedback count
        int totalCount = allPublicFeedback.size();
        
        // This month count
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long thisMonthCount = allPublicFeedback.stream()
                .filter(f -> f.getCreatedAt().isAfter(startOfMonth))
                .count();
        
        // Active contributors (unique senders)
        long activeContributors = allPublicFeedback.stream()
                .map(f -> f.getSender().getId())
                .distinct()
                .count();
        
        // Update stat cards directly
        if (totalFeedbackValue != null) {
            totalFeedbackValue.setText(String.valueOf(totalCount));
        }
        if (thisMonthValue != null) {
            thisMonthValue.setText(String.valueOf(thisMonthCount));
        }
        if (activeUsersValue != null) {
            activeUsersValue.setText(String.valueOf(activeContributors));
        }
    }
    
    private void filterAndDisplayFeedback() {
        if (allPublicFeedback == null) return;
        
        // Apply filters
        List<Feedback> filteredFeedback = allPublicFeedback.stream()
                .filter(this::matchesFilters)
                .collect(Collectors.toList());
        
        // Apply sorting
        switch (sortFilter.getValue()) {
            case "Oldest First":
                filteredFeedback.sort(Comparator.comparing(Feedback::getCreatedAt));
                break;
            case "Most Recent Activity":
                // For now, same as newest first, but could be enhanced to consider updates
                filteredFeedback.sort(Comparator.comparing(Feedback::getCreatedAt).reversed());
                break;
            default: // "Newest First"
                filteredFeedback.sort(Comparator.comparing(Feedback::getCreatedAt).reversed());
                break;
        }
        
        // Display feedback cards
        displayFeedbackCards(filteredFeedback);
    }
    
    private boolean matchesFilters(Feedback feedback) {
        // Category filter
        if (!"All Categories".equals(categoryFilter.getValue()) && 
            !categoryFilter.getValue().equals(feedback.getCategory())) {
            return false;
        }
        
        // Search filter
        String searchTerm = searchField.getValue();
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String lowerSearchTerm = searchTerm.toLowerCase();
            return feedback.getContent().toLowerCase().contains(lowerSearchTerm) ||
                   feedback.getSender().getFullName().toLowerCase().contains(lowerSearchTerm) ||
                   feedback.getRecipient().getFullName().toLowerCase().contains(lowerSearchTerm) ||
                   feedback.getCategory().toLowerCase().contains(lowerSearchTerm);
        }
        
        return true;
    }
    
    private void displayFeedbackCards(List<Feedback> feedbackList) {
        feedbackCardsLayout.removeAll();
        
        if (feedbackList.isEmpty()) {
            Div noResultsMessage = new Div();
            noResultsMessage.setText("No appreciations found matching your filters. Try adjusting your search criteria.");
            noResultsMessage.getStyle()
                    .set("text-align", "center")
                    .set("padding", "var(--lumo-space-xl)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            feedbackCardsLayout.add(noResultsMessage);
            return;
        }
        
        // Create a grid-like layout for cards
        HorizontalLayout currentRow = null;
        int cardsInCurrentRow = 0;
        final int CARDS_PER_ROW = 2; // Adjust based on desired layout
        
        for (Feedback feedback : feedbackList) {
            if (currentRow == null || cardsInCurrentRow >= CARDS_PER_ROW) {
                currentRow = new HorizontalLayout();
                currentRow.setWidthFull();
                currentRow.setSpacing(true);
                feedbackCardsLayout.add(currentRow);
                cardsInCurrentRow = 0;
            }
            
            Component card = createFeedbackCard(feedback);
            currentRow.add(card);
            currentRow.setFlexGrow(1, card);
            cardsInCurrentRow++;
        }
    }
    
    private Component createFeedbackCard(Feedback feedback) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(true);
        card.setPadding(true);
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-m)")
                .set("border-left", "4px solid var(--lumo-success-color)")
                .set("transition", "transform 0.2s ease, box-shadow 0.2s ease")
                .set("cursor", "default")
                .set("margin-bottom", "var(--lumo-space-m)");
        
        // Add hover effect
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("transform", "translateY(-2px)");
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-l)");
        });
        
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().set("transform", "translateY(0)");
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        });
        
        // Header with sender and recipient info
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.START);
        
        // Left side - sender to recipient
        VerticalLayout fromToLayout = new VerticalLayout();
        fromToLayout.setSpacing(false);
        fromToLayout.setPadding(false);
        
        HorizontalLayout fromLayout = new HorizontalLayout();
        fromLayout.setSpacing(false);
        fromLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Icon fromIcon = VaadinIcon.USER_HEART.create();
        fromIcon.setSize("1em");
        fromIcon.getStyle().set("color", "var(--lumo-primary-color)");
        
        Span fromLabel = new Span("From: ");
        fromLabel.getStyle().set("font-size", "0.9em").set("color", "var(--lumo-secondary-text-color)");
        
        Span senderName = new Span(feedback.getSender().getFullName());
        senderName.getStyle().set("font-weight", "bold").set("color", "var(--lumo-primary-color)");
        
        fromLayout.add(fromIcon, fromLabel, senderName);
        
        HorizontalLayout toLayout = new HorizontalLayout();
        toLayout.setSpacing(false);
        toLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Icon toIcon = VaadinIcon.ARROW_RIGHT.create();
        toIcon.setSize("1em");
        toIcon.getStyle().set("color", "var(--lumo-success-color)");
        
        Span toLabel = new Span("To: ");
        toLabel.getStyle().set("font-size", "0.9em").set("color", "var(--lumo-secondary-text-color)");
        
        Span recipientName = new Span(feedback.getRecipient().getFullName());
        recipientName.getStyle().set("font-weight", "bold").set("color", "var(--lumo-success-color)");
        
        toLayout.add(toIcon, toLabel, recipientName);
        
        fromToLayout.add(fromLayout, toLayout);
        
        // Right side - date and category
        VerticalLayout metaLayout = new VerticalLayout();
        metaLayout.setSpacing(false);
        metaLayout.setPadding(false);
        metaLayout.setAlignItems(FlexComponent.Alignment.END);
        
        Span dateSpan = new Span(formatDate(feedback.getCreatedAt()));
        dateSpan.getStyle()
                .set("font-size", "0.8em")
                .set("color", "var(--lumo-secondary-text-color)");
        
        Span categorySpan = new Span(feedback.getCategory());
        categorySpan.getStyle()
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-color)")
                .set("padding", "2px 8px")
                .set("border-radius", "12px")
                .set("font-size", "0.8em")
                .set("font-weight", "bold");
        
        metaLayout.add(dateSpan, categorySpan);
        
        header.add(fromToLayout, metaLayout);
        
        // Content section
        Div contentDiv = new Div();
        contentDiv.setText(feedback.getContent());
        contentDiv.getStyle()
                .set("margin", "var(--lumo-space-m) 0")
                .set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border-left", "3px solid var(--lumo-primary-color)")
                .set("font-style", "italic")
                .set("line-height", "1.6");
        
        // Footer with appreciation icon
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Icon heartIcon = VaadinIcon.HEART.create();
        heartIcon.getStyle().set("color", "var(--lumo-error-color)");
        
        Span appreciationLabel = new Span("Public Appreciation");
        appreciationLabel.getStyle()
                .set("font-size", "0.8em")
                .set("color", "var(--lumo-error-color)")
                .set("font-weight", "bold");
        
        footer.add(heartIcon, appreciationLabel);
        
        card.add(header, contentDiv, footer);
        return card;
    }
    
    private String formatDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");
        return dateTime.format(formatter);
    }
    
    private void navigateToFeedbackForm() {
        UI.getCurrent().navigate("feedback-form");
    }
    
    private void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}