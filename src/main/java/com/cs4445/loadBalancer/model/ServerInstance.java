package com.cs4445.loadBalancer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a backend server instance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInstance {

    private String id;
    private String host;
    private int port;
    private boolean healthy;
    private int currentConnections;
    private double cpuUsage;
    private double memoryUsage;
    private int weight; // For weighted load balancing

    public String getUrl() {
        return String.format("http://%s:%d", host, port);
    }

    public boolean isAvailable() {
        return healthy && currentConnections < 100; // Max connections threshold
    }
}
