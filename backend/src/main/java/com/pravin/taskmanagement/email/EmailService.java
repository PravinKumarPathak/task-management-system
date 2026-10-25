package com.pravin.taskmanagement.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.pravin.taskmanagement.user.PasswordResetToken;
import com.pravin.taskmanagement.user.PasswordResetTokenRepository;
import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final RestClient restClient;
	private final UserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;

	@Value("${app.frontend.url}")
	private String frontendUrl;

	@Value("${app.mail.from-email}")
	private String fromEmail;

	public EmailService(
			@Value("${brevo.api.key}") String apiKey,
			UserRepository userRepository,
			PasswordResetTokenRepository tokenRepository) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		
		// Initialize RestClient targeting Brevo's HTTPS v3 API endpoint over port 443
		this.restClient = RestClient.builder()
				.baseUrl("https://api.brevo.com/v3")
				.defaultHeader("api-key", apiKey)
				.defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Async
	@Transactional
	public void processForgotPasswordRequest(String email) {
		Optional<User> userOptional = userRepository.findByEmail(email);
		if (userOptional.isEmpty()) {
			log.info("Password reset requested for non-existent email address: {}", email);
			return; // Fail silently to prevent User Enumeration attacks
		}

		User user = userOptional.get();

		String rawToken = UUID.randomUUID().toString();
		String hashedToken = hashToken(rawToken);

		PasswordResetToken resetToken = tokenRepository.findByUser(user).map(existingToken -> {
			existingToken.setTokenHash(hashedToken);
			existingToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
			return existingToken;
		}).orElseGet(() -> new PasswordResetToken(hashedToken, user, 15));

		tokenRepository.save(resetToken);

		String resetLink = frontendUrl + "/reset-password?token=" + rawToken;

		// Format HTML payload for Brevo API
		String subject = "Password Reset Request";
		String htmlContent = "<p>Click the following link to reset your password:</p>" +
				"<p><a href=\"" + resetLink + "\">Reset Password</a></p>" +
				"<p>This link expires in 15 minutes.</p>";

		Sender sender = new Sender("Task Management System", fromEmail);
		Recipient recipient = new Recipient(email, user.getUsername());
		BrevoPayload payload = new BrevoPayload(sender, List.of(recipient), subject, htmlContent);

		try {
			restClient.post()
					.uri("/smtp/email")
					.body(payload)
					.retrieve()
					.toBodilessEntity();
			
			log.info("Password reset email successfully sent via Brevo HTTPS API to {}", email);
		} catch (Exception e) {
			log.error("Brevo API Mail Sending Failed for {}: {}", email, e.getMessage(), e);
			
			// Safe console fallback for debugging or live demonstrations if needed
			log.info("FALLBACK PASSWORD RESET LINK FOR TESTING: {}", resetLink);
		}
	}

	public static String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 algorithm not available", e);
		}
	}

	// Modern Java records mapping directly to Brevo's API JSON payload structure
	private record BrevoPayload(Sender sender, List<Recipient> to, String subject, String htmlContent) {}
	private record Sender(String name, String email) {}
	private record Recipient(String email, String name) {}
}
