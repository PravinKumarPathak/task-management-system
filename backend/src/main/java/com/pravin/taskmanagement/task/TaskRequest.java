package com.pravin.taskmanagement.task;

import java.time.LocalDate;

public record TaskRequest(String taskDescription, LocalDate dueDate, String status) {
}