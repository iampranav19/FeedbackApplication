package com.feedback.config;

import com.feedback.ui.views.login.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        System.out.println("Configuring Spring Security...");
        
        // Set the login view BEFORE calling super.configure()
        setLoginView(http, LoginView.class, "/login");
        
        // Important: Call super.configure(http) to set up Vaadin security
        // This handles most authorization rules automatically
        super.configure(http);
        
        // Allow H2 console in development (optional)
        http.headers(headers -> headers.frameOptions().sameOrigin());
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        
        // Additional configuration for custom authentication
        http.sessionManagement(session -> session.maximumSessions(1).maxSessionsPreventsLogin(false));
        
        System.out.println("Spring Security configuration completed");
    }
}