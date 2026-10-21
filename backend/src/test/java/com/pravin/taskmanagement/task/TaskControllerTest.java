package com.pravin.taskmanagement.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pravin.taskmanagement.security.CustomUserDetailsService;
import com.pravin.taskmanagement.security.JwtSecurityConfiguration;
import com.pravin.taskmanagement.security.JwtTokenService;
import com.pravin.taskmanagement.security.RateLimitingFilter;

@WebMvcTest(controllers = TaskController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(JwtSecurityConfiguration.class)
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskService taskService;

	@MockitoBean
	private JwtTokenService jwtTokenService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RateLimitingFilter rateLimitingFilter;

	// Register JavaTimeModule so LocalDate can be serialized properly in tests
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@BeforeEach
	void setUp() throws Exception {
		UserDetails adminDetails = User.withUsername("admin").password("password").roles("ADMIN").build();

		UserDetails userDetails = User.withUsername("pravin").password("password").roles("USER").build();

		when(customUserDetailsService.loadUserByUsername("admin")).thenReturn(adminDetails);
		when(customUserDetailsService.loadUserByUsername("pravin")).thenReturn(userDetails);

		// Ensure the mocked filter continues the filter chain instead of blocking
		// requests
		doAnswer(invocation -> {
			jakarta.servlet.ServletRequest request = invocation.getArgument(0);
			jakarta.servlet.ServletResponse response = invocation.getArgument(1);
			jakarta.servlet.FilterChain chain = invocation.getArgument(2);
			chain.doFilter(request, response);
			return null;
		}).when(rateLimitingFilter).doFilter(any(), any(), any());
	}

	@Test
	void getAllTasks_WhenAdmin_ShouldReturnAllTasks() throws Exception {
		TaskResponse taskResponse = new TaskResponse(1L, "pravin", "Test Task", LocalDate.now(), "PENDING");
		when(taskService.getAllTasks()).thenReturn(List.of(taskResponse));

		mockMvc.perform(get("/api/v1/tasks").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[0].username").value("pravin"));
	}

	@Test
	void getAllTasks_WhenRegularUser_ShouldReturnForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/tasks").with(user("pravin").roles("USER"))).andExpect(status().isForbidden());
	}

	@Test
	void getUserTasks_ShouldReturnTasks() throws Exception {
		TaskResponse taskResponse = new TaskResponse(1L, "pravin", "Test Task", LocalDate.now(), "PENDING");
		when(taskService.getUserTasks("pravin")).thenReturn(List.of(taskResponse));

		mockMvc.perform(get("/api/v1/users/pravin/tasks").with(user("pravin").roles("USER"))).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[0].username").value("pravin"))
				.andExpect(jsonPath("$[0].taskDescription").value("Test Task"))
				.andExpect(jsonPath("$[0].status").value("PENDING"));
	}

	@Test
	void getUserTasks_WhenAdminAccessingUserResource_ShouldSucceed() throws Exception {
		TaskResponse taskResponse = new TaskResponse(1L, "pravin", "Test Task", LocalDate.now(), "PENDING");
		when(taskService.getUserTasks("pravin")).thenReturn(List.of(taskResponse));

		mockMvc.perform(get("/api/v1/users/pravin/tasks").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("pravin"));
	}

	@Test
	void getUserTasks_WhenAccessingOtherUserResource_ShouldReturnForbidden() throws Exception {
		mockMvc.perform(get("/api/v1/users/otheruser/tasks").with(user("pravin").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void getTask_WhenTaskExists_ShouldReturnTask() throws Exception {
		TaskResponse taskResponse = new TaskResponse(1L, "pravin", "Test Task", LocalDate.now(), "PENDING");
		when(taskService.getTask(1L, "pravin")).thenReturn(Optional.of(taskResponse));

		mockMvc.perform(get("/api/v1/users/pravin/tasks/1").with(user("pravin").roles("USER")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.username").value("pravin"))
				.andExpect(jsonPath("$.taskDescription").value("Test Task"));
	}

	@Test
	void getTask_WhenTaskNotFound_ShouldReturnNotFound() throws Exception {
		when(taskService.getTask(1L, "pravin")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/users/pravin/tasks/1").with(user("pravin").roles("USER")))
				.andExpect(status().isNotFound());
	}

	@Test
	void createTask_ShouldReturnCreatedStatus() throws Exception {
		TaskRequest request = new TaskRequest("New Task", LocalDate.now().plusDays(1), "PENDING");
		TaskResponse createdResponse = new TaskResponse(1L, "pravin", "New Task", request.dueDate(), request.status());

		when(taskService.createTask(eq("pravin"), any(TaskRequest.class))).thenReturn(createdResponse);

		mockMvc.perform(post("/api/v1/users/pravin/tasks").with(user("pravin").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated()).andExpect(header().exists("Location"));
	}

	@Test
	void updateTask_ShouldReturnUpdatedTask() throws Exception {
		TaskRequest request = new TaskRequest("Updated Task", LocalDate.now(), "COMPLETED");
		TaskResponse updatedResponse = new TaskResponse(1L, "pravin", "Updated Task", request.dueDate(),
				request.status());

		when(taskService.updateTask(eq(1L), eq("pravin"), any(TaskRequest.class)))
				.thenReturn(Optional.of(updatedResponse));

		mockMvc.perform(put("/api/v1/users/pravin/tasks/1").with(user("pravin").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.taskDescription").value("Updated Task"))
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void updateTask_WhenTaskNotFound_ShouldReturnNotFound() throws Exception {
		TaskRequest request = new TaskRequest("Updated Task", LocalDate.now(), "COMPLETED");

		when(taskService.updateTask(eq(1L), eq("pravin"), any(TaskRequest.class))).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/v1/users/pravin/tasks/1").with(user("pravin").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteTask_ShouldReturnNoContent() throws Exception {
		when(taskService.deleteTask(1L, "pravin")).thenReturn(1L);

		mockMvc.perform(delete("/api/v1/users/pravin/tasks/1").with(user("pravin").roles("USER")).with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void deleteTask_WhenTaskNotFound_ShouldReturnNotFound() throws Exception {
		when(taskService.deleteTask(1L, "pravin")).thenReturn(0L);

		mockMvc.perform(delete("/api/v1/users/pravin/tasks/1").with(user("pravin").roles("USER")).with(csrf()))
				.andExpect(status().isNotFound());
	}
}