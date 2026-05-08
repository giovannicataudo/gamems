package it.gamems.game_service.enums;

/**
 * Contiene i tre stati possibili di una partita
 * Le partite verrano salvate "istantanemente" in stato PENDING
 * e poi aggiornate in base a se la scrittura sul db è andata a buon fine
 */
public enum GameStatus {
    PENDING,
    COMPLETED,
    FAILED
}