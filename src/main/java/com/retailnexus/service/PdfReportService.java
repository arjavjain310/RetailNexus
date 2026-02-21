package com.retailnexus.service;

import com.retailnexus.entity.Sale;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generateSalesReportPdf(LocalDate date, List<Sale> sales, BigDecimal total) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>");
        html.append("body{font-family:Helvetica,sans-serif;margin:20px;} table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ddd;padding:8px;}");
        html.append("</style></head><body>");
        html.append("<h1>Daily Sales Report - ").append(date).append("</h1>");
        html.append("<table><tr><th>Sale ID</th><th>Time</th><th>Total</th></tr>");
        for (Sale s : sales) {
            html.append("<tr>")
                .append("<td>").append(s.getId()).append("</td>")
                .append("<td>").append(s.getSaleDate().format(DF)).append("</td>")
                .append("<td>").append(s.getTotalAmount()).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");
        html.append("<p><strong>Total Sales: ").append(total != null ? total : BigDecimal.ZERO).append("</strong></p>");
        html.append("</body></html>");

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.toStream(os);
            builder.withHtmlContent(html.toString(), null);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
