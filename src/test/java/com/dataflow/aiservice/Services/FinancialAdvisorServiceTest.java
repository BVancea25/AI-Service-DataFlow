package com.dataflow.aiservice.Services;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialAdvisorServiceTest {

    @Test
    void buildsCurrentDateContextFromProvidedClock() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-06-04T09:15:30Z"),
                ZoneId.of("Europe/Bucharest")
        );

        FinancialAdvisorService.CurrentDateContext context = FinancialAdvisorService.buildCurrentDateContext(clock);

        assertThat(context.currentDate()).isEqualTo("2026-06-04");
        assertThat(context.currentTimezone()).isEqualTo("Europe/Bucharest");
        assertThat(context.currentDateTime()).isEqualTo("2026-06-04T12:15:30+03:00[Europe/Bucharest]");
    }

    @Test
    void buildsDifferentContextWhenClockChanges() {
        Clock firstClock = Clock.fixed(
                Instant.parse("2026-06-04T09:15:30Z"),
                ZoneId.of("Europe/Bucharest")
        );
        Clock secondClock = Clock.fixed(
                Instant.parse("2026-06-05T09:15:30Z"),
                ZoneId.of("Europe/Bucharest")
        );

        FinancialAdvisorService.CurrentDateContext firstContext = FinancialAdvisorService.buildCurrentDateContext(firstClock);
        FinancialAdvisorService.CurrentDateContext secondContext = FinancialAdvisorService.buildCurrentDateContext(secondClock);

        assertThat(firstContext.currentDate()).isEqualTo("2026-06-04");
        assertThat(secondContext.currentDate()).isEqualTo("2026-06-05");
        assertThat(firstContext).isNotEqualTo(secondContext);
    }

    @Test
    void buildsUserPromptWithTemporalContextFromProvidedClock() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-06-04T09:15:30Z"),
                ZoneId.of("Europe/Bucharest")
        );

        String prompt = FinancialAdvisorService.buildUserPromptWithTemporalContext(
                "How much did I spend last month?",
                clock
        );

        assertThat(prompt).contains("Current date: 2026-06-04");
        assertThat(prompt).contains("Current date-time: 2026-06-04T12:15:30+03:00[Europe/Bucharest]");
        assertThat(prompt).contains("Current timezone: Europe/Bucharest");
        assertThat(prompt).contains("For reporting tools, map relative periods to the closest AiPeriod enum.");
        assertThat(prompt).contains("User question:\nHow much did I spend last month?");
    }

    @Test
    void buildsDifferentUserPromptsWhenClockChanges() {
        Clock firstClock = Clock.fixed(
                Instant.parse("2026-06-04T09:15:30Z"),
                ZoneId.of("Europe/Bucharest")
        );
        Clock secondClock = Clock.fixed(
                Instant.parse("2026-06-05T09:15:30Z"),
                ZoneId.of("Europe/Bucharest")
        );

        String firstPrompt = FinancialAdvisorService.buildUserPromptWithTemporalContext("What is today?", firstClock);
        String secondPrompt = FinancialAdvisorService.buildUserPromptWithTemporalContext("What is today?", secondClock);

        assertThat(firstPrompt).contains("Current date: 2026-06-04");
        assertThat(secondPrompt).contains("Current date: 2026-06-05");
        assertThat(firstPrompt).isNotEqualTo(secondPrompt);
    }
}
