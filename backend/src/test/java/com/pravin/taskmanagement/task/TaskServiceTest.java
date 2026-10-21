package com.pravin.taskmanagement.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private TaskMapper taskMapper;

	@InjectMocks
	private TaskService taskService;

	private User testUser;
	private Task testTask;
	private TaskResponse testTaskResponse;
	private TaskRequest testTaskRequest;

	@BeforeEach
	void setUp() {
		testUser = User.builder().id(1L).username("rohan").email("rohan@tms.com").build();

		testTask = Task.builder().id(1L).user(testUser).taskDescription("Test Task").dueDate(LocalDate.of(2026, 12, 31))
				.status("PENDING").build();

		testTaskResponse = new TaskResponse(1L, "rohan", "Test Task", LocalDate.of(2026, 12, 31), "PENDING");

		testTaskRequest = new TaskRequest("Updated Task Description", LocalDate.of(2026, 12, 31), "IN_PROGRESS");
	}

	@Test
	void getAllTasks_ShouldReturnAllTasksList() {
		// Arrange
		when(taskRepository.findAllWithUser()).thenReturn(List.of(testTask));
		when(taskMapper.toResponseList(any())).thenReturn(List.of(testTaskResponse));

		// Act
		List<TaskResponse> responses = taskService.getAllTasks();

		// Assert
		assertNotNull(responses);
		assertEquals(1, responses.size());
		assertEquals("Test Task", responses.get(0).taskDescription());
		verify(taskRepository, times(1)).findAllWithUser();
	}

	@Test
	void getUserTasks_ShouldReturnTaskList() {
		// Arrange
		when(taskRepository.findByUserUsernameWithUser("rohan")).thenReturn(List.of(testTask));
		when(taskMapper.toResponseList(any())).thenReturn(List.of(testTaskResponse));

		// Act
		List<TaskResponse> responses = taskService.getUserTasks("rohan");

		// Assert
		assertNotNull(responses);
		assertEquals(1, responses.size());
		assertEquals("Test Task", responses.get(0).taskDescription());
		verify(taskRepository, times(1)).findByUserUsernameWithUser("rohan");
	}

	@Test
	void getTask_ShouldReturnTask_WhenTaskExists() {
		// Arrange
		when(taskRepository.findByIdAndUserUsernameWithUser(1L, "rohan")).thenReturn(Optional.of(testTask));
		when(taskMapper.toResponse(testTask)).thenReturn(testTaskResponse);

		// Act
		Optional<TaskResponse> response = taskService.getTask(1L, "rohan");

		// Assert
		assertTrue(response.isPresent());
		assertEquals("Test Task", response.get().taskDescription());
		verify(taskRepository, times(1)).findByIdAndUserUsernameWithUser(1L, "rohan");
	}

	@Test
	void getTask_ShouldReturnEmpty_WhenTaskNotFound() {
		// Arrange
		when(taskRepository.findByIdAndUserUsernameWithUser(99L, "rohan")).thenReturn(Optional.empty());

		// Act
		Optional<TaskResponse> response = taskService.getTask(99L, "rohan");

		// Assert
		assertFalse(response.isPresent());
		verify(taskRepository, times(1)).findByIdAndUserUsernameWithUser(99L, "rohan");
	}

	@Test
	void createTask_ShouldSaveAndReturnTask_WhenUserExists() {
		// Arrange
		when(userRepository.findByUsername("rohan")).thenReturn(Optional.of(testUser));
		when(taskMapper.toEntity(testTaskRequest, testUser)).thenReturn(testTask);
		when(taskRepository.save(testTask)).thenReturn(testTask);
		when(taskMapper.toResponse(testTask)).thenReturn(testTaskResponse);

		// Act
		TaskResponse response = taskService.createTask("rohan", testTaskRequest);

		// Assert
		assertNotNull(response);
		assertEquals("Test Task", response.taskDescription());
		verify(userRepository, times(1)).findByUsername("rohan");
		verify(taskRepository, times(1)).save(testTask);
	}

	@Test
	void createTask_ShouldThrowException_WhenUserNotFound() {
		// Arrange
		when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

		// Act & Assert
		UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
				() -> taskService.createTask("unknown", testTaskRequest));

		assertEquals("User not found: unknown", exception.getMessage());
		verify(userRepository, times(1)).findByUsername("unknown");
	}

	@Test
	void updateTask_ShouldUpdateAndReturnTask_WhenTaskExists() {
		// Arrange
		when(taskRepository.findByIdAndUserUsernameWithUser(1L, "rohan")).thenReturn(Optional.of(testTask));
		when(taskRepository.save(testTask)).thenReturn(testTask);
		when(taskMapper.toResponse(testTask)).thenReturn(testTaskResponse);

		// Act
		Optional<TaskResponse> response = taskService.updateTask(1L, "rohan", testTaskRequest);

		// Assert
		assertTrue(response.isPresent());
		verify(taskRepository, times(1)).findByIdAndUserUsernameWithUser(1L, "rohan");
		verify(taskRepository, times(1)).save(testTask);
		verify(taskMapper, times(1)).updateEntityFromRequest(testTask, testTaskRequest);
	}

	@Test
	void updateTask_ShouldReturnEmpty_WhenTaskNotFound() {
		// Arrange
		when(taskRepository.findByIdAndUserUsernameWithUser(99L, "rohan")).thenReturn(Optional.empty());

		// Act
		Optional<TaskResponse> response = taskService.updateTask(99L, "rohan", testTaskRequest);

		// Assert
		assertFalse(response.isPresent());
		verify(taskRepository, times(1)).findByIdAndUserUsernameWithUser(99L, "rohan");
	}

	@Test
	void deleteTask_ShouldExecuteDeletion() {
		// Arrange
		when(taskRepository.deleteByIdAndUserUsername(1L, "rohan")).thenReturn(1L);

		// Act
		long deletedCount = taskService.deleteTask(1L, "rohan");

		// Assert
		assertEquals(1L, deletedCount);
		verify(taskRepository, times(1)).deleteByIdAndUserUsername(1L, "rohan");
	}
}