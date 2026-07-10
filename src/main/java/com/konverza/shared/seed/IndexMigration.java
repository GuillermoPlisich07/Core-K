package com.konverza.shared.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class IndexMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final String[] INDICES = {
        "CREATE INDEX IF NOT EXISTS idx_transcripts_session  ON transcripts(session_id)",
        "CREATE INDEX IF NOT EXISTS idx_biometrics_session   ON biometric_samples(session_id)",
        "CREATE INDEX IF NOT EXISTS idx_sessions_started     ON sessions(started_at DESC)",
        "CREATE INDEX IF NOT EXISTS idx_sessions_status      ON sessions(status)",
    };

    @Override
    public void run(String... args) {
        log.info("Aplicando índices PostgreSQL...");
        for (String sql : INDICES) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.warn("No se pudo aplicar índice: {}", e.getMessage());
            }
        }
        log.info("Índices PostgreSQL verificados.");
    }
}
