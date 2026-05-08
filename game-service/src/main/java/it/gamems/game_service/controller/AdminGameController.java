package it.gamems.game_service.controller;

import it.gamems.game_service.dto.GameStatusRequestDto;
import it.gamems.game_service.dto.MessageResponseDto;
import it.gamems.game_service.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controller admin per attivare o disattivare il gioco
@RestController
@RequestMapping("/api/v1/admin/game")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGameController {

    private final GameService gameService;

    public AdminGameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PatchMapping("/status")
    public ResponseEntity<MessageResponseDto> updateGameStatus(@Valid @RequestBody GameStatusRequestDto request) {
        gameService.setGameStatus(request.active());
        
        String message = request.active() ? "Gioco attivato con successo." : "Gioco disattivato (Manutenzione).";
        return ResponseEntity.ok(new MessageResponseDto(message));
    }
}