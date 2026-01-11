package com.cs4445.loadBalancer.service;

import com.cs4445.loadBalancer.dto.request.loadBalancer.ForwardPacketRequest;
import com.cs4445.loadBalancer.dto.response.loadBalancer.ForwardPacketResponse;
import com.cs4445.loadBalancer.dto.response.loadBalancer.ServerHealthResponse;
import com.cs4445.loadBalancer.dto.request.core.MethodType;
import com.cs4445.loadBalancer.model.ServerInstance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiService {

    //==========================================Variable==========================================
    private final LoadBalancerService loadBalancerService = new LoadBalancerService();
    private final ServerRegistryService serverRegistryService = new ServerRegistryService();
    private final RestTemplate restTemplate = new RestTemplate();

    //=========================================Forward API=========================================

    /**
     * Forward packet đến sub-server (endpoint cho AI gọi để lan truyền packet)
     */
    public ForwardPacketResponse forwardPacket(ForwardPacketRequest request) {
        // Define variables
        long startTime;
        String packetId;
        Optional<ServerInstance> serverOpt;
        ServerInstance server;
        Map<String, Object> subServerRequest;
        String url;
        HttpHeaders headers;
        HttpEntity<Map<String, Object>> entity;
        ResponseEntity<Map> response;
        long processingTime;
        Map<String, Object> responseBody;

        // Logic
        startTime = System.currentTimeMillis();

        packetId = request.getPacketId();
        if (packetId == null || packetId.isEmpty()) {
            packetId = UUID.randomUUID().toString();
        }

        if (request.getTargetServerId() != null && !request.getTargetServerId().isEmpty()) {
            serverOpt = loadBalancerService.selectServer(request.getTargetServerId());
        } else {
            serverOpt = loadBalancerService.selectServer();
        }

        if (serverOpt.isEmpty()) {
            loadBalancerService.incrementFailedRequests();
            return ForwardPacketResponse.builder()
                    .packetId(packetId)
                    .status("NO_SERVER_AVAILABLE")
                    .errorMessage("No healthy server available")
                    .timestamp(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        server = serverOpt.get();

        try {
            serverRegistryService.incrementConnections(server.getId());

            subServerRequest = new HashMap<>();
            subServerRequest.put("packetId", packetId);
            subServerRequest.put("cpuIntensity", request.getCpuIntensity() != null ? request.getCpuIntensity() : 5);
            subServerRequest.put("ramIntensity", request.getRamIntensity() != null ? request.getRamIntensity() : 5);
            subServerRequest.put("processingTimeMs", request.getProcessingTimeMs() != null ? request.getProcessingTimeMs() : 1000);
            subServerRequest.put("payload", request.getPayload());

            url = server.getUrl() + "/api/v1/fakePacket";
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            entity = new HttpEntity<>(subServerRequest, headers);

            response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            processingTime = System.currentTimeMillis() - startTime;
            loadBalancerService.incrementProcessedRequests();

            responseBody = response.getBody();
            if (responseBody != null) {
                return ForwardPacketResponse.builder()
                        .packetId(packetId)
                        .targetServerId(server.getId())
                        .targetServerUrl(server.getUrl())
                        .status((String) responseBody.get("status"))
                        .processingTimeMs(responseBody.get("processingTimeMs") != null ?
                                ((Number) responseBody.get("processingTimeMs")).longValue() : processingTime)
                        .cpuCycles(responseBody.get("cpuCycles") != null ?
                                ((Number) responseBody.get("cpuCycles")).longValue() : null)
                        .memoryUsedBytes(responseBody.get("memoryUsedBytes") != null ?
                                ((Number) responseBody.get("memoryUsedBytes")).longValue() : null)
                        .result((String) responseBody.get("result"))
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            return ForwardPacketResponse.builder()
                    .packetId(packetId)
                    .targetServerId(server.getId())
                    .targetServerUrl(server.getUrl())
                    .status("SUCCESS")
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            processingTime = System.currentTimeMillis() - startTime;
            loadBalancerService.incrementFailedRequests();

            log.error("Failed to forward packet {} to server {}: {}", packetId, server.getId(), e.getMessage());

            return ForwardPacketResponse.builder()
                    .packetId(packetId)
                    .targetServerId(server.getId())
                    .targetServerUrl(server.getUrl())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .processingTimeMs(processingTime)
                    .timestamp(LocalDateTime.now())
                    .build();

        } finally {
            serverRegistryService.decrementConnections(server.getId());
        }
    }

    //==========================================Proxy API==========================================

    /**
     * Proxy request đến sub-server
     */
    public ResponseEntity<byte[]> proxyRequest(MethodType method, String path, Map<String, String> headers, byte[] body) {
        // Define variables
        Optional<ServerInstance> serverOpt;
        ServerInstance server;
        String url;
        HttpHeaders httpHeaders;
        HttpEntity<byte[]> entity;
        HttpMethod httpMethod;
        ResponseEntity<byte[]> response;

        // Logic
        serverOpt = loadBalancerService.selectServer();

        if (serverOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No healthy server available".getBytes());
        }

        server = serverOpt.get();

        try {
            serverRegistryService.incrementConnections(server.getId());

            url = server.getUrl() + path;

            httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::set);
            httpHeaders.set("X-Forwarded-By", "CS4445-LoadBalancer");
            httpHeaders.set("X-Target-Server", server.getId());

            entity = new HttpEntity<>(body, httpHeaders);
            httpMethod = HttpMethod.valueOf(method.getName());

            //===AI Handler===

            response = restTemplate.exchange(url, httpMethod, entity, byte[].class);

            loadBalancerService.incrementProcessedRequests();

            return response;
            //================

        } catch (Exception e) {
            loadBalancerService.incrementFailedRequests();
            log.error("Proxy request failed to {}: {}", server.getId(), e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(("Proxy error: " + e.getMessage()).getBytes());

        } finally {
            serverRegistryService.decrementConnections(server.getId());
        }
    }

    //========================================Health Check API=====================================

    /**
     * Kiểm tra health của tất cả servers
     */
    public List<ServerHealthResponse> checkAllServersHealth() {
        // Define variables
        List<ServerInstance> servers;

        // Logic
        servers = serverRegistryService.getAllServers();

        return servers.stream()
                .map(this::checkServerHealth)
                .toList();
    }

    /**
     * Kiểm tra health của một server cụ thể
     */
    public ServerHealthResponse checkServerHealth(ServerInstance server) {
        // Define variables
        long startTime;
        ServerHealthResponse.ServerHealthResponseBuilder responseBuilder;
        String healthUrl;
        ResponseEntity<String> healthResponse;
        long responseTime;
        boolean isHealthy;
        boolean serverOpen;
        String statusUrl;
        ResponseEntity<Map> statusResponse;
        ServerHealthResponse response;

        // Logic
        startTime = System.currentTimeMillis();
        responseBuilder = ServerHealthResponse.builder()
                .serverId(server.getId())
                .serverUrl(server.getUrl())
                .lastChecked(LocalDateTime.now());

        try {
            healthUrl = server.getUrl() + "/api/v1/health";
            healthResponse = restTemplate.getForEntity(healthUrl, String.class);

            responseTime = System.currentTimeMillis() - startTime;

            isHealthy = healthResponse.getStatusCode().is2xxSuccessful();

            serverOpen = false;
            try {
                statusUrl = server.getUrl() + "/api/v1/server/status";
                statusResponse = restTemplate.getForEntity(statusUrl, Map.class);
                if (statusResponse.getBody() != null) {
                    serverOpen = Boolean.TRUE.equals(statusResponse.getBody().get("open"));
                }
            } catch (Exception e) {
                log.warn("Failed to get server status for {}: {}", server.getId(), e.getMessage());
            }

            serverRegistryService.updateServerHealth(server.getId(), isHealthy && serverOpen);

            response = responseBuilder
                    .healthy(isHealthy)
                    .serverOpen(serverOpen)
                    .responseTimeMs(responseTime)
                    .currentConnections(server.getCurrentConnections())
                    .cpuUsage(server.getCpuUsage())
                    .memoryUsage(server.getMemoryUsage())
                    .build();

            loadBalancerService.updateLastHealthCheck(server.getId(), response);

            log.debug("Health check for {}: healthy={}, open={}, responseTime={}ms",
                    server.getId(), isHealthy, serverOpen, responseTime);

            return response;

        } catch (Exception e) {
            responseTime = System.currentTimeMillis() - startTime;

            serverRegistryService.updateServerHealth(server.getId(), false);

            response = responseBuilder
                    .healthy(false)
                    .serverOpen(false)
                    .responseTimeMs(responseTime)
                    .errorMessage(e.getMessage())
                    .build();

            loadBalancerService.updateLastHealthCheck(server.getId(), response);

            log.warn("Health check failed for {}: {}", server.getId(), e.getMessage());

            return response;
        }
    }

    /**
     * Kiểm tra health của một server theo ID
     */
    public ServerHealthResponse checkServerHealth(String serverId) {
        return serverRegistryService.getServer(serverId)
                .map(this::checkServerHealth)
                .orElse(ServerHealthResponse.builder()
                        .serverId(serverId)
                        .healthy(false)
                        .errorMessage("Server not found")
                        .lastChecked(LocalDateTime.now())
                        .build());
    }

    /**
     * Lấy kết quả health check gần nhất
     */
    public ServerHealthResponse getLastHealthCheck(String serverId) {
        return loadBalancerService.getLastHealthCheck(serverId);
    }

    /**
     * Lấy tất cả kết quả health check gần nhất
     */
    public List<ServerHealthResponse> getAllLastHealthChecks() {
        return loadBalancerService.getAllLastHealthChecks();
    }
}
