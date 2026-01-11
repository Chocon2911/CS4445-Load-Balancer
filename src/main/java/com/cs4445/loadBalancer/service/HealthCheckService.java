package com.cs4445.loadBalancer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    //==========================================Variable==========================================
    private final ApiService apiService;

    //=========================================Scheduled Job========================================

    /**
     * Scheduled health check chạy mỗi 10 giây
     */
    @Scheduled(fixedRate = 10000)
    public void scheduledHealthCheck() {
        log.debug("Running scheduled health check...");
        apiService.checkAllServersHealth();
    }
}
