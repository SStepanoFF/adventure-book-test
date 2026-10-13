package com.example.adventurebook.api.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OptionRequestDto(String description, int gotoId, ConsequenceRequestDto consequence) {
}
