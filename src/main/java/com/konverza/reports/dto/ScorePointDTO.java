package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder @AllArgsConstructor
public class ScorePointDTO {
    private LocalDateTime date;
    private BigDecimal score;
}
