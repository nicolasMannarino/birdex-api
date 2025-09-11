package com.birdex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    OffsetDateTime timestamp;
    int status;
    String error;
    String code;
    String message;
    String path;
    String method;
    String traceId;
    Map<String, Object> details;
    List<Violation> violations;

    @Value
    @Builder
    public static class Violation {
        String field;
        String reason;
        Object rejectedValue;
    }
}
