
package com.reviewlens.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reviewlens.service.DashboardService;

@RestController
@RequestMapping("/api/reviews")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String topic) {

        return ResponseEntity.ok(
                dashboardService.getDashboardStatistics(product, sentiment, topic)
        );
    }
}
