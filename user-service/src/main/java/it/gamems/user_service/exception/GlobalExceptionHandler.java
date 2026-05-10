package it.gamems.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * ========================================================
 * INTERCEPTOR: GlobalExceptionHandler
 * ========================================================
 * Cattura le eccezioni lanciate dai Service e dai Controller
 * e le traduce nel nostro ErrorResponseDto standard.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Errore di Registrazione: Email già presente -> 409 CONFLICT
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Registration Conflict", ex.getMessage());
    }

    // 2. Errore di Login: Password errata o Utente inesistente -> 401 UNAUTHORIZED
    // Questa eccezione viene lanciata in automatico dall'AuthenticationManager di Spring Security
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Email o password non validi.");
    }

    // 3. Errore di Validazione: Payload JSON errato (es. email senza chiocciola) -> 400 BAD REQUEST
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", errorMessage);
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

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDto> handleDisabledException(DisabledException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Account Locked", ex.getMessage());
    }

    // 4. Fallback: Errore imprevisto del server -> 500 INTERNAL SERVER ERROR
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        // In produzione, qui si loggherebbe lo stacktrace completo
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Si è verificato un errore imprevisto.");
    }

    // Metodo che traduce le eccezioni nel DTO
    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponseDto response = new ErrorResponseDto(LocalDateTime.now(), status.value(), error, message);
        return new ResponseEntity<>(response, status);
    }
}