package com.feedback.service;

import com.feedback.model.User;
import com.feedback.repository.UserRepository;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public static final String CURRENT_USER_SESSION_ATTRIBUTE = "current_user";
    
    @Autowired
    public AuthenticationService(UserRepository userRepository, 
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Authenticate user with email and password using VaadinSession
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
                    
                    // Store user in VaadinSession (this is what actually works)
                    VaadinSession session = VaadinSession.getCurrent();
                    if (session != null) {
                        session.setAttribute(CURRENT_USER_SESSION_ATTRIBUTE, user);
                        System.out.println("User stored in VaadinSession: " + user.getFullName());
                        
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
     * Get currently authenticated user from VaadinSession
     * FIXED: Removed database refresh logic that was causing session clearing
     */
    public User getCurrentUser() {
        try {
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                User user = (User) session.getAttribute(CURRENT_USER_SESSION_ATTRIBUTE);
                if (user != null) {
                    System.out.println("getCurrentUser() returning: " + user.getFullName());
                    return user; // Return user from session directly - no database refresh
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
     * Get fresh user data from database (separate method)
     */
    public User getFreshUserData() {
        User sessionUser = getCurrentUser();
        if (sessionUser != null) {
            try {
                Optional<User> freshUser = userRepository.findById(sessionUser.getId());
                if (freshUser.isPresent()) {
                    System.out.println("Refreshed user data from database");
                    // Update the session with fresh data
                    VaadinSession session = VaadinSession.getCurrent();
                    if (session != null) {
                        session.setAttribute(CURRENT_USER_SESSION_ATTRIBUTE, freshUser.get());
                    }
                    return freshUser.get();
                } else {
                    System.err.println("User no longer exists in database!");
                    // Don't clear session automatically - let caller handle this
                }
            } catch (Exception e) {
                System.err.println("Error refreshing user data: " + e.getMessage());
            }
        }
        return sessionUser; // Return session user if database refresh fails
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
        System.out.println("isCurrentUserSuperAdmin() returning: " + isSuperAdmin + 
                          (currentUser != null ? " (role: " + currentUser.getRole().getName() + ")" : ""));
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
                System.out.println("VaadinSession cleared and closed");
            } else {
                System.out.println("No VaadinSession to logout");
            }
        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());
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
     * Check if user can manage other users (only super admin and admin)
     */
    public boolean canManageUsers() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;
        
        String roleName = currentUser.getRole().getName();
        boolean canManage = "SUPER_ADMIN".equals(roleName) || "ADMIN".equals(roleName);
        System.out.println("canManageUsers() for role " + roleName + ": " + canManage);
        return canManage;
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