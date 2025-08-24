package com.feedback.config;

import com.feedback.model.User;
import com.feedback.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("CustomUserDetailsService: Loading user by email: " + email);
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            System.out.println("CustomUserDetailsService: User not found: " + email);
            throw new UsernameNotFoundException("User not found: " + email);
        }
        
        User user = userOptional.get();
        System.out.println("CustomUserDetailsService: User found: " + user.getFullName() + 
                          ", Active: " + user.isActive() + ", Role: " + user.getRole().getName());
        
        if (!user.isActive()) {
            System.out.println("CustomUserDetailsService: User is inactive: " + email);
            throw new UsernameNotFoundException("User is inactive: " + email);
        }
        
        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Create authority from user role
        String roleAuthority = "ROLE_" + user.getRole().getName();
        GrantedAuthority authority = new SimpleGrantedAuthority(roleAuthority);
        
        System.out.println("CustomUserDetailsService: Returning UserDetails for: " + 
                          user.getFullName() + " with authority: " + roleAuthority);
        
        // Return Spring Security UserDetails implementation
        return new CustomUserDetails(user, Collections.singletonList(authority));
    }
    
    /**
     * Custom UserDetails implementation that wraps our User entity
     */
    public static class CustomUserDetails implements UserDetails {
        private final User user;
        private final java.util.Collection<? extends GrantedAuthority> authorities;
        
        public CustomUserDetails(User user, java.util.Collection<? extends GrantedAuthority> authorities) {
            this.user = user;
            this.authorities = authorities;
        }
        
        public User getUser() {
            return user;
        }
        
        @Override
        public java.util.Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }
        
        @Override
        public String getPassword() {
            return user.getPassword();
        }
        
        @Override
        public String getUsername() {
            return user.getEmail();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return true;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            return user.isActive();
        }
        
        @Override
        public String toString() {
            return "CustomUserDetails{user=" + user.getFullName() + ", authorities=" + authorities + "}";
        }
    }
}