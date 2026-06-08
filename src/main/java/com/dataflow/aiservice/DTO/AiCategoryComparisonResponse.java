package com.dataflow.aiservice.DTO;

import java.util.List;

public record AiCategoryComparisonResponse(
        AiResolvedFilter resolvedFilter,
        List<CategoryComparisonDTO> categoryComparison
) {
}
