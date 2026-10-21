package com.pravin.taskmanagement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

	private JwtTokenService jwtTokenService;

	// A valid 256-bit base64-encoded secret key for HS256
	private final String testSecret = "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3R0ZXN0aW5ncHVycG9zZXNhdGxlYXN0MjU5Ynl0ZXM=";
	private final long testExpiration = 3600000; // 1 hour

	private UserDetails userDetails;

	@BeforeEach
	void setUp() {
		jwtTokenService = new JwtTokenService();

		// Inject private @Value fields using ReflectionTestUtils for pure unit testing
		ReflectionTestUtils.setField(jwtTokenService, "secretKey", testSecret);
		ReflectionTestUtils.setField(jwtTokenService, "jwtExpiration", testExpiration);

		userDetails = new User("pravin", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void generateToken_ShouldReturnValidToken() {
		// Act
		String token = jwtTokenService.generateToken(userDetails);

		// Assert
		assertNotNull(token);
		assertFalse(token.isBlank());
	}

	@Test
	void generateToken_ShouldReturnValidToken_WhenAuthenticationObjectProvided() {
		// Arrange
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

		// Act
		String token = jwtTokenService.generateToken(auth);

		// Assert
		assertNotNull(token);
		assertEquals("pravin", jwtTokenService.extractUsername(token));
	}

	@Test
	void extractUsername_ShouldReturnCorrectUsername() {
		// Arrange
		String token = jwtTokenService.generateToken(userDetails);

		// Act
		String extractedUsername = jwtTokenService.extractUsername(token);

		// Assert
		assertEquals("pravin", extractedUsername);
	}

	@Test
	void isTokenValid_ShouldReturnTrue_WhenTokenIsValid() {
		// Arrange
		String token = jwtTokenService.generateToken(userDetails);

		// Act
		boolean isValid = jwtTokenService.isTokenValid(token, userDetails);

		// Assert
		assertTrue(isValid);
	}

	@Test
	void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
		// Arrange
		String token = jwtTokenService.generateToken(userDetails);
		UserDetails differentUser = new User("otheruser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

		// Act
		boolean isValid = jwtTokenService.isTokenValid(token, differentUser);

		// Assert
		assertFalse(isValid);
	}

	@Test
	void isTokenValid_ShouldReturnFalse_WhenTokenIsExpired() {
		// Arrange: Temporarily set expiration to a negative value so the token is
		// instantly expired
		ReflectionTestUtils.setField(jwtTokenService, "jwtExpiration", -1000L);
		String expiredToken = jwtTokenService.generateToken(userDetails);

		// Act
		boolean isValid = jwtTokenService.isTokenValid(expiredToken, userDetails);

		// Assert
		assertFalse(isValid);
	}

	@Test
	void isTokenValid_ShouldReturnFalse_WhenTokenIsMalformed() {
		// Arrange
		String malformedToken = "header.invalidpayload.signature";

		// Act
		boolean isValid = jwtTokenService.isTokenValid(malformedToken, userDetails);

		// Assert
		assertFalse(isValid);
	}

	@Test
	void extractClaim_ShouldContainCorrectRoles() {
		// Arrange
		String token = jwtTokenService.generateToken(userDetails);

		// Act
		List<?> roles = jwtTokenService.extractClaim(token, claims -> claims.get("roles", List.class));

		// Assert
		assertNotNull(roles);
		assertTrue(roles.contains("ROLE_USER"));
	}
}