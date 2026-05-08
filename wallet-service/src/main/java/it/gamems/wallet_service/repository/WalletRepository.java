package it.gamems.wallet_service.repository;

import it.gamems.wallet_service.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ========================================================
 * REPOSITORY: WalletRepository
 * ========================================================
 * Questo strato gestisce l'accesso ai dati (Data Access Object) 
 * per l'entità Wallet. Estendendo JpaRepository, ereditiamo 
 * automaticamente tutti i metodi standard (save, findById, delete) 
 * ottimizzati dal framework.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * CORE BUSINESS QUERY: Trova un portafoglio tramite l'ID Utente.
     * * PERCHÉ È FONDAMENTALE: 
     * Il nostro API Gateway non inoltrerà mai l'ID interno del database (Long id),
     * ma inoltrerà l'ID dell'utente estratto dal token JWT (es. "user-uuid-123").
     * * COME FUNZIONA:
     * Spring Data JPA analizza il nome del metodo ("findBy" + "UserId") 
     * e genera automaticamente questa query SQL dietro le quinte:
     * SELECT * FROM wallets WHERE user_id = ?
     * * @param userId L'identificativo univoco dell'utente proveniente dal Gateway.
     * @return Un Optional contenente il Wallet se esiste, o vuoto se è il 
     * primo accesso dell'utente. L'uso di Optional ci eviterà i 
     * pericolosi NullPointerException nel layer Service.
     */
    Optional<Wallet> findByUserId(String userId);

}