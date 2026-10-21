package com.pravin.taskmanagement.user;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pravin.taskmanagement.security.CustomUserDetailsService;
import com.pravin.taskmanagement.security.JwtSecurityConfiguration;
import com.pravin.taskmanagement.security.JwtTokenService;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(JwtSecurityConfiguration.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtTokenService jwtTokenService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@BeforeEach
	void setUp() {
		UserDetails userDetails = User.withUsername("pravin").password("password").roles("USER").build();

		when(customUserDetailsService.loadUserByUsername("pravin")).thenReturn(userDetails);
	}

	@Test
	void getCurrentUser_ShouldReturnUserProfile() throws Exception {
		UserResponse mockResponse = new UserResponse(1L, "pravin", "pravin@example.com", Set.of("ROLE_USER"));

		when(userService.getUserProfile("pravin")).thenReturn(mockResponse);

		mockMvc.perform(get("/api/v1/users/me").with(user("pravin").roles("USER"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("pravin"))
				.andExpect(jsonPath("$.email").value("pravin@example.com"));
	}

	@Test
	void deleteOwnAccount_ShouldReturnNoContent() throws Exception {
		doNothing().when(userService).deleteOwnAccount("pravin");

		mockMvc.perform(delete("/api/v1/users/me").with(user("pravin").roles("USER")))
				.andExpect(status().isNoContent());
	}

	@Test
	void deleteOwnAccount_WhenSoleAdmin_ShouldReturnBadRequest() throws Exception {
		doThrow(new IllegalStateException("Cannot delete your account: You are the sole administrator in the system."))
				.when(userService).deleteOwnAccount("pravin");

		mockMvc.perform(delete("/api/v1/users/me").with(user("pravin").roles("ADMIN")))
				.andExpect(status().isBadRequest());
	}
}