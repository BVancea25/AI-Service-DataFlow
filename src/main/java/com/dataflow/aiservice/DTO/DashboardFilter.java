package com.dataflow.aiservice.DTO;

import jakarta.annotation.Nullable;

import java.time.LocalDate;

public record DashboardFilter(
        @Nullable LocalDate from,
        @Nullable LocalDate to,
        @Nullable DataTimeMeasure timeMeasure,
        @Nullable PaymentMethod paymentMethod,
        @Nullable String category,
        @Nullable TransactionType type,
        @Nullable String currencyCode
) {}
