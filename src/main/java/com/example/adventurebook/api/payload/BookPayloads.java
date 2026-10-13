package com.example.adventurebook.api.payload;

import static com.example.adventurebook.api.payload.BookBuilder.aBook;
import static com.example.adventurebook.api.payload.Options.go;
import static com.example.adventurebook.api.payload.Options.hurt;
import static com.example.adventurebook.api.payload.Options.hurtRaw;

import com.example.adventurebook.api.dto.request.BookRequestDto;
import com.example.adventurebook.api.dto.type.SectionType;

/**
 * Ready-made request bodies for the scenarios the tests need. Valid books are tiny and deterministic;
 * every invalid book breaks exactly ONE rule so a failing test points at one cause.
 */
public final class BookPayloads {

    public static final String SECTION_1 = "Section 1: you stand at a crossroads.";
    public static final String SECTION_2 = "Section 2: you cross the old bridge.";
    public static final String SECTION_3 = "Section 3: a cave opens in front of you.";
    public static final String SECTION_END = "The end: you found the treasure.";

    private BookPayloads() {
    }

    // =============== valid books ===============

    /** 1 BEGIN -> 2 END. */
    public static BookRequestDto minimal() {
        return aBook()
                .begin(1, SECTION_1, go(2, "Walk home"))
                .end(2, SECTION_END)
                .build();
    }

    /** 1 BEGIN -> 2 NODE -> 3 NODE -> 4 END, every step has exactly one option (index 0). No damage. */
    public static BookRequestDto threeStepStory() {
        return aBook()
                .begin(1, SECTION_1, go(2, "Open the door"))
                .node(2, SECTION_2, go(3, "Cross the bridge"))
                .node(3, SECTION_3, go(4, "Enter the cave"))
                .end(4, SECTION_END)
                .build();
    }

    /**
     * Option 0 = safe path to the END. Option 1 = risky path costing {@code damage} health into NODE 2,
     * from where option 0 leads to the END. With damage >= 10 the risky option kills the player at NODE 2.
     */
    public static BookRequestDto withRiskyChoice(int damage) {
        return aBook()
                .begin(1, SECTION_1, go(3, "Take the safe path"),
                        hurt(2, "Jump over the cliff", damage, "You hit the rocks hard."))
                .node(2, SECTION_2, go(3, "Limp onwards"))
                .end(3, SECTION_END)
                .build();
    }

    /** Two consecutive hits of 6 (total 12 > 10 starting health). Option 0 everywhere. */
    public static BookRequestDto twoHitsOfSix() {
        return aBook()
                .begin(1, SECTION_1, hurt(2, "Run through the fire", 6, "The flames burn you."))
                .node(2, SECTION_2, hurt(3, "Run through the fire again", 6, "Burned again."))
                .node(3, SECTION_3, go(4, "Keep going"))
                .end(4, SECTION_END)
                .build();
    }

    /** The damaging option leads DIRECTLY to an END section. */
    public static BookRequestDto lethalChoiceIntoEnding() {
        return aBook()
                .begin(1, SECTION_1, hurt(2, "Drink the poison", 10, "The poison is fast."))
                .end(2, SECTION_END)
                .build();
    }

    /** A "negative damage" consequence (value "-5"): the validator only checks that it is numeric. */
    public static BookRequestDto negativeDamage() {
        return aBook()
                .begin(1, SECTION_1, hurtRaw(2, "Drink the strange potion", "-5"))
                .node(2, SECTION_2, go(3, "Continue"))
                .end(3, SECTION_END)
                .build();
    }

    // =============== invalid books: exactly one broken rule each ===============

    /** Rule: exactly one BEGIN - there is none. */
    public static BookRequestDto noBeginning() {
        return aBook()
                .node(1, SECTION_1, go(2, "Continue"))
                .end(2, SECTION_END)
                .build();
    }

    /** Rule: exactly one BEGIN - there are two. */
    public static BookRequestDto twoBeginnings() {
        return aBook()
                .begin(1, SECTION_1, go(3, "Left"))
                .begin(2, SECTION_2, go(3, "Right"))
                .end(3, SECTION_END)
                .build();
    }

    /** Rule: at least one END - there is none (the story loops forever). */
    public static BookRequestDto noEnding() {
        return aBook()
                .begin(1, SECTION_1, go(2, "Continue"))
                .node(2, SECTION_2, go(1, "Go back"))
                .build();
    }

    /** Rule: every option must point to an existing section. */
    public static BookRequestDto invalidNextSection() {
        return aBook()
                .begin(1, SECTION_1, go(2, "Continue"), go(99, "Walk into the void"))
                .end(2, SECTION_END)
                .build();
    }

    /** Rule: a non-END section must have options - the {@code options} field is omitted (null). */
    public static BookRequestDto nonEndingWithoutOptionsField() {
        return aBook()
                .begin(1, SECTION_1, go(2, "Continue"), go(3, "Other way"))
                .sectionWithoutOptionsField(2, SectionType.NODE, SECTION_2)
                .end(3, SECTION_END)
                .build();
    }

    /** Rule: a non-END section must have options - {@code "options": []}. */
    public static BookRequestDto nonEndingWithEmptyOptions() {
        return aBook()
                .begin(1, SECTION_1, go(2, "Continue"), go(3, "Other way"))
                .node(2, SECTION_2)
                .end(3, SECTION_END)
                .build();
    }

    /** Extra rule enforced by the backend: section ids are unique. */
    public static BookRequestDto duplicateSectionIds() {
        return aBook()
                .begin(1, SECTION_1, go(2, "Continue"))
                .end(2, SECTION_END)
                .end(2, SECTION_END)
                .build();
    }

    /** Consequence value must be an integer. */
    public static BookRequestDto nonNumericConsequence() {
        return aBook()
                .begin(1, SECTION_1, hurtRaw(2, "Touch the idol", "lots"))
                .end(2, SECTION_END)
                .build();
    }

    /** Field validation: blank title. */
    public static BookRequestDto blankTitle() {
        return aBook().title("  ")
                .begin(1, SECTION_1, go(2, "Continue"))
                .end(2, SECTION_END)
                .build();
    }

    /** Field validation: no sections at all. */
    public static BookRequestDto noSections() {
        return aBook().build();
    }
}
