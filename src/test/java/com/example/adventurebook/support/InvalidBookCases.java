package com.example.adventurebook.support;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.payload.BookPayloads;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * One catalogue of "broken book" cases shared by the API and the UI tests: the same rule is verified on both
 * levels with the same data. {@code errorFragment} is a part of the message the backend reports.
 */
public final class InvalidBookCases {

    private InvalidBookCases() {
    }

    public record Case(String name, BookRequestDto book, String errorFragment) {
        @Override
        public String toString() {
            return name;
        }
    }

    public static Stream<Arguments> structuralRules() {
        return Stream.of(
                new Case("no beginning", BookPayloads.noBeginning(), "exactly one BEGIN"),
                new Case("two beginnings", BookPayloads.twoBeginnings(), "exactly one BEGIN"),
                new Case("no ending", BookPayloads.noEnding(), "at least one END"),
                new Case("invalid next section id", BookPayloads.invalidNextSection(), "unknown section id"),
                new Case("non-ending section without options field", BookPayloads.nonEndingWithoutOptionsField(),
                        "must have at least one option"),
                new Case("duplicate section ids", BookPayloads.duplicateSectionIds(), "Duplicate section id"),
                new Case("non-numeric consequence", BookPayloads.nonNumericConsequence(), "non-numeric")
        ).map(Arguments::of);
    }
}
