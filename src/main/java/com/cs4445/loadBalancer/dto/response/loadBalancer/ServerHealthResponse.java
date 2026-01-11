package com.cs4445.loadBalancer.dto.response.loadBalancer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerHealthResponse {

    //==========================================Variable==========================================
    private String serverId;
    private String serverUrl;
    private boolean healthy;
    private boolean serverOpen;
    private int currentConnections;
    private double cpuUsage;
    private double memoryUsage;
    private long responseTimeMs;
    private LocalDateTime lastChecked;
    private String errorMessage;
}
