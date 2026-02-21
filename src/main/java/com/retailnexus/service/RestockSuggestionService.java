package com.retailnexus.service;

import com.retailnexus.entity.Product;
import com.retailnexus.repository.BatchRepository;
import com.retailnexus.repository.SaleItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RestockSuggestionService {

    private final SaleItemRepository saleItemRepository;
    private final ProductService productService;
    private final BatchRepository batchRepository;

    private static final int DAYS_LOOKBACK = 30;

    public RestockSuggestionService(SaleItemRepository saleItemRepository, ProductService productService,
                                    BatchRepository batchRepository) {
        this.saleItemRepository = saleItemRepository;
        this.productService = productService;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public List<RestockSuggestion> getSuggestions() {
        var since = java.time.LocalDateTime.now().minusDays(DAYS_LOOKBACK);
        List<Object[]> salesByProduct = saleItemRepository.findSalesByProductSince(since);
        Map<Product, Long> soldQty = salesByProduct.stream()
            .collect(Collectors.toMap(row -> (Product) row[0], row -> toLongQuantity(row[1])));

        List<RestockSuggestion> result = new ArrayList<>();
        for (Product p : productService.findAll()) {
            long avgSold = soldQty.getOrDefault(p, 0L) / DAYS_LOOKBACK;
            Integer current = batchRepository.sumQuantityByProduct(p);
            int stock = (current != null ? current : 0);
            if (avgSold > 0 && stock < avgSold * 7) { // suggest if stock less than ~1 week of sales
                result.add(new RestockSuggestion(p, stock, (int) avgSold, (int) (avgSold * 14)));
            }
        }
        return result;
    }

    public static class RestockSuggestion {
        private final Product product;
        private final int currentStock;
        private final int avgDailySales;
        private final int suggestedOrder;

        public RestockSuggestion(Product product, int currentStock, int avgDailySales, int suggestedOrder) {
            this.product = product;
            this.currentStock = currentStock;
            this.avgDailySales = avgDailySales;
            this.suggestedOrder = suggestedOrder;
        }
        public Product getProduct() { return product; }
        public int getCurrentStock() { return currentStock; }
        public int getAvgDailySales() { return avgDailySales; }
        public int getSuggestedOrder() { return suggestedOrder; }
    }

    private static long toLongQuantity(Object value) {
        if (value == null) return 0L;
        if (value instanceof BigDecimal) return ((BigDecimal) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
