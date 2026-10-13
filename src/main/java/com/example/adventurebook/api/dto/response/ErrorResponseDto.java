package com.example.adventurebook.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponseDto(String message, List<String> details, String timestamp) {

    public List<String> detailsOrEmpty() {
        return details == null ? List.of() : details;
    }
}
