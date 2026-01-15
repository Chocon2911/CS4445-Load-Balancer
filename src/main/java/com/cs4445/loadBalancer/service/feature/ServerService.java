package com.cs4445.loadBalancer.service.feature;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.cs4445.loadBalancer.dto.response.core.ErrorResponse;
import com.cs4445.loadBalancer.dto.response.loadBalancer.ServerHealthResponse;
import com.cs4445.loadBalancer.model.ServerUrl;

import org.springframework.stereotype.Service;

@Service
public class ServerService {

    //==========================================Variable==========================================
    private final RestTemplate restTemplate = new RestTemplate();

    //============================================Url=============================================
    public List<ServerUrl> getServerUrls() {
        return List.of(
                ServerUrl.builder().dns("http://130.94.65.44").port(8081).build(),
                ServerUrl.builder().dns("http://38.54.56.98").port(8082).build(),
                ServerUrl.builder().dns("http://149.104.78.74").port(8083).build()
        );
    }

    //===========================================Health===========================================
    public List<ResponseEntity<?>> getAllServersHealth(List<ServerUrl> urls) {
        return urls.stream()
                .map(this::getServerHealth)
                .toList();
    }

    public ResponseEntity<?> getServerHealth(ServerUrl url) {
        String endpoint = url.getUrl() + "/server/health";
        try {
            ResponseEntity<ServerHealthResponse> response = restTemplate.
                    getForEntity(endpoint, ServerHealthResponse.class);
            return response;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Lỗi 4xx, 5xx từ server
            ErrorResponse error = ErrorResponse.builder()
                    .message(e.getStatusText())
                    .timestamp(LocalDateTime.now())
                    .build();
            ResponseEntity<ErrorResponse> response = ResponseEntity
                    .status(e.getStatusCode())
                    .body(error);
            return response;

        } catch (Exception e) {
            // Lỗi connection, timeout,...
            ErrorResponse error = ErrorResponse.builder()
                    .message("Failed to connect: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            ResponseEntity<ErrorResponse> response = ResponseEntity
                    .status(500)
                    .body(error);
            return response;
        }
    }
}
