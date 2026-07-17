package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Escenario Completo completion status for a specific user (user-activity-detail-panel). */
@Getter @Builder @AllArgsConstructor
public class FullScenarioActivityDTO {
    private UUID id;
    private String name;
    private boolean completed;
    private LocalDateTime lastCompletedAt;
}
