package com.gentleia.landingtarjetas.dashboard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@RequestParam(required = false) String month) {
        return dashboardService.summary(month);
    }

    @GetMapping("/categories")
    public List<CategoryBreakdownResponse> categoryBreakdown(@RequestParam(required = false) String month) {
        return dashboardService.categoryBreakdown(month);
    }
}
