package com.dataflow.aiservice.DTO;

import jakarta.annotation.Nullable;

public record AiToolRequest(
        AiPeriod period,
        @Nullable
        String currencyCode,
        @Nullable
        String categoryName,
        @Nullable
        String paymentMethod
) {
}
