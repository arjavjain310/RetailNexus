package com.retailnexus.repository;

import com.retailnexus.entity.Batch;
import com.retailnexus.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByProductOrderByExpiryDateAsc(Product product);
    List<Batch> findByProductAndQuantityGreaterThanOrderByExpiryDateAsc(Product product, int quantity);

    @Query("SELECT b FROM Batch b WHERE b.product = :product AND b.quantity > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findAvailableBatchesByProductFifo(Product product);

    @Query("SELECT SUM(b.quantity) FROM Batch b WHERE b.product = :product AND b.quantity > 0")
    Integer sumQuantityByProduct(Product product);

    @Query("SELECT SUM(b.quantity) FROM Batch b WHERE b.product = :product")
    Integer sumQuantityByProductAll(Product product);

    @Query("SELECT b FROM Batch b WHERE b.quantity > 0 AND b.expiryDate BETWEEN :from AND :to ORDER BY b.expiryDate")
    List<Batch> findNearExpiry(LocalDate from, LocalDate to);

    @Query("SELECT b FROM Batch b WHERE b.quantity > 0 ORDER BY b.expiryDate")
    List<Batch> findAllWithStock();
}
