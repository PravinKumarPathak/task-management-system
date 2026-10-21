package com.pravin.taskmanagement.task;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.pravin.taskmanagement.security.CanAccessUserResource;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {

	private final TaskService taskService;

	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@GetMapping("/tasks")
	public ResponseEntity<List<TaskResponse>> getAllTasks() {
		return ResponseEntity.ok(taskService.getAllTasks());
	}

	@CanAccessUserResource
	@GetMapping("/users/{username}/tasks")
	public ResponseEntity<List<TaskResponse>> getUserTasks(@PathVariable String username) {
		return ResponseEntity.ok(taskService.getUserTasks(username));
	}

	@CanAccessUserResource
	@GetMapping("/users/{username}/tasks/{id}")
	public ResponseEntity<TaskResponse> getTask(@PathVariable String username, @PathVariable long id) {
		return taskService.getTask(id, username).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@CanAccessUserResource
	@DeleteMapping("/users/{username}/tasks/{id}")
	public ResponseEntity<Void> deleteTask(@PathVariable String username, @PathVariable long id) {
		long deletedCount = taskService.deleteTask(id, username);
		if (deletedCount == 0) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}

	@CanAccessUserResource
	@PutMapping("/users/{username}/tasks/{id}")
	public ResponseEntity<TaskResponse> updateTask(@PathVariable String username, @PathVariable long id,
			@RequestBody TaskRequest task) {
		return taskService.updateTask(id, username, task).map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@CanAccessUserResource
	@PostMapping("/users/{username}/tasks")
	public ResponseEntity<Void> createTask(@PathVariable String username, @RequestBody TaskRequest task) {
		TaskResponse createdTask = taskService.createTask(username, task);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdTask.id())
				.toUri();
		return ResponseEntity.created(uri).build();
	}
}