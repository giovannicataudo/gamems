package it.gamems.wallet_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * ========================================================
 * INTERCEPTOR GLOBALE: GlobalExceptionHandler
 * ========================================================
 * @RestControllerAdvice dice a Spring: "Qualsiasi eccezione 
 * venga lanciata da qualsiasi Controller in questo microservizio, 
 * falla passare da qui prima di rispondere al client".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Definiamo il logger specifico per tracciare gli errori critici sul server
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * GESTORE 1: Errori di Business logic (es. Saldo insufficiente)
     * Intercetta la nostra WalletOperationException e restituisce un 400 Bad Request.
     */
    @ExceptionHandler(WalletOperationException.class)
    public ResponseEntity<ErrorResponseDto> handleWalletOperationException(WalletOperationException ex) {
        
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request - Business Rule Violation",
                ex.getMessage()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * GESTORE 2: Errori di Validazione DTO
     * Intercetta le eccezioni generate dalle annotazioni @Valid (es. @NotNull, @DecimalMin).
     * Estrae tutti i messaggi dai campi falliti e li unisce in una singola stringa.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        
        // Es. trasforma gli errori in: "amount: L'importo non può essere nullo"
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                errorMessage
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * GESTORE 4: Errori di Autorizzazione (403 Forbidden)
     * Intercetta i blocchi di Spring Security (es. fallimento del controllo @PreAuthorize).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException ex) {
        
        log.warn("Tentativo di accesso negato a una rotta protetta: {}", ex.getMessage());
        
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Access Denied",
                "Non hai i permessi necessari per eseguire questa operazione."
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Gestione dei problemi con i provider di pagamento esterni -> 503 Service Unavailable
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ErrorResponseDto> handlePaymentGatewayException(PaymentGatewayException ex) {
        ErrorResponseDto response = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Payment Provider Error",
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * GESTORE: Errore di sintassi JSON o Body mancante -> 400 Bad Request
     * Scatta quando il client invia un JSON malformato (es. virgole mancanti, tipi errati).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleJsonSyntaxError(HttpMessageNotReadableException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON Request",
                "Impossibile leggere la richiesta. Controlla la sintassi del JSON o assicurati di aver inviato il body corretto."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * GESTORE: Metodo HTTP non supportato -> 405 Method Not Allowed
     * Scatta quando il client chiama una rotta esistente ma con il verbo sbagliato (es. GET invece di POST).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String supportedMethods = ex.getSupportedHttpMethods() != null ? 
                                  ex.getSupportedHttpMethods().toString() : "Sconosciuto";
                                  
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                "Metodo HTTP non supportato per questa rotta. Metodi ammessi: " + supportedMethods
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Se c'è una concorrenza il servizio aspetta e retrya 3 volte ma superate queste
    // tre volte viene sollevato questo errore.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        log.warn("Conflitto di concorrenza rilevato sul Wallet. Transazione respinta.");
        // Il codice 409 Conflict istruisce il frontend che la richiesta era valida, 
        // ma lo stato del server è cambiato nel frattempo.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Elaborazione in corso. Riprova tra un istante.");
    }

    /**
     * GESTORE 3: Fallback Globale (Catch-All)
     * Intercetta qualsiasi altro errore imprevisto (es. Database offline, NullPointer).
     * Nasconde la stack trace vera al client per sicurezza, ma la salva nei log del server.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        
        // CRITICO: Logghiamo l'eccezione completa di stack trace.
        // Questo apparirà nei log del container Docker/Console, ma NON verrà inviato al client.
        log.error("ERRORE DI SISTEMA NON GESTITO: ", ex);
        
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Si è verificato un errore imprevisto. Contatta il supporto tecnico."
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}