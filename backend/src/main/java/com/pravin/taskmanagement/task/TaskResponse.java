package com.pravin.taskmanagement.task;

import java.time.LocalDate;

public record TaskResponse(Long id, String username, String taskDescription, LocalDate dueDate, String status) {
}