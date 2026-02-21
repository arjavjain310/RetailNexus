package com.retailnexus.controller;

import com.retailnexus.entity.Sale;
import com.retailnexus.service.PdfReportService;
import com.retailnexus.service.ReportService;
import com.retailnexus.service.ReportService.ProductStockDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportsController {

    private final ReportService reportService;
    private final PdfReportService pdfReportService;
    private final com.retailnexus.repository.SaleRepository saleRepository;

    public ReportsController(ReportService reportService, PdfReportService pdfReportService,
                             com.retailnexus.repository.SaleRepository saleRepository) {
        this.reportService = reportService;
        this.pdfReportService = pdfReportService;
        this.saleRepository = saleRepository;
    }

    @GetMapping
    public String index() {
        return "reports/index";
    }

    @GetMapping("/daily")
    public String daily(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate d = date != null ? date : LocalDate.now();
        List<Sale> sales = reportService.dailySalesReport(d);
        BigDecimal total = saleRepository.totalSalesByDate(d);
        model.addAttribute("date", d);
        model.addAttribute("sales", sales);
        model.addAttribute("total", total != null ? total : BigDecimal.ZERO);
        return "reports/daily";
    }

    @GetMapping(value = "/daily/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> dailyPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Sale> sales = reportService.dailySalesReport(date);
        BigDecimal total = saleRepository.totalSalesByDate(date);
        byte[] pdf = pdfReportService.generateSalesReportPdf(date, sales, total);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily-sales-" + date + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/monthly")
    public String monthly(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month, Model model) {
        int y = year != null ? year : LocalDate.now().getYear();
        int m = month != null ? month : LocalDate.now().getMonthValue();
        List<Sale> sales = reportService.monthlyReport(y, m);
        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("sales", sales);
        return "reports/monthly";
    }

    @GetMapping("/low-stock")
    public String lowStock(@RequestParam(defaultValue = "10") int threshold, Model model) {
        List<ProductStockDto> items = reportService.lowStockReport(threshold);
        model.addAttribute("items", items);
        model.addAttribute("threshold", threshold);
        return "reports/low-stock";
    }

    @GetMapping("/dead-stock")
    public String deadStock(Model model) {
        model.addAttribute("items", reportService.deadStockReport());
        return "reports/dead-stock";
    }
}
