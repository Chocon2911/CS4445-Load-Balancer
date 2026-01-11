package com.cs4445.loadBalancer.service;

import com.cs4445.loadBalancer.dto.response.loadBalancer.ServerHealthResponse;
import com.cs4445.loadBalancer.model.ServerInstance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadBalancerService {

    //==========================================Variable==========================================
    private final ServerRegistryService serverRegistryService;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    private final AtomicLong totalRequestsProcessed = new AtomicLong(0);
    private final AtomicLong totalRequestsFailed = new AtomicLong(0);
    private final Map<String, ServerHealthResponse> lastHealthChecks = new ConcurrentHashMap<>();
    private String currentAlgorithm = "ROUND_ROBIN";

    //==========================================Metrics===========================================

    public void incrementProcessedRequests() {
        totalRequestsProcessed.incrementAndGet();
    }

    public void incrementFailedRequests() {
        totalRequestsFailed.incrementAndGet();
    }

    //======================================Select Server=========================================

    /**
     * Chọn server tốt nhất dựa trên thuật toán hiện tại
     */
    public Optional<ServerInstance> selectServer() {
        // Define variables
        List<ServerInstance> availableServers;
        ServerInstance selected;

        // Logic
        availableServers = getAvailableServers();

        if (availableServers.isEmpty()) {
            log.warn("No available servers for load balancing");
            return Optional.empty();
        }

        selected = switch (currentAlgorithm) {
            case "ROUND_ROBIN" -> selectRoundRobin(availableServers);
            case "LEAST_CONNECTIONS" -> selectLeastConnections(availableServers);
            case "WEIGHTED" -> selectWeighted(availableServers);
            case "RANDOM" -> selectRandom(availableServers);
            default -> selectRoundRobin(availableServers);
        };

        log.debug("Selected server {} using {} algorithm", selected.getId(), currentAlgorithm);
        return Optional.of(selected);
    }

    /**
     * Chọn server cụ thể theo ID (cho AI sử dụng)
     */
    public Optional<ServerInstance> selectServer(String serverId) {
        return serverRegistryService.getServer(serverId)
                .filter(server -> isServerAvailable(serverId));
    }

    /**
     * Lấy danh sách servers available (healthy + open)
     */
    private List<ServerInstance> getAvailableServers() {
        return serverRegistryService.getAllServers().stream()
                .filter(server -> isServerAvailable(server.getId()))
                .toList();
    }

    //=====================================Algorithm Methods======================================

    /**
     * Round Robin: Chọn server theo thứ tự vòng tròn
     */
    private ServerInstance selectRoundRobin(List<ServerInstance> servers) {
        // Define variables
        int index;

        // Logic
        index = roundRobinCounter.getAndIncrement() % servers.size();
        return servers.get(index);
    }

    /**
     * Least Connections: Chọn server có ít connections nhất
     */
    private ServerInstance selectLeastConnections(List<ServerInstance> servers) {
        return servers.stream()
                .min(Comparator.comparingInt(ServerInstance::getCurrentConnections))
                .orElse(servers.get(0));
    }

    /**
     * Weighted: Chọn server dựa trên weight
     */
    private ServerInstance selectWeighted(List<ServerInstance> servers) {
        // Define variables
        int totalWeight;
        int random;
        int cumulativeWeight;

        // Logic
        totalWeight = servers.stream()
                .mapToInt(ServerInstance::getWeight)
                .sum();

        random = new Random().nextInt(totalWeight);
        cumulativeWeight = 0;

        for (ServerInstance server : servers) {
            cumulativeWeight += server.getWeight();
            if (random < cumulativeWeight) {
                return server;
            }
        }

        return servers.get(0);
    }

    /**
     * Random: Chọn server ngẫu nhiên
     */
    private ServerInstance selectRandom(List<ServerInstance> servers) {
        return servers.get(new Random().nextInt(servers.size()));
    }

    //=====================================Health Check Cache=====================================

    /**
     * Cập nhật kết quả health check gần nhất
     */
    public void updateLastHealthCheck(String serverId, ServerHealthResponse response) {
        lastHealthChecks.put(serverId, response);
    }

    /**
     * Lấy kết quả health check gần nhất
     */
    public ServerHealthResponse getLastHealthCheck(String serverId) {
        return lastHealthChecks.get(serverId);
    }

    /**
     * Lấy tất cả kết quả health check gần nhất
     */
    public List<ServerHealthResponse> getAllLastHealthChecks() {
        return lastHealthChecks.values().stream().toList();
    }

    /**
     * Kiểm tra xem server có available không (healthy + open)
     */
    public boolean isServerAvailable(String serverId) {
        // Define variables
        ServerHealthResponse lastCheck;

        // Logic
        lastCheck = lastHealthChecks.get(serverId);
        if (lastCheck == null) {
            return false;
        }
        return lastCheck.isHealthy() && lastCheck.isServerOpen();
    }

    //============================================Get=============================================

    public String getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    public long getTotalRequestsProcessed() {
        return totalRequestsProcessed.get();
    }

    public long getTotalRequestsFailed() {
        return totalRequestsFailed.get();
    }

    //============================================Set=============================================

    public void setAlgorithm(String algorithm) {
        this.currentAlgorithm = algorithm;
        log.info("Load balancing algorithm changed to: {}", algorithm);
    }
}
