package com.pravin.taskmanagement.user;

import java.util.Set;

public record UserResponse(Long id, String username, String email, Set<String> roles) {
}