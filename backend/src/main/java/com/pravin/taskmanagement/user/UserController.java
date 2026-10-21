package com.pravin.taskmanagement.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
		UserResponse response = userService.getUserProfile(authentication.getName());
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteOwnAccount(Authentication authentication) {
		userService.deleteOwnAccount(authentication.getName());
		return ResponseEntity.noContent().build();
	}
}