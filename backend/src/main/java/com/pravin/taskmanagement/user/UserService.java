package com.pravin.taskmanagement.user;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pravin.taskmanagement.email.EmailService;
import com.pravin.taskmanagement.exception.ResourceNotFoundException;
import com.pravin.taskmanagement.task.Task;
import com.pravin.taskmanagement.task.TaskRepository;
import com.pravin.taskmanagement.task.TaskResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final String ROLE_ADMIN = "ROLE_ADMIN";

	private final UserRepository userRepository;
	private final TaskRepository taskRepository;
	private final PasswordResetTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public String registerUser(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new IllegalArgumentException("Username is already taken!");
		}

		if (userRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("Email is already in use!");
		}

		User newUser = User.builder().username(request.username()).email(request.email())
				.password(passwordEncoder.encode(request.password())).enabled(true)
				.roles(Collections.singleton("ROLE_USER")).build();

		userRepository.save(newUser);
		return "User registered successfully!";
	}

	@Transactional(readOnly = true)
	public User getUserByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
	}

	@Transactional(readOnly = true)
	public UserResponse getUserProfile(String username) {
		User user = getUserByUsername(username);
		return mapToUserResponse(user);
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(this::mapToUserResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> getTasksByUsername(String username) {
		if (!userRepository.existsByUsername(username)) {
			throw new ResourceNotFoundException("User not found with username: " + username);
		}

		List<Task> tasks = taskRepository.findByUserUsernameWithUser(username);
		return tasks.stream().map(this::mapToTaskResponse).toList();
	}

	@Transactional
	public UserResponse updateUserRoles(Long targetUserId, Set<String> newRoles, String currentAdminUsername) {
		User currentAdmin = getUserByUsername(currentAdminUsername);
		User targetUser = userRepository.findById(targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

		boolean isSelf = targetUser.getId().equals(currentAdmin.getId());
		boolean isTargetAdmin = targetUser.getRoles().contains(ROLE_ADMIN);
		boolean willBeAdmin = newRoles.contains(ROLE_ADMIN);

		if (!isSelf && isTargetAdmin) {
			throw new AccessDeniedException("Admins cannot modify roles of peer administrators.");
		}

		if (isSelf && isTargetAdmin && !willBeAdmin) {
			long adminCount = userRepository.countByRolesContaining(ROLE_ADMIN);
			if (adminCount <= 1) {
				throw new IllegalStateException(
						"Cannot revoke your admin role because you are the sole remaining administrator.");
			}
		}

		targetUser.setRoles(newRoles);
		return mapToUserResponse(userRepository.save(targetUser));
	}

	@Transactional
	public void deleteOwnAccount(String username) {
		User user = getUserByUsername(username);

		if (user.getRoles() != null && user.getRoles().contains(ROLE_ADMIN)) {
			long adminCount = userRepository.countByRolesContaining(ROLE_ADMIN);
			if (adminCount <= 1) {
				throw new IllegalStateException(
						"Cannot delete your account: You are the sole administrator in the system.");
			}
		}

		userRepository.delete(user);
	}

	@Transactional
	public void deleteUserAccount(Long targetUserId, String currentAdminUsername) {
		User currentAdmin = getUserByUsername(currentAdminUsername);
		User targetUser = userRepository.findById(targetUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

		boolean isSelfDelete = targetUser.getId().equals(currentAdmin.getId());
		boolean isTargetAdmin = targetUser.getRoles().contains(ROLE_ADMIN);

		if (!isSelfDelete && isTargetAdmin) {
			throw new AccessDeniedException("Admins cannot delete peer administrator accounts.");
		}

		if (isSelfDelete && isTargetAdmin) {
			long adminCount = userRepository.countByRolesContaining(ROLE_ADMIN);
			if (adminCount <= 1) {
				throw new IllegalStateException(
						"Cannot delete your account because you are the sole remaining administrator.");
			}
		}

		userRepository.delete(targetUser);
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		String hashedToken = EmailService.hashToken(rawToken);
		PasswordResetToken token = tokenRepository.findByTokenHash(hashedToken)
				.orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset link."));

		if (token.isExpired()) {
			tokenRepository.delete(token);
			throw new IllegalArgumentException("Invalid or expired password reset link.");
		}

		User user = token.getUser();
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
		tokenRepository.delete(token);
	}

	private UserResponse mapToUserResponse(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
	}

	private TaskResponse mapToTaskResponse(Task task) {
		return new TaskResponse(task.getId(), task.getUser() != null ? task.getUser().getUsername() : null,
				task.getTaskDescription(), task.getDueDate(),
				task.getStatus() != null ? task.getStatus().toString() : null);
	}
}