package com.feedback.service;

import com.feedback.model.User;
import com.feedback.repository.UserRepository;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public static final String CURRENT_USER_SESSION_ATTRIBUTE = "current_user";
    
    @Autowired
    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Authenticate user with email and password
     */
    public boolean authenticate(String email, String password) {
        System.out.println("AuthenticationService.authenticate() called with email: " + email);
        
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                System.out.println("User found: " + user.getFullName() + ", Active: " + user.isActive());
                
                if (user.isActive() && passwordEncoder.matches(password, user.getPassword())) {
                    System.out.println("Password matches for user: " + user.getFullName());
                    
                    // Update last login time
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    System.out.println("Updated last login time for user");
                    
                    // Store user in session
                    VaadinSession session = VaadinSession.getCurrent();
                    if (session != null) {
                        session.setAttribute(CURRENT_USER_SESSION_ATTRIBUTE, user);
                        System.out.println("User stored in session: " + user.getFullName());
                        
                        // Verify it was stored
                        User storedUser = (User) session.getAttribute(CURRENT_USER_SESSION_ATTRIBUTE);
                        if (storedUser != null) {
                            System.out.println("Verification: User found in session: " + storedUser.getFullName());
                        } else {
                            System.err.println("ERROR: User not found in session after storing!");
                        }
                    } else {
                        System.err.println("ERROR: VaadinSession is null!");
                    }
                    
                    // IMPORTANT: Also set Spring Security context
                    setSpringSecurityContext(user);
                    
                    return true;
                } else {
                    System.out.println("Password does not match or user is inactive");
                }
            } else {
                System.out.println("No user found with email: " + email);
            }
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Set Spring Security context for Vaadin integration
     */
    private void setSpringSecurityContext(User user) {
        try {
            // Create authority based on user's role
            String roleAuthority = "ROLE_" + user.getRole().getName();
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleAuthority);
            
            // Create authentication token
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), // principal
                null, // credentials (we don't store the password)
                Collections.singletonList(authority) // authorities
            );
            
            // Set in SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            
            System.out.println("Spring Security context set for user: " + user.getFullName() + " with role: " + roleAuthority);
            
        } catch (Exception e) {
            System.err.println("Error setting Spring Security context: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get currently authenticated user
     */
    public User getCurrentUser() {
        try {
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                User user = (User) session.getAttribute(CURRENT_USER_SESSION_ATTRIBUTE);
                if (user != null) {
                    System.out.println("getCurrentUser() returning: " + user.getFullName());
                    // Refresh user data from database to get latest info
                    Optional<User> freshUser = userRepository.findById(user.getId());
                    if (freshUser.isPresent()) {
                        System.out.println("Refreshed user data from database");
                        return freshUser.get();
                    } else {
                        System.err.println("User no longer exists in database!");
                        // Clear invalid session
                        session.setAttribute(CURRENT_USER_SESSION_ATTRIBUTE, null);
                        clearSpringSecurityContext();
                    }
                } else {
                    System.out.println("getCurrentUser() - no user in session");
                }
            } else {
                System.out.println("getCurrentUser() - no VaadinSession");
            }
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        boolean authenticated = getCurrentUser() != null;
        System.out.println("isAuthenticated() returning: " + authenticated);
        return authenticated;
    }
    
    /**
     * Check if current user is super admin
     */
    public boolean isCurrentUserSuperAdmin() {
        User currentUser = getCurrentUser();
        boolean isSuperAdmin = currentUser != null && currentUser.isSuperAdmin();
        System.out.println("isCurrentUserSuperAdmin() returning: " + isSuperAdmin);
        return isSuperAdmin;
    }
    
    /**
     * Logout current user
     */
    public void logout() {
        System.out.println("Logout called");
        try {
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                session.setAttribute(CURRENT_USER_SESSION_ATTRIBUTE, null);
                session.close();
                System.out.println("Session cleared and closed");
            } else {
                System.out.println("No session to logout");
            }
            
            // Also clear Spring Security context
            clearSpringSecurityContext();
            
        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear Spring Security context
     */
    private void clearSpringSecurityContext() {
        try {
            SecurityContextHolder.clearContext();
            System.out.println("Spring Security context cleared");
        } catch (Exception e) {
            System.err.println("Error clearing Spring Security context: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Change user password
     */
    public boolean changePassword(User user, String oldPassword, String newPassword) {
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    /**
     * Hash password for new user (used by super admin)
     */
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
    
    /**
     * Validate password strength
     */
    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
    
    /**
     * Check if user can manage other users (only super admin)
     */
    public boolean canManageUsers() {
        return isCurrentUserSuperAdmin();
    }
    
    /**
     * Check if user can view/edit specific user data
     */
    public boolean canAccessUser(User targetUser) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        // Super admin can access anyone
        if (currentUser.isSuperAdmin()) {
            return true;
        }
        
        // Users can only access their own data
        return currentUser.getId().equals(targetUser.getId());
    }
    
    /**
     * Check if user can give feedback to target user
     */
    public boolean canGiveFeedbackTo(User targetUser) {
        User currentUser = getCurrentUser();
        if (currentUser == null || targetUser == null) {
            return false;
        }
        
        // Users cannot give feedback to themselves
        if (currentUser.getId().equals(targetUser.getId())) {
            return false;
        }
        
        // All authenticated users can give feedback to others
        return true;
    }
    
    /**
     * Get user by email for authentication purposes
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}