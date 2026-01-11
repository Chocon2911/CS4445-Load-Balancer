package com.cs4445.loadBalancer.controller;

import com.cs4445.loadBalancer.dto.request.loadBalancer.ForwardPacketRequest;
import com.cs4445.loadBalancer.dto.response.loadBalancer.ForwardPacketResponse;
import com.cs4445.loadBalancer.dto.response.loadBalancer.LoadBalancerStatusResponse;
import com.cs4445.loadBalancer.dto.response.loadBalancer.ServerHealthResponse;
import com.cs4445.loadBalancer.dto.request.core.MethodType;
import com.cs4445.loadBalancer.service.ApiService;
import com.cs4445.loadBalancer.service.LoadBalancerService;
import com.cs4445.loadBalancer.service.ServerRegistryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ServerController {

    //==========================================Variable==========================================
    private final ApiService apiService;
    private final LoadBalancerService loadBalancerService;
    private final ServerRegistryService serverRegistryService;

    //===========================================Proxy===========================================

    /**
     * Proxy tất cả requests đến sub-servers
     * Loại trừ các endpoints của load balancer (/api/lb/**)
     */
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        // Define variables
        String uri;
        MethodType method;
        String queryString;
        String fullPath;
        Map<String, String> headers;
        Enumeration<String> headerNames;
        byte[] body;
        String remoteAddr;

        // Logic
        uri = request.getRequestURI();
        method = MethodType.fromName(request.getMethod());
        queryString = request.getQueryString();

        fullPath = queryString == null ? uri : uri + "?" + queryString;

        headers = new HashMap<>();
        headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        body = request.getInputStream().readAllBytes();

        remoteAddr = request.getRemoteAddr();
        log.info("Proxying {} {} from {}", method, fullPath, remoteAddr);

        return apiService.proxyRequest(method, fullPath, headers, body);
    }

    //======================================Load Balancer API=====================================

    /**
     * Endpoint cho AI gọi để lan truyền packet đến sub-server
     * AI sẽ gọi endpoint này để forward packet
     */
    @PostMapping("/api/lb/forward")
    public ResponseEntity<ForwardPacketResponse> forwardPacket(@RequestBody ForwardPacketRequest request) {
        // Define variables
        ForwardPacketResponse response;

        // Logic
        log.info("AI forwarding packet: {}", request.getPacketId());
        response = apiService.forwardPacket(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Forward nhiều packets cùng lúc (batch)
     */
    @PostMapping("/api/lb/forward/batch")
    public ResponseEntity<List<ForwardPacketResponse>> forwardPacketsBatch(
            @RequestBody List<ForwardPacketRequest> requests) {
        // Define variables
        List<ForwardPacketResponse> responses;

        // Logic
        log.info("AI forwarding {} packets in batch", requests.size());
        responses = requests.stream()
                .map(apiService::forwardPacket)
                .toList();

        return ResponseEntity.ok(responses);
    }

    //========================================Health Check========================================

    /**
     * Lấy health của một server cụ thể
     */
    @GetMapping("/api/lb/server/health/{serverId}")
    public ResponseEntity<ServerHealthResponse> getServerHealth(@PathVariable String serverId) {
        // Define variables
        ServerHealthResponse health;

        // Logic
        health = apiService.checkServerHealth(serverId);
        return ResponseEntity.ok(health);
    }

    /**
     * Lấy health của tất cả servers
     */
    @GetMapping("/api/lb/server/healths")
    public ResponseEntity<List<ServerHealthResponse>> getAllServerHealths() {
        // Define variables
        List<ServerHealthResponse> healths;

        // Logic
        healths = apiService.checkAllServersHealth();
        return ResponseEntity.ok(healths);
    }

    /**
     * Lấy kết quả health check gần nhất (không gọi lại)
     */
    @GetMapping("/api/lb/server/healths/cached")
    public ResponseEntity<List<ServerHealthResponse>> getCachedServerHealths() {
        // Define variables
        List<ServerHealthResponse> healths;

        // Logic
        healths = apiService.getAllLastHealthChecks();
        return ResponseEntity.ok(healths);
    }

    //==========================================Status============================================

    /**
     * Lấy trạng thái tổng quan của Load Balancer
     */
    @GetMapping("/api/lb/status")
    public ResponseEntity<LoadBalancerStatusResponse> getStatus() {
        // Define variables
        LoadBalancerStatusResponse status;

        // Logic
        status = LoadBalancerStatusResponse.builder()
                .totalServers(serverRegistryService.getTotalServers())
                .healthyServers(serverRegistryService.getHealthyServerCount())
                .unhealthyServers(serverRegistryService.getUnhealthyServerCount())
                .totalRequestsProcessed(loadBalancerService.getTotalRequestsProcessed())
                .totalRequestsFailed(loadBalancerService.getTotalRequestsFailed())
                .currentAlgorithm(loadBalancerService.getCurrentAlgorithm())
                .servers(apiService.getAllLastHealthChecks())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(status);
    }

    /**
     * Health check của load balancer
     */
    @GetMapping("/api/lb/health")
    public ResponseEntity<Map<String, Object>> health() {
        // Define variables
        Map<String, Object> health;

        // Logic
        health = new HashMap<>();
        health.put("status", "UP");
        health.put("totalServers", serverRegistryService.getTotalServers());
        health.put("healthyServers", serverRegistryService.getHealthyServerCount());
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }

    //========================================Configuration=======================================

    /**
     * Thay đổi thuật toán load balancing
     * Các thuật toán: ROUND_ROBIN, LEAST_CONNECTIONS, WEIGHTED, RANDOM
     */
    @PostMapping("/api/lb/config/algorithm")
    public ResponseEntity<Map<String, String>> setAlgorithm(@RequestParam String algorithm) {
        // Define variables
        Map<String, String> response;

        // Logic
        loadBalancerService.setAlgorithm(algorithm);

        response = new HashMap<>();
        response.put("message", "Algorithm changed successfully");
        response.put("algorithm", algorithm);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin cấu hình hiện tại
     */
    @GetMapping("/api/lb/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        // Define variables
        Map<String, Object> config;

        // Logic
        config = new HashMap<>();
        config.put("algorithm", loadBalancerService.getCurrentAlgorithm());
        config.put("totalServers", serverRegistryService.getTotalServers());
        config.put("healthCheckInterval", "10 seconds");
        return ResponseEntity.ok(config);
    }
}
