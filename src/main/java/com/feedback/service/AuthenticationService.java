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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public static final String CURRENT_USER_ID_SESSION_ATTRIBUTE = "current_user_id";
    
    @Autowired
    public AuthenticationService(UserRepository userRepository, 
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Authenticate user with both custom session and Spring Security
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
                    
                    // Store user ID in VaadinSession
                    VaadinSession session = VaadinSession.getCurrent();
                    if (session != null) {
                        session.setAttribute(CURRENT_USER_ID_SESSION_ATTRIBUTE, user.getId());
                        System.out.println("User ID stored in VaadinSession: " + user.getId());
                    }
                    
                    // IMPORTANT: Also authenticate with Spring Security
                    authenticateWithSpringSecurity(user);
                    
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
     * Authenticate the user with Spring Security
     */
    private void authenticateWithSpringSecurity(User user) {
        try {
            // Create Spring Security authority from user role
            String roleAuthority = "ROLE_" + user.getRole().getName();
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleAuthority));
            
            // Create Spring Security authentication token
            Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), 
                null, // Don't store password in auth token
                authorities
            );
            
            // Set the authentication in Spring Security context
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // Store in HTTP session so it persists across requests
            SecurityContext securityContext = SecurityContextHolder.getContext();
            VaadinSession vaadinSession = VaadinSession.getCurrent();
            if (vaadinSession != null && vaadinSession.getSession() != null) {
                vaadinSession.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                    securityContext
                );
            }
            
            System.out.println("Spring Security authentication set for: " + user.getEmail() + 
                              " with authority: " + roleAuthority);
            
        } catch (Exception e) {
            System.err.println("Error setting Spring Security authentication: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get currently authenticated user by fetching from database using stored ID
     */
    public User getCurrentUser() {
        try {
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                Long userId = (Long) session.getAttribute(CURRENT_USER_ID_SESSION_ATTRIBUTE);
                if (userId != null) {
                    Optional<User> userOptional = userRepository.findById(userId);
                    if (userOptional.isPresent()) {
                        return userOptional.get();
                    } else {
                        System.err.println("getCurrentUser() - user ID " + userId + " no longer exists in database!");
                        // Clear invalid session
                        session.setAttribute(CURRENT_USER_ID_SESSION_ATTRIBUTE, null);
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    System.out.println("getCurrentUser() - no user ID in session");
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
        try {
            // Check both custom authentication and Spring Security
            boolean customAuth = getCurrentUser() != null;
            boolean springAuth = SecurityContextHolder.getContext().getAuthentication() != null &&
                                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                                !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser");
            
            System.out.println("isAuthenticated() - Custom: " + customAuth + ", Spring: " + springAuth);
            return customAuth && springAuth;
        } catch (Exception e) {
            System.err.println("Error checking authentication: " + e.getMessage());
            return false;
        }
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
     * Logout current user from both systems
     */
    public void logout() {
        System.out.println("Logout called");
        try {
            // Clear custom session
            VaadinSession session = VaadinSession.getCurrent();
            if (session != null) {
                session.setAttribute(CURRENT_USER_ID_SESSION_ATTRIBUTE, null);
            }
            
            // Clear Spring Security context
            SecurityContextHolder.clearContext();
            
            // Clear from HTTP session
            if (session != null && session.getSession() != null) {
                session.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                    null
                );
                session.close();
            }
            
            System.out.println("Logout completed - cleared both custom and Spring Security sessions");
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
     * Check if user can access specific user data
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