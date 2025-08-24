package com.feedback.repository;

import com.feedback.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByManagerId(Long managerId);
    List<User> findByDepartmentId(Long departmentId);
    List<User> findByIsActiveTrue();
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}