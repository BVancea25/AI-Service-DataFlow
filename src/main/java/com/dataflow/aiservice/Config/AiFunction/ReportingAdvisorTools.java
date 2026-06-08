package com.dataflow.aiservice.Config.AiFunction;

import com.dataflow.aiservice.DTO.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Component
public class ReportingAdvisorTools {
    private static final Logger log = LoggerFactory.getLogger(ReportingAdvisorTools.class);
    private static final Path DEBUG_LOG_PATH = Path.of("debug-e039fd.log");

    @Value("${services.reporting.url}")
    private String reportingUrl;

    private final RestTemplate restTemplate;

    public ReportingAdvisorTools(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Tool(description = "Get an AI-ready financial summary for a predefined period. Use AiToolRequest with period enum values like CURRENT_MONTH, LAST_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, YEAR_TO_DATE, CURRENT_YEAR, LAST_YEAR. The backend resolves exact dates and defaults.")
    public AiFinancialSummaryResponse financialSummaryTool(
            @ToolParam(description = "Intent-based reporting request. period is required. currencyCode, categoryName, and paymentMethod are optional and should be omitted unless explicitly requested by the user.")
            AiToolRequest request,
            ToolContext toolContext
    ) {
        HttpEntity<AiToolRequest> entity = buildAiToolRequestEntity(request, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/tools/financial-summary";
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                AiFinancialSummaryResponse.class
        ).getBody();
    }

    @Tool(description = "Get AI-ready expense analysis for a predefined period. Use for questions about spending, expenses, top spending categories, or expense payment methods. The backend resolves exact dates, currency default, category name, and expense type.")
    public AiAnalysisResponse expenseAnalysisTool(
            @ToolParam(description = "Intent-based expense analysis request. period is required. currencyCode, categoryName, and paymentMethod are optional and should be omitted unless explicitly requested by the user.")
            AiToolRequest request,
            ToolContext toolContext
    ) {
        HttpEntity<AiToolRequest> entity = buildAiToolRequestEntity(request, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/tools/expense-analysis";
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                AiAnalysisResponse.class
        ).getBody();
    }

    @Tool(description = "Get AI-ready income analysis for a predefined period. Use for questions about income, earnings, income categories, or income payment methods. The backend resolves exact dates, currency default, category name, and income type.")
    public AiAnalysisResponse incomeAnalysisTool(
            @ToolParam(description = "Intent-based income analysis request. period is required. currencyCode, categoryName, and paymentMethod are optional and should be omitted unless explicitly requested by the user.")
            AiToolRequest request,
            ToolContext toolContext
    ) {
        HttpEntity<AiToolRequest> entity = buildAiToolRequestEntity(request, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/tools/income-analysis";
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                AiAnalysisResponse.class
        ).getBody();
    }

    @Tool(description = "Get AI-ready top category comparison for income and expenses over a predefined period. The backend resolves exact dates, currency default, and optional payment method or category name.")
    public AiCategoryComparisonResponse categoryComparisonTool(
            @ToolParam(description = "Intent-based category comparison request. period is required. currencyCode, categoryName, and paymentMethod are optional and should be omitted unless explicitly requested by the user.")
            AiToolRequest request,
            ToolContext toolContext
    ) {
        HttpEntity<AiToolRequest> entity = buildAiToolRequestEntity(request, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/tools/category-comparison";
        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                AiCategoryComparisonResponse.class
        ).getBody();
    }

    public List<OverviewPointDTO> overviewFunction(DashboardFilter filter, ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        log.info("overviewFunction called. jwtPresent={} filter={}", jwtToken != null && !jwtToken.isEmpty(), filter);
        HttpEntity<DashboardFilter> entity = buildRequestEntity(filter, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/overview";
        var result = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<OverviewPointDTO>>() {}
        ).getBody();
        log.info("overviewFunction response={}", result);
        // #region agent log
        debugLog("H3", "ReportingAdvisorTools:50", "overview_result_size", Map.of(
                "resultCount", result == null ? 0 : result.size()
        ));
        // #endregion
        return result;
    }

    public DashboardKpiDTO kpisFunction(DashboardFilter filter, ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        log.info("kpisFunction called. jwtPresent={} filter={}", jwtToken != null && !jwtToken.isEmpty(), filter);
        HttpEntity<DashboardFilter> entity = buildRequestEntity(filter, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/kpis";
        var result = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                DashboardKpiDTO.class
        ).getBody();
        log.info("kpisFunction response={}", result);
        // #region agent log
        debugLog("H5", "ReportingAdvisorTools:71", "kpis_result_present", Map.of(
                "resultPresent", result != null
        ));
        // #endregion
        return result;
    }

    public List<CategoryBreakdownDTO> categoryBreakdownFunction(DashboardFilter filter, ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        log.info("categoryBreakdownFunction called. jwtPresent={} filter={}", jwtToken != null && !jwtToken.isEmpty(), filter);
        HttpEntity<DashboardFilter> entity = buildRequestEntity(filter, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/category-breakdown";
        var result = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<CategoryBreakdownDTO>>() {}
        ).getBody();
        log.info("categoryBreakdownFunction response={}", result);
        return result;
    }

    public List<CategoryComparisonDTO> categoryComparisonFunction(DashboardFilter filter, ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        log.info("categoryComparisonFunction called. jwtPresent={} filter={}", jwtToken != null && !jwtToken.isEmpty(), filter);
        HttpEntity<DashboardFilter> entity = buildRequestEntity(filter, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/category-comparison";
        var result = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<CategoryComparisonDTO>>() {}
        ).getBody();
        log.info("categoryComparisonFunction response={}", result);
        return result;
    }

    public List<PaymentMethodBreakdownDTO> paymentMethodBreakdownFunction(DashboardFilter filter, ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        log.info("paymentMethodBreakdownFunction called. jwtPresent={} filter={}", jwtToken != null && !jwtToken.isEmpty(), filter);
        HttpEntity<DashboardFilter> entity = buildRequestEntity(filter, toolContext);
        String url = reportingUrl + "/api/dashboard/ai/payment-method-breakdown";
        var result = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<PaymentMethodBreakdownDTO>>() {}
        ).getBody();
        log.info("paymentMethodBreakdownFunction response={}", result);
        return result;
    }

