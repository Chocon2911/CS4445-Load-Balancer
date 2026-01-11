package com.cs4445.loadBalancer.service;

import java.util.List;

import com.cs4445.loadBalancer.model.ServerUrl;

public class ServerService {

    //===========================================Method===========================================
    public static List<ServerUrl> getServerUrls() {
        return List.of(new ServerUrl("http://localhost", 8081),
                new ServerUrl("http://localhost", 8082),
                new ServerUrl("http://localhost", 8083),
                new ServerUrl("http://localhost", 8084),
                new ServerUrl("http://localhost", 8085)
        );
    }
}
