package it.gamems.wallet_service.repository;

import it.gamems.wallet_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Tabella di "servizio" dove salviamo i match già accreditati per
// evitare duplicati (rabbit at least onece)
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    // Non servono query custom. Useremo il metodo standard existsById(Long id)
}