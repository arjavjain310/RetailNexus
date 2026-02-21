package com.retailnexus.repository;

import com.retailnexus.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE CAST(s.saleDate AS date) = :date")
    BigDecimal totalSalesByDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.saleDate >= :from AND s.saleDate < :to")
    BigDecimal totalSalesBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.totalProfit), 0) FROM Sale s WHERE s.saleDate >= :from AND s.saleDate < :to")
    BigDecimal totalProfitBetween(LocalDateTime from, LocalDateTime to);

    List<Sale> findBySaleDateBetweenOrderBySaleDateDesc(LocalDateTime from, LocalDateTime to);
}
