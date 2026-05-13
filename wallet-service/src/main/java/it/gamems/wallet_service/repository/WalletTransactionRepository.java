package it.gamems.wallet_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


import it.gamems.wallet_service.entity.WalletTransaction;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long>{
    Optional<WalletTransaction> findByMatchId(Long matchId);
    
}
