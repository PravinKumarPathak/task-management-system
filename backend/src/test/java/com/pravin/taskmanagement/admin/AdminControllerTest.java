package com.pravin.taskmanagement.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravin.taskmanagement.security.CustomUserDetailsService;
import com.pravin.taskmanagement.security.JwtSecurityConfiguration;
import com.pravin.taskmanagement.security.JwtTokenService;
import com.pravin.taskmanagement.task.TaskResponse;
import com.pravin.taskmanagement.user.UserResponse;
import com.pravin.taskmanagement.user.UserService;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(JwtSecurityConfiguration.class)
class AdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtTokenService jwtTokenService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		UserDetails adminDetails = User.withUsername("admin").password("password").roles("ADMIN").build();

		UserDetails userDetails = User.withUsername("user").password("password").roles("USER").build();

		when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(adminDetails);
		when(customUserDetailsService.loadUserByUsername("user")).thenReturn(userDetails);
	}

	@Test
	void getAllUsers_WhenUserIsAdmin_ShouldReturnUserList() throws Exception {
		UserResponse userResponse = new UserResponse(1L, "pravin", "pravin@example.com", Set.of("ROLE_ADMIN"));
		when(userService.getAllUsers()).thenReturn(List.of(userResponse));

		mockMvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("pravin"))
				.andExpect(jsonPath("$[0].email").value("pravin@example.com"));
	}

	@Test
	void getAllUsers_WhenUserIsRegularUser_ShouldReturnForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users").with(user("user").roles("USER"))).andExpect(status().isForbidden());
	}

	@Test
	void getTasksByUsername_WhenAdmin_ShouldReturnTaskList() throws Exception {
		TaskResponse taskResponse = new TaskResponse(1L, "targetUser", "Complete project documentation", null,
				"PENDING");
		when(userService.getTasksByUsername("targetUser")).thenReturn(List.of(taskResponse));

		mockMvc.perform(get("/api/v1/admin/tasks").param("username", "targetUser").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].taskDescription").value("Complete project documentation"))
				.andExpect(jsonPath("$[0].username").value("targetUser"));
	}

	@Test
	void updateUserRoles_WhenAdmin_ShouldReturnUpdatedUser() throws Exception {
		Set<String> newRoles = Set.of("ROLE_USER", "ROLE_ADMIN");
		UserResponse updatedResponse = new UserResponse(1L, "targetUser", "target@example.com", newRoles);

		when(userService.updateUserRoles(eq(1L), any(), eq("admin"))).thenReturn(updatedResponse);

		mockMvc.perform(put("/api/v1/admin/users/1/roles").with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(newRoles)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.roles").isArray());
	}

	@Test
	void deleteUserAccount_WhenAdmin_ShouldReturnNoContent() throws Exception {
		doNothing().when(userService).deleteUserAccount(eq(1L), eq("admin"));

		mockMvc.perform(delete("/api/v1/admin/users/1").with(user("admin").roles("ADMIN")))
				.andExpect(status().isNoContent());
	}
}