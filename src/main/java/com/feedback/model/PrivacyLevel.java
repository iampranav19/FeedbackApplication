package com.feedback.model;

public enum PrivacyLevel {
    PUBLIC("Public - Viewable by everyone"),
    DEPARTMENT("Department - Viewable within department only"),
    PRIVATE("Private - Viewable only by sender and recipient"),
    ANONYMOUS("Anonymous - Sender identity hidden");
    
    private final String description;
    
    PrivacyLevel(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}