package com.retailnexus.repository;

import com.retailnexus.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByNameAsc();
    Optional<Product> findByBarcode(String barcode);
    List<Product> findByCategoryOrderByNameAsc(String category);
    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String name);
    List<Product> findByCategoryContainingIgnoreCaseOrNameContainingIgnoreCase(String category, String name);

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllDistinctCategories();
}
