package com.feedback.service;

import com.feedback.model.FeedbackTemplate;
import com.feedback.repository.FeedbackTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackTemplateService {
    
    private final FeedbackTemplateRepository feedbackTemplateRepository;
    
    @Autowired
    public FeedbackTemplateService(FeedbackTemplateRepository feedbackTemplateRepository) {
        this.feedbackTemplateRepository = feedbackTemplateRepository;
    }
    
    public List<FeedbackTemplate> findAllTemplates() {
        return feedbackTemplateRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<FeedbackTemplate> findActiveTemplates() {
        List<FeedbackTemplate> templates = feedbackTemplateRepository.findByIsActiveTrue();
        // Force initialization of the questions collection for each template
        templates.forEach(t -> t.getQuestions().size());
        return templates;
    }
    
    @Transactional(readOnly = true)
    public Optional<FeedbackTemplate> findTemplateById(Long id) {
        Optional<FeedbackTemplate> template = feedbackTemplateRepository.findById(id);
        // Force initialization of the questions collection
        template.ifPresent(t -> t.getQuestions().size());
        return template;
    }
    
    public FeedbackTemplate saveTemplate(FeedbackTemplate template) {
        return feedbackTemplateRepository.save(template);
    }
    
    public void deleteTemplate(Long id) {
        feedbackTemplateRepository.deleteById(id);
    }
    
    // Initialize default templates
    public void initializeDefaultTemplates() {
        if (feedbackTemplateRepository.count() == 0) {
            // Create a performance feedback template
            FeedbackTemplate performanceTemplate = new FeedbackTemplate();
            performanceTemplate.setName("Performance Feedback");
            performanceTemplate.setDescription("Template for providing performance feedback");
            performanceTemplate.setInstructions("Please provide specific examples to support your feedback.");
            performanceTemplate.setQuestions(List.of(
                "What did the person do well?",
                "What could the person improve?",
                "How can they develop in their role?",
                "What specific actions would you recommend?"
            ));
            feedbackTemplateRepository.save(performanceTemplate);
            
            // Create a project feedback template
            FeedbackTemplate projectTemplate = new FeedbackTemplate();
            projectTemplate.setName("Project Feedback");
            projectTemplate.setDescription("Template for providing feedback on project work");
            projectTemplate.setInstructions("Please be specific about the project contributions.");
            projectTemplate.setQuestions(List.of(
                "How effectively did the person contribute to the project?",
                "How well did they collaborate with the team?",
                "What skills did they demonstrate during the project?",
                "What recommendations do you have for future projects?"
            ));
            feedbackTemplateRepository.save(projectTemplate);
            
            // Create a SBI (Situation-Behavior-Impact) feedback template
            FeedbackTemplate sbiTemplate = new FeedbackTemplate();
            sbiTemplate.setName("SBI Framework");
            sbiTemplate.setDescription("Situation-Behavior-Impact feedback framework");
            sbiTemplate.setInstructions("Use this structured approach to provide clear and actionable feedback.");
            sbiTemplate.setQuestions(List.of(
                "Situation: Describe the specific situation",
                "Behavior: Describe the specific behaviors you observed",
                "Impact: Explain the impact of those behaviors",
                "Request: What specific change would you like to see?"
            ));
            feedbackTemplateRepository.save(sbiTemplate);
        }
    }
}