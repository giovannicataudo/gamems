package it.gamems.user_service.service;

import it.gamems.user_service.dto.UserProfileDto;
import it.gamems.user_service.entity.User;
import it.gamems.user_service.repository.UserRepository;
import it.gamems.user_service.enums.Role;

import java.util.stream.Collectors;
import java.util.List;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ========================================================
 * SERVICE: UserService
 * ========================================================
 * Gestisce le operazioni sui profili utenti.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Recupera il profilo dell'utente loggato.
     * @param email L'email estratta dal contesto di sicurezza (JWT).
     */
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato"));

        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    /**
     * ADMIN: Recupera l'elenco di tutti gli utenti registrati.
     */
    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserProfileDto(
                        user.getId(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * ADMIN: Cambia lo stato di un utente (Ban/Unban).
     */
    @Transactional
    public void updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato con ID: " + id));
        
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /**
     * ADMIN: Promuove un utente al ruolo ADMIN.
     */
    @Transactional
    public void promoteToAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato con ID: " + id));
        
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }
}