package com.pravin.taskmanagement.security;

import java.util.List;

public record JwtTokenResponse(String token, List<String> roles) {
}