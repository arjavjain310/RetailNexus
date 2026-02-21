package com.retailnexus.service;

import com.retailnexus.entity.Sale;
import com.retailnexus.entity.SaleItem;
import com.retailnexus.entity.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generateInvoicePdf(Sale sale) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>");
        html.append("body{font-family:Helvetica,sans-serif;margin:20px;} table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ddd;padding:8px;text-align:left;} th{background:#f5f5f5;}");
        html.append("</style></head><body>");
        html.append("<h1>RetailNexus - Invoice</h1>");
        html.append("<p><strong>Invoice #</strong> ").append(sale.getId()).append("</p>");
        html.append("<p><strong>Date</strong> ").append(sale.getSaleDate().format(DF)).append("</p>");
        User u = sale.getSoldBy();
        if (u != null) html.append("<p><strong>Sold by</strong> ").append(u.getUsername()).append("</p>");
        if (sale.getPaymentMethod() != null) html.append("<p><strong>Payment</strong> ").append(escape(sale.getPaymentMethod().getDisplayName())).append("</p>");
        html.append("<table><tr><th>Product</th><th>Quantity</th><th>MRP</th><th>GST %</th><th>GST Amt</th><th>Line Total</th></tr>");
        for (SaleItem si : sale.getItems()) {
            String gstPct = si.getGstPercent() != null ? si.getGstPercent().toString() : "-";
            html.append("<tr>")
                .append("<td>").append(escape(si.getProduct().getName())).append("</td>")
                .append("<td>").append(escape(si.getQuantityWithUnit())).append("</td>")
                .append("<td>").append(si.getUnitPrice()).append("</td>")
                .append("<td>").append(gstPct).append("</td>")
                .append("<td>").append(si.getGstAmount()).append("</td>")
                .append("<td>").append(si.getTotalPrice()).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");
        html.append("<p><strong>Total Amount: ").append(sale.getTotalAmount()).append("</strong></p>");
        html.append("<p><strong>Total GST: ").append(sale.getTotalGst()).append("</strong></p>");
        html.append("<p><strong>Total Profit: ").append(sale.getTotalProfit()).append("</strong></p>");
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

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
