package com.retailnexus.service;

import com.retailnexus.entity.Batch;
import com.retailnexus.entity.InventoryTransaction;
import com.retailnexus.entity.Product;
import com.retailnexus.repository.BatchRepository;
import com.retailnexus.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final InventoryTransactionRepository transactionRepository;

    public BatchService(BatchRepository batchRepository, InventoryTransactionRepository transactionRepository) {
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<Batch> findAvailableByProductFifo(Product product) {
        return batchRepository.findAvailableBatchesByProductFifo(product);
    }

    @Transactional(readOnly = true)
    public int getTotalStock(Product product) {
        Integer sum = batchRepository.sumQuantityByProductAll(product);
        return sum != null ? sum : 0;
    }

    @Transactional
    public Batch save(Batch batch) {
        Batch saved = batchRepository.save(batch);
        InventoryTransaction tx = new InventoryTransaction();
        tx.setBatch(saved);
        tx.setType(InventoryTransaction.TransactionType.RESTOCK);
        tx.setQuantityChange(saved.getQuantity());
        tx.setReference("RESTOCK");
        transactionRepository.save(tx);
        return saved;
    }

    @Transactional
    public void deductStock(Batch batch, int qty, String reference) {
        batch.setQuantity(batch.getQuantity() - qty);
        batchRepository.save(batch);
        InventoryTransaction tx = new InventoryTransaction();
        tx.setBatch(batch);
        tx.setType(InventoryTransaction.TransactionType.SALE);
        tx.setQuantityChange(-qty);
        tx.setReference(reference);
        transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public List<Batch> findNearExpiry(int withinDays) {
        LocalDate from = LocalDate.now();
        LocalDate to = LocalDate.now().plusDays(withinDays);
        return batchRepository.findNearExpiry(from, to);
    }

    @Transactional(readOnly = true)
    public List<Batch> findAllWithStock() {
        return batchRepository.findAllWithStock();
    }

    @Transactional(readOnly = true)
    public List<Batch> findByProduct(Product product) {
        return batchRepository.findByProductOrderByExpiryDateAsc(product);
    }

    @Transactional(readOnly = true)
    public Optional<Batch> findById(Long id) {
        return batchRepository.findById(id);
    }

    /** Get an existing batch for the product, or create one with quantity 0 for allocations (e.g. oversell). */
    @Transactional
    public Batch getOrCreateBatchForProduct(Product product) {
        List<Batch> existing = batchRepository.findByProductOrderByExpiryDateAsc(product);
        if (!existing.isEmpty()) return existing.get(0);
        Batch batch = new Batch();
        batch.setProduct(product);
        batch.setBatchNumber("DEF-" + product.getId());
        batch.setExpiryDate(LocalDate.now().plusYears(1));
        batch.setQuantity(0);
        return batchRepository.save(batch);
    }

    /** Update batch quantity (user-set stock). Does not create inventory transaction. */
    @Transactional
    public void setBatchQuantity(Batch batch, int quantity) {
        batch.setQuantity(quantity);
        batchRepository.save(batch);
    }

    /** Add quantity to product's stock (used by Add Stock form). No batch number or expiry shown. */
    @Transactional
    public void addProductStock(Product product, int quantity) {
        int current = getTotalStock(product);
        setProductStock(product, current + quantity);
    }

    /** Set total stock for a product (user-editable from inventory list). Uses one batch; others set to 0. */
    @Transactional
    public void setProductStock(Product product, int quantity) {
        List<Batch> batches = batchRepository.findByProductOrderByExpiryDateAsc(product);
        if (batches.isEmpty()) {
            Batch batch = new Batch();
            batch.setProduct(product);
            batch.setBatchNumber("STOCK-" + product.getId());
            batch.setExpiryDate(LocalDate.now().plusYears(1));
            batch.setQuantity(quantity);
            batchRepository.save(batch);
            return;
        }
        batches.get(0).setQuantity(quantity);
        batchRepository.save(batches.get(0));
        for (int i = 1; i < batches.size(); i++) {
            batches.get(i).setQuantity(0);
            batchRepository.save(batches.get(i));
        }
    }
}
