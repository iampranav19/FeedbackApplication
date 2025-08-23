package com.feedback.repository;

import com.feedback.model.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findByAssignedToId(Long userId);
    List<ActionItem> findByFeedbackId(Long feedbackId);
    List<ActionItem> findByStatus(String status);
    long countByAssignedToIdAndStatusNot(Long userId, String notStatus);
}