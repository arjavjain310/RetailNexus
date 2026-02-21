package com.retailnexus.controller;

import com.retailnexus.entity.Product;
import com.retailnexus.entity.Sale;
import com.retailnexus.entity.User;

import java.math.BigDecimal;
import com.retailnexus.service.BatchService;
import com.retailnexus.service.PdfReportService;
import com.retailnexus.service.ProductService;
import com.retailnexus.service.SaleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final ProductService productService;
    private final BatchService batchService;
    private final SaleService saleService;
    private final PdfReportService pdfReportService;
    private final com.retailnexus.repository.UserRepository userRepository;

    public BillingController(ProductService productService, BatchService batchService,
                              SaleService saleService, PdfReportService pdfReportService,
                              com.retailnexus.repository.UserRepository userRepository) {
        this.productService = productService;
        this.batchService = batchService;
        this.saleService = saleService;
        this.pdfReportService = pdfReportService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String billing(Model model) {
        List<Product> products = productService.findAll();
        model.addAttribute("products", products);
        java.util.Map<Long, Integer> stockByProduct = new java.util.HashMap<>();
        for (Product p : products) {
            stockByProduct.put(p.getId(), batchService.getTotalStock(p));
        }
        model.addAttribute("stockByProduct", stockByProduct);
        return "billing/index";
    }

    @GetMapping("/invoice/{id}")
    public String invoice(@PathVariable Long id, Model model) {
        Sale sale = saleService.findById(id).orElseThrow();
        model.addAttribute("sale", sale);
        return "billing/invoice";
    }

    @GetMapping("/invoice/{id}/pdf")
    public ResponseEntity<byte[]> invoicePdf(@PathVariable Long id) {
        byte[] pdf = pdfReportService.generateInvoicePdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PostMapping("/complete")
    public String complete(@RequestParam("items") String itemsJson,
                           @RequestParam(value = "paymentMethod", defaultValue = "CASH") String paymentMethodStr,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes ra) {
        List<SaleService.CartItem> cart = parseCart(itemsJson);
        if (cart.isEmpty()) {
            ra.addFlashAttribute("error", "Cart is empty.");
            return "redirect:/billing";
        }
        Sale.PaymentMethod method = Sale.PaymentMethod.CASH;
        try {
            method = Sale.PaymentMethod.valueOf(paymentMethodStr.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        Sale sale = saleService.createSale(cart, user, method);
        ra.addFlashAttribute("saleId", sale.getId());
        return "redirect:/billing/invoice/" + sale.getId();
    }

    private List<SaleService.CartItem> parseCart(String itemsJson) {
        List<SaleService.CartItem> cart = new ArrayList<>();
        if (itemsJson == null || itemsJson.isBlank()) return cart;
        for (String part : itemsJson.split(";")) {
            String[] kv = part.split(":");
            if (kv.length < 2) continue;
            Long productId = Long.parseLong(kv[0]);
            BigDecimal qty;
            try { qty = new BigDecimal(kv[1].trim()); } catch (Exception e) { continue; }
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal unitPrice = null;
            if (kv.length >= 3 && kv[2] != null && !kv[2].isBlank()) {
                try { unitPrice = new BigDecimal(kv[2]); } catch (Exception ignored) {}
            }
            BigDecimal gstPercent = null;
            if (kv.length >= 4 && kv[3] != null && !kv[3].isBlank()) {
                try { gstPercent = new BigDecimal(kv[3]); } catch (Exception ignored) {}
            }
            Optional<Product> opt = productService.findById(productId);
            if (opt.isEmpty()) continue;
            Product p = opt.get();
            SaleService.CartItem item = new SaleService.CartItem();
            item.setProduct(p);
            item.setQuantity(qty);
            item.setUnitPrice(unitPrice);
            item.setGstPercent(gstPercent);
            cart.add(item);
        }
        return cart;
    }

    public static class CartEntry {
        private Long productId;
        private String productName;
        private int quantity;
        private String price;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
    }
}
