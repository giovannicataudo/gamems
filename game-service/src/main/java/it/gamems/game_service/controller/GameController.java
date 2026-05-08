package it.gamems.game_service.controller;

import it.gamems.game_service.dto.GameHistoryItemDto;
import it.gamems.game_service.dto.GamePlayResponseDto;
import it.gamems.game_service.dto.GameStatusResponseDto;
import it.gamems.game_service.dto.PlayRequestDto;
import it.gamems.game_service.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ========================================================
 * CONTROLLER: GameController
 * ========================================================
 * Espone gli endpoint REST per interagire con il motore di gioco.
 * Intercetta le chiamate HTTP e delega l'orchestrazione al GameService.
 * * * SICUREZZA (Depth):
 * L'identità dell'utente viene controllata anche localmente oltre 
 * che dal gataway
 */
@RestController
@RequestMapping("/api/v1/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * ENDPOINT: Verifica lo stato di attivazione del gioco.
     * Accessibile a tutti gli utenti autenticati.
     */
    @GetMapping("/status")
    public ResponseEntity<GameStatusResponseDto> checkStatus() {
        boolean isActive = gameService.isGameActive();
        return ResponseEntity.ok(new GameStatusResponseDto(isActive));
    }

    /**
     * ENDPOINT: Effettua una giocata (Lancio della moneta).
     * @param userId Identificativo dell'utente estratto dal JWT.
     * @param request Payload JSON con puntata e scelta (TESTA/CROCE).
     * @return L'esito della giocata con il dettaglio della vincita e feedback UX.
     */
    @PostMapping("/play")
    public ResponseEntity<GamePlayResponseDto> play(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PlayRequestDto request) {

        GamePlayResponseDto response = gameService.play(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT: Recupera lo storico delle partite.
     * @param userId Identificativo dell'utente estratto dal JWT.
     * @return Una lista di giocate passate, ottimizzata per la visualizzazione tabellare.
     */
    @GetMapping("/history")
    public ResponseEntity<List<GameHistoryItemDto>> getHistory(
            @AuthenticationPrincipal String userId) {

        List<GameHistoryItemDto> history = gameService.getHistory(userId);
        return ResponseEntity.ok(history);
    }
}