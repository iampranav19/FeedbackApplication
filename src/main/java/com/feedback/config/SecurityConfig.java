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
        
        System.out.println("Spring Security configuration completed");
    }
}