package com.retailnexus.controller;

import com.retailnexus.entity.Product;
import com.retailnexus.service.BatchService;
import com.retailnexus.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final ProductService productService;
    private final BatchService batchService;

    public InventoryController(ProductService productService, BatchService batchService) {
        this.productService = productService;
        this.batchService = batchService;
    }

    @GetMapping
    public String list(Model model) {
        List<Product> products = productService.findAll();
        model.addAttribute("products", products);
        java.util.Map<Long, Integer> stockByProduct = new java.util.HashMap<>();
        for (Product p : products) {
            stockByProduct.put(p.getId(), batchService.getTotalStock(p));
        }
        model.addAttribute("stockByProduct", stockByProduct);
        return "inventory/list";
    }

    @PostMapping("/set-stock")
    public String setStock(@RequestParam Long productId, @RequestParam int quantity, RedirectAttributes ra) {
        Product product = productService.findById(productId).orElseThrow();
        batchService.setProductStock(product, quantity);
        ra.addFlashAttribute("message", "Stock updated for " + product.getName() + ".");
        return "redirect:/inventory";
    }

    @GetMapping("/add-stock")
    public String addStockForm(Model model) {
        model.addAttribute("products", productService.findAll());
        return "inventory/add-stock";
    }

    @PostMapping("/add-stock")
    public String addStock(@RequestParam Long productId, @RequestParam int quantity, RedirectAttributes ra) {
        if (quantity < 1) {
            ra.addFlashAttribute("error", "Quantity must be at least 1.");
            return "redirect:/inventory/add-stock";
        }
        Product product = productService.findById(productId).orElseThrow();
        batchService.addProductStock(product, quantity);
        ra.addFlashAttribute("message", "Stock added for " + product.getName() + ".");
        return "redirect:/inventory";
    }
}
