package com.example.adventurebook.api.payload;

import com.example.adventurebook.api.dto.request.ConsequenceRequestDto;
import com.example.adventurebook.api.dto.type.ConsequenceType;
import com.example.adventurebook.api.dto.request.OptionRequestDto;

/** Short factories for options, to keep book definitions readable. */
public final class Options {

    private Options() {
    }

    public static OptionRequestDto go(int gotoId, String description) {
        return new OptionRequestDto(description, gotoId, null);
    }

    /** Option that costs {@code damage} health points. */
    public static OptionRequestDto hurt(int gotoId, String description, int damage, String flavourText) {
        return new OptionRequestDto(description, gotoId,
                new ConsequenceRequestDto(ConsequenceType.LOSE_HEALTH, String.valueOf(damage), flavourText));
    }

    /** Option with a free-form consequence value (for invalid / edge-case values). */
    public static OptionRequestDto hurtRaw(int gotoId, String description, String rawValue) {
        return new OptionRequestDto(description, gotoId,
                new ConsequenceRequestDto(ConsequenceType.LOSE_HEALTH, rawValue, "raw consequence"));
    }
}
