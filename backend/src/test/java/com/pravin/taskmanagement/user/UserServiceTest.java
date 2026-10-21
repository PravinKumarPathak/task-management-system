package com.pravin.taskmanagement.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pravin.taskmanagement.email.EmailService;
import com.pravin.taskmanagement.exception.ResourceNotFoundException;
import com.pravin.taskmanagement.task.TaskRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private PasswordResetTokenRepository tokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	private User testUser;
	private User adminUser;
	private User peerAdminUser;

	@BeforeEach
	void setUp() {
		testUser = User.builder().id(1L).username("pravin").email("pravin@tms.com").password("encodedPassword")
				.enabled(true).roles(Set.of("ROLE_USER")).build();

		adminUser = User.builder().id(2L).username("admin1").email("admin1@tms.com").password("encodedPassword")
				.enabled(true).roles(Set.of("ROLE_ADMIN", "ROLE_USER")).build();

		peerAdminUser = User.builder().id(3L).username("admin2").email("admin2@tms.com").password("encodedPassword")
				.enabled(true).roles(Set.of("ROLE_ADMIN", "ROLE_USER")).build();
	}

	@Test
	void getUserByUsername_ShouldReturnUser_WhenUserExists() {
		when(userRepository.findByUsername("pravin")).thenReturn(Optional.of(testUser));

		User foundUser = userService.getUserByUsername("pravin");

		assertNotNull(foundUser);
		assertEquals("pravin", foundUser.getUsername());
		verify(userRepository, times(1)).findByUsername("pravin");
	}

	@Test
	void getUserByUsername_ShouldThrowException_WhenUserNotFound() {
		when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> {
			userService.getUserByUsername("unknown");
		});
	}

	@Test
	void getUserProfile_ShouldReturnUserResponse_WhenUserExists() {
		when(userRepository.findByUsername("pravin")).thenReturn(Optional.of(testUser));

		UserResponse response = userService.getUserProfile("pravin");

		assertNotNull(response);
		assertEquals("pravin", response.username());
		assertEquals("pravin@tms.com", response.email());
	}

	@Test
	void getAllUsers_ShouldReturnListOfUserResponses() {
		when(userRepository.findAll()).thenReturn(List.of(testUser, adminUser));

		List<UserResponse> responses = userService.getAllUsers();

		assertNotNull(responses);
		assertEquals(2, responses.size());
		verify(userRepository, times(1)).findAll();
	}

	@Test
	void registerUser_ShouldSucceed_WhenValidRequest() {
		RegisterRequest request = new RegisterRequest("newuser", "new@tms.com", "Password123!");
		when(userRepository.existsByUsername("newuser")).thenReturn(false);
		when(userRepository.existsByEmail("new@tms.com")).thenReturn(false);
		when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");

		String result = userService.registerUser(request);

		assertEquals("User registered successfully!", result);
		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	void registerUser_ShouldThrowException_WhenUsernameAlreadyTaken() {
		RegisterRequest request = new RegisterRequest("pravin", "new@tms.com", "Password123!");
		when(userRepository.existsByUsername("pravin")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> {
			userService.registerUser(request);
		});
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void registerUser_ShouldThrowException_WhenEmailAlreadyInUse() {
		RegisterRequest request = new RegisterRequest("newuser", "pravin@tms.com", "Password123!");
		when(userRepository.existsByUsername("newuser")).thenReturn(false);
		when(userRepository.existsByEmail("pravin@tms.com")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> {
			userService.registerUser(request);
		});
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUserRoles_ShouldSucceed_WhenAdminUpdatesRegularUser() {
		when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
		when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		when(userRepository.save(any(User.class))).thenReturn(testUser);

		UserResponse response = userService.updateUserRoles(1L, Set.of("ROLE_USER", "ROLE_ADMIN"), "admin1");

		assertNotNull(response);
		verify(userRepository, times(1)).save(testUser);
	}

	@Test
	void updateUserRoles_ShouldThrowAccessDenied_WhenModifyingPeerAdmin() {
		when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
		when(userRepository.findById(3L)).thenReturn(Optional.of(peerAdminUser));

		assertThrows(AccessDeniedException.class, () -> {
			userService.updateUserRoles(3L, Set.of("ROLE_USER"), "admin1");
		});
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUserRoles_ShouldThrowIllegalState_WhenSoleAdminTriesToDemoteSelf() {
		when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
		when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
		when(userRepository.countByRolesContaining("ROLE_ADMIN")).thenReturn(1L);

		assertThrows(IllegalStateException.class, () -> {
			userService.updateUserRoles(2L, Set.of("ROLE_USER"), "admin1");
		});
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void deleteUserAccount_ShouldSucceed_WhenAdminDeletesRegularUser() {
		when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
		when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

		userService.deleteUserAccount(1L, "admin1");

		verify(userRepository, times(1)).delete(testUser);
	}

	@Test
	void deleteUserAccount_ShouldThrowAccessDenied_WhenDeletingPeerAdmin() {
		when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
		when(userRepository.findById(3L)).thenReturn(Optional.of(peerAdminUser));

		assertThrows(AccessDeniedException.class, () -> {
			userService.deleteUserAccount(3L, "admin1");
		});
		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	void deleteUserAccount_ShouldThrowIllegalState_WhenSoleAdminTriesToDeleteSelf() {
		when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
		when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
		when(userRepository.countByRolesContaining("ROLE_ADMIN")).thenReturn(1L);

		assertThrows(IllegalStateException.class, () -> {
			userService.deleteUserAccount(2L, "admin1");
		});
		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	void deleteOwnAccount_ShouldSucceed_WhenUserIsNotSoleAdmin() {
		when(userRepository.findByUsername("pravin")).thenReturn(Optional.of(testUser));

		userService.deleteOwnAccount("pravin");

		verify(userRepository, times(1)).delete(testUser);
	}

	@Test
	void deleteOwnAccount_ShouldThrowIllegalState_WhenSoleAdminTriesToSelfDelete() {
		when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(adminUser));
		when(userRepository.countByRolesContaining("ROLE_ADMIN")).thenReturn(1L);

		assertThrows(IllegalStateException.class, () -> {
			userService.deleteOwnAccount("admin1");
		});
		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	void getTasksByUsername_ShouldThrowResourceNotFound_WhenUserDoesNotExist() {
		when(userRepository.existsByUsername("ghost")).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> {
			userService.getTasksByUsername("ghost");
		});
	}

	@Test
	void getTasksByUsername_ShouldReturnTasks_WhenUserExists() {
		when(userRepository.existsByUsername("pravin")).thenReturn(true);
		when(taskRepository.findByUserUsernameWithUser("pravin")).thenReturn(List.of());

		var tasks = userService.getTasksByUsername("pravin");

		assertNotNull(tasks);
		verify(taskRepository, times(1)).findByUserUsernameWithUser("pravin");
	}

	@Test
	void resetPassword_ShouldSucceed_WhenTokenIsValid() {
		String rawToken = "valid-token";
		String hashedToken = EmailService.hashToken(rawToken);

		PasswordResetToken mockToken = PasswordResetToken.builder().tokenHash(hashedToken).user(testUser)
				.expiryDate(LocalDateTime.now().plusHours(1)).build();

		when(tokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(mockToken));
		when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");

		userService.resetPassword(rawToken, "NewPassword123!");

		verify(userRepository, times(1)).save(testUser);
		verify(tokenRepository, times(1)).delete(mockToken);
	}

	@Test
	void resetPassword_ShouldThrowException_WhenTokenNotFound() {
		String rawToken = "nonexistent-token";
		String hashedToken = EmailService.hashToken(rawToken);

		when(tokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> {
			userService.resetPassword(rawToken, "NewPassword123!");
		});

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void resetPassword_ShouldThrowException_WhenTokenIsExpired() {
		String rawToken = "expired-token";
		String hashedToken = EmailService.hashToken(rawToken);

		PasswordResetToken mockToken = PasswordResetToken.builder().tokenHash(hashedToken).user(testUser)
				.expiryDate(LocalDateTime.now().minusHours(1)).build();

		when(tokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(mockToken));

		assertThrows(IllegalArgumentException.class, () -> {
			userService.resetPassword(rawToken, "NewPassword123!");
		});

		verify(tokenRepository, times(1)).delete(mockToken);
		verify(userRepository, never()).save(any(User.class));
	}
}