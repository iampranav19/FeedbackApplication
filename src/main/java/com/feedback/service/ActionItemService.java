package com.feedback.service;

import com.feedback.model.ActionItem;
import com.feedback.repository.ActionItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ActionItemService {
    
    private final ActionItemRepository actionItemRepository;
    
    @Autowired
    public ActionItemService(ActionItemRepository actionItemRepository) {
        this.actionItemRepository = actionItemRepository;
    }
    
    public List<ActionItem> findAllActionItems() {
        return actionItemRepository.findAll();
    }
    
    public Optional<ActionItem> findById(Long id) {
        return actionItemRepository.findById(id);
    }
    
    public List<ActionItem> findActionItemsByUser(Long userId) {
        return actionItemRepository.findByAssignedToId(userId);
    }
    
    public List<ActionItem> findActionItemsByFeedback(Long feedbackId) {
        return actionItemRepository.findByFeedbackId(feedbackId);
    }
    
    public List<ActionItem> findActionItemsByStatus(String status) {
        return actionItemRepository.findByStatus(status);
    }
    
    public long countActiveActionItems(Long userId) {
        return actionItemRepository.countByAssignedToIdAndStatusNot(userId, "Completed");
    }
    
    public ActionItem saveActionItem(ActionItem actionItem) {
        return actionItemRepository.save(actionItem);
    }
    
    public void completeActionItem(Long id) {
        actionItemRepository.findById(id).ifPresent(actionItem -> {
            actionItem.setStatus("Completed");
            actionItem.setCompletedAt(LocalDateTime.now());
            actionItemRepository.save(actionItem);
        });
    }
    
    public void updateActionItemStatus(Long id, String status) {
        actionItemRepository.findById(id).ifPresent(actionItem -> {
            actionItem.setStatus(status);
            if (status.equals("Completed")) {
                actionItem.setCompletedAt(LocalDateTime.now());
            } else {
                actionItem.setCompletedAt(null);
            }
            actionItemRepository.save(actionItem);
        });
    }
    
    public void deleteActionItem(Long id) {
        actionItemRepository.deleteById(id);
    }
}