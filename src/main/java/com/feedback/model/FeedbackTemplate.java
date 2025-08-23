package com.feedback.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class FeedbackTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    
    @Column(length = 500)
    private String instructions;
    
    @ElementCollection
    @CollectionTable(name = "template_questions", joinColumns = @JoinColumn(name = "template_id"))
    @Column(name = "question", length = 500)
    private List<String> questions = new ArrayList<>();
    
    private boolean isActive = true;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}