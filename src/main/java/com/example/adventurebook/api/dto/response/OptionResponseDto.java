package com.example.adventurebook.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Magnitude/text of a consequence are intentionally hidden until the option is chosen. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OptionResponseDto(int index, String description, boolean hasConsequence) {
}
