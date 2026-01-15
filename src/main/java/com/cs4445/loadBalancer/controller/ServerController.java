package com.cs4445.loadBalancer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

import com.cs4445.loadBalancer.service.api.ProxyApi;
import com.cs4445.loadBalancer.service.api.StatusApi;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ServerController {

    //==========================================Variable==========================================
    private final ProxyApi proxyApi;
    private final StatusApi statusApi;

    //===========================================Proxy============================================

    /**
     * Proxy tất cả requests đến sub-servers
     * Loại trừ các endpoints của load balancer (/api/lb/**)
     */
    @RequestMapping(value = "/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        ResponseEntity<byte[]> response = proxyApi.handleRequest(request);
        return response;
    }

    //===========================================Status===========================================
    @PostMapping("/api/lb/status/open/{serverUrl}")
    public ResponseEntity<?> openServer(
            @PathVariable String serverUrl) {
        return statusApi.openServer(serverUrl);
    }

    @PostMapping("/api/lb/status/close/{serverUrl}")
    public ResponseEntity<?> closeServer(
            @PathVariable String serverUrl) {
        return statusApi.closeServer(serverUrl);
    }
}
