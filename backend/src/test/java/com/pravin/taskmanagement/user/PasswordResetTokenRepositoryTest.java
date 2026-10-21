package com.pravin.taskmanagement.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import jakarta.persistence.EntityManager;

@DataJpaTest
class PasswordResetTokenRepositoryTest {

	@Autowired
	private PasswordResetTokenRepository tokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private User testUser;
	private PasswordResetToken testToken;

	@BeforeEach
	void setUp() {
		tokenRepository.deleteAll();
		userRepository.deleteAll();

		testUser = User.builder().username("pravin").email("pravin@example.com").password("encodedPassword")
				.enabled(true).roles(Set.of("ROLE_USER")).build();
		testUser = userRepository.save(testUser);

		testToken = PasswordResetToken.builder().tokenHash("hashedtoken123").user(testUser)
				.expiryDate(LocalDateTime.now().plusMinutes(15)).build();
		testToken = tokenRepository.save(testToken);

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void findByTokenHash_ShouldReturnToken_WhenHashExists() {
		Optional<PasswordResetToken> found = tokenRepository.findByTokenHash("hashedtoken123");

		assertThat(found).isPresent();
		assertThat(found.get().getUser().getUsername()).isEqualTo("pravin");
	}

	@Test
	void findByUser_ShouldReturnToken_WhenUserMatches() {
		Optional<PasswordResetToken> found = tokenRepository.findByUser(testUser);

		assertThat(found).isPresent();
		assertThat(found.get().getTokenHash()).isEqualTo("hashedtoken123");
	}

	@Test
	void deleteByUser_ShouldRemoveTokenAssociatedWithUser() {
		tokenRepository.deleteByUser(testUser);
		entityManager.flush();
		entityManager.clear();

		Optional<PasswordResetToken> found = tokenRepository.findByTokenHash("hashedtoken123");
		assertThat(found).isEmpty();
	}
}