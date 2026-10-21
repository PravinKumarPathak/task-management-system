package com.pravin.taskmanagement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.pravin.taskmanagement.user.User;
import com.pravin.taskmanagement.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService userDetailsService;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = User.builder().id(1L).username("pravin").email("pravin@tms.com").password("encodedPassword")
				.enabled(true).roles(Set.of("ROLE_USER")).build();
	}

	@Test
	void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
		// Arrange
		when(userRepository.findByUsername("pravin")).thenReturn(Optional.of(testUser));

		// Act
		UserDetails userDetails = userDetailsService.loadUserByUsername("pravin");

		// Assert
		assertNotNull(userDetails);
		assertEquals("pravin", userDetails.getUsername());
		assertEquals("encodedPassword", userDetails.getPassword());
		assertEquals(1, userDetails.getAuthorities().size());
		verify(userRepository, times(1)).findByUsername("pravin");
	}

	@Test
	void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
		// Arrange
		when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

		// Act & Assert
		UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
				() -> userDetailsService.loadUserByUsername("unknown"));

		assertEquals("User not found with username: unknown", exception.getMessage());
		verify(userRepository, times(1)).findByUsername("unknown");
	}
}