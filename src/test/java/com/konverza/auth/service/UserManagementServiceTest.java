package com.konverza.auth.service;

import com.konverza.auth.dto.ChangePasswordRequest;
import com.konverza.auth.dto.CreateUserRequest;
import com.konverza.auth.dto.UpdateUserRequest;
import com.konverza.auth.entity.User;
import com.konverza.auth.exception.EmailAlreadyExistsException;
import com.konverza.auth.exception.InvalidCurrentPasswordException;
import com.konverza.auth.exception.UserNotFoundException;
import com.konverza.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AvatarStorageService avatarStorageService;

    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(userRepository, passwordEncoder, avatarStorageService);
    }

    @Test
    @DisplayName("findAll returns every user")
    void findAll_returnsAllUsers() {
        List<User> users = List.of(
                User.builder().id(UUID.randomUUID()).email("a@konverza.com").role(User.Role.EMPLOYEE).build(),
                User.builder().id(UUID.randomUUID()).email("b@konverza.com").role(User.Role.ADMIN).build()
        );
        when(userRepository.findAll()).thenReturn(users);

        assertThat(userManagementService.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("create saves a new user with an encoded password")
    void create_newEmail_savesUser() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("nuevo@konverza.com");
        req.setPassword("plain-text-password");
        req.setRole(User.Role.EMPLOYEE);

        when(userRepository.findByEmailIgnoreCase("nuevo@konverza.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-text-password")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userManagementService.create(req);

        assertThat(created.getEmail()).isEqualTo("nuevo@konverza.com");
        assertThat(created.getPasswordHash()).isEqualTo("hashed");
        assertThat(created.getRole()).isEqualTo(User.Role.EMPLOYEE);
        assertThat(created.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("create rejects a duplicate email")
    void create_duplicateEmail_throws() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("existente@konverza.com");
        req.setPassword("x");
        req.setRole(User.Role.EMPLOYEE);

        when(userRepository.findByEmailIgnoreCase("existente@konverza.com"))
                .thenReturn(Optional.of(User.builder().build()));

        assertThatThrownBy(() -> userManagementService.create(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("update changes role and enabled status")
    void update_existingUser_updatesRoleAndEnabled() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("x@konverza.com").role(User.Role.EMPLOYEE).enabled(true).build();
        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(User.Role.ADMIN);
        req.setEnabled(false);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userManagementService.update(id, req);

        assertThat(updated.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("update applies a new email when it isn't taken by another account")
    void update_newUniqueEmail_updatesEmail() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("old@konverza.com").role(User.Role.EMPLOYEE).enabled(true).build();
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("new@konverza.com");
        req.setRole(User.Role.EMPLOYEE);
        req.setEnabled(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailIgnoreCase("new@konverza.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userManagementService.update(id, req);

        assertThat(updated.getEmail()).isEqualTo("new@konverza.com");
    }

    @Test
    @DisplayName("update rejects an email already used by another account")
    void update_emailTakenByAnotherUser_throws() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("old@konverza.com").role(User.Role.EMPLOYEE).enabled(true).build();
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("taken@konverza.com");
        req.setRole(User.Role.EMPLOYEE);
        req.setEnabled(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailIgnoreCase("taken@konverza.com"))
                .thenReturn(Optional.of(User.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> userManagementService.update(id, req))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("update sets a new password hash directly, without checking the current password")
    void update_withPassword_setsEncodedHashDirectly() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("x@konverza.com").role(User.Role.EMPLOYEE).enabled(true).build();
        UpdateUserRequest req = new UpdateUserRequest();
        req.setPassword("nueva-contrasena");
        req.setRole(User.Role.EMPLOYEE);
        req.setEnabled(true);

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("nueva-contrasena")).thenReturn("hashed-nueva");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userManagementService.update(id, req);

        assertThat(updated.getPasswordHash()).isEqualTo("hashed-nueva");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("changeOwnPassword updates the hash when the current password matches")
    void changeOwnPassword_correctCurrentPassword_updatesHash() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("x@konverza.com").passwordHash("old-hash").role(User.Role.EMPLOYEE).build();
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("actual");
        req.setNewPassword("nueva-contrasena");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("actual", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("nueva-contrasena")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userManagementService.changeOwnPassword(id, req);

        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    @DisplayName("changeOwnPassword rejects an incorrect current password")
    void changeOwnPassword_wrongCurrentPassword_throws() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("x@konverza.com").passwordHash("old-hash").role(User.Role.EMPLOYEE).build();
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("incorrecta");
        req.setNewPassword("nueva-contrasena");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("incorrecta", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> userManagementService.changeOwnPassword(id, req))
                .isInstanceOf(InvalidCurrentPasswordException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadOwnAvatar stores the file and persists the returned URL")
    void uploadOwnAvatar_storesAndPersistsUrl() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("x@konverza.com").role(User.Role.EMPLOYEE).build();

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(avatarStorageService.store(eq(id), any())).thenReturn("/uploads/avatars/" + id + ".png");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userManagementService.uploadOwnAvatar(id, null);

        assertThat(updated.getAvatarUrl()).isEqualTo("/uploads/avatars/" + id + ".png");
    }

    @Test
    @DisplayName("update throws for a nonexistent user")
    void update_missingUser_throws() {
        UUID id = UUID.randomUUID();
        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(User.Role.ADMIN);
        req.setEnabled(true);

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.update(id, req))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("delete removes an existing user")
    void delete_existingUser_removesIt() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).email("x@konverza.com").role(User.Role.EMPLOYEE).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        userManagementService.delete(id);

        verify(userRepository).delete(existing);
    }

    @Test
    @DisplayName("delete throws for a nonexistent user")
    void delete_missingUser_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.delete(id))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).delete(any());
    }
}
