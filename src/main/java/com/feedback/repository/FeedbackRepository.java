package com.feedback.repository;

import com.feedback.model.Feedback;
import com.feedback.model.PrivacyLevel;
import com.feedback.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findBySenderId(Long senderId);
    List<Feedback> findByRecipientId(Long recipientId);
    
    // Get feedback that a user can see based on privacy settings
    @Query("SELECT f FROM Feedback f WHERE f.recipient.id = :userId OR f.sender.id = :userId OR " +
           "(f.privacyLevel = 'PUBLIC') OR " +
           "(f.privacyLevel = 'DEPARTMENT' AND f.recipient.department.id = " +
           "(SELECT u.department.id FROM User u WHERE u.id = :userId))")
    List<Feedback> findVisibleFeedbackForUser(Long userId);
    
    // Count unread feedback
    long countByRecipientIdAndIsReadFalse(Long recipientId);
}