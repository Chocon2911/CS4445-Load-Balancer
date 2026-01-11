package com.cs4445.loadBalancer.dto.response.loadBalancer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadBalancerStatusResponse {

    //==========================================Variable==========================================
    private int totalServers;
    private int healthyServers;
    private int unhealthyServers;
    private long totalRequestsProcessed;
    private long totalRequestsFailed;
    private String currentAlgorithm;
    private List<ServerHealthResponse> servers;
    private LocalDateTime timestamp;
}
