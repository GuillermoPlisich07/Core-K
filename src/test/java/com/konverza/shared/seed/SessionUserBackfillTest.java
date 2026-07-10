package com.konverza.shared.seed;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.repository.SessionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionUserBackfillTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private UserRepository userRepository;

    private SessionUserBackfill backfill;

    @BeforeEach
    void setUp() {
        backfill = new SessionUserBackfill(sessionRepository, userRepository);
    }

    private User user(String email) {
        return User.builder().id(UUID.randomUUID()).email(email).role(User.Role.EMPLOYEE).build();
    }

    private Session sessionWithVendorName(String vendorName) {
        return Session.builder().id(UUID.randomUUID()).vendorName(vendorName)
                .status(Session.Status.COMPLETED).build();
    }

    @Test
    @DisplayName("does nothing when there are no unassigned sessions")
    void run_noUnassignedSessions_savesNothing() {
        when(sessionRepository.findAllByUserIsNull()).thenReturn(List.of());

        backfill.run();

        verify(sessionRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("links a session whose vendorName exactly matches a user's email local part")
    void run_vendorNameMatchesEmailLocalPart_linksUser() {
        User vendedor = user("vendedor@konverza.com");
        Session session = sessionWithVendorName("vendedor");

        when(sessionRepository.findAllByUserIsNull()).thenReturn(List.of(session));
        when(userRepository.findAll()).thenReturn(List.of(vendedor));

        backfill.run();

        assertThat(session.getUser()).isEqualTo(vendedor);
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("matching is case-insensitive and ignores spaces")
    void run_vendorNameDifferentCase_stillMatches() {
        User admin = user("admin@konverza.com");
        Session session = sessionWithVendorName("  Admin  ");

        when(sessionRepository.findAllByUserIsNull()).thenReturn(List.of(session));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        backfill.run();

        assertThat(session.getUser()).isEqualTo(admin);
    }

    @Test
    @DisplayName("leaves a session unassigned when vendorName matches no user")
    void run_vendorNameNoMatch_leavesUnassigned() {
        User admin = user("admin@konverza.com");
        Session session = sessionWithVendorName("Carlos Perez");

        when(sessionRepository.findAllByUserIsNull()).thenReturn(List.of(session));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        backfill.run();

        assertThat(session.getUser()).isNull();
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("skips sessions with a blank vendorName")
    void run_blankVendorName_skipsWithoutError() {
        Session session = sessionWithVendorName("  ");

        when(sessionRepository.findAllByUserIsNull()).thenReturn(List.of(session));
        when(userRepository.findAll()).thenReturn(List.of(user("admin@konverza.com")));

        backfill.run();

        assertThat(session.getUser()).isNull();
        verify(sessionRepository, never()).save(any());
    }
}
