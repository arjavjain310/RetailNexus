package com.retailnexus.service;

import com.retailnexus.entity.Product;
import com.retailnexus.repository.BatchRepository;
import com.retailnexus.repository.ProductRepository;
import com.retailnexus.repository.SaleItemRepository;
import com.retailnexus.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;

    public DashboardService(SaleRepository saleRepository, SaleItemRepository saleItemRepository,
                            ProductRepository productRepository, BatchRepository batchRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalSalesToday() {
        BigDecimal v = saleRepository.totalSalesByDate(LocalDate.now());
        return v != null ? v : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal monthlyRevenue() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        BigDecimal v = saleRepository.totalSalesBetween(start, end);
        return v != null ? v : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalProfit() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        BigDecimal v = saleRepository.totalProfitBetween(start, end);
        return v != null ? v : BigDecimal.ZERO;
    }

    private static final int LOW_STOCK_THRESHOLD = 10;

    @Transactional(readOnly = true)
    public long lowStockCount() {
        List<Product> products = productRepository.findAll();
        long count = 0;
        for (Product p : products) {
            Integer sum = batchRepository.sumQuantityByProduct(p);
            int stock = (sum != null ? sum : 0);
            if (stock > 0 && stock <= LOW_STOCK_THRESHOLD) count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public long nearExpiryCount() {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(5);
        return batchRepository.findNearExpiry(from, to).size();
    }

    @Transactional(readOnly = true)
    public long deadStockCount() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> salesByProduct = saleItemRepository.findSalesByProductSince(since);
        Set<Long> soldProductIds = salesByProduct.stream()
            .map(row -> ((Product) row[0]).getId())
            .collect(Collectors.toSet());
        List<Product> all = productRepository.findAll();
        long count = 0;
        for (Product p : all) {
            if (soldProductIds.contains(p.getId())) continue;
            Integer sum = batchRepository.sumQuantityByProduct(p);
            if (sum != null && sum > 0) count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> monthlySalesTrend(int months) {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1);
            BigDecimal v = saleRepository.totalSalesBetween(monthStart.atStartOfDay(), monthEnd.atStartOfDay());
            trend.put(monthStart.getMonth().name().substring(0, 3) + " " + monthStart.getYear(),
                v != null ? v : BigDecimal.ZERO);
        }
        return trend;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> categoryWiseSales(int months) {
        LocalDateTime since = LocalDateTime.now().minusMonths(months);
        List<Object[]> data = saleItemRepository.findSalesByProductSince(since);
        Map<String, BigDecimal> byCategory = new HashMap<>();
        for (Object[] row : data) {
            Product p = (Product) row[0];
            BigDecimal qty = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : BigDecimal.valueOf(((Number) row[1]).longValue());
            if (qty == null) qty = BigDecimal.ZERO;
            BigDecimal amount = p.getSellingPrice().multiply(qty);
            byCategory.merge(p.getCategory(), amount, BigDecimal::add);
        }
        return byCategory;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> profitDistribution(int months) {
        LocalDateTime start = LocalDateTime.now().minusMonths(months);
        List<com.retailnexus.entity.Sale> sales = saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(start, LocalDateTime.now());
        Map<String, BigDecimal> byCategory = new HashMap<>();
        for (com.retailnexus.entity.Sale s : sales) {
            for (com.retailnexus.entity.SaleItem si : s.getItems()) {
                String cat = si.getProduct().getCategory();
                byCategory.merge(cat, si.getProfit(), BigDecimal::add);
            }
        }
        return byCategory;
    }
}
