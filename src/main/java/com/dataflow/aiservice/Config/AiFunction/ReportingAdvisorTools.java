package com.dataflow.aiservice.Config.AiFunction;

import com.dataflow.aiservice.DTO.CategoryBreakdownDTO;
import com.dataflow.aiservice.DTO.CategoryComparisonDTO;
import com.dataflow.aiservice.DTO.DashboardFilter;
import com.dataflow.aiservice.DTO.DashboardKpiDTO;
import com.dataflow.aiservice.DTO.OverviewPointDTO;
import com.dataflow.aiservice.DTO.PaymentMethodBreakdownDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
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

    @Tool(description = "Get dashboard overview points for a date/filter range. Use for trend summaries of income and expenses.")
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

    @Tool(description = "Get dashboard KPI totals like income, expenses, net, and savings rate for a selected period.")
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

    @Tool(description = "Get category spending or income breakdown for the selected dashboard filters.")
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

    @Tool(description = "Compare category totals grouped by transaction type for the selected period and filters.")
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

    @Tool(description = "Get totals by payment method for the selected dashboard filters.")
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
