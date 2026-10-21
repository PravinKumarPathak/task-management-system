package com.pravin.taskmanagement.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pravin.taskmanagement.email.EmailService;
import com.pravin.taskmanagement.security.CustomUserDetailsService;
import com.pravin.taskmanagement.security.JwtSecurityConfiguration;
import com.pravin.taskmanagement.security.JwtTokenRequest;
import com.pravin.taskmanagement.security.JwtTokenService;
import com.pravin.taskmanagement.security.RateLimitingFilter;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(JwtSecurityConfiguration.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private EmailService emailService;

	@MockitoBean
	private JwtTokenService jwtTokenService;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RateLimitingFilter rateLimitingFilter;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@BeforeEach
	void setUp() throws Exception {
		// Ensure the mocked rate limiting filter continues the filter chain instead of
		// blocking requests
		doAnswer(invocation -> {
			jakarta.servlet.ServletRequest request = invocation.getArgument(0);
			jakarta.servlet.ServletResponse response = invocation.getArgument(1);
			jakarta.servlet.FilterChain chain = invocation.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(rateLimitingFilter).doFilter(any(), any(), any());
	}

	@Test
	void register_ShouldReturnCreatedStatus() throws Exception {
		RegisterRequest request = new RegisterRequest("pravin", "pravin@example.com", "password123");
		when(userService.registerUser(any(RegisterRequest.class))).thenReturn("User registered successfully");

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("User registered successfully"));
	}

	@Test
	void register_WhenInvalidInput_ShouldReturnBadRequest() throws Exception {
		// Missing username, invalid email, and password too short
		RegisterRequest request = new RegisterRequest("", "invalid-email", "123");

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void authenticate_ShouldReturnJwtTokenAndRoles() throws Exception {
		JwtTokenRequest request = new JwtTokenRequest("pravin", "password123");

		Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
		UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);

		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(userDetails);

		// Use doReturn to avoid strict generic collection capture mismatches
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();

		when(jwtTokenService.generateToken(any(Authentication.class))).thenReturn("mock-jwt-token");

		mockMvc.perform(post("/api/v1/auth/authenticate").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("mock-jwt-token"))
				.andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
	}

	@Test
	void forgotPassword_ShouldReturnSuccessMessage() throws Exception {
		ForgotPasswordRequest request = new ForgotPasswordRequest("pravin@example.com");

		mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(
						"If an account with that email exists, a password reset link has been sent to your inbox."));
	}

	@Test
	void forgotPassword_WhenInvalidEmail_ShouldReturnBadRequest() throws Exception {
		ForgotPasswordRequest request = new ForgotPasswordRequest("not-an-email");

		mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void resetPassword_ShouldReturnSuccessMessage() throws Exception {
		ResetPasswordRequest request = new ResetPasswordRequest("reset-token-123", "newPassword123");

		mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Password updated successfully. You can now log in."));
	}

	@Test
	void resetPassword_WhenInvalidInput_ShouldReturnBadRequest() throws Exception {
		// Blank token and password too short (< 6 characters)
		ResetPasswordRequest request = new ResetPasswordRequest("", "123");

		mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}
}