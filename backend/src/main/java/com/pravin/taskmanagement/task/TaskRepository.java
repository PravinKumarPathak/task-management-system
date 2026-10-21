package com.pravin.taskmanagement.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

	@Query("SELECT t FROM Task t JOIN FETCH t.user")
	List<Task> findAllWithUser();

	@Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.user.username = :username")
	List<Task> findByUserUsernameWithUser(@Param("username") String username);

	@Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.id = :id AND t.user.username = :username")
	Optional<Task> findByIdAndUserUsernameWithUser(@Param("id") Long id, @Param("username") String username);

	@Modifying
	long deleteByIdAndUserUsername(Long id, String username);
}