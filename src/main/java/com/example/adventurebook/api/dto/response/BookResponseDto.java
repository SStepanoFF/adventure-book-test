package com.example.adventurebook.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** GET /api/books, GET /api/books/{id}, POST /api/books (201). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookResponseDto(long id, String title, String author, String description,
                           List<String> tags, int sectionCount) {
}
