package com.konverza.shared.seed;

import com.konverza.auth.entity.User;
import com.konverza.auth.repository.UserRepository;
import com.konverza.sessions.entity.Session;
import com.konverza.sessions.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Best-effort backfill for {@code sessions.user_id} (add-rbac-permission-matrix).
 * User has no display-name field, so this only matches a session's free-text
 * vendorName against a user's email local-part exactly (e.g. vendorName
 * "vendedor" matches "vendedor@konverza.com") — deliberately conservative to
 * avoid mis-attributing a session to the wrong account. Rows that don't match
 * stay unassigned and are only visible to ADMIN/EXEC (see Session.user).
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class SessionUserBackfill implements CommandLineRunner {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        List<Session> unassigned = sessionRepository.findAllByUserIsNull();
        if (unassigned.isEmpty()) {
            return;
        }

        Map<String, User> byEmailLocalPart = new HashMap<>();
        for (User user : userRepository.findAll()) {
            String localPart = user.getEmail().split("@")[0].toLowerCase();
            byEmailLocalPart.put(localPart, user);
        }

        int matched = 0;
        for (Session session : unassigned) {
            String vendorName = session.getVendorName();
            if (vendorName == null || vendorName.isBlank()) {
                continue;
            }
            String key = vendorName.trim().toLowerCase().replace(" ", "");
            User match = byEmailLocalPart.get(key);
            if (match != null) {
                session.setUser(match);
                sessionRepository.save(session);
                matched++;
            }
        }
        log.info("Backfill sessions.user_id: {} de {} sesiones vinculadas a un usuario existente " +
                "(el resto queda sin asignar, visible solo para ADMIN/EXEC).", matched, unassigned.size());
    }
}
