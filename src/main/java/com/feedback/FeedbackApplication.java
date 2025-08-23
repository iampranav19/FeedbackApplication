package com.feedback;

import org.springframework.boot.SpringApplication;

import com.feedback.service.FeedbackTemplateService;
import com.feedback.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FeedbackApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedbackApplication.class, args);
		System.out.println("+++++++++ Feedback Application Up and running ++++++++");
	}

	@Bean
	public CommandLineRunner initializeData(UserService userService, FeedbackTemplateService templateService) {
		return args -> {
			// Initialize default data when the application starts
			userService.initializeDefaultData();
			templateService.initializeDefaultTemplates();
		};
	}

}
