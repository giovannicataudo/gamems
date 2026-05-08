package it.gamems.user_service.security;

import it.gamems.user_service.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * ========================================================
 * SERVICE: CustomUserDetailsService
 * ========================================================
 * Fa da ponte tra Spring Security e il nostro database.
 * Quando Spring ha bisogno di validare chi è un utente, 
 * chiama questa classe passandogli l'email estratta dal JWT.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Metodo core di Spring Security. 
     * Noi usiamo l'email come "username" di sistema.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Cerchiamo l'utente tramite il repository creato in precedenza
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato con email: " + username));
    }
}