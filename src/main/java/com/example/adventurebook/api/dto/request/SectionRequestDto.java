package com.example.adventurebook.api.dto.request;

import com.example.adventurebook.api.dto.type.SectionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** @param options null = field omitted from the JSON; empty list = {@code "options": []} */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SectionRequestDto(int id, String text, SectionType type, List<OptionRequestDto> options) {
}
