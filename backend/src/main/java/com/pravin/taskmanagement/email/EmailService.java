package com.pravin.taskmanagement.email;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pravin.taskmanagement.user.PasswordResetToken;
import com.pravin.taskmanagement.user.PasswordResetTokenRepository;
import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;
	private final UserRepository userRepository;
	private final PasswordResetTokenRepository tokenRepository;

	@Value("${app.frontend.url}")
	private String frontendUrl;

	@Value("${app.mail.from-email}")
	private String fromEmail;

	public EmailService(JavaMailSender mailSender, UserRepository userRepository,
			PasswordResetTokenRepository tokenRepository) {
		this.mailSender = mailSender;
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
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

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromEmail);
		message.setTo(email);
		message.setSubject("Password Reset Request");
		message.setText("Click the following link to reset your password:\n" + resetLink
				+ "\n\nThis link expires in 15 minutes.");

		try {
			mailSender.send(message);
			log.info("Password reset email successfully sent to {}", email);
		} catch (Exception e) {
			log.error("SMTP Mail Sending Failed for {}: {}", email, e.getMessage(), e);
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
}