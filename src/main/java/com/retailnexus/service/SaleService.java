package com.retailnexus.service;

import com.retailnexus.entity.*;
import com.retailnexus.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final BatchService batchService;

    public SaleService(SaleRepository saleRepository, BatchService batchService) {
        this.saleRepository = saleRepository;
        this.batchService = batchService;
    }

    @Transactional
    public Sale createSale(List<CartItem> cartItems, User soldBy, com.retailnexus.entity.Sale.PaymentMethod paymentMethod) {
        Sale sale = new Sale();
        sale.setSoldBy(soldBy);
        sale.setPaymentMethod(paymentMethod != null ? paymentMethod : Sale.PaymentMethod.CASH);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        List<BatchDeduction> deductions = new ArrayList<>();

        for (CartItem cart : cartItems) {
            Product product = cart.getProduct();
            BigDecimal needed = cart.getQuantity();
            if (needed == null || needed.compareTo(BigDecimal.ZERO) <= 0) continue;
            List<Batch> batches = batchService.findAvailableByProductFifo(product);
            BigDecimal remaining = needed;
            for (Batch batch : batches) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                int batchQty = Math.max(0, batch.getQuantity());
                if (batchQty <= 0) continue;
                BigDecimal take = remaining.min(BigDecimal.valueOf(batchQty));
                if (take.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal unitPrice = cart.getUnitPrice() != null ? cart.getUnitPrice() : product.getSellingPrice();
                BigDecimal gstPct = cart.getGstPercent() != null ? cart.getGstPercent() : product.getGstPercent();
                if (gstPct == null) gstPct = BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(take);
                BigDecimal gstAmount = subtotal.multiply(gstPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal totalPrice = subtotal.add(gstAmount);
                BigDecimal cost = product.getCostPrice().multiply(take);
                BigDecimal profit = totalPrice.subtract(gstAmount).subtract(cost);

                SaleItem item = new SaleItem();
                item.setSale(sale);
                item.setProduct(product);
                item.setBatch(batch);
                item.setQuantity(take);
                item.setUnitPrice(unitPrice);
                item.setGstPercent(gstPct);
                item.setGstAmount(gstAmount);
                item.setTotalPrice(totalPrice);
                item.setProfit(profit);
                sale.getItems().add(item);

                totalAmount = totalAmount.add(totalPrice);
                totalGst = totalGst.add(gstAmount);
                totalProfit = totalProfit.add(profit);

                int deductQty = take.setScale(0, RoundingMode.UP).intValue();
                deductions.add(new BatchDeduction(batch, deductQty));
                remaining = remaining.subtract(take);
            }
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                Batch batch = batchService.getOrCreateBatchForProduct(product);
                BigDecimal take = remaining;
                BigDecimal unitPrice = cart.getUnitPrice() != null ? cart.getUnitPrice() : product.getSellingPrice();
                BigDecimal gstPct = cart.getGstPercent() != null ? cart.getGstPercent() : product.getGstPercent();
                if (gstPct == null) gstPct = BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(take);
                BigDecimal gstAmount = subtotal.multiply(gstPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal totalPrice = subtotal.add(gstAmount);
                BigDecimal cost = product.getCostPrice().multiply(take);
                BigDecimal profit = totalPrice.subtract(gstAmount).subtract(cost);

                SaleItem item = new SaleItem();
                item.setSale(sale);
                item.setProduct(product);
                item.setBatch(batch);
                item.setQuantity(take);
                item.setUnitPrice(unitPrice);
                item.setGstPercent(gstPct);
                item.setGstAmount(gstAmount);
                item.setTotalPrice(totalPrice);
                item.setProfit(profit);
                sale.getItems().add(item);

                totalAmount = totalAmount.add(totalPrice);
                totalGst = totalGst.add(gstAmount);
                totalProfit = totalProfit.add(profit);

                int deductQty = take.setScale(0, RoundingMode.UP).intValue();
                deductions.add(new BatchDeduction(batch, deductQty));
            }
        }

        sale.setTotalAmount(totalAmount);
        sale.setTotalGst(totalGst);
        sale.setTotalProfit(totalProfit);
        sale = saleRepository.save(sale);
        for (BatchDeduction d : deductions) {
            batchService.deductStock(d.batch, d.qty, "SALE-" + sale.getId());
        }
        return sale;
    }

    private record BatchDeduction(Batch batch, int qty) {}

    @Transactional(readOnly = true)
    public Optional<Sale> findById(Long id) {
        return saleRepository.findById(id);
    }

    /** Load sale with items for PDF/report (initializes lazy collection in same transaction). */
    @Transactional(readOnly = true)
    public Optional<Sale> findByIdWithItems(Long id) {
        return saleRepository.findById(id).map(sale -> {
            sale.getItems().size();
            return sale;
        });
    }

    @Transactional(readOnly = true)
    public BigDecimal totalSalesToday() {
        return saleRepository.totalSalesByDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal monthlyRevenue() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        return saleRepository.totalSalesBetween(start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalProfit() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        return saleRepository.totalProfitBetween(start, end);
    }

    public static class CartItem {
        private Product product;
        private BigDecimal quantity;
        private BigDecimal unitPrice; // optional override; if null use product.sellingPrice
        private BigDecimal gstPercent; // optional override; if null use product.gstPercent

        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getGstPercent() { return gstPercent; }
        public void setGstPercent(BigDecimal gstPercent) { this.gstPercent = gstPercent; }
    }
}
