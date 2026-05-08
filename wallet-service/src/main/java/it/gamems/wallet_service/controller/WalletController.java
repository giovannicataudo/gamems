package it.gamems.wallet_service.controller;

import it.gamems.wallet_service.dto.AmountRequestDto;
import it.gamems.wallet_service.dto.MessageResponseDto;
import it.gamems.wallet_service.dto.WalletResponseDto;
import it.gamems.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ========================================================
 * CONTROLLER: WalletController
 * ========================================================
 * Espone le API REST del microservizio. 
 * Intercetta le richieste HTTP in ingresso, estrae i dati e 
 * delega l'elaborazione al WalletService.
 * * * SICUREZZA STATELESS:
 * Tutti i metodi richiedono l'header "X-User-Id", che viene iniettato
 * in modo sicuro dall'API Gateway dopo aver validato il JWT.
 */
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    // L'iniezione delle dipendenze tramite costruttore è la best practice
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * ENDPOINT: Ottieni il saldo.
     * Chiamato dal frontend quando l'utente apre l'app.
     */
    @GetMapping
    public ResponseEntity<WalletResponseDto> getWalletStatus(
            @RequestHeader("X-User-Id") String userId) {
        
        WalletResponseDto response = walletService.getWalletStatus(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT: Ricarica conto (Deposito).
     * @Valid attiva le regole definite nel DepositRequestDto (@NotNull, @DecimalMin).
     * Se il JSON è errato, Spring blocca la richiesta e restituisce 400 Bad Request
     * prima ancora di entrare nel metodo.
     */
    @PostMapping("/deposit")
    public ResponseEntity<MessageResponseDto> deposit(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AmountRequestDto request) {
        
        walletService.deposit(userId, request.amount());
        return ResponseEntity.ok(new MessageResponseDto("Ricarica di " + request.amount() + "€ completata con successo."));    }

    /**
     * ENDPOINT: Prelievo vincite.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<MessageResponseDto> withdraw(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AmountRequestDto request) {
        
        walletService.withdraw(userId, request.amount());
        return ResponseEntity.ok(new MessageResponseDto("Prelievo di " + request.amount() + "€ elaborato correttamente."));
    }
}