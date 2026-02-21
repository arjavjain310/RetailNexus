package com.retailnexus.repository;

import com.retailnexus.entity.Product;
import com.retailnexus.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("SELECT si.product, SUM(si.quantity) FROM SaleItem si WHERE si.sale.saleDate >= :since GROUP BY si.product")
    List<Object[]> findSalesByProductSince(LocalDateTime since);

    List<SaleItem> findBySaleId(Long saleId);
}
