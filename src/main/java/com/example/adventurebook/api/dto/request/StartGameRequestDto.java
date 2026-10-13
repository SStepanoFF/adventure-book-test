package com.example.adventurebook.api.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Body of POST /api/games ({@code Long} so that "missing bookId" can be expressed as null). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StartGameRequestDto(Long bookId) {
}
