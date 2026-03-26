package com.dataflow.aiservice.Services;

import com.dataflow.aiservice.Config.AiFunction.BudgetingAdvisorTools;
import com.dataflow.aiservice.Config.AiFunction.ReportingAdvisorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

@Service
public class FinancialAdvisorService {
    private static final Path DEBUG_LOG_PATH = Path.of("debug-e039fd.log");
    private final ChatClient chatClient;
    private final BudgetingAdvisorTools budgetingAdvisorTools;
    private final ReportingAdvisorTools reportingAdvisorTools;

    public FinancialAdvisorService(ChatClient.Builder builder, BudgetingAdvisorTools budgetingAdvisorTools, ReportingAdvisorTools reportingAdvisorTools){
        this.chatClient = builder
                .defaultSystem("""
                         You are 'DataFlow AI', a financial advisor assistant.
                        \s
                         You have access to the following tools:
                         - budgetStatusFunction: Call this to retrieve the user's current budget limits and spending progress. It requires no input parameters.
                         - overviewFunction(filter): Call for dashboard trend overview points (income and expense over time).
                         - kpisFunction(filter): Call for dashboard KPI totals (income, expenses, net, savingsRate).
                         - categoryBreakdownFunction(filter): Call for category level totals.
                         - categoryComparisonFunction(filter): Call for category totals grouped by transaction type.
                         - paymentMethodBreakdownFunction(filter): Call for payment method totals.
                        \s
                         RULES:
                         - Do not use strings like "1 year ago" for timeMeasure and date filter values
                         - Valid timeMeasure values: DAY, MONTH, YEAR (this is used to group by days, months and years for the overview tool)
                         - Valid date format is ISO
                         - If the function doesn't return data, just tell that nicely to the user, don't try to re-call the same tool with the same filters more then twice for an user query.
                         - Call budgetStatusFunction for specific budgeting questions.
                         - Call reporting tools for report, KPI, dashboard, trend, comparison, breakdown and analysis requests.
                         - The DashboardFilter sent to reporting tools must include only fields relevant to the user request.
                         - For unknown fields, omit them from the tool input instead of sending explicit null values.
                         - Do not use placeholders like "all", "none", or guessed IDs in DashboardFilter fields.
                         - NEVER simulate or write code for tool calls. Only use the actual tool.
                         - If the question is unrelated to any tool, respond to the user question without using the provided tools.
                         - If data from the tools isn't helpful in responding to the user, don't use the tools.
                         - NEVER narrate your reasoning, decision-making, or whether you are calling a tool. Just respond directly.
                         - NEVER output phrases like "No function call is needed", "I will now call...", "Let me check...", or any other internal monologue.
                         - The answers should be concise for the most part, unless the user query requires analysis.
                        """)
                .build();
        this.budgetingAdvisorTools = budgetingAdvisorTools;
        this.reportingAdvisorTools = reportingAdvisorTools;
    }

    public Flux<String> processQuery(String message){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String tokenValue = "";

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            tokenValue = jwtAuth.getToken().getTokenValue();
        }

        String finalTokenValue = tokenValue;
        String runId = UUID.randomUUID().toString();
        return Flux.create(sink -> {
            try {
                // #region agent log
                debugLog(runId, "H1", "FinancialAdvisorService:61", "chat_request_start", Map.of(
                        "messageLength", message == null ? 0 : message.length(),
                        "jwtPresent", finalTokenValue != null && !finalTokenValue.isEmpty()
                ));
                // #endregion
                String response = chatClient.prompt()
                        .tools(budgetingAdvisorTools, reportingAdvisorTools)
                        .toolContext(Map.of("jwtToken", finalTokenValue))
                        .user(message)
                        .call()       // blocking — tool calls are properly intercepted
                        .content();
                // #region agent log
                debugLog(runId, "H4", "FinancialAdvisorService:74", "chat_request_success", Map.of(
                        "responseLength", response == null ? 0 : response.length()
                ));
                // #endregion
                sink.next(response);
                sink.complete();
            } catch (Exception e) {
                String err = e.getMessage() == null ? "" : e.getMessage();
                // #region agent log
                debugLog(runId, "H2", "FinancialAdvisorService:82", "chat_request_exception", Map.of(
                        "exceptionClass", e.getClass().getName(),
                        "hasDateParseSignal", err.contains("LocalDate") || err.contains("DateTimeParseException") || err.contains("could not be parsed"),
                        "hasRateLimitSignal", err.contains("rate_limit_exceeded") || err.contains("Request too large") || err.contains("TPM"),
                        "messageSnippet", err.length() > 300 ? err.substring(0, 300) : err
                ));
                // #endregion
                sink.error(e);
            }
        });
    }

    private void debugLog(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        try {
            String line = String.format(
                    "{\"sessionId\":\"e039fd\",\"runId\":\"%s\",\"hypothesisId\":\"%s\",\"location\":\"%s\",\"message\":\"%s\",\"data\":%s,\"timestamp\":%d}%n",
                    escape(runId),
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