    private HttpEntity<DashboardFilter> buildRequestEntity(DashboardFilter filter, ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        return new HttpEntity<>(filter, headers);
    }

    private HttpEntity<AiToolRequest> buildAiToolRequestEntity(AiToolRequest request, ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        AiToolRequest safeRequest = sanitizeAiToolRequest(request);
        return new HttpEntity<>(safeRequest, headers);
    }

    AiToolRequest sanitizeAiToolRequest(AiToolRequest request) {
        if (request == null) {
            return new AiToolRequest(null, null, null, null);
        }

        return new AiToolRequest(
                request.period(),
                sanitizeOptionalText(request.currencyCode()),
                sanitizeOptionalText(request.categoryName()),
                sanitizePaymentMethod(request.paymentMethod())
        );
    }

    private String sanitizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }

        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.equals("all") || lowered.equals("none") || lowered.equals("default")) {
            return null;
        }

        return normalized;
    }

    private String sanitizePaymentMethod(String value) {
        String normalized = sanitizeOptionalText(value);
        if (normalized == null) {
            return null;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "CARD", "CASH", "TRANSFER" -> upper;
            default -> null;
        };
    }

    private void debugLog(String hypothesisId, String location, String message, Map<String, Object> data) {
        try {
            String line = String.format(
                    "{\"sessionId\":\"e039fd\",\"runId\":\"runtime\",\"hypothesisId\":\"%s\",\"location\":\"%s\",\"message\":\"%s\",\"data\":%s,\"timestamp\":%d}%n",
                    escape(hypothesisId),
                    escape(location),
                    escape(message),
                    toJsonObject(data),
                    System.currentTimeMillis()
            );
            Files.writeString(DEBUG_LOG_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private String toJsonObject(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(entry.getKey())).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
