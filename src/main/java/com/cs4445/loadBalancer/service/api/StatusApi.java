package com.cs4445.loadBalancer.service.api;

import com.cs4445.loadBalancer.dto.response.core.ErrorResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusApi {
    //==========================================Variable==========================================
    private final RestTemplate restTemplate;

    //===========================================Method===========================================
    public ResponseEntity<?> openServer(String url) {
        String endpoint = url + "/server/open";
        try {
            ResponseEntity<?> response = restTemplate.
                    postForEntity(endpoint, null, String.class);
            return response;
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to connect: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(500).body(error);
        }
    }

    public ResponseEntity<?> closeServer(String url) {
        String endpoint = url + "/server/close";
        try {
            ResponseEntity<?> response = restTemplate.
                    postForEntity(endpoint, null, String.class);
            return response;
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to connect: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(500).body(error);
        }
    }
}