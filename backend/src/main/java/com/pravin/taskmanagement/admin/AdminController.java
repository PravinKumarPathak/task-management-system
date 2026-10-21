package com.pravin.taskmanagement.admin;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pravin.taskmanagement.task.TaskResponse;
import com.pravin.taskmanagement.user.UserResponse;
import com.pravin.taskmanagement.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

	private final UserService userService;

	@GetMapping("/users")
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@GetMapping("/tasks")
	public ResponseEntity<List<TaskResponse>> getTasksByUsername(@RequestParam String username) {
		return ResponseEntity.ok(userService.getTasksByUsername(username));
	}

	@PutMapping("/users/{userId}/roles")
	public ResponseEntity<UserResponse> updateUserRoles(@PathVariable Long userId, @RequestBody Set<String> newRoles,
			Authentication authentication) {
		return ResponseEntity.ok(userService.updateUserRoles(userId, newRoles, authentication.getName()));
	}

	@DeleteMapping("/users/{userId}")
	public ResponseEntity<Void> deleteUserAccount(@PathVariable Long userId, Authentication authentication) {
		userService.deleteUserAccount(userId, authentication.getName());
		return ResponseEntity.noContent().build();
	}
}