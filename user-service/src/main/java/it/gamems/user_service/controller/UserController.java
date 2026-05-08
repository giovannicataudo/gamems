package it.gamems.user_service.controller;

import it.gamems.user_service.dto.UserProfileDto;
import it.gamems.user_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ========================================================
 * CONTROLLER: UserController
 * ========================================================
 * Espone endpoint protetti relativi ai dati dell'utente.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * ENDPOINT: Recupera il profilo dell'utente corrente.
     * @param userDetails Iniettato automaticamente da Spring Security 
     * grazie al JwtAuthenticationFilter.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        // userDetails.getUsername() contiene l'email verificata dal token
        UserProfileDto profile = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }
}