package com.pravin.taskmanagement.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import jakarta.persistence.EntityManager;

@DataJpaTest
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private User testUser;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();

		testUser = User.builder().username("pravin").email("pravin@example.com").password("encodedPassword")
				.enabled(true).roles(Set.of("ROLE_ADMIN", "ROLE_USER")).build();
		userRepository.save(testUser);

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void findByUsername_ShouldReturnUser_WhenUsernameExists() {
		Optional<User> found = userRepository.findByUsername("pravin");

		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("pravin@example.com");
	}

	@Test
	void findByUsername_ShouldReturnEmpty_WhenUsernameDoesNotExist() {
		Optional<User> found = userRepository.findByUsername("nonexistent");
		assertThat(found).isEmpty();
	}

	@Test
	void findByEmail_ShouldReturnUser_WhenEmailExists() {
		Optional<User> found = userRepository.findByEmail("pravin@example.com");

		assertThat(found).isPresent();
		assertThat(found.get().getUsername()).isEqualTo("pravin");
	}

	@Test
	void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
		Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
		assertThat(found).isEmpty();
	}

	@Test
	void existsByUsername_ShouldReturnTrue_WhenUsernameExists() {
		boolean exists = userRepository.existsByUsername("pravin");
		assertThat(exists).isTrue();
	}

	@Test
	void existsByUsername_ShouldReturnFalse_WhenUsernameDoesNotExist() {
		boolean exists = userRepository.existsByUsername("nonexistent");
		assertThat(exists).isFalse();
	}

	@Test
	void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
		boolean exists = userRepository.existsByEmail("pravin@example.com");
		assertThat(exists).isTrue();
	}

	@Test
	void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
		boolean exists = userRepository.existsByEmail("nonexistent@example.com");
		assertThat(exists).isFalse();
	}

	@Test
	void countByRolesContaining_ShouldReturnCorrectCount() {
		long adminCount = userRepository.countByRolesContaining("ROLE_ADMIN");
		assertThat(adminCount).isEqualTo(1);

		long nonExistentRoleCount = userRepository.countByRolesContaining("ROLE_NON_EXISTENT");
		assertThat(nonExistentRoleCount).isZero();
	}
}