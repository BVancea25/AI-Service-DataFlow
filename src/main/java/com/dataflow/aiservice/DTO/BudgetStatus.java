package com.dataflow.aiservice.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetStatus(
        String budgetId,
        String categoryName,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        double progressPercentage,
        String status,
        String period,
        String currencyCode,
        LocalDate startDate
) {}