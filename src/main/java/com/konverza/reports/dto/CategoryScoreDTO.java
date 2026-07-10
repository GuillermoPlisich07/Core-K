package com.konverza.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder @AllArgsConstructor
public class CategoryScoreDTO {
    private String category;
    private BigDecimal avgScore;
}
