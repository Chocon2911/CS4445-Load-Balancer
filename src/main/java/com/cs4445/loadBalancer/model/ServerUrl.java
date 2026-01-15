package com.cs4445.loadBalancer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerUrl {
    //==========================================Variable==========================================
    @Builder.Default
    private String dns = "http://localhost";

    @Builder.Default
    private int port = 8080;

    //===========================================Method===========================================

    public String getUrl() {
        return dns + ":" + port;
    }
}
