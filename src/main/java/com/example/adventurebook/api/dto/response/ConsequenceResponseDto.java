package com.example.adventurebook.api.dto.response;

import com.example.adventurebook.api.dto.type.ConsequenceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsequenceResponseDto(ConsequenceType type, int value, String text) {
}
