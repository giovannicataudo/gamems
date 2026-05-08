package it.gamems.user_service.controller;

import it.gamems.user_service.dto.MessageResponseDto;
import it.gamems.user_service.dto.UserListResponseDto;
import it.gamems.user_service.dto.UserProfileDto;
import it.gamems.user_service.dto.UserStatusRequestDto;
import it.gamems.user_service.service.UserService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ========================================================
 * CONTROLLER: AdminController
 * ========================================================
 * Gestisce rotte accessibili solo agli utenti con ruolo ADMIN.
 * * La protezione @PreAuthorize garantisce che solo chi ha ROLE_ADMIN 
 * nel JWT possa entrare.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * ENDPOINT: Elenco di tutti gli utenti.
     */
    @GetMapping("/users")
    public ResponseEntity<UserListResponseDto> listAllUsers() {
        List<UserProfileDto> users = userService.getAllUsers();
        return ResponseEntity.ok(new UserListResponseDto(users, users.size()));
    }

    /**
     * ENDPOINT: Ban immediato per abusi o riattivazione account.
     */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<MessageResponseDto> updateUserStatus(
            @PathVariable Long id, 
            @Valid @RequestBody UserStatusRequestDto request) {
        
        userService.updateUserStatus(id, request.enabled());
        String message = request.enabled() ? "Utente riattivato." : "Utente bannato con successo.";
        return ResponseEntity.ok(new MessageResponseDto(message));
    }

    /**
     * ENDPOINT: Gestione del team admin (Promozione utente).
     */
    @PostMapping("/users/{id}/promote")
    public ResponseEntity<MessageResponseDto> promoteToAdmin(@PathVariable Long id) {
        userService.promoteToAdmin(id);
        return ResponseEntity.ok(new MessageResponseDto("Utente promosso a ADMIN con successo."));
    }
}