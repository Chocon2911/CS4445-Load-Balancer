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
    private double cpuUsagePercent;
    private double memoryUsagePercent;
    private double avgProcessingTimeSec;
    private int currConnections ;
    private boolean isOpen;
}
