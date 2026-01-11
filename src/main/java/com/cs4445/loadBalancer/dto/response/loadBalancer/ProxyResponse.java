package com.cs4445.loadBalancer.dto.response.loadBalancer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyResponse {

    //==========================================Variable==========================================
    private String requestId;
    private String targetServerId;
    private String targetServerUrl;
    private int statusCode;
    private Map<String, String> headers;
    private byte[] body;
    private long processingTimeMs;
    private boolean success;
    private String errorMessage;
    private LocalDateTime timestamp;
}
