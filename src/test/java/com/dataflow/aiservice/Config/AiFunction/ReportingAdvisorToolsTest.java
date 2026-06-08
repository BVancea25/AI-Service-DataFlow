package com.dataflow.aiservice.Config.AiFunction;

import com.dataflow.aiservice.DTO.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportingAdvisorToolsTest {

    @Test
    void financialSummaryToolForwardsJwtAndIntentRequest() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReportingAdvisorTools tools = new ReportingAdvisorTools(restTemplate);
        ReflectionTestUtils.setField(tools, "reportingUrl", "https://reporting.test");

        AiFinancialSummaryResponse response = new AiFinancialSummaryResponse(
                new AiResolvedFilter(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31),
                        DataTimeMeasure.DAY,
                        "RON",
                        null,
                        null,
                        null,
                        null
                ),
                new DashboardKpiDTO(new BigDecimal("1000"), new BigDecimal("400"), new BigDecimal("600"), new BigDecimal("0.6000")),
                List.of(new OverviewPointDTO("2026-05-01", new BigDecimal("1000"), new BigDecimal("400")))
        );

        AiToolRequest request = new AiToolRequest(AiPeriod.LAST_MONTH, null, null, "CARD");
        when(restTemplate.exchange(
                eq("https://reporting.test/api/dashboard/ai/tools/financial-summary"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<AiToolRequest>>argThat(entity -> {
                    assertThat(entity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer jwt-token");
                    assertThat(entity.getBody()).isEqualTo(request);
                    return true;
                }),
                eq(AiFinancialSummaryResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        AiFinancialSummaryResponse result = tools.financialSummaryTool(
                request,
                new ToolContext(Map.of("jwtToken", "jwt-token"))
        );

        assertThat(result).isEqualTo(response);
    }

    @Test
    void sanitizesPlaceholderPaymentMethodsBeforeForwarding() {
        ReportingAdvisorTools tools = new ReportingAdvisorTools(mock(RestTemplate.class));

        assertThat(tools.sanitizeAiToolRequest(new AiToolRequest(AiPeriod.LAST_MONTH, null, null, "")).paymentMethod())
                .isNull();
        assertThat(tools.sanitizeAiToolRequest(new AiToolRequest(AiPeriod.LAST_MONTH, null, null, "all")).paymentMethod())
                .isNull();
        assertThat(tools.sanitizeAiToolRequest(new AiToolRequest(AiPeriod.LAST_MONTH, null, null, "default")).paymentMethod())
                .isNull();
        assertThat(tools.sanitizeAiToolRequest(new AiToolRequest(AiPeriod.LAST_MONTH, null, null, "card")).paymentMethod())
                .isEqualTo("CARD");
    }

    @Test
    void expenseAnalysisToolForwardsOnlySanitizedRequest() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReportingAdvisorTools tools = new ReportingAdvisorTools(restTemplate);
        ReflectionTestUtils.setField(tools, "reportingUrl", "https://reporting.test");

        AiAnalysisResponse response = new AiAnalysisResponse(
                new AiResolvedFilter(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31),
                        DataTimeMeasure.DAY,
                        "RON",
                        null,
                        null,
                        null,
                        TransactionType.EXPENSE
                ),
                new DashboardKpiDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(),
                List.of()
        );

        when(restTemplate.exchange(
                eq("https://reporting.test/api/dashboard/ai/tools/expense-analysis"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<AiToolRequest>>argThat(entity -> {
                    assertThat(entity.getHeaders().getFirst("Authorization")).isEqualTo("Bearer jwt-token");
                    assertThat(entity.getBody()).isEqualTo(new AiToolRequest(AiPeriod.LAST_MONTH, null, null, null));
                    return true;
                }),
                eq(AiAnalysisResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        AiAnalysisResponse result = tools.expenseAnalysisTool(
                new AiToolRequest(AiPeriod.LAST_MONTH, "", "all", ""),
                new ToolContext(Map.of("jwtToken", "jwt-token"))
        );

        assertThat(result).isEqualTo(response);
    }

    @Test
    void rawDashboardFilterMethodsAreNotExposedAsTools() throws Exception {
        assertThatToolAnnotationIsAbsent("overviewFunction", DashboardFilter.class, ToolContext.class);
        assertThatToolAnnotationIsAbsent("kpisFunction", DashboardFilter.class, ToolContext.class);
        assertThatToolAnnotationIsAbsent("categoryBreakdownFunction", DashboardFilter.class, ToolContext.class);
        assertThatToolAnnotationIsAbsent("categoryComparisonFunction", DashboardFilter.class, ToolContext.class);
        assertThatToolAnnotationIsAbsent("paymentMethodBreakdownFunction", DashboardFilter.class, ToolContext.class);
    }

    private void assertThatToolAnnotationIsAbsent(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = ReportingAdvisorTools.class.getDeclaredMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(Tool.class)).isFalse();
    }
}
