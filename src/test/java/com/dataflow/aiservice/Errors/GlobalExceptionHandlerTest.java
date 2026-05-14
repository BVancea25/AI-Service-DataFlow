package com.dataflow.aiservice.Errors;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ai/chat/stream");

    @Test
    void badRequestReturnsSanitizedValidationError() {
        var response = handler.handleBadRequest(new IllegalArgumentException("raw prompt parsing detail"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ApiErrorCode.VALIDATION_ERROR);
        assertThat(response.getBody().message()).doesNotContain("raw prompt parsing detail");
    }

    @Test
    void upstreamErrorReturnsSanitizedBadGateway() {
        var response = handler.handleUpstream(new UpstreamServiceException("provider stack trace"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ApiErrorCode.UPSTREAM_SERVICE_ERROR);
        assertThat(response.getBody().message()).doesNotContain("provider stack trace");
    }
}
