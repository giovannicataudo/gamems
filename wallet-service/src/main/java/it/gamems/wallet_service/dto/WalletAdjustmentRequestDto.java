package it.gamems.wallet_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// DTO da usare quando un admin deve modificiare un portafoglio
public record WalletAdjustmentRequestDto(
    @NotNull(message = "L'importo è obbligatorio")
    @DecimalMin(value = "0.01", message = "L'importo deve essere superiore a zero")
    BigDecimal amount,

    @NotBlank(message = "Specificare il tipo di saldo (REAL o WITHDRAWABLE)")
    String balanceType,

    @NotBlank(message = "La causale è obbligatoria per l'audit")
    String reason
) {}