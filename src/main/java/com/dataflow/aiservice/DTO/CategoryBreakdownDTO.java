package com.dataflow.aiservice.DTO;

import java.math.BigDecimal;

public record CategoryBreakdownDTO(
        String label,
        BigDecimal value
) {}
