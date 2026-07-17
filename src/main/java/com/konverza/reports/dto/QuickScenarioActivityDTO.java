package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Name + aggregate metrics only for an Escenario Rápido on the per-user
 * activity panel (user-activity-detail-panel) — deliberately excludes
 * content fields (systemPrompt, objectionsGuide, forbiddenPhrases, faq) and
 * transcripts, per scenario-lifecycle's narrow ADMIN/EXEC exception.
 */
@Getter @Builder @AllArgsConstructor
public class QuickScenarioActivityDTO {
    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private boolean enabled;
    private long sessionCount;
    private BigDecimal avgScore;
}
