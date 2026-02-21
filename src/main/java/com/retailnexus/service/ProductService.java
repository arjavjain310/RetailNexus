package com.retailnexus.service;

import com.retailnexus.entity.Product;
import com.retailnexus.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> search(String query, String category) {
        List<Product> list;
        if (query != null && !query.isBlank() && category != null && !category.isBlank()) {
            list = productRepository.findByCategoryContainingIgnoreCaseOrNameContainingIgnoreCase(category, query);
        } else if (query != null && !query.isBlank()) {
            list = productRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query);
        } else if (category != null && !category.isBlank()) {
            list = productRepository.findByCategoryOrderByNameAsc(category);
        } else {
            list = productRepository.findAllByOrderByNameAsc();
        }
        return list.stream().sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> findAllCategories() {
        return productRepository.findAllDistinctCategories();
    }
}
