package com.dataflow.aiservice.DTO;

import java.util.List;

public record AiAnalysisResponse(
        AiResolvedFilter resolvedFilter,
        DashboardKpiDTO kpis,
        List<CategoryBreakdownDTO> categoryBreakdown,
        List<PaymentMethodBreakdownDTO> paymentMethodBreakdown
) {
}
