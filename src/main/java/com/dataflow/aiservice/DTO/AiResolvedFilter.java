package com.dataflow.aiservice.DTO;

import java.time.LocalDate;

public record AiResolvedFilter(
        LocalDate from,
        LocalDate to,
        DataTimeMeasure timeMeasure,
        String currencyCode,
        String categoryId,
        String categoryName,
        PaymentMethod paymentMethod,
        TransactionType type
) {
}
