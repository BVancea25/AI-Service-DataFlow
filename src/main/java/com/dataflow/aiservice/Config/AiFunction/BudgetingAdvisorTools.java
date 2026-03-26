package com.dataflow.aiservice.Config.AiFunction;

import com.dataflow.aiservice.DTO.BudgetStatus;
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

import java.util.List;

@Component
public class BudgetingAdvisorTools {
    private static final Logger log = LoggerFactory.getLogger(BudgetingAdvisorTools.class);

    @Value("${services.budgeting.url}")
    private String budgetingUrl;

    private final RestTemplate restTemplate;

    public BudgetingAdvisorTools(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Tool(description = "Get current budget status, limits, and spending progress for the user. Call this for budget related question.")
    public List<BudgetStatus> budgetStatusFunction(ToolContext toolContext) {
        String jwtToken = (String) toolContext.getContext().get("jwtToken");
        log.info("budgetStatusFunction called. jwtPresent={}", jwtToken != null && !jwtToken.isEmpty());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = budgetingUrl + "/api/v1/budgets/status";
        var result = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<BudgetStatus>>() {}
        ).getBody();

        log.info("budgetStatusFunction response={}", result);
        return result;
    }
}
