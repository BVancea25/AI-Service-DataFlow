package com.dataflow.aiservice.DTO;

import java.math.BigDecimal;

public record PaymentMethodBreakdownDTO(
        PaymentMethod paymentMethod,
        BigDecimal total
) {}
