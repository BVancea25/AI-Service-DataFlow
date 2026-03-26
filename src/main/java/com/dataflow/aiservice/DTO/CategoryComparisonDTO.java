package com.dataflow.aiservice.DTO;

import java.math.BigDecimal;

public record CategoryComparisonDTO(
        TransactionType type,
        String category,
        BigDecimal total
) {}
