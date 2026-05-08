package it.gamems.user_service.repository;

import it.gamems.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ========================================================
 * REPOSITORY: UserRepository
 * ========================================================
 * Gestisce l'accesso ai dati per l'entità User.
 * Interagisce con lo schema 'user_db' in PostgreSQL.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * QUERY CRITICA: Ricerca utente per email.
     * * È il metodo principale utilizzato da Spring Security durante 
     * la fase di autenticazione (LoadUserByUsername).
     * @param email L'indirizzo email inserito dall'utente nel form di login.
     * @return Un Optional contenente l'utente se trovato, altrimenti vuoto.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica l'esistenza di un utente prima della registrazione.
     * @param email Email da controllare.
     * @return true se l'email è già presente nel sistema.
     */
    boolean existsByEmail(String email);
}