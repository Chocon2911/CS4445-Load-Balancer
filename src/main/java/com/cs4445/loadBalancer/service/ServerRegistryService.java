package com.cs4445.loadBalancer.service;

import com.cs4445.loadBalancer.model.ServerInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ServerRegistryService {

    //==========================================Variable==========================================
    private final Map<String, ServerInstance> servers = new ConcurrentHashMap<>();

    //========================================Constructor=========================================
    @PostConstruct
    public void init() {
        // Khởi tạo danh sách servers mặc định
        registerDefaultServers();
        log.info("ServerRegistryService initialized with {} servers", servers.size());
    }

    //==========================================Init Method========================================

    private void registerDefaultServers() {
        for (int i = 1; i <= 5; i++) {
            String serverId = "server-" + i;
            ServerInstance server = ServerInstance.builder()
                    .id(serverId)
                    .host("localhost")
                    .port(8080 + i)
                    .healthy(false)  // Mặc định chưa biết healthy, cần health check
                    .currentConnections(0)
                    .cpuUsage(0.0)
                    .memoryUsage(0.0)
                    .weight(1)
                    .build();
            servers.put(serverId, server);
            log.debug("Registered server: {}", server);
        }
    }

    //======================================Register/Unregister===================================

    public void registerServer(ServerInstance server) {
        servers.put(server.getId(), server);
        log.info("Registered new server: {}", server.getId());
    }

    public void unregisterServer(String serverId) {
        ServerInstance removed = servers.remove(serverId);
        if (removed != null) {
            log.info("Unregistered server: {}", serverId);
        }
    }

    //============================================Get=============================================

    public Optional<ServerInstance> getServer(String serverId) {
        return Optional.ofNullable(servers.get(serverId));
    }

    public List<ServerInstance> getAllServers() {
        return new ArrayList<>(servers.values());
    }

    public List<ServerInstance> getHealthyServers() {
        return servers.values().stream()
                .filter(ServerInstance::isHealthy)
                .toList();
    }

    public List<ServerInstance> getAvailableServers() {
        return servers.values().stream()
                .filter(ServerInstance::isAvailable)
                .toList();
    }

    public int getTotalServers() {
        return servers.size();
    }

    public int getHealthyServerCount() {
        return (int) servers.values().stream()
                .filter(ServerInstance::isHealthy)
                .count();
    }

    public int getUnhealthyServerCount() {
        return (int) servers.values().stream()
                .filter(s -> !s.isHealthy())
                .count();
    }

    //============================================Update==========================================

    public void updateServerHealth(String serverId, boolean healthy) {
        ServerInstance server = servers.get(serverId);
        if (server != null) {
            server.setHealthy(healthy);
            log.debug("Updated server {} health: {}", serverId, healthy);
        }
    }

    public void updateServerMetrics(String serverId, int connections, double cpuUsage, double memoryUsage) {
        ServerInstance server = servers.get(serverId);
        if (server != null) {
            server.setCurrentConnections(connections);
            server.setCpuUsage(cpuUsage);
            server.setMemoryUsage(memoryUsage);
            log.debug("Updated server {} metrics: connections={}, cpu={}, memory={}",
                    serverId, connections, cpuUsage, memoryUsage);
        }
    }

    //=========================================Connections========================================

    public void incrementConnections(String serverId) {
        ServerInstance server = servers.get(serverId);
        if (server != null) {
            server.setCurrentConnections(server.getCurrentConnections() + 1);
        }
    }

    public void decrementConnections(String serverId) {
        ServerInstance server = servers.get(serverId);
        if (server != null) {
            int current = server.getCurrentConnections();
            server.setCurrentConnections(Math.max(0, current - 1));
        }
    }
}
