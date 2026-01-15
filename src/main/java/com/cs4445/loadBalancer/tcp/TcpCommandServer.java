package com.cs4445.loadBalancer.tcp;

import com.cs4445.loadBalancer.dto.response.loadBalancer.ServerHealthResponse;
import com.cs4445.loadBalancer.model.ServerUrl;
import com.cs4445.loadBalancer.service.api.StatusApi;
import com.cs4445.loadBalancer.service.feature.ServerService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TcpCommandServer {

    //==========================================Constant==========================================
    private static final Logger log = LoggerFactory.getLogger(TcpCommandServer.class);

    // Command constants
    public static final String CMD_GET_STATUS = "GET_STATUS";
    public static final String CMD_OPEN_SERVER = "OPEN_SERVER";
    public static final String CMD_CLOSE_SERVER = "CLOSE_SERVER";
    public static final String CMD_PING = "PING";

    // Response constants
    public static final String RESP_OK = "OK";
    public static final String RESP_ERROR = "ERROR";
    public static final String RESP_PONG = "PONG";

    //==========================================Variable==========================================
    @Value("${loadbalancer.tcp.port:9090}")
    private int tcpPort;

    private final StatusApi statusApi;
    private final ServerService serverService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    //=========================================Constructor=========================================
    public TcpCommandServer(StatusApi statusApi, ServerService serverService) {
        this.statusApi = statusApi;
        this.serverService = serverService;
    }

    //===========================================Lifecycle==========================================
    @PostConstruct
    public void start() {
        executorService = Executors.newCachedThreadPool();
        running.set(true);

        executorService.submit(this::runServer);
        log.info("TCP Command Server starting on port {}", tcpPort);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("TCP Command Server stopped");
    }

    //===========================================Server============================================
    private void runServer() {
        try {
            serverSocket = new ServerSocket(tcpPort);
            log.info("TCP Command Server listening on port {}", tcpPort);

            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.debug("New connection from {}", clientSocket.getRemoteSocketAddress());
                    executorService.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running.get()) {
                        log.error("Error accepting connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to start TCP server on port {}", tcpPort, e);
        }
    }

    //==========================================Handler============================================
    /**
     * Xử lý client connection với Length-Prefixed Protocol
     * Protocol: [4-byte big-endian length][JSON payload]
     * Request JSON format:
     * {
     *   "action": "GET_STATUS" | "OPEN_SERVER" | "CLOSE_SERVER" | "PING",
     *   "serverUrl": "http://localhost:8081"  // optional, required for OPEN/CLOSE
     * }
     */
    private void handleClient(Socket clientSocket) {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            // Giữ kết nối mở để xử lý nhiều request liên tiếp
            while (running.get() && !clientSocket.isClosed()) {
                try {
                    // Đọc 4 bytes đầu tiên (big-endian length)
                    int length = in.readInt();
                    if (length <= 0 || length > 1024 * 1024) { // Max 1MB
                        log.warn("Invalid message length: {}", length);
                        break;
                    }

                    // Đọc JSON payload
                    byte[] payload = new byte[length];
                    in.readFully(payload);
                    String jsonRequest = new String(payload, StandardCharsets.UTF_8);

                    log.debug("Received request (len={}): {}", length, jsonRequest);

                    // Xử lý request
                    String response = processJsonRequest(jsonRequest.trim());

                    // Gửi response với length-prefix
                    sendLengthPrefixedResponse(out, response);

                } catch (EOFException e) {
                    // Client đóng kết nối - bình thường
                    log.debug("Client disconnected: {}", clientSocket.getRemoteSocketAddress());
                    break;
                }
            }

        } catch (IOException e) {
            if (running.get()) {
                log.error("Error handling client connection", e);
            }
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("Error closing client socket", e);
            }
        }
    }

    //=========================================Processing==========================================
    /**
     * Parse và xử lý JSON request
     */
    private String processJsonRequest(String jsonRequest) {
        try {
            JsonNode requestNode = objectMapper.readTree(jsonRequest);

            // Lấy action từ JSON
            JsonNode actionNode = requestNode.get("action");
            if (actionNode == null || actionNode.asText().isEmpty()) {
                return buildErrorJson("Missing 'action' field");
            }

            String action = actionNode.asText().toUpperCase();

            // Lấy serverUrl nếu có
            String serverUrl = "";
            JsonNode serverUrlNode = requestNode.get("serverUrl");
            if (serverUrlNode != null) {
                serverUrl = serverUrlNode.asText();
            }

            // Xử lý theo action
            switch (action) {
                case CMD_PING:
                    return buildPingResponse();

                case CMD_GET_STATUS:
                    return handleGetStatus();

                case CMD_OPEN_SERVER:
                    return handleOpenServer(serverUrl);

                case CMD_CLOSE_SERVER:
                    return handleCloseServer(serverUrl);

                default:
                    log.warn("Unknown action: {}", action);
                    return buildErrorJson("Unknown action: " + action);
            }

        } catch (Exception e) {
            log.error("Error parsing JSON request: {}", jsonRequest, e);
            return buildErrorJson("Invalid JSON format: " + e.getMessage());
        }
    }

    /**
     * Build PING response as JSON
     */
    private String buildPingResponse() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", RESP_OK);
            root.put("message", RESP_PONG);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"status\":\"OK\",\"message\":\"PONG\"}";
        }
    }

    //==========================================Handlers===========================================

    /**
     * Lấy status của tất cả servers sử dụng ServerService.getAllServersHealth()
     * Response JSON format:
     * {
     *   "status": "OK",
     *   "servers": [
     *     {"url": "...", "statusCode": 200, "health": {...}},
     *     ...
     *   ]
     * }
     */
    private String handleGetStatus() {
        try {
            List<ServerUrl> servers = serverService.getServerUrls();
            List<ResponseEntity<?>> healthResponses = serverService.getAllServersHealth(servers);

            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", RESP_OK);

            ArrayNode serversArray = objectMapper.createArrayNode();
            for (int i = 0; i < servers.size(); i++) {
                ObjectNode serverNode = objectMapper.createObjectNode();
                serverNode.put("url", servers.get(i).getUrl());
                serverNode.put("statusCode", healthResponses.get(i).getStatusCode().value());

                // Thêm health data nếu response thành công
                Object body = healthResponses.get(i).getBody();
                if (body instanceof ServerHealthResponse) {
                    ServerHealthResponse health = (ServerHealthResponse) body;
                    ObjectNode healthNode = objectMapper.createObjectNode();
                    healthNode.put("cpuUsagePercent", health.getCpuUsagePercent());
                    healthNode.put("memoryUsagePercent", health.getMemoryUsagePercent());
                    healthNode.put("avgProcessingTimeSec", health.getAvgProcessingTimeSec());
                    healthNode.put("currConnections", health.getCurrConnections());
                    healthNode.put("isOpen", health.isOpen());
                    serverNode.set("health", healthNode);
                }

                serversArray.add(serverNode);
            }
            root.set("servers", serversArray);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Error getting server status", e);
            return buildErrorJson(e.getMessage());
        }
    }

    /**
     * Mở server sử dụng StatusApi.openServer()
     */
    private String handleOpenServer(String serverUrl) {
        if (serverUrl.isEmpty()) {
            return buildErrorJson("Server URL required");
        }

        try {
            log.info("Opening server: {}", serverUrl);
            ResponseEntity<?> response = statusApi.openServer(serverUrl);

            ObjectNode root = objectMapper.createObjectNode();
            if (response.getStatusCode().is2xxSuccessful()) {
                root.put("status", RESP_OK);
                root.put("message", "Server " + serverUrl + " opened");
                root.put("serverUrl", serverUrl);
            } else {
                root.put("status", RESP_ERROR);
                root.put("message", "Failed to open server");
                root.put("statusCode", response.getStatusCode().value());
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Error opening server {}", serverUrl, e);
            return buildErrorJson(e.getMessage());
        }
    }

    /**
     * Đóng server sử dụng StatusApi.closeServer()
     */
    private String handleCloseServer(String serverUrl) {
        if (serverUrl.isEmpty()) {
            return buildErrorJson("Server URL required");
        }

        try {
            log.info("Closing server: {}", serverUrl);
            ResponseEntity<?> response = statusApi.closeServer(serverUrl);

            ObjectNode root = objectMapper.createObjectNode();
            if (response.getStatusCode().is2xxSuccessful()) {
                root.put("status", RESP_OK);
                root.put("message", "Server " + serverUrl + " closed");
                root.put("serverUrl", serverUrl);
            } else {
                root.put("status", RESP_ERROR);
                root.put("message", "Failed to close server");
                root.put("statusCode", response.getStatusCode().value());
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Error closing server {}", serverUrl, e);
            return buildErrorJson(e.getMessage());
        }
    }

    private String buildErrorJson(String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("status", RESP_ERROR);
            root.put("message", message);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"message\":\"" + message + "\"}";
        }
    }

    //===========================================Helper============================================
    /**
     * Gửi response với Length-Prefixed Protocol
     * Format: [4-byte big-endian length][JSON payload]
     */
    private void sendLengthPrefixedResponse(DataOutputStream out, String response) throws IOException {
        byte[] payload = response.getBytes(StandardCharsets.UTF_8);
        out.writeInt(payload.length);  // 4-byte big-endian length
        out.write(payload);
        out.flush();
        log.debug("Sent response (len={}): {}", payload.length, response);
    }
}
