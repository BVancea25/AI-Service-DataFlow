package com.dataflow.aiservice.DTO;

import java.math.BigDecimal;

public record DashboardKpiDTO(
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal net,
        BigDecimal savingsRate
) {}
