package com.example.adventurebook.api.dto.response;

import com.example.adventurebook.api.dto.type.GameStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of every game endpoint. Only the fields the tests care about are declared; anything else the backend
 * adds later is ignored (see @JsonIgnoreProperties) - additions are caught by the contract tests instead.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameStateResponseDto(String sessionId, long bookId, String bookTitle, int health, int maxHealth,
                                GameStatus status, boolean gameOver, SectionResponseDto currentSection,
                                ConsequenceResponseDto lastConsequence) {
}
