package it.gamems.wallet_service.controller;

import it.gamems.wallet_service.dto.BetRequestDto;
import it.gamems.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ========================================================
 * CONTROLLER: InternalWalletController
 * ========================================================
 * Questo controller espone API riservate esclusivamente alla
 * comunicazione tra microservizi (es. chiamata dal Game Service).
 * * SICUREZZA:
 * Queste rotte NON devono essere esposte pubblicamente dall'API Gateway.
 * Qualsiasi richiesta esterna verso /api/v1/internal/** deve essere scartata.
 */
@RestController
@RequestMapping("/api/v1/internal/wallet")
public class InternalWalletController {

    private static final Logger log = LoggerFactory.getLogger(InternalWalletController.class);
    private final WalletService walletService;

    public InternalWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * ENDPOINT INTERNO: Elabora la scommessa scalando il saldo.
     * Viene chiamato dal Game Service prima di ogni lancio della moneta.
     * @param userId Identificativo dell'utente (passato dal Game Service)
     * @param request DTO contenente l'importo da scalare
     * @return Messaggio di conferma operazione avvenuta
     */
    @PostMapping("/bet")
    public ResponseEntity<String> processBet(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody BetRequestDto request) {
        
        log.info("Ricevuta richiesta interna di bet per utente [{}]: {}€ per partita #{}", userId, request.amount(), request.matchId());
        
        // Esecuzione della logica di business (Playthrough 1x e verifica saldo)
        walletService.processGameBet(userId, request.amount(), request.matchId());
        
        return ResponseEntity.ok("Puntata di " + request.amount() + "€ accettata e scalata con successo.");
    }
}