package com.gentleia.landingtarjetas.dashboard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/months")
    public List<DashboardMonthResponse> months() {
        return dashboardService.months();
    }

    @GetMapping("/months/{yearMonth:\\d{4}-\\d{2}}")
    public DashboardMonthDetailResponse monthDetail(@PathVariable String yearMonth) {
        return dashboardService.monthDetail(yearMonth);
    }
}
