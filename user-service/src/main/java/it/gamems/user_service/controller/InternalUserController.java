package it.gamems.user_service.controller;

import it.gamems.user_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    // Questa rotta sarà: GET /api/v1/internal/users/banned-ids
    // Verrà chiamata solo dall'api per salvare in ram la lista
    // degli utenti bannati al suo avvio
    @GetMapping("/banned-ids")
    public ResponseEntity<List<Long>> getBannedUserIds() {
        List<Long> bannedIds = userService.getBannedUserIds();
        return ResponseEntity.ok(bannedIds);
    }
}