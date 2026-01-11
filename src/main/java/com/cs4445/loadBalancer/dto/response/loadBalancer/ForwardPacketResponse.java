package com.cs4445.loadBalancer.dto.response.loadBalancer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwardPacketResponse {

    //==========================================Variable==========================================
    private String packetId;
    private String targetServerId;
    private String targetServerUrl;
    private String status;  // SUCCESS, FAILED, NO_SERVER_AVAILABLE
    private Long processingTimeMs;
    private Long cpuCycles;
    private Long memoryUsedBytes;
    private String result;
    private LocalDateTime timestamp;
    private String errorMessage;
}
