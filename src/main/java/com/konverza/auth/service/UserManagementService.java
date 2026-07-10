package com.konverza.auth.service;

import com.konverza.auth.dto.CreateUserRequest;
import com.konverza.auth.dto.UpdateProfileRequest;
import com.konverza.auth.dto.UpdateUserRequest;
import com.konverza.auth.entity.User;
import com.konverza.auth.exception.EmailAlreadyExistsException;
import com.konverza.auth.exception.UserNotFoundException;
import com.konverza.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * User account management — kept separate from {@link AuthService}, which
 * owns login/refresh/logout only. Every method here is Administrador-only per
 * the permission matrix (add-rbac-permission-matrix); enforced via
 * {@code @PreAuthorize} on UserController, not here — this service trusts its
 * caller the same way ScenarioService/SessionService do.
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User create(CreateUserRequest req) {
        userRepository.findByEmailIgnoreCase(req.getEmail()).ifPresent(u -> {
            throw new EmailAlreadyExistsException(req.getEmail());
        });
        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User update(UUID id, UpdateUserRequest req) {
        User user = findById(id);
        user.setRole(req.getRole());
        user.setEnabled(req.getEnabled());
        return userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    /**
     * Self-service profile update — scoped to the caller's own row via
     * {@code id} sourced from {@link com.konverza.auth.security.CurrentUser},
     * never a path parameter, so it cannot act on another user's account
     * (kept separate from Admin-only {@link #update}, which acts on any user
     * by id — see design.md's "Self-service profile endpoint" decision).
     */
    @Transactional
    public User updateOwnProfile(UUID id, UpdateProfileRequest req) {
        User user = findById(id);
        user.setAge(req.getAge());
        user.setPersonality(req.getPersonality());
        user.setSelfDescription(req.getSelfDescription());
        user.setProfileCompleted(true);
        return userRepository.save(user);
    }
}
