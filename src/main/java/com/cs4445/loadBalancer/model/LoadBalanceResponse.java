    package com.cs4445.loadBalancer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for load balancing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadBalanceResponse {

    private String requestId;
    private String serverId;
    private String serverUrl;
    private boolean success;
    private String message;
    private long processingTimeMs;
}
