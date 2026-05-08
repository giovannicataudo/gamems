package it.gamems.wallet_service.controller;

import it.gamems.wallet_service.dto.MessageResponseDto;
import it.gamems.wallet_service.dto.WalletAdjustmentRequestDto;
import it.gamems.wallet_service.dto.WalletResponseDto;
import it.gamems.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ========================================================
 * CONTROLLER: AdminWalletController
 * ========================================================
 * Espone endpoint amministrativi per la gestione dei portafogli.
 * La sicurezza è garantita a livello di classe: solo utenti 
 * con ruolo 'ADMIN' possono accedere a queste risorse.
 */
@RestController
@RequestMapping("/api/v1/admin/wallets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWalletController {

    private final WalletService walletService;

    public AdminWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * ENDPOINT: Rettifica manuale del saldo (REAL o WITHDRAWABLE).
     * Utilizzato per correzioni tecniche, rimborsi o bonus manuali.
     * 
     * @param userId L'ID dell'utente a cui modificare il saldo
     * @param request DTO contenente importo, tipo di saldo e causale
     * @return Messaggio di conferma dell'operazione
     */
    @PostMapping("/{userId}/adjust")
    public ResponseEntity<MessageResponseDto> adjustBalance(
            @PathVariable String userId,
            @Valid @RequestBody WalletAdjustmentRequestDto request) {
        
        // Chiamata al servizio transazionale per l'aggiornamento del DB
        walletService.adjustWalletBalance(userId, request);
        
        return ResponseEntity.ok(new MessageResponseDto("Operazione di rettifica completata con successo per l'utente: " + userId));
    }

    /**
     * ENDPOINT: Visualizzazione stato portafoglio utente (ADMIN view).
     * Permette all'amministratore di controllare i saldi di qualsiasi utente.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponseDto> getWalletDetails(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getWalletOrThrow(userId));
    }
}