package com.feedback.config;

import com.feedback.ui.views.login.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        System.out.println("Configuring Spring Security for Vaadin...");
        
        // IMPORTANT: Call super.configure(http) FIRST to set up Vaadin security
        super.configure(http);
        
        // Set the login view
        setLoginView(http, LoginView.class);
        
        // Allow H2 console in development (optional)
        http.headers(headers -> headers.frameOptions().sameOrigin());
        
        // Additional session configuration to work with our integrated approach
        http.sessionManagement(session -> 
            session.maximumSessions(1)
                   .maxSessionsPreventsLogin(false)
                   .sessionRegistry(sessionRegistry())
        );
        
        System.out.println("Spring Security configuration completed");
    }
    
    @org.springframework.context.annotation.Bean
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }
}