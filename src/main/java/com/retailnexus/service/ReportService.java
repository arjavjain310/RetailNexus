package com.retailnexus.service;

import com.retailnexus.entity.Product;
import com.retailnexus.entity.Sale;
import com.retailnexus.repository.BatchRepository;
import com.retailnexus.repository.SaleItemRepository;
import com.retailnexus.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final BatchRepository batchRepository;
    private final ProductService productService;

    public ReportService(SaleRepository saleRepository, SaleItemRepository saleItemRepository,
                         BatchRepository batchRepository, ProductService productService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.batchRepository = batchRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<Sale> dailySalesReport(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(start, end);
    }

    @Transactional(readOnly = true)
    public List<Sale> monthlyReport(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        return saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(start.atStartOfDay(), end.atStartOfDay());
    }

    @Transactional(readOnly = true)
    public List<ProductStockDto> lowStockReport(int threshold) {
        List<Product> products = productService.findAll();
        List<ProductStockDto> result = new ArrayList<>();
        for (Product p : products) {
            Integer sum = batchRepository.sumQuantityByProduct(p);
            int stock = (sum != null ? sum : 0);
            if (stock <= threshold && stock >= 0)
                result.add(new ProductStockDto(p, stock));
        }
        result.sort(Comparator.comparingInt(ProductStockDto::getStock));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProductStockDto> deadStockReport() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> sold = saleItemRepository.findSalesByProductSince(since);
        Set<Long> soldIds = sold.stream().map(row -> ((Product) row[0]).getId()).collect(Collectors.toSet());
        List<ProductStockDto> result = new ArrayList<>();
        for (Product p : productService.findAll()) {
            if (soldIds.contains(p.getId())) continue;
            Integer sum = batchRepository.sumQuantityByProduct(p);
            if (sum != null && sum > 0)
                result.add(new ProductStockDto(p, sum));
        }
        return result;
    }

    public static class ProductStockDto {
        private final Product product;
        private final int stock;

        public ProductStockDto(Product product, int stock) {
            this.product = product;
            this.stock = stock;
        }
        public Product getProduct() { return product; }
        public int getStock() { return stock; }
    }
}
