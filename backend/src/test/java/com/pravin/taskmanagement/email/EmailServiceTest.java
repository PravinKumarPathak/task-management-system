package com.pravin.taskmanagement.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.pravin.taskmanagement.user.PasswordResetToken;
import com.pravin.taskmanagement.user.PasswordResetTokenRepository;
import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordResetTokenRepository tokenRepository;

	private EmailService emailService;

	private User testUser;

	@BeforeEach
	void setUp() {
		// Instantiate EmailService manually with a dummy API key to initialize RestClient
		emailService = new EmailService("xkeysib-test-dummy-key", userRepository, tokenRepository);

		// Inject @Value properties matching application.properties defaults
		ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
		ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");

		testUser = User.builder().id(1L).username("pravin").email("pravin@tms.com").build();
	}

	@Test
	void processForgotPasswordRequest_ShouldFailSilently_WhenEmailDoesNotExist() {
		// Arrange
		String unknownEmail = "ghost@tms.com";
		when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

		// Act
		emailService.processForgotPasswordRequest(unknownEmail);

		// Assert - Verify security guardrail (User Enumeration protection)
		verify(tokenRepository, never()).save(any());
	}

	@Test
	void processForgotPasswordRequest_ShouldSaveToken_WhenEmailExists() {
		// Arrange
		when(userRepository.findByEmail("pravin@tms.com")).thenReturn(Optional.of(testUser));
		when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

		// Act
		emailService.processForgotPasswordRequest("pravin@tms.com");

		// Assert
		verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
	}

	@Test
	void processForgotPasswordRequest_ShouldUpdateExistingToken_WhenTokenAlreadyExists() {
		// Arrange
		when(userRepository.findByEmail("pravin@tms.com")).thenReturn(Optional.of(testUser));

		PasswordResetToken existingToken = new PasswordResetToken();
		existingToken.setUser(testUser);
		existingToken.setTokenHash("old-hash");

		when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(existingToken));

		// Act
		emailService.processForgotPasswordRequest("pravin@tms.com");

		// Assert
		verify(tokenRepository, times(1)).save(existingToken);
	}

	@Test
	void hashToken_ShouldProduceValidSha256Hex() {
		// Act
		String hashed = EmailService.hashToken("my-secret-raw-token");

		// Assert
		assertNotNull(hashed);
		// SHA-256 hex string is always 64 characters long
		assertEquals(64, hashed.length());
	}
}
