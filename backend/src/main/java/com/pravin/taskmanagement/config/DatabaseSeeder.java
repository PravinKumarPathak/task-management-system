package com.pravin.taskmanagement.config;

import java.time.LocalDate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pravin.taskmanagement.task.Task;
import com.pravin.taskmanagement.task.TaskRepository;
import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

@Configuration
public class DatabaseSeeder {

	private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

	// Primary Admin (i.e., Pravin) Properties
	@Value("${app.seed.admin.username}")
	private String adminUsername;

	@Value("${app.seed.admin.email}")
	private String adminEmail;

	@Value("${app.seed.admin.password}")
	private String adminPassword;

	// Alex Properties
	@Value("${app.seed.alex.username}")
	private String alexUsername;

	@Value("${app.seed.alex.email}")
	private String alexEmail;

	@Value("${app.seed.alex.password}")
	private String alexPassword;

	// Rohan Properties
	@Value("${app.seed.rohan.username}")
	private String rohanUsername;

	@Value("${app.seed.rohan.email}")
	private String rohanEmail;

	@Value("${app.seed.rohan.password}")
	private String rohanPassword;

	// Sam Properties
	@Value("${app.seed.sam.username}")
	private String samUsername;

	@Value("${app.seed.sam.email}")
	private String samEmail;

	@Value("${app.seed.sam.password}")
	private String samPassword;

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository, TaskRepository taskRepository,
			PasswordEncoder passwordEncoder) {
		return args -> {
			// 1. Seed Configurable Primary Admin idempotently
			User admin = userRepository.findByUsername(adminUsername).orElseGet(() -> {
				User newAdmin = User.builder().username(adminUsername).email(adminEmail)
						.password(passwordEncoder.encode(adminPassword)).enabled(true)
						.roles(Set.of("ROLE_ADMIN", "ROLE_USER")).build();
				log.info("Seeding primary administrator: {} ({})", adminUsername, adminEmail);
				return userRepository.save(newAdmin);
			});

			// 2. Seed Additional Development Users idempotently with unique configs
			User alex = userRepository.findByUsername(alexUsername).orElseGet(() -> {
				log.info("Seeding development user: {} ({})", alexUsername, alexEmail);
				return userRepository.save(User.builder().username(alexUsername).email(alexEmail)
						.password(passwordEncoder.encode(alexPassword)).enabled(true)
						.roles(Set.of("ROLE_ADMIN", "ROLE_USER")).build());
			});

			User rohan = userRepository.findByUsername(rohanUsername).orElseGet(() -> {
				log.info("Seeding development user: {} ({})", rohanUsername, rohanEmail);
				return userRepository.save(User.builder().username(rohanUsername).email(rohanEmail)
						.password(passwordEncoder.encode(rohanPassword)).enabled(true).roles(Set.of("ROLE_USER"))
						.build());
			});

			User sam = userRepository.findByUsername(samUsername).orElseGet(() -> {
				log.info("Seeding development user: {} ({})", samUsername, samEmail);
				return userRepository.save(User.builder().username(samUsername).email(samEmail)
						.password(passwordEncoder.encode(samPassword)).enabled(true).roles(Set.of("ROLE_USER"))
						.build());
			});

			// 3. Seed Production-Grade Engineering Tasks with Dynamic Relative Dates if
			// none exist
			if (taskRepository.count() == 0) {
				taskRepository.save(Task.builder().user(admin).taskDescription(
						"Integrate Spring Security, Bucket4j rate limiting, and SMTP email service for secure authentication")
						.status("IN_PROGRESS").dueDate(LocalDate.now().plusDays(3)).build());

				taskRepository.save(Task.builder().user(alex).taskDescription(
						"Implement custom JPQL queries with JOIN FETCH optimizations and derived repository methods")
						.status("PENDING").dueDate(LocalDate.now().plusDays(7)).build());

				taskRepository.save(Task.builder().user(rohan)
						.taskDescription("Implement Angular HTTP Interceptors for JWT and global error handling")
						.status("COMPLETED").dueDate(LocalDate.now().minusDays(5)).build());

				taskRepository.save(Task.builder().user(sam).taskDescription(
						"Execute comprehensive backend and frontend testing including JUnit/Mockito and Vitest unit/component validation")
						.status("IN_PROGRESS").dueDate(LocalDate.now().plusDays(4)).build());

				log.info("Successfully seeded database with professional enterprise-grade tasks using relative dates.");
			}
		};
	}
}