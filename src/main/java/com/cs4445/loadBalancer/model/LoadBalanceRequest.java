package com.cs4445.loadBalancer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for load balancing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadBalanceRequest {

    private String requestId;
    private String payload;
    private int priority; // 1 = high, 2 = medium, 3 = low
}
