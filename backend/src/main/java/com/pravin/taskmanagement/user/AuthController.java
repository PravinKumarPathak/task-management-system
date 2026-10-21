package com.pravin.taskmanagement.user;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pravin.taskmanagement.email.EmailService;
import com.pravin.taskmanagement.security.JwtTokenRequest;
import com.pravin.taskmanagement.security.JwtTokenResponse;
import com.pravin.taskmanagement.security.JwtTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;
	private final EmailService emailService;
	private final JwtTokenService tokenService;
	private final AuthenticationManager authenticationManager;

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		String message = userService.registerUser(request);
		return new ResponseEntity<>(new RegisterResponse(message), HttpStatus.CREATED);
	}

	@PostMapping("/authenticate")
	public ResponseEntity<JwtTokenResponse> authenticate(@Valid @RequestBody JwtTokenRequest request) {
		var authenticationToken = new UsernamePasswordAuthenticationToken(request.username(), request.password());
		var authentication = authenticationManager.authenticate(authenticationToken);
		var token = tokenService.generateToken(authentication);

		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtTokenResponse(token, roles));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		emailService.processForgotPasswordRequest(request.email());
		return ResponseEntity.ok(Map.of("message",
				"If an account with that email exists, a password reset link has been sent to your inbox."));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		userService.resetPassword(request.token(), request.newPassword());
		return ResponseEntity.ok(Map.of("message", "Password updated successfully. You can now log in."));
	}
}