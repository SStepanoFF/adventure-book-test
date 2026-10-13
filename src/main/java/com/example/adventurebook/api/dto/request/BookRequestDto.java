package com.example.adventurebook.api.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Body of POST /api/books. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookRequestDto(String title, String author, String description,
                          List<String> tags, List<SectionRequestDto> sections) {
}
