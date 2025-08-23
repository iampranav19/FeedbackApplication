package com.feedback.repository;

import com.feedback.model.FeedbackTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackTemplateRepository extends JpaRepository<FeedbackTemplate, Long> {
    List<FeedbackTemplate> findByIsActiveTrue();
}