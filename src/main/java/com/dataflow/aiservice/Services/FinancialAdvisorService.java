package com.dataflow.aiservice.Services;

import com.dataflow.aiservice.Config.AiFunction.BudgetingAdvisorTools;
import com.dataflow.aiservice.Config.AiFunction.ReportingAdvisorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    private final Clock clock;

    public FinancialAdvisorService(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            BudgetingAdvisorTools budgetingAdvisorTools,
            ReportingAdvisorTools reportingAdvisorTools,
            Clock clock
    ){
        this.chatClient = builder
                .defaultSystem("""
                         You are 'DataFlow AI', a financial advisor assistant.
                        \s
                         You have access to the following tools:
                         - budgetStatusFunction: Call this to retrieve the user's current budget limits and spending progress. It requires no input parameters.
                         - financialSummaryTool(request): Call for overall income, expenses, net, savings rate, and trend overview.
                         - expenseAnalysisTool(request): Call for spending/expense questions, top expense categories, and expense payment methods.
                         - incomeAnalysisTool(request): Call for income/earnings questions, income categories, and income payment methods.
                         - categoryComparisonTool(request): Call to compare top income and expense categories.
                        \s
                         RULES:
                         - Prefer the intent-based reporting tools: financialSummaryTool, expenseAnalysisTool, incomeAnalysisTool, categoryComparisonTool.
                         - Do not build raw DashboardFilter objects unless absolutely necessary.
                         - For reporting tools, use AiToolRequest with period enum values only.
                         - Valid period values: CURRENT_MONTH, LAST_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, YEAR_TO_DATE, CURRENT_YEAR, LAST_YEAR.
                         - If the user does not specify a period, use CURRENT_MONTH.
                         - By default, use only 1 to 2 tools for one user query, if you think running more tools would be useful for the analysis ask the user if you should do so
                         - Once a tool returns a result, you MUST respond to the user immediately with that data. Never call the same tool twice in a single turn.
                         - After receiving any tool result, your next output MUST be a final text response to the user, not another tool call.
                         - Call budgetStatusFunction for specific budgeting questions.
                         - Call reporting tools for report, KPI, dashboard, trend, comparison, breakdown and analysis requests.
                         - For optional AiToolRequest fields, omit unknown values instead of sending placeholders like "all", "none", or guessed IDs.
                         - Optional AiToolRequest fields must be omitted unless the user explicitly stated them.
                         - Never send "all", "default", "none", or empty strings for optional AiToolRequest fields.
                         - If there is no payment method filter, omit paymentMethod.
                         - NEVER simulate or write code for tool calls. Only use the actual tool.
                         - If the question is unrelated to any tool, respond to the user question without using the provided tools.
                         - If data from the tools isn't helpful in responding to the user, don't use the tools.
                         - NEVER narrate your reasoning, decision-making, or whether you are calling a tool. Just respond directly.
                         - NEVER output phrases like "No function call is needed", "I will now call...", "Let me check...", or any other internal monologue.
                         - The answers should be concise for the most part, unless the user query requires analysis.
                         - When providing statistics to the user, be sure they are in a readable format.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.budgetingAdvisorTools = budgetingAdvisorTools;
        this.reportingAdvisorTools = reportingAdvisorTools;
        this.clock = clock;
    }

    public Flux<String> processQuery(String message, String conversationId){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String tokenValue = "";
        String userId = auth == null ? "anonymous" : auth.getName();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            tokenValue = jwtAuth.getToken().getTokenValue();
            if (jwtAuth.getToken().getSubject() != null && !jwtAuth.getToken().getSubject().isBlank()) {
                userId = jwtAuth.getToken().getSubject();
            }
        }

        String finalTokenValue = tokenValue;
        String conversationKey = userId + ":" + normalizeConversationId(conversationId);
        String runId = UUID.randomUUID().toString();
        return Flux.create(sink -> {
            try {

                String response = chatClient.prompt()
                        .tools(budgetingAdvisorTools, reportingAdvisorTools)
                        .toolContext(Map.of("jwtToken", finalTokenValue))
                        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationKey))
                        .user(buildUserPromptWithTemporalContext(message))
                        .call()       // blocking — tool calls are properly intercepted
                        .content();

                sink.next(response);
                sink.complete();
            } catch (Exception e) {
                String err = e.getMessage() == null ? "" : e.getMessage();
                System.out.println(err);
                // #region agent log
                debugLog(runId, "H2", "FinancialAdvisorService:82", "chat_request_exception", Map.of(
                        "exceptionClass", e.getClass().getName(),
                        "hasDateParseSignal", err.contains("LocalDate") || err.contains("DateTimeParseException") || err.contains("could not be parsed"),
                        "hasRateLimitSignal", err.contains("rate_limit_exceeded") || err.contains("Request too large") || err.contains("TPM"),
                        "messageSnippet", err.length() > 300 ? err.substring(0, 300) : err
                ));
                // #endregion
                sink.next("I'm having trouble connecting to your data right now.");
                sink.complete();
            }
        });
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "default";
        }

        String normalized = conversationId.replaceAll("[^a-zA-Z0-9_-]", "");
        if (normalized.isBlank()) {
            return "default";
        }

        return normalized.substring(0, Math.min(normalized.length(), 80));
    }

    String buildUserPromptWithTemporalContext(String message) {
        return buildUserPromptWithTemporalContext(message, clock);
    }

    static String buildUserPromptWithTemporalContext(String message, Clock clock) {
        CurrentDateContext context = buildCurrentDateContext(clock);
        String userMessage = message == null ? "" : message;

        return """
                Current date: %s
                Current date-time: %s
                Current timezone: %s
                Use this temporal context for relative periods such as today, this month, last month, this year, last year, and year to date.
                For reporting tools, map relative periods to the closest AiPeriod enum.
                
                User question:
                %s
                """.formatted(
                context.currentDate(),
                context.currentDateTime(),
                context.currentTimezone(),
                userMessage
        );
    }

    static CurrentDateContext buildCurrentDateContext(Clock clock) {
        LocalDate currentDate = LocalDate.now(clock);
        ZonedDateTime currentDateTime = ZonedDateTime.now(clock);

        return new CurrentDateContext(
                currentDate.toString(),
                currentDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                clock.getZone().toString()
        );
    }

    record CurrentDateContext(String currentDate, String currentDateTime, String currentTimezone) {
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
