package com.pravin.taskmanagement.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
class TaskRepositoryTest {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private User testUser;
	private Task testTask;

	@BeforeEach
	void setUp() {
		taskRepository.deleteAll();
		userRepository.deleteAll();

		testUser = User.builder().username("pravin").email("pravin@example.com").password("encodedPassword")
				.enabled(true).roles(Set.of("ROLE_USER")).build();
		testUser = userRepository.save(testUser);

		testTask = Task.builder().taskDescription("Complete Spring Boot testing suite")
				.dueDate(LocalDate.now().plusDays(5)).status("PENDING").user(testUser).build();
		testTask = taskRepository.save(testTask);

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void findAllWithUser_ShouldReturnAllTasksWithFetchedUser() {
		List<Task> tasks = taskRepository.findAllWithUser();

		assertThat(tasks).isNotEmpty();
		assertThat(tasks.get(0).getUser()).isNotNull();
		assertThat(tasks.get(0).getUser().getUsername()).isEqualTo("pravin");
	}

	@Test
	void findByUserUsernameWithUser_ShouldReturnTasksWithFetchedUser() {
		List<Task> tasks = taskRepository.findByUserUsernameWithUser("pravin");

		assertThat(tasks).isNotEmpty();
		assertThat(tasks.get(0).getTaskDescription()).isEqualTo("Complete Spring Boot testing suite");
		assertThat(tasks.get(0).getUser()).isNotNull();
		assertThat(tasks.get(0).getUser().getUsername()).isEqualTo("pravin");
	}

	@Test
	void findByIdAndUserUsernameWithUser_ShouldReturnTask_WhenIdAndUsernameMatch() {
		Optional<Task> foundTask = taskRepository.findByIdAndUserUsernameWithUser(testTask.getId(), "pravin");

		assertThat(foundTask).isPresent();
		assertThat(foundTask.get().getId()).isEqualTo(testTask.getId());
		assertThat(foundTask.get().getUser().getUsername()).isEqualTo("pravin");
	}

	@Test
	void findByIdAndUserUsernameWithUser_ShouldReturnEmpty_WhenUsernameDoesNotMatch() {
		Optional<Task> foundTask = taskRepository.findByIdAndUserUsernameWithUser(testTask.getId(), "wronguser");

		assertThat(foundTask).isEmpty();
	}

	@Test
	void deleteByIdAndUserUsername_ShouldRemoveTask() {
		long deletedCount = taskRepository.deleteByIdAndUserUsername(testTask.getId(), "pravin");

		assertThat(deletedCount).isEqualTo(1);
		assertThat(taskRepository.findById(testTask.getId())).isEmpty();
	}
}