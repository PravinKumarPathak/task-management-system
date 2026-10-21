package com.pravin.taskmanagement.security;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pravin.taskmanagement.exception.ErrorDetails;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

	private final Map<String, Bucket> authCache = new ConcurrentHashMap<>();
	private final Map<String, Bucket> adminCache = new ConcurrentHashMap<>();
	private final Map<String, Bucket> generalCache = new ConcurrentHashMap<>();
	private final ObjectMapper objectMapper;

	public RateLimitingFilter() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	// Strict tier for Auth/Security: 5 requests per minute per IP
	private Bucket createAuthBucket() {
		return Bucket.builder()
				.addLimit(Bandwidth.builder().capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build()).build();
	}

	// Moderate tier for Admin operations: 20 requests per minute per IP
	private Bucket createAdminBucket() {
		return Bucket.builder()
				.addLimit(Bandwidth.builder().capacity(20).refillIntervally(20, Duration.ofMinutes(1)).build()).build();
	}

	// Standard tier for General API endpoints: 60 requests per minute per IP
	private Bucket createGeneralBucket() {
		return Bucket.builder()
				.addLimit(Bandwidth.builder().capacity(60).refillIntervally(60, Duration.ofMinutes(1)).build()).build();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String clientIp = request.getRemoteAddr();
		String uri = request.getRequestURI();

		Bucket bucket;
		String errorMessage;

		// Route requests to different rate-limit buckets based on path
		if (uri.contains("/api/v1/auth/")) {
			bucket = authCache.computeIfAbsent(clientIp, k -> createAuthBucket());
			errorMessage = "Too many authentication or password reset attempts. Please try again later.";
		} else if (uri.contains("/api/v1/admin/")) {
			bucket = adminCache.computeIfAbsent(clientIp, k -> createAdminBucket());
			errorMessage = "Too many admin requests. Please slow down.";
		} else {
			bucket = generalCache.computeIfAbsent(clientIp, k -> createGeneralBucket());
			errorMessage = "Too many requests - Rate limit exceeded. Please try again later.";
		}

		if (bucket.tryConsume(1)) {
			filterChain.doFilter(request, response);
		} else {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType("application/json");

			ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), errorMessage, request.getRequestURI(),
					HttpStatus.TOO_MANY_REQUESTS.value());

			response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
		}
	}
}