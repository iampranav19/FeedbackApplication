package com.feedback.service;

import com.feedback.model.Feedback;
import com.feedback.model.User;
import com.feedback.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    
    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }
    
    public List<Feedback> findAllFeedback() {
        return feedbackRepository.findAll();
    }
    
    public Optional<Feedback> findById(Long id) {
        return feedbackRepository.findById(id);
    }
    
    public List<Feedback> findFeedbackSentByUser(Long userId) {
        return feedbackRepository.findBySenderId(userId);
    }
    
    public List<Feedback> findFeedbackReceivedByUser(Long userId) {
        return feedbackRepository.findByRecipientId(userId);
    }
    
    public List<Feedback> findVisibleFeedbackForUser(Long userId) {
        return feedbackRepository.findVisibleFeedbackForUser(userId);
    }
    
    public long countUnreadFeedback(Long userId) {
        return feedbackRepository.countByRecipientIdAndIsReadFalse(userId);
    }
    
    public Feedback saveFeedback(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }
    
    public void markAsRead(Long feedbackId) {
        feedbackRepository.findById(feedbackId).ifPresent(feedback -> {
            feedback.setRead(true);
            feedbackRepository.save(feedback);
        });
    }
    
    public void updateFeedbackStatus(Long feedbackId, String status) {
        feedbackRepository.findById(feedbackId).ifPresent(feedback -> {
            feedback.setStatus(status);
            feedbackRepository.save(feedback);
        });
    }
    
    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
    }
}