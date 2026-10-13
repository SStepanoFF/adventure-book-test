package com.example.adventurebook.api.payload;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.request.OptionRequestDto;
import com.example.adventurebook.api.dto.request.SectionRequestDto;
import com.example.adventurebook.api.dto.type.SectionType;
import com.example.adventurebook.data.Unique;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Fluent builder of request bodies. Defaults give a unique title and tag, so every test owns its data.
 * Deliberately separate from the DTOs (shape of the contract) and from the services (HTTP calls).
 */
public final class BookBuilder {

    // Lombok generates the fluent setters title(..), author(..), description(..) returning the builder
    @Setter @Accessors(fluent = true, chain = true) private String title = Unique.name("QA Book");
    @Setter @Accessors(fluent = true, chain = true) private String author = "QA Bot";
    @Setter @Accessors(fluent = true, chain = true) private String description = "Created by the automated test suite";
    private List<String> tags = new ArrayList<>(List.of(Unique.tag()));
    private final List<SectionRequestDto> sections = new ArrayList<>();

    private BookBuilder() {
    }

    public static BookBuilder aBook() {
        return new BookBuilder();
    }

    public BookBuilder tags(String... tags) {
        this.tags = tags == null ? null : new ArrayList<>(Arrays.asList(tags));
        return this;
    }

    public BookBuilder begin(int id, String text, OptionRequestDto... options) {
        return section(id, SectionType.BEGIN, text, options);
    }

    public BookBuilder node(int id, String text, OptionRequestDto... options) {
        return section(id, SectionType.NODE, text, options);
    }

    public BookBuilder end(int id, String text) {
        return section(id, SectionType.END, text, new OptionRequestDto[0]);
    }

    public BookBuilder section(int id, SectionType type, String text, OptionRequestDto... options) {
        sections.add(new SectionRequestDto(id, text, type, new ArrayList<>(Arrays.asList(options))));
        return this;
    }

    /** Section with the {@code options} field omitted entirely (null). */
    public BookBuilder sectionWithoutOptionsField(int id, SectionType type, String text) {
        sections.add(new SectionRequestDto(id, text, type, null));
        return this;
    }

    public BookRequestDto build() {
        return new BookRequestDto(title, author, description, tags, new ArrayList<>(sections));
    }
}
