package com.cs4445.loadBalancer.dto.request.core;

public enum MethodType {
    //===========================================Value============================================
    GET(0),
    POST(1),
    PUT(2),
    DELETE(3),
    PATCH(4),
    HEAD(5),
    OPTIONS(6),
    TRACE(7);

    //==========================================Variable==========================================
    private final int code;

    //========================================Constructor=========================================
    MethodType(int code) {
        this.code = code;
    }

    //===========================================Method===========================================
    public int getCode() {
        return code;
    }

    public String getName() {
        return switch (this) {
            case GET -> "GET";
            case POST -> "POST";
            case PUT -> "PUT";
            case DELETE -> "DELETE";
            case PATCH -> "PATCH";
            case HEAD -> "HEAD";
            case OPTIONS -> "OPTIONS";
            case TRACE -> "TRACE";
            default -> "UNKNOWN";
        };
    }

    public static MethodType fromCode(int code) {
        for (MethodType method : MethodType.values()) {
            if (method.code != code) continue;
            return method;
        }
        
        throw new IllegalArgumentException("Invalid MethodType code: " + code);
    }

    public static MethodType fromName(String name) {
        for (MethodType method : MethodType.values()) {
            if (!method.getName().equalsIgnoreCase(name)) continue;
            return method;
        }
        
        throw new IllegalArgumentException("Invalid MethodType name: " + name);
    }
}
