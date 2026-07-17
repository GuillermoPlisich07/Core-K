package com.konverza.scenarios.job;

import com.konverza.scenarios.entity.Scenario;
import com.konverza.scenarios.repository.ScenarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Auto-disables each Escenario Rápido 2 months after its creation
 * (scenario-privacy-and-lifecycle). Runs outside any authenticated request,
 * so it goes straight through the repository rather than
 * ScenarioService's CurrentUser-scoped methods.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioExpirationJob {

    private static final String EXPRESS_ORIGIN = "EXPRESS_AI";
    private static final int EXPIRATION_MONTHS = 2;

    private final ScenarioRepository scenarioRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void disableExpiredQuickScenarios() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(EXPIRATION_MONTHS);
        List<Scenario> expired = scenarioRepository.findByCreatedByAndEnabledTrueAndCreatedAtBefore(EXPRESS_ORIGIN, cutoff);
        if (expired.isEmpty()) return;

        expired.forEach(s -> s.setEnabled(false));
        scenarioRepository.saveAll(expired);
        log.info("Desactivados {} Escenarios Rápidos por expiración (>{} meses)", expired.size(), EXPIRATION_MONTHS);
    }
}
