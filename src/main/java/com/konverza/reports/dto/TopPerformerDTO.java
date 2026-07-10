package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder @AllArgsConstructor
public class TopPerformerDTO {
    private String email;
    private BigDecimal avgScore;
    private long sessionCount;
}
