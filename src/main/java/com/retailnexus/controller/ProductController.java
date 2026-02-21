package com.retailnexus.controller;

import com.retailnexus.entity.Product;
import com.retailnexus.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String search, @RequestParam(required = false) String category, Model model) {
        List<Product> products = productService.search(search, category);
        model.addAttribute("products", products);
        model.addAttribute("categories", productService.findAllCategories());
        model.addAttribute("search", search);
        model.addAttribute("categoryFilter", category);
        return "products/list";
    }

    @GetMapping("/new")
    public String newProduct(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", productService.findAllCategories());
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id).orElseThrow());
        model.addAttribute("categories", productService.findAllCategories());
        return "products/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("product") Product product, BindingResult result, Model model, RedirectAttributes ra) {
        if (product.getBarcode() != null && !product.getBarcode().isBlank()) {
            productService.findByBarcode(product.getBarcode().trim()).ifPresent(existing -> {
                if (product.getId() == null || !existing.getId().equals(product.getId())) {
                    result.rejectValue("barcode", "duplicate.barcode",
                        "Another product with this barcode already exists. Use a different barcode or edit the existing product.");
                }
            });
        }
        if (result.hasErrors()) {
            model.addAttribute("categories", productService.findAllCategories());
            return "products/form";
        }
        productService.save(product);
        ra.addFlashAttribute("message", "Product saved successfully.");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        productService.deleteById(id);
        ra.addFlashAttribute("message", "Product deleted.");
        return "redirect:/products";
    }
}
