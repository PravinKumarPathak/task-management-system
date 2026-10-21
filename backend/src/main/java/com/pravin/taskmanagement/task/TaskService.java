package com.pravin.taskmanagement.task;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	private final TaskMapper taskMapper;

	@Transactional(readOnly = true)
	public List<TaskResponse> getAllTasks() {
		List<Task> tasks = taskRepository.findAllWithUser();
		return taskMapper.toResponseList(tasks);
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> getUserTasks(String username) {
		List<Task> tasks = taskRepository.findByUserUsernameWithUser(username);
		return taskMapper.toResponseList(tasks);
	}

	@Transactional(readOnly = true)
	public Optional<TaskResponse> getTask(Long id, String username) {
		return taskRepository.findByIdAndUserUsernameWithUser(id, username).map(taskMapper::toResponse);
	}

	@Transactional
	public long deleteTask(Long id, String username) {
		return taskRepository.deleteByIdAndUserUsername(id, username);
	}

	@Transactional
	public Optional<TaskResponse> updateTask(Long id, String username, TaskRequest taskRequest) {
		return taskRepository.findByIdAndUserUsernameWithUser(id, username).map(existingTask -> {
			taskMapper.updateEntityFromRequest(existingTask, taskRequest);
			Task updatedTask = taskRepository.save(existingTask);
			return taskMapper.toResponse(updatedTask);
		});
	}

	@Transactional
	public TaskResponse createTask(String username, TaskRequest taskRequest) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		Task newTask = taskMapper.toEntity(taskRequest, user);
		Task savedTask = taskRepository.save(newTask);
		return taskMapper.toResponse(savedTask);
	}
}