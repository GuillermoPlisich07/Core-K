package com.konverza.reports.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Shared score-averaging logic — extracted from DashboardService so
 * UserActivityService (user-activity-detail-panel) reuses the same rounding
 * rule instead of duplicating it.
 */
public final class ScoreMath {

    private ScoreMath() {}

    public static BigDecimal average(List<BigDecimal> scores) {
        List<BigDecimal> nonNull = scores.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) return null;
        return nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(nonNull.size()), 2, RoundingMode.HALF_UP);
    }
}
