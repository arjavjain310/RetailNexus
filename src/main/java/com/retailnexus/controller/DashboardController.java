package com.retailnexus.controller;

import com.retailnexus.service.DashboardService;
import com.retailnexus.service.RestockSuggestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/dashboard")
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final RestockSuggestionService restockSuggestionService;

    public DashboardController(DashboardService dashboardService, RestockSuggestionService restockSuggestionService) {
        this.dashboardService = dashboardService;
        this.restockSuggestionService = restockSuggestionService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalSalesToday", dashboardService.totalSalesToday());
        model.addAttribute("monthlyRevenue", dashboardService.monthlyRevenue());
        model.addAttribute("totalProfit", dashboardService.totalProfit());
        model.addAttribute("lowStockCount", dashboardService.lowStockCount());
        model.addAttribute("nearExpiryCount", dashboardService.nearExpiryCount());
        model.addAttribute("deadStockCount", dashboardService.deadStockCount());

        Map<String, BigDecimal> trend = dashboardService.monthlySalesTrend(6);
        model.addAttribute("monthlyLabels", trend.keySet().stream().collect(Collectors.toList()));
        model.addAttribute("monthlyData", trend.values().stream().collect(Collectors.toList()));

        Map<String, BigDecimal> categorySales = dashboardService.categoryWiseSales(1);
        model.addAttribute("categoryLabels", categorySales.keySet().stream().collect(Collectors.toList()));
        model.addAttribute("categoryData", categorySales.values().stream().collect(Collectors.toList()));

        Map<String, BigDecimal> profitDist = dashboardService.profitDistribution(1);
        model.addAttribute("profitLabels", profitDist.keySet().stream().collect(Collectors.toList()));
        model.addAttribute("profitData", profitDist.values().stream().collect(Collectors.toList()));

        model.addAttribute("restockSuggestions", restockSuggestionService.getSuggestions());
        return "dashboard";
    }
}
