package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Builder @AllArgsConstructor
public class RecentSessionDTO {
    private UUID id;
    private String scenarioName;
    private BigDecimal score;
    private LocalDateTime startedAt;
    private Integer durationSeconds;
}
