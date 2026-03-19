package com.dataflow.aiservice.Config.AiFunction;

import com.dataflow.aiservice.DTO.BudgetStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Configuration
public class AiFunctionConfig {
    @Value("${services.budgeting.url}")
    private String budgetingUrl;

    @Value("${services.reporting.url}")
    private String reportingUrl;

    @Bean
    @Description("Get current budget status, limits, and spending progress for the user.")
    public BiFunction<EmptyInput, Map<String, Object>, List<BudgetStatus>> budgetStatusFunction(RestTemplate restTemplate){
        return (request, toolContext) ->{
          String jwtToken = (String) toolContext.get("jwtToken");

          HttpHeaders headers = new HttpHeaders();
          headers.setBearerAuth(jwtToken);
          HttpEntity<Void> entity = new HttpEntity<>(headers);

          String url = budgetingUrl + "/api/v1/budgets/status";
          return restTemplate.exchange(url, HttpMethod.GET,
                  entity,
                  new ParameterizedTypeReference<List<BudgetStatus>>() {}).getBody();
        };
    }

    public record EmptyInput() {}
}
