package com.cs4445.loadBalancer.dto.request.loadBalancer;

import com.cs4445.loadBalancer.dto.request.core.MethodType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyRequest {

    //==========================================Variable==========================================
    private String requestId;
    private MethodType method;
    private String uri;
    private String queryString;
    private Map<String, String> headers;
    private String contentType;
    private byte[] body;
    private String remoteAddr;
    private int remotePort;
    private String protocol;

    //===========================================Method===========================================
    public String getFullPath() {
        return queryString == null ? uri : uri + "?" + queryString;
    }
}
