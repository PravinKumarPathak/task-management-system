package com.pravin.taskmanagement.task;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.pravin.taskmanagement.user.User;

@Component
public class TaskMapper {

	/**
	 * Converts an incoming TaskRequest DTO and the associated User entity into a
	 * JPA Task Entity using Lombok Builder.
	 */
	public Task toEntity(TaskRequest request, User user) {
		if (request == null) {
			return null;
		}

		return Task.builder().user(user).taskDescription(request.taskDescription()).dueDate(request.dueDate())
				.status(request.status()).build();
	}

	/**
	 * Updates an existing Task JPA Entity with new values from a TaskRequest DTO.
	 */
	public void updateEntityFromRequest(Task task, TaskRequest request) {
		if (task == null || request == null) {
			return;
		}

		task.setTaskDescription(request.taskDescription());
		task.setDueDate(request.dueDate());
		task.setStatus(request.status());
	}

	/**
	 * Maps a Task JPA Entity to a TaskResponse DTO for API outputs.
	 */
	public TaskResponse toResponse(Task task) {
		if (task == null) {
			return null;
		}

		return new TaskResponse(task.getId(), task.getUser() != null ? task.getUser().getUsername() : null,
				task.getTaskDescription(), task.getDueDate(), task.getStatus());
	}

	/**
	 * Maps a list of Task Entities to a list of TaskResponse DTOs.
	 */
	public List<TaskResponse> toResponseList(List<Task> taskList) {
		if (taskList == null || taskList.isEmpty()) {
			return Collections.emptyList();
		}

		return taskList.stream().map(this::toResponse).toList();
	}
}