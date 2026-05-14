package com.dataflow.aiservice.Errors;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        ApiErrorCode code,
        String message,
        String requestId,
        String path
) {
}
