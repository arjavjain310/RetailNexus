package com.retailnexus.repository;

import com.retailnexus.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByBatchIdOrderByTransactionDateDesc(Long batchId);
}
