package com.almousleck.repository;

import com.almousleck.enums.UserStatus;
import com.almousleck.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByUsername(String username);
    Optional<User> findByUsernameOrPhoneNumber(String username, String phoneNumber);
    Optional<User> findUserByPhoneNumber(String phoneNumber);
    Optional<User> findByUsername(String identifier);
    Page<User> findByStatus(UserStatus status, Pageable pageable);
}
