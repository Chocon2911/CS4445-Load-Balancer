package com.cs4445.loadBalancer.dto.request.loadBalancer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwardPacketRequest {

    //==========================================Variable==========================================
    private String packetId;
    private Integer cpuIntensity;
    private Integer ramIntensity;
    private Integer processingTimeMs;
    private String payload;
    private String targetServerId;  // Optional: AI có thể chỉ định server cụ thể
}
