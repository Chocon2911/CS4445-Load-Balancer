package com.cs4445.loadBalancer.service.api;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.cs4445.loadBalancer.dto.response.loadBalancer.ServerHealthResponse;
import com.cs4445.loadBalancer.model.ServerUrl;
import com.cs4445.loadBalancer.service.feature.ServerService;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyApi {
    //==========================================Variable==========================================
    private final ServerService serverService = new ServerService();

    //===========================================Method===========================================
    public ResponseEntity<byte[]> handleRequest(HttpServletRequest request) {
        List<ServerUrl> serverUrls = serverService.getServerUrls();
        List<ResponseEntity<?>> healthResponses = serverService.getAllServersHealth(serverUrls);

        List<ResponseEntity<ServerHealthResponse>> healthyServers = new ArrayList<>();
        for (ResponseEntity<?> healthResponse : healthResponses) {
            if (healthResponse.getStatusCode() != HttpStatus.OK) continue;
            healthyServers.add((ResponseEntity<ServerHealthResponse>) healthResponse);
        }

        if (healthyServers.isEmpty()) return null;
        log.info("Forwarding request to healthy server.");
        // Logic chuyển tiếp request ở đây (sử dụng RestTemplate hoặc WebClient)
        // Trả về response từ server đích
        // Ví dụ giả định:
        // return restTemplate.exchange(...);

        return null;
    }
}
