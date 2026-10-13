package com.example.adventurebook.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Viewport presets for responsive testing (Playwright emulation, no real device needed). */
@Getter
@RequiredArgsConstructor
public enum DeviceProfile {
    DESKTOP(1440, 900, false),
    TABLET_LANDSCAPE(1024, 768, true),
    TABLET_PORTRAIT(768, 1024, true);

    private final int width;
    private final int height;
    private final boolean touch;
}
