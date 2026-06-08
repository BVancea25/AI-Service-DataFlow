package com.dataflow.aiservice.DTO;

import java.util.List;

public record AiFinancialSummaryResponse(
        AiResolvedFilter resolvedFilter,
        DashboardKpiDTO kpis,
        List<OverviewPointDTO> overview
) {
}
