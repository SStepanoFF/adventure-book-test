package com.example.adventurebook.api.dto.response;

import com.example.adventurebook.api.dto.type.SectionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SectionResponseDto(int id, String text, SectionType type, List<OptionResponseDto> options) {
}
