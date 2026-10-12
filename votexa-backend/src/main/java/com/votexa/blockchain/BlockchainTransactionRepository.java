package com.votexa.blockchain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, Long> {
    List<BlockchainTransaction> findTop20ByStatusOrderByIdAsc(String status);
    List<BlockchainTransaction> findTop200ByOrderByIdDesc();
    long countByStatus(String status);
}
