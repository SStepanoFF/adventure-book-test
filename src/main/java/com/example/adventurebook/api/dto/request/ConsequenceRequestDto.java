package com.example.adventurebook.api.dto.request;

import com.example.adventurebook.api.dto.type.ConsequenceType;
import com.fasterxml.jackson.annotation.JsonInclude;

/** @param value magnitude as a STRING, per the book JSON contract (e.g. "6") */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsequenceRequestDto(ConsequenceType type, String value, String text) {
}
